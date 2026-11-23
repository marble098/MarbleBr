package com.marble098.marblebr.feature.browser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * جستجوی منوی سه‌نقطه.
 *
 * شکایتِ کاربر: «برای پیدا کردنِ گزینهٔ دلخواهم باید دو ساعت اسکرول کنم.» این تست همان رفتارِ
 * فیلتر را می‌سنجد که زیرِ کادرِ جستجوی منو اجرا می‌شود (بدونِ نیاز به Compose یا دستگاه).
 */
class BrowserMenuSearchTest {

    @Test
    fun `an empty query keeps everything`() {
        assertTrue(menuEntryMatches("", "Save right now", "We make a safe copy of your login"))
        assertTrue(menuEntryMatches("   ", "Download", null))
    }

    @Test
    fun `matching is case insensitive and covers the title`() {
        assertTrue(menuEntryMatches("FIND", "Find on this page", null))
        assertTrue(menuEntryMatches("find", "Find on this page", null))
    }

    @Test
    fun `matching also covers the subtitle of a row`() {
        // کاربر «کد» را می‌نویسد و باید به «گذاشتنِ کدِ یکبارمصرف» برسد.
        assertTrue(menuEntryMatches("کد", "رمز یکبارمصرف", "کد پیامکی را خودت وارد کن"))
        assertFalse(menuEntryMatches("کد", "رمز یکبارمصرف", "چیز دیگری"))
    }

    @Test
    fun `a row without subtitle never matches on a blank subtitle`() {
        assertFalse(menuEntryMatches("download", "Settings", null))
    }

    @Test
    fun `section filter drops empty sections and keeps the rest`() {
        val page = MenuSection(
            title = "This page",
            entries = listOf(
                MenuEntry(
                    tag = "menu_find_in_page",
                    icon = Icons.Default.Search,
                    title = "Find on this page",
                    accent = Color.Unspecified,
                    onClick = {},
                ),
            ),
        )
        val protection = MenuSection(
            title = "Protection",
            entries = listOf(
                MenuEntry(
                    tag = "menu_login_compatibility",
                    icon = Icons.Default.Search,
                    title = "Login compatibility",
                    subtitle = "Turn this on if the sign-in button does nothing",
                    accent = Color.Unspecified,
                    onClick = {},
                ),
            ),
        )

        val filtered = filterMenuSections(listOf(page, protection), "sign-in")

        assertEquals(1, filtered.size)
        assertEquals("Protection", filtered.first().title)
        assertEquals("menu_login_compatibility", filtered.first().entries.first().tag)
        assertTrue(filterMenuSections(listOf(page, protection), "").size == 2)
        assertTrue(filterMenuSections(listOf(page, protection), "nothing-matches").isEmpty())
    }
}
