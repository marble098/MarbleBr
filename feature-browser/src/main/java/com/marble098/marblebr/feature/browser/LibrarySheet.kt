package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.model.HistoryItem

/**
 * شیتِ «کتابخانه»: نشان‌ها و تاریخچه، کامل و حرفه‌ای.
 *
 * **نقدِ نسخهٔ قبل:** شیت فقط دو فهرستِ خام را نشان می‌داد: نه جستجو، نه پوشه، نه ویرایش،
 * نه حذفِ تک‌آیتم، نه پاک‌سازیِ زمانی؛ یعنی کاربر هیچ راهی برای مدیریتِ دادهٔ خودش نداشت.
 * حالا:
 * - جستجوی زنده در نشان‌ها و تاریخچه؛
 * - نشان‌ها: افزودن، ویرایشِ عنوان/آدرس/پوشه، انتقال به پوشه و حذف؛
 * - تاریخچه: گروه‌بندیِ زمانی، حذفِ هر آدرس، پاک‌کردنِ کل تاریخچه با تأیید، و حذفِ
 *   بازدیدهای قدیمی‌تر از ۷ یا ۳۰ روز.
 *
 * همهٔ ردیف‌ها هدفِ لمسِ کامل دارند و عملیاتِ مخرب پشتِ منوی سه‌نقطه است تا حذفِ تصادفی
 * پیش نیاید (یکی از شکایت‌های «کلیک خراب» همین بود).
 */
