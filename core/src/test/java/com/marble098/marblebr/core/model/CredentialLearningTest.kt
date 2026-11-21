package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ذخیرهٔ هوشمندِ اطلاعات ورود و پیشنهادِ پرکردن خودکار.
 */
class CredentialLearningTest {

    @Test
    fun `a successful submitted login is eligible`() {
        val assessment = CredentialCaptureHeuristics.assess(
            LoginFormSignal(
                pageUrl = "https://www.example.com/login",
                username = "user@example.com",
                password = "S3cure-Passw0rd!",
            ),
        )
        assertTrue(assessment.shouldPrompt)
        assertEquals("example.com", assessment.domain)
        assertNotNull(assessment.passwordStrength)
    }

    @Test
    fun `registration and error pages are rejected`() {
        val base = LoginFormSignal(
            pageUrl = "https://example.com/signup",
            username = "someone",
            password = "Secret123!",
        )
        assertEquals(
            CaptureDecision.REGISTRATION_PAGE,
            CredentialCaptureHeuristics.assess(base.copy(isRegistrationPage = true)).decision,
        )
        assertEquals(
            CaptureDecision.ERROR_SHOWN,
            CredentialCaptureHeuristics.assess(base.copy(errorIndicators = true)).decision,
        )
        assertEquals(
            CaptureDecision.NOT_SUBMITTED,
            CredentialCaptureHeuristics.assess(base.copy(submitted = false)).decision,
        )
        assertEquals(
            CaptureDecision.NO_SUCCESS_EVIDENCE,
            CredentialCaptureHeuristics.assess(
                base.copy(navigationAfterSubmit = false, successIndicators = false),
            ).decision,
        )
        assertEquals(
            CaptureDecision.EMPTY_CREDENTIALS,
            CredentialCaptureHeuristics.assess(base.copy(password = "")).decision,
        )
    }

    @Test
    fun `one time codes are not stored as passwords`() {
        val assessment = CredentialCaptureHeuristics.assess(
            LoginFormSignal(
                pageUrl = "https://bank.example/otp",
                username = "09120000000",
                password = "123456",
                hasOneTimeCodeField = true,
            ),
        )
        assertEquals(CaptureDecision.ONE_TIME_CODE, assessment.decision)
        assertFalse(assessment.shouldPrompt)
    }

    @Test
    fun `strength checker punishes common and personal passwords`() {
        assertEquals(PasswordStrengthLevel.VERY_WEAK, PasswordQualityChecker.evaluate("password").level)
        assertTrue(PasswordQualityChecker.evaluate("12345678").level.ordinal <= PasswordStrengthLevel.WEAK.ordinal)
        assertEquals(PasswordStrengthLevel.VERY_WEAK, PasswordQualityChecker.evaluate("").level)
        val strong = PasswordQualityChecker.evaluate("vT7#qLm2!xR9pWs4", "someone", "example.com")
        assertTrue(strong.score >= 65)
        assertTrue(strong.fraction in 0f..1f)
        val sameAsUser = PasswordQualityChecker.evaluate("someone-2024", "someone", "example.com")
        assertTrue("DONT_REUSE_USERNAME" in sameAsUser.suggestions)
        val repeated = PasswordQualityChecker.evaluate("aaaaaaaaaaaa", "u", "example.com")
        assertTrue(repeated.level.ordinal <= PasswordStrengthLevel.WEAK.ordinal)
    }

    @Test
    fun `autofill prefers exact domain then the most recently used`() {
        val candidates = listOf(
            AutofillCandidate("old", "example.com", "old-user", 100L, lastUsedAtEpochMillis = 10L),
            AutofillCandidate("new", "example.com", "new-user", 50L, lastUsedAtEpochMillis = 900L),
            AutofillCandidate("other", "other.test", "other-user", 999L, lastUsedAtEpochMillis = 9_999L),
        )
        val plan = CredentialAutofillPlanner.plan("https://www.example.com/login", candidates, biometricLockRequired = true)
        assertEquals("new", plan.candidate?.id)
        assertTrue(plan.requiresBiometricUnlock)
        assertEquals(1, plan.alternativesCount)
    }

    @Test
    fun `subdomain matches its parent site`() {
        val candidates = listOf(
            AutofillCandidate("parent", "example.com", "user", 1L),
        )
        val plan = CredentialAutofillPlanner.plan("https://accounts.example.com/signin", candidates, false)
        assertEquals("parent", plan.candidate?.id)
        assertFalse(plan.requiresBiometricUnlock)
    }

    @Test
    fun `unknown domain yields no suggestion`() {
        val plan = CredentialAutofillPlanner.plan(
            "https://unknown.test/",
            listOf(AutofillCandidate("id", "example.com", "user", 1L)),
            biometricLockRequired = true,
        )
        assertNull(plan.candidate)
        assertFalse(plan.requiresBiometricUnlock)
    }

    @Test
    fun `usernames are masked but still recognizable`() {
        val email = CredentialCaptureHeuristics.maskUsername("someone@example.com")
        assertTrue(email.contains("•••"))
        assertTrue(email.endsWith("@example.com"))
        assertFalse(email.contains("someone@"))
        assertEquals("ab•••yz", CredentialCaptureHeuristics.maskUsername("abcdefyz"))
        assertEquals("", CredentialCaptureHeuristics.maskUsername("   "))
    }
}
