package com.marble098.marblebr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.marble098.marblebr.engine.KeepAliveScheduler
import com.marble098.marblebr.vault.NotificationBridgeProvider
import com.marble098.marblebr.vault.NotificationCodeBridge
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ریشهٔ اپ. علاوه بر Hilt، پلِ «رمزِ یکبارمصرف» را هم در دسترس می‌گذارد.
 *
 * چرا این‌جا؟ چون `NotificationListenerService` را Hilt تزریق نمی‌کند؛ سرویس
 * `applicationContext` را به [NotificationBridgeProvider] تبدیل می‌کند و نمونهٔ تک‌نمونه‌ایِ
 * [NotificationCodeBridge] را می‌گیرد. اگر این اپ در همان فرایند بالا نیامده باشد، سرویس
 * بی‌صدا غیرفعال می‌ماند (هیچ‌چیز نمی‌شکند).
 */
@HiltAndroidApp
class MarbleBrApplication : Application(), Configuration.Provider, NotificationBridgeProvider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var keepAliveScheduler: KeepAliveScheduler

    @Inject
    lateinit var notificationCodeBridge: NotificationCodeBridge

    override val notificationBridge: NotificationCodeBridge
        get() = notificationCodeBridge

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { keepAliveScheduler.refreshSchedule() }
    }
}
