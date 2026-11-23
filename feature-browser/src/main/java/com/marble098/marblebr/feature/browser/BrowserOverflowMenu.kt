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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChromeReaderMode
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
 * منوی سه‌نقطهٔ نوار بالا — نسخهٔ جستجوپذیر.
 *
 * ### مشکلِ کاربر
 * «برای پیدا کردنِ گزینهٔ دلخواهم باید دو ساعت اسکرول کنم.» منوی قبلی یک ستونِ بلندِ بی‌جستجو
 * بود و با اضافه شدنِ ابزارهای تازه (بلندخوانی، حالت مطالعه، دسترسی‌های سایت، حساب‌ها،
 * ماشین زمان…) پیدا کردنِ یک گزینه از روی حافظه سخت شده بود.
 *
 * ### راه‌حل
 * - فهرستِ گزینه‌ها داده‌محور شد ([MenuSection] / [MenuEntry])؛ هر ردیف عنوان و توضیحِ کوتاهِ
 *   خودش را دارد و همهٔ ردیف‌ها از یک قالبِ یکدست می‌آیند.
 * - یک **کادرِ جستجو** بالای منو، فهرست را همان‌جا فیلتر می‌کند (هم روی عنوان، هم روی توضیح).
 * - بخش‌ها با تیترِ کوچک از هم جدا می‌شوند و ارتفاعِ منو سقف دارد تا از صفحه بیرون نزند.
 * - ردیفِ «حالت سازگاری ورود» به بخشِ محافظت اضافه شد تا اگر دکمهٔ ورودِ یک سایت کار نکرد،
 *   کاربر بدونِ گشتن در تنظیمات همان‌جا حلش کند.
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
    /** آیا در این صفحه ورودِ ذخیره‌شده‌ای برای پرکردن هست؟ */
    val hasSavedLogin: Boolean = false,
    /** آیا این صفحه کادرِ رمزِ یکبارمصرف دارد؟ */
    val acceptsOneTimeCode: Boolean = false,
    /** آیا «حالت سازگاری ورود» برای همین سایت روشن است؟ */
    val isLoginCompatibilityOn: Boolean = false,
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
    // قابلیت تازه: به اشتراک‌گذاشتن صفحه و نشان کردن در نشان‌ها.
    val onSharePage: () -> Unit = {},
    val onBookmarkPage: () -> Unit = {},
    // ورودِ هوشمند: پرکردنِ فرم و گذاشتنِ کدِ یکبارمصرف.
    val onAutofillLogin: () -> Unit = {},
    val onFillOneTimeCode: () -> Unit = {},
    // اگر ورودِ سایتی کار نکرد، محافظتِ همین تب یک پله کنار می‌رود.
    val onToggleLoginCompatibility: () -> Unit = {},
)

/** یک ردیفِ منو؛ داده‌محور تا جستجو و بازچینش ساده باشد. */
internal data class MenuEntry(
    val tag: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val accent: Color,
    val badge: Int = 0,
    /** آیا این گزینه همین حالا روشن است؟ (فقط برای ردیفِ حالتِ سازگاری) */
    val active: Boolean = false,
    val activeLabel: String? = null,
    val onClick: () -> Unit,
)

/** یک بخشِ عنوان‌دارِ منو. */
internal data class MenuSection(val title: String, val entries: List<MenuEntry>)

/**
 * آیا این ردیف با عبارتِ جستجو می‌خواند؟
 *
 * خالص و قابلِ تست است: هم عنوان و هم توضیح بررسی می‌شوند تا کاربر بتواند «کد» را بنویسد و
 * گزینهٔ «گذاشتن کد یکبارمصرف» را پیدا کند.
 */
internal fun menuEntryMatches(query: String, title: String, subtitle: String?): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return title.contains(needle, ignoreCase = true) || subtitle?.contains(needle, ignoreCase = true) == true
}

/** فیلترکردنِ بخش‌ها و حذفِ بخش‌های خالی. */
internal fun filterMenuSections(sections: List<MenuSection>, query: String): List<MenuSection> {
    if (query.isBlank()) return sections
    return sections.mapNotNull { section ->
        val remaining = section.entries.filter { menuEntryMatches(query, it.title, it.subtitle) }
        if (remaining.isEmpty()) null else section.copy(entries = remaining)
    }
}

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
    var query by remember { mutableStateOf("") }

    val sections = remember(state, actions, strings, colors) { buildMenuSections(state, strings, actions, colors, onDismiss) }
    val visibleSections = remember(sections, query) { filterMenuSections(sections, query) }

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
            Column(modifier = Modifier.heightIn(max = MENU_MAX_HEIGHT)) {
                MenuHeader(state = state, strings = strings)
                MenuSearchField(
                    query = query,
                    placeholder = strings.menuSearchHint,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                )
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                ) {
                    if (visibleSections.isEmpty()) {
                        Text(
                            text = strings.menuSearchEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    } else {
                        visibleSections.forEachIndexed { index, section ->
                            // وقتی جستجو فعال است، خطِ جداکننده حذف می‌شود تا فهرست کوتاه و
                            // خوانا بماند؛ در حالت عادی بخش‌ها را جدا می‌کند.
                            if (index > 0 && query.isBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = colors.outlineVariant.copy(alpha = 0.35f),
                                )
                            }
                            MenuSectionTitle(section.title)
                            section.entries.forEach { entry -> MenuRow(entry = entry) }
                        }
                    }
                }
            }
        }
    }
}

