package com.marble098.marblebr.engine

import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.vault.AesGcmVaultCipher
import com.marble098.marblebr.vault.CookiePersistenceConverter
import com.marble098.marblebr.vault.SessionVaultManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.geckoview.StorageController
import javax.crypto.KeyGenerator

class SessionHealthGuardTest {

    private lateinit var vaultManager: SessionVaultManager
    private lateinit var healthGuard: SessionHealthGuard
    private lateinit var fakeHealthEventDao: FakeHealthEventDao

    @Before
    fun setUp() {
        val secretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = AesGcmVaultCipher { secretKey }
        val converter = CookiePersistenceConverter()
        vaultManager = SessionVaultManager(
            snapshotDao = InMemorySnapshotDao(),
            vaultCipher = cipher,
            cookieConverter = converter,
        )
        fakeHealthEventDao = FakeHealthEventDao()
        healthGuard = SessionHealthGuard(
            vaultManager = vaultManager,
            healthEventDao = fakeHealthEventDao,
        )
    }

    @Test
    fun `detects suspicious login redirects accurately`() {
        assertTrue(
            healthGuard.isSuspiciousLoginRedirect(
                fromUrl = "https://github.com/marble098/MarbleBr/settings",
                toUrl = "https://github.com/login?return_to=%2Fmarble098%2FMarbleBr",
            ),
        )
        assertTrue(
            healthGuard.isSuspiciousLoginRedirect(
                fromUrl = "https://mail.example.com/inbox",
                toUrl = "https://mail.example.com/auth/login",
            ),
        )
        assertFalse(
            healthGuard.isSuspiciousLoginRedirect(
                fromUrl = "https://github.com/marble098/MarbleBr",
                toUrl = "https://github.com/marble098/MarbleBr/pulls",
            ),
        )
    }

    @Test
    fun `auto-heals from latest healthy snapshot on HTTP 401 and escalates after circuit breaker limit`() = runTest {
        val containerId = "ctx_github"
        vaultManager.captureSnapshot(
            payload = SessionSnapshotPayload(
                origin = "https://github.com",
                containerId = containerId,
                capturedAtEpochMillis = 10_000L,
                cookies = listOf(
                    CookieRecord(
                        name = "user_session",
                        value = "alive_token",
                        domain = "github.com",
                    ),
                ),
            ),
        )

        val signal = SessionHealthSignal.HttpUnauthorized(
            containerId = containerId,
            domain = "github.com",
            url = "https://github.com/notifications",
            statusCode = 401,
            timestampEpochMillis = 20_000L,
        )

        // Attempt 1 -> Restores from Session Vault
        val first = healthGuard.evaluateSignal(signal)
        assertEquals(SessionHealthStatus.RECOVERED_FROM_VAULT, first.status)
        assertEquals(HealthGuardAction.RESTORE_LATEST_SNAPSHOT, first.action)

        // Attempt 2 -> Restores from Session Vault
        val second = healthGuard.evaluateSignal(signal.copy(timestampEpochMillis = 25_000L))
        assertEquals(HealthGuardAction.RESTORE_LATEST_SNAPSHOT, second.action)

        // Attempt 3 within 60s window -> Circuit breaker trips, escalates to Credential Manager / Passkey fallback!
        val third = healthGuard.evaluateSignal(signal.copy(timestampEpochMillis = 30_000L))
        assertEquals(SessionHealthStatus.SERVER_REVOKED_FALLBACK, third.status)
        assertEquals(HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK, third.action)
    }

    @Test
    fun `does not interfere when neverSignOutEnabled is false for site`() = runTest {
        val signal = SessionHealthSignal.AuthCookieMissing(
            containerId = "ctx_custom",
            domain = "example.org",
            missingCookieNames = listOf("session_id"),
        )
        val assessment = healthGuard.evaluateSignal(signal, neverSignOutEnabled = false)
        assertEquals(SessionHealthStatus.DISABLED, assessment.status)
        assertEquals(HealthGuardAction.NONE, assessment.action)
    }

    @Test
    fun `cache-only clear flags never include cookies or dom storages or auth sessions`() {
        val flags = ContainerSessionManager.SAFE_CACHE_ONLY_CLEAR_FLAGS
        assertEquals(0L, flags and StorageController.ClearFlags.COOKIES)
        assertEquals(0L, flags and StorageController.ClearFlags.DOM_STORAGES)
        assertEquals(0L, flags and StorageController.ClearFlags.AUTH_SESSIONS)
        assertTrue((flags and StorageController.ClearFlags.ALL_CACHES) != 0L)
    }

    @Test
    fun `lightweight ad blocker blocks known ad domains and allows first-party pages`() {
        val blocker = LightweightAdBlocker()
        assertTrue(
            blocker.shouldBlockRequest(
                requestUrl = "https://ad.doubleclick.net/ddm/adj/N123",
                pageOriginUrl = "https://news.example.com",
            ),
        )
        assertFalse(
            blocker.shouldBlockRequest(
                requestUrl = "https://news.example.com/articles/2026",
                pageOriginUrl = "https://news.example.com",
            ),
        )
        assertEquals(1L, blocker.blockedCount.value)
    }

    private class InMemorySnapshotDao : SnapshotDao {
        private val items = mutableListOf<SnapshotEntity>()

        override fun observeSnapshotsForContainer(containerId: String): Flow<List<SnapshotEntity>> =
            MutableStateFlow(items.filter { it.containerId == containerId })

        override fun observeAllSnapshots(): Flow<List<SnapshotEntity>> =
            MutableStateFlow(items.toList())

        override suspend fun getSnapshotsForContainer(containerId: String): List<SnapshotEntity> =
            items.filter { it.containerId == containerId }.sortedByDescending { it.capturedAtEpochMillis }

        override suspend fun getLatestHealthySnapshot(containerId: String): SnapshotEntity? =
            items.filter { it.containerId == containerId && it.isHealthy }.maxByOrNull { it.capturedAtEpochMillis }

        override suspend fun getById(snapshotId: String): SnapshotEntity? =
            items.firstOrNull { it.id == snapshotId }

        override suspend fun getPinnedForContainer(containerId: String): List<SnapshotEntity> =
            items.filter { it.containerId == containerId && it.isPinned }
                .sortedByDescending { it.capturedAtEpochMillis }

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

    private class FakeHealthEventDao : HealthEventDao {
        val events = mutableListOf<HealthEventEntity>()

        override fun observeRecentEvents(): Flow<List<HealthEventEntity>> =
            MutableStateFlow(events.toList())

        override suspend fun insert(event: HealthEventEntity) {
            events.add(event)
        }

        override suspend fun clearForContainer(containerId: String) {
            events.removeAll { it.containerId == containerId }
        }
    }
}
