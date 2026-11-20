package com.marble098.marblebr.core.model

/**
 * سازندهٔ «نامِ هوشمندِ تب» — قابلیت سوم.
 *
 * سایت‌های زیادی عنوانِ طولانی و پرنویز دارند (`«خانه | فروشگاه اینترنتی مثال — ارسال رایگان»`).
 * در نوار تب‌ها این عنوان‌ها بریده می‌شوند و همه شبیه هم می‌شوند. [smartTabTitle] عنوان را
 * پاک‌سازی می‌کند، بخش‌های تکراری/تبلیغاتیِ انتهایی را می‌اندازد و در صورت خالی بودن، از
 * دامنهٔ سایت نام می‌سازد. منطق کاملاً خالص است و با تست واحد پوشش داده می‌شود.
 */
object TabTitleEngine {

    /** بخش‌هایی که معمولاً پس از «|» یا «-» می‌آیند و ارزشِ نمایش ندارند. */
    private val noiseSegments = listOf(
        "خانه", "صفحه اصلی", "home", "homepage", "official site", "سایت رسمی",
        "ارسال رایگان", "free shipping", "blog", "وبلاگ", "login", "ورود",
    )

    private val separators = charArrayOf('|', '—', '–', '·', '•', '_', '/')
    private const val MAX_TITLE_LENGTH = 60

    /**
     * عنوانِ تمیز و کوتاه برای نوار تب.
     *
     * @param rawTitle عنوانی که سایت گزارش داده (ممکن است خالی یا پرنویز باشد).
     * @param url نشانی صفحه، برای ساختنِ نام جایگزین.
     */
    fun smartTabTitle(rawTitle: String?, url: String?): String {
        val cleaned = rawTitle.orEmpty()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .removeSuffix("-")
            .removeSuffix("|")
            .trim()

        val core = pickMeaningfulSegment(cleaned)
        if (core.isNotBlank()) return core.take(MAX_TITLE_LENGTH).trim()

        val fallback = domainOf(url)
        return fallback.ifBlank { "" }
    }

    /** از میان بخش‌های جدا‌شده، معنی‌دارترین (بلندترین بخشِ غیرنویزی) را برمی‌گرداند. */
    private fun pickMeaningfulSegment(title: String): String {
        if (title.isBlank()) return ""
        val parts = title.split(*separators, ' ', '-')
            .map { it.trim() }
            .filter { it.length >= 3 }
        if (parts.isEmpty()) return title
        val meaningful = parts.filterNot { part -> noiseSegments.any { it.equals(part, ignoreCase = true) } }
        val pool = meaningful.ifEmpty { parts }
        // بلندترین بخش معمولاً همان نامِ اصلیِ صفحه است، نه پسوندِ برند.
        return pool.maxByOrNull { it.length }.orEmpty()
    }

    /** دامنهٔ کوتاه و خوانا از یک نشانی. */
    fun domainOf(url: String?): String {
        val trimmed = url.orEmpty().trim()
        if (trimmed.isEmpty()) return ""
        val host = runCatching {
            val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            java.net.URI(normalized).host.orEmpty()
        }.getOrDefault("")
        val clean = host.ifBlank {
            trimmed.substringAfter("://").substringBefore('/').substringBefore('?')
        }
        return clean.lowercase().removePrefix("www.").removePrefix("m.")
    }

    /**
     * آیا دو عنوان عملاً یکی‌اند؟ (برای ادغام تب‌های تکراری در نمای تب‌ها)
     * مقایسه پس از نرمال‌سازی و بدون حساسیت به بزرگی/کوچکی حروف انجام می‌شود.
     */
    fun sameTitle(first: String?, second: String?): Boolean =
        normalize(first) == normalize(second) && normalize(first).isNotEmpty()

    private fun normalize(value: String?): String =
        value.orEmpty().replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim().lowercase()
}
