package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkBackupCodecTest {
    @Test
    fun `round trip retains bookmark fields but creates fresh ids`() {
        val original = listOf(
            BookmarkItem(
                id = "device-local-id",
                title = "Bank portal",
                url = "https://neon.enbank.ir/",
                folder = "Finance",
                createdAtEpochMillis = 1_700_000_000_000,
            ),
        )

        val decoded = BookmarkBackupCodec.decode(BookmarkBackupCodec.encode(original))

        assertEquals(1, decoded.size)
        assertEquals(original.single().title, decoded.single().title)
        assertEquals(original.single().url, decoded.single().url)
        assertEquals(original.single().folder, decoded.single().folder)
        assertEquals(original.single().createdAtEpochMillis, decoded.single().createdAtEpochMillis)
        assertTrue(decoded.single().id != original.single().id)
    }

    @Test
    fun `encode omits unsafe bookmarks instead of exporting embedded credentials`() {
        val bookmarks = listOf(
            BookmarkItem("safe", "Safe", "https://example.com/", "general", 1),
            BookmarkItem("unsafe", "Credentials", "https://user:secret@example.net/", "general", 1),
        )
        val backup = BookmarkBackupCodec.encode(bookmarks)

        val exported = BookmarkBackupCodec.decode(backup)
        assertEquals(1, exported.size)
        assertEquals("https://example.com/", exported.single().url)
        assertEquals(1, BookmarkBackupCodec.exportableCount(bookmarks))
    }

    @Test
    fun `decode skips unsafe urls and duplicate urls`() {
        val backup = """
            {
              "format": "marblebr-bookmarks",
              "version": 1,
              "bookmarks": [
                {"title":"Safe","url":"https://example.com/","folder":"general","createdAtEpochMillis":1},
                {"title":"Duplicate","url":"https://example.com/","folder":"other","createdAtEpochMillis":2},
                {"title":"Unsafe","url":"javascript:alert(1)","folder":"general","createdAtEpochMillis":1},
                {"title":"Credential leak","url":"https://user:secret@example.net/","folder":"general","createdAtEpochMillis":1}
              ]
            }
        """.trimIndent()

        val decoded = BookmarkBackupCodec.decode(backup)

        assertEquals(1, decoded.size)
        assertEquals("Safe", decoded.single().title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects unsupported backup versions`() {
        BookmarkBackupCodec.decode("""{"format":"marblebr-bookmarks","version":99,"bookmarks":[]}""")
    }
}
