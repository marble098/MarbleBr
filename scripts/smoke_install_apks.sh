#!/usr/bin/env bash
set -Eeuo pipefail
trap 'printf "::error title=APK install smoke test::Failed at %s:%s: %s\n" "${BASH_SOURCE[0]}" "$LINENO" "$BASH_COMMAND"' ERR

staging="${1:?Expected directory with verified APKs}"
shift
package=com.marble098.marblebr
abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
shopt -s nullglob
split=("$staging"/MarbleBr-v*-"$abi".apk)
universal=("$staging"/MarbleBr-v*-universal.apk)
[[ ${#split[@]} == 1 && ${#universal[@]} == 1 ]] || { echo "No unique $abi or universal APK" >&2; exit 1; }

reset_app() {
    if [[ -n "$(adb shell pm path "$package")" ]]; then
        adb uninstall "$package"
    fi
}

install() {
    local apk="$1"
    shift
    echo "Installing $(basename "$apk") on $abi (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
    # Don't use -t, -d or install-multiple: those can hide broken end-user installs.
    adb install "$@" "$apk" || { adb logcat -d -s PackageManager PackageInstaller; return 1; }
}

launch() {
    adb shell am force-stop "$package"
    local result
    result="$(adb shell am start -W -a android.intent.action.VIEW -d https://example.invalid/ \
        -n "$package/.MainActivity")"
    echo "$result"
    if ! grep -q '^Status: ok' <<< "$result" || ! adb shell pidof "$package"; then
        adb logcat -d -b crash
        return 1
    fi
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
