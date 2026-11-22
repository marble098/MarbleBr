package com.marble098.marblebr.core.model

/**
 * «پیشنهادِ سریعِ ذخیره» — سیاستِ تصمیم‌گیریِ مستقل از اندروید.
 *
 * سه حالتِ ممکن بعد از یک ورودِ موفق:
 * ۱. [SaveOfferAction.Offer] — کارتِ آماده با فیلدهای از پیش‌پر‌شده نشان داده شود؛ کاربر با یک
 *    ضربه ذخیره می‌کند (یا نامِ حساب را عوض می‌کند). این حالت وقتی برمی‌گردد که «ابهام» وجود دارد:
 *    چند حسابِ مختلف برای یک سایت، اولین ورود، یا گذرواژهٔ تازه (احتمالِ تغییرِ رمز).
 * ۲. [SaveOfferAction.AutoSave] — بدونِ پرسیدن ذخیره شود؛ فقط وقتی کاربر صریحاً «ذخیرهٔ خودکار»
 *    را روشن کرده باشد و تصمیم بی‌ابهام باشد.
 * ۳. [SaveOfferAction.Suppressed] — هیچ پیشنهادی ساخته نشود (تبِ خصوصی، پیش‌تر رد شده، و…).
 *
 * چرا این تفکیک لازم است؟ تجربهٔ رایجِ مرورگرها این است که کاربر بعد از ورود، کارتِ «ذخیره کنم؟»
 * را رد می‌کند؛ ولی نسخهٔ فعلی MarbleBr حتی برای حسابِ «همیشه همان» هم هر بار می‌پرسید و
 * «قبلاً ذخیره شده» را در وضعیتِ یک‌بارمصرفِ حافظه نگه می‌داشت که با بستنِ اپ پاک می‌شد. این‌جا
 * تصمیم بر پایهٔ **داده‌های ماندگارِ مخزن** گرفته می‌شود، نه یک مجموعهٔ موقت.
 */

enum class CredentialSaveMode {
    /** هرگز ذخیره نکن (کاربر خاموش کرده). */
    DISABLED,

    /** همیشه بپرس (پیش‌فرضِ محافظه‌کارانه). */
    ASK,

    /** بی‌ابهام‌ها را بی‌صدا ذخیره کن؛ مبهم‌ها را بپرس. */
    AUTO_SAVE,
}

enum class AutoSaveReason {
    /** برای این سایت هیچ حسابی ذخیره نشده بود. */
    FIRST_ACCOUNT,

    /** همان حساب با گذرواژهٔ تازه (احتمالِ تغییرِ رمز یا ورودِ تازه). */
    PASSWORD_UPDATED,
}

enum class SuppressReason {
    PRIVATE_TAB,
    SAVE_DISABLED,
    RECENTLY_DISMISSED,
    ALREADY_SAVED,
    NO_PASSWORD,
    UNSAFE_ORIGIN,
    ONE_TIME_CODE_ONLY,
}

sealed interface SaveOfferAction {
    data class Offer(val reason: OfferReason) : SaveOfferAction

    data class AutoSave(val reason: AutoSaveReason) : SaveOfferAction

    data class Suppressed(val reason: SuppressReason) : SaveOfferAction
}

enum class OfferReason {
    /** هیچ حسابی برای این سایت نداریم. */
    FIRST_ACCOUNT,

    /** حسابِ دیگری (نامِ کاربریِ متفاوت) برای همین سایت داریم؛ کاربر باید حساب را تأیید کند. */
    ANOTHER_ACCOUNT,

    /** همان حساب با گذرواژهٔ تازه. */
    PASSWORD_CHANGED,
}

/** حسابِ ذخیره‌شده‌ای که ممکن است به این ورود مربوط باشد. */
data class KnownAccount(
    val username: String,
    val site: String,
    val updatedAtEpochMillis: Long = 0L,
    val strengthScore: Int = 0,
    /** اثر انگشتِ کوتاهِ گذرواژهٔ ذخیره‌شده (نه خودِ گذرواژه)؛ خالی یعنی در دسترس نیست. */
    val passwordFingerprint: String = "",
)

object CredentialSavePolicy {

    /** فاصلهٔ احترام به «الآن نه»ِ کاربر برای همان سایت. */
    const val DISMISS_COOLDOWN_MILLIS = 12L * 60L * 60L * 1000L

