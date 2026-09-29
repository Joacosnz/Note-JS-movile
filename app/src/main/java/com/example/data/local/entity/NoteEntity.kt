package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val reminderTimestamp: Long? = null,
    val isReminderDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
