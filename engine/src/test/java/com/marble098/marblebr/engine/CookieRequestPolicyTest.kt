package com.marble098.marblebr.engine

import com.marble098.marblebr.core.model.CookieRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CookieRequestPolicyTest {
    @Test
    fun keepAliveCannotChangeOrigin() {
        assertEquals("https://my.mci.ir/panel", CookieRequestPolicy.targetUrl("https://my.mci.ir", "/panel"))
        assertNull(CookieRequestPolicy.targetUrl("https://my.mci.ir", "//attacker.example/"))
        assertNull(CookieRequestPolicy.targetUrl("https://user@my.mci.ir", "/"))
    }

    @Test
    fun onlyCookiesMatchingHostPathSecurityAndExpiryAreSent() {
        val valid = CookieRecord("session", "one", "my.mci.ir", path = "/panel", isSecure = true)
        val cookies = listOf(
            valid,
            valid.copy(name = "other-host", domain = "other.mci.ir"),
            valid.copy(name = "wrong-path", path = "/admin"),
            valid.copy(name = "expired", expiresEpochMillis = 10),
            valid.copy(name = "bad-value", value = "x\r\nheader"),
            valid.copy(name = "bad name"),
            valid.copy(name = "null-byte", value = "value\u0000header"),
            valid.copy(name = "__Host-token", isHostOnly = false),
            valid.copy(name = "__Secure-token", isSecure = false),
            valid.copy(name = "samesite-none", isSecure = false, sameSite = "None"),
        )
        assertEquals(listOf(valid), CookieRequestPolicy.cookiesForUrl(cookies, "https://my.mci.ir/panel/view", now = 100))
        assertEquals(emptyList<CookieRecord>(), CookieRequestPolicy.cookiesForUrl(listOf(valid), "http://my.mci.ir/panel", now = 100))
        assertEquals(emptyList<CookieRecord>(), CookieRequestPolicy.cookiesForUrl(listOf(valid), "https://my.mci.ir/panel2", now = 100))
        assertEquals(emptyList<CookieRecord>(), CookieRequestPolicy.cookiesForUrl(listOf(valid), "https://x.my.mci.ir/panel", now = 100))
        assertEquals(1, CookieRequestPolicy.cookiesForUrl(listOf(valid.copy(isHostOnly = false)), "https://x.my.mci.ir/panel").size)
    }
}
