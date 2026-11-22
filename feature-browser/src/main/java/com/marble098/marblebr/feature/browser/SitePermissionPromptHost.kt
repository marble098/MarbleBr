package com.marble098.marblebr.feature.browser

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marble098.marblebr.core.designsystem.LocalMarbleStrings

/**
 * میزبانِ پرسش‌های دسترسی.
 *
 * **نقدِ نسخهٔ قبل:** پروفایلِ دسترسی‌ها برای حالتِ ASK فقط `VALUE_PROMPT` برمی‌گرداند؛
 * اما هیچ پنجره‌ای در اپ وجود نداشت که آن «بپرس» را نشان دهد. نتیجه: هر سایتی که موقعیت یا
 * دوربین می‌خواست، بی‌صدا رد می‌شد و کاربر فکر می‌کرد سایت خراب است. همین‌جا هر سه نوع
 * درخواست (popup، دسترسیِ محتوایی، دسترسیِ اندروید) یک دیالوگِ روشن می‌گیرند.
 *
 * حالتِ ANDROID دیالوگِ خودِ سیستم را باز می‌کند و نتیجه را به Gecko برمی‌گرداند؛ بدون این،
 * اجازهٔ کاربر هرگز به Gecko نمی‌رسید.
 */
@Composable
internal fun SitePermissionPromptHost(
    pending: PendingSitePermission?,
    onResolve: (PendingSitePermission, Boolean) -> Unit,
) {
    val strings = LocalMarbleStrings.current

    val inFlight = remember { mutableStateOf<PendingSitePermission?>(null) }
    val androidLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val request = inFlight.value
        inFlight.value = null
        if (request != null) onResolve(request, request.androidPermissions.all { results[it] == true })
    }

    LaunchedEffect(pending, inFlight.value) {
        val current = pending ?: return@LaunchedEffect
        if (current.kind != PendingSitePermission.Kind.ANDROID || inFlight.value != null) return@LaunchedEffect
        if (current.androidPermissions.isEmpty()) {
            onResolve(current, false)
        } else {
            inFlight.value = current
            androidLauncher.launch(current.androidPermissions.toTypedArray())
        }
    }

    val current = pending ?: return
    if (current.kind == PendingSitePermission.Kind.ANDROID) return

    val domain = com.marble098.marblebr.core.model.CredentialCaptureHeuristics.domainOf(current.url)
        .ifBlank { current.url }
    val title = if (current.kind == PendingSitePermission.Kind.POPUP) {
        strings.permissionPopupTitle
    } else {
        strings.permissionAskTitle
    }
    val body = if (current.kind == PendingSitePermission.Kind.POPUP) {
        strings.permissionPopupBody
    } else {
        val permissionName = current.permission?.let { permissionLabel(strings, it) }.orEmpty()
        strings.permissionRequestBody(domain, permissionName)
    }

    AlertDialog(
        onDismissRequest = { onResolve(current, false) },
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (domain.isNotBlank() && current.kind == PendingSitePermission.Kind.POPUP) {
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
                if (current.kind == PendingSitePermission.Kind.CONTENT) {
                    Text(
                        text = strings.permissionAndroidRequired,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onResolve(current, true) }) {
                Text(if (current.kind == PendingSitePermission.Kind.POPUP) strings.libraryOpenInNewTab else strings.permissionAllow)
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(current, false) }) { Text(strings.permissionBlock) }
        },
    )
}
