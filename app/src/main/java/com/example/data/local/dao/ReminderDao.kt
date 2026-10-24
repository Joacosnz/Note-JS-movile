package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // --- Entidad Reminder (tabla `reminder_items`) ---

    @Query("""
        SELECT * FROM reminder_items
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY timestamp ASC, priority DESC
    """)
    fun observeReminderItemsForUser(userId: String): Flow<List<Reminder>>

    @Query("""
        SELECT * FROM reminder_items
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY timestamp ASC, priority DESC
    """)
    suspend fun getReminderItemsForUserList(userId: String): List<Reminder>

    @Query("SELECT * FROM reminder_items WHERE pendingDelete = 0 ORDER BY timestamp ASC")
    fun getAllReminderItems(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminder_items WHERE id = :id LIMIT 1")
    suspend fun getReminderItemById(id: Long): Reminder?

    @Query("SELECT * FROM reminder_items WHERE remoteUuid = :remoteUuid AND userId = :userId LIMIT 1")
    suspend fun getReminderItemByRemoteUuidAndUser(remoteUuid: String, userId: String): Reminder?

    @Query("SELECT * FROM reminder_items WHERE remoteUuid = :remoteUuid LIMIT 1")
    suspend fun getReminderItemByRemoteUuid(remoteUuid: String): Reminder?

    @Query("SELECT * FROM reminder_items WHERE linkedNoteId = :noteId AND pendingDelete = 0 ORDER BY timestamp ASC")
    fun observeReminderItemsByLinkedNoteId(noteId: Long): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderItem(reminder: Reminder): Long

    @Update
    suspend fun updateReminderItem(reminder: Reminder)

    @Delete
    suspend fun deleteReminderItem(reminder: Reminder)

    @Query("DELETE FROM reminder_items WHERE id = :id")
    suspend fun deleteReminderItemById(id: Long)

    @Query("DELETE FROM reminder_items WHERE remoteUuid = :remoteUuid")
    suspend fun deleteReminderItemByRemoteUuid(remoteUuid: String)

    @Query("""
        UPDATE reminder_items
        SET pendingDelete = 1,
            isSynced = 0,
            updatedAt = :timestamp,
            lastModifiedLocal = :timestamp
        WHERE id = :id
    """)
    suspend fun markReminderItemPendingDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM reminder_items
        WHERE userId = :userId
          AND pendingDelete = 1
    """)
    suspend fun getPendingDeleteReminderItems(userId: String): List<Reminder>

    @Query("""
        SELECT * FROM reminder_items
        WHERE userId = :userId
          AND pendingDelete = 0
          AND isSynced = 0
        ORDER BY lastModifiedLocal ASC
    """)
    suspend fun getUnsyncedReminderItems(userId: String): List<Reminder>

    @Query("""
        UPDATE reminder_items
        SET userId = :authenticatedUserId,
            isSynced = 0,
            lastModifiedLocal = CASE WHEN lastModifiedLocal > 0 THEN lastModifiedLocal ELSE updatedAt END
        WHERE userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}'
    """)
    suspend fun claimAnonymousReminderItems(authenticatedUserId: String)

    @Query("""
        UPDATE reminder_items
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE id = :id
    """)
    suspend fun markReminderItemSyncedById(id: Long, serverTimestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE reminder_items
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE remoteUuid = :remoteUuid
          AND lastModifiedLocal <= :expectedLocalTimestamp
    """)
    suspend fun markReminderItemSyncedIfUnchanged(
        remoteUuid: String,
        expectedLocalTimestamp: Long,
        serverTimestamp: Long = System.currentTimeMillis()
    )

    // --- Entidad unificada ReminderEntity (tabla `reminders`) ---

    @Query("""
        SELECT * FROM reminders
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY scheduledAt ASC, priority DESC
    """)
    fun observeRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Query("""
        SELECT * FROM reminders
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY scheduledAt ASC, priority DESC
    """)
    suspend fun getRemindersForUserList(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE remoteUuid = :remoteUuid AND userId = :userId LIMIT 1")
    suspend fun getReminderByRemoteUuidAndUser(remoteUuid: String, userId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE remoteUuid = :remoteUuid LIMIT 1")
    suspend fun getReminderByRemoteUuid(remoteUuid: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE linkedNoteId = :noteId AND pendingDelete = 0 ORDER BY scheduledAt ASC")
    suspend fun getRemindersByLinkedNoteId(noteId: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("""
        UPDATE reminders
        SET pendingDelete = 1,
            isSynced = 0,
            updatedAt = :timestamp,
            lastModifiedLocal = :timestamp
        WHERE id = :id
    """)
    suspend fun markReminderPendingDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM reminders
        WHERE userId = :userId
          AND pendingDelete = 1
    """)
    suspend fun getPendingDeleteReminders(userId: String): List<ReminderEntity>

    @Query("""
        SELECT * FROM reminders
        WHERE userId = :userId
          AND pendingDelete = 0
          AND isSynced = 0
        ORDER BY lastModifiedLocal ASC
    """)
    suspend fun getUnsyncedReminders(userId: String): List<ReminderEntity>

    @Query("""
        UPDATE reminders
        SET userId = :authenticatedUserId,
            isSynced = 0,
            lastModifiedLocal = CASE WHEN lastModifiedLocal > 0 THEN lastModifiedLocal ELSE updatedAt END
        WHERE userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}'
    """)
    suspend fun claimAnonymousReminders(authenticatedUserId: String)

    @Query("""
        UPDATE reminders
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE id = :id
    """)
    suspend fun markReminderSyncedById(id: Long, serverTimestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE reminders
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE remoteUuid = :remoteUuid
          AND lastModifiedLocal <= :expectedLocalTimestamp
    """)
    suspend fun markReminderSyncedIfUnchanged(
        remoteUuid: String,
        expectedLocalTimestamp: Long,
        serverTimestamp: Long = System.currentTimeMillis()
    )
}
