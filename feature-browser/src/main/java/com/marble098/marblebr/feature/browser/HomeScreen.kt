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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.AddressSuggestionEngine
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.HistoryItem
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
    showNewTabTile: Boolean,
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
    onOpenTabs: () -> Unit,
    modifier: Modifier = Modifier,
    tabsCount: Int = 0,
    bottomContentInset: Dp = 0.dp,
    passwordCount: Int = 0,
    protectedSiteCount: Int = 0,
    pulseSites: List<String> = emptyList(),
    onOpenVault: () -> Unit = {},
    onAddAnySite: (String) -> Unit = {},
    onOpenPasswordStudio: () -> Unit = {},
) {
    val strings = LocalMarbleStrings.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var homeSearch by remember { mutableStateOf("") }
    // پیشنهادهای زنده از همان موتورِ نوار آدرس: تاریخچه، نشان‌ها و پربازدیدها.
    // محاسبهٔ محلی و سبک است (حداکثر چند صد آیتم) و هیچ داده‌ای ذخیره نمی‌کند.
    val homeSuggestions = remember(homeSearch, bookmarks, historyItems) {
        AddressSuggestionEngine.build(
            query = homeSearch,
            history = historyItems,
            bookmarks = bookmarks,
            limit = 5,
        )
    }
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
                start = MarbleLayout.Screen,
                end = MarbleLayout.Screen,
                top = MarbleLayout.SectionGap,
                bottom = MarbleLayout.Screen + MarbleLayout.SectionGap + bottomContentInset,
            ),
            verticalArrangement = Arrangement.spacedBy(MarbleLayout.SectionGap),
        ) {
            item(key = "hero") {
                HomeHeader(
                    blockedAdsCount = blockedAdsCount,
                    strings = strings,
                    onOpenSettings = onOpenSettings,
                )
            }

            item(key = "search") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeSearchBar(
                        value = homeSearch,
                        placeholder = strings.homeSearchHint,
                        onValueChange = { homeSearch = it },
                        onSubmit = { submit(homeSearch) },
                        onClear = { homeSearch = "" },
                        chromeBarStyle = chromeBarStyle,
                    )
                    // پیشنهادهای زنده: همان موتورِ نوار آدرس، تا رفتارِ جستجو در خانه و
                    // داخلِ صفحه یکسان باشد.
                    if (homeSearch.isNotBlank() && homeSuggestions.isNotEmpty()) {
                        AddressSuggestionList(
                            suggestions = homeSuggestions,
                            onPick = { suggestion ->
                                homeSearch = ""
                                submit(suggestion.url)
                            },
                        )
                    }
                }
            }

            item(key = "quick_actions") {
                HomeQuickActions(
                    strings = strings,
                    chromeBarStyle = chromeBarStyle,
                    showNewTabTile = showNewTabTile,
                    downloadsCount = downloadsCount,
                    onNewTab = onNewTab,
                    onNewPrivateTab = onNewPrivateTab,
                    onOpenBookmarksHistory = onOpenBookmarksHistory,
                    onOpenDownloads = onOpenDownloads,
                    onOpenSettings = onOpenSettings,
                    onOpenQuickActions = onOpenQuickActions,
                    onOpenTabs = onOpenTabs,
                    tabsCount = tabsCount,
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
                bookmarks.take(4).chunked(2).forEachIndexed { index, row ->
                    item(key = "shortcut_row_$index") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
                        ) {
                            row.forEach { bookmark ->
                                ShortcutCard(
                                    bookmark = bookmark,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(MarbleLayout.ShortcutHeight),
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
                // حداکثر سه بازدیدِ آخر: فهرستِ بلندِ ده‌تایی فقط اسکرول را طولانی می‌کرد.
                items(historyItems.take(3), key = { it.id }) { item ->
                    RecentRow(item = item, onClick = { submit(item.url) })
                }
            }

            // کارت‌های «ابزار» عمداً به آخرِ صفحه رفتند: چیزی که کاربر هر روز می‌خواهد
            // (جستجو، میانبرها، سایت‌های اخیر) اول می‌آید و کارت‌های توضیحی آخر.
            item(key = "any_site") {
                AddAnySiteCard(
                    strings = strings,
                    onAdd = onAddAnySite,
                    chromeBarStyle = chromeBarStyle,
                )
            }

            item(key = "vault_glance") {
                VaultGlanceCard(
                    strings = strings,
                    passwordCount = passwordCount,
                    protectedSiteCount = protectedSiteCount,
                    pulseSites = pulseSites,
                    chromeBarStyle = chromeBarStyle,
                    onOpenVault = onOpenVault,
                    onOpenPasswordStudio = onOpenPasswordStudio,
                )
            }

        }
    }
}

/* ------------------------------------ افزودن هر سایتی ------------------------------------ */

/**
 * کارتِ «افزودن هر سایتی».
 *
 * پیامِ این کارت هیچ‌وقت «این سایت پشتیبانی نمی‌شود» نیست؛ آدرس را می‌گیرد و به نردبانِ مسیرها
 * می‌سپارد. اگر یک راه جواب نداد، همان کارت راهِ بعدی را پیشنهاد می‌کند.
 */
@Composable
private fun AddAnySiteCard(
    strings: MarbleStrings,
    onAdd: (String) -> Unit,
    chromeBarStyle: ChromeBarStyle,
) {
    val colors = MaterialTheme.colorScheme
    var input by rememberSaveable { mutableStateOf("") }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = shape,
                fillAlphaOverride = if (chromeBarStyle == ChromeBarStyle.SOLID) 0.98f else 0.55f,
                glow = false,
                elevation = 4.dp,
            )
            // بدنهٔ فشرده: توضیحِ دوخطی حذف شد و جای آن راهنمای کوتاه داخلِ خودِ فیلد است؛
            // این کارت پیش‌تر حدودِ یک‌سومِ صفحهٔ خانه را می‌گرفت.
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = strings.addAnySiteTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it.trimStart() },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.isNotBlank()) {
                        onAdd(input)
                        input = ""
                    }
                }),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceContainerHigh.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("home_add_any_site_field"),
                decorationBox = { inner ->
                    if (input.isEmpty()) {
                        Text(
                            text = strings.addAnySiteHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
            FilledTonalButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onAdd(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.testTag("home_add_any_site_btn"),
            ) {
                Text(strings.addAnySiteAction, maxLines = 1)
            }
        }
    }
}

