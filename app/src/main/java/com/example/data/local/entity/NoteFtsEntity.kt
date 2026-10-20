package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity::class)
data class NoteFtsEntity(
    val title: String,
    val searchableText: String,
)
