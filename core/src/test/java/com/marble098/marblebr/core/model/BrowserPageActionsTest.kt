package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowserPageActionsTest {
    @Test
    fun `clean share url removes tracking parameters but keeps useful query and fragment`() {
        assertEquals(
            "https://example.com/search?q=marble&page=2#results",
            BrowserPageActions.cleanShareUrl(
                "https://example.com/search?utm_source=newsletter&q=marble&fbclid=abc&page=2#results",
            ),
        )
    }

    @Test
    fun `clean share url handles encoded names and leaves unrelated urls alone`() {
        assertEquals(
            "https://example.com/path?x=1",
            BrowserPageActions.cleanShareUrl("https://example.com/path?%75tm_campaign=spring&x=1"),
        )
        assertEquals("mailto:hello@example.com", BrowserPageActions.cleanShareUrl("mailto:hello@example.com"))
        assertEquals("not a url", BrowserPageActions.cleanShareUrl("not a url"))
    }

    @Test
    fun `clean share url strips embedded credentials as well as tracking parameters`() {
        assertEquals(
            "https://example.com/private?next=%2Fhome",
            BrowserPageActions.cleanShareUrl("https://user:secret@example.com/private?utm_source=mail&next=%2Fhome"),
        )
    }

    @Test
    fun `https candidate is explicit and preserves path query fragment and non-default port`() {
        assertEquals(
            "https://neon.enbank.ir/login?next=%2Fhome#form",
            BrowserPageActions.httpsUpgradeCandidate("http://neon.enbank.ir/login?next=%2Fhome#form"),
        )
        assertEquals(
            "https://example.com:8080/path",
            BrowserPageActions.httpsUpgradeCandidate("http://example.com:8080/path"),
        )
        assertEquals("https://example.com/path", BrowserPageActions.httpsUpgradeCandidate("http://example.com:80/path"))
        assertNull(BrowserPageActions.httpsUpgradeCandidate("https://example.com/"))
        assertNull(BrowserPageActions.httpsUpgradeCandidate("http://user:secret@example.com/"))
        assertNull(BrowserPageActions.httpsUpgradeCandidate("javascript:alert(1)"))
    }

    @Test
    fun `page zoom changes in bounded ten percent steps`() {
        assertEquals(110, BrowserPageActions.stepZoom(100, 1))
        assertEquals(90, BrowserPageActions.stepZoom(100, -1))
        assertEquals(BrowserPageActions.MAX_ZOOM_PERCENT, BrowserPageActions.stepZoom(160, 1))
        assertEquals(BrowserPageActions.MIN_ZOOM_PERCENT, BrowserPageActions.stepZoom(80, -1))
        assertEquals(100, BrowserPageActions.clampZoom(100))
    }
}
