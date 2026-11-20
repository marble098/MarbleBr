package com.marble098.marblebr.feature.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAddressUrlResolverTest {

    private val resolver = SmartAddressUrlResolver()

    @Test
    fun `handles blank input port and query without loading an unintended site`() {
        assertEquals("about:blank", resolver.resolveInputToUrl("  "))
        assertEquals("https://example.org:8080?test=1", resolver.resolveInputToUrl("example.org:8080?test=1"))
        assertEquals("https://www.google.com/search?q=two+words", resolver.resolveInputToUrl("two words", "https://example.org"))
    }

    @Test
    fun `normalizes bare domains to https`() {
        assertEquals("https://github.com", resolver.resolveInputToUrl("github.com"))
        assertEquals("https://web.telegram.org/k/", resolver.resolveInputToUrl("web.telegram.org/k/"))
    }

    @Test
    fun `preserves explicit http and https urls`() {
        assertEquals("https://marble098.com/login", resolver.resolveInputToUrl("https://marble098.com/login"))
        assertEquals("http://192.168.1.1:8080", resolver.resolveInputToUrl("http://192.168.1.1:8080"))
    }

    @Test
    fun `converts multi-word or Persian queries into search engine url`() {
        val searchUrl = resolver.resolveInputToUrl("مرورگر بدون خروج از سایت")
        assertTrue(searchUrl.startsWith("https://www.google.com/search?q="))
    }
}
