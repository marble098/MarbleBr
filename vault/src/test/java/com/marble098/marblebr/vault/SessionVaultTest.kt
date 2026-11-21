package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator

class SessionVaultTest {

    private lateinit var cipher: AesGcmVaultCipher
    private lateinit var converter: CookiePersistenceConverter
    private lateinit var fakeSnapshotDao: FakeSnapshotDao
    private lateinit var vaultManager: SessionVaultManager

    @Before
    fun setUp() {
        val testSecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        cipher = AesGcmVaultCipher { testSecretKey }
        converter = CookiePersistenceConverter()
        fakeSnapshotDao = FakeSnapshotDao()
        vaultManager = SessionVaultManager(
            snapshotDao = fakeSnapshotDao,
            vaultCipher = cipher,
            cookieConverter = converter,
        )
    }

    @Test
    fun `aesGcmVaultCipher encrypts and decrypts payload with sha256 verification`() {
        val original = """{"session_token":"marble-secret-987654","user":"ali"}"""
        val blob = cipher.encrypt(original, associatedData = "container_github")

        assertTrue(blob.ciphertextBase64.isNotBlank())
        assertTrue(blob.ivBase64.isNotBlank())
        assertEquals(64, blob.plaintextSha256.length)

        val decrypted = cipher.decrypt(blob, associatedData = "container_github")
        assertEquals(original, decrypted)
    }

    @Test(expected = Exception::class)
    fun `aesGcmVaultCipher rejects decryption when associated containerId mismatches`() {
        val blob = cipher.encrypt("sensitive-cookie-data", associatedData = "container_a")
        cipher.decrypt(blob, associatedData = "container_b")
    }

    @Test
    fun `cookiePersistenceConverter promotes session cookies to persistent with target days`() {
        val now = 1_763_500_000_000L
        val sessionCookie = CookieRecord(
            name = "user_session",
            value = "xyz987",
            domain = "github.com",
            path = "/",
            expiresEpochMillis = null,
            isSessionOnly = true,
            isSecure = true,
            isHttpOnly = true,
            sameSite = "Lax",
        )

        val promoted = converter.promoteToPersistent(
            cookie = sessionCookie,
            targetExpirationDays = 400,
            nowEpochMillis = now,
        )

        assertFalse(promoted.isSessionOnly)
        val expectedExpiry = now + TimeUnit.DAYS.toMillis(400)
        assertEquals(expectedExpiry, promoted.expiresEpochMillis)
        assertTrue(promoted.isHttpOnly)
        assertTrue(promoted.isSecure)

        val header = converter.formatSetCookieHeader(promoted)
        assertTrue(header.contains("user_session=xyz987"))
        assertTrue(header.contains("HttpOnly"))
        assertTrue(header.contains("Secure"))
    }

    @Test
    fun `cookie promotion never changes an explicit server expiry`() {
        val cookie = CookieRecord(
            name = "session", value = "secret", domain = "example.test",
            expiresEpochMillis = 2_000L, isSessionOnly = true,
        )
        assertEquals(cookie, converter.promoteToPersistent(cookie, targetExpirationDays = 30, nowEpochMillis = 1_000L))
        assertEquals(cookie, converter.promoteToPersistent(cookie, targetExpirationDays = 30, nowEpochMillis = 3_000L))
    }

    @Test
    fun `sessionVaultManager captures encrypted snapshots and recovers latest healthy snapshot`() = runTest {
        val containerId = "ctx_telegram_web"
        val healthyPayload = SessionSnapshotPayload(
            origin = "https://web.telegram.org",
            containerId = containerId,
            capturedAtEpochMillis = 1_000L,
            cookies = listOf(
                CookieRecord(
                    name = "stel_token",
                    value = "valid_auth_token_123",
                    domain = "web.telegram.org",
                    isSessionOnly = true,
                ),
            ),
            localStorage = mapOf("user_auth" to """{"id":42,"dc":2}"""),
            indexedDbDump = mapOf("tweb_db" to "encrypted_keys_v1"),
        )

        val meta1 = vaultManager.captureSnapshot(
            payload = healthyPayload,
            targetCookieExpirationDays = 365,
            label = "لاگین اولیه تلگرام",
        )
        assertTrue(meta1.isHealthy)
        assertEquals(1, meta1.cookieCount)

        // Capture a later empty/signed-out snapshot that is NOT healthy
        val emptyPayload = SessionSnapshotPayload(
            origin = "https://web.telegram.org",
            containerId = containerId,
            capturedAtEpochMillis = 2_000L,
            cookies = emptyList(),
            localStorage = emptyMap(),
        )
        val meta2 = vaultManager.captureSnapshot(
            payload = emptyPayload,
            label = "پس از پریدن نشست",
        )
        assertFalse(meta2.isHealthy)

        // Automatic recovery must skip the unhealthy snapshot and restore the latest healthy one
        val recovered = vaultManager.restoreLatestHealthySnapshot(containerId)
        assertNotNull(recovered)
        assertEquals("valid_auth_token_123", recovered?.cookies?.firstOrNull()?.value)
        assertEquals(false, recovered?.cookies?.firstOrNull()?.isSessionOnly)
        assertEquals("""{"id":42,"dc":2}""", recovered?.localStorage?.get("user_auth"))
    }

