package com.marble098.marblebr.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.model.SiteBadge
import kotlin.math.absoluteValue

/**
 * رنگ‌های تازه و پرانرژی برای «لوگوی» سایت‌ها.
 * هر دامنه همیشه یکی از این جفت‌رنگ‌ها را می‌گیرد؛ پس نشان هر سایت ثابت است.
 */
private val BadgePalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF3B82F6) to Color(0xFF8B5CF6),
    Color(0xFF06B6D4) to Color(0xFF3B82F6),
    Color(0xFFF97316) to Color(0xFFEC4899),
    Color(0xFF10B981) to Color(0xFF06B6D4),
    Color(0xFF8B5CF6) to Color(0xFFEC4899),
    Color(0xFFF59E0B) to Color(0xFFEF4444),
    Color(0xFF14B8A6) to Color(0xFF84CC16),
    Color(0xFF6366F1) to Color(0xFF22D3EE),
    Color(0xFFEC4899) to Color(0xFFF97316),
    Color(0xFF0EA5E9) to Color(0xFF6366F1),
)

/**
 * نشانِ رنگی و گردِ یک سایت (به‌جای آیکن عمومیِ «سایت»).
 *
 * چون MarbleBr هیچ درخواستی به سرورهای بیرونی نمی‌فرستد، لوگو از نام و دامنه ساخته می‌شود:
 * یک مونوگرام کوتاه روی یک گرادیان رنگیِ ثابت.
 */
@Composable
fun SiteBadgeLogo(
    displayName: String,
    domain: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val badge = remember(displayName, domain) { SiteBadge.of(displayName, domain) }
    val palette = BadgePalettes[badge.colorSeed % BadgePalettes.size]
    val foreground = if (palette.first.luminance() > 0.6f) Color(0xFF0B1020) else Color.White

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.first, palette.second)))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badge.monogram,
            color = foreground,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.38f).sp,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

/** یک رنگ ملایم و اختصاصی برای حاشیه/هالهٔ هر سایت. */
fun siteBadgeAccent(domain: String): Color {
    val seed = SiteBadge.seedOf(domain).absoluteValue
    return BadgePalettes[seed % BadgePalettes.size].first
}
