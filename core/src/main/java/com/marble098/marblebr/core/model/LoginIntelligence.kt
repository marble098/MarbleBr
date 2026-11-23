package com.marble098.marblebr.core.model

/**
 * مغزِ «ورودِ هوشمند» — یک ماشین‌حالتِ کامل، بی‌طرف از اندروید و GeckoView.
 *
 * چرا یک ماشین‌حالتِ واحد؟ پیش‌تر تصمیم‌ها در سه لایهٔ جدا (content script، PasswordStudio و
 * ViewModel) پخش بودند و هر لایه بخشی از وضعیت را نگه می‌داشت؛ نتیجه این بود که:
 * - کارتِ «ذخیره کنم؟» بعد از بستنِ اپ دیگر هرگز برنمی‌گشت (وضعیت در حافظه بود).
 * - «پرکردنِ خودکار» عملاً هیچ‌وقت فیلدها را پر نمی‌کرد (فقط یک اعلان می‌ساخت).
 * - کدِ یکبارمصرف هیچ مسیری برای رسیدن به کادر نداشت.
 *
 * این‌جا همهٔ ورودی‌ها (کاوشِ صفحه، ارسالِ فرم، پیامکِ تازه، تصمیمِ کاربر) و همهٔ خروجی‌ها
 * ([LoginAdvance] → دستور به موتور) در یک جا و آزمون‌پذیر هستند.
 */

/** یک حسابِ ذخیره‌شده با اطلاعاتِ کافی برای «پرکردن». */
data class LoginAccount(
    val id: String,
    val domain: String,
    val username: String,
    val strengthScore: Int = 0,
    val updatedAtEpochMillis: Long = 0L,
    val lastUsedAtEpochMillis: Long? = null,
    /** امیدِ عمرِ کوکیِ ورود (روز)؛ برای رتبه‌بندیِ «تازه‌ترین ورود» به‌کار می‌رود. */
    val sessionDays: Int = 0,
) {
    fun asCandidate(): AutofillCandidate = AutofillCandidate(
        id = id,
        domain = domain,
        username = username,
        updatedAtEpochMillis = updatedAtEpochMillis,
        lastUsedAtEpochMillis = lastUsedAtEpochMillis,
        strengthScore = strengthScore,
    )
}

/** تنظیماتِ زنده‌ای که از صفحهٔ تنظیمات می‌آید. */
data class LoginPolicy(
    val saveMode: CredentialSaveMode = CredentialSaveMode.ASK,
    val autoFillEnabled: Boolean = true,
    val loginGuardEnabled: Boolean = true,
    val otpAssistEnabled: Boolean = true,
    val biometricConfirmedInSession: Boolean = false,
    /** آیا برای پرکردنِ خودکار قفلِ بیومتریک لازم است؟ (تنظیمِ کاربر، جدا از پنجرهٔ اعتماد) */
    val biometricLockRequired: Boolean = true,
)

/** آنچه از فرمِ صفحه می‌دانیم. هیچ مقدارِ محرمانه‌ای در این ساختار نیست. */
data class LoginFormFacts(
    val passwordFieldCount: Int = 0,
    val usernameFieldCount: Int = 0,
    val otpFieldCount: Int = 0,
    val captchaPresent: Boolean = false,
    val isVisible: Boolean = true,
)

/** نتیجهٔ یک ارسالِ فرم که content script در همان لحظه خوانده است. */
data class SubmittedLogin(
    val username: String,
    val password: String,
    val otpFieldCount: Int = 0,
)

/** دستوری که موتور باید اجرا کند. */
sealed interface LoginAdvance {
    /** این صفحه فرمِ ورود ندارد؛ کاری لازم نیست. */
    data object Nothing : LoginAdvance

    /** هشدارِ دامنهٔ بدل؛ پیش از هر پرکردن/ذخیره‌ای. */
    data class WarnOrigin(val verdict: LoginGuardVerdict) : LoginAdvance

