package com.marble098.marblebr.core.model

/**
 * «رمزِ یکبارمصرف» — استخراجِ هوشمندِ کد از متنِ پیامک یا اعلان.
 *
 * چرا این فایل جدا و بدونِ اندروید است؟ چون سخت‌ترین بخشِ قابلیت همین «تشخیص» است: یک پیامکِ
 * بانکی می‌تواند هم‌زمان شمارهٔ کارت، مبلغ، تاریخ، کدِ پیگیری و رمزِ پویا داشته باشد. اگر فقط
 * «اولین عدد» برداشته شود، کاربر رمزِ اشتباه را در فرم می‌بیند. پس این‌جا یک امتیازدهیِ
 * زمینه‌محور (context scoring) انجام می‌شود:
 *
 * ۱. متن نرمال می‌شود (ارقامِ فارسی/عربی → لاتین، فاصله‌های ویژه یکدست، حذفِ URL).
 * ۲. همهٔ توالی‌های رقمیِ ۴ تا ۸ رقمی (و حالت‌های گروه‌بندی‌شده مثل «۱۲۳-۴۵۶») پیدا می‌شوند.
 * ۳. هر کاندید بر اساسِ فاصله‌اش از کلیدواژه‌های «رمز/کد/تأیید/OTP…» امتیاز می‌گیرد و بر اساسِ
 *    زمینه‌های فریب‌دهنده (کدِ پیگیری، مبلغ، شمارهٔ سفارش، سالِ تاریخ، شمارهٔ کارت) جریمه می‌شود.
 * ۴. بهترین کاندید فقط اگر از آستانهٔ اطمینان بگذرد برگردانده می‌شود؛ در غیرِ این صورت `null`.
 *
 * هیچ‌چیز این‌جا ذخیره یا ارسال نمی‌شود؛ ورودی متنِ یک پیام و خروجی یک عدد است.
 */

/** منبعِ پیامِ حاویِ کد. */
enum class OneTimeCodeSource {
    SMS,
    NOTIFICATION,
}

/** یک پیامِ خام که ممکن است کد داشته باشد (فقط در حافظه). */
data class CodeMessage(
    val text: String,
    val sender: String = "",
    val receivedAtEpochMillis: Long = 0L,
    val source: OneTimeCodeSource = OneTimeCodeSource.SMS,
)

/** نتیجهٔ استخراج: کد + میزانِ اطمینان + جایگاهش در متن (برای دیباگِ بدونِ افشای متن). */
data class OneTimeCodeHit(
    val code: String,
    val confidence: Int,
    val source: OneTimeCodeSource,
    val receivedAtEpochMillis: Long = 0L,
    val sender: String = "",
    /** فاصلهٔ کد از ابتدای متن؛ فقط عدد است و محتوایی از پیام را افشا نمی‌کند. */
    val position: Int = 0,
) {
    /** طولِ کد برای نمایشِ «کدِ ۵ رقمی پیدا شد» بدونِ افشای خودِ کد. */
    val length: Int get() = code.length
}

object OneTimeCodeExtractor {

    const val MIN_DIGITS = 4
    const val MAX_DIGITS = 8

    /** حداقلِ امتیاز برای اینکه «مطمئن» باشیم این عدد رمزِ یکبارمصرف است. */
    const val CONFIDENCE_THRESHOLD = 40

    /** پنجرهٔ اثرِ زمینه‌های منفی (کاراکتر). */
    private const val PENALTY_WINDOW = 70

    /** حداکثر جداکننده‌ای که هنگامِ سنجشِ «عددِ بزرگ‌تر» رد می‌شود (مثل ۶۰۳۷-۹۹۷۷-۱۲۳۴-۵۶۷۸). */
    private const val MAX_GROUP_SEPARATORS = 3

    /** عمرِ مفیدِ یک کد؛ پیامکِ قدیمی‌تر از این معمولاً منقضی است. */
    const val DEFAULT_MAX_AGE_MILLIS = 10L * 60L * 1000L

