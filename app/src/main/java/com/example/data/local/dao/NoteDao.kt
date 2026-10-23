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
    // --- CONSULTAS REACTIVAS CON AISLAMIENTO POR UID (OFFLINE-FIRST) ---

    @Query("""
        SELECT * FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND pendingDelete = 0
          AND isTrashed = 0
          AND isArchived = 0
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getActiveNotesForUser(userId: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND pendingDelete = 0
          AND isTrashed = 0
          AND isArchived = 1
        ORDER BY updatedAt DESC
    """)
    fun getArchivedNotesForUser(userId: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND pendingDelete = 0
          AND isTrashed = 1
        ORDER BY updatedAt DESC
    """)
    fun getTrashedNotesForUser(userId: String): Flow<List<NoteEntity>>

    // --- CONSULTAS GENERALES (COMPATIBILIDAD) ---

    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 20")
    suspend fun getActiveNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 1")
    suspend fun getLatestActiveNote(): NoteEntity?

    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND isTrashed = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE (content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR pdfExtractedText LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') AND isTrashed = 0 AND pendingDelete = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT notes.* FROM notes JOIN notes_fts ON notes.id = notes_fts.rowid WHERE notes_fts MATCH :searchQuery AND notes.pendingDelete = 0")
    suspend fun searchNotesFts(searchQuery: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE pendingDelete = 0 AND isTrashed = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE remoteUuid = :remoteUuid LIMIT 1")
    suspend fun getNoteByRemoteUuid(remoteUuid: String): NoteEntity?

    @Query("""
        SELECT * FROM notes
        WHERE remoteUuid = :remoteUuid
          AND (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
        LIMIT 1
    """)
    suspend fun getNoteByRemoteUuidAndUser(remoteUuid: String, userId: String): NoteEntity?

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

    @Query("UPDATE notes SET isTrashed = 1, isPinned = 0, isSynced = 0, updatedAt = :timestamp, lastModifiedLocal = :timestamp WHERE id IN (:ids)")
    suspend fun moveToTrashBatch(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deletePermanentlyBatch(ids: List<Long>)

    @Query("SELECT * FROM notes WHERE pendingDelete = 0")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS') AND pendingDelete = 0")
    suspend fun getAllNotesForUserList(userId: String): List<NoteEntity>

    @Query("UPDATE notes SET folder = :newName, isSynced = 0, updatedAt = :timestamp, lastModifiedLocal = :timestamp WHERE LOWER(folder) = LOWER(:oldName)")
    suspend fun renameFolderInNotes(oldName: String, newName: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folder = '', isSynced = 0, updatedAt = :timestamp, lastModifiedLocal = :timestamp WHERE LOWER(folder) = LOWER(:folderName)")
    suspend fun clearFolderInNotes(folderName: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folder = :folder, isSynced = 0, updatedAt = :timestamp, lastModifiedLocal = :timestamp WHERE id IN (:ids)")
    suspend fun updateFolderBatch(ids: List<Long>, folder: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET colorIndex = :colorIndex, isSynced = 0, updatedAt = :updatedAt, lastModifiedLocal = :updatedAt WHERE id IN (:ids)")
    suspend fun updateColorBatch(ids: List<Long>, colorIndex: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET colorIndex = :colorIndex, isSynced = 0, updatedAt = :updatedAt, lastModifiedLocal = :updatedAt WHERE id = :id")
    suspend fun updateNoteColor(id: Long, colorIndex: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // --- COLA DE SINCRONIZACIÓN OFFLINE-FIRST Y AISLAMIENTO MULTI-CUENTA ---

    /**
     * Obtiene todas las notas creadas o modificadas localmente que aún no han sido subidas a Firestore.
     */
    @Query("""
        SELECT * FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND isSynced = 0
          AND pendingDelete = 0
          AND syncEnabled = 1
        ORDER BY lastModifiedLocal ASC
    """)
    suspend fun getUnsyncedUpserts(userId: String): List<NoteEntity>

    /**
     * Obtiene las notas marcadas para eliminación permanente (pendingDelete = 1) pendientes de borrar en Firestore.
     */
    @Query("""
        SELECT * FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND pendingDelete = 1
    """)
    suspend fun getPendingDeletes(userId: String): List<NoteEntity>

    /**
     * Marca una nota con eliminación diferida (soft-delete) para que el motor de sincronización la elimine de Firestore.
     */
    @Query("""
        UPDATE notes
        SET pendingDelete = 1,
            isSynced = 0,
            updatedAt = :timestamp,
            lastModifiedLocal = :timestamp
        WHERE id = :id
    """)
    suspend fun markNotePendingDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE notes
        SET pendingDelete = 1,
            isSynced = 0,
            updatedAt = :timestamp,
            lastModifiedLocal = :timestamp
        WHERE id IN (:ids)
    """)
    suspend fun markNotesPendingDeleteBatch(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    /**
     * Reclama las notas creadas sin sesión ('LOCAL_ANONYMOUS') y las asigna al UID autenticado.
     */
    @Query("""
        UPDATE notes
        SET userId = :newUserId,
            isSynced = 0
        WHERE userId = 'LOCAL_ANONYMOUS'
    """)
    suspend fun claimAnonymousNotes(newUserId: String)

    /**
     * Marca la nota como sincronizada ÚNICAMENTE si el usuario no volvió a editarla mientras se subía a Firestore.
     */
    @Query("""
        UPDATE notes
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE remoteUuid = :remoteUuid
          AND lastModifiedLocal = :expectedLocalTimestamp
    """)
    suspend fun markNoteSyncedIfUnchanged(
        remoteUuid: String,
        expectedLocalTimestamp: Long,
        serverTimestamp: Long
    )

    @Query("""
        UPDATE notes
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE id = :id
    """)
    suspend fun markNoteSyncedById(
        id: Long,
        serverTimestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE notes
        SET isSynced = 1,
            updatedAtServer = :serverTimestamp
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND pendingDelete = 0
    """)
    suspend fun markAllNotesSyncedForUser(
        userId: String,
        serverTimestamp: Long = System.currentTimeMillis()
    )

    /**
     * Flujo reactivo con la cantidad de operaciones pendientes de sincronizar para mostrar en la UI.
     */
    @Query("""
        SELECT COUNT(*) FROM notes
        WHERE (userId = :userId OR userId = 'LOCAL_ANONYMOUS')
          AND (isSynced = 0 OR pendingDelete = 1)
          AND syncEnabled = 1
    """)
    fun observePendingSyncCount(userId: String): Flow<Int>
}

