package com.marble098.marblebr.vault

import com.marble098.marblebr.core.model.CredentialSaveMode
import com.marble098.marblebr.core.model.LoginPolicy
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * پلِ تنظیمات برای لایهٔ ورود.
 *
 * چرا رابط (Interface)؟ تا `:vault` به `:core` وابسته بماند و در آزمون‌های واحد بتوان یک نسخهٔ
 * درون‌حافظه‌ای تزریق کرد؛ نسخهٔ قبلی `PasswordStudio` مستقیم به DataStore وصل بود و همین باعث
 * می‌شد مسیرِ «پرکردنِ خودکار» فقط با اجرای کاملِ اپ قابلِ آزمون باشد.
 */
interface LoginSettings {
    /** تنظیماتِ ورود به‌صورتِ جریانِ زنده؛ هر تغییر در صفحهٔ تنظیمات فوراً اثر می‌گذارد. */
    fun policyFlow(): Flow<LoginPolicy>

    /** «الآن نه»های ماندگار برای هر دامنه. */
    fun dismissalsFlow(): Flow<Map<String, Long>>

    suspend fun rememberDismissal(domain: String, atEpochMillis: Long)

    suspend fun clearDismissal(domain: String)
}

/** پیاده‌سازیِ واقعی روی DataStore. */
@Singleton
class PreferencesLoginSettings @Inject constructor(
    private val preferencesRepository: BrowserPreferencesRepository,
) : LoginSettings {

    override fun policyFlow(): Flow<LoginPolicy> = preferencesRepository.preferencesFlow.map { it.toLoginPolicy() }

    override fun dismissalsFlow(): Flow<Map<String, Long>> = preferencesRepository.loginDismissals

    override suspend fun rememberDismissal(domain: String, atEpochMillis: Long) {
        preferencesRepository.rememberLoginDismissal(domain, atEpochMillis)
    }

    override suspend fun clearDismissal(domain: String) {
        preferencesRepository.clearLoginDismissal(domain)
    }

    private fun BrowserPreferences.toLoginPolicy(): LoginPolicy = LoginPolicy(
        saveMode = credentialSaveMode,
        autoFillEnabled = autoFillCredentials,
        loginGuardEnabled = loginGuardEnabled,
        otpAssistEnabled = otpAssistEnabled,
        // بیومتریک در هر نشست دوباره پرسیده می‌شود؛ این مقدار در `PasswordStudio` پس از تأیید
        // موفق به‌روز می‌شود.
        biometricConfirmedInSession = false,
        biometricLockRequired = biometricLockRequired,
    )

    companion object {
        /** حالتِ ذخیرهٔ پیش‌فرض وقتی تنظیمات هنوز خوانده نشده است. */
        val DEFAULT_POLICY = LoginPolicy(saveMode = CredentialSaveMode.ASK)
    }
}
