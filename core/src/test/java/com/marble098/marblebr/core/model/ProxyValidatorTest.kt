package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اعتبارسنجیِ پروکسی: هر میزبان/پورتِ محلی یا ریموت با هر پورتِ معتبر باید پذیرفته شود.
 *
 * پیش‌تر پورتِ نامعتبر بی‌صدا با ۱۰۸۰ جایگزین می‌شد و میزبانِ خالی ذخیره می‌شد؛ نتیجه یک
 * پروکسیِ «ذخیره‌شده» بود که هرگز کار نمی‌کرد.
 */
class ProxyValidatorTest {

    @Test
    fun `no proxy is always valid`() {
        assertTrue(ProxyValidator.validate(ProxyType.NONE, "", 0).isValid)
        assertTrue(ProxyValidator.validate(ProxyType.NONE, "garbage!!", 99_999).isValid)
    }

    @Test
    fun `local and remote hosts with any valid port are accepted`() {
        assertTrue(ProxyValidator.validate(ProxyType.HTTP, "127.0.0.1", 8080).isValid)
        assertTrue(ProxyValidator.validate(ProxyType.SOCKS5, "127.0.0.1", 1080).isValid)
        assertTrue(ProxyValidator.validate(ProxyType.HTTP, "proxy.example.com", 1).isValid)
        assertTrue(ProxyValidator.validate(ProxyType.SOCKS5, "10.0.0.5", 65535).isValid)
        assertTrue(ProxyValidator.validate(ProxyType.HTTP, "[2001:db8::1]", 443).isValid)
    }

    @Test
    fun `out of range ports are rejected`() {
        val tooBig = ProxyValidator.validate(ProxyType.HTTP, "proxy.example.com", 70_000)
        assertFalse(tooBig.isValid)
        assertTrue(tooBig.portInvalid)

        val zero = ProxyValidator.validate(ProxyType.SOCKS5, "proxy.example.com", 0)
        assertTrue(zero.portInvalid)
    }

    @Test
    fun `blank or malformed hosts are rejected`() {
        val blank = ProxyValidator.validate(ProxyType.HTTP, "   ", 8080)
        assertTrue(blank.hostBlank)

        val bad = ProxyValidator.validate(ProxyType.HTTP, "bad host!", 8080)
        assertFalse(bad.isValid)
        assertTrue(bad.hostInvalid)
    }

    @Test
    fun `scheme and port suffix are stripped from the host`() {
        assertEquals("proxy.example.com", ProxyValidator.normalizeHost("https://proxy.example.com:8443/path"))
        assertEquals("proxy.example.com", ProxyValidator.normalizeHost("www.proxy.example.com"))
        assertEquals("127.0.0.1", ProxyValidator.normalizeHost("127.0.0.1:8080"))
        assertEquals("[2001:db8::1]", ProxyValidator.normalizeHost("[2001:db8::1]:443"))
        assertEquals("host", ProxyValidator.normalizeHost("socks5://host"))
    }

    @Test
    fun `hasUsableProxy follows validation`() {
        assertFalse(ProxyDnsConfig().hasUsableProxy())
        assertFalse(ProxyDnsConfig(proxyType = ProxyType.HTTP, proxyHost = "", proxyPort = 8080).hasUsableProxy())
        assertFalse(ProxyDnsConfig(proxyType = ProxyType.HTTP, proxyHost = "p.example.com", proxyPort = 0).hasUsableProxy())
        assertTrue(
            ProxyDnsConfig(
                proxyType = ProxyType.HTTP,
                proxyHost = "p.example.com",
                proxyPort = 8080,
            ).hasUsableProxy(),
        )
    }
}
