# راهنمای کار و قوانین الزام‌آور برای Claude و سایر Agentها (CLAUDE.md)

این فایل هم‌تراز با `AGENTS.md` است و تمامی دستورالعمل‌های فنی و قوانین الزام‌آور مخزن **MarbleBr** را برای Agentها مشخص می‌کند.

## خلاصه دستورات کلیدی

- **اجرای تست‌های واحد:** `./gradlew testDebugUnitTest`
- **اجرای تست الگوریتم نسخه‌دهی:** `./scripts/test_bump_version.sh`
- **آپدیت یکپارچه همه وابستگی‌ها:** `python3 scripts/update_dependencies.py --apply`
- **تست اسکریپت آپدیت وابستگی‌ها:** `python3 scripts/test_update_dependencies.py`
- **بررسی اعتبار Changelog فارسی:** `./scripts/check_changelog.sh`
- **بررسی کیفیت و استایل کد:** `./gradlew ktlintCheck detekt`
- **ساخت تمام APKهای ریلیز (۵ معماری):** `./gradlew assembleRelease`

## قوانین طلایی و غیرقابل‌تخطی (CI-Enforced)

1. **هرگز بدون به‌روزرسانی `CHANGELOG.md` کد را به `main` مرج نکنید.**
   - متن `CHANGELOG.md` باید به زبان فارسی و دارای دسته‌بندی‌های `✨ جدید`، `🛠 بهبود` یا `🐛 رفع باگ` باشد.
   - CI با اجرای `scripts/check_changelog.sh` در صورت نبود ورودی جدید در `CHANGELOG.md` خطا می‌دهد.

2. **الگوریتم نسخه‌دهی خودکار (`scripts/bump_version.kts` و `scripts/bump_version.sh`):**
   - حجم تغییرات = مجموع خطوط اضافه‌شده + حذف‌شده (بدون احتساب فایل‌های lock/generated/binary).
   - کمتر از ۵۰ خط: `patch++`
   - بین ۵۰ تا ۵۰۰ خط: `minor++` و `patch = 0`
   - بیش از ۵۰۰ خط یا دارای نشانگر Breaking: `major++` و `minor = 0, patch = 0`
   - `versionCode` همیشه صعودی است.

3. **خروجی‌های الزامی Release در هر Merge به `main`:**
   - ۵ فایل APK مجزا: `arm64-v8a`، `armeabi-v7a`، `x86`، `x86_64` و `universal`.
   - امضاشده با Keystore از GitHub Secrets و پیوست‌شده به GitHub Release همراه با متن فارسی آخرین نسخه از `CHANGELOG.md`.

4. **معماری ماژولار:**
   - `:app`: پوسته اصلی اپ، DI، ناوبری، تم Material 3 Expressive و تنظیمات بیلد/ABI splits.
   - `:core`: مدل‌های دامنه، دیتابیس Room، تنظیمات DataStore و سیستم طراحی فارسی/RTL.
   - `:vault`: موتور رمزنگاری AES-256-GCM با Android Keystore، مدیریت اسنپ‌شات‌های Session Vault، ماشین زمان (Session Time Machine) و قفل بیومتریک.
   - `:engine`: مدیریت GeckoView با `contextId` ایزوله برای هر سایت/PWA، افزونه داخلی Never Sign-out، محافظ سلامت نشست (Session Health Guard)، سیستم Smart Keep-alive با WorkManager، و ورود مجدد خودکار با Credential Manager / Passkey.
   - `:feature-browser`: رابط کاربری مرورگر، تب‌ها، تب خصوصی، نوار آدرس هوشمند، بوکمارک، تاریخچه، دانلود، نصب PWA، جستجو در صفحه و حالت دسکتاپ.
   - `:feature-vault`: داشبورد مدیریت Never Sign-out، ماشین زمان نشست‌ها، و خروج دستی واقعی (`Sign out now`).
   - `:feature-settings`: تنظیمات ظاهر، امنیت بیومتریک، بلوکر تبلیغ و تنظیمات Proxy / DNS-over-HTTPS.
   - `:baselineprofile`: تولید و اعمال Baseline Profile برای شتاب‌دهی به راه‌اندازی اولیه.
