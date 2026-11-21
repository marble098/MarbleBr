package com.marble098.marblebr.feature.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های کمک‌تابع‌های صفحهٔ خانه.
 *
 * [siteDomain] ورودیِ کاربر (نشانیِ کامل، دامنهٔ خالی از پروتکل یا مقدار خراب) را به دامنهٔ
 * کوتاه تبدیل می‌کند؛ خروجی همین تابع هم روی کارت میانبر و هم روی لوگوی سایت استفاده می‌شود،
 * پس یک اشتباه در آن همهٔ کارت‌ها را خراب می‌کند.
 */
class HomeScreenHelpersTest {

    @Test
    fun `strips scheme and www and keeps the host`() {
        assertEquals("github.com", siteDomain("https://github.com/marble098/MarbleBr"))
        assertEquals("github.com", siteDomain("https://www.github.com"))
        assertEquals("web.telegram.org", siteDomain("web.telegram.org"))
    }

    @Test
    fun `lowercases hosts and keeps subdomains`() {
        assertEquals("mail.google.com", siteDomain("http://Mail.Google.com/inbox"))
        assertEquals("example.org", siteDomain("EXAMPLE.ORG"))
    }

    @Test
    fun `falls back to the raw-ish host for unusual input`() {
        assertEquals("", siteDomain(""))
        // ورودیِ بدون دامنهٔ معتبر نباید استثنا بدهد و نباید رشتهٔ خالیِ بی‌معنا برگرداند.
        assertFalse(siteDomain("about:blank").startsWith("https://"))
        assertTrue(siteDomain("https://github.com").isNotEmpty())
    }

    @Test
    fun `never throws for malformed urls`() {
        val samples = listOf("http://", "https://??", "javascript:alert(1)", ":::", "https://a b c")
        samples.forEach { sample ->
            runCatching { siteDomain(sample) }.onFailure { error ->
                throw AssertionError("siteDomain threw for '$sample'", error)
            }
        }
    }

    @Test
    fun `login method and blocker labels never return blanks`() {
        val strings = com.marble098.marblebr.core.i18n.MarbleStrings.forLanguage(
            com.marble098.marblebr.core.i18n.AppLanguage.PERSIAN,
        )
        com.marble098.marblebr.core.model.LoginMethod.entries.forEach { method ->
            assertTrue("missing label for $method", loginMethodLabel(method, strings).isNotBlank())
        }
        com.marble098.marblebr.core.model.SiteBlocker.entries.forEach { blocker ->
            assertTrue("missing label for $blocker", blockerLabel(blocker, strings).isNotBlank())
        }
        com.marble098.marblebr.core.model.PasswordStrengthLevel.entries.forEach { level ->
            assertTrue("missing label for $level", strengthLabel(level, strings).isNotBlank())
        }
    }
}
