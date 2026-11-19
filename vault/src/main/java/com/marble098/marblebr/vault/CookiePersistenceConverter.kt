package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.CookieRecord
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ویژگی ۱ Never Sign-out Engine:
 * تبدیل کوکی‌های Session (بدون تاریخ انقضا) یا کوکی‌های کوتاه‌مدت به کوکی‌های ماندگار (Persistent)
 * با انقضای بلند و قابل تنظیم برای هر سایت.
 */
@Singleton
class CookiePersistenceConverter @Inject constructor() {

    /**
     * اگر کوکی از نوع Session باشد یا تاریخ انقضای آن کمتر از مقدار هدف باشد،
     * آن را به یک کوکی ماندگار با انقضای `targetExpirationDays` تبدیل می‌کند.
     */
    fun promoteToPersistent(
        cookie: CookieRecord,
        targetExpirationDays: Int = DEFAULT_EXPIRATION_DAYS,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): CookieRecord {
        val safeDays = targetExpirationDays.coerceIn(MIN_EXPIRATION_DAYS, MAX_EXPIRATION_DAYS)
        val targetExpiryMillis = nowEpochMillis + TimeUnit.DAYS.toMillis(safeDays.toLong())
        val currentExpiry = cookie.expiresEpochMillis

        val shouldExtend = cookie.isSessionOnly ||
            currentExpiry == null ||
            currentExpiry < targetExpiryMillis

        return if (shouldExtend) {
            cookie.copy(
                expiresEpochMillis = targetExpiryMillis,
                isSessionOnly = false,
            )
        } else {
            cookie
        }
    }

    fun promoteAll(
        cookies: List<CookieRecord>,
        targetExpirationDays: Int = DEFAULT_EXPIRATION_DAYS,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): List<CookieRecord> = cookies.map {
        promoteToPersistent(it, targetExpirationDays, nowEpochMillis)
    }

    /**
     * ساخت رشته استاندارد هدر Set-Cookie برای تزریق در کانتینر GeckoView یا افزونه داخلی.
     */
    fun formatSetCookieHeader(cookie: CookieRecord): String = buildString {
        append("${cookie.name}=${cookie.value}")
        if (cookie.domain.isNotBlank()) {
            append("; Domain=${cookie.domain}")
        }
        append("; Path=${cookie.path.ifBlank { "/" }}")
        cookie.expiresEpochMillis?.let { millis ->
            val httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(millis))
            append("; Expires=$httpDate")
            val maxAgeSeconds = ((millis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
            append("; Max-Age=$maxAgeSeconds")
        }
        if (cookie.isSecure) {
            append("; Secure")
        }
        if (cookie.isHttpOnly) {
            append("; HttpOnly")
        }
        if (cookie.sameSite.isNotBlank()) {
            append("; SameSite=${cookie.sameSite}")
        }
    }

    companion object {
        const val DEFAULT_EXPIRATION_DAYS = 365
        const val MIN_EXPIRATION_DAYS = 7
        const val MAX_EXPIRATION_DAYS = 730
    }
}