    /**
     * @param knownAccounts همهٔ حساب‌های ذخیره‌شدهٔ همین دامنه (نه همهٔ سایت‌ها)
     * @param dismissedAt آخرین بارِ «الآن نه» برای همین دامنه (`null` یعنی هرگز)
     */
    fun decide(
        username: String,
        password: String,
        isPrivateTab: Boolean,
        saveEnabled: Boolean,
        mode: CredentialSaveMode,
        knownAccounts: List<KnownAccount>,
        dismissedAtEpochMillis: Long?,
        nowEpochMillis: Long,
        originIsWarning: Boolean = false,
        hasOneTimeCodeOnly: Boolean = false,
    ): SaveOfferAction {
        if (mode == CredentialSaveMode.DISABLED) return SaveOfferAction.Suppressed(SuppressReason.SAVE_DISABLED)
        if (isPrivateTab) return SaveOfferAction.Suppressed(SuppressReason.PRIVATE_TAB)
        if (!saveEnabled) return SaveOfferAction.Suppressed(SuppressReason.SAVE_DISABLED)
        if (password.isBlank()) return SaveOfferAction.Suppressed(SuppressReason.NO_PASSWORD)
        if (hasOneTimeCodeOnly) return SaveOfferAction.Suppressed(SuppressReason.ONE_TIME_CODE_ONLY)
        if (originIsWarning) return SaveOfferAction.Suppressed(SuppressReason.UNSAFE_ORIGIN)

        val sameAccount = knownAccounts.firstOrNull { normalizeUsername(it.username) == normalizeUsername(username) }

        if (dismissedAtEpochMillis != null && nowEpochMillis - dismissedAtEpochMillis in 0 until DISMISS_COOLDOWN_MILLIS) {
            // اگر کاربر بعد از ردکردن، گذرواژه را عوض کرده باشد، پیشنهادِ تازه معنا دارد.
            if (sameAccount == null) return SaveOfferAction.Suppressed(SuppressReason.RECENTLY_DISMISSED)
        }

        // همان حسابِ ذخیره‌شده: فقط اگر گذرواژه واقعاً عوض شده باشد پیشنهاد/ذخیرهٔ تازه معنا دارد.
        if (sameAccount != null) {
            val changed = passwordDiffers(sameAccount, password)
            if (!changed) return SaveOfferAction.Suppressed(SuppressReason.ALREADY_SAVED)
            return when (mode) {
                CredentialSaveMode.AUTO_SAVE -> SaveOfferAction.AutoSave(AutoSaveReason.PASSWORD_UPDATED)
                else -> SaveOfferAction.Offer(OfferReason.PASSWORD_CHANGED)
            }
        }

        val othersOnSite = knownAccounts.filter { it.username.isNotBlank() }
        if (othersOnSite.isNotEmpty()) {
            // چند حسابِ هم‌سایت: هرگز خودکار ذخیره نکن، چون ممکن است حساب را اشتباه به هم بچسبانیم.
            return SaveOfferAction.Offer(OfferReason.ANOTHER_ACCOUNT)
        }

        return when (mode) {
            CredentialSaveMode.AUTO_SAVE -> SaveOfferAction.AutoSave(AutoSaveReason.FIRST_ACCOUNT)
            else -> SaveOfferAction.Offer(OfferReason.FIRST_ACCOUNT)
        }
    }

    /**
     * آیا گذرواژهٔ تازه با گذرواژهٔ ذخیره‌شده فرق دارد؟
     *
     * مخزن عمداً هشِ بدونِ کلید از گذرواژه نگه نمی‌دارد (حملهٔ حدسِ آفلاین)؛ پس مقایسهٔ مستقیم
     * ممکن نیست. به‌جایش از «اثر انگشتِ کوتاه و بی‌خطر» استفاده می‌شود که لایهٔ مخزن هنگامِ
     * ذخیره می‌سازد و فقط ۴ بایت از یک HMAC با کلیدِ دستگاه است. اگر اثر انگشت نباشد،
     * تشخیص تغییر را به لایهٔ بالاتر (که خودِ رکورد را رمزگشایی می‌کند) واگذار می‌کنیم.
     */
    fun passwordDiffers(account: KnownAccount, password: String): Boolean {
        val fingerprint = account.passwordFingerprint
        // اثر انگشتِ خالی یعنی «نمی‌دانیم»؛ در این حالت مزاحمت نمی‌کنیم (وصول به مخزن همیشه
        // اثر انگشت را از گذرواژهٔ رمزگشایی‌شده می‌سازد، پس این فقط برای رکوردهای کهنه است).
        if (fingerprint.isBlank()) return false
        return fingerprint != fingerprintOf(password)
    }

    /** اثر انگشتِ کوتاه؛ همان الگوریتمی که لایهٔ مخزن استفاده می‌کند. */
    fun fingerprintOf(password: String): String = PasswordFingerprint.of(password)

    /**
     * نامِ کاربری برای مقایسهٔ حساب‌ها.
     *
     * موردِ رایج: سایت‌ها شمارهٔ موبایل را یک‌بار `09123456789` و یک‌بار `+989123456789` نشان
     * می‌دهند؛ اگر این دو را یکی ندانیم، هر ورود یک حسابِ تازه ساخته می‌شود (باگِ پرشدنِ مخزن با
     * رکوردهای تکراری).
     */
    fun normalizeUsername(username: String): String {
        val trimmed = username.trim().lowercase()
        if (trimmed.isEmpty()) return ""
        val digitsOnly = trimmed.filter { it.isDigit() || it == '+' }
        val isPhoneLike = digitsOnly.length >= 10 && digitsOnly.length <= 16 &&
            (digitsOnly.startsWith("+") || digitsOnly.startsWith("0") || digitsOnly.startsWith("98"))
        if (!isPhoneLike) return trimmed
        return digitsOnly
            .removePrefix("+98")
            .removePrefix("0098")
            .removePrefix("98")
            .removePrefix("0")
            .let { "0$it" }
    }

    /** فهرستِ ایمن برای نمایشِ حساب‌ها در کارتِ پیشنهاد. */
    fun describeAccounts(accounts: List<KnownAccount>): List<String> =
        accounts.map { it.username }.filter { it.isNotBlank() }.distinct()

    /** آیا این ورود، همان ورودِ ذخیره‌شده است؟ (برای نمایشِ «قبلاً ذخیره شده») */
    fun isSameSaved(account: KnownAccount?, username: String, password: String): Boolean =
        account != null &&
            normalizeUsername(account.username) == normalizeUsername(username) &&
            password.isNotBlank()
}
