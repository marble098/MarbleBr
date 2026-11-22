package com.marble098.marblebr.core.model

import kotlinx.serialization.Serializable

/**
 * کانتینر مستقل برای هر وب‌اپ یا PWA با contextId اختصاصی در GeckoView.
 * خرابی یا پاک‌سازی یک کانتینر هیچ اثری روی سایر کانتینرها ندارد.
 */
@Serializable
data class WebAppContainer(
    val id: String,
    val domain: String,
    val displayName: String,
    val contextId: String = "marble_ctx_$id",
    val neverSignOutEnabled: Boolean = true,
    val cookieExpirationDays: Int = 365,
    val keepAliveEnabled: Boolean = true,
    val keepAliveIntervalMinutes: Int = 30,
    val keepAliveEndpointPath: String = "/",
    val storagePersistRequested: Boolean = true,
    val isPinnedPwa: Boolean = false,
    val pwaStartUrl: String? = null,
    val themeColorHex: String? = null,
    val lastActiveAtEpochMillis: Long = System.currentTimeMillis(),
)

/**
 * وضعیت فعلی محافظ سلامت نشست (Session Health Guard) برای یک تب یا کانتینر.
 */
@Serializable
enum class SessionHealthStatus {
    HEALTHY,
    PROTECTED_ACTIVE,
    HEALING_IN_PROGRESS,
    RECOVERED_FROM_VAULT,
    SERVER_REVOKED_FALLBACK,
    DISABLED,
}

/**
 * مدل یک تب مرورگر (معمولی یا خصوصی).
 */
@Serializable
data class BrowserTab(
    val id: String,
    val containerId: String,
    val contextId: String,
    val url: String,
    val title: String = "",
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isSecure: Boolean = true,
    val healthStatus: SessionHealthStatus = SessionHealthStatus.PROTECTED_ACTIVE,
    val activeFindQuery: String = "",
    val findMatchIndex: Int = 0,
    val findTotalMatches: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val lastVisitedAtEpochMillis: Long = System.currentTimeMillis(),
)

/**
 * مدل یک کوکی ذخیره‌شده در Session Vault یا تبدیل‌شده به کوکی ماندگار.
 */
@Serializable
data class CookieRecord(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val expiresEpochMillis: Long? = null,
    val isSessionOnly: Boolean = false,
    val isSecure: Boolean = true,
    val isHttpOnly: Boolean = false,
    val sameSite: String = "Lax",
    val isHostOnly: Boolean = true,
) {
    /**
     * بررسی اینکه آیا این کوکی یکی از کوکی‌های کلیدی احراز هویت است یا خیر.
     */
    fun isLikelyAuthCookie(): Boolean {
        val lower = name.lowercase()
        return AUTH_COOKIE_KEYWORDS.any { keyword -> lower.contains(keyword) } ||
            name.startsWith("__Host-") ||
            name.startsWith("__Secure-")
    }

    companion object {
        val AUTH_COOKIE_KEYWORDS = listOf(
            "session",
            "sid",
            "ssid",
            "hsid",
            "apisid",
            "sapisid",
            "token",
            "auth",
            "jwt",
            "access_token",
            "refresh_token",
            "remember",
            "logged_in",
            "user_session",
            "connect.sid",
            "phpsessid",
            "jsessionid",
            "csrftoken",
            "xsrf",
        )
    }
}

/**
 * محتوای کامل یک اسنپ‌شات پیش از رمزنگاری AES-GCM در Session Vault.
 */
@Serializable
data class SessionSnapshotPayload(
    val origin: String,
    val containerId: String,
    val capturedAtEpochMillis: Long,
    val cookies: List<CookieRecord> = emptyList(),
    val localStorage: Map<String, String> = emptyMap(),
    val sessionStorage: Map<String, String> = emptyMap(),
    val indexedDbDump: Map<String, String> = emptyMap(),
    val storagePersisted: Boolean = true,
)

/**
 * فراداده یک اسنپ‌شات برای نمایش در Session Time Machine بدون نیاز به رمزگشایی فوری همه اسنپ‌شات‌ها.
 */
