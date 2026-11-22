package com.marble098.marblebr.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.marble098.marblebr.core.model.BookmarkItem
import com.marble098.marblebr.core.model.DownloadItem
import com.marble098.marblebr.core.model.DownloadState
import com.marble098.marblebr.core.model.HistoryItem
import com.marble098.marblebr.core.model.SessionSnapshotMeta
import com.marble098.marblebr.core.model.WebAppContainer

@Entity(
    tableName = "containers",
    indices = [Index(value = ["domain"], unique = false), Index(value = ["contextId"], unique = true)],
)
data class ContainerEntity(
    @PrimaryKey val id: String,
    val domain: String,
    val displayName: String,
    val contextId: String,
    val neverSignOutEnabled: Boolean,
    val cookieExpirationDays: Int,
    val keepAliveEnabled: Boolean,
    val keepAliveIntervalMinutes: Int,
    val keepAliveEndpointPath: String,
    val storagePersistRequested: Boolean,
    val isPinnedPwa: Boolean,
    val pwaStartUrl: String?,
    val themeColorHex: String?,
    val lastActiveAtEpochMillis: Long,
) {
    fun toDomain(): WebAppContainer = WebAppContainer(
        id = id,
        domain = domain,
        displayName = displayName,
        contextId = contextId,
        neverSignOutEnabled = neverSignOutEnabled,
        cookieExpirationDays = cookieExpirationDays,
        keepAliveEnabled = keepAliveEnabled,
        keepAliveIntervalMinutes = keepAliveIntervalMinutes,
        keepAliveEndpointPath = keepAliveEndpointPath,
        storagePersistRequested = storagePersistRequested,
        isPinnedPwa = isPinnedPwa,
        pwaStartUrl = pwaStartUrl,
        themeColorHex = themeColorHex,
        lastActiveAtEpochMillis = lastActiveAtEpochMillis,
    )

    companion object {
        fun fromDomain(model: WebAppContainer): ContainerEntity = ContainerEntity(
            id = model.id,
            domain = model.domain,
            displayName = model.displayName,
            contextId = model.contextId,
            neverSignOutEnabled = model.neverSignOutEnabled,
            cookieExpirationDays = model.cookieExpirationDays,
            keepAliveEnabled = model.keepAliveEnabled,
            keepAliveIntervalMinutes = model.keepAliveIntervalMinutes,
            keepAliveEndpointPath = model.keepAliveEndpointPath,
            storagePersistRequested = model.storagePersistRequested,
            isPinnedPwa = model.isPinnedPwa,
            pwaStartUrl = model.pwaStartUrl,
            themeColorHex = model.themeColorHex,
            lastActiveAtEpochMillis = model.lastActiveAtEpochMillis,
        )
    }
}

@Entity(
    tableName = "session_snapshots",
    indices = [
        Index(value = ["containerId", "capturedAtEpochMillis"]),
        Index(value = ["domain"]),
    ],
)
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val containerId: String,
    val domain: String,
    val capturedAtEpochMillis: Long,
    val encryptedPayloadBase64: String,
    val ivBase64: String,
    val payloadSha256: String,
    val cookieCount: Int,
    val storageKeyCount: Int,
    val isHealthy: Boolean,
    val triggerReason: String,
    val label: String,
    /**
     * «نسخهٔ طلایی»: نسخه‌ای که کاربر دستی قفل کرده است و چرخشِ خودکار هرگز آن را حذف نمی‌کند.
     * این ستون در نسخهٔ ۲ دیتابیس اضافه شده و پیش‌فرضش «قفل‌نشده» است.
     */
    val isPinned: Boolean = false,
) {
    fun toMeta(): SessionSnapshotMeta = SessionSnapshotMeta(
        id = id,
        containerId = containerId,
        domain = domain,
        capturedAtEpochMillis = capturedAtEpochMillis,
        cookieCount = cookieCount,
        storageKeyCount = storageKeyCount,
        payloadSha256 = payloadSha256,
        isHealthy = isHealthy,
        triggerReason = triggerReason,
        label = label,
        isPinned = isPinned,
    )
}

@Entity(tableName = "bookmarks", indices = [Index(value = ["url"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val folder: String,
    val createdAtEpochMillis: Long,
) {
    fun toDomain(): BookmarkItem = BookmarkItem(
        id = id,
        title = title,
        url = url,
        folder = folder,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    companion object {
        fun fromDomain(item: BookmarkItem): BookmarkEntity = BookmarkEntity(
            id = item.id,
            title = item.title,
            url = item.url,
            folder = item.folder,
            createdAtEpochMillis = item.createdAtEpochMillis,
        )
    }
}

@Entity(tableName = "history_items", indices = [Index(value = ["visitedAtEpochMillis"])])
data class HistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val containerId: String,
    val visitedAtEpochMillis: Long,
    /** تعداد بازدیدهای این آدرس؛ برای پربازدیدترین‌ها و مرتب‌سازیِ پیشنهادها. */
    @ColumnInfo(defaultValue = "1")
    val visitCount: Int = 1,
) {
    fun toDomain(): HistoryItem = HistoryItem(
        id = id,
        title = title,
        url = url,
        containerId = containerId,
        visitedAtEpochMillis = visitedAtEpochMillis,
        visitCount = visitCount,
    )
}

@Entity(tableName = "download_items", indices = [Index(value = ["createdAtEpochMillis"])])
data class DownloadEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val stateName: String,
    val destinationPath: String,
    val createdAtEpochMillis: Long,
) {
    fun toDomain(): DownloadItem = DownloadItem(
        id = id,
        fileName = fileName,
        url = url,
        mimeType = mimeType,
        totalBytes = totalBytes,
        downloadedBytes = downloadedBytes,
        state = runCatching { DownloadState.valueOf(stateName) }.getOrDefault(DownloadState.IN_PROGRESS),
        destinationPath = destinationPath,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}

@Entity(tableName = "health_events", indices = [Index(value = ["containerId", "timestampEpochMillis"])])
data class HealthEventEntity(
    @PrimaryKey val id: String,
    val containerId: String,
    val domain: String,
    val statusName: String,
    val actionName: String,
    val descriptionFa: String,
    val timestampEpochMillis: Long,
)

/** Only ciphertext and non-sensitive timestamps are persisted for saved credentials. */
@Entity(tableName = "saved_credentials")
data class CredentialEntity(
    @PrimaryKey val id: String,
    val encryptedPayloadBase64: String,
    val ivBase64: String,
    val payloadSha256: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
