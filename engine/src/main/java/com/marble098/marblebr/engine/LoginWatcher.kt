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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * «رصدگرِ ورود» — پلِ میانِ صفحهٔ وب و استودیوی ورود.
 *
 * این کلاس هیچ تصمیمی نمی‌گیرد؛ فقط:
 * ۱. پس از کامل‌شدنِ هر صفحه، کاوشِ بی‌خطر را اجرا می‌کند و نتیجه را به [PasswordStudio] می‌دهد.
 * ۲. اگر دستورِ پرکردن/نوشتنِ کد آمد، همان اسکریپت را در همان تب اجرا می‌کند.
 * ۳. ارسالِ فرم را از content script می‌گیرد و پس از تأییدِ موفقیت، کارتِ ذخیره را می‌سازد.
 * ۴. وقتی صفحه‌ای کادرِ کد دارد، پیامک/اعلانِ تازه را می‌شنود و کد را می‌نویسد.
 *
 * نکتهٔ مهمِ بازنویسی: نسخهٔ قبلی برای هر بار بارگذاری یک درخواستِ خودکار به صفحه می‌فرستاد و
 * نتیجه را در یک `ConcurrentHashMap` نگه می‌داشت، اما هیچ‌جا کدِ یکبارمصرف را جست‌وجو نمی‌کرد و
 * اسکریپتِ پرکردن را اجرا نمی‌کرد. حالا هر دو مسیر واقعاً به اجرا می‌رسند.
 */
