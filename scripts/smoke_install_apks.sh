#!/usr/bin/env bash
set -Eeuo pipefail
trap 'printf "::error title=APK install smoke test::Failed at %s:%s: %s\n" "${BASH_SOURCE[0]}" "$LINENO" "$BASH_COMMAND"' ERR

staging="${1:?Expected directory with verified APKs}"
shift
package=com.marble098.marblebr

# مقادیر قابل تنظیم (تست‌های محلی با مقادیر کوچک اجرا می‌شوند)
boot_timeout="${SMOKE_BOOT_TIMEOUT:-240}"
launch_timeout="${SMOKE_LAUNCH_TIMEOUT:-30}"
launch_attempts="${SMOKE_LAUNCH_ATTEMPTS:-3}"
expected_page_size="${EXPECTED_PAGE_SIZE:-}"

# شبیه‌ساز گاهی چند ثانیه پس از «Success» هنوز کامل بوت نشده است؛ پیش از هر بررسی
# باید بوت کامل و صفحهٔ قفل باز شده باشد، وگرنه راه‌اندازی اپ به‌صورت تصادفی شکست می‌خورد.
wait_for_boot() {
    local deadline=$((SECONDS + boot_timeout))
    adb wait-for-device
    while [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]]; do
        if ((SECONDS >= deadline)); then
            echo "Emulator did not report sys.boot_completed within ${boot_timeout}s" >&2
            return 1
        fi
        sleep 2
    done
    # باز کردن قفل صفحه (اگر وجود داشته باشد) تا اپ واقعاً در پیش‌زمینه اجرا شود.
    adb shell input keyevent 82 >/dev/null 2>&1 || true
}

wait_for_boot

abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
page_size="$(adb shell getconf PAGE_SIZE 2>/dev/null | tr -d '\r' || true)"
echo "Emulator ABI: $abi | ABI list: $(adb shell getprop ro.product.cpu.abilist 2>/dev/null | tr -d '\r' || true)"
echo "Emulator API: $api | memory page size: ${page_size:-unknown}"

if [[ -n "$expected_page_size" && -n "$page_size" && "$page_size" != "$expected_page_size" ]]; then
    echo "Expected ${expected_page_size}-byte memory pages but the emulator reports $page_size" >&2
    exit 1
fi

shopt -s nullglob
split=("$staging"/MarbleBr-v*-"$abi".apk)
universal=("$staging"/MarbleBr-v*-universal.apk)
[[ ${#split[@]} == 1 && ${#universal[@]} == 1 ]] || { echo "No unique $abi or universal APK" >&2; exit 1; }

reset_app() {
    if [[ -n "$(adb shell pm path "$package" 2>/dev/null || true)" ]]; then
        adb uninstall "$package"
    fi
}

install() {
    local apk="$1"
    shift
    echo "Installing $(basename "$apk") on $abi (API $api)"
    # Don't use -t, -d or install-multiple: those can hide broken end-user installs.
    adb install "$@" "$apk" || { adb logcat -d -s PackageManager PackageInstaller; return 1; }
}

process_alive() {
    [[ -n "$(adb shell pidof "$package" 2>/dev/null | tr -d '\r' || true)" ]]
}

# انتظار برای زنده‌شدن فرایند به‌جای یک بررسی لحظه‌ای: روی شبیه‌سازهای کند (به‌ویژه
# تصویر ۱۶ کیلوبایتی) فرایند چند ثانیه پس از «Status: ok» ثبت می‌شود.
wait_for_process() {
    local timeout="${1:-$launch_timeout}"
    local deadline=$((SECONDS + timeout))
    until process_alive; do
        if ((SECONDS >= deadline)); then
            return 1
        fi
        sleep 1
    done
    return 0
}

launch() {
    local attempt result
    # Activity Manager can stall after an APK replacement. Bound each command so the next attempt can run.
    for ((attempt = 1; attempt <= launch_attempts; attempt++)); do
        if timeout "${launch_timeout}s" adb shell am force-stop "$package" >/dev/null 2>&1; then
            if result="$(timeout "${launch_timeout}s" adb shell am start -W -a android.intent.action.VIEW -d https://example.invalid/ \
                -n "$package/.MainActivity" 2>&1 | tr -d '\r')"; then
                echo "$result"
                if grep -q '^Status: ok' <<< "$result" && wait_for_process; then
                    return 0
                fi
            else
                echo "$result"
            fi
        fi
        echo "Launch attempt $attempt/$launch_attempts did not complete with a live app process; retrying..." >&2
        sleep 2
    done

    echo "The app process never stayed alive after launch" >&2
    adb shell dumpsys activity activities 2>/dev/null \
        | grep -E 'mResumedActivity|topResumedActivity|ResumedActivity' | head -n 5 || true
    adb logcat -d -b crash 2>/dev/null | tail -n 120 || true
    adb logcat -d -s AndroidRuntime:E ActivityManager:W 2>/dev/null | tail -n 120 || true
    return 1
}

reset_app
install "${split[0]}"
launch
install "${universal[0]}" -r
launch
reset_app
install "${universal[0]}"
launch
install "${split[0]}" -r
launch

for baseline in "$@"; do
    reset_app
    install "$baseline"
    # Replace without uninstalling, exercising Android's signer/version and ABI checks.
    install "${universal[0]}" -r
    launch
done
