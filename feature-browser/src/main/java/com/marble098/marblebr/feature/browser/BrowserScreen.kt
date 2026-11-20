package com.marble098.marblebr.feature.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.preferences.AddressSelectionMode
import com.marble098.marblebr.core.model.BackNavigationResolver
import com.marble098.marblebr.core.model.BackTarget
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.DownloadState
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.MarbleLinks
import com.marble098.marblebr.core.preferences.BrowserPreferences
import com.marble098.marblebr.core.model.HomeShortcutPosition
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.WebAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenVaultDashboard: (containerId: String?) -> Unit,
    onOpenSettings: () -> Unit,
    onTriggerBiometricPasskeyRelogin: (domain: String) -> Unit,
    renderGeckoWebView: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val blockedAdsCount by viewModel.adBlocker.blockedCount.collectAsState()
    val strings = LocalMarbleStrings.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val showHome = uiState.showHome
    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var backPromptVisible by remember { mutableStateOf(false) }
    var pendingCloseTabId by remember { mutableStateOf<String?>(null) }

    val activeTab = uiState.activeTab
    val hasHomeShortcut = showHome && prefs.homeShortcutPosition == HomeShortcutPosition.BOTTOM
    val hasPage = !activeTab?.url.isNullOrBlank()
    val focusManager = LocalFocusManager.current
    val clipboardContext = LocalContext.current
    val submit: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboardController?.hide()
            viewModel.navigateToInput(query)
        }
    }
    // دکمهٔ بازگشت: رفتارش را کاربر در تنظیمات انتخاب می‌کند.
    val backTarget = BackNavigationResolver.resolve(
        behavior = prefs.backButtonBehavior,
        isOnHome = showHome,
        canGoBack = activeTab?.canGoBack == true,
        tabCount = uiState.tabs.size,
        canExitApp = showHome,
    )
    val activity = LocalContext.current.findActivity()
    val exitBack: () -> Unit = { if (activity == null || !activity.moveTaskToBack(true)) viewModel.showHome() }
    BackHandler(enabled = true) {
        when (backTarget) {
            BackTarget.PAGE_BACK -> viewModel.goBack()
            BackTarget.HOME -> viewModel.showHome()
            BackTarget.EXIT -> exitBack()
            BackTarget.ASK -> backPromptVisible = true
        }
    }
    val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }

    // در حالت شیشه‌ای شناور، نوارها روی محتوا می‌نشینند؛ پس خودِ محتوا فقط به اندازهٔ
    // ارتفاع نوارها فاصلهٔ داخلی می‌گیرد تا صفحه واقعاً از پشت شیشه دیده شود.
    val floatingChrome = prefs.chromeBarStyle == ChromeBarStyle.FLOATING
    val floatingTopSpacer = if (floatingChrome) 104.dp else 0.dp
    val floatingBottomSpacer = if (floatingChrome) 76.dp else 0.dp

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .liquidGlass(prefs.chromeBarStyle)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(
                        onClick = { onOpenVaultDashboard(activeTab?.containerId) },
                        modifier = Modifier
                            .size(40.dp)
                            .floatingGlassPill(prefs.chromeBarStyle, radius = 20)
                            .testTag("never_signout_shield_btn"),
                    ) {
                        Icon(
                            imageVector = if (activeTab?.isPrivate == true) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Shield
                            },
                            contentDescription = strings.protectionLabel,
                            tint = when (activeTab?.healthStatus) {
                                SessionHealthStatus.PROTECTED_ACTIVE,
                                SessionHealthStatus.HEALTHY,
                                SessionHealthStatus.RECOVERED_FROM_VAULT,
                                -> MaterialTheme.colorScheme.primary
                                SessionHealthStatus.SERVER_REVOKED_FALLBACK -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    key(activeTab?.id) {
                        BrowserAddressField(
                            url = if (showHome) "" else activeTab?.url.orEmpty(),
                            secure = activeTab?.isSecure == true,
                            isBookmarked = isBookmarked,
                            canBookmark = hasPage && activeTab?.isPrivate != true,
                            mode = prefs.addressSelectionMode,
                            onSubmit = submit,
                            onBookmark = viewModel::toggleBookmarkCurrentPage,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("browser_overflow_menu_btn"),
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = strings.browserMenu)
                        }
                        BrowserOverflowMenu(
                            expanded = showOverflowMenu,
                            pageUrl = activeTab?.url?.ifBlank { null },
                            isPrivate = activeTab?.isPrivate == true,
                            canDuplicate = activeTab != null,
                            onDuplicate = viewModel::duplicateTab,
                            onDismiss = { showOverflowMenu = false },
                            isDesktopMode = activeTab?.isDesktopMode == true,
                            downloadsCount = downloads.size,
                            onSaveState = viewModel::captureManualSnapshotNow,
                            onOpenTimeMachine = { onOpenVaultDashboard(activeTab?.containerId) },
                            onFindInPage = { viewModel.toggleFindInPage(true) },
                            onToggleDesktop = viewModel::toggleDesktopMode,
                            onInstallShortcut = viewModel::installCurrentSiteAsPwa,
                            onFreeSpace = viewModel::clearCacheWithoutTouchingSessions,
                            onOpenDownloads = { showDownloadsSheet = true },
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }

                // به‌جای پیامِ «مراقب این سایت هستیم»، دکمه‌های میانبرِ واقعی و کاربردی.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = viewModel::goBack, enabled = activeTab?.canGoBack == true) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.goBack,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = viewModel::goForward, enabled = activeTab?.canGoForward == true) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = strings.goForward,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = if (activeTab?.isLoading == true) viewModel::stopLoading else viewModel::reloadPage,
                        enabled = !showHome && (hasPage || activeTab?.isLoading == true),
                    ) {
                        val loading = activeTab?.isLoading == true
                        Icon(
                            if (loading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (loading) strings.stopLoading else strings.reload,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    QuickShortcutChip(
                        icon = Icons.Default.Bolt,
                        label = strings.quickActionsTitle,
                        style = prefs.chromeBarStyle,
                        highlighted = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_actions_btn"),
                        onClick = { showQuickActions = true },
                    )
                    if (!showHome) {
                        QuickShortcutChip(
                            icon = Icons.Default.Fingerprint,
                            label = strings.quickSaveLogin,
                            style = prefs.chromeBarStyle,
                            modifier = Modifier.weight(1f),
                            enabled = activeTab?.isPrivate != true,
                            onClick = viewModel::captureManualSnapshotNow,
                        )
                    }
                    QuickShortcutChip(
                        icon = Icons.Default.Tab,
                        label = "${strings.quickTabs} ${uiState.tabs.size}",
                        style = prefs.chromeBarStyle,
                        modifier = Modifier.weight(1f),
                        onClick = { showTabsSheet = true },
                    )
                }
                AnimatedVisibility(
                    visible = showHome && blockedAdsCount > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AssistChip(
                        onClick = onOpenSettings,
                        label = {
                            Text(
                                text = strings.adsBlocked(blockedAdsCount.toInt()),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = strings.adBlockChip(blockedAdsCount.toInt()),
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }

                Box(Modifier.fillMaxWidth().height(3.dp)) {
                    if (!showHome && activeTab?.isLoading == true) {
                        val progress = (activeTab.progress / 100f).coerceIn(0f, 1f)
                        if (progress == 0f) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxSize())
                        } else {
                            val visibleProgress by animateFloatAsState(progress, label = "page load")
                            LinearProgressIndicator(progress = { visibleProgress }, modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isFindInPageVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    FindInPageBar(
                        query = uiState.findQuery,
                        matchIndex = activeTab?.findMatchIndex ?: 0,
                        totalMatches = activeTab?.findTotalMatches ?: 0,
                        onQueryChanged = { viewModel.updateFindQuery(it, backwards = false) },
                        onNext = { viewModel.updateFindQuery(uiState.findQuery, backwards = false) },
                        onPrevious = { viewModel.updateFindQuery(uiState.findQuery, backwards = true) },
                        onClose = { viewModel.toggleFindInPage(false) },
                    )
                }

                if (uiState.bannerMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.bannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::dismissBanner) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = strings.dismiss,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                Modifier
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .liquidGlass(prefs.chromeBarStyle),
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    NavigationBarItem(
                        selected = showHome,
                        onClick = viewModel::showHome,
                        icon = { Icon(Icons.Default.Home, contentDescription = strings.homeTitle) },
                        label = { Text(strings.homeTitle) },
                        modifier = Modifier.testTag("nav_home_btn"),
                    )
                    NavigationBarItem(
                        selected = showTabsSheet,
                        onClick = { showTabsSheet = true },
                        icon = {
                            BadgedBox(
                                badge = { Badge { Text("${uiState.tabs.size}") } },
                            ) {
                                Icon(Icons.Default.Tab, contentDescription = strings.tabsLabel)
                            }
                        },
                        label = { Text(strings.tabsLabel) },
                        modifier = Modifier.testTag("nav_tabs_btn"),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onOpenVaultDashboard(activeTab?.containerId) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = strings.protectionLabel) },
                        label = { Text(strings.protectionLabel) },
                        modifier = Modifier.testTag("nav_vault_btn"),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showBookmarksHistorySheet = true },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = strings.bookmarksLabel) },
                        label = { Text(strings.bookmarksLabel) },
                        modifier = Modifier.testTag("nav_bookmarks_btn"),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onOpenSettings,
                        icon = { Icon(Icons.Default.Settings, contentDescription = strings.settingsLabel) },
                        label = { Text(strings.settingsLabel) },
                        modifier = Modifier.testTag("nav_settings_btn"),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (floatingChrome) floatingTopSpacer else innerPadding.calculateTopPadding(),
                    bottom = if (floatingChrome) floatingBottomSpacer else innerPadding.calculateBottomPadding(),
                )
                .padding(bottom = if (hasHomeShortcut) 58.dp else 0.dp),
        ) {
            // با عوض شدن صفحه یا تب، پرسشِ بازگشتِ بی‌ربط بسته می‌شود.
            LaunchedEffect(uiState.activeTabId, showHome) {
                backPromptVisible = false
            }
            val isInspection = LocalInspectionMode.current
            val container = uiState.activeContainer

            if (showHome) {
                HomeContent(
                    strings = strings,
                    bookmarks = bookmarks,
                    historyItems = historyItems,
                    shortcutPosition = prefs.homeShortcutPosition,
                    onSearch = submit,
                    onNewTab = { viewModel.openNewTab() },
                    onNewPrivateTab = { viewModel.openNewTab(isPrivate = true) },
                    onOpenBookmarksHistory = { showBookmarksHistorySheet = true },
                    onOpenSettings = onOpenSettings,
                )
            } else if (activeTab != null && hasPage && renderGeckoWebView && !isInspection && container != null && container.id == activeTab.containerId) {
                key(activeTab.id, activeTab.contextId) {
                    GeckoSessionWebView(tab = activeTab, container = container, viewModel = viewModel)
                }
            } else if (!renderGeckoWebView || isInspection) {
                ReadyCard(
                    tab = activeTab,
                    onCaptureSnapshot = viewModel::captureManualSnapshotNow,
                    onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
                )
            }

            if (hasHomeShortcut) {
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    HomeShortcutButton(
                        onClick = { viewModel.openNewTab() },
                    )
                }
            }
        }
    }

    if (showQuickActions) {
        ModalBottomSheet(onDismissRequest = { showQuickActions = false }) {
            QuickActionsSheet(
                strings = strings,
                hasPage = hasPage,
                activeTab = activeTab,
                prefs = prefs,
                blockedAdsCount = blockedAdsCount.toInt(),
                downloadsCount = downloads.size,
                onDismiss = { showQuickActions = false },
                viewModel = viewModel,
                onSubmit = submit,
                onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
                onOpenBookmarks = { showBookmarksHistorySheet = true },
                onOpenDownloads = { showDownloadsSheet = true },
                onOpenSettings = onOpenSettings,
                onCopyLink = { url -> copyTextToClipboard(context = clipboardContext, label = strings.quickCopyLink, text = url) },
                onShareLink = { url ->
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                    clipboardContext.startActivity(Intent.createChooser(share, strings.quickShare))
                },
            )
        }
    }

    if (showBookmarksHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksHistorySheet = false }) {
            BookmarksAndHistorySheet(
                bookmarks = bookmarks,
                historyItems = historyItems,
                onSelectUrl = { url ->
                    showBookmarksHistorySheet = false
                    submit(url)
                },
                onClearHistory = viewModel::clearBrowsingHistory,
            )
        }
    }

    if (showDownloadsSheet) {
        LaunchedEffect(Unit) {
            while (isActive) {
                viewModel.refreshDownloads()
                delay(1_500)
            }
        }
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            DownloadsSheet(downloads = downloads)
        }
    }

    if (showTabsSheet) {
        ModalBottomSheet(onDismissRequest = { showTabsSheet = false }) {
            TabsSwitcherSheet(
                tabs = uiState.tabs,
                activeTabId = uiState.activeTabId,
                onSelectTab = {
                    viewModel.selectTab(it)
                    showTabsSheet = false
                },
                onCloseTab = { tabId ->
                    if (prefs.askBeforeClosingTabs) pendingCloseTabId = tabId else viewModel.closeTab(tabId)
                },
                canReopenTab = uiState.canReopenTab,
                onReopenTab = {
                    viewModel.reopenClosedTab()
                    showTabsSheet = false
                },
                onCloseAll = viewModel::closeAllTabs,
                onNewNormalTab = {
                    viewModel.openNewTab(isPrivate = false)
                    showTabsSheet = false
                },
                onNewPrivateTab = {
                    viewModel.openNewTab(isPrivate = true)
                    showTabsSheet = false
                },
            )
        }
    }

    // پرسشِ «برگرد به کجا؟» وقتی کاربر حالت «هر بار از من بپرس» را انتخاب کرده است.
    if (backPromptVisible) {
        AlertDialog(
            onDismissRequest = { backPromptVisible = false },
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
            title = { Text(strings.backAskTitle) },
            text = { Text(strings.backAskBody) },
            confirmButton = {
                Button(
                    onClick = {
                        backPromptVisible = false
                        if (activeTab?.canGoBack == true) viewModel.goBack() else viewModel.showHome()
                    },
                    enabled = !showHome && activeTab?.canGoBack == true,
                ) {
                    Text(strings.backAskToPage)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            backPromptVisible = false
                            viewModel.showHome()
                        },
                        enabled = !showHome,
                    ) {
                        Text(strings.backAskToHome)
                    }
                    TextButton(
                        onClick = {
                            backPromptVisible = false
                            if (activity == null || !activity.moveTaskToBack(true)) viewModel.showHome()
                        },
                    ) {
                        Text(strings.backAskExit)
                    }
                }
            },
        )
    }

    pendingCloseTabId?.let { tabId ->
        val title = uiState.tabs.firstOrNull { it.id == tabId }?.title.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingCloseTabId = null },
            title = { Text(strings.closeTabConfirmTitle) },
            text = { Text(strings.closeTabConfirmBody(title.ifBlank { strings.tabsLabel })) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingCloseTabId = null
                        viewModel.closeTab(tabId)
                    },
                ) {
                    Text(strings.closeTab)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloseTabId = null }) { Text(strings.cancel) }
            },
        )
    }

    uiState.fallbackPromptDomain?.let { revokedDomain ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFallbackPrompt,
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text(strings.reloginTitle) },
            text = { Text(strings.reloginBody(revokedDomain)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissFallbackPrompt()
                        onTriggerBiometricPasskeyRelogin(revokedDomain)
                    },
                ) {
                    Text(strings.reloginConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFallbackPrompt) {
                    Text(strings.reloginDismiss)
                }
            },
        )
    }
}

