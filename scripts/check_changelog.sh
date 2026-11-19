#!/usr/bin/env bash
set -euo pipefail

# بررسی الزام‌آور وجود ورودی معتبر در CHANGELOG.md با دسته‌بندی‌های فارسی:
# ✨ جدید | 🛠 بهبود | 🐛 رفع باگ

CHANGELOG_FILE="${1:-CHANGELOG.md}"
ENFORCE_DIFF="${ENFORCE_CHANGELOG_DIFF:-false}"
BASE_REF="${BASE_REF:-origin/main}"

if [[ ! -f "$CHANGELOG_FILE" ]]; then
    echo "❌ خطا: فایل $CHANGELOG_FILE یافت نشد!" >&2
    exit 1
fi

if ! grep -qE "^## \[[0-9]+\.[0-9]+\.[0-9]+\]" "$CHANGELOG_FILE"; then
    echo "❌ خطا: هیچ سرتیتر نسخه‌ای با فرمت '## [X.Y.Z]' در $CHANGELOG_FILE وجود ندارد." >&2
    exit 1
fi

if ! grep -qE "✨ جدید|🛠 بهبود|🐛 رفع باگ" "$CHANGELOG_FILE"; then
    echo "❌ خطا: دسته‌بندی‌های الزامی فارسی (✨ جدید / 🛠 بهبود / 🐛 رفع باگ) در $CHANGELOG_FILE یافت نشد." >&2
    exit 1
fi

if [[ "$ENFORCE_DIFF" == "true" ]]; then
    if git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
        if ! git diff --name-only "$BASE_REF"...HEAD | grep -qx "$CHANGELOG_FILE"; then
            echo "❌ خطا: تغییر یا Merge بدون به‌روزرسانی $CHANGELOG_FILE مجاز نیست (قانون الزام‌آور AGENTS.md)." >&2
            exit 1
        fi
    fi
fi

echo "✅ بررسی CHANGELOG.md با موفقیت انجام شد."
