package com.marble098.marblebr.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.marble098.marblebr.core.database.BookmarkDao
import com.marble098.marblebr.core.database.CredentialDao
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

    /**
     * مهاجرتِ ۱ → ۲: افزودنِ ستونِ «نسخهٔ طلایی» (`isPinned`) بدون پاک‌کردنِ داده‌ها.
     *
     * چرا مهم است؟ مرورگری که تمام ارزشش «نگه‌داشتنِ ورود» است، نباید با یک آپدیت نسخه‌های
     * ذخیره‌شده و کانتینرها را از دست بدهد. پیش از این، ارتقای نسخهٔ دیتابیس به
     * `fallbackToDestructiveMigration` می‌رسید و همهٔ جدول‌ها پاک می‌شدند؛ حالا این مهاجرت
     * داده‌های موجود را دست‌نخورده نگه می‌دارد و فقط ستون تازه را با مقدار پیش‌فرض ۰ می‌سازد.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE session_snapshots ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    /** Add the encrypted credential vault without removing any existing session data. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS saved_credentials (" +
                    "id TEXT NOT NULL PRIMARY KEY, encryptedPayloadBase64 TEXT NOT NULL, " +
                    "ivBase64 TEXT NOT NULL, payloadSha256 TEXT NOT NULL, " +
                    "createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)",
            )
        }
    }

    /**
     * مهاجرتِ ۳ → ۴: ستونِ «تعداد بازدید» برای تاریخچه.
     *
     * پیش از این هر بازدیدِ دوباره فقط زمانِ آخرین بازدید را به‌روز می‌کرد و تعداد بازدیدها
     * گم می‌شد؛ در نتیجه «پربازدیدترین‌ها» و پیشنهادهای نوار آدرس نمی‌توانستند بین یک سایتِ
     * ده‌بار بازدیدشده و یک سایتِ تازه تفکیک کنند. مقدار پیش‌فرض ۱ است تا رکوردهای قدیمی
     * معتبر بمانند.
     */
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                "ALTER TABLE history_items ADD COLUMN visitCount INTEGER NOT NULL DEFAULT 1",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MarbleBrDatabase =
        Room.databaseBuilder(
            context,
            MarbleBrDatabase::class.java,
            "marblebr_vault.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            // آخرین سدِ دفاعی برای نسخه‌های نامعلوم/آینده؛ مسیر عادی همین مهاجرت است.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCredentialDao(db: MarbleBrDatabase): CredentialDao = db.credentialDao()

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
