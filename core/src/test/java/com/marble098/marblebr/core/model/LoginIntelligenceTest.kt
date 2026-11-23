package com.marble098.marblebr.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ماشین‌حالتِ «ورودِ هوشمند»: پرکردنِ خودکار، پیشنهادِ ذخیره و تزریقِ کدِ یکبارمصرف.
 *
 * این آزمون‌ها همان مسیرهایی را می‌سنجند که در نسخهٔ قبلی بی‌صدا شکسته بودند:
 * ۱. پرکردنِ خودکار واقعاً یک دستورِ پرکردن برمی‌گرداند (نه فقط یک اعلان).
 * ۲. کارتِ ذخیره برای حسابِ تکراری دوباره ظاهر نمی‌شود.
 * ۳. کدِ پیامکی به صفحهٔ درست و کادرِ درست می‌رسد و دوباره نوشته نمی‌شود.
 */
class LoginIntelligenceTest {

    private val now = 1_700_000_000_000L
    private val loginUrl = "https://www.example.com/login"
    private val loginForm = LoginPageObservation(passwordFieldCount = 1, usernameFieldCount = 1)

    private fun intelligence(policy: LoginPolicy = LoginPolicy()) = LoginIntelligence(policy, now).apply {
        updateKnownDomains(setOf("example.com"))
        restoreDismissals(emptyMap())
    }

    private fun account(
        id: String = "acc-1",
        username: String = "user@example.com",
        domain: String = "example.com",
        updatedAt: Long = now - 60_000L,
        lastUsed: Long? = null,
        sessionDays: Int = 30,
    ) = LoginAccount(
        id = id,
        domain = domain,
        username = username,
        updatedAtEpochMillis = updatedAt,
        lastUsedAtEpochMillis = lastUsed,
        strengthScore = 80,
        sessionDays = sessionDays,
    )

    @Test
    fun `stored account fills the login form instead of only notifying`() {
        val advance = intelligence().onPageProbe(loginUrl, loginForm, listOf(account()))
        assertTrue(advance is LoginAdvance.FillCredentials)
        val fill = advance as LoginAdvance.FillCredentials
        assertEquals("acc-1", fill.account.id)
        assertTrue("باید برای بار اول بیومتریک بخواهد", fill.needsBiometric)
    }

    @Test
    fun `alternative accounts are reported so the user can switch`() {
        val advance = intelligence().onPageProbe(
            loginUrl,
            loginForm,
            listOf(account("a"), account("b", username = "other@example.com"), account("c", username = "third@example.com")),
        )
        assertEquals(2, (advance as LoginAdvance.FillCredentials).alternatives)
    }

    @Test
    fun `lookalike domain is blocked before any password is typed`() {
        val advance = intelligence().onPageProbe("https://examp1e.com/login", loginForm, emptyList())
        assertTrue(advance is LoginAdvance.WarnOrigin)
        assertEquals(OriginRisk.LOOKALIKE, (advance as LoginAdvance.WarnOrigin).verdict.risk)
    }

