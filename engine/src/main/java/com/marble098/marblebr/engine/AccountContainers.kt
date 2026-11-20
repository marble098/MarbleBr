package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.WebAppContainer

/** Identity is independent of the site name and survives tab closure and process recreation. */
object AccountContainers {
    fun defaultId(domain: String): String = domain.replace(Regex("[^a-zA-Z0-9_-]"), "_")

    fun newAccount(source: WebAppContainer, name: String, nonce: String): WebAppContainer {
        require(name.isNotBlank() && nonce.isNotBlank())
        val id = "${defaultId(source.domain)}_account_$nonce"
        return source.copy(
            id = id,
            contextId = "marble_ctx_$id",
            displayName = name.trim(),
            isPinnedPwa = false,
            pwaStartUrl = null,
            lastActiveAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
