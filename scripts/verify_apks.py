#!/usr/bin/env python3
"""Gate release artifacts before they are tested, uploaded, or offered to users."""

import argparse
import hashlib
import json
import os
from shutil import copyfile
from pathlib import Path
import re
import struct
import subprocess
import sys
from zipfile import BadZipFile, ZipFile

ABIS = {"arm64-v8a": (2, 183), "armeabi-v7a": (1, 40), "x86_64": (2, 62)}
VARIANTS = (*ABIS, "universal")
PACKAGE = "com.marble098.marblebr"
MIN_SDK = 26
GECKO_LIBS = {"libxul.so", "libmozglue.so"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def build_tools():
    sdk = os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"]
    candidates = [p for p in (Path(sdk) / "build-tools").iterdir()
                  if re.fullmatch(r"\d+\.\d+\.\d+", p.name)
                  and all((p / tool).is_file() for tool in ("apksigner", "aapt2", "zipalign"))]
    require(candidates, "No stable Android SDK build-tools installed")
    return max(candidates, key=lambda p: tuple(map(int, p.name.split("."))))


def inspect_identity(apk, tools):
    def tool(name, *args):
        result = subprocess.run([str(tools / name), *args], stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True)
        require(result.returncode == 0, f"{name} failed for {apk.name}:\n{result.stdout}")
        return result.stdout

    signature = tool("apksigner", "verify", "--min-sdk-version", str(MIN_SDK),
                     "--verbose", "--print-certs", str(apk))
    certs = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})$", signature, re.M)
    require(len(certs) == 1, f"{apk.name}: expected one verified signing certificate")
    badging = tool("aapt2", "dump", "badging", str(apk))
    package = next((line for line in badging.splitlines() if line.startswith("package: ")), "")
    fields = dict(re.findall(r"(\w+)='([^']*)'", package))
    require(fields.get("name") == PACKAGE and not fields.get("split"),
            f"{apk.name}: not a standalone {PACKAGE} APK: {package}")
    return fields["versionName"], int(fields["versionCode"]), certs[0], badging, tool


def verify_16k_segments(lib, header, label):
    phoff = struct.unpack_from("<Q", header, 32)[0]
    phentsize, phnum = struct.unpack_from("<HH", header, 54)
    require(phoff >= 64 and phentsize == 56 and 0 < phnum < 4096, f"{label}: invalid ELF program table")
    lib.seek(phoff)
    loadable = False
    for _ in range(phnum):
        data = lib.read(phentsize)
        require(len(data) == phentsize, f"{label}: truncated ELF program table")
        p_type, _, offset, vaddr, _, _, _, alignment = struct.unpack("<II6Q", data)
        if p_type == 1:  # PT_LOAD
            loadable = True
            require(alignment >= 16384 and (vaddr - offset) % 16384 == 0,
                    f"{label}: ELF load segments must support 16 KB Android memory pages")
    require(loadable, f"{label}: missing ELF load segments")


def native_inventory(apk, expected_abis):
    libs = {abi: {} for abi in expected_abis}
    with ZipFile(apk) as archive:
        require(len(archive.namelist()) == len(set(archive.namelist())),
                f"{apk.name}: duplicate ZIP entries")
        for entry in archive.infolist():
            if not entry.filename.startswith("lib/") or entry.is_dir():
                continue
            path = entry.filename.split("/")
            require(len(path) == 3 and path[1] in libs and path[2].endswith(".so"),
                    f"{apk.name}: unexpected native entry {entry.filename}")
            _, abi, name = path
            elf_class, machine = ABIS[abi]
            label = f"{apk.name}: {entry.filename}"
            with archive.open(entry) as lib:
                header = lib.read(64 if elf_class == 2 else 52)
                require(len(header) >= 20 and header[:6] == b"\x7fELF" + bytes([elf_class, 1])
                        and int.from_bytes(header[18:20], "little") == machine,
                        f"{label} is not a {abi} ELF library")
                if elf_class == 2:
                    require(len(header) == 64, f"{label}: truncated ELF header")
                    verify_16k_segments(lib, header, label)
            libs[abi][name] = (entry.file_size, entry.CRC)
        for abi, inventory in libs.items():
            require(GECKO_LIBS <= inventory.keys(),
                    f"{apk.name}: missing GeckoView for {abi}: {GECKO_LIBS - inventory.keys()}")
    return libs


