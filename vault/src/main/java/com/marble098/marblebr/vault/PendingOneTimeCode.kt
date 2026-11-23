package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.OneTimeCodeExtractor
import com.marble098.marblebr.core.model.OneTimeCodeHit
import com.marble098.marblebr.core.model.OneTimeCodeSource
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicReference

/** In-memory identity for consuming exactly the code that was actually filled. */
internal data class CodeIdentity(val fingerprint: String, val receivedAtEpochMillis: Long, val source: OneTimeCodeSource)

internal fun OneTimeCodeHit.identity(): CodeIdentity =
    CodeIdentity(OneTimeCodeExtractor.fingerprint(code), receivedAtEpochMillis, source)

internal class PendingOneTimeCode {
    private val current = AtomicReference<OneTimeCodeHit?>(null)

    fun offer(hit: OneTimeCodeHit): Boolean {
        while (true) {
            val previous = current.get()
            if (previous?.identity() == hit.identity()) return false
            if (current.compareAndSet(previous, hit)) return true
        }
    }

    fun peek(): OneTimeCodeHit? = current.get()

    fun clear() {
        current.set(null)
    }

    fun consumeMatching(identity: CodeIdentity): Boolean {
        while (true) {
            val hit = current.get() ?: return false
            if (hit.identity() != identity) return false
            if (current.compareAndSet(hit, null)) return true
        }
    }
}

internal fun MutableStateFlow<OneTimeCodeHit?>.consumeMatching(identity: CodeIdentity): Boolean {
    while (true) {
        val hit = value ?: return false
        if (hit.identity() != identity) return false
        if (compareAndSet(hit, null)) return true
    }
}
