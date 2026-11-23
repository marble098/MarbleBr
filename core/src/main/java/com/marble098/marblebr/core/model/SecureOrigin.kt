package com.marble098.marblebr.core.model

import java.net.URI

/** A credential is scoped to an HTTPS origin, not a registrable domain or a URL path. */
data class SecureOrigin private constructor(val host: String, val port: Int) {
    override fun toString(): String = "https://$host${if (port == 443) "" else ":$port"}"

    companion object {
        /** Bare hosts are accepted only for addresses explicitly saved by the user. */
        fun from(value: String, allowBareHost: Boolean = false): SecureOrigin? = runCatching {
            val input = value.trim()
            if (input.isEmpty()) return null
            val url = if (allowBareHost && !input.contains("://")) "https://$input" else input
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null || host.isBlank()) return null
            val port = if (uri.port == -1) 443 else uri.port
            if (port !in 1..65535) return null
            SecureOrigin(host, port)
        }.getOrNull()
    }
}
