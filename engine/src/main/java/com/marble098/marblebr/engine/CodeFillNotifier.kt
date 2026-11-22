package com.marble098.marblebr.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * اعلانِ کوتاه و بی‌صدای «کد در کادر نوشته شد».
 *
 * چرا اعلان؟ چون کاربر روی صفحهٔ سایت است و اپ نمی‌تواند Snackbarِ خودش را روی صفحهٔ وبِ GeckoView
 * نشان دهد بدونِ اینکه نوارِ آدرس را شلوغ کند. این اعلان هیچ کدی را نشان نمی‌دهد (فقط تعدادِ
 * رقم‌ها) تا حتی در صفحهٔ قفل هم محرمانه بماند.
 */
@Singleton
class CodeFillNotifier @Inject constructor(private val context: Context) {

    fun show(length: Int, fromSms: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
        val text = if (fromSms) {
            "$length رقمی از پیامک در کادر نوشته شد"
        } else {
            "$length رقمی از اعلان در کادر نوشته شد"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(TITLE)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    companion object {
        private const val CHANNEL_ID = "marblebr_login_assist"
        private const val CHANNEL_NAME = "ورودِ هوشمند"
        private const val CHANNEL_DESCRIPTION = "اطلاعِ کوتاه دربارهٔ نوشتنِ رمزِ یکبارمصرف"
        private const val NOTIFICATION_ID = 4102
        private const val TITLE = "کد در کادر نوشته شد"
    }
}