@Composable
private fun BrowserAddressField(
    url: String,
    secure: Boolean,
    isBookmarked: Boolean,
    canBookmark: Boolean,
    mode: AddressSelectionMode,
    onSubmit: (String) -> Unit,
    onBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalMarbleStrings.current
    var focused by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(TextFieldValue(url)) }
    LaunchedEffect(url, focused) {
        if (!focused) input = TextFieldValue(url)
    }
    LaunchedEffect(focused) {
        if (focused && mode != AddressSelectionMode.CURSOR) {
            input = input.copy(selection = addressSelection(input.text, input.selection.end, mode))
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = modifier.testTag("smart_address_bar")
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key != Key.Enter && event.key != Key.NumPadEnter) {
                    false
                } else {
                    if (event.type == KeyEventType.KeyUp) onSubmit(input.text)
                    true
                }
            },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyMedium,
        leadingIcon = {
            Icon(if (secure) Icons.Default.Lock else Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (focused) {
                IconButton(onClick = { onSubmit(input.text) }, enabled = input.text.isNotBlank()) {
                    Icon(Icons.Default.ArrowRightAlt, contentDescription = strings.goToAddress)
                }
            } else if (canBookmark) {
                IconButton(onClick = onBookmark) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = strings.bookmarkToggle,
                    )
                }
            }
        },
        placeholder = { Text(strings.addressPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onSubmit(input.text) }),
    )
}

