package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.engine.PageContextTarget
import java.net.URI

private fun isSafeWebLink(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
}

/**
 * پیوند و تصویر روی صفحه را نشان می‌دهد؛ با همان منو، وقتی روی هر بخشی از صفحه
 * نگه داشتید، گزینه‌های مختلف (کپی متن، به اشتراک‌گذاشتن صفحه، نشان کردن در نشان‌ها و…).
 */
@Composable
internal fun PageContextMenu(
    target: PageContextTarget,
    canDownload: Boolean,
    onOpen: (String) -> Unit,
    onOpenPrivate: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownload: (String) -> Unit,
    onSavePdf: () -> Unit,
    onSharePage: () -> Unit,
    onBookmarkPage: () -> Unit,
    onCopyText: () -> Unit,
    onSearchText: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text(strings.contextTitle, style = MaterialTheme.typography.titleLarge)
        target.linkUrl?.let { link ->
            Text(link, maxLines = 2, style = MaterialTheme.typography.bodySmall)
            if (isSafeWebLink(link)) {
                TextButton(onClick = { onOpen(link) }) { Text(strings.contextOpenTab) }
                TextButton(onClick = { onOpenPrivate(link) }) { Text(strings.contextOpenPrivateTab) }
                TextButton(onClick = { onOpenExternal(link) }) { Text(strings.contextOpenExternal) }
            }
            TextButton(onClick = { onCopy(link) }) { Text(strings.contextCopyLink) }
            TextButton(onClick = { onShare(link) }) { Text(strings.contextShareLink) }
        }
        target.imageUrl?.let { image ->
            TextButton(onClick = { onCopy(image) }) { Text(strings.contextCopyImage) }
            if (canDownload) TextButton(onClick = { onDownload(image) }) { Text(strings.contextSaveImage) }
        }
        if (!target.text.isNullOrBlank()) {
            TextButton(onClick = onCopyText) { Text(strings.contextCopyText) }
            TextButton(onClick = onSearchText) { Text(strings.contextSearchSelection) }
        }
        TextButton(onClick = onSharePage) { Text(strings.menuSharePage) }
        TextButton(onClick = onBookmarkPage) { Text(strings.bookmarkToggle) }
        TextButton(onClick = onSavePdf) { Text(strings.contextSavePdf) }
    }
}
