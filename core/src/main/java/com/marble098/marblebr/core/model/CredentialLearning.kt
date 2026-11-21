package com.marble098.marblebr.core.model

import java.net.URI

/**
 * «ذخیرهٔ هوشمند و خودکارِ نام کاربری و گذرواژه» — مغزِ تصمیم‌گیری، جدا از اندروید و رمزنگاری.
 *
 * MarbleBr پس از یک ورودِ **موفق** (تشخیص از روی ناوبری به صفحهٔ بدونِ فرمِ ورود، پاسخِ ۲xx/۳xx
 * و نبودِ پیامِ خطا) از کاربر می‌پرسد «این ورود را برای این سایت ذخیره کنم؟» و اگر پذیرفت،
 * اطلاعات با AES-GCM در همان مخزنِ محلی ذخیره می‌شود. این فایل فقط سه تصمیم می‌گیرد:
 *
 * ۱. [CredentialCaptureHeuristics] — آیا این ورود «قابلِ ذخیره» است؟ (نه کدِ یک‌بارمصرف، نه
 *    گذرواژهٔ خالی، نه صفحهٔ ثبت‌نام).
 * ۲. [PasswordQualityChecker] — چند درصد گذرواژهٔ تازه امن است و چه چیزی را باید بهتر کند.
 * ۳. [CredentialAutofillPlanner] — برای این دامنه کدام رکورد «بهترین» است و آیا بازکردنِ قفلِ
 *    بیومتریک لازم است.
 */

/** یک مشاهدهٔ خام از فرمِ ورود، همان‌طور که content script می‌خواند. */
data class LoginFormSignal(
    val pageUrl: String,
    val username: String = "",
    val password: String = "",
    val passwordFieldCount: Int = 0,
    val isRegistrationPage: Boolean = false,
    val hasOneTimeCodeField: Boolean = false,
    val hiddenPasswordField: Boolean = false,
    val submitted: Boolean = true,
    val navigationAfterSubmit: Boolean = true,
    val successIndicators: Boolean = true,
    val errorIndicators: Boolean = false,
) {
    val hasCredentials: Boolean get() = username.isNotBlank() && password.isNotBlank()
}

enum class CaptureDecision {
    ELIGIBLE,
    EMPTY_CREDENTIALS,
    REGISTRATION_PAGE,
    ONE_TIME_CODE,
    NOT_SUBMITTED,
    NO_SUCCESS_EVIDENCE,
    ERROR_SHOWN,
}

data class CaptureAssessment(
    val decision: CaptureDecision,
    val domain: String,
    /** آیا این گذرواژه شایستهٔ پیشنهادِ «قوی‌کردن» است؟ */
    val passwordStrength: PasswordStrength? = null,
) {
    val shouldPrompt: Boolean get() = decision == CaptureDecision.ELIGIBLE
}

object CredentialCaptureHeuristics {

    fun assess(signal: LoginFormSignal, nowEpochMillis: Long = System.currentTimeMillis()): CaptureAssessment {
        val domain = domainOf(signal.pageUrl)
        val decision = when {
            !signal.hasCredentials -> CaptureDecision.EMPTY_CREDENTIALS
            signal.isRegistrationPage -> CaptureDecision.REGISTRATION_PAGE
            signal.hasOneTimeCodeField && signal.password.length <= 8 -> CaptureDecision.ONE_TIME_CODE
            !signal.submitted -> CaptureDecision.NOT_SUBMITTED
            signal.errorIndicators -> CaptureDecision.ERROR_SHOWN
            !signal.navigationAfterSubmit && !signal.successIndicators -> CaptureDecision.NO_SUCCESS_EVIDENCE
            else -> CaptureDecision.ELIGIBLE
        }
        return CaptureAssessment(
            decision = decision,
            domain = domain,
            passwordStrength = if (decision == CaptureDecision.ELIGIBLE) {
                PasswordQualityChecker.evaluate(signal.password, signal.username, domain)
            } else {
                null
            },
        )
    }

    fun domainOf(url: String): String = runCatching {
        val normalized = if (url.contains("://")) url.trim() else "https://${url.trim()}"
        URI(normalized).host?.lowercase()?.removePrefix("www.")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: ""

    /** قالبِ نمایشِ نام کاربری روی کارت پیشنهاد؛ ایمیل/شماره کوتاه می‌شود. */
    fun maskUsername(username: String): String {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return ""
        val at = trimmed.indexOf('@')
        if (at > 0) {
            val name = trimmed.substring(0, at)
            val domain = trimmed.substring(at)
            val visible = if (name.length <= 2) name.take(1) else name.take(2)
            return "$visible•••$domain"
        }
        if (trimmed.length <= 4) return trimmed
        return "${trimmed.take(2)}•••${trimmed.takeLast(2)}"
    }
}

enum class PasswordStrengthLevel { VERY_WEAK, WEAK, FAIR, STRONG, EXCELLENT }

data class PasswordStrength(
    val score: Int,
    val level: PasswordStrengthLevel,
    val suggestions: List<String> = emptyList(),
) {
    /** برای نمایشِ نوارِ ۰..۱ در رابط کاربری. */
    val fraction: Float get() = (score.coerceIn(0, 100)) / 100f
}

object PasswordQualityChecker {

