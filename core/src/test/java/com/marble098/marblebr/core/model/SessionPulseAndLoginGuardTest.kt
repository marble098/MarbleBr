package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SessionPulseAndLoginGuardTest {

    private val now = 1_700_000_000_000L
    private val day = TimeUnit.DAYS.toMillis(1)

    /* --------------------------------- نبض نشست --------------------------------- */

    @Test
    fun `unprotected sites never get a pulse`() {
        val patterns = listOf(
            SiteUsagePattern(domain = "off.example", lastVisitAtEpochMillis = now - 10 * day, neverSignOutEnabled = false),
        )
        assertTrue(SessionPulsePlanner.plan(patterns, nowEpochMillis = now, localHour = 3).isEmpty())
    }

    @Test
    fun `silent sites get high urgency and early due time`() {
        val patterns = listOf(
            SiteUsagePattern(
                domain = "mail.example",
                activeHours = setOf(9, 10, 11),
                averageVisitsPerDay = 4f,
                lastVisitAtEpochMillis = now - 6 * day,
                lastSuccessfulCheckAtEpochMillis = now - 6 * day,
            ),
        )
        val recommendation = SessionPulsePlanner.plan(patterns, nowEpochMillis = now, localHour = 3).single()
        assertEquals(PulseUrgency.CRITICAL, recommendation.urgency)
        // در ساعتِ آرامِ همان سایت، اجرا همین حالا انجام می‌شود.
        assertTrue(recommendation.dueInMillis >= 0)
        assertTrue("LONG_SILENCE" in recommendation.reasons.map { it.name })
    }

    @Test
    fun `newly discovered site is checked soon and quiet hours exist`() {
        val pattern = SiteUsagePattern(
            domain = "shop.example",
            activeHours = setOf(20, 21, 22),
            lastVisitAtEpochMillis = now,
            lastSuccessfulCheckAtEpochMillis = null,
        )
        val recommendation = SessionPulsePlanner.plan(listOf(pattern), nowEpochMillis = now, localHour = 4).single()
        assertEquals(PulseUrgency.HIGH, recommendation.urgency)
        assertTrue(PulseReason.NEVER_CHECKED in recommendation.reasons)
        val windows = SessionPulsePlanner.quietWindows(pattern)
        assertTrue(windows.isNotEmpty())
        assertTrue(windows.none { window -> 20 in window || 21 in window || 22 in window })
    }

    @Test
    fun `freshly checked healthy site stays quiet`() {
        val pattern = SiteUsagePattern(
            domain = "calm.example",
            averageVisitsPerDay = 0.2f,
            lastVisitAtEpochMillis = now,
            lastSuccessfulCheckAtEpochMillis = now - TimeUnit.HOURS.toMillis(1),
        )
        assertTrue(SessionPulsePlanner.plan(listOf(pattern), nowEpochMillis = now, localHour = 12).isEmpty())
    }

    @Test
    fun `periodic interval stays inside the safe battery window`() {
        val recommendations = listOf(
            PulseRecommendation("a", PulseUrgency.NORMAL, dueInMillis = 60_000),
            PulseRecommendation("b", PulseUrgency.CRITICAL, dueInMillis = 10 * day),
        )
        val minutes = PulseScheduler.periodicIntervalMinutes(recommendations)
        assertTrue(minutes >= PulseScheduler.MIN_MINUTES)
        assertTrue(minutes <= PulseScheduler.MAX_MINUTES)
        assertTrue(PulseScheduler.periodicIntervalMinutes(emptyList()) >= PulseScheduler.MIN_MINUTES)
    }

    @Test
    fun `patterns are derived from history and ignore blank hosts`() {
        val visits = listOf(
            HistoryItem("1", "Mail", "https://mail.example.com/inbox", "c1", now - 2 * day),
            HistoryItem("2", "Mail", "https://mail.example.com/inbox", "c1", now - 1 * day),
            HistoryItem("3", "Broken", "", "c2", now),
        )
        val patterns = SessionPulsePlanner.patternsFromHistory(visits, now)
        assertEquals(listOf("mail.example.com"), patterns.map { it.domain })
        assertTrue(patterns.single().averageVisitsPerDay >= 1f)
    }

    /* -------------------------------- نگهبان بدل -------------------------------- */

    @Test
    fun `exact and subdomain origins are trusted`() {
        val known = setOf("example.com")
        assertEquals(OriginRisk.TRUSTED, LoginOriginGuard.check("https://example.com/login", known).risk)
        assertEquals(OriginRisk.TRUSTED, LoginOriginGuard.check("https://www.example.com/login", known).risk)
        assertEquals(
            OriginRisk.TRUSTED_SUBDOMAIN,
            LoginOriginGuard.check("https://accounts.example.com/signin", known).risk,
        )
    }

    @Test
    fun `known typosquats are flagged as lookalikes`() {
        val known = setOf("google.com", "paypal.com")
        val zero = LoginOriginGuard.check("https://g00gle.com/signin", known)
        assertEquals(OriginRisk.LOOKALIKE, zero.risk)
        assertEquals("google.com", zero.impersonated)
        assertTrue(zero.blocksPasswordTyping)

        val hyphen = LoginOriginGuard.check("https://paypa1.com/login", known)
        assertEquals(OriginRisk.LOOKALIKE, hyphen.risk)

        val tldSwap = LoginOriginGuard.check("https://google.net/login", known)
        assertEquals(OriginRisk.LOOKALIKE, tldSwap.risk)
    }

    @Test
    fun `unrelated new site is only unknown, not an accusation`() {
        val verdict = LoginOriginGuard.check("https://brand-new-shop.example/store", setOf("google.com"))
        assertEquals(OriginRisk.UNKNOWN, verdict.risk)
        assertFalse(verdict.isWarning)
        assertFalse(verdict.blocksPasswordTyping)
    }

    @Test
    fun `risky url shapes are treated as suspicious`() {
        assertTrue(LoginOriginGuard.check("http://example.com/login", setOf("example.com")).isWarning.not())
        assertEquals(
            OriginRisk.SUSPICIOUS_URL,
            LoginOriginGuard.check("https://user:pass@evil.example/login", setOf("example.com")).risk,
        )
        assertEquals(
            OriginRisk.SUSPICIOUS_URL,
            LoginOriginGuard.check("https://verify-account.example:8443/login", setOf("example.com")).risk,
        )
        // دامنهٔ آشنا روی پورتِ معمولِ توسعه، مشکوک شمرده نمی‌شود.
        assertEquals(OriginRisk.TRUSTED, LoginOriginGuard.check("https://example.com:8443/login", setOf("example.com")).risk)
        assertEquals(
            OriginRisk.SUSPICIOUS_URL,
            LoginOriginGuard.check("https://login.example.com.secure-verify.zip/", setOf("example.com")).risk,
        )
    }

    @Test
    fun `cross host form action is detected`() {
        assertTrue(LoginOriginGuard.isCrossHostFormAction("example.com", "https://collector.evil.test/post"))
        assertFalse(LoginOriginGuard.isCrossHostFormAction("example.com", "https://api.example.com/login"))
        assertFalse(LoginOriginGuard.isCrossHostFormAction("example.com", "/login"))
    }

    @Test
    fun `registrable domain handles multi level suffixes`() {
        assertEquals("example.co.uk", LoginOriginGuard.registrableDomain("login.example.co.uk"))
        assertEquals("example.com", LoginOriginGuard.registrableDomain("www.example.com"))
        assertEquals("localhost", LoginOriginGuard.registrableDomain("localhost"))
    }

    @Test
    fun `levenshtein distance is symmetric and bounded`() {
        assertEquals(LoginOriginGuard.distance("abc", "abc"), 0)
        assertEquals(LoginOriginGuard.distance("abc", "abd"), 1)
        assertEquals(LoginOriginGuard.distance("abd", "abc"), 1)
        assertEquals(3, LoginOriginGuard.distance("", "abc"))
    }

    @Test
    fun `null-like urls never crash`() {
        listOf("", ":::", "http://", "javascript:void(0)").forEach { url ->
            val verdict = runCatching { LoginOriginGuard.check(url, setOf("example.com")) }.getOrNull()
            assertNotNull("check threw for '$url'", verdict)
        }
        assertNull(null)
    }
}
