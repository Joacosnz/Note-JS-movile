package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.dao.WebLinkCacheDao
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NoteFtsEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.local.entity.WebLinkCacheEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteFtsEntity::class,
        FolderEntity::class,
        SignatureEntity::class,
        WebLinkCacheEntity::class
    ],
    version = 15,
    exportSchema = false,
)
@TypeConverters(UriConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun signatureDao(): SignatureDao
    abstract fun webLinkCacheDao(): WebLinkCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pdfExtractedText TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `signatures` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pointsJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN remoteUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN updatedAtServer INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE notes 
                    SET content = '[' || '{"type":"text","id":"legacy_' || id || '","content":' || quote(content) || ',"style":"BODY"}' || ']'
                    WHERE content IS NOT NULL 
                      AND content != '' 
                      AND content NOT LIKE '[%'
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN remoteUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN searchableText TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING fts4(`title` TEXT NOT NULL, `searchableText` TEXT NOT NULL, content=`notes`)")
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES('rebuild')")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN syncEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT NOT NULL DEFAULT 'LOCAL_ANONYMOUS'")
                db.execSQL("ALTER TABLE notes ADD COLUMN pendingDelete INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN lastModifiedLocal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN remoteImageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE notes ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("UPDATE notes SET lastModifiedLocal = updatedAt WHERE lastModifiedLocal = 0")
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES('rebuild')")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `web_link_cache` (
                        `url` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `localImagePath` TEXT,
                        `siteName` TEXT NOT NULL,
                        `faviconUrl` TEXT NOT NULL,
                        `localFaviconPath` TEXT,
                        `articleExcerpt` TEXT NOT NULL,
                        `displayMode` TEXT NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        `isLoadedFromRemote` INTEGER NOT NULL,
                        PRIMARY KEY(`url`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auranotes_v3.db"
                )
                    .addMigrations(
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.let {
                    if (it.isOpen) {
                        it.close()
                    }
                    INSTANCE = null
                }
            }
        }
    }
}