@Composable
private fun HomeShortcutButton(onClick: () -> Unit) {
    val strings = LocalMarbleStrings.current
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(52.dp).testTag("home_shortcut_btn"),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = strings.newTab,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun HomeContent(
    strings: com.marble098.marblebr.core.i18n.MarbleStrings,
    bookmarks: List<BookmarkItem>,
    historyItems: List<HistoryItem>,
    shortcutPosition: HomeShortcutPosition,
    onSearch: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onOpenBookmarksHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val showTopShortcut = shortcutPosition == HomeShortcutPosition.TOP
    var homeSearch by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = strings.homeTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = strings.homeIntro,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = homeSearch,
            onValueChange = { homeSearch = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (homeSearch.isNotBlank()) {
                    IconButton(onClick = { homeSearch = "" }) {
                        Icon(Icons.Default.Close, contentDescription = strings.dismiss, modifier = Modifier.size(18.dp))
                    }
                }
            },
            placeholder = { Text(text = strings.homeSearchHint) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                if (homeSearch.isNotBlank()) {
                    onSearch(homeSearch)
                    homeSearch = ""
                }
            }),
        )

        if (showTopShortcut) {
            HomeShortcutActionRow(onNewTab = onNewTab)
        }

        SectionHeader(strings = strings, title = strings.homeShortcutsTitle, action = onOpenBookmarksHistory)
        if (bookmarks.isEmpty()) {
            EmptyState(icon = Icons.Default.BookmarkBorder, text = strings.homeEmptyShortcuts)
        } else {
            bookmarks.take(6).forEach { bookmark ->
                LinkCard(bookmark.title.ifBlank { bookmark.url }, bookmark.url, Icons.Default.Bookmark) { onSearch(bookmark.url) }
            }
        }

        SectionHeader(strings = strings, title = strings.homeRecentTitle, action = onOpenBookmarksHistory)
        if (historyItems.isEmpty()) {
            EmptyState(icon = Icons.Default.History, text = strings.homeEmptyRecent)
        } else {
            historyItems.take(8).forEach { item ->
                LinkCard(item.title.ifBlank { item.url }, item.url, Icons.Default.History) { onSearch(item.url) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickActionButton(icon = Icons.Default.Add, label = strings.newTab, onClick = onNewTab)
            QuickActionButton(icon = Icons.Default.VisibilityOff, label = strings.newPrivateTab, onClick = onNewPrivateTab)
            QuickActionButton(icon = Icons.Default.Bookmark, label = strings.bookmarksLabel, onClick = onOpenBookmarksHistory)
            QuickActionButton(icon = Icons.Default.Settings, label = strings.settingsLabel, onClick = onOpenSettings)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(
    strings: com.marble098.marblebr.core.i18n.MarbleStrings,
    title: String,
    action: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = action) { Text(text = strings.homeSeeAll) }
    }
}

@Composable
private fun HomeShortcutActionRow(onNewTab: () -> Unit) {
    val strings = LocalMarbleStrings.current
    FilledTonalButton(
        onClick = onNewTab,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = strings.newTab, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(40.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

/* ---------------- existing browser sub-components (kept) ---------------- */

@Composable
private fun BrowserOverflowMenu(
    pageUrl: String?,
    isPrivate: Boolean,
    canDuplicate: Boolean,
    onDuplicate: () -> Unit,
    expanded: Boolean,
    onDismiss: () -> Unit,
    isDesktopMode: Boolean,
    downloadsCount: Int,
    onSaveState: () -> Unit,
    onOpenTimeMachine: () -> Unit,
    onFindInPage: () -> Unit,
    onToggleDesktop: () -> Unit,
    onInstallShortcut: () -> Unit,
    onFreeSpace: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (!pageUrl.isNullOrBlank()) {
            DropdownMenuItem(
                text = { Text(strings.copyLink) },
                onClick = {
                    onDismiss()
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(strings.copyLink, pageUrl))
                },
            )
            DropdownMenuItem(
                text = { Text(strings.shareLink) },
                onClick = {
                    onDismiss()
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, pageUrl)
                    context.startActivity(Intent.createChooser(share, strings.shareLink))
                },
            )
        }
        if (canDuplicate) {
            DropdownMenuItem(
                text = { Text(strings.duplicateTab) },
                onClick = {
                    onDismiss()
                    onDuplicate()
                },
            )
        }
        if (pageUrl != null && !isPrivate) {
            MenuEntry(
                title = strings.menuSaveState,
                subtitle = strings.menuSaveStateHint,
                icon = Icons.Default.Security,
                onClick = {
                    onDismiss()
                    onSaveState()
                },
            )
            MenuEntry(
                title = strings.menuTimeMachine,
                subtitle = strings.menuTimeMachineHint,
                icon = Icons.Default.History,
                onClick = {
                    onDismiss()
                    onOpenTimeMachine()
                },
            )
            DropdownMenuItem(
                text = { Text(strings.menuSaveShortcut) },
                leadingIcon = { Icon(Icons.Default.InstallMobile, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onInstallShortcut()
                },
            )
        }
        if (pageUrl != null) {
            DropdownMenuItem(
                text = { Text(strings.menuFindInPage) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onFindInPage()
                },
            )
            DropdownMenuItem(
                text = { Text(if (isDesktopMode) strings.menuDesktopModeOff else strings.menuDesktopModeOn) },
                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onToggleDesktop()
                },
            )
        }
        MenuEntry(
            title = strings.menuFreeSpace,
            subtitle = strings.menuFreeSpaceHint,
            icon = Icons.Default.CleaningServices,
            onClick = {
                onDismiss()
                onFreeSpace()
            },
        )
        DropdownMenuItem(
            text = { Text(strings.downloadsMenu(downloadsCount)) },
            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenDownloads()
            },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(strings.settingsTitle) },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                onDismiss()
                onOpenSettings()
            },
        )
    }
}

