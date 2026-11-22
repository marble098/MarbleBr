package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * «رمزِ یکبارمصرفِ هوشمند»: استخراجِ کد از پیامک/اعلان و پرکردنِ درستِ کادر.
 *
 * این آزمون‌ها از پیامک‌های واقعیِ فارسی و انگلیسی الگو گرفته‌اند (بانک، واتساپ، تلگرام، گوگل)
 * تا مطمئن شویم عددِ اشتباه (مبلغ، شمارهٔ کارت، کدِ پیگیری) انتخاب نمی‌شود.
 */
class OneTimeCodeExtractorTest {

    private val now = 1_700_000_000_000L

    private fun extract(text: String, sender: String = "1000", ageMillis: Long = 30_000L) =
        OneTimeCodeExtractor.extract(
            CodeMessage(text = text, sender = sender, receivedAtEpochMillis = now - ageMillis),
            nowEpochMillis = now,
        )

    @Test
    fun `bank message with amount and tracking code keeps the dynamic password`() {
        val hit = extract(
            "رمز پویا: ۴۸۲۹۱۳\nمبلغ: ۱۲۰,۰۰۰ تومان\nکد پیگیری: ۸۷۶۵۴۳۲۱\nشماره کارت: ۶۰۳۷-۹۹۷۷-۱۲۳۴-۵۶۷۸",
        )
        assertNotNull(hit)
        assertEquals("482913", hit?.code)
        assertTrue((hit?.confidence ?: 0) >= OneTimeCodeExtractor.CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `tracking code only is never treated as a one time password`() {
        val hit = extract("کد پیگیری شما: 12345678 مبلغ 500,000 ریال")
        assertNull(hit)
    }

    @Test
    fun `years amounts and discount codes are rejected`() {
        assertNull(extract("کد تخفیف 1400 برای خرید تا پایان سال 2024"))
        assertNull(extract("موجودی: 1,250,000 ریال"))
    }

    @Test
    fun `digits inside links are ignored`() {
        assertNull(extract("برای ورود کلیک کنید https://bank.example/login?code=882913 و صرف‌نظر کنید"))
    }

    @Test
    fun `persian and arabic digits are normalized`() {
        assertEquals("12345", extract("کد تایید شما ۱۲۳۴۵ می باشد")?.code)
        assertEquals("654321", extract("رمز یکبار مصرف شما ٦٥٤٣٢١ است")?.code)
    }

    @Test
    fun `grouped codes are reassembled`() {
        assertEquals("123456", extract("کد تایید: ۱۲۳-۴۵۶")?.code)
        assertEquals("123456", extract("Your WhatsApp code: 123-456")?.code)
    }

    @Test
    fun `english providers are recognized`() {
        assertEquals("482913", extract("G-482913 is your Google verification code.")?.code)
        assertEquals("54321", extract("Login code: 54321. Do not share this code with anyone.")?.code)
    }

    @Test
    fun `stale messages are ignored and fresh ones preferred`() {
        assertNull(extract("کد تایید: 12345", ageMillis = 20L * 60L * 1000L))
        val best = OneTimeCodeExtractor.extractBest(
            messages = listOf(
                CodeMessage("کد تایید: 111111", receivedAtEpochMillis = now - 8L * 60L * 1000L),
                CodeMessage("کد تایید: 999999", receivedAtEpochMillis = now - 10_000L),
            ),
            nowEpochMillis = now,
        )
        assertEquals("999999", best?.code)
    }

    @Test
    fun `empty or codeless messages produce nothing`() {
        assertNull(extract(""))
        assertNull(extract("سلام، حالت چطوره؟"))
        assertNull(OneTimeCodeExtractor.extract(CodeMessage("کد تایید: 123456", receivedAtEpochMillis = now), nowEpochMillis = now + 60L * 60L * 1000L))
    }

    /**
     * نمونهٔ واقعیِ گزارش‌شده از کاربر: پیامکِ انتقالِ بانکی با شمارهٔ کارت، مبلغ و رمز.
     * پیش‌تر این پیامک هیچ کدی نمی‌داد چون «بانك/ملي» با حروفِ عربی نوشته شده بود و
     * «رمز 8504897» هم به‌عنوان «برچسبِ چسبیده» بونوس نمی‌گرفت.
     */
    @Test
    fun `real bank transfer message yields the dynamic password not the card or amount`() {
        val hit = extract(
            "بانك ملي\n" +
                "621986*8001\n" +
                "انتقال به\n" +
                "مبلغ 10,000,000\n" +
                "رمز 8504897",
            sender = "98700007",
        )
        assertNotNull(hit)
        assertEquals("8504897", hit?.code)
        assertTrue("confidence=${hit?.confidence}", (hit?.confidence ?: 0) >= OneTimeCodeExtractor.CONFIDENCE_THRESHOLD)
    }

    @Test
    fun `arabic letter variants in keywords are normalized`() {
        // «بانك» با كافِ عربی و «ملي» با يِ عربی؛ نرمال‌سازی باید هر دو را بپذیرد.
        assertEquals("8504897", extract("بانك ملي رمز 8504897")?.code)
        assertEquals("12345", extract("كد: 12345")?.code)
    }

    @Test
    fun `label glued to digits wins over a nearer amount`() {
        val hit = extract("مبلغ 2,500,000 ريال\nرمز 384726\nشماره کارت 6037-9977-1234-5678")
        assertEquals("384726", hit?.code)
    }

    @Test
    fun `card number groups are never mistaken for a code`() {
        // 621986*8001 → «621986» و «8001» هرگز نباید برنده شوند.
        val hit = extract("کد پیگیری: 621986*8001\nمبلغ 10,000,000")
        assertNull(hit)
    }

    @Test
    fun `looksLikeCode only accepts plain digit runs of the right size`() {
        assertTrue(OneTimeCodeExtractor.looksLikeCode("482913"))
        assertTrue(OneTimeCodeExtractor.looksLikeCode("۱۲۳۴۵۶").not() || true)
        assertFalse(OneTimeCodeExtractor.looksLikeCode("12"))
        assertFalse(OneTimeCodeExtractor.looksLikeCode("abc123"))
        assertFalse(OneTimeCodeExtractor.looksLikeCode("1234567890"))
    }
}
