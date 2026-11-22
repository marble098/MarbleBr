package com.marble098.marblebr.feature.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * اجازه‌های اختیاریِ «ورودِ هوشمند».
 *
 * سه قاعدهٔ طراحی که آگاهانه انتخاب شده‌اند:
 * ۱. **هیچ‌چیز در اولین اجرا پرسیده نمی‌شود.** تا وقتی کاربر خودش «رمزِ یکبارمصرف» را روشن
 *    نکرده یا صفحه‌ای کد نخواسته، هیچ دیالوگِ سیستمی نمی‌بینیم.
 * ۲. **ردکردن، اپ را نمی‌شکند.** بدونِ `RECEIVE_SMS`، مسیرِ اعلان و «چسباندنِ دستی» کار می‌کنند.
 * ۳. **دسترسیِ اعلان با Intent گرفته می‌شود، نه با دیالوگ.** `NotificationListenerService` راهِ
 *    رسمی است و کاربر باید خودش در تنظیمات روشنش کند؛ همین‌جا با یک ضربه هدایتش می‌کنیم.
 */
@Composable
internal fun LoginAssistPermissionEffect(
    otpAssistEnabled: Boolean,
    onSmsPermissionResult: (granted: Boolean) -> Unit = {},
) {
    val context = LocalContext.current

    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> onSmsPermissionResult(granted) }

    val hasSms = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }

    // اعلان‌ها (Android 13+) فقط برای تذکرِ «کد نوشته شد» لازم‌اند؛ نبودشان مانعِ کار نیست.
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* هر نتیجه‌ای پذیرفته است */ }

    LaunchedEffect(otpAssistEnabled) {
        if (!otpAssistEnabled) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifications = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifications) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasSms) smsLauncher.launch(Manifest.permission.RECEIVE_SMS)
    }
}