@Serializable
data class SessionSnapshotMeta(
    val id: String,
    val containerId: String,
    val domain: String,
    val capturedAtEpochMillis: Long,
    val cookieCount: Int,
    val storageKeyCount: Int,
    val payloadSha256: String,
    val isHealthy: Boolean,
    val triggerReason: String,
    val label: String,
    /** نسخهٔ «طلایی» (قفل‌شده توسط کاربر) که چرخشِ خودکار حذفش نمی‌کند. */
    val isPinned: Boolean = false,
)

/**
 * سیگنال‌های دریافتی توسط Session Health Guard برای تشخیص خروج ناخواسته.
 */
sealed interface SessionHealthSignal {
    val containerId: String
    val domain: String
    val timestampEpochMillis: Long

    data class HttpUnauthorized(
        override val containerId: String,
        override val domain: String,
        val url: String,
        val statusCode: Int,
        override val timestampEpochMillis: Long = System.currentTimeMillis(),
    ) : SessionHealthSignal

    data class LoginRedirectDetected(
        override val containerId: String,
        override val domain: String,
        val fromUrl: String,
        val redirectedToUrl: String,
        override val timestampEpochMillis: Long = System.currentTimeMillis(),
    ) : SessionHealthSignal

    data class AuthCookieMissing(
        override val containerId: String,
        override val domain: String,
        val missingCookieNames: List<String>,
        override val timestampEpochMillis: Long = System.currentTimeMillis(),
    ) : SessionHealthSignal

    data class StorageClearedUnexpectedly(
        override val containerId: String,
        override val domain: String,
        override val timestampEpochMillis: Long = System.currentTimeMillis(),
    ) : SessionHealthSignal

    data class HeartbeatOk(
        override val containerId: String,
        override val domain: String,
        val url: String,
        override val timestampEpochMillis: Long = System.currentTimeMillis(),
    ) : SessionHealthSignal
}

/**
 * نتیجه ارزیابی سلامت نشست و اقدام پیشنهادی محافظ سلامت (Health Guard).
 */
enum class HealthGuardAction {
    NONE,
    RESTORE_LATEST_SNAPSHOT,
    ESCALATE_TO_CREDENTIAL_FALLBACK,
}

data class SessionHealthAssessment(
    val containerId: String,
    val domain: String,
    val status: SessionHealthStatus,
    val action: HealthGuardAction,
    val reason: HealthReason = HealthReason.UNKNOWN,
    val targetSnapshotId: String? = null,
)

/**
 * دلیل فنیِ یک رویداد نشست؛ متنِ قابل‌نمایش برای کاربر از روی همین مقدار و به زبان انتخابی ساخته می‌شود.
 */
@Serializable
enum class HealthReason {
    SITE_PROTECTION_OFF,
    HEALTHY,
    UNAUTHORIZED,
    LOGIN_REDIRECT,
    AUTH_COOKIE_MISSING,
    STORAGE_CLEARED,
    HEALED,
    SERVER_REVOKED,
    UNKNOWN,
}

@Serializable
data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val folder: String = "general",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class HistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val containerId: String,
    val visitedAtEpochMillis: Long = System.currentTimeMillis(),
    /** چند بار این آدرس باز شده؛ برای «پربازدیدترین‌ها» و مرتب‌سازیِ پیشنهادها. */
    val visitCount: Int = 1,
)

/** منبعِ یک پیشنهادِ نوار آدرس. */
@Serializable
enum class AddressSuggestionSource {
    HISTORY,
    BOOKMARK,
    MOST_VISITED,
    OPEN_TAB,
}

/** یک پیشنهادِ هوشمند برای نوار آدرس (تاریخچه، نشان یا پربازدید). */
@Serializable
data class AddressSuggestion(
    val url: String,
    val title: String,
    val source: AddressSuggestionSource,
    val visitCount: Int = 0,
    val lastVisitedAtEpochMillis: Long = 0,
)

