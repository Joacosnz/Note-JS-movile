package com.example.data.remote.mapper

import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RecurrenceRule
import com.example.data.local.entity.ReminderEntity
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.util.NoteBlockSerializer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.UUID

/**
 * Mappers de extensión para sincronización con Firebase Firestore.
 */

fun NoteEntity.toFirestoreCreateMap(): Map<String, Any> {
    val ensuredUuid = remoteUuid.ifBlank { UUID.randomUUID().toString() }
    val map = mutableMapOf<String, Any?>(
        "remoteUuid" to ensuredUuid,
        "userId" to userId,
        "title" to title.take(5000),
        "content" to content.take(500000),
        "noteType" to noteType.ifBlank { "TEXT" }.take(50),
        "colorIndex" to colorIndex,
        "paperStyle" to paperStyle.ifBlank { "BLANK" }.take(50),
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "isTrashed" to isTrashed,
        "isLocked" to isLocked,
        "lockPin" to lockPin.take(128),
        "tags" to tags.take(5000),
        "folder" to folder.take(200),
        "checklistData" to checklistData.take(200000),
        "drawingData" to drawingData.take(500000),
        "mathEvaluations" to mathEvaluations.take(100000),
        "pdfPath" to pdfPath.take(2000),
        "pdfAnnotationData" to pdfAnnotationData.take(500000),
        "reminderTimestamp" to reminderTimestamp,
        "isReminderDone" to isReminderDone,
        "lastModifiedLocal" to lastModifiedLocal.coerceAtLeast(0L),
        "remoteUrl" to remoteUrl.take(2000),
        "remoteImageUrl" to remoteImageUrl.take(2000),
        "attachmentsJson" to attachmentsJson.ifBlank { "[]" }.take(200000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return map.filterValues { it != null } as Map<String, Any>
}

fun NoteEntity.toFirestoreUpdateMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any?>(
        "title" to title.take(5000),
        "content" to content.take(500000),
        "noteType" to noteType.ifBlank { "TEXT" }.take(50),
        "colorIndex" to colorIndex,
        "paperStyle" to paperStyle.ifBlank { "BLANK" }.take(50),
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "isTrashed" to isTrashed,
        "isLocked" to isLocked,
        "lockPin" to lockPin.take(128),
        "tags" to tags.take(5000),
        "folder" to folder.take(200),
        "checklistData" to checklistData.take(200000),
        "drawingData" to drawingData.take(500000),
        "mathEvaluations" to mathEvaluations.take(100000),
        "pdfPath" to pdfPath.take(2000),
        "pdfAnnotationData" to pdfAnnotationData.take(500000),
        "reminderTimestamp" to (reminderTimestamp ?: FieldValue.delete()),
        "isReminderDone" to isReminderDone,
        "lastModifiedLocal" to lastModifiedLocal.coerceAtLeast(0L),
        "remoteUrl" to remoteUrl.take(2000),
        "remoteImageUrl" to remoteImageUrl.take(2000),
        "attachmentsJson" to attachmentsJson.ifBlank { "[]" }.take(200000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return map.filterValues { it != null } as Map<String, Any>
}

fun NoteEntity.toFirestoreMap(): Map<String, Any?> {
    val blocks = NoteBlockSerializer.deserialize(content)
    val blocksList = blocks.map { block -> block.toFirestoreBlockMap() }

    return mapOf(
        "remoteUuid" to remoteUuid,
        "userId" to userId,
        "title" to title,
        "content" to content,
        "blocks" to blocksList,
        "noteType" to noteType,
        "colorIndex" to colorIndex,
        "paperStyle" to paperStyle,
        "isPinned" to isPinned,
        "isArchived" to isArchived,
        "isTrashed" to isTrashed,
        "isLocked" to isLocked,
        "lockPin" to lockPin,
        "tags" to tags,
        "folder" to folder,
        "checklistData" to checklistData,
        "drawingData" to drawingData,
        "mathEvaluations" to mathEvaluations,
        "pdfPath" to pdfPath,
        "pdfAnnotationData" to pdfAnnotationData,
        "reminderTimestamp" to reminderTimestamp,
        "isReminderDone" to isReminderDone,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "lastModifiedLocal" to lastModifiedLocal,
        "updatedAtServer" to System.currentTimeMillis(),
        "remoteUrl" to remoteUrl,
        "remoteImageUrl" to remoteImageUrl,
        "attachmentsJson" to attachmentsJson
    )
}

fun NoteBlock.toFirestoreBlockMap(): Map<String, Any?> {
    return when (this) {
        is NoteBlock.Text -> mapOf(
            "type" to "text",
            "id" to id,
            "content" to content,
            "style" to style.name,
            "isCollapsed" to isCollapsed,
            "isIndented" to isIndented
        )
        is NoteBlock.Checklist -> mapOf(
            "type" to "checklist",
            "id" to id,
            "content" to content,
            "isChecked" to isChecked
        )
        is NoteBlock.SimpleList -> mapOf(
            "type" to "simpleList",
            "id" to id,
            "content" to content,
            "isNumbered" to isNumbered,
            "number" to number
        )
        is NoteBlock.CalloutBox -> mapOf(
            "type" to "callout",
            "id" to id,
            "content" to content,
            "backgroundColorHex" to backgroundColorHex
        )
        is NoteBlock.Drawing -> mapOf(
            "type" to "drawing",
            "id" to id,
            "filePath" to filePath,
            "pathData" to pathData
        )
        is NoteBlock.Table -> mapOf(
            "type" to "table",
            "id" to id,
            "rows" to rows,
            "cols" to cols,
            "hasHeaderRow" to hasHeaderRow,
            "hasZebraStripes" to hasZebraStripes
        )
        is NoteBlock.Pdf -> mapOf(
            "type" to "pdf",
            "id" to id,
            "filePath" to filePath,
            "remoteUrl" to remoteUrl,
            "fileName" to fileName,
            "fileSizeFormatted" to fileSizeFormatted,
            "pageCount" to pageCount,
            "currentPageIndex" to currentPageIndex
        )
        is NoteBlock.ImageBlock -> mapOf(
            "type" to "image",
            "id" to id,
            "localUri" to localUri,
            "remoteUrl" to remoteUrl
        )
        is NoteBlock.Image -> mapOf(
            "type" to "image",
            "id" to id,
            "filePath" to filePath,
            "remoteUrl" to remoteUrl,
            "fileName" to fileName
        )
        is AudioBlock -> mapOf(
            "type" to "audio",
            "id" to id,
            "filePath" to filePath,
            "remoteUrl" to remoteUrl,
            "durationMs" to durationMs,
            "transcription" to transcription,
            "isTranscribing" to isTranscribing,
            "isTranscriptionVisible" to isTranscriptionVisible
        )
        is NoteBlock.Math -> mapOf(
            "type" to "math",
            "id" to id,
            "latexCode" to latexCode
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toNoteEntity(fallbackUserId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID): NoteEntity {
    val remoteUuid = this["remoteUuid"] as? String ?: UUID.randomUUID().toString()
    val userId = (this["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId
    val title = this["title"] as? String ?: ""
    val rawContent = this["content"] as? String ?: ""
    val colorIndex = (this["colorIndex"] as? Number)?.toInt() ?: 0
    val paperStyle = this["paperStyle"] as? String ?: "BLANK"
    val noteType = this["noteType"] as? String ?: "TEXT"
    val isPinned = this["isPinned"] as? Boolean ?: false
    val isArchived = this["isArchived"] as? Boolean ?: false
    val isTrashed = this["isTrashed"] as? Boolean ?: false
    val isLocked = this["isLocked"] as? Boolean ?: false
    val lockPin = this["lockPin"] as? String ?: ""
    val tags = this["tags"] as? String ?: ""
    val folder = this["folder"] as? String ?: "Notas"
    val checklistData = this["checklistData"] as? String ?: ""
    val drawingData = this["drawingData"] as? String ?: ""
    val mathEvaluations = this["mathEvaluations"] as? String ?: ""
    val pdfPath = this["pdfPath"] as? String ?: ""
    val pdfAnnotationData = this["pdfAnnotationData"] as? String ?: ""
    val reminderTimestamp = (this["reminderTimestamp"] as? Number)?.toLong()
    val isReminderDone = this["isReminderDone"] as? Boolean ?: false

    val createdAt = (this["createdAt"] as? Timestamp)?.toDate()?.time
        ?: (this["createdAt"] as? Number)?.toLong()
        ?: System.currentTimeMillis()
    val serverUpdatedAt = (this["updatedAt"] as? Timestamp)?.toDate()?.time
        ?: (this["updatedAtServer"] as? Number)?.toLong()
        ?: (this["updatedAt"] as? Number)?.toLong()
        ?: System.currentTimeMillis()
    val lastModifiedLocal = (this["lastModifiedLocal"] as? Number)?.toLong() ?: serverUpdatedAt
    val updatedAt = maxOf(lastModifiedLocal, serverUpdatedAt)

    val remoteUrl = this["remoteUrl"] as? String ?: ""
    val remoteImageUrl = this["remoteImageUrl"] as? String ?: ""
    val attachmentsJson = this["attachmentsJson"] as? String ?: "[]"

    return NoteEntity(
        remoteUuid = remoteUuid,
        userId = userId,
        isSynced = true,
        pendingDelete = false,
        lastModifiedLocal = lastModifiedLocal,
        updatedAtServer = serverUpdatedAt,
        title = title,
        content = rawContent,
        noteType = noteType,
        colorIndex = colorIndex,
        paperStyle = paperStyle,
        isPinned = isPinned,
        isArchived = isArchived,
        isTrashed = isTrashed,
        isLocked = isLocked,
        lockPin = lockPin,
        tags = tags,
        folder = folder,
        checklistData = checklistData,
        drawingData = drawingData,
        mathEvaluations = mathEvaluations,
        pdfPath = pdfPath,
        pdfAnnotationData = pdfAnnotationData,
        reminderTimestamp = reminderTimestamp,
        isReminderDone = isReminderDone,
        createdAt = createdAt,
        updatedAt = updatedAt,
        remoteUrl = remoteUrl,
        remoteImageUrl = remoteImageUrl,
        attachmentsJson = attachmentsJson
    )
}

// --- MAPPERS PARA RECORDATORIOS Y EVENTOS DE CALENDARIO ---

fun ReminderEntity.toFirestoreCreateMap(): Map<String, Any> {
    val ensuredUuid = remoteUuid.ifBlank { UUID.randomUUID().toString() }
    val normalizedType = if (itemType.equals(CalendarItemType.EVENT.code, ignoreCase = true)) {
        CalendarItemType.EVENT.code
    } else {
        CalendarItemType.REMINDER.code
    }
    val safeTitle = title.trim().ifBlank { "Recordatorio" }.take(2000)
    val safeListName = listName.trim().ifBlank { "Personal" }.take(200)
    val safeRecurrence = recurrenceRule.trim().ifBlank { RecurrenceRule.NONE.code }.take(100)

    val map = mutableMapOf<String, Any?>(
        "remoteUuid" to ensuredUuid,
        "userId" to userId,
        "itemType" to normalizedType,
        "title" to safeTitle,
        "notes" to notes.take(50000),
        "location" to location.take(2000),
        "scheduledAt" to scheduledAt.coerceAtLeast(0L),
        "endTimeMillis" to endTimeMillis.coerceAtLeast(0L),
        "isAllDay" to isAllDay,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt.coerceAtLeast(0L),
        "priority" to priority.coerceIn(0, 3),
        "listName" to safeListName,
        "colorHex" to colorHex,
        "recurrenceRule" to safeRecurrence,
        "subtasksJson" to subtasksJson.ifBlank { "[]" }.take(100000),
        "linkedNoteId" to linkedNoteId,
        "linkedNoteUuid" to linkedNoteUuid.take(128),
        "linkedNoteTitle" to linkedNoteTitle.take(5000),
        "alertOffsetMinutes" to alertOffsetMinutes.coerceIn(-1, 43200),
        "lastModifiedLocal" to lastModifiedLocal.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return map.filterValues { it != null } as Map<String, Any>
}

fun ReminderEntity.toFirestoreUpdateMap(): Map<String, Any> {
    val normalizedType = if (itemType.equals(CalendarItemType.EVENT.code, ignoreCase = true)) {
        CalendarItemType.EVENT.code
    } else {
        CalendarItemType.REMINDER.code
    }
    val safeTitle = title.trim().ifBlank { "Recordatorio" }.take(2000)
    val safeListName = listName.trim().ifBlank { "Personal" }.take(200)
    val safeRecurrence = recurrenceRule.trim().ifBlank { RecurrenceRule.NONE.code }.take(100)

    val map = mutableMapOf<String, Any?>(
        "itemType" to normalizedType,
        "title" to safeTitle,
        "notes" to notes.take(50000),
        "location" to location.take(2000),
        "scheduledAt" to scheduledAt.coerceAtLeast(0L),
        "endTimeMillis" to endTimeMillis.coerceAtLeast(0L),
        "isAllDay" to isAllDay,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt.coerceAtLeast(0L),
        "priority" to priority.coerceIn(0, 3),
        "listName" to safeListName,
        "colorHex" to colorHex,
        "recurrenceRule" to safeRecurrence,
        "subtasksJson" to subtasksJson.ifBlank { "[]" }.take(100000),
        "linkedNoteId" to (linkedNoteId ?: FieldValue.delete()),
        "linkedNoteUuid" to linkedNoteUuid.take(128),
        "linkedNoteTitle" to linkedNoteTitle.take(5000),
        "alertOffsetMinutes" to alertOffsetMinutes.coerceIn(-1, 43200),
        "lastModifiedLocal" to lastModifiedLocal.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return map.filterValues { it != null } as Map<String, Any>
}

fun ReminderEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "remoteUuid" to remoteUuid,
        "userId" to userId,
        "itemType" to itemType,
        "title" to title,
        "notes" to notes,
        "location" to location,
        "scheduledAt" to scheduledAt,
        "endTimeMillis" to endTimeMillis,
        "isAllDay" to isAllDay,
        "isCompleted" to isCompleted,
        "completedAt" to completedAt,
        "priority" to priority,
        "listName" to listName,
        "colorHex" to colorHex,
        "recurrenceRule" to recurrenceRule,
        "subtasksJson" to subtasksJson,
        "linkedNoteId" to linkedNoteId,
        "linkedNoteUuid" to linkedNoteUuid,
        "linkedNoteTitle" to linkedNoteTitle,
        "alertOffsetMinutes" to alertOffsetMinutes,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "lastModifiedLocal" to lastModifiedLocal,
        "updatedAtServer" to System.currentTimeMillis()
    )
}

fun Map<String, Any?>.toReminderEntity(
    fallbackUserId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID
): ReminderEntity {
    val remoteUuid = this["remoteUuid"] as? String ?: UUID.randomUUID().toString()
    val userId = (this["userId"] as? String)?.takeIf { it.isNotBlank() } ?: fallbackUserId
    val itemType = (this["itemType"] as? String)?.takeIf { it.isNotBlank() } ?: CalendarItemType.REMINDER.code
    val title = (this["title"] as? String)?.takeIf { it.isNotBlank() } ?: "Recordatorio"
    val notes = this["notes"] as? String ?: ""
    val location = this["location"] as? String ?: ""
    val scheduledAt = (this["scheduledAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    val endTimeMillis = (this["endTimeMillis"] as? Number)?.toLong() ?: 0L
    val isAllDay = this["isAllDay"] as? Boolean ?: false
    val isCompleted = this["isCompleted"] as? Boolean ?: false
    val completedAt = (this["completedAt"] as? Number)?.toLong() ?: 0L
    val priority = ((this["priority"] as? Number)?.toInt() ?: 0).coerceIn(0, 3)
    val listName = (this["listName"] as? String)?.takeIf { it.isNotBlank() } ?: "Personal"
    val colorHex = (this["colorHex"] as? Number)?.toLong() ?: 0xFF007AFFL
    val recurrenceRule = (this["recurrenceRule"] as? String)?.takeIf { it.isNotBlank() } ?: RecurrenceRule.NONE.code
    val subtasksJson = (this["subtasksJson"] as? String)?.takeIf { it.isNotBlank() } ?: "[]"
    val linkedNoteId = (this["linkedNoteId"] as? Number)?.toLong()
    val linkedNoteUuid = this["linkedNoteUuid"] as? String ?: ""
    val linkedNoteTitle = this["linkedNoteTitle"] as? String ?: ""
    val alertOffsetMinutes = ((this["alertOffsetMinutes"] as? Number)?.toInt() ?: 0).coerceIn(-1, 43200)

    val createdAt = (this["createdAt"] as? Timestamp)?.toDate()?.time
        ?: (this["createdAt"] as? Number)?.toLong()
        ?: System.currentTimeMillis()
    val serverUpdatedAt = (this["updatedAt"] as? Timestamp)?.toDate()?.time
        ?: (this["updatedAtServer"] as? Number)?.toLong()
        ?: (this["updatedAt"] as? Number)?.toLong()
        ?: System.currentTimeMillis()
    val lastModifiedLocal = (this["lastModifiedLocal"] as? Number)?.toLong() ?: serverUpdatedAt
    val updatedAt = maxOf(lastModifiedLocal, serverUpdatedAt)

    return ReminderEntity(
        remoteUuid = remoteUuid,
        userId = userId,
        itemType = itemType,
        title = title,
        notes = notes,
        location = location,
        scheduledAt = scheduledAt,
        endTimeMillis = endTimeMillis,
        isAllDay = isAllDay,
        isCompleted = isCompleted,
        completedAt = completedAt,
        priority = priority,
        listName = listName,
        colorHex = colorHex,
        recurrenceRule = recurrenceRule,
        subtasksJson = subtasksJson,
        linkedNoteId = linkedNoteId,
        linkedNoteUuid = linkedNoteUuid,
        linkedNoteTitle = linkedNoteTitle,
        alertOffsetMinutes = alertOffsetMinutes,
        isSynced = true,
        pendingDelete = false,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastModifiedLocal = lastModifiedLocal,
        updatedAtServer = serverUpdatedAt
    )
}

fun com.example.data.local.entity.Reminder.toFirestoreCreateMap(): Map<String, Any> =
    toReminderEntity().toFirestoreCreateMap()

fun com.example.data.local.entity.Reminder.toFirestoreUpdateMap(): Map<String, Any> =
    toReminderEntity().toFirestoreUpdateMap()

fun com.example.data.local.entity.Reminder.toFirestoreMap(): Map<String, Any?> =
    toReminderEntity().toFirestoreMap()

fun Map<String, Any?>.toReminder(
    fallbackUserId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID
): com.example.data.local.entity.Reminder =
    com.example.data.local.entity.Reminder.fromReminderEntity(toReminderEntity(fallbackUserId))

fun com.example.data.local.entity.CalendarEvent.toFirestoreCreateMap(): Map<String, Any> =
    toReminderEntity().toFirestoreCreateMap()

fun com.example.data.local.entity.CalendarEvent.toFirestoreUpdateMap(): Map<String, Any> =
    toReminderEntity().toFirestoreUpdateMap()

fun com.example.data.local.entity.CalendarEvent.toFirestoreMap(): Map<String, Any?> =
    toReminderEntity().toFirestoreMap()

fun Map<String, Any?>.toCalendarEvent(
    fallbackUserId: String = NoteEntity.LOCAL_ANONYMOUS_USER_ID
): com.example.data.local.entity.CalendarEvent =
    com.example.data.local.entity.CalendarEvent.fromReminderEntity(toReminderEntity(fallbackUserId))

