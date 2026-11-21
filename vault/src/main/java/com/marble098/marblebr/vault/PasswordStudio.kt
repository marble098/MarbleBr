package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.CaptureAction
import com.marble098.marblebr.core.model.CapturePreferences
import com.marble098.marblebr.core.model.CredentialAutofillPlanner
import com.marble098.marblebr.core.model.CredentialCaptureCoordinator
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.LoginObservationProbe
import com.marble098.marblebr.core.model.LoginOriginGuard
import com.marble098.marblebr.core.model.LoginPageObservation
import com.marble098.marblebr.core.model.PasswordQualityChecker
import com.marble098.marblebr.core.model.PasswordStrength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * «استودیوی گذرواژه» — لایهٔ واسطِ میانِ صفحهٔ وب و مخزنِ رمزنگاری‌شده.
 *
 * این کلاس هیچ‌وقت خودش داده‌ای به شبکه نمی‌فرستد و فقط با سه ورودی کار می‌کند:
 * ۱. خروجیِ کاوشگرِ صفحه ([LoginObservationProbe])، ۲. تصمیمِ کاربر (ذخیره/رد)،
 * ۳. رکوردهایی که از مخزن می‌خواند.
 *
 * خروجی‌اش هم فقط برای همان تب است. گذرواژه فقط در حافظهٔ همین فرایند می‌ماند و بی‌درنگ به
 * مخزنِ AES-GCM می‌رود.
 */
