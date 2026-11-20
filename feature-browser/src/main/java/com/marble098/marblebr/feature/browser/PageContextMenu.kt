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

@Composable
internal fun PageContextMenu(
    target: PageContextTarget,
    canDownload: Boolean,
    onOpen: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownload: (String) -> Unit,
    onSavePdf: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text(strings.contextTitle, style = MaterialTheme.typography.titleLarge)
        target.linkUrl?.let { link ->
            Text(link, maxLines = 2, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onOpen(link) }) { Text(strings.contextOpenTab) }
            TextButton(onClick = { onCopy(link) }) { Text(strings.contextCopyLink) }
            TextButton(onClick = { onShare(link) }) { Text(strings.contextShareLink) }
        }
        target.imageUrl?.let { image ->
            TextButton(onClick = { onCopy(image) }) { Text(strings.contextCopyImage) }
            if (canDownload) TextButton(onClick = { onDownload(image) }) { Text(strings.contextSaveImage) }
        }
        TextButton(onClick = onSavePdf) { Text(strings.contextSavePdf) }
    }
}
