package com.example.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MediaType {
    IMAGE,
    PDF,
    AUDIO
}

data class NoteMediaAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: MediaType = MediaType.IMAGE,
    val displayName: String = "",
    val mimeType: String = "application/octet-stream",
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val storagePath: String? = null,
    val sizeBytes: Long = 0L,
    val pageCount: Int = 0
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteUuid: String = UUID.randomUUID().toString(),
    val userId: String = LOCAL_ANONYMOUS_USER_ID, // Aislamiento multi-cuenta por UID de Firebase Auth
    val isSynced: Boolean = false,
    val pendingDelete: Boolean = false, // Soft-delete offline para sincronizar borrados con Firestore
    val lastModifiedLocal: Long = System.currentTimeMillis(),
    val updatedAtServer: Long = 0L,
    val title: String = "",
    val content: String = "",
    val noteType: String = "TEXT", // TEXT, CHECKLIST, DRAWING, MATH
    val colorIndex: Int = 0,
    val paperStyle: String = "BLANK", // BLANK, RULED, GRID, DOTS
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isLocked: Boolean = false,
    val lockPin: String = "",
    val tags: String = "", // Comma-separated tags e.g. "Ideas,Trabajo"
    val folder: String = "Notas", // Folder name e.g. "Notas", "Personal", etc.
    val checklistData: String = "", // JSON representation of items
    val drawingData: String = "", // JSON representation of stroke paths
    val mathEvaluations: String = "", // Results evaluated
    val pdfPath: String = "",           // Ruta del archivo PDF en el almacenamiento interno privado
    val pdfAnnotationData: String = "", // JSON con los trazos, resaltados y firmas por página
    val reminderTimestamp: Long? = null,
    val isReminderDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val imageUri: Uri? = null, // URI local de imagen adjunta (Photo Picker)
    val remoteImageUrl: String = "", // URL remota de la imagen principal en Firebase Storage
    val pdfExtractedText: String? = null, // Para Búsqueda Profunda (Deep Search) en PDFs
    val remoteUrl: String = "", // URL remota del archivo PDF principal en Firebase Cloud Storage
    val attachmentsJson: String = "[]", // Lista serializada de NoteMediaAttachment (URI local + URL remota)
    val searchableText: String = "", // Texto aplanado centralizado para FTS4
    val syncEnabled: Boolean = true
) {
    companion object {
        const val LOCAL_ANONYMOUS_USER_ID = "LOCAL_ANONYMOUS"
    }
}

/**
 * Typealias to allow referencing 'Note' objects directly as Room entities
 * for offline persistence of titles, content, and metadata.
 */
typealias Note = NoteEntity