/* -------------------------------------- نگاهِ مخزن -------------------------------------- */

/** کارتِ کوتاهِ «گذرواژه‌ها و نشست‌ها» با شمارنده‌ها و پیشنهادِ نبضِ نشست. */
@Composable
private fun VaultGlanceCard(
    strings: MarbleStrings,
    passwordCount: Int,
    protectedSiteCount: Int,
    pulseSites: List<String>,
    chromeBarStyle: ChromeBarStyle,
    onOpenVault: () -> Unit,
    onOpenPasswordStudio: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlass(
                style = ChromeBarStyle.GLASS,
                shape = shape,
                fillAlphaOverride = if (chromeBarStyle == ChromeBarStyle.SOLID) 0.98f else 0.55f,
                glow = false,
                elevation = 4.dp,
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = strings.backupTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onOpenVault) { Text(strings.homeSeeAll) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap)) {
            GlanceStat(
                modifier = Modifier.weight(1f),
                value = passwordCount.toString(),
                label = strings.backupIncludeCredentials,
                accent = colors.primary,
            )
            GlanceStat(
                modifier = Modifier.weight(1f),
                value = protectedSiteCount.toString(),
                label = strings.protectionLabel,
                accent = colors.tertiary,
            )
            GlanceStat(
                modifier = Modifier.weight(1f),
                value = pulseSites.size.toString(),
                label = strings.pulseTitle,
                accent = colors.secondary,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onOpenPasswordStudio,
                modifier = Modifier.testTag("home_vault_glance_btn"),
            ) {
                Text(strings.saveLoginFillTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (pulseSites.isNotEmpty()) {
                Text(
                    text = strings.pulseDescription,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GlanceStat(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            .size(42.dp)
            .clip(RoundedCornerShape(15.dp))
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
    showNewTabTile: Boolean,
    downloadsCount: Int,
    tabsCount: Int,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onOpenBookmarksHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickActions: () -> Unit,
    onOpenTabs: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    // دو ردیفِ دقیقاً چهارستونی؛ هر هشت کاشی هم‌عرض و هم‌ارتفاع‌اند (ارتفاع ثابت در QuickTile)
    // و فاصله‌ها همه از MarbleLayout می‌آیند تا هیچ ردیفی از ردیفِ دیگر جلو/عقب نزند.
    // (قبلاً ردیف دوم یک جای خالی داشت و ردیف‌ها ناهم‌تراز می‌شدند.)
    QuickRow {
        // «تبِ تازه» دیگر یک دکمهٔ شناورِ مستقل روی صفحه نیست: کاشیِ اصلی و پُررنگِ همین
        // شبکه است. همیشه اول دیده می‌شود و دقیقاً کنار بقیهٔ میانبرها می‌نشیند.
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Add,
            label = strings.newTab,
            accent = colors.primary,
            chromeBarStyle = chromeBarStyle,
            highlighted = true,
            enabled = showNewTabTile,
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
            icon = Icons.Default.Tab,
            label = strings.tabsLabel,
            accent = colors.tertiary,
            chromeBarStyle = chromeBarStyle,
            badge = tabsCount,
            onClick = onOpenTabs,
        )
        QuickTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            label = strings.settingsLabel,
            accent = colors.onSurfaceVariant,
            chromeBarStyle = chromeBarStyle,
            onClick = onOpenSettings,
        )
    }
}

/** یک ردیفِ چهارستونیِ هم‌اندازه؛ کاشی‌ها همیشه هم‌عرض‌اند (وزن یکسان). */
@Composable
private fun QuickRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
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
    highlighted: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "quick tile press",
    )
    val shape = RoundedCornerShape(MarbleLayout.TileCorner)
    val scheme = MaterialTheme.colorScheme
    // کاشیِ «اصلی» (تبِ تازه) یک بدنهٔ گرادیانیِ رنگی و خوانا می‌گیرد تا نقشِ دکمهٔ افزودن را
    // همان‌جا و بدون یک عنصر شناورِ اضافه ایفا کند.
    val tileBrush = if (highlighted) {
        Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))
    } else {
        Brush.linearGradient(listOf(scheme.surface.copy(alpha = 0.55f), scheme.surface.copy(alpha = 0.55f)))
    }
    val labelColor = if (highlighted) scheme.onPrimary else scheme.onSurface
    Column(
        modifier = modifier
            .height(MarbleLayout.TileHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.9f
            }
            .clip(shape)
            .background(tileBrush)
            .border(
                width = 0.9.dp,
                color = if (highlighted) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.45f),
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick,
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (highlighted) {
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.16f)))
                        } else {
                            Brush.linearGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.12f)))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (highlighted) Color.White else accent,
                    modifier = Modifier.size(19.dp),
                )
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
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.5.sp, lineHeight = 13.sp),
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 26.dp),
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
    val shape = RoundedCornerShape(MarbleLayout.CardCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "shortcut press",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(0.9.dp, accent.copy(alpha = 0.28f), shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            .height(MarbleLayout.RecentRowHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
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
