package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureOriginTest {
    @Test
    fun `scheme host and effective port define the origin`() {
        val origin = SecureOrigin.from("https://Example.com/login")
        assertEquals(origin, SecureOrigin.from("https://example.com:443/other?x=1"))
        assertNotEquals(origin, SecureOrigin.from("https://www.example.com/login"))
        assertNotEquals(origin, SecureOrigin.from("https://example.com:8443/login"))
        assertNotEquals(origin, SecureOrigin.from("https://accounts.example.com/login"))
        assertNull(SecureOrigin.from("http://example.com/login"))
        assertNull(SecureOrigin.from("https://example.com@evil.test/login"))
        assertNull(SecureOrigin.from("https://example.com:99999/login"))
        assertNull(SecureOrigin.from("javascript:alert(1)"))
    }

    @Test
    fun `bare saved sites require explicit permission and preserve ports`() {
        assertNull(SecureOrigin.from("www.example.com:8443/login"))
        assertEquals("https://www.example.com:8443", SecureOrigin.from("www.example.com:8443/login", true).toString())
        assertEquals("https://example.com", SecureOrigin.from("EXAMPLE.com", true).toString())
    }
}
