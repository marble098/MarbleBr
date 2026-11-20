package com.marble098.marblebr.core.database

import androidx.room.Database
import androidx.room.Migration
import androidx.room.RoomDatabase
import androidx.room.SupportSQLiteDatabase

@Database(
    entities = [
        ContainerEntity::class,
        SnapshotEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        DownloadEntity::class,
        HealthEventEntity::class,
        BrowserTabEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MarbleBrDatabase : RoomDatabase() {

    abstract fun containerDao(): ContainerDao

    abstract fun snapshotDao(): SnapshotDao

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun browserTabDao(): BrowserTabDao

    abstract fun historyDao(): HistoryDao

    abstract fun downloadDao(): DownloadDao

    abstract fun healthEventDao(): HealthEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS browser_tabs (
                        id TEXT NOT NULL,
                        containerId TEXT NOT NULL,
                        contextId TEXT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isPrivate INTEGER NOT NULL DEFAULT 0,
                        isDesktopMode INTEGER NOT NULL DEFAULT 0,
                        createdAtEpochMillis INTEGER NOT NULL,
                        lastVisitedAtEpochMillis INTEGER NOT NULL,
                        positionIndex INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (id)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_browser_tabs_positionIndex ON browser_tabs (positionIndex)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_browser_tabs_lastVisitedAtEpochMillis ON browser_tabs (lastVisitedAtEpochMillis)",
                )
            }
        }
    }
}
