package com.marble098.marblebr.vault

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.marble098.marblebr.core.model.CodeMessage
import com.marble098.marblebr.core.model.OneTimeCodeExtractor
import com.marble098.marblebr.core.model.OneTimeCodeHit
import com.marble098.marblebr.core.model.OneTimeCodeSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * «خواندنِ پیامک» — کاملاً روی دستگاه و فقط برای پیدا کردنِ رمزِ یکبارمصرف.
 *
 * سه تصمیمِ مهمِ حریم خصوصی:
 * ۱. **هیچ پیامکی ذخیره نمی‌شود.** متن فقط در حافظهٔ همین فرایند خوانده، امتیازدهی و سپس
 *    رها می‌شود؛ نه لاگ، نه دیتابیس، نه ارسال به شبکه.
 * ۲. **دسترسی اختیاری است.** اگر کاربر اجازه ندهد، همه‌چیز از مسیرِ اعلان‌ها کار می‌کند و اگر آن
 *    هم نباشد، کاربر می‌تواند کد را از اعلان کپی کند و اپ آن را «جای‌گذاری» می‌کند.
 * ۳. **فقط صندوقِ ورودی و فقط پیام‌های تازه.** `INBOX` با بازهٔ ۱۰ دقیقه؛ `SENT` و `DRAFT` هرگز.
 */
@Singleton
class SmsOneTimeCodeReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * آیا اپ می‌تواند هم پیامکِ تازه را بگیرد و هم صندوقِ ورودی را بخواند؟
     *
     * نکتهٔ مهم: `RECEIVE_SMS` فقط برای دریافتِ broadcast است؛ خواندنِ `Telephony.Sms.Inbox`
     * به `READ_SMS` نیاز دارد. پیش‌تر فقط `RECEIVE_SMS` بررسی می‌شد و در نتیجه
     * `recentMessages()` همیشه خالی بود (یا SecurityException می‌داد) و کدِ پیامکی که چند
     * ثانیه قبل از باز شدنِ صفحهٔ کد رسیده بود هرگز پیدا نمی‌شد.
     */
    fun hasPermission(): Boolean = hasReceivePermission() && hasReadPermission()

    fun hasReceivePermission(): Boolean = runCatching {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun hasReadPermission(): Boolean = runCatching {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /**
     * پیام‌های تازهٔ صندوقِ ورودی (حداکثر ۸ پیامِ ۱۰ دقیقهٔ اخیر).
     *
     * اگر دسترسی نباشد بی‌صدا فهرستِ خالی برمی‌گردد؛ هیچ استثنایی به لایهٔ بالاتر نمی‌رسد تا
     * «رمزِ یکبارمصرف» هرگز باعثِ از کار افتادنِ مرورگر نشود.
     */
    fun recentMessages(
        nowEpochMillis: Long,
        lookBackMillis: Long = DEFAULT_LOOKBACK_MILLIS,
        senderFilter: Set<String> = emptySet(),
    ): List<CodeMessage> {
        if (!hasReadPermission()) return emptyList()
        val cursor: Cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf((nowEpochMillis - lookBackMillis).toString()),
                "${Telephony.Sms.DATE} DESC",
            )
        }.getOrNull() ?: return emptyList()

        val messages = mutableListOf<CodeMessage>()
        cursor.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            if (addressIndex < 0 || bodyIndex < 0 || dateIndex < 0) return emptyList()
            while (it.moveToNext() && messages.size < MAX_MESSAGES) {
                val body = it.getString(bodyIndex) ?: continue
                // فیلترِ زودهنگام: پیامِ بدونِ عدد یا خیلی بلند، ارزشِ پردازش ندارد.
                if (body.length > MAX_BODY_LENGTH) continue
                if (body.none(Char::isDigit) && body.none { char -> isPersianDigit(char) }) continue
                val sender = it.getString(addressIndex).orEmpty()
                if (senderFilter.isNotEmpty() && senderFilter.none { allowed -> sender.contains(allowed, ignoreCase = true) }) {
                    continue
                }
                messages += CodeMessage(
                    text = body,
                    sender = sender,
                    receivedAtEpochMillis = it.getLong(dateIndex),
                    source = OneTimeCodeSource.SMS,
                )
            }
        }
        return messages
    }

    /** بهترین کدِ موجود در صندوقِ ورودی؛ `null` یعنی هنوز چیزی نرسیده است. */
    fun findRecentCode(
        nowEpochMillis: Long,
        lookBackMillis: Long = DEFAULT_LOOKBACK_MILLIS,
        senderFilter: Set<String> = emptySet(),
    ): OneTimeCodeHit? = OneTimeCodeExtractor.extractBest(
        messages = recentMessages(nowEpochMillis, lookBackMillis, senderFilter),
        nowEpochMillis = nowEpochMillis,
        maxAgeMillis = lookBackMillis,
    )

    companion object {
        const val DEFAULT_LOOKBACK_MILLIS = 10L * 60L * 1000L
        private const val MAX_MESSAGES = 8
        private const val MAX_BODY_LENGTH = 900

        fun isPersianDigit(char: Char): Boolean = char in '\u06F0'..'\u06F9' || char in '\u0660'..'\u0669'
    }
}

