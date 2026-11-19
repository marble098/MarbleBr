package com.marble098.marblebr.feature.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.SessionHealthStatus
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

    var showTabsSheet by remember { mutableStateOf(false) }
    var showBookmarksHistorySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val activeTab = uiState.activeTab
    val isBookmarked = activeTab != null && bookmarks.any { it.url == activeTab.url }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Never Sign-out Shield Badge
                    FilledTonalIconButton(
                        onClick = { onOpenVaultDashboard(activeTab?.containerId) },
                        modifier = Modifier.testTag("never_signout_shield_btn"),
                    ) {
                        Icon(
                            imageVector = if (activeTab?.isPrivate == true) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Shield
                            },
                            contentDescription = "وضعیت Never Sign-out",
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

                    // Smart Address Bar
                    OutlinedTextField(
                        value = uiState.addressBarInput,
                        onValueChange = viewModel::onAddressBarChanged,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smart_address_bar"),
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = {
                            Icon(
                                imageVector = if (activeTab?.isSecure == true) Icons.Default.Lock else Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = viewModel::toggleBookmarkCurrentPage) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "بوکمارک",
                                    )
                                }
                                IconButton(onClick = { viewModel.navigateToInput() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "بارگذاری",
                                    )
                                }
                            }
                        },
                        placeholder = { Text("جستجو یا ورود آدرس سایت...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { viewModel.navigateToInput() }),
                    )

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("browser_overflow_menu_btn"),
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "منوی مرورگر")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("ثبت اسنپ‌شات فوری در Vault") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.captureManualSnapshotNow()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("ماشین زمان نشست‌ها (Time Machine)") },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenVaultDashboard(activeTab?.containerId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("جستجو در صفحه (Find in page)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleFindInPage(true)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (activeTab?.isDesktopMode == true) {
                                            "خروج از حالت دسکتاپ"
                                        } else {
                                            "نمایش در حالت دسکتاپ"
                                        },
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleDesktopMode()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("نصب وب‌اپ در صفحه اصلی (PWA)") },
                                leadingIcon = { Icon(Icons.Default.InstallMobile, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.installCurrentSiteAsPwa()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("پاک کردن کش (بدون خروج از سایت‌ها)") },
                                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.clearCacheWithoutTouchingSessions()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("دانلودها (${downloads.size})") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showDownloadsSheet = true
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("تنظیمات و پروکسی / DNS") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenSettings()
                                },
                            )
                        }
                    }
                }

                // Active Container & Never Sign-out Status Chip Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val container = uiState.activeContainer
                    AssistChip(
                        onClick = { onOpenVaultDashboard(container?.id) },
                        label = {
                            Text(
                                text = if (activeTab?.isPrivate == true) {
                                    "تب خصوصی (بدون ذخیره نشست)"
                                } else if (container?.neverSignOutEnabled != false) {
                                    "Never Sign-out فعال • کانتینر: ${container?.contextId ?: "ایزوله"}"
                                } else {
                                    "Never Sign-out غیرفعال برای این سایت"
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        ),
                    )

                    if (blockedAdsCount > 0) {
                        AssistChip(
                            onClick = onOpenSettings,
                            label = { Text("تبلیغ مسدودشده: $blockedAdsCount") },
                        )
                    }
                }

                if (activeTab?.isLoading == true) {
                    LinearProgressIndicator(
                        progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }

                // Find in Page Bar
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

                // Status Banner
                AnimatedVisibility(visible = uiState.statusBannerMessageFa != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.statusBannerMessageFa.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = viewModel::dismissBanner) {
                                Icon(Icons.Default.Close, contentDescription = "بستن پیام")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { showTabsSheet = true },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge { Text("${uiState.tabs.size}") }
                            },
                        ) {
                            Icon(Icons.Default.Tab, contentDescription = "تب‌ها")
                        }
                    },
                    label = { Text("تب‌ها") },
                    modifier = Modifier.testTag("nav_tabs_btn"),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onOpenVaultDashboard(activeTab?.containerId) },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "گاوصندوق نشست") },
                    label = { Text("Never Sign-out") },
                    modifier = Modifier.testTag("nav_vault_btn"),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showBookmarksHistorySheet = true },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "بوکمارک و تاریخچه") },
                    label = { Text("بوکمارک‌ها") },
                    modifier = Modifier.testTag("nav_bookmarks_btn"),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                    label = { Text("تنظیمات") },
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
            if (activeTab != null && renderGeckoWebView && !isInspection) {
                val container = uiState.activeContainer
                if (container != null) {
                    GeckoSessionWebView(
                        tab = activeTab,
                        container = container,
                        viewModel = viewModel,
                    )
                }
            } else {
                // Fallback card for UI tests & preview mode
                ActiveContainerSummaryCard(
                    tab = activeTab,
                    onCaptureSnapshot = viewModel::captureManualSnapshotNow,
                    onOpenVault = { onOpenVaultDashboard(activeTab?.containerId) },
                )
            }
        }
    }

    // Tabs Bottom Sheet
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

    // Bookmarks & History Bottom Sheet
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

    // Downloads Bottom Sheet
    if (showDownloadsSheet) {
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            DownloadsSheet(downloads = downloads)
        }
    }

    // Defense Layer 8: Credential Manager / Passkey Biometric Re-login Dialog
    uiState.fallbackPromptDomain?.let { revokedDomain ->
        AlertDialog(
            onDismissRequest = viewModel::dismissFallbackPrompt,
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text("نشست توسط سرور باطل شده است") },
            text = {
                Text(
                    "سرور سایت $revokedDomain نشست قبلی را به‌طور کامل منقضی کرده است (مثلاً به دلیل تغییر رمز یا انقضای مطلق سرور). " +
                        "آیا می‌خواهید با تأیید اثر انگشت و Passkey / Credential Manager به‌طور بی‌صدا مجدداً وارد شوید؟",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissFallbackPrompt()
                        onTriggerBiometricPasskeyRelogin(revokedDomain)
                    },
                ) {
                    Text("ورود خودکار با بیومتریک")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFallbackPrompt) {
                    Text("بعداً")
                }
            },
        )
    }
}

