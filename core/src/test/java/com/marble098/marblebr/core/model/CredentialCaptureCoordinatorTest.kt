package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialCaptureCoordinatorTest {

    private val now = 1_700_000_000_000L
    private val loginPage = "https://www.example.com/login"
    private val observation = LoginPageObservation(
        passwordFieldCount = 1,
        usernameFieldCount = 2,
    )

    private fun coordinator(preferences: CapturePreferences = CapturePreferences()) =
        CredentialCaptureCoordinator(preferences = preferences, nowEpochMillis = now)

    private fun submitted(username: String = "user@example.com", password: String = "vT7#qLm2!xR9pWs4") =
        LoginObservationProbe.SubmittedCredentials(username = username, password = password)

    @Test
    fun `phishing lookalike blocks everything before any password is typed`() {
        val action = coordinator().onPageObserved(
            pageUrl = "https://examp1e.com/login",
            observation = observation,
            knownCredentials = emptyList(),
            knownDomains = setOf("example.com"),
        )
        assertTrue(action is CaptureAction.PhishingWarning)
        assertEquals(OriginRisk.LOOKALIKE, (action as CaptureAction.PhishingWarning).verdict.risk)
    }

    @Test
    fun `known credential is offered for autofill on the trusted site`() {
        val action = coordinator().onPageObserved(
            pageUrl = loginPage,
            observation = observation,
            knownCredentials = listOf(AutofillCandidate("id", "example.com", "user@example.com", 10L)),
            knownDomains = setOf("example.com"),
        )
        assertTrue(action is CaptureAction.Autofill)
        assertEquals("user@example.com", (action as CaptureAction.Autofill).username)
    }

    @Test
    fun `no login form means no prompt`() {
        val action = coordinator().onPageObserved(
            pageUrl = "https://example.com/articles",
            observation = LoginPageObservation(),
            knownCredentials = emptyList(),
            knownDomains = setOf("example.com"),
        )
        assertEquals("NO_LOGIN_FORM", (action as CaptureAction.None).reason)
    }

    @Test
    fun `a fresh successful login asks to save`() {
        val action = coordinator().onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertTrue(action is CaptureAction.AskToSave)
        val ask = action as CaptureAction.AskToSave
        assertEquals("example.com", ask.site)
        assertTrue(ask.strength.score > 0)
    }

    @Test
    fun `the same session never nags twice for one site`() {
        val session = CredentialCaptureCoordinator(nowEpochMillis = now)
        val first = session.onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertTrue(first is CaptureAction.AskToSave)
        val second = session.onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertEquals("ALREADY_SAVED", (second as CaptureAction.None).reason)
    }

    @Test
    fun `auto save off means nothing is stored`() {
        val action = coordinator(CapturePreferences(autoSaveEnabled = false))
            .onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertEquals("AUTO_SAVE_OFF", (action as CaptureAction.None).reason)
    }

    @Test
    fun `weaker replacement password only warns`() {
        val existing = AutofillCandidate("id", "example.com", "user@example.com", 1L, strengthScore = 95)
        val action = coordinator().onSubmitObserved(
            loginPage,
            submitted(password = "password123"),
            observation,
            existing = existing,
        )
        assertTrue(action is CaptureAction.WarnWeakerPassword)
    }

    @Test
    fun `dismissal is respected during the cooldown window`() {
        val session = CredentialCaptureCoordinator(nowEpochMillis = now)
        session.onUserDismissed(loginPage)
        val action = session.onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertEquals("COOLDOWN", (action as CaptureAction.None).reason)

        session.advanceClock(now + CapturePreferences().promptCooldownMillis + 1)
        val later = session.onSubmitObserved(loginPage, submitted(), observation, existing = null)
        assertTrue(later is CaptureAction.AskToSave)
    }

    @Test
    fun `one time code submissions are ignored`() {
        val otpObservation = observation.copy(oneTimeCodeFieldCount = 1)
        val action = coordinator().onSubmitObserved(
            loginPage,
            submitted(password = "123456"),
            otpObservation,
            existing = null,
        )
        assertEquals("ONE_TIME_CODE", (action as CaptureAction.None).reason)
    }

    @Test
    fun `filled credentials quoted as javascript never break out`() {
        val dangerousUser = "a\"'; alert(1); //"
        val dangerousPassword = "p<script>\\</script>"
        val script = LoginObservationProbe.autofillScript(dangerousUser, dangerousPassword)
        assertTrue(script.contains("\\\""))
        assertTrue(script.contains("\\u0027"))
        assertTrue(script.contains("\\/script"))
        assertEquals("\"a\\\"\\u0027; alert(1); \\/\\/\"", JsLiteral.quote("a\"'; alert(1); //"))
    }

    @Test
    fun `observation parser reads probe output safely`() {
        val parsed = LoginObservationProbe.parse(
            "{\"passwordFieldCount\":2,\"googleButtonPresent\":true,\"browserRefusalText\":false,\"differentOriginIframeCount\":1,\"extra\":[1,2]}",
        )
        assertEquals(2, parsed.passwordFieldCount)
        assertTrue(parsed.googleButtonPresent)
        assertEquals(1, parsed.differentOriginIframeCount)
        assertEquals(LoginPageObservation.EMPTY, LoginObservationProbe.parse("not json"))
        assertEquals(LoginPageObservation.EMPTY, LoginObservationProbe.parse(null))
    }

    @Test
    fun `submitted credentials parser requires a password`() {
        val parsed = LoginObservationProbe.parseSubmitted("{\"username\":\"u\",\"password\":\"p\",\"otpFields\":0}")
        assertEquals("u", parsed?.username)
        assertEquals(0, parsed?.otpFieldCount)
        assertEquals(null, LoginObservationProbe.parseSubmitted("{\"username\":\"u\",\"password\":\"\"}"))
    }
}
