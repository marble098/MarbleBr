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
 * محل دکمه‌ی میانبر صفحه‌ی خانه: بالا، پایین یا هیچ‌کدام (بسته به انتخاب کاربر در تنظیمات).
 */
@Serializable
enum class HomeShortcutPosition {
    NONE,
    TOP,
    BOTTOM,
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
