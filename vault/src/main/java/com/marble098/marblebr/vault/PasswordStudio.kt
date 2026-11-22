package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.CredentialSavePolicy
import com.marble098.marblebr.core.model.KnownAccount
import com.marble098.marblebr.core.model.LoginAccount
import com.marble098.marblebr.core.model.LoginAdvance
import com.marble098.marblebr.core.model.LoginFlowRequest
import com.marble098.marblebr.core.model.LoginFlowStep
import com.marble098.marblebr.core.model.LoginFormFacts
import com.marble098.marblebr.core.model.LoginIntelligence
import com.marble098.marblebr.core.model.LoginNotice
import com.marble098.marblebr.core.model.LoginPolicy
import com.marble098.marblebr.core.model.LoginSubmissionVerifier
import com.marble098.marblebr.core.model.SubmittedLogin
import com.marble098.marblebr.core.model.LoginOriginGuard
import com.marble098.marblebr.core.model.OneTimeCodeHit
import com.marble098.marblebr.core.model.OneTimeCodeSource
import com.marble098.marblebr.core.model.OtpFieldDescriptor
import com.marble098.marblebr.core.model.OtpFieldMatcher
import com.marble098.marblebr.core.model.OtpFieldProbe
import com.marble098.marblebr.core.model.OtpFillPlan
import com.marble098.marblebr.core.model.PasswordQualityChecker
import com.marble098.marblebr.core.model.PasswordStrength
import com.marble098.marblebr.core.model.SubmissionVerdict
import com.marble098.marblebr.core.model.LoginObservationProbe
import com.marble098.marblebr.core.model.PasswordNotice
import com.marble098.marblebr.core.model.PasswordStrengthLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * «استودیوی ورود» — لایهٔ واسطِ میانِ صفحهٔ وب، مخزنِ رمزنگاری‌شده و پیام‌های دستگاه.
 *
 * تاریخچهٔ بازنویسی (چرا این کلاس از نو نوشته شد):
 * - نسخهٔ قبلی بعد از رسیدنِ کدِ یکبارمصرف هیچ کاری نمی‌کرد (نه استخراج، نه نوشتن در کادر).
 * - پرکردنِ خودکار فقط یک اعلان می‌ساخت و فیلدها را پر نمی‌کرد.
 * - کارتِ ذخیره در حافظهٔ یک `StateFlow` می‌ماند و با بستنِ اپ یا رفتن به صفحهٔ دیگر گم می‌شد.
 * - «الآن نه» در حافظه نگه داشته می‌شد و بعد از هر راه‌اندازی از یاد می‌رفت.
 *
 * معماریِ تازه: **یک** ماشین‌حالتِ خالص ([LoginIntelligence]) تصمیم می‌گیرد، این کلاس داده‌های
 * واقعیِ مخزن و دستگاه را آماده می‌کند و اسکریپت‌های اجرا را می‌سازد. تصمیم‌ها آزمون‌پذیر و
 * مسیرها تک‌تک قابلِ ردیابی‌اند.
 */
