# راهنمای تنظیم GitHub Secrets برای امضای خودکار APKها

برای اینکه خط لوله CI/CD در GitHub Actions بتواند در هر Merge به شاخه `main` تمام ۵ خروجی APK (`arm64-v8a`، `armeabi-v7a`، `x86`، `x86_64` و `universal`) را با کلید اختصاصی شما امضا و در **GitHub Releases** منتشر کند، مراحل زیر را انجام دهید:

---

## ۱. ساخت فایل Keystore (در صورتی که قبلاً ندارید)

در ترمینال سیستم خود دستور زیر را اجرا کنید:

```bash
keytool -genkeypair -v \
  -keystore marblebr-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias marblebr
```

---

## ۲. تبدیل فایل Keystore به رشته Base64

- **در لینوکس:**
  ```bash
  base64 -w 0 marblebr-release.jks > keystore_base64.txt
  ```
- **در مک (macOS):**
  ```bash
  base64 -i marblebr-release.jks | tr -d '\n' > keystore_base64.txt
  ```

---

## ۳. ثبت متغیرها در GitHub Secrets

وارد مخزن خود در GitHub شوید و به مسیر زیر بروید:
**Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**

چهار Secret زیر را اضافه کنید:

| نام Secret | مقدار |
|---|---|
| `KEYSTORE_BASE64` | محتوای فایل `keystore_base64.txt` (رشته Base64 فایل `.jks`) |
| `KEYSTORE_PASSWORD` | رمز عبور مخزن کلید (Store Password) |
| `KEY_ALIAS` | نام مستعار کلید (مثلاً `marblebr`) |
| `KEY_PASSWORD` | رمز عبور کلید (Key Password) |

> برای انتشار از همان کلید نسخهٔ قبلی استفاده کنید. بدون کلید ثابت انتشار متوقف می‌شود؛ کلید موقت فقط برای بررسی CI است.

نام مستعار را می‌توان با دستور `keytool -list -keystore marblebr-release.jks` مشاهده کرد. اسکریپت `scripts/prepare_release_keystore.sh` فاصله‌های اضافی اطراف مقادیر Secretها را حذف می‌کند و پیش از بیلد، Keystore و رمزها را اعتبارسنجی میکند. اگر `KEY_ALIAS` با هیچ کلید خصوصی داخل Keystore مطابقت نداشته باشد و Keystore تنها یک کلید داشته باشد، از همان کلید استفاده می‌شود و هشداری ثبت می‌شود؛ در غیر این صورت انتشار با خطای واضح متوقف می‌شود.
