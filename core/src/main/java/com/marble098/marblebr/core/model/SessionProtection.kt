package com.marble098.marblebr.core.model

import java.util.concurrent.TimeUnit

/**
 * منطقِ خالصِ سه قابلیتِ تازهٔ «نگه‌داشتنِ ورود» در MarbleBr.
 *
 * این فایل عمداً هیچ وابستگی‌ای به اندروید، Room یا Compose ندارد تا بتوان همهٔ تصمیم‌های
 * مهم را با تستِ واحد سنجید؛ لایه‌های بالاتر (Vault/UI) فقط نتیجهٔ همین توابع را اجرا می‌کنند.
 *
 * ۱. [SnapshotRotationPlanner] — «نگهبانِ نسخه‌ها»: چرخشِ نسخه‌ها هرگز نسخهٔ طلاییِ قفل‌شده و
 *    تازه‌ترین نسخه‌های سالم را قربانی نمی‌کند و اول از همه نسخه‌های ناسالم را حذف می‌کند.
 * ۲. [SessionExpiryGuard] — «نگهبان انقضا»: نزدیک‌شدنِ انقضای کوکی‌های ورود را پیش از موعد
 *    تشخیص می‌دهد تا با یک لمس تمدید شوند.
 * ۳. [SessionInsurancePolicy] — «بیمهٔ نشست»: پیش از کارهای پرخطر (پاک‌کردن کش، بستنِ همهٔ
 *    تب‌ها، آپدیت) تصمیم می‌گیرد از کدام سایت‌ها نسخهٔ تازه گرفته شود.
 */

/* ------------------------------- ۱. نگهبان نسخه‌ها ------------------------------- */

/** یک نسخهٔ ذخیره‌شده، فقط با اطلاعاتی که برای تصمیمِ چرخش لازم است. */
data class SnapshotRotationItem(
    val id: String,
    val capturedAtEpochMillis: Long,
    val isHealthy: Boolean,
    val isPinned: Boolean,
)

/**
 * تصمیمِ «کدام نسخه‌ها حذف شوند» هنگام رسیدن به سقفِ نگه‌داری.
 *
 * قواعد (همه در تست پوشش داده شده‌اند):
 * - نسخهٔ **طلایی** (`isPinned`) هرگز حذف نمی‌شود، حتی اگر از سقف بیشتر باشد؛ این انتخابِ
 *   صریحِ کاربر است.
 * - از نسخه‌های غیرِطلایی، ابتدا **نسخه‌های ناسالم** حذف می‌شوند؛ نسخهٔ ناسالم نباید جای
 *   نسخهٔ سالم را بگیرد (این دقیقاً همان مسیری بود که باعث می‌شد پشتیبان‌های خوب از بین بروند).
 * - در بین هم‌کیفیت‌ها، قدیمی‌ترها اول حذف می‌شوند.
 * - دست‌کم یک نسخهٔ غیرِطلایی (تازه‌ترین) همیشه نگه داشته می‌شود تا تاریخچه خالی نماند.
 */
object SnapshotRotationPlanner {

    fun planDeletions(items: List<SnapshotRotationItem>, maxKeep: Int): List<String> {
        if (items.isEmpty()) return emptyList()
        val safeMax = maxKeep.coerceAtLeast(1)
        val pinnedIds = items.filter { it.isPinned }.map { it.id }.toSet()
        val candidates = items.filterNot { it.id in pinnedIds }
        if (candidates.isEmpty()) return emptyList()

        val budget = (safeMax - pinnedIds.size).coerceAtLeast(1)
        val ranked = candidates.sortedWith(
            compareByDescending<SnapshotRotationItem> { it.isHealthy }
                .thenByDescending { it.capturedAtEpochMillis },
        )
        return ranked.drop(budget).map { it.id }
    }
}

/* ------------------------------- ۲. نگهبان انقضا ------------------------------- */

