package com.marble098.marblebr.feature.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleGlassIconButton
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.HomeShortcutPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * صفحهٔ خانهٔ MarbleBr — بازطراحی کامل.
 *
 * مشکلات نسخهٔ قبل: تیتر و متنِ توضیحیِ تکراری، جستجوی بی‌قاب، کارت‌های پُرحجمِ تک‌ستونی،
 * ردیفِ چهار دکمهٔ نامتقارن که متن‌هایش سرریز می‌شد و ترتیب بصریِ نامنظم.
 *
 * ساختار تازه:
 * ۱. سربرگ فشرده با نشان برنامه و وضعیت محافظت (نه یک تیتر بزرگ و بی‌مصرف).
 * ۲. جستجوی شیشه‌ایِ تمام‌عرض با دکمهٔ «برو» که همیشه دیده می‌شود.
 * ۳. ردیف میانبرهای سريع به‌صورت کاشی‌های هم‌اندازه و هم‌تراز (Grid Style).
 * ۴. نشان‌ها به‌صورت شبکهٔ دو ستونی با لوگوی رنگی هر سایت.
 * ۵. بازدیدهای اخیر در قالبِ فشرده با زمانِ نسبی.
 * ۶. حالت خالیِ دلنشین به‌جای متن خشک.
 */
@Composable
internal fun HomeContent(
    bookmarks: List<BookmarkItem>,
    historyItems: List<HistoryItem>,
    shortcutPosition: HomeShortcutPosition,
    chromeBarStyle: ChromeBarStyle,
    blockedAdsCount: Int,
    downloadsCount: Int,
    onSearch: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onOpenBookmarksHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickActions: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentInset: Dp = 0.dp,
) {
    val strings = LocalMarbleStrings.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var homeSearch by remember { mutableStateOf("") }
    val submit: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            focusManager.clearFocus()
            keyboard?.hide()
            onSearch(query)
            homeSearch = ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeBackdrop()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            // فضای پایین = ارتفاع نوار شناور تا آخرین کارت پشت شیشه پنهان نشود.
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 34.dp + bottomContentInset,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero") {
                HomeHeader(
                    blockedAdsCount = blockedAdsCount,
                    strings = strings,
                    onOpenSettings = onOpenSettings,
                )
            }

            item(key = "search") {
                HomeSearchBar(
                    value = homeSearch,
                    placeholder = strings.homeSearchHint,
                    onValueChange = { homeSearch = it },
                    onSubmit = { submit(homeSearch) },
                    onClear = { homeSearch = "" },
                    chromeBarStyle = chromeBarStyle,
                )
            }

            if (shortcutPosition == HomeShortcutPosition.TOP) {
                item(key = "top_tile") { HomePrimaryTile(strings = strings, onNewTab = onNewTab) }
            }

            item(key = "quick_actions") {
                HomeQuickActions(
                    strings = strings,
                    chromeBarStyle = chromeBarStyle,
                    downloadsCount = downloadsCount,
                    onNewTab = onNewTab,
                    onNewPrivateTab = onNewPrivateTab,
                    onOpenBookmarksHistory = onOpenBookmarksHistory,
                    onOpenDownloads = onOpenDownloads,
                    onOpenSettings = onOpenSettings,
                    onOpenQuickActions = onOpenQuickActions,
                )
            }

            item(key = "shortcuts_header") {
                HomeSectionHeader(
                    title = strings.homeShortcutsTitle,
                    actionLabel = if (bookmarks.isEmpty()) null else strings.homeSeeAll,
                    onAction = onOpenBookmarksHistory,
                )
            }

            if (bookmarks.isEmpty()) {
                item(key = "shortcuts_empty") {
                    HomeEmptyCard(
                        icon = Icons.Default.BookmarkBorder,
                        title = strings.homeEmptyShortcuts,
                        hint = strings.homeSearchHint,
                    )
                }
            } else {
                // شبکهٔ دو ستونیِ نشان‌ها؛ با سایت‌های پرتکرار همیشه هم‌اندازه و منظم می‌ماند.
                bookmarks.take(6).chunked(2).forEachIndexed { index, row ->
                    item(key = "shortcut_row_$index") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { bookmark ->
                                ShortcutCard(
                                    bookmark = bookmark,
                                    modifier = Modifier.weight(1f),
                                    onClick = { submit(bookmark.url) },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "recent_header") {
                HomeSectionHeader(
                    title = strings.homeRecentTitle,
                    actionLabel = if (historyItems.isEmpty()) null else strings.homeSeeAll,
                    onAction = onOpenBookmarksHistory,
                )
            }

            if (historyItems.isEmpty()) {
                item(key = "recent_empty") {
                    HomeEmptyCard(
                        icon = Icons.Default.History,
                        title = strings.homeEmptyRecent,
                        hint = strings.homeIntro,
                    )
                }
            } else {
                items(historyItems.take(5), key = { it.id }) { item ->
                    RecentRow(item = item, onClick = { submit(item.url) })
                }
            }
        }
    }
}