    private const val MIN_LENGTH = 8
    private const val GOOD_LENGTH = 14

    fun evaluate(password: String, username: String = "", domain: String = ""): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(0, PasswordStrengthLevel.VERY_WEAK, listOf("CHOOSE_A_PASSWORD"))
        }
        var score = 0
        val suggestions = mutableListOf<String>()

        score += when {
            password.length >= 20 -> 30
            password.length >= GOOD_LENGTH -> 26
            password.length >= 12 -> 20
            password.length >= MIN_LENGTH -> 12
            else -> 2
        }
        if (password.length < GOOD_LENGTH) suggestions += "USE_LONGER_PASSWORD"

        val classes = listOf(
            password.any(Char::isLowerCase),
            password.any(Char::isUpperCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        score += classes * 10
        if (classes < 3) suggestions += "MIX_LETTERS_NUMBERS_SYMBOLS"

        val uniqueRatio = password.toSet().size.toFloat() / password.length
        score += (uniqueRatio * 15).toInt()
        if (uniqueRatio < 0.5f) suggestions += "AVOID_REPEATED_CHARACTERS"

        val lowered = password.lowercase()
        val commonFragments = listOf(
            "password", "123456", "qwerty", "admin", "welcome", "iloveyou", "letmein", "abc123",
            "111111", "000000", "changeme", "secret", "marble",
        )
        if (commonFragments.any { lowered.contains(it) }) {
            score -= 35
            suggestions += "AVOID_COMMON_PASSWORDS"
        }
        if (username.length >= 3 && lowered.contains(username.lowercase())) {
            score -= 20
            suggestions += "DONT_REUSE_USERNAME"
        }
        val domainLabel = domain.substringBefore('.').lowercase()
        if (domainLabel.length >= 3 && lowered.contains(domainLabel)) {
            score -= 15
            suggestions += "DONT_USE_SITE_NAME"
        }
        if (password.all { it.isDigit() }) {
            score -= 25
            suggestions += "DONT_USE_ONLY_NUMBERS"
        }
        if (Regex("^(.)\\1+$").matches(password)) {
            score -= 40
            suggestions += "AVOID_REPEATED_CHARACTERS"
        }

        val clamped = score.coerceIn(0, 100)
        val level = when {
            clamped < 25 -> PasswordStrengthLevel.VERY_WEAK
            clamped < 45 -> PasswordStrengthLevel.WEAK
            clamped < 65 -> PasswordStrengthLevel.FAIR
            clamped < 85 -> PasswordStrengthLevel.STRONG
            else -> PasswordStrengthLevel.EXCELLENT
        }
        return PasswordStrength(clamped, level, suggestions.distinct())
    }
}

/** رکوردِ سبکِ لازم برای تصمیمِ «کدام ورود را پیشنهاد بدهیم». */
data class AutofillCandidate(
    val id: String,
    val domain: String,
    val username: String,
    val updatedAtEpochMillis: Long,
    val lastUsedAtEpochMillis: Long? = null,
    val strengthScore: Int = 0,
)

data class AutofillPlan(
    val candidate: AutofillCandidate?,
    val requiresBiometricUnlock: Boolean,
    val alternativesCount: Int = 0,
)

object CredentialAutofillPlanner {

    /**
     * بهترین رکورد برای دامنهٔ جاری.
     *
     * ترتیبِ اولویت: دقیقاً همان دامنه → زیردامنهٔ همان دامنه → تازه‌ترین استفاده → تازه‌ترین
     * ویرایش. «همیشه قوی‌ترین گذرواژه» معیارِ درستی نیست؛ کاربر ممکن است برای یک سایت چند حساب
     * داشته باشد و انتظار دارد آخرین ورود باز شود.
     */
    fun plan(
        pageUrl: String,
        candidates: List<AutofillCandidate>,
        biometricLockRequired: Boolean,
    ): AutofillPlan {
        val domain = domainOf(pageUrl)
        if (domain.isBlank() || candidates.isEmpty()) {
            return AutofillPlan(candidate = null, requiresBiometricUnlock = false)
        }
        val exact = candidates.filter { it.domain.equals(domain, ignoreCase = true) }
        val related = if (exact.isNotEmpty()) {
            exact
        } else {
            candidates.filter { candidate ->
                val other = candidate.domain.lowercase()
                other.isNotBlank() && (domain.endsWith(".$other") || other.endsWith(".$domain"))
            }
        }
        val ranked = related.sortedWith(
            compareByDescending<AutofillCandidate> { it.lastUsedAtEpochMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.updatedAtEpochMillis }
                .thenByDescending { it.strengthScore },
        )
        return AutofillPlan(
            candidate = ranked.firstOrNull(),
            requiresBiometricUnlock = biometricLockRequired && ranked.isNotEmpty(),
            alternativesCount = (ranked.size - 1).coerceAtLeast(0),
        )
    }

    fun domainOf(url: String): String = CredentialCaptureHeuristics.domainOf(url)
}
