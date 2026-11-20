package com.marble098.marblebr.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HealthEventEntity
import com.marble098.marblebr.core.i18n.LocalizationManager
import com.marble098.marblebr.core.i18n.MarbleStrings
import com.marble098.marblebr.core.model.SessionExpiryAssessment
import com.marble098.marblebr.core.model.SessionExpiryGuard
import com.marble098.marblebr.core.model.SessionInsurancePolicy
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.SiteInsuranceCandidate
import com.marble098.marblebr.core.model.WebAppContainer
import com.marble098.marblebr.engine.ContainerSessionManager
import com.marble098.marblebr.engine.KeepAliveScheduler
import com.marble098.marblebr.vault.SessionVaultManager
import com.marble098.marblebr.vault.CredentialVault
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * فیلترِ فهرستِ سایت‌های داشبورد «مراقبت از حساب‌ها».
 */
enum class VaultSiteFilter {
    ALL,
    PROTECTED,
    UNPROTECTED,
    PINNED,
}

/**
 * اطلاعاتِ تجمیعیِ داشبورد؛ یک بار محاسبه و در یک StateFlow نگه داشته می‌شود تا
 * هر بازترکیب جدید باعث بازمحاسبهٔ لیست‌ها و پرش تصویر نشود.
 */
data class VaultOverview(
    val filteredContainers: List<WebAppContainer> = emptyList(),
    val allContainers: List<WebAppContainer> = emptyList(),
    val totalSites: Int = 0,
    val protectedSites: Int = 0,
    val protectedPercent: Float = 0f,
    val totalSnapshots: Int = 0,
    val latestSnapshotAt: Long? = null,
    val lastHealthEventAt: Long? = null,
)

/**
 * یک سایتِ نزدیکِ انقضا در کارتِ «نگهبان انقضا».
 */
data class ExpiringSite(
    val containerId: String,
    val domain: String,
    val daysLeft: Int,
    val thresholdDays: Int,
    val authCookieCount: Int,
) {
    val isExpired: Boolean get() = daysLeft <= 0
}

/**
 * ViewModel داشبورد «مراقبت از حساب‌ها» — بازنویسی‌شده.
 *
 * ### باگ‌های ریشه‌ای که در نسخهٔ قبل وجود داشت
 * ۱. **تغییر وضعیت سایت تا باز شدن مجدد صفحه اعمال نمی‌شد:** کاربر سوییچِ «مراقبت از این
 *    سایت» را عوض می‌کرد، اما پنلِ جزئیات هنوز شیءِ قدیمیِ `WebAppContainer` را در
 *    `detailsContainer` نگه می‌داشت. (حالا شناسه نگه داشته می‌شود و شیء از جریان Room
 *    خوانده می‌شود.)
 * ۲. **اسنپ‌شاتِ «بازگشت» بعد از موفقیت پیام موفقیت نمی‌داد / پیام خطا نمی‌گفت چرا.**
 *    (حالا نتیجهٔ بازیابی، تعداد کوکی‌ها و زمان مقصد گزارش می‌شود.)
 * ۳. **حذف اسنپ‌شات و «خروج کامل» بدون بازتاب در فهرست زمان‌ها.** (هر دو به Room وصل‌اند.)
 * ۴. **نبودِ جستجو/فیلتر و شمارندهٔ سلامت.** (فیلتر + جستجوی محلی + آمار کلی اضافه شد.)
 * ۵. **اجرای چندبارهٔ عملیات سنگین روی چند کلیک پشت‌سرهم** (double-tap روی «برگردان»).
 *    (هر عملیات در حال اجرا علامت‌گذاری می‌شود و دکمه تا پایان غیرفعال است.)
 */
