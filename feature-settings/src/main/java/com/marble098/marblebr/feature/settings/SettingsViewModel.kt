package com.marble098.marblebr.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.ProxyDnsConfig
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
