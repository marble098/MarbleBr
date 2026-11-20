package com.marble098.marblebr.engine

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.vault.CookiePersistenceConverter
import com.marble098.marblebr.vault.SessionVaultManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ویژگی ۵ Never Sign-out Engine:
 * Smart Keep-alive — با WorkManager و رعایت محدودیت باتری/شبکه، سایت‌های دارای Sliding Session
 * را در پس‌زمینه با یک درخواست سبک تازه نگه می‌دارد و کوکی‌های تمدیدشده را در Session Vault ذخیره می‌کند.
 */
@HiltWorker
class SmartKeepAliveWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val containerDao: ContainerDao,
    private val vaultManager: SessionVaultManager,
    private val cookieConverter: CookiePersistenceConverter,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val eligibleContainers = containerDao.getKeepAliveEligibleContainers()
        if (eligibleContainers.isEmpty()) {
            return@withContext Result.success()
        }

        for (container in eligibleContainers) {
            runCatching {
                val snapshot = vaultManager.restoreLatestHealthySnapshot(container.id) ?: return@runCatching
                if (snapshot.cookies.isEmpty()) return@runCatching

                val targetUrl = CookieRequestPolicy.targetUrl(snapshot.origin, container.keepAliveEndpointPath) ?: return@runCatching
                val targetHost = URI(targetUrl).host ?: return@runCatching
                if (targetHost.lowercase().removePrefix("www.") != container.domain) return@runCatching
                val eligible = CookieRequestPolicy.cookiesForUrl(snapshot.cookies, targetUrl)
                if (eligible.isEmpty()) return@runCatching
                val cookieHeader = eligible.joinToString("; ") { "${it.name}=${it.value}" }

                val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    instanceFollowRedirects = false
                    setRequestProperty("Cookie", cookieHeader)
                    setRequestProperty("User-Agent", KEEP_ALIVE_USER_AGENT)
                    setRequestProperty("Accept", "*/*")
                }

                try {
                    val responseCode = connection.responseCode
                    if (responseCode in 200..399) {
                        val setCookieHeaders = connection.headerFields["Set-Cookie"]
                            ?: connection.headerFields["set-cookie"]
                            ?: emptyList()

                        if (setCookieHeaders.isNotEmpty()) {
                            val updatedCookies = mergeRefreshedCookies(
                                existing = snapshot.cookies,
                                setCookieHeaders = setCookieHeaders,
                                defaultDomain = targetHost,
                                expirationDays = container.cookieExpirationDays,
                            )
                            vaultManager.captureSnapshot(
                                payload = snapshot.copy(
                                    capturedAtEpochMillis = System.currentTimeMillis(),
                                    cookies = updatedCookies,
                                ),
                                targetCookieExpirationDays = container.cookieExpirationDays,
                                triggerReason = "SMART_KEEP_ALIVE",
                                label = "background-keep-alive",
                            )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }

        Result.success()
    }

    internal fun mergeRefreshedCookies(
        existing: List<CookieRecord>,
        setCookieHeaders: List<String>,
        defaultDomain: String,
        expirationDays: Int,
    ): List<CookieRecord> {
        fun key(cookie: CookieRecord) = Triple(cookie.domain.removePrefix(".").lowercase(), cookie.path, cookie.name)
        val byScope = existing.associateBy(::key).toMutableMap()
        for (rawHeader in setCookieHeaders) {
            val parsed = parseSetCookieHeader(rawHeader, defaultDomain) ?: continue
            val domain = parsed.domain.removePrefix(".").lowercase()
            val host = defaultDomain.lowercase()
            if (host != domain && (parsed.isHostOnly || !host.endsWith(".$domain"))) continue
            val expiry = parsed.expiresEpochMillis
            if (expiry != null && expiry <= System.currentTimeMillis()) {
                byScope.remove(key(parsed))
            } else {
                byScope[key(parsed)] = if (expiry == null) {
                    cookieConverter.promoteToPersistent(parsed, expirationDays)
                } else {
                    parsed
                }
            }
        }
        return byScope.values.toList()
    }

    internal fun parseSetCookieHeader(rawHeader: String, defaultDomain: String): CookieRecord? {
        val segments = rawHeader.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        val nameValue = segments.first()
        val eqIdx = nameValue.indexOf('=')
        if (eqIdx <= 0) return null

        val name = nameValue.substring(0, eqIdx).trim()
        val value = nameValue.substring(eqIdx + 1).trim()
        if (name.isEmpty()) return null

        var domain = defaultDomain
        var path = "/"
        var isSecure = false
        var isHttpOnly = false
        var isHostOnly = true
        var sameSite = "Lax"
        var expiry: Long? = null
        var maxAge: Long? = null

        for (attr in segments.drop(1)) {
            val lower = attr.lowercase()
            when {
                lower.startsWith("domain=") -> {
                    domain = attr.substringAfter("=").trim()
                    isHostOnly = false
                }
                lower.startsWith("path=") -> path = attr.substringAfter("=").trim()
                lower == "secure" -> isSecure = true
                lower == "httponly" -> isHttpOnly = true
                lower.startsWith("samesite=") -> sameSite = attr.substringAfter("=").trim()
                lower.startsWith("max-age=") -> maxAge = attr.substringAfter("=").trim().toLongOrNull()
                lower.startsWith("expires=") -> expiry = runCatching {
                    ZonedDateTime.parse(attr.substringAfter("=").trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli()
                }.getOrNull()
            }
        }

        if (maxAge != null) {
            expiry = if (maxAge <= 0) 0L else System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge.coerceAtMost(63_072_000))
        }
        return CookieRecord(
            name = name,
            value = value,
            domain = domain,
            path = path,
            isSessionOnly = expiry == null,
            expiresEpochMillis = expiry,
            isSecure = isSecure,
            isHttpOnly = isHttpOnly,
            isHostOnly = isHostOnly,
            sameSite = sameSite,
        )
    }

    companion object {
        const val WORK_NAME = "marblebr_smart_keep_alive_periodic_work"
        private const val KEEP_ALIVE_USER_AGENT =
            "Mozilla/5.0 (Android 16; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0 MarbleBr/1.0"
    }
}

@Singleton
class KeepAliveScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedulePeriodicKeepAlive(intervalMinutes: Long = 30L) {
        val safeInterval = intervalMinutes.coerceAtLeast(15L)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<SmartKeepAliveWorker>(
            safeInterval,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SmartKeepAliveWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancelKeepAlive() {
        WorkManager.getInstance(context).cancelUniqueWork(SmartKeepAliveWorker.WORK_NAME)
    }
}
