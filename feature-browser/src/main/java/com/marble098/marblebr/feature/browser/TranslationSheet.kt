package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.engine.BrowserTranslationLanguage
import com.marble098.marblebr.engine.BrowserTranslationLanguages
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslationSheet(
    languages: BrowserTranslationLanguages?,
    isLoading: Boolean,
    detectedLanguage: String?,
    preferredLanguage: String,
    isTranslated: Boolean,
    onTranslate: (fromLanguage: String, toLanguage: String, onComplete: (Boolean) -> Unit) -> Unit,
    onRestoreOriginal: ((Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val availableFrom = languages?.fromLanguages.orEmpty()
    val availableTo = languages?.toLanguages.orEmpty()
    val initialFrom = remember(languages, detectedLanguage) {
        availableFrom.firstOrNull { it.code.equals(detectedLanguage, ignoreCase = true) }
            ?: availableFrom.firstOrNull()
    }
    val initialTo = remember(languages, preferredLanguage) {
        val preferredBase = Locale.forLanguageTag(preferredLanguage).language
        availableTo.firstOrNull { it.code.equals(preferredLanguage, ignoreCase = true) }
            ?: availableTo.firstOrNull { Locale.forLanguageTag(it.code).language.equals(preferredBase, ignoreCase = true) }
            ?: availableTo.firstOrNull()
    }
    var fromLanguage by remember(languages, detectedLanguage) { mutableStateOf(initialFrom) }
    var toLanguage by remember(languages, preferredLanguage) { mutableStateOf(initialTo) }
    val effectiveToLanguage = toLanguage?.takeIf { it.code != fromLanguage?.code }
        ?: availableTo.firstOrNull { it.code != fromLanguage?.code }
        ?: availableTo.firstOrNull()
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MarbleLayout.Screen)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings.translatePage,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = strings.translateDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            if (isLoading || busy) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text(strings.translateLoading, color = colors.onSurfaceVariant)
                }
            } else if (availableFrom.isEmpty() || availableTo.isEmpty()) {
                Text(
                    text = strings.translateUnavailable,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LanguagePicker(
                    label = strings.translateFrom,
                    languages = availableFrom,
                    selected = fromLanguage ?: availableFrom.first(),
                    onSelect = {
                        fromLanguage = it
                        if (toLanguage?.code == it.code) {
                            toLanguage = availableTo.firstOrNull { option -> option.code != it.code }
                        }
                        failed = false
                    },
                )
                LanguagePicker(
                    label = strings.translateTo,
                    languages = availableTo.filterNot { it.code == fromLanguage?.code }.ifEmpty { availableTo },
                    selected = effectiveToLanguage ?: availableTo.first(),
                    onSelect = { toLanguage = it; failed = false },
                )
                if (failed) {
                    Text(strings.translateFailed, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val from = fromLanguage ?: return@Button
                        val to = effectiveToLanguage ?: return@Button
                        if (from.code == to.code) {
                            failed = true
                            return@Button
                        }
                        busy = true
                        failed = false
                        onTranslate(from.code, to.code) { success ->
                            busy = false
                            if (success) onDismiss() else failed = true
                        }
                    },
                    enabled = !busy && fromLanguage != null && effectiveToLanguage != null &&
                        fromLanguage?.code != effectiveToLanguage?.code,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.translateStart)
                }
            }

            if (isTranslated && !busy) {
                TextButton(
                    onClick = {
                        busy = true
                        failed = false
                        onRestoreOriginal { success ->
                            busy = false
                            if (success) onDismiss() else failed = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(strings.translateRestore)
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker(
    label: String,
    languages: List<BrowserTranslationLanguage>,
    selected: BrowserTranslationLanguage,
    onSelect: (BrowserTranslationLanguage) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    var expanded by remember(languages) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        androidx.compose.foundation.layout.Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.surfaceContainerHighest.copy(alpha = 0.42f))
                    .border(0.8.dp, colors.outlineVariant.copy(alpha = 0.52f), shape)
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selected.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("▾", color = colors.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = colors.surfaceContainerHigh,
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(language); expanded = false },
                    )
                }
            }
        }
    }
}