    /**
     * کلیدواژه‌هایی که «این عدد رمز است» را تقویت می‌کنند.
     * وزنِ هر گروه جداگانه اعمال می‌شود (قوی‌ترین گروه فقط یک‌بار).
     */
    private val STRONG_KEYWORDS = listOf(
        "رمز یکبار مصرف", "رمز یک‌بار مصرف", "رمز یکبارمصرف", "رمز پویا", "رمزِ پویا",
        "رمز دوم پویا", "رمز دوم", "رمزِ دوم", "رمز یکبار", "رمز موقت", "رمز تایید",
        "رمز تأیید", "رمز ورود", "رمزعبور یکبار",
        "کد یکبار مصرف", "کد یک‌بار مصرف", "کد تایید", "کد تأیید", "کد فعالسازی", "کد فعال سازی",
        "کد ورود", "کد امنیتی", "کد احراز", "کد دسترسی", "کد پویا",
        "one time code", "one-time code", "one time password", "verification code",
        "security code", "login code", "sign-in code", "sign in code", "auth code",
        "authentication code", "access code", "confirm code", "confirmation code",
        "otp", "2fa", "two-factor", "two factor", "passcode", "pin code", "dynamic password",
    )

    /** کلیدواژه‌های عمومی‌تر (وزنِ کمتر). */
    private val WEAK_KEYWORDS = listOf(
        "رمز", "کد", "تایید", "تأیید", "فعالسازی", "فعال سازی", "احراز", "کد پیامکی", "پویا",
        "code", "pin", "verify", "verification", "token", "your",
    )

    /**
     * زمینه‌هایی که یعنی عدد «چیزِ دیگری» است: کدِ پیگیری، مبلغ، شمارهٔ کارت، تاریخ، ساعت…
     * این‌ها کاندید را جریمه می‌کنند، نه اینکه کلِ پیام را رد کنند؛ چون یک پیامکِ بانکیِ واقعی
     * می‌تواند هم مبلغ داشته باشد و هم رمزِ پویا.
     */
    private val NEGATIVE_KEYWORDS = listOf(
        "کد پیگیری", "کد رهگیری", "شماره پیگیری", "شماره رهگیری", "شماره سفارش",
        "شماره تراکنش", "شماره حساب", "شماره کارت", "کد ملی", "کد پستی", "کد بورسی",
        "شناسه پرداخت", "شناسه قبض", "قبض", "مبلغ", "موجودی", "مانده", "ریال", "تومان",
        "تخفیف", "امتیاز", "کد تخفیف", "کد معرف", "لینک", "جهت لغو", "لغو 11",
        "tracking", "order", "invoice", "reference", "amount", "balance", "discount",
        "coupon", "zip", "postal", "national id", "transaction", "receipt",
    )