@Composable
private fun MenuEntry(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(title)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun GeckoSessionWebView(
    tab: BrowserTab,
    container: WebAppContainer,
    viewModel: BrowserViewModel,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            GeckoView(ctx).apply {
                BrowserViewRegistry.register(this)
                val geckoSession = viewModel.sessionManager.getOrCreateGeckoSession(
                    tabId = tab.id,
                    container = container,
                    initialUrl = tab.url,
                    isPrivate = tab.isPrivate,
                    isDesktopMode = tab.isDesktopMode,
                    adBlockEnabled = viewModel.preferences.value.adBlockEnabled,
                    onLocationChanged = { url, secure -> viewModel.onPageLocationChanged(tab.id, url, secure) },
                    onTitleChanged = { title -> viewModel.onPageTitleChanged(tab.id, title) },
                    onProgressChanged = { progress, loading -> viewModel.onPageProgressChanged(tab.id, progress, loading) },
                    onNavStateChanged = { back, fwd -> viewModel.onNavStateChanged(tab.id, back, fwd) },
                    onHealthRestored = viewModel::onHealthRestored,
                    onServerRevokedFallbackRequired = viewModel::onServerRevokedFallbackRequired,
                    onDownloadRequested = { url, _ -> if (!tab.isPrivate) viewModel.enqueueDownload(url) },
                )
                setSession(geckoSession)
            }
        },
        onRelease = {
            BrowserViewRegistry.register(null)
            it.releaseSession()
        },
    )
}

