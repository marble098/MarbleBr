#!/usr/bin/env bash
set -Eeuo pipefail

# تست آفلاین scripts/smoke_install_apks.sh با یک `adb` قلابی.
# هدف: تضمین این‌که اسکریپت روی شبیه‌سازِ کند (که فرایند اپ را با کمی تأخیر ثبت می‌کند)
# به‌صورت تصادفی شکست نمی‌خورد، ولی خرابیِ واقعی (اپ هرگز بالا نمی‌آید) همچنان Fail می‌شود.

root="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

apks="$tmp/apks"
mkdir -p "$apks"
printf 'fake-apk\n' > "$apks/MarbleBr-v9.9.9-x86_64.apk"
printf 'fake-apk\n' > "$apks/MarbleBr-v9.9.9-universal.apk"

cat > "$tmp/adb" <<'MOCK'
#!/usr/bin/env bash
set -Eeuo pipefail
state="${MOCK_STATE:?}"
record() {
    printf '%s\n' "$*" >> "$state/calls"
}
record "$*"
case "$*" in
    'wait-for-device') exit 0 ;;
    'shell getprop ro.product.cpu.abi') echo x86_64 ;;
    'shell getprop ro.product.cpu.abilist') echo 'x86_64,x86' ;;
    'shell getprop ro.build.version.sdk') echo 35 ;;
    'shell getprop sys.boot_completed') echo 1 ;;
    'shell getconf PAGE_SIZE') echo "${MOCK_PAGE_SIZE:-16384}" ;;
    'shell input keyevent 82') exit 0 ;;
    'shell pm path '*)
        if [[ "${MOCK_INSTALLED:-yes}" == yes ]]; then echo 'package:/data/app/base.apk'; fi
        ;;
    'shell am force-stop '*) exit 0 ;;
    'shell am start -W '*)
        echo 'Starting: Intent { act=android.intent.action.VIEW }'
        echo 'Status: ok'
        echo 'LaunchState: UNKNOWN (0)'
        echo 'Complete'
        ;;
    'shell pidof '*)
        if [[ "${MOCK_PIDOF_MODE:-ok}" == never ]]; then
            exit 1
        fi
        count=0
        [[ -f "$state/pidof_count" ]] && count="$(cat "$state/pidof_count")"
        count=$((count + 1))
        echo "$count" > "$state/pidof_count"
        if [[ "${MOCK_PIDOF_MODE:-ok}" == flaky ]] && ((count < 3)); then
            # فرایند اپ هنوز در ActivityManager ثبت نشده است.
            exit 1
        fi
        echo 4242
        ;;
    install*|'uninstall '*) echo 'Success' ;;
    'logcat '*) echo 'logcat unavailable in this test' ;;
    'shell dumpsys '*) echo 'mResumedActivity: ActivityRecord{1 com.marble098.marblebr/.MainActivity}' ;;
    *) exit 0 ;;
esac
MOCK
chmod +x "$tmp/adb"

run_smoke() {
    local scenario="$1"
    shift
    local state="$tmp/$scenario"
    mkdir -p "$state"
    env MOCK_STATE="$state" MOCK_PIDOF_MODE="$scenario" \
        SMOKE_LAUNCH_TIMEOUT=2 SMOKE_LAUNCH_ATTEMPTS=2 SMOKE_BOOT_TIMEOUT=5 \
        PATH="$tmp:$PATH" "$@" \
        "$root/scripts/smoke_install_apks.sh" "$apks"
}

echo "▶️ Scenario: app process appears with a short delay (must pass)"
run_smoke flaky > "$tmp/flaky.log" 2>&1 || {
    cat "$tmp/flaky.log" >&2
    echo "Smoke test must tolerate a delayed app process" >&2
    exit 1
}

echo "▶️ Scenario: app process never appears (must fail)"
if run_smoke never > "$tmp/never.log" 2>&1; then
    cat "$tmp/never.log" >&2
    echo "Smoke test must fail when the app process never starts" >&2
    exit 1
fi
grep -q 'never stayed alive after launch' "$tmp/never.log" || {
    cat "$tmp/never.log" >&2
    echo "Failure message for a dead app process is missing" >&2
    exit 1
}

echo "▶️ Scenario: wrong memory page size (must fail)"
if run_smoke ok env MOCK_PAGE_SIZE=4096 EXPECTED_PAGE_SIZE=16384 > "$tmp/pages.log" 2>&1; then
    cat "$tmp/pages.log" >&2
    echo "Smoke test must reject a device with an unexpected page size" >&2
    exit 1
fi
grep -q 'Expected 16384-byte memory pages' "$tmp/pages.log" || {
    cat "$tmp/pages.log" >&2
    echo "Page-size mismatch message is missing" >&2
    exit 1
}

echo "✅ APK install smoke-test logic tests passed."