    /** بخش‌هایی از متن که نباید کاندید بسازند (کد داخلِ لینک معمولاً رمز نیست). */
    private val URL_PATTERN = Regex("""\b(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

    /** توالی رقمی ساده. */
    private val DIGIT_RUN = Regex("""\d+""")

    /** حالتِ گروه‌بندی‌شده: «۱۲۳-۴۵۶»، «123 456»، «12 34 56». */
    private val GROUPED_CODE = Regex("""\d{2,4}(?:[ -]\d{2,4}){1,2}""")

    /**
     * الگویِ «برچسبِ چسبیده به عدد» — دقیقاً همان چیزی که در پیامک‌های بانکی دیده می‌شود:
     * «رمز 8504897»، «رمز: 482913»، «کد 12345»، «Your code is 123456».
     *
     * این الگو عمداً **فقط** فاصله/دونقطه/خط‌تیره/«is» را بینِ برچسب و عدد قبول می‌کند؛ پس
     * «کد پیگیری 123456» یا «کد تخفیف 1400» هرگز با آن مطابقت نمی‌کنند (چون واژهٔ دیگری بین
     * برچسب و عدد آمده). به همین دلیل یک بونوسِ قوی و بی‌خطر به کاندید می‌دهد.
     */
    private val LABELLED_CODE = Regex(
        """(?i)(?<![\p{L}\p{N}])(?:رمز|کد|otp|one[ -]?time[ -]?(?:code|password)|""" +
            """verification[ -]?code|security[ -]?code|login[ -]?code|sign[ -]?in[ -]?code|""" +
            """auth(?:entication)?[ -]?code|access[ -]?code|confirm(?:ation)?[ -]?code|""" +
            """passcode|pin[ -]?code|dynamic[ -]?password|password)""" +
            """\s*(?:is|=|:|-|—)?\s*([0-9]{4,8})""",
    )

    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

    /**
     * ارقامِ فارسی/عربی را به لاتین تبدیل می‌کند و فاصله‌های غیرِ معمول (NBSP، نیم‌فاصله) را
     * به فاصلهٔ ساده می‌رساند. طولِ رشته تغییر نمی‌کند تا جایگاه‌ها معتبر بمانند.
     */
    fun normalize(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when {
                char in PERSIAN_DIGITS -> append('0' + PERSIAN_DIGITS.indexOf(char))
                char in ARABIC_DIGITS -> append('0' + ARABIC_DIGITS.indexOf(char))
                // Some banks/typesetters use Arabic letter variants (ي/ك) instead of Persian
                // (ی/ک). Keywords are written with the Persian forms, so normalize both.
                char == '\u064A' -> append('\u06CC') // Arabic yeh -> Persian yeh
                char == '\u0643' -> append('\u06A9') // Arabic kaf -> Persian kaf
                char == '\u06C0' -> append('\u0647') // heh with yeh -> heh
                char == '\u00A0' || char == '\u200C' || char == '\u200F' || char == '\u200E' || char == '\t' -> append(' ')
                char == '\u06F0' -> append('0')
                else -> append(char)
            }
        }
    }

    /**
     * بهترین کدِ موجود در یک پیام.
     *
     * @param nowEpochMillis زمانِ فعلی برای سنجشِ عمرِ پیام
     * @param maxAgeMillis حداکثر عمرِ قابل‌قبول؛ پیام‌های قدیمی‌تر نادیده گرفته می‌شوند
     */
    fun extract(
        message: CodeMessage,
        nowEpochMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    ): OneTimeCodeHit? {
        if (message.text.isBlank()) return null
        if (message.receivedAtEpochMillis > 0 && nowEpochMillis > 0) {
            val age = nowEpochMillis - message.receivedAtEpochMillis
            // پیامِ آینده‌دار (ساعتِ دستگاه عقب است) پذیرفته می‌شود، پیامِ کهنه نه.
            if (age > maxAgeMillis) return null
        }
        val normalized = normalize(message.text)
        val masked = maskUrls(normalized)
        val lower = masked.lowercase()

        val candidates = mutableListOf<Candidate>()
        DIGIT_RUN.findAll(masked).forEach { match ->
            val code = match.value
            if (code.length in MIN_DIGITS..MAX_DIGITS && isCleanBoundary(masked, match.range.first, match.range.last)) {
                candidates += Candidate(code, match.range.first, match.range.last)
            }
        }
        // توالی‌های گروه‌بندی‌شده («۱۲۳-۴۵۶») به‌صورتِ یک کدِ کامل هم بررسی می‌شوند.
        GROUPED_CODE.findAll(masked).forEach { match ->
            val raw = match.value
            val digits = raw.filter(Char::isDigit)
            if (digits.length in MIN_DIGITS..MAX_DIGITS && isCleanBoundary(masked, match.range.first, match.range.last)) {
                candidates += Candidate(digits, match.range.first, match.range.last)
            }
        }
        if (candidates.isEmpty()) return null

        val scored = candidates.map { candidate -> candidate to score(lower, candidate) }
            .filter { (candidate, score) ->
                score >= CONFIDENCE_THRESHOLD && candidate.code.length in MIN_DIGITS..MAX_DIGITS
            }
        if (scored.isEmpty()) return null

        // بهترین امتیاز؛ در تساوی، کدی که دیرتر در متن آمده (معمولاً بعد از برچسبِ «رمز»).
        val best = scored.maxWithOrNull(
            compareBy<Pair<Candidate, Int>> { it.second }.thenBy { it.first.start },
        ) ?: return null

        return OneTimeCodeHit(
            code = best.first.code,
            confidence = best.second.coerceIn(0, 100),
            source = message.source,
            receivedAtEpochMillis = message.receivedAtEpochMillis,
            sender = message.sender,
            position = best.first.start,
        )
    }

    /**
     * بهترین پیام از میانِ چند پیام (مثلاً صندوقِ پیامک). تازگی هم امتیاز دارد تا کدِ
     * درخواستِ همین حالا بر کدِ ده دقیقه پیش مقدم باشد.
     */
    fun extractBest(
        messages: List<CodeMessage>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS,
    ): OneTimeCodeHit? = messages
        .mapNotNull { message ->
            extract(message, nowEpochMillis, maxAgeMillis)?.let { hit ->
                val ageSeconds = if (nowEpochMillis > 0 && message.receivedAtEpochMillis > 0) {
                    ((nowEpochMillis - message.receivedAtEpochMillis) / 1000L).coerceAtLeast(0)
                } else {
                    0L
                }
                // هر دقیقه تأخیر ۱ امتیاز؛ کدِ تازه‌تر اولویت دارد.
                hit.copy(confidence = (hit.confidence - (ageSeconds / 60L).toInt().coerceAtMost(30)).coerceIn(0, 100))
            }
        }
        .maxByOrNull { it.confidence }

    /** آیا این عدد واقعاً می‌تواند کد باشد؟ (بدونِ نیاز به متنِ پیرامون) */
    fun looksLikeCode(value: String): Boolean {
        val digits = value.filter(Char::isDigit)
        return digits.length in MIN_DIGITS..MAX_DIGITS && digits.length == value.length
    }

    private data class Candidate(val code: String, val start: Int, val end: Int)

    private fun maskUrls(text: String): String {
        if (!text.contains("http", ignoreCase = true) && !text.contains("www.", ignoreCase = true)) return text
        return URL_PATTERN.replace(text) { match -> " ".repeat(match.value.length) }
    }

    /**
     * عدد باید «تنها» باشد: نه بخشی از یک عددِ بلندتر، نه چسبیده به جداکنندهٔ هزارگان
     * («۱,۲۳۴»)، نه داخلِ کدِ کارت/تاریخ.
     *
     * نکتهٔ کلیدی: کدِ کارت با فاصله/خط‌تیره گروه‌بندی می‌شود («۶۰۳۷-۹۹۷۷-۱۲۳۴-۵۶۷۸»). اگر فقط
     * به کاراکترِ بعدی نگاه کنیم، هر گروهِ چهاررقمی یک «کد» به‌نظر می‌رسد؛ پس از روی جداکننده‌ها
     * عبور می‌کنیم و اگر رقمِ دیگری در ادامه بود، این عدد بخشی از یک عددِ بزرگ‌تر است.
     */
    private fun isCleanBoundary(text: String, start: Int, end: Int): Boolean {
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(end + 1)
        if (after != null && (after.isDigit() || after == '/' || after == ':')) return false
        if (before != null && (before.isDigit() || before == '/' || before == ':')) return false
        if (before != null && (before == ',' || before == '.') && text.getOrNull(start - 2)?.isDigit() == true) return false
        if (after != null && (after == ',' || after == '.') && text.getOrNull(end + 2)?.isDigit() == true) return false
        if (continuesNumberThroughSeparators(text, start, -1)) return false
        if (continuesNumberThroughSeparators(text, end, 1)) return false
        return true
    }

    /** آیا با عبور از فاصله/خط‌تیره/ویرگول، باز هم رقم می‌رسد؟ (یعنی این عدد ادامه دارد) */
    private fun continuesNumberThroughSeparators(text: String, index: Int, direction: Int): Boolean {
        var cursor = index + direction
        var skipped = 0
        while (cursor in text.indices && skipped <= MAX_GROUP_SEPARATORS) {
            val char = text[cursor]
            when {
                char == '-' || char == '–' || char == '—' || char == ',' || char == '.' || char == ' ' -> {
                    skipped++
                    cursor += direction
                }

                char.isDigit() -> return true
                else -> return false
            }
        }
        return false
    }

    /**
     * امتیازدهیِ زمینه‌محور.
     *
     * قاعدهٔ اصلی: **برچسبِ نزدیک‌تر حاکم است.** اگر کد به «رمز پویا» چسبیده باشد و «کد پیگیری»
     * سی کاراکترِ بعدتر آمده باشد، عدد رمز است؛ اگر برعکس «شماره کارت» نزدیک‌تر باشد، عدد
     * شمارهٔ کارت است. حالتِ دوم جریمهٔ کامل می‌گیرد و حالتِ اول فقط جریمهٔ باقی‌ماندهٔ کوچک
     * (چون یک پیامکِ بانکیِ واقعی هم مبلغ دارد و هم رمز).
     */
    private fun score(lower: String, candidate: Candidate): Int {
        val base = 12
        val positive = nearestKeyword(lower, candidate, STRONG_KEYWORDS)
            ?.let { it.copy(weight = weightFor(it.distance, 65, 34)) }
            ?.takeIf { it.weight > 0 }
            ?: nearestKeyword(lower, candidate, WEAK_KEYWORDS)
                ?.let { it.copy(weight = weightFor(it.distance, 32, 12)) }
                ?.takeIf { it.weight > 0 }

        var score = base + (positive?.weight ?: 0)

        // A label directly glued to the digits is the strongest possible signal. This is what
        // makes real bank SMS such as «رمز 8504897» reliable even when the same message also
        // contains a card number, an amount and a tracking code.
        if (isDirectlyLabelled(lower, candidate)) score += DIRECT_LABEL_BONUS

        val negative = nearestKeyword(lower, candidate, NEGATIVE_KEYWORDS)
        if (negative != null && negative.distance <= PENALTY_WINDOW) {
            val weight = negativePenalty(lower, negative)
            val closerThanLabel = positive == null || negative.distance < positive.distance
            score -= if (closerThanLabel) weight else weight / 4
        }

        score -= shapePenalty(candidate.code, (positive?.weight ?: 0))
        return score.coerceAtMost(100)
    }

    /** بونوسِ «برچسبِ چسبیده»؛ از بونوسِ کلیدواژهٔ معمولی جدا اعمال می‌شود. */
    private const val DIRECT_LABEL_BONUS = 52

    private data class KeywordHit(val distance: Int, val weight: Int, val keyword: String)

    /** آیا این کاندید دقیقاً با الگوی «رمز/کد + عدد» مطابقت دارد؟ */
    private fun isDirectlyLabelled(lower: String, candidate: Candidate): Boolean {
        LABELLED_CODE.findAll(lower).forEach { match ->
            val digits = match.groups[1] ?: return@forEach
            if (digits.value == candidate.code && digits.range.first == candidate.start) return true
        }
        return false
    }

    /** نزدیک‌ترین تکرار از هر کلیدواژه (نه فقط اولین تکرار در متن). */
    private fun nearestKeyword(lower: String, candidate: Candidate, keywords: List<String>): KeywordHit? {
        var best: KeywordHit? = null
        keywords.forEach { keyword ->
            var index = lower.indexOf(keyword)
            while (index >= 0) {
                val keywordEnd = index + keyword.length
                val distance = when {
                    keywordEnd <= candidate.start -> candidate.start - keywordEnd
                    index >= candidate.end -> index - candidate.end
                    else -> 0
                }
                val current = best
                if (current == null || distance < current.distance) {
                    best = KeywordHit(distance, 0, keyword)
                }
                index = lower.indexOf(keyword, index + 1)
            }
        }
        return best
    }

    private fun weightFor(distance: Int, near: Int, far: Int): Int = when {
        distance <= 12 -> near
        distance <= 45 -> (near + far) / 2
        distance <= 120 -> far
        else -> 0
    }

    /** شدتِ جریمه بر اساسِ نوعِ زمینهٔ فریب‌دهنده. */
    private fun negativePenalty(lower: String, hit: KeywordHit): Int {
        val identifierLike = listOf(
            "پیگیری", "رهگیری", "سفارش", "حساب", "کارت", "ملی", "پستی", "بورسی", "شناسه",
            "تراکنش", "قبض", "tracking", "order", "reference", "transaction", "receipt",
            "invoice", "national", "postal",
        ).any { hit.keyword.contains(it) }
        return when {
            identifierLike && hit.distance <= 28 -> 70
            identifierLike -> 38
            hit.distance <= 28 -> 24
            else -> 10
        }
    }

    private fun shapePenalty(code: String, positiveWeight: Int): Int {
        val numeric = code.toLongOrNull() ?: 0L
        var penalty = 0
        // سالِ میلادی/شمسی و ساعت، رمزِ یکبارمصرف نیستند.
        if (code.length == 4 && (numeric in 1300L..1500L || numeric in 1900L..2100L)) penalty += 30
        if (code.length == 4 && code.all { it == code.first() }) penalty += 25
        // کدهای ۸ رقمی معمولاً شناسه‌اند، مگر با برچسبِ صریح.
        if (code.length == 8 && positiveWeight < 40) penalty += 18
        return penalty
    }

    /** متنِ کد برای مقایسه/کش: فقط شماره‌های تأییدشده و بدونِ هیچ متنِ دیگری. */
    fun fingerprint(code: String): String = code.filter(Char::isDigit)
}