/** ساختِ فهرستِ کاملِ منو؛ تنها جایی که ترتیب و شرطِ دیده‌شدنِ گزینه‌ها تعیین می‌شود. */
private fun buildMenuSections(
    state: BrowserMenuState,
    strings: MarbleStrings,
    actions: BrowserMenuActions,
    colors: ColorScheme,
    onDismiss: () -> Unit,
): List<MenuSection> {
    fun action(run: () -> Unit): () -> Unit = {
        onDismiss()
        run()
    }

    val sections = mutableListOf<MenuSection>()

    if (state.hasPage) {
        val page = mutableListOf<MenuEntry>()
        page += MenuEntry(
            tag = "menu_copy_link",
            icon = Icons.Default.ContentCopy,
            title = strings.copyLink,
            subtitle = state.domain.takeIf { it.isNotBlank() },
            accent = colors.primary,
            onClick = action(actions.onCopyLink),
        )
        page += MenuEntry(
            tag = "menu_share_link",
            icon = Icons.Default.Share,
            title = strings.shareLink,
            accent = colors.tertiary,
            onClick = action(actions.onShareLink),
        )
        page += MenuEntry(
            tag = "menu_save_pdf",
            icon = Icons.Default.Save,
            title = strings.contextSavePdf,
            accent = colors.primary,
            onClick = action(actions.onSavePdf),
        )
        page += MenuEntry(
            tag = "menu_duplicate_tab",
            icon = Icons.Default.Bookmark,
            title = strings.duplicateTab,
            accent = colors.secondary,
            onClick = action(actions.onDuplicateTab),
        )
        page += MenuEntry(
            tag = "menu_find_in_page",
            icon = Icons.Default.Security,
            title = strings.menuFindInPage,
            accent = colors.primary,
            onClick = action(actions.onFindInPage),
        )
        page += MenuEntry(
            tag = "menu_desktop_mode",
            icon = Icons.Default.DesktopWindows,
            title = if (state.isDesktopMode) strings.menuDesktopModeOff else strings.menuDesktopModeOn,
            accent = colors.tertiary,
            onClick = action(actions.onToggleDesktop),
        )
        page += MenuEntry(
            tag = "menu_read_aloud",
            icon = Icons.Default.RecordVoiceOver,
            title = strings.menuReadAloud,
            subtitle = strings.readAloudDescription,
            accent = colors.secondary,
            onClick = action(actions.onReadAloud),
        )
        page += MenuEntry(
            tag = "menu_reader_mode",
            icon = Icons.Default.ChromeReaderMode,
            title = strings.menuReaderMode,
            subtitle = strings.readerModeDescription,
            accent = colors.primary,
            onClick = action(actions.onReaderMode),
        )
        page += MenuEntry(
            tag = "menu_site_permissions",
            icon = Icons.Default.Tune,
            title = strings.menuSitePermissions,
            subtitle = strings.sitePermissionsDescription,
            accent = colors.tertiary,
            onClick = action(actions.onSitePermissions),
        )
        page += MenuEntry(
            tag = "menu_share_page",
            icon = Icons.Default.Share,
            title = strings.menuSharePage,
            accent = colors.tertiary,
            onClick = action(actions.onSharePage),
        )
        page += MenuEntry(
            tag = "menu_bookmark_page",
            icon = Icons.Default.Bookmark,
            title = strings.bookmarkToggle,
            accent = colors.secondary,
            onClick = action(actions.onBookmarkPage),
        )
        sections += MenuSection(strings.menuSectionPage, page)
    }

    if (state.hasPage && !state.isPrivate) {
        val protection = mutableListOf<MenuEntry>()
        if (state.hasSavedLogin) {
            protection += MenuEntry(
                tag = "menu_autofill_login",
                icon = Icons.Default.Key,
                title = strings.autofillFillNow,
                subtitle = strings.autofillConfirmBody,
                accent = colors.primary,
                onClick = action(actions.onAutofillLogin),
            )
        }
        if (state.acceptsOneTimeCode) {
            protection += MenuEntry(
                tag = "menu_fill_code",
                icon = Icons.Default.Pin,
                title = strings.codeFieldChip,
                subtitle = strings.codeManualHint,
                accent = colors.tertiary,
                onClick = action(actions.onFillOneTimeCode),
            )
        }
        if (state.canManageSiteAccounts) {
            protection += MenuEntry(
                tag = "menu_site_accounts",
                icon = Icons.Default.People,
                title = strings.accountsTitle,
                subtitle = strings.accountsDescription,
                accent = colors.secondary,
                onClick = action(actions.onSiteAccounts),
            )
        }
        // چرا این ردیف همیشه هست؟ چون «دکمهٔ ورود کار نمی‌کند» رایج‌ترین شکایتِ کاربر است و
        // پیدا کردنش نباید به گشتن در تنظیمات گره بخورد.
        protection += MenuEntry(
            tag = "menu_login_compatibility",
            icon = Icons.Default.LockOpen,
            title = strings.menuLoginCompatibility,
            subtitle = strings.menuLoginCompatibilityHint,
            accent = colors.primary,
            active = state.isLoginCompatibilityOn,
            activeLabel = strings.menuLoginCompatibilityOnChip,
            onClick = action(actions.onToggleLoginCompatibility),
        )
        protection += MenuEntry(
            tag = "menu_save_state",
            icon = Icons.Default.Shield,
            title = strings.menuSaveState,
            subtitle = strings.menuSaveStateHint,
            accent = colors.primary,
            onClick = action(actions.onSaveState),
        )
        protection += MenuEntry(
            tag = "menu_time_machine",
            icon = Icons.Default.History,
            title = strings.menuTimeMachine,
            subtitle = strings.menuTimeMachineHint,
            accent = colors.tertiary,
            onClick = action(actions.onOpenTimeMachine),
        )
        protection += MenuEntry(
            tag = "menu_install_shortcut",
            icon = Icons.Default.InstallMobile,
            title = strings.menuSaveShortcut,
            accent = colors.secondary,
            onClick = action(actions.onInstallShortcut),
        )
        sections += MenuSection(strings.menuSectionProtection, protection)
    }

    sections += MenuSection(
        strings.menuSectionTools,
        listOf(
            MenuEntry(
                tag = "menu_downloads",
                icon = Icons.Default.Download,
                title = strings.downloadsMenu(state.downloadsCount),
                accent = colors.primary,
                badge = state.downloadsCount,
                onClick = action(actions.onOpenDownloads),
            ),
            MenuEntry(
                tag = "menu_free_space",
                icon = Icons.Default.CleaningServices,
                title = strings.menuFreeSpace,
                subtitle = strings.menuFreeSpaceHint,
                accent = colors.secondary,
                onClick = action(actions.onFreeSpace),
            ),
        ),
    )

    sections += MenuSection(
        strings.menuSectionApp,
        listOf(
            MenuEntry(
                tag = "menu_settings",
                icon = Icons.Default.Settings,
                title = strings.settingsTitle,
                accent = colors.onSurfaceVariant,
                onClick = action(actions.onOpenSettings),
            ),
        ),
    )

    return sections
}

