package com.marble098.marblebr.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.model.DnsMode
import com.marble098.marblebr.core.model.ProxyDnsConfig
import com.marble098.marblebr.core.model.ProxyType
import com.marble098.marblebr.core.model.ThemeMode
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.KeepAliveScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: BrowserPreferencesRepository,
    private val keepAliveScheduler: KeepAliveScheduler,
    private val sessionManager: ContainerSessionManager,
) : ViewModel() {

    val preferences: StateFlow<BrowserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserPreferences())

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

    fun setForcePersianRtl(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setForcePersianRtl(enabled)
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
        }
    }

    fun clearCachesOnly() {
        sessionManager.clearCachesOnly()
    }
}
