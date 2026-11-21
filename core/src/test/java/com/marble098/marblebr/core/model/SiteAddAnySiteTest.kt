package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «افزودن هر سایتی» — هیچ آدرسی رد نمی‌شود.
 *
 * این تست روی رفتارِ کاربر متمرکز است: هر آدرسی که آدم معمولی تایپ می‌کند باید دست‌کم یک راهِ
 * بازکردن داشته باشد و پیامِ «نمی‌شود» هیچ‌جا ساخته نشود.
 */
class SiteAddAnySiteTest {

    @Test
    fun everyAddressProducesAtLeastOneRoute() {
        val addresses = listOf(
            "example.com",
            "https://example.com/login",
            "http://old.example.org/panel",
            "app.example.ir:8443/sso",
            "https://www.example.co.uk/account",
            "shop.example/deep/path?x=1",
            "http://m.example.com/",
            "https://xn--mgbaam7a8h.example/",
        )
        addresses.forEach { address ->
            val plan = SiteAdditionPlanner.plan(address)
            assertTrue("$address should have a route", plan.steps.isNotEmpty())
            assertNotNull(plan.best)
        }
    }

    @Test
    fun blockedLoginStillOffersManualVaultRoute() {
        val observation = LoginPageObservation(
            passwordFieldCount = 0,
            loginIframePresent = true,
            differentOriginIframeCount = 2,
        )
        val plan = SiteAdditionPlanner.plan("https://bank.example/login", observation)
        assertTrue(SiteRoute.VAULT_MANUAL_LOGIN in plan.routeSummary())
        assertEquals(LoginMethod.UNKNOWN, plan.assessment.primary)
    }

    @Test
    fun browserRefusalFallsBackToDesktopAndReadOnly() {
        val observation = LoginPageObservation(browserRefusalText = true)
        val plan = SiteAdditionPlanner.plan("https://legacy.example/", observation)
        val routes = plan.routeSummary()
        assertTrue(routes.contains(SiteRoute.DESKTOP_USER_AGENT))
        assertTrue(routes.contains(SiteRoute.READ_ONLY_SNAPSHOT))
        assertTrue(
            plan.steps.first { it.route == SiteRoute.DESKTOP_USER_AGENT }.useDesktopUserAgent,
        )
    }

    @Test
    fun failedRoutesAreSkippedUntilLadderExhausts() {
        val first = SiteAdditionPlanner.plan("https://example.com/")
        val withoutDirect = SiteAdditionPlanner.plan(
            "https://example.com/",
            failedRoutes = setOf(first.steps.first().route),
        )
        assertFalse(withoutDirect.steps.any { it.route == first.steps.first().route })
        assertTrue(withoutDirect.steps.isNotEmpty())

        val noOptions = SiteAdditionPlanner.plan(
            "https://example.com/",
            failedRoutes = SiteRoute.entries.toSet(),
        )
        // حتی وقتی همهٔ راه‌ها شکست خورده‌اند، یک راهِ مستقیم برای «دوباره تلاش کن» می‌ماند.
        assertEquals(listOf(SiteRoute.DIRECT), noOptions.routeSummary())
    }

    @Test
    fun shortAddressIsAcceptedAsSite() {
        assertEquals("example.com", SiteAdditionPlanner.domainOf("example.com"))
        assertEquals("example.com", SiteAdditionPlanner.domainOf("https://www.example.com/login"))
        assertEquals("my.mci.ir", SiteAdditionPlanner.domainOf("my.mci.ir"))
    }

    @Test
    fun urlVariantsNeverLoseTheOriginalAddress() {
        // آدرسِ کاملِ کاربر همیشه دست‌نخورده می‌ماند و نسخهٔ دسکتاپ/موبایل کنارش ساخته می‌شود.
        assertEquals("https://example.com/", UrlVariantResolver.variants("https://example.com/").direct)
        assertEquals("https://m.example.com/", UrlVariantResolver.variants("https://m.example.com/").direct)
        assertEquals(
            "https://example.com/login",
            UrlVariantResolver.variants("https://www.example.com/login").canonicalHost,
        )
        assertEquals(
            "https://m.example.com/login",
            UrlVariantResolver.variants("https://example.com/login").mobileHost,
        )
        // سایت‌های تک‌برچسبی (مثل سرورِ محلی) نسخهٔ موبایل ندارند.
        assertEquals(null, UrlVariantResolver.variants("localhost:8080").mobileHost)
    }
}