@Singleton
class PasswordStudio @Inject constructor(
    private val credentialVault: CredentialVault,
    private val settings: LoginSettings,
    private val smsReader: SmsOneTimeCodeReader,
    private val notificationBridge: NotificationCodeBridge,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val intelligence = LoginIntelligence()

    private val _pendingPrompt = MutableStateFlow<PasswordSavePrompt?>(null)

    /** کارتِ «این ورود را ذخیره کنم؟»؛ تا وقتی کاربر تصمیم نگیرد معتبر می‌ماند. */
    val pendingPrompt: StateFlow<PasswordSavePrompt?> = _pendingPrompt.asStateFlow()

    private val _notice = MutableStateFlow<PasswordNotice?>(null)

    /** آخرین تذکر/اطلاعِ کوتاه. */
    val notice: StateFlow<PasswordNotice?> = _notice.asStateFlow()

    private val _lastActivity = MutableStateFlow<LoginActivity?>(null)

    /** وضعیتِ زندهٔ ورود برای نمایش در نوارِ مرورگر (بدونِ هیچ مقدارِ محرمانه). */
    val activity: StateFlow<LoginActivity?> = _lastActivity.asStateFlow()

    /** حسابی که صفحهٔ فعلی به آن رسیده؛ مقصدِ کدِ یکبارمصرف. */
    private var activeAccountId: String? = null
    private var activeTabUrl: String = ""

    init {
        // «الآن نه»های ماندگار را یک‌بار در شروع کار بازیابی می‌کنیم.
        scope.launch {
            runCatching { settings.dismissalsFlow().collect { intelligence.restoreDismissals(it) } }
        }
        scope.launch {
            runCatching {
                settings.policyFlow().collect { policy -> intelligence.policyState = policy }
            }
        }
        // کدهای رسیده از اعلان‌ها؛ فقط در حافظهٔ همین سرویس.
        scope.launch {
            notificationBridge.lastCode.collect { hit ->
                if (hit != null) _lastActivity.value = LoginActivity.OneTimeCodeAvailable(hit.length)
            }
        }
    }

    /* ----------------------------------------- ورود به صفحه ----------------------------------------- */

    /**
     * پس از هر بار کامل‌شدنِ صفحه فراخوانی می‌شود.
     *
     * تصمیم بر اساسِ ماشین‌حالت گرفته می‌شود و خروجیِ آن در قالبِ [LoginPageDirective] به لایهٔ
     * مرورگر برمی‌گردد تا همان‌جا در صفحه اجرا شود:
     * - [LoginPageDirective.FillCredentials] → اسکریپتِ پرکردنِ نام کاربری/گذرواژه
     * - [LoginPageDirective.OriginWarning] → دامنهٔ بدل؛ هیچ‌چیز تایپ نشود
     * - [LoginPageDirective.None] → کاری لازم نیست (کدِ یکبارمصرف مسیرِ جدا دارد)
     */
    suspend fun onPageReady(
        tabId: String,
        pageUrl: String,
        observationRaw: String?,
        isPrivateTab: Boolean = false,
    ): LoginPageDirective = mutex.withLock {
        activeTabUrl = pageUrl
        val observation = LoginObservationProbe.parse(observationRaw)
        val accounts = loginAccounts()
        val knownDomains = (accounts.map { it.domain } + CredentialCaptureHeuristics.domainOf(pageUrl)).toSet()

        val advance = intelligence.run(
            LoginFlowRequest(
                step = LoginFlowStep.PAGE_LOADED,
                url = pageUrl,
                observation = observation,
                accounts = accounts,
                knownDomains = knownDomains,
                isPrivateTab = isPrivateTab,
                nowEpochMillis = System.currentTimeMillis(),
            ),
        )
        when (advance) {
            is LoginAdvance.WarnOrigin -> {
                _notice.value = PasswordNotice.PhishingWarning(advance.verdict.host, advance.verdict.impersonated)
                LoginPageDirective.OriginWarning(advance.verdict)
            }

            is LoginAdvance.FillCredentials -> {
                activeAccountId = advance.account.id
                val script = buildFillScript(advance.account.id)
                if (script == null) {
                    LoginPageDirective.None
                } else {
                    // ثبتِ «آخرین استفاده» تا بعداً همین حساب اولویت بگیرد.
                    runCatching { credentialVault.touchCredential(advance.account.id, autofilledAtEpochMillis = System.currentTimeMillis()) }
                    _lastActivity.value = LoginActivity.Filled(advance.account.username, advance.needsBiometric)
                    LoginPageDirective.FillCredentials(
                        script = script,
                        username = advance.account.username,
                        needsBiometricConfirmation = advance.needsBiometric,
                        alternatives = advance.alternatives,
                    )
                }
            }

            else -> {
                activeAccountId = null
                // شاید این صفحه یک صفحهٔ کدِ تأیید است (مثلاً بعد از ورودِ قبلی): کد آماده است؟
                offerOneTimeCodeIfPossible(pageUrl, isPrivateTab)
                LoginPageDirective.None
            }
        }
    }

    /**
     * «شاید این صفحه منتظرِ کد است» — بدونِ کاوشِ کادرها فقط کد را آماده می‌کند.
     *
     * چرا این‌جا نوشتن انجام نمی‌شود؟ چون نوشتن به فهرستِ کادرهای همان صفحه نیاز دارد و آن را
     * فقط [LoginWatcher] از دلِ صفحه می‌گیرد. این تابع فقط وضعیتِ «کد در دسترس است» را اعلام
     * می‌کند تا نوارِ مرورگر دکمهٔ «کد را بگذار» را نشان دهد؛ خودِ نوشتن با
     * [requestOneTimeCode] انجام می‌شود. این جداسازی جلوی نوشتنِ کد در تبِ اشتباه را می‌گیرد.
     */
    private suspend fun offerOneTimeCodeIfPossible(pageUrl: String, isPrivateTab: Boolean) {
        if (isPrivateTab) return
        if (!intelligence.policyState.otpAssistEnabled) return
        val hit = runCatching { findOneTimeCode(pageUrl) }.getOrNull() ?: return
        _lastActivity.value = LoginActivity.OneTimeCodeAvailable(hit.length)
    }

    /**
     * صفحه‌ای که احتمالاً کدِ یکبارمصرف می‌خواهد. اگر کدی در پیامک/اعلان باشد، دستورِ نوشتن
     * برمی‌گردد. این تابع را لایهٔ مرورگر هم می‌تواند صریحاً صدا بزند (دکمهٔ «کد را بگذار»).
     */
    suspend fun requestOneTimeCode(
        pageUrl: String,
        observationRaw: String?,
        fieldsRaw: String?,
        isPrivateTab: Boolean = false,
    ): LoginOneTimeCodeDirective = mutex.withLock {
        if (isPrivateTab) return@withLock LoginOneTimeCodeDirective.Unavailable("PRIVATE_TAB")
        val fields = OtpFieldDescriptor.parseAll(fieldsRaw)
        val hit = findOneTimeCode(pageUrl)
            ?: return@withLock LoginOneTimeCodeDirective.Unavailable("NO_CODE")
        val plan = OtpFieldMatcher.plan(fields, hit.code)
        val advance = intelligence.run(
            LoginFlowRequest(
                step = LoginFlowStep.ONE_TIME_CODE_ARRIVED,
                url = pageUrl,
                codeHit = hit,
                fieldPlan = plan,
                isPrivateTab = isPrivateTab,
                nowEpochMillis = System.currentTimeMillis(),
            ),
        )
        if (advance !is LoginAdvance.FillOneTimeCode) {
            return@withLock LoginOneTimeCodeDirective.Unavailable(
                when {
                    plan is OtpFillPlan.NoMatch -> "FIELD_NOT_FOUND"
                    plan is OtpFillPlan.AlreadyFilled -> "ALREADY_FILLED"
                    else -> "ALREADY_USED"
                },
            )
        }
        activeAccountId?.let { id ->
            runCatching { credentialVault.touchCredential(id, oneTimeCodeAtEpochMillis = System.currentTimeMillis()) }
        }
        scoreForCode(hit)
        LoginOneTimeCodeDirective.Ready(
            script = OtpFieldProbe.fillScript(hit.code, plan),
            digits = hit.code.filter(Char::isDigit).length,
            fromSms = hit.source == OneTimeCodeSource.SMS,
        )
    }

    /** آیا صفحهٔ فعلی اصلاً کادرِ کد دارد؟ (برای اینکه هر صفحه‌ای کد نخواهد) */
    suspend fun hasCodeField(fieldsRaw: String?, pageUrl: String): Boolean {
        val fields = OtpFieldDescriptor.parseAll(fieldsRaw)
        if (fields.isEmpty()) return false
        // کدِ فرضی ۶ رقمی رایج‌ترین حالت است؛ فقط برای سنجشِ وجودِ کادر.
        val plan = OtpFieldMatcher.plan(fields, "123456")
        return plan.isActionable
    }

    /**
     * کدِ تازه‌ای رسید (از پیامک یا اعلان).
     *
     * این‌جا هیچ نوشتنی در صفحه انجام نمی‌شود؛ فقط «کد در دسترس است» اعلام می‌شود. نوشتن، کارِ
     * `LoginWatcher` است چون فقط او می‌داند کدام تب کادرِ کد دارد. همین جداسازی باعث می‌شود کد
     * هرگز در تبِ اشتباه نوشته نشود.
     */
    fun onCodeArrived(hit: OneTimeCodeHit) {
        _lastActivity.value = LoginActivity.OneTimeCodeAvailable(hit.length)
    }

    /** آخرین کدِ رسیده از اعلان را مصرف می‌کند (تا دوباره استفاده نشود). */
    fun consumeNotificationCode() = notificationBridge.consume()

    /* ---------------------------------------- پیشنهادِ ذخیره ---------------------------------------- */

    /**
     * ثبتِ لحظهٔ ارسالِ فرم. **همین‌جا هیچ کارت و هیچ نوشتنی انجام نمی‌شود**؛ فقط «ارسال دیده شد»
     * ثبت می‌شود تا پس از تأییدِ موفقیت (ناوبری یا ناپدیدشدنِ فرم) کارت ساخته شود.
     *
     * چرا؟ چون گذرواژهٔ اشتباه هم فرم را ارسال می‌کند؛ ذخیرهٔ فوریِ آن، یک رکوردِ خراب می‌سازد.
     */
    suspend fun onLoginSubmit(
        pageUrl: String,
        username: String,
        password: String,
        otpFieldCount: Int = 0,
        isPrivateTab: Boolean = false,
    ) {
        if (isPrivateTab || password.isBlank()) return
        pendingSubmission = PendingSubmission(
            url = pageUrl,
            username = username,
            password = password,
            otpFieldCount = otpFieldCount,
            passwordFieldCount = 1,
            submittedAtEpochMillis = System.currentTimeMillis(),
        )
        _lastActivity.value = LoginActivity.Submitted
        scope.launch {
            val pending = pendingSubmission ?: return@launch
            // دو نوبتِ داوری: بارِ اول فقط «شاید حالا تأیید شود»، بارِ دوم حقِ رد کردن دارد.
            //
            // چرا دو نوبت؟ سایت‌های کند گاهی چند ثانیه بعد از ارسال فرم را عوض می‌کنند و ردکردنِ
            // زودهنگام یک ورودِ درست را از دست می‌داد. برعکسش هم بد بود: نسخهٔ قبلی تصمیمِ رد را
            // فقط در «دقیقاً ۶ ثانیه» می‌گرفت و عملاً هیچ‌وقت اتفاق نمی‌افتاد.
            repeat(2) { round ->
                delay(LoginSubmissionVerifier.SETTLE_TIMEOUT_MILLIS)
                if (pendingSubmission?.submittedAtEpochMillis != pending.submittedAtEpochMillis) return@launch
                settlePendingSubmission(
                    movedToNewUrl = false,
                    passwordFieldsAfter = null,
                    allowRejection = round == 1,
                )
            }
        }
    }

    private var pendingSubmission: PendingSubmission? = null

    /**
     * تأیید/ردِ ارسالِ قبلی.
     *
     * @param movedToNewUrl آیا پیمایش به آدرسِ تازه رخ داد
     * @param passwordFieldsAfter تعدادِ گذرواژه‌فیلدِ صفحهٔ تازه؛ `null` یعنی نمی‌دانیم
     * @param errorVisible آیا پیامِ خطا دیده شد
     * @param otpFieldsAfter آیا صفحهٔ تازه کادرِ کدِ یکبارمصرف دارد
     * @param allowRejection آیا «فرم هنوز سرِ جایش است» می‌تواند به ردکردن تبدیل شود؟ در نوبتِ
     *   نخستِ داوری `false` است تا سایتِ کند فرصت داشته باشد؛ در نوبتِ آخر `true` تا ارسالِ
     *   ناموفق هرگز «در انتظار» نماند.
     */
    suspend fun settlePendingSubmission(
        movedToNewUrl: Boolean,
        passwordFieldsAfter: Int?,
        errorVisible: Boolean = false,
        otpFieldsAfter: Int = 0,
        allowRejection: Boolean = true,
    ): LoginAdvance? = mutex.withLock {
        val pending = pendingSubmission ?: return@withLock null
        val elapsed = System.currentTimeMillis() - pending.submittedAtEpochMillis
        val verdict = LoginSubmissionVerifier.verify(
            passwordFieldsAfter = passwordFieldsAfter
                ?: if (movedToNewUrl) 0 else pending.passwordFieldCount,
            errorTextVisible = errorVisible,
            otpFieldsAfter = otpFieldsAfter,
            elapsedMillis = elapsed,
            navigatedToNewUrl = movedToNewUrl,
            originalUrl = pending.url,
            currentUrl = activeTabUrl,
        )
        when (verdict) {
            SubmissionVerdict.ACCEPTED_FORM_GONE,
            SubmissionVerdict.ACCEPTED_NAVIGATION,
            SubmissionVerdict.ACCEPTED_OTP_SCREEN,
            -> {
                pendingSubmission = null
                handleAcceptedSubmission(pending)
            }

            SubmissionVerdict.REJECTED_ERROR_SHOWN,
            SubmissionVerdict.REJECTED_FORM_STILL_THERE,
            -> {
                if (!allowRejection) {
                    // هنوز رد نمی‌کنیم؛ ارسال دست‌نخورده می‌ماند تا نوبتِ بعدی یا یک شاهدِ تازه.
                    null
                } else {
                    pendingSubmission = null
                    _notice.value = PasswordNotice.LoginRejected
                    null
                }
            }

            // «هنوز چیزی نشده» و هر داوریِ تازه‌ای که بعداً اضافه شود.
            else -> null
        }
    }

    private suspend fun handleAcceptedSubmission(pending: PendingSubmission): LoginAdvance {
        val accounts = knownAccountsFor(pending.url)
        val advance = intelligence.run(
            LoginFlowRequest(
                step = LoginFlowStep.FORM_SUBMITTED,
                url = pending.url,
                submitted = SubmittedLogin(
                    username = pending.username,
                    password = pending.password,
                ),
                facts = LoginFormFacts(passwordFieldCount = 1, otpFieldCount = pending.otpFieldCount),
                knownAccounts = accounts,
                nowEpochMillis = System.currentTimeMillis(),
            ),
        )
        when (advance) {
            is LoginAdvance.OfferSave -> {
                _pendingPrompt.value = PasswordSavePrompt(
                    site = advance.site,
                    url = pending.url,
                    username = advance.username,
                    password = advance.password,
                    reason = advance.reason,
                    strength = advance.strength,
                    alternatives = accounts.map { it.username },
                )
                _lastActivity.value = LoginActivity.PromptReady
            }

            is LoginAdvance.AutoSave -> {
                val saved = runCatching {
                    saveCaptured(pending.url, advance.username, advance.password)
                }.isSuccess
                // فقط وقتی ذخیره واقعاً موفق بود «ذخیره شد» علامت بزن؛ وگرنه کارتِ بعدی بی‌دلیل
                // فشرده می‌شد و کاربر فکر می‌کرد ورودش ذخیره شده است.
                if (saved) intelligence.markSaved(pending.url)
                _notice.value = if (saved) {
                    PasswordNotice.SavedAndFilled(filled = false)
                } else {
                    PasswordNotice.SaveFailed
                }
            }

            is LoginAdvance.Notify -> {
                _notice.value = when (val payload = advance.notice) {
                    is LoginNotice.WeakerPassword -> PasswordNotice.Weaker(
                        site = payload.site,
                        strength = payload.strength,
                        existingLevel = PasswordStrengthLevel.FAIR,
                    )
                    else -> PasswordNotice.LoginRejected
                }
            }

            else -> Unit
        }
        // گذرواژه فقط تا این لحظه در حافظه بود؛ دیگر لازم نیست.
        pending.password.toCharArray().fill('\u0000')
        return advance
    }

    /** کاربر پذیرفت؛ ذخیره، پاک‌کردنِ تردید و اطلاع‌رسانی. */
    suspend fun acceptPendingPrompt(editedUsername: String? = null): PasswordSaveResult = mutex.withLock {
        val prompt = _pendingPrompt.value ?: return@withLock PasswordSaveResult.NothingToSave
        _pendingPrompt.value = null
        val username = editedUsername?.takeIf { it.isNotBlank() } ?: prompt.username
        val result = runCatching {
            val existing = credentialVault.loginAccountIndex().firstOrNull {
                it.domain.equals(prompt.site, ignoreCase = true) &&
                    CredentialSavePolicy.normalizeUsername(it.fields.username) ==
                    CredentialSavePolicy.normalizeUsername(username)
            }
            saveCaptured(prompt.url, username, prompt.password, existing?.id)
        }
        prompt.password.toCharArray().fill('\u0000')
        if (result.isSuccess) {
            intelligence.markSaved(prompt.url)
            settings.clearDismissal(prompt.site)
            activeAccountId = result.getOrNull()?.id
            _notice.value = PasswordNotice.SavedAndFilled(filled = false)
            PasswordSaveResult.Saved(result.getOrNull()?.id.orEmpty())
        } else {
            _notice.value = PasswordNotice.SaveFailed
            PasswordSaveResult.Failed
        }
    }

    /**
     * کاربر گفت «الآن نه»؛ تصمیم **ماندگار** می‌شود تا بعد از راه‌اندازیِ مجدد هم یادش بماند.
     *
     * این مسیر عمداً «موقت» است: فقط [CredentialSavePolicy.DISMISS_COOLDOWN_MILLIS] احترام
     * می‌گیرد. ردِ همیشگی مسیرِ جداگانه‌ای دارد ([neverAskForSiteAgain]) چون کاربر بینِ «حالا
     * نه» و «هرگز» فرق می‌گذارد و یکی‌کردنِ این دو، یا پرسشِ آزاردهنده می‌سازد یا سکوتِ ناخواسته.
     */
    suspend fun dismissPendingPrompt(): Boolean = discardPendingPrompt { prompt ->
        intelligence.markDismissed(prompt.url)
        runCatching { settings.rememberDismissal(prompt.site, System.currentTimeMillis()) }
    }

    /** «برای این سایت دیگر نپرس»: ردِ همیشگی؛ فقط با ذخیرهٔ صریحِ بعدی پاک می‌شود. */
    suspend fun neverAskForSiteAgain(): Boolean = discardPendingPrompt { prompt ->
        intelligence.markNeverAskAgain(prompt.url)
        runCatching { settings.rememberDismissal(prompt.site, CredentialSavePolicy.NEVER_ASK_SENTINEL) }
    }

    /** کارتِ ذخیره را برمی‌دارد و گذرواژهٔ نگه‌داشته‌شده را از حافظه پاک می‌کند. */
    private fun discardPendingPrompt(persist: (PasswordSavePrompt) -> Unit): Boolean {
        val prompt = _pendingPrompt.value ?: return false
        _pendingPrompt.value = null
        persist(prompt)
        prompt.password.toCharArray().fill('\u0000')
        return true
    }

    fun consumeNotice() {
        _notice.value = null
    }

    /* ------------------------------------------ پرکردنِ دستی ------------------------------------------ */

    /** فهرستِ حساب‌های همین سایت برای پنلِ دستی (بدونِ گذرواژه). */
    suspend fun candidatesFor(pageUrl: String): List<AutofillCandidate> {
        val accounts = loginAccounts()
        val plan = intelligence.planFor(pageUrl, accounts)
        val domain = CredentialCaptureHeuristics.domainOf(pageUrl)
        return accounts
            .filter {
                it.domain.equals(domain, true) || domain.endsWith(".${it.domain}") || it.domain.endsWith(".$domain")
            }
            .map { account ->
                account.asCandidate().copy(
                    lastUsedAtEpochMillis = if (plan.candidate?.id == account.id) {
                        System.currentTimeMillis()
                    } else {
                        account.lastUsedAtEpochMillis
                    },
                )
            }
    }

    /** پرکردنِ دستی از پنلِ مخزن؛ اسکریپتِ آماده یا `null`. */
    suspend fun buildManualAutofillScript(pageUrl: String, credentialId: String): String? {
        val verdict = LoginOriginGuard.check(
            pageUrl,
            credentialVault.loginAccountIndex().map { it.domain }.toSet(),
        )
        if (verdict.isWarning) {
            _notice.value = PasswordNotice.PhishingWarning(verdict.host, verdict.impersonated)
            return null
        }
        return buildFillScript(credentialId).also {
            if (it != null) {
                activeAccountId = credentialId
                runCatching { credentialVault.touchCredential(credentialId, autofilledAtEpochMillis = System.currentTimeMillis()) }
            }
        }
    }

    /**
     * اسکریپتِ پرکردنِ فرم؛ گذرواژه فقط در همین رشته و برای همین لحظه وجود دارد.
     * پیش از ساخت، تاریخچهٔ «حساب‌های این دامنه» دوباره سنجیده می‌شود.
     */
    private suspend fun buildFillScript(credentialId: String): String? {
        val secret = credentialVault.credentialsForAutofill(credentialId) ?: return null
        return LoginObservationProbe.autofillScript(secret.username, secret.password)
    }

    /* ------------------------------------- کدِ یکبارمصرف ------------------------------------- */

    /**
     * بهترین کدِ موجود برای این صفحه: اول اعلان‌ها (تازه‌ترین)، بعد پیامک.
     *
     * چرا اعلان اول؟ چون اغلب اپ‌های بانکی/پیام‌رسان اعلان را زودتر از پیامکِ ذخیره‌شده نشان
     * می‌دهند و کدِ اعلان معمولاً همان کدِ درخواستِ کاربر است.
     */
    suspend fun findOneTimeCode(pageUrl: String, lookBackMillis: Long = SmsOneTimeCodeReader.DEFAULT_LOOKBACK_MILLIS): OneTimeCodeHit? {
        val now = System.currentTimeMillis()
        val fromNotification = notificationBridge.lastCode.value?.takeIf { hit ->
            now - hit.receivedAtEpochMillis <= lookBackMillis
        }
        if (fromNotification != null) return fromNotification
        return smsReader.findRecentCode(now, lookBackMillis)
    }

    private fun scoreForCode(hit: OneTimeCodeHit) {
        _lastActivity.value = LoginActivity.OneTimeCodeFilled(
            length = hit.code.filter(Char::isDigit).length,
            fromSms = hit.source == OneTimeCodeSource.SMS,
        )
    }

    /* ----------------------------------------- دادهٔ مخزن ----------------------------------------- */

    /** حساب‌های قابلِ استفاده (فراوردِ کامل از مخزن) برای تصمیم‌های ماشین‌حالت. */
    private suspend fun loginAccounts(): List<LoginAccount> = credentialVault.loginAccountIndex().map { saved ->
        LoginAccount(
            id = saved.id,
            domain = saved.domain,
            username = saved.fields.username,
            strengthScore = PasswordQualityChecker.evaluate(saved.fields.password, saved.fields.username, saved.domain).score,
            updatedAtEpochMillis = saved.updatedAtEpochMillis,
            lastUsedAtEpochMillis = saved.fields.lastAutofillAtEpochMillis.takeIf { it > 0 },
        )
    }

    private suspend fun knownAccountsFor(pageUrl: String): List<KnownAccount> {
        val domain = CredentialCaptureHeuristics.domainOf(pageUrl)
        return credentialVault.loginAccountIndex()
            .filter { it.domain.equals(domain, true) }
            .map { saved ->
                KnownAccount(
                    username = saved.fields.username,
                    site = saved.fields.site,
                    updatedAtEpochMillis = saved.updatedAtEpochMillis,
                    strengthScore = PasswordQualityChecker.evaluate(saved.fields.password, saved.fields.username, saved.domain).score,
                    passwordFingerprint = runCatching {
                        credentialVault.fingerprintOf(saved.fields.password, saved.id)
                    }.getOrDefault(""),
                )
            }
    }

    private suspend fun saveCaptured(url: String, username: String, password: String, id: String? = null) =
        credentialVault.saveCaptured(
            site = CredentialVault.normalizeSite(url),
            username = username,
            password = password,
            id = id,
        )

    /* ---------------------------------------- برای رابط کاربری ---------------------------------------- */

    /** شمارندهٔ حساب‌های ذخیره‌شده برای کارتِ صفحهٔ خانه. */
    fun candidatesForHome(): Flow<List<AutofillCandidate>> =
        credentialVault.observeCredentials().map { result ->
            result.credentials.map { saved ->
                AutofillCandidate(
                    id = saved.id,
                    domain = saved.domain,
                    username = saved.fields.username,
                    updatedAtEpochMillis = saved.updatedAtEpochMillis,
                    lastUsedAtEpochMillis = saved.fields.lastAutofillAtEpochMillis.takeIf { it > 0 },
                    strengthScore = PasswordQualityChecker.evaluate(
                        saved.fields.password,
                        saved.fields.username,
                        saved.domain,
                    ).score,
                )
            }
        }

    /** فهرستِ حساب‌ها با گذرواژه برای صفحهٔ مخزن (فقط پس از قفل‌گشایی). */
    fun storedCredentials(): Flow<CredentialReadResult> = credentialVault.observeCredentials()

    /** کیفیتِ گذرواژهٔ ورودی برای فرمِ افزودنِ دستی. */
    fun evaluate(password: String, username: String, pageUrl: String): PasswordStrength =
        PasswordQualityChecker.evaluate(password, username, CredentialCaptureHeuristics.domainOf(pageUrl))

    /**
     * کاربر پذیرفت که با بیومتریک قفل باز شود؛ تا پایانِ همین نشست (process) دوباره پرسیده
     * نمی‌شود. این «پنجرهٔ اعتماد» عمداً در حافظه است و با خروج از اپ پاک می‌شود.
     */
    fun onBiometricConfirmed() {
        intelligence.markBiometricConfirmed()
        _lastActivity.value = null
    }

    /** آیا برای پرکردنِ این لحظه اجازهٔ بیومتریک لازم است؟ */
    fun requiresBiometricForFill(): Boolean =
        intelligence.policyState.biometricLockRequired && !intelligence.policyState.biometricConfirmedInSession

    /** ثبتِ اینکه کاربر بیومتریک را رد کرد تا پرکردن انجام نشود. */
    fun onBiometricDeclined() {
        _lastActivity.value = null
    }

    /** وضعیتِ «آیا اپ می‌تواند کد را بخواند؟» برای راهنمای کاربر. */
    fun oneTimeCodeCapability(policy: LoginPolicy): OneTimeCodeCapability = OneTimeCodeCapability(
        enabled = policy.otpAssistEnabled,
        smsPermissionGranted = smsReader.hasPermission(),
        notificationAccessGranted = notificationBridge.isEnabled(),
        // حتی بدونِ هیچ دسترسی‌ای، کاربر می‌تواند از راهنمای «کد را بگذار» استفاده کند.
        manualPasteAvailable = true,
    )

    /** برای آزمون و برای بازنشانیِ وضعیت پس از خروجِ کامل از سایت. */
    fun forgetTab() {
        activeAccountId = null
        _pendingPrompt.value = null
        pendingSubmission = null
    }

    private data class PendingSubmission(
        val url: String,
        val username: String,
        val password: String,
        val otpFieldCount: Int,
        /** تعدادِ گذرواژه‌فیلدی که در همان لحظهٔ ارسال دیده شد (از حقیقتِ صفحه، نه حدس). */
        val passwordFieldCount: Int,
        val submittedAtEpochMillis: Long,
    )
}

