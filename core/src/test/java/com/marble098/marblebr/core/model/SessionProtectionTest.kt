package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * تست‌های منطقِ خالصِ سه قابلیتِ نگه‌داشتنِ ورود:
 * نگهبانِ نسخه‌ها (چرخشِ امن)، نگهبانِ انقضا و بیمهٔ نشست.
 */
class SessionProtectionTest {

    private val dayMillis = TimeUnit.DAYS.toMillis(1)

    /* --------------------------- نگهبان نسخه‌ها --------------------------- */

    @Test
    fun `rotation keeps the newest healthy versions and deletes the oldest`() {
        val items = (1..5).map { index ->
            SnapshotRotationItem(
                id = "v$index",
                capturedAtEpochMillis = index * 1_000L,
                isHealthy = true,
                isPinned = false,
            )
        }

        val deleted = SnapshotRotationPlanner.planDeletions(items, maxKeep = 3)

        assertEquals(listOf("v2", "v1"), deleted)
    }

    @Test
    fun `golden pinned version is never deleted by rotation`() {
        val items = listOf(
            SnapshotRotationItem("golden", 1_000L, isHealthy = true, isPinned = true),
            SnapshotRotationItem("v2", 2_000L, isHealthy = true, isPinned = false),
            SnapshotRotationItem("v3", 3_000L, isHealthy = true, isPinned = false),
            SnapshotRotationItem("v4", 4_000L, isHealthy = true, isPinned = false),
        )

        val deleted = SnapshotRotationPlanner.planDeletions(items, maxKeep = 2)

        assertFalse(deleted.contains("golden"))
        assertEquals(listOf("v3", "v2"), deleted)
    }

    @Test
    fun `unhealthy version is dropped before a healthy one`() {
        val items = listOf(
            SnapshotRotationItem("healthyOld", 1_000L, isHealthy = true, isPinned = false),
            SnapshotRotationItem("healthyNew", 2_000L, isHealthy = true, isPinned = false),
            SnapshotRotationItem("brokenNewest", 9_000L, isHealthy = false, isPinned = false),
        )

        val deleted = SnapshotRotationPlanner.planDeletions(items, maxKeep = 2)

        assertEquals(listOf("brokenNewest"), deleted)
    }

    @Test
    fun `rotation never empties the timeline and tolerates empty input`() {
        assertTrue(SnapshotRotationPlanner.planDeletions(emptyList(), maxKeep = 3).isEmpty())

        val single = listOf(SnapshotRotationItem("only", 1L, isHealthy = false, isPinned = false))
        assertTrue(SnapshotRotationPlanner.planDeletions(single, maxKeep = 1).isEmpty())
    }

    /* ---------------------------- نگهبان انقضا ---------------------------- */

    private fun authCookie(expiresInDays: Long, now: Long, name: String = "session_id") = CookieRecord(
        name = name,
        value = "token-value",
        domain = "example.com",
        expiresEpochMillis = now + TimeUnit.DAYS.toMillis(expiresInDays),
    )

    @Test
    fun `expiry guardian finds the nearest expiring auth cookie`() {
        val now = System.currentTimeMillis()
        val cookies = listOf(
            authCookie(expiresInDays = 30, now = now),
            authCookie(expiresInDays = 10, now = now, name = "user_session"),
            CookieRecord(name = "theme", value = "dark", domain = "example.com", expiresEpochMillis = now + dayMillis),
        )

        val assessment = SessionExpiryGuard.assess(cookies, now)

        assertEquals(2, assessment.authCookieCount)
        assertTrue(assessment.hasExpiringAuthCookie)
        assertEquals(10, assessment.daysUntilExpiry)
        assertTrue(assessment.needsRenewal(thresholdDays = 14))
        assertFalse(assessment.needsRenewal(thresholdDays = 5))
    }

    @Test
    fun `expiry guardian ignores non-auth and non-expiring cookies`() {
        val now = System.currentTimeMillis()
        val cookies = listOf(
            CookieRecord(name = "lang", value = "fa", domain = "example.com"),
            CookieRecord(name = "session_id", value = "", domain = "example.com", expiresEpochMillis = now + dayMillis),
        )

        val assessment = SessionExpiryGuard.assess(cookies, now)

        assertFalse(assessment.hasExpiringAuthCookie)
        assertNull(assessment.daysUntilExpiry)
        assertFalse(assessment.needsRenewal())
    }

    @Test
    fun `partial future days are not marked as expired`() {
        val now = 1_800_000_000_000L
        fun assessment(offset: Long): SessionExpiryAssessment = SessionExpiryGuard.assess(
            cookies = listOf(
                CookieRecord(name = "session_id", value = "v", domain = "example.com", expiresEpochMillis = now + offset),
            ),
            nowEpochMillis = now,
        )

        assertEquals(1, assessment(1).daysUntilExpiry)
        assertFalse(assessment(1).isExpired)
        assertEquals(1, assessment(dayMillis).daysUntilExpiry)
        assertEquals(2, assessment(dayMillis + 1).daysUntilExpiry)
        assertTrue(assessment(0).isExpired)
        assertTrue(assessment(-1).isExpired)
    }

    @Test
    fun `expired login is reported as expired`() {
        val now = System.currentTimeMillis()
        val assessment = SessionExpiryGuard.assess(
            cookies = listOf(
                CookieRecord(
                    name = "auth_token",
                    value = "v",
                    domain = "example.com",
                    expiresEpochMillis = now - TimeUnit.DAYS.toMillis(2),
                ),
            ),
            nowEpochMillis = now,
        )

        assertTrue(assessment.isExpired)
        assertTrue(assessment.needsRenewal(thresholdDays = 0))
    }

    @Test
    fun `renewal threshold scales with the configured lifetime`() {
        assertEquals(21, SessionExpiryGuard.renewalThresholdDays(configuredLifetimeDays = 365))
        assertEquals(3, SessionExpiryGuard.renewalThresholdDays(configuredLifetimeDays = 7))
        assertEquals(10, SessionExpiryGuard.renewalThresholdDays(configuredLifetimeDays = 20))
    }

    /* ---------------------------- بیمهٔ نشست ---------------------------- */

    @Test
    fun `insurance selects only stale protected sites`() {
        val now = 10_000_000_000L
        val candidates = listOf(
            SiteInsuranceCandidate("a", "a.com", isProtected = true, lastSnapshotAtEpochMillis = null),
            SiteInsuranceCandidate("b", "b.com", isProtected = true, lastSnapshotAtEpochMillis = now - 1_000L),
            SiteInsuranceCandidate("c", "c.com", isProtected = false, lastSnapshotAtEpochMillis = null),
            SiteInsuranceCandidate("d", "d.com", isProtected = true, lastSnapshotAtEpochMillis = now - 10_000_000L),
        )

        val selected = SessionInsurancePolicy.selectSitesToInsure(
            candidates = candidates,
            nowEpochMillis = now,
            staleAfterMillis = 6_000_000L,
        )

        assertEquals(listOf("a", "d"), selected.map { it.containerId })
    }
}
