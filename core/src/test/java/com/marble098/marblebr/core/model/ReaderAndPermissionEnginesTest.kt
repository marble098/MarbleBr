package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست موتورهای سه قابلیتِ تازه:
 * ۱. [ReaderTextEngine] — حالت مطالعه و بلندخوانی
 * ۲. [SitePermissionPolicy] — دسترسی‌های هر سایت
 * ۳. [TabTitleEngine] — نامِ هوشمند تب
 */
class ReaderAndPermissionEnginesTest {

    /* ---------------------------------- حالت مطالعه/بلندخوانی ---------------------------------- */

    @Test
    fun `readable text is separated from the script prefix`() {
        val raw = ReaderTextEngine.READABLE_TEXT_RESULT_PREFIX + "سلام دنیا.\nاین یک تست است."
        assertEquals("سلام دنیا.\nاین یک تست است.", ReaderTextEngine.extractReadableText(raw))
        assertEquals("", ReaderTextEngine.extractReadableText(null))
        assertEquals("plain", ReaderTextEngine.extractReadableText("plain"))
    }

    @Test
    fun `hasEnoughText rejects tiny snippets and accepts real paragraphs`() {
        assertFalse(ReaderTextEngine.hasEnoughText(""))
        assertFalse(ReaderTextEngine.hasEnoughText("کوتاه"))
        assertTrue(
            ReaderTextEngine.hasEnoughText(
                "این یک پاراگراف کامل است. جملهٔ دومی هم دارد که نشان می‌دهد متن برای خواندن کافی است. و جملهٔ سوم هم دارد.",
            ),
        )
        assertTrue(ReaderTextEngine.hasEnoughText("x".repeat(40) + "." + "x".repeat(200)))
    }

    @Test
    fun `speech chunks never exceed the limit and keep every character`() {
        val sentence = "این جمله برای تست موتور گفتار نوشته شده است. "
        val text = sentence.repeat(30)
        val chunks = ReaderTextEngine.speechChunks(text, maxChars = 120)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.length <= 120 })
        // هیچ کاراکتر معنی‌داری گم نمی‌شود (فقط فاصله‌های تکراری در مرزها حذف می‌شوند).
        val joined = chunks.joinToString(" ").replace(Regex("""\s+"""), " ").trim()
        val original = text.replace(Regex("""\s+"""), " ").trim()
        assertEquals(original, joined)
    }

    @Test
    fun `speech chunks of a short text is a single chunk`() {
        assertEquals(listOf("یک جمله کوتاه."), ReaderTextEngine.speechChunks("یک جمله کوتاه."))
        assertTrue(ReaderTextEngine.speechChunks("   ").isEmpty())
    }

    @Test
    fun `reader scripts are non empty and mention the marker style`() {
        assertTrue(ReaderTextEngine.readerScript.contains("marble-reader-style"))
        assertTrue(ReaderTextEngine.readerResetScript.contains("marble-reader-style"))
        assertTrue(
            ReaderTextEngine.extractReadableTextScript.contains(ReaderTextEngine.READABLE_TEXT_RESULT_PREFIX),
        )
    }

    /* --------------------------------------- دسترسی‌ها --------------------------------------- */

    @Test
    fun `defaults are safe and profiles fall back to them`() {
        val profile = SitePermissionProfile(domain = "example.org")
        assertEquals(PermissionState.BLOCK, profile.stateOf(SitePermission.AUTOPLAY))
        assertEquals(PermissionState.ASK, profile.stateOf(SitePermission.CAMERA))
        assertTrue(profile.javascriptAllowed())
        assertFalse(profile.autoplayAllowed())
        assertFalse(profile.isCustomized())
    }

    @Test
    fun `explicit decisions override the defaults`() {
        val profile = SitePermissionProfile(domain = "example.org")
            .with(SitePermission.JAVASCRIPT, PermissionState.BLOCK)
            .with(SitePermission.AUTOPLAY, PermissionState.ALLOW)
        assertFalse(profile.javascriptAllowed())
        assertTrue(profile.autoplayAllowed())
        assertTrue(profile.isCustomized())
    }

    @Test
    fun `cycle moves allow then ask then block and counts blocked permissions`() {
        assertEquals(PermissionState.ASK, SitePermissionPolicy.nextState(PermissionState.ALLOW))
        assertEquals(PermissionState.BLOCK, SitePermissionPolicy.nextState(PermissionState.ASK))
        assertEquals(PermissionState.ALLOW, SitePermissionPolicy.nextState(PermissionState.BLOCK))

        val base = SitePermissionProfile(domain = "a.b")
        // فقط پیش‌فرضِ پخش خودکار بلاک است.
        assertEquals(1, SitePermissionPolicy.blockedCount(base))
        val blockedBoth = base.with(SitePermission.CAMERA, PermissionState.BLOCK)
        assertEquals(2, SitePermissionPolicy.blockedCount(blockedBoth))
        assertEquals(6, SitePermissionPolicy.ordered.size)
    }

    /* ------------------------------------ نامِ هوشمند تب ------------------------------------ */

    @Test
    fun `smart title keeps the meaningful part of a noisy title`() {
        assertEquals(
            "فروشگاه اینترنتی مثال",
            TabTitleEngine.smartTabTitle("خانه | فروشگاه اینترنتی مثال — ارسال رایگان", "https://shop.example"),
        )
        assertEquals("MarbleBr", TabTitleEngine.smartTabTitle("MarbleBr | GitHub", "https://github.com/marble098"))
    }

    @Test
    fun `smart title falls back to the domain when the title is empty`() {
        assertEquals("example.org", TabTitleEngine.smartTabTitle("", "https://www.example.org/a/b"))
        assertEquals("example.org", TabTitleEngine.smartTabTitle(null, "www.example.org"))
        assertEquals("", TabTitleEngine.smartTabTitle("", ""))
    }

    @Test
    fun `smart title never exceeds the limit and never throws`() {
        val long = "کلمه ".repeat(60)
        assertTrue(TabTitleEngine.smartTabTitle(long, "https://x.y").length <= 60)
        listOf("", ":::", "javascript:alert(1)", "https://??").forEach { url ->
            runCatching { TabTitleEngine.smartTabTitle("عنوان", url) }
                .onFailure { throw AssertionError("smartTabTitle threw for '$url'", it) }
        }
    }

    @Test
    fun `sameTitle normalizes punctuation and case`() {
        assertTrue(TabTitleEngine.sameTitle("Hello World!", "hello   world"))
        assertFalse(TabTitleEngine.sameTitle("", ""))
        assertFalse(TabTitleEngine.sameTitle("one", "two"))
    }
}
