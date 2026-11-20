package com.marble098.marblebr.core.model

/**
 * موتور «حالت مطالعه» و «بلندخوانی».
 *
 * این فایل هیچ وابستگی‌ای به Android یا GeckoView ندارد؛ بنابراین هم می‌توان آن را با تست
 * واحد سنجید و هم می‌توان همان منطق را به‌صورت متن به موتور وب داد.
 *
 * ### قابلیت ۱ — حالت مطالعه (Reader Mode)
 * صفحه‌های وب پر از منو، تبلیغ و فوتر هستند. [readerScript] اسکریپتی برمی‌گرداند که در همان
 * صفحه اجرا می‌شود و عناصر مزاحم (`nav`, `aside`, `footer`, تبلیغ‌ها و…) را پنهان می‌کند و
 * اندازه و فاصلهٔ متن را برای خواندن راحت می‌چیند. [readerResetScript] همه را دقیقاً به
 * حالت اول برمی‌گرداند.
 *
 * ### قابلیت ۲ — بلندخوانی (Read aloud)
 * [extractReadableTextScript] فقط «متنِ اصلی» صفحه را جمع می‌کند (پاراگراف‌های معنادار،
 * بدون منو و دکمه) و در یک متغیر سراسری می‌گذارد. [READABLE_TEXT_RESULT_PREFIX] پیشوندِ
 * نتیجه است تا لایهٔ Android بتواند آن را از پاسخِ Gecko تشخیص دهد و به موتور گفتار گوشی
 * (TextToSpeech) بدهد. [speechChunks] متن را به تکه‌های کوتاه می‌شکند چون موتورهای گفتار
 * روی متن‌های چندصدهزارکاراکتری قطع می‌کنند.
 */
object ReaderTextEngine {

    /** پیشوندی که نتیجهٔ استخراج متن با آن برمی‌گردد. */
    const val READABLE_TEXT_RESULT_PREFIX = "MARBLE_READABLE_TEXT:"

    /** حداکثر کاراکتری که برای یک بلندخوانی معقول پردازش می‌شود (جلوگیری از مکث طولانی). */
    const val MAX_READABLE_CHARS = 12_000

    /** حداکثر طول هر تکهٔ گفتار؛ موتور TTS روی متن‌های بلندتر بریده می‌کند. */
    const val MAX_CHUNK_CHARS = 320

    private const val HIDDEN_ATTRIBUTE = "data-marble-reader-hidden"

    /**
     * استایل و پنهان‌سازی عناصر مزاحم. با `id` ثابت اجرا می‌شود تا اجرای دوباره صفحه را
     * خراب نکند (idempotent).
     */
    val readerScript: String = """
        (function () {
          try {
            if (document.getElementById('marble-reader-style')) { return 'on'; }
            var style = document.createElement('style');
            style.id = 'marble-reader-style';
            style.textContent = ''
              + 'nav, aside, footer, form, [role="navigation"], [role="banner"],'
              + ' [aria-hidden="true"], iframe, video, audio, .ad, .ads, .advert,'
              + ' [class*="banner"], [class*="sidebar"], [class*="popup"], [class*="cookie"] {'
              + ' display: none !important; }'
              + 'body { background: #ffffff !important; color: #1a1a1a !important; }'
              + 'article, main, [role="main"], .content, .post, .article {'
              + ' max-width: 720px !important; margin: 0 auto !important;'
              + ' font-size: 18px !important; line-height: 1.85 !important;'
              + ' font-family: system-ui, sans-serif !important; padding: 18px !important; }'
              + 'h1, h2, h3 { line-height: 1.4 !important; }'
              + 'img { max-width: 100% !important; height: auto !important; }';
            (document.head || document.documentElement).appendChild(style);
            return 'on';
          } catch (error) { return 'error'; }
        })();
    """.trimIndent()

