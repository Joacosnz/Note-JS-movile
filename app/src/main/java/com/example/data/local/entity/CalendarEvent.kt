package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad de Room para representar un Evento de Calendario (`CalendarEvent`),
 * incluyendo `id`, `title`, `description`, `timestamp`, `linkedNoteId` (nullable) e `isCompleted`.
 */
@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedNoteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["linkedNoteId"]),
        Index(value = ["timestamp"]),
        Index(value = ["remoteUuid"]),
        Index(value = ["userId", "pendingDelete", "timestamp"])
    ]
)
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "''")
    val description: String = "",
    val timestamp: Long,
    val linkedNoteId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val endTimestamp: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val isAllDay: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val location: String = "",
    @ColumnInfo(defaultValue = "''")
    val remoteUuid: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "'LOCAL_ANONYMOUS'")
    val userId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID,
    @ColumnInfo(defaultValue = "'Personal'")
    val listName: String = "Personal",
    @ColumnInfo(defaultValue = "4278221567")
    val colorHex: Long = 0xFF007AFFL,
    @ColumnInfo(defaultValue = "'NONE'")
    val recurrenceRule: String = RecurrenceRule.NONE.code,
    @ColumnInfo(defaultValue = "''")
    val linkedNoteUuid: String = "",
    @ColumnInfo(defaultValue = "''")
    val linkedNoteTitle: String = "",
    @ColumnInfo(defaultValue = "0")
    val alertOffsetMinutes: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val pendingDelete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val lastModifiedLocal: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val updatedAtServer: Long = 0L
) {
    fun toReminderEntity(): ReminderEntity {
        return ReminderEntity(
            id = id,
            remoteUuid = remoteUuid,
            userId = userId,
            itemType = CalendarItemType.EVENT.code,
            title = title,
            notes = description,
            location = location,
            scheduledAt = timestamp,
            endTimeMillis = if (endTimestamp > timestamp) endTimestamp else timestamp + 3_600_000L,
            isAllDay = isAllDay,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) updatedAt else 0L,
            priority = 0,
            listName = listName,
            colorHex = colorHex,
            recurrenceRule = recurrenceRule,
            subtasksJson = "[]",
            linkedNoteId = linkedNoteId,
            linkedNoteUuid = linkedNoteUuid,
            linkedNoteTitle = linkedNoteTitle,
            alertOffsetMinutes = alertOffsetMinutes,
            isSynced = isSynced,
            pendingDelete = pendingDelete,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastModifiedLocal = lastModifiedLocal,
            updatedAtServer = updatedAtServer
        )
    }

    companion object {
        fun fromReminderEntity(entity: ReminderEntity): CalendarEvent {
            return CalendarEvent(
                id = entity.id,
                title = entity.title,
                description = entity.notes,
                timestamp = entity.scheduledAt,
                linkedNoteId = entity.linkedNoteId,
                isCompleted = entity.isCompleted,
                endTimestamp = entity.effectiveEndMillis,
                isAllDay = entity.isAllDay,
                location = entity.location,
                remoteUuid = entity.remoteUuid,
                userId = entity.userId,
                listName = entity.listName,
                colorHex = entity.colorHex,
                recurrenceRule = entity.recurrenceRule,
                linkedNoteUuid = entity.linkedNoteUuid,
                linkedNoteTitle = entity.linkedNoteTitle,
                alertOffsetMinutes = entity.alertOffsetMinutes,
                isSynced = entity.isSynced,
                pendingDelete = entity.pendingDelete,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                lastModifiedLocal = entity.lastModifiedLocal,
                updatedAtServer = entity.updatedAtServer
            )
        }
    }
}
