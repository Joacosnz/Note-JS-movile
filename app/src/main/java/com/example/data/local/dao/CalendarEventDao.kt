package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Query("""
        SELECT * FROM calendar_events
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY timestamp ASC
    """)
    fun observeCalendarEventsForUser(userId: String): Flow<List<CalendarEvent>>

    @Query("""
        SELECT * FROM calendar_events
        WHERE (userId = :userId OR userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}')
          AND pendingDelete = 0
        ORDER BY timestamp ASC
    """)
    suspend fun getCalendarEventsForUserList(userId: String): List<CalendarEvent>

    @Query("SELECT * FROM calendar_events WHERE pendingDelete = 0 ORDER BY timestamp ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE id = :id LIMIT 1")
    suspend fun getCalendarEventById(id: Long): CalendarEvent?

    @Query("SELECT * FROM calendar_events WHERE remoteUuid = :remoteUuid AND userId = :userId LIMIT 1")
    suspend fun getCalendarEventByRemoteUuidAndUser(remoteUuid: String, userId: String): CalendarEvent?

    @Query("SELECT * FROM calendar_events WHERE remoteUuid = :remoteUuid LIMIT 1")
    suspend fun getCalendarEventByRemoteUuid(remoteUuid: String): CalendarEvent?

    @Query("SELECT * FROM calendar_events WHERE linkedNoteId = :noteId AND pendingDelete = 0 ORDER BY timestamp ASC")
    fun observeCalendarEventsByLinkedNoteId(noteId: Long): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE timestamp BETWEEN :startMillis AND :endMillis AND pendingDelete = 0 ORDER BY timestamp ASC")
    fun observeCalendarEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEvent): Long

    @Update
    suspend fun updateCalendarEvent(event: CalendarEvent)

    @Delete
    suspend fun deleteCalendarEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEventById(id: Long)

    @Query("DELETE FROM calendar_events WHERE remoteUuid = :remoteUuid")
    suspend fun deleteCalendarEventByRemoteUuid(remoteUuid: String)

    @Query("""
        UPDATE calendar_events
        SET pendingDelete = 1,
            isSynced = 0,
            updatedAt = :timestamp,
            lastModifiedLocal = :timestamp
        WHERE id = :id
    """)
    suspend fun markCalendarEventPendingDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT * FROM calendar_events
        WHERE userId = :userId
          AND pendingDelete = 1
    """)
    suspend fun getPendingDeleteCalendarEvents(userId: String): List<CalendarEvent>

    @Query("""
        SELECT * FROM calendar_events
        WHERE userId = :userId
          AND pendingDelete = 0
          AND isSynced = 0
        ORDER BY lastModifiedLocal ASC
    """)
    suspend fun getUnsyncedCalendarEvents(userId: String): List<CalendarEvent>

    @Query("""
        UPDATE calendar_events
        SET userId = :authenticatedUserId,
            isSynced = 0,
            lastModifiedLocal = CASE WHEN lastModifiedLocal > 0 THEN lastModifiedLocal ELSE updatedAt END
        WHERE userId = '${NoteEntity.LOCAL_ANONYMOUS_USER_ID}'
    """)
    suspend fun claimAnonymousCalendarEvents(authenticatedUserId: String)

    @Query("""
        UPDATE calendar_events
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE id = :id
    """)
    suspend fun markCalendarEventSyncedById(id: Long, serverTimestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE calendar_events
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE remoteUuid = :remoteUuid
          AND lastModifiedLocal <= :expectedLocalTimestamp
    """)
    suspend fun markCalendarEventSyncedIfUnchanged(
        remoteUuid: String,
        expectedLocalTimestamp: Long,
        serverTimestamp: Long = System.currentTimeMillis()
    )
}
