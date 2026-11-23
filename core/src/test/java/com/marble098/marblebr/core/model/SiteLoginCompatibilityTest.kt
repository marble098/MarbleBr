package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «حالت سازگاری ورود»: سایت‌های بانکی باید مسیرِ ورودشان باز باشد، ولی این حالت نباید روی
 * سایت‌های معمولی روشن شود.
 */
class SiteLoginCompatibilityTest {

    @Test
    fun `known bank host and its subdomains are sensitive`() {
        assertTrue(SiteLoginCompatibility.isSensitiveHost("enbank.ir"))
        assertTrue(SiteLoginCompatibility.isSensitiveHost("neon.enbank.ir"))
        assertTrue(SiteLoginCompatibility.isSensitiveHost("www.bankmellat.ir"))
        assertTrue(SiteLoginCompatibility.isSensitiveHost("sep.shaparak.ir"))
        assertTrue(SiteLoginCompatibility.isSensitiveHost("my.sb24.ir"))
    }

    @Test
    fun `ordinary sites are not relaxed automatically`() {
        assertFalse(SiteLoginCompatibility.isSensitiveHost("example.com"))
        assertFalse(SiteLoginCompatibility.isSensitiveHost("github.com"))
        assertFalse(SiteLoginCompatibility.isSensitiveHost("notabank.example"))
        assertFalse(SiteLoginCompatibility.isSensitiveHost(""))
    }

    @Test
    fun `host is parsed from every url shape`() {
        assertEquals("enbank.ir", SiteLoginCompatibility.hostOf("https://neon.enbank.ir/login?x=1"))
        assertEquals("enbank.ir", SiteLoginCompatibility.hostOf("https://www.enbank.ir"))
        assertEquals("enbank.ir", SiteLoginCompatibility.hostOf("neon.enbank.ir/login"))
        assertEquals("", SiteLoginCompatibility.hostOf("   "))
    }

    @Test
    fun `a bank url gets a relaxed plan without a manual request`() {
        val plan = SiteLoginCompatibility.planFor("https://neon.enbank.ir/")
        assertTrue(plan.relaxedContentBlocking)
        assertTrue(plan.allowPopups)
        assertTrue(plan.isRelaxed)
        assertEquals(LoginCompatibilityReason.KNOWN_FINANCIAL_HOST, plan.reason)
    }

    @Test
    fun `a normal url stays standard`() {
        val plan = SiteLoginCompatibility.planFor("https://example.com/account")
        assertFalse(plan.relaxedContentBlocking)
        assertFalse(plan.allowPopups)
        assertFalse(plan.isRelaxed)
        assertEquals(LoginCompatibilityReason.NONE, plan.reason)
    }

    @Test
    fun `a manual request relaxes any site`() {
        val plan = SiteLoginCompatibility.planFor("https://example.com/account", manual = true)
        assertTrue(plan.relaxedContentBlocking)
        assertTrue(plan.allowPopups)
        assertEquals(LoginCompatibilityReason.MANUAL, plan.reason)
    }

    @Test
    fun `automatic detection can be switched off by the user`() {
        val plan = SiteLoginCompatibility.planFor("https://neon.enbank.ir/", automatic = false)
        assertFalse(plan.isRelaxed)
        assertEquals(LoginCompatibilityReason.NONE, plan.reason)
    }
}
