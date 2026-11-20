package com.marble098.marblebr.feature.browser

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings
import com.marble098.marblebr.core.designsystem.MarbleLayout
import com.marble098.marblebr.core.designsystem.SiteBadgeLogo
import com.marble098.marblebr.core.designsystem.siteBadgeAccent
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.PermissionState
import com.marble098.marblebr.core.model.ReaderTextEngine
import com.marble098.marblebr.core.model.SitePermission
import com.marble098.marblebr.core.model.SitePermissionPolicy
import com.marble098.marblebr.core.model.SitePermissionProfile
import com.marble098.marblebr.core.model.TabTitleEngine
import java.util.Locale

/**
 * کنترلرِ «بلندخوانی».
 *
 * ### چرا این‌جوری نوشته شده؟
 * موتور گفتارِ اندروید ([TextToSpeech]) چند نکتهٔ ظریف دارد:
 * - راه‌اندازی‌اش **غیرهمگام** است؛ اگر پیش از آماده شدن `speak` صدا بزنیم، متن بی‌صدا
 *   دور ریخته می‌شود. پس تا رسیدنِ `onInit(SUCCESS)` صف می‌سازیم.
 * - روی متن‌های بلند بریده می‌شود؛ برای همین متن را با
 *   [ReaderTextEngine.speechChunks] تکه‌تکه می‌کنیم و `QUEUE_ADD` می‌گذاریم.
 * - باید در پایان یا با بستنِ شیت **آزاد** شود، وگرنه موتور گفتار در حافظه می‌ماند.
 *
 * خروجی این تابع یک تابع توقف برمی‌گرداند تا UI بتواند با یک لمس صدا را قطع کند.
 */
@Composable
private fun rememberSpeechController(
    text: String?,
    onDone: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val doneState = rememberUpdatedState(onDone)
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                // اگر متن پیش از آماده‌شدنِ موتور آمده باشد، همین‌جا خوانده می‌شود.
                pendingText?.let { queued -> speakChunks(engine, queued) }
                pendingText = null
            }
        }
        engine = tts
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    // فقط پایانِ آخرین تکه، پایانِ خواندن است.
                    if (utteranceId == LAST_UTTERANCE_SUFFIX) doneState.value()
                }
            },
        )
        onDispose {
            runCatching { tts.stop() }
            runCatching { tts.shutdown() }
            engine = null
        }
    }

    LaunchedEffect(text, ready) {
        val body = text ?: return@LaunchedEffect
        if (body.isBlank()) return@LaunchedEffect
        if (!ready) {
            pendingText = body
            return@LaunchedEffect
        }
        speakChunks(engine, body)
    }

    return {
        runCatching { engine?.stop() }
        doneState.value()
    }
}

/** شناسهٔ آخرین تکه، تا `onDone` فقط یک‌بار پیامِ پایان بدهد. */
private const val LAST_UTTERANCE_SUFFIX = "marble-last"

/** تکه‌تکه کردنِ متن و صف‌بندی در موتور گفتار. */
private fun speakChunks(engine: TextToSpeech?, text: String) {
    if (engine == null) return
    runCatching {
        engine.language = Locale.getDefault()
        val chunks = ReaderTextEngine.speechChunks(text)
        if (chunks.isEmpty()) return
        chunks.forEachIndexed { index, chunk ->
            val id = if (index == chunks.lastIndex) LAST_UTTERANCE_SUFFIX else "marble-$index"
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(chunk, mode, null, id)
        }
    }
}

/**
 * شیتِ «بلندخوانی»: وضعیتِ خواندن + توقف.
 *
 * طراحی ساده و آرام است: یک آیکنِ گرادیانی، یک جملهٔ کوتاه و دو دکمه. هیچ گزینهٔ فنی‌ای
 * نمایش داده نمی‌شود تا برای همه قابل فهم باشد.
 */
@Composable
internal fun ReadAloudSheet(
    text: String,
    onDismiss: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    var stopped by remember { mutableStateOf(false) }
    val stopSpeaking = rememberSpeechController(text = if (stopped) null else text) { stopped = true }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MarbleLayout.Screen)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (stopped) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = strings.readAloudTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = strings.readAloudDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (stopped) {
                Text(
                    text = strings.readAloudStop,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.outline,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MarbleLayout.Gap),
            ) {
                Button(
                    onClick = {
                        stopped = true
                        stopSpeaking()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !stopped,
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(strings.readAloudStop, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(strings.close, maxLines = 1)
                }
            }
        }
    }
}

/**
 * شیتِ «دسترسی‌های این سایت» — قابلیت سوم.
 *
 * برای هر دسترسی یک ردیف با نام، توضیح و قرصِ سه‌حالته (اجازه / بپرس / بلاک) نمایش داده
 * می‌شود. لمسِ قرص، وضعیت را می‌چرخاند و بلافاصله روی همان نشست اعمال می‌شود.
 */
@Composable
internal fun SitePermissionsSheet(
    profile: SitePermissionProfile,
    pageUrl: String,
    onCycle: (SitePermission) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val domain = TabTitleEngine.domainOf(pageUrl)
    val accent = remember(domain) { siteBadgeAccent(domain) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = MarbleLayout.Screen,
                end = MarbleLayout.Screen,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MarbleLayout.Inner),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SiteBadgeLogo(displayName = domain, domain = domain, size = 38.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.sitePermissionsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item(key = "body") {
                Text(
                    text = strings.sitePermissionsDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            SitePermissionPolicy.ordered.forEach { permission ->
                item(key = "perm_${permission.name}") {
                    PermissionRow(
                        permission = permission,
                        state = profile.stateOf(permission),
                        onCycle = { onCycle(permission) },
                    )
                }
            }
            item(key = "reset") {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_site_permissions_btn"),
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(strings.sitePermissionsReset)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: SitePermission,
    state: PermissionState,
    onCycle: () -> Unit,
) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    val (label, tint) = when (state) {
        PermissionState.ALLOW -> strings.permissionAllowed to colors.primary
        PermissionState.BLOCK -> strings.permissionBlocked to colors.error
        PermissionState.ASK -> strings.permissionAsk to colors.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerLow.copy(alpha = 0.7f))
            .clickable(role = Role.Button, onClick = onCycle)
            .testTag("site_permission_${permission.name}")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permissionLabel(strings, permission),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tint.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
    }
}

private fun permissionLabel(strings: MarbleStrings, permission: SitePermission): String = when (permission) {
    SitePermission.JAVASCRIPT -> strings.permissionJavascript
    SitePermission.AUTOPLAY -> strings.permissionAutoplay
    SitePermission.GEOLOCATION -> strings.permissionGeolocation
    SitePermission.CAMERA -> strings.permissionCamera
    SitePermission.MICROPHONE -> strings.permissionMicrophone
    SitePermission.NOTIFICATIONS -> strings.permissionNotifications
}

/**
 * نوارِ باریکِ «حالت مطالعه روشن است».
 *
 * چرا لازم است؟ وقتی حالت مطالعه روشن می‌شود، صفحه ساده و بی‌منو می‌شود؛ بدون این نشانگر
 * کاربر فکر می‌کند سایت خراب شده. یک نوار کوتاه با دکمهٔ «خاموش کن» این ابهام را رفع می‌کند.
 */
@Composable
internal fun ReaderModeNotice(onTurnOff: () -> Unit) {
    val strings = LocalMarbleStrings.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MarbleLayout.Screen, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("reader_mode_notice"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = strings.readerModeTitle,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
            color = colors.primary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onTurnOff, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text(
                text = strings.readerModeOff,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}
