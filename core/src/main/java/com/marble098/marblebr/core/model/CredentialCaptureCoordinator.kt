package com.marble098.marblebr.core.model

/**
 * هماهنگ‌کنندهٔ «ذخیرهٔ خودکار حرفه‌ای و هوشمند» — یک ماشینِ حالتِ کوچک و بی‌طرف.
 *
 * چرا ماشینِ حالت؟ چون تصمیم‌های این قابلیت به هم وابسته‌اند: پرکردنِ خودکار فقط اگر دامنه قابلِ
 * اعتماد باشد، پیشنهادِ ذخیره فقط اگر همان ورود قبلاً ذخیره نشده باشد، و مزاحمتِ دوباره فقط بعد از
 * مدتی. جدا کردنِ این وضعیت از GeckoView/Compose باعث می‌شود همهٔ مسیرها با تست واحد سنجیده شوند.
 */
sealed interface CaptureAction {
    /** فیلدها پر شوند. */
    data class Autofill(val username: String, val password: String, val site: String) : CaptureAction

    /** کارتِ «این ورود را ذخیره کنم؟» نمایش داده شود. */
    data class AskToSave(val site: String, val username: String, val password: String, val strength: PasswordStrength) : CaptureAction

    /** گذرواژهٔ تازه از گذرواژهٔ ذخیره‌شده ضعیف‌تر است؛ فقط تذکر بده. */
    data class WarnWeakerPassword(val site: String, val strength: PasswordStrength) : CaptureAction

    /** هشدارِ بدلِ ورود؛ پیش از تایپِ گذرواژه. */
    data class PhishingWarning(val verdict: LoginGuardVerdict) : CaptureAction

    /** هیچ کاری نکن. */
    data class None(val reason: String = "") : CaptureAction
}

data class CapturePreferences(
    val autoSaveEnabled: Boolean = true,
    val autoFillEnabled: Boolean = true,
    val biometricLockRequired: Boolean = true,
    /** فاصلهٔ حداقلی بین دو پیشنهادِ ذخیره برای همان سایت (تا مزاحمت نشود). */
    val promptCooldownMillis: Long = 12L * 60L * 60L * 1000L,
)

data class CaptureSessionState(
    val promptedSites: Set<String> = emptySet(),
    val dismissedAtMillis: Map<String, Long> = emptyMap(),
    val lastAutofilledSite: String? = null,
)

/**
 * ترتیبِ تصمیم‌ها دقیقاً همین است و هر مرحله می‌تواند کار را متوقف کند:
 * ۱. نگهبانِ بدل → ۲. پرکردنِ خودکار → ۳. مشاهدهٔ ارسال فرم → ۴. سنجشِ قابلِ ذخیره بودن →
 * ۵. مقایسه با آنچه از قبل داریم → ۶. پرسش.
 */
