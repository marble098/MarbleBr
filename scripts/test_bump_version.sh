#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/bump_version.sh
source "${SCRIPT_DIR}/bump_version.sh"

assert_eq() {
    local expected="$1"
    local actual="$2"
    local label="$3"
    if [[ "$expected" != "$actual" ]]; then
        echo "❌ FAIL: $label (expected '$expected', got '$actual')" >&2
        exit 1
    else
        echo "✅ PASS: $label -> $actual"
    fi
}

echo "Running unit tests for MarbleBr Semantic Versioning algorithm..."

# 1. Patch bump (< 500 lines)
assert_eq "1.2.4" "$(compute_next_semver "1.2.3" 0 false)" "0 lines -> patch++"
assert_eq "1.2.4" "$(compute_next_semver "1.2.3" 12 false)" "12 lines -> patch++"
assert_eq "1.2.4" "$(compute_next_semver "1.2.3" 499 false)" "499 lines -> patch++"

# 2. Minor bump (500 .. 5000 lines)
assert_eq "1.3.0" "$(compute_next_semver "1.2.3" 500 false)" "500 lines -> minor++ and patch=0"
assert_eq "1.3.0" "$(compute_next_semver "1.2.3" 2400 false)" "2400 lines -> minor++ and patch=0"
assert_eq "1.3.0" "$(compute_next_semver "1.2.3" 5000 false)" "5000 lines -> minor++ and patch=0"

# 3. Major bump (> 5000 lines or breaking change)
assert_eq "2.0.0" "$(compute_next_semver "1.2.3" 5001 false)" "5001 lines -> major++ and minor=0, patch=0"
assert_eq "2.0.0" "$(compute_next_semver "1.2.3" 18500 false)" "18500 lines -> major++ and minor=0, patch=0"
assert_eq "2.0.0" "$(compute_next_semver "1.2.3" 10 true)" "10 lines with breaking=true -> major++"

# 4. Excluded lock & generated files in git diff --numstat
SAMPLE_NUMSTAT="$(printf "20\t15\tapp/src/main/java/com/marble098/marblebr/MainActivity.kt\n900\t400\tpackage-lock.json\n500\t100\tapp/build/generated/ksp/debug/Database_Impl.kt\n-\t-\tgradle/wrapper/gradle-wrapper.jar\n8\t4\tengine/src/main/java/com/marble098/marblebr/engine/SessionHealthGuard.kt")"
CALC_LINES="$(calculate_changed_lines_from_numstat "$SAMPLE_NUMSTAT")"
assert_eq "47" "$CALC_LINES" "numstat excludes lock, build/generated, and binary files (20+15+8+4 = 47)"
assert_eq "1.0.1" "$(compute_next_semver "1.0.0" "$CALC_LINES" false)" "47 filtered lines -> patch++"

# 5. Monotonic versionCode
assert_eq "15" "$(compute_monotonic_version_code 14 5 10)" "monotonic code increments when prev_code >= commit/run"
assert_eq "42" "$(compute_monotonic_version_code 10 25 42)" "monotonic code uses run_number when higher"
assert_eq "30" "$(compute_monotonic_version_code 10 30 0)" "monotonic code uses commit_count when higher"

# 6. Breaking change detection
if has_breaking_indicator "feat(vault)!: migrate snapshot encryption format"; then
    echo "✅ PASS: detects conventional commit breaking bang (!:)"
else
    echo "❌ FAIL: did not detect conventional commit breaking bang" >&2
    exit 1
fi

if has_breaking_indicator "refactor: update API\n\nBREAKING CHANGE: drop legacy schema"; then
    echo "✅ PASS: detects BREAKING CHANGE footer"
else
    echo "❌ FAIL: did not detect BREAKING CHANGE footer" >&2
    exit 1
fi

echo "🎉 All version bump unit tests passed!"