    /** فرم را از حسابِ ذخیره‌شده پر کن (اسکریپت با رمزنگاری در لایهٔ مخزن ساخته می‌شود). */
    data class FillCredentials(
        val account: LoginAccount,
        val needsBiometric: Boolean,
        val alternatives: Int,
        /** اسکریپتِ آماده برای اجرا؛ `null` یعنی لایهٔ مخزن باید بسازد. */
        val script: String? = null,
    ) : LoginAdvance

    /** کدِ یکبارمصرف را در کادر(های) صفحه بنویس. */
    data class FillOneTimeCode(val code: String, val plan: OtpFillPlan) : LoginAdvance

    /** کارتِ «این ورود را ذخیره کنم؟» را با مقادیرِ از پیش‌پر نشان بده. */
    data class OfferSave(
        val site: String,
        val username: String,
        val password: String,
        val reason: OfferReason,
        val strength: PasswordStrength,
    ) : LoginAdvance

    /** ذخیره‌ی بی‌صدا (کاربر «ذخیرهٔ خودکار» را روشن کرده و تصمیم بی‌ابهام است). */
    data class AutoSave(
        val site: String,
        val username: String,
        val password: String,
        val reason: AutoSaveReason,
        val strength: PasswordStrength,
    ) : LoginAdvance

    /** فقط یک تذکر/اطلاع کوتاه (مثلاً گذرواژهٔ تازه ضعیف‌تر است). */
    data class Notify(val notice: LoginNotice) : LoginAdvance
}

/** پیام‌های کوتاهِ وضعیت؛ ترجمه در لایهٔ رابط انجام می‌شود. */
sealed interface LoginNotice {
    data class WeakerPassword(val site: String, val strength: PasswordStrength) : LoginNotice

    data class CodeFound(val length: Int, val fromSms: Boolean) : LoginNotice

    data object CodeExpired : LoginNotice

    data object CodeNotMatched : LoginNotice

    data object SavedAndFilled : LoginNotice
}

/**
 * ماشین‌حالتِ یک تب.
 *
 * چرخهٔ عمرِ معمول:
 * `onPageProbe(login form)` → [LoginAdvance.FillCredentials] (اگر حسابِ ذخیره‌شده باشد)
 * `onFormSubmitted(...)` → [LoginAdvance.OfferSave] (یا AutoSave/Suppressed)
 * `onOtpScreenProbe()` → جست‌وجوی پیامک و [LoginAdvance.FillOneTimeCode]
 */