def output_apks(directory):
    metadata = json.loads((directory / "output-metadata.json").read_text())
    apks = {}
    for entry in metadata["elements"]:
        filters = entry["filters"]
        if filters:
            require(len(filters) == 1 and filters[0]["filterType"] == "ABI",
                    f"Unexpected APK filters: {filters}")
        abi = filters[0].get("value", filters[0].get("identifier")) if filters else "universal"
        name = entry["outputFile"]
        require(abi in VARIANTS and abi not in apks and Path(name).name == name
                and name.endswith(".apk"), f"Unexpected or duplicate APK output: {entry}")
        apks[abi] = directory / name
    require(set(apks) == set(VARIANTS) and len(set(apks.values())) == len(VARIANTS)
            and set(apks.values()) == set(directory.glob("*.apk")),
            f"Expected one APK per supported variant, found {apks}")
    return apks


def file_hash(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def verify(directory, version, code, tools, baselines):
    apks = output_apks(directory)
    signer = None
    inventories = {}
    for abi, apk in apks.items():
        name, actual_code, cert, badging, tool = inspect_identity(apk, tools)
        require((name, actual_code) == (version, code), f"{apk.name}: wrong version {name}/{actual_code}")
        require(f"sdkVersion:'{MIN_SDK}'" in badging.splitlines()
                and not any(line.startswith("maxSdkVersion:") for line in badging.splitlines()),
                f"{apk.name}: unexpected supported Android version range")
        manifest = tool("aapt2", "dump", "xmltree", str(apk), "--file", "AndroidManifest.xml")
        for attr in ("testOnly", "debuggable", "isSplitRequired"):
            require(not re.search(rf"A: android:{attr}(?:\([^)]*\))?=(?:\(type 0x12\))?(?:0xffffffff|true)\b",
                                  manifest), f"{apk.name}: android:{attr} must not be true")
        require(not re.search(r"^\s*E: uses-split\b", manifest, re.M), f"{apk.name}: requires a split APK")
        tool("zipalign", "-c", "-P", "16", "4", str(apk))
        inventories[abi] = native_inventory(apk, ABIS if abi == "universal" else (abi,))
        require(signer is None or signer == cert, f"{apk.name}: a different signer from the other outputs")
        signer = cert
        print(f"Verified {apk.name} ({abi}, {name}/{actual_code}, signer {cert})")
    for abi in ABIS:
        require(inventories["universal"][abi] == inventories[abi][abi],
                f"Universal APK native payload differs from the {abi} standalone APK")
    for apk in baselines:
        _, previous_code, previous_cert, _, _ = inspect_identity(apk, tools)
        require(signer == previous_cert, f"Signing certificate changed since {apk.name}; updates would fail")
        require(code > previous_code, f"versionCode {code} must exceed {previous_code} in {apk.name}")
    return apks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("staging", type=Path)
    parser.add_argument("--baseline", type=Path, action="append", default=[])
    parser.add_argument("--properties", type=Path, default=Path("gradle.properties"))
    args = parser.parse_args()
    properties = dict(line.split("=", 1) for line in args.properties.read_text().splitlines()
                      if line.startswith("MARBLE_VERSION_"))
    version, code = properties["MARBLE_VERSION_NAME"], int(properties["MARBLE_VERSION_CODE"])
    require(re.fullmatch(r"\d+\.\d+\.\d+", version) and 0 < code <= 2_100_000_000,
            "Invalid expected release version")
    apks = verify(args.directory, version, code, build_tools(), args.baseline)
    args.staging.mkdir()  # Fail on stale outputs rather than reusing them.
    checksums = []
    for abi in VARIANTS:
        dest = args.staging / f"MarbleBr-v{version}-{abi}.apk"
        copyfile(apks[abi], dest)
        checksums.append(f"{file_hash(dest)}  {dest.name}\n")
    (args.staging / "SHA256SUMS").write_text("".join(checksums))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, BadZipFile) as exc:
        sys.exit(f"APK verification failed: {exc}")