    @Test
    fun `sessionTimeMachine retains max snapshots and supports one-tap rollback by id`() = runTest {
        val containerId = "ctx_dashboard"
        val savedMetas = (1..5).map { idx ->
            vaultManager.captureSnapshot(
                payload = SessionSnapshotPayload(
                    origin = "https://panel.example.com",
                    containerId = containerId,
                    capturedAtEpochMillis = idx * 10_000L,
                    cookies = listOf(
                        CookieRecord(
                            name = "session_id",
                            value = "token_v$idx",
                            domain = "panel.example.com",
                        ),
                    ),
                    localStorage = mapOf("step" to "v$idx"),
                ),
                maxSnapshotsToKeep = 3,
                label = "نسخه $idx",
            )
        }

        // Only the newest 3 snapshots (v3, v4, v5) should remain after pruning
        val remaining = fakeSnapshotDao.getSnapshotsForContainer(containerId)
        assertEquals(3, remaining.size)
        assertNull(vaultManager.restoreSnapshotById(savedMetas[0].id))

        // Rollback to snapshot v3 (index 2)
        val rolledBack = vaultManager.restoreSnapshotById(savedMetas[2].id)
        assertNotNull(rolledBack)
        assertEquals("token_v3", rolledBack?.cookies?.first()?.value)
        assertEquals("v3", rolledBack?.localStorage?.get("step"))
    }

    private class FakeSnapshotDao : SnapshotDao {
        private val items = MutableStateFlow<List<SnapshotEntity>>(emptyList())

        override fun observeSnapshotsForContainer(containerId: String): Flow<List<SnapshotEntity>> =
            items.map { list ->
                list.filter { it.containerId == containerId }.sortedByDescending { it.capturedAtEpochMillis }
            }

        override fun observeAllSnapshots(): Flow<List<SnapshotEntity>> =
            items.map { list -> list.sortedByDescending { it.capturedAtEpochMillis } }

        override suspend fun getSnapshotsForContainer(containerId: String): List<SnapshotEntity> =
            items.value.filter { it.containerId == containerId }.sortedByDescending { it.capturedAtEpochMillis }

        override suspend fun getLatestHealthySnapshot(containerId: String): SnapshotEntity? =
            items.value
                .filter { it.containerId == containerId && it.isHealthy }
                .maxByOrNull { it.capturedAtEpochMillis }

        override suspend fun getById(snapshotId: String): SnapshotEntity? =
            items.value.firstOrNull { it.id == snapshotId }

        override suspend fun getPinnedForContainer(containerId: String): List<SnapshotEntity> =
            items.value.filter { it.containerId == containerId && it.isPinned }
                .sortedByDescending { it.capturedAtEpochMillis }
        override suspend fun getAllForBackup(): List<SnapshotEntity> = items.value

        override suspend fun setPinned(snapshotId: String, pinned: Boolean) {
            items.value = items.value.map { if (it.id == snapshotId) it.copy(isPinned = pinned) else it }
        }

        override suspend fun insert(snapshot: SnapshotEntity) {
            items.value = items.value.filterNot { it.id == snapshot.id } + snapshot
        }

        override suspend fun deleteById(snapshotId: String) {
            items.value = items.value.filterNot { it.id == snapshotId }
        }

        override suspend fun deleteAllForContainer(containerId: String) {
            items.value = items.value.filterNot { it.containerId == containerId }
        }
    }
}
