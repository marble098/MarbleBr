package com.marble098.marblebr.core.model

import java.net.URI
import java.net.URLDecoder

/** Page-level browser actions that do not rewrite or upload page content. */
object BrowserPageActions {
    const val MIN_ZOOM_PERCENT = 80
    const val MAX_ZOOM_PERCENT = 160
    const val ZOOM_STEP_PERCENT = 10

    private val trackingParameterNames = setOf(
        "fbclid",
        "gclid",
        "dclid",
        "gbraid",
        "wbraid",
        "msclkid",
        "mc_cid",
        "mc_eid",
        "igshid",
        "vero_id",
        "oly_anon_id",
        "oly_enc_id",
        "_hsenc",
        "_hsmi",
        "ref_src",
    )

    /** Remove credentials and common tracking parameters without changing useful query data. */
    fun cleanShareUrl(url: String): String {
        val input = url.trim()
        val uri = runCatching { URI(input) }.getOrNull() ?: return url
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return url
        val safeInput = if (uri.rawUserInfo == null) {
            input
        } else {
            val authority = uri.rawAuthority?.substringAfterLast('@') ?: return url
            buildString {
                append(uri.scheme)
                append("://")
                append(authority)
                append(uri.rawPath.orEmpty())
                uri.rawQuery?.let { append('?'); append(it) }
                uri.rawFragment?.let { append('#'); append(it) }
            }
        }
        val question = safeInput.indexOf('?')
        if (question < 0) return safeInput
        val fragment = safeInput.indexOf('#', startIndex = question).let { if (it < 0) safeInput.length else it }
        val rawQuery = safeInput.substring(question + 1, fragment)
        val parts = rawQuery.split('&').filter(String::isNotBlank)
        val retained = parts.filterNot { part ->
            val rawName = part.substringBefore('=')
            val name = runCatching {
                URLDecoder.decode(rawName.replace("+", "%2B"), Charsets.UTF_8.name())
            }.getOrDefault(rawName).lowercase()
            name.startsWith("utm_") || name in trackingParameterNames
        }
        if (retained.size == parts.size) return safeInput
        val beforeQuery = safeInput.substring(0, question)
        val suffix = safeInput.substring(fragment)
        return if (retained.isEmpty()) "$beforeQuery$suffix" else "$beforeQuery?${retained.joinToString("&")}$suffix"
    }

    /** Return an explicit HTTPS candidate only for a valid HTTP web URL. Never silently redirect. */
    fun httpsUpgradeCandidate(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("http", ignoreCase = true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
        val authority = uri.rawAuthority ?: return null
        val secureAuthority = if (uri.port == 80) authority.removeSuffix(":80") else authority
        return buildString {
            append("https://")
            append(secureAuthority)
            append(uri.rawPath.orEmpty())
            uri.rawQuery?.let { append('?'); append(it) }
            uri.rawFragment?.let { append('#'); append(it) }
        }
    }

    fun clampZoom(percent: Int): Int = percent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)

    fun stepZoom(currentPercent: Int, direction: Int): Int {
        if (direction == 0) return clampZoom(currentPercent)
        val step = if (direction > 0) ZOOM_STEP_PERCENT else -ZOOM_STEP_PERCENT
        return clampZoom(currentPercent + step)
    }
}
