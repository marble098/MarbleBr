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
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.preferences.AddressSelectionMode

/**
 * حالت شیشه‌ای مات (رفتار پیش‌فرض پیشین) با هالهٔ رنگی و خط بازتاب نور.
 */
@Composable
internal fun Modifier.liquidGlass(): Modifier = liquidGlass(ChromeBarStyle.GLASS)

/**
 * ظاهر نوارهای بالا و پایین مرورگر بر اساس انتخاب کاربر در تنظیمات.
 *
 * - [ChromeBarStyle.SOLID]: یکدست و مات، بدون شفافیت.
 * - [ChromeBarStyle.GLASS]: شیشه‌ای مات با هالهٔ رنگی و بازتاب نور.
 * - [ChromeBarStyle.FLOATING]: کاملاً شفاف و شناور؛ هیچ پس‌زمینه‌ای ندارد و صفحهٔ وب
 *   از پشت نوار دیده می‌شود.
 */
@Composable
internal fun Modifier.liquidGlass(style: ChromeBarStyle): Modifier {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    return when (style) {
        ChromeBarStyle.SOLID -> clip(shape).background(colors.surface)
        ChromeBarStyle.GLASS -> glassSurface(shape)
        ChromeBarStyle.FLOATING -> clip(shape)
    }
}

/**
 * «حباب» شیشه‌ای بسیار نازک برای اجزای داخلی نوار شناور (نوار آدرس، دکمه‌های میانبر).
 *
 * حتی در حالت کاملاً شناور، خودِ کنترل‌ها یک لایهٔ نازک و بسیار شفاف دارند تا متن
 * روی هر سایتی خوانا بماند.
 */
@Composable
internal fun Modifier.floatingGlassPill(style: ChromeBarStyle, radius: Int = 22): Modifier {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(radius.dp)
    val alpha = when (style) {
        ChromeBarStyle.SOLID -> 1f
        ChromeBarStyle.GLASS -> 0.85f
        ChromeBarStyle.FLOATING -> 0.32f
    }
    return clip(shape)
        .background(colors.surface.copy(alpha = alpha))
        .border(1.dp, colors.outlineVariant.copy(alpha = 0.35f), shape)
}

@Composable
private fun Modifier.glassSurface(shape: RoundedCornerShape): Modifier {
    val colors = MaterialTheme.colorScheme
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
