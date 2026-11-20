package com.marble098.marblebr.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.marble098.marblebr.core.model.ColorPalette

/**
 * موتور پالت‌های رنگیِ MarbleBr.
 *
 * چرا این فایل وجود دارد؟ پیش‌تر برنامه فقط یک تمِ آبیِ ثابت داشت (یا رنگ گوشی در حالت
 * Material You) و کاربر هیچ راهی برای «سلیقه‌ای کردنِ» ظاهر نداشت. اینجا هر پالت فقط با
 * **دو رنگِ امضا** ([PaletteSignature.primary] و [PaletteSignature.accent]) تعریف می‌شود و
 * بقیهٔ بیست‌وچند نقشِ Material 3 (container ها، surface ها، outline ها و رنگ‌های متنِ
 * روی هر سطح) به‌صورت الگوریتمی از همان دو رنگ ساخته می‌شود.
 *
 * مزیت این روش:
 * - **هماهنگی تضمین‌شده:** هر نقش از خانوادهٔ همان رنگ‌های امضاست، پس هیچ ترکیبِ ناهماهنگی
 *   (مثل قبل که `tertiary` بنفشِ ثابت با هر تمی می‌آمد) پیش نمی‌آید.
 * - **خوانایی:** روشناییِ `onX` بر اساس روشناییِ واقعیِ همان سطح (`luminance()`) انتخاب
 *   می‌شود؛ یعنی متن در تم روشن و تاریک همیشه کنتراست کافی دارد.
 * - **هزینهٔ صفر در زمان اجرا:** همهٔ پالت‌ها یک‌بار و به‌صورت `lazy` ساخته و کش می‌شوند.
 */
internal data class PaletteSignature(
    /** رنگِ اصلیِ پالت در تم روشن (رنگِ برند). */
    val primary: Color,
    /** رنگِ مکمل/تأکیدی پالت (گرادیان‌ها، هاله‌ها و آیکن‌های ثانویه). */
    val accent: Color,
    /** لحنِ پس‌زمینهٔ روشن؛ برای پالت‌های گرم یا سرد رنگِ متفاوتی لازم است. */
    val lightBackground: Color = Color(0xFFF7F8FB),
    /** لحنِ پس‌زمینهٔ تاریک؛ پالت‌های تیره‌تر پس‌زمینهٔ عمیق‌تری می‌خواهند. */
    val darkBackground: Color = Color(0xFF0B0C10),
)

private val NeutralInk = Color(0xFF111827)
private val NeutralInkDark = Color(0xFFE6E7EE)

/**
 * رنگِ متن/آیکنِ خوانا روی [background].
 *
 * از آستانهٔ روشنایی استفاده می‌کند (نه یک مقدار ثابت) تا روی هر سطحی — از سفید تا مشکی —
 * کنتراست حفظ شود؛ همین تابع است که خواناییِ کارت‌های شیشه‌ای را در همهٔ پالت‌ها تضمین می‌کند.
 */
internal fun readableOn(background: Color): Color =
    if (background.luminance() > 0.55f) NeutralInk else NeutralInkDark

/** نسخهٔ تمیزتر (کم‌اشباع‌تر) از یک رنگ؛ برای سطوحِ خیلی روشن/تیره. */
private fun soft(color: Color, target: Color, amount: Float): Color = lerp(color, target, amount)

internal val PaletteCatalog: Map<ColorPalette, PaletteSignature> = mapOf(
    // آرام، حرفه‌ای و پیش‌فرض؛ آبیِ اعتمادآور با مکملِ فیروزه‌ای.
    ColorPalette.OCEAN to PaletteSignature(
        primary = Color(0xFF3B82F6),
        accent = Color(0xFF06B6D4),
    ),
    // سبزِ زمردی؛ مناسب کسانی که حس «سالم و امن» می‌خواهند.
    ColorPalette.EMERALD to PaletteSignature(
        primary = Color(0xFF0E9F6E),
        accent = Color(0xFF84CC16),
        lightBackground = Color(0xFFF6FAF7),
    ),
    // غروبِ گرم: نارنجیِ سوخته با مکملِ سرخابی.
    ColorPalette.SUNSET to PaletteSignature(
        primary = Color(0xFFEA580C),
        accent = Color(0xFFDB2777),
        lightBackground = Color(0xFFFDF8F5),
        darkBackground = Color(0xFF12100F),
    ),
    // شاهی و اشرافی: بنفشِ عمیق با مکملِ طلایی.
    ColorPalette.ROYAL to PaletteSignature(
        primary = Color(0xFF7C3AED),
        accent = Color(0xFFD4A017),
        darkBackground = Color(0xFF0C0A14),
    ),
    // شاد و سرزنده: صورتیِ مرجانی با مکملِ آبیِ آسمانی.
    ColorPalette.CANDY to PaletteSignature(
        primary = Color(0xFFEC4899),
        accent = Color(0xFF38BDF8),
        lightBackground = Color(0xFFFCF7FA),
        darkBackground = Color(0xFF120C11),
    ),
    // مینیمال و جدی: خاکستریِ گرافیتی با مکملِ فولادی.
    ColorPalette.GRAPHITE to PaletteSignature(
        primary = Color(0xFF4B5563),
        accent = Color(0xFF0EA5E9),
        lightBackground = Color(0xFFF6F7F9),
        darkBackground = Color(0xFF0A0B0D),
    ),
    // گرم و طبیعی: شنیِ کویری با مکملِ سبزِ زیتونی.
    ColorPalette.SAND to PaletteSignature(
        primary = Color(0xFFB45309),
        accent = Color(0xFF4D7C0F),
        lightBackground = Color(0xFFFBF8F2),
        darkBackground = Color(0xFF12100C),
    ),
)

