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
import javax.crypto.KeyGenerator

/**
 * تست‌های سه قابلیتِ تازهٔ نگه‌داشتنِ ورود در لایهٔ Vault:
 * - «نسخهٔ طلایی» (قفل‌کردنِ نسخه و مصون‌بودن از چرخش)
 * - «نگهبان انقضا» (سنجش و تمدیدِ یک‌لمسیِ عمرِ ورود)
 * - نگهبانِ نسخه‌ها (نسخهٔ ناسالم جای نسخهٔ سالم را نمی‌گیرد)
 */
class SessionVaultFeaturesTest {

    private lateinit var dao: FakeSnapshotDao
    private lateinit var vaultManager: SessionVaultManager

    @Before
    fun setUp() {
        val testSecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        dao = FakeSnapshotDao()
        vaultManager = SessionVaultManager(
            snapshotDao = dao,
            vaultCipher = AesGcmVaultCipher { testSecretKey },
            cookieConverter = CookiePersistenceConverter(),
        )
    }

    private fun payload(containerId: String = "ctx_golden", cookieValue: String = "auth_v1") = SessionSnapshotPayload(
        origin = "https://panel.example.com",
        containerId = containerId,
        capturedAtEpochMillis = System.currentTimeMillis(),
        cookies = listOf(
            CookieRecord(
                name = "session_id",
                value = cookieValue,
                domain = "panel.example.com",
                isSessionOnly = true,
            ),
        ),
        localStorage = mapOf("auth_user" to "42"),
    )

    @Test
    fun `golden version survives rotation and restores first`() = runTest {
        val first = vaultManager.captureSnapshot(payload = payload(cookieValue = "auth_first"), maxSnapshotsToKeep = 2)
        val second = vaultManager.captureSnapshot(payload = payload(cookieValue = "auth_second"), maxSnapshotsToKeep = 2)

        assertNotNull(second)
        val pinned = vaultManager.setSnapshotPinned(first.id, true)
        assertEquals(true, pinned)

        // نسخهٔ سوم باعث چرخش می‌شود؛ نسخهٔ طلایی باید بماند و نسخهٔ قدیمیِ غیرِطلایی برود.
        vaultManager.captureSnapshot(payload = payload(cookieValue = "auth_third"), maxSnapshotsToKeep = 2)

        val remaining = dao.getSnapshotsForContainer("ctx_golden")
        assertTrue(remaining.any { it.id == first.id })
        assertTrue(remaining.first { it.id == first.id }.isPinned)
        assertEquals(2, remaining.size)

        val golden = vaultManager.restoreGoldenSnapshot("ctx_golden")
        assertNotNull(golden)
        assertEquals("auth_first", golden?.cookies?.first()?.value)
    }

    @Test
    fun `unpinning releases the version again`() = runTest {
        val meta = vaultManager.captureSnapshot(payload = payload())

        // setSnapshotPinned وضعیتِ تازه را برمی‌گرداند: true برای قفل، false برای بازکردن.
        assertEquals(true, vaultManager.setSnapshotPinned(meta.id, true))
        assertTrue(dao.getBySnapshotId(meta.id)?.isPinned == true)

        assertEquals(false, vaultManager.setSnapshotPinned(meta.id, false))
        assertFalse(dao.getBySnapshotId(meta.id)?.isPinned == true)

        assertNull(vaultManager.setSnapshotPinned("missing-id", true))
    }

    @Test
    fun `expiry guardian renews a short lived login`() = runTest {
        vaultManager.captureSnapshot(
            payload = payload(),
            targetCookieExpirationDays = 7,
            maxSnapshotsToKeep = 10,
        )

        val before = vaultManager.assessExpiryForContainer("ctx_golden")
        assertTrue(before.hasExpiringAuthCookie)
        assertTrue(before.isExpired.not())
        assertEquals(1, before.authCookieCount)

        val renewed = vaultManager.renewSnapshotExpiry(containerId = "ctx_golden", targetExpirationDays = 30)
        assertNotNull(renewed)

        val after = vaultManager.assessExpiryForContainer("ctx_golden")
        assertNotNull(after.daysUntilExpiry)
        assertTrue((after.daysUntilExpiry ?: 0) >= 28)
    }

    @Test
    fun `renewal returns null when there is nothing stored`() = runTest {
        assertNull(vaultManager.renewSnapshotExpiry(containerId = "ctx_missing", targetExpirationDays = 30))
        val assessment = vaultManager.assessExpiryForContainer("ctx_missing")
        assertFalse(assessment.hasExpiringAuthCookie)
    }

    @Test
    fun `unhealthy snapshot never takes the place of a healthy one`() = runTest {
        vaultManager.captureSnapshot(payload = payload(cookieValue = "healthy_a"), maxSnapshotsToKeep = 2)
        vaultManager.captureSnapshot(payload = payload(cookieValue = "healthy_b"), maxSnapshotsToKeep = 2)
        // نسخهٔ خالی/ناسالم: نه کوکی دارد و نه حافظه.
        val signedOut = SessionSnapshotPayload(
            origin = "https://panel.example.com",
            containerId = "ctx_golden",
            capturedAtEpochMillis = System.currentTimeMillis() + 5_000L,
        )
        val broken = vaultManager.captureSnapshot(payload = signedOut, maxSnapshotsToKeep = 2)
        assertFalse(broken.isHealthy)

        val restored = vaultManager.restoreLatestHealthySnapshot("ctx_golden")
        assertNotNull(restored)
        assertTrue(restored?.cookies?.isNotEmpty() == true)
    }

    private class FakeSnapshotDao : SnapshotDao {
        private val items = MutableStateFlow<List<SnapshotEntity>>(emptyList())

        fun getBySnapshotId(snapshotId: String): SnapshotEntity? = items.value.firstOrNull { it.id == snapshotId }

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
            items.value
                .filter { it.containerId == containerId && it.isPinned }
                .sortedByDescending { it.capturedAtEpochMillis }

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
