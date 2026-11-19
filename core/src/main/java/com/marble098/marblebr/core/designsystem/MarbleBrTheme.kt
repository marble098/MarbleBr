package com.marble098.marblebr.core.designsystem

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.model.ThemeMode

private val MarblePrimaryLight = Color(0xFF2855B8)
private val MarbleOnPrimaryLight = Color(0xFFFFFFFF)
private val MarblePrimaryContainerLight = Color(0xFFD9E2FF)
private val MarbleOnPrimaryContainerLight = Color(0xFF001945)
private val MarbleSecondaryLight = Color(0xFF006A60)
private val MarbleOnSecondaryLight = Color(0xFFFFFFFF)
private val MarbleSecondaryContainerLight = Color(0xFF74F8E5)
private val MarbleOnSecondaryContainerLight = Color(0xFF00201C)
private val MarbleTertiaryLight = Color(0xFF6F43C0)
private val MarbleTertiaryContainerLight = Color(0xFFEBDDFF)
private val MarbleBackgroundLight = Color(0xFFF8F9FF)
private val MarbleSurfaceLight = Color(0xFFF8F9FF)

private val MarblePrimaryDark = Color(0xFFB0C6FF)
private val MarbleOnPrimaryDark = Color(0xFF002C6F)
private val MarblePrimaryContainerDark = Color(0xFF063E9E)
private val MarbleOnPrimaryContainerDark = Color(0xFFD9E2FF)
private val MarbleSecondaryDark = Color(0xFF53DBC9)
private val MarbleOnSecondaryDark = Color(0xFF003731)
private val MarbleSecondaryContainerDark = Color(0xFF005048)
private val MarbleOnSecondaryContainerDark = Color(0xFF74F8E5)
private val MarbleTertiaryDark = Color(0xFFD3BBFF)
private val MarbleTertiaryContainerDark = Color(0xFF5629A6)
private val MarbleBackgroundDark = Color(0xFF0E131F)
private val MarbleSurfaceDark = Color(0xFF131926)

private val LightColorScheme = lightColorScheme(
    primary = MarblePrimaryLight,
    onPrimary = MarbleOnPrimaryLight,
    primaryContainer = MarblePrimaryContainerLight,
    onPrimaryContainer = MarbleOnPrimaryContainerLight,
    secondary = MarbleSecondaryLight,
    onSecondary = MarbleOnSecondaryLight,
    secondaryContainer = MarbleSecondaryContainerLight,
    onSecondaryContainer = MarbleOnSecondaryContainerLight,
    tertiary = MarbleTertiaryLight,
    tertiaryContainer = MarbleTertiaryContainerLight,
    background = MarbleBackgroundLight,
    surface = MarbleSurfaceLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = MarblePrimaryDark,
    onPrimary = MarbleOnPrimaryDark,
    primaryContainer = MarblePrimaryContainerDark,
    onPrimaryContainer = MarbleOnPrimaryContainerDark,
    secondary = MarbleSecondaryDark,
    onSecondary = MarbleOnSecondaryDark,
    secondaryContainer = MarbleSecondaryContainerDark,
    onSecondaryContainer = MarbleOnSecondaryContainerDark,
    tertiary = MarbleTertiaryDark,
    tertiaryContainer = MarbleTertiaryContainerDark,
    background = MarbleBackgroundDark,
    surface = MarbleSurfaceDark,
)

val MarbleExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

val MarbleExpressiveTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
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
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun MarbleBrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    forceRtl: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val direction = if (forceRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MarbleExpressiveTypography,
            shapes = MarbleExpressiveShapes,
            content = content,
        )
    }
}
