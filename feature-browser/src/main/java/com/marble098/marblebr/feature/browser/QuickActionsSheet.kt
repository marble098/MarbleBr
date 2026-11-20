package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BrowserTab
import com.marble098.marblebr.core.model.MarbleLinks
import com.marble098.marblebr.core.model.SessionHealthStatus
import com.marble098.marblebr.core.preferences.BrowserPreferences

/**
 * شیتِ «میانبرهای سریع» — بازطراحی کامل.
 *
 * نسخهٔ قبلی یک FlowRow بی‌ترتیب از ۱۵+ کاشی با عرض ثابت ۸۴dp بود؛ متن‌ها در زبان فارسی
 * سرریز می‌شدند، ترتیب منطقی نداشت و ردیف‌ها به‌هم می‌ریختند.
 *
 * ساختار تازه:
 * ۱. سربرگ با عنوان و کارتِ وضعیتِ همین صفحه (به‌جای پیام عمومیِ مبهم).
 * ۲. جستجو/آدرس فوری با دکمهٔ همیشه‌پیدا.
 * ۳. دسته‌بندیِ روشن: «این صفحه»، «تب و پیمایش»، «کتابخانه»، «محافظت و ابزار».
 * ۴. هر کاشی دقیقاً هم‌عرض (شبکهٔ ۴ ستونی) با متن دو خطی و آیکن رنگی.
 */