    @Test
    fun `private tabs never autofill or save`() {
        val loaded = intelligence().onPageProbe(loginUrl, loginForm, listOf(account()), isPrivateTab = true)
        assertEquals(LoginAdvance.Nothing, loaded)
        val submitted = intelligence().onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = emptyList(),
            isPrivateTab = true,
        )
        assertEquals(LoginAdvance.Nothing, submitted)
    }

    @Test
    fun `successful first login offers a prefilled save card`() {
        val advance = intelligence().onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
            LoginFormFacts(passwordFieldCount = 1, usernameFieldCount = 1),
            knownAccounts = emptyList(),
        )
        assertTrue(advance is LoginAdvance.OfferSave)
        val offer = advance as LoginAdvance.OfferSave
        assertEquals("example.com", offer.site)
        assertEquals("vT7#qLm2!xR9pWs4", offer.password)
        assertEquals(OfferReason.FIRST_ACCOUNT, offer.reason)
    }

    @Test
    fun `second submission for the same account does not nag again`() {
        val machine = intelligence()
        val newPassword = "vT7#qLm2!xR9pWs4"
        val oldAccount = KnownAccount(
            username = "user@example.com",
            site = "https://example.com",
            strengthScore = 40,
            passwordFingerprint = PasswordFingerprint.of("an-older-password-1"),
        )
        val first = machine.onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", newPassword),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = listOf(oldAccount),
        )
        assertTrue(first is LoginAdvance.OfferSave)
        assertEquals(OfferReason.PASSWORD_CHANGED, (first as LoginAdvance.OfferSave).reason)

        // پس از ذخیره، مخزن اثر انگشتِ گذرواژهٔ تازه را دارد؛ دیگر نباید بپرسد.
        val savedAccount = oldAccount.copy(passwordFingerprint = PasswordFingerprint.of(newPassword))
        val second = machine.onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", newPassword),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = listOf(savedAccount),
        )
        assertEquals(LoginAdvance.Nothing, second)
    }

    @Test
    fun `known account without fingerprint is treated as already saved`() {
        val machine = intelligence()
        val advance = machine.onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = listOf(KnownAccount("user@example.com", "https://example.com")),
        )
        assertEquals(LoginAdvance.Nothing, advance)
    }

    @Test
    fun `declining is remembered as a dismissal`() {
        val machine = intelligence()
        machine.markDismissed(loginUrl)
        val advance = machine.onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = emptyList(),
        )
        assertEquals(LoginAdvance.Nothing, advance)
        assertEquals(setOf("example.com"), machine.dismissalSnapshot().keys)
    }

    @Test
    fun `otp only submissions are never stored as passwords`() {
        val advance = intelligence().onFormSubmitted(
            loginUrl,
            SubmittedLogin("09120000000", "123456", otpFieldCount = 1),
            LoginFormFacts(otpFieldCount = 1, passwordFieldCount = 0),
            knownAccounts = emptyList(),
        )
        assertEquals(LoginAdvance.Nothing, advance)
    }

    @Test
    fun `auto save mode writes unambiguous logins silently`() {
        val machine = intelligence(LoginPolicy(saveMode = CredentialSaveMode.AUTO_SAVE))
        val advance = machine.onFormSubmitted(
            loginUrl,
            SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
            LoginFormFacts(passwordFieldCount = 1),
            knownAccounts = emptyList(),
        )
        assertTrue(advance is LoginAdvance.AutoSave)
    }

    @Test
    fun `incoming code is written into the matching box once`() {
        val machine = intelligence()
        val plan = OtpFieldPlanForTest.single()
        val hit = OneTimeCodeHit("482913", 80, OneTimeCodeSource.SMS, now - 5_000L, "1000")
        val first = machine.onOtpScreen(loginUrl, hit, plan)
        assertTrue(first is LoginAdvance.FillOneTimeCode)
        assertEquals("482913", (first as LoginAdvance.FillOneTimeCode).code)
        // A failed or cancelled proposal can be retried. Only a completed fill is consumed.
        assertTrue(machine.onOtpScreen(loginUrl, hit, plan) is LoginAdvance.FillOneTimeCode)
        machine.markOneTimeCodeFilled(loginUrl, OneTimeCodeExtractor.fingerprint(hit.code))
        assertEquals(LoginAdvance.Nothing, machine.onOtpScreen(loginUrl, hit, plan))
    }

    @Test
    fun `code is not injected when it cannot be matched to a field`() {
        val machine = intelligence()
        val hit = OneTimeCodeHit("482913", 80, OneTimeCodeSource.SMS, now - 5_000L, "1000")
        assertEquals(
            LoginAdvance.Nothing,
            machine.onOtpScreen(loginUrl, hit, OtpFillPlan.NoMatch("NO_OTP_FIELD")),
        )
    }

    @Test
    fun `disable switches are respected`() {
        val noAutofill = intelligence(LoginPolicy(autoFillEnabled = false))
        assertEquals(LoginAdvance.Nothing, noAutofill.onPageProbe(loginUrl, loginForm, listOf(account())))

        val noOtp = intelligence(LoginPolicy(otpAssistEnabled = false))
        assertEquals(
            LoginAdvance.Nothing,
            noOtp.onOtpScreen(
                loginUrl,
                OneTimeCodeHit("482913", 80, OneTimeCodeSource.SMS),
                OtpFieldPlanForTest.single(),
            ),
        )
    }

    @Test
    fun `unified run entry point answers every step`() {
        val machine = intelligence()
        val load = machine.run(
            LoginFlowRequest(
                step = LoginFlowStep.PAGE_LOADED,
                url = loginUrl,
                observation = loginForm,
                accounts = listOf(account()),
            ),
        )
        assertTrue(load is LoginAdvance.FillCredentials)

        val submit = machine.run(
            LoginFlowRequest(
                step = LoginFlowStep.FORM_SUBMITTED,
                url = loginUrl,
                submitted = SubmittedLogin("user@example.com", "vT7#qLm2!xR9pWs4"),
                facts = LoginFormFacts(passwordFieldCount = 1),
            ),
        )
        assertTrue(submit is LoginAdvance.OfferSave)

        val code = machine.run(
            LoginFlowRequest(
                step = LoginFlowStep.ONE_TIME_CODE_ARRIVED,
                url = loginUrl,
                codeHit = OneTimeCodeHit("482913", 80, OneTimeCodeSource.SMS),
                fieldPlan = OtpFieldPlanForTest.single(),
            ),
        )
        assertTrue(code is LoginAdvance.FillOneTimeCode)
    }

    @Test
    fun `submission verifier waits for real success evidence`() {
        assertEquals(
            SubmissionVerdict.REJECTED_ERROR_SHOWN,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 1,
                errorTextVisible = true,
                otpFieldsAfter = 0,
                elapsedMillis = 3_000L,
                navigatedToNewUrl = false,
                originalUrl = loginUrl,
                currentUrl = loginUrl,
            ),
        )
        assertEquals(
            SubmissionVerdict.UNKNOWN,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 1,
                errorTextVisible = false,
                otpFieldsAfter = 0,
                elapsedMillis = 200L,
                navigatedToNewUrl = false,
                originalUrl = loginUrl,
                currentUrl = loginUrl,
            ),
        )
        assertEquals(
            SubmissionVerdict.ACCEPTED_NAVIGATION,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 0,
                errorTextVisible = false,
                otpFieldsAfter = 0,
                elapsedMillis = 2_000L,
                navigatedToNewUrl = true,
                originalUrl = loginUrl,
                currentUrl = "https://www.example.com/dashboard",
            ),
        )
        assertEquals(
            SubmissionVerdict.ACCEPTED_OTP_SCREEN,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 0,
                errorTextVisible = false,
                otpFieldsAfter = 1,
                elapsedMillis = 2_000L,
                navigatedToNewUrl = true,
                originalUrl = loginUrl,
                currentUrl = "https://www.example.com/verify",
            ),
        )
        assertEquals(
            SubmissionVerdict.REJECTED_FORM_STILL_THERE,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 1,
                errorTextVisible = false,
                otpFieldsAfter = 0,
                elapsedMillis = 7_000L,
                navigatedToNewUrl = false,
                originalUrl = loginUrl,
                currentUrl = loginUrl,
            ),
        )
    }

    private object OtpFieldPlanForTest {
        fun single() = OtpFillPlan.Single(fieldIndex = 2, confidence = 96, fieldName = "otp")
    }

    @Test
    fun `form still present is only rejected once the settle window has fully elapsed`() {
        // باگِ ظریفِ نسخهٔ قبل: ردکردن فقط در «دقیقاً ۶ ثانیه» ممکن بود؛ چون کاوش هر ۲.۵ ثانیه
        // یک‌بار می‌آید، در عمل هرگز به آن عدد نمی‌رسید و فرمِ سرِ جایش «در انتظار» می‌ماند تا
        // ابد. حالا «بزرگ‌تر یا مساویِ مهلت» رد می‌شود و قبل از آن صادقانه UNKNOWN است.
        fun verdict(elapsed: Long, fields: Int = 1) = LoginSubmissionVerifier.verify(
            passwordFieldsAfter = fields,
            errorTextVisible = false,
            otpFieldsAfter = 0,
            elapsedMillis = elapsed,
            navigatedToNewUrl = false,
            originalUrl = loginUrl,
            currentUrl = loginUrl,
        )
        assertEquals(SubmissionVerdict.UNKNOWN, verdict(LoginSubmissionVerifier.MIN_SETTLE_MILLIS - 1))
        assertEquals(SubmissionVerdict.UNKNOWN, verdict(LoginSubmissionVerifier.SETTLE_TIMEOUT_MILLIS - 1))
        assertEquals(SubmissionVerdict.REJECTED_FORM_STILL_THERE, verdict(LoginSubmissionVerifier.SETTLE_TIMEOUT_MILLIS))
        assertEquals(SubmissionVerdict.REJECTED_FORM_STILL_THERE, verdict(LoginSubmissionVerifier.SETTLE_TIMEOUT_MILLIS + 2_500L))
        // شاهدِ «کادرِ کد آمد» حتی پیش از پایانِ مهلت هم پذیرفته است؛ منتظر ماندن لازم نیست.
        assertEquals(
            SubmissionVerdict.ACCEPTED_OTP_SCREEN,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 0,
                errorTextVisible = false,
                otpFieldsAfter = 1,
                elapsedMillis = 0L,
                navigatedToNewUrl = false,
                originalUrl = loginUrl,
                currentUrl = loginUrl,
            ),
        )
        // پیامِ خطا همیشه برنده است، حتی پیش از پایانِ مهلت.
        assertEquals(
            SubmissionVerdict.REJECTED_ERROR_SHOWN,
            LoginSubmissionVerifier.verify(
                passwordFieldsAfter = 1,
                errorTextVisible = true,
                otpFieldsAfter = 0,
                elapsedMillis = 100L,
                navigatedToNewUrl = false,
                originalUrl = loginUrl,
                currentUrl = loginUrl,
            ),
        )
    }

    @Test
    fun `transient notices never interrupt the user`() {
        // قاعدهٔ رابط کاربری: خبرِ کوتاه در نوارِ صفحه، هشدار در دیالوگ. اگر این‌ها اشتباه دسته
        // شوند، یا کاربر برای هر کدِ نوشته‌شده یک دیالوگ می‌بیند یا هشدارِ دامنهٔ بدل ناپدید می‌شود.
        val transient = listOf(
            PasswordNotice.CodeFilled(6),
            PasswordNotice.CodeNotFound,
            PasswordNotice.CodeFieldNotFound,
            PasswordNotice.Filled("user", true),
            PasswordNotice.NoStoredLogin,
            PasswordNotice.SavedAndFilled(true),
        )
        transient.forEach { assertTrue("transient: " + it::class.simpleName, it.isTransient) }

        val blocking = listOf(
            PasswordNotice.PhishingWarning("examp1e.com", "example.com"),
            PasswordNotice.SaveFailed,
            PasswordNotice.LoginRejected,
            PasswordNotice.Weaker(
                site = "example.com",
                strength = PasswordStrength(10, PasswordStrengthLevel.WEAK),
            ),
        )
        blocking.forEach { assertTrue("blocking: " + it::class.simpleName, !it.isTransient) }
    }

    @Test
    fun `username masking hides most of the identity`() {
        // کارتِ ذخیره نباید نام کاربریِ کامل را روی صفحهٔ قفل‌شده لو بدهد.
        assertEquals("al•••@gmail.com", CredentialCaptureHeuristics.maskUsername("ali.reza@gmail.com"))
        assertEquals("a•••@b.com", CredentialCaptureHeuristics.maskUsername("a@b.com"))
        assertEquals("09•••89", CredentialCaptureHeuristics.maskUsername("09123456789"))
        assertEquals("ali", CredentialCaptureHeuristics.maskUsername("ali"))
        assertEquals("", CredentialCaptureHeuristics.maskUsername("  "))
    }
}