@Composable
internal fun LibrarySheet(
    bookmarks: List<BookmarkItem>,
    historyItems: List<HistoryItem>,
    onSelectUrl: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearHistoryOlderThan: (Int) -> Unit,
    onAddBookmark: (title: String, url: String, folder: String, (Boolean) -> Unit) -> Unit,
    onUpdateBookmark: (BookmarkItem, title: String, url: String, folder: String) -> Unit,
    onMoveBookmark: (BookmarkItem, folder: String) -> Unit,
    onDeleteBookmark: (BookmarkItem) -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onSearch: suspend (String) -> LibrarySearchResult,
) {
    val strings = LocalMarbleStrings.current
    var section by rememberSaveable { mutableStateOf(0) }
    var sortOrder by rememberSaveable { mutableStateOf(0) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchResult by remember { mutableStateOf<LibrarySearchResult?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showOlderDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BookmarkItem?>(null) }
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(query, section, bookmarks, historyItems) {
        searchResult = if (query.isBlank()) null else onSearch(query)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MarbleLayout.Screen)
            .testTag("library_sheet"),
        verticalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
    ) {
        Text(
            text = strings.libraryTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        LibrarySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = strings.librarySearchHint,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                label = { Text(strings.libraryBookmarksTab) },
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                label = { Text(strings.libraryHistoryTab) },
            )
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = strings.librarySort,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(strings.librarySortDate) },
                        onClick = { sortOrder = 0; sortMenuOpen = false },
                    )
                    if (section == 1) {
                        DropdownMenuItem(
                            text = { Text(strings.librarySortVisits) },
                            onClick = { sortOrder = 1; sortMenuOpen = false },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(strings.librarySortTitle) },
                        onClick = { sortOrder = 2; sortMenuOpen = false },
                    )
                }
            }
            if (section == 0) {
                TextButton(onClick = { adding = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(strings.libraryAddBookmark)
                }
            } else if (historyItems.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) { Text(strings.clearHistory) }
            }
        }

        if (section == 0) {
            val visibleBookmarks = (searchResult?.bookmarks ?: bookmarks).let { items ->
                if (sortOrder == 2) {
                    items.sortedBy { it.title.lowercase() }
                } else {
                    items.sortedByDescending { it.createdAtEpochMillis }
                }
            }
            if (visibleBookmarks.isEmpty()) {
                LibraryEmpty(
                    if (query.isBlank()) strings.emptyBookmarks else strings.libraryEmptyBookmarksSearch,
                )
            } else {
                val byFolder = visibleBookmarks.groupBy { it.folder.ifBlank { "general" } }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    byFolder.forEach { (folder, folderBookmarks) ->
                        item(key = "folder-$folder") {
                            LibraryFolderHeader(
                                label = if (folder == "general") strings.libraryAllBookmarks else folder,
                                count = folderBookmarks.size,
                            )
                        }
                        items(folderBookmarks, key = { "bm-${it.id}" }) { bookmark ->
                            LibraryRow(
                                title = bookmark.title,
                                url = bookmark.url,
                                subtitle = bookmark.folder,
                                onClick = { onSelectUrl(bookmark.url) },
                                onOpenInNewTab = { onOpenInNewTab(bookmark.url) },
                                onEdit = { editing = bookmark },
                                onDelete = { onDeleteBookmark(bookmark) },
                                onMove = { folderName -> onMoveBookmark(bookmark, folderName) },
                                moveLabel = strings.libraryMoveToFolder,
                                allFoldersLabel = strings.libraryAllBookmarks,
                            )
                        }
                    }
                }
            }
        } else {
            val visibleHistory = (searchResult?.history ?: historyItems).let { items ->
                when (sortOrder) {
                    1 -> items.sortedWith(compareByDescending<HistoryItem> { it.visitCount }.thenByDescending { it.visitedAtEpochMillis })
                    2 -> items.sortedBy { it.title.lowercase() }
                    else -> items.sortedByDescending { it.visitedAtEpochMillis }
                }
            }
            if (visibleHistory.isEmpty()) {
                LibraryEmpty(
                    if (query.isBlank()) strings.emptyHistory else strings.libraryEmptyHistorySearch,
                )
            } else {
                val grouped = remember(visibleHistory, strings) { groupByRecency(visibleHistory, strings) }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    grouped.forEach { (label, historyEntries) ->
                        item(key = "hist-$label") {
                            LibraryFolderHeader(label = label, count = historyEntries.size)
                        }
                        items(historyEntries, key = { "hist-${it.id}" }) { item ->
                            LibraryRow(
                                title = item.title,
                                url = item.url,
                                subtitle = if (item.visitCount > 1) "×${item.visitCount}" else "",
                                onClick = { onSelectUrl(item.url) },
                                onOpenInNewTab = { onOpenInNewTab(item.url) },
                                onEdit = null,
                                onDelete = { onDeleteHistoryItem(item) },
                                onMove = null,
                                moveLabel = strings.libraryMoveToFolder,
                                allFoldersLabel = strings.libraryAllBookmarks,
                            )
                        }
                    }
                    item(key = "hist-clear-old") {
                        TextButton(onClick = { showOlderDialog = true }) {
                            Text(strings.libraryClearOlder)
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        BookmarkEditorDialog(
            title = "",
            url = "",
            folder = "general",
            heading = strings.libraryAddBookmark,
            onDismiss = { adding = false },
            onConfirm = { title, url, folder ->
                adding = false
                onAddBookmark(title, url, folder) {}
            },
        )
    }

    editing?.let { bookmark ->
        BookmarkEditorDialog(
            title = bookmark.title,
            url = bookmark.url,
            folder = bookmark.folder,
            heading = strings.libraryEditBookmark,
            onDismiss = { editing = null },
            onConfirm = { title, url, folder ->
                onUpdateBookmark(bookmark, title, url, folder)
                editing = null
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text(strings.libraryClearHistoryTitle) },
            text = { Text(strings.libraryClearHistoryBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    },
                ) { Text(strings.clearHistory) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(strings.dismiss) }
            },
        )
    }

    if (showOlderDialog) {
        AlertDialog(
            onDismissRequest = { showOlderDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text(strings.libraryClearOlder) },
            text = { Text(strings.libraryClearOlderBody) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showOlderDialog = false
                        onClearHistoryOlderThan(7)
                    }) { Text(strings.libraryClearOlder7) }
                    TextButton(onClick = {
                        showOlderDialog = false
                        onClearHistoryOlderThan(30)
                    }) { Text(strings.libraryClearOlder30) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOlderDialog = false }) { Text(strings.dismiss) }
            },
        )
    }
}

@Composable
private fun LibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(
                style = ChromeBarStyle.FLOATING,
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(18.dp),
        )
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
                textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                cursorBrush = Brush.verticalGradient(listOf(colors.primary, colors.primary)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_field"),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LibraryFolderHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibraryEmpty(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 20.dp),
    )
}

/**
 * یک ردیفِ کتابخانه.
 *
 * خودِ ردیف با یک ضربه آدرس را باز می‌کند؛ عملیات‌ها در منوی سه‌نقطه‌اند (و «انتقال به
 * پوشه» یک دکمهٔ جدا برای نشان‌ها) تا لمسِ اشتباه به حذف یا تغییر منجر نشود.
 */