@Composable
internal fun QuickActionsSheet(
    hasPage: Boolean,
    activeTab: BrowserTab?,
    prefs: BrowserPreferences,
    blockedAdsCount: Int,
    downloadsCount: Int,
    tabsCount: Int,
    viewModel: BrowserViewModel,
    onSubmit: (String) -> Unit,
    onOpenVault: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopyLink: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val pageUrl = activeTab?.url?.takeIf { it.isNotBlank() && hasPage }
    val isPrivate = activeTab?.isPrivate == true
    var quickInput by remember { mutableStateOf("") }

    val perform: (() -> Unit) -> Unit = { action ->
        action()
        onDismiss()
    }
    val submitQuick: () -> Unit = {
        if (quickInput.isNotBlank()) {
            onSubmit(quickInput)
            onDismiss()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        // فاصله‌های شیت هم از ریتمِ مشترک می‌آید تا با صفحهٔ خانه و داشبورد یکی باشد.
        contentPadding = PaddingValues(
            start = MarbleLayout.Screen,
            end = MarbleLayout.Screen,
            top = 4.dp,
            bottom = MarbleLayout.Screen + MarbleLayout.SectionGap,
        ),
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
        verticalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.quickActionsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = if (isPrivate) strings.privateTabChip else strings.quickActionsTitle,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (pageUrl != null) {
                    SessionStatusCard(
                        activeTab = activeTab,
                        pageUrl = pageUrl,
                        prefs = prefs,
                        strings = strings,
                        onOpenVault = { perform(onOpenVault) },
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "search") {
            QuickSearchField(
                value = quickInput,
                onValueChange = { quickInput = it },
                onSubmit = submitQuick,
                placeholder = strings.addressPlaceholder,
            )
        }

        if (pageUrl != null) {
            sectionHeader(key = "section_page", title = strings.quickPageSection)
            item(key = "copy") {
                QuickTile(strings.quickCopyLink, Icons.Default.ContentCopy, colors.primary) {
                    perform { onCopyLink(pageUrl) }
                }
            }
            item(key = "share") {
                QuickTile(strings.quickShare, Icons.Default.Share, colors.tertiary) {
                    perform { onShareLink(pageUrl) }
                }
            }
            item(key = "find") {
                QuickTile(strings.quickFindInPage, Icons.Default.Search, colors.secondary) {
                    perform { viewModel.toggleFindInPage(true) }
                }
            }
            item(key = "shot") {
                QuickTile(strings.quickScreenshot, Icons.Default.PhotoCamera, colors.primary) {
                    perform {
                        captureAndSaveScreenshot(context) { path -> viewModel.reportScreenshot(path) }
                    }
                }
            }
            item(key = "desktop") {
                QuickTile(
                    label = strings.quickDesktop,
                    icon = Icons.Default.DesktopWindows,
                    accent = colors.tertiary,
                    active = activeTab?.isDesktopMode == true,
                ) {
                    perform { viewModel.toggleDesktopMode() }
                }
            }
            item(key = "add_shortcut") {
                QuickTile(strings.quickAddShortcut, Icons.Default.InstallMobile, colors.secondary) {
                    perform { viewModel.installCurrentSiteAsPwa() }
                }
            }
            if (!isPrivate) {
                item(key = "save_login") {
                    QuickTile(strings.quickSaveLogin, Icons.Default.Fingerprint, colors.primary) {
                        perform { viewModel.captureManualSnapshotNow() }
                    }
                }
                item(key = "time_machine") {
                    QuickTile(strings.quickTimeMachine, Icons.Default.History, colors.tertiary) {
                        perform(onOpenVault)
                    }
                }
                item(key = "signout") {
                    QuickTile(strings.quickSignOut, Icons.AutoMirrored.Filled.ExitToApp, colors.error) {
                        perform { viewModel.signOutCurrentSite() }
                    }
                }
            }
        }

        sectionHeader(key = "section_tabs", title = strings.tabsLabel)
        item(key = "new_tab") {
            QuickTile(strings.quickNewTab, Icons.Default.Add, colors.primary) {
                perform { viewModel.openNewTab() }
            }
        }
        item(key = "private") {
            QuickTile(strings.quickPrivate, Icons.Default.VisibilityOff, colors.tertiary) {
                perform { viewModel.openNewTab(isPrivate = true) }
            }
        }
        item(key = "home") {
            QuickTile(strings.quickHome, Icons.Default.Home, colors.primary) {
                perform { viewModel.showHome() }
            }
        }
        item(key = "tabs") {
            // باگ قبلی: این کاشی شیت را می‌بست و هیچ کاری نمی‌کرد (برچسبش «تب‌ها» بود).
            // حالا واقعاً شیتِ تب‌ها را باز می‌کند.
            QuickTile(
                label = strings.tabsLabel,
                icon = Icons.Default.Tab,
                accent = colors.secondary,
                badge = tabsCount,
            ) {
                onOpenTabs()
                onDismiss()
            }
        }

        sectionHeader(key = "section_library", title = strings.quickLibrarySection)
        item(key = "bookmarks") {
            QuickTile(strings.quickBookmarks, Icons.Default.Bookmark, colors.tertiary) {
                perform(onOpenBookmarks)
            }
        }
        item(key = "downloads") {
            QuickTile(
                label = strings.quickDownloads,
                icon = Icons.Default.Download,
                accent = colors.primary,
                badge = downloadsCount,
            ) {
                perform(onOpenDownloads)
            }
        }
        item(key = "settings") {
            QuickTile(strings.quickSettings, Icons.Default.Settings, colors.secondary) {
                perform(onOpenSettings)
            }
        }

        sectionHeader(key = "section_tools", title = strings.quickToolsSection)
        item(key = "clear_cache") {
            QuickTile(strings.quickClearCache, Icons.Default.CleaningServices, colors.secondary) {
                perform { viewModel.clearCacheWithoutTouchingSessions() }
            }
        }
        if (blockedAdsCount > 0) {
            item(key = "adblock") {
                QuickTile(
                    label = strings.adBlockChip(blockedAdsCount),
                    icon = Icons.Default.Security,
                    accent = colors.error,
                ) {
                    perform(onOpenSettings)
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { perform { onCopyLink(MarbleLinks.GITHUB_URL) } }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "${strings.appName} · ${MarbleLinks.GITHUB_DISPLAY}",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.sectionHeader(key: String, title: String) {
    item(span = { GridItemSpan(maxLineSpan) }, key = key) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** کارتِ وضعیتِ همین صفحه؛ جای «مراقب این سایت هستیم»ِ مبهمِ قبلی. */
@Composable
private fun SessionStatusCard(
    activeTab: BrowserTab?,
    pageUrl: String,
    prefs: BrowserPreferences,
    strings: MarbleStrings,
    onOpenVault: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = remember(pageUrl) { siteBadgeAccent(siteDomain(pageUrl)) }
    val status = activeTab?.healthStatus ?: SessionHealthStatus.DISABLED
    // متن به بازهٔ زنده‌سازیِ پیش‌فرضِ برنامه اشاره می‌کند (پنل جزئیاتِ هر سایت عدد دقیق
    // همان سایت را نشان می‌دهد)؛ قبلاً این عدد به‌صورت جادویی ۳۰ در متن نوشته شده بود.
    val keepAliveMinutes = DEFAULT_KEEP_ALIVE_MINUTES
    val protected = status == SessionHealthStatus.PROTECTED_ACTIVE ||
        status == SessionHealthStatus.HEALTHY ||
        status == SessionHealthStatus.RECOVERED_FROM_VAULT
    val shape = RoundedCornerShape(MarbleLayout.CardCorner)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.55f))
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .clickable(role = Role.Button, onClick = onOpenVault)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SiteBadgeLogo(
            displayName = activeTab?.title.orEmpty(),
            domain = siteDomain(pageUrl),
            size = 36.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activeTab?.title?.takeIf { it.isNotBlank() } ?: siteDomain(pageUrl),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (protected) {
                    strings.quickSiteSignedInFor(
                        days = prefs.defaultCookieLifetimeDays,
                        minutes = keepAliveMinutes,
                    )
                } else {
                    strings.healthStatusLabel(status)
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = if (protected) colors.primary else colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (protected) colors.primary else colors.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun QuickSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val enabled = value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh.copy(alpha = 0.85f))
            .border(0.9.dp, colors.outlineVariant.copy(alpha = 0.45f), shape)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_actions_search"),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (enabled) colors.primary else colors.surfaceContainerHighest)
                .clickable(enabled = enabled, role = Role.Button, onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = strings.goToAddress,
                tint = if (enabled) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** بازهٔ پیش‌فرضِ زنده‌سازی که در متنِ وضعیت استفاده می‌شود. */
private const val DEFAULT_KEEP_ALIVE_MINUTES = 30

/** یک کاشیِ هم‌عرض و هم‌ارتفاع در شبکهٔ میانبرها. */
@Composable
private fun QuickTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    active: Boolean = false,
    badge: Int = 0,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(MarbleLayout.TileCorner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(MarbleLayout.QuickTileHeight)
            .clip(shape)
            .background(
                if (active) accent.copy(alpha = 0.20f) else colors.surface.copy(alpha = 0.55f),
            )
            .border(
                width = if (active) 1.2.dp else 0.9.dp,
                color = if (active) accent.copy(alpha = 0.65f) else colors.outlineVariant.copy(alpha = 0.40f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.10f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(colors.error),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                        color = colors.onError,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 26.dp),
        )
    }
}
