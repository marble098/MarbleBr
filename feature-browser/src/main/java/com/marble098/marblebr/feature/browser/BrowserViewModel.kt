package com.marble098.marblebr.feature.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.marble098.marblebr.core.model.AddressSuggestion
import com.marble098.marblebr.core.model.AddressSuggestionEngine
import com.marble098.marblebr.core.model.AutofillCandidate
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.HealthReason
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.PermissionState
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.SessionPulsePlanner
import com.marble098.marblebr.core.model.SitePermission
import com.marble098.marblebr.core.model.SitePermissionPolicy
import com.marble098.marblebr.core.model.SitePermissionProfile
import com.marble098.marblebr.core.model.SiteRoute
import com.marble098.marblebr.core.model.TabTitleEngine
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import com.marble098.marblebr.core.preferences.SavedBrowserTabs
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.LightweightAdBlocker
import com.marble098.marblebr.engine.LoginWatcher
import com.marble098.marblebr.engine.PageContextTarget
import com.marble098.marblebr.engine.ContentPermissionRequest
import com.marble098.marblebr.core.model.CredentialCaptureHeuristics
import com.marble098.marblebr.core.model.PasswordNotice
import com.marble098.marblebr.vault.LoginActivity
import com.marble098.marblebr.vault.LoginPageDirective
import com.marble098.marblebr.vault.OneTimeCodeCapability
import com.marble098.marblebr.vault.PasswordSavePrompt
import com.marble098.marblebr.vault.PasswordSaveResult
import com.marble098.marblebr.vault.PasswordStudio
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * یک درخواستِ دسترسی که از سوی صفحه آمده و منتظرِ پاسخِ کاربر است.
 *
 * چرا مدلِ جدا؟ چون سه مسیرِ متفاوت (popup، دسترسیِ محتوایی Gecko و دسترسیِ اندروید)
 * یکی باید در رابط کاربری دیده شوند و پاسخ با همان `respond` به Gecko برگردد؛ در غیر این
 * صورت `GeckoResult` هرگز کامل نمی‌شود و صفحهٔ سایت تا ابد منتظر می‌ماند.
 */
/** نتیجهٔ جستجو در کتابخانه (نشان‌ها + تاریخچه). */
data class LibrarySearchResult(
    val bookmarks: List<BookmarkItem> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
)