    /** برداشتنِ کاملِ حالت مطالعه و برگشت به ظاهر اصلیِ سایت. */
    val readerResetScript: String = """
        (function () {
          try {
            var style = document.getElementById('marble-reader-style');
            if (style && style.parentNode) { style.parentNode.removeChild(style); }
            var nodes = document.querySelectorAll('[$HIDDEN_ATTRIBUTE]');
            for (var i = 0; i < nodes.length; i++) {
              nodes[i].removeAttribute('$HIDDEN_ATTRIBUTE');
            }
            return 'off';
          } catch (error) { return 'error'; }
        })();
    """.trimIndent()

    /**
     * استخراج متنِ خوانا. نتیجهٔ همین اسکریپت به شکل
     * `MARBLE_READABLE_TEXT:<متن>` در پاسخ برمی‌گردد (خطوط با `\n` از هم جدا می‌شوند).
     */
    val extractReadableTextScript: String = """
        (function () {
          try {
            var root = document.querySelector('article, main, [role="main"]') || document.body;
            if (!root) { return '$READABLE_TEXT_RESULT_PREFIX'; }
            var blocks = root.querySelectorAll('h1, h2, h3, p, li, blockquote');
            var lines = [];
            var total = 0;
            for (var i = 0; i < blocks.length; i++) {
              var text = (blocks[i].innerText || '').replace(/\s+/g, ' ').trim();
              if (text.length < 24) { continue; }
              lines.push(text);
              total += text.length;
              if (total > $MAX_READABLE_CHARS) { break; }
            }
            if (!lines.length) {
              var fallback = (root.innerText || '').replace(/\s+/g, ' ').trim();
              if (fallback) { lines.push(fallback.slice(0, $MAX_READABLE_CHARS)); }
            }
            return '$READABLE_TEXT_RESULT_PREFIX' + lines.join('\n');
          } catch (error) { return '$READABLE_TEXT_RESULT_PREFIX'; }
        })();
    """.trimIndent()

    /**
     * متنِ خامِ برگشتی را از نتیجهٔ اسکریپت جدا می‌کند.
     *
     * @return متنِ قابل خواندن، یا رشتهٔ خالی وقتی صفحه متنی نداشت.
     */
    fun extractReadableText(evaluationResult: String?): String {
        val raw = evaluationResult.orEmpty()
        val payload = if (raw.startsWith(READABLE_TEXT_RESULT_PREFIX)) {
            raw.removePrefix(READABLE_TEXT_RESULT_PREFIX)
        } else {
            raw
        }
        return payload.replace("\r\n", "\n").trim()
    }

    /** آیا این متن ارزشِ خواندن دارد؟ (حداقل دو جمله یا یک پاراگرافِ بلند) */
    fun hasEnoughText(text: String): Boolean {
        val compact = text.replace(Regex("""\s+"""), " ").trim()
        if (compact.length < 80) return false
        val sentenceCount = compact.count { it == '.' || it == '!' || it == '?' || it == '۔' || it == '؟' }
        return sentenceCount >= 2 || compact.length >= 220
    }

    /**
     * شکستنِ متن به تکه‌های گفتار.
     *
     * مرزها به ترتیبِ اولویت: پایان جمله → کاما → فاصله. هیچ تکه‌ای از [MAX_CHUNK_CHARS]
     * بلندتر نمی‌شود تا موتور گفتار جمله را نیمه‌کاره رها نکند.
     */
    fun speechChunks(text: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        val clean = text.replace(Regex("""\s+"""), " ").trim()
        if (clean.isEmpty()) return emptyList()
        val limit = maxChars.coerceAtLeast(40)
        val chunks = mutableListOf<String>()
        var rest = clean
        while (rest.isNotEmpty()) {
            if (rest.length <= limit) {
                chunks.add(rest)
                break
            }
            val window = rest.take(limit)
            val boundary = window.lastIndexOfAny(charArrayOf('.', '!', '?', '؟', '۔', ';', '،', ','))
                .takeIf { it > limit / 3 }
                ?: window.lastIndexOf(' ').takeIf { it > limit / 3 }
                ?: (limit - 1)
            val chunk = rest.take(boundary + 1).trim()
            if (chunk.isNotEmpty()) chunks.add(chunk)
            rest = rest.drop(boundary + 1).trim()
        }
        return chunks
    }
}
