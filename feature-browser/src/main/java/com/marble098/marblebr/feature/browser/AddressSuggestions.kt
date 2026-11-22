package com.marble098.marblebr.feature.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.liquidGlass
import com.marble098.marblebr.core.model.AddressSuggestion
import com.marble098.marblebr.core.model.AddressSuggestionSource
import com.marble098.marblebr.core.model.ChromeBarStyle

/**
 * فهرستِ پیشنهادهای زندهٔ زیرِ نوار آدرس.
 *
 * طراحی:
 * - هر ردیف یک «هدفِ لمسِ» کامل دارد (حداقل ۴۸dp) و با یک ضربه همان آدرس را باز می‌کند؛
 *   پیش‌تر کاربر باید کل آدرس را تایپ می‌کرد.
 * - منبعِ هر پیشنهاد با آیکون و برچسبِ کوتاه مشخص است تا کاربر بداند چرا این مورد آمده
 *   (تاریخچه / نشان / پربازدید / تبِ باز).
 * - پدیدارشدن و رفتن با `expandVertically`+`fadeIn` نرم است، نه یک پرشِ ناگهانی؛
 *   همین موضوع یکی از شکایت‌های اصلیِ «انیمیشن‌های زشت» بود.
 */
@Composable
internal fun AddressSuggestionList(
    suggestions: List<AddressSuggestion>,
    onPick: (AddressSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = expandVertically(animationSpec = tween(180)) + fadeIn(animationSpec = tween(150)),
        exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(110)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .liquidGlass(
                    style = ChromeBarStyle.FLOATING,
                    shape = RoundedCornerShape(18.dp),
                    elevation = 6.dp,
                )
                .padding(vertical = 6.dp),
        ) {
            suggestions.forEach { suggestion ->
                AddressSuggestionRow(
                    suggestion = suggestion,
                    label = suggestion.sourceLabel(strings),
                    onClick = { onPick(suggestion) },
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun AddressSuggestionRow(
    suggestion: AddressSuggestion,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) colors.primary.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "${suggestion.title} ${suggestion.url}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = suggestion.source.icon(),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = suggestion.title.ifBlank { suggestion.url },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = suggestion.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "· $label",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                )
                if (suggestion.visitCount > 1) {
                    Text(
                        text = "· ×${suggestion.visitCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun AddressSuggestion.sourceLabel(strings: com.marble098.marblebr.core.i18n.MarbleStrings): String =
    when (source) {
        AddressSuggestionSource.HISTORY -> strings.suggestionHistory
        AddressSuggestionSource.BOOKMARK -> strings.suggestionBookmark
        AddressSuggestionSource.MOST_VISITED -> strings.suggestionMostVisited
        AddressSuggestionSource.OPEN_TAB -> strings.suggestionOpenTabs
    }

private fun AddressSuggestionSource.icon(): ImageVector = when (this) {
    AddressSuggestionSource.HISTORY -> Icons.Default.History
    AddressSuggestionSource.BOOKMARK -> Icons.Default.Star
    AddressSuggestionSource.MOST_VISITED -> Icons.Default.TrendingUp
    AddressSuggestionSource.OPEN_TAB -> Icons.Default.Tab
}
