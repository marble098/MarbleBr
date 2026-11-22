package com.marble098.marblebr.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.BackButtonBehavior
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.ColorPalette
import com.marble098.marblebr.core.model.CredentialSaveMode
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyValidation
import com.marble098.marblebr.core.model.ProxyValidator
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.SearchEngine
import com.marble098.marblebr.core.model.ThemeMode
import com.marble098.marblebr.core.preferences.AddressSelectionMode
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.KeepAliveScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: BrowserPreferencesRepository,
    private val keepAliveScheduler: KeepAliveScheduler,
    private val sessionManager: ContainerSessionManager,
    private val localizationManager: LocalizationManager,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val preferences: StateFlow<BrowserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserPreferences())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val appVersionName: String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull().orEmpty()

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    /** انتخاب پالت رنگی؛ از همین لحظه کل اپ با تمِ تازه بازترکیب می‌شود. */
    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch {
            preferencesRepository.setColorPalette(palette)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColorEnabled(enabled)
        }
    }

    fun setGlobalNeverSignOut(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setGlobalNeverSignOut(enabled)
        }
    }

    fun setBiometricLockRequired(required: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricLockRequired(required)
        }
    }

    fun setSmartKeepAliveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmartKeepAliveEnabled(enabled)
            if (enabled) {
                keepAliveScheduler.schedulePeriodicKeepAlive()
            } else {
                keepAliveScheduler.cancelKeepAlive()
            }
        }
    }

    fun setCredentialFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCredentialFallbackEnabled(enabled)
        }
    }

    /**
     * حالتِ ذخیرهٔ ورود. سه گزینه است و هر سه در مخزنِ رمز اثر می‌گذارند، پس تغییر فوراً در
     * DataStore نوشته می‌شود و `PasswordStudio` در اولین تصمیمِ بعدی آن را می‌خواند.
     */
    fun setCredentialSaveMode(mode: CredentialSaveMode) {
        viewModelScope.launch {
            preferencesRepository.setCredentialSaveMode(mode)
        }
    }

    fun setOtpAssistEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOtpAssistEnabled(enabled)
        }
    }

    fun setAutoFillCredentials(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoFillCredentials(enabled)
        }
    }

    fun setLoginGuardEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLoginGuardEnabled(enabled)
        }
    }

    fun setSessionPulseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSessionPulseEnabled(enabled)
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAdBlockEnabled(enabled)
        }
    }

    fun selectSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            preferencesRepository.setSearchEngine(engine)
        }
    }

    fun setAddressSelectionMode(mode: AddressSelectionMode) {
        viewModelScope.launch { preferencesRepository.setAddressSelectionMode(mode) }
    }

    fun setOpenGoogleOnNewTab(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOpenGoogleOnNewTab(enabled) }
    }

    fun setHomeShortcutPosition(position: HomeShortcutPosition) {
        viewModelScope.launch {
            preferencesRepository.setHomeShortcutPosition(position)
        }
    }

    fun setChromeBarStyle(style: ChromeBarStyle) {
        viewModelScope.launch { preferencesRepository.setChromeBarStyle(style) }
    }

    fun setBackButtonBehavior(behavior: BackButtonBehavior) {
        viewModelScope.launch { preferencesRepository.setBackButtonBehavior(behavior) }
    }

    fun setAskBeforeClosingTabs(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAskBeforeClosingTabs(enabled) }
    }

    /**
     * اعتبارسنجیِ همین‌جا و پیش از ذخیره.
     *
     * **باگِ نسخهٔ قبل:** پورتِ نامعتبر با `?: 1080` بی‌صدا جایگزین می‌شد و میزبانِ خالی هم
     * ذخیره می‌شد؛ کاربر «ذخیره شد» می‌دید ولی پروکسی هیچ‌وقت اعمال نمی‌شد. حالا یا همه‌چیز
     * معتبر است یا پیامِ روشنِ خطا و هیچ ذخیره‌ای.
     */
    fun validateProxy(type: ProxyType, host: String, portText: String): ProxyValidation {
        if (type == ProxyType.NONE) return ProxyValidation(isValid = true)
        val port = portText.trim().toIntOrNull()
            ?: return ProxyValidation(isValid = false, portInvalid = true)
        return ProxyValidator.validate(type, host, port)
    }

    /** پیامِ خطای مناسب برای هر حالتِ نامعتبر (یا null اگر همه‌چیز درست است). */
    fun proxyValidationMessage(validation: ProxyValidation): String? {
        if (validation.isValid) return null
        val strings = localizationManager.strings(preferences.value.language)
        return when {
            validation.hostBlank || validation.hostInvalid -> strings.proxyHostInvalid
            validation.portInvalid -> strings.proxyPortInvalid
            else -> strings.proxyHostInvalid
        }
    }

    /**
     * ذخیرهٔ DNS/پروکسی؛ [Boolean] نشان می‌دهد ذخیره انجام شد یا نه.
     */
    fun saveNetwork(
        dnsMode: DnsMode,
        customDohUri: String,
        proxyType: ProxyType,
        proxyHost: String,
        proxyPortText: String,
    ): Boolean {
        val validation = validateProxy(proxyType, proxyHost, proxyPortText)
        proxyValidationMessage(validation)?.let { message ->
            _snackbarMessage.value = message
            return false
        }
        val port = proxyPortText.trim().toIntOrNull() ?: 1080
        updateDnsAndProxy(
            dnsMode = dnsMode,
            customDohUri = customDohUri,
            proxyType = proxyType,
            proxyHost = ProxyValidator.normalizeHost(proxyHost),
            proxyPort = port,
        )
        return true
    }

    /**
     * آیا موتورِ Gecko قبلاً ساخته شده؟ پروکسی فقط در ساختِ موتور خوانده می‌شود، پس اگر
     * موتور بالا آمده باشد کاربر باید اپ را ببند و دوباره باز کند.
     */
    fun isRuntimeCreated(): Boolean = sessionManager.isRuntimeCreated()

    fun updateDnsAndProxy(
        dnsMode: DnsMode,
        customDohUri: String,
        proxyType: ProxyType,
        proxyHost: String,
        proxyPort: Int,
    ) {
        viewModelScope.launch {
            preferencesRepository.setProxyDnsConfig(
                ProxyDnsConfig(
                    dnsMode = dnsMode,
                    customDohUri = customDohUri,
                    proxyType = proxyType,
                    proxyHost = proxyHost,
                    proxyPort = proxyPort,
                ),
            )
            notify { it.networkSaved }
        }
    }

    fun clearCachesOnly() {
        sessionManager.clearCachesOnly()
        notify { it.freeSpaceDone }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    private fun notify(builder: (com.marble098.marblebr.core.i18n.MarbleStrings) -> String) {
        viewModelScope.launch {
            _snackbarMessage.value = builder(localizationManager.current())
        }
    }
}
