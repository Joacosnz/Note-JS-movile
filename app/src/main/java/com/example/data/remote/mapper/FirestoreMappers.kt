package com.example.data.remote.mapper

import com.example.data.local.entity.NoteEntity
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
        "reminderTimestamp" to reminderTimestamp,
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
