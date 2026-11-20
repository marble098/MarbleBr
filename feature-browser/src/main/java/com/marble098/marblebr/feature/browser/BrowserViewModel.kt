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
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.core.preferences.SavedBrowserTabs
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.LightweightAdBlocker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val showHome: Boolean = true,
    val canReopenTab: Boolean = false,
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

    val preferences: StateFlow<BrowserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserPreferences())

    val bookmarks: StateFlow<List<BookmarkItem>> = bookmarkDao.observeAll()
        .map { list -> list.map(BookmarkEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyItems: StateFlow<List<HistoryItem>> = historyDao.observeRecent()
        .map { list -> list.map(HistoryEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = downloadDao.observeAll()
        .map { list -> list.map(DownloadEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val containers = mutableMapOf<String, WebAppContainer>()
    private val pendingNavigation = mutableMapOf<String, Job>()
    private val recentlyClosed = ArrayDeque<BrowserTab>()
    private val restore = viewModelScope.launch { restoreTabs() }

    init {
        viewModelScope.launch {
            restore.join()
            uiState.map { SavedBrowserTabs.fromTabs(it.tabs, it.activeTabId, it.showHome) }
                .distinctUntilChanged()
                .collect { snapshot -> preferencesRepository.saveBrowserTabs(snapshot) }
        }
    }

    private suspend fun restoreTabs() {
        val saved = preferencesRepository.savedBrowserTabs.first()
        if (saved == null || saved.tabs.isEmpty()) {
            if (preferencesRepository.preferencesFlow.first().openGoogleOnNewTab) {
                loadPage(createTab(isPrivate = false), GOOGLE_HOME)
            }
            return
        }
        val tabs = saved.tabs.map { tab ->
            val container = if (tab.url.isBlank()) {
                null
            } else {
                containerDao.getById(tab.containerId)?.toDomain() ?: sessionManager.getOrCreateContainerForUrl(tab.url)
            }
            if (container != null) containers[tab.id] = container
            BrowserTab(
                id = tab.id,
                containerId = container?.id.orEmpty(),
                contextId = container?.contextId.orEmpty(),
                url = tab.url,
                title = tab.title,
                isDesktopMode = tab.isDesktopMode,
                isLoading = container != null,
                isSecure = tab.url.startsWith("https://"),
                healthStatus = healthStatus(container),
            )
        }
        val active = tabs.firstOrNull { it.id == saved.activeTabId } ?: tabs.lastOrNull()
        _uiState.update { state ->
            state.copy(
                tabs = tabs,
                activeTabId = active?.id.orEmpty(),
                activeContainer = active?.id?.let(containers::get),
                showHome = saved.showHome || active?.url.isNullOrBlank(),
            )
        }
    }

    fun navigateToInput(rawInput: String) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            restore.join()
            loadPage(_uiState.value.activeTab ?: createTab(isPrivate = false), rawInput)
        }
    }

    fun openNewTab(initialUrl: String? = null, isPrivate: Boolean = false, pinnedContainerId: String? = null) {
        viewModelScope.launch {
            restore.join()
            val tab = createTab(isPrivate)
            val input = initialUrl ?: if (!isPrivate && preferencesRepository.preferencesFlow.first().openGoogleOnNewTab) GOOGLE_HOME else ""
            val pinned = if (!isPrivate && pinnedContainerId != null) {
                containerDao.getById(pinnedContainerId)?.toDomain()?.takeIf { it.isPinnedPwa && it.pwaStartUrl == input }
            } else {
                null
            }
            if (input.isNotBlank() && uiState.value.tabs.any { it.id == tab.id }) loadPage(tab, input, pinned)
        }
    }

    private fun createTab(isPrivate: Boolean, desktop: Boolean = false): BrowserTab {
        val tab = BrowserTab(
            id = UUID.randomUUID().toString(),
            containerId = "",
            contextId = "",
            url = "",
            isPrivate = isPrivate,
            isDesktopMode = desktop,
            isSecure = false,
            healthStatus = SessionHealthStatus.DISABLED,
        )
        _uiState.update { it.copy(tabs = it.tabs + tab) }
        activateTab(tab)
        return tab
    }

    private fun loadPage(tab: BrowserTab, input: String, preferredContainer: WebAppContainer? = null) {
        pendingNavigation.remove(tab.id)?.cancel()
        if (_uiState.value.activeTabId == tab.id) toggleFindInPage(false)
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tab.id) it.copy(isLoading = true, progress = 0) else it },
                showHome = state.showHome && state.activeTabId != tab.id,
            )
        }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val target = urlResolver.resolveInputToUrl(input, preferencesRepository.preferencesFlow.first().searchEngineTemplate)
                val domain = ContainerSessionManager.extractDomain(target)
                val pinned = containers[tab.id]?.takeIf { it.isPinnedPwa && it.domain == domain }
                val container = preferredContainer ?: pinned ?: if (tab.isPrivate) {
                    WebAppContainer(id = tab.id, domain = domain, displayName = "")
                } else {
                    sessionManager.getOrCreateContainerForUrl(target)
                }
                val recreate = !tab.isPrivate && containers[tab.id]?.contextId != container.contextId
                if (recreate) sessionManager.closeTabSession(tab.id)
                containers[tab.id] = container
                updateTab(tab.id) {
                    it.copy(
                        url = target,
                        title = container.displayName,
                        containerId = container.id,
                        contextId = container.contextId,
                        isSecure = target.startsWith("https://"),
                        canGoBack = !recreate && it.canGoBack,
                        canGoForward = !recreate && it.canGoForward,
                        healthStatus = healthStatus(container.takeUnless { tab.isPrivate }),
                    )
                }
                _uiState.update { if (it.activeTabId == tab.id) it.copy(activeContainer = container) else it }
                if (!recreate) sessionManager.loadInOpenSession(tab.id, target)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateTab(tab.id) { it.copy(isLoading = false) }
                if (tab.url.isBlank() && uiState.value.activeTabId == tab.id) showHome()
                showBanner(localizationManager.current().navigationFailed)
            } finally {
                if (pendingNavigation[tab.id] === coroutineContext[Job]) pendingNavigation.remove(tab.id)
            }
        }
        pendingNavigation[tab.id] = job
        job.start()
    }

    private fun healthStatus(container: WebAppContainer?): SessionHealthStatus =
        if (container?.neverSignOutEnabled == true) SessionHealthStatus.PROTECTED_ACTIVE else SessionHealthStatus.DISABLED

    fun showHome() {
        toggleFindInPage(false)
        _uiState.update { it.copy(showHome = true) }
    }

    fun selectTab(tabId: String) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        activateTab(tab)
    }

    private fun activateTab(tab: BrowserTab?) {
        toggleFindInPage(false)
        _uiState.update {
            it.copy(
                activeTabId = tab?.id.orEmpty(),
                activeContainer = tab?.id?.let(containers::get),
                showHome = tab?.url.isNullOrBlank() && tab?.isLoading != true,
            )
        }
    }

    fun closeTab(tabId: String) {
        val state = _uiState.value
        val tab = state.tabs.firstOrNull { it.id == tabId } ?: return
        rememberClosedTab(tab)
        releaseTab(tabId)
        val remaining = state.tabs.filterNot { it.id == tabId }
        _uiState.update { it.copy(tabs = remaining, canReopenTab = recentlyClosed.isNotEmpty()) }
        if (state.activeTabId == tabId) activateTab(remaining.lastOrNull())
        if (state.showHome) showHome()
    }

    fun closeAllTabs() {
        // «بیمهٔ نشست»: پیش از بستنِ همهٔ تب‌ها یک نسخهٔ تازه از صفحهٔ فعلی گرفته می‌شود تا
        // بستنِ اشتباهی هیچ ورودی را از بین نبرد.
        viewModelScope.launch {
            insureActivePage()
            closeAllTabsNow()
        }
    }

    private fun closeAllTabsNow() {
        val state = _uiState.value
        val active = state.activeTab
        (state.tabs.filterNot { it == active } + listOfNotNull(active)).forEach { tab ->
            rememberClosedTab(tab)
            releaseTab(tab.id)
        }
        _uiState.update { it.copy(tabs = emptyList(), canReopenTab = recentlyClosed.isNotEmpty()) }
        activateTab(null)
    }

    private fun releaseTab(tabId: String) {
        pendingNavigation.remove(tabId)?.cancel()
        containers.remove(tabId)
        sessionManager.closeTabSession(tabId)
    }

    private fun rememberClosedTab(tab: BrowserTab) {
        if (tab.isPrivate || tab.url.isBlank()) return
        if (recentlyClosed.size >= 10) recentlyClosed.removeFirst()
        recentlyClosed.addLast(tab)
    }

    fun reopenClosedTab() {
        val tab = recentlyClosed.removeLastOrNull() ?: return
        _uiState.update { it.copy(canReopenTab = recentlyClosed.isNotEmpty()) }
        copyTab(tab)
    }

    fun duplicateTab() {
        _uiState.value.activeTab?.let(::copyTab)
    }

    private fun copyTab(source: BrowserTab) {
        val tab = createTab(source.isPrivate, desktop = source.isDesktopMode)
        if (source.url.isNotBlank()) loadPage(tab, source.url, containers[source.id]?.takeUnless { source.isPrivate })
    }

    fun stopLoading() {
        val tab = _uiState.value.activeTab ?: return
        pendingNavigation.remove(tab.id)?.cancel()
        sessionManager.stopLoading(tab.id)
        updateTab(tab.id) { it.copy(isLoading = false) }
        if (tab.url.isBlank()) showHome()
    }

    override fun onCleared() {
        _uiState.value.tabs.forEach { sessionManager.closeTabSession(it.id) }
        super.onCleared()
    }

    fun goBack() {
        _uiState.value.activeTab?.let { sessionManager.goBack(it.id) }
    }

    fun goForward() {
        _uiState.value.activeTab?.let { sessionManager.goForward(it.id) }
    }

    fun reloadPage() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.url.isBlank()) return
        updateTab(tab.id) { it.copy(progress = 0, isLoading = true) }
        sessionManager.reload(tab.id)
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
            if (tab.url.isBlank() || tab.isPrivate) return@launch
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
            if (tab.url.isBlank() || tab.isPrivate) return@launch
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

    /** خروجِ کامل از سایتِ تبِ فعلی (Sign out now) با همان مسیر امنِ داشبورد. */
    fun signOutCurrentSite() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            if (tab.url.isBlank() || tab.isPrivate) return@launch
            val container = containers[tab.id]
                ?: containerDao.getById(tab.containerId)?.toDomain()
                ?: return@launch
            sessionManager.signOutNow(container)
            containers.remove(tab.id)
            updateTab(tab.id) {
                it.copy(healthStatus = SessionHealthStatus.DISABLED, canGoBack = false, canGoForward = false)
            }
            showLocalizedBanner { it.feedbackSignedOut(container.domain) }
        }
    }

    fun captureManualSnapshotNow() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            val strings = localizationManager.current()
            if (tab.url.isBlank() || tab.isPrivate) return@launch
            val saved = sessionManager.capturePageSnapshot(tab.id, tab.url)
            showBanner(if (saved) strings.bannerSnapshotSaved else strings.snapshotUnavailable)
        }
    }

    fun clearCacheWithoutTouchingSessions() {
        viewModelScope.launch {
            // «بیمهٔ نشست»: قبل از پاک‌کردنِ کش، نسخهٔ تازهٔ صفحه ذخیره می‌شود.
            insureActivePage()
            sessionManager.clearCachesOnly()
            showLocalizedBanner { it.bannerCacheCleared }
        }
    }

    /**
     * «بیمهٔ نشست» (پیش از اقدام‌های پرخطر): یک نسخهٔ تازه از صفحهٔ فعلی می‌گیرد.
     * خطاها بی‌صدا رد می‌شوند؛ این کار نباید هیچ‌وقت جلوی کار اصلیِ کاربر را بگیرد.
     */
    private suspend fun insureActivePage() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.url.isBlank() || tab.isPrivate) return
        runCatching { sessionManager.capturePageSnapshot(tab.id, tab.url) }
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
            val started = downloadLauncher.enqueueDownload(url = url)
            showBanner(if (started) strings.bannerDownloadStarted else strings.downloadFailed)
        }
    }

    suspend fun refreshDownloads() {
        downloadLauncher.refreshDownloads()
    }

    fun onPageLocationChanged(tabId: String, url: String, isSecure: Boolean) {
        if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) return
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) { it.copy(url = url, isSecure = isSecure) }
        if (!tab.isPrivate) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val title = containers[tabId]?.displayName ?: url
                val existing = historyDao.getLatestByUrl(url)
                if (existing != null) {
                    historyDao.updateVisit(existing.id, title, now)
                } else {
                    historyDao.insert(HistoryEntity(UUID.randomUUID().toString(), title, url, tab.containerId, now))
                }
            }
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

    /**
     * نتیجهٔ عکس‌برداری از صفحه را به کاربر اطلاع می‌دهد.
     * مسیر خالی/`null` یعنی عکس گرفته نشد.
     */
    fun reportScreenshot(path: String?) {
        if (path.isNullOrBlank()) {
            showLocalizedBanner { it.snapshotUnavailable }
        } else {
            showBanner("✓ " + path.substringAfterLast('/'))
        }
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

    private companion object {
        const val GOOGLE_HOME = "https://www.google.com/"
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { if (it.id == tabId) transform(it) else it },
            )
        }
    }
}
