package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 20")
    suspend fun getActiveNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveNote(): NoteEntity?

    @Query("SELECT * FROM notes WHERE isTrashed = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE (content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR pdfExtractedText LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') AND isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT notes.* FROM notes JOIN notes_fts ON notes.id = notes_fts.rowid WHERE notes_fts MATCH :searchQuery")
    suspend fun searchNotesFts(searchQuery: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remoteUuid = :remoteUuid LIMIT 1")
    suspend fun getNoteByRemoteUuid(remoteUuid: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    suspend fun getNotesByIds(ids: List<Long>): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET isTrashed = 1, isPinned = 0 WHERE id IN (:ids)")
    suspend fun moveToTrashBatch(ids: List<Long>)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deletePermanentlyBatch(ids: List<Long>)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("UPDATE notes SET folder = :newName WHERE LOWER(folder) = LOWER(:oldName)")
    suspend fun renameFolderInNotes(oldName: String, newName: String)

    @Query("UPDATE notes SET folder = '' WHERE LOWER(folder) = LOWER(:folderName)")
    suspend fun clearFolderInNotes(folderName: String)

    @Query("UPDATE notes SET folder = :folder WHERE id IN (:ids)")
    suspend fun updateFolderBatch(ids: List<Long>, folder: String)

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()
}
