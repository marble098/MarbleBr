package com.marble098.marblebr.core.model

import java.util.concurrent.TimeUnit

/**
 * پیشنهاد ثبت نسخهٔ محلی از تب باز بر اساس تاریخچهٔ استفاده و زمان نسخه‌های قبلی.
 * این برنامه‌ریز هیچ درخواست شبکه‌ای نمی‌فرستد، اعتبار ورود را بررسی نمی‌کند و انقضای سرور را
 * پیش‌بینی نمی‌کند. اجرای زمان‌بندی‌شدهٔ اندروید ممکن است به تعویق بیفتد.
 */
data class SiteUsagePattern(
    val domain: String,
    /** ساعت‌های محلی (۰ تا ۲۳) که کاربر معمولاً در آن‌ها سایت را باز می‌کند. */
    val activeHours: Set<Int> = emptySet(),
    val averageVisitsPerDay: Float = 0f,
    val lastVisitAtEpochMillis: Long = 0L,
    /** Local checkpoint time only; not an authentication health check. */
    val lastSnapshotAtEpochMillis: Long? = null,
    val neverSignOutEnabled: Boolean = true,
    val cookieDaysConfigured: Int = 365,
)

enum class PulseUrgency { LOW, NORMAL, HIGH, CRITICAL }

data class PulseRecommendation(
    val domain: String,
    val urgency: PulseUrgency,
    /** بازهٔ پیشنهادی برای اجرای بازدیدِ آرام (میلی‌ثانیه، از «الان»). */
    val dueInMillis: Long,
    /** چرا این تصمیم گرفته شد؛ برای نمایشِ شفاف به کاربر. */
    val reasons: List<PulseReason> = emptyList(),
    /** آیا بهتر است همین حالا و با تبِ باز انجام شود (کاربر در حال استفاده است). */
    val runWhileUserIsActive: Boolean = false,
)

enum class PulseReason {
    LATE_NIGHT_HABIT,
    EARLY_MORNING_HABIT,
    FREQUENT_DAILY_USE,
    LONG_SILENCE,
    SHORT_LOCAL_RETENTION,
    NEVER_SNAPSHOTTED,
    PROTECTION_OFF,
}

/**
 * برنامه‌ریزِ نبض.
 *
 * قاعده‌ها:
 * - سایتِ بدونِ محافظت هیچ‌وقت نبض نمی‌گیرد.
 * - هرچه عادتِ کاربر به آن سایت بیشتر باشد (بازدیدِ روزانهٔ بالاتر یا ساعتِ ثابتِ شبانه)،
 *   نبض زودتر و با اطمینانِ بیشتری زده می‌شود.
 * - اگر سایت مدت‌هاست «نفس نکشیده»، فاصله کوتاه‌تر می‌شود؛ اما هرگز به سایت‌هایی که کاربر
 *   واقعاً استفاده نمی‌کند حمله نمی‌شود (سقفِ ۶ ساعت تا ۲۴ ساعت).
 */
object SessionPulsePlanner {

    const val MIN_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L
    const val MAX_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    private val HOUR_MILLIS = TimeUnit.HOURS.toMillis(1)

    fun plan(
        patterns: List<SiteUsagePattern>,
        nowEpochMillis: Long,
        localHour: Int = hourOf(nowEpochMillis),
    ): List<PulseRecommendation> = patterns
        .filter { it.neverSignOutEnabled }
        .mapNotNull { pattern -> recommend(pattern, nowEpochMillis, localHour) }
        .filter { it.urgency != PulseUrgency.LOW }
        .sortedWith(
            compareByDescending<PulseRecommendation> { it.urgency.ordinal }
                .thenBy { it.dueInMillis },
        )

    /** کدام ساعت‌ها برای «بازدیدِ آرام» بی‌خطرترند؟ اولین بازهٔ دور از عادتِ کاربر. */
    fun quietWindows(pattern: SiteUsagePattern): List<IntRange> {
        val busy = pattern.activeHours
        if (busy.isEmpty()) return listOf(2..5)
        val windows = mutableListOf<IntRange>()
        var start: Int? = null
        for (hour in 0..47) {
            val normalized = hour % 24
            if (normalized in busy) {
                if (start != null) {
                    windows += start..((hour - 1) % 24)
                    start = null
                }
            } else if (start == null) {
                start = normalized
            }
        }
        if (start != null) windows += start..start
        return windows.sortedByDescending { window -> windowLength(window) }.take(2)
    }

