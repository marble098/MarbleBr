package com.marble098.marblebr.feature.browser

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.liquidGlass as coreLiquidGlass
import com.marble098.marblebr.core.model.ChromeBarStyle

/**
 * نوار تب پایینِ شیشه‌ای و شناور مرورگر.
 *
 * قرارداد بصری (خواستهٔ محصول):
 * - **بدون پس‌زمینه:** بدنهٔ نوار هیچ رنگ یا پنلی ندارد؛ صفحهٔ وب از پشت آن دیده می‌شود.
 * - **شیشه‌ای شناور:** هر دکمه یک کپسول شیشه‌ای مستقل است که روی محتوا شناور است.
 * - **Liquid Glass Premium:** بازتاب نور، هالهٔ رنگی و سایهٔ نرم فقط روی خودِ کپسول‌ها.
 * - **انتخابِ روان:** کپسول گزینهٔ فعال با رنگ اصلی و انیمیشن spring برجسته می‌شود و هر
 *   دکمه هنگام فشردن به‌آرامی جمع می‌شود (بدون ripple سنگین).
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            // هیچ پس‌زمینه‌ای روی بدنه گذاشته نمی‌شود؛ فقط فاصلهٔ درونی برای چیدمان.
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BottomBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = strings.goBack,
                enabled = canGoBack,
                style = style,
                onClick = onBack,
                testTag = "nav_back_btn",
            )
            BottomBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = strings.goForward,
                enabled = canGoForward,
                style = style,
                onClick = onForward,
                testTag = "nav_forward_btn",
            )
            BottomBarAction(
                icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                label = if (isLoading) strings.stopLoading else strings.reload,
                enabled = reloadEnabled,
                style = style,
                onClick = onReloadOrStop,
                testTag = "nav_reload_btn",
            )
            BottomBarAction(
                icon = Icons.Default.Home,
                label = strings.homeTitle,
                selected = isHomeSelected,
                style = style,
                onClick = onHome,
                testTag = "nav_home_btn",
            )
            BottomBarAction(
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

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    style: ChromeBarStyle,
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
            pressed && enabled -> 0.88f
            selected -> 1.04f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 850f),
        label = "bottom bar press",
    )

    val shape = RoundedCornerShape(22.dp)
    // هر دکمه یک کپسول شیشه‌ای مستقل است؛ حتی غیرفعال‌ها یک لایهٔ بسیار نازک دارند تا
    // لبهٔ شیشه دیده شود، اما بدنهٔ نوار هیچ پس‌زمینه‌ای ندارد.
    val containerAlpha = when {
        style == ChromeBarStyle.SOLID -> if (selected) 1f else 0.08f
        selected -> if (dark) 0.30f else 0.34f
        style == ChromeBarStyle.GLASS -> if (dark) 0.14f else 0.18f
        else -> if (dark) 0.07f else 0.10f
    }
    val tint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.32f)
        selected -> colors.primary
        else -> colors.onSurface.copy(alpha = if (dark) 0.92f else 0.86f)
    }

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (containerAlpha > 0f) {
                    // هر کپسول فقط یک رینگ نورانی دارد؛ حاشیهٔ دوباره خاموش است تا شیشه‌ها
                    // به‌هم نچسبند و روی هم سایهٔ اضافه نسازند.
                    Modifier.coreLiquidGlass(
                        style = if (style == ChromeBarStyle.SOLID) ChromeBarStyle.GLASS else style,
                        shape = shape,
                        fillAlphaOverride = containerAlpha,
                        glow = selected,
                        elevation = if (selected) 6.dp else 0.dp,
                        bordered = false,
                    )
                } else {
                    Modifier
                },
            )
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
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .width(18.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.primary),
            )
        }
        BadgedBox(
            badge = {
                if (badge != null && badge > 0) {
                    Badge(containerColor = colors.primary, contentColor = colors.onPrimary) { Text(badge.toString()) }
                }
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
