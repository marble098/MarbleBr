package com.marble098.marblebr.vault

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * سرویسِ «دسترسی به اعلان‌ها» برای پیدا کردنِ رمزِ یکبارمصرف.
 *
 * چرا سرویس و نه خواندنِ مستقیمِ اعلان‌ها؟ اندروید متنِ اعلان را جز از این مسیر در اختیارِ اپ
 * نمی‌گذارد. سرویس فقط زمانی زنده است که کاربر در تنظیماتِ سیستم روشنش کرده باشد و در همان
 * لحظه هم هیچ‌چیز را نگه نمی‌دارد: متن فقط به [NotificationCodeBridge] می‌رود، استخراج می‌شود و
 * بلافاصله رها می‌شود. هیچ متنِ اعلانی لاگ، ذخیره یا ارسال نمی‌شود.
 *
 * نکتهٔ سازگاری: `@AndroidEntryPoint` روی `NotificationListenerService` توسط Hilt پشتیبانی
 * نمی‌شود (فقط Activity/Fragment/View/Service/BroadcastReceiver) و همین باعثِ خطای کامپایلِ
 * «ندارد superclass» می‌شد. به‌جای تزریق، سرویس نمونهٔ تک‌نمونه‌ایِ برنامه را از
 * `MarbleBrApplication` می‌گیرد؛ اگر اپ در همان فرایند بالا نیامده باشد، سرویس بی‌صدا کاری نمی‌کند.
 */
class MarbleNotificationListener : NotificationListenerService() {

    /** پلِ کد؛ اگر اپ این رابط را پیاده نکرده باشد `null` است و سرویس بی‌صدا کاری نمی‌کند. */
    private val bridge: NotificationCodeBridge?
        get() = runCatching {
            (application as? NotificationBridgeProvider)?.notificationBridge
        }.getOrNull()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        // اعلان‌های Ongoing (موسیقی، آپلود) و اعلان‌های خودِ اپ هرگز بررسی نمی‌شوند.
        if (sbn.isOngoing) return
        if (sbn.packageName == packageName) return
        val extras = notification.extras ?: return

        val parts = mutableListOf<String>()
        (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())?.let { parts += it }
        (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())?.let { parts += it }
        (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())?.let { parts += it }
        (extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())?.let { parts += it }
        if (parts.isEmpty()) return

        val target = bridge ?: return
        runCatching {
            target.offerNotificationText(
                text = parts.joinToString(" "),
                packageNameOfSender = sbn.packageName,
                postedAtEpochMillis = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
        }
    }

    companion object {
        /** Intentِ درست برای بردنِ کاربر به صفحهٔ «دسترسی به اعلان‌ها». */
        fun settingsIntent(): Intent = NotificationCodeBridge.settingsIntent()
    }
}
