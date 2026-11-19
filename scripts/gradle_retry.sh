#!/usr/bin/env bash
set -euo pipefail

# اجرای مقاوم Gradle برای CI.
# در محیط CI گاهی حل پلاگین‌ها یا دریافت وابستگی‌ها به‌صورت گذرا شکست می‌خورد
# (مثلاً خطای PluginRequestsHandler هنگام apply کردن پلاگین). این اسکریپت:
#   • هر دستور Gradle را تا ۳ بار با backoff نمایی تکرار می‌کند
#   • خروجی هر تلاش را در فایل لاگ ذخیره می‌کند (برای خلاصه خطا در Workflow)
#   • کد خروج نهایی را دقیقاً از آخرین تلاش بازمی‌گرداند
#
# استفاده:
#   ./scripts/gradle_retry.sh /tmp/lint.log ktlintCheck detekt --stacktrace

LOG_FILE="${1:?مسیر فایل لاگ الزامی است}"
shift
if [[ $# -eq 0 ]]; then
    echo "❌ هیچ آرگومانی برای Gradle داده نشد." >&2
    exit 2
fi

MAX_ATTEMPTS="${GRADLE_MAX_ATTEMPTS:-3}"
DELAY_SECONDS="${GRADLE_RETRY_DELAY:-15}"

# نشانه‌های خطای گذرا (شبکه/حل پلاگین) در برابر خطای واقعی کد
TRANSIENT_PATTERN='Could not (resolve|GET|HEAD|download)|PluginRequestsHandler|Plugin \[|Connection (reset|refused|timed out)|Read timed out|UnknownHostException|Temporary failure in name resolution|Received status code 5[0-9][0-9]|Service Unavailable|Gateway Time'

attempt=1
exit_code=1
while (( attempt <= MAX_ATTEMPTS )); do
    echo "▶️ تلاش ${attempt}/${MAX_ATTEMPTS}: ./gradlew $*"
    set +e
    ./gradlew "$@" 2>&1 | tee "$LOG_FILE"
    exit_code=${PIPESTATUS[0]}
    set -e

    if (( exit_code == 0 )); then
        echo "✅ Gradle با موفقیت اجرا شد (تلاش ${attempt})."
        exit 0
    fi

    if ! grep -qE "$TRANSIENT_PATTERN" "$LOG_FILE"; then
        echo "❌ شکست غیرگذرا شناسایی شد؛ تلاش مجدد بی‌فایده است."
        exit "$exit_code"
    fi

    if (( attempt < MAX_ATTEMPTS )); then
        wait_seconds=$(( DELAY_SECONDS * attempt ))
        echo "⏳ خطای گذرا شناسایی شد. تلاش مجدد پس از ${wait_seconds} ثانیه..."
        sleep "$wait_seconds"
    fi
    attempt=$(( attempt + 1 ))
done

echo "❌ Gradle پس از ${MAX_ATTEMPTS} تلاش شکست خورد."
exit "$exit_code"
