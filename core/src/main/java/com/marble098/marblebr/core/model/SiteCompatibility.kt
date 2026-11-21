package com.marble098.marblebr.core.model

import java.net.URI

/**
 * «افزودن هر سایتی» — موتور سازگاری سایت.
 *
 * پیش‌تر اگر سایتی با پنجرهٔ کانتینر ما کنار نمی‌آمد (مثلاً ورودش فقط با گوگل بود، فرمش داخل
 * iframe بود، یا صفحه می‌گفت «مرورگر شما پشتیبانی نمی‌شود»)، تنها راهِ کاربر این بود که سایت را
 * رها کند. این فایل همان مسئله را به یک تصمیمِ قابلِ تست تبدیل می‌کند:
 *
 * ۱. [LoginMethodDetector] روشِ ورودِ واقعیِ سایت را از نشانه‌های صفحه حدس می‌زند
 *    (گذرواژه، گوگل، اپل، کد پیامکی، لینک جادویی، پاسکی، کیف پول، SSO سازمانی).
 * ۲. [SiteBlockerDetector] دلیلِ «نشد» را نام‌گذاری می‌کند (iframe، تشخیصِ مرورگر، کوکیِ
 *    سوم‌شخص، نبودِ جاوااسکریپت، صفحهٔ خطای شبکه…).
 * ۳. [SiteAdditionPlanner] به‌جای یک «نه»ی خشک، یک **نردبانِ تلاش** می‌سازد: مسیرهای جایگزین
 *    (کاربر‌عاملِ دسکتاپ، زیردامنهٔ موبایل، دامنهٔ بدون www، پنجرهٔ PWA، ورودِ دستی با مخزن) که
 *    تا یکی جواب بدهد ادامه پیدا می‌کند. هیچ سایتی از فهرست حذف نمی‌شود؛ فقط «بهترین راه» عوض می‌شود.
 *
 * همهٔ توابع این فایل خالص‌اند (بدون اندروید/شبکه) تا با تست واحد سنجیده شوند.
 */
enum class LoginMethod {
    PASSWORD,
    GOOGLE,
    APPLE,
    PHONE_CODE,
    EMAIL_CODE,
    MAGIC_LINK,
    PASSKEY,
    WALLET,
    CORPORATE_SSO,
    UNKNOWN,
}

/** دلیلِ واقعیِ اینکه یک سایت در پنجرهٔ داخلی راحت باز نمی‌شود. */
enum class SiteBlocker {
    /** فرمِ ورود داخل iframe است و اسکریپتِ ما به آن دسترسی ندارد. */
    IFRAME_LOGIN,

    /** سایت صریحاً مرورگرِ داخلی را نمی‌پذیرد («مرورگر پشتیبانی نمی‌شود»). */
    BROWSER_REFUSED,

    /** ورود به کوکیِ سوم‌شخص یا ریدایرکتِ بین‌دامنه‌ای وابسته است. */
    THIRD_PARTY_COOKIES,

    /** صفحه بدونِ جاوااسکریپت چیزی نشان نمی‌دهد (یا برعکس، فقط با JS کار می‌کند). */
    JAVASCRIPT_ONLY,

    /** خطای شبکه/DNS یا صفحهٔ خطای خودِ سایت. */
    NETWORK_ERROR,

    /** کدِ یک‌بارمصرف/تأیید پیامکی، ذخیرهٔ خودکار گذرواژه را بی‌اثر می‌کند. */
    ONE_TIME_CODE_ONLY,

    NONE,
}