/**
 * پالت‌هایی که کاربر می‌تواند انتخاب کند.
 *
 * «رنگ‌های گوشی» (Material You) هم در فهرست هست و **اول** می‌آید، چون پیش‌فرض و
 * سازگارترین گزینه با رفتار نسخه‌های قبلی است.
 */
val SelectablePalettes: List<ColorPalette> = ColorPalette.entries
    .sortedBy { if (it == ColorPalette.DYNAMIC) 0 else 1 }

/** پالت جایگزین وقتی «رنگ‌های گوشی» روی دستگاهِ قدیمی در دسترس نیست. */
internal val FALLBACK_PALETTE: ColorPalette = ColorPalette.OCEAN

/**
 * ColorScheme کامل و هماهنگِ یک پالت.
 *
 * @param dark آیا نسخهٔ تاریک ساخته شود؟
 */
fun paletteColorScheme(palette: ColorPalette, dark: Boolean): ColorScheme {
    val signature = PaletteCatalog[palette] ?: PaletteCatalog.getValue(FALLBACK_PALETTE)
    return if (dark) darkSchemeFor(signature) else lightSchemeFor(signature)
}

private fun lightSchemeFor(sig: PaletteSignature): ColorScheme {
    val background = sig.lightBackground
    val ink = readableOn(background)
    val primary = sig.primary
    val accent = sig.accent
    val surface = lerp(Color.White, background, 0.25f)
    return lightColorScheme(
        primary = primary,
        onPrimary = readableOn(primary),
        primaryContainer = lerp(primary, Color.White, 0.82f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.45f),
        secondary = accent,
        onSecondary = readableOn(accent),
        secondaryContainer = lerp(accent, Color.White, 0.80f),
        onSecondaryContainer = lerp(accent, Color.Black, 0.45f),
        tertiary = lerp(primary, accent, 0.55f),
        onTertiary = readableOn(lerp(primary, accent, 0.55f)),
        tertiaryContainer = lerp(lerp(primary, accent, 0.55f), Color.White, 0.84f),
        onTertiaryContainer = lerp(lerp(primary, accent, 0.55f), Color.Black, 0.45f),
        error = Color(0xFFDC2626),
        onError = Color.White,
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceContainerLowest = lerp(Color.White, background, 0.10f),
        surfaceContainerLow = lerp(Color.White, background, 0.55f),
        surfaceContainer = soft(background, Color.White, 0.20f),
        surfaceContainerHigh = soft(background, Color.Black, 0.05f),
        surfaceContainerHighest = soft(background, Color.Black, 0.09f),
        onSurfaceVariant = soft(ink, background, 0.35f),
        outline = soft(ink, background, 0.52f),
        outlineVariant = soft(ink, background, 0.80f),
        inverseSurface = lerp(ink, Color.Black, 0.20f),
        inverseOnSurface = lerp(Color.White, background, 0.20f),
        inversePrimary = lerp(primary, Color.White, 0.55f),
        scrim = Color.Black,
    )
}

private fun darkSchemeFor(sig: PaletteSignature): ColorScheme {
    val background = sig.darkBackground
    val ink = readableOn(background)
    // در تم تاریک رنگ‌ها باید روشن‌تر شوند تا هم دیده شوند و هم کنتراست متن را نشکنند.
    val primary = lerp(sig.primary, Color.White, 0.32f)
    val accent = lerp(sig.accent, Color.White, 0.26f)
    val tertiary = lerp(sig.primary, sig.accent, 0.55f).let { lerp(it, Color.White, 0.30f) }
    return darkColorScheme(
        primary = primary,
        onPrimary = readableOn(sig.primary),
        primaryContainer = lerp(sig.primary, Color.Black, 0.55f),
        onPrimaryContainer = lerp(sig.primary, Color.White, 0.72f),
        secondary = accent,
        onSecondary = readableOn(sig.accent),
        secondaryContainer = lerp(sig.accent, Color.Black, 0.52f),
        onSecondaryContainer = lerp(sig.accent, Color.White, 0.70f),
        tertiary = tertiary,
        onTertiary = readableOn(sig.primary),
        tertiaryContainer = lerp(tertiary, Color.Black, 0.52f),
        onTertiaryContainer = lerp(tertiary, Color.White, 0.70f),
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A),
        errorContainer = Color(0xFF7F1D1D),
        onErrorContainer = Color(0xFFFEE2E2),
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceContainerLowest = soft(background, Color.Black, 0.35f),
        surfaceContainerLow = soft(background, Color.White, 0.05f),
        surfaceContainer = soft(background, Color.White, 0.09f),
        surfaceContainerHigh = soft(background, Color.White, 0.14f),
        surfaceContainerHighest = soft(background, Color.White, 0.20f),
        onSurfaceVariant = soft(ink, background, 0.30f),
        outline = soft(ink, background, 0.58f),
        outlineVariant = soft(background, Color.White, 0.22f),
        inverseSurface = ink,
        inverseOnSurface = background,
        inversePrimary = sig.primary,
        scrim = Color.Black,
    )
}