/**
 * دریافتِ پیامکِ تازه در همان لحظه (بدونِ انتظارِ ۱۰ دقیقه‌ای).
 *
 * از `SMS_RECEIVED` استفاده می‌کند که با `RECEIVE_SMS` کار می‌کند. این گیرنده فقط زمانی زنده
 * می‌شود که یک صفحهٔ کدِ تأیید باز باشد و کاربر «رمزِ یکبارمصرف» را روشن کرده باشد؛ در بقیهٔ
 * اوقات هیچ پیامکی خوانده نمی‌شود.
 */
class SmsCodeListener(
    private val onCode: (OneTimeCodeHit) -> Unit,
) {
    private var receiver: BroadcastReceiver? = null

    /**
     * شروعِ شنیدن. `registerReceiver` تا زمانی که `unregisterReceiver` نشود زنده می‌ماند، پس
     * پیش از ثبت همیشه از آزادبودنِ مسیر مطمئن می‌شویم؛ وگرنه دو ثبتِ پیاپی، `IllegalStateException`
     * می‌دهد و «کدِ یکبارمصرف» در دومین ورود کار نمی‌کرد.
     *
     * @return آیا گیرنده واقعاً ثبت شد (دسترسی هست و ثبت موفق بود)
     */
    fun start(context: Context): Boolean {
        if (receiver != null) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        val listener = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
                val messages = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()
                    ?: return
                val now = System.currentTimeMillis()
                messages.forEach { message ->
                    val body = message?.messageBody ?: return@forEach
                    val hit = OneTimeCodeExtractor.extract(
                        CodeMessage(
                            text = body,
                            sender = message.displayOriginatingAddress.orEmpty(),
                            receivedAtEpochMillis = message.timestampMillis.takeIf { it > 0 } ?: now,
                            source = OneTimeCodeSource.SMS,
                        ),
                        nowEpochMillis = now,
                    )
                    if (hit != null) onCode(hit)
                }
            }
        }
        // `RECEIVER_EXPORTED` عمداً انتخاب شده: گیرندهٔ پیامکِ سیستم از بیرونِ اپ می‌آید.
        val registered = runCatching {
            ContextCompat.registerReceiver(
                context,
                listener,
                IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.isSuccess
        receiver = if (registered) listener else null
        return registered
    }

    fun stop(context: Context) {
        receiver?.let { active -> runCatching { context.unregisterReceiver(active) } }
        receiver = null
    }

    val isListening: Boolean get() = receiver != null
}