class LoginIntelligence(
    private var policy: LoginPolicy = LoginPolicy(),
    private var nowEpochMillis: Long = System.currentTimeMillis(),
) {
    var policyState: LoginPolicy
        get() = policy
        set(value) {
            policy = value
        }

    /** دامنه‌های شناخته‌شده (همهٔ حساب‌های مخزن) برای نگهبانِ بدل. */
    private var knownDomains: Set<String> = emptySet()

    /** «الآن نه»های کاربر برای هر دامنه؛ این وضعیت باید بیرون از این کلاس ماندگار شود. */
    private var dismissals: Map<String, Long> = emptyMap()

    /** کدهایی که قبلاً در همین صفحه نوشته شده‌اند تا دوباره نوشته نشوند. */
    private val filledCodes = mutableSetOf<String>()

    private var currentUrl: String = ""

    fun advanceClock(epochMillis: Long) {
        nowEpochMillis = epochMillis
    }

    fun updateKnownDomains(domains: Set<String>) {
        knownDomains = domains.map { it.lowercase().removePrefix("www.") }.toSet()
    }

    fun restoreDismissals(values: Map<String, Long>) {
        dismissals = values.toMap()
    }

    fun dismissalSnapshot(): Map<String, Long> = dismissals.toMap()

    /** تصمیمِ لحظهٔ بازشدنِ صفحه؛ سازگاریِ کامل با API پیشین. */
    fun onPageProbe(
        url: String,
        observation: LoginPageObservation,
        accounts: List<LoginAccount>,
        isPrivateTab: Boolean = false,
    ): LoginAdvance = run(
        LoginFlowRequest(
            step = LoginFlowStep.PAGE_LOADED,
            url = url,
            observation = observation,
            accounts = accounts,
            knownDomains = knownDomains,
            isPrivateTab = isPrivateTab,
        ),
    )

    /**
     * **تنها نقطهٔ ورودِ تصمیم‌ها** — هر رویداد (بارگذاریِ صفحه، ارسالِ فرم، رسیدنِ کد) با یک
     * [LoginFlowRequest] می‌آید و دقیقاً یک [LoginAdvance] برمی‌گرداند.
     *
     * چرا این یکپارچگی؟ چون تجربهٔ قبلی نشان داد وقتی سه لایه (اسکریپتِ صفحه، PasswordStudio و
     * ViewModel) هر کدام بخشی از تصمیم را می‌گرفتند، مسیرهایی مثلِ «پرکردنِ فیلدها» و «مهلتِ
     * کارتِ ذخیره» بی‌آنکه کسی متوجه شود می‌شکستند. حالا هر نقش (تب، صفحه و مخزن) فقط داده
     * می‌دهد و این‌جا — در یک تابعِ خالص — تصمیم گرفته می‌شود.
     */
    fun run(request: LoginFlowRequest): LoginAdvance {
        if (request.nowEpochMillis > 0) nowEpochMillis = request.nowEpochMillis
        if (request.knownDomains.isNotEmpty()) knownDomains = request.knownDomains.map { it.lowercase().removePrefix("www.") }.toSet()
        currentUrl = request.url.ifBlank { currentUrl }
        return when (request.step) {
            LoginFlowStep.PAGE_LOADED -> decideOnPageLoad(
                url = request.url,
                observation = request.observation,
                accounts = request.accounts,
                isPrivateTab = request.isPrivateTab,
            )

            LoginFlowStep.FORM_SUBMITTED -> decideOnSubmit(
                url = request.url,
                submitted = request.submitted ?: return LoginAdvance.Nothing,
                facts = request.facts,
                knownAccounts = request.knownAccounts,
                isPrivateTab = request.isPrivateTab,
            )

            LoginFlowStep.ONE_TIME_CODE_ARRIVED -> decideOnOneTimeCode(
                url = request.url,
                codeHit = request.codeHit,
                fieldPlan = request.fieldPlan,
                isPrivateTab = request.isPrivateTab,
            )
        }
    }

    private fun decideOnPageLoad(
        url: String,
        observation: LoginPageObservation,
        accounts: List<LoginAccount>,
        isPrivateTab: Boolean,
    ): LoginAdvance {
        if (isPrivateTab) return LoginAdvance.Nothing

        val verdict = if (policy.loginGuardEnabled) {
            LoginOriginGuard.check(url, knownDomains)
        } else {
            LoginGuardVerdict(OriginRisk.UNKNOWN, hostOf(url))
        }
        if (verdict.isWarning) return LoginAdvance.WarnOrigin(verdict)

        if (observation.passwordFieldCount <= 0) return LoginAdvance.Nothing
        if (!policy.autoFillEnabled || accounts.isEmpty()) return LoginAdvance.Nothing

        val plan = planFor(url, accounts)
        val candidate = plan.candidate ?: return LoginAdvance.Nothing
        val account = accounts.firstOrNull { it.id == candidate.id } ?: return LoginAdvance.Nothing
        val needsBiometric = policy.biometricLockRequired && !policy.biometricConfirmedInSession
        return LoginAdvance.FillCredentials(
            account = account,
            needsBiometric = needsBiometric,
            alternatives = plan.alternativesCount,
            script = null,
        )
    }

    private fun decideOnOneTimeCode(
        url: String,
        codeHit: OneTimeCodeHit?,
        fieldPlan: OtpFillPlan,
        isPrivateTab: Boolean,
    ): LoginAdvance {
        if (isPrivateTab) return LoginAdvance.Nothing
        if (!policy.otpAssistEnabled) return LoginAdvance.Nothing
        if (codeHit == null || !fieldPlan.isActionable) return LoginAdvance.Nothing
        val fingerprint = OneTimeCodeExtractor.fingerprint(codeHit.code)
        if (codeKey(url, fingerprint) in filledCodes) return LoginAdvance.Nothing
        return LoginAdvance.FillOneTimeCode(codeHit.code, fieldPlan)
    }

    /** A proposed fill is not a completed fill; mark it only after the page reports success. */
    fun markOneTimeCodeFilled(url: String, fingerprint: String) {
        filledCodes += codeKey(url, fingerprint)
    }

    private fun codeKey(url: String, fingerprint: String): String =
        "$fingerprint@${SecureOrigin.from(url) ?: hostOf(url)}"

    /** ارسالِ فرمِ ورود؛ سازگاریِ کامل با API پیشین. */
    fun onFormSubmitted(
        url: String,
        submitted: SubmittedLogin,
        facts: LoginFormFacts,
        knownAccounts: List<KnownAccount>,
        isPrivateTab: Boolean = false,
    ): LoginAdvance = decideOnSubmit(url, submitted, facts, knownAccounts, isPrivateTab)

    /** درونِ [decideOnSubmit]: منطقِ «آیا این ورود ارزشِ ذخیره دارد؟». */
    private fun decideOnSubmit(
        url: String,
        submitted: SubmittedLogin,
        facts: LoginFormFacts,
        knownAccounts: List<KnownAccount>,
        isPrivateTab: Boolean,
    ): LoginAdvance {
        if (isPrivateTab) return LoginAdvance.Nothing

        val site = CredentialCaptureHeuristics.domainOf(url)
        val otpOnly = facts.otpFieldCount > 0 && facts.passwordFieldCount == 0 &&
            submitted.password.length <= 8 && submitted.password.all(Char::isDigit)
        if (otpOnly) return LoginAdvance.Nothing

        if (submitted.password.isBlank()) {
            // ارسالِ فقط با کدِ پیامکی: نه ذخیره، شاید ارسالِ خودکار اگر کاربر اجازه داده باشد.
            return LoginAdvance.Nothing
        }

        val originWarning = policy.loginGuardEnabled && LoginOriginGuard.check(url, knownDomains).isWarning
        val dismissed = dismissals[site]

        val decision = CredentialSavePolicy.decide(
            username = submitted.username,
            password = submitted.password,
            isPrivateTab = isPrivateTab,
            saveEnabled = policy.saveMode != CredentialSaveMode.DISABLED,
            mode = policy.saveMode,
            knownAccounts = knownAccounts,
            dismissedAtEpochMillis = dismissed,
            nowEpochMillis = nowEpochMillis,
            originIsWarning = originWarning,
            hasOneTimeCodeOnly = otpOnly,
        )
        val strength = PasswordQualityChecker.evaluate(submitted.password, submitted.username, site)
        return when (decision) {
            is SaveOfferAction.Suppressed -> {
                val weaker = knownAccounts
                    .firstOrNull { CredentialSavePolicy.normalizeUsername(it.username) == CredentialSavePolicy.normalizeUsername(submitted.username) }
                    ?.takeIf { it.strengthScore > 0 && strength.score < it.strengthScore - WEAKER_MARGIN }
                    ?.let { LoginNotice.WeakerPassword(site, strength) }
                if (weaker != null) LoginAdvance.Notify(weaker) else LoginAdvance.Nothing
            }

            is SaveOfferAction.Offer -> LoginAdvance.OfferSave(
                site = site,
                username = submitted.username,
                password = submitted.password,
                reason = decision.reason,
                strength = strength,
            )

            is SaveOfferAction.AutoSave -> LoginAdvance.AutoSave(
                site = site,
                username = submitted.username,
                password = submitted.password,
                reason = decision.reason,
                strength = strength,
            )
        }
    }

    /** تصمیمِ صفحهٔ کدِ یکبارمصرف؛ سازگاریِ کامل با API پیشین. */
    fun onOtpScreen(
        url: String,
        codeHit: OneTimeCodeHit?,
        fieldPlan: OtpFillPlan,
        isPrivateTab: Boolean = false,
    ): LoginAdvance = decideOnOneTimeCode(url, codeHit, fieldPlan, isPrivateTab)

    /** کاربر ذخیره را پذیرفت (یا خودکار ذخیره شد). */
    fun markSaved(url: String) {
        dismissals = dismissals - CredentialCaptureHeuristics.domainOf(url)
    }

    /** کاربر گفت «الآن نه»؛ فقط برای مدتِ [CredentialSavePolicy.DISMISS_COOLDOWN_MILLIS]. */
    fun markDismissed(url: String) {
        dismissals = dismissals + (CredentialCaptureHeuristics.domainOf(url) to nowEpochMillis)
    }

    /** کاربر گفت «برای این سایت دیگر نپرس»؛ بی‌پایان و تا وقتی خودش ذخیره کند. */
    fun markNeverAskAgain(url: String) {
        dismissals = dismissals + (CredentialCaptureHeuristics.domainOf(url) to CredentialSavePolicy.NEVER_ASK_SENTINEL)
    }

    /** پس از بازکردنِ بیومتریک، تا پایانِ همین نشست دوباره پرسیده نمی‌شود. */
    fun markBiometricConfirmed() {
        policy = policy.copy(biometricConfirmedInSession = true)
    }

    fun hostOf(url: String): String = LoginOriginGuard.check(url, emptySet()).host

    fun resetForNewPage(url: String) {
        if (CredentialCaptureHeuristics.domainOf(url) != CredentialCaptureHeuristics.domainOf(currentUrl)) {
            filledCodes.clear()
        }
        currentUrl = url
    }

    /**
     * رتبه‌بندیِ حساب‌ها برای «کدام را پر کنیم؟».
     *
     * ترتیب: دامنهٔ دقیق → زیردامنه، سپس تازه‌ترین استفاده، سپس تازه‌ترین ویرایش، سپس عمرِ
     * بلندترِ نشست. «قوی‌ترین گذرواژه» عیارِ درستی نیست؛ کاربر انتظار دارد حسابی باز شود که
     * آخرین بار با آن وارد شده است.
     */
    fun planFor(url: String, accounts: List<LoginAccount>): AutofillPlan {
        val domain = CredentialCaptureHeuristics.domainOf(url)
        if (domain.isBlank() || accounts.isEmpty()) {
            return AutofillPlan(candidate = null, requiresBiometricUnlock = false)
        }
        val exact = accounts.filter { it.domain.equals(domain, ignoreCase = true) }
        val related = if (exact.isNotEmpty()) {
            exact
        } else {
            accounts.filter { account ->
                val other = account.domain.lowercase()
                other.isNotBlank() && (domain.endsWith(".$other") || other.endsWith(".$domain"))
            }
        }
        val ranked = related.sortedWith(
            compareByDescending<LoginAccount> { it.lastUsedAtEpochMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.updatedAtEpochMillis }
                .thenByDescending { it.sessionDays }
                .thenByDescending { it.strengthScore },
        )
        val best = ranked.firstOrNull() ?: return AutofillPlan(candidate = null, requiresBiometricUnlock = false)
        return AutofillPlan(
            candidate = best.asCandidate(),
            requiresBiometricUnlock = !policy.biometricConfirmedInSession,
            alternativesCount = (ranked.size - 1).coerceAtLeast(0),
        )
    }

    private companion object {
        const val WEAKER_MARGIN = 10
    }
}

