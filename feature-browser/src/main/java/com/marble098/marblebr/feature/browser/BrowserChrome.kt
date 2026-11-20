package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.preferences.AddressSelectionMode

@Composable
internal fun Modifier.liquidGlass(): Modifier {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    return shadow(6.dp, shape)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(colors.surfaceContainerHighest, colors.surface, colors.surfaceContainerHigh)))
        .drawWithCache {
            val leftGlow = Brush.radialGradient(
                listOf(colors.primary.copy(alpha = 0.19f), Color.Transparent),
                center = Offset(size.width * 0.16f, 0f),
                radius = size.width * 0.55f,
            )
            val rightGlow = Brush.radialGradient(
                listOf(colors.tertiary.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width, size.height),
                radius = size.width * 0.6f,
            )
            val reflection = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.65f), Color.Transparent))
            onDrawBehind {
                drawRect(leftGlow)
                drawRect(rightGlow)
                drawLine(reflection, Offset(24.dp.toPx(), 1.dp.toPx()), Offset(size.width - 24.dp.toPx(), 1.dp.toPx()))
            }
        }
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), colors.outlineVariant)), shape)
}

internal fun addressSelection(text: String, cursor: Int, mode: AddressSelectionMode): TextRange {
    val caret = cursor.coerceIn(0, text.length)
    return when (mode) {
        AddressSelectionMode.SELECT_ALL -> TextRange(0, text.length)
        AddressSelectionMode.CURSOR -> TextRange(caret)
        AddressSelectionMode.SELECT_PART -> {
            val delimiters = ":/.?&#= "
            val start = text.take(caret).indexOfLast { it in delimiters } + 1
            val end = text.indexOfAny(delimiters.toCharArray(), caret).takeIf { it >= 0 } ?: text.length
            TextRange(start, end)
        }
    }
}
