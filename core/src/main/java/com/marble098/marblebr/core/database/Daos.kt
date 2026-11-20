package com.marble098.marblebr.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT * FROM containers ORDER BY lastActiveAtEpochMillis DESC")
    fun observeAll(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM containers ORDER BY lastActiveAtEpochMillis DESC")
    suspend fun getAll(): List<ContainerEntity>

    @Query("SELECT * FROM containers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContainerEntity?

    @Query("SELECT * FROM containers WHERE domain = :domain AND isPinnedPwa = 0 LIMIT 1")
    suspend fun getByDomain(domain: String): ContainerEntity?

    @Query("SELECT * FROM containers WHERE neverSignOutEnabled = 1 AND keepAliveEnabled = 1")
    suspend fun getKeepAliveEligibleContainers(): List<ContainerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(container: ContainerEntity)

    @Query("UPDATE containers SET neverSignOutEnabled = :enabled WHERE id = :containerId")
    suspend fun updateNeverSignOut(containerId: String, enabled: Boolean)

    @Query("UPDATE containers SET cookieExpirationDays = :days WHERE id = :containerId")
    suspend fun updateCookieExpirationDays(containerId: String, days: Int)

    @Query("UPDATE containers SET keepAliveEnabled = :enabled, keepAliveIntervalMinutes = :intervalMinutes WHERE id = :containerId")
    suspend fun updateKeepAlivePolicy(containerId: String, enabled: Boolean, intervalMinutes: Int)

    @Query("DELETE FROM containers WHERE id = :containerId")
    suspend fun deleteById(containerId: String)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM session_snapshots WHERE containerId = :containerId ORDER BY capturedAtEpochMillis DESC")
    fun observeSnapshotsForContainer(containerId: String): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM session_snapshots ORDER BY capturedAtEpochMillis DESC")
    fun observeAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM session_snapshots WHERE containerId = :containerId ORDER BY capturedAtEpochMillis DESC")
    suspend fun getSnapshotsForContainer(containerId: String): List<SnapshotEntity>

    @Query("SELECT * FROM session_snapshots WHERE containerId = :containerId AND isHealthy = 1 ORDER BY capturedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestHealthySnapshot(containerId: String): SnapshotEntity?

    @Query("SELECT * FROM session_snapshots WHERE id = :snapshotId LIMIT 1")
    suspend fun getById(snapshotId: String): SnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: SnapshotEntity)

    @Query("DELETE FROM session_snapshots WHERE id = :snapshotId")
    suspend fun deleteById(snapshotId: String)

    @Query("DELETE FROM session_snapshots WHERE containerId = :containerId")
    suspend fun deleteAllForContainer(containerId: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY visitedAtEpochMillis DESC LIMIT 500")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_items WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAtEpochMillis DESC LIMIT 50")
    suspend fun search(query: String): List<HistoryEntity>

    @Query("SELECT * FROM history_items WHERE url = :url ORDER BY visitedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestByUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity)

    @Query("UPDATE history_items SET title = :title, visitedAtEpochMillis = :visitedAtEpochMillis WHERE id = :id")
    suspend fun updateVisit(id: String, title: String, visitedAtEpochMillis: Long)

    @Query("DELETE FROM history_items")
    suspend fun clearAll()
}

@Dao
interface BrowserTabDao {
    @Query("SELECT * FROM browser_tabs WHERE isPrivate = 0 ORDER BY positionIndex ASC, lastVisitedAtEpochMillis DESC")
    fun observeAllTabs(): Flow<List<BrowserTabEntity>>

    @Query("SELECT * FROM browser_tabs WHERE id = :id LIMIT 1")
    suspend fun getTab(id: String): BrowserTabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTab(tab: BrowserTabEntity)

    @Query("UPDATE browser_tabs SET positionIndex = :position WHERE id = :id")
    suspend fun updateTabPosition(id: String, position: Int)

    @Query("UPDATE browser_tabs SET url = :url, title = :title, lastVisitedAtEpochMillis = :lastVisitedAtEpochMillis WHERE id = :id")
    suspend fun updateTabVisit(id: String, url: String, title: String, lastVisitedAtEpochMillis: Long)

    @Query("DELETE FROM browser_tabs WHERE id = :id")
    suspend fun deleteTab(id: String)

    @Query("DELETE FROM browser_tabs")
    suspend fun deleteAllTabs()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_items ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadEntity)

    @Query("DELETE FROM download_items WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface HealthEventDao {
    @Query("SELECT * FROM health_events ORDER BY timestampEpochMillis DESC LIMIT 100")
    fun observeRecentEvents(): Flow<List<HealthEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: HealthEventEntity)

    @Query("DELETE FROM health_events WHERE containerId = :containerId")
    suspend fun clearForContainer(containerId: String)
}
