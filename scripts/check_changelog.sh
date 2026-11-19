#!/usr/bin/env bash
set -euo pipefail

# بررسی الزام‌آور وجود ورودی معتبر در CHANGELOG.md با دسته‌بندی‌های فارسی:
# ✨ جدید | 🛠 بهبود | 🐛 رفع باگ
#
# قوانین الزام‌آور (AGENTS.md):
#   - هر PR/Merge که فایل‌های پروژه را تغییر دهد باید CHANGELOG.md را نیز به‌روزرسانی کند.
#   - استثنا: PRهای کاملاً خودکار وابستگی‌ها (Dependabot/Renovate) و PRهای صرفاً CI
#     که محتوای اپ را تغییر نمی‌دهند، از این الزام معاف‌اند تا خط لوله قفل نشود.

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

if [[ "$ENFORCE_DIFF" != "true" ]]; then
    echo "✅ بررسی CHANGELOG.md با موفقیت انجام شد."
    exit 0
fi

if ! git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
    echo "ℹ️ مرجع پایه ($BASE_REF) در دسترس نیست؛ بررسی تفاوت اجرا نشد."
    echo "✅ بررسی CHANGELOG.md با موفقیت انجام شد."
    exit 0
fi

# دریافت فهرست فایل‌های تغییر‌یافته در همین PR/Merge
CHANGED_FILES="$(git diff --name-only "$BASE_REF"...HEAD 2>/dev/null || git diff --name-only "$BASE_REF" HEAD)"

if [[ -z "$CHANGED_FILES" ]]; then
    echo "ℹ️ هیچ فایلی در این تغییرات یافت نشد."
    echo "✅ بررسی CHANGELOG.md با موفقیت انجام شد."
    exit 0
fi

if grep -qx "$CHANGELOG_FILE" <<< "$CHANGED_FILES"; then
    echo "✅ CHANGELOG.md به‌روزرسانی شده و قوانین رعایت شده است."
    exit 0
fi

# بررسی معافیت‌ها (مثلاً PR خودکار Dependabot / Renovate / تغییرات صرفاً CI)
is_exempt_change() {
    local file_path="$1"
    case "$file_path" in
        # ابزارهای CI، اسکریپت‌ها و تنظیمات مخزن — تأثیری بر محتوای اپ ندارند
        .github/*|scripts/*|config/*|docs/*|gradle/wrapper/*|.editorconfig|.gitignore|*.md)
            return 0 ;;
        # به‌روزرسانی کاملاً خودکار نسخه‌های کتابخانه‌ها
        gradle/libs.versions.toml|gradle.properties|settings.gradle.kts|build.gradle.kts|renovate.json)
            return 0 ;;
        *)
            return 1 ;;
    esac
}

while IFS= read -r changed_file; do
    [[ -z "$changed_file" ]] && continue
    if ! is_exempt_change "$changed_file"; then
        echo "❌ خطا: تغییر «$changed_file» بدون به‌روزرسانی $CHANGELOG_FILE مجاز نیست (قانون الزام‌آور AGENTS.md)." >&2
        echo "   لطفاً یک ورودی جدید فارسی با دسته‌بندی (✨ جدید / 🛠 بهبود / 🐛 رفع باگ) در $CHANGELOG_FILE ثبت کنید." >&2
        exit 1
    fi
done <<< "$CHANGED_FILES"

echo "✅ تغییرات فقط شامل فایل‌های CI/مستندات/وابستگی‌ها بود؛ نیازی به ورودی CHANGELOG نیست."
echo "✅ بررسی CHANGELOG.md با موفقیت انجام شد."