/** نتیجهٔ بررسیِ انقضای کوکی‌های ورودِ یک نسخه. */
data class SessionExpiryAssessment(
    val authCookieCount: Int = 0,
    val earliestExpiryEpochMillis: Long? = null,
    val daysUntilExpiry: Int? = null,
) {
    /** آیا اصلاً کوکیِ ورودی با تاریخ انقضا داریم؟ */
    val hasExpiringAuthCookie: Boolean get() = earliestExpiryEpochMillis != null

    /** آیا ورود از قبل منقضی شده است؟ */
    val isExpired: Boolean get() = (daysUntilExpiry ?: Int.MAX_VALUE) <= 0

    fun needsRenewal(thresholdDays: Int = SessionExpiryGuard.DEFAULT_WARN_DAYS): Boolean {
        val days = daysUntilExpiry ?: return false
        return days <= thresholdDays
    }
}

/**
 * «نگهبان انقضا»: از میان کوکی‌ها، آن‌هایی را که واقعاً هویتِ کاربر را نگه می‌دارند جدا می‌کند،
 * نزدیک‌ترین تاریخِ انقضا را پیدا می‌کند و می‌گوید چند روز تا پایانِ ورود باقی است.
 */
object SessionExpiryGuard {
    const val DEFAULT_WARN_DAYS = 21

    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

    fun assess(cookies: List<CookieRecord>, nowEpochMillis: Long): SessionExpiryAssessment {
        val authCookies = cookies.filter { it.isLikelyAuthCookie() && it.value.isNotBlank() }
        val earliest = authCookies
            .mapNotNull { it.expiresEpochMillis }
            .filter { it > 0L }
            .minOrNull()
        val days = earliest?.let { expiry -> ((expiry - nowEpochMillis) / DAY_MILLIS).toInt() }
        return SessionExpiryAssessment(
            authCookieCount = authCookies.size,
            earliestExpiryEpochMillis = earliest,
            daysUntilExpiry = days,
        )
    }

    /**
     * آستانهٔ هشدار برای هر سایت.
     *
     * اگر کاربر «مدت ماندن ورود» را کوتاه گذاشته باشد (مثلاً ۷ روز)، هشدارِ ۲۱ روزه همیشه روشن
     * می‌ماند و بی‌معنا می‌شود؛ پس آستانه نصفِ عمرِ تنظیم‌شده است و از [maxWarnDays] بیشتر نمی‌شود.
     */
    fun renewalThresholdDays(
        configuredLifetimeDays: Int,
        maxWarnDays: Int = DEFAULT_WARN_DAYS,
    ): Int {
        val safeLifetime = configuredLifetimeDays.coerceAtLeast(1)
        val half = (safeLifetime / 2).coerceAtLeast(1)
        return minOf(maxWarnDays, half)
    }
}

/* ------------------------------- ۳. بیمهٔ نشست ------------------------------- */

/** یک سایتِ نامزدِ «بیمه»؛ آخرین زمانِ نسخهٔ سالمش هم همراه است. */
data class SiteInsuranceCandidate(
    val containerId: String,
    val domain: String,
    val isProtected: Boolean,
    val lastSnapshotAtEpochMillis: Long?,
)

/**
 * «بیمهٔ نشست»: پیش از هر کارِ پرخطر (پاک‌کردن کش، بستنِ همهٔ تب‌ها، نصب آپدیت) از سایت‌های
 * محافظت‌شده‌ای که نسخهٔ تازه ندارند یک نسخهٔ فوری گرفته می‌شود تا هیچ ورودی به‌خاطر یک
 * اشتباه از دست نرود.
 */
object SessionInsurancePolicy {
    const val DEFAULT_STALE_AFTER_MILLIS = 6L * 60L * 60L * 1000L

    fun selectSitesToInsure(
        candidates: List<SiteInsuranceCandidate>,
        nowEpochMillis: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
    ): List<SiteInsuranceCandidate> {
        val safeStale = staleAfterMillis.coerceAtLeast(0L)
        return candidates.filter { candidate ->
            if (!candidate.isProtected) return@filter false
            val last = candidate.lastSnapshotAtEpochMillis
            last == null || nowEpochMillis - last >= safeStale
        }
    }
}
