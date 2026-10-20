package com.example.data.local.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteUuid: String = UUID.randomUUID().toString(),
    val isSynced: Boolean = false,
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
    val imageUri: Uri? = null, // <- Añadido para guardar el URI de Photo Picker
    val pdfExtractedText: String? = null, // <- Para Búsqueda Profunda (Deep Search) en PDFs
    val remoteUrl: String = "", // <- URL remota del archivo adjunto en Firebase Cloud Storage
    val searchableText: String = "", // <- Texto aplanado centralizado para FTS4
    val syncEnabled: Boolean = true
)
