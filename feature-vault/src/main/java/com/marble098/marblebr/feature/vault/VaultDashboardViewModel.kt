package com.marble098.marblebr.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.vault.SessionVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultDashboardViewModel @Inject constructor(
    private val containerDao: ContainerDao,
    private val vaultManager: SessionVaultManager,
    private val sessionManager: ContainerSessionManager,
    private val localizationManager: LocalizationManager,
    healthEventDao: HealthEventDao,
) : ViewModel() {

    val containers: StateFlow<List<WebAppContainer>> = containerDao.observeAll()
        .map { list -> list.map(ContainerEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snapshots: StateFlow<List<SessionSnapshotMeta>> = vaultManager.observeAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val healthEvents: StateFlow<List<HealthEventEntity>> = healthEventDao.observeRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun toggleSiteNeverSignOut(container: WebAppContainer, enabled: Boolean) {
        viewModelScope.launch {
            containerDao.updateNeverSignOut(container.id, enabled)
            notify { it.feedbackProtection(container.domain, enabled) }
        }
    }

    fun updateCookieExpirationDays(container: WebAppContainer, days: Int) {
        viewModelScope.launch {
            containerDao.updateCookieExpirationDays(container.id, days.coerceIn(7, 730))
            notify { it.feedbackCookieDays(container.domain, days) }
        }
    }

    fun updateKeepAlivePolicy(container: WebAppContainer, enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            containerDao.updateKeepAlivePolicy(container.id, enabled, intervalMinutes)
            notify {
                if (enabled) {
                    it.feedbackKeepAliveOn(container.domain, intervalMinutes)
                } else {
                    it.feedbackKeepAliveOff(container.domain)
                }
            }
        }
    }

    fun rollbackToSnapshot(snapshot: SessionSnapshotMeta) {
        viewModelScope.launch {
            val restored = vaultManager.restoreSnapshotById(snapshot.id)
            notify {
                if (restored != null) {
                    it.feedbackRestored(snapshot.label.ifBlank { snapshot.domain }, restored.cookies.size)
                } else {
                    it.feedbackRestoreFailed
                }
            }
        }
    }

    fun signOutNow(container: WebAppContainer) {
        viewModelScope.launch {
            sessionManager.signOutNow(container)
            notify { it.feedbackSignedOut(container.domain) }
        }
    }

    fun dismissFeedback() {
        _feedbackMessage.value = null
    }

    private fun notify(builder: (MarbleStrings) -> String) {
        viewModelScope.launch {
            _feedbackMessage.value = builder(localizationManager.current())
        }
    }
}