/** نشانه‌هایی که content script از همان صفحه می‌خواند (همه اختیاری و پیش‌فرض‌های امن). */
data class LoginPageObservation(
    val passwordFieldCount: Int = 0,
    val usernameFieldCount: Int = 0,
    val oneTimeCodeFieldCount: Int = 0,
    val captchaPresent: Boolean = false,
    val differentOriginIframeCount: Int = 0,
    val loginIframePresent: Boolean = false,
    val googleButtonPresent: Boolean = false,
    val appleButtonPresent: Boolean = false,
    val passkeyButtonPresent: Boolean = false,
    val walletButtonPresent: Boolean = false,
    val ssoDomainHint: Boolean = false,
    val magicLinkHint: Boolean = false,
    val browserRefusalText: Boolean = false,
    val passwordManagerBlocked: Boolean = false,
    val javascriptDisabledPage: Boolean = false,
    val lastLoadFailed: Boolean = false,
    val wasThreeTenOrOtpRedirect: Boolean = false,
) {
    val hasAnyLoginSignal: Boolean
        get() = passwordFieldCount > 0 ||
            googleButtonPresent ||
            appleButtonPresent ||
            passkeyButtonPresent ||
            walletButtonPresent ||
            oneTimeCodeFieldCount > 0 ||
            magicLinkHint

    companion object {
        val EMPTY = LoginPageObservation()
    }
}

/** نتیجهٔ تشخیص: روشِ ورودِ پیشنهادی + همهٔ گزینه‌های دیگر به‌ترتیبِ اطمینان. */
data class LoginMethodAssessment(
    val primary: LoginMethod,
    val alternatives: List<LoginMethod> = emptyList(),
    val blockers: List<SiteBlocker> = emptyList(),
) {
    /** آیا ورودِ خودکارِ گذرواژه برای این سایت معنا دارد؟ */
    val supportsPasswordAutofill: Boolean
        get() = primary == LoginMethod.PASSWORD || LoginMethod.PASSWORD in alternatives

    val needsManualHelp: Boolean
        get() = primary != LoginMethod.PASSWORD
}

object LoginMethodDetector {

    fun assess(observation: LoginPageObservation): LoginMethodAssessment {
        val candidates = mutableListOf<Pair<LoginMethod, Int>>()
        if (observation.passwordFieldCount > 0) candidates += LoginMethod.PASSWORD to 100
        if (observation.passkeyButtonPresent) candidates += LoginMethod.PASSKEY to 70
        if (observation.googleButtonPresent) candidates += LoginMethod.GOOGLE to 65
        if (observation.appleButtonPresent) candidates += LoginMethod.APPLE to 60
        if (observation.ssoDomainHint) candidates += LoginMethod.CORPORATE_SSO to 55
        if (observation.walletButtonPresent) candidates += LoginMethod.WALLET to 50
        if (observation.magicLinkHint) candidates += LoginMethod.MAGIC_LINK to 40
        if (observation.oneTimeCodeFieldCount > 0) candidates += LoginMethod.PHONE_CODE to 35
        if (observation.wasThreeTenOrOtpRedirect) candidates += LoginMethod.EMAIL_CODE to 20

        val ranked = candidates.sortedByDescending { it.second }.map { it.first }
        return LoginMethodAssessment(
            primary = ranked.firstOrNull() ?: LoginMethod.UNKNOWN,
            alternatives = ranked.drop(1),
            blockers = SiteBlockerDetector.detect(observation),
        )
    }
}

object SiteBlockerDetector {

    fun detect(observation: LoginPageObservation): List<SiteBlocker> {
        val blockers = mutableListOf<SiteBlocker>()
        if (observation.loginIframePresent) blockers += SiteBlocker.IFRAME_LOGIN
        if (observation.browserRefusalText) blockers += SiteBlocker.BROWSER_REFUSED
        if (observation.differentOriginIframeCount > 0) blockers += SiteBlocker.THIRD_PARTY_COOKIES
        if (observation.javascriptDisabledPage) blockers += SiteBlocker.JAVASCRIPT_ONLY
        if (observation.lastLoadFailed) blockers += SiteBlocker.NETWORK_ERROR
        if (observation.oneTimeCodeFieldCount > 0 && observation.passwordFieldCount == 0) {
            blockers += SiteBlocker.ONE_TIME_CODE_ONLY
        }
        return blockers.ifEmpty { listOf(SiteBlocker.NONE) }
    }
}

