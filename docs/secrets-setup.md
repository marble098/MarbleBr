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

> **نکته:** تا پیش از تنظیم این چهار متغیر، ورک‌فلوهای `build.yml` و `release.yml` به‌صورت هوشمند یک کلید موقت (Fallback Keystore) در محیط CI می‌سازند تا بیلد و انتشار هر ۵ معماری APK بدون توقف انجام شود.
