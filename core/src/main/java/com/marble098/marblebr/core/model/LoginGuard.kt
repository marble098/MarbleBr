package com.marble098.marblebr.core.model

import java.net.URI

/**
 * «نگهبانِ بدلِ ورود» (Login Origin Guard) — ایدهٔ تازهٔ ۲.
 *
 * بزرگ‌ترین خطرِ یک مرورگرِ «همیشه لاگین» این است که کاربر، عادت‌شده به نبودِ صفحهٔ ورود، یک
 * پیامِ «نشستت منقضی شد» را باور کند و گذرواژه‌اش را جایی بگذارد که شبیه سایتِ اصلی است ولی
 * نیست. MarbleBr همیشه می‌داند کاربر روی کدام دامنه قبلاً وارد شده و گذرواژه‌اش را کجا ذخیره
 * کرده؛ پس می‌تواند **قبل از تایپ** هشدار بدهد:
 *
 * - دامنهٔ فعلی، «کج‌نویسِ» یک سایتِ ذخیره‌شده است (`g00gle.com` در برابر `google.com`).
 * - صفحهٔ ورود روی دامنه‌ای است که با دامنهٔ ثبت‌شدهٔ مخزن نمی‌خواند.
 * - فرمِ ورود، گذرواژه را به میزبانِ دیگری می‌فرستد.
 *
 * هیچ داده‌ای به بیرون نمی‌رود؛ همه‌چیز با فهرستِ محلیِ سایت‌های خودِ کاربر سنجیده می‌شود.
 */
enum class OriginRisk {
    /** دامنهٔ دقیقاً همان است که قبلاً در آن وارد شده‌ای. */
    TRUSTED,

    /** زیردامنهٔ همان سایت است (مثل `accounts.google.com`). */
    TRUSTED_SUBDOMAIN,

    /** دامنهٔ ناشناخته؛ خطری اثبات نشده اما بررسی هم نشده. */
    UNKNOWN,

    /** شبیه یک سایتِ آشناست ولی دامنهٔ ثبت‌شدهٔ دیگری دارد. */
    LOOKALIKE,

    /** آدرس اصلی از یک کاربر/پورت غیرمنتظره استفاده می‌کند. */
    SUSPICIOUS_URL,
}

data class LoginGuardVerdict(
    val risk: OriginRisk,
    val host: String,
    /** اگر شبیه‌سازی است، سایتِ اصلي که شبیهش شده. */
    val impersonated: String? = null,
    val reasons: List<String> = emptyList(),
    /** آیا پیش از تایپ گذرواژه باید تأیید/هشدار نشان داده شود؟ */
    val blocksPasswordTyping: Boolean = false,
) {
    val isWarning: Boolean get() = risk == OriginRisk.LOOKALIKE || risk == OriginRisk.SUSPICIOUS_URL
}

object LoginOriginGuard {

    /** حروف/ارقامی که در حملاتِ «کج‌نویس» جای هم می‌نشینند. */
    private val HOMOGLYPHS: Map<Char, Char> = mapOf(
        '0' to 'o',
        '1' to 'l',
        '3' to 'e',
        '4' to 'a',
        '5' to 's',
        '7' to 't',
        '8' to 'b',
        '9' to 'g',
        'i' to 'l',
        'j' to 'i',
        'q' to 'g',
        'v' to 'u',
        'w' to 'v',
        'y' to 'v',
        'z' to 's',
        '2' to 'z',
        '6' to 'b',
    )

    private val SUSPICIOUS_TLDS = setOf(
        "zip", "mov", "top", "xyz", "click", "link", "review", "country", "gq", "tk", "ml", "cf",
        "ga", "work", "rest", "cam", "surf", "lol", "fit", "beauty", "makeup", "mom",
    )