/**
 * مسیرهای عملیِ بازکردن یک سایت، از «ساده‌ترین» تا «جایگزین».
 *
 * هر مسیر فقط یک تغییرِ کوچک است (کاربرعامل، آدرس، نحوهٔ ورود) تا اگر سایتی با مسیرِ اول جواب
 * نداد، همان کانتینر و همان ورود حفظ شود و کاربر از نو شروع نکند.
 */
enum class SiteRoute {
    /** همان آدرس، همان کانتینر. */
    DIRECT,

    /** با کاربرعاملِ مرورگرِ دسکتاپ: سایت‌هایی که نسخهٔ موبایلشان ناقص است. */
    DESKTOP_USER_AGENT,

    /** دامنهٔ موبایلِ سایت (`m.` یا `mobile.`) که سبک‌تر و سازگارتر است. */
    MOBILE_SUBDOMAIN,

    /** حذف `www.` یا افزودن `https://` صریح؛ برای ریدایرکت‌های شکسته. */
    CANONICAL_HOST,

    /** باز کردن به‌صورت PWA/پنجرهٔ مستقل: میان‌برِ صفحهٔ خانه با نشست مستقل. */
    PWA_WINDOW,

    /** ورود دستی از مخزن گذرواژه؛ برای سایت‌هایی که هرگز پرکردنِ خودکار را نمی‌پذیرند. */
    VAULT_MANUAL_LOGIN,

    /** فقط خواندن: نسخهٔ ذخیره‌شده/حالت مطالعه تا کاربر حداقل محتوا را ببیند. */
    READ_ONLY_SNAPSHOT,
}

data class SiteAdditionStep(
    val route: SiteRoute,
    val targetUrl: String,
    val useDesktopUserAgent: Boolean = false,
    val note: SiteBlocker = SiteBlocker.NONE,
)

data class SiteAdditionPlan(
    val domain: String,
    val assessment: LoginMethodAssessment,
    val steps: List<SiteAdditionStep>,
) {
    val best: SiteAdditionStep? get() = steps.firstOrNull()
    val hasFallbacks: Boolean get() = steps.size > 1

    /** پیامِ کوتاهِ «چه کاری می‌کنیم» برای نمایش به کاربر. */
    fun routeSummary(): List<SiteRoute> = steps.map { it.route }
}

object SiteAdditionPlanner {

