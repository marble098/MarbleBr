package com.marble098.marblebr

import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.engine.LightweightAdBlocker
import com.marble098.marblebr.engine.SessionHealthGuard
import com.marble098.marblebr.feature.browser.SmartAddressUrlResolver
import com.marble098.marblebr.vault.AesGcmVaultCipher
import com.marble098.marblebr.vault.CookiePersistenceConverter
import com.marble098.marblebr.vault.SessionVaultManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

/**
 * تست جریان‌های اصلی (End-to-End Main Flows) مرورگر MarbleBr:
 * ۱. ایزوله‌سازی کانتینرها (contextId مستقل برای هر وب‌اپ/PWA)
 * ۲. ثبت اسنپ‌شات رمزنگاری‌شده در Session Vault و بازیابی با Session Time Machine
 * ۳. تشخیص خروج ناخواسته در Session Health Guard و ترمیم خودکار
 * ۴. خروج دستی واقعی (Sign out now) بدون اثرگذاری روی سایر سایت‌ها
 */
class MarbleBrMainFlowsTest {

    @Test
    fun `end-to-end flow - isolated containers, encrypted vault snapshot, auto-healing and manual sign-out`() = runTest {
        val secretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = AesGcmVaultCipher { secretKey }
        val converter = CookiePersistenceConverter()
        val snapshotDao = InMemorySnapshotDao()
        val healthEventDao = InMemoryHealthEventDao()
        val vaultManager = SessionVaultManager(snapshotDao, cipher, converter)
        val healthGuard = SessionHealthGuard(vaultManager, healthEventDao)
        val urlResolver = SmartAddressUrlResolver()
        val adBlocker = LightweightAdBlocker()

        // Flow 1: Smart address resolution & isolated containers for two web apps
        val githubUrl = urlResolver.resolveInputToUrl("github.com")
        val telegramUrl = urlResolver.resolveInputToUrl("web.telegram.org")
        assertEquals("https://github.com", githubUrl)
        assertEquals("https://web.telegram.org", telegramUrl)

        val githubContainer = WebAppContainer(id = "github_com", domain = "github.com", displayName = "GitHub")
        val telegramContainer = WebAppContainer(id = "web_telegram_org", domain = "web.telegram.org", displayName = "Telegram")
        assertNotEquals(githubContainer.contextId, telegramContainer.contextId)

        // Flow 2: Capture encrypted snapshots for both containers
        val githubSnap1 = vaultManager.captureSnapshot(
            payload = SessionSnapshotPayload(
                origin = githubUrl,
                containerId = githubContainer.id,
                capturedAtEpochMillis = 1_000L,
                cookies = listOf(
                    CookieRecord(name = "user_session", value = "gh_token_v1", domain = "github.com", isSessionOnly = true),
                ),
                localStorage = mapOf("theme" to "dark"),
            ),
            label = "لاگین اول گیت‌هاب",
        )
        val githubSnap2 = vaultManager.captureSnapshot(
            payload = SessionSnapshotPayload(
                origin = githubUrl,
                containerId = githubContainer.id,
                capturedAtEpochMillis = 2_000L,
                cookies = listOf(
                    CookieRecord(name = "user_session", value = "gh_token_v2", domain = "github.com", isSessionOnly = true),
                ),
                localStorage = mapOf("theme" to "dark"),
            ),
            label = "لاگین دوم گیت‌هاب",
        )
        vaultManager.captureSnapshot(
            payload = SessionSnapshotPayload(
                origin = telegramUrl,
                containerId = telegramContainer.id,
                capturedAtEpochMillis = 3_000L,
                cookies = listOf(
                    CookieRecord(name = "stel_token", value = "tg_secret_999", domain = "web.telegram.org", isSessionOnly = true),
                ),
            ),
            label = "لاگین تلگرام",
        )

        // Flow 3: Session Health Guard detects unexpected redirect to /login and auto-heals
        val assessment = healthGuard.evaluateSignal(
            signal = SessionHealthSignal.LoginRedirectDetected(
                containerId = githubContainer.id,
                domain = githubContainer.domain,
                fromUrl = "https://github.com/settings/profile",
                redirectedToUrl = "https://github.com/login?return_to=%2Fsettings%2Fprofile",
            ),
        )
        assertEquals(HealthGuardAction.RESTORE_LATEST_SNAPSHOT, assessment.action)
        val latestRestored = vaultManager.restoreLatestHealthySnapshot(githubContainer.id)
        assertEquals("gh_token_v2", latestRestored?.cookies?.first()?.value)

        // Flow 4: Session Time Machine 1-tap rollback to v1
        val rolledBackV1 = vaultManager.restoreSnapshotById(githubSnap1.id)
        assertNotNull(rolledBackV1)
        assertEquals("gh_token_v1", rolledBackV1?.cookies?.first()?.value)
        assertTrue(githubSnap2.isHealthy)

        // Flow 5: Manual "Sign out now" on GitHub purges GitHub snapshots without affecting Telegram container
        vaultManager.purgeSnapshotsForContainer(githubContainer.id)
        assertNull(vaultManager.restoreLatestHealthySnapshot(githubContainer.id))
        val telegramStillSignedIn = vaultManager.restoreLatestHealthySnapshot(telegramContainer.id)
        assertNotNull(telegramStillSignedIn)
        assertEquals("tg_secret_999", telegramStillSignedIn?.cookies?.first()?.value)

        // Flow 6: Ad blocker blocks third-party ad network
        assertTrue(adBlocker.shouldBlockRequest("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js", githubUrl))
    }

    private class InMemorySnapshotDao : SnapshotDao {
        private val list = mutableListOf<SnapshotEntity>()
        override fun observeSnapshotsForContainer(containerId: String): Flow<List<SnapshotEntity>> =
            MutableStateFlow(list.filter { it.containerId == containerId })
        override fun observeAllSnapshots(): Flow<List<SnapshotEntity>> = MutableStateFlow(list.toList())
        override suspend fun getSnapshotsForContainer(containerId: String): List<SnapshotEntity> =
            list.filter { it.containerId == containerId }.sortedByDescending { it.capturedAtEpochMillis }
        override suspend fun getLatestHealthySnapshot(containerId: String): SnapshotEntity? =
            list.filter { it.containerId == containerId && it.isHealthy }.maxByOrNull { it.capturedAtEpochMillis }
        override suspend fun getById(snapshotId: String): SnapshotEntity? = list.firstOrNull { it.id == snapshotId }
        override suspend fun insert(snapshot: SnapshotEntity) {
            list.removeAll { it.id == snapshot.id }
            list.add(snapshot)
        }
        override suspend fun deleteById(snapshotId: String) {
            list.removeAll { it.id == snapshotId }
        }
        override suspend fun deleteAllForContainer(containerId: String) {
            list.removeAll { it.containerId == containerId }
        }
    }

    private class InMemoryHealthEventDao : HealthEventDao {
        private val events = mutableListOf<HealthEventEntity>()
        override fun observeRecentEvents(): Flow<List<HealthEventEntity>> = MutableStateFlow(events.toList())
        override suspend fun insert(event: HealthEventEntity) {
            events.add(event)
        }
        override suspend fun clearForContainer(containerId: String) {
            events.removeAll { it.containerId == containerId }
        }
    }
}
