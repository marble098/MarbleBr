package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.LoginObservationProbe
import com.marble098.marblebr.core.model.OtpFieldProbe
import com.marble098.marblebr.core.model.OneTimeCodeExtractor
import com.marble098.marblebr.core.model.OneTimeCodeHit
import com.marble098.marblebr.vault.LoginOneTimeCodeDirective
import com.marble098.marblebr.vault.LoginPageDirective
import com.marble098.marblebr.vault.PasswordStudio
import com.marble098.marblebr.vault.SmsCodeListener
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/** Connects login and OTP operations to the exact Gecko document that requested them. */
@Singleton
class LoginWatcher @Inject constructor(
    private val siteEngine: SiteCompatibilityEngine,
    private val sessionManager: ContainerSessionManager,
    private val passwordStudio: PasswordStudio,
    private val smsBridge: Provider<SmsBridge>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun currentPage(tabId: String, url: String): LoginPage? = sessionManager.currentLoginPage(tabId, url)

    fun isCurrent(page: LoginPage): Boolean = currentPage(page.tabId, page.url) == page

    suspend fun observePage(page: LoginPage): LoginPageDirective =
        withContext(Dispatchers.Main.immediate) {
            val raw = sessionManager.evalInPageForLogin(page, LoginObservationProbe.OBSERVE_SCRIPT)
                ?: return@withContext LoginPageDirective.None
            if (!isCurrent(page)) return@withContext LoginPageDirective.None

            siteEngine.onPageLoaded(page.tabId, page.url, raw)
            val observation = LoginObservationProbe.parse(raw)
            passwordStudio.settlePendingSubmission(
                tabId = page.tabId,
                currentUrl = page.url,
                passwordFieldsAfter = observation.passwordFieldCount,
                otpFieldsAfter = observation.oneTimeCodeFieldCount,
            )
            if (!isCurrent(page)) return@withContext LoginPageDirective.None
            val domain = ContainerSessionManager.extractDomain(page.url)
            val canAutoFill = page.containerId == AccountContainers.defaultId(domain)
            passwordStudio.onPageReady(page.tabId, page.url, raw, allowAutofill = canAutoFill)
        }

    suspend fun executeAutofill(page: LoginPage, script: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            sessionManager.evalInPageForLogin(page, script) == "filled"
        }

    /** کد را همین حالا از پیامک/اعلان پیدا و در کادر بنویس؛ برای دکمهٔ «کد را بگذار». */
    suspend fun fillOneTimeCodeNow(page: LoginPage): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val fieldsRaw = sessionManager.evalInPageForLogin(page, OtpFieldProbe.DISCOVER_SCRIPT) ?: return@withContext false
            if (!passwordStudio.hasCodeField(fieldsRaw, page.url)) return@withContext false
            val directive = passwordStudio.requestOneTimeCode(
                pageUrl = page.url,
                observationRaw = null,
                fieldsRaw = fieldsRaw,
            )
            if (directive !is LoginOneTimeCodeDirective.Ready) return@withContext false
            val result = sessionManager.evalInPageForLogin(page, directive.script)
            val filled = result == "filled" || result == "already"
            if (filled) {
                passwordStudio.consumeNotificationCode()
                smsBridge.get().notifyCodeFilled(directive.digits, directive.fromSms)
            }
            filled
        }

    /**
     * شنیدنِ پیامک‌های تازه؛ کدِ رسیده بی‌درنگ به استودیو می‌رود تا اگر صفحه منتظرِ کد باشد
     * نوشته شود. اگر دسترسی نباشد، `SmsCodeListener` خودش بی‌صدا هیچ کاری نمی‌کند.
     */
    fun startSmsListening(onCode: (OneTimeCodeHit) -> Unit): Boolean = smsBridge.get().start(onCode)

    fun stopSmsListening() {
        smsBridge.get().stop()
    }

    /** آیا این صفحه کادرِ کد دارد؟ (برای نمایشِ دکمه در نوار بالا) */
    suspend fun pageAcceptsOneTimeCode(tabId: String, url: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val page = currentPage(tabId, url) ?: return@withContext false
            val fieldsRaw = sessionManager.evalInPageForLogin(page, OtpFieldProbe.DISCOVER_SCRIPT) ?: return@withContext false
            passwordStudio.hasCodeField(fieldsRaw, url)
        }

    /**
     * ارسالِ فرم توسط خودِ صفحه؛ مقادیر همان لحظه خوانده شده‌اند (پیش از ناوبری).
     *
     * این‌جا هیچ ذخیره‌ای انجام نمی‌شود: فقط «ارسال دیده شد» ثبت می‌شود و پس از شاهدِ موفقیت
     * (پیمایش/ناپدیدشدنِ فرم) کارت ساخته می‌شود.
     */
    fun captureSubmitted(
        tabId: String,
        url: String,
        username: String,
        password: String,
        otpFieldCount: Int = 0,
        /** پیامِ خطا همان لحظهٔ ارسال دیده شد؟ اگر بله، ورود موفق نبوده است. */
        errorVisible: Boolean = false,
    ) {
        if (password.isBlank() || errorVisible) return
        scope.launch {
            runCatching {
                passwordStudio.onLoginSubmit(tabId, url, username, password, otpFieldCount)
            }
        }
    }

    /** خواندنِ همین‌حالا‌ی فرم و ثبتِ ارسالِ دستی (دکمهٔ «این ورود را ذخیره کن»). */
    fun captureCurrentForm(tabId: String, url: String) {
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                val page = currentPage(tabId, url) ?: return@withContext
                val raw = sessionManager.evalInPageForLogin(page, LoginObservationProbe.CAPTURE_SUBMIT_SCRIPT)
                if (!isCurrent(page)) return@withContext
                val submitted = LoginObservationProbe.parseSubmitted(raw) ?: return@withContext
                passwordStudio.onLoginSubmit(
                    tabId = tabId,
                    pageUrl = url,
                    username = submitted.username,
                    password = submitted.password,
                    otpFieldCount = submitted.otpFieldCount,
                    isPrivateTab = sessionManager.isPrivateSession(tabId),
                )
                // فرم دستی خوانده شد؛ اگر خطا دیده شد رد، وگرنه «فرم ناپدید شد» شاهدِ موفقیت است.
                passwordStudio.settlePendingSubmission(tabId, url, passwordFieldsAfter = 0)
            }
        }
    }

    /** پارسِ امنِ پاسخِ content script برای آزمون‌های واحد. */
    fun parseObservation(raw: String?): Map<String, Any?> {
        val observation = LoginObservationProbe.parse(raw)
        return mapOf(
            "passwordFieldCount" to observation.passwordFieldCount,
            "oneTimeCodeFieldCount" to observation.oneTimeCodeFieldCount,
            "browserRefusalText" to observation.browserRefusalText,
        )
    }

    fun forget(tabId: String) {
        siteEngine.forgetTab(tabId)
        scope.launch { passwordStudio.forgetTab(tabId) }
    }
}