    /**
     * نردبانِ تلاش را می‌سازد.
     *
     * @param failedRoutes مسیرهایی که در همین نشست قبلاً شکست خورده‌اند؛ دقیقاً همان‌ها دیگر
     *   پیشنهاد نمی‌شوند تا کاربر دورِ باطل نزند.
     */
    fun plan(
        url: String,
        observation: LoginPageObservation = LoginPageObservation.EMPTY,
        failedRoutes: Set<SiteRoute> = emptySet(),
        preferDesktop: Boolean = false,
    ): SiteAdditionPlan {
        val domain = domainOf(url)
        val assessment = LoginMethodDetector.assess(observation)
        val targets = UrlVariantResolver.variants(url)
        val candidates = mutableListOf<SiteAdditionStep>()

        // ۱) اگر خودِ کاربر حالت دسکتاپ را خواسته، همان اول می‌آید؛ وگرنه آخر.
        if (preferDesktop) {
            candidates += SiteAdditionStep(SiteRoute.DESKTOP_USER_AGENT, targets.direct, useDesktopUserAgent = true)
        }
        candidates += SiteAdditionStep(SiteRoute.DIRECT, targets.direct)

        // ۲) دامنهٔ موبایل، اگر سایت چنین چیزی داشته باشد و با آدرس فعلی یکی نباشد.
        targets.mobileHost?.let { mobile ->
            candidates += SiteAdditionStep(SiteRoute.MOBILE_SUBDOMAIN, mobile)
        }
        targets.canonicalHost?.let { canonical ->
            candidates += SiteAdditionStep(SiteRoute.CANONICAL_HOST, canonical)
        }
        if (!preferDesktop) {
            candidates += SiteAdditionStep(
                SiteRoute.DESKTOP_USER_AGENT,
                targets.direct,
                useDesktopUserAgent = true,
                note = SiteBlocker.BROWSER_REFUSED,
            )
        }

        // ۳) اگر ورود داخل iframe یا فقط با کدِ یک‌بارمصرف است، مسیرِ «ورود دستی» همیشه باز است.
        if (SiteBlocker.IFRAME_LOGIN in assessment.blockers ||
            SiteBlocker.ONE_TIME_CODE_ONLY in assessment.blockers ||
            assessment.needsManualHelp
        ) {
            candidates += SiteAdditionStep(
                SiteRoute.VAULT_MANUAL_LOGIN,
                targets.direct,
                note = SiteBlocker.IFRAME_LOGIN,
            )
        }

        // ۴) پنجرهٔ مستقل (PWA) برای سایت‌هایی که در تبِ معمولی رفتار عجیب دارند.
        candidates += SiteAdditionStep(SiteRoute.PWA_WINDOW, targets.direct)

        // ۵) آخرین سد: نمایشِ نسخهٔ ذخیره‌شده در حالت خواندن، تا دست‌کم محتوا از دست نرود.
        if (SiteBlocker.NETWORK_ERROR in assessment.blockers || SiteBlocker.BROWSER_REFUSED in assessment.blockers) {
            candidates += SiteAdditionStep(SiteRoute.READ_ONLY_SNAPSHOT, targets.direct, note = SiteBlocker.NETWORK_ERROR)
        }

        val distinct = candidates
            .filterNot { it.route in failedRoutes }
            .distinctBy { it.route }
            .ifEmpty { listOf(SiteAdditionStep(SiteRoute.DIRECT, targets.direct)) }

        return SiteAdditionPlan(domain = domain, assessment = assessment, steps = distinct)
    }

    fun domainOf(url: String): String = runCatching {
        val normalized = if (url.contains("://")) url.trim() else "https://${url.trim()}"
        URI(normalized).host?.lowercase()?.removePrefix("www.")
    }.getOrNull() ?: url.trim().lowercase()

    /** آیا این مسیر به یک پنجرهٔ مستقل (PWA) نیاز دارد؟ */
    fun routeRequiresPwa(route: SiteRoute): Boolean = route == SiteRoute.PWA_WINDOW
}

data class UrlVariants(
    val direct: String,
    val mobileHost: String? = null,
    val canonicalHost: String? = null,
)

object UrlVariantResolver {
    private val MOBILE_PREFIXES = listOf("m.", "mobile.")

    fun variants(rawUrl: String): UrlVariants {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return UrlVariants(direct = "about:blank")
        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
        val uri = runCatching { URI(normalized) }.getOrNull()
            ?: return UrlVariants(direct = normalized)
        val host = uri.host?.lowercase() ?: return UrlVariants(direct = normalized)
        val path = buildString {
            append(uri.rawPath?.takeIf { it.isNotBlank() } ?: "/")
            uri.rawQuery?.let { append("?").append(it) }
        }
        val canonical = host.removePrefix("www.")
        val mobileBase = MOBILE_PREFIXES.firstOrNull { canonical.startsWith(it) }
        val mobileHost = when {
            mobileBase != null -> null

            canonical.isBlank() -> null

            // سایت‌های تک‌برچسبی مثل localhost نسخهٔ موبایل ندارند.
            !canonical.contains('.') -> null

            else -> "https://m.$canonical$path"
        }
        val canonicalHost = if (host != canonical) "https://$canonical$path" else null
        return UrlVariants(
            direct = "https://$host$path",
            mobileHost = mobileHost,
            canonicalHost = canonicalHost,
        )
    }
}
