package com.marble098.marblebr.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.model.ChromeBarStyle

/**
 * «شیشهٔ مایع پریمیوم» (Liquid Glass Premium) — زیربنای ظاهری نوارهای بالا و پایین مرورگر.
 *
 * چهار لایهٔ بصری که با هم حس شیشهٔ واقعی می‌سازند:
 * ۱. سایهٔ نرم و رنگیِ بیرونی (عمق و شناوری)
 * ۲. گرادیان درونیِ نیمه‌شفاف (بدنهٔ شیشه)
 * ۳. بازتاب نور در لبهٔ بالا و هالهٔ رنگی ملایم در گوشه‌ها (specular highlight)
 * ۴. رینگ نورانی دور تا دور (rim light) به‌جای حاشیهٔ خاکستری
 *
 * همهٔ این لایه‌ها با [drawWithCache] یک‌بار محاسبه و کش می‌شوند؛ بنابراین هنگام بازترکیب یا
 * باز شدن کیبورد هیچ محاسبهٔ رنگی تازه‌ای انجام نمی‌شود (بدون لگ).
 *
 * @param style حالت انتخاب‌شده در تنظیمات: مات، شیشه‌ای یا شیشه‌ای شناورِ بی‌پس‌زمینه.
 * @param shape شکل نهایی شیشه.
 * @param fillAlphaOverride برای جاهایی که متن باید روی هر پس‌زمینه‌ای خوانا بماند (نوار آدرس).
 */
@Composable
fun Modifier.liquidGlass(
    style: ChromeBarStyle,
    shape: Shape = RoundedCornerShape(28.dp),
    fillAlphaOverride: Float? = null,
    glow: Boolean = true,
    elevation: Dp = 8.dp,
    bordered: Boolean = true,
): Modifier {
    val colors = MaterialTheme.colorScheme
    if (style == ChromeBarStyle.SOLID) {
        return shadow(elevation, shape).clip(shape).background(colors.surface)
    }

    val dark = colors.background.luminance() < 0.5f
    val baseAlpha = fillAlphaOverride ?: when (style) {
        ChromeBarStyle.GLASS -> if (dark) 0.78f else 0.86f
        else -> if (dark) 0.22f else 0.30f
    }
    val fillTop = colors.surface.copy(alpha = baseAlpha)
    val fillBottom = colors.surface.copy(alpha = (baseAlpha - 0.08f).coerceAtLeast(0.06f))
    val hasFill = baseAlpha > 0.02f

    var modifier = shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = colors.primary.copy(alpha = 0.28f),
        spotColor = Color.Black.copy(alpha = 0.32f),
    )
        .clip(shape)

    if (hasFill) {
        modifier = modifier.background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
    }

    return modifier
        .drawWithCache {
            val highlight = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = if (dark) 0.26f else 0.52f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.85f,
            )
            val leftGlow = if (glow) {
                Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.10f, 0f),
                    radius = size.width.coerceAtLeast(1f) * 0.65f,
                )
            } else {
                null
            }
            val rightGlow = if (glow) {
                Brush.radialGradient(
                    colors = listOf(colors.tertiary.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(size.width, size.height),
                    radius = size.width.coerceAtLeast(1f) * 0.7f,
                )
            } else {
                null
            }
            val topLine = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.40f else 0.75f), Color.Transparent),
            )
            onDrawBehind {
                leftGlow?.let { drawRect(it) }
                rightGlow?.let { drawRect(it) }
                drawRect(highlight)
                drawLine(
                    brush = topLine,
                    start = Offset(size.width * 0.12f, 0.6.dp.toPx()),
                    end = Offset(size.width * 0.88f, 0.6.dp.toPx()),
                    strokeWidth = 1.1.dp.toPx(),
                )
            }
        }
        .then(if (bordered) Modifier.border(width = 0.9.dp, brush = rimBrush(dark), shape = shape) else Modifier)
}

@Composable
private fun rimBrush(dark: Boolean): Brush {
    val colors = MaterialTheme.colorScheme
    return Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.30f else 0.60f),
            colors.outlineVariant.copy(alpha = 0.30f),
        ),
    )
}

/**
 * کپسول شیشه‌ایِ خوانا برای اجزای داخل نوار (نوار آدرس، فیلد جستجو، چیپ‌ها).
 * در حالت شیشه‌ای شناور، پس‌زمینهٔ نوار حذف می‌شود ولی خودِ کپسول یک لایهٔ نازک دارد تا
 * متن روی هر سایتی خوانا بماند.
 */
