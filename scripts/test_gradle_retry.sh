#!/usr/bin/env bash
set -Eeuo pipefail

# تست آفلاین scripts/gradle_retry.sh با یک `./gradlew` قلابی:
#   • خطای گذرای حل وابستگی باید تکرار شود و در تلاش بعدی کش دور زده شود.
#   • خطای واقعی کد نباید بی‌دلیل تکرار شود (زمان CI هدر نرود).

root="$(cd "$(dirname "$0")/.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

scenario_dir() {
    local dir="$1"
    mkdir -p "$dir"
    cat > "$dir/gradlew" <<'MOCK'
#!/usr/bin/env bash
attempts=0
[[ -f .attempts ]] && attempts="$(cat .attempts)"
attempts=$((attempts + 1))
echo "$attempts" > .attempts
echo "gradlew $*"
mode="$(cat .mode)"
if [[ "$mode" == transient ]]; then
    if ((attempts < 2)); then
        echo "   > Could not find org.example:missing:1.0.0." >&2
        exit 1
    fi
    echo 'BUILD SUCCESSFUL'
    exit 0
fi
echo "e: file.kt:1:1 unresolved reference" >&2
exit 1
MOCK
    chmod +x "$dir/gradlew"
}

run_retry() {
    local dir="$1"
    shift
    (cd "$dir" && GRADLE_RETRY_DELAY=0 "$root/scripts/gradle_retry.sh" "$dir/gradle.log" testDebugUnitTest)
}

transient="$tmp/transient"
scenario_dir "$transient"
echo transient > "$transient/.mode"
run_retry "$transient" > "$tmp/transient.out" 2>&1 || {
    cat "$tmp/transient.out" >&2
    echo "Transient dependency failures must be retried" >&2
    exit 1
}
grep -q -- '--refresh-dependencies' "$tmp/transient.out" || {
    cat "$tmp/transient.out" >&2
    echo "Retry after a resolution failure must bypass the dependency cache" >&2
    exit 1
}
[[ "$(cat "$transient/.attempts")" == 2 ]] || {
    echo "Expected exactly two Gradle attempts, got $(cat "$transient/.attempts")" >&2
    exit 1
}

fatal="$tmp/fatal"
scenario_dir "$fatal"
echo fatal > "$fatal/.mode"
if run_retry "$fatal" > "$tmp/fatal.out" 2>&1; then
    cat "$tmp/fatal.out" >&2
    echo "Non-transient failures must not be retried as success" >&2
    exit 1
fi
[[ "$(cat "$fatal/.attempts")" == 1 ]] || {
    echo "Non-transient failures must stop after one attempt" >&2
    exit 1
}

echo "✅ Gradle retry logic tests passed."
