#!/usr/bin/env python3
"""
اسکریپت آپدیت هفتگی خودکار وابستگی‌های MarbleBr.
آخرین نسخه stable یا beta کتابخانه‌ها (GeckoView, Compose BOM, Kotlin, AGP, Room, WorkManager, Hilt)
را از مخازن رسمی Maven بررسی کرده و در صورت وجود نسخه جدید، gradle/libs.versions.toml و CHANGELOG.md را به‌روز می‌کند.
"""

from __future__ import annotations

import datetime
import re
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

TOML_PATH = Path("gradle/libs.versions.toml")
CHANGELOG_PATH = Path("CHANGELOG.md")

MAVEN_TARGETS = {
    "geckoview": "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml",
    "composeBom": "https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml",
    "room": "https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/maven-metadata.xml",
    "work": "https://dl.google.com/dl/android/maven2/androidx/work/work-runtime-ktx/maven-metadata.xml",
    "credentials": "https://dl.google.com/dl/android/maven2/androidx/credentials/credentials/maven-metadata.xml",
    "biometric": "https://dl.google.com/dl/android/maven2/androidx/biometric/biometric/maven-metadata.xml",
    "hilt": "https://repo1.maven.org/maven2/com/google/dagger/hilt-android/maven-metadata.xml",
}

STABLE_OR_BETA_RE = re.compile(r"^[0-9]+\.[0-9]+(\.[0-9]+)?((-beta|-rc|\.)[0-9]+)?$")


def fetch_latest_version(metadata_url: str) -> str | None:
    try:
        req = urllib.request.Request(metadata_url, headers={"User-Agent": "MarbleBr-Updater/1.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            xml_bytes = resp.read()
        root = ET.fromstring(xml_bytes)
        release = root.findtext("./versioning/release")
        if release and STABLE_OR_BETA_RE.match(release):
            return release.strip()
        versions = [
            v.text.strip()
            for v in root.findall("./versioning/versions/version")
            if v.text and STABLE_OR_BETA_RE.match(v.text.strip())
        ]
        return versions[-1] if versions else None
    except Exception as exc:
        print(f"[WARN] Could not fetch {metadata_url}: {exc}")
        return None


def main() -> None:
    if not TOML_PATH.exists():
        return

    toml_text = TOML_PATH.read_text(encoding="utf-8")
    updates: list[str] = []

    for key, url in MAVEN_TARGETS.items():
        latest = fetch_latest_version(url)
        if not latest:
            continue
        pattern = re.compile(rf'^({re.escape(key)}\s*=\s*")([^"]+)(")', re.MULTILINE)
        match = pattern.search(toml_text)
        if match and match.group(2) != latest:
            old_ver = match.group(2)
            toml_text = pattern.sub(rf"\g<1>{latest}\g<3>", toml_text)
            updates.append(f"- ارتقای `{key}` از نسخه `{old_ver}` به `{latest}`")
            print(f"[UPDATE] {key}: {old_ver} -> {latest}")

    if not updates:
        print("All checked dependencies are already up to date.")
        return

    TOML_PATH.write_text(toml_text, encoding="utf-8")

    if CHANGELOG_PATH.exists():
        changelog = CHANGELOG_PATH.read_text(encoding="utf-8")
        today = datetime.date.today().isoformat()
        entry = (
            f"## [Unreleased] - {today}\n\n"
            f"### 🛠 بهبود\n"
            + "\n".join(updates)
            + "\n\n"
        )
        first_header = changelog.find("## [")
        if first_header != -1:
            changelog = changelog[:first_header] + entry + changelog[first_header:]
        else:
            changelog = changelog + "\n" + entry
        CHANGELOG_PATH.write_text(changelog, encoding="utf-8")


if __name__ == "__main__":
    main()