@Singleton
class LoginWatcher @Inject constructor(
    private val siteEngine: SiteCompatibilityEngine,
    private val sessionManager: ContainerSessionManager,
    private val passwordStudio: PasswordStudio,
    private val smsBridge: Provider<SmsBridge>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** آخرین آدرسی که برای هر تب کاوش شده؛ کاوشِ دوباره روی همان آدرس لازم نیست. */
    private val lastObserved = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** کاوش‌های در جریان، تا دو کاوشِ هم‌زمان روی یک تب اجرا نشود. */
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Job>()

    /** پایشگرهای فعالِ کدِ یکبارمصرف برای هر تب. */
    private val codeWatchers = java.util.concurrent.ConcurrentHashMap<String, Job>()

    /**
     * پس از هر بارگذاریِ موفقِ صفحه.
     *
     * @param url آدرسِ نهاییِ صفحه (پس از ریدایرکت‌ها)
     */
    fun observePage(tabId: String, url: String) {
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return
        if (lastObserved[tabId] == url) return
        lastObserved[tabId] = url
        inFlight.remove(tabId)?.cancel()

        // ارسالِ فرمِ قبلی ممکن است با همین پیمایش تأیید شود. «فرمِ صفحهٔ تازه» را حدس نمی‌زنیم؛
        // `null` می‌فرستیم تا استودیو با دانشِ خودش (همان لحظهٔ ارسال) داوری کند.
        scope.launch {
            runCatching {
                passwordStudio.settlePendingSubmission(
                    movedToNewUrl = true,
                    passwordFieldsAfter = null,
                    otpFieldsAfter = if (url.contains("otp", true) || url.contains("verif", true)) 1 else 0,
                )
            }
        }

        inFlight[tabId] = scope.launch {
            withContext(Dispatchers.Main.immediate) {
                runCatching { observeNow(tabId, url) }
            }
        }
    }

    private suspend fun observeNow(tabId: String, url: String) {
        val isPrivate = sessionManager.isPrivateSession(tabId)
        val observationRaw = sessionManager.evalInPageForLogin(tabId, LoginObservationProbe.OBSERVE_SCRIPT)
        siteEngine.onPageLoaded(tabId, url, observationRaw)
        if (isPrivate) return

        val directive = passwordStudio.onPageReady(
            tabId = tabId,
            pageUrl = url,
            observationRaw = observationRaw,
            isPrivateTab = isPrivate,
        )
        when (directive) {
            is LoginPageDirective.FillCredentials -> {
                // پرکردن فقط پس از تأییدِ بیومتریک (اگر تنظیم شده باشد) توسط لایهٔ رابط انجام
                // می‌شود؛ این‌جا فقط اسکریپتِ آماده را ثبت می‌کنیم تا UI اجرا کند.
                pendingFillDirective = directive
            }

            is LoginPageDirective.OriginWarning -> Unit

            LoginPageDirective.None -> {
                // شاید این صفحه کادرِ کد داشته باشد؛ آن‌وقت کد را دنبال می‌کنیم.
                watchForOneTimeCode(tabId, url)
            }
        }
    }

    /** دستورِ پرکردنِ منتظرِ تأییدِ کاربر/بیومتریک (فقط در حافظه؛ هیچ گذرواژه‌ای در جریان نیست). */
    @Volatile
    var pendingFillDirective: LoginPageDirective.FillCredentials? = null
        private set

    fun consumeFillDirective(): LoginPageDirective.FillCredentials? = pendingFillDirective.also { pendingFillDirective = null }

    /**
     * اجرای اسکریپتِ پرکردن (`filled` / `no-form` / `no-password` / `failed`).
     * بلافاصله پس از پرکردن، `autofill` در مخزن ثبت می‌شود تا این حساب اولویت بگیرد.
     */
    suspend fun executeAutofill(tabId: String, script: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val result = sessionManager.evalInPageForLogin(tabId, script)
            result == "filled"
        }

    /**
     * پایشِ کدِ یکبارمصرف تا وقتی این صفحه باز است.
     *
     * چرا پایش و نه فقط یک‌بار خواندن؟ چون پیامک معمولاً ۱ تا ۱۵ ثانیه بعد از درخواست می‌رسد.
     * این پایش حداکثر [CODE_WATCH_WINDOW_MILLIS] طول می‌کشد، هر چند ثانیه یک‌بار صندوق را
     * می‌بیند و در پایان خودش را می‌بندد (هیچ منبعی معطل نمی‌ماند).
     */
    fun watchForOneTimeCode(tabId: String, url: String) {
        if (codeWatchers[tabId]?.isActive == true) return
        codeWatchers[tabId] = scope.launch {
            val deadline = System.currentTimeMillis() + CODE_WATCH_WINDOW_MILLIS
            var lastTried = ""
            while (System.currentTimeMillis() < deadline && sessionManager.hasOpenSession(tabId)) {
                delay(CODE_POLL_INTERVAL_MILLIS)
                val fieldsRaw = sessionManager.evalInPageForLogin(tabId, OtpFieldProbe.DISCOVER_SCRIPT)
                    ?: continue
                if (!passwordStudio.hasCodeField(fieldsRaw, url)) continue
                val directive = passwordStudio.requestOneTimeCode(
                    pageUrl = url,
                    observationRaw = null,
                    fieldsRaw = fieldsRaw,
                    isPrivateTab = sessionManager.isPrivateSession(tabId),
                )
                if (directive is LoginOneTimeCodeDirective.Ready) {
                    if (directive.script == lastTried) continue
                    lastTried = directive.script
                    val filled = sessionManager.evalInPageForLogin(tabId, directive.script)
                    if (filled == "filled" || filled == "already") {
                        passwordStudio.consumeNotificationCode()
                        smsBridge.get().notifyCodeFilled(directive.digits, directive.fromSms)
                        return@launch
                    }
                }
            }
        }
    }

    /** کد را همین حالا از پیامک/اعلان پیدا و در کادر بنویس؛ برای دکمهٔ «کد را بگذار». */
    suspend fun fillOneTimeCodeNow(tabId: String, url: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            val fieldsRaw = sessionManager.evalInPageForLogin(tabId, OtpFieldProbe.DISCOVER_SCRIPT) ?: return@withContext false
            if (!passwordStudio.hasCodeField(fieldsRaw, url)) return@withContext false
            val directive = passwordStudio.requestOneTimeCode(
                pageUrl = url,
                observationRaw = null,
                fieldsRaw = fieldsRaw,
                isPrivateTab = sessionManager.isPrivateSession(tabId),
            )
            if (directive !is LoginOneTimeCodeDirective.Ready) return@withContext false
            val result = sessionManager.evalInPageForLogin(tabId, directive.script)
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
            val fieldsRaw = sessionManager.evalInPageForLogin(tabId, OtpFieldProbe.DISCOVER_SCRIPT) ?: return@withContext false
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
        if (password.isBlank()) return
        scope.launch {
            runCatching {
                passwordStudio.onLoginSubmit(
                    pageUrl = url,
                    username = username,
                    password = password,
                    otpFieldCount = otpFieldCount,
                    isPrivateTab = sessionManager.isPrivateSession(tabId),
                )
                // ارسال‌هایی که فقط کد می‌خواهند (بدونِ ناوبری) هم باید تأیید شوند؛ اگر گذرواژه
                // همراه بود، «ناوبری» یا «ناپدیدشدنِ فرم» شاهدِ موفقیت است و این فراخوانیِ زودهنگام
                // چیزی را خراب نمی‌کند چون تا رسیدنِ مهلت، تصمیم `UNKNOWN` می‌ماند.
                // `null` یعنی «تعدادِ آخرین کاوش را خودِ استودیو می‌داند»؛ حدس نمی‌زنیم.
                passwordStudio.settlePendingSubmission(
                    movedToNewUrl = false,
                    passwordFieldsAfter = null,
                )
            }
        }
    }

    /** خواندنِ همین‌حالا‌ی فرم و ثبتِ ارسالِ دستی (دکمهٔ «این ورود را ذخیره کن»). */
    fun captureCurrentForm(tabId: String, url: String) {
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                val raw = sessionManager.evalInPageForLogin(tabId, LoginObservationProbe.CAPTURE_SUBMIT_SCRIPT)
                val submitted = LoginObservationProbe.parseSubmitted(raw) ?: return@withContext
                passwordStudio.onLoginSubmit(
                    pageUrl = url,
                    username = submitted.username,
                    password = submitted.password,
                    otpFieldCount = submitted.otpFieldCount,
                    isPrivateTab = sessionManager.isPrivateSession(tabId),
                )
                // فرم دستی خوانده شد؛ اگر خطا دیده شد رد، وگرنه «فرم ناپدید شد» شاهدِ موفقیت است.
                passwordStudio.settlePendingSubmission(movedToNewUrl = false, passwordFieldsAfter = 0)
            }
        }
    }

    /** اجرای یک اسکریپتِ آماده در صفحهٔ همان تب (پرکردنِ دستی از پنل مخزن). */
    fun runInPage(tabId: String, script: String, onResult: (String?) -> Unit = {}) {
        sessionManager.evaluateInPageForLogin(tabId, script, onResult)
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
        lastObserved.remove(tabId)
        inFlight.remove(tabId)?.cancel()
        codeWatchers.remove(tabId)?.cancel()
        siteEngine.forgetTab(tabId)
        pendingFillDirective = null
    }

    /** برای آزمون: آیا الان پایشگری برای این تب فعال است؟ */
    fun isWatchingForCode(tabId: String): Boolean = codeWatchers[tabId]?.isActive == true

    companion object {
        const val CODE_WATCH_WINDOW_MILLIS = 90_000L
        const val CODE_POLL_INTERVAL_MILLIS = 2_500L
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
