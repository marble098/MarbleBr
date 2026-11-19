# تصمیم‌های معماری و فنی (Architecture & Engineering Decisions)

این سند تصمیم‌های کلیدی معماری و فنی پروژه **MarbleBr** را توضیح می‌دهد تا علت انتخاب هر راهکار برای توسعه‌دهندگان و Agentها شفاف باشد.

---

## ۱. انتخاب موتور GeckoView و ایزوله‌سازی مبتنی بر `contextId`

- **تصمیم:** استفاده از موتور **Mozilla GeckoView** (`org.mozilla.geckoview:geckoview`) به‌جای `android.webkit.WebView` سیستمی، و اختصاص یک `contextId` یکتا (`marble_ctx_<containerId>`) در `GeckoSessionSettings.Builder().contextId(...)` برای هر وب‌اپ و PWA.
- **دلیل:**
  1. در `WebView` اندروید، `CookieManager.getInstance()` و مخزن ذخیره‌سازی بین تمام تب‌ها و وب‌اپ‌ها مشترک است و امکان داشتن چندین کانتینر کاملاً ایزوله وجود ندارد.
  2. در GeckoView، پارامتر `contextId` به‌طور بومی Cookie Jar، `localStorage`، `IndexedDB` و Cache API را در سطح موتور مرورگر ایزوله می‌کند.
  3. خرابی، هنگ یا خروج دستی (`Sign out now`) در یک وب‌اپ هیچ اثری روی نشست سایر وب‌اپ‌ها ندارد.

---

## ۲. معماری دوگانه برای دسترسی به کوکی‌ها و ذخیره‌سازی (WebExtension + Native Bridge)

- **تصمیم:** استفاده از یک افزونه داخلی امضاشده/Built-in در GeckoView (`resource://android/assets/extensions/never_signout/`) در کنار لایه Kotlin در ماژول‌های `:engine` و `:vault`.
- **دلیل:**
  1. موتور GeckoView به دلایل امنیتی اجازه دست‌کاری مستقیم کوکی‌های `HttpOnly` یا خواندن `localStorage` و `IndexedDB` را از طریق API جاوا بدون افزونه نمی‌دهد.
  2. افزونه داخلی با دسترسی‌های `cookies`، `storage`، `webRequest`، `webRequestBlocking` و `<all_urls>` می‌تواند:
     - کوکی‌های Session (بدون تاریخ انقضا) را در لحظه دریافت به کوکی‌های ماندگار با انقضای بلند (پیش‌فرض ۳۶۵ روز، قابل تنظیم برای هر سایت) تبدیل کند.
     - فراخوانی خودکار `navigator.storage.persist()` را در هر سایت اجرا کند تا سیستم‌عامل اندروید تحت فشار حافظه، داده‌های سایت را پاک نکند.
     - از کوکی‌ها، `localStorage` و `IndexedDB` اسنپ‌شات گرفته و از طریق `runtime.sendNativeMessage("marble_never_signout", ...)` به ماژول `:vault` تحویل دهد تا با AES-256-GCM رمزنگاری شود.

---

## ۳. رمزنگاری سخت‌افزاری Session Vault با Android Keystore و AES-256-GCM

- **تصمیم:** تمامی اسنپ‌شات‌های نشست پیش از ذخیره در دیتابیس Room، توسط کلید متقارن ۲۵۶ بیتی تولیدشده در `AndroidKeyStore` با الگوریتم `AES/GCM/NoPadding` (طول IV برابر ۱۲ بایت و تگ احراز هویت ۱۲۸ بیت) رمزنگاری می‌شوند و همراه با هش یکپارچگی `SHA-256` ذخیره می‌گردند.
- **دلیل:**
  - کلید رمزنگاری هرگز از محیط امن سخت‌افزاری (TEE / StrongBox در صورت پشتیبانی دستگاه) خارج نمی‌شود.
  - دسترسی به بازیابی دستی اسنپ‌شات‌ها و مدیریت حساس Vault پشت تایید بیومتریک (`BiometricPrompt` با `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) قرار دارد.
  - طراحی `SecretKeyProvider` به‌صورت رابط (Interface) امکان تزریق کلید در تست‌های واحد JVM را فراهم می‌کند تا منطق رمزنگاری و رمزگشایی AES-GCM به‌طور واقعی و بدون وابستگی به شبیه‌ساز تست شود.

---

## ۴. منطق Session Health Guard و جلوگیری از حلقه بی‌نهایت (Circuit Breaker)

- **تصمیم:** پایش پیوسته وضعیت نشست از طریق سه سیگنال:
  1. کدهای وضعیت HTTP (`401 Unauthorized`، `419 Authentication Timeout`، `440 Login Time-out`).
  2. ریدایرکت‌های ناگهانی به مسیرهای احراز هویت (`/login`، `/signin`، `/auth`، `/sso` همراه با پارامترهایی نظیر `returnUrl=` یا `redirect_to=`).
  3. حذف یا خالی شدن کوکی‌های حیاتی احراز هویت (`session`، `sid`، `token`، `jwt`، `__Host-*`، `__Secure-*`).
- **دلیل و مکانیزم ایمنی:**
  - به محض تشخیص خروج ناخواسته، `SessionHealthGuard` آخرین اسنپ‌شات سالم (`isHealthy = true`) را از `SessionVault` بازیابی می‌کند.
  - **محدودیت صادقانه و سد دفاعی هشتم:** اگر سرور نشست را در سمت خود باطل کرده باشد (مثلاً کاربر از دستگاه دیگر رمز عبور را تغییر داده یا نشست را Revoke کرده باشد)، بازگردانی کوکی قبلی مجدداً با پاسخ `401` مواجه می‌شود. در این حالت، **Circuit Breaker** پس از ۲ تلاش ناموفق در بازه زمانی کوتاه، ابطال سمت سرور (`SERVER_REVOKED`) را تشخیص داده و به‌جای تکرار بی‌فایده، به سد دفاعی هشتم (**Credential Manager / Passkey** با تأیید بیومتریک) سوییچ می‌کند تا ورود مجدد بی‌صدا انجام شود.

---

## ۵. جداسازی کامل «پاک کردن کش» از داده‌های نشست

- **تصمیم:** دکمه «پاک کردن کش» در اپ صرفاً پرچم `StorageController.ClearFlags.ALL_CACHES` (`NETWORK_CACHE | IMAGE_CACHE`) را صدا می‌زند و هرگز پرچم‌های `COOKIES`، `DOM_STORAGES` یا `AUTH_SESSIONS` را لمس نمی‌کند.
- **دلیل:** در مرورگرهای سنتی، کاربران هنگام آزادسازی فضای ذخیره‌سازی به اشتباه از تمام حساب‌های خود خارج می‌شوند. در MarbleBr تنها دکمه صریح **«خروج واقعی از این سایت (Sign out now)»** مجاز به پاک کردن کوکی‌ها، کانتینر و اسنپ‌شات‌های آن سایت است.

---

## ۶. معماری ماژولار و سازگاری با AGP 9.5 + Kotlin 2.4

- **تصمیم:** تفکیک پروژه به ۸ ماژول مجزا (`:app`، `:core`، `:vault`، `:engine`، `:feature-browser`، `:feature-vault`، `:feature-settings`، `:baselineprofile`) با استفاده از Version Catalog (`gradle/libs.versions.toml`)، Hilt + KSP و Compose Material 3 Expressive.
- **دلیل:** کاهش زمان بیلد، جداسازی مسئولیت‌ها، قابلیت تست‌پذیری مستقل هر ماژول، و بهینه‌سازی زمان بالا آمدن اپ با Baseline Profile.
