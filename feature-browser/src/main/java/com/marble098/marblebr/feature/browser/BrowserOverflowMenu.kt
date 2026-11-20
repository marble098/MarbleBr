package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.i18n.MarbleStrings

/**
 * منوی سه‌نقطهٔ نوار بالا — بازنویسیِ کامل.
 *
 * ### باگ اصلیِ نسخهٔ قبل
 * منو در ستونِ چیدمانِ صفحه (بعد از نوار بالا) رندر می‌شد، نه داخل خودِ دکمه؛ پس `DropdownMenu`
 * مکانِ خودش را از **والدِ چیدمانی** می‌گرفت و در گوشهٔ بالا-شروعِ صفحه باز می‌شد، نه زیرِ سه‌نقطه.
 * حالا منو یک [Popup] است که داخل همان `Box`ی رندر می‌شود که دکمه را نگه داشته؛ پس همیشه
 * دقیقاً زیرِ همان دکمه و چسبیده به لبهٔ آن باز می‌شود (در RTL هم درست است، چون
 * `Alignment.TopEnd` با جهتِ چیدمان جلو/عقب می‌رود).
 *
 * ### بازطراحیِ ظاهری
 * - کارتِ شیشه‌ایِ گرد با سایهٔ نرم و حاشیهٔ نازک (به‌جای لیستِ تختِ پیش‌فرض).
 * - سربرگ با لوگوی سایت و وضعیتِ محافظت، تا معلوم باشد منو مربوط به کدام صفحه است.
 * - چهار گروهِ عنوان‌دار: «این صفحه»، «محافظت از ورود»، «کتابخانه و ابزار»، «برنامه».
 * - هر ردیف: حبابِ رنگیِ آیکن + عنوان + توضیحِ کوتاه؛ فاصله‌ها و ارتفاع‌ها یکدست‌اند.
 * - اسکرولِ داخلی و عرضِ واکنش‌گرا تا روی نمایشگرهای کوچک هم از صفحه بیرون نزند.
 */

/** داده‌های نمایشیِ منو (همه از وضعیتِ همان تبِ فعال می‌آیند). */
internal data class BrowserMenuState(
    val domain: String = "",
    val displayName: String = "",
    val isPrivate: Boolean = false,
    val isProtected: Boolean = false,
    val isDesktopMode: Boolean = false,
    val hasPage: Boolean = false,
    val canManageSiteAccounts: Boolean = false,
    val downloadsCount: Int = 0,
)

/** کارهایی که منو انجام می‌دهد؛ یک شیء تا امضای نوار بالا کوتاه بماند. */
internal data class BrowserMenuActions(
    val onCopyLink: () -> Unit,
    val onShareLink: () -> Unit,
    val onFindInPage: () -> Unit,
    val onToggleDesktop: () -> Unit,
    val onSaveState: () -> Unit,
    val onOpenTimeMachine: () -> Unit,
    val onInstallShortcut: () -> Unit,
    val onDuplicateTab: () -> Unit,
    val onOpenDownloads: () -> Unit,
    val onFreeSpace: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onReadAloud: () -> Unit = {},
    val onReaderMode: () -> Unit = {},
    val onSitePermissions: () -> Unit = {},
    val onSiteAccounts: () -> Unit = {},
    val onSavePdf: () -> Unit = {},
)

