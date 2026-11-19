package com.marble098.marblebr.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface BiometricAuthResult {
    data object Authenticated : BiometricAuthResult
    data object NotAvailableOrNotEnrolled : BiometricAuthResult
    data class Failed(val message: String) : BiometricAuthResult
}

/**
 * قفل امنیتی بیومتریک (BiometricPrompt) برای محافظت از اسنپ‌شات‌های Session Vault،
 * ماشین زمان (Session Time Machine) و ورود مجدد بی‌صدا با Credential Manager.
 */
@Singleton
class BiometricVaultLock @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(appContext)
        return manager.canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
    ): BiometricAuthResult {
        if (!canAuthenticate()) {
            return BiometricAuthResult.NotAvailableOrNotEnrolled
        }

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricAuthResult.Authenticated)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resume(BiometricAuthResult.Failed(errString.toString()))
                    }
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()

            prompt.authenticate(promptInfo)
        }
    }
}
