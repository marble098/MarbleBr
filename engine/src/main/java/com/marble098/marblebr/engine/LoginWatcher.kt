package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.CapturePreferences
import com.marble098.marblebr.core.model.FlatJson
import com.marble098.marblebr.core.model.LoginObservationProbe
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * رصدگرِ ورود در engine (پیش از این فهرست آمده بود اما ساخته نشده بود).
 *
 * مسئولیت: پس از هر بار کامل‌شدنِ صفحه، کاوشِ بی‌خطر را در همان صفحه اجرا کند و نتیجه را به
 * [SiteCompatibilityEngine] و [com.marble098.marblebr.vault.PasswordStudio] بدهد.
 */
@Singleton
class LoginWatcher @Inject constructor(
    private val siteEngine: SiteCompatibilityEngine,
    private val sessionManager: ContainerSessionManager,
    private val preferencesRepository: BrowserPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** نامِ صفحه‌ای که کاوش برایش انجام شده؛ برای جلوگیری از اجرای تکراری روی همان آدرس. */
    private val lastObserved = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun observePage(tabId: String, url: String) {
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return
        if (lastObserved[tabId] == url) return
        lastObserved[tabId] = url
        scope.launch {
            val preferences = runCatching { preferencesRepository.preferencesFlow.first() }.getOrNull()
            siteEngine.passwordStudio.configure(
                CapturePreferences(
                    autoSaveEnabled = preferences?.autoSaveCredentials ?: true,
                    autoFillEnabled = preferences?.autoFillCredentials ?: true,
                    biometricLockRequired = preferences?.biometricLockRequired ?: true,
                ),
            )
            sessionManager.evaluateInPageForLogin(tabId, LoginObservationProbe.OBSERVE_SCRIPT) { raw ->
                siteEngine.onPageLoaded(tabId, url, raw)
                siteEngine.passwordStudio.onPageObserved(
                    tabId = tabId,
                    pageUrl = url,
                    observationRaw = raw,
                )
            }
        }
    }

    /**
     * ارسالِ فرم توسط خودِ صفحه (رویداد submit).
     *
     * مقادیر را خودِ content script در همان لحظه خوانده و به این‌جا می‌دهد؛ هیچ درخواستِ دومی به
     * صفحه فرستاده نمی‌شود (چون ناوبری شروع شده و فیلدها ممکن است دیگر نباشند). سپس تصمیمِ
     * «ذخیره کنم؟» با همان قواعدِ [com.marble098.marblebr.core.model.CredentialCaptureCoordinator]
     * گرفته می‌شود و کاربر باید صریحاً تأیید کند.
     */
    fun captureSubmitted(
        tabId: String,
        url: String,
        username: String,
        password: String,
        otpFieldCount: Int = 0,
    ) {
        if (password.isEmpty()) return
        scope.launch {
            runCatching {
                siteEngine.passwordStudio.onSubmitObserved(
                    tabId = tabId,
                    pageUrl = url,
                    submitted = LoginObservationProbe.SubmittedCredentials(
                        username = username,
                        password = password,
                        otpFieldCount = otpFieldCount,
                    ),
                    observation = siteEngine.stateOf(tabId).observation,
                )
            }
        }
    }

    /** اجرای یک اسکریپتِ آماده در صفحهٔ همان تب (پرکردنِ دستی از پنل مخزن). */
    fun runInPage(tabId: String, script: String, onResult: (String?) -> Unit = {}) {
        sessionManager.evaluateInPageForLogin(tabId, script, onResult)
    }

    /** پارسِ امنِ پاسخِ content script برای آزمون‌های واحد. */
    fun parseObservation(raw: String?): Map<String, Any?> {
        val flat = FlatJson.parse(raw) ?: return emptyMap()
        return mapOf(
            "passwordFieldCount" to flat.int("passwordFieldCount"),
            "oneTimeCodeFieldCount" to flat.int("oneTimeCodeFieldCount"),
            "browserRefusalText" to flat.bool("browserRefusalText"),
        )
    }

    /**
     * خواندنِ فرمِ ورود در همین لحظه (رویدادِ کاربر مثل «بررسی این صفحه»).
     * فقط مقادیرِ فیلدها خوانده می‌شود تا کارتِ ذخیره ساخته شود؛ چیزی ذخیره نمی‌شود.
     */
    fun captureCurrentForm(tabId: String, url: String) {
        sessionManager.evaluateInPageForLogin(tabId, LoginObservationProbe.CAPTURE_SUBMIT_SCRIPT) { raw ->
            val submitted = LoginObservationProbe.parseSubmitted(raw) ?: return@runInPage
            captureSubmitted(
                tabId = tabId,
                url = url,
                username = submitted.username,
                password = submitted.password,
                otpFieldCount = submitted.otpFieldCount,
            )
        }
    }

    fun forget(tabId: String) {
        lastObserved.remove(tabId)
        siteEngine.forgetTab(tabId)
    }

}
