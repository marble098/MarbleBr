package com.marble098.marblebr.engine

import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.HealthGuardAction
import com.marble098.marblebr.core.model.SessionHealthSignal
import com.marble098.marblebr.core.model.SessionHealthStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.geckoview.StorageController

class SessionHealthGuardTest {

    private lateinit var healthGuard: SessionHealthGuard
    private lateinit var fakeHealthEventDao: FakeHealthEventDao

    @Before
    fun setUp() {
        fakeHealthEventDao = FakeHealthEventDao()
        healthGuard = SessionHealthGuard(fakeHealthEventDao)
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
    fun `unauthorized responses require sign in and record the observed reason`() = runTest {
        val signal = SessionHealthSignal.HttpUnauthorized(
            containerId = "ctx_github",
            domain = "github.com",
            url = "https://github.com/notifications",
            statusCode = 401,
            timestampEpochMillis = 20_000L,
        )
        repeat(3) { attempt ->
            val assessment = healthGuard.evaluateSignal(signal.copy(timestampEpochMillis = 20_000L + attempt))
            assertEquals(SessionHealthStatus.REAUTH_REQUIRED, assessment.status)
            assertEquals(HealthGuardAction.ESCALATE_TO_CREDENTIAL_FALLBACK, assessment.action)
            assertEquals(HealthReason.UNAUTHORIZED, assessment.reason)
        }
        assertTrue(fakeHealthEventDao.events.all { it.descriptionFa == HealthReason.UNAUTHORIZED.name })
    }

    @Test
    fun `missing browser data alone does not trigger a sign in prompt`() = runTest {
        val assessment = healthGuard.evaluateSignal(
            SessionHealthSignal.StorageClearedUnexpectedly("ctx_example", "example.org"),
        )
        assertEquals(SessionHealthStatus.PROTECTED_ACTIVE, assessment.status)
        assertEquals(HealthGuardAction.NONE, assessment.action)
        assertEquals(HealthReason.STORAGE_CLEARED, assessment.reason)
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
    fun `manual restore records local results without verifying sign in`() = runTest {
        healthGuard.recordRestoreOutcome("ctx_example", "example.org", VaultRestoreOutcome(cookiesRestored = 1))
        healthGuard.recordRestoreOutcome("ctx_example", "example.org", VaultRestoreOutcome(superseded = true))

        val events = fakeHealthEventDao.events
        assertEquals(SessionHealthStatus.RESTORE_UNVERIFIED.name, events.first().statusName)
        assertEquals(HealthReason.RESTORE_ATTEMPTED.name, events.first().descriptionFa)
        assertEquals(SessionHealthStatus.RESTORE_FAILED.name, events.last().statusName)
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