/** گام‌های جریانِ ورود؛ هر گام یک نوع دادهٔ ورودیِ خودش را دارد. */
enum class LoginFlowStep {
    PAGE_LOADED,
    FORM_SUBMITTED,
    ONE_TIME_CODE_ARRIVED,
}

/**
 * یک درخواستِ تصمیم‌گیری.
 *
 * ساختارِ «همه‌چیز در یک شیء» عمدی است: لایهٔ موتور فقط یک تابع صدا می‌زند و لازم نیست بداند
 * کدام ترکیبِ پارامتر برای کدام گام معنا دارد؛ فیلدهای بی‌ربط خالی می‌مانند.
 */
data class LoginFlowRequest(
    val step: LoginFlowStep,
    val url: String,
    val observation: LoginPageObservation = LoginPageObservation.EMPTY,
    val accounts: List<LoginAccount> = emptyList(),
    val knownAccounts: List<KnownAccount> = emptyList(),
    val knownDomains: Set<String> = emptySet(),
    val submitted: SubmittedLogin? = null,
    val facts: LoginFormFacts = LoginFormFacts(),
    val codeHit: OneTimeCodeHit? = null,
    val fieldPlan: OtpFillPlan = OtpFillPlan.NoMatch("NONE"),
    val isPrivateTab: Boolean = false,
    val nowEpochMillis: Long = 0L,
)

