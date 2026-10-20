package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.remote.FirebaseStorageManager
import com.example.data.remote.FirestoreManager
import com.example.data.remote.mapper.toNoteEntity
import com.example.model.NoteBlock
import com.example.model.AudioBlock
import com.example.util.NoteBlockSerializer
import com.example.util.isValidForSync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val signatureDao: SignatureDao,
    private val firestoreManager: FirestoreManager,
    private val firebaseStorageManager: FirebaseStorageManager = FirebaseStorageManager(),
    private val context: Context? = null
) {

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var currentUserId: String? = null
        get() {
            if (field.isNullOrBlank()) {
                field = try {
                    FirebaseAuth.getInstance().currentUser?.uid
                } catch (e: Exception) {
                    null
                }
            }
            return field
        }

    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val trashedNotes: Flow<List<NoteEntity>> = noteDao.getTrashedNotes()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val signatures: Flow<List<SignatureEntity>> = signatureDao.getAllSignatures()

    suspend fun insertSignature(signature: SignatureEntity): Long = signatureDao.insertSignature(signature)

    suspend fun deleteSignature(signature: SignatureEntity) = signatureDao.deleteSignature(signature)

    suspend fun deleteSignatureById(id: Long) = signatureDao.deleteSignatureById(id)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    // --- LÓGICA DE APLANAMIENTO (FLATTEN) Y BÚSQUEDA FTS4 ---

    /**
     * Extrae y concatena el texto de todos los bloques (Texto, Listas, Tablas, PDF OCR/transcripciones de audio),
     * además de tags y pdfExtractedText para centralizarlo en [NoteEntity.searchableText].
     */
    fun flattenNoteText(note: NoteEntity): String {
        val blocks = NoteBlockSerializer.deserialize(note.content)
        val textParts = mutableListOf<String>()

        for (block in blocks) {
            when (block) {
                is NoteBlock.Text -> {
                    if (block.content.isNotBlank()) textParts.add(block.content)
                }
                is NoteBlock.Checklist -> {
                    if (block.content.isNotBlank()) textParts.add(block.content)
                }
                is NoteBlock.SimpleList -> {
                    if (block.content.isNotBlank()) textParts.add(block.content)
                }
                is NoteBlock.CalloutBox -> {
                    if (block.content.isNotBlank()) textParts.add(block.content)
                }
                is NoteBlock.Table -> {
                    val cellTexts = block.matrix.flatten()
                        .map { it.text.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                    if (cellTexts.isNotBlank()) textParts.add(cellTexts)
                }
                is NoteBlock.Pdf -> {
                    if (block.indexedText.isNotBlank()) textParts.add(block.indexedText)
                    if (block.fileName.isNotBlank()) textParts.add(block.fileName)
                }
                is AudioBlock -> {
                    if (block.transcription.isNotBlank()) textParts.add(block.transcription)
                }
                is NoteBlock.ImageBlock -> {}
                is NoteBlock.Image -> {
                    if (block.fileName.isNotBlank()) textParts.add(block.fileName)
                }
                is NoteBlock.Drawing -> {}
            }
        }

        if (!note.pdfExtractedText.isNullOrBlank()) {
            textParts.add(note.pdfExtractedText)
        }

        if (note.tags.isNotBlank()) {
            textParts.add(note.tags)
        }

        return textParts.joinToString(" ")
    }

    /**
     * Realiza una búsqueda ultra rápida utilizando el operador MATCH de FTS4.
     * Envuelve el término de búsqueda entre asteriscos (*query*) para permitir prefix matching.
     */
    suspend fun searchNotesFts(query: String): List<NoteEntity> {
        if (query.isBlank()) return emptyList()
        val sanitizedQuery = query.trim().replace("\"", "")
        val formattedQuery = "*$sanitizedQuery*"
        return noteDao.searchNotesFts(formattedQuery)
    }

    // --- ESCRITURAS (LOCAL -> NUBE) ---
    suspend fun insert(note: NoteEntity): Long {
        val flattenedText = flattenNoteText(note)
        val noteToSave = note.copy(searchableText = flattenedText, isSynced = false)
        val localId = noteDao.insertNote(noteToSave)
        val savedNote = noteToSave.copy(id = localId)

        syncToCloud(savedNote)
        return localId
    }

    suspend fun update(note: NoteEntity) {
        val flattenedText = flattenNoteText(note)
        val noteToUpdate = note.copy(searchableText = flattenedText, isSynced = false, updatedAt = System.currentTimeMillis())
        noteDao.updateNote(noteToUpdate)

        syncToCloud(noteToUpdate)
    }

    private fun syncToCloud(note: NoteEntity) {
        val userId = currentUserId ?: return

        if (!note.isValidForSync()) return

        if (!note.syncEnabled) {
            // Si el usuario apagó la sincronización pero la nota ya existía en la nube, la borramos para liberar espacio.
            if (note.remoteUuid.isNotEmpty()) {
                syncScope.launch {
                    try {
                        firestoreManager.deleteNote(userId, note.remoteUuid)
                    } catch (e: Exception) {
                        Log.e("NoteRepository", "Error deleting note from firestore", e)
                    }
                }
            }
            return
        }

        syncScope.launch {
            try {
                var updatedNote = if (note.remoteUuid.isEmpty()) {
                    val newUuid = UUID.randomUUID().toString()
                    val noteWithUuid = note.copy(remoteUuid = newUuid, isSynced = false)
                    noteDao.updateNote(noteWithUuid)
                    noteWithUuid
                } else {
                    note
                }

                var isModified = false

                coroutineScope {
                    // 1. Subir archivo adjunto principal de NoteEntity si existe pdfPath con remoteUrl vacía
                    val mainPdfDeferred = if (updatedNote.pdfPath.isNotBlank() && updatedNote.remoteUrl.isBlank()) {
                        async {
                            try {
                                firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localUriString = updatedNote.pdfPath,
                                    fileName = Uri.parse(updatedNote.pdfPath).lastPathSegment ?: "pdf_file"
                                )
                            } catch (e: Exception) {
                                Log.e("NoteRepository", "Error uploading main pdf to storage", e)
                                null
                            }
                        }
                    } else null

                    // 2. Subir archivos adjuntos en los bloques (PDF, Imágenes, Audio) en paralelo
                    val blocks = NoteBlockSerializer.deserialize(updatedNote.content)
                    val blockUploadDeferreds = blocks.map { block ->
                        when (block) {
                            is NoteBlock.Pdf -> {
                                if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                                    async {
                                        val url = try {
                                            firebaseStorageManager.uploadFile(
                                                userId = userId,
                                                localUriString = block.filePath,
                                                fileName = if (block.fileName.isNotBlank()) block.fileName else Uri.parse(block.filePath).lastPathSegment ?: "pdf_file"
                                            )
                                        } catch (e: Exception) {
                                            Log.e("NoteRepository", "Error uploading block pdf to storage", e)
                                            null
                                        }
                                        if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else block
                                    }
                                } else null
                            }
                            is NoteBlock.ImageBlock -> {
                                if (block.localUri.isNotBlank() && block.remoteUrl.isBlank() && !block.localUri.startsWith("http")) {
                                    async {
                                        val url = try {
                                            firebaseStorageManager.uploadFile(
                                                userId = userId,
                                                localUriString = block.localUri,
                                                fileName = Uri.parse(block.localUri).lastPathSegment ?: "image_file"
                                            )
                                        } catch (e: Exception) {
                                            Log.e("NoteRepository", "Error uploading image block to storage", e)
                                            null
                                        }
                                        if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else block
                                    }
                                } else null
                            }
                            is NoteBlock.Image -> {
                                if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                                    async {
                                        val url = try {
                                            firebaseStorageManager.uploadFile(
                                                userId = userId,
                                                localUriString = block.filePath,
                                                fileName = if (block.fileName.isNotBlank()) block.fileName else Uri.parse(block.filePath).lastPathSegment ?: "image_file"
                                            )
                                        } catch (e: Exception) {
                                            Log.e("NoteRepository", "Error uploading image to storage", e)
                                            null
                                        }
                                        if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else block
                                    }
                                } else null
                            }
                            is AudioBlock -> {
                                if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                                    async {
                                        val url = try {
                                            firebaseStorageManager.uploadFile(
                                                userId = userId,
                                                localUriString = block.filePath,
                                                fileName = Uri.parse(block.filePath).lastPathSegment ?: "audio_file"
                                            )
                                        } catch (e: Exception) {
                                            Log.e("NoteRepository", "Error uploading audio to storage", e)
                                            null
                                        }
                                        if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else block
                                    }
                                } else null
                            }
                            else -> null
                        }
                    }

                    // Await main PDF
                    val mainPdfRemoteUrl = mainPdfDeferred?.await()
                    if (!mainPdfRemoteUrl.isNullOrEmpty()) {
                        updatedNote = updatedNote.copy(remoteUrl = mainPdfRemoteUrl)
                        isModified = true
                    }

                    // Await block uploads
                    val updatedBlocks = blocks.mapIndexed { index, originalBlock ->
                        val deferred = blockUploadDeferreds[index]
                        if (deferred != null) {
                            val newBlock = deferred.await()
                            if (newBlock != originalBlock) {
                                isModified = true
                            }
                            newBlock
                        } else {
                            originalBlock
                        }
                    }

                    if (isModified) {
                        val serializedContent = NoteBlockSerializer.serialize(updatedBlocks)
                        updatedNote = updatedNote.copy(content = serializedContent)
                        noteDao.updateNote(updatedNote)
                    }
                }

                val success = try {
                    firestoreManager.pushNote(userId, updatedNote)
                } catch (e: Exception) {
                    Log.e("NoteRepository", "Error pushing note to firestore", e)
                    false
                }

                if (success) {
                    noteDao.updateNote(
                        updatedNote.copy(
                            isSynced = true,
                            updatedAtServer = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error in syncToCloud", e)
            }
        }
    }

    suspend fun delete(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun deleteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun emptyTrash(trashedNotes: List<NoteEntity> = emptyList()) {
        val userId = currentUserId
        if (!userId.isNullOrEmpty()) {
            for (note in trashedNotes) {
                if (note.remoteUuid.isNotEmpty()) {
                    firestoreManager.deleteNote(userId, note.remoteUuid)
                }
            }
        }
        noteDao.emptyTrash()
    }

    suspend fun moveToTrashBatch(ids: List<Long>) = noteDao.moveToTrashBatch(ids)

    suspend fun deletePermanentlyBatch(ids: List<Long>) = noteDao.deletePermanentlyBatch(ids)

    suspend fun updateFolderBatch(ids: List<Long>, folder: String) = noteDao.updateFolderBatch(ids, folder)

    suspend fun insertFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)

    suspend fun renameFolder(oldName: String, newName: String) {
        val foldersList = folderDao.getAllFoldersList()
        foldersList.find { it.name.equals(oldName, ignoreCase = true) }?.let { entity ->
            folderDao.updateFolder(entity.copy(name = newName))
        }
        noteDao.renameFolderInNotes(oldName, newName)
    }

    suspend fun deleteFolderByName(folderName: String) {
        val foldersList = folderDao.getAllFoldersList()
        foldersList.find { it.name.equals(folderName, ignoreCase = true) }?.let { entity ->
            folderDao.deleteFolder(entity)
        }
        noteDao.clearFolderInNotes(folderName)
    }

    suspend fun renameTag(oldTag: String, newTag: String) {
        val notesList = noteDao.getAllNotesList()
        notesList.forEach { note ->
            var updated = false
            var tagsList = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tagsList.any { it.equals(oldTag, ignoreCase = true) }) {
                tagsList = tagsList.map { if (it.equals(oldTag, ignoreCase = true)) newTag else it }
                updated = true
            }
            var newTitle = note.title
            var newContent = note.content
            if (newTitle.contains("#$oldTag", ignoreCase = true)) {
                newTitle = newTitle.replace("#$oldTag", "#$newTag", ignoreCase = true)
                updated = true
            }
            if (newContent.contains("#$oldTag", ignoreCase = true)) {
                newContent = newContent.replace("#$oldTag", "#$newTag", ignoreCase = true)
                updated = true
            }
            if (updated) {
                val updatedNote = note.copy(
                    title = newTitle,
                    content = newContent,
                    tags = tagsList.joinToString(","),
                    updatedAt = System.currentTimeMillis()
                )
                update(updatedNote)
            }
        }
    }

    suspend fun deleteTagByName(tagName: String) {
        val notesList = noteDao.getAllNotesList()
        notesList.forEach { note ->
            var updated = false
            var tagsList = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tagsList.any { it.equals(tagName, ignoreCase = true) }) {
                tagsList = tagsList.filter { !it.equals(tagName, ignoreCase = true) }
                updated = true
            }
            var newTitle = note.title
            var newContent = note.content
            if (newTitle.contains("#$tagName", ignoreCase = true)) {
                newTitle = newTitle.replace("#$tagName", "", ignoreCase = true).trim()
                updated = true
            }
            if (newContent.contains("#$tagName", ignoreCase = true)) {
                newContent = newContent.replace("#$tagName", "", ignoreCase = true).trim()
                updated = true
            }
            if (updated) {
                val updatedNote = note.copy(
                    title = newTitle,
                    content = newContent,
                    tags = tagsList.joinToString(","),
                    updatedAt = System.currentTimeMillis()
                )
                update(updatedNote)
            }
        }
    }

    suspend fun addTagBatch(ids: List<Long>, tag: String) {
        val trimmedTag = tag.trim().removePrefix("#")
        if (trimmedTag.isBlank()) return
        val notes = noteDao.getNotesByIds(ids)
        for (note in notes) {
            val currentTags = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (!currentTags.contains(trimmedTag)) {
                val newTags = (currentTags + trimmedTag).joinToString(",")
                val updatedNote = note.copy(tags = newTags, updatedAt = System.currentTimeMillis())
                update(updatedNote)
            }
        }
    }

    suspend fun togglePin(note: NoteEntity) {
        val updatedNote = note.copy(
            isPinned = !note.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        update(updatedNote)
    }

    suspend fun toggleArchive(note: NoteEntity) {
        val willBeArchived = !note.isArchived
        val updatedNote = note.copy(
            isArchived = willBeArchived,
            isPinned = if (willBeArchived) false else note.isPinned,
            updatedAt = System.currentTimeMillis()
        )
        update(updatedNote)
    }

    suspend fun moveToTrash(note: NoteEntity) {
        update(note.copy(isTrashed = true, isSynced = false))
    }

    suspend fun deletePermanently(note: NoteEntity) {
        val userId = currentUserId
        if (!userId.isNullOrEmpty() && note.remoteUuid.isNotEmpty()) {
            firestoreManager.deleteNote(userId, note.remoteUuid)
        }
        noteDao.deleteNote(note)
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        val updatedNote = note.copy(
            isTrashed = false,
            updatedAt = System.currentTimeMillis()
        )
        update(updatedNote)
    }

    // --- LECTURAS EN TIEMPO REAL (NUBE -> LOCAL) ---
    fun startListeningToCloud(userId: String) {
        this.currentUserId = userId

        syncScope.launch {
            firestoreManager.getNotesFlow(userId).collectLatest { remoteNotesMap ->
                remoteNotesMap.forEach { map ->
                    val remoteUuid = map["remoteUuid"] as? String ?: return@forEach
                    val remoteUpdatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L

                    val localNote = noteDao.getNoteByRemoteUuid(remoteUuid)

                    if (localNote == null || remoteUpdatedAt > localNote.updatedAt) {
                        var newNote = map.toNoteEntity()
                        if (localNote != null) {
                            newNote = newNote.copy(
                                id = localNote.id,
                                imageUri = localNote.imageUri,
                                pdfExtractedText = localNote.pdfExtractedText
                            )
                        }
                        val flattenedText = flattenNoteText(newNote)
                        newNote = newNote.copy(searchableText = flattenedText)

                        // 1. Guardar o actualizar la nota en Room de forma INSTANTÁNEA para no bloquear la UI
                        if (localNote == null) {
                            noteDao.insertNote(newNote)
                        } else {
                            noteDao.updateNote(newNote)
                        }

                        // 2. Descargar archivos adjuntos en segundo plano de forma asíncrona
                        syncScope.launch {
                            val currentSavedNote = noteDao.getNoteByRemoteUuid(remoteUuid) ?: newNote
                            var isBlocksModified = false
                            val blocks = NoteBlockSerializer.deserialize(currentSavedNote.content)
                            val updatedBlocks = blocks.map { block ->
                                when (block) {
                                    is NoteBlock.Pdf -> {
                                        if (block.remoteUrl.isNotBlank()) {
                                            val fileExists = block.filePath.isNotBlank() && File(block.filePath).exists()
                                            if (!fileExists) {
                                                val safeFileName = if (block.fileName.isNotBlank()) block.fileName else File(block.filePath).name.ifBlank { "${UUID.randomUUID()}.pdf" }
                                                val pdfDir = context?.let { File(it.filesDir, "pdfs") }
                                                pdfDir?.mkdirs()
                                                val destPath = File(pdfDir ?: File(""), safeFileName).absolutePath
                                                val downloaded = firebaseStorageManager.downloadFile(block.remoteUrl, destPath)
                                                if (downloaded) {
                                                    isBlocksModified = true
                                                    block.copy(filePath = destPath)
                                                } else block
                                            } else block
                                        } else block
                                    }
                                    is NoteBlock.ImageBlock -> {
                                        if (block.remoteUrl.isNotBlank()) {
                                            val fileExists = block.localUri.isNotBlank() && File(block.localUri).exists()
                                            if (!fileExists) {
                                                val imgDir = context?.let { File(it.filesDir, "images") }
                                                if (imgDir != null && !imgDir.exists()) imgDir.mkdirs()
                                                val destFile = File(imgDir ?: File(""), "img_${block.id}.jpg").absolutePath
                                                val downloaded = firebaseStorageManager.downloadFile(block.remoteUrl, destFile)
                                                if (downloaded) {
                                                    isBlocksModified = true
                                                    block.copy(localUri = destFile)
                                                } else block
                                            } else block
                                        } else block
                                    }
                                    is NoteBlock.Image -> {
                                        if (block.remoteUrl.isNotBlank()) {
                                            val fileExists = block.filePath.isNotBlank() && File(block.filePath).exists()
                                            if (!fileExists) {
                                                val imgDir = context?.let { File(it.filesDir, "images") }
                                                if (imgDir != null && !imgDir.exists()) imgDir.mkdirs()
                                                val safeFileName = if (block.fileName.isNotBlank()) block.fileName else "img_${block.id}.jpg"
                                                val destPath = File(imgDir ?: File(""), safeFileName).absolutePath
                                                val downloaded = firebaseStorageManager.downloadFile(block.remoteUrl, destPath)
                                                if (downloaded) {
                                                    isBlocksModified = true
                                                    block.copy(filePath = destPath)
                                                } else block
                                            } else block
                                        } else block
                                    }
                                    is AudioBlock -> {
                                        if (block.remoteUrl.isNotBlank()) {
                                            val fileExists = block.filePath.isNotBlank() && File(block.filePath).exists()
                                            if (!fileExists) {
                                                val audioDir = context?.let { File(it.filesDir, "audio_notes") }
                                                if (audioDir != null && !audioDir.exists()) audioDir.mkdirs()
                                                
                                                // Always generate a valid local path within the current app's filesDir
                                                val fileName = if (block.filePath.isNotBlank()) {
                                                    File(block.filePath).name
                                                } else {
                                                    "${UUID.randomUUID()}.m4a"
                                                }
                                                val destPath = File(audioDir ?: File(""), fileName).absolutePath
                                                
                                                val downloaded = firebaseStorageManager.downloadFile(block.remoteUrl, destPath)
                                                if (downloaded) {
                                                    isBlocksModified = true
                                                    block.copy(filePath = destPath)
                                                } else block
                                            } else block
                                        } else block
                                    }
                                    else -> block
                                }
                            }

                            var updatedEntity = currentSavedNote
                            if (isBlocksModified) {
                                updatedEntity = updatedEntity.copy(content = NoteBlockSerializer.serialize(updatedBlocks))
                            }

                            if (updatedEntity.remoteUrl.isNotBlank()) {
                                val fileExists = updatedEntity.pdfPath.isNotBlank() && File(updatedEntity.pdfPath).exists()
                                if (!fileExists) {
                                    val safeFileName = File(updatedEntity.pdfPath).name.ifBlank { "${UUID.randomUUID()}.pdf" }
                                    val pdfDir = context?.let { File(it.filesDir, "pdfs") }
                                    pdfDir?.mkdirs()
                                    val destPath = File(pdfDir ?: File(""), safeFileName).absolutePath
                                    val downloaded = firebaseStorageManager.downloadFile(updatedEntity.remoteUrl, destPath)
                                    if (downloaded) {
                                        updatedEntity = updatedEntity.copy(pdfPath = destPath)
                                    }
                                }
                            }

                            if (isBlocksModified || updatedEntity.pdfPath != currentSavedNote.pdfPath) {
                                noteDao.updateNote(updatedEntity)
                            }
                        }
                    }
                }
            }
        }
    }
}
