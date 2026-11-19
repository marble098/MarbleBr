package com.marble098.marblebr.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
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
    healthEventDao: HealthEventDao,
) : ViewModel() {

    val containers: StateFlow<List<WebAppContainer>> = containerDao.observeAll()
        .map { list -> list.map(ContainerEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snapshots: StateFlow<List<SessionSnapshotMeta>> = vaultManager.observeAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val healthEvents: StateFlow<List<HealthEventEntity>> = healthEventDao.observeRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _feedbackMessageFa = MutableStateFlow<String?>(null)
    val feedbackMessageFa: StateFlow<String?> = _feedbackMessageFa.asStateFlow()

    fun toggleSiteNeverSignOut(container: WebAppContainer, enabled: Boolean) {
        viewModelScope.launch {
            containerDao.updateNeverSignOut(container.id, enabled)
            _feedbackMessageFa.value = if (enabled) {
                "Never Sign-out برای ${container.domain} فعال شد"
            } else {
                "Never Sign-out برای ${container.domain} غیرفعال شد"
            }
        }
    }

    fun updateCookieExpirationDays(container: WebAppContainer, days: Int) {
        viewModelScope.launch {
            containerDao.updateCookieExpirationDays(container.id, days.coerceIn(7, 730))
            _feedbackMessageFa.value = "ماندگاری کوکی‌های ${container.domain} روی $days روز تنظیم شد"
        }
    }

    fun updateKeepAlivePolicy(container: WebAppContainer, enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            containerDao.updateKeepAlivePolicy(container.id, enabled, intervalMinutes)
            _feedbackMessageFa.value = if (enabled) {
                "Smart Keep-alive برای ${container.domain} (هر $intervalMinutes دقیقه) فعال شد"
            } else {
                "Smart Keep-alive برای ${container.domain} غیرفعال شد"
            }
        }
    }

    fun rollbackToSnapshot(snapshot: SessionSnapshotMeta) {
        viewModelScope.launch {
            val restored = vaultManager.restoreSnapshotById(snapshot.id)
            _feedbackMessageFa.value = if (restored != null) {
                "بازگشت با یک لمس به اسنپ‌شات «${snapshot.label}» (${restored.cookies.size} کوکی) با موفقیت انجام شد"
            } else {
                "خطا در رمزگشایی اسنپ‌شات"
            }
        }
    }

    fun signOutNow(container: WebAppContainer) {
        viewModelScope.launch {
            sessionManager.signOutNow(container)
            _feedbackMessageFa.value =
                "خروج واقعی از ${container.domain} انجام شد و تمام کوکی‌ها و اسنپ‌شات‌های آن کانتینر پاک شدند"
        }
    }

    fun dismissFeedback() {
        _feedbackMessageFa.value = null
    }
}
