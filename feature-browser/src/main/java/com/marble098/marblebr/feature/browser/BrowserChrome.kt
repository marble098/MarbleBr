package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.liquidGlass as coreLiquidGlass
import com.marble098.marblebr.core.designsystem.liquidGlassPill as coreLiquidGlassPill
import com.marble098.marblebr.core.model.ChromeBarStyle
import com.marble098.marblebr.core.preferences.AddressSelectionMode

/**
 * ظاهر نوارهای بالا و پایین مرورگر بر اساس انتخاب کاربر در تنظیمات.
 *
 * همهٔ حالت‌ها از موتور مشترک «شیشهٔ مایع پریمیوم» در ماژول `core` استفاده می‌کنند تا
 * نوارهای مرورگر، تنظیمات و «مراقبت از حساب‌ها» یک زبان بصری داشته باشند.
 *
 * - [ChromeBarStyle.SOLID]: یکدست و مات، بدون شفافیت.
 * - [ChromeBarStyle.GLASS]: شیشهٔ ماتِ پریمیوم با هالهٔ رنگی و بازتاب نور.
 * - [ChromeBarStyle.FLOATING]: کاملاً شفاف و شناور؛ بدنهٔ نوار **هیچ** پس‌زمینه‌ای ندارد و
 *   صفحهٔ وب از پشت آن دیده می‌شود. فقط دکمه‌ها و نوار آدرس لایهٔ نازکِ خوانایی دارند.
 */
@Composable
internal fun Modifier.liquidGlass(style: ChromeBarStyle): Modifier =
    liquidGlass(style = style, shape = RoundedCornerShape(30.dp))

/** نسخهٔ کامل با شکل و ارتفاع دلخواه (برای نوار بالا، کارت‌ها و شیت‌ها). */
@Composable
internal fun Modifier.liquidGlass(
    style: ChromeBarStyle,
    shape: androidx.compose.ui.graphics.Shape,
    elevation: androidx.compose.ui.unit.Dp = 8.dp,
): Modifier {
    return if (style == ChromeBarStyle.FLOATING) {
        // شیشهٔ شناور: بدون بدنه و بدون سایه؛ فقط یک رینگ نورانیِ بسیار نازک برای تعریف لبه.
        clip(shape)
    } else {
        Modifier.coreLiquidGlass(style = style, shape = shape, elevation = elevation)
    }
}

/**
 * کپسول شیشه‌ایِ نازک برای اجزای داخلی نوار (نوار آدرس، دکمه‌های میانبر).
 *
 * حتی در حالت کاملاً شناور، خودِ کنترل‌ها یک لایهٔ نازک و بسیار شفاف دارند تا متن
 * روی هر سایتی خوانا بماند.
 */
@Composable
internal fun Modifier.floatingGlassPill(style: ChromeBarStyle, radius: Int = 22, emphasized: Boolean = false): Modifier =
    coreLiquidGlassPill(style = style, shape = RoundedCornerShape(radius.dp), emphasized = emphasized)

/** رنگ شیشهٔ نوار آدرس؛ در حالت شناور کمی روشن‌تر تا متنِ آدرس همیشه خوانا باشد. */
@Composable
internal fun addressFieldContainer(style: ChromeBarStyle, focused: Boolean): androidx.compose.ui.graphics.Color {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    return when {
        focused -> colors.surfaceContainerHighest.copy(alpha = if (dark) 0.96f else 0.99f)
        style == ChromeBarStyle.SOLID -> colors.surfaceContainerHigh
        else -> colors.surface.copy(alpha = if (dark) 0.55f else 0.72f)
    }
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
