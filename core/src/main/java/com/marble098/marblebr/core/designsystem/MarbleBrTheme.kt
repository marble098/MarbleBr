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
    primary = Color(0xFF4338CA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F766E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF1A1B20),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F5FA),
    surfaceContainer = Color(0xFFEFF1F8),
    surfaceContainerHigh = Color(0xFFE9EBF4),
    surfaceContainerHighest = Color(0xFFE3E6F0),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C7D0),
    inverseSurface = Color(0xFF2E3038),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFB4C4FF),
    scrim = Color(0xFF000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF0B2678),
    primaryContainer = Color(0xFF24388F),
    onPrimaryContainer = Color(0xFFDCE1FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF115E56),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFD6BCFF),
    onTertiary = Color(0xFF3B0D82),
    tertiaryContainer = Color(0xFF52299B),
    onTertiaryContainer = Color(0xFFEDE0FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101218),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF101218),
    onSurface = Color(0xFFE3E2E9),
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF16181F),
    surfaceContainer = Color(0xFF1B1D25),
    surfaceContainerHigh = Color(0xFF25272F),
    surfaceContainerHighest = Color(0xFF30323A),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F909A),
    outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF2E3038),
    inversePrimary = Color(0xFF2A46B8),
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
