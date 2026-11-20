package com.marble098.marblebr.core.model

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * تست‌های منطق دکمهٔ بازگشت و نشانِ سایت‌ها (بدون وابستگی به اندروید).
 */
class BackNavigationAndBadgeTest {

    // ------------------------------------------------ دکمهٔ بازگشت

    @Test
    fun `smart behavior goes back inside a page then returns home`() {
        assertEquals(
            BackTarget.PAGE_BACK,
            BackNavigationResolver.resolve(BackButtonBehavior.SMART, isOnHome = false, canGoBack = true, tabCount = 2, canExitApp = false),
        )
        assertEquals(
            BackTarget.HOME,
            BackNavigationResolver.resolve(BackButtonBehavior.SMART, isOnHome = false, canGoBack = false, tabCount = 2, canExitApp = false),
        )
        assertEquals(
            BackTarget.EXIT,
            BackNavigationResolver.resolve(BackButtonBehavior.SMART, isOnHome = true, canGoBack = false, tabCount = 2, canExitApp = true),
        )
    }

    @Test
    fun `always-home behavior never walks the page history`() {
        assertEquals(
            BackTarget.HOME,
            BackNavigationResolver.resolve(BackButtonBehavior.HOME, isOnHome = false, canGoBack = true, tabCount = 1, canExitApp = false),
        )
        assertEquals(
            BackTarget.EXIT,
            BackNavigationResolver.resolve(BackButtonBehavior.HOME, isOnHome = true, canGoBack = false, tabCount = 1, canExitApp = true),
        )
    }

    @Test
    fun `page-then-exit behavior never shows the home screen`() {
        assertEquals(
            BackTarget.PAGE_BACK,
            BackNavigationResolver.resolve(BackButtonBehavior.PAGE_THEN_EXIT, isOnHome = false, canGoBack = true, tabCount = 3, canExitApp = false),
        )
        assertEquals(
            BackTarget.EXIT,
            BackNavigationResolver.resolve(BackButtonBehavior.PAGE_THEN_EXIT, isOnHome = false, canGoBack = false, tabCount = 3, canExitApp = false),
        )
    }

    @Test
    fun `ask behavior only prompts when a choice actually exists`() {
        assertEquals(
            BackTarget.ASK,
            BackNavigationResolver.resolve(BackButtonBehavior.ASK, isOnHome = false, canGoBack = true, tabCount = 1, canExitApp = false),
        )
        // در خانه با یک تب، پرسیدن بی‌معنی است؛ مستقیم خارج می‌شویم.
        assertEquals(
            BackTarget.EXIT,
            BackNavigationResolver.resolve(BackButtonBehavior.ASK, isOnHome = true, canGoBack = false, tabCount = 1, canExitApp = true),
        )
    }

    @Test
    fun `ask options follow the current state`() {
        val optionsInsidePage = BackNavigationResolver.askOptions(isOnHome = false, canGoBack = true)
        assertEquals(listOf(BackTarget.PAGE_BACK, BackTarget.HOME, BackTarget.EXIT), optionsInsidePage)

        val optionsOnHome = BackNavigationResolver.askOptions(isOnHome = true, canGoBack = false)
        assertEquals(listOf(BackTarget.EXIT), optionsOnHome)
    }

    @Test
    fun `page-then-home walks back then shows home`() {
        assertEquals(
            BackTarget.PAGE_BACK,
            BackNavigationResolver.resolve(BackButtonBehavior.PAGE_THEN_HOME, isOnHome = false, canGoBack = true, tabCount = 1, canExitApp = false),
        )
        assertEquals(
            BackTarget.HOME,
            BackNavigationResolver.resolve(BackButtonBehavior.PAGE_THEN_HOME, isOnHome = false, canGoBack = false, tabCount = 1, canExitApp = false),
        )
    }

    // ------------------------------------------------ نشانِ سایت‌ها

    @Test
    fun `known sites get their recognizable monogram`() {
        assertEquals("GH", SiteBadge.of("GitHub", "github.com").monogram)
        assertEquals("X", SiteBadge.of("X", "x.com").monogram)
        assertEquals("YT", SiteBadge.of("YouTube", "youtube.com").monogram)
    }

    @Test
    fun `two-word names produce two letters`() {
        assertEquals("GD", SiteBadge.of("Google Docs", "docs.google.com").monogram)
    }

    @Test
    fun `unknown sites fall back to the first letters of the domain`() {
        assertEquals("Ma", SiteBadge.of("", "marble.dev").monogram)
        assertEquals("Ex", SiteBadge.of("", "example.org").monogram)
    }

    @Test
    fun `the same domain always gets the same colour seed`() {
        assertEquals(SiteBadge.of("A", "example.org").colorSeed, SiteBadge.of("B", "example.org").colorSeed)
        assertNotEquals(SiteBadge.of("A", "example.org").colorSeed, SiteBadge.of("A", "github.com").colorSeed)
        assertTrue(SiteBadge.of("A", "example.org").colorSeed >= 0)
    }

    @Test
    fun `www prefix is ignored`() {
        assertEquals(SiteBadge.of("", "example.org"), SiteBadge.of("", "www.example.org"))
    }
}

/**
 * تست پالت‌های رنگی: هر پالت باید هم روشن و هم تاریک ساخته شود و متنِ روی سطح‌ها
 * همیشه کنتراست کافی داشته باشد (نسبتِ روشنایی بین متن و پس‌زمینه).
 */
class PaletteEngineTest {

    @Test
    fun `every selectable palette builds light and dark schemes`() {
        com.marble098.marblebr.core.designsystem.SelectablePalettes.filter {
            it != com.marble098.marblebr.core.model.ColorPalette.DYNAMIC
        }.forEach { palette ->
            val light = com.marble098.marblebr.core.designsystem.paletteColorScheme(palette, dark = false)
            val dark = com.marble098.marblebr.core.designsystem.paletteColorScheme(palette, dark = true)
            assertNotEquals("$palette light scheme must have a primary", 0, light.primary.value.toInt())
            assertNotEquals("$palette dark scheme must have a primary", 0, dark.primary.value.toInt())
            assertTrue("$palette light bg should be bright", light.background.luminance() > 0.5f)
            assertTrue("$palette dark bg should be dark", dark.background.luminance() < 0.3f)
            assertTrue(
                "$palette light text contrast",
                abs(light.onBackground.luminance() - light.background.luminance()) > 0.4f,
            )
            assertTrue(
                "$palette dark text contrast",
                abs(dark.onBackground.luminance() - dark.background.luminance()) > 0.4f,
            )
        }
    }

    @Test
    fun `dynamic palette falls back to a real palette`() {
        val scheme = com.marble098.marblebr.core.designsystem.paletteColorScheme(
            com.marble098.marblebr.core.model.ColorPalette.DYNAMIC,
            dark = false,
        )
        assertTrue(scheme.primary.luminance() > 0f)
    }
}