@Composable
private fun MenuSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            // کادرِ جستجو شیشه‌ای و کم‌رنگ است؛ روی کارتِ منو یک «جعبهٔ سفید» نمی‌سازد.
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.50f))
            .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("menu_search_field"),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(13.dp),
                )
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
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
    ) {
        if (state.domain.isNotBlank()) {
            SiteBadgeLogo(
                displayName = state.displayName.ifBlank { state.domain },
                domain = state.domain,
                size = 32.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
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
                    modifier = Modifier.size(17.dp),
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
                    state.isLoginCompatibilityOn -> strings.menuLoginCompatibilityOnChip
                    state.isProtected -> strings.menuProtectedChip
                    else -> strings.browserMenu
                },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp),
                color = if (state.isLoginCompatibilityOn || (state.isProtected && !state.isPrivate)) colors.primary else colors.onSurfaceVariant,
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
private fun MenuRow(entry: MenuEntry) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .clip(shape)
            .then(
                if (entry.active) {
                    Modifier.background(colors.primary.copy(alpha = 0.12f))
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = entry.onClick)
            .testTag(entry.tag)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(entry.accent.copy(alpha = 0.24f), entry.accent.copy(alpha = 0.09f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = entry.icon, contentDescription = null, tint = entry.accent, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!entry.subtitle.isNullOrBlank()) {
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.badge > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.error)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.badge.coerceAtMost(99).toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                    color = colors.onError,
                )
            }
        } else if (entry.active) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = colors.primary,
                )
            }
        }
    }
}

private val MENU_WIDTH = 306.dp
private val MIN_MENU_WIDTH = 240.dp
private val MENU_MAX_HEIGHT = 520.dp
private val MENU_ANCHOR_GAP = 6.dp