/* ----------------------------------------- سربرگ ----------------------------------------- */

@Composable
private fun HomeHeader(
    blockedAdsCount: Int,
    strings: MarbleStrings,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MarbleLogoMark()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.homeTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = strings.homeIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (blockedAdsCount > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = blockedAdsCount.coerceAtMost(999).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        MarbleGlassIconButton(
            icon = Icons.Default.Settings,
            contentDescription = strings.settingsLabel,
            onClick = onOpenSettings,
            style = ChromeBarStyle.FLOATING,
            size = 40.dp,
            iconSize = 19.dp,
        )
    }
}

/** نشانِ اختصاصی برنامه: کرهٔ مرمریِ گرادیانی با حرف M. */
@Composable
private fun MarbleLogoMark() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)),
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "M",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
    }
}

/* ----------------------------------------- جستجو ----------------------------------------- */

@Composable
private fun HomeSearchBar(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    chromeBarStyle: ChromeBarStyle,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val shape = RoundedCornerShape(24.dp)
    val enabled = value.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = shape,
                fillAlphaOverride = if (chromeBarStyle == ChromeBarStyle.SOLID) 0.98f else (if (dark) 0.62f else 0.80f),
                glow = true,
                elevation = 8.dp,
            )
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_field"),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.dismiss,
                    modifier = Modifier.size(17.dp),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(colors.primary, colors.tertiary))
                    } else {
                        Brush.linearGradient(listOf(colors.surfaceContainerHigh, colors.surfaceContainerHigh))
                    },
                )
                .clickable(enabled = enabled, role = Role.Button, onClick = onSubmit)
                .testTag("home_search_go_btn"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = strings.goToAddress,
                tint = if (enabled) Color.White else colors.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/* ------------------------------------- میانبرهای سریع ------------------------------------- */

@Composable
private fun HomeQuickActions(
    strings: MarbleStrings,
    chromeBarStyle: ChromeBarStyle,
    downloadsCount: Int,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onOpenBookmarksHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickActions: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    // نسخهٔ قبل هفت کاشیِ هم‌وزن را در یک FlowRow می‌ریخت؛ ردیفِ آخر تک‌وتنها می‌ماند و
    // کاشی‌ها با متن‌های فارسیِ بلند ناهم‌تراز می‌شدند. حالا هر ردیف دقیقاً چهار کاشیِ
    // هم‌اندازه دارد و دو گروهِ معنادار («شروع سریع» و «کتابخانه») با فاصلهٔ بصری جدا شده‌اند.
    QuickRow {
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = strings.newTab,
            accent = colors.primary,
            chromeBarStyle = chromeBarStyle,
            onClick = onNewTab,
            testTag = "home_new_tab_btn",
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.VisibilityOff,
            label = strings.newPrivateTab,
            accent = colors.tertiary,
            chromeBarStyle = chromeBarStyle,
            onClick = onNewPrivateTab,
            testTag = "home_new_private_btn",
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bolt,
            label = strings.quickActionsTitle,
            accent = colors.secondary,
            chromeBarStyle = chromeBarStyle,
            onClick = onOpenQuickActions,
            testTag = "home_quick_actions_btn",
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Shield,
            label = strings.protectionLabel,
            accent = colors.primary,
            chromeBarStyle = chromeBarStyle,
            onClick = onOpenSettings,
        )
    }

    Spacer(Modifier.height(2.dp))

    QuickRow {
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bookmark,
            label = strings.bookmarksLabel,
            accent = colors.secondary,
            chromeBarStyle = chromeBarStyle,
            onClick = onOpenBookmarksHistory,
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Download,
            label = strings.quickDownloads,
            accent = colors.primary,
            chromeBarStyle = chromeBarStyle,
            badge = downloadsCount,
            onClick = onOpenDownloads,
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            label = strings.settingsLabel,
            accent = colors.onSurfaceVariant,
            chromeBarStyle = chromeBarStyle,
            onClick = onOpenSettings,
        )
        // کاشیِ «میان‌بر خالی» عمداً خالی می‌ماند تا شبکه‌ی چهارستونی نشکند.
        Spacer(Modifier.weight(1f))
    }
}

