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
    val title: String = "تب جدید",
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
    val reasonFa: String,
    val targetSnapshotId: String? = null,
)

@Serializable
data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String,
    val folder: String = "عمومی",
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
enum class DnsMode(val labelFa: String, val dohUri: String?) {
    SYSTEM("پیش‌فرض سیستم", null),
    CLOUDFLARE("کلودفلر (1.1.1.1)", "https://cloudflare-dns.com/dns-query"),
    GOOGLE("گوگل (8.8.8.8)", "https://dns.google/dns-query"),
    QUAD9("کواد۹ (امنیتی)", "https://dns.quad9.net/dns-query"),
    CUSTOM("سرور DoH سفارشی", null),
}

@Serializable
enum class ProxyType(val labelFa: String) {
    NONE("بدون پروکسی"),
    HTTP("پروکسی HTTP"),
    SOCKS5("پروکسی SOCKS5"),
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
enum class ThemeMode(val labelFa: String) {
    SYSTEM("هماهنگ با سیستم"),
    LIGHT("روشن"),
    DARK("تاریک"),
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
