package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * پیشنهادهای نوار آدرس: تاریخچه، نشان‌ها و پربازدیدها باید در یک فهرستِ مرتب ادغام شوند.
 *
 * این آزمون‌ها رفتارِ موتور را بدون هیچ وابستگی به اندروید بررسی می‌کنند (همان چیزی که
 * نوار آدرس و جستجوی صفحهٔ خانه هر دو استفاده می‌کنند).
 */
class AddressSuggestionEngineTest {

    private val now = 1_700_000_000_000L

    private fun history(
        url: String,
        title: String = titleOf(url),
        visits: Int = 1,
        ageDays: Int = 0,
    ) = HistoryItem(
        id = url,
        title = title,
        url = url,
        containerId = "site",
        visitedAtEpochMillis = now - ageDays * 86_400_000L,
        visitCount = visits,
    )

    private fun titleOf(url: String) = url.removePrefix("https://").removePrefix("www.")

    @Test
    fun `empty query returns most visited first`() {
        val suggestions = AddressSuggestionEngine.build(
            query = "",
            history = listOf(
                history("https://github.com", visits = 1),
                history("https://mail.google.com", visits = 9),
                history("https://news.ycombinator.com", visits = 4),
            ),
            bookmarks = emptyList(),
        )
        assertTrue(suggestions.isNotEmpty())
        assertEquals("https://mail.google.com", suggestions.first().url)
        assertEquals(AddressSuggestionSource.MOST_VISITED, suggestions.first().source)
    }

    @Test
    fun `typing matches host prefix before a deeper path`() {
        val suggestions = AddressSuggestionEngine.build(
            query = "github",
            history = listOf(
                history("https://gist.github.com/some/long/path", visits = 2),
                history("https://github.com", visits = 1),
            ),
            bookmarks = emptyList(),
        )
        assertEquals("https://github.com", suggestions.first().url)
        assertTrue(suggestions.any { it.url.contains("gist.github.com") })
    }

    @Test
    fun `bookmarks are merged with history and win a bonus`() {
        val suggestions = AddressSuggestionEngine.build(
            query = "docs",
            history = listOf(history("https://example.com/docs/old", visits = 1)),
            bookmarks = listOf(
                BookmarkItem(id = "b1", title = "Kotlin docs", url = "https://kotlinlang.org/docs/home.html"),
            ),
        )
        assertEquals("https://kotlinlang.org/docs/home.html", suggestions.first().url)
        assertEquals(AddressSuggestionSource.BOOKMARK, suggestions.first().source)
    }

    @Test
    fun `same url is merged into a single suggestion`() {
        val suggestions = AddressSuggestionEngine.build(
            query = "",
            history = listOf(history("https://example.com/a", visits = 2), history("https://example.com/a", visits = 3)),
            bookmarks = listOf(BookmarkItem(id = "b", title = "A", url = "https://www.example.com/a")),
        )
        assertEquals(1, suggestions.size)
        assertEquals(5, suggestions.first().visitCount)
        assertEquals(AddressSuggestionSource.BOOKMARK, suggestions.first().source)
    }

    @Test
    fun `limit is respected and zero yields nothing`() {
        val many = (1..20).map { history("https://site$it.com", visits = it) }
        assertEquals(5, AddressSuggestionEngine.build("", many, emptyList(), limit = 5).size)
        assertTrue(AddressSuggestionEngine.build("", many, emptyList(), limit = 0).isEmpty())
    }

    @Test
    fun `blank entries and invalid urls are dropped`() {
        val suggestions = AddressSuggestionEngine.build(
            query = "",
            history = listOf(history("   "), history("")),
            bookmarks = listOf(BookmarkItem(id = "b", title = "t", url = "  ")),
        )
        assertTrue(suggestions.isEmpty())
    }
}