/** خروجیِ ورود به صفحه؛ تصمیمِ موتور دربارهٔ همان تب. */
sealed interface LoginPageDirective {
    data object None : LoginPageDirective

    data class OriginWarning(val verdict: com.marble098.marblebr.core.model.LoginGuardVerdict) : LoginPageDirective

    data class FillCredentials(
        val script: String,
        val username: String,
        val needsBiometricConfirmation: Boolean,
        val alternatives: Int,
    ) : LoginPageDirective
}

/** خروجیِ درخواستِ کدِ یکبارمصرف. */
sealed interface LoginOneTimeCodeDirective {
    data class Ready(val script: String, val digits: Int, val fromSms: Boolean) : LoginOneTimeCodeDirective

    data class Unavailable(val reason: String) : LoginOneTimeCodeDirective
}

/** کارتِ ذخیره؛ با دلیلِ روشن تا متنِ رابط کاربری بتواند دقیق توضیح دهد. */
data class PasswordSavePrompt(
    val site: String,
    val url: String,
    val username: String,
    val password: String,
    val reason: com.marble098.marblebr.core.model.OfferReason,
    val strength: PasswordStrength,
    /** نام‌های کاربریِ دیگر همین سایت، برای انتخابِ سریع. */
    val alternatives: List<String> = emptyList(),
) {
    /** متنِ قابلِ نمایشِ نام کاربری (برای کارت؛ ایمیلی/شماره‌ای کامل نمایش داده نمی‌شود). */
    val maskedUsername: String
        get() = CredentialCaptureHeuristics.maskUsername(username)

    /** نام کاربری پیشنهادی برای تکمیلِ خودکار (اگر خالی باشد، اولین گزینهٔ شناخته‌شده). */
    val suggestedUsername: String
        get() = username.ifBlank { alternatives.firstOrNull().orEmpty() }
}

/** نتیجهٔ ذخیرهٔ کارت. */
sealed interface PasswordSaveResult {
    data class Saved(val id: String) : PasswordSaveResult

    data object Failed : PasswordSaveResult

    data object NothingToSave : PasswordSaveResult
}

/** وضعیتِ زندهٔ ورود برای نمایش در نوار. */
sealed interface LoginActivity {
    data object Submitted : LoginActivity

    data object PromptReady : LoginActivity

    data class Filled(val username: String, val needsBiometric: Boolean) : LoginActivity

    data class OneTimeCodeAvailable(val length: Int) : LoginActivity

    data class OneTimeCodeFilled(val length: Int, val fromSms: Boolean) : LoginActivity
}

/** چه مسیرهایی برای خواندنِ کد در دسترس است؟ */
data class OneTimeCodeCapability(
    val enabled: Boolean,
    val smsPermissionGranted: Boolean,
    val notificationAccessGranted: Boolean,
    val manualPasteAvailable: Boolean,
) {
    /** آیا حداقل یک مسیرِ خودکار برای رساندنِ کد وجود دارد؟ */
    val hasAutomaticSource: Boolean get() = smsPermissionGranted || notificationAccessGranted
}