@Serializable
enum class DownloadState {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

@Serializable
data class DownloadItem(
    val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val state: DownloadState,
    val destinationPath: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Serializable
enum class DnsMode(val dohUri: String?) {
    SYSTEM(null),
    CLOUDFLARE("https://cloudflare-dns.com/dns-query"),
    GOOGLE("https://dns.google/dns-query"),
    QUAD9("https://dns.quad9.net/dns-query"),
    CUSTOM(null),
}

@Serializable
enum class ProxyType {
    NONE,
    HTTP,
    SOCKS5,
}

@Serializable
data class ProxyDnsConfig(
    val dnsMode: DnsMode = DnsMode.SYSTEM,
    val customDohUri: String = "https://cloudflare-dns.com/dns-query",
    val proxyType: ProxyType = ProxyType.NONE,
    val proxyHost: String = "",
    val proxyPort: Int = 1080,
) {
    fun resolvedDohUri(): String? = when (dnsMode) {
        DnsMode.SYSTEM -> null
        DnsMode.CUSTOM -> customDohUri.takeIf { it.startsWith("https://") }
        else -> dnsMode.dohUri
    }

    /** آیا یک پروکسیِ فعال و معتبر برای اعمال تنظیم شده است؟ */
    fun hasUsableProxy(): Boolean =
        proxyType != ProxyType.NONE && ProxyValidator.validate(proxyType, proxyHost, proxyPort).isValid
}

/** نتیجهٔ اعتبارسنجیِ تنظیماتِ پروکسی؛ متنِ خطا در رابط کاربر ساخته می‌شود. */
data class ProxyValidation(
    val isValid: Boolean,
    val hostBlank: Boolean = false,
    val hostInvalid: Boolean = false,
    val portInvalid: Boolean = false,
)

/**
 * اعتبارسنجیِ سبکِ پروکسی — بدون وابستگی به اندروید تا در آزمون واحد هم بررسی شود.
 *
 * چرا لازم است؟ پیش‌تر پورتِ نامعتبر (مثلاً ۹۹۹۹۹) بی‌صدا ذخیره می‌شد و بعداً Gecko با یک
 * فایلِ کانفیگِ خراب بالا می‌آمد؛ یا میزبانِ خالی باعث می‌شد پروکسی عملاً اعمال نشود ولی
 * کاربر فکر می‌کند متصل است.
 */
object ProxyValidator {
    private const val MIN_PORT = 1
    private const val MAX_PORT = 65535

    /** میزبان: دامنه، IPv4 یا IPv6 داخلِ کروشه؛ بدونِ فاصله و کاراکترهای کنترلی. */
    private val HOST_PATTERN = Regex(
        """^(?:\[[0-9A-Fa-f:.]+]|[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)$""",
    )

    fun validate(type: ProxyType, rawHost: String, port: Int): ProxyValidation {
        if (type == ProxyType.NONE) return ProxyValidation(isValid = true)
        val host = normalizeHost(rawHost)
        val hostBlank = host.isBlank()
        val hostInvalid = !hostBlank && !HOST_PATTERN.matches(host)
        val portInvalid = port !in MIN_PORT..MAX_PORT
        return ProxyValidation(
            isValid = !hostBlank && !hostInvalid && !portInvalid,
            hostBlank = hostBlank,
            hostInvalid = hostInvalid,
            portInvalid = portInvalid,
        )
    }

    /** حذفِ پروتکل، مسیر، `www.` و فاصله‌های دورِ میزبان؛ IPv6 داخلِ کروشه حفظ می‌شود. */
    fun normalizeHost(rawHost: String): String {
        var value = rawHost.trim()
        val schemeSeparator = value.indexOf("://")
        if (schemeSeparator >= 0) value = value.substring(schemeSeparator + 3)
        value = value.substringBefore('/')
        if (value.startsWith("[")) {
            val closing = value.indexOf(']')
            if (closing > 0) value = value.substring(0, closing + 1)
        } else {
            value = value.substringBefore(':')
        }
        return value.lowercase().removePrefix("www.")
    }
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * رفتار دکمهٔ «بازگشت» گوشی.
 *
 * - `SMART` (پیش‌فرض): اگر صفحه‌ی وب عقب‌تر برود به همان صفحه برمی‌گردد، در غیر این‌صورت به خانه.
 * - `PAGE_THEN_HOME`: همیشه اول صفحه‌ی قبلی وب و بعد خانه.
 * - `HOME`: همیشه مستقیم به خانه می‌رود.
 * - `PAGE_THEN_EXIT`: اول صفحه‌ی قبلی وب و در نهایت خروج از برنامه.
 * - `ASK`: هر بار از کاربر می‌پرسد که چه کاری انجام شود.
 */
@Serializable
enum class BackButtonBehavior {
    SMART,
    PAGE_THEN_HOME,
    HOME,
    PAGE_THEN_EXIT,
    ASK,
}

/**
 * موتورهای جستجوی پیش‌فرضی که کاربر می‌تواند در صفحه‌ی خانه و نوار آدرس از بین آن‌ها انتخاب کند.
 */
@Serializable
data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrlTemplate: String,
) {
    /** چند موتور جستجوی معروف برای انتخاب در تنظیمات و صفحه‌ی خانه. */
    companion object {
        val DEFAULT = SearchEngine("google", "Google", "https://www.google.com/search?q=%s")

        val all: List<SearchEngine> = listOf(
            DEFAULT,
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
            SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
            SearchEngine("yahoo", "Yahoo", "https://search.yahoo.com/search?p=%s"),
            SearchEngine("brave", "Brave Search", "https://search.brave.com/search?q=%s"),
            SearchEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s"),
        )

        fun findById(id: String): SearchEngine =
            all.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * انتخابِ ظاهرِ دکمهٔ «تبِ تازه» در صفحهٔ خانه.
 *
 * پیش‌تر این گزینه «میان‌بر صفحهٔ خانه» نام داشت و یک دکمهٔ مستقلِ شناور روی محتوا می‌ساخت؛
 * همان چیزی که کاربر از آن شکایت داشت (دکمهٔ اضافه، روی صفحهٔ سایت و تکراری با کاشی «تب تازه»).
 * حالا رفتار پیش‌فرض [BUILT_IN] است: دکمه داخل کاشیِ «تب تازه» درون شبکهٔ میانبرها زندگی می‌کند
 * و هیچ عنصر شناوری روی صفحه نمی‌نشیند. حالت [FLOATING] فقط برای کسانی است که دکمهٔ شناور
 * را ترجیح می‌دهند و [NONE] آن را کامل پنهان می‌کند.
 */
@Serializable
enum class HomeShortcutPosition {
    NONE,
    FLOATING,
    BUILT_IN,
}

/**
 * پالت‌های رنگیِ حرفه‌ای و آمادهٔ MarbleBr.
 *
 * هر پالت یک جفت «رنگ اصلی / رنگ مکمل» دارد و تم ([MarbleBrTheme]) از روی آن یک
 * ColorScheme کامل و هماهنگ (Material 3) می‌سازد. [DYNAMIC] همان Material You است که
 * رنگ‌ها را از تصویر پس‌زمینهٔ گوشی می‌گیرد و [OCEAN] پالت پیش‌فرض و آرامِ برنامه است.
 */
@Serializable
enum class ColorPalette {
    DYNAMIC,
    OCEAN,
    EMERALD,
    SUNSET,
    ROYAL,
    CANDY,
    GRAPHITE,
    SAND,
}

/**
 * شکل و شفافیت نوارهای مرورگر (بالا و پایین).
 *
 * - `SOLID`: پس‌زمینهٔ یکدست و مات.
 * - `GLASS`: شیشه‌ای مات با هالهٔ رنگی (رفتار قدیمی).
 * - `FLOATING`: کاملاً شیشه‌ای و شناور، بدون هیچ پس‌زمینه‌ای — محتوا از پشت نوار دیده می‌شود.
 */
@Serializable
enum class ChromeBarStyle {
    SOLID,
    GLASS,
    FLOATING,
}

@Serializable
data class PwaManifestInfo(
    val name: String,
    val shortName: String,
    val startUrl: String,
    val scopeUrl: String,
    val themeColorHex: String? = null,
    val backgroundColorHex: String? = null,
    val displayMode: String = "standalone",
)
