package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «افزودن هر سایتی»: هر ورودی باید به یک نقشهٔ راه برسد، نه به یک «نه».
 */
class SiteCompatibilityTest {

    @Test
    fun `a plain password site is added directly`() {
        val plan = SiteAdditionPlanner.plan(
            url = "https://example.com/account",
            observation = LoginPageObservation(passwordFieldCount = 1, usernameFieldCount = 1),
        )
        assertEquals("example.com", plan.domain)
        assertEquals(SiteRoute.DIRECT, plan.best?.route)
        assertEquals("https://example.com/account", plan.best?.targetUrl)
        assertTrue(plan.assessment.supportsPasswordAutofill)
    }

    @Test
    fun `every site always gets at least one route even with a hostile page`() {
        val hostile = LoginPageObservation(
            loginIframePresent = true,
            differentOriginIframeCount = 2,
            browserRefusalText = true,
            oneTimeCodeFieldCount = 1,
            lastLoadFailed = true,
        )
        val plan = SiteAdditionPlanner.plan("weird.example", hostile)
        assertTrue(plan.steps.isNotEmpty())
        // «نه» وجود ندارد: مسیر دستی و پنجرهٔ مستقل باید در فهرست باشند.
        assertTrue(SiteRoute.VAULT_MANUAL_LOGIN in plan.routeSummary())
        assertTrue(SiteRoute.PWA_WINDOW in plan.routeSummary())
        assertTrue(SiteBlocker.IFRAME_LOGIN in plan.assessment.blockers)
        assertTrue(SiteBlocker.BROWSER_REFUSED in plan.assessment.blockers)
        assertTrue(SiteBlocker.ONE_TIME_CODE_ONLY in plan.assessment.blockers)
        assertTrue(plan.hasFallbacks)
    }

    @Test
    fun `failed routes are not suggested again`() {
        val plan = SiteAdditionPlanner.plan(
            url = "https://shop.example",
            failedRoutes = setOf(SiteRoute.DIRECT, SiteRoute.DESKTOP_USER_AGENT),
        )
        assertNotEquals(SiteRoute.DIRECT, plan.best?.route)
        assertFalse(SiteRoute.DIRECT in plan.routeSummary())
        assertFalse(SiteRoute.DESKTOP_USER_AGENT in plan.routeSummary())
    }

    @Test
    fun `when nothing remains the direct route is the safe default`() {
        val all = SiteRoute.entries.toSet()
        val plan = SiteAdditionPlanner.plan("https://example.org", failedRoutes = all)
        assertEquals(listOf(SiteRoute.DIRECT), plan.routeSummary())
    }

    @Test
    fun `desktop preference moves the desktop route to the front`() {
        val plan = SiteAdditionPlanner.plan("https://bank.example", preferDesktop = true)
        assertEquals(SiteRoute.DESKTOP_USER_AGENT, plan.best?.route)
        assertTrue(plan.best?.useDesktopUserAgent == true)
    }

    @Test
    fun `login method detector ranks password above social login`() {
        val assessment = LoginMethodDetector.assess(
            LoginPageObservation(passwordFieldCount = 1, googleButtonPresent = true, appleButtonPresent = true),
        )
        assertEquals(LoginMethod.PASSWORD, assessment.primary)
        assertTrue(LoginMethod.GOOGLE in assessment.alternatives)
        assertTrue(LoginMethod.APPLE in assessment.alternatives)
    }

    @Test
    fun `passkey only site is detected and needs manual help`() {
        val assessment = LoginMethodDetector.assess(LoginPageObservation(passkeyButtonPresent = true))
        assertEquals(LoginMethod.PASSKEY, assessment.primary)
        assertTrue(assessment.needsManualHelp)
        assertFalse(assessment.supportsPasswordAutofill)
    }

    @Test
    fun `unknown page has unknown method but no crash`() {
        val assessment = LoginMethodDetector.assess(LoginPageObservation.EMPTY)
        assertEquals(LoginMethod.UNKNOWN, assessment.primary)
        assertEquals(listOf(SiteBlocker.NONE), assessment.blockers)
    }

    @Test
    fun `url variants handle http, www, mobile and single label hosts`() {
        val www = UrlVariantResolver.variants("https://www.example.com/a/b?x=1")
        assertEquals("https://www.example.com/a/b?x=1", www.direct)
        assertEquals("https://example.com/a/b?x=1", www.canonicalHost)
        assertEquals("https://m.example.com/a/b?x=1", www.mobileHost)

        val mobile = UrlVariantResolver.variants("https://m.example.com")
        assertNull(mobile.mobileHost)

        val bare = UrlVariantResolver.variants("example.com")
        assertEquals("https://example.com/", bare.direct)

        val localhost = UrlVariantResolver.variants("http://localhost:8080/app")
        assertNull(localhost.mobileHost)
    }

    @Test
    fun `domain extraction never throws`() {
        assertEquals("example.com", SiteAdditionPlanner.domainOf("https://WWW.Example.com/x"))
        val samples = listOf("", "http://", ":::", "javascript:alert(1)", "https://a b c")
        samples.forEach { sample ->
            runCatching { SiteAdditionPlanner.domainOf(sample) }
                .onFailure { throw AssertionError("planner threw for '$sample'", it) }
        }
    }

    @Test
    fun `desktop route preference and pwa detection are consistent`() {
        assertTrue(SiteAdditionPlanner.routeRequiresPwa(SiteRoute.PWA_WINDOW))
        assertFalse(SiteAdditionPlanner.routeRequiresPwa(SiteRoute.DIRECT))
    }
}
