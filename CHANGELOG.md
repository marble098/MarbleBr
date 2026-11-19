# تغییرات نسخه‌ها (Changelog)

تمامی تغییرات مهم پروژه **MarbleBr** در این فایل به زبان فارسی و با دسته‌بندی استاندارد ثبت می‌شوند.

## [1.1.0] - 2026-11-19

### 🛠 بهبود
- راه‌اندازی Workflow یکپارچه **Dependency Update (Unified)** برای آپدیت هم‌زمان تمام کتابخانه‌ها و تکنولوژی‌ها: Version Catalog کامل (AndroidX، Compose، Room، DataStore، Work، Hilt، Credentials)، موتور **GeckoView**، **Gradle Wrapper**، پلاگین‌های بیلد (AGP، Kotlin، KSP، Ktlint، Detekt) و تمام **GitHub Actions** — همه در یک اجرا و یک Pull Request واحد.
- ارتقای همه اکشن‌های GitHub به آخرین نسخه‌های اصلی: `actions/checkout@v7`، `actions/setup-java@v6`، `actions/upload-artifact@v7`، `gradle/actions@v6` و `softprops/action-gh-release@v3` (رفع هشدار منسوخ‌شدن Node 20).
- به‌روزرسانی کتابخانه‌ها: `appcompat` به `1.8.0`، `androidx.hilt` و `hilt-navigation-compose` به `1.4.0`، `datastore` به `1.3.0`، `kotlinx-serialization-json` به `1.13.0`، `robolectric` به `4.18.1` و `uiautomator` به `2.4.1`.
- حذف ابزارهای تکراری و متناقض آپدیت وابستگی (Dependabot و Renovate و Workflow هفتگی قدیمی) به‌نفع یک منبع واحد و قابل‌اتکا.
- استفاده از کش متن‌باز `basic` در `gradle/actions/setup-gradle@v6` و غیرفعال‌سازی اعتبارسنجی wrapper برای بیلد پایدارتر در CI.

### 🐛 رفع باگ
- **رفع شکست Workflow انتشار (`Release on Main Merge`):** کلیدهای `MARBLE_VERSION_NAME` و `MARBLE_VERSION_CODE` در `gradle.properties` وجود نداشتند و اسکریپت نسخه‌دهی با `set -o pipefail` بی‌صدا خارج می‌شد؛ اکنون کلیدها به‌صورت پیش‌فرض ثبت شده‌اند و اسکریپت در نبود آن‌ها مقدار پیش‌فرض ایمن (`0.0.0` / `0`) را می‌سازد و می‌نویسد.
- **رفع شکست همه PRهای وابستگی:** بررسی الزام‌آور `CHANGELOG.md` باعث Fail شدن PRهای خودکار Dependabot می‌شد؛ اکنون تغییرات صرفاً شامل وابستگی، CI، اسکریپت‌ها و مستندات از این الزام معاف‌اند، اما تغییرات کد اپ همچنان ورودی فارسی الزامی دارند.
- سخت‌سازی جمع‌آوری ۵ فایل APK در Workflow انتشار با خطای شفاف در صورت نبود هر معماری و مقاوم‌سازی Commit نسخه در برابر تغییرات هم‌زمان شاخه `main`.
- رفع خطای «workflow file issue» در Workflow انتشار (کلید YAML تکراری در گام `setup-gradle`) و افزودن تست دائمی تشخیص کلید تکراری به CI.
- افزودن تست‌های واحد آفلاین برای اسکریپت آپدیت وابستگی‌ها (`scripts/test_update_dependencies.py`) و اجرای آن‌ها در CI.
- پایدارسازی CI در برابر خطاهای گذرا: استفاده از کش متن‌باز `basic` در همه Workflowها و اعتبارسنجی خودکار ساختار YAML (کلید تکراری، فرمت و اکشن‌های مجاز) پیش از هر Commit.

## [1.0.0] - 2026-11-19

### ✨ جدید
- ساخت کامل موتور **Never Sign-out Engine** با تبدیل خودکار کوکی‌های موقت (Session Cookies) به کوکی‌های ماندگار با انقضای قابل تنظیم.
- پیاده‌سازی **Session Vault** با رمزنگاری سخت‌افزاری **AES-256-GCM (Android Keystore)** و محافظت بیومتریک (**BiometricPrompt**) برای ذخیره‌سازی دوره‌ای کوکی‌ها، `localStorage` و `IndexedDB`.
- افزودن **Session Time Machine** برای نگه‌داری تاریخچه اسنپ‌شات‌های سالم هر سایت و بازگشت به وضعیت قبلی تنها با یک لمس.
- پیاده‌سازی **Session Health Guard** جهت تشخیص هوشمند خروج ناخواسته (پاسخ `401`، ریدایرکت به صفحه لاگین، یا حذف کوکی احراز هویت) و ترمیم خودکار نشست پیش از متوجه شدن کاربر.
- افزودن **Smart Keep-alive** مبتنی بر **WorkManager** با رعایت محدودیت‌های باتری و شبکه برای تازه نگه‌داشتن نشست‌های دارای انقضای لغزشی (Sliding Sessions) در پس‌زمینه.
- ایزوله‌سازی کامل هر وب‌اپ و PWA در یک **Container مستقل** با `contextId` مجزا در موتور **GeckoView**.
- پیاده‌سازی سد دفاعی آخر با **Credential Manager / Passkey** و تأیید بیومتریک برای ورود مجدد بی‌صدا در صورت ابطال عمدی نشست توسط سرور.
- ساخت مرورگر کامل با رابط کاربری **Jetpack Compose + Material 3 Expressive**، پشتیبانی کامل فارسی/راست‌به‌چپ (RTL)، تب‌های معمولی و خصوصی، نوار آدرس هوشمند، بوکمارک، تاریخچه، مدیریت دانلود، نصب PWA در صفحه اصلی، جستجو در صفحه (Find in page)، حالت دسکتاپ، بلوکر تبلیغ سبک و تنظیمات Proxy/DNS-over-HTTPS.

### 🛠 بهبود
- بهینه‌سازی سرعت راه‌اندازی اولیه اپ با **Baseline Profile** و معماری ماژولار (`app`, `core`, `vault`, `engine`, `feature-browser`, `feature-vault`, `feature-settings`, `baselineprofile`).
- راه‌اندازی خط لوله CI/CD برای ساخت خودکار ۵ خروجی APK (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, `universal`)، ارتقای خودکار نسخه بر اساس حجم تغییرات، و انتشار خودکار در GitHub Releases.

### 🐛 رفع باگ
- جداسازی کامل عملیات «پاک کردن کش» (`ALL_CACHES`) از داده‌های نشست تا پاک‌سازی کش هرگز باعث خروج از حساب‌های کاربری نشود.