@Composable
internal fun BrowserOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: BrowserMenuState,
    actions: BrowserMenuActions,
) {
    if (!expanded) return
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = (screenWidth - 32.dp).coerceAtLeast(MIN_MENU_WIDTH)
    val menuWidth = MENU_WIDTH.coerceAtMost(maxWidth)
    // ارتفاعِ دکمه (۴۲dp) + فاصلهٔ نازک، تا منو دقیقاً زیرِ سه‌نقطه بنشیند.
    val verticalOffset = with(density) { (MENU_ANCHOR_GAP + 42.dp).roundToPx() }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = verticalOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .width(menuWidth)
                .testTag("browser_overflow_menu"),
            shape = RoundedCornerShape(26.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(0.9.dp, colors.outlineVariant.copy(alpha = 0.45f)),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
            ) {
                MenuHeader(state = state, strings = strings)

                if (state.hasPage) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        color = colors.outlineVariant.copy(alpha = 0.35f),
                    )
                    MenuSectionTitle(strings.menuSectionPage)
                    MenuRow(
                        icon = Icons.Default.ContentCopy,
                        title = strings.copyLink,
                        subtitle = state.domain.takeIf { it.isNotBlank() },
                        accent = colors.primary,
                        testTag = "menu_copy_link",
                    ) { onDismiss(); actions.onCopyLink() }
                    MenuRow(
                        icon = Icons.Default.Share,
                        title = strings.shareLink,
                        accent = colors.tertiary,
                        testTag = "menu_share_link",
                    ) { onDismiss(); actions.onShareLink() }
                    MenuRow(
                        icon = Icons.Default.Save,
                        title = strings.contextSavePdf,
                        accent = colors.primary,
                        testTag = "menu_save_pdf",
                    ) { onDismiss(); actions.onSavePdf() }
                    MenuRow(
                        icon = Icons.Default.Bookmark,
                        title = strings.duplicateTab,
                        accent = colors.secondary,
                        testTag = "menu_duplicate_tab",
                    ) { onDismiss(); actions.onDuplicateTab() }
                    MenuRow(
                        icon = Icons.Default.Security,
                        title = strings.menuFindInPage,
                        accent = colors.primary,
                        testTag = "menu_find_in_page",
                    ) { onDismiss(); actions.onFindInPage() }
                    MenuRow(
                        icon = Icons.Default.DesktopWindows,
                        title = if (state.isDesktopMode) strings.menuDesktopModeOff else strings.menuDesktopModeOn,
                        accent = colors.tertiary,
                        testTag = "menu_desktop_mode",
                    ) { onDismiss(); actions.onToggleDesktop() }
                    // سه ابزار «خواندن و دسترسی» که تازه اضافه شده‌اند.
                    MenuRow(
                        icon = Icons.Default.RecordVoiceOver,
                        title = strings.menuReadAloud,
                        subtitle = strings.readAloudDescription,
                        accent = colors.secondary,
                        testTag = "menu_read_aloud",
                    ) { onDismiss(); actions.onReadAloud() }
                    MenuRow(
                        icon = Icons.Default.ChromeReaderMode,
                        title = strings.menuReaderMode,
                        subtitle = strings.readerModeDescription,
                        accent = colors.primary,
                        testTag = "menu_reader_mode",
                    ) { onDismiss(); actions.onReaderMode() }
                    MenuRow(
                        icon = Icons.Default.Tune,
                        title = strings.menuSitePermissions,
                        subtitle = strings.sitePermissionsDescription,
                        accent = colors.tertiary,
                        testTag = "menu_site_permissions",
                    ) { onDismiss(); actions.onSitePermissions() }
                }

                if (state.hasPage && !state.isPrivate) {
                    MenuSectionTitle(strings.menuSectionProtection)
                    if (state.canManageSiteAccounts) {
                        MenuRow(
                            icon = Icons.Default.People,
                            title = strings.accountsTitle,
                            subtitle = strings.accountsDescription,
                            accent = colors.secondary,
                            testTag = "menu_site_accounts",
                        ) { onDismiss(); actions.onSiteAccounts() }
                    }
                    MenuRow(
                        icon = Icons.Default.Shield,
                        title = strings.menuSaveState,
                        subtitle = strings.menuSaveStateHint,
                        accent = colors.primary,
                        testTag = "menu_save_state",
                    ) { onDismiss(); actions.onSaveState() }
                    MenuRow(
                        icon = Icons.Default.History,
                        title = strings.menuTimeMachine,
                        subtitle = strings.menuTimeMachineHint,
                        accent = colors.tertiary,
                        testTag = "menu_time_machine",
                    ) { onDismiss(); actions.onOpenTimeMachine() }
                    MenuRow(
                        icon = Icons.Default.InstallMobile,
                        title = strings.menuSaveShortcut,
                        accent = colors.secondary,
                        testTag = "menu_install_shortcut",
                    ) { onDismiss(); actions.onInstallShortcut() }
                }

                MenuSectionTitle(strings.menuSectionTools)
                MenuRow(
                    icon = Icons.Default.Download,
                    title = strings.downloadsMenu(state.downloadsCount),
                    accent = colors.primary,
                    badge = state.downloadsCount,
                    testTag = "menu_downloads",
                ) { onDismiss(); actions.onOpenDownloads() }
                MenuRow(
                    icon = Icons.Default.CleaningServices,
                    title = strings.menuFreeSpace,
                    subtitle = strings.menuFreeSpaceHint,
                    accent = colors.secondary,
                    testTag = "menu_free_space",
                ) { onDismiss(); actions.onFreeSpace() }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = colors.outlineVariant.copy(alpha = 0.35f),
                )
                MenuSectionTitle(strings.menuSectionApp)
                MenuRow(
                    icon = Icons.Default.Settings,
                    title = strings.settingsTitle,
                    accent = colors.onSurfaceVariant,
                    testTag = "menu_settings",
                ) { onDismiss(); actions.onOpenSettings() }
            }
        }
    }
}

@Composable
private fun MenuHeader(state: BrowserMenuState, strings: MarbleStrings) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
    ) {
        if (state.domain.isNotBlank()) {
            SiteBadgeLogo(
                displayName = state.displayName.ifBlank { state.domain },
                domain = state.domain,
                size = 34.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.30f), colors.tertiary.copy(alpha = 0.18f))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.domain.ifBlank { strings.browserMenu },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    state.isPrivate -> strings.menuPrivateHint
                    state.isProtected -> strings.menuProtectedChip
                    else -> strings.browserMenu
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = if (state.isProtected && !state.isPrivate) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MenuSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: Int = 0,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.09f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.error)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                    color = colors.onError,
                )
            }
        }
    }
}

private val MENU_WIDTH = 300.dp
private val MIN_MENU_WIDTH = 236.dp
private val MENU_ANCHOR_GAP = 6.dp
