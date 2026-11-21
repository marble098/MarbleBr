package com.marble098.marblebr.vault

import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.ContainerEntity
import com.marble098.marblebr.core.database.SnapshotDao
import com.marble098.marblebr.core.database.SnapshotEntity
import com.marble098.marblebr.core.model.BackupProblem
import com.marble098.marblebr.core.model.BackupRestoreOptions
import com.marble098.marblebr.core.model.KdfMetadata
import com.marble098.marblebr.core.model.MergePlan
import com.marble098.marblebr.core.model.VaultBackupCodec
import com.marble098.marblebr.core.model.VaultBackupCredential
import com.marble098.marblebr.core.model.VaultBackupFormat
import com.marble098.marblebr.core.model.VaultBackupHeader
import com.marble098.marblebr.core.model.VaultBackupMerger
import com.marble098.marblebr.core.model.VaultBackupParseResult
import com.marble098.marblebr.core.model.VaultBackupPayload
import com.marble098.marblebr.core.model.VaultBackupSession
import com.marble098.marblebr.core.model.VaultBackupSite
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** خلاصهٔ «چه چیزی در بسته هست» برای نمایش به کاربر پیش و پس از ساختِ پشتیبان. */
@Serializable
data class BackupScanResult(
    val credentialCount: Int = 0,
    val sessionCount: Int = 0,
    val siteCount: Int = 0,
    val unreadableCredentials: Int = 0,
    val createdAtEpochMillis: Long = 0,
)

/** نتیجهٔ واقعیِ بازگردانی؛ هیچ «موفقیتِ ساختگی» نداریم. */
@Serializable
data class BackupRestoreReport(
    val credentialsAdded: Int = 0,
    val credentialsUpdated: Int = 0,
    val credentialsSkipped: Int = 0,
    val credentialsFailed: Int = 0,
    val sessionsAdded: Int = 0,
    val sessionsUpdated: Int = 0,
    val sitesAdded: Int = 0,
    val sitesUpdated: Int = 0,
    /** اطلاعات ورودِ داخلِ بسته که با وضعیتِ فعلیِ این دستگاه قابلِ تبدیل نبود. */
    val credentialsNotDecryptableOnThisDevice: Int = 0,
) {
    val totalChanges: Int get() = credentialsAdded + credentialsUpdated + sessionsAdded + sessionsUpdated + sitesAdded + sitesUpdated
}

sealed interface BackupOutcome {
    data class Success(val text: String, val fileName: String, val counts: BackupScanResult) : BackupOutcome
    data class Failure(val problem: BackupProblem) : BackupOutcome
}

sealed interface RestoreOutcome {
    data class Success(val preview: RestorePreview) : RestoreOutcome
    data class Applied(val report: BackupRestoreReport) : RestoreOutcome
    data class Failure(val problem: BackupProblem) : RestoreOutcome
}

/** پیش‌نمایشِ صادقانهٔ «چه چیزی اضافه/به‌روز می‌شود» پیش از نوشتن روی داده‌های موجود. */
@Serializable
data class RestorePreview(
    val newCredentials: Int = 0,
    val updatedCredentials: Int = 0,
    val duplicateCredentials: Int = 0,
    val newSessions: Int = 0,
    val updatedSessions: Int = 0,
    val newSites: Int = 0,
    val updatedSites: Int = 0,
    val createdAtEpochMillis: Long = 0,
) {
    val totalChanges: Int get() = newCredentials + updatedCredentials + newSessions + updatedSessions + newSites + updatedSites
}

/**
 * «پشتیبان‌گیری از گذرواژه‌ها و ورودها» — لایهٔ اجرا در مخزن.
 *
 * چرا این‌طور ساخته شده؟
 * - بسته با **گذرواژهٔ خودِ کاربر** رمز می‌شود (PBKDF2-HMAC-SHA256 + AES-256-GCM). فایل روی هر
 *   دستگاه یا فضای ابری که برود، بدون آن گذرواژه خواندنی نیست. کلیدِ Android Keystore هم هرگز
 *   از دستگاه بیرون نمی‌رود، پس «نسخهٔ پشتیبانِ رمزنگاری‌شده با کلیدِ دستگاه» به درد گوشیِ نو
 *   نمی‌خورد؛ این طرح دقیقاً همان مشکل را حل می‌کند.
 * - هدرِ فایل هیچ داده‌ای از کاربر ندارد و گذرواژه هم در آن ذخیره نمی‌شود؛ فقط یک «اثرانگشتِ
 *   تأیید» تا رمزِ اشتباه پیامِ روشن بدهد، نه خطای مبهم.
 * - بازگردانی دو مرحله‌ای است: اول پیش‌نمایشِ بی‌خطر، بعد نوشتن. هیچ دادهٔ موجودی بی‌خبر پاک
 *   نمی‌شود و رکوردهای ناسازگار فقط گزارش می‌شوند.
 */
