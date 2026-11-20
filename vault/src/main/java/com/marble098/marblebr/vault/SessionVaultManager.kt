package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.SessionExpiryAssessment
import com.marble098.marblebr.core.model.SessionExpiryGuard
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.SessionSnapshotPayload
import com.marble098.marblebr.core.model.SnapshotRotationItem
import com.marble098.marblebr.core.model.SnapshotRotationPlanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ویژگی‌های ۲، ۳، ۹، ۱۰ و ۱۱ Never Sign-out Engine:
 * - Session Vault: اسنپ‌شات دوره‌ای و رمزنگاری‌شده (AES-256-GCM) از کوکی‌ها، localStorage و IndexedDB هر سایت،
 *   همراه با بازیابی خودکار از آخرین اسنپ‌شات سالم.
 * - Session Time Machine: نگه‌داشتن تاریخچه چند اسنپ‌شات قبلی و امکان برگشت با یک لمس به هر نسخه دلخواه.
 * - «نسخهٔ طلایی» (نسخهٔ ۱.۳): قفل‌کردنِ یک نسخهٔ سالم تا چرخشِ خودکار هرگز حذفش نکند و همیشه
 *   اولین گزینه برای بازگردانی خودکار باشد.
 * - «نگهبان انقضا»: سنجشِ نزدیک‌ترین انقضای کوکی‌های ورود و تمدیدِ یک‌لمسیِ خودِ نسخه (بدون
 *   نیاز به باز بودنِ تب) تا ورود پیش از موعد از بین نرود.
 */