/**
 * خواندنِ کد از **اعلان‌ها** — بدونِ هیچ دسترسیِ اضافه.
 *
 * از Android 4.4 به بعد، اپ‌ها با `NotificationListenerService` می‌توانند متنِ اعلان‌ها را ببینند؛
 * این تنها راهی است که واتساپ/تلگرام/اپ‌های بانکیِ بدونِ پیامک هم پوشش داده شوند. متنِ اعلان
 * فقط در حافظهٔ سرویس می‌ماند، به `OneTimeCodeExtractor` داده می‌شود و در یک `StateFlow` کوچک
 * (آخرین کد + زمان) قرار می‌گیرد که لایهٔ مرورگر آن را مصرف می‌کند.
 */
@Singleton
class NotificationCodeBridge @Inject constructor(@ApplicationContext private val context: Context) {

    private val _lastCode = kotlinx.coroutines.flow.MutableStateFlow<OneTimeCodeHit?>(null)

    /** آخرین کدِ استخراج‌شده از اعلان؛ مصرف‌کننده با [consume] پاکش می‌کند. */
    val lastCode: kotlinx.coroutines.flow.StateFlow<OneTimeCodeHit?> = _lastCode

    /**
     * آیا کاربر به اپ اجازهٔ «دسترسی به اعلان‌ها» داده است؟
     * صفحهٔ تنظیمات و راهنمای مرورگر از همین تابع استفاده می‌کنند.
     */
    fun isEnabled(): Boolean = runCatching {
        val flat = android.provider.Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS,
        ).orEmpty()
        val packageName = context.packageName
        flat.split(':').any { component ->
            component.startsWith(packageName) || component.contains("$packageName/")
        }
    }.getOrDefault(false)

    /** فراخوانی از سرویسِ اعلان‌ها؛ متنِ اعلان هرگز ذخیره یا لاگ نمی‌شود. */
    fun offerNotificationText(text: String?, packageNameOfSender: String, postedAtEpochMillis: Long) {
        val body = text?.trim().orEmpty()
        if (body.length < 4 || body.length > MAX_NOTIFICATION_LENGTH) return
        if (body.none(Char::isDigit) && body.none { SmsOneTimeCodeReader.isPersianDigit(it) }) return
        val hit = OneTimeCodeExtractor.extract(
            CodeMessage(
                text = body,
                sender = packageNameOfSender,
                receivedAtEpochMillis = postedAtEpochMillis,
                source = OneTimeCodeSource.NOTIFICATION,
            ),
            nowEpochMillis = System.currentTimeMillis(),
        ) ?: return
        _lastCode.value = hit
    }

    fun consume() {
        _lastCode.value = null
    }

    companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
        private const val MAX_NOTIFICATION_LENGTH = 600

        /** Intentِ صفحهٔ تنظیماتِ «دسترسی به اعلان‌ها» برای هدایتِ کاربر. */
        fun settingsIntent(): Intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /**
         * نامِ مجوزِ اختیاریِ پیامک. `Manifest.permission.RECEIVE_SMS` مقدارِ `const` از پیش‌ساختهٔ
         * کتابخانهٔ اندروید است و «ثابتِ کامپایل» به‌حساب نمی‌آید؛ برای همین رشته صریح نوشته شده
         * تا اپ همچنان کامپایل شود (مقدارش دقیقاً همان است).
         */
        const val SMS_PERMISSION: String = "android.permission.RECEIVE_SMS"
    }
}

/**
 * دسترسیِ سرویسِ اعلان به پلِ کد، بدونِ Hilt.
 *
 * چرا رابط (و نه تزریق)؟ چون Hilt روی `NotificationListenerService` کار نمی‌کند؛ اپ این رابط را
 * پیاده می‌کند و نمونهٔ تک‌نمونه‌ایِ [NotificationCodeBridge] را می‌دهد. سرویس اگر آن را پیدا نکند
 * بی‌صدا غیرفعال می‌ماند (مثلاً در فرایندی که اپ در آن بالا نیامده).
 */
interface NotificationBridgeProvider {
    val notificationBridge: NotificationCodeBridge
}