@Singleton
class VaultBackupManager @Inject constructor(
    private val credentialVault: CredentialVault,
    private val snapshotDao: SnapshotDao,
    private val containerDao: ContainerDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    /** آخرین بستهٔ تأییدشده برای مرحلهٔ دوم؛ فقط در حافظه و فقط تا نوشتن. */
    private var pendingPayload: VaultBackupPayload? = null

    fun hasPendingRestore(): Boolean = pendingPayload != null

    /**
     * بستهٔ رمزنگاری‌شده می‌سازد.
     *
     * رکوردهای اطلاعات ورود برای خروجی رمزگشایی می‌شوند (کلیدش روی همین دستگاه است) و بعد در
     * بستهٔ تازه با گذرواژهٔ کاربر دوباره رمز می‌شوند.
     */
    suspend fun createBackup(
        password: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
        appVersionName: String = "",
    ): BackupOutcome {
        if (password.length < MIN_PASSWORD_LENGTH) return BackupOutcome.Failure(BackupProblem.EMPTY_PASSWORD)

        val credentials = credentialVault.credentialsForBackup()
        val snapshots = snapshotDao.getAllForBackup()
        val sites = containerDao.getAll().map { it.toBackupSite() }

        if (credentials.isEmpty() && snapshots.isEmpty() && sites.isEmpty()) {
            return BackupOutcome.Failure(BackupProblem.EMPTY_BACKUP)
        }

        val payload = VaultBackupPayload(
            createdAtEpochMillis = nowEpochMillis,
            appVersionName = appVersionName,
            kdf = KdfMetadata(iterations = VaultBackupFormat.KDF_ITERATIONS),
            credentials = credentials,
            sessions = snapshots.map { it.toBackupSession() },
            sites = sites,
        )
        val sealed = runCatching {
            BackupCipher.seal(json.encodeToString(payload), password, VaultBackupFormat.KDF_ITERATIONS)
        }.getOrElse { return BackupOutcome.Failure(BackupProblem.CORRUPTED) }

        val counts = BackupScanResult(
            credentialCount = credentials.size,
            sessionCount = snapshots.size,
            siteCount = sites.size,
            unreadableCredentials = credentialVault.unreadableCredentialCount(),
            createdAtEpochMillis = nowEpochMillis,
        )
        val header = VaultBackupHeader(
            createdAtEpochMillis = nowEpochMillis,
            iterations = VaultBackupFormat.KDF_ITERATIONS,
            saltBase64 = sealed.saltBase64,
            ivBase64 = sealed.ivBase64,
            ciphertextSha256 = sealed.sha256,
            kdfCheckBase64 = VaultBackupCodec.passwordCheck(sealed.saltBase64, VaultBackupFormat.KDF_ITERATIONS, password),
            credentialCount = counts.credentialCount,
            sessionCount = counts.sessionCount,
            siteCount = counts.siteCount,
        )
        return BackupOutcome.Success(
            text = VaultBackupCodec.encode(header, sealed.ciphertextBase64),
            fileName = VaultBackupFormat.suggestedFileName(nowEpochMillis),
            counts = counts,
        )
    }

    /**
     * بسته را باز و **فقط پیش‌نمایش** می‌دهد؛ نوشتن جداگانه با [applyPendingRestore] انجام می‌شود.
     */
    suspend fun previewRestore(
        text: String,
        password: String,
        overwriteExisting: Boolean = true,
    ): RestoreOutcome {
        val decrypted = decodePayload(text, password)
        if (decrypted is RestoreOutcome.Failure) return decrypted
        val payload = pendingPayload ?: return RestoreOutcome.Failure(BackupProblem.CORRUPTED)

        val credentialPlan = VaultBackupMerger.planCredentials(
            incoming = payload.credentials,
            existing = credentialVault.credentialsForBackup(),
            overwriteExisting = overwriteExisting,
        )
        val existingSnapshotIds = snapshotDao.getAllForBackup().map { it.id }.toSet()
        val sessionPlan = VaultBackupMerger.planSessions(payload.sessions, existingSnapshotIds, overwriteExisting)
        val sitePlan = planSites(
            incoming = payload.sites,
            existing = containerDao.getAll().map { it.toBackupSite() },
            overwrite = overwriteExisting,
        )
        return RestoreOutcome.Success(
            RestorePreview(
                newCredentials = credentialPlan.additions.size,
                updatedCredentials = credentialPlan.updates.size,
                duplicateCredentials = credentialPlan.skipped.size,
                newSessions = sessionPlan.additions.size,
                updatedSessions = sessionPlan.updates.size,
                newSites = sitePlan.additions.size,
                updatedSites = sitePlan.updates.size,
                createdAtEpochMillis = payload.createdAtEpochMillis,
            ),
        )
    }

    /** اعمالِ همان بسته‌ای که پیش‌نمایشش به کاربر نشان داده شد. */
    suspend fun applyPendingRestore(options: BackupRestoreOptions = BackupRestoreOptions()): RestoreOutcome {
        val payload = pendingPayload ?: return RestoreOutcome.Failure(BackupProblem.CORRUPTED)
        pendingPayload = null

        var credentialsAdded = 0
        var credentialsUpdated = 0
        var credentialsFailed = 0
        var credentialsSkipped = 0
        if (options.restoreCredentials) {
            val plan: MergePlan<VaultBackupCredential> = VaultBackupMerger.planCredentials(
                incoming = payload.credentials,
                existing = credentialVault.credentialsForBackup(),
                overwriteExisting = options.overwriteExisting,
            )
            for (credential in plan.additions) {
                val saved = runCatching { credentialVault.saveFromBackup(credential) }.getOrNull()
                if (saved != null) credentialsAdded++ else credentialsFailed++
            }
            for (credential in plan.updates) {
                val saved = runCatching { credentialVault.saveFromBackup(credential, credential.id) }.getOrNull()
                if (saved != null) credentialsUpdated++ else credentialsFailed++
            }
            credentialsSkipped = plan.skipped.size
        }

        var sessionsAdded = 0
        var sessionsUpdated = 0
        if (options.restoreSessions) {
            val existingIds = snapshotDao.getAllForBackup().map { it.id }.toSet()
            val plan = VaultBackupMerger.planSessions(payload.sessions, existingIds, options.overwriteExisting)
            for (session in plan.additions) {
                runCatching { snapshotDao.insert(session.toEntity()) }.onSuccess { sessionsAdded++ }
            }
            for (session in plan.updates) {
                runCatching { snapshotDao.insert(session.toEntity()) }.onSuccess { sessionsUpdated++ }
            }
        }

        var sitesAdded = 0
        var sitesUpdated = 0
        if (options.restoreSites) {
            val existing = containerDao.getAll()
            val plan = planSites(payload.sites, existing.map { it.toBackupSite() }, options.overwriteExisting)
            val byDomain = existing.groupBy { it.domain.lowercase().removePrefix("www.") }
            for (site in plan.additions + plan.updates) {
                val previous = byDomain[site.domain.lowercase().removePrefix("www.")]?.firstOrNull()
                val entity = previous?.let { existingContainer ->
                    existingContainer.copy(
                        displayName = site.displayName.ifBlank { existingContainer.displayName },
                        neverSignOutEnabled = site.neverSignOutEnabled,
                        cookieExpirationDays = site.cookieExpirationDays.coerceIn(7, 730),
                        keepAliveEnabled = site.keepAliveEnabled,
                        keepAliveIntervalMinutes = site.keepAliveIntervalMinutes.coerceIn(5, 240),
                    )
                } ?: ContainerEntity(
                    id = "restored_${site.domain.lowercase().replace('.', '_')}",
                    domain = site.domain.lowercase().removePrefix("www."),
                    displayName = site.displayName.ifBlank { site.domain },
                    contextId = "marble_ctx_restored_${site.domain.lowercase().replace('.', '_')}",
                    neverSignOutEnabled = site.neverSignOutEnabled,
                    cookieExpirationDays = site.cookieExpirationDays.coerceIn(7, 730),
                    keepAliveEnabled = site.keepAliveEnabled,
                    keepAliveIntervalMinutes = site.keepAliveIntervalMinutes.coerceIn(5, 240),
                    keepAliveEndpointPath = "/",
                    storagePersistRequested = true,
                    isPinnedPwa = false,
                    pwaStartUrl = null,
                    themeColorHex = null,
                    lastActiveAtEpochMillis = System.currentTimeMillis(),
                )
                runCatching { containerDao.upsert(entity) }.onSuccess {
                    if (previous != null) sitesUpdated++ else sitesAdded++
                }
            }
        }

        return RestoreOutcome.Applied(
            BackupRestoreReport(
                credentialsAdded = credentialsAdded,
                credentialsUpdated = credentialsUpdated,
                credentialsSkipped = credentialsSkipped,
                credentialsFailed = credentialsFailed,
                sessionsAdded = sessionsAdded,
                sessionsUpdated = sessionsUpdated,
                sitesAdded = sitesAdded,
                sitesUpdated = sitesUpdated,
            ),
        )
    }

    /** حذفِ بستهٔ در انتظار (مثلاً کاربر پیش‌نمایش را بست). */
    fun discardPendingRestore() {
        pendingPayload = null
    }

    fun isValidBackupFile(text: String): Boolean = VaultBackupCodec.decode(text) is VaultBackupParseResult.Ready

    /* ------------------------------------------ کمکی‌های رمز ------------------------------------------ */

    private suspend fun decodePayload(text: String, password: String): RestoreOutcome {
        val parsed = VaultBackupCodec.decode(text)
        val ready = when (parsed) {
            is VaultBackupParseResult.Invalid -> return RestoreOutcome.Failure(parsed.reason)
            is VaultBackupParseResult.Ready -> parsed
        }
        if (password.isEmpty()) return RestoreOutcome.Failure(BackupProblem.EMPTY_PASSWORD)

        if (ready.header.kdfCheckBase64.isNotEmpty()) {
            val actual = VaultBackupCodec.passwordCheck(
                ready.header.saltBase64,
                ready.header.iterations,
                password,
            )
            if (!MessageDigest.isEqual(actual.toByteArray(), ready.header.kdfCheckBase64.toByteArray())) {
                return RestoreOutcome.Failure(BackupProblem.WRONG_PASSWORD)
            }
        }
        if (!BackupCipher.verify(ready.ciphertextBase64, ready.header.ciphertextSha256)) {
            return RestoreOutcome.Failure(BackupProblem.CORRUPTED)
        }
        val plaintext = runCatching {
            BackupCipher.open(
                ciphertextBase64 = ready.ciphertextBase64,
                password = password,
                saltBase64 = ready.header.saltBase64,
                ivBase64 = ready.header.ivBase64,
                iterations = ready.header.iterations,
            )
        }.getOrNull() ?: return RestoreOutcome.Failure(BackupProblem.WRONG_PASSWORD)
        val payload = runCatching { json.decodeFromString<VaultBackupPayload>(plaintext) }.getOrNull()
            ?: return RestoreOutcome.Failure(BackupProblem.CORRUPTED)
        pendingPayload = payload
        return RestoreOutcome.Success(RestorePreview(createdAtEpochMillis = payload.createdAtEpochMillis))
    }

    private fun planSites(
        incoming: List<VaultBackupSite>,
        existing: List<VaultBackupSite>,
        overwrite: Boolean,
    ): MergePlan<VaultBackupSite> {
        val existingByDomain = existing.associateBy { it.domain.lowercase().removePrefix("www.") }
        val additions = mutableListOf<VaultBackupSite>()
        val updates = mutableListOf<VaultBackupSite>()
        val skipped = mutableListOf<VaultBackupSite>()
        for (site in incoming) {
            when {
                existingByDomain[site.domain.lowercase().removePrefix("www.")] == null -> additions += site
                overwrite -> updates += site
                else -> skipped += site
            }
        }
        return MergePlan(additions, updates, skipped)
    }

    private fun SnapshotEntity.toBackupSession(): VaultBackupSession = VaultBackupSession(
        id = id,
        containerId = containerId,
        domain = domain,
        capturedAtEpochMillis = capturedAtEpochMillis,
        ciphertextBase64 = encryptedPayloadBase64,
        ivBase64 = ivBase64,
        payloadSha256 = payloadSha256,
        cookieCount = cookieCount,
        storageKeyCount = storageKeyCount,
        isHealthy = isHealthy,
        triggerReason = triggerReason,
        label = label,
        isPinned = isPinned,
    )

    private fun VaultBackupSession.toEntity(): SnapshotEntity = SnapshotEntity(
        id = id,
        containerId = containerId,
        domain = domain,
        capturedAtEpochMillis = capturedAtEpochMillis,
        encryptedPayloadBase64 = ciphertextBase64,
        ivBase64 = ivBase64,
        payloadSha256 = payloadSha256,
        cookieCount = cookieCount,
        storageKeyCount = storageKeyCount,
        isHealthy = isHealthy,
        triggerReason = triggerReason,
        label = label,
        isPinned = isPinned,
    )

    private fun ContainerEntity.toBackupSite(): VaultBackupSite = VaultBackupSite(
        domain = domain.lowercase().removePrefix("www."),
        displayName = displayName,
        neverSignOutEnabled = neverSignOutEnabled,
        cookieExpirationDays = cookieExpirationDays,
        keepAliveEnabled = keepAliveEnabled,
        keepAliveIntervalMinutes = keepAliveIntervalMinutes,
    )

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
