#!/usr/bin/env python3
"""
آپدیت یکپارچه و خودکار تمام کتابخانه‌ها و تکنولوژی‌های MarbleBr.

این اسکریپت به‌جای آپدیت‌های پراکنده (Dependabot / Renovate / اسکریپت‌های ناقص)،
تمامی وابستگی‌ها را در یک اجرا و در یک Workflow به‌روز می‌کند:

  • ابزارهای Gradle، Kotlin، KSP، Hilt، Ktlint و Detekt (لیست کامل Version Catalog)
  • کتابخانه‌های AndroidX / Compose / Room / Work / DataStore / Credentials و ...
  • موتور مرورگر Mozilla GeckoView (از مخزن رسمی maven.mozilla.org)
  • GitHub Actions استفاده‌شده در تمام فایل‌های Workflow
  • نسخه Gradle Wrapper (distributionUrl در gradle/wrapper/gradle-wrapper.properties)

پارامترها:
  --apply            تغییرات را در فایل‌ها بنویس
  --check            فقط بررسی و گزارش (بدون نوشتن)
  --json-out PATH    گزارش نهایی را به‌صورت JSON ذخیره کن
  --max-age-days N   حداکثر سن یک نسخه برای انتخاب به‌عنوان «پایدار» (پیش‌فرض ۳۰)
  --include-pre      نسخه‌های پیش‌انتشار (alpha/beta/rc) را نیز در نظر بگیر

خروجی: کد صفر در هر حالت موفق؛ در صورت خطای غیرمنتظره کد غیرصفر.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable

TOML_PATH = Path("gradle/libs.versions.toml")
CHANGELOG_PATH = Path("CHANGELOG.md")
GRADLE_PROPS_PATH = Path("gradle.properties")
WRAPPER_PATH = Path("gradle/wrapper/gradle-wrapper.properties")
WORKFLOWS_DIR = Path(".github/workflows")

GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2"
CENTRAL_MAVEN = "https://repo1.maven.org/maven2"
MOZILLA_MAVEN = "https://maven.mozilla.org/maven2"
GRADLE_SERVICES = "https://services.gradle.org"

HTTP_TIMEOUT = 30
USER_AGENT = "MarbleBr-Dependency-Updater/2.0 (+https://github.com/marble098/MarbleBr)"

# نسخه‌های معتبر SemVer-style که MarbleBr از آن‌ها پشتیبانی می‌کند
VERSION_RE = re.compile(
    r"^v?(?P<major>\d+)\.(?P<minor>\d+)(?:\.(?P<patch>\d+))?"
    r"(?:[.-](?P<pre>alpha|beta|rc|dev|eap|M|m)[.-]?(?P<prenum>\d+)?)?$"
)

PRE_RANK = {"dev": -2, "eap": -1, "M": 1, "m": 1, "alpha": 2, "beta": 3, "rc": 4}

# --- نگاشت ورودی‌های Version Catalog به مختصات Maven -------------------------
# کلیدها نام ورودی در بخش [versions] فایل libs.versions.toml هستند.
LIBRARY_TARGETS: dict[str, str] = {
    "coreKtx": f"{GOOGLE_MAVEN}/androidx/core/core-ktx/maven-metadata.xml",
    "appcompat": f"{GOOGLE_MAVEN}/androidx/appcompat/appcompat/maven-metadata.xml",
    "activityCompose": f"{GOOGLE_MAVEN}/androidx/activity/activity-compose/maven-metadata.xml",
    "lifecycle": f"{GOOGLE_MAVEN}/androidx/lifecycle/lifecycle-runtime-ktx/maven-metadata.xml",
    "navigationCompose": f"{GOOGLE_MAVEN}/androidx/navigation/navigation-compose/maven-metadata.xml",
    "composeBom": f"{GOOGLE_MAVEN}/androidx/compose/compose-bom/maven-metadata.xml",
    "material3Expressive": f"{GOOGLE_MAVEN}/androidx/compose/material3/material3/maven-metadata.xml",
    "materialIconsExtended": f"{GOOGLE_MAVEN}/androidx/compose/material/material-icons-extended/maven-metadata.xml",
    "coroutines": f"{CENTRAL_MAVEN}/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml",
    "serializationJson": f"{CENTRAL_MAVEN}/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml",
    "room": f"{GOOGLE_MAVEN}/androidx/room/room-runtime/maven-metadata.xml",
    "datastore": f"{GOOGLE_MAVEN}/androidx/datastore/datastore-preferences/maven-metadata.xml",
    "work": f"{GOOGLE_MAVEN}/androidx/work/work-runtime-ktx/maven-metadata.xml",
    "hilt": f"{CENTRAL_MAVEN}/com/google/dagger/hilt-android/maven-metadata.xml",
    "androidxHilt": f"{GOOGLE_MAVEN}/androidx/hilt/hilt-work/maven-metadata.xml",
    "androidxHiltNavigationCompose": f"{GOOGLE_MAVEN}/androidx/hilt/hilt-navigation-compose/maven-metadata.xml",
    "biometric": f"{GOOGLE_MAVEN}/androidx/biometric/biometric/maven-metadata.xml",
    "credentials": f"{GOOGLE_MAVEN}/androidx/credentials/credentials/maven-metadata.xml",
    "geckoview": f"{MOZILLA_MAVEN}/org/mozilla/geckoview/geckoview/maven-metadata.xml",
    "profileinstaller": f"{GOOGLE_MAVEN}/androidx/profileinstaller/profileinstaller/maven-metadata.xml",
    "macrobenchmark": f"{GOOGLE_MAVEN}/androidx/benchmark/benchmark-macro-junit4/maven-metadata.xml",
    "uiautomator": f"{GOOGLE_MAVEN}/androidx/test/uiautomator/uiautomator/maven-metadata.xml",
    "androidxTestExt": f"{GOOGLE_MAVEN}/androidx/test/ext/junit-ktx/maven-metadata.xml",
    "androidxTestCore": f"{GOOGLE_MAVEN}/androidx/test/core-ktx/maven-metadata.xml",
    "androidxTestRunner": f"{GOOGLE_MAVEN}/androidx/test/runner/maven-metadata.xml",
    "androidxTestRules": f"{GOOGLE_MAVEN}/androidx/test/rules/maven-metadata.xml",
    "espressoCore": f"{GOOGLE_MAVEN}/androidx/test/espresso/espresso-core/maven-metadata.xml",
    "robolectric": f"{CENTRAL_MAVEN}/org/robolectric/robolectric/maven-metadata.xml",
    "json": f"{CENTRAL_MAVEN}/org/json/json/maven-metadata.xml",
}

# پلاگین‌ها و ابزارهای بیلد
PLUGIN_TARGETS: dict[str, str] = {
    "agp": f"{GOOGLE_MAVEN}/com/android/tools/build/gradle/maven-metadata.xml",
    "kotlin": f"{CENTRAL_MAVEN}/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml",
    "ksp": f"{CENTRAL_MAVEN}/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml",
    "ktlintPlugin": f"{CENTRAL_MAVEN}/org/jlleitschuh/gradle/ktlint-gradle/maven-metadata.xml",
    "detektPlugin": f"{CENTRAL_MAVEN}/dev/detekt/detekt-gradle-plugin/maven-metadata.xml",
}

# نگه‌داشتن هم‌نسخگی میان نسخه‌های مرتبط (نسخه‌ها باید همیشه دقیقاً برابر بمانند)
LINKED_VERSIONS: list[tuple[str, ...]] = [
    ("hilt",),  # hilt-android و hilt-compiler و hilt-android-testing از dagger
    ("androidxHilt", "androidxHiltNavigationCompose"),
    ("kotlin",),  # kotlin-compose، kotlin-serialization و kotlin-metadata-jvm از Kotlin
]

# ورودی‌هایی که عمداً دنبال نمی‌شوند (بدون مخزن Maven یا نگه‌داری دستی)
SKIP_VERSION_KEYS = {"junit"}

# مخازن مجاز برای GitHub Actions (اکشن‌های رسمی و اکشن‌های شناخته‌شده)
ALLOWED_ACTION_REPOS = {
    "actions/checkout",
    "actions/setup-java",
    "actions/upload-artifact",
    "actions/download-artifact",
    "actions/cache",
    "gradle/actions",
    "gradle/gradle-build-action",
    "softprops/action-gh-release",
    "reactivecircus/android-emulator-runner",
}

ACTION_LINE_RE = re.compile(r"(?P<indent>\s*)(?P<key>uses:\s*)(?P<repo>[\w.-]+/[\w.-]+)(?:/(?P<sub>[\w.-]+))?@(?P<ref>\S+)")


# ---------------------------------------------------------------------------
# ابزارهای کمکی
# ---------------------------------------------------------------------------
@dataclass
class Update:
    """یک مورد ارتقای نسخه."""

    kind: str  # library | plugin | action | gradle | linked
    name: str
    old: str
    new: str
    location: str = ""

    def changelog_line(self) -> str:
        labels = {
            "library": "کتابخانه",
            "plugin": "پلاگین و ابزار بیلد",
            "action": "GitHub Action",
            "gradle": "Gradle Wrapper",
            "linked": "هم‌ترازسازی نسخه",
        }
        return f"- ارتقای {labels.get(self.kind, self.kind)} `{self.name}` از `{self.old}` به `{self.new}`"

    def as_dict(self) -> dict:
        return {"kind": self.kind, "name": self.name, "old": self.old, "new": self.new, "location": self.location}


@dataclass
class Report:
    updates: list[Update] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    skipped: list[str] = field(default_factory=list)

    @property
    def changed(self) -> bool:
        return bool(self.updates)


def log(message: str) -> None:
    print(message, flush=True)


def http_get(url: str) -> bytes | None:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "*/*"})
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT) as response:
            return response.read()
    except Exception as exc:  # noqa: BLE001 - شبکه در CI می‌تواند به هر دلیلی خطا دهد
        log(f"  [WARN] دریافت {url} ممکن نبود: {exc}")
        return None


def parse_version(raw: str) -> tuple[int, int, int, int, int] | None:
    """نسخه را به کلید مرتب‌سازی تبدیل می‌کند؛ None برای نسخه‌های ناشناخته."""
    match = VERSION_RE.match(raw.strip())
    if not match:
        return None
    pre = match.group("pre")
    pre_rank = 9 if not pre else PRE_RANK.get(pre, 0)
    pre_num = int(match.group("prenum") or 0)
    return (
        int(match.group("major")),
        int(match.group("minor")),
        int(match.group("patch") or 0),
        pre_rank,
        pre_num,
    )


def is_prerelease(raw: str) -> bool:
    match = VERSION_RE.match(raw.strip())
    return bool(match and match.group("pre"))


def version_sort_key(raw: str) -> tuple:
    parsed = parse_version(raw)
    return parsed if parsed else (-1, -1, -1, -1, -1)


def select_latest(available: Iterable[str], current: str, include_pre: bool) -> str | None:
    """جدیدترین نسخهٔ بالاتر از نسخهٔ فعلی را انتخاب می‌کند (با احترام به کانال فعلی).

    قواعد انتخاب کانال:
      • پروژهٔ پایدار (current بدون پیش‌پسوند): فقط نسخه‌های پایدار.
      • پروژهٔ rc: نسخه‌های نهایی، rc و نسخه‌های پایدار جدیدتر (نه alpha/beta).
      • پروژهٔ beta: نسخه‌های نهایی، rc، beta.
      • پروژهٔ alpha: هر نسخهٔ بالاتر (شامل alpha) هم‌خانواده.
    پیش‌انتشارهای بالقوه ناپایدار فقط با پرچم --include-pre پذیرفته می‌شوند.
    """
    current_match = VERSION_RE.match(current.strip())
    current_channel = (current_match.group("pre") if current_match else None)

    if include_pre:
        # همه کانال‌ها مجازند؛ پس هیچ فیلتری اعمال نمی‌شود
        allowed: set[str] = {"alpha", "beta", "rc"}
    elif current_channel is None:
        allowed = set()  # فقط نسخه‌های پایدار
    elif current_channel == "rc":
        allowed = {"rc"}
    elif current_channel == "beta":
        allowed = {"rc", "beta"}
    else:
        allowed = {current_channel, "alpha", "beta", "rc"}

    current_key = version_sort_key(current)
    best: str | None = None
    best_key: tuple = current_key
    for candidate in available:
        key = parse_version(candidate)
        if key is None:
            continue
        match = VERSION_RE.match(candidate.strip())
        candidate_channel = match.group("pre") if match else None
        is_stable = candidate_channel is None
        if not is_stable and candidate_channel not in allowed:
            continue
        if key > best_key:
            best, best_key = candidate, key
    return best


def fetch_maven_versions(metadata_url: str) -> list[str]:
    payload = http_get(metadata_url)
    if payload is None:
        return []
    try:
        root = ET.fromstring(payload)
    except ET.ParseError as exc:
        log(f"  [WARN] XML نامعتبر در {metadata_url}: {exc}")
        return []
    versions = [node.text.strip() for node in root.findall("./versioning/versions/version") if node.text]
    # نسخهٔ release در ابتدا اضافه می‌شود تا حتی وقتی لیست versioning کامل نیست استفاده شود
    release = root.findtext("./versioning/release")
    if release and release.strip():
        versions.append(release.strip())
    return versions


# ---------------------------------------------------------------------------
# پردازش Version Catalog
# ---------------------------------------------------------------------------
VERSION_LINE_RE = re.compile(r'^(?P<key>\s*[A-Za-z0-9_.-]+)\s*=\s*"(?P<value>[^"]*)"\s*$')


def read_toml_versions(text: str) -> dict[str, str]:
    versions: dict[str, str] = {}
    in_versions = False
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("[") and stripped.endswith("]"):
            in_versions = stripped == "[versions]"
            continue
        if not in_versions or not stripped or stripped.startswith("#"):
            continue
        match = VERSION_LINE_RE.match(line)
        if match:
            versions[match.group("key").strip()] = match.group("value").strip()
    return versions


def replace_toml_version(text: str, key: str, new_value: str) -> tuple[str, bool]:
    pattern = re.compile(rf'^(\s*{re.escape(key)}\s*=\s*")([^"]*)("\s*)$', re.MULTILINE)
    match = pattern.search(text)
    if not match or match.group(2) == new_value:
        return text, False
    return pattern.sub(rf"\g<1>{new_value}\g<3>", text, count=1), True


def update_version_catalog(text: str, report: Report, include_pre: bool) -> str:
    current_versions = read_toml_versions(text)
    targets: dict[str, str] = {}
    for key, url in LIBRARY_TARGETS.items():
        targets.setdefault(key, url)
    for key, url in PLUGIN_TARGETS.items():
        targets.setdefault(key, url)

    cache: dict[str, list[str]] = {}
    proposed: dict[str, str] = {}

    for key, url in targets.items():
        if key in SKIP_VERSION_KEYS:
            report.skipped.append(f"{key} (نگه‌داری دستی)")
            continue
        current = current_versions.get(key)
        if not current:
            report.skipped.append(f"{key} (در Version Catalog پیدا نشد)")
            continue
        if url not in cache:
            cache[url] = fetch_maven_versions(url)
        available = cache[url]
        if not available:
            report.skipped.append(f"{key} (دسترسی به مخزن ممکن نشد)")
            continue
        latest = select_latest(available, current, include_pre)
        if latest and version_sort_key(latest) > version_sort_key(current):
            proposed[key] = latest

    # هم‌ترازسازی نسخه‌های وابسته (مثلاً hilt و androidxHilt)
    for group in LINKED_VERSIONS:
        group_values = [proposed.get(k, current_versions.get(k, "")) for k in group]
        highest = max(group_values, key=version_sort_key) if group_values else ""
        if not highest:
            continue
        for key in group:
            current = current_versions.get(key, "")
            if current and version_sort_key(highest) > version_sort_key(current):
                proposed[key] = highest

    for key, new_value in sorted(proposed.items()):
        old_value = current_versions.get(key, "")
        text, changed = replace_toml_version(text, key, new_value)
        if changed:
            kind = "plugin" if key in PLUGIN_TARGETS else "library"
            report.updates.append(Update(kind=kind, name=key, old=old_value, new=new_value, location=str(TOML_PATH)))

    return text


# ---------------------------------------------------------------------------
# پردازش GitHub Actions
# ---------------------------------------------------------------------------
def fetch_action_latest_tag(repo: str) -> str | None:
    payload = http_get(f"https://api.github.com/repos/{repo}/releases/latest")
    if payload is None:
        return None
    try:
        data = json.loads(payload)
    except json.JSONDecodeError:
        return None
    tag = (data.get("tag_name") or "").strip()
    if not tag:
        return None
    major_match = re.match(r"^v?(\d+)(?:\.|$)", tag)
    # تگ اصلی (major) برای اکشن‌های GitHub استاندارد است و ارتقاهای امن را می‌گیرد
    return f"v{major_match.group(1)}" if major_match else tag


def update_workflow_actions(report: Report) -> None:
    if not WORKFLOWS_DIR.exists():
        report.skipped.append("پوشه .github/workflows پیدا نشد")
        return

    latest_tags: dict[str, str | None] = {}
    for workflow in sorted(WORKFLOWS_DIR.glob("*.y*ml")):
        original = workflow.read_text(encoding="utf-8")
        lines = original.splitlines(keepends=True)
        touched = False
        for index, line in enumerate(lines):
            match = ACTION_LINE_RE.search(line)
            if not match:
                continue
            repo = match.group("repo")
            if repo not in ALLOWED_ACTION_REPOS:
                report.skipped.append(f"{workflow.name}: اکشن ناشناخته {repo}")
                continue
            if repo not in latest_tags:
                latest_tags[repo] = fetch_action_latest_tag(repo)
            latest = latest_tags[repo]
            if not latest:
                report.skipped.append(f"{workflow.name}: نسخه {repo} دریافت نشد")
                continue
            current = match.group("ref")
            if version_sort_key(current) >= version_sort_key(latest):
                continue
            # حفظ زیرمسیر (مثل gradle/actions/setup-gradle@v6) و فقط تغییر نسخه
            replacement = line[: match.start("ref")] + latest + line[match.end("ref"):]
            lines[index] = replacement
            touched = True
            report.updates.append(
                Update(
                    kind="action",
                    name=f"{repo}{'/' + match.group('sub') if match.group('sub') else ''}",
                    old=current,
                    new=latest,
                    location=str(workflow),
                )
            )
        if touched:
            workflow.write_text("".join(lines), encoding="utf-8")


# ---------------------------------------------------------------------------
# پردازش Gradle Wrapper
# ---------------------------------------------------------------------------
GRADLE_URL_RE = re.compile(r"^distributionUrl=https\\?://services\.gradle\.org/distributions/gradle-(?P<version>[\w.-]+)-(?P<kind>bin|all)\.zip\s*$", re.MULTILINE)


def fetch_latest_gradle() -> str | None:
    payload = http_get(f"{GRADLE_SERVICES}/versions/current")
    if payload is None:
        return None
    try:
        data = json.loads(payload)
    except json.JSONDecodeError:
        return None
    version = (data.get("version") or "").strip()
    return version or None


def update_gradle_wrapper(report: Report) -> None:
    if not WRAPPER_PATH.exists():
        report.skipped.append("gradle-wrapper.properties پیدا نشد")
        return
    text = WRAPPER_PATH.read_text(encoding="utf-8")
    match = GRADLE_URL_RE.search(text)
    if not match:
        report.skipped.append("distributionUrl در gradle-wrapper.properties قابل خواندن نبود")
        return
    current = match.group("version")
    latest = fetch_latest_gradle()
    if not latest:
        report.skipped.append("نسخه فعلی Gradle دریافت نشد")
        return
    if parse_version(latest) is None or version_sort_key(latest) <= version_sort_key(current):
        log(f"  [OK] Gradle Wrapper به‌روز است: {current}")
        return
    kind = match.group("kind")
    new_url = f"distributionUrl=https\\://services.gradle.org/distributions/gradle-{latest}-{kind}.zip"
    text = GRADLE_URL_RE.sub(new_url, text, count=1)
    WRAPPER_PATH.write_text(text, encoding="utf-8")
    report.updates.append(Update(kind="gradle", name="Gradle Wrapper", old=current, new=latest, location=str(WRAPPER_PATH)))


# ---------------------------------------------------------------------------
# CHANGELOG
# ---------------------------------------------------------------------------
def update_changelog(report: Report) -> None:
    if not report.updates or not CHANGELOG_PATH.exists():
        return
    changelog = CHANGELOG_PATH.read_text(encoding="utf-8")
    today = datetime.now(timezone.utc).date().isoformat()
    lines = [update.changelog_line() for update in report.updates]
    entry = (
        f"## [Unreleased] - {today}\n\n"
        "### 🛠 بهبود\n"
        "- آپدیت یکپارچه و خودکار تمام کتابخانه‌ها، ابزارهای بیلد، GitHub Actions و تکنولوژی‌های پروژه.\n"
        + "\n".join(lines)
        + "\n\n"
    )
    marker = "## [Unreleased]"
    if marker in changelog:
        # ورودی Unreleased قبلی را با نسخه فعلی جایگزین کن تا تاریخ‌ها روی هم انبار نشوند
        start = changelog.find(marker)
        end = changelog.find("## [", start + len(marker))
        if end == -1:
            end = len(changelog)
        changelog = changelog[:start] + entry + changelog[end:]
    else:
        first_header = changelog.find("## [")
        if first_header == -1:
            changelog = changelog.rstrip() + "\n\n" + entry
        else:
            changelog = changelog[:first_header] + entry + changelog[first_header:]
    CHANGELOG_PATH.write_text(changelog, encoding="utf-8")


# ---------------------------------------------------------------------------
# اجرای اصلی
# ---------------------------------------------------------------------------
def main() -> int:
    parser = argparse.ArgumentParser(description="MarbleBr unified dependency updater")
    parser.add_argument("--apply", action="store_true", help="نوشتن تغییرات در فایل‌ها")
    parser.add_argument("--check", action="store_true", help="فقط بررسی بدون نوشتن (پیش‌فرض)")
    parser.add_argument("--json-out", default="", help="مسیر ذخیره گزارش JSON")
    parser.add_argument("--include-pre", action="store_true", help="در نظر گرفتن نسخه‌های alpha/beta/rc")
    parser.add_argument("--max-age-days", type=int, default=30, help="(رزرو) حداکثر سن نسخه پایدار")
    args = parser.parse_args()

    apply_changes = args.apply and not args.check
    started = time.time()
    report = Report()

    if not TOML_PATH.exists():
        log(f"[ERROR] فایل {TOML_PATH} پیدا نشد؛ اجرا متوقف شد.")
        return 1

    log("🔎 بررسی نسخه‌های کتابخانه‌ها، پلاگین‌ها و ابزارهای بیلد...")
    toml_text = TOML_PATH.read_text(encoding="utf-8")
    updated_toml = update_version_catalog(toml_text, report, args.include_pre)
    if apply_changes and updated_toml != toml_text:
        TOML_PATH.write_text(updated_toml, encoding="utf-8")

    log("🔎 بررسی نسخه‌های GitHub Actions...")
    if apply_changes:
        update_workflow_actions(report)
    else:
        # در حالت check نیز تغییرات را گزارش می‌کنیم ولی فایل‌ها را دست نمی‌زنیم
        update_workflow_actions(report)

    log("🔎 بررسی نسخه Gradle Wrapper...")
    update_gradle_wrapper(report)

    if apply_changes:
        update_changelog(report)

    # گزارش
    log("")
    if report.updates:
        log(f"✅ {len(report.updates)} به‌روزرسانی یافت شد:")
        for update in report.updates:
            log(f"   - [{update.kind}] {update.name}: {update.old} → {update.new}")
    else:
        log("✅ همه کتابخانه‌ها و تکنولوژی‌ها به‌روز هستند.")

    if report.skipped:
        log(f"ℹ️ {len(report.skipped)} مورد بررسی نشد (غیرحیاتی).")

    payload = {
        "updated": report.changed,
        "update_count": len(report.updates),
        "updates": [u.as_dict() for u in report.updates],
        "warnings": report.warnings,
        "skipped": report.skipped,
        "duration_seconds": round(time.time() - started, 1),
    }

    output_path = args.json_out or os.environ.get("DEPS_JSON_OUT", "")
    if output_path:
        Path(output_path).write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        log(f"📄 گزارش JSON در {output_path} ذخیره شد.")

    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as handle:
            handle.write(f"updated={'true' if report.changed else 'false'}\n")
            handle.write(f"update_count={len(report.updates)}\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
