package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** انتخابِ کادرِ درست برای کد؛ مهم‌ترین سدِ امنیتیِ «پرکردنِ خودکار»ی که می‌تواند اشتباه کند. */
class OtpFieldMatchingTest {

    private fun field(
        index: Int,
        name: String = "",
        id: String = "",
        autocomplete: String = "",
        maxLength: Int = 0,
        inputMode: String = "",
        placeholder: String = "",
        label: String = "",
        hasValue: Boolean = false,
        type: String = "text",
        visible: Boolean = true,
        digitsOnly: Boolean = false,
    ) = OtpFieldDescriptor(
        index = index,
        type = type,
        name = name,
        id = id,
        autocomplete = autocomplete,
        inputMode = inputMode,
        placeholder = placeholder,
        label = label,
        maxLength = maxLength,
        hasValue = hasValue,
        visible = visible,
        digitsOnly = digitsOnly,
    )

    @Test
    fun `autocomplete one-time-code wins over other numeric fields`() {
        val fields = listOf(
            field(1, name = "amount", inputMode = "numeric"),
            field(2, name = "otp", autocomplete = "one-time-code", maxLength = 6, inputMode = "numeric"),
            field(3, name = "search"),
        )
        val plan = OtpFieldMatcher.plan(fields, "482913")
        assertTrue(plan is OtpFillPlan.Single)
        assertEquals(2, (plan as OtpFillPlan.Single).fieldIndex)
    }

    @Test
    fun `localized label is enough when there is no autocomplete hint`() {
        val plan = OtpFieldMatcher.plan(
            listOf(field(1, label = "رمز یکبار مصرف", maxLength = 6, inputMode = "numeric")),
            "123456",
        )
        assertTrue(plan is OtpFillPlan.Single)
    }

    @Test
    fun `six one-digit boxes are filled as a split group`() {
        val fields = (1..6).map { index ->
            field(index, name = "digit$index", autocomplete = "one-time-code", maxLength = 1, inputMode = "numeric")
        }
        val plan = OtpFieldMatcher.plan(fields, "482913")
        assertTrue(plan is OtpFillPlan.Split)
        val split = plan as OtpFillPlan.Split
        assertEquals(listOf(1, 2, 3, 4, 5, 6), split.fieldIndexes)
        assertEquals(1, split.digitsPerField)
    }

    @Test
    fun `three two-digit boxes are split two digits at a time`() {
        val fields = listOf(
            field(1, name = "code1", maxLength = 2, inputMode = "numeric", label = "کد تایید"),
            field(2, name = "code2", maxLength = 2, inputMode = "numeric"),
            field(3, name = "code3", maxLength = 2, inputMode = "numeric"),
        )
        val plan = OtpFieldMatcher.plan(fields, "482913")
        assertTrue(plan is OtpFillPlan.Split)
        assertEquals(2, (plan as OtpFillPlan.Split).digitsPerField)
    }

    @Test
    fun `login form fields are never mistaken for otp boxes`() {
        val fields = listOf(
            field(1, name = "username", type = "text"),
            field(2, name = "password", type = "password", autocomplete = "current-password"),
            field(3, name = "amount", inputMode = "numeric"),
        )
        val plan = OtpFieldMatcher.plan(fields, "482913")
        assertFalse(plan.isActionable)
        assertEquals("NO_OTP_FIELD", (plan as OtpFillPlan.NoMatch).reason)
    }

    @Test
    fun `captcha and card fields are disqualified`() {
        val fields = listOf(
            field(1, name = "captcha", maxLength = 6, inputMode = "numeric"),
            field(2, name = "card-number", maxLength = 6, inputMode = "numeric"),
        )
        val plan = OtpFieldMatcher.plan(fields, "482913")
        assertFalse(plan.isActionable)
    }

    @Test
    fun `invisible fields are ignored`() {
        val plan = OtpFieldMatcher.plan(
            listOf(field(1, name = "otp", maxLength = 6, visible = false)),
            "482913",
        )
        assertFalse(plan.isActionable)
    }

    @Test
    fun `single two digit pair alone is not a full code group`() {
        val plan = OtpFieldMatcher.plan(
            listOf(
                field(1, name = "minute", maxLength = 2, inputMode = "numeric"),
                field(2, name = "second", maxLength = 2, inputMode = "numeric"),
            ),
            "1234",
        )
        assertFalse(plan.isActionable)
    }

    @Test
    fun `descriptor parser reads the flat probe payload`() {
        val raw = "{\"count\":2,\"f0ix\":1,\"f0n\":\"otp\",\"f0ac\":\"one-time-code\",\"f0ml\":6," +
            "\"f0vis\":true,\"f0dg\":true,\"f1ix\":2,\"f1n\":\"email\",\"f1ml\":0,\"f1vis\":false}"
        val fields = OtpFieldDescriptor.parseAll(raw)
        assertEquals(2, fields.size)
        assertEquals("otp", fields[0].name)
        assertEquals(6, fields[0].maxLength)
        assertTrue(fields[0].isOneTimeCodeAutocomplete)
        assertFalse(fields[1].visible)
        assertTrue(OtpFieldDescriptor.parseAll("not json").isEmpty())
    }

    @Test
    fun `fill script targets tagged fields and escapes the code`() {
        val script = OtpFieldProbe.fillScript("482913", OtpFillPlan.Single(3, 90))
        assertTrue(script.contains("data-marble-otp"))
        assertTrue(script.contains("482913"))
        assertTrue(script.contains("[3]"))
        // کدِ آلوده به کاراکترهای غیرِ رقمی حتی به‌صورتِ رشته هم به صفحه نمی‌رسد.
        val withQuotes = OtpFieldProbe.fillScript("12\"34", OtpFillPlan.Single(1, 90))
        assertTrue(withQuotes.contains("1234"))
        assertFalse(withQuotes.contains("12\\\"34"))
    }

    @Test
    fun `probe script only reports metadata never values`() {
        val script = OtpFieldProbe.DISCOVER_SCRIPT
        assertTrue(script.contains("one-time-code"))
        assertTrue(script.contains("hasValue"))
        // مقدارِ فیلد نباید هیچ‌جا خوانده یا ارسال شود.
        assertFalse(script.contains(".value;"))
        assertTrue(script.contains("!!el.value"))
    }
}
