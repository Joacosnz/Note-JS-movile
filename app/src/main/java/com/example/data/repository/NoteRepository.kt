package com.example.data.repository

import android.content.Context
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

    // --- ESCRITURAS (LOCAL -> NUBE) ---
    suspend fun insert(note: NoteEntity): Long {
        val noteToSave = note.copy(isSynced = false)
        val localId = noteDao.insertNote(noteToSave)
        val savedNote = noteToSave.copy(id = localId)

        syncToCloud(savedNote)
        return localId
    }

    suspend fun update(note: NoteEntity) {
        val noteToUpdate = note.copy(isSynced = false, updatedAt = System.currentTimeMillis())
        noteDao.updateNote(noteToUpdate)

        syncToCloud(noteToUpdate)
    }

    private fun syncToCloud(note: NoteEntity) {
        val userId = currentUserId ?: return

        if (!note.isValidForSync()) return

        syncScope.launch {
            var updatedNote = if (note.remoteUuid.isEmpty()) {
                val newUuid = UUID.randomUUID().toString()
                val noteWithUuid = note.copy(remoteUuid = newUuid, isSynced = false)
                noteDao.updateNote(noteWithUuid)
                noteWithUuid
            } else {
                note
            }

            var isModified = false

            // 1. Subir archivo adjunto principal de NoteEntity si existe pdfPath con remoteUrl vacía
            if (updatedNote.pdfPath.isNotBlank() && updatedNote.remoteUrl.isBlank()) {
                val file = File(updatedNote.pdfPath)
                if (file.exists()) {
                    val remoteUrl = firebaseStorageManager.uploadFile(
                        userId = userId,
                        localPath = updatedNote.pdfPath,
                        fileName = file.name
                    )
                    if (!remoteUrl.isNullOrEmpty()) {
                        updatedNote = updatedNote.copy(remoteUrl = remoteUrl)
                        isModified = true
                    }
                }
            }

            // 2. Subir archivos adjuntos en los bloques (PDF, Imágenes)
            val blocks = NoteBlockSerializer.deserialize(updatedNote.content)
            val updatedBlocks = blocks.map { block ->
                when (block) {
                    is NoteBlock.Pdf -> {
                        if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                            val file = File(block.filePath)
                            if (file.exists()) {
                                val url = firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localPath = block.filePath,
                                    fileName = if (block.fileName.isNotBlank()) block.fileName else file.name
                                )
                                if (!url.isNullOrEmpty()) {
                                    isModified = true
                                    block.copy(remoteUrl = url)
                                } else block
                            } else block
                        } else block
                    }
                    is NoteBlock.ImageBlock -> {
                        if (block.localUri.isNotBlank() && block.remoteUrl.isBlank() && !block.localUri.startsWith("http")) {
                            val file = File(block.localUri)
                            if (file.exists()) {
                                val url = firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localPath = block.localUri,
                                    fileName = file.name
                                )
                                if (!url.isNullOrEmpty()) {
                                    isModified = true
                                    block.copy(remoteUrl = url)
                                } else block
                            } else block
                        } else block
                    }
                    is NoteBlock.Image -> {
                        if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                            val file = File(block.filePath)
                            if (file.exists()) {
                                val url = firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localPath = block.filePath,
                                    fileName = if (block.fileName.isNotBlank()) block.fileName else file.name
                                )
                                if (!url.isNullOrEmpty()) {
                                    isModified = true
                                    block.copy(remoteUrl = url)
                                } else block
                            } else block
                        } else block
                    }
                    is AudioBlock -> {
                        if (block.filePath.isNotBlank() && block.remoteUrl.isBlank()) {
                            val file = File(block.filePath)
                            if (file.exists()) {
                                val url = firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localPath = block.filePath,
                                    fileName = file.name
                                )
                                if (!url.isNullOrEmpty()) {
                                    isModified = true
                                    block.copy(remoteUrl = url)
                                } else block
                            } else block
                        } else block
                    }
                    else -> block
                }
            }

            if (isModified) {
                val serializedContent = NoteBlockSerializer.serialize(updatedBlocks)
                updatedNote = updatedNote.copy(content = serializedContent)
                noteDao.updateNote(updatedNote)
            }

            val success = firestoreManager.pushNote(userId, updatedNote)
            if (success) {
                noteDao.updateNote(
                    updatedNote.copy(
                        isSynced = true,
                        updatedAtServer = System.currentTimeMillis()
                    )
                )
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

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)

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
                                                val destPath = if (block.filePath.isNotBlank()) {
                                                    block.filePath
                                                } else {
                                                    val pdfDir = context?.let { File(it.filesDir, "pdfs") }
                                                    if (pdfDir != null && !pdfDir.exists()) pdfDir.mkdirs()
                                                    File(pdfDir ?: File(""), if (block.fileName.isNotBlank()) block.fileName else "${UUID.randomUUID()}.pdf").absolutePath
                                                }
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
                                                val destFile = File(imgDir, "img_${block.id}.jpg")
                                                val downloaded = firebaseStorageManager.downloadFile(block.remoteUrl, destFile.absolutePath)
                                                if (downloaded) {
                                                    isBlocksModified = true
                                                    block.copy(localUri = destFile.absolutePath)
                                                } else block
                                            } else block
                                        } else block
                                    }
                                    is NoteBlock.Image -> {
                                        if (block.remoteUrl.isNotBlank()) {
                                            val fileExists = block.filePath.isNotBlank() && File(block.filePath).exists()
                                            if (!fileExists) {
                                                val destPath = if (block.filePath.isNotBlank()) {
                                                    block.filePath
                                                } else {
                                                    val imgDir = context?.let { File(it.filesDir, "images") }
                                                    if (imgDir != null && !imgDir.exists()) imgDir.mkdirs()
                                                    File(imgDir ?: File(""), if (block.fileName.isNotBlank()) block.fileName else "${UUID.randomUUID()}.jpg").absolutePath
                                                }
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
                                    val destPath = if (updatedEntity.pdfPath.isNotBlank()) {
                                        updatedEntity.pdfPath
                                    } else {
                                        val pdfDir = context?.let { File(it.filesDir, "pdfs") }
                                        if (pdfDir != null && !pdfDir.exists()) pdfDir.mkdirs()
                                        File(pdfDir ?: File(""), "${UUID.randomUUID()}.pdf").absolutePath
                                    }
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
