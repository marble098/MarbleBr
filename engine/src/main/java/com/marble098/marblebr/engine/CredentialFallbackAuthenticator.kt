package com.marble098.marblebr.engine

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.fragment.app.FragmentActivity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.vault.BiometricAuthResult
import com.marble098.marblebr.vault.BiometricVaultLock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SilentReloginOutcome {
    data class PasswordRetrieved(val username: String, val password: String) : SilentReloginOutcome
    data class PasskeyAssertionRetrieved(val authenticationResponseJson: String) : SilentReloginOutcome
    data class CancelledOrUnavailable(val reason: String) : SilentReloginOutcome
}

/**
 * ویژگی ۸ Never Sign-out Engine (آخرین سد دفاعی):
 * اگر سرور نشست را واقعاً در سمت خود باطل کرده باشد (تغییر رمز عبور، انقضای مطلق سمت سرور)،
 * با استفاده از Credential Manager / Passkey اندروید و تأیید بیومتریک، ورود مجدد بی‌صدا انجام می‌شود.
 */
@Singleton
class CredentialFallbackAuthenticator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val biometricVaultLock: BiometricVaultLock,
    private val localizationManager: LocalizationManager,
) {
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(appContext)
    }

    suspend fun attemptBiometricRelogin(
        activity: FragmentActivity,
        rpIdOrDomain: String,
        webAuthnChallengeJson: String? = null,
    ): SilentReloginOutcome {
        val strings = localizationManager.current()

        // 1. Verify biometric lock before releasing stored credentials/passkeys
        val bioResult = biometricVaultLock.authenticate(
            activity = activity,
            title = strings.reloginAutoTitle(rpIdOrDomain),
            subtitle = strings.reloginPromptSubtitle,
        )
        if (bioResult is BiometricAuthResult.Failed) {
            val message = bioResult.message.ifBlank { strings.biometricCancelled }
            return SilentReloginOutcome.CancelledOrUnavailable(message)
        }

        // 2. Request Passkey or saved Password from Android Credential Manager
        return runCatching {
            val requestBuilder = GetCredentialRequest.Builder()
                .addCredentialOption(GetPasswordOption())

            if (!webAuthnChallengeJson.isNullOrBlank()) {
                requestBuilder.addCredentialOption(
                    GetPublicKeyCredentialOption(requestJson = webAuthnChallengeJson),
                )
            }

            val response = credentialManager.getCredential(
                context = activity,
                request = requestBuilder.build(),
            )

            when (val credential = response.credential) {
                is PasswordCredential -> SilentReloginOutcome.PasswordRetrieved(
                    username = credential.id,
                    password = credential.password,
                )
                is PublicKeyCredential -> SilentReloginOutcome.PasskeyAssertionRetrieved(
                    authenticationResponseJson = credential.authenticationResponseJson,
                )
                // «افزودن هر سایتی»: به‌جای پیامِ «پشتیبانی نمی‌شود»، راهِ جایگزین پیشنهاد می‌شود.
                is CustomCredential -> SilentReloginOutcome.CancelledOrUnavailable(strings.reloginManualHint)
                else -> SilentReloginOutcome.CancelledOrUnavailable(strings.reloginUnavailable)
            }
        }.getOrElse { error ->
            SilentReloginOutcome.CancelledOrUnavailable(error.localizedMessage ?: strings.reloginUnavailable)
        }
    }
}