    /**
     * @param currentUrl آدرسی که کاربر روی آن است.
     * @param knownDomains دامنه‌هایی که در مخزن/کانتینرهای خودِ کاربر وجود دارند.
     */
    fun check(currentUrl: String, knownDomains: Set<String>): LoginGuardVerdict {
        val uri = runCatching { URI(if (currentUrl.contains("://")) currentUrl else "https://$currentUrl") }.getOrNull()
        val host = uri?.host?.lowercase()?.trimEnd('.') ?: return LoginGuardVerdict(
            risk = OriginRisk.SUSPICIOUS_URL,
            host = "",
            reasons = listOf("UNPARSEABLE_HOST"),
            blocksPasswordTyping = true,
        )
        if (host.isBlank()) {
            return LoginGuardVerdict(OriginRisk.SUSPICIOUS_URL, host, reasons = listOf("EMPTY_HOST"), blocksPasswordTyping = true)
        }

        // آدرس‌هایی که «شکل»شان حمله است، حتی روی دامنهٔ آشنا هم قابلِ اعتماد نیستند:
        // user:pass@host همیشه یعنی اعتماد به میزبانِ بعد از @، نه به چیزی که دیده می‌شود.
        if (uri.rawUserInfo != null) {
            return LoginGuardVerdict(
                risk = OriginRisk.SUSPICIOUS_URL,
                host = host,
                reasons = listOf("USERINFO_IN_URL"),
                blocksPasswordTyping = true,
            )
        }

        val normalizedKnown = knownDomains
            .map { it.lowercase().removePrefix("www.").trim() }
            .filter { it.isNotBlank() }
            .toSet()

        if (normalizedKnown.any { host == it || host == "www.$it" }) {
            return LoginGuardVerdict(OriginRisk.TRUSTED, host)
        }
        val parentTrusted = normalizedKnown.firstOrNull { known ->
            known.contains('.') && host.endsWith(".$known")
        }
        if (parentTrusted != null) {
            return LoginGuardVerdict(OriginRisk.TRUSTED_SUBDOMAIN, host, impersonated = parentTrusted)
        }

        // نشانه‌های آدرسِ مشکوک
        val reasons = mutableListOf<String>()
        val registrable = registrableDomain(host)
        val tld = registrable.substringAfterLast('.', "")
        if (tld in SUSPICIOUS_TLDS) reasons += "RISKY_TLD"
        if (host.count { it == '.' } >= 4) reasons += "TOO_MANY_SUBDOMAINS"
        if (host.contains("xn--")) reasons += "PUNYCODE_HOST"
        if (uri.port != -1 && uri.port !in setOf(80, 443)) reasons += "NON_STANDARD_PORT"
        if (!currentUrl.startsWith("https://", ignoreCase = true)) reasons += "NOT_HTTPS"

        // شبیه‌سازیِ دامنهٔ آشنا
        val lookalike = normalizedKnown
            .filter { it != registrable }
            .minByOrNull { distance(it, registrable) }
            ?.takeIf { candidate ->
                candidate != registrable && (isLookalike(registrable, candidate) || isLookalike(host, candidate))
            }

        if (reasons.isNotEmpty()) {
            return LoginGuardVerdict(
                risk = OriginRisk.SUSPICIOUS_URL,
                host = host,
                impersonated = lookalike,
                reasons = reasons,
                blocksPasswordTyping = true,
            )
        }
        if (lookalike != null) {
            return LoginGuardVerdict(
                risk = OriginRisk.LOOKALIKE,
                host = host,
                impersonated = lookalike,
                reasons = listOf("LOOKS_LIKE_${lookalike.uppercase().replace('.', '_')}"),
                blocksPasswordTyping = true,
            )
        }
        return LoginGuardVerdict(OriginRisk.UNKNOWN, host)
    }

    /** آیا «شکلِ» دو دامنه آن‌قدر نزدیک است که کاربر اشتباه کند؟ */
    fun isLookalike(candidate: String, trusted: String): Boolean {
        if (candidate == trusted) return false
        val a = candidate.removePrefix("www.").lowercase()
        val b = trusted.removePrefix("www.").lowercase()
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return false
        if (normalizeHomoglyphs(a) == normalizeHomoglyphs(b)) return true
        // فاصلهٔ ویرایشیِ کوچک فقط برای دامنه‌های هم‌طول معنا دارد؛ وگرنه هر دامنه‌ای شبیه دیگری است.
        if (kotlin.math.abs(a.length - b.length) <= 2 && distance(a, b) <= 1) return true
        // دامنهٔ اصلی یکی و TLD عوض‌شده (google.com در برابر google.co یا google.net)
        return a.substringBeforeLast('.') == b.substringBeforeLast('.') &&
            a.substringAfterLast('.') != b.substringAfterLast('.')
    }

    fun normalizeHomoglyphs(domain: String): String = domain.lowercase().map { char ->
        HOMOGLYPHS[char] ?: char
    }.joinToString("")

    /** فاصلهٔ لِوِنشتاین با حافظهٔ خطی. */
    fun distance(first: String, second: String): Int {
        if (first == second) return 0
        val a = first.lowercase()
        val b = second.lowercase()
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    /**
     * دامنهٔ ثبت‌شدهٔ تقریبی. برای پایه‌های دو‌سطحیِ رایج (co.uk، com.au…) دو بخش آخر را نگه
     * می‌دارد تا `example.co.uk` با `co.uk` مقایسه نشود.
     */
    fun registrableDomain(host: String): String {
        val parts = host.removePrefix("www.").split('.').filter { it.isNotBlank() }
        if (parts.size <= 2) return parts.joinToString(".")
        val lastTwo = parts.takeLast(2).joinToString(".")
        return if (lastTwo in listOf("co.uk", "org.uk", "ac.uk", "co.jp", "com.au", "co.in", "com.br", "co.ir")) {
            parts.takeLast(3).joinToString(".")
        } else {
            parts.takeLast(2).joinToString(".")
        }
    }

    /** آیا مقصدِ ارسال فرمِ ورود، میزبانِ دیگری است؟ */
    fun isCrossHostFormAction(pageHost: String, actionUrl: String): Boolean {
        if (actionUrl.isBlank()) return false
        val host = runCatching {
            URI(if (actionUrl.contains("://")) actionUrl else "https://$actionUrl").host?.lowercase()
        }.getOrNull() ?: return false
        return host.isNotBlank() && host != pageHost.lowercase() && !host.endsWith(".${pageHost.lowercase()}")
    }
}