/** یک ردیفِ چهارستونیِ هم‌اندازه؛ کاشی‌ها همیشه هم‌عرض‌اند (وزن یکسان). */
@Composable
private fun QuickRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/**
 * کاشی میان‌بر: شبکهٔ ۴ ستونی، همه هم‌عرض و هم‌ارتفاع با آیکن رنگی در حبابِ اختصاصی.
 *
 * برخلاف نسخهٔ قبل، متن حداکثر در دو خط کوتاه می‌آید و اندازهٔ کاشی از محتوای همسایه‌ها
 * پیروی نمی‌کند؛ پس ردیف هرگز نامنظم نمی‌شود.
 */
@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    chromeBarStyle: ChromeBarStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
    testTag: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "quick tile press",
    )
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .border(0.9.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.12f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badge.coerceAtMost(9).toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ----------------------------------------- بخش‌ها ----------------------------------------- */

@Composable
private fun HomeSectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    bookmark: BookmarkItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = remember(bookmark.url) { siteBadgeAccent(siteDomain(bookmark.url)) }
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "shortcut press",
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(0.9.dp, accent.copy(alpha = 0.28f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SiteBadgeLogo(
            displayName = bookmark.title,
            domain = siteDomain(bookmark.url),
            size = 34.dp,
        )
        Text(
            text = bookmark.title.ifBlank { siteDomain(bookmark.url) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = siteDomain(bookmark.url),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentRow(item: HistoryItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SiteBadgeLogo(
            displayName = item.title,
            domain = siteDomain(item.url),
            size = 30.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { siteDomain(item.url) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = siteDomain(item.url),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = relativeTime(item.visitedAtEpochMillis),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun HomeEmptyCard(
    icon: ImageVector,
    title: String,
    hint: String,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    ),
                ),
                shape = shape,
            )
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomePrimaryTile(strings: MarbleStrings, onNewTab: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onNewTab)
            .testTag("home_shortcut_btn"),
        shape = RoundedCornerShape(20.dp),
        color = colors.primaryContainer.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(20.dp))
            Text(
                text = strings.newTab,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/* ----------------------------------------- پس‌زمینه ----------------------------------------- */

/** هالهٔ گرادیانی بسیار ملایم؛ جایگزینِ پس‌زمینهٔ یکدستِ بی‌روحِ قبلی. */
@Composable
private fun HomeBackdrop() {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primary.copy(alpha = if (dark) 0.16f else 0.09f),
                        colors.background,
                        colors.tertiary.copy(alpha = if (dark) 0.10f else 0.05f),
                    ),
                ),
            ),
    )
}

/* ------------------------------------------ کمکی ------------------------------------------ */

/** دامنهٔ کوتاه یک نشانی برای نمایش. */
internal fun siteDomain(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    val host = runCatching {
        val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        java.net.URI(normalized).host.orEmpty()
    }.getOrDefault("")
    if (host.isNotBlank()) return host.lowercase().removePrefix("www.")
    // ورودیِ غیرمعمول (مثل about:blank یا نشانیِ ناقص): تنها بخشِ میزبان‌مانند را نگه می‌داریم
    // و هرگز رشتهٔ کاملِ حاوی پروتکل را برنمی‌گردانیم تا لوگو و برچسبِ کارت تمیز بماند.
    return trimmed
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .lowercase()
        .removePrefix("www.")
}

/** زمان نسبی کوتاه و دوزبانه‌پسند (از روی فاصلهٔ زمانی، نه قالبِ متنی). */
private fun relativeTime(epochMillis: Long): String {
    val diff = (System.currentTimeMillis() - epochMillis).coerceAtLeast(0)
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "•"
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        minutes < 10_080 -> "${minutes / 1_440}d"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(epochMillis))
    }
}
