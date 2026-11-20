package com.marble098.marblebr.feature.browser

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marble098.marblebr.core.i18n.AppLanguage
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.MarbleLinks
import java.util.Locale

/**
 * پنلِ دعوت به استار دادن مخزن گیت‌هاب.
 *
 * طراحی عمداً پرانرژی است: گرادیان متحرک، ستارهٔ چشمک‌زن، نشان‌های اعتماد و
 * دکمهٔ بزرگ و درخشان. متن‌ها بر اساس زبانِ گوشی (نه زبانِ انتخاب‌شده در تنظیمات) نمایش داده می‌شوند
 * تا حتی پیش از انتخاب زبان هم قابل‌فهم باشند.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RateAppDialog(
    strings: MarbleStrings = MarbleStrings.forLanguage(
        AppLanguage.SYSTEM.resolve(Locale.getDefault().toLanguageTag()),
    ),
    onStarNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    val context = LocalContext.current
    val animation = rememberInfiniteTransition(label = "star-panel")

    val glow by animation.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0),
        ),
        label = "glow",
    )
    val starSpin by animation.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "spin",
    )
    val shine by animation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_800),
        ),
        label = "shine",
    )

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    Dialog(
        onDismissRequest = onLater,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 32.dp)
                .testTag("star_repo_dialog"),
            shape = RoundedCornerShape(34.dp),
            color = Color(0xFF0B1024),
            tonalElevation = 0.dp,
            shadowElevation = 24.dp,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // پس‌زمینهٔ گرادیانی پرانرژی + هالهٔ نورانی متحرک
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1B1145),
                                    Color(0xFF2C1065),
                                    Color(0xFF0B2A5B),
                                    Color(0xFF07203F),
                                ),
                                start = Offset(shine * 900f, 0f),
                                end = Offset(shine * 900f + 700f, 1200f),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .offset(x = (-40).dp, y = (-70).dp)
                        .scale(glow)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x66FFB86B), Color(0x33C084FC), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // ستارهٔ درخشان با هالهٔ نرم
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(glow)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0x88FFD479), Color(0x22FFD479), Color.Transparent),
                                    ),
                                    CircleShape,
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .rotate(starSpin)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFFE29F), Color(0xFFFF9A3C)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF3A2200),
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    // خطِ بولد و وسط‌چین — قلبِ این پنل
                    Text(
                        text = strings.starRepoBoldLine,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFE6A7),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("star_repo_bold_headline"),
                    )

                    Text(
                        text = strings.starRepoHeadline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = strings.starRepoSubline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFB9C4E6),
                        textAlign = TextAlign.Center,
                    )

                    // نشان‌های اعتماد
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        maxItemsInEachRow = 2,
                    ) {
                        TrustBadge(Icons.Default.VolunteerActivism, strings.starRepoBadgeFree)
                        TrustBadge(Icons.Default.Lock, strings.starRepoBadgePrivate)
                        TrustBadge(Icons.Default.Bolt, strings.starRepoBadgeFast)
                        TrustBadge(Icons.Default.Language, strings.starRepoBadgeBilingual)
                    }

                    // کارتِ آدرس مخزن — برجسته و خوانا
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color(0x66FFD479), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD479),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = MarbleLinks.GITHUB_DISPLAY,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF3D0),
                        )
                    }

                    // دکمهٔ اصلیِ بزرگ و درخشان
                    Button(
                        onClick = {
                            openUrl(MarbleLinks.GITHUB_URL)
                            onStarNow()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("star_repo_confirm_btn"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC44D),
                            contentColor = Color(0xFF2A1A00),
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.starRepoButton,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PillButton(
                            text = strings.starRepoTelegram,
                            modifier = Modifier.weight(1f),
                            onClick = { openUrl(MarbleLinks.TELEGRAM_URL) },
                        )
                        PillButton(
                            text = strings.starRepoWeb,
                            modifier = Modifier.weight(1f),
                            onClick = { openUrl(MarbleLinks.GITHUB_URL) },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onLater) {
                            Text(text = strings.starRepoLater, color = Color(0xFFB9C4E6))
                        }
                        TextButton(onClick = onNever) {
                            Text(text = strings.starRepoNever, color = Color(0xFF8E97B8))
                        }
                    }

                    Text(
                        text = strings.starRepoThanks,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF8E97B8),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustBadge(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF7FF0C3),
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFD6DDF5),
        )
    }
}

@Composable
private fun PillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFE8EDFF),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