@Composable
fun Modifier.liquidGlassPill(
    style: ChromeBarStyle,
    shape: Shape = RoundedCornerShape(24.dp),
    emphasized: Boolean = false,
): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val alpha = when (style) {
        ChromeBarStyle.SOLID -> 1f
        ChromeBarStyle.GLASS -> if (dark) 0.72f else 0.82f
        ChromeBarStyle.FLOATING -> if (emphasized) (if (dark) 0.46f else 0.58f) else (if (dark) 0.30f else 0.42f)
    }
    return liquidGlass(
        style = if (style == ChromeBarStyle.SOLID) ChromeBarStyle.SOLID else ChromeBarStyle.FLOATING,
        shape = shape,
        fillAlphaOverride = alpha,
        glow = false,
        elevation = 4.dp,
    )
}

/** لایهٔ ساده و سریع برای آیکن‌های شیشه‌ای (بدون سایهٔ سنگین) — مناسب نوار پایین. */
@Composable
fun Modifier.marbleFloatingChip(
    style: ChromeBarStyle,
    selected: Boolean,
    shape: Shape = RoundedCornerShape(50),
): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    val alpha = when {
        style == ChromeBarStyle.SOLID -> if (selected) 1f else 0.10f
        style == ChromeBarStyle.GLASS -> if (selected) 0.92f else (if (dark) 0.34f else 0.48f)
        selected -> if (dark) 0.62f else 0.74f
        else -> if (dark) 0.16f else 0.24f
    }
    val tint = if (selected) colors.primaryContainer.copy(alpha = alpha) else colors.surface.copy(alpha = alpha)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = (alpha - 0.06f).coerceAtLeast(0.05f)))))
        .drawWithCache {
            val highlight = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = if (dark) 0.18f else 0.42f), Color.Transparent),
                startY = 0f,
                endY = size.height,
            )
            onDrawBehind { drawRect(highlight) }
        }
        .border(
            width = 0.9.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (dark) 0.22f else 0.55f),
                    colors.outlineVariant.copy(alpha = 0.22f),
                ),
            ),
            shape = shape,
        )
}

/**
 * دکمهٔ آیکنیِ شیشه‌ای با انیمیشن فشردنِ نرم (spring) و هالهٔ فعال.
 *
 * از ripple استفاده نمی‌کند؛ به‌جای آن مقیاس و روشنایی را با spring تغییر می‌دهد تا حس
 * «شیشهٔ واقعی» بدهد و هیچ بازترکیب سنگینی ایجاد نکند.
 */
@Composable
fun MarbleGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChromeBarStyle = ChromeBarStyle.FLOATING,
    selected: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    badgeCount: Int = 0,
    tint: Color? = null,
    containerColor: Color? = null,
    testTag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "glass press",
    )
    val iconTint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.35f)
        tint != null -> tint
        selected -> colors.onPrimaryContainer
        else -> colors.onSurface
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.6f
            }
            .then(
                if (containerColor != null) {
                    Modifier.clip(RoundedCornerShape(50)).background(containerColor)
                } else {
                    Modifier.marbleFloatingChip(style = style, selected = selected)
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
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * دکمهٔ بازگشتِ اختصاصیِ MarbleBr: شیشه‌ای، با انیمیشن فشردن و فلشِ کشویی.
 * در تنظیمات و «مراقبت از حساب‌ها» استفاده می‌شود تا حس حرکت یکدست باشد.
 */
@Composable
fun MarbleBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: ChromeBarStyle = ChromeBarStyle.FLOATING,
    testTag: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shift by animateFloatAsState(
        targetValue = if (pressed) -2.5f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "back shift",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "back scale",
    )
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .marbleFloatingChip(style = style, selected = false)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = colors.onSurface,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { translationX = shift },
        )
    }
}

/**
 * ردیف انتخابِ شیشه‌ای (جایگزین TabRow در تنظیمات).
 *
 * به‌جای تب‌های متنیِ فشرده و اسکرول‌نشده، هر گزینه یک کپسول مستقل با آیکن است؛ گزینهٔ فعال
 * با گرادیان رنگی و انیمیشن spring برجسته می‌شود و کل ردیف افقی اسکرول می‌شود.
 */
@Composable
fun <T> MarbleSegmentedRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    style: ChromeBarStyle = ChromeBarStyle.FLOATING,
    testTagPrefix: String? = null,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val colors = MaterialTheme.colorScheme
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.96f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
                label = "segment scale",
            )
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.primary.copy(alpha = 0.95f),
                                        colors.tertiary.copy(alpha = 0.90f),
                                    ),
                                ),
                            )
                        } else {
                            Modifier.marbleFloatingChip(style = style, selected = false)
                        },
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(option) },
                    )
                    .then(
                        if (testTagPrefix != null) {
                            Modifier.testTag(testTagPrefix)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon(option),
                    contentDescription = null,
                    tint = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                )
            }
        }
    }
}