@Singleton
class SessionVaultManager @Inject constructor(
    private val snapshotDao: SnapshotDao,
    private val vaultCipher: AesGcmVaultCipher,
    private val cookieConverter: CookiePersistenceConverter,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeTimelineForContainer(containerId: String): Flow<List<SessionSnapshotMeta>> =
        snapshotDao.observeSnapshotsForContainer(containerId).map { list ->
            list.map(SnapshotEntity::toMeta)
        }

    fun observeAllSnapshots(): Flow<List<SessionSnapshotMeta>> =
        snapshotDao.observeAllSnapshots().map { list ->
            list.map(SnapshotEntity::toMeta)
        }

    /**
     * ثبت یک اسنپ‌شات رمزنگاری‌شده جدید برای کانتینر و حذف اسنپ‌شات‌های قدیمی‌تر از حد مجاز Time Machine.
     */
    suspend fun captureSnapshot(
        payload: SessionSnapshotPayload,
        targetCookieExpirationDays: Int = CookiePersistenceConverter.DEFAULT_EXPIRATION_DAYS,
        maxSnapshotsToKeep: Int = DEFAULT_MAX_SNAPSHOTS_PER_CONTAINER,
        triggerReason: String = "AUTO_PERIODIC",
        label: String = "",
    ): SessionSnapshotMeta {
        val promotedCookies = cookieConverter.promoteAll(
            cookies = payload.cookies,
            targetExpirationDays = targetCookieExpirationDays,
            nowEpochMillis = payload.capturedAtEpochMillis,
        )
        val normalizedPayload = payload.copy(cookies = promotedCookies)
        val isHealthy = evaluatePayloadHealth(normalizedPayload)

        val serializedJson = json.encodeToString(normalizedPayload)
        val encryptedBlob = vaultCipher.encrypt(
            plaintext = serializedJson,
            associatedData = payload.containerId,
        )

        val domain = extractDomain(payload.origin)
        val storageKeyCount =
            normalizedPayload.localStorage.size +
                normalizedPayload.sessionStorage.size +
                normalizedPayload.indexedDbDump.size

        val entity = SnapshotEntity(
            id = UUID.randomUUID().toString(),
            containerId = payload.containerId,
            domain = domain,
            capturedAtEpochMillis = payload.capturedAtEpochMillis,
            encryptedPayloadBase64 = encryptedBlob.ciphertextBase64,
            ivBase64 = encryptedBlob.ivBase64,
            payloadSha256 = encryptedBlob.plaintextSha256,
            cookieCount = promotedCookies.size,
            storageKeyCount = storageKeyCount,
            isHealthy = isHealthy,
            triggerReason = triggerReason,
            label = label,
            isPinned = false,
        )

        snapshotDao.insert(entity)
        pruneExcessSnapshots(payload.containerId, maxSnapshotsToKeep)
        return entity.toMeta()
    }

    /**
     * بازیابی خودکار از آخرین اسنپ‌شات سالم کانتینر.
     * در صورتی که آخرین اسنپ‌شات دچار دست‌کاری یا خرابی شده باشد، به‌صورت خودکار سراغ اسنپ‌شات سالم قبلی در Time Machine می‌رود.
     */
    suspend fun restoreLatestHealthySnapshot(containerId: String): SessionSnapshotPayload? {
        val candidates = snapshotDao.getSnapshotsForContainer(containerId)
            .filter { it.isHealthy }
            .sortedByDescending { it.capturedAtEpochMillis }

        for (candidate in candidates) {
            val decrypted = runCatching { decryptEntity(candidate) }.getOrNull()
            if (decrypted != null) {
                return decrypted
            }
        }
        return null
    }

    /**
     * «نسخهٔ طلایی» — بازیابیِ نسخهٔ قفل‌شدهٔ کاربر (تازه‌ترین نسخهٔ طلاییِ سالم).
     *
     * تفاوتش با [restoreLatestHealthySnapshot] این است که اولویت را به انتخابِ صریحِ کاربر
     * می‌دهد، نه به تازه‌ترین نسخه.
     */
    suspend fun restoreGoldenSnapshot(containerId: String): SessionSnapshotPayload? {
        val pinned = snapshotDao.getPinnedForContainer(containerId)
        for (candidate in pinned) {
            val decrypted = runCatching { decryptEntity(candidate) }.getOrNull()
            if (decrypted != null) return decrypted
        }
        return null
    }

    /**
     * Session Time Machine: بازگشت با یک لمس به یک اسنپ‌شات مشخص از تاریخچه.
     */
    suspend fun restoreSnapshotById(snapshotId: String): SessionSnapshotPayload? {
        val entity = snapshotDao.getById(snapshotId) ?: return null
        return runCatching { decryptEntity(entity) }.getOrNull()
    }

    /**
     * حذف کامل تمام اسنپ‌شات‌های یک سایت/کانتینر (مثلاً هنگام اجرای «Sign out now» دستی توسط کاربر).
     */
    suspend fun purgeSnapshotsForContainer(containerId: String) {
        snapshotDao.deleteAllForContainer(containerId)
    }

    suspend fun deleteSnapshot(snapshotId: String) {
        snapshotDao.deleteById(snapshotId)
    }

    /**
     * «نسخهٔ طلایی» — قفل‌کردن یا آزادکردنِ یک نسخه.
     *
     * @return وضعیت تازهٔ قفل، یا `null` اگر نسخه پیدا نشد.
     */
    suspend fun setSnapshotPinned(snapshotId: String, pinned: Boolean): Boolean? {
        snapshotDao.getById(snapshotId) ?: return null
        snapshotDao.setPinned(snapshotId, pinned)
        return pinned
    }

    /** نسخه‌های طلاییِ یک سایت (تازه‌ترین اول). */
    suspend fun pinnedSnapshotsForContainer(containerId: String): List<SessionSnapshotMeta> =
        snapshotDao.getPinnedForContainer(containerId).map(SnapshotEntity::toMeta)

    /**
     * «نگهبان انقضا» — سنجشِ نزدیک‌ترین انقضای کوکی‌های ورود در تازه‌ترین نسخهٔ سالمِ سایت.
     * اگر نسخه‌ای موجود نباشد، ارزیابیِ خالی برمی‌گردد (یعنی چیزی برای تمدید نیست).
     */
    suspend fun assessExpiryForContainer(
        containerId: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): SessionExpiryAssessment {
        val payload = restoreLatestHealthySnapshot(containerId) ?: return SessionExpiryAssessment()
        return SessionExpiryGuard.assess(payload.cookies, nowEpochMillis)
    }

    /**
     * تمدیدِ یک‌لمسیِ ورود: همان نسخهٔ سالم با عمرِ تازه دوباره ذخیره می‌شود.
     *
     * چرا لازم است؟ بعضی سایت‌ها کوکیِ ورود را با عمر کوتاه می‌سازند؛ پیش از این تنها راهِ
     * تمدید، بازکردنِ دستی سایت و انتظار برای زنده‌سازیِ پس‌زمینه بود. حالا خودِ نسخه (که
     * مبنای بازگردانی است) با تاریخ انقضای تازه بازنویسی می‌شود.
     */
    suspend fun renewSnapshotExpiry(
        containerId: String,
        targetExpirationDays: Int,
        triggerReason: String = "EXPIRY_GUARDIAN",
        label: String = "",
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): SessionSnapshotMeta? {
        val payload = restoreLatestHealthySnapshot(containerId) ?: return null
        if (payload.cookies.isEmpty() && payload.localStorage.isEmpty()) return null
        val renewedPayload = payload.copy(
            capturedAtEpochMillis = nowEpochMillis,
            cookies = cookieConverter.promoteAll(
                cookies = payload.cookies,
                targetExpirationDays = targetExpirationDays,
                nowEpochMillis = nowEpochMillis,
            ),
        )
        return captureSnapshot(
            payload = renewedPayload,
            targetCookieExpirationDays = targetExpirationDays,
            triggerReason = triggerReason,
            label = label,
        )
    }

    /**
     * بررسی اینکه آیا اسنپ‌شات حاوی داده‌های معتبر احراز هویت هست یا خیر.
     */
    fun evaluatePayloadHealth(payload: SessionSnapshotPayload): Boolean {
        val hasAuthCookie = payload.cookies.any { it.isLikelyAuthCookie() && it.value.isNotBlank() }
        val hasNonEmptyCookies = payload.cookies.any { it.value.isNotBlank() }
        val hasAuthStorageKey = (payload.localStorage.keys + payload.sessionStorage.keys + payload.indexedDbDump.keys)
            .any { key ->
                val lower = key.lowercase()
                lower.contains("token") ||
                    lower.contains("auth") ||
                    lower.contains("session") ||
                    lower.contains("user") ||
                    lower.contains("jwt")
            }
        return hasAuthCookie || hasAuthStorageKey || (hasNonEmptyCookies && payload.localStorage.isNotEmpty())
    }

    private fun decryptEntity(entity: SnapshotEntity): SessionSnapshotPayload {
        val plaintext = vaultCipher.decrypt(
            blob = EncryptedVaultBlob(
                ciphertextBase64 = entity.encryptedPayloadBase64,
                ivBase64 = entity.ivBase64,
                plaintextSha256 = entity.payloadSha256,
            ),
            associatedData = entity.containerId,
        )
        return json.decodeFromString<SessionSnapshotPayload>(plaintext)
    }

    /**
     * چرخشِ نسخه‌ها از [SnapshotRotationPlanner] استفاده می‌کند تا:
     * - نسخه‌های طلاییِ قفل‌شده هرگز حذف نشوند،
     * - اول نسخه‌های ناسالم حذف شوند (نسخهٔ ناسالم جای نسخهٔ سالم را نگیرد)،
     * - دست‌کم یک نسخهٔ تازه همیشه بماند.
     */
    private suspend fun pruneExcessSnapshots(containerId: String, maxSnapshotsToKeep: Int) {
        val all = snapshotDao.getSnapshotsForContainer(containerId)
        if (all.size <= maxSnapshotsToKeep.coerceAtLeast(1)) return
        val deletions = SnapshotRotationPlanner.planDeletions(
            items = all.map { entity ->
                SnapshotRotationItem(
                    id = entity.id,
                    capturedAtEpochMillis = entity.capturedAtEpochMillis,
                    isHealthy = entity.isHealthy,
                    isPinned = entity.isPinned,
                )
            },
            maxKeep = maxSnapshotsToKeep,
        )
        for (snapshotId in deletions) {
            snapshotDao.deleteById(snapshotId)
        }
    }

    private fun extractDomain(originOrUrl: String): String =
        runCatching {
            val uri = URI(if (originOrUrl.contains("://")) originOrUrl else "https://$originOrUrl")
            uri.host?.removePrefix("www.") ?: originOrUrl
        }.getOrDefault(originOrUrl)

    companion object {
        const val DEFAULT_MAX_SNAPSHOTS_PER_CONTAINER = 10
    }
}
