package com.marble098.marblebr.feature.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
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
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.model.WebAppContainer
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
    val bookmarks by viewModel.bookmarks.collectAsState()
    val historyItems by viewModel.historyItems.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val blockedAdsCount by viewModel.adBlocker.blockedCount.collectAsState()
    val strings = LocalMarbleStrings.current

    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val activeTab = uiState.activeTab
    val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(
                        onClick = { onOpenVaultDashboard(activeTab?.containerId) },
                        modifier = Modifier
                            .size(44.dp)
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
                        )
                    }

                    OutlinedTextField(
                        value = uiState.addressBarInput,
                        onValueChange = viewModel::onAddressBarChanged,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smart_address_bar"),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        leadingIcon = {
                            Icon(
                                imageVector = if (activeTab?.isSecure == true) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = viewModel::toggleBookmarkCurrentPage) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = strings.bookmarkToggle,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        placeholder = {
                            Text(
                                text = strings.addressPlaceholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { viewModel.navigateToInput() }),
                    )

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("browser_overflow_menu_btn"),
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = strings.browserMenu)
                        }
                        BrowserOverflowMenu(
                            expanded = showOverflowMenu,
                            pageUrl = activeTab?.url,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = viewModel::goBack, enabled = activeTab?.canGoBack == true) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.goBack,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = viewModel::goForward, enabled = activeTab?.canGoForward == true) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = strings.goForward,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = viewModel::reloadPage) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.reload, modifier = Modifier.size(20.dp))
                    }

                    AssistChip(
                        onClick = { onOpenVaultDashboard(uiState.activeContainer?.id) },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = when {
                                    activeTab?.isPrivate == true -> strings.privateTabChip
                                    uiState.activeContainer?.neverSignOutEnabled == false -> strings.protectionOffChip
                                    else -> strings.protectionOnChip
                                },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ),
                    )

                    if (blockedAdsCount > 0) {
                        AssistChip(
                            onClick = onOpenSettings,
                            label = {
                                Text(
                                    text = blockedAdsCount.toString(),
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
                }

                if (activeTab?.isLoading == true) {
                    LinearProgressIndicator(
                        progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .height(4.dp),
                    )
                }

                AnimatedVisibility(visible = uiState.isFindInPageVisible) {
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

                AnimatedVisibility(visible = uiState.bannerMessage != null) {
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
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(
                    selected = false,
                    onClick = { showTabsSheet = true },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge { Text("${uiState.tabs.size}") }
                            },
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
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isInspection = LocalInspectionMode.current
            val container = uiState.activeContainer
            if (activeTab != null && renderGeckoWebView && !isInspection && container != null) {
                GeckoSessionWebView(
                    tab = activeTab,
                    container = container,
                    viewModel = viewModel,
                )
            } else {
                ReadyCard(
                    tab = activeTab,
                    onCaptureSnapshot = viewModel::captureManualSnapshotNow,
                    onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
                )
            }
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
                onCloseTab = viewModel::closeTab,
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

    if (showBookmarksHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksHistorySheet = false }) {
            BookmarksAndHistorySheet(
                bookmarks = bookmarks,
                historyItems = historyItems,
                onSelectUrl = { url ->
                    showBookmarksHistorySheet = false
                    viewModel.navigateToInput(url)
                },
                onClearHistory = viewModel::clearBrowsingHistory,
            )
        }
    }

    if (showDownloadsSheet) {
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            DownloadsSheet(downloads = downloads)
        }
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
private fun BrowserOverflowMenu(
    pageUrl: String?,
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
        DropdownMenuItem(
            text = { Text(strings.menuSaveShortcut) },
            leadingIcon = { Icon(Icons.Default.InstallMobile, contentDescription = null) },
            onClick = {
                onDismiss()
                onInstallShortcut()
            },
        )
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
                val geckoSession = viewModel.sessionManager.getOrCreateGeckoSession(
                    tabId = tab.id,
                    container = container,
                    isPrivate = tab.isPrivate,
                    isDesktopMode = tab.isDesktopMode,
                    onLocationChanged = { url, secure ->
                        viewModel.onPageLocationChanged(tab.id, url, secure)
                    },
                    onTitleChanged = { title ->
                        viewModel.onPageTitleChanged(tab.id, title)
                    },
                    onProgressChanged = { progress, loading ->
                        viewModel.onPageProgressChanged(tab.id, progress, loading)
                    },
                    onNavStateChanged = { back, fwd ->
                        viewModel.onNavStateChanged(tab.id, back, fwd)
                    },
                    onHealthRestored = viewModel::onHealthRestored,
                    onServerRevokedFallbackRequired = viewModel::onServerRevokedFallbackRequired,
                    onDownloadRequested = { url, _ ->
                        viewModel.enqueueDownload(url)
                    },
                )
                setSession(geckoSession)
                geckoSession.loadUri(tab.url)
            }
        },
        update = { geckoView ->
            val targetSession = viewModel.sessionManager.getOrCreateGeckoSession(
                tabId = tab.id,
                container = container,
                isPrivate = tab.isPrivate,
                isDesktopMode = tab.isDesktopMode,
            )
            if (geckoView.session !== targetSession) {
                geckoView.releaseSession()
                geckoView.setSession(targetSession)
                targetSession.loadUri(tab.url)
            }
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
            modifier = Modifier
                .weight(1f)
                .testTag("find_in_page_input"),
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
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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
    onNewNormalTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
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
            Text(
                text = strings.openTabsTitle(tabs.size),
                style = MaterialTheme.typography.titleLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId
                Card(
                    modifier = Modifier
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
                                text = tab.title.ifBlank { tab.url },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = tab.url,
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
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
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
                            Text(
                                text = strings.savedPath(dl.destinationPath),
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
