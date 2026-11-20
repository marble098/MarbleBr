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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
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
import com.marble098.marblebr.core.model.ThemeMode

/** متن‌های زبانِ فعال؛ در همه‌ی صفحه‌ها از همین استفاده می‌شود. */
val LocalMarbleStrings = staticCompositionLocalOf { MarbleStrings.forLanguage(AppLanguage.ENGLISH) }

/** زبانِ حل‌شده (بعد از اعمال حالت «خودکار»). */
val LocalMarbleLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBFDBFE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    error = Color(0xFFEF4444),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F7),
    surfaceContainer = Color(0xFFEEF0F4),
    surfaceContainerHigh = Color(0xFFE7E9EF),
    surfaceContainerHighest = Color(0xFFDFE1E7),
    onSurfaceVariant = Color(0xFF474B55),
    outline = Color(0xFF8E93A0),
    outlineVariant = Color(0xFFC8CCD6),
    inverseSurface = Color(0xFF2E3038),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFF93B5FF),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF93B5FF),
    onPrimary = Color(0xFF0B2560),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF67E8F9),
    onSecondary = Color(0xFF043343),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF4C1D95),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF0B0C10),
    onSurface = Color(0xFFE3E2E9),
    surfaceContainerLowest = Color(0xFF090A0E),
    surfaceContainerLow = Color(0xFF121318),
    surfaceContainer = Color(0xFF181A20),
    surfaceContainerHigh = Color(0xFF22242C),
    surfaceContainerHighest = Color(0xFF2E3038),
    onSurfaceVariant = Color(0xFFA5A9B3),
    outline = Color(0xFF444852),
    outlineVariant = Color(0xFF2A2D36),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF1A1C24),
    inversePrimary = Color(0xFF3B82F6),
    scrim = Color(0xFF000000),
)

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
 * - `dynamicColor` در اندروید ۱۲+ رنگ‌ها را از تصویر پس‌زمینه‌ی گوشی می‌گیرد (Material You).
 * - `language` هم متن‌ها و هم جهت چیدمان (فارسی = راست‌به‌چپ) را تعیین می‌کند.
 */
@Composable
fun MarbleBrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    language: AppLanguage = AppLanguage.SYSTEM,
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
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
