package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageZoomSheet(
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MarbleLayout.Screen)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.pageZoomTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = strings.pageZoomDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = { onZoomChange((zoomPercent - 10).coerceAtLeast(80)) },
                    enabled = zoomPercent > 80,
                    shape = RoundedCornerShape(16.dp),
                ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.size(20.dp))
                Text(
                    text = "$zoomPercent%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(20.dp))
                FilledTonalButton(
                    onClick = { onZoomChange((zoomPercent + 10).coerceAtMost(160)) },
                    enabled = zoomPercent < 160,
                    shape = RoundedCornerShape(16.dp),
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            TextButton(onClick = { onZoomChange(100) }, enabled = zoomPercent != 100) {
                Text(strings.pageZoomReset)
            }
        }
    }
}
