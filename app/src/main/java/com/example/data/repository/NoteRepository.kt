package com.example.data.repository

import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao, private val folderDao: FolderDao) {

    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insert(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun update(note: NoteEntity) = noteDao.updateNote(note)

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun moveToTrashBatch(ids: List<Long>) = noteDao.moveToTrashBatch(ids)

    suspend fun deletePermanentlyBatch(ids: List<Long>) = noteDao.deletePermanentlyBatch(ids)

    suspend fun updateFolderBatch(ids: List<Long>, folder: String) = noteDao.updateFolderBatch(ids, folder)

    suspend fun insertFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)

    suspend fun addTagBatch(ids: List<Long>, tag: String) {
        val trimmedTag = tag.trim().removePrefix("#")
        if (trimmedTag.isBlank()) return
        val notes = noteDao.getNotesByIds(ids)
        for (note in notes) {
            val currentTags = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (!currentTags.contains(trimmedTag)) {
                val newTags = (currentTags + trimmedTag).joinToString(",")
                noteDao.updateNote(note.copy(tags = newTags, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun togglePin(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun toggleArchive(note: NoteEntity) {
        val willBeArchived = !note.isArchived
        noteDao.updateNote(
            note.copy(
                isArchived = willBeArchived,
                isPinned = if (willBeArchived) false else note.isPinned,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveToTrash(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isTrashed = true,
                isPinned = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        noteDao.updateNote(
            note.copy(
                isTrashed = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
