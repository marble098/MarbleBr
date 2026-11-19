package com.marble098.marblebr.engine

import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.SessionHealthAssessment
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.vault.SessionVaultManager
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ویژگی ۴ Never Sign-out Engine:
 * Session Health Guard — تشخیص نشانه‌های خروج ناخواسته (ریدایرکت به صفحه لاگین، پاسخ 401/419/440،
 * حذف کوکی احراز هویت یا پاک شدن ناگهانی Storage) و ترمیم خودکار از آخرین اسنپ‌شات سالم پیش از اینکه کاربر متوجه شود.
 *
 * دارای مکانیزم Circuit Breaker: اگر سرور نشست را واقعاً در سمت خود باطل کرده باشد (تغییر رمز یا انقضای مطلق سرور)،
 * پس از حداکثر تلاش مجاز، به سد دفاعی هشتم (Credential Manager / Passkey) سوییچ می‌کند.
 */
@Singleton
class SessionHealthGuard @Inject constructor(
    private val vaultManager: SessionVaultManager,
    private val healthEventDao: HealthEventDao,
) {
    private val repairAttemptsByContainer = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * بررسی اینکه آیا تغییر مسیر از `fromUrl` به `toUrl` نشان‌دهنده ریدایرکت ناخواسته به صفحه ورود است یا خیر.
     */
    fun isSuspiciousLoginRedirect(fromUrl: String, toUrl: String): Boolean {
        if (fromUrl.isBlank() || toUrl.isBlank() || fromUrl == toUrl) return false
        if (isLoginOrAuthUrl(fromUrl)) return false

        return isLoginOrAuthUrl(toUrl) && hasReturnRedirectParameter(toUrl) ||
            (sameBaseDomain(fromUrl, toUrl) && isLoginOrAuthUrl(toUrl))
    }

    fun isLoginOrAuthUrl(url: String): Boolean {
        val lower = url.lowercase()
        val path = runCatching { URI(url).path?.lowercase().orEmpty() }.getOrDefault(lower)
        return LOGIN_PATH_SEGMENTS.any { seg ->
            path.endsWith(seg) || path.contains("$seg/") || path.contains("$seg?")
        }
    }

    fun hasReturnRedirectParameter(url: String): Boolean {
        val query = runCatching { URI(url).rawQuery?.lowercase().orEmpty() }.getOrDefault(url.lowercase())
        return REDIRECT_QUERY_KEYS.any { key -> query.contains("$key=") }
    }

    /**
     * ارزیابی سیگنال سلامت نشست و اجرای تصمیم ترمیم خودکار یا ارجاع به Credential Fallback.
     */
    suspend fun evaluateSignal(
        signal: SessionHealthSignal,
        neverSignOutEnabled: Boolean = true,
    ): SessionHealthAssessment {
        if (!neverSignOutEnabled) {
            return SessionHealthAssessment(
                containerId = signal.containerId,
                domain = signal.domain,
                status = SessionHealthStatus.DISABLED,
                action = HealthGuardAction.NONE,
                reasonFa = "ویژگی Never Sign-out برای این سایت غیرفعال شده است.",
            )
        }

        if (signal is SessionHealthSignal.HeartbeatOk) {
            repairAttemptsByContainer.remove(signal.containerId)
            return SessionHealthAssessment(
                containerId = signal.containerId,
                domain = signal.domain,
                status = SessionHealthStatus.PROTECTED_ACTIVE,
                action = HealthGuardAction.NONE,
                reasonFa = "نشست سالم و پایدار است.",
            )
        }

        val symptomReasonFa = when (signal) {
            is SessionHealthSignal.HttpUnauthorized ->
                "دریافت کد وضعیت غیرمجاز (${signal.statusCode}) در آدرس ${signal.url}"
            is SessionHealthSignal.LoginRedirectDetected ->
                "تشخیص ریدایرکت ناگهانی به صفحه ورود (${signal.redirectedToUrl})"
            is SessionHealthSignal.AuthCookieMissing ->
                "حذف کوکی احراز هویت (${signal.missingCookieNames.joinToString(", ")})"
            is SessionHealthSignal.StorageClearedUnexpectedly ->
                "پاک شدن ناخواسته حافظه محلی سایت (${signal.domain})"
            is SessionHealthSignal.HeartbeatOk -> "نشست سالم"
        }

        val now = signal.timestampEpochMillis
        val attempts = repairAttemptsByContainer.getOrPut(signal.containerId) { mutableListOf() }
        synchronized(attempts) {
            attempts.removeAll { timestamp -> now - timestamp > REPAIR_WINDOW_MILLIS }
        }

        val latestHealthy = vaultManager.restoreLatestHealthySnapshot(signal.containerId)

        val assessment = if (latestHealthy != null && attempts.size < MAX_REPAIR_ATTEMPTS_IN_WINDOW) {
            synchronized(attempts) {
                attempts.add(now)
            }
            SessionHealthAssessment(
                containerId = signal.containerId,
                domain = signal.domain,
                status = SessionHealthStatus.RECOVERED_FROM_VAULT,
                action = HealthGuardAction.RESTORE_LATEST_SNAPSHOT,
                reasonFa = "$symptomReasonFa — ترمیم خودکار از آخرین اسنپ‌شات سالم Session Vault انجام شد.",
            )
        } else {
            SessionHealthAssessment(
                containerId = signal.containerId,
                domain = signal.domain,
                status = SessionHealthStatus.SERVER_REVOKED_FALLBACK,
                action = HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK,
                reasonFa = "$symptomReasonFa — نشست توسط سرور باطل شده است؛ فعال‌سازی ورود مجدد بی‌صدا با Credential Manager / Passkey.",
            )
        }

        healthEventDao.insert(
            HealthEventEntity(
                id = UUID.randomUUID().toString(),
                containerId = assessment.containerId,
                domain = assessment.domain,
                statusName = assessment.status.name,
                actionName = assessment.action.name,
                descriptionFa = assessment.reasonFa,
                timestampEpochMillis = now,
            ),
        )

        return assessment
    }

    fun resetCircuitBreaker(containerId: String) {
        repairAttemptsByContainer.remove(containerId)
    }

    private fun sameBaseDomain(urlA: String, urlB: String): Boolean {
        val hostA = runCatching { URI(urlA).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
        val hostB = runCatching { URI(urlB).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
        if (hostA.isEmpty() || hostB.isEmpty()) return false
        return hostA == hostB || hostA.endsWith(".$hostB") || hostB.endsWith(".$hostA")
    }

    companion object {
        const val MAX_REPAIR_ATTEMPTS_IN_WINDOW = 2
        const val REPAIR_WINDOW_MILLIS = 60_000L

        private val LOGIN_PATH_SEGMENTS = listOf(
            "/login",
            "/signin",
            "/sign-in",
            "/auth/login",
            "/session/new",
            "/accounts/login",
            "/sso/login",
            "/oauth/authorize",
        )

        private val REDIRECT_QUERY_KEYS = listOf(
            "returnurl",
            "return_to",
            "redirect_to",
            "redirect_uri",
            "continue",
            "next",
        )
    }
}
