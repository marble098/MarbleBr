package com.marble098.marblebr.core.model

import java.net.URI
import java.util.Locale

/**
 * «پیشنهادهای نوار آدرس» — ساختِ پیشنهادهای هوشمند از تاریخچه، نشان‌ها و پربازدیدترین‌ها.
 *
 * چرا این موتور در `core` است؟ چون سه لایهٔ مختلف (نوار آدرس، جستجوی صفحهٔ خانه و
 * آزمون‌های واحد) باید دقیقاً یک نتیجه را ببینند. منطق عمداً ساده و قابل پیش‌بینی است:
 *
 * ۱. همهٔ آدرس‌ها بر اساسِ URL یکسان‌سازی و ادغام می‌شوند (یک آدرس، یک پیشنهاد).
 * ۲. اگر کاربر چیزی ننوشته باشد، پربازدیدترین‌ها و نشان‌ها نمایش داده می‌شوند.
 * ۳. اگر تایپ کرده باشد، تطابق در دامنه، آدرس و عنوان سنجیده می‌شود؛ تطابقِ ابتدای دامنه
 *    strongest است و «github» باید `github.com` را بالاتر از `gist.github.com/...` بیاورد.
 * ۴. نشان‌ها بونوس می‌گیرند چون کاربر خودش آن‌ها را ذخیره کرده است.
 *
 * هیچ داده‌ای این‌جا ذخیره نمی‌شود؛ ورودی فهرست‌های موجود و خروجی یک فهرستِ کوتاه است.
 */
object AddressSuggestionEngine {

    const val DEFAULT_LIMIT = 8

    fun build(
        query: String,
        history: List<HistoryItem>,
        bookmarks: List<BookmarkItem>,
        limit: Int = DEFAULT_LIMIT,
    ): List<AddressSuggestion> {
        if (limit <= 0) return emptyList()
        val needle = normalizeQuery(query)
        val now = System.currentTimeMillis()

        val byUrl = LinkedHashMap<String, MutableEntry>()
        history.forEach { item ->
            val key = normalizeUrl(item.url)
            if (key.isBlank()) return@forEach
            val entry = byUrl.getOrPut(key) { MutableEntry(url = item.url, title = item.title) }
            if (entry.title.isBlank() && item.title.isNotBlank()) entry.title = item.title
            entry.visitCount += item.visitCount.coerceAtLeast(1)
            entry.lastVisitedAtEpochMillis = maxOf(entry.lastVisitedAtEpochMillis, item.visitedAtEpochMillis)
        }
        bookmarks.forEach { bookmark ->
            val key = normalizeUrl(bookmark.url)
            if (key.isBlank()) return@forEach
            val entry = byUrl.getOrPut(key) { MutableEntry(url = bookmark.url, title = bookmark.title) }
            entry.isBookmark = true
            if (entry.title.isBlank()) entry.title = bookmark.title
            entry.lastVisitedAtEpochMillis = maxOf(entry.lastVisitedAtEpochMillis, bookmark.createdAtEpochMillis)
        }

        val entries = byUrl.values.filter { entry ->
            needle.isBlank() || matches(entry, needle)
        }

        return entries
            .map { entry ->
                val score = if (needle.isBlank()) {
                    entry.visitCount * 3 + (if (entry.isBookmark) 20 else 0) +
                        recencyScore(entry.lastVisitedAtEpochMillis, now)
                } else {
                    matchScore(entry, needle) + (if (entry.isBookmark) 25 else 0) +
                        (entry.visitCount * 2).coerceAtMost(30) +
                        recencyScore(entry.lastVisitedAtEpochMillis, now)
                }
                ScoredEntry(entry, score)
            }
            .sortedWith(
                compareByDescending<ScoredEntry> { it.score }
                    .thenByDescending { it.entry.visitCount }
                    .thenByDescending { it.entry.lastVisitedAtEpochMillis },
            )
            .take(limit)
            .map { scored ->
                AddressSuggestion(
                    url = scored.entry.url,
                    title = scored.entry.title.ifBlank { hostOf(scored.entry.url) },
                    source = when {
                        scored.entry.isBookmark -> AddressSuggestionSource.BOOKMARK
                        scored.entry.visitCount > 1 -> AddressSuggestionSource.MOST_VISITED
                        else -> AddressSuggestionSource.HISTORY
                    },
                    visitCount = scored.entry.visitCount,
                    lastVisitedAtEpochMillis = scored.entry.lastVisitedAtEpochMillis,
                )
            }
    }

    private data class MutableEntry(
        val url: String,
        var title: String,
        var visitCount: Int = 0,
        var lastVisitedAtEpochMillis: Long = 0L,
        var isBookmark: Boolean = false,
    )

    private data class ScoredEntry(val entry: MutableEntry, val score: Int)

    private fun matches(entry: MutableEntry, needle: String): Boolean {
        val host = hostOf(entry.url)
        return host.contains(needle) ||
            entry.url.lowercase(Locale.ROOT).contains(needle) ||
            entry.title.lowercase(Locale.ROOT).contains(needle)
    }

    private fun matchScore(entry: MutableEntry, needle: String): Int {
        val host = hostOf(entry.url)
        val url = entry.url.lowercase(Locale.ROOT)
        val title = entry.title.lowercase(Locale.ROOT)
        return when {
            host == needle -> 130
            host.startsWith(needle) -> 105
            url.startsWith(needle) -> 95
            title.startsWith(needle) -> 85
            host.contains(".$needle") -> 70
            title.contains(needle) -> 55
            else -> 35
        }
    }

    private fun recencyScore(lastVisitedAtEpochMillis: Long, now: Long): Int {
        if (lastVisitedAtEpochMillis <= 0L) return 0
        val days = ((now - lastVisitedAtEpochMillis).coerceAtLeast(0L) / 86_400_000L).toInt()
        return when {
            days <= 1 -> 12
            days <= 7 -> 8
            days <= 30 -> 4
            else -> 1
        }
    }

    /** حذفِ پروتکل، `www.` و مسیر برای مقایسه؛ بدونِ تغییرِ خودِ URL برای باز شدن. */
    fun normalizeUrl(url: String): String {
        val trimmed = url.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return ""
        val withoutScheme = trimmed.substringAfter("://", trimmed)
        return withoutScheme.removePrefix("www.").trimEnd('/')
    }

    /** کوچک‌سازی و حذفِ فاصله‌های اضافی؛ برای تطابقِ فارسی/انگلیسی. */
    fun normalizeQuery(query: String): String = query.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun hostOf(url: String): String = runCatching {
        val normalized = if (url.contains("://")) url else "https://$url"
        URI(normalized).host?.lowercase(Locale.ROOT)?.removePrefix("www.").orEmpty()
    }.getOrDefault("")
}