class CredentialCaptureCoordinator(
    private val preferences: CapturePreferences = CapturePreferences(),
    private val state: CaptureSessionState = CaptureSessionState(),
    private var nowEpochMillis: Long = System.currentTimeMillis(),
) {
    private val prompted = state.promptedSites.toMutableSet()
    private val dismissed = state.dismissedAtMillis.toMutableMap()

    fun advanceClock(epochMillis: Long) {
        nowEpochMillis = epochMillis
    }

    /**
     * تصمیمِ لحظهٔ بازشدنِ صفحه.
     *
     * @param observation نتیجهٔ اسکریپتِ کاوشِ صفحه
     * @param knownCredentials ورودهای ذخیره‌شده‌ای که دامنه‌شان به این صفحه می‌خورد
     */
    fun onPageObserved(
        pageUrl: String,
        observation: LoginPageObservation,
        knownCredentials: List<AutofillCandidate>,
        knownDomains: Set<String>,
    ): CaptureAction {
        val verdict = LoginOriginGuard.check(pageUrl, knownDomains)
        if (verdict.isWarning) return CaptureAction.PhishingWarning(verdict)

        val hasLoginForm = observation.passwordFieldCount > 0 || observation.oneTimeCodeFieldCount > 0
        if (!hasLoginForm) return CaptureAction.None("NO_LOGIN_FORM")

        if (preferences.autoFillEnabled) {
            val plan = CredentialAutofillPlanner.plan(pageUrl, knownCredentials, preferences.biometricLockRequired)
            val candidate = plan.candidate
            if (candidate != null) {
                // پرکردنِ خودکار فقط برای ورودهای همین دامنه؛ هرگز گذرواژهٔ سایت دیگری تزریق نمی‌شود.
                return CaptureAction.Autofill(username = candidate.username, password = "", site = candidate.domain)
            }
        }
        return CaptureAction.None("AWAITING_SUBMIT")
    }

    /**
     * تصمیمِ پس از ارسالِ فرمِ ورود.
     *
     * @param existing همان سایت/کاربر که از قبل در مخزن است (اگر باشد)
     */
    fun onSubmitObserved(
        pageUrl: String,
        submitted: LoginObservationProbe.SubmittedCredentials,
        observation: LoginPageObservation,
        existing: AutofillCandidate?,
    ): CaptureAction {
        if (!preferences.autoSaveEnabled) return CaptureAction.None("AUTO_SAVE_OFF")

        val signal = LoginFormSignal(
            pageUrl = pageUrl,
            username = submitted.username,
            password = submitted.password,
            passwordFieldCount = observation.passwordFieldCount,
            hasOneTimeCodeField = submitted.otpFieldCount > 0 || observation.oneTimeCodeFieldCount > 0,
            hiddenPasswordField = observation.passwordFieldCount == 0,
            submitted = true,
            navigationAfterSubmit = true,
            successIndicators = observation.passwordFieldCount == 0 || !observation.captchaPresent,
            errorIndicators = observation.browserRefusalText,
        )
        val assessment = CredentialCaptureHeuristics.assess(signal, nowEpochMillis)
        if (!assessment.shouldPrompt) return CaptureAction.None(assessment.decision.name)

        val site = normalizeSite(pageUrl)
        val strength = assessment.passwordStrength ?: PasswordQualityChecker.evaluate(submitted.password)
        val lastDismiss = dismissed[site]
        val cooling = lastDismiss != null && nowEpochMillis - lastDismiss < preferences.promptCooldownMillis

        if (cooling) return CaptureAction.None("COOLDOWN")
        if (site in prompted) return CaptureAction.None("ALREADY_SAVED")

        if (existing != null) {
            // ورودِ تکراری: اگر گذرواژه عوض نشده، مزاحمت نکن. اگر عوض شده اما ضعیف‌تر است، فقط تذکر.
            if (strength.score < existing.strengthScore - WEAKER_MARGIN) {
                prompted += site
                return CaptureAction.WarnWeakerPassword(site, strength)
            }
            return CaptureAction.None("ALREADY_SAVED")
        }

        prompted += site
        return CaptureAction.AskToSave(
            site = site,
            username = submitted.username,
            password = submitted.password,
            strength = strength,
        )
    }

    fun onUserDismissed(pageUrl: String) {
        dismissed[normalizeSite(pageUrl)] = nowEpochMillis
    }

    fun onUserAccepted(pageUrl: String) {
        dismissed.remove(normalizeSite(pageUrl))
    }

    /** بازگرداندنِ وضعیتِ یک هماهنگ‌کنندهٔ قبلی (مثلاً پس از تغییرِ تنظیمات). */
    fun restore(previous: CaptureSessionState) {
        prompted.clear()
        prompted += previous.promptedSites
        dismissed.clear()
        dismissed += previous.dismissedAtMillis
    }

    fun snapshot(): CaptureSessionState = CaptureSessionState(
        promptedSites = prompted.toSet(),
        dismissedAtMillis = dismissed.toMap(),
        lastAutofilledSite = state.lastAutofilledSite,
    )

    companion object {
        /** اختلافِ امتیاز که «ضعیف‌تر شدن» حساب می‌شود. */
        const val WEAKER_MARGIN = 10

        fun normalizeSite(url: String): String = CredentialCaptureHeuristics.domainOf(url)
    }
}
