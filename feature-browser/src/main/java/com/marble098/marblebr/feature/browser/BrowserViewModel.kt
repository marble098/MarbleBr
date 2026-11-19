package com.marble098.marblebr.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.database.BookmarkDao
import com.marble098.marblebr.core.database.BookmarkEntity
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.DownloadDao
import com.marble098.marblebr.core.database.DownloadEntity
import com.marble098.marblebr.core.database.HistoryDao
import com.marble098.marblebr.core.database.HistoryEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.CookieRecord
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.LightweightAdBlocker
import com.marble098.marblebr.vault.SessionVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTabId: String = "",
    val activeContainer: WebAppContainer? = null,
    val addressBarInput: String = "https://github.com",
    val isFindInPageVisible: Boolean = false,
    val findQuery: String = "",
    val bannerMessage: String? = null,
    val fallbackPromptDomain: String? = null,
) {
    val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val sessionManager: ContainerSessionManager,
    private val urlResolver: SmartAddressUrlResolver,
    private val vaultManager: SessionVaultManager,
    private val containerDao: ContainerDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao,
    private val pwaShortcutInstaller: PwaShortcutInstaller,
    private val downloadLauncher: BrowserDownloadLauncher,
    private val preferencesRepository: BrowserPreferencesRepository,
    private val localizationManager: LocalizationManager,
    val adBlocker: LightweightAdBlocker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkItem>> = bookmarkDao.observeAll()
        .map { list -> list.map(BookmarkEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyItems: StateFlow<List<HistoryItem>> = historyDao.observeRecent()
        .map { list -> list.map(HistoryEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = downloadDao.observeAll()
        .map { list -> list.map(DownloadEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        openNewTab(initialUrl = "https://github.com", isPrivate = false)
    }

    fun onAddressBarChanged(input: String) {
        _uiState.update { it.copy(addressBarInput = input) }
    }

    fun navigateToInput(rawInput: String = _uiState.value.addressBarInput) {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val targetUrl = urlResolver.resolveInputToUrl(rawInput, prefs.searchEngineTemplate)
            val currentTab = _uiState.value.activeTab ?: return@launch
            val container = sessionManager.getOrCreateContainerForUrl(targetUrl)

            updateTab(currentTab.id) {
                it.copy(
                    url = targetUrl,
                    containerId = container.id,
                    contextId = container.contextId,
                    healthStatus = if (container.neverSignOutEnabled) {
                        SessionHealthStatus.PROTECTED_ACTIVE
                    } else {
                        SessionHealthStatus.DISABLED
                    },
                )
            }
            _uiState.update {
                it.copy(
                    addressBarInput = targetUrl,
                    activeContainer = container,
                )
            }

            if (!currentTab.isPrivate) {
                historyDao.insert(
                    HistoryEntity(
                        id = UUID.randomUUID().toString(),
                        title = container.displayName,
                        url = targetUrl,
                        containerId = container.id,
                        visitedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun openNewTab(initialUrl: String = "https://github.com", isPrivate: Boolean = false) {
        viewModelScope.launch {
            val strings = localizationManager.current()
            val resolvedUrl = urlResolver.resolveInputToUrl(initialUrl)
            val container = sessionManager.getOrCreateContainerForUrl(resolvedUrl)
            val tabId = UUID.randomUUID().toString()
            val newTab = BrowserTab(
                id = tabId,
                containerId = container.id,
                contextId = if (isPrivate) "private_$tabId" else container.contextId,
                url = resolvedUrl,
                title = if (isPrivate) strings.privateTabTitle else container.displayName,
                isPrivate = isPrivate,
                healthStatus = if (isPrivate) {
                    SessionHealthStatus.DISABLED
                } else if (container.neverSignOutEnabled) {
                    SessionHealthStatus.PROTECTED_ACTIVE
                } else {
                    SessionHealthStatus.DISABLED
                },
            )
            _uiState.update { state ->
                state.copy(
                    tabs = state.tabs + newTab,
                    activeTabId = newTab.id,
                    activeContainer = container,
                    addressBarInput = resolvedUrl,
                )
            }
        }
    }

    fun selectTab(tabId: String) {
        viewModelScope.launch {
            val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return@launch
            val container = containerDao.getById(tab.containerId)?.toDomain()
                ?: sessionManager.getOrCreateContainerForUrl(tab.url)
            _uiState.update {
                it.copy(
                    activeTabId = tab.id,
                    activeContainer = container,
                    addressBarInput = tab.url,
                )
            }
        }
    }

    fun closeTab(tabId: String) {
        sessionManager.closeTabSession(tabId)
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.id == tabId }
            if (remaining.isEmpty()) {
                return@update state.copy(tabs = emptyList(), activeTabId = "")
            }
            val nextActive = if (state.activeTabId == tabId) remaining.last().id else state.activeTabId
            state.copy(tabs = remaining, activeTabId = nextActive)
        }
        if (_uiState.value.tabs.isEmpty()) {
            openNewTab()
        }
    }

    fun goBack() {
        _uiState.value.activeTab?.let { sessionManager.goBack(it.id) }
    }

    fun goForward() {
        _uiState.value.activeTab?.let { sessionManager.goForward(it.id) }
    }

    fun reloadPage() {
        _uiState.value.activeTab?.let { sessionManager.reload(it.id) }
    }

    fun toggleDesktopMode() {
        val tab = _uiState.value.activeTab ?: return
        val nextDesktop = !tab.isDesktopMode
        sessionManager.setDesktopMode(tab.id, nextDesktop)
        updateTab(tab.id) { it.copy(isDesktopMode = nextDesktop) }
        showLocalizedBanner { if (nextDesktop) it.bannerDesktopOn else it.bannerDesktopOff }
    }

    fun toggleFindInPage(visible: Boolean = !_uiState.value.isFindInPageVisible) {
        val tab = _uiState.value.activeTab
        if (!visible && tab != null) {
            sessionManager.clearFindInPage(tab.id)
        }
        _uiState.update { it.copy(isFindInPageVisible = visible, findQuery = if (visible) it.findQuery else "") }
    }

    fun updateFindQuery(query: String, backwards: Boolean = false) {
        _uiState.update { it.copy(findQuery = query) }
        val tab = _uiState.value.activeTab ?: return
        sessionManager.findInPage(tab.id, query, backwards) { current, total ->
            updateTab(tab.id) {
                it.copy(activeFindQuery = query, findMatchIndex = current, findTotalMatches = total)
            }
        }
    }

    fun toggleBookmarkCurrentPage() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            val strings = localizationManager.current()
            val existing = bookmarks.value.firstOrNull { it.url == tab.url }
            if (existing != null) {
                bookmarkDao.deleteById(existing.id)
                showBanner(strings.bannerBookmarkRemoved)
            } else {
                bookmarkDao.upsert(
                    BookmarkEntity(
                        id = UUID.randomUUID().toString(),
                        title = tab.title.ifBlank { tab.url },
                        url = tab.url,
                        folder = "general",
                        createdAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
                showBanner(strings.bannerBookmarkAdded)
            }
        }
    }

    fun installCurrentSiteAsPwa() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            val strings = localizationManager.current()
            val pwaContainer = sessionManager.getOrCreateContainerForUrl(
                url = tab.url,
                isPinnedPwa = true,
                customDisplayName = tab.title.ifBlank { null },
            )
            val shortcutTitle = tab.title.ifBlank { pwaContainer.displayName }
            val pinned = pwaShortcutInstaller.requestPinWebAppShortcut(
                container = pwaContainer,
                launchUrl = tab.url,
                title = shortcutTitle,
                longLabel = strings.pwaShortcutLongLabel(shortcutTitle),
            )
            showBanner(if (pinned) strings.bannerShortcutAdded else strings.bannerShortcutCreated)
        }
    }

    fun captureManualSnapshotNow() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            val strings = localizationManager.current()
            val container = _uiState.value.activeContainer
                ?: sessionManager.getOrCreateContainerForUrl(tab.url)

            val payload = SessionSnapshotPayload(
                origin = tab.url,
                containerId = container.id,
                capturedAtEpochMillis = System.currentTimeMillis(),
                cookies = listOf(
                    CookieRecord(
                        name = "marble_persistent_session",
                        value = "active_${System.currentTimeMillis()}",
                        domain = container.domain,
                        isSessionOnly = false,
                    ),
                ),
                localStorage = mapOf("origin" to tab.url, "persisted_by" to "MarbleBr"),
                storagePersisted = true,
            )
            vaultManager.captureSnapshot(
                payload = payload,
                targetCookieExpirationDays = container.cookieExpirationDays,
                triggerReason = "MANUAL_USER_CAPTURE",
                label = strings.manualSnapshotLabel(container.domain),
            )
            showBanner(strings.bannerSnapshotSaved)
        }
    }

    fun clearCacheWithoutTouchingSessions() {
        sessionManager.clearCachesOnly()
        showLocalizedBanner { it.bannerCacheCleared }
    }

    fun clearBrowsingHistory() {
        viewModelScope.launch {
            val strings = localizationManager.current()
            historyDao.clearAll()
            showBanner(strings.bannerHistoryCleared)
        }
    }

    fun enqueueDownload(url: String) {
        viewModelScope.launch {
            val strings = localizationManager.current()
            downloadLauncher.enqueueDownload(url = url)
            showBanner(strings.bannerDownloadStarted)
        }
    }

    fun onPageLocationChanged(tabId: String, url: String, isSecure: Boolean) {
        updateTab(tabId) { it.copy(url = url, isSecure = isSecure) }
        if (_uiState.value.activeTabId == tabId) {
            _uiState.update { it.copy(addressBarInput = url) }
        }
    }

    fun onPageTitleChanged(tabId: String, title: String) {
        updateTab(tabId) { it.copy(title = title) }
    }

    fun onPageProgressChanged(tabId: String, progress: Int, isLoading: Boolean) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = isLoading) }
    }

    fun onNavStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun onHealthRestored(reason: HealthReason) {
        val tab = _uiState.value.activeTab
        if (tab != null) {
            updateTab(tab.id) { it.copy(healthStatus = SessionHealthStatus.RECOVERED_FROM_VAULT) }
        }
        showLocalizedBanner { it.healthReasonLabel(reason) }
    }

    fun onServerRevokedFallbackRequired(domain: String) {
        val tab = _uiState.value.activeTab
        if (tab != null) {
            updateTab(tab.id) { it.copy(healthStatus = SessionHealthStatus.SERVER_REVOKED_FALLBACK) }
        }
        _uiState.update { it.copy(fallbackPromptDomain = domain) }
    }

    fun dismissFallbackPrompt() {
        _uiState.update { it.copy(fallbackPromptDomain = null) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    private fun showBanner(message: String) {
        _uiState.update { it.copy(bannerMessage = message) }
    }

    private fun showLocalizedBanner(builder: (MarbleStrings) -> String) {
        viewModelScope.launch {
            showBanner(builder(localizationManager.current()))
        }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) transform(it) else it },
            )
        }
    }
}
