package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

/**
 * تست‌های رفت‌وبرگشتِ Session Vault.
 *
 * این تست‌ها همان مسیری را می‌سنجند که دکمهٔ «برگردان» داشبورد طی می‌کند: ذخیرهٔ رمزنگاری‌شده،
 * خواندنِ دوباره و بازیابیِ کوکی‌های HttpOnly. اگر رمزنگاری، برچسبِ associatedData یا
 * فیلترِ سلامتی خراب شود، بازگردانی بی‌صدا داده از دست می‌دهد؛ این تست‌ها جلوی آن را می‌گیرند.
 */
class VaultRestoreRoundTripTest {

    private lateinit var vaultManager: SessionVaultManager
    private lateinit var dao: InMemorySnapshotDao

    @Before
    fun setUp() {
        val secretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        dao = InMemorySnapshotDao()
        vaultManager = SessionVaultManager(
            snapshotDao = dao,
            vaultCipher = AesGcmVaultCipher { secretKey },
            cookieConverter = CookiePersistenceConverter(),
        )
    }

    private fun payload(containerId: String = "ctx_github", cookieValue: String = "alive") = SessionSnapshotPayload(
        origin = "https://github.com",
        containerId = containerId,
        capturedAtEpochMillis = 1_000L,
        cookies = listOf(
            CookieRecord(name = "user_session", value = cookieValue, domain = "github.com", isHttpOnly = true),
        ),
        localStorage = mapOf("gh_token" to "abc"),
        sessionStorage = mapOf("flash" to "1"),
    )

    @Test
    fun `snapshot survives an encrypt decrypt round trip with httpOnly cookies intact`() = runTest {
        val saved = vaultManager.captureSnapshot(payload = payload(), label = "manual")

        val restored = vaultManager.restoreSnapshotById(saved.id)

        assertNotNull(restored)
        requireNotNull(restored)
        assertEquals("https://github.com", restored.origin)
        assertEquals(1, restored.cookies.size)
        assertEquals("user_session", restored.cookies.first().name)
        assertTrue("کوکی HttpOnly باید حفظ شود", restored.cookies.first().isHttpOnly)
        assertEquals("abc", restored.localStorage["gh_token"])
    }

    @Test
    fun `capture preserves explicit expiry and drops expired cookies`() = runTest {
        val original = payload()
        val session = original.cookies.single().copy(name = "session", isSessionOnly = true)
        val explicit = session.copy(name = "short_session", isSessionOnly = false, expiresEpochMillis = 5_000L)
        val expired = explicit.copy(name = "expired_session", expiresEpochMillis = 999L)
        val saved = vaultManager.captureSnapshot(original.copy(cookies = listOf(session, explicit, expired)))
        val restored = requireNotNull(vaultManager.restoreSnapshotById(saved.id))
        assertEquals(listOf("session", "short_session"), restored.cookies.map { it.name })
        assertTrue(requireNotNull(restored.cookies.first().expiresEpochMillis) > 5_000L)
        assertEquals(5_000L, restored.cookies.last().expiresEpochMillis)
    }

    @Test
    fun `indexed database metadata alone is not a restorable healthy session`() = runTest {
        val metadataOnly = payload().copy(
            cookies = emptyList(),
            localStorage = emptyMap(),
            sessionStorage = emptyMap(),
            indexedDbDump = mapOf("user_auth_database" to "1"),
        )
        assertTrue(!vaultManager.hasRestorableData(metadataOnly, metadataOnly.capturedAtEpochMillis))
        val snapshot = vaultManager.captureSnapshot(metadataOnly)
        assertTrue(!snapshot.isHealthy)
        assertEquals(0, snapshot.storageKeyCount)
    }

    @Test
    fun `latest healthy snapshot is the newest healthy one`() = runTest {
        vaultManager.captureSnapshot(payload = payload(cookieValue = "older"), label = "first")
        vaultManager.captureSnapshot(
            payload = payload(cookieValue = "newer").copy(capturedAtEpochMillis = 5_000L),
            label = "second",
        )

        val restored = vaultManager.restoreLatestHealthySnapshot("ctx_github")

        assertNotNull(restored)
        assertEquals("newer", restored?.cookies?.first()?.value)
    }

    @Test
    fun `deleting a snapshot makes it unrestorable and leaves siblings alone`() = runTest {
        val first = vaultManager.captureSnapshot(payload = payload(), label = "first")
        val second = vaultManager.captureSnapshot(
            payload = payload(cookieValue = "second").copy(capturedAtEpochMillis = 9_000L),
            label = "second",
        )

        vaultManager.deleteSnapshot(first.id)

        assertNull(vaultManager.restoreSnapshotById(first.id))
        assertNotNull(vaultManager.restoreSnapshotById(second.id))
    }

    @Test
    fun `sign out purge removes every snapshot of the container only`() = runTest {
        vaultManager.captureSnapshot(payload = payload(containerId = "ctx_github"), label = "gh")
        val mail = vaultManager.captureSnapshot(
            payload = payload(containerId = "ctx_mail").copy(origin = "https://mail.google.com"),
            label = "mail",
        )

        vaultManager.purgeSnapshotsForContainer("ctx_github")

        assertNull(vaultManager.restoreLatestHealthySnapshot("ctx_github"))
        assertNotNull(vaultManager.restoreSnapshotById(mail.id))
    }

    @Test
    fun `payload without any restorable data is empty`() {
        val empty = SessionSnapshotPayload(
            origin = "https://example.org",
            containerId = "ctx_example",
            capturedAtEpochMillis = 1L,
        )
        assertTrue("payload بدون کوکی و حافظه نباید سالم شمرده شود", !vaultManager.hasRestorableData(empty, empty.capturedAtEpochMillis))
    }

    private class InMemorySnapshotDao : SnapshotDao {
        private val items = mutableListOf<SnapshotEntity>()

        override fun observeSnapshotsForContainer(containerId: String): Flow<List<SnapshotEntity>> =
            MutableStateFlow(items.filter { it.containerId == containerId })

        override fun observeAllSnapshots(): Flow<List<SnapshotEntity>> = MutableStateFlow(items.toList())

        override suspend fun getSnapshotsForContainer(containerId: String): List<SnapshotEntity> =
            items.filter { it.containerId == containerId }.sortedByDescending { it.capturedAtEpochMillis }

        override suspend fun getLatestHealthySnapshot(containerId: String): SnapshotEntity? =
            items.filter { it.containerId == containerId && it.isHealthy }.maxByOrNull { it.capturedAtEpochMillis }

        override suspend fun getById(snapshotId: String): SnapshotEntity? = items.firstOrNull { it.id == snapshotId }

        override suspend fun getPinnedForContainer(containerId: String): List<SnapshotEntity> =
            items.filter { it.containerId == containerId && it.isPinned }.sortedByDescending { it.capturedAtEpochMillis }

        override suspend fun getAllForBackup(): List<SnapshotEntity> = items.toList()

        override suspend fun setPinned(snapshotId: String, pinned: Boolean) {
            val index = items.indexOfFirst { it.id == snapshotId }
            if (index >= 0) items[index] = items[index].copy(isPinned = pinned)
        }

        override suspend fun insert(snapshot: SnapshotEntity) {
            items.removeAll { it.id == snapshot.id }
            items.add(snapshot)
        }

        override suspend fun deleteById(snapshotId: String) {
            items.removeAll { it.id == snapshotId }
        }

        override suspend fun deleteAllForContainer(containerId: String) {
            items.removeAll { it.containerId == containerId }
        }
    }
}