data class PendingSitePermission(
    val kind: Kind,
    val tabId: String,
    val url: String,
    val permission: SitePermission? = null,
    val androidPermissions: List<String> = emptyList(),
    val respond: (Boolean) -> Unit,
) {
    enum class Kind { POPUP, CONTENT, ANDROID }
}

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
    val contextTarget: PageContextTarget? = null,
    /** حالت مطالعه روی تبِ فعال روشن است؟ */
    val isReaderModeOn: Boolean = false,
    /** متنِ در حال بلندخوانی (اگر بلندخوانی فعال باشد). */
    val readAloudText: String? = null,
    /** پروفایلِ دسترسی‌های سایتِ تبِ فعال. */
    val activePermissionProfile: SitePermissionProfile? = null,
    /** بستهٔ دسترسی‌ها باز است؟ */
    val showSitePermissions: Boolean = false,
    /** نردبانِ «افزودن هر سایتی» برای صفحهٔ جاری. */
    val sitePlan: com.marble098.marblebr.core.model.SiteAdditionPlan? = null,
    /** تبی که نردبان به آن تعلق دارد. */
    val sitePlanTabId: String? = null,
    /** آدرسی که کاربر خواسته اضافه کند (برای نمایش و بازمحاسبهٔ نردبان). */
    val sitePlanTitle: String? = null,
    /** پنلِ «چطور وارد شویم» و پرکردنِ دستی. */
    val showSiteCompatibilitySheet: Boolean = false,
    /** پرسشِ «برای پرکردنِ ورود، اثر انگشت» باز است؟ */
    val showAutofillUnlockPrompt: Boolean = false,
    /** آیا برای دامنهٔ همین صفحه، ورودِ ذخیره‌شده داریم؟ (برای منوی «پر کردن») */
    val hasStoredLoginForCurrentSite: Boolean = false,
    /** متنِ در حال تایپ در نوار آدرس (برای پیشنهادهای زنده). */
    val addressQuery: String = "",
    /** آیا نوار آدرس فوکوس دارد و پیشنهادها باید نمایش داده شوند؟ */
    val addressFocused: Boolean = false,
    /** پرسشِ منتظرِ پاسخِ کاربر: popup یا دسترسیِ محتوایی. */
    val pendingPermission: PendingSitePermission? = null,
) {
    val activeTab: BrowserTab?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()

    /**
     * تبِ فعال فقط اگر صفحه‌ای باز باشد.
     *
     * چرا این تابع لازم است؟ در حالت «خانه»، GeckoSession هنوز برای تبِ قبلی وجود دارد و
     * ابزارهایی مثل حالت مطالعه/بلندخوانی/دسترسی‌ها نباید روی صفحهٔ قبلی اجرا شوند.
     */
    fun activeWebTab(): BrowserTab? = if (showHome) null else activeTab?.takeIf { it.url.isNotBlank() }

    /** A cross-site navigation remains in its original context (important for OAuth redirects). */
    fun activeAccountForCurrentSite(): WebAppContainer? {
        val tab = activeWebTab()?.takeUnless { it.isPrivate } ?: return null
        return activeContainer?.takeIf { it.domain == ContainerSessionManager.extractDomain(tab.url) }
    }
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
    private val sitePermissionStore: com.marble098.marblebr.core.preferences.SitePermissionStore,
    private val loginWatcher: LoginWatcher,
    val passwordStudio: PasswordStudio,
    private val siteEngine: com.marble098.marblebr.engine.SiteCompatibilityEngine,
    @ApplicationContext private val appContext: Context,
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

    /**
     * پیشنهادهای زندهٔ نوار آدرس: تاریخچه، نشان‌ها و پربازدیدها.
     *
     * نقدِ نسخهٔ قبل: نوار آدرس هیچ پیشنهادی نداشت؛ کاربر باید کل آدرس را از حفظ می‌نوشت.
     */
    val addressSuggestions: StateFlow<List<AddressSuggestion>> =
        combine(bookmarks, historyItems, uiState) { marks, visits, state ->
            AddressSuggestionEngine.build(
                query = state.addressQuery,
                history = visits,
                bookmarks = marks,
                limit = 8,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** شمارندهٔ گذرواژه‌های ذخیره‌شده برای کارتِ «نگاهِ مخزن» در صفحهٔ خانه. */
    val storedPasswordCount: StateFlow<Int> = passwordStudio.candidatesForHome()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /**
     * آیا برای دامنهٔ صفحهٔ فعلی، ورودِ ذخیره‌شده‌ای داریم؟
     *
     * منوی سایت با همین پرچم، «پر کردنِ فرم» را نشان می‌دهد؛ یعنی هیچ‌وقت دکمه‌ای نمی‌بینیم که
     * کار نکند. دامنه‌ها با همان قاعدهٔ مخزن سنجیده می‌شوند (زیردامنه‌ها پذیرفته‌اند).
     */
    private val hasStoredLoginForCurrentSite: StateFlow<Boolean> = run {
        val domains = passwordStudio.candidatesForHome()
            .map { candidates -> candidates.map { it.domain.lowercase() }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet<String>())
        combine(domains, uiState) { known, state ->
            val site = state.activeWebTab()?.url?.let(CredentialCaptureHeuristics::domainOf)
                ?: return@combine false
            known.any { it == site || site.endsWith(".$it") || it.endsWith(".$site") }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    }

    /** تعدادِ سایت‌هایی که «مراقبت از ورود» روی آن‌ها روشن است. */
    val protectedSiteCount: StateFlow<Int> = containerDao.observeAll()
        .map { entries -> entries.count { it.neverSignOutEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** دامنه‌هایی که «نبضِ نشست» برایشان پیشنهادِ بیدارسازی دارد. */
    val pulseDomains: StateFlow<List<String>> = combine(containerDao.observeAll(), historyItems) { entries, history ->
        val patterns = SessionPulsePlanner.patternsFromHistory(history, System.currentTimeMillis())
            .map { pattern ->
                pattern.copy(
                    neverSignOutEnabled = entries.firstOrNull { it.domain == pattern.domain }?.neverSignOutEnabled
                        ?: false,
                    cookieDaysConfigured = entries.firstOrNull { it.domain == pattern.domain }?.cookieExpirationDays
                        ?: 365,
                )
            }
            .filter { it.neverSignOutEnabled }
        SessionPulsePlanner.plan(patterns, System.currentTimeMillis())
            .map { it.domain }
            .distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val siteAccounts: StateFlow<List<WebAppContainer>> = combine(containerDao.observeAll(), uiState) { entries, state ->
        val domain = state.activeAccountForCurrentSite()?.domain
        if (domain == null) {
            emptyList()
        } else {
            entries
            .filter { it.domain == domain && !it.isPinnedPwa }
            .map { it.toDomain() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val popupOpeners = mutableMapOf<String, String>()
    private val containers = mutableMapOf<String, WebAppContainer>()
    private val pendingNavigation = mutableMapOf<String, Job>()
    private val recentlyClosed = ArrayDeque<BrowserTab>()
    private val restore = viewModelScope.launch { restoreTabs() }

    init {
        viewModelScope.launch {
            hasStoredLoginForCurrentSite.collect { hasStored ->
                _uiState.update { it.copy(hasStoredLoginForCurrentSite = hasStored) }
            }
        }
        viewModelScope.launch {
            restore.join()
            containerDao.observeAll().collect { entities ->
                val freshById = entities.associate { it.id to it.toDomain() }
                containers.toMap().forEach { (tabId, old) -> freshById[old.id]?.let { containers[tabId] = it } }
                _uiState.update { state ->
                    state.copy(
                        activeContainer = containers[state.activeTabId],
                        tabs = state.tabs.map { tab ->
                            val current = containers[tab.id]
                            if (tab.isPrivate || current == null) {
                                tab
                            } else {
                                val status = when {
                                    !current.neverSignOutEnabled -> SessionHealthStatus.DISABLED
                                    tab.healthStatus == SessionHealthStatus.DISABLED -> SessionHealthStatus.PROTECTED_ACTIVE
                                    else -> tab.healthStatus
                                }
                                tab.copy(healthStatus = status)
                            }
                        },
                    )
                }
            }
        }
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
            if (container != null) {
                containers[tab.id] = container
                sessionManager.applySitePermissions(tab.id, sitePermissionStore.profileFor(tab.url))
            }
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

    /** Open another durable account in a new tab, keeping all other contexts untouched. */
    fun createSiteAccount(name: String) {
        val state = _uiState.value
        val source = state.activeAccountForCurrentSite() ?: return
        val tab = state.activeWebTab() ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val account = sessionManager.createAdditionalAccount(source, name)
            val target = tab.url.takeIf { ContainerSessionManager.extractDomain(it) == source.domain }
                ?: "https://${source.domain}/"
            loadPage(createTab(isPrivate = false), target, account)
        }
    }

    fun openSiteAccount(account: WebAppContainer) {
        val state = _uiState.value
        val source = state.activeAccountForCurrentSite() ?: return
        val tab = state.activeWebTab() ?: return
        if (account.domain != source.domain || account.isPinnedPwa) return
        val existing = state.tabs.firstOrNull {
            !it.isPrivate && it.containerId == account.id && ContainerSessionManager.extractDomain(it.url) == account.domain
        }
        if (existing != null) {
            selectTab(existing.id)
        } else {
            val target = tab.url.takeIf { ContainerSessionManager.extractDomain(it) == account.domain }
                ?: "https://${account.domain}/"
            loadPage(createTab(isPrivate = false), target, account)
        }
    }

    fun openLinkInNewTab(url: String, sourceTabId: String? = null) {
        val source = sourceTabId?.let { id -> _uiState.value.tabs.firstOrNull { it.id == id } }
            ?: _uiState.value.activeWebTab()
            ?: return
        val current = containers[source.id]
            ?.takeIf { !source.isPrivate && it.domain == ContainerSessionManager.extractDomain(url) }
        loadPage(createTab(isPrivate = source.isPrivate, desktop = source.isDesktopMode), url, current)
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
        if (_uiState.value.pendingPermission?.tabId == tab.id) dismissPendingPermission()
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
                val current = containers[tab.id]?.takeIf {
                    it.domain == domain || ContainerSessionManager.extractDomain(tab.url) == domain
                }
                val container = preferredContainer ?: current ?: if (tab.isPrivate) {
                    WebAppContainer(id = tab.id, domain = domain, displayName = "")
                } else {
                    sessionManager.getOrCreateContainerForUrl(target)
                }
                val recreate = !tab.isPrivate && containers[tab.id]?.contextId != container.contextId
                if (recreate) sessionManager.closeTabSession(tab.id)
                val profile = sitePermissionStore.profileFor(target)
                sessionManager.applySitePermissions(tab.id, profile)
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

    private fun BrowserPreferences.toLoginPolicy() = com.marble098.marblebr.core.model.LoginPolicy(
        saveMode = credentialSaveMode,
        autoFillEnabled = autoFillCredentials,
        loginGuardEnabled = loginGuardEnabled,
        otpAssistEnabled = otpAssistEnabled,
        biometricLockRequired = biometricLockRequired,
    )

    private fun healthStatus(container: WebAppContainer?): SessionHealthStatus =
        if (container?.neverSignOutEnabled == true) SessionHealthStatus.PROTECTED_ACTIVE else SessionHealthStatus.DISABLED

    fun showHome() {
        dismissPendingPermission()
        toggleFindInPage(false)
        _uiState.update { it.copy(showHome = true) }
    }

    fun selectTab(tabId: String) {
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        activateTab(tab)
    }

    private fun activateTab(tab: BrowserTab?) {
        if (_uiState.value.pendingPermission?.tabId != tab?.id) dismissPendingPermission()
        toggleFindInPage(false)
        _uiState.update {
            it.copy(
                activeTabId = tab?.id.orEmpty(),
                activeContainer = tab?.id?.let(containers::get),
                showHome = tab?.url.isNullOrBlank() && tab?.isLoading != true,
                contextTarget = null,
                activePermissionProfile = tab?.id?.let(sessionManager::permissionProfileOf),
            )
        }
    }

    fun closeTab(tabId: String) {
        val state = _uiState.value
        val tab = state.tabs.firstOrNull { it.id == tabId } ?: return
        rememberClosedTab(tab)
        val opener = popupOpeners[tabId]
        releaseTab(tabId)
        val remaining = state.tabs.filterNot { it.id == tabId }
        _uiState.update { it.copy(tabs = remaining, canReopenTab = recentlyClosed.isNotEmpty()) }
        if (state.activeTabId == tabId) activateTab(remaining.firstOrNull { it.id == opener } ?: remaining.lastOrNull())
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
        if (_uiState.value.pendingPermission?.tabId == tabId) dismissPendingPermission()
        popupOpeners.remove(tabId)
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
        if (source.url.isNotBlank()) {
            viewModelScope.launch {
                val account = if (source.isPrivate) {
                    null
                } else {
                    containers[source.id] ?: containerDao.getById(source.containerId)?.toDomain()
                }
                if (uiState.value.tabs.any { it.id == tab.id }) loadPage(tab, source.url, account)
            }
        }
    }

    fun stopLoading() {
        val tab = _uiState.value.activeTab ?: return
        pendingNavigation.remove(tab.id)?.cancel()
        sessionManager.stopLoading(tab.id)
        updateTab(tab.id) { it.copy(isLoading = false) }
        if (tab.url.isBlank()) showHome()
    }

    override fun onCleared() {
        dismissPendingPermission()
        if (codeListenerStarted) loginWatcher.stopSmsListening()
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

    /** به‌اشتراک‌گذاشتن پیوند همین صفحه با مرورگرهای دیگر (اپ‌های نصب‌شدهٔ Android). */
    fun shareCurrentPage() {
        val tab = _uiState.value.activeTab ?: return
        val url = tab.url.takeIf { it.isNotBlank() } ?: return
        val strings = localizationManager.strings()
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
        appContext.startActivity(Intent.createChooser(intent, strings.menuSharePage))
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
            _uiState.update { state ->
                state.copy(
                    showHome = true,
                    tabs = state.tabs.map {
                        if (it.containerId == container.id) {
                            it.copy(healthStatus = SessionHealthStatus.DISABLED, canGoBack = false, canGoForward = false)
                        } else {
                            it
                        }
                    },
                )
            }
            showLocalizedBanner { it.feedbackSignedOut(container.domain) }
        }
    }

    fun captureManualSnapshotNow() {
        viewModelScope.launch {
            val tab = _uiState.value.activeTab ?: return@launch
            val strings = localizationManager.current()
            if (tab.url.isBlank() || tab.isPrivate) return@launch
            val saved = sessionManager.capturePageSnapshot(tab.id, tab.url, manual = true)
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

    fun onAddressQueryChanged(query: String) {
        _uiState.update { if (it.addressQuery == query) it else it.copy(addressQuery = query) }
    }

    fun onAddressFocusChanged(focused: Boolean) {
        _uiState.update { if (it.addressFocused == focused) it else it.copy(addressFocused = focused) }
    }

    internal fun browserSession(
        tab: BrowserTab,
        container: WebAppContainer,
        openedByGecko: Boolean = false,
    ): org.mozilla.geckoview.GeckoSession = sessionManager.getOrCreateGeckoSession(
        tabId = tab.id,
        container = container,
        initialUrl = tab.url,
        isPrivate = tab.isPrivate,
        isDesktopMode = tab.isDesktopMode,
        adBlockEnabled = preferences.value.adBlockEnabled,
        onLocationChanged = { url, secure -> onPageLocationChanged(tab.id, url, secure) },
        onTitleChanged = { title -> onPageTitleChanged(tab.id, title) },
        onProgressChanged = { progress, loading -> onPageProgressChanged(tab.id, progress, loading) },
        onNavStateChanged = { back, fwd -> onNavStateChanged(tab.id, back, fwd) },
        onHealthRestored = ::onHealthRestored,
        onServerRevokedFallbackRequired = ::onServerRevokedFallbackRequired,
        onDownloadRequested = { url, _ -> if (!tab.isPrivate) enqueueDownload(url) },
        onContextMenu = { target -> onPageContextMenu(tab.id, target) },
        onPopupRequest = ::onPopupRequest,
        onContentPermissionRequest = ::onContentPermissionRequest,
        onAndroidPermissionRequest = ::onAndroidPermissionRequest,
        onWindowClose = { if (openedByGecko) closeTab(tab.id) },
        openedByGecko = openedByGecko,
    )

    private fun onPopupRequest(request: com.marble098.marblebr.engine.PopupPermissionRequest) {
        val respond: (Boolean) -> Unit = { allow ->
            val source = _uiState.value.activeWebTab()?.takeIf { it.id == request.tabId }
            val container = source?.let { containers[it.id] }
            if (allow && source != null && container != null) {
                val tab = source.copy(
                    id = UUID.randomUUID().toString(),
                    url = request.targetUri,
                    title = "",
                    isLoading = true,
                    progress = 0,
                    isSecure = false,
                    canGoBack = false,
                    canGoForward = false,
                )
                containers[tab.id] = container
                popupOpeners[tab.id] = source.id
                val session = browserSession(tab, container, openedByGecko = true)
                _uiState.update { it.copy(tabs = it.tabs + tab) }
                request.respond(session)
                activateTab(tab)
            } else {
                request.respond(null)
            }
        }
        if (!request.shouldPrompt && canAcceptPermissionFrom(request.tabId)) {
            respond(true)
        } else {
            val displayUrl = request.targetUri.takeUnless { it == "about:blank" }
                ?: _uiState.value.activeWebTab()?.url.orEmpty()
            offerPermission(PendingSitePermission(PendingSitePermission.Kind.POPUP, request.tabId, displayUrl, respond = respond))
        }
    }

    private fun onContentPermissionRequest(request: ContentPermissionRequest) {
        offerPermission(
            PendingSitePermission(
                kind = PendingSitePermission.Kind.CONTENT,
                tabId = request.tabId,
                url = request.url,
                permission = request.permission,
                respond = request.respond,
            ),
        )
    }

    private fun onAndroidPermissionRequest(request: com.marble098.marblebr.engine.AndroidPermissionRequest) {
        offerPermission(
            PendingSitePermission(
                kind = PendingSitePermission.Kind.ANDROID,
                tabId = request.tabId,
                url = "",
                androidPermissions = request.permissions,
                respond = request.respond,
            ),
        )
    }

    private fun canAcceptPermissionFrom(tabId: String): Boolean = _uiState.value.let {
        it.activeTabId == tabId && !it.showHome && it.pendingPermission == null
    }

    private fun offerPermission(request: PendingSitePermission) {
        if (canAcceptPermissionFrom(request.tabId)) {
            _uiState.update { it.copy(pendingPermission = request) }
        } else {
            request.respond(false)
        }
    }

    internal fun resolvePendingPermission(request: PendingSitePermission, allow: Boolean) {
        if (_uiState.value.pendingPermission !== request) return
        _uiState.update { it.copy(pendingPermission = null) }
        request.respond(allow)
    }

    fun dismissPendingPermission() {
        _uiState.value.pendingPermission?.let { resolvePendingPermission(it, false) }
    }

    /* ------------------------------------------ کتابخانه (نشان‌ها/تاریخچه) ------------------------------------------ */

    /**
     * افزودنِ دستیِ نشان. آدرس بدونِ پروتکل هم پذیرفته می‌شود تا کاربر لازم نباشد
     * `https://` را تایپ کند؛ ولی آدرسِ خالی یا بدونِ نقطه رد می‌شود.
     */
    fun addBookmark(title: String, url: String, folder: String = "general", onResult: (Boolean) -> Unit = {}) {
        val normalizedUrl = url.trim().let { value ->
            when {
                value.isEmpty() -> ""
                value.startsWith("http://", true) || value.startsWith("https://", true) -> value
                else -> "https://$value"
            }
        }
        val domain = normalizedUrl.removePrefix("https://").removePrefix("http://")
        if (normalizedUrl.isBlank() || !domain.contains('.') || domain.contains(' ')) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val existing = bookmarks.value.firstOrNull { it.url == normalizedUrl }
            bookmarkDao.upsert(
                BookmarkEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    title = title.trim().ifBlank { domain },
                    url = normalizedUrl,
                    folder = folder.trim().ifBlank { "general" },
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis(),
                ),
            )
            showLocalizedBanner { it.libraryBookmarkSaved }
            onResult(true)
        }
    }

    fun updateBookmark(bookmark: BookmarkItem, title: String, url: String, folder: String) {
        viewModelScope.launch {
            bookmarkDao.updateBookmark(
                id = bookmark.id,
                title = title.trim().ifBlank { bookmark.title },
                url = url.trim().ifBlank { bookmark.url },
                folder = folder.trim().ifBlank { "general" },
            )
            showLocalizedBanner { it.libraryBookmarkUpdated }
        }
    }

    fun moveBookmarkToFolder(bookmark: BookmarkItem, folder: String) {
        viewModelScope.launch {
            bookmarkDao.moveToFolder(bookmark.id, folder.trim().ifBlank { "general" })
        }
    }

    fun deleteBookmark(bookmark: BookmarkItem) {
        viewModelScope.launch {
            bookmarkDao.deleteById(bookmark.id)
            showLocalizedBanner { it.libraryBookmarkDeleted }
        }
    }

    /** حذفِ یک ردیفِ تاریخچه (همان آدرس، همهٔ بازدیدهایش). */
    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            historyDao.deleteByUrl(item.url)
            showLocalizedBanner { it.libraryHistoryDeleted }
        }
    }

    /** حذفِ همهٔ بازدیدهای یک دامنه. */
    fun deleteHistoryForSite(url: String) {
        viewModelScope.launch {
            val domain = CredentialCaptureHeuristics.domainOf(url)
            if (domain.isBlank()) return@launch
            historyDao.deleteByUrl(url)
            showLocalizedBanner { it.librarySiteHistoryDeleted }
        }
    }

    /** پاک‌کردنِ تاریخچهٔ قدیمی‌تر از [days] روز (برای «پاک‌سازیِ دوره‌ای»). */
    fun clearHistoryOlderThan(days: Int) {
        viewModelScope.launch {
            historyDao.deleteOlderThan(System.currentTimeMillis() - days * 86_400_000L)
        }
    }

    /** جستجو در نشان‌ها و تاریخچه؛ نتیجه در دو فهرستِ جدا برمی‌گردد. */
    suspend fun searchLibrary(query: String): LibrarySearchResult {
        val needle = query.trim()
        if (needle.isBlank()) return LibrarySearchResult()
        return LibrarySearchResult(
            bookmarks = bookmarkDao.search(needle).map(BookmarkEntity::toDomain),
            history = historyDao.search(needle).map(HistoryEntity::toDomain),
        )
    }

    fun onPageContextMenu(tabId: String, target: PageContextTarget) {
        if (_uiState.value.activeWebTab()?.id == tabId) _uiState.update { it.copy(contextTarget = target) }
    }

    fun dismissPageContextMenu() {
        _uiState.update { it.copy(contextTarget = null) }
    }

    fun savePageAsPdf(tabId: String, destination: Uri) {
        sessionManager.savePageAsPdf(tabId, destination) { saved ->
            showLocalizedBanner { if (saved) it.pdfSaved else it.pdfFailed }
        }
    }

    fun onPageLocationChanged(tabId: String, url: String, isSecure: Boolean) {
        if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) return
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        if (ContainerSessionManager.extractDomain(tab.url) != ContainerSessionManager.extractDomain(url) &&
            _uiState.value.pendingPermission?.tabId == tabId
        ) {
            dismissPendingPermission()
        }
        updateTab(tabId) { it.copy(url = url, isSecure = isSecure) }
        // دسترسی‌های همان دامنه روی نشست اعمال می‌شوند (جاوااسکریپت/پخش خودکار/دیالوگ‌ها).
        viewModelScope.launch {
            val profile = sitePermissionStore.profileFor(url)
            if (_uiState.value.tabs.none { it.id == tabId && it.url == url }) return@launch
            sessionManager.applySitePermissions(tabId, profile)
            _uiState.update { state ->
                if (state.activeTabId == tabId) state.copy(activePermissionProfile = profile) else state
            }
        }
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
        // نامِ هوشمند تب: عنوانِ طولانی/نویزی سایت پاک‌سازی می‌شود و در صورت خالی بودن،
        // دامنهٔ سایت جایگزین می‌شود؛ بنابراین نوار تب‌ها هرگز عنوانِ خالی یا بریده ندارد.
        updateTab(tabId) { it.copy(title = TabTitleEngine.smartTabTitle(title, it.url)) }
    }

    /**
     * پیشرفتِ بارگذاری؛ پایانِ بارگذاری نقطهٔ شروعِ همهٔ تصمیم‌های «ورود» است.
     *
     * چرا همین‌جا و نه در `LoginWatcher`؟ چون این لایه از نوعِ تب و وضعیتِ فعال/خصوصی باخبر است و
     * می‌تواند دکمهٔ «کد را بگذار» را هم به‌روز کند. تصمیم‌گیری همچنان در لایهٔ موتور است.
     */
    fun onPageProgressChanged(tabId: String, progress: Int, isLoading: Boolean) {
        updateTab(tabId) { it.copy(progress = progress, isLoading = isLoading) }
        if (!isLoading) {
            val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
            if (tab.url.startsWith("http")) onPageSettled(tabId, tab.url)
        }
    }

    fun onNavStateChanged(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    /* ------------------------------------ سه قابلیتِ تازه ------------------------------------ */

    /** روشن/خاموش کردنِ «حالت مطالعه» برای تبِ فعال. */
    fun toggleReaderMode() {
        val tab = _uiState.value.activeWebTab()
        if (tab == null || tab.url.isBlank()) {
            showLocalizedBanner { it.readerModeUnavailable }
            return
        }
        val next = !_uiState.value.isReaderModeOn
        sessionManager.setReaderMode(tab.id, next) { applied ->
            if (!applied) {
                showLocalizedBanner { it.readerModeUnavailable }
            } else {
                _uiState.update { it.copy(isReaderModeOn = next) }
                showLocalizedBanner { if (next) it.readerModeOn else it.readerModeOff }
            }
        }
    }

    /**
     * «بلندخوانی صفحه»: متنِ اصلی از صفحه استخراج می‌شود و در state قرار می‌گیرد تا لایهٔ
     * UI آن را به موتور گفتار گوشی بدهد (TextToSpeech). برای توقف، [stopReadAloud].
     */
    fun startReadAloud() {
        val tab = _uiState.value.activeWebTab()
        if (tab == null || tab.url.isBlank()) {
            showLocalizedBanner { it.readAloudUnavailable }
            return
        }
        sessionManager.readPageText(tab.id) { text ->
            if (text.isBlank()) {
                showLocalizedBanner { it.readAloudUnavailable }
            } else {
                _uiState.update { it.copy(readAloudText = text) }
            }
        }
    }

    fun stopReadAloud() {
        _uiState.update { it.copy(readAloudText = null) }
    }

    /** باز/بسته کردنِ پنلِ «دسترسی‌های سایت». */
    fun toggleSitePermissionsPanel(visible: Boolean = !_uiState.value.showSitePermissions) {
        _uiState.update { it.copy(showSitePermissions = visible) }
        if (visible) {
            viewModelScope.launch {
                val tab = _uiState.value.activeWebTab() ?: return@launch
                val profile = sitePermissionStore.profileFor(tab.url)
                _uiState.update { it.copy(activePermissionProfile = profile) }
            }
        }
    }

    /** تغییرِ وضعیتِ یک دسترسی و اعمالِ فوری روی نشست. */
    fun setSitePermission(permission: SitePermission, state: PermissionState) {
        viewModelScope.launch {
            val tab = _uiState.value.activeWebTab() ?: return@launch
            if (tab.url.isBlank()) return@launch
            sitePermissionStore.setPermission(tab.url, permission, state)
            val profile = sitePermissionStore.profileFor(tab.url)
            sessionManager.applySitePermissions(tab.id, profile)
            _uiState.update { it.copy(activePermissionProfile = profile) }
        }
    }

    /** چرخهٔ سه‌حالتهٔ یک دسترسی (اجازه → بپرس → بلاک). */
    fun cycleSitePermission(permission: SitePermission) {
        val current = _uiState.value.activePermissionProfile?.stateOf(permission)
            ?: SitePermissionPolicy.defaults[permission]
            ?: PermissionState.ASK
        setSitePermission(permission, SitePermissionPolicy.nextState(current))
    }

    /** برگشتِ همهٔ دسترسی‌های این سایت به پیش‌فرض. */
    fun resetSitePermissions() {
        viewModelScope.launch {
            val tab = _uiState.value.activeWebTab() ?: return@launch
            sitePermissionStore.reset(tab.url)
            val profile = sitePermissionStore.profileFor(tab.url)
            sessionManager.applySitePermissions(tab.id, profile)
            _uiState.update {
                it.copy(
                    activePermissionProfile = profile,
                )
            }
            showLocalizedBanner { it.sitePermissionsResetDone }
        }
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

    /* ------------------------- ذخیرهٔ هوشمند گذرواژه و افزودن هر سایتی ------------------------- */

    /** کارتِ «این ورود را ذخیره کنم؟» که از موتورِ ورود می‌آید. */
    val savePrompt: StateFlow<PasswordSavePrompt?> = passwordStudio.pendingPrompt

    /** تذکر/هشدارِ کوتاه (ذخیره شد، کد نوشته شد، دامنهٔ بدل، …). */
    val passwordNotice: StateFlow<PasswordNotice?> = passwordStudio.notice

    /** وضعیتِ زندهٔ ورود در همین تب (برای نوارِ بالای مرورگر). */
    val loginActivity: StateFlow<LoginActivity?> = passwordStudio.activity

    private var codeListenerStarted = false

    /** آیا این صفحه کادرِ رمزِ یکبارمصرف دارد؟ دکمهٔ «کد را بگذار» با همین نمایش داده می‌شود. */
    private val _codeFieldAvailable = MutableStateFlow(false)
    val codeFieldAvailable: StateFlow<Boolean> = _codeFieldAvailable.asStateFlow()

    /** دستورِ پرکردنِ منتظرِ تأییدِ بیومتریک. */
    private var pendingFill: LoginPageDirective.FillCredentials? = null

    /**
     * پس از هر بار کامل‌شدنِ صفحه (از نوار پیشرفتِ مرورگر).
     *
     * اگر استودیو یک دستورِ پرکردن داده باشد و قفلِ بیومتریک لازم باشد، همین‌جا درخواستِ
     * اثر انگشت آماده می‌شود؛ در غیرِ این صورت اجرا انجام می‌شود. این همان حلقه‌ای است که در
     * نسخهٔ قبلی نبود و «پرکردنِ خودکار» را عملاً بی‌اثر می‌کرد.
     */
    fun onPageSettled(tabId: String, url: String) {
        viewModelScope.launch {
            val directive = loginWatcher.consumeFillDirective() ?: run {
                _codeFieldAvailable.value = loginWatcher.pageAcceptsOneTimeCode(tabId, url)
                return@launch
            }
            pendingFill = directive
            if (directive.needsBiometricConfirmation) {
                _uiState.update { it.copy(showAutofillUnlockPrompt = true) }
            } else {
                applyPendingFill()
            }
            _codeFieldAvailable.value = loginWatcher.pageAcceptsOneTimeCode(tabId, url)
        }
    }

    /**
     * «این ورود را با اثر انگشت پر کن» — کاری که منوی میانبر و نوارِ ورود انجام می‌دهند.
     * اگر اعتمادِ بیومتریک در همین نشست هست، بدونِ پرسش دوباره پر می‌کند.
     */
    fun requestAutofill() {
        viewModelScope.launch {
            val tab = _uiState.value.activeWebTab() ?: return@launch
            val url = tab.url
            if (url.isBlank()) return@launch
            val accounts = passwordStudio.candidatesFor(url)
            if (accounts.isEmpty()) {
                showLocalizedBanner { it.saveLoginNoStored }
                return@launch
            }
            if (passwordStudio.requiresBiometricForFill()) {
                _uiState.update { it.copy(showAutofillUnlockPrompt = true) }
            } else {
                autofillFromVault(accounts.first().id)
            }
        }
    }

    /** لایهٔ رابط پس از تأییدِ بیومتریک این را صدا می‌زند. */
    fun onBiometricConfirmedForAutofill() {
        passwordStudio.onBiometricConfirmed()
        _uiState.update { it.copy(showAutofillUnlockPrompt = false) }
        val directive = pendingFill
        if (directive != null) {
            applyPendingFill()
        } else {
            requestAutofillDirect()
        }
    }

    fun dismissAutofillUnlock() {
        passwordStudio.onBiometricDeclined()
        pendingFill = null
        _uiState.update { it.copy(showAutofillUnlockPrompt = false) }
    }

    private fun requestAutofillDirect() {
        viewModelScope.launch {
            val tab = _uiState.value.activeWebTab() ?: return@launch
            val candidate = passwordStudio.candidatesFor(tab.url).firstOrNull() ?: return@launch
            autofillFromVault(candidate.id)
        }
    }

    private fun applyPendingFill() {
        // `pendingFill` فقط وقتی پاک می‌شود که واقعاً اجرا شود؛ اگر تبی در کار نباشد، دستور
        // برای بارِ بعد می‌ماند (مثلاً کاربر هنگامِ پرسشِ اثر انگشت به صفحهٔ دیگری رفته است).
        val directive = pendingFill ?: return
        val tab = _uiState.value.activeWebTab() ?: return
        if (tab.isPrivate) {
            pendingFill = null
            return
        }
        pendingFill = null
        viewModelScope.launch {
            val filled = loginWatcher.executeAutofill(tab.id, directive.script)
            if (filled) {
                // «پر شد» بدونِ نامِ کاربری: هیچ شناسه‌ای در نوارِ بالای صفحه دیده نمی‌شود.
                showLocalizedBanner { it.saveLoginFillAction }
            } else {
                // صفحه عوض شده یا فرمی نبوده؛ دکمهٔ «کد را بگذار» را به‌روز می‌کنیم و ساکت می‌مانیم.
                _codeFieldAvailable.value = loginWatcher.pageAcceptsOneTimeCode(tab.id, tab.url)
            }
        }
    }

    /** دکمهٔ «کد را بگذار»: کد را از پیامک/اعلان پیدا و در کادر می‌نویسد. */
    fun fillOneTimeCodeNow() {
        val tab = _uiState.value.activeWebTab() ?: return
        viewModelScope.launch {
            val filled = loginWatcher.fillOneTimeCodeNow(tab.id, tab.url)
            if (!filled) {
                // «کدی پیدا نشد» یا «کادرِ کد نبود» — دو پیامِ متفاوت، نه یک متنِ مبهم.
                showLocalizedBanner { it.codeUnavailableTitle }
            }
        }
    }

    /**
     * شروعِ گوش‌دادن به پیامک‌های تازه (اگر دسترسی باشد). کم‌هزینه است: یک گیرندهٔ برادکست که
     * فقط تا وقتی «رمزِ یکبارمصرف» روشن است زنده می‌ماند، و در `onCleared` بسته می‌شود.
     */
    fun startCodeListening(force: Boolean = false) {
        if (codeListenerStarted && !force) return
        // فقط وقتی پرچم را روشن می‌کنیم که گیرنده واقعاً ثبت شده باشد. پیش‌تر این پرچم
        // پیش از دادنِ دسترسی `true` می‌شد و بعد از «اجازه» دیگر هرگز شنونده شروع نمی‌شد.
        codeListenerStarted = loginWatcher.startSmsListening { hit -> passwordStudio.onCodeArrived(hit) }
    }

    fun stopCodeListening() {
        if (codeListenerStarted) loginWatcher.stopSmsListening()
        codeListenerStarted = false
        passwordStudio.clearLatestCode()
    }

    /** آیا اپ می‌تواند کد را از پیامک/اعلان بخواند؟ برای راهنمای تنظیمات. */
    fun oneTimeCodeCapability(): OneTimeCodeCapability =
        passwordStudio.oneTimeCodeCapability(preferences.value.toLoginPolicy())

    /** تأییدِ کاربر: ذخیرهٔ ورود در مخزنِ رمزنگاری‌شده. */
    fun acceptPasswordPrompt(editedUsername: String? = null) {
        viewModelScope.launch {
            when (passwordStudio.acceptPendingPrompt(editedUsername)) {
                is PasswordSaveResult.Saved -> showLocalizedBanner { it.saveLoginSaved }
                PasswordSaveResult.Failed -> showLocalizedBanner { it.saveLoginFailed }
                PasswordSaveResult.NothingToSave -> Unit
            }
        }
    }

    fun dismissPasswordPrompt() {
        viewModelScope.launch {
            passwordStudio.dismissPendingPrompt()
        }
    }

    /** «برای این سایت دیگر نپرس»: ردِ همیشگی، نه فقط مهلتِ چندساعتهٔ «الآن نه». */
    fun neverAskForSiteAgain() {
        viewModelScope.launch {
            passwordStudio.neverAskForSiteAgain()
        }
    }

    fun consumePasswordNotice() {
        passwordStudio.consumeNotice()
    }

    /** پرکردنِ فرمِ ورودِ همین صفحه از رکوردی که کاربر انتخاب می‌کند. */
    fun autofillFromVault(credentialId: String) {
        val tab = _uiState.value.activeWebTab() ?: return
        viewModelScope.launch {
            val script = passwordStudio.buildManualAutofillScript(tab.url, credentialId)
            if (script == null) {
                showLocalizedBanner { it.saveLoginNoStored }
                return@launch
            }
            val filled = loginWatcher.executeAutofill(tab.id, script)
            if (!filled) showLocalizedBanner { it.saveLoginNoStored }
        }
    }

    /**
     * «افزودن هر سایتی»: به‌جای ردکردنِ سایت، نردبانِ راه‌ها را می‌سازد.
     *
     * اولین مسیر (معمولاً بازکردنِ مستقیم) همین حالا اجرا می‌شود و مسیرهای بعدی در
     * [BrowserUiState.sitePlan] می‌مانند تا اگر صفحه جواب نداد، کاربر با یک ضربه بعدی را برود.
     */
    fun addAnySite(rawInput: String) {
        if (rawInput.isBlank()) return
        viewModelScope.launch {
            restore.join()
            val strings = localizationManager.current()
            val tab = createTab(isPrivate = false)
            // نردبانِ قبلی نباید با نردبانِ آدرسِ تازه قاطی شود.
            loginWatcher.forget(tab.id)
            val plan = runCatching {
                siteEngine.planFor(tabId = tab.id, url = rawInput, preferDesktop = false)
            }.getOrNull()
            val first = plan?.best
            if (plan == null || first == null) {
                loadPage(tab, rawInput)
            } else {
                updateTab(tab.id) { it.copy(isDesktopMode = first.useDesktopUserAgent) }
                loadPage(tab, first.targetUrl)
            }
            if (plan != null) {
                _uiState.update {
                    it.copy(
                        sitePlan = plan,
                        sitePlanTabId = tab.id,
                        sitePlanTitle = rawInput,
                        bannerMessage = strings.addAnySiteRoute(routeLabel(plan.best?.route, strings)),
                    )
                }
            }
        }
    }

    /** مسیر بعدیِ همان نردبان؛ `null` یعنی همهٔ راه‌ها امتحان شده‌اند. */
    fun tryNextSiteRoute() {
        val plan = _uiState.value.sitePlan ?: return
        val tabId = _uiState.value.sitePlanTabId ?: return
        val tab = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        viewModelScope.launch {
            val strings = localizationManager.current()
            // اگر کاربر خودش آدرس را عوض کرده، نردبانِ تازه از آدرسِ واقعیِ تب ساخته می‌شود.
            if (tab.url.startsWith("http") && tab.url != plan.steps.firstOrNull()?.targetUrl) {
                runCatching {
                    _uiState.update { it.copy(sitePlan = siteEngine.planFor(tabId, tab.url)) }
                }
            }
            val currentPlan = _uiState.value.sitePlan ?: return@launch
            val next = currentPlan.steps.firstOrNull()
            if (next == null) {
                siteEngine.markRouteFailed(tabId, SiteRoute.DIRECT)
                _uiState.update { it.copy(sitePlan = null, sitePlanTabId = null, sitePlanTitle = null) }
                showBanner(strings.addAnySiteTriedAll)
                return@launch
            }
            siteEngine.markRouteFailed(tabId, next.route)
            val refreshed = siteEngine.planFor(tabId, _uiState.value.sitePlanTitle.orEmpty())
            val attempt = siteEngine.stateOf(tabId).attemptedRoutes.size
            updateTab(tabId) { it.copy(isDesktopMode = next.useDesktopUserAgent) }
            if (next.route == SiteRoute.VAULT_MANUAL_LOGIN) {
                // ورودِ دستی: پنلِ مخزن باز می‌شود، نه یک صفحهٔ دیگر.
                _uiState.update { it.copy(showSiteCompatibilitySheet = true) }
                return@launch
            }
            loadPage(tab, next.targetUrl)
            _uiState.update {
                it.copy(
                    sitePlan = refreshed,
                    bannerMessage = strings.addAnySiteFallback(attempt = attempt, of = currentPlan.steps.size),
                )
            }
        }
    }

    fun dismissSitePlan() {
        _uiState.update {
            it.copy(sitePlan = null, sitePlanTabId = null, sitePlanTitle = null, showSiteCompatibilitySheet = false)
        }
    }

    fun openSiteCompatibilitySheet() {
        _uiState.update { it.copy(showSiteCompatibilitySheet = true) }
    }

    fun closeSiteCompatibilitySheet() {
        _uiState.update { it.copy(showSiteCompatibilitySheet = false) }
    }

    /** کاندیدهای پرکردن برای پنلِ دستی (بدونِ گذرواژه). */
    suspend fun siteCandidates(url: String): List<AutofillCandidate> = passwordStudio.candidatesFor(url)

    /**
     * «همین فرم را ذخیره کن» — برای وقتی که کاربر می‌داند ورودش موفق بوده ولی کارت نیامده
     * (مثلاً سایت با لایهٔ جاوااسکریپت ناوبری می‌کند و رویدادِ پایانِ بارگذاری نمی‌آید).
     */
    fun checkForSubmittableLogin() {
        val tab = _uiState.value.activeWebTab() ?: return
        if (tab.isPrivate) return
        loginWatcher.captureCurrentForm(tab.id, tab.url)
    }

    private fun routeLabel(route: SiteRoute?, strings: MarbleStrings): String = when (route) {
        SiteRoute.DIRECT -> strings.routeDirect
        SiteRoute.DESKTOP_USER_AGENT -> strings.routeDesktop
        SiteRoute.MOBILE_SUBDOMAIN -> strings.routeMobile
        SiteRoute.CANONICAL_HOST -> strings.routeCanonical
        SiteRoute.PWA_WINDOW -> strings.routePwa
        SiteRoute.VAULT_MANUAL_LOGIN -> strings.routeManual
        SiteRoute.READ_ONLY_SNAPSHOT -> strings.routeReadOnly
        null -> strings.routeDirect
    }

    fun showBanner(message: String) {
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