    private fun recommend(
        pattern: SiteUsagePattern,
        nowEpochMillis: Long,
        localHour: Int,
    ): PulseRecommendation? {
        val reasons = mutableListOf<PulseReason>()
        val silenceMillis = (nowEpochMillis - pattern.lastVisitAtEpochMillis).coerceAtLeast(0)
        val sinceCheck = pattern.lastSnapshotAtEpochMillis
            ?.let { (nowEpochMillis - it).coerceAtLeast(0) }

        if (pattern.lastSnapshotAtEpochMillis == null) reasons += PulseReason.NEVER_SNAPSHOTTED
        if (pattern.averageVisitsPerDay >= 1.5f) reasons += PulseReason.FREQUENT_DAILY_USE
        if (silenceMillis >= TimeUnit.DAYS.toMillis(2)) reasons += PulseReason.LONG_SILENCE
        if (pattern.cookieDaysConfigured <= 14) reasons += PulseReason.SHORT_LOCAL_RETENTION

        val lateNight = (0..3).any { it in pattern.activeHours } || (22..23).any { it in pattern.activeHours }
        if (lateNight) reasons += PulseReason.LATE_NIGHT_HABIT
        val earlyMorning = (5..7).any { it in pattern.activeHours }
        if (earlyMorning) reasons += PulseReason.EARLY_MORNING_HABIT

        if (reasons.isEmpty()) return null

        // فاصلهٔ پایه: هرچه استفاده پرتکرارتر، نبض کوتاه‌تر؛ اما هرگز کمتر از سقفِ مجاز.
        val habitFactor = (1f - (pattern.averageVisitsPerDay / 6f)).coerceIn(0.25f, 1f)
        var interval = (MIN_INTERVAL_MILLIS + (MAX_INTERVAL_MILLIS - MIN_INTERVAL_MILLIS) * habitFactor).toLong()
        if (sinceCheck != null && sinceCheck < MIN_INTERVAL_MILLIS) {
            interval = interval.coerceAtLeast(MIN_INTERVAL_MILLIS - sinceCheck)
        }
        if (PulseReason.SHORT_LOCAL_RETENTION in reasons) interval = (interval * 0.6f).toLong()
        val due = interval.coerceIn(MIN_INTERVAL_MILLIS / 2, MAX_INTERVAL_MILLIS)

        val urgency = when {
            sinceCheck == null -> PulseUrgency.HIGH
            sinceCheck >= TimeUnit.DAYS.toMillis(5) -> PulseUrgency.CRITICAL
            sinceCheck >= TimeUnit.DAYS.toMillis(2) -> PulseUrgency.HIGH
            sinceCheck >= MAX_INTERVAL_MILLIS -> PulseUrgency.NORMAL
            else -> PulseUrgency.LOW
        }

        // اگر همین حالا در «پنجرهٔ آرام» این سایت هستیم، بهترین لحظه است.
        val quietNow = quietWindows(pattern).any { localHour in it }
        return PulseRecommendation(
            domain = pattern.domain,
            urgency = urgency,
            dueInMillis = if (quietNow) 0L else due,
            reasons = reasons.distinct(),
            runWhileUserIsActive = quietNow && silenceMillis < TimeUnit.HOURS.toMillis(2),
        )
    }

    private fun windowLength(range: IntRange): Int =
        if (range.last >= range.first) range.last - range.first + 1 else (24 - range.first) + range.last + 1

    fun hourOf(epochMillis: Long): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        return calendar.get(java.util.Calendar.HOUR_OF_DAY)
    }

    /**
     * از تاریخچهٔ بازدید، الگویِ هر دامنه را می‌سازد.
     */
    fun patternsFromHistory(visits: List<HistoryItem>, nowEpochMillis: Long): List<SiteUsagePattern> =
        visits.groupBy { CredentialCaptureHeuristics.domainOf(it.url) }
            .filterKeys { it.isNotBlank() }
            .map { (domain, items) ->
                val hours = items.map { hourOf(it.visitedAtEpochMillis) }.toSet()
                val distinctDays = items.map { it.visitedAtEpochMillis / TimeUnit.DAYS.toMillis(1) }.toSet().size
                SiteUsagePattern(
                    domain = domain,
                    activeHours = hours,
                    averageVisitsPerDay = if (distinctDays == 0) 0f else items.size.toFloat() / distinctDays,
                    lastVisitAtEpochMillis = items.maxOf { it.visitedAtEpochMillis },
                    // هرگز بازدیدشده یعنی «سالم‌بودن نامعلوم»؛ همان نشانهٔ NEVER_SNAPSHOTTED را می‌دهد
                    // و نبض را به سمتِ سایت‌های محافظت‌شده‌ای می‌برد که تا حالا بررسی نشده‌اند.
                    lastSnapshotAtEpochMillis = null,
                )
            }
}

/** تصمیمِ «این بازدید را همین حالا بزن یا به پس‌زمینه بسپار». */
object PulseScheduler {
    /** آیا این نبض را می‌توان با همان نشستِ بازِ کاربر اجرا کرد (بدون هزینهٔ باتری)؟ */
    fun canReuseOpenTab(recommendation: PulseRecommendation, hasOpenTabForDomain: Boolean): Boolean =
        hasOpenTabForDomain && recommendation.runWhileUserIsActive &&
            recommendation.urgency != PulseUrgency.CRITICAL

    /** فاصلهٔ کارِ دوره‌ای پس‌زمینه؛ محدود به بازهٔ امن. */
    fun periodicIntervalMinutes(recommendations: List<PulseRecommendation>): Long {
        val soonest = recommendations.minOfOrNull { it.dueInMillis } ?: MIN_MINUTES * 60_000L
        val minutes = (soonest / 60_000L).coerceIn(MIN_MINUTES, MAX_MINUTES)
        return minutes
    }

    const val MIN_MINUTES = 60L
    const val MAX_MINUTES = 720L
}
