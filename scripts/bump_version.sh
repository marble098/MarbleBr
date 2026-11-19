#!/usr/bin/env bash
set -euo pipefail

# الگوریتم نسخه‌دهی خودکار MarbleBr بر اساس حجم تغییر کد (Diff Size) و تغییرات Breaking
# قوانین:
# - حجم تغییر = مجموع خطوط اضافه‌شده + حذف‌شده در diff آن merge، به‌جز فایل‌های lock/generated.
# - کمتر از ۵۰ خط (< 50): patch++
# - بین ۵۰ تا ۵۰۰ خط (50 <= lines <= 500): minor++ و patch=0
# - بیش از ۵۰۰ خط (> 500) یا تغییر breaking: major++ و minor=0, patch=0
# - versionCode = شماره run یا تعداد commitها (همیشه صعودی)

is_excluded_file() {
    local file_path="$1"
    case "$file_path" in
        *.lock|*lock.json|*package-lock.json|*yarn.lock|*pnpm-lock.yaml|*gradle.lockfile)
            return 0 ;;
        */build/*|build/*|*/generated/*|generated/*|*gradle-wrapper.jar|*.png|*.webp|*.apk|*.aab)
            return 0 ;;
        *)
            return 1 ;;
    esac
}

calculate_changed_lines_from_numstat() {
    local numstat_input="$1"
    local total=0
    while IFS=$'\t' read -r added deleted filepath || [[ -n "${added:-}" ]]; do
        [[ -z "${added:-}" ]] && continue
        [[ "$added" == "-" || "$deleted" == "-" ]] && continue
        if is_excluded_file "$filepath"; then
            continue
        fi
        if [[ "$added" =~ ^[0-9]+$ ]] && [[ "$deleted" =~ ^[0-9]+$ ]]; then
            total=$((total + added + deleted))
        fi
    done <<< "$numstat_input"
    echo "$total"
}

has_breaking_indicator() {
    local msg="$1"
    if echo "$msg" | grep -qiE "BREAKING[ -]CHANGE"; then
        return 0
    fi
    if echo "$msg" | grep -qE "^[a-zA-Z]+(\([^)]+\))?!:"; then
        return 0
    fi
    return 1
}

compute_next_semver() {
    local current_version="${1#v}"
    local changed_lines="$2"
    local is_breaking="${3:-false}"

    local major minor patch
    IFS='.' read -r major minor patch <<< "$current_version"

    if [[ "$is_breaking" == "true" ]] || (( changed_lines > 500 )); then
        major=$((major + 1))
        minor=0
        patch=0
    elif (( changed_lines >= 50 )); then
        minor=$((minor + 1))
        patch=0
    else
        patch=$((patch + 1))
    fi

    echo "${major}.${minor}.${patch}"
}

compute_monotonic_version_code() {
    local prev_code="$1"
    local commit_count="$2"
    local run_number="${3:-0}"

    local candidate="$commit_count"
    if (( run_number > candidate )); then
        candidate="$run_number"
    fi
    local min_next=$((prev_code + 1))
    if (( candidate > min_next )); then
        echo "$candidate"
    else
        echo "$min_next"
    fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    PROPS_FILE="${PROPS_FILE:-gradle.properties}"
    BASE_REF="HEAD~1"
    LINES_OVERRIDE=""
    BREAKING_OVERRIDE="false"
    DRY_RUN="false"

    for arg in "$@"; do
        case "$arg" in
            --base=*) BASE_REF="${arg#*=}" ;;
            --lines=*) LINES_OVERRIDE="${arg#*=}" ;;
            --breaking) BREAKING_OVERRIDE="true" ;;
            --dry-run) DRY_RUN="true" ;;
            --props=*) PROPS_FILE="${arg#*=}" ;;
        esac
    done

    if [[ ! -f "$PROPS_FILE" ]]; then
        echo "Error: $PROPS_FILE not found" >&2
        exit 1
    fi

    CURRENT_VERSION=$(grep -E '^MARBLE_VERSION_NAME=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
    CURRENT_CODE=$(grep -E '^MARBLE_VERSION_CODE=' "$PROPS_FILE" | cut -d'=' -f2 | tr -d '[:space:]')

    if [[ -n "$LINES_OVERRIDE" ]]; then
        CHANGED_LINES="$LINES_OVERRIDE"
    else
        if git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
            NUMSTAT=$(git diff --numstat "$BASE_REF" HEAD)
        else
            NUMSTAT=$(git show --numstat --format="" HEAD)
        fi
        CHANGED_LINES=$(calculate_changed_lines_from_numstat "$NUMSTAT")
    fi

    COMMIT_MSG=$(git log -1 --pretty=%B 2>/dev/null || echo "")
    IS_BREAKING="$BREAKING_OVERRIDE"
    if [[ "$IS_BREAKING" != "true" ]] && has_breaking_indicator "$COMMIT_MSG"; then
        IS_BREAKING="true"
    fi

    NEXT_VERSION=$(compute_next_semver "$CURRENT_VERSION" "$CHANGED_LINES" "$IS_BREAKING")
    COMMIT_COUNT=$(git rev-list --count HEAD 2>/dev/null || echo "1")
    RUN_NUMBER="${GITHUB_RUN_NUMBER:-0}"
    NEXT_CODE=$(compute_monotonic_version_code "$CURRENT_CODE" "$COMMIT_COUNT" "$RUN_NUMBER")

    if [[ "$DRY_RUN" == "true" ]]; then
        echo "NEXT_VERSION=$NEXT_VERSION"
        echo "NEXT_CODE=$NEXT_CODE"
        echo "CHANGED_LINES=$CHANGED_LINES"
        echo "IS_BREAKING=$IS_BREAKING"
        exit 0
    fi

    sed -i "s/^MARBLE_VERSION_NAME=.*/MARBLE_VERSION_NAME=${NEXT_VERSION}/" "$PROPS_FILE"
    sed -i "s/^MARBLE_VERSION_CODE=.*/MARBLE_VERSION_CODE=${NEXT_CODE}/" "$PROPS_FILE"

    echo "Bumped MarbleBr version: ${CURRENT_VERSION} (${CURRENT_CODE}) -> ${NEXT_VERSION} (${NEXT_CODE}) [changedLines=${CHANGED_LINES}, breaking=${IS_BREAKING}]"
fi