@Composable
private fun LibraryRow(
    title: String,
    url: String,
    subtitle: String,
    onClick: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    onMove: ((String) -> Unit)?,
    moveLabel: String,
    allFoldersLabel: String,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    var menuOpen by remember { mutableStateOf(false) }
    var moveOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SiteBadgeLogo(displayName = title, domain = siteDomain(url), size = 30.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { siteDomain(url) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = siteDomain(url),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = "· $subtitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        if (onMove != null) {
            Box {
                IconButton(onClick = { moveOpen = true }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = moveLabel,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(expanded = moveOpen, onDismissRequest = { moveOpen = false }) {
                    LIBRARY_FOLDERS.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(if (folder == "general") allFoldersLabel else folder) },
                            onClick = {
                                moveOpen = false
                                onMove(folder)
                            },
                        )
                    }
                }
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = strings.libraryMoreOptions,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(strings.libraryOpenInNewTab) },
                    leadingIcon = {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onOpenInNewTab()
                    },
                )
                if (onEdit != null) {
                    DropdownMenuItem(
                        text = { Text(strings.libraryEditBookmark) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(strings.libraryDeleteBookmark) },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** پوشه‌های آماده؛ کاربر می‌تواند در ویرایشِ نشان هر نامِ دیگری هم بنویسد. */
private val LIBRARY_FOLDERS = listOf("general", "work", "personal", "bank", "shop")

/** دیالوگِ افزودن/ویرایشِ نشان. */
@Composable
private fun BookmarkEditorDialog(
    title: String,
    url: String,
    folder: String,
    heading: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, folder: String) -> Unit,
) {
    val strings = LocalMarbleStrings.current
    var titleValue by remember { mutableStateOf(title) }
    var urlValue by remember { mutableStateOf(url) }
    var folderValue by remember { mutableStateOf(folder) }
    val valid = urlValue.trim().isNotEmpty() &&
        urlValue.trim().removePrefix("https://").removePrefix("http://").contains('.')
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = { Text(heading) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleValue,
                    onValueChange = { titleValue = it },
                    label = { Text(strings.libraryBookmarkTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = urlValue,
                    onValueChange = { urlValue = it },
                    label = { Text(strings.libraryBookmarkUrl) },
                    singleLine = true,
                    isError = urlValue.isNotEmpty() && !valid,
                    supportingText = if (urlValue.isNotEmpty() && !valid) {
                        { Text(strings.libraryInvalidBookmark) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_bookmark_url"),
                )
                OutlinedTextField(
                    value = folderValue,
                    onValueChange = { folderValue = it },
                    label = { Text(strings.libraryBookmarkFolder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(titleValue, urlValue, folderValue) },
                enabled = valid,
            ) { Text(strings.librarySave) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.dismiss) } },
    )
}

/** گروه‌بندیِ تاریخچه بر اساسِ تازگی (امروز/دیروز/این هفته/این ماه/قدیمی‌تر). */
internal fun groupByRecency(
    items: List<HistoryItem>,
    strings: MarbleStrings,
    now: Long = System.currentTimeMillis(),
): List<Pair<String, List<HistoryItem>>> {
    if (items.isEmpty()) return emptyList()
    val day = 86_400_000L
    val buckets = linkedMapOf(
        strings.libraryToday to mutableListOf<HistoryItem>(),
        strings.libraryYesterday to mutableListOf<HistoryItem>(),
        strings.libraryThisWeek to mutableListOf<HistoryItem>(),
        strings.libraryThisMonth to mutableListOf<HistoryItem>(),
        strings.libraryOlder to mutableListOf<HistoryItem>(),
    )
    items.forEach { item ->
        val age = (now - item.visitedAtEpochMillis).coerceAtLeast(0L)
        val bucket = when {
            age < day -> strings.libraryToday
            age < 2 * day -> strings.libraryYesterday
            age < 7 * day -> strings.libraryThisWeek
            age < 30 * day -> strings.libraryThisMonth
            else -> strings.libraryOlder
        }
        buckets[bucket]?.add(item)
    }
    return buckets.filter { it.value.isNotEmpty() }.toList()
}
