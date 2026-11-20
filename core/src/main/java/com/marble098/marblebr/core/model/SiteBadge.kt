package com.marble098.marblebr.core.model

/**
 * نشانِ محلیِ هر سایت (Monogram logo).
 *
 * MarbleBr هیچ درخواستی برای گرفتن لوگوی سایت‌ها به سرورهای بیرونی نمی‌فرستد
 * (قانون حریم خصوصی AGENTS.md)؛ پس نشان هر سایت از روی خودِ نام و دامنه ساخته می‌شود:
 * حروف کوتاه‌شده + یک رنگ ثابت که همیشه برای همان سایت یکی است.
 */
data class SiteBadge(
    val monogram: String,
    val colorSeed: Int,
) {
    companion object {

        /** سایت‌های پرکاربردی که شکل نوشتاری بهتری از حرف اولشان دارند. */
        private val KNOWN_MONOGRAMS = mapOf(
            "github.com" to "GH",
            "gitlab.com" to "GL",
            "web.telegram.org" to "TG",
            "telegram.org" to "TG",
            "t.me" to "TG",
            "web.whatsapp.com" to "WA",
            "whatsapp.com" to "WA",
            "instagram.com" to "IG",
            "x.com" to "X",
            "twitter.com" to "X",
            "youtube.com" to "YT",
            "m.youtube.com" to "YT",
            "gmail.com" to "GM",
            "mail.google.com" to "GM",
            "google.com" to "G",
            "www.google.com" to "G",
            "drive.google.com" to "GD",
            "reddit.com" to "RD",
            "linkedin.com" to "IN",
            "discord.com" to "DC",
            "stackoverflow.com" to "SO",
            "notion.so" to "N",
            "figma.com" to "FG",
            "chatgpt.com" to "AI",
            "chat.openai.com" to "AI",
            "claude.ai" to "CL",
            "digikala.com" to "DK",
            "aparat.com" to "AP",
            "bale.ai" to "BL",
            "eitaa.com" to "ET",
            "sandoogh.ir" to "SB",
            "virgool.io" to "VG",
            "divar.ir" to "DV",
            "shad.ir" to "SH",
            "bankmellat.ir" to "BM",
        )

        /**
         * نشانِ یک سایت را می‌سازد.
         *
         * @param displayName نام نمایشی (اگر خالی باشد از دامنه استفاده می‌شود).
         * @param domain دامنهٔ سایت، مثل `github.com`.
         */
        fun of(displayName: String, domain: String): SiteBadge {
            val normalizedDomain = domain.trim().lowercase().removePrefix("www.")
            val direct = KNOWN_MONOGRAMS[normalizedDomain]
                ?: KNOWN_MONOGRAMS[domain.trim().lowercase()]
            if (direct != null) return SiteBadge(direct, seedOf(normalizedDomain))

            val name = displayName.trim().ifBlank { normalizedDomain.substringBefore('.') }
            val monogram = pickMonogram(name = name, domain = normalizedDomain)
            return SiteBadge(monogram, seedOf(normalizedDomain))
        }

        /**
         * از نام سایت یک مونوگرام یک یا دو حرفی می‌سازد.
         * برای نام‌های فارسی/عربی، حرف اول به‌عنوان نشان استفاده می‌شود.
         */
        internal fun pickMonogram(name: String, domain: String): String {
            val words = name.split(' ', '.', '-', '_').filter { it.isNotBlank() }
            if (words.size >= 2) {
                val first = words[0].first()
                val second = words[1].first()
                if (isLatin(first) && isLatin(second)) {
                    return "${first.uppercaseChar()}${second.uppercaseChar()}"
                }
                return first.toString()
            }
            val single = words.firstOrNull() ?: domain.substringBefore('.')
            if (single.isEmpty()) return "?"
            // دامنه‌هایی که با www شروع می‌شوند حذف شده‌اند؛ اولین بخش meaningful را بردار
            val core = single.substringBefore('.')
            return if (isLatin(core.first())) {
                core.take(2).let { it.first().uppercaseChar() + it.drop(1) }
            } else {
                core.first().toString()
            }
        }

        /** رنگ ثابت و تکرارپذیر برای هر دامنه. */
        internal fun seedOf(domain: String): Int {
            var hash = 7
            domain.forEach { char -> hash = hash * 31 + char.code }
            return hash and 0x7FFFFFFF
        }

        private fun isLatin(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'
    }
}
