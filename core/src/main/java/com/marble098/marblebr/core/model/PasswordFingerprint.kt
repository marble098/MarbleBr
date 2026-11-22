package com.marble098.marblebr.core.model

import java.security.MessageDigest

/**
 * «اثر انگشتِ کوتاهِ گذرواژه» — چطور بفهمیم گذرواژهٔ تازه با ذخیره‌شده یکی است، بدونِ نگه‌داشتنِ
 * چیزی که به حملهٔ حدسِ آفلاین کمک کند؟
 *
 * قاعده: هشِ بدونِ کلید ممنوع است. اگر `sha256(password)` را ذخیره کنیم، هر کسی که به دیتابیس
 * دسترسی پیدا کند می‌تواند با یک جدولِ رنگین‌کمانی گذرواژه را پیدا کند. پس:
 * - کلیدِ HMAC یک رشتهٔ تصادفی و **دستگاه‌محلی** است (در Keystore ذخیره می‌شود و هرگز از اپ
 *   بیرون نمی‌رود). بدونِ آن کلید، اثر انگشت هیچ اطلاعاتی از گذرواژه نمی‌دهد.
 * - خروجی فقط ۳۲ بیت (۴ بایت) است؛ عمداً کوتاه تا «تأیید یکسان بودن» کار کند و «بازسازی» نه.
 * - نمک به ازای هر رکورد (شناسه) اضافه می‌شود تا دو حساب با گذرواژهٔ یکسان اثر انگشتِ یکسان نداشته باشند.
 *
 * این کلاسِ خالص فقط ریاضیات است؛ سیاستِ کلید در لایهٔ مخزن است.
 */
object PasswordFingerprint {

    const val LENGTH = 8

    /**
     * نسخهٔ آمادهٔ [Mac] — برای وقتی که کلید **قابل استخراج نیست** (مثل AndroidKeyStore).
     *
     * چرا لازم شد؟ کلیدِ HMAC در Keystore با `PURPOSE_SIGN` ساخته می‌شود و `key.encoded` آن `null`
     * است؛ پس راهِ درست این است که خودِ `Mac` را با آن کلید بسازیم و بدهیم، نه بایت‌های کلید را.
     * اگر آن‌جا `encoded` را بخوانیم، اثر انگشت همیشه خطا می‌داد و «گذرواژه عوض شد» هرگز
     * تشخیص داده نمی‌شد.
     */
    fun of(password: String, mac: javax.crypto.Mac, salt: String = ""): String {
        if (password.isEmpty()) return ""
        mac.update(salt.toByteArray(Charsets.UTF_8))
        mac.update(0)
        val digest = mac.doFinal(password.toByteArray(Charsets.UTF_8))
        return digest.take(4).joinToString("") { "%02x".format(it) }.take(LENGTH)
    }

    /**
     * @param key رازِ دستگاه (حداقل ۱۶ بایت)؛ برای آزمون‌ها می‌شود رشتهٔ ثابت داد
     * @param salt شناسهٔ رکورد (نه نامِ کاربر) تا اثر انگشت‌ها یکتا شوند
     */
    fun of(password: String, key: ByteArray = DEFAULT_TEST_KEY, salt: String = ""): String {
        if (password.isEmpty()) return ""
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256"))
        return of(password, mac, salt)
    }

    /** آیا کلیدِ داده‌شده برای HMAC معتبر است؟ (نه خالی، نه کوتاه) */
    fun isUsableKey(key: ByteArray?): Boolean = key != null && key.size >= 16

    /** هشِ یکسان‌سازِ کوتاه برای مقایسه در آزمون‌ها و دیباگ؛ هرگز برای گذرواژه استفاده نمی‌شود. */
    fun stableHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .take(4)
        .joinToString("") { "%02x".format(it) }

    /**
     * کلیدِ پیش‌فرض فقط برای آزمون‌های واحد. در اپ همیشه کلیدِ دستگاه تزریق می‌شود؛ اگر کسی
     * این پیش‌فرض را در مسیرِ واقعی ببیند، یعنی باگِ پیکربندی است و آزمونِ مخزن باید بگیرد.
     */
    val DEFAULT_TEST_KEY: ByteArray = "marblebr-test-fingerprint-key".toByteArray(Charsets.UTF_8)
}
