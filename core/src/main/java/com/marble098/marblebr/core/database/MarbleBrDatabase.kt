package com.marble098.marblebr.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContainerEntity::class,
        SnapshotEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        HealthEventEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MarbleBrDatabase : RoomDatabase() {
    abstract fun containerDao(): ContainerDao
    abstract fun snapshotDao(): SnapshotDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun healthEventDao(): HealthEventDao
}
