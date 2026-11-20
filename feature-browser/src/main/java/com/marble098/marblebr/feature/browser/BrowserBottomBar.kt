package com.marble098.marblebr.feature.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.model.ChromeBarStyle

/**
 * نوار پایینیِ شناورِ مرورگر: کنترل‌های ناوبری و تب‌ها در یک کپسولِ شیشه‌ای.
 *
 * قبلاً پایین صفحه یک NavigationBar با پنج برچسبِ متنی بود که بخش بزرگی از فضا را می‌گرفت و
 * دکمه‌های بازگشت/پیش/بارگذاری در نوار بالا تکرار می‌شدند. حالا:
 * - ناوبری صفحه (عقب، جلو، بارگذاری یا توقف) و تب‌ها/خانه در یک جا جمع شده‌اند.
 * - وضعیتِ فعال (خانه) با رنگ کانتینر نشان داده می‌شود و شمارندهٔ تب‌ها روی آیکن دیده می‌شود.
 * - امکانات کم‌استفاده (وُلت، نشانک‌ها، تنظیمات) از میانبرهای سریع و منوی بالا در دسترس‌اند.
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .floatingGlassPill(style, radius = 32)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = strings.goBack,
                enabled = canGoBack,
                onClick = onBack,
            )
            BottomBarAction(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = strings.goForward,
                enabled = canGoForward,
                onClick = onForward,
            )
            BottomBarAction(
                icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                label = if (isLoading) strings.stopLoading else strings.reload,
                enabled = reloadEnabled,
                onClick = onReloadOrStop,
            )
            BottomBarAction(
                icon = Icons.Default.Home,
                label = strings.homeTitle,
                selected = isHomeSelected,
                onClick = onHome,
                testTag = "nav_home_btn",
            )
            BottomBarAction(
                icon = Icons.Default.Tab,
                label = strings.tabsLabel,
                badge = tabCount,
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
    enabled: Boolean = true,
    selected: Boolean = false,
    badge: Int? = null,
    testTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        selected -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    val container = if (selected) colors.primaryContainer else Color.Transparent
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (badge != null && badge > 0) {
                    Badge(containerColor = colors.primary, contentColor = colors.onPrimary) { Text(badge.toString()) }
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}