/**
 * «تأییدِ ورودِ موفق» — چرا کاربر باید منتظر بماند؟
 *
 * ارسالِ فرم با موفقیت یکی نیست: گذرواژهٔ اشتباه هم فرم را ارسال می‌کند و همان لحظه پیامِ خطا
 * می‌آید. اگر بلافاصله کارتِ ذخیره را نشان دهیم، کاربر گذرواژهٔ غلط را ذخیره می‌کند (باگِ رایجِ
 * بسیاری از مرورگرها). پس تصمیمِ ذخیره فقط بعد از یکی از این شاهدها گرفته می‌شود:
 * ۱. ناوبری به آدرسی که دیگر فرمِ ورود ندارد.
 * ۲. ناپدیدشدنِ فرمِ ورود در همان صفحه (SPA) بدونِ پیامِ خطا.
 * ۳. رسیدنِ کاربر به صفحهٔ کدِ یکبارمصرف (یعنی گذرواژه پذیرفته شد).
 */
data class SubmissionVerdict(
    val accepted: Boolean,
    val reason: String,
) {
    companion object {
        val UNKNOWN = SubmissionVerdict(false, "UNKNOWN")
        val ACCEPTED_NAVIGATION = SubmissionVerdict(true, "NAVIGATION")
        val ACCEPTED_FORM_GONE = SubmissionVerdict(true, "FORM_GONE")
        val ACCEPTED_OTP_SCREEN = SubmissionVerdict(true, "OTP_SCREEN")
        val REJECTED_ERROR_SHOWN = SubmissionVerdict(false, "ERROR_SHOWN")
        val REJECTED_FORM_STILL_THERE = SubmissionVerdict(false, "FORM_STILL_THERE")
    }
}