@Composable
private fun GeckoSessionWebView(
    tab: BrowserTab,
    container: com.marble098.marblebr.core.model.WebAppContainer,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .testTag("find_in_page_input"),
            singleLine = true,
            placeholder = { Text("جستجو در متن صفحه...") },
            trailingIcon = {
                Text(
                    text = "$matchIndex/$totalMatches",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
        )
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "قبلی")
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "بعدی")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "بستن جستجو")
        }
    }
}

@Composable
private fun ActiveContainerSummaryCard(
    tab: BrowserTab?,
    onCaptureSnapshot: () -> Unit,
    onOpenVault: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "موتور Never Sign-out آماده محافظت است",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "آدرس فعلی: ${tab?.url ?: "https://github.com"}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "کانتینر ایزوله GeckoView: ${tab?.contextId ?: "marble_ctx_default"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCaptureSnapshot) {
                        Text("اسنپ‌شات فوری")
                    }
                    FilledTonalButton(onClick = onOpenVault) {
                        Text("ماشین زمان نشست‌ها")
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
                text = "تب‌های باز (${tabs.size})",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onNewPrivateTab) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تب خصوصی")
                }
                Button(onClick = onNewNormalTab) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تب جدید")
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
                            MaterialTheme.colorScheme.surfaceVariant
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
                                text = tab.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${tab.url} • ${tab.contextId}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onCloseTab(tab.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن تب")
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
                FilledTonalButton(onClick = { selectedSection = 0 }) {
                    Text("بوکمارک‌ها (${bookmarks.size})")
                }
                FilledTonalButton(onClick = { selectedSection = 1 }) {
                    Text("تاریخچه (${historyItems.size})")
                }
            }
            if (selectedSection == 1 && historyItems.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("پاک کردن تاریخچه")
                }
            }
        }

        if (selectedSection == 0) {
            if (bookmarks.isEmpty()) {
                Text("هنوز بوکمارکی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bookmarks, key = { it.id }) { bm ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectUrl(bm.url) },
                            tonalElevation = 2.dp,
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(bm.title, style = MaterialTheme.typography.titleMedium)
                                Text(bm.url, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else {
            if (historyItems.isEmpty()) {
                Text("تاریخچه مرور خالی است.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyItems, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onSelectUrl(item.url) },
                            tonalElevation = 2.dp,
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(item.url, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DownloadsSheet(downloads: List<DownloadItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("مدیریت دانلودها", style = MaterialTheme.typography.titleLarge)
        if (downloads.isEmpty()) {
            Text("هنوز فایلی دانلود نشده است.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads, key = { it.id }) { dl ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(dl.fileName, style = MaterialTheme.typography.titleMedium)
                            Text(dl.url, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text("مسیر: ${dl.destinationPath}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
