package com.marble098.marblebr.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.marble098.marblebr.core.database.BookmarkDao
import com.marble098.marblebr.core.database.ContainerDao
import com.marble098.marblebr.core.database.DownloadDao
import com.marble098.marblebr.core.database.HealthEventDao
import com.marble098.marblebr.core.database.HistoryDao
import com.marble098.marblebr.core.database.MarbleBrDatabase
import com.marble098.marblebr.core.database.SnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MarbleBrDatabase =
        Room.databaseBuilder(
            context,
            MarbleBrDatabase::class.java,
            "marblebr_vault.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideContainerDao(db: MarbleBrDatabase): ContainerDao = db.containerDao()

    @Provides
    fun provideSnapshotDao(db: MarbleBrDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun provideBookmarkDao(db: MarbleBrDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideHistoryDao(db: MarbleBrDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideDownloadDao(db: MarbleBrDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideHealthEventDao(db: MarbleBrDatabase): HealthEventDao = db.healthEventDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("marblebr_prefs") },
        )
}
