package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import java.util.Locale

/**
 * پنلِ امتیازدهیِ اولین ورود (Rate App).
 * متن‌ها بر اساس زبانِ گوشی (نه زبانِ انتخاب‌شده در تنظیمات) نمایش داده می‌شوند.
 */
@Composable
fun RateAppDialog(
    strings: MarbleStrings = MarbleStrings.forLanguage(
        AppLanguage.resolve(Locale.getDefault().toLanguageTag()),
    ),
    onRateNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(text = strings.rateTitle) },
        text = { Text(text = strings.rateMessage) },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onNever) {
                    Text(text = strings.rateDontAskAgain)
                }
                TextButton(onClick = onLater) {
                    Text(text = strings.rateLater)
                }
                Button(onClick = onRateNow) {
                    Text(text = strings.rateNow)
                }
            }
        },
        dismissButton = null,
    )
}
