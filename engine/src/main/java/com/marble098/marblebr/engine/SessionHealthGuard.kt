package com.marble098.marblebr.engine

import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.SessionHealthAssessment
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionHealthStatus
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Records observed authentication challenges without replaying saved credentials or cookies. */
@Singleton
class SessionHealthGuard @Inject constructor(
    private val healthEventDao: HealthEventDao,
) {
    fun isSuspiciousLoginRedirect(fromUrl: String, toUrl: String): Boolean {
        if (fromUrl.isBlank() || toUrl.isBlank() || fromUrl == toUrl) return false
        if (isLoginOrAuthUrl(fromUrl)) return false

        return (isLoginOrAuthUrl(toUrl) && hasReturnRedirectParameter(toUrl)) || (sameBaseDomain(fromUrl, toUrl) && isLoginOrAuthUrl(toUrl))
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
                reason = HealthReason.SITE_PROTECTION_OFF,
            )
        }

        if (signal is SessionHealthSignal.HeartbeatOk) {
            return SessionHealthAssessment(
                containerId = signal.containerId,
                domain = signal.domain,
                status = SessionHealthStatus.PROTECTED_ACTIVE,
                action = HealthGuardAction.NONE,
                reason = HealthReason.HEALTHY,
            )
        }

        val reason = when (signal) {
            is SessionHealthSignal.HttpUnauthorized -> HealthReason.UNAUTHORIZED
            is SessionHealthSignal.LoginRedirectDetected -> HealthReason.LOGIN_REDIRECT
            is SessionHealthSignal.AuthCookieMissing -> HealthReason.AUTH_COOKIE_MISSING
            is SessionHealthSignal.StorageClearedUnexpectedly -> HealthReason.STORAGE_CLEARED
            is SessionHealthSignal.HeartbeatOk -> HealthReason.HEALTHY
        }
        val requiresSignIn = signal is SessionHealthSignal.HttpUnauthorized || signal is SessionHealthSignal.LoginRedirectDetected
        val assessment = SessionHealthAssessment(
            containerId = signal.containerId,
            domain = signal.domain,
            status = if (requiresSignIn) SessionHealthStatus.REAUTH_REQUIRED else SessionHealthStatus.PROTECTED_ACTIVE,
            action = if (requiresSignIn) HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK else HealthGuardAction.NONE,
            reason = reason,
        )

        healthEventDao.insert(
            HealthEventEntity(
                id = UUID.randomUUID().toString(),
                containerId = assessment.containerId,
                domain = assessment.domain,
                statusName = assessment.status.name,
                actionName = assessment.action.name,
                // ستون قدیمی `descriptionFa` حالا کدِ پایدارِ دلیل را نگه می‌دارد تا متن‌ها دوزبانه نمایش داده شوند.
                descriptionFa = assessment.reason.name,
                timestampEpochMillis = signal.timestampEpochMillis,
            ),
        )

        return assessment
    }

    private fun sameBaseDomain(urlA: String, urlB: String): Boolean {
        val hostA = runCatching { URI(urlA).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
        val hostB = runCatching { URI(urlB).host?.lowercase()?.removePrefix("www.").orEmpty() }.getOrDefault("")
        if (hostA.isEmpty() || hostB.isEmpty()) return false
        return hostA == hostB || hostA.endsWith(".$hostB") || hostB.endsWith(".$hostA")
    }

    companion object {
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
