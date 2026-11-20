package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.CookieRecord
import java.net.URI

/** Restrict background requests to the snapshot origin and send only cookies valid for that URL. */
object CookieRequestPolicy {
    private val cookieName = Regex("^[!#\$%&'*+.^_`|~0-9A-Za-z-]+$")

    fun targetUrl(origin: String, path: String): String? = runCatching {
        val base = URI(origin)
        if (base.scheme?.lowercase() !in setOf("http", "https") || base.host.isNullOrBlank() || base.userInfo != null) {
            return null
        }
        val safePath = if (path.startsWith("/")) path else "/$path"
        val target = base.resolve(safePath)
        if (target.scheme != base.scheme || target.host != base.host || target.port != base.port || target.userInfo != null) {
            return null
        }
        target.toASCIIString()
    }.getOrNull()

    fun cookiesForUrl(cookies: List<CookieRecord>, url: String, now: Long = System.currentTimeMillis()): List<CookieRecord> {
        val target = runCatching { URI(url) }.getOrNull() ?: return emptyList()
        val host = target.host?.lowercase() ?: return emptyList()
        val requestPath = target.rawPath.ifBlank { "/" }
        val isHttps = target.scheme.equals("https", ignoreCase = true)
        if (!isHttps && !target.scheme.equals("http", ignoreCase = true)) return emptyList()
        return cookies.filter { cookie ->
            val domain = cookie.domain.removePrefix(".").lowercase()
            val path = cookie.path.ifBlank { "/" }
            val domainMatches = host == domain || (!cookie.isHostOnly && host.endsWith(".$domain"))
            val pathMatches = requestPath == path ||
                (requestPath.startsWith(path) && (path.endsWith("/") || requestPath.getOrNull(path.length) == '/'))
            val expiry = cookie.expiresEpochMillis
            val safeValue = cookieName.matches(cookie.name) && cookie.value.none { it.code < 0x20 || it.code >= 0x7f || it == ';' }
            val securePrefixes = (!cookie.name.startsWith("__Secure-") || cookie.isSecure) &&
                (!cookie.name.startsWith("__Host-") || (cookie.isSecure && cookie.isHostOnly && path == "/"))
            val safeSameSite = cookie.sameSite.lowercase() !in setOf("none", "no_restriction") || cookie.isSecure
            domain.isNotBlank() && domainMatches && path.startsWith("/") && pathMatches && safeValue && securePrefixes && safeSameSite &&
                (!cookie.isSecure || isHttps) && (expiry == null || expiry > now)
        }
    }
}