@HiltViewModel
class VaultDashboardViewModel @Inject constructor(
    private val containerDao: ContainerDao,
    val credentialVault: CredentialVault,
    private val vaultManager: SessionVaultManager,
    private val sessionManager: ContainerSessionManager,
    private val localizationManager: LocalizationManager,
    private val keepAliveScheduler: KeepAliveScheduler,
    private val healthEventDao: HealthEventDao,
) : ViewModel() {

    val containers: StateFlow<List<WebAppContainer>> = containerDao.observeAll()
        .map { list -> list.map(ContainerEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snapshots: StateFlow<List<SessionSnapshotMeta>> = vaultManager.observeAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val healthEvents: StateFlow<List<HealthEventEntity>> = healthEventDao.observeRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _feedback = MutableStateFlow<VaultFeedback?>(null)
    val feedback: StateFlow<VaultFeedback?> = _feedback.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(VaultSiteFilter.ALL)
    val filter: StateFlow<VaultSiteFilter> = _filter.asStateFlow()

    /** شناسهٔ سایتِ باز در پنل جزئیات (نه خودِ شیء، تا همیشه با دیتابیس هم‌گام باشد). */
    private val _openSiteId = MutableStateFlow<String?>(null)
    val openSiteId: StateFlow<String?> = _openSiteId.asStateFlow()

    /** عملیات در حال اجرا؛ برای غیرفعال‌کردن دکمه‌ها و جلوگیری از اجرای تکراری. */
    private val _busyAction = MutableStateFlow<String?>(null)
    val busyAction: StateFlow<String?> = _busyAction.asStateFlow()

    private val _globalProtectionEnabled = MutableStateFlow(true)
    val globalProtectionEnabled: StateFlow<Boolean> = _globalProtectionEnabled.asStateFlow()

    /** با هر تمدید/قفلِ نسخه، این شمارنده زیاد می‌شود تا «نگهبان انقضا» دوباره محاسبه شود. */
    private val _expiryRefreshTick = MutableStateFlow(0)

    /**
     * «نگهبان انقضا»: سایت‌هایی که ورودشان به انقضا نزدیک است.
     *
     * محاسبه از تازه‌ترین نسخهٔ سالمِ هر سایت انجام می‌شود؛ اگر نسخه‌ای نباشد یا کوکیِ ورودِ
     * زمان‌دار نداشته باشد، آن سایت در فهرست نمی‌آید (یعنی چیزی برای تمدید نیست).
     */
    val expiryWatch: StateFlow<List<ExpiringSite>> = combine(containers, _expiryRefreshTick) { list, tick -> list to tick }
        .map { (list, _) -> buildExpiryWatch(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<VaultOverview> = combine(
        containers,
        snapshots,
        healthEvents,
        query,
        filter,
        ::buildOverview,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultOverview())

    /** سایتِ بازشده در پنل جزئیات؛ همیشه از جریان دیتابیس آمده و تازه است. */
    val openSite: StateFlow<WebAppContainer?> = combine(containers, openSiteId) { list, id ->
        id?.let { target -> list.firstOrNull { it.id == target } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** اسنپ‌شات‌های همان سایتِ بازشده، تازه‌ترین اول. */
    val openSiteSnapshots: StateFlow<List<SessionSnapshotMeta>> =
        combine(snapshots, openSiteId) { list, id ->
            if (id == null) emptyList() else list.filter { it.containerId == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            // پرچمِ سراسریِ محافظت را از تنظیمات می‌خوانیم تا داشبورد بتواند هشدارِ درست بدهد.
            runCatching {
                _globalProtectionEnabled.value = sessionManager.neverSignOutGlobalEnabled()
            }
        }
    }

    private fun buildOverview(
        containers: List<WebAppContainer>,
        snapshots: List<SessionSnapshotMeta>,
        healthEvents: List<HealthEventEntity>,
        query: String,
        filter: VaultSiteFilter,
    ): VaultOverview {
        val needle = query.trim()
        val filtered = containers.filter { container ->
            val matchesFilter = when (filter) {
                VaultSiteFilter.ALL -> true
                VaultSiteFilter.PROTECTED -> container.neverSignOutEnabled
                VaultSiteFilter.UNPROTECTED -> !container.neverSignOutEnabled
                VaultSiteFilter.PINNED -> container.isPinnedPwa
            }
            val matchesQuery = needle.isEmpty() ||
                container.domain.contains(needle, ignoreCase = true) ||
                container.displayName.contains(needle, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        val protectedCount = containers.count { it.neverSignOutEnabled }
        return VaultOverview(
            filteredContainers = filtered,
            allContainers = containers,
            totalSites = containers.size,
            protectedSites = protectedCount,
            protectedPercent = if (containers.isEmpty()) 0f else protectedCount.toFloat() / containers.size,
            totalSnapshots = snapshots.size,
            latestSnapshotAt = snapshots.maxOfOrNull { it.capturedAtEpochMillis },
            lastHealthEventAt = healthEvents.maxOfOrNull { it.timestampEpochMillis },
        )
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setFilter(value: VaultSiteFilter) {
        _filter.value = value
    }

    fun openSiteDetails(container: WebAppContainer) {
        _openSiteId.value = container.id
    }

    fun closeSiteDetails() {
        _openSiteId.value = null
    }

    fun toggleSiteNeverSignOut(container: WebAppContainer, enabled: Boolean) {
        runBusy("toggle-${container.id}") {
            containerDao.updateNeverSignOut(container.id, enabled)
            sessionManager.refreshCookiePolicies()
            // اگر محافظت خاموش شد، زنده‌سازی پس‌زمینه هم باید متوقف شود (وگرنه نشستِ کاربر
            // برخلافِ انتخابش تازه می‌ماند).
            if (!enabled) {
                containerDao.updateKeepAlivePolicy(container.id, false, container.keepAliveIntervalMinutes)
            }
            refreshKeepAliveSchedule()
            notify { it.feedbackProtection(container.domain, enabled) }
        }
    }

    fun updateCookieExpirationDays(container: WebAppContainer, days: Int) {
        val safeDays = days.coerceIn(MIN_COOKIE_DAYS, MAX_COOKIE_DAYS)
        runBusy("cookie-${container.id}") {
            containerDao.updateCookieExpirationDays(container.id, safeDays)
            sessionManager.refreshCookiePolicies()
            notify { it.feedbackCookieDays(container.domain, safeDays) }
        }
    }

    fun updateKeepAlivePolicy(container: WebAppContainer, enabled: Boolean, intervalMinutes: Int) {
        runBusy("keepalive-${container.id}") {
            val safeInterval = intervalMinutes.coerceIn(MIN_KEEP_ALIVE_MINUTES, MAX_KEEP_ALIVE_MINUTES)
            containerDao.updateKeepAlivePolicy(container.id, enabled, safeInterval)
            refreshKeepAliveSchedule()
            notify {
                if (enabled) {
                    it.feedbackKeepAliveOn(container.domain, safeInterval)
                } else {
                    it.feedbackKeepAliveOff(container.domain)
                }
            }
        }
    }

    fun updateKeepAliveInterval(container: WebAppContainer, intervalMinutes: Int) {
        updateKeepAlivePolicy(container, container.keepAliveEnabled, intervalMinutes)
    }

    /**
     * بازگردانیِ یک نسخه روی مرورگر.
     *
     * مهم‌ترین باگِ نسخهٔ قبل: این تابع فقط نسخه را رمزگشایی می‌کرد و هیچ داده‌ای روی GeckoView
     * نوشته نمی‌شد؛ پس «برگردان» عملاً هیچ اثری نداشت ولی پیامِ موفقیت می‌داد. حالا
     * [ContainerSessionManager.restoreSnapshotOnOpenTab] کوکی‌ها (از جمله HttpOnly) و حافظهٔ
     * صفحه را واقعاً می‌نویسد و نتیجهٔ دقیق را برمی‌گرداند.
     */
    fun rollbackToSnapshot(snapshot: SessionSnapshotMeta) {
        runBusy("rollback-${snapshot.id}") {
            val outcome = sessionManager.restoreSnapshotOnOpenTab(snapshot)
            when {
                outcome.payloadUnavailable -> notify { it.feedbackRestoreFailed }
                outcome.openTabMissing -> notify { it.vaultRestoreNeedsOpenTab }
                outcome.cookiesRestored == 0 && outcome.storageKeysRestored == 0 ->
                    notify { it.feedbackRestoreEmpty }
                else -> notify {
                    it.feedbackRestored(
                        snapshot.domain.ifBlank { it.restore },
                        outcome.cookiesRestored,
                    )
                }
            }
        }
    }

    fun deleteSnapshot(snapshot: SessionSnapshotMeta) {
        runBusy("delete-${snapshot.id}") {
            vaultManager.deleteSnapshot(snapshot.id)
            notify { it.vaultDeleteSnapshotDone(snapshot.domain) }
        }
    }

    fun captureSnapshotNow(container: WebAppContainer) {
        runBusy("capture-${container.id}") {
            val saved = sessionManager.captureSiteSnapshotNow(container)
            notify { if (saved) it.bannerSnapshotSaved else it.snapshotUnavailable }
        }
    }

    /**
     * روشن/خاموش کردن محافظت از همهٔ سایت‌ها با یک لمس.
     *
     * قبلاً برای هر سایت باید جداگانه به پنل جزئیات می‌رفت؛ این تابع همان تصمیم را برای
     * همهٔ سایت‌ها (آن‌هایی که با فیلتر فعلی دیده می‌شوند) یک‌جا اعمال می‌کند.
     */
    fun setProtectionForAll(enabled: Boolean) {
        runBusy("protect-all") {
            containers.value.forEach { container ->
                if (container.neverSignOutEnabled != enabled) {
                    containerDao.updateNeverSignOut(container.id, enabled)
                    if (!enabled) {
                        containerDao.updateKeepAlivePolicy(container.id, false, container.keepAliveIntervalMinutes)
                    }
                }
            }
            sessionManager.refreshCookiePolicies()
            refreshKeepAliveSchedule()
            notify { if (enabled) it.vaultProtectAllOnDone else it.vaultProtectAllOffDone }
        }
    }

    fun signOutNow(container: WebAppContainer) {
        _openSiteId.value = null
        runBusy("signout-${container.id}") {
            sessionManager.signOutNow(container)
            refreshKeepAliveSchedule()
            notify { it.feedbackSignedOut(container.domain) }
        }
    }

    /* ------------------------- قابلیت‌های تازهٔ نگه‌داشتنِ ورود ------------------------- */

    /**
     * «نسخهٔ طلایی»: قفل/آزادکردنِ یک نسخه.
     * نسخهٔ قفل‌شده هرگز توسط چرخشِ خودکار حذف نمی‌شود و در بازیابیِ اضطراری اولویت دارد.
     */
    fun toggleSnapshotPinned(snapshot: SessionSnapshotMeta) {
        runBusy("pin-${snapshot.id}") {
            val targetPinned = !snapshot.isPinned
            val result = runCatching { vaultManager.setSnapshotPinned(snapshot.id, targetPinned) }.getOrNull()
            if (result == null) {
                notify { it.snapshotUnavailable }
            } else {
                notify { strings ->
                    if (targetPinned) {
                        strings.vaultGoldenPinnedDone(snapshot.domain)
                    } else {
                        strings.vaultGoldenRemoved(snapshot.domain)
                    }
                }
            }
        }
    }

    /** Re-capture an open account after the user has refreshed or signed into the site. */
    fun renewSiteLogin(container: WebAppContainer) {
        runBusy("renew-${container.id}") {
            val saved = runCatching { sessionManager.captureSiteSnapshotNow(container) }.getOrDefault(false)
            _expiryRefreshTick.update { it + 1 }
            notify { if (saved) it.vaultExpiryRenewed(container.domain) else it.snapshotUnavailable }
        }
    }

    /** Re-capture only accounts with an open page; never manufacture a server expiry. */
    fun renewAllExpiringLogins() {
        runBusy("renew-all") {
            val byId = containers.value.associateBy { it.id }
            val targets = expiryWatch.value
            if (targets.isEmpty()) {
                notify { it.vaultExpiryAllFresh }
                return@runBusy
            }
            var saved = 0
            targets.forEach { site ->
                val container = byId[site.containerId] ?: return@forEach
                if (runCatching { sessionManager.captureSiteSnapshotNow(container) }.getOrDefault(false)) saved++
            }
            _expiryRefreshTick.update { it + 1 }
            notify { it.vaultExpiryRenewedCount(saved) }
        }
    }

    /**
     * «بیمهٔ نشست»: پیش از کارهای پرخطر یک نسخهٔ تازه از همهٔ سایت‌های محافظت‌شدهٔ قدیمی گرفته می‌شود.
     * فقط سایت‌هایی که تبی باز دارند موفق می‌شوند؛ بقیه بی‌صدا رد می‌شوند (fail-soft).
     */
    fun insureSessionsNow() {
        runBusy("insure-all") {
            val selected = SessionInsurancePolicy.selectSitesToInsure(
                candidates = buildInsuranceCandidates(),
                nowEpochMillis = System.currentTimeMillis(),
            )
            val byId = containers.value.associateBy { it.id }
            var saved = 0
            selected.forEach { candidate ->
                val container = byId[candidate.containerId] ?: return@forEach
                if (runCatching { sessionManager.captureSiteSnapshotNow(container) }.getOrDefault(false)) {
                    saved++
                }
            }
            notify { it.vaultInsuranceDone(saved) }
        }
    }

    private suspend fun buildExpiryWatch(containerList: List<WebAppContainer>): List<ExpiringSite> =
        containerList
            .filter { it.neverSignOutEnabled }
            .mapNotNull { container ->
                val assessment: SessionExpiryAssessment = runCatching {
                    vaultManager.assessExpiryForContainer(container.id)
                }.getOrNull() ?: return@mapNotNull null
                val daysLeft = assessment.daysUntilExpiry ?: return@mapNotNull null
                val threshold = SessionExpiryGuard.renewalThresholdDays(container.cookieExpirationDays)
                if (daysLeft > threshold) return@mapNotNull null
                ExpiringSite(
                    containerId = container.id,
                    domain = container.domain,
                    daysLeft = daysLeft,
                    thresholdDays = threshold,
                    authCookieCount = assessment.authCookieCount,
                )
            }
            .sortedBy { it.daysLeft }

    private fun buildInsuranceCandidates(): List<SiteInsuranceCandidate> {
        val latestByContainer = snapshots.value
            .groupBy { it.containerId }
            .mapValues { (_, list) -> list.maxOf { it.capturedAtEpochMillis } }
        return containers.value.map { container ->
            SiteInsuranceCandidate(
                containerId = container.id,
                domain = container.domain,
                isProtected = container.neverSignOutEnabled,
                lastSnapshotAtEpochMillis = latestByContainer[container.id],
            )
        }
    }

    fun dismissFeedback() {
        _feedback.value = null
    }

    /**
     * پس از هر تغییرِ سیاستِ زنده‌سازی، کارِ پس‌زمینه با بازهٔ **واقعیِ** سایت‌ها دوباره
     * زمان‌بندی می‌شود؛ قبلاً فقط بازهٔ ثابتِ ۳۰ دقیقه در `Application.onCreate` ثبت می‌شد و
     * تغییراتِ کاربر هیچ اثری نداشتند.
     */
    private fun refreshKeepAliveSchedule() {
        viewModelScope.launch {
            runCatching {
                val eligible = containers.value.filter { it.neverSignOutEnabled && it.keepAliveEnabled }
                if (eligible.isEmpty()) {
                    keepAliveScheduler.cancelKeepAlive()
                } else {
                    keepAliveScheduler.schedulePeriodicKeepAlive(eligible.minOf { it.keepAliveIntervalMinutes }.toLong())
                }
            }
        }
    }

    private inline fun runBusy(key: String, crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            if (_busyAction.value != null) return@launch
            _busyAction.value = key
            try {
                block()
            } finally {
                _busyAction.value = null
            }
        }
    }

    private fun notify(builder: (MarbleStrings) -> String) {
        viewModelScope.launch {
            _feedback.update { VaultFeedback(builder(localizationManager.current())) }
        }
    }

    companion object {
        const val MIN_COOKIE_DAYS = 7
        const val MAX_COOKIE_DAYS = 730
        const val MIN_KEEP_ALIVE_MINUTES = 15
        const val MAX_KEEP_ALIVE_MINUTES = 720
    }
}

/** پیامِ بازخوردِ داشبورد با شناسهٔ یکتا تا هر بار نمایش داده شود. */
data class VaultFeedback(
    val message: String,
    val id: Long = System.nanoTime(),
)