/**
 * سنجشِ موفقیتِ ورود، مستقل از متنِ صفحه و زبانِ آن.
 *
 * فقط به «شکلِ» صفحه نگاه می‌کنیم (آیا گذرواژه‌فیلد هست؟ آیا پیامِ خطا دیده می‌شود؟) و به
 * نشانه‌های بومی برای چند زبانِ رایج. هیچ متنِ صفحه‌ای به بیرون فرستاده نمی‌شود.
 */
object LoginSubmissionVerifier {

    const val MIN_SETTLE_MILLIS = 900L
    const val SETTLE_TIMEOUT_MILLIS = 6_000L

    /**
     * @param passwordFieldsAfter تعدادِ گذرواژه‌فیلدِ دیده‌شده پس از ارسال
     * @param errorTextVisible آیا پیامِ خطا (هر زبانی) دیده می‌شود
     * @param otpFieldsAfter تعدادِ کادرِ کدِ یکبارمصرف پس از ارسال
     * @param elapsedMillis فاصله از لحظهٔ ارسال
     */
    fun verify(
        passwordFieldsAfter: Int,
        errorTextVisible: Boolean,
        otpFieldsAfter: Int,
        elapsedMillis: Long,
        navigatedToNewUrl: Boolean,
        originalUrl: String,
        currentUrl: String,
    ): SubmissionVerdict {
        if (errorTextVisible) return SubmissionVerdict.REJECTED_ERROR_SHOWN
        // «کادرِ کد آمده» پیش از سنجشِ مهلت بررسی می‌شود: این شاهدِ خودش کافی است و منتظر
        // ماندن فقط کارتِ ذخیره را بی‌دلیل عقب می‌اندازد.
        if (passwordFieldsAfter == 0 && otpFieldsAfter > 0) return SubmissionVerdict.ACCEPTED_OTP_SCREEN
        if (elapsedMillis < MIN_SETTLE_MILLIS) return SubmissionVerdict.UNKNOWN
        if (navigatedToNewUrl && currentUrl != originalUrl && passwordFieldsAfter == 0) {
            return SubmissionVerdict.ACCEPTED_NAVIGATION
        }
        if (passwordFieldsAfter == 0) return SubmissionVerdict.ACCEPTED_FORM_GONE
        if (elapsedMillis >= SETTLE_TIMEOUT_MILLIS) return SubmissionVerdict.REJECTED_FORM_STILL_THERE
        // هنوز فرم سرِ جایش است ولی مهلت تمام نشده: تصمیم به کاوشِ بعدی موکول می‌شود.
        // (`UNKNOWN` هرگز نباید به «موفق» یا «ناموفق» تبدیل شود؛ فقط یعنی «هنوز نرسیده».)
        return SubmissionVerdict.UNKNOWN
    }
}

