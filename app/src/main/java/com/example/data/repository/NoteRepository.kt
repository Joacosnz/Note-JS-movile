package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.UriConverters
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NoteMediaAttachment
import com.example.data.local.entity.SignatureEntity
import com.example.data.remote.FirebaseStorageManager
import com.example.data.remote.FirestoreManager
import com.example.data.remote.mapper.toNoteEntity
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.util.NoteBlockSerializer
import com.example.util.isValidForSync
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val signatureDao: SignatureDao,
    private val firestoreManager: FirestoreManager,
    private val firebaseStorageManager: FirebaseStorageManager = FirebaseStorageManager(),
    private val context: Context? = null
) {

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var cloudSyncJob: Job? = null
    private val uriConverters = UriConverters()

    private val _activeUserId = MutableStateFlow(
        try {
            FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID
        } catch (_: Exception) {
            NoteEntity.LOCAL_ANONYMOUS_USER_ID
        }
    )
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    private var currentUserId: String? = null
        get() {
            if (field.isNullOrBlank()) {
                field = try {
                    FirebaseAuth.getInstance().currentUser?.uid
                } catch (_: Exception) {
                    null
                }
            }
            return field
        }

    val activeNotes: Flow<List<NoteEntity>> = _activeUserId.flatMapLatest { uid ->
        noteDao.getActiveNotesForUser(uid)
    }

    val archivedNotes: Flow<List<NoteEntity>> = _activeUserId.flatMapLatest { uid ->
        noteDao.getArchivedNotesForUser(uid)
    }

    val trashedNotes: Flow<List<NoteEntity>> = _activeUserId.flatMapLatest { uid ->
        noteDao.getTrashedNotesForUser(uid)
    }

    val pendingSyncCount: Flow<Int> = _activeUserId.flatMapLatest { uid ->
        noteDao.observePendingSyncCount(uid)
    }

    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val signatures: Flow<List<SignatureEntity>> = signatureDao.getAllSignatures()

    suspend fun insertSignature(signature: SignatureEntity): Long = signatureDao.insertSignature(signature)

    suspend fun deleteSignature(signature: SignatureEntity) = signatureDao.deleteSignature(signature)

    suspend fun deleteSignatureById(id: Long) = signatureDao.deleteSignatureById(id)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun getAllNotes(): List<NoteEntity> = noteDao.getAllNotesForUserList(_activeUserId.value)

    // --- EXTRACCIÓN DE METADATOS DE ADJUNTOS (LOCAL URI + REMOTE URL) ---

    fun buildAttachmentsJson(note: NoteEntity): String {
        val attachments = mutableListOf<NoteMediaAttachment>()
        val blocks = NoteBlockSerializer.deserialize(note.content)

        if (note.pdfPath.isNotBlank() || note.remoteUrl.isNotBlank()) {
            val pdfFile = if (note.pdfPath.isNotBlank()) File(note.pdfPath) else null
            attachments.add(
                NoteMediaAttachment(
                    id = "main_pdf_${note.remoteUuid}",
                    type = MediaType.PDF,
                    displayName = pdfFile?.name ?: "Documento.pdf",
                    mimeType = "application/pdf",
                    localUri = note.pdfPath.ifBlank { null },
                    remoteUrl = note.remoteUrl.ifBlank { null },
                    sizeBytes = if (pdfFile?.exists() == true) pdfFile.length() else 0L
                )
            )
        }

        if (note.imageUri != null || note.remoteImageUrl.isNotBlank()) {
            attachments.add(
                NoteMediaAttachment(
                    id = "main_img_${note.remoteUuid}",
                    type = MediaType.IMAGE,
                    displayName = "Imagen adjunta",
                    mimeType = "image/jpeg",
                    localUri = note.imageUri?.toString(),
                    remoteUrl = note.remoteImageUrl.ifBlank { null }
                )
            )
        }

        for (block in blocks) {
            when (block) {
                is NoteBlock.Pdf -> {
                    attachments.add(
                        NoteMediaAttachment(
                            id = block.id,
                            type = MediaType.PDF,
                            displayName = block.fileName,
                            mimeType = "application/pdf",
                            localUri = block.filePath.ifBlank { null },
                            remoteUrl = block.remoteUrl.ifBlank { null },
                            pageCount = block.pageCount
                        )
                    )
                }
                is NoteBlock.ImageBlock -> {
                    attachments.add(
                        NoteMediaAttachment(
                            id = block.id,
                            type = MediaType.IMAGE,
                            displayName = "img_${block.id}.jpg",
                            mimeType = "image/jpeg",
                            localUri = block.localUri.ifBlank { null },
                            remoteUrl = block.remoteUrl.ifBlank { null }
                        )
                    )
                }
                is NoteBlock.Image -> {
                    attachments.add(
                        NoteMediaAttachment(
                            id = block.id,
                            type = MediaType.IMAGE,
                            displayName = block.fileName.ifBlank { "img_${block.id}.jpg" },
                            mimeType = "image/jpeg",
                            localUri = block.filePath.ifBlank { null },
                            remoteUrl = block.remoteUrl.ifBlank { null }
                        )
                    )
                }
                is AudioBlock -> {
                    attachments.add(
                        NoteMediaAttachment(
                            id = block.id,
                            type = MediaType.AUDIO,
                            displayName = File(block.filePath).name.ifBlank { "audio_${block.id}.m4a" },
                            mimeType = "audio/mp4",
                            localUri = block.filePath.ifBlank { null },
                            remoteUrl = block.remoteUrl.ifBlank { null }
                        )
                    )
                }
                else -> {}
            }
        }

        return uriConverters.fromAttachmentsList(attachments)
    }

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
                is NoteBlock.Math -> {
                    if (block.latexCode.isNotBlank()) textParts.add(block.latexCode)
                }
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

    // --- ESCRITURAS OFFLINE-FIRST (LOCAL -> COLA -> NUBE) ---

    suspend fun insert(note: NoteEntity): Long {
        val now = System.currentTimeMillis()
        val effectiveUserId = currentUserId ?: _activeUserId.value
        val flattenedText = flattenNoteText(note)
        val ensuredUuid = note.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val baseNote = note.copy(
            remoteUuid = ensuredUuid,
            userId = if (note.userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID || note.userId.isBlank()) effectiveUserId else note.userId,
            searchableText = flattenedText,
            isSynced = false,
            pendingDelete = false,
            updatedAt = now,
            lastModifiedLocal = now
        )
        val noteToSave = baseNote.copy(attachmentsJson = buildAttachmentsJson(baseNote))
        val localId = noteDao.insertNote(noteToSave)
        val savedNote = noteToSave.copy(id = localId)

        syncToCloud(savedNote)
        return localId
    }

    suspend fun update(note: NoteEntity) {
        val now = System.currentTimeMillis()
        val effectiveUserId = currentUserId ?: _activeUserId.value
        val flattenedText = flattenNoteText(note)
        val baseNote = note.copy(
            userId = if (note.userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID || note.userId.isBlank()) effectiveUserId else note.userId,
            searchableText = flattenedText,
            isSynced = false,
            updatedAt = now,
            lastModifiedLocal = now
        )
        val noteToUpdate = baseNote.copy(attachmentsJson = buildAttachmentsJson(baseNote))
        noteDao.updateNote(noteToUpdate)

        syncToCloud(noteToUpdate)
    }

    /**
     * Sincroniza todas las operaciones pendientes en la cola offline (borrados diferidos y ediciones/creaciones).
     */
    fun syncPendingNotes(userId: String? = currentUserId) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        syncScope.launch {
            syncPendingNotesSuspending(targetUid)
        }
    }

    suspend fun syncPendingNotesSuspending(userId: String? = currentUserId) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        try {
            if (targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                noteDao.claimAnonymousNotes(targetUid)
            }

            // 1. Drenar eliminaciones pendientes (pendingDelete = 1)
            val pendingDeletes = noteDao.getPendingDeletes(targetUid)
            for (noteToDelete in pendingDeletes) {
                if (noteToDelete.remoteUuid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                    val deletedRemotely = firestoreManager.deleteNote(targetUid, noteToDelete.remoteUuid, context)
                    if (deletedRemotely) {
                        noteDao.deleteNoteById(noteToDelete.id)
                    }
                } else {
                    noteDao.deleteNoteById(noteToDelete.id)
                }
            }

            // 2. Drenar creaciones y ediciones pendientes (isSynced = 0, pendingDelete = 0)
            val unsyncedNotes = noteDao.getUnsyncedUpserts(targetUid)
            for (dirtyNote in unsyncedNotes) {
                pushSingleNoteInternal(targetUid, dirtyNote)
            }

            noteDao.markAllNotesSyncedForUser(targetUid)
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error drenando cola offline", e)
        }
    }

    suspend fun markAllCurrentUserNotesSynced(userId: String? = currentUserId) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            noteDao.claimAnonymousNotes(targetUid)
        }
        noteDao.markAllNotesSyncedForUser(targetUid)
    }

    private fun syncToCloud(note: NoteEntity) {
        val userId = currentUserId ?: _activeUserId.value
        if (userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return

        syncScope.launch {
            pushSingleNoteInternal(userId, note)
        }
    }

    private suspend fun pushSingleNoteInternal(userId: String, note: NoteEntity) {
        if (!note.isValidForSync()) {
            noteDao.markNoteSyncedById(note.id)
            return
        }

        if (!note.syncEnabled) {
            if (note.remoteUuid.isNotEmpty() && userId != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                try {
                    firestoreManager.deleteNote(userId, note.remoteUuid, context)
                } catch (e: Exception) {
                    Log.e("NoteRepository", "Error deleting disabled-sync note from firestore", e)
                }
            }
            noteDao.markNoteSyncedById(note.id)
            return
        }

        try {
            val expectedLocalTimestamp = note.lastModifiedLocal
            var updatedNote = if (note.remoteUuid.isEmpty()) {
                val newUuid = UUID.randomUUID().toString()
                val noteWithUuid = note.copy(remoteUuid = newUuid, userId = userId, isSynced = false)
                noteDao.updateNote(noteWithUuid)
                noteWithUuid
            } else if (note.userId != userId) {
                val noteWithUser = note.copy(userId = userId)
                noteDao.updateNote(noteWithUser)
                noteWithUser
            } else {
                note
            }

            var isModified = false

            coroutineScope {
                // 1. Subir archivo PDF principal de NoteEntity si existe pdfPath con remoteUrl vacía
                val mainPdfDeferred = if (updatedNote.pdfPath.isNotBlank() && updatedNote.remoteUrl.isBlank()) {
                    async {
                        try {
                            firebaseStorageManager.uploadFile(
                                userId = userId,
                                localUriString = updatedNote.pdfPath,
                                fileName = Uri.parse(updatedNote.pdfPath).lastPathSegment ?: "pdf_${updatedNote.remoteUuid}.pdf"
                            )
                        } catch (e: Exception) {
                            Log.e("NoteRepository", "Error uploading main pdf to storage", e)
                            null
                        }
                    }
                } else null

                // 2. Subir imagen principal de NoteEntity si existe imageUri con remoteImageUrl vacía
                val mainImageDeferred = if (updatedNote.imageUri != null && updatedNote.remoteImageUrl.isBlank()) {
                    val imgUriStr = updatedNote.imageUri.toString()
                    if (!imgUriStr.startsWith("http")) {
                        async {
                            try {
                                firebaseStorageManager.uploadFile(
                                    userId = userId,
                                    localUriString = imgUriStr,
                                    fileName = "img_main_${updatedNote.remoteUuid}.jpg"
                                )
                            } catch (e: Exception) {
                                Log.e("NoteRepository", "Error uploading main image to storage", e)
                                null
                            }
                        }
                    } else null
                } else null

                // 3. Subir archivos adjuntos en los bloques (PDF, Imágenes, Audio) en paralelo
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
                                            fileName = if (block.fileName.isNotBlank()) block.fileName else Uri.parse(block.filePath).lastPathSegment ?: "pdf_file.pdf"
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
                                            fileName = Uri.parse(block.localUri).lastPathSegment ?: "img_${block.id}.jpg"
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
                                            fileName = if (block.fileName.isNotBlank()) block.fileName else Uri.parse(block.filePath).lastPathSegment ?: "img_${block.id}.jpg"
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
                                            fileName = Uri.parse(block.filePath).lastPathSegment ?: "audio_${block.id}.m4a"
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

                val mainPdfRemoteUrl = mainPdfDeferred?.await()
                if (!mainPdfRemoteUrl.isNullOrEmpty()) {
                    updatedNote = updatedNote.copy(remoteUrl = mainPdfRemoteUrl)
                    isModified = true
                }

                val mainImageRemoteUrl = mainImageDeferred?.await()
                if (!mainImageRemoteUrl.isNullOrEmpty()) {
                    updatedNote = updatedNote.copy(remoteImageUrl = mainImageRemoteUrl)
                    isModified = true
                }

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
                    val withContent = updatedNote.copy(content = serializedContent)
                    updatedNote = withContent.copy(attachmentsJson = buildAttachmentsJson(withContent))
                    noteDao.updateNote(updatedNote)
                }
            }

            val success = try {
                firestoreManager.pushNote(userId, updatedNote, context)
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error pushing note to firestore", e)
                false
            }

            if (success) {
                val serverTs = System.currentTimeMillis()
                noteDao.markNoteSyncedIfUnchanged(
                    remoteUuid = updatedNote.remoteUuid,
                    expectedLocalTimestamp = expectedLocalTimestamp,
                    serverTimestamp = serverTs
                )
                noteDao.markNoteSyncedById(updatedNote.id, serverTs)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error in pushSingleNoteInternal", e)
        }
    }

    suspend fun delete(note: NoteEntity) = deletePermanently(note)

    suspend fun deleteById(id: Long) {
        val note = noteDao.getNoteById(id)
        if (note != null) {
            deletePermanently(note)
        } else {
            noteDao.deleteNoteById(id)
        }
    }

    suspend fun emptyTrash(trashedNotes: List<NoteEntity> = emptyList()) {
        val userId = currentUserId
        if (!userId.isNullOrEmpty() && userId != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            val ids = trashedNotes.map { it.id }
            if (ids.isNotEmpty()) {
                noteDao.markNotesPendingDeleteBatch(ids)
                syncPendingNotes(userId)
            }
        } else {
            noteDao.emptyTrash()
        }
    }

    suspend fun moveToTrashBatch(ids: List<Long>) {
        noteDao.moveToTrashBatch(ids)
        syncPendingNotes()
    }

    suspend fun deletePermanentlyBatch(ids: List<Long>) {
        val userId = currentUserId
        if (!userId.isNullOrEmpty() && userId != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            noteDao.markNotesPendingDeleteBatch(ids)
            syncPendingNotes(userId)
        } else {
            noteDao.deletePermanentlyBatch(ids)
        }
    }

    suspend fun updateFolderBatch(ids: List<Long>, folder: String) {
        noteDao.updateFolderBatch(ids, folder)
        syncPendingNotes()
    }

    suspend fun updateColorBatch(ids: List<Long>, colorIndex: Int) {
        noteDao.updateColorBatch(ids, colorIndex)
        syncPendingNotes()
    }

    suspend fun updateNoteColor(id: Long, colorIndex: Int) {
        noteDao.updateNoteColor(id, colorIndex)
        syncPendingNotes()
    }

    suspend fun insertFolder(folder: FolderEntity): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: FolderEntity) = folderDao.deleteFolder(folder)

    suspend fun renameFolder(oldName: String, newName: String) {
        val foldersList = folderDao.getAllFoldersList()
        foldersList.find { it.name.equals(oldName, ignoreCase = true) }?.let { entity ->
            folderDao.updateFolder(entity.copy(name = newName))
        }
        noteDao.renameFolderInNotes(oldName, newName)
        syncPendingNotes()
    }

    suspend fun deleteFolderByName(folderName: String) {
        val foldersList = folderDao.getAllFoldersList()
        foldersList.find { it.name.equals(folderName, ignoreCase = true) }?.let { entity ->
            folderDao.deleteFolder(entity)
        }
        noteDao.clearFolderInNotes(folderName)
        syncPendingNotes()
    }

    suspend fun renameTag(oldTag: String, newTag: String) {
        val notesList = getAllNotes()
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
        val notesList = getAllNotes()
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
        update(note.copy(isTrashed = true, isPinned = false, isSynced = false))
    }

    suspend fun deletePermanently(note: NoteEntity) {
        val userId = currentUserId
        if (!userId.isNullOrEmpty() && userId != NoteEntity.LOCAL_ANONYMOUS_USER_ID && note.remoteUuid.isNotEmpty()) {
            // Soft-delete offline-first: se oculta de inmediato en la UI y se borra en Firestore en cuanto haya red
            noteDao.markNotePendingDelete(note.id)
            syncPendingNotes(userId)
        } else {
            noteDao.deleteNote(note)
        }
    }

    suspend fun restoreFromTrash(note: NoteEntity) {
        val updatedNote = note.copy(
            isTrashed = false,
            updatedAt = System.currentTimeMillis()
        )
        update(updatedNote)
    }

    // --- RESOLUCIÓN DE CONFLICTOS Y LECTURAS EN TIEMPO REAL (NUBE -> LOCAL) ---

    /**
     * Combina los bloques remotos con las rutas locales ya descargadas en el dispositivo
     * para evitar perder archivos locales en caché cuando llega una actualización remota.
     */
    private fun preserveLocalBlockPaths(remoteContent: String, localContent: String): String {
        if (remoteContent.isBlank() || localContent.isBlank()) return remoteContent
        return try {
            val remoteBlocks = NoteBlockSerializer.deserialize(remoteContent)
            val localBlocksMap = NoteBlockSerializer.deserialize(localContent).associateBy { it.id }

            val merged = remoteBlocks.map { remoteBlock ->
                val localBlock = localBlocksMap[remoteBlock.id] ?: return@map remoteBlock
                when (remoteBlock) {
                    is NoteBlock.Pdf -> {
                        val lb = localBlock as? NoteBlock.Pdf
                        if (lb != null && lb.filePath.isNotBlank() && File(lb.filePath).exists()) {
                            remoteBlock.copy(filePath = lb.filePath)
                        } else remoteBlock
                    }
                    is NoteBlock.ImageBlock -> {
                        val lb = localBlock as? NoteBlock.ImageBlock
                        if (lb != null && lb.localUri.isNotBlank() && File(lb.localUri).exists()) {
                            remoteBlock.copy(localUri = lb.localUri)
                        } else remoteBlock
                    }
                    is NoteBlock.Image -> {
                        val lb = localBlock as? NoteBlock.Image
                        if (lb != null && lb.filePath.isNotBlank() && File(lb.filePath).exists()) {
                            remoteBlock.copy(filePath = lb.filePath)
                        } else remoteBlock
                    }
                    is AudioBlock -> {
                        val lb = localBlock as? AudioBlock
                        if (lb != null && lb.filePath.isNotBlank() && File(lb.filePath).exists()) {
                            remoteBlock.copy(filePath = lb.filePath)
                        } else remoteBlock
                    }
                    else -> remoteBlock
                }
            }
            NoteBlockSerializer.serialize(merged)
        } catch (_: Exception) {
            remoteContent
        }
    }

    fun startListeningToCloud(userId: String) {
        stopListeningToCloud(resetToAnonymous = false)
        this.currentUserId = userId
        this._activeUserId.value = userId

        cloudSyncJob = syncScope.launch {
            // 1. Migrar notas creadas sin cuenta ('LOCAL_ANONYMOUS') al UID autenticado
            noteDao.claimAnonymousNotes(userId)

            // 2. Empujar cualquier cambio o borrado pendiente en la cola offline
            syncPendingNotes(userId)

            // 3. Escuchar cambios en tiempo real desde Firestore con resolución de conflictos
            firestoreManager.getNotesFlow(userId, context).collectLatest { remoteNotesMap ->
                remoteNotesMap.forEach { map ->
                    val remoteUuid = map["remoteUuid"] as? String ?: return@forEach
                    val remoteUpdatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L

                    val localNote = noteDao.getNoteByRemoteUuidAndUser(remoteUuid, userId)
                        ?: noteDao.getNoteByRemoteUuid(remoteUuid)

                    // CASO A: La nota está marcada localmente como pendingDelete -> El borrado local gana
                    if (localNote != null && localNote.pendingDelete) {
                        syncScope.launch {
                            if (firestoreManager.deleteNote(userId, remoteUuid)) {
                                noteDao.deleteNoteById(localNote.id)
                            }
                        }
                        return@forEach
                    }

                    // CASO B: Colisión Offline (La nota local tiene cambios sin subir `isSynced == false`
                    // y el servidor recibió una edición distinta desde otro dispositivo)
                    if (localNote != null && !localNote.isSynced) {
                        val remoteEntity = map.toNoteEntity(fallbackUserId = userId)
                        val hasDivergentContent =
                            localNote.title != remoteEntity.title || localNote.content != remoteEntity.content

                        if (localNote.updatedAtServer > 0L &&
                            remoteUpdatedAt > localNote.updatedAtServer &&
                            hasDivergentContent
                        ) {
                            // Bifurcación segura (Copia de conflicto) para garantizar CERO pérdida de datos
                            val conflictCopy = localNote.copy(
                                id = 0,
                                remoteUuid = UUID.randomUUID().toString(),
                                userId = userId,
                                title = "${localNote.title.ifBlank { "Nota" }} (Copia de conflicto)",
                                isSynced = false,
                                pendingDelete = false,
                                updatedAt = System.currentTimeMillis(),
                                lastModifiedLocal = System.currentTimeMillis()
                            )
                            val conflictId = noteDao.insertNote(conflictCopy)
                            syncToCloud(conflictCopy.copy(id = conflictId))

                            // Avanzar la nota principal a la versión remota
                            applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                        } else if (localNote.lastModifiedLocal >= remoteUpdatedAt) {
                            // La edición local es más reciente; subir a Firestore
                            syncToCloud(localNote)
                        } else {
                            applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                        }
                        return@forEach
                    }

                    // CASO C: Nota nueva en el dispositivo o nota local limpia (`isSynced == true`) desactualizada
                    if (localNote == null || remoteUpdatedAt > localNote.updatedAt) {
                        val remoteEntity = map.toNoteEntity(fallbackUserId = userId)
                        applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                    }
                }
            }
        }
    }

    private suspend fun applyRemoteNoteToLocal(
        userId: String,
        remoteUuid: String,
        remoteEntity: NoteEntity,
        localNote: NoteEntity?
    ) {
        var newNote = remoteEntity.copy(userId = userId, isSynced = true, pendingDelete = false)
        if (localNote != null) {
            val mergedContent = preserveLocalBlockPaths(newNote.content, localNote.content)
            val preservedPdfPath = if (localNote.pdfPath.isNotBlank() && File(localNote.pdfPath).exists()) {
                localNote.pdfPath
            } else {
                newNote.pdfPath
            }
            newNote = newNote.copy(
                id = localNote.id,
                content = mergedContent,
                pdfPath = preservedPdfPath,
                imageUri = localNote.imageUri ?: newNote.imageUri,
                pdfExtractedText = localNote.pdfExtractedText ?: newNote.pdfExtractedText,
                syncEnabled = localNote.syncEnabled
            )
        }

        val flattenedText = flattenNoteText(newNote)
        newNote = newNote.copy(
            searchableText = flattenedText,
            attachmentsJson = buildAttachmentsJson(newNote)
        )

        if (localNote == null) {
            noteDao.insertNote(newNote)
        } else {
            noteDao.updateNote(newNote)
        }

        // Descargar archivos adjuntos en segundo plano de forma asíncrona
        syncScope.launch {
            val currentSavedNote = noteDao.getNoteByRemoteUuidAndUser(remoteUuid, userId) ?: newNote
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

            if (updatedEntity.remoteImageUrl.isNotBlank() && updatedEntity.imageUri == null) {
                val imgDir = context?.let { File(it.filesDir, "images") }
                imgDir?.mkdirs()
                val destPath = File(imgDir ?: File(""), "img_main_${updatedEntity.remoteUuid}.jpg").absolutePath
                val downloaded = firebaseStorageManager.downloadFile(updatedEntity.remoteImageUrl, destPath)
                if (downloaded) {
                    updatedEntity = updatedEntity.copy(imageUri = Uri.fromFile(File(destPath)))
                }
            }

            if (isBlocksModified ||
                updatedEntity.pdfPath != currentSavedNote.pdfPath ||
                updatedEntity.imageUri != currentSavedNote.imageUri
            ) {
                updatedEntity = updatedEntity.copy(attachmentsJson = buildAttachmentsJson(updatedEntity))
                noteDao.updateNote(updatedEntity)
            }
        }
    }

    /**
     * Cancela la recolección activa en segundo plano, cierra el listener de Firestore
     * y aísla la vista local cambiando al espacio `LOCAL_ANONYMOUS` al cerrar sesión.
     */
    fun stopListeningToCloud(resetToAnonymous: Boolean = true) {
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        firestoreManager.clearListeners()
        currentUserId = null
        if (resetToAnonymous) {
            _activeUserId.value = NoteEntity.LOCAL_ANONYMOUS_USER_ID
        }
    }
}

