#!/usr/bin/env python3
"""
تست‌های واحد اسکریپت آپدیت یکپارچه وابستگی‌ها (scripts/update_dependencies.py).

این تست‌ها کاملاً آفلاین اجرا می‌شوند (شبکه Mock می‌شود) تا در CI پایدار باشند.
اجرا: python3 scripts/test_update_dependencies.py
"""

from __future__ import annotations

import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import update_dependencies as ud  # noqa: E402

PASSED = 0


def check(condition: bool, label: str) -> None:
    global PASSED
    if not condition:
        print(f"❌ FAIL: {label}", file=sys.stderr)
        raise SystemExit(1)
    PASSED += 1


def test_version_parsing() -> None:
    check(ud.parse_version("1.10.0") > ud.parse_version("1.9.9"), "numeric ordering (1.10 > 1.9)")
    check(ud.parse_version("2.0.0") > ud.parse_version("2.0.0-rc01"), "stable > rc")
    check(ud.parse_version("1.4.0") > ud.parse_version("1.4.0-rc01"), "1.4.0 > 1.4.0-rc01")
    check(ud.parse_version("1.4.0-beta01") > ud.parse_version("1.4.0-alpha01"), "beta > alpha")
    check(ud.parse_version("1.4.0-rc02") > ud.parse_version("1.4.0-rc01"), "rc02 > rc01")
    check(ud.is_prerelease("1.4.0-rc01") and not ud.is_prerelease("1.4.0"), "prerelease detection")
    check(ud.parse_version("160.0.20261116130455") is not None, "GeckoView date version parses")
    check(ud.parse_version("not-a-version") is None, "garbage version rejected")


def test_channel_selection() -> None:
    stable_pool = ["1.0.0", "1.1.0", "2.0.0-alpha01", "1.2.0-rc01"]
    check(ud.select_latest(stable_pool, "1.0.0", False) == "1.1.0", "stable project skips prereleases")
    check(ud.select_latest(["1.4.0-rc01", "1.4.0-rc02", "1.5.0-alpha01"], "1.4.0-rc01", False) == "1.4.0-rc02",
          "rc project advances to next rc only")
    check(ud.select_latest(["1.4.0-rc01"], "1.4.0-rc01", False) is None, "no downgrade/same-version churn")
    check(ud.select_latest(["1.4.0-beta02", "1.4.0-rc01", "1.5.0-alpha01"], "1.4.0-beta02", False) == "1.4.0-rc01",
          "beta project advances to rc, not alpha")
    check(ud.select_latest(["1.0.0", "2.0.0-rc01"], "1.0.0", True) == "2.0.0-rc01", "--include-pre allows rc")
    check(ud.select_latest(["1.0.0"], "1.1.0", False) is None, "never proposes older versions")


def test_toml_parsing() -> None:
    toml = (
        '[versions]\n'
        'appcompat = "1.7.0"\n'
        'geckoview = "160.0.20261116130455"\n'
        "\n"
        "[libraries]\n"
        'androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }\n'
    )
    versions = ud.read_toml_versions(toml)
    check(versions.get("appcompat") == "1.7.0", "reads [versions] entries")
    check("androidx-appcompat" not in versions, "ignores [libraries] entries")
    updated, changed = ud.replace_toml_version(toml, "appcompat", "1.8.0")
    check(changed and 'appcompat = "1.8.0"' in updated, "rewrites a version key")
    check("[libraries]" in updated and 'name = "appcompat"' in updated, "preserves the rest of the file")
    _, changed_again = ud.replace_toml_version(updated, "appcompat", "1.8.0")
    check(not changed_again, "idempotent rewrite")


def test_catalog_update_with_mocked_network() -> None:
    pool = {
        "appcompat": ["1.7.0", "1.8.0", "1.9.0-alpha01"],
        "geckoview": ["160.0.20261116130455", "161.0.20261118000000"],
        "biometric": ["1.4.0-alpha01", "1.4.0-beta01", "1.4.0-rc01", "1.4.0-rc02"],
        "hilt-android": ["2.60.1", "2.61"],
        "hilt-work": ["1.2.0", "1.4.0"],
        "hilt-navigation-compose": ["1.2.0"],
    }
    original_fetch = ud.fetch_maven_versions
    try:
        ud.fetch_maven_versions = lambda url: pool.get(url.rstrip("/").split("/")[-2], [])  # type: ignore[assignment]
        catalog = (
            "[versions]\n"
            'appcompat = "1.7.0"\n'
            'geckoview = "160.0.20261116130455"\n'
            'biometric = "1.4.0-rc01"\n'
            'hilt = "2.60.1"\n'
            'androidxHilt = "1.2.0"\n'
            'androidxHiltNavigationCompose = "1.2.0"\n'
            "\n[libraries]\n"
            'androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }\n'
        )
        report = ud.Report()
        updated = ud.update_version_catalog(catalog, report, include_pre=False)
        applied = {update.name: (update.old, update.new) for update in report.updates}
        check(applied.get("appcompat") == ("1.7.0", "1.8.0"), "bumps stable library")
        check(applied.get("geckoview") == ("160.0.20261116130455", "161.0.20261118000000"),
              "bumps GeckoView date based version")
        check(applied.get("biometric") == ("1.4.0-rc01", "1.4.0-rc02"), "bumps biometric rc")
        check(applied.get("hilt") == ("2.60.1", "2.61"), "bumps Hilt")
        check(applied.get("androidxHilt") == ("1.2.0", "1.4.0"), "bumps AndroidX Hilt")
        check(applied.get("androidxHiltNavigationCompose") == ("1.2.0", "1.4.0"),
              "keeps Hilt Navigation Compose aligned")
        check('androidxHiltNavigationCompose = "1.4.0"' in updated, "writes linked version to catalog")
    finally:
        ud.fetch_maven_versions = original_fetch  # type: ignore[assignment]


def test_action_parsing() -> None:
    line = "        uses: gradle/actions/setup-gradle@v4\n"
    match = ud.ACTION_LINE_RE.search(line)
    check(match is not None, "parses sub-path action reference")
    assert match is not None
    check(match.group("repo") == "gradle/actions", "extracts action repo")
    check(match.group("sub") == "setup-gradle", "extracts action sub-path")
    check(match.group("ref") == "v4", "extracts action ref")
    check("gradle/actions" in ud.ALLOWED_ACTION_REPOS, "official gradle action allow-listed")


def test_changelog_entry() -> None:
    report = ud.Report(updates=[ud.Update(kind="library", name="appcompat", old="1.7.0", new="1.8.0")])
    check("کتابخانه" in report.updates[0].changelog_line(), "changelog line is Persian")
    check(report.changed, "report reports changes")
    check(not ud.Report().changed, "empty report has no changes")


def main() -> int:
    for name, func in sorted(globals().items()):
        if name.startswith("test_") and callable(func):
            func()
    print(f"🎉 All {PASSED} dependency updater tests passed!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