/**
 * پیام‌های کوتاهِ ورود برای نمایش در رابط کاربری (بدونِ هیچ مقدارِ محرمانه).
 *
 * پیش‌تر این نوع در لایهٔ مخزن بود و فقط دو حالت داشت؛ حالا همهٔ اتفاق‌های مسیرِ ورود را پوشش
 * می‌دهد تا کاربر بداند دقیقاً چه شد (کد پیدا نشد؟ کادر پیدا نشد؟ ذخیره شد؟).
 */
sealed interface PasswordNotice {
    /** ورود ذخیره شد و (در صورتِ امکان) فرم پر شد. */
    data class SavedAndFilled(val filled: Boolean) : PasswordNotice

    data object SaveFailed : PasswordNotice

    /** سرور ورود را نپذیرفت؛ چیزی ذخیره نشد. */
    data object LoginRejected : PasswordNotice

    /** گذرواژهٔ تازه از ذخیره‌شده ضعیف‌تر است. */
    data class Weaker(
        val site: String,
        val strength: PasswordStrength,
        val existingLevel: PasswordStrengthLevel = PasswordStrengthLevel.FAIR,
    ) : PasswordNotice

    /** آدرس شبیه یک سایتِ آشناست ولی دامنهٔ دیگری دارد. */
    data class PhishingWarning(val host: String, val impersonated: String?) : PasswordNotice

    /** کدِ یکبارمصرف در کادر نوشته شد. */
    data class CodeFilled(val length: Int) : PasswordNotice

    /** کدی در پیامک/اعلان پیدا نشد. */
    data object CodeNotFound : PasswordNotice

    /** کد پیدا شد ولی کادرِ مناسبی در صفحه نبود. */
    data object CodeFieldNotFound : PasswordNotice

    /** فرم پر شد؛ `needsBiometric` یعنی پیش از پرکردن، اثر انگشت لازم بوده است. */
    data class Filled(val username: String, val needsBiometric: Boolean) : PasswordNotice

    /** برای این صفحه ورودِ ذخیره‌شده‌ای نداریم. */
    data object NoStoredLogin : PasswordNotice
}

/**
 * آیا این پیام فقط «خبر» است و نباید کارِ کاربر را قطع کند؟
 *
 * خبرهای کوتاه (کد نوشته شد، کدی پیدا نشد، فرم پر شد) در نوارِ بالای صفحه دیده می‌شوند؛
 * هشدارها (دامنهٔ بدل، گذرواژهٔ ضعیف‌تر، ذخیرهٔ ناموفق) باید تا تأییدِ کاربر بمانند.
 */
val PasswordNotice.isTransient: Boolean
    get() = when (this) {
        is PasswordNotice.CodeFilled,
        PasswordNotice.CodeNotFound,
        PasswordNotice.CodeFieldNotFound,
        is PasswordNotice.Filled,
        PasswordNotice.NoStoredLogin,
        is PasswordNotice.SavedAndFilled,
        -> true

        else -> false
    }
