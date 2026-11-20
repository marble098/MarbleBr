package com.marble098.marblebr.feature.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.liquidGlass as coreLiquidGlass
import com.marble098.marblebr.core.model.ChromeBarStyle

/**
 * نوار تب پایینِ MarbleBr — نسخهٔ بازطراحی‌شده.
 *
 * ### چه چیزی خراب بود؟
 * ۱. **لبهٔ سفیدِ بالای دکمه‌ها:** هر دکمه یک کپسولِ شیشه‌ای جدا با `bordered = false` و
 *    گرادیانِ روشنِ داخلی بود؛ گرادیانِ `Color.White` در بالای کپسول روی پس‌زمینهٔ تیره
 *    «هالهٔ سفید در لبهٔ بالا» می‌ساخت و دکمه‌ها وصله‌وصله و ارزان دیده می‌شدند.
 * ۲. **پراکندگی:** پنج کپسولِ مستقل با رنگ‌های متفاوت، بدون یک بدنهٔ واحد، حس «نوار» نمی‌داد.
 *
 * ### طراحی تازه
 * - **یک بدنهٔ شیشه‌ایِ واحد** با شکلِ کپسولی و سایهٔ ملایم که کل نوار را به یک شیء واحد تبدیل
 *   می‌کند (به‌جای پنج حبابِ شناور).
 * - **بدون هالهٔ سفید:** بازتابِ نور فقط یک خطِ نازک در لبهٔ بالای همان بدنه است و کپسول‌های
 *   داخلی هیچ گرادیانِ سفیدی ندارند — پس دیگر هیچ لبهٔ سفیدی روی دکمه‌ها دیده نمی‌شود.
 * - **حالتِ فعالِ خوانا:** گزینهٔ انتخاب‌شده یک «قرص» رنگیِ توپُر (پرایمری) با آیکنِ سفید و
 *   برچسبِ کوچک می‌گیرد؛ بقیه فقط آیکنِ خطی و برچسبِ محو دارند.
 * - **شبکهٔ هم‌فاصله:** هر پنج دکمه وزن برابر دارند و برچسبِ متنی زیر آیکن می‌آید تا نوار
 *   «طراحی‌شده» و قابل‌فهم باشد، نه پنج آیکنِ سرگردان.
 */
@Composable
internal fun BrowserBottomBar(
    style: ChromeBarStyle,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    reloadEnabled: Boolean,
    isHomeSelected: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val pillShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                // بدنهٔ یکپارچه: در حالت شناور هیچ پس‌زمینه‌ای ندارد و فقط یک خطِ نورِ نازک
                // لبهٔ بالا را تعریف می‌کند؛ در دو حالت دیگر شیشهٔ واقعی با سایهٔ نرم می‌آید.
                .then(
                    if (style == ChromeBarStyle.FLOATING) {
                        Modifier
                            .clip(pillShape)
                            .drawBehind { drawTopHairline(colors.primary) }
                    } else {
                        Modifier.coreLiquidGlass(
                            style = style,
                            shape = pillShape,
                            fillAlphaOverride = if (dark) 0.55f else 0.66f,
                            glow = true,
                            elevation = 10.dp,
                            bordered = true,
                        )
                    },
                )
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BottomBarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = strings.goBack,
                enabled = canGoBack,
                style = style,
                onClick = onBack,
                testTag = "nav_back_btn",
            )
            BottomBarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = strings.goForward,
                enabled = canGoForward,
                style = style,
                onClick = onForward,
                testTag = "nav_forward_btn",
            )
            BottomBarAction(
                modifier = Modifier.weight(1f),
                icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                label = if (isLoading) strings.stopLoading else strings.reload,
                enabled = reloadEnabled,
                style = style,
                onClick = onReloadOrStop,
                testTag = "nav_reload_btn",
            )
            BottomBarAction(
                modifier = Modifier.weight(1f),
                icon = if (isHomeSelected) Icons.Default.Home else Icons.Outlined.Home,
                label = strings.homeTitle,
                selected = isHomeSelected,
                style = style,
                onClick = onHome,
                testTag = "nav_home_btn",
            )
            BottomBarAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Tab,
                label = strings.tabsLabel,
                badge = tabCount,
                style = style,
                onClick = onTabs,
                testTag = "nav_tabs_btn",
            )
        }
    }
}

/** خطِ نورِ نازکِ لبهٔ بالا؛ فقط یک بازتابِ ظریف، بدون هالهٔ سفیدِ ضخیمِ قبلی. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopHairline(accent: Color) {
    val y = 0.75.dp.toPx()
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.Transparent),
        ),
        start = Offset(size.width * 0.18f, y),
        end = Offset(size.width * 0.82f, y),
        strokeWidth = 1.dp.toPx(),
    )
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    style: ChromeBarStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    badge: Int? = null,
    testTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed && enabled -> 0.90f
            selected -> 1.02f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "bottom bar press",
    )
    // قرصِ حالت فعال: در همهٔ سبک‌ها یک رنگِ توپُرِ خوانا (بدون گرادیانِ سفید).
    val activeFill = if (style == ChromeBarStyle.SOLID) {
        colors.primary
    } else {
        colors.primary.copy(alpha = if (dark) 0.92f else 1f)
    }
    val iconTint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.30f)
        selected -> colors.onPrimary
        else -> colors.onSurface.copy(alpha = if (dark) 0.86f else 0.72f)
    }
    val labelTint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.26f)
        selected -> colors.onPrimary
        else -> colors.onSurfaceVariant.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // قرصِ حالت فعال پیش از محتوا کشیده می‌شود تا آیکن و برچسب همیشه روی آن دیده شوند
            // (قبلاً شکلِ قرص بعد از محتوا اعمال می‌شد و متن را می‌پوشاند).
            .then(if (selected) Modifier.activePillShape(activeFill) else Modifier.clip(RoundedCornerShape(20.dp)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BadgedBox(
                badge = {
                    if (badge != null && badge > 0) {
                        Badge(
                            containerColor = if (selected) colors.onPrimary else colors.primary,
                            contentColor = if (selected) colors.primary else colors.onPrimary,
                        ) {
                            Text(if (badge > 99) "99+" else badge.toString(), style = MaterialTheme.typography.labelLarge.copy(fontSize = 9.sp))
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            // برچسب فقط برای گزینهٔ فعال نمایش داده می‌شود تا نوار در حالت عادی جمع‌وجور و
            // تمیز بماند، اما کاربر بداند کجاست (الگوی Material 3 برای نوار فشرده).
            if (selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = labelTint,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * شکلِ قرصِ حالت فعال؛ **پیش از** محتوا کشیده می‌شود.
 *
 * نکتهٔ باگ قبلی: با `background` (که بعد از محتوا اعمال می‌شود، چون ترتیبِ modifier ها را
 * رعایت نکرده بودیم) قرص روی آیکن و برچسب می‌افتاد. حالا با `drawBehind` رنگ و بازتابِ
 * نرم زیر محتوا رسم می‌شود و هیچ لبهٔ سفیدی هم در بالای دکمه ساخته نمی‌شود.
 */
private fun Modifier.activePillShape(fill: Color): Modifier =
    this
        .clip(RoundedCornerShape(20.dp))
        .drawBehind {
            drawRoundRect(color = fill, cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()))
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.7f,
                ),
                size = Size(size.width, size.height * 0.7f),
            )
        }