/**
 * پلِ سبکِ پیامک برای لایهٔ مرورگر: راه‌اندازیِ شنوندهٔ پیامکِ تازه و اعلامِ «کد نوشته شد».
 *
 * چرا جدا؟ چون `LoginWatcher` به `Context` دسترسی ندارد و باید بدونِ وابستگی به Activity
 * آزمون‌پذیر بماند. این کلاس در لایهٔ `:engine` و با تزریقِ `ApplicationContext` ساخته می‌شود.
 */
@Singleton
class SmsBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var listener: SmsCodeListener? = null

    /** شروعِ شنیدن؛ اگر دسترسی نباشد بی‌صدا هیچ کاری نمی‌کند. */
    fun start(onCode: (OneTimeCodeHit) -> Unit): Boolean {
        if (listener != null) return true
        // فقط کدهای تازه: پیامکِ تکراریِ همان کد نباید دوباره به استودیو برود.
        val seen = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val active = SmsCodeListener { hit ->
            if (seen.add(OneTimeCodeExtractor.fingerprint(hit.code))) onCode(hit)
        }
        if (!active.start(context)) return false
        listener = active
        return true
    }

    fun stop() {
        listener?.let { runCatching { it.stop(context) } }
        listener = null
    }

    /** نوتیفیکیشنِ محلیِ «کد نوشته شد» — عمداً بی‌صدا و فقط برای اطمینانِ کاربر. */
    fun notifyCodeFilled(length: Int, fromSms: Boolean) {
        // فقط وقتی اپ اجازهٔ اعلان دارد؛ وگرنه `runCatching` کافی است و خطا بالا نمی‌آید.
        runCatching { CodeFillNotifier(context).show(length, fromSms) }
    }
}
