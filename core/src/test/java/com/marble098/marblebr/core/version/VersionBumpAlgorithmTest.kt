package com.marble098.marblebr.core.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionBumpAlgorithmTest {

    @Test
    fun `patch increments when changed lines are less than 50`() {
        val current = SemanticVersion.parse("1.4.2")
        assertEquals(SemanticVersion(1, 4, 3), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 0))
        assertEquals(SemanticVersion(1, 4, 3), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 15))
        assertEquals(SemanticVersion(1, 4, 3), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 49))
    }

    @Test
    fun `minor increments and patch resets to zero when changed lines are between 50 and 500`() {
        val current = SemanticVersion.parse("1.4.2")
        assertEquals(SemanticVersion(1, 5, 0), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 50))
        assertEquals(SemanticVersion(1, 5, 0), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 275))
        assertEquals(SemanticVersion(1, 5, 0), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 500))
    }

    @Test
    fun `major increments and minor and patch reset when changed lines exceed 500 or breaking`() {
        val current = SemanticVersion.parse("1.4.2")
        assertEquals(SemanticVersion(2, 0, 0), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 501))
        assertEquals(SemanticVersion(2, 0, 0), VersionBumpAlgorithm.computeNextVersion(current, changedLines = 2400))
        assertEquals(
            SemanticVersion(2, 0, 0),
            VersionBumpAlgorithm.computeNextVersion(current, changedLines = 12, isBreaking = true),
        )
    }

    @Test
    fun `numstat parser excludes lock and generated files from line count`() {
        val numstat = """
            18	7	app/src/main/java/com/marble098/marblebr/MainActivity.kt
            1200	800	package-lock.json
            450	50	gradle.lockfile
            300	100	app/build/generated/ksp/debug/Database_Impl.kt
            -	-	gradle/wrapper/gradle-wrapper.jar
            10	5	vault/src/main/java/com/marble098/marblebr/vault/SessionVaultManager.kt
        """.trimIndent()

        val totalLines = VersionBumpAlgorithm.calculateChangedLinesFromNumstat(numstat)
        assertEquals(40, totalLines)
    }

    @Test
    fun `breaking change indicator detects bang and footer`() {
        assertTrue(VersionBumpAlgorithm.hasBreakingChangeIndicator("feat(engine)!: isolate containers"))
        assertTrue(VersionBumpAlgorithm.hasBreakingChangeIndicator("fix: auth\n\nBREAKING CHANGE: new token format"))
        assertFalse(VersionBumpAlgorithm.hasBreakingChangeIndicator("feat(vault): add time machine rollback"))
    }

    @Test
    fun `monotonic version code is always strictly increasing`() {
        assertEquals(11, VersionBumpAlgorithm.computeNextVersionCode(previousVersionCode = 10, commitCount = 4, githubRunNumber = 8))
        assertEquals(45, VersionBumpAlgorithm.computeNextVersionCode(previousVersionCode = 10, commitCount = 20, githubRunNumber = 45))
        assertEquals(32, VersionBumpAlgorithm.computeNextVersionCode(previousVersionCode = 10, commitCount = 32, githubRunNumber = null))
    }
}
