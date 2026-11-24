package com.marble098.marblebr.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.UUID

/** Versioned, password-free JSON interchange for the browser's bookmark library. */
object BookmarkBackupCodec {
    const val MAX_DOCUMENT_CHARACTERS = 1_500_000
    const val MAX_BOOKMARKS = 10_000
    private const val FORMAT = "marblebr-bookmarks"
    private const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportableCount(bookmarks: List<BookmarkItem>): Int = bookmarks.asSequence()
        .filter { it.title.isNotBlank() }
        .mapNotNull { safeWebUrl(it.url) }
        .distinct()
        .count()

    fun encode(bookmarks: List<BookmarkItem>): String {
        require(bookmarks.size <= MAX_BOOKMARKS) { "Too many bookmarks" }
        val document = BookmarkBackupDocument(
            format = FORMAT,
            version = VERSION,
            bookmarks = bookmarks.mapNotNull { bookmark ->
                val url = safeWebUrl(bookmark.url) ?: return@mapNotNull null
                val title = bookmark.title.trim().take(MAX_TITLE_LENGTH)
                if (title.isBlank()) return@mapNotNull null
                BookmarkBackupEntry(
                    title = title,
                    url = url,
                    folder = bookmark.folder.trim().take(MAX_FOLDER_LENGTH).ifBlank { "general" },
                    createdAtEpochMillis = bookmark.createdAtEpochMillis,
                )
            }.distinctBy(BookmarkBackupEntry::url),
        )
        return json.encodeToString(BookmarkBackupDocument.serializer(), document)
            .also { require(it.length <= MAX_DOCUMENT_CHARACTERS) { "Bookmark backup is too large" } }
    }

    /** Reject malformed/unsupported files; skip individual unsafe or incomplete entries. */
    fun decode(serialized: String): List<BookmarkItem> {
        require(serialized.length <= MAX_DOCUMENT_CHARACTERS) { "Bookmark backup is too large" }
        val document = try {
            json.decodeFromString(BookmarkBackupDocument.serializer(), serialized)
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Invalid bookmark backup", error)
        }
        require(document.format == FORMAT && document.version == VERSION) { "Unsupported bookmark backup" }
        require(document.bookmarks.size <= MAX_BOOKMARKS) { "Too many bookmarks" }

        val now = System.currentTimeMillis()
        return document.bookmarks.mapNotNull { entry ->
            val url = safeWebUrl(entry.url) ?: return@mapNotNull null
            val title = entry.title.trim().take(MAX_TITLE_LENGTH).ifBlank { return@mapNotNull null }
            val folder = entry.folder.trim().take(MAX_FOLDER_LENGTH).ifBlank { "general" }
            val createdAt = entry.createdAtEpochMillis.takeIf { it in 1..now } ?: now
            BookmarkItem(
                id = UUID.randomUUID().toString(),
                title = title,
                url = url,
                folder = folder,
                createdAtEpochMillis = createdAt,
            )
        }.distinctBy { it.url }
    }

    private fun safeWebUrl(rawUrl: String): String? {
        val value = rawUrl.trim()
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
        return value.takeIf { it.length <= MAX_URL_LENGTH }
    }

    @Serializable
    private data class BookmarkBackupDocument(
        val format: String,
        val version: Int,
        val bookmarks: List<BookmarkBackupEntry>,
    )

    @Serializable
    private data class BookmarkBackupEntry(
        val title: String,
        val url: String,
        val folder: String = "general",
        val createdAtEpochMillis: Long = 0,
    )

    private const val MAX_TITLE_LENGTH = 512
    private const val MAX_FOLDER_LENGTH = 80
    private const val MAX_URL_LENGTH = 8_192
}
