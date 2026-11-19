# قوانین الزام‌آور برای همه Agentها (AGENTS.md)

این سند قوانین قطعی و غیرقابل‌تخطی پروژه **MarbleBr** را برای تمامی مهندسان و هوش‌های مصنوعی (Coding Agents) مشخص می‌کند. تمامی این قوانین در خط لوله CI بررسی و اجبار (Enforce) می‌شوند.

---

## ۱. قوانین انتشار و Merge به شاخه `main`

با هر Merge یا Push به شاخه `main` (و در بررسی‌های Pull Request):

1. **ساخت APK برای تمامی معماری‌ها (ABI Splits + Universal):**
   - بیلد ریلیز باید دقیقاً ۵ فایل APK تولید کند:
     - `arm64-v8a`
     - `armeabi-v7a`
     - `x86`
     - `x86_64`
     - `universal` (شامل تمام معماری‌ها)
   - تمامی APKها باید با Keystore تعریف‌شده در **GitHub Secrets** (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) امضا شوند.

2. **ثبت الزامی تغییرات در `CHANGELOG.md`:**
   - برای هر نسخه یا PR، یک ورودی جدید به زبان **فارسی**، خلاصه و دقیق در بالای فایل `CHANGELOG.md` ثبت شود.
   - دسته‌بندی‌های مجاز و الزامی:
     - `### ✨ جدید`
     - `### 🛠 بهبود`
     - `### 🐛 رفع باگ`
   - اسکریپت `scripts/check_changelog.sh` در CI اجرا می‌شود و اگر Merge یا PR فاقد به‌روزرسانی معتبر در `CHANGELOG.md` باشد، بیلد **Fail** خواهد شد.

3. **ارتقای خودکار نسخه (Semantic Versioning بر اساس حجم تغییر کد):**
   - نسخه اپ (`MARBLE_VERSION_NAME` و `MARBLE_VERSION_CODE` در `gradle.properties`) توسط `scripts/bump_version.kts` / `scripts/bump_version.sh` محاسبه و ارتقا می‌یابد:
     - **حجم تغییر** = مجموع خطوط اضافه‌شده + حذف‌شده در `git diff --numstat` آن تغییر، **به‌جز فایل‌های lock، باینری و generated**.
     - **کمتر از ۵۰۰ خط (`< 500`)**: `patch++`
     - **بین ۵۰۰ تا ۵۰۰۰ خط (`500 .. 5000`)**: `minor++` و `patch = 0`
     - **بیش از ۵۰۰۰ خط (`> 5000`) یا تغییر Breaking**: `major++` و `minor = 0`, `patch = 0`
     - **`versionCode`**: بیشینه شماره اجرای CI (`GITHUB_RUN_NUMBER`)، تعداد کل کامیت‌ها، و `previousVersionCode + 1` (همواره اکیداً صعودی).

4. **انتشار خودکار در GitHub Releases:**
   - هر ۵ فایل APK امضاشده به GitHub Release متناظر با تگ نسخه (`vX.Y.Z`) پیوست می‌شوند.
   - متن Release دقیقاً از آخرین بخش ثبت‌شده در `CHANGELOG.md` استخراج می‌شود.

5. **آپدیت یکپارچه وابستگی‌ها (تک‌منبع واحد):**
   - تمام کتابخانه‌ها و تکنولوژی‌ها (Version Catalog، GeckoView، Gradle Wrapper، ابزارهای بیلد و GitHub Actions) فقط از طریق Workflow `.github/workflows/dependency-update.yml` و اسکریپت `scripts/update_dependencies.py` به‌روزرسانی می‌شوند.
   - Dependabot و Renovate و هر اسکریپت آپدیت موازی دیگری حذف شده‌اند و نباید دوباره اضافه شوند.
   - این Workflow تنها زمانی PR می‌سازد که بیلد کامل، `ktlintCheck`، `detekt` و `testDebugUnitTest` با نسخه‌های جدید سبز باشند.

---

## ۲. قوانین معماری و حریم خصوصی

- **عدم خروج داده‌ها:** هیچ‌یک از داده‌های نشست، کوکی‌ها، اسنپ‌شات‌های Session Vault یا اطلاعات کاربری نباید به هیچ سرور خارجی ارسال شود.
- **بدون تله‌متری و ردیاب:** افزودن هرگونه SDK تحلیلی، تبلیغاتی یا تله‌متری (مانند Firebase Analytics، Crashlytics، Sentry و غیره) ممنوع است.
- **ایزوله‌سازی کانتینرها:** هر وب‌اپ/PWA باید در `contextId` اختصاصی خود در GeckoView اجرا شود.
- **حفاظت از نشست در پاک‌سازی کش:** دکمه «پاک کردن کش» صرفاً مجاز به پاک‌سازی `StorageController.ClearFlags.ALL_CACHES` است و هرگز نباید کوکی‌ها یا `DOM_STORAGES` را حذف کند.
- **کیفیت کد:** پیش از هر Merge، تست‌های واحد (`./gradlew testDebugUnitTest`)، تست‌های اسکریپت نسخه (`./scripts/test_bump_version.sh`)، بررسی فرمت (`./gradlew ktlintCheck`) و تحلیل ایستا (`./gradlew detekt`) باید کاملاً سبز باشند.
