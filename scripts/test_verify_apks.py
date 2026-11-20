#!/usr/bin/env python3
import json
from pathlib import Path
import struct
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from zipfile import ZipFile

import verify_apks as gate


class ApkGateTest(unittest.TestCase):
    def setUp(self):
        tmp = TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        self.meta = self.root / "output-metadata.json"
        self.apks = {abi: self.root / f"app-{abi}-release.apk" for abi in gate.VARIANTS}
        self.entries = [{"outputFile": apk.name, "filters": [] if abi == "universal" else
                         [{"filterType": "ABI", "value": abi}]} for abi, apk in self.apks.items()]
        self.meta.write_text(json.dumps({"elements": self.entries}))
        for abi, apk in self.apks.items():
            self.write_apk(apk, gate.ABIS if abi == "universal" else (abi,))

    def write_apk(self, apk, abis, libraries=gate.GECKO_LIBS, machine_override=None, alignment=16384):
        with ZipFile(apk, "w") as archive:
            for abi in abis:
                elf_class, machine = gate.ABIS[abi]
                for lib in libraries:
                    elf = (b"\x7fELF" + bytes([elf_class, 1]) + bytes(12)
                           + (machine_override or machine).to_bytes(2, "little"))
                    if elf_class == 2:
                        header = bytearray(64)
                        header[:20] = elf
                        struct.pack_into("<Q", header, 32, 64)
                        struct.pack_into("<HH", header, 54, 56, 1)
                        elf = header + struct.pack("<II6Q", 1, 5, 0, 0, 0, 0, 0, alignment)
                    archive.writestr(f"lib/{abi}/{lib}", elf)

    def test_exact_output_set(self):
        self.assertEqual(gate.output_apks(self.root), self.apks)
        (self.root / "stale-x86.apk").touch()
        with self.assertRaisesRegex(ValueError, "one APK per supported variant"):
            gate.output_apks(self.root)

    def test_metadata_cannot_point_outside_build(self):
        self.entries[0]["outputFile"] = "../bad.apk"
        self.meta.write_text(json.dumps({"elements": self.entries}))
        with self.assertRaisesRegex(ValueError, "Unexpected or duplicate"):
            gate.output_apks(self.root)

    def test_mislabeled_or_missing_engine(self):
        apk = self.apks["arm64-v8a"]
        self.write_apk(apk, ("arm64-v8a",), machine_override=62)
        with self.assertRaisesRegex(ValueError, "not a arm64-v8a ELF"):
            gate.native_inventory(apk, ("arm64-v8a",))
        self.write_apk(apk, ("arm64-v8a",), libraries=("libxul.so",))
        with self.assertRaisesRegex(ValueError, "missing GeckoView"):
            gate.native_inventory(apk, ("arm64-v8a",))

    def test_reject_4k_only_64bit_native_payload(self):
        apk = self.apks["arm64-v8a"]
        self.write_apk(apk, ("arm64-v8a",), alignment=4096)
        with self.assertRaisesRegex(ValueError, "support 16 KB"):
            gate.native_inventory(apk, ("arm64-v8a",))

    def test_upgrade_and_universal_payload_checks(self):
        baseline = self.root / "previous.apk"

        def inspect(apk, _tools):
            result = previous if apk == baseline else ("2.0.0", 42, "same")
            return (*result, "sdkVersion:'26'", lambda *_args: "")

        with patch.object(gate, "inspect_identity", side_effect=inspect):
            previous = ("1.0.0", 41, "same")
            self.assertEqual(gate.verify(self.root, "2.0.0", 42, None, [baseline]), self.apks)
            previous = ("1.0.0", 41, "other")
            with self.assertRaisesRegex(ValueError, "Signing certificate changed"):
                gate.verify(self.root, "2.0.0", 42, None, [baseline])
            previous = ("1.0.0", 43, "same")
            with self.assertRaisesRegex(ValueError, "versionCode 42 must exceed 43"):
                gate.verify(self.root, "2.0.0", 42, None, [baseline])
            self.write_apk(self.apks["universal"], gate.ABIS, gate.GECKO_LIBS | {"extra.so"})
            with self.assertRaisesRegex(ValueError, "Universal APK native payload differs"):
                gate.verify(self.root, "2.0.0", 42, None, [])


if __name__ == "__main__":
    unittest.main()