@Singleton
class PasswordStudio @Inject constructor(
    private val credentialVault: CredentialVault,
) {
    private val mutex = Mutex()

    private val _pendingPrompt = MutableStateFlow<PasswordSavePrompt?>(null)
    val pendingPrompt: StateFlow<PasswordSavePrompt?> = _pendingPrompt.asStateFlow()

    /** آخرین تذکر/هشدارِ تازه برای نمایشِ کوتاه در مرورگر. */
    private val _notice = MutableStateFlow<PasswordNotice?>(null)
    val notice: StateFlow<PasswordNotice?> = _notice.asStateFlow()

    private var coordinator = CredentialCaptureCoordinator()
    private var lastTabId: String = ""
    private var lastPageUrl: String = ""
    private var lastObservation: LoginPageObservation = LoginPageObservation.EMPTY

    fun configure(preferences: CapturePreferences) {
        val snapshot = coordinator.snapshot()
        coordinator = CredentialCaptureCoordinator(preferences = preferences)
        coordinator.restore(snapshot)
    }

    /** تصمیمِ لحظهٔ بازشدنِ صفحه؛ اگر پرکردن لازم باشد اسکریپت آماده برمی‌گردد. */
    suspend fun onPageObserved(
        tabId: String,
        pageUrl: String,
        observationRaw: String?,
        knownDomains: Set<String> = emptySet(),
    ): PasswordAutofillDirective? = mutex.withLock {
        val observation = LoginObservationProbe.parse(observationRaw)
        lastTabId = tabId
        lastPageUrl = pageUrl
        lastObservation = observation
        val candidates = credentialVault.autofillCandidates()
        val domains = knownDomains + candidates.map { it.domain }
        when (val action = coordinator.onPageObserved(pageUrl, observation, candidates, domains)) {
            is CaptureAction.Autofill -> {
                // گذرواژه فقط وقتی از مخزن خوانده می‌شود که خودِ صفحه/کاربر درخواست کرده باشد؛
                // در این مرحله چیزی تزریق نمی‌کنیم و اسکریپت را با مقدارِ خالی می‌سازیم تا
                // کاربر با یک لمس، پرکردن را تأیید کند.
                PasswordAutofillDirective(
                    site = action.site,
                    username = action.username,
                    hasStoredPassword = true,
                )
            }

            is CaptureAction.PhishingWarning -> {
                _notice.value = PasswordNotice.PhishingWarning(action.verdict.host, action.verdict.impersonated)
                null
            }

            else -> null
        }
    }

    /** ساختِ اسکریپتِ پرکردن پس از تأییدِ کاربر (یا با بیومتریک، یا با لمسِ کارت). */
    suspend fun buildAutofillScript(tabId: String, pageUrl: String): String? = mutex.withLock {
        if (tabId != lastTabId || pageUrl != lastPageUrl) return@withLock null
        val candidates = credentialVault.autofillCandidates()
        val plan = CredentialAutofillPlanner.plan(pageUrl, candidates, biometricLockRequired = false)
        val candidate = plan.candidate ?: return@withLock null
        val secret = credentialVault.credentialsForAutofill(candidate.id) ?: return@withLock null
        val verdict = LoginOriginGuard.check(pageUrl, candidates.map { it.domain }.toSet())
        if (verdict.isWarning) {
            _notice.value = PasswordNotice.PhishingWarning(verdict.host, verdict.impersonated)
            return@withLock null
        }
        LoginObservationProbe.autofillScript(secret.username, secret.password)
    }

    /**
     * تصمیمِ پس از ارسالِ فرم.
     *
     * دو ورودیِ ممکن دارد: مقدارهایی که content script در همان لحظه خوانده است (مسیرِ اصلی) یا
     * پاسخِ خامِ [LoginObservationProbe.CAPTURE_SUBMIT_SCRIPT] برای سازگاری.
     */
    suspend fun onSubmitObserved(
        tabId: String,
        pageUrl: String,
        submitted: LoginObservationProbe.SubmittedCredentials?,
        observation: LoginPageObservation = lastObservation,
    ): CaptureAction = mutex.withLock {
        if (submitted == null || submitted.password.isEmpty()) {
            return@withLock CaptureAction.None("NO_CREDENTIALS")
        }
        val existing = credentialVault.autofillCandidates()
            .firstOrNull { candidate ->
                candidate.domain.equals(CredentialCaptureHeuristics.domainOf(pageUrl), ignoreCase = true) &&
                    candidate.username.equals(submitted.username, ignoreCase = true)
            }
        val action = coordinator.onSubmitObserved(pageUrl, submitted, observation, existing)
        when (action) {
            is CaptureAction.AskToSave -> {
                lastTabId = tabId
                lastPageUrl = pageUrl
                _pendingPrompt.value = PasswordSavePrompt(
                    tabId = tabId,
                    site = action.site,
                    username = action.username,
                    password = action.password,
                    strength = action.strength,
                )
            }

            is CaptureAction.WarnWeakerPassword -> {
                _notice.value = PasswordNotice.WeakerPassword(action.site, action.strength)
            }

            else -> Unit
        }
        action
    }

    /** همان تصمیم، اما با پاسخِ خامِ اسکریپتِ صفحه (مسیرِ سازگاری/تست). */
    suspend fun onSubmitObserved(tabId: String, pageUrl: String, submittedRaw: String?): CaptureAction =
        onSubmitObserved(
            tabId = tabId,
            pageUrl = pageUrl,
            submitted = LoginObservationProbe.parseSubmitted(submittedRaw),
        )

    /** ذخیرهٔ ورودی که کاربر تأیید کرده است. */
    suspend fun acceptPendingPrompt(): Boolean = mutex.withLock {
        val prompt = _pendingPrompt.value ?: return@withLock false
        _pendingPrompt.value = null
        return@withLock runCatching {
            val existing = credentialVault.autofillCandidates().firstOrNull {
                it.domain.equals(prompt.site, ignoreCase = true) &&
                    it.username.equals(prompt.username, ignoreCase = true)
            }
            credentialVault.saveCaptured(
                site = "https://${prompt.site}/",
                username = prompt.username,
                password = prompt.password,
                id = existing?.id,
            )
            coordinator.onUserAccepted(lastPageUrl)
            true
        }.getOrDefault(false)
    }

    fun dismissPendingPrompt() {
        val prompt = _pendingPrompt.value ?: return
        coordinator.onUserDismissed(lastPageUrl)
        _pendingPrompt.value = null
        prompt.password.toCharArray().fill('\u0000')
    }

    fun consumeNotice() {
        _notice.value = null
    }

    /** وضعیتِ کیفیتِ گذرواژهٔ ورودی برای نمایشِ نوارِ قدرت (بدون ذخیره). */
    fun evaluate(password: String, username: String, pageUrl: String): PasswordStrength =
        PasswordQualityChecker.evaluate(password, username, CredentialCaptureHeuristics.domainOf(pageUrl))

    /**
     * کاندیدهای ذخیره‌شده به‌صورت جریانِ زنده (برای شمارندهٔ صفحهٔ خانه).
     * فقط فرادادهٔ غیرحساس؛ هیچ گذرواژه‌ای در این جریان نیست.
     */
    fun candidatesForHome(): kotlinx.coroutines.flow.Flow<List<AutofillCandidate>> =
        credentialVault.observeCredentials().map { result ->
            result.credentials.map { saved ->
                AutofillCandidate(
                    id = saved.id,
                    domain = CredentialCaptureHeuristics.domainOf(saved.fields.site),
                    username = saved.fields.username,
                    updatedAtEpochMillis = saved.updatedAtEpochMillis,
                    strengthScore = PasswordQualityChecker.evaluate(
                        saved.fields.password,
                        saved.fields.username,
                        CredentialCaptureHeuristics.domainOf(saved.fields.site),
                    ).score,
                )
            }
        }

    /** فهرست کاندیدهای پرکردن برای پنلِ دستی. */
    suspend fun candidatesFor(pageUrl: String): List<AutofillCandidate> = credentialVault.autofillCandidates()

    /** پرکردنِ دستی از پنل مخزن: پیدا کردنِ رکورد و ساختنِ اسکریپت. */
    suspend fun buildManualAutofillScript(pageUrl: String, credentialId: String): String? {
        val secret = credentialVault.credentialsForAutofill(credentialId) ?: return null
        val verdict = LoginOriginGuard.check(
            pageUrl,
            credentialVault.autofillCandidates().map { it.domain }.toSet(),
        )
        if (verdict.isWarning) {
            _notice.value = PasswordNotice.PhishingWarning(verdict.host, verdict.impersonated)
            return null
        }
        return LoginObservationProbe.autofillScript(secret.username, secret.password)
    }
}

data class PasswordAutofillDirective(
    val site: String,
    val username: String,
    val hasStoredPassword: Boolean,
)

data class PasswordSavePrompt(
    val tabId: String,
    val site: String,
    val username: String,
    val password: String,
    val strength: PasswordStrength,
)

sealed interface PasswordNotice {
    data class WeakerPassword(val site: String, val strength: PasswordStrength) : PasswordNotice
    data class PhishingWarning(val host: String, val impersonated: String?) : PasswordNotice
}

/** دادهٔ لازم برای ارسالِ گذرواژه به فیلدهای صفحه. فقط در حافظه. */
data class AutofillSecret(val username: String, val password: String)
