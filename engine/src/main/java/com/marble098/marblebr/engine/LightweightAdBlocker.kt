package com.marble098.marblebr.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * بلوکر تبلیغ و ردیاب سبک برای مرورگر پایه.
 */
@Singleton
class LightweightAdBlocker @Inject constructor() {

    private val _blockedCount = MutableStateFlow(0L)
    val blockedCount: StateFlow<Long> = _blockedCount.asStateFlow()

    fun shouldBlockRequest(requestUrl: String, pageOriginUrl: String? = null, enabled: Boolean = true): Boolean {
        if (!enabled || requestUrl.isBlank()) return false

        val host = extractHost(requestUrl)
        if (host.isEmpty()) return false

        // Do not block first-party main navigation to the same domain
        val pageHost = pageOriginUrl?.let(::extractHost).orEmpty()
        if (pageHost.isNotEmpty() && host == pageHost) {
            return AD_PATH_PATTERNS.any { requestUrl.lowercase().contains(it) }
        }

        val isBlockedHost = BLOCKED_AD_DOMAINS.any { domain ->
            host == domain || host.endsWith(".$domain")
        }
        val isBlockedPath = AD_PATH_PATTERNS.any { pattern ->
            requestUrl.lowercase().contains(pattern)
        }

        if (isBlockedHost || isBlockedPath) {
            _blockedCount.value += 1
            return true
        }
        return false
    }

    private fun extractHost(url: String): String =
        runCatching {
            URI(url).host?.lowercase()?.removePrefix("www.").orEmpty()
        }.getOrDefault("")

    companion object {
        val BLOCKED_AD_DOMAINS = setOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "googletagmanager.com",
            "adnxs.com",
            "adsrvr.org",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "criteo.net",
            "scorecardresearch.com",
            "quantserve.com",
            "moatads.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "adform.net",
            "smartadserver.com",
            "popads.net",
            "propellerads.com",
            "yektanet.com",
            "tapsell.ir",
        )

        private val AD_PATH_PATTERNS = listOf(
            "/pagead/",
            "/ads/banners/",
            "/adserver/",
            "/doubleclick/",
        )
    }
}
