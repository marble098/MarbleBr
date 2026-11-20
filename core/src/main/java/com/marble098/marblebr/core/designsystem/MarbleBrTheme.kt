package com.marble098.marblebr.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.ColorPalette
import com.marble098.marblebr.core.model.ThemeMode

/** متن‌های زبانِ فعال؛ در همه‌ی صفحه‌ها از همین استفاده می‌شود. */
val LocalMarbleStrings = staticCompositionLocalOf { MarbleStrings.forLanguage(AppLanguage.ENGLISH) }

/** زبانِ حل‌شده (بعد از اعمال حالت «خودکار»). */
val LocalMarbleLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

val MarbleExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

val MarbleExpressiveTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 25.sp,
        lineHeight = 33.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

/**
 * تم اصلی MarbleBr.
 *
 * - `themeMode` روشن/تاریک/هماهنگ با گوشی را تعیین می‌کند.
 * - `palette` پالت رنگیِ انتخاب‌شده در تنظیمات است. پیش‌فرض [ColorPalette.DYNAMIC] است تا
 *   رفتار قبلی (هماهنگی با رنگ گوشی) برای همه حفظ شود. حالت [ColorPalette.DYNAMIC] روی
 *   اندروید ۱۲+ رنگ‌ها را از تصویر پس‌زمینهٔ گوشی می‌گیرد (Material You) و بقیهٔ پالت‌ها
 *   از موتور پالت‌های حرفه‌ای ([paletteColorScheme]) ساخته می‌شوند.
 * - `dynamicColor` سازگاریِ عقب‌رو است؛ اگر پالت «رنگ‌های گوشی» باشد همان Material You
 *   اعمال می‌شود، وگرنه نادیده گرفته می‌شود.
 * - `language` هم متن‌ها و هم جهت چیدمان (فارسی = راست‌به‌چپ) را تعیین می‌کند.
 */
@Composable
fun MarbleBrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    language: AppLanguage = AppLanguage.SYSTEM,
    palette: ColorPalette = ColorPalette.DYNAMIC,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val resolvedLanguage = language.resolve()
    val strings = remember(resolvedLanguage) { MarbleStrings.forResolvedLanguage(resolvedLanguage) }

    val context = LocalContext.current
    // «رنگ‌های گوشی» فقط روی اندروید ۱۲+ معنا دارد؛ روی نسخه‌های قدیمی‌تر به پالت پیش‌فرض
    // برمی‌گردیم تا کاربر هرگز یک تمِ خالی یا سیاه‌وسفید نبیند.
    val useDynamicColor = palette == ColorPalette.DYNAMIC &&
        dynamicColor &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = remember(palette, darkTheme, useDynamicColor) {
        when {
            useDynamicColor -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            palette == ColorPalette.DYNAMIC -> paletteColorScheme(FALLBACK_PALETTE, darkTheme)
            darkTheme -> paletteColorScheme(palette, dark = true)
            else -> paletteColorScheme(palette, dark = false)
        }
    }

    val direction = if (resolvedLanguage == AppLanguage.PERSIAN) LayoutDirection.Rtl else LayoutDirection.Ltr

    ApplySystemBarAppearance(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalMarbleStrings provides strings,
        LocalMarbleLanguage provides resolvedLanguage,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MarbleExpressiveTypography,
            shapes = MarbleExpressiveShapes,
            content = content,
        )
    }
}

/**
 * رنگ آیکن‌های نوار وضعیت و نوار ناوبری را با تمِ برنامه هماهنگ می‌کند تا متن‌ها زیر نوار وضعیت گم نشوند.
 */
@Composable
private fun ApplySystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
