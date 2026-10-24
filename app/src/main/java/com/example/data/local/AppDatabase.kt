package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CalendarEventDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.dao.WebLinkCacheDao
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NoteFtsEntity
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.local.entity.WebLinkCacheEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteFtsEntity::class,
        FolderEntity::class,
        SignatureEntity::class,
        WebLinkCacheEntity::class,
        ReminderEntity::class,
        Reminder::class,
        CalendarEvent::class
    ],
    version = 17,
    exportSchema = false,
)
@TypeConverters(UriConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun signatureDao(): SignatureDao
    abstract fun webLinkCacheDao(): WebLinkCacheDao
    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventDao(): CalendarEventDao

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

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `remoteUuid` TEXT NOT NULL DEFAULT '',
                        `userId` TEXT NOT NULL DEFAULT 'LOCAL_ANONYMOUS',
                        `itemType` TEXT NOT NULL DEFAULT 'REMINDER',
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `location` TEXT NOT NULL DEFAULT '',
                        `scheduledAt` INTEGER NOT NULL,
                        `endTimeMillis` INTEGER NOT NULL DEFAULT 0,
                        `isAllDay` INTEGER NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER NOT NULL DEFAULT 0,
                        `priority` INTEGER NOT NULL DEFAULT 0,
                        `listName` TEXT NOT NULL DEFAULT 'Personal',
                        `colorHex` INTEGER NOT NULL DEFAULT 4278221567,
                        `recurrenceRule` TEXT NOT NULL DEFAULT 'NONE',
                        `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                        `linkedNoteId` INTEGER,
                        `linkedNoteUuid` TEXT NOT NULL DEFAULT '',
                        `linkedNoteTitle` TEXT NOT NULL DEFAULT '',
                        `alertOffsetMinutes` INTEGER NOT NULL DEFAULT 0,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `pendingDelete` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastModifiedLocal` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtServer` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_remoteUuid` ON `reminders` (`remoteUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_userId_pendingDelete_scheduledAt` ON `reminders` (`userId`, `pendingDelete`, `scheduledAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linkedNoteId` ON `reminders` (`linkedNoteId`)")

                // Migrate existing note-level reminders into the standalone reminders table linked to their note
                db.execSQL(
                    """
                    INSERT INTO `reminders` (
                        `remoteUuid`, `userId`, `itemType`, `title`, `notes`, `location`,
                        `scheduledAt`, `endTimeMillis`, `isAllDay`, `isCompleted`, `completedAt`,
                        `priority`, `listName`, `colorHex`, `recurrenceRule`, `subtasksJson`,
                        `linkedNoteId`, `linkedNoteUuid`, `linkedNoteTitle`, `alertOffsetMinutes`,
                        `isSynced`, `pendingDelete`, `createdAt`, `updatedAt`, `lastModifiedLocal`, `updatedAtServer`
                    )
                    SELECT
                        'rem_note_' || `id` || '_' || COALESCE(NULLIF(`remoteUuid`, ''), hex(randomblob(6))),
                        `userId`,
                        'REMINDER',
                        CASE WHEN TRIM(`title`) = '' THEN 'Recordatorio de nota' ELSE `title` END,
                        '',
                        '',
                        `reminderTimestamp`,
                        0,
                        0,
                        `isReminderDone`,
                        CASE WHEN `isReminderDone` = 1 THEN `updatedAt` ELSE 0 END,
                        0,
                        CASE WHEN TRIM(`folder`) = '' OR `folder` = 'Notas' THEN 'Personal' ELSE `folder` END,
                        4278221567,
                        'NONE',
                        '[]',
                        `id`,
                        `remoteUuid`,
                        CASE WHEN TRIM(`title`) = '' THEN 'Nota vinculada' ELSE `title` END,
                        0,
                        0,
                        0,
                        `createdAt`,
                        `updatedAt`,
                        `lastModifiedLocal`,
                        0
                    FROM `notes`
                    WHERE `reminderTimestamp` IS NOT NULL
                      AND `isTrashed` = 0
                      AND `pendingDelete` = 0
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminder_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL,
                        `linkedNoteId` INTEGER,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `remoteUuid` TEXT NOT NULL DEFAULT '',
                        `userId` TEXT NOT NULL DEFAULT 'LOCAL_ANONYMOUS',
                        `priority` INTEGER NOT NULL DEFAULT 0,
                        `listName` TEXT NOT NULL DEFAULT 'Personal',
                        `colorHex` INTEGER NOT NULL DEFAULT 4278221567,
                        `recurrenceRule` TEXT NOT NULL DEFAULT 'NONE',
                        `subtasksJson` TEXT NOT NULL DEFAULT '[]',
                        `linkedNoteUuid` TEXT NOT NULL DEFAULT '',
                        `linkedNoteTitle` TEXT NOT NULL DEFAULT '',
                        `alertOffsetMinutes` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER NOT NULL DEFAULT 0,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `pendingDelete` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastModifiedLocal` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtServer` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`linkedNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_items_linkedNoteId` ON `reminder_items` (`linkedNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_items_timestamp` ON `reminder_items` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_items_remoteUuid` ON `reminder_items` (`remoteUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_items_userId_pendingDelete_timestamp` ON `reminder_items` (`userId`, `pendingDelete`, `timestamp`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `calendar_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `timestamp` INTEGER NOT NULL,
                        `linkedNoteId` INTEGER,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `endTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `isAllDay` INTEGER NOT NULL DEFAULT 0,
                        `location` TEXT NOT NULL DEFAULT '',
                        `remoteUuid` TEXT NOT NULL DEFAULT '',
                        `userId` TEXT NOT NULL DEFAULT 'LOCAL_ANONYMOUS',
                        `listName` TEXT NOT NULL DEFAULT 'Personal',
                        `colorHex` INTEGER NOT NULL DEFAULT 4278221567,
                        `recurrenceRule` TEXT NOT NULL DEFAULT 'NONE',
                        `linkedNoteUuid` TEXT NOT NULL DEFAULT '',
                        `linkedNoteTitle` TEXT NOT NULL DEFAULT '',
                        `alertOffsetMinutes` INTEGER NOT NULL DEFAULT 0,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `pendingDelete` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastModifiedLocal` INTEGER NOT NULL DEFAULT 0,
                        `updatedAtServer` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`linkedNoteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_linkedNoteId` ON `calendar_events` (`linkedNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_timestamp` ON `calendar_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_remoteUuid` ON `calendar_events` (`remoteUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_events_userId_pendingDelete_timestamp` ON `calendar_events` (`userId`, `pendingDelete`, `timestamp`)")

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `reminder_items` (
                        `id`, `title`, `description`, `timestamp`, `linkedNoteId`, `isCompleted`,
                        `remoteUuid`, `userId`, `priority`, `listName`, `colorHex`, `recurrenceRule`,
                        `subtasksJson`, `linkedNoteUuid`, `linkedNoteTitle`, `alertOffsetMinutes`,
                        `completedAt`, `isSynced`, `pendingDelete`, `createdAt`, `updatedAt`,
                        `lastModifiedLocal`, `updatedAtServer`
                    )
                    SELECT
                        `id`, `title`, `notes`, `scheduledAt`,
                        CASE WHEN `linkedNoteId` IN (SELECT `id` FROM `notes`) THEN `linkedNoteId` ELSE NULL END,
                        `isCompleted`, `remoteUuid`, `userId`, `priority`, `listName`, `colorHex`,
                        `recurrenceRule`, `subtasksJson`, `linkedNoteUuid`, `linkedNoteTitle`,
                        `alertOffsetMinutes`, `completedAt`, `isSynced`, `pendingDelete`,
                        `createdAt`, `updatedAt`, `lastModifiedLocal`, `updatedAtServer`
                    FROM `reminders`
                    WHERE UPPER(`itemType`) != 'EVENT'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `calendar_events` (
                        `id`, `title`, `description`, `timestamp`, `linkedNoteId`, `isCompleted`,
                        `endTimestamp`, `isAllDay`, `location`, `remoteUuid`, `userId`, `listName`,
                        `colorHex`, `recurrenceRule`, `linkedNoteUuid`, `linkedNoteTitle`,
                        `alertOffsetMinutes`, `isSynced`, `pendingDelete`, `createdAt`, `updatedAt`,
                        `lastModifiedLocal`, `updatedAtServer`
                    )
                    SELECT
                        `id`, `title`, `notes`, `scheduledAt`,
                        CASE WHEN `linkedNoteId` IN (SELECT `id` FROM `notes`) THEN `linkedNoteId` ELSE NULL END,
                        `isCompleted`, `endTimeMillis`, `isAllDay`, `location`, `remoteUuid`, `userId`,
                        `listName`, `colorHex`, `recurrenceRule`, `linkedNoteUuid`, `linkedNoteTitle`,
                        `alertOffsetMinutes`, `isSynced`, `pendingDelete`, `createdAt`, `updatedAt`,
                        `lastModifiedLocal`, `updatedAtServer`
                    FROM `reminders`
                    WHERE UPPER(`itemType`) = 'EVENT'
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
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17
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