@Composable
private fun FindInPageBar(
    query: String,
    matchIndex: Int,
    totalMatches: Int,
    onQueryChanged: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f).testTag("find_in_page_input"),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            placeholder = { Text(strings.findInPagePlaceholder) },
            trailingIcon = {
                Text(
                    text = "$matchIndex/$totalMatches",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = strings.findPrevious)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = strings.findNext)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = strings.findClose)
        }
    }
}

@Composable
private fun ReadyCard(
    tab: BrowserTab?,
    onCaptureSnapshot: () -> Unit,
    onOpenVault: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = strings.readyTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = strings.readyBody,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = tab?.url ?: "https://github.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCaptureSnapshot) {
                        Text(strings.quickSaveState)
                    }
                    FilledTonalButton(onClick = onOpenVault) {
                        Text(strings.quickEarlierCopies)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabsSwitcherSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    canReopenTab: Boolean,
    onReopenTab: () -> Unit,
    onCloseAll: () -> Unit,
    onNewNormalTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var filter by remember { mutableStateOf("") }
    var confirmCloseAll by remember { mutableStateOf(false) }
    val visibleTabs = tabs.filter {
        it.url.contains(filter, ignoreCase = true) || it.title.contains(filter, ignoreCase = true)
    }
    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text(strings.closeAllTabs) },
            text = { Text(strings.confirmCloseAllTabs) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloseAll = false
                    onCloseAll()
                }) { Text(strings.closeAllTabs) }
            },
            dismissButton = { TextButton(onClick = { confirmCloseAll = false }) { Text(strings.dismiss) } },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(strings.openTabsTitle(tabs.size), style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onNewPrivateTab) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.newPrivateTab)
            }
            Button(onClick = onNewNormalTab) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.newTab)
            }
            TextButton(onClick = onReopenTab, enabled = canReopenTab) { Text(strings.reopenTab) }
            if (tabs.isNotEmpty()) {
                TextButton(onClick = { confirmCloseAll = true }) { Text(strings.closeAllTabs) }
            }
        }
        if (tabs.isNotEmpty()) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(strings.searchTabs) },
            )
        }
        if (visibleTabs.isEmpty()) Text(if (tabs.isEmpty()) strings.noTabs else strings.noMatchingTabs)
        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visibleTabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId
                Card(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clickable { onSelectTab(tab.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title.ifBlank { tab.url.ifBlank { if (tab.isPrivate) strings.privateTabTitle else strings.newTab } },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (tab.isPrivate) strings.privateTabTitle else tab.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onCloseTab(tab.id) }) {
                            Icon(Icons.Default.Close, contentDescription = strings.closeTab, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BookmarksAndHistorySheet(
    bookmarks: List<BookmarkItem>,
    historyItems: List<HistoryItem>,
    onSelectUrl: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var selectedSection by remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text(strings.bookmarksCount(bookmarks.size)) },
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text(strings.historyCount(historyItems.size)) },
                )
            }
            if (selectedSection == 1 && historyItems.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text(strings.clearHistory)
                }
            }
        }

        val isBookmarks = selectedSection == 0
        val isEmpty = if (isBookmarks) bookmarks.isEmpty() else historyItems.isEmpty()
        if (isEmpty) {
            Text(
                text = if (isBookmarks) strings.emptyBookmarks else strings.emptyHistory,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                if (isBookmarks) {
                    items(bookmarks, key = { it.id }) { bm ->
                        LinkRow(title = bm.title, url = bm.url, onClick = { onSelectUrl(bm.url) })
                    }
                } else {
                    items(historyItems, key = { it.id }) { item ->
                        LinkRow(title = item.title, url = item.url, onClick = { onSelectUrl(item.url) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(title: String, url: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title.ifBlank { url }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DownloadsSheet(downloads: List<DownloadItem>) {
    val strings = LocalMarbleStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(strings.downloadsMenu(downloads.size), style = MaterialTheme.typography.titleLarge)
        if (downloads.isEmpty()) {
            Text(
                text = strings.emptyDownloads,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(downloads, key = { it.id }) { dl ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(dl.fileName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = dl.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val total = dl.totalBytes
                            val status = when (dl.state) {
                                DownloadState.COMPLETED -> strings.savedPath(dl.destinationPath)
                                DownloadState.FAILED -> strings.downloadFailed
                                DownloadState.IN_PROGRESS -> strings.downloadInProgress
                            }
                            if (dl.state == DownloadState.IN_PROGRESS && total > 0) {
                                val fraction = (dl.downloadedBytes.toFloat() / total).coerceIn(0f, 1f)
                                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            }
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------------------------- میانبرهای کاربردیِ نوار بالا ---------------------------- */

/** دکمهٔ کپسولیِ کوچک برای میانبرهای نوار بالا (با همان شیشهٔ شناور). */
@Composable
private fun QuickShortcutChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    style: ChromeBarStyle,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val container = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (style == ChromeBarStyle.FLOATING) 0.55f else 0.95f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.floatingGlassPill(style, radius = 18),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = container,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * شیتِ «میانبرهای سریع»: به‌جای پیامِ بی‌فایدهٔ «مراقب این سایت هستیم»، همهٔ کارهای
 * پرکاربردِ مرورگر در یک صفحه و با آیکن و رنگ.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsSheet(
    strings: com.marble098.marblebr.core.i18n.MarbleStrings,
    hasPage: Boolean,
    activeTab: BrowserTab?,
    prefs: BrowserPreferences,
    blockedAdsCount: Int,
    downloadsCount: Int,
    viewModel: BrowserViewModel,
    onSubmit: (String) -> Unit,
    onOpenVault: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val pageUrl = activeTab?.url?.ifBlank { null }
    val shortcutContext = LocalContext.current
    var quickInput by remember { mutableStateOf("") }
    val perform: (() -> Unit) -> Unit = { action ->
        action()
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // جستجو/آدرس فوری
        OutlinedTextField(
            value = quickInput,
            onValueChange = { quickInput = it },
            modifier = Modifier.fillMaxWidth().testTag("quick_actions_search"),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                FilledTonalIconButton(
                    onClick = {
                        if (quickInput.isNotBlank()) {
                            onSubmit(quickInput)
                            onDismiss()
                        }
                    },
                    enabled = quickInput.isNotBlank(),
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Default.ArrowRightAlt, contentDescription = strings.goToAddress, modifier = Modifier.size(18.dp))
                }
            },
            placeholder = { Text(strings.addressPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (quickInput.isNotBlank()) {
                        onSubmit(quickInput)
                        onDismiss()
                    }
                },
            ),
        )

        if (pageUrl != null && activeTab?.isPrivate != true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    text = strings.quickSiteSignedInFor(
                        days = prefs.defaultCookieLifetimeDays,
                        minutes = 30,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else if (activeTab?.isPrivate == true) {
            Text(
                text = strings.privateTabChip,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(strings.quickActionsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (pageUrl != null) {
                QuickActionTile(strings.quickCopyLink, Icons.Default.ContentCopy, MaterialTheme.colorScheme.primary) {
                    perform { onCopyLink(pageUrl) }
                }
                QuickActionTile(strings.quickShare, Icons.Default.Share, MaterialTheme.colorScheme.tertiary) {
                    perform { onShareLink(pageUrl) }
                }
                QuickActionTile(strings.quickFindInPage, Icons.Default.Search, MaterialTheme.colorScheme.secondary) {
                    perform { viewModel.toggleFindInPage(true) }
                }
                QuickActionTile(
                    label = strings.quickScreenshot,
                    icon = Icons.Default.PhotoCamera,
                    accent = MaterialTheme.colorScheme.primary,
                ) {
                    perform {
                        captureAndSaveScreenshot(shortcutContext) { path ->
                            viewModel.reportScreenshot(path)
                        }
                    }
                }
                QuickActionTile(
                    label = strings.quickDesktop,
                    icon = Icons.Default.DesktopWindows,
                    accent = MaterialTheme.colorScheme.tertiary,
                    active = activeTab?.isDesktopMode == true,
                ) {
                    perform { viewModel.toggleDesktopMode() }
                }
            }
            QuickActionTile(strings.quickNewTab, Icons.Default.Add, MaterialTheme.colorScheme.primary) {
                perform { viewModel.openNewTab() }
            }
            QuickActionTile(strings.quickPrivate, Icons.Default.VisibilityOff, MaterialTheme.colorScheme.tertiary) {
                perform { viewModel.openNewTab(isPrivate = true) }
            }
            QuickActionTile(strings.quickHome, Icons.Default.Home, MaterialTheme.colorScheme.primary) {
                perform { viewModel.showHome() }
            }
            QuickActionTile(strings.quickBookmarks, Icons.Default.Bookmark, MaterialTheme.colorScheme.tertiary) {
                perform(onOpenBookmarks)
            }
            QuickActionTile(strings.quickDownloads, Icons.Default.Download, MaterialTheme.colorScheme.primary) {
                perform(onOpenDownloads)
            }
            QuickActionTile(strings.quickSettings, Icons.Default.Settings, MaterialTheme.colorScheme.secondary) {
                perform(onOpenSettings)
            }
            if (pageUrl != null && activeTab?.isPrivate != true) {
                QuickActionTile(strings.quickSaveLogin, Icons.Default.Fingerprint, MaterialTheme.colorScheme.primary) {
                    perform { viewModel.captureManualSnapshotNow() }
                }
                QuickActionTile(strings.quickTimeMachine, Icons.Default.History, MaterialTheme.colorScheme.tertiary) {
                    perform(onOpenVault)
                }
                QuickActionTile(strings.quickAddShortcut, Icons.Default.InstallMobile, MaterialTheme.colorScheme.secondary) {
                    perform { viewModel.installCurrentSiteAsPwa() }
                }
            }
            QuickActionTile(strings.quickClearCache, Icons.Default.CleaningServices, MaterialTheme.colorScheme.secondary) {
                perform { viewModel.clearCacheWithoutTouchingSessions() }
            }
            if (blockedAdsCount > 0) {
                QuickActionTile(
                    label = strings.adBlockChip(blockedAdsCount),
                    icon = Icons.Default.Security,
                    accent = MaterialTheme.colorScheme.error,
                ) {
                    perform(onOpenSettings)
                }
            }
            if (downloadsCount > 0) {
                QuickActionTile(
                    label = strings.downloadsMenu(downloadsCount),
                    icon = Icons.Default.Download,
                    accent = MaterialTheme.colorScheme.primary,
                ) {
                    perform(onOpenDownloads)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { perform { onCopyLink(MarbleLinks.GITHUB_URL) } }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(
                text = "${strings.appName} · ${MarbleLinks.GITHUB_DISPLAY}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

/** یک کاشیِ گرد و رنگی برای مینحبرهای سریع. */
@Composable
private fun QuickActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .size(width = 84.dp, height = 86.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (active) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------ کمکی‌ها ------------------------------------ */

/** کپی متن در کلیپ‌بورد سیستم. */
private fun copyTextToClipboard(context: Context, label: String, text: String) {
    runCatching {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

/** پیدا کردن Activity از Context برای بستن برنامه. */
internal fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
