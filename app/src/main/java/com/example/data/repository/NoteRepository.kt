package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.local.UriConverters
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.MediaType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NoteMediaAttachment
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.remote.FirebaseStorageManager
import com.example.data.remote.FirestoreManager
import com.example.data.remote.OfflineSyncWorker
import com.example.data.remote.mapper.toNoteEntity
import com.example.data.remote.mapper.toReminderEntity
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.model.SyncState
import com.example.util.NoteBlockSerializer
import com.example.util.ReminderScheduler
import com.example.util.isValidForSync
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val signatureDao: SignatureDao,
    private val firestoreManager: FirestoreManager,
    private val firebaseStorageManager: FirebaseStorageManager = FirebaseStorageManager(),
    private val context: Context? = null,
    private val reminderDao: ReminderDao? = null
) {

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    private val remoteProcessMutex = Mutex()
    private val remoteReminderMutex = Mutex()
    private val inFlightNoteUuids: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val inFlightReminderUuids: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private var cloudSyncJob: Job? = null
    private var remindersSyncJob: Job? = null
    private var listeningUserId: String? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
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

    val reminders: Flow<List<ReminderEntity>> = _activeUserId.flatMapLatest { uid ->
        reminderDao?.observeRemindersForUser(uid) ?: flowOf(emptyList())
    }

    val pendingSyncCount: Flow<Int> = _activeUserId.flatMapLatest { uid ->
        noteDao.observePendingSyncCount(uid)
    }

    val anonymousNotesCount: Flow<Int> = noteDao.observeAnonymousNotesCount()

    init {
        if (context != null) {
            OfflineSyncWorker.schedulePeriodicSync(context)
        }
    }

    /**
     * Flujo reactivo de todas las notas locales no eliminadas (persistencia offline Room).
     */
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun observeNoteById(id: Long): Flow<NoteEntity?> = noteDao.observeNoteById(id)

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
        val ensuredUuid = note.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val baseNote = note.copy(
            remoteUuid = ensuredUuid,
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
    fun syncPendingNotes(userId: String? = currentUserId, forcePushAll: Boolean = false) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        OfflineSyncWorker.enqueueWhenConnected(context)
        syncScope.launch {
            syncPendingNotesSuspending(targetUid, forcePushAll = forcePushAll)
        }
    }

    suspend fun syncPendingNotesSuspending(
        userId: String? = currentUserId,
        forcePushAll: Boolean = false
    ) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid.isBlank() || targetUid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        syncMutex.withLock {
            try {
                noteDao.claimAnonymousNotes(targetUid)

                // 1. Drenar eliminaciones pendientes (pendingDelete = 1)
                val pendingDeletes = noteDao.getPendingDeletes(targetUid)
                for (noteToDelete in pendingDeletes) {
                    if (noteToDelete.remoteUuid.isNotBlank()) {
                        val deletedRemotely = firestoreManager.deleteNote(targetUid, noteToDelete.remoteUuid, context)
                        if (deletedRemotely) {
                            noteDao.deleteNoteById(noteToDelete.id)
                        } else {
                            OfflineSyncWorker.enqueueWhenConnected(context)
                        }
                    } else {
                        noteDao.deleteNoteById(noteToDelete.id)
                    }
                }

                // 2. Drenar creaciones y ediciones locales pendientes (isSynced = 0)
                val unsyncedNotes = noteDao.getUnsyncedUpserts(targetUid)
                for (dirtyNote in unsyncedNotes) {
                    pushSingleNoteInternal(targetUid, dirtyNote)
                }

                // 3. Descargar y reconciliar notas remotas desde Firestore (inserciones, ediciones y eliminaciones remotas)
                pullRemoteNotesSuspending(targetUid)

                // 4. Si se solicitó forcePushAll (sincronización manual), empujar notas locales válidas restantes
                if (forcePushAll) {
                    val remainingNotes = noteDao.getAllNotesForUserList(targetUid)
                        .filter { it.syncEnabled && !it.pendingDelete }
                    for (note in remainingNotes) {
                        pushSingleNoteInternal(targetUid, note)
                    }
                }

                // 5. Sincronizar Recordatorios y Eventos de Calendario (Offline-First + Firestore)
                reminderDao?.let { rDao ->
                    rDao.claimAnonymousReminders(targetUid)

                    val pendingRemDeletes = rDao.getPendingDeleteReminders(targetUid)
                    for (remToDelete in pendingRemDeletes) {
                        if (remToDelete.remoteUuid.isNotBlank()) {
                            val deleted = firestoreManager.deleteReminder(targetUid, remToDelete.remoteUuid)
                            if (deleted) {
                                rDao.deleteReminderById(remToDelete.id)
                            } else {
                                OfflineSyncWorker.enqueueWhenConnected(context)
                            }
                        } else {
                            rDao.deleteReminderById(remToDelete.id)
                        }
                    }

                    val unsyncedReminders = rDao.getUnsyncedReminders(targetUid)
                    for (dirtyReminder in unsyncedReminders) {
                        pushSingleReminderInternal(targetUid, dirtyReminder)
                    }

                    pullRemoteRemindersSuspending(targetUid)

                    if (forcePushAll) {
                        val allReminders = rDao.getRemindersForUserList(targetUid)
                            .filter { !it.pendingDelete }
                        for (rem in allReminders) {
                            pushSingleReminderInternal(targetUid, rem)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error drenando cola offline", e)
                OfflineSyncWorker.enqueueWhenConnected(context)
            }
        }
    }

    suspend fun pullRemoteNotesSuspending(userId: String? = currentUserId): Int {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid.isBlank() || targetUid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return 0
        return try {
            val remoteNotesMap = firestoreManager.fetchNotesOnce(targetUid)
            processRemoteNotesList(targetUid, remoteNotesMap)
            remoteNotesMap.size
        } catch (e: Exception) {
            Log.w("NoteRepository", "No se pudieron obtener notas remotas en pull puntual", e)
            0
        }
    }

    suspend fun pullRemoteRemindersSuspending(userId: String? = currentUserId): Int {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid.isBlank() || targetUid == NoteEntity.LOCAL_ANONYMOUS_USER_ID || reminderDao == null) return 0
        return try {
            val remoteRemindersMap = firestoreManager.fetchRemindersOnce(targetUid)
            processRemoteRemindersList(targetUid, remoteRemindersMap)
            remoteRemindersMap.size
        } catch (e: Exception) {
            Log.w("NoteRepository", "No se pudieron obtener recordatorios remotos en pull puntual", e)
            0
        }
    }

    // --- CRUD OFFLINE-FIRST Y SINCRONIZACIÓN DE RECORDATORIOS Y EVENTOS ---

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao?.getReminderById(id)

    suspend fun getRemindersByLinkedNoteId(noteId: Long): List<ReminderEntity> =
        reminderDao?.getRemindersByLinkedNoteId(noteId) ?: emptyList()

    suspend fun insertReminder(reminder: ReminderEntity): Long {
        val rDao = reminderDao ?: return 0L
        val now = System.currentTimeMillis()
        val effectiveUserId = currentUserId ?: _activeUserId.value
        val ensuredUuid = reminder.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val resolvedNoteUuid = if (reminder.linkedNoteUuid.isNotBlank()) {
            reminder.linkedNoteUuid
        } else if (reminder.linkedNoteId != null && reminder.linkedNoteId > 0L) {
            noteDao.getNoteById(reminder.linkedNoteId)?.remoteUuid.orEmpty()
        } else ""

        val toSave = reminder.copy(
            remoteUuid = ensuredUuid,
            userId = if (reminder.userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID || reminder.userId.isBlank()) effectiveUserId else reminder.userId,
            linkedNoteUuid = resolvedNoteUuid,
            isSynced = false,
            pendingDelete = false,
            createdAt = if (reminder.createdAt > 0L) reminder.createdAt else now,
            updatedAt = now,
            lastModifiedLocal = now
        )
        val localId = rDao.insertReminder(toSave)
        val saved = toSave.copy(id = localId)
        context?.let { ReminderScheduler.scheduleStandaloneReminder(it, saved) }
        syncReminderToCloud(saved)
        return localId
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        val rDao = reminderDao ?: return
        val now = System.currentTimeMillis()
        val effectiveUserId = currentUserId ?: _activeUserId.value
        val ensuredUuid = reminder.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val resolvedNoteUuid = if (reminder.linkedNoteUuid.isNotBlank()) {
            reminder.linkedNoteUuid
        } else if (reminder.linkedNoteId != null && reminder.linkedNoteId > 0L) {
            noteDao.getNoteById(reminder.linkedNoteId)?.remoteUuid.orEmpty()
        } else ""

        val toUpdate = reminder.copy(
            remoteUuid = ensuredUuid,
            userId = if (reminder.userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID || reminder.userId.isBlank()) effectiveUserId else reminder.userId,
            linkedNoteUuid = resolvedNoteUuid,
            isSynced = false,
            updatedAt = now,
            lastModifiedLocal = now
        )
        rDao.updateReminder(toUpdate)
        context?.let { ReminderScheduler.scheduleStandaloneReminder(it, toUpdate) }
        syncReminderToCloud(toUpdate)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        val rDao = reminderDao ?: return
        context?.let { ReminderScheduler.cancelStandaloneReminder(it, reminder.id) }
        val userId = currentUserId ?: _activeUserId.value
        if (userId.isNotBlank() && userId != NoteEntity.LOCAL_ANONYMOUS_USER_ID && reminder.remoteUuid.isNotBlank()) {
            rDao.markReminderPendingDelete(reminder.id)
            syncPendingNotes(userId)
        } else {
            rDao.deleteReminderById(reminder.id)
        }
    }

    private fun syncReminderToCloud(reminder: ReminderEntity) {
        val userId = currentUserId ?: _activeUserId.value
        if (userId.isBlank() || userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        OfflineSyncWorker.enqueueWhenConnected(context)
        syncScope.launch {
            pushSingleReminderInternal(userId, reminder)
        }
    }

    private suspend fun pushSingleReminderInternal(userId: String, reminder: ReminderEntity) {
        val rDao = reminderDao ?: return
        if (context != null && !firestoreManager.checkNetworkAvailable(context)) {
            firestoreManager.setSyncState(SyncState.OFFLINE)
            OfflineSyncWorker.enqueueWhenConnected(context)
            return
        }
        val trackingUuid = reminder.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        inFlightReminderUuids.add(trackingUuid)
        try {
            val expectedLocalTs = reminder.lastModifiedLocal
            val normalized = if (reminder.remoteUuid.isBlank() || reminder.userId != userId) {
                val updated = reminder.copy(remoteUuid = trackingUuid, userId = userId)
                rDao.updateReminder(updated)
                updated
            } else {
                reminder
            }
            val success = firestoreManager.pushReminder(userId, normalized, context)
            if (success) {
                val serverTs = System.currentTimeMillis()
                if (expectedLocalTs > 0L) {
                    rDao.markReminderSyncedIfUnchanged(
                        remoteUuid = normalized.remoteUuid,
                        expectedLocalTimestamp = expectedLocalTs,
                        serverTimestamp = serverTs
                    )
                } else {
                    rDao.markReminderSyncedById(normalized.id, serverTs)
                }
            } else {
                OfflineSyncWorker.enqueueWhenConnected(context)
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "Error pushing reminder to Firestore", e)
            OfflineSyncWorker.enqueueWhenConnected(context)
        } finally {
            inFlightReminderUuids.remove(trackingUuid)
        }
    }

    suspend fun markAllCurrentUserNotesSynced(userId: String? = currentUserId) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            noteDao.claimAnonymousNotes(targetUid)
        }
        noteDao.markAllNotesSyncedForUser(targetUid)
    }

    // --- OPERACIONES CRUD DIRECTAS EN FIRESTORE CON SINCRONIZACIÓN LOCAL EN ROOM ---

    suspend fun addNoteToFirestore(
        note: NoteEntity,
        userId: String? = currentUserId
    ): NoteEntity {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        require(targetUid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            "Se requiere un usuario autenticado para crear notas en Firestore"
        }
        val now = System.currentTimeMillis()
        val prepared = note.copy(
            remoteUuid = note.remoteUuid.ifBlank { UUID.randomUUID().toString() },
            userId = targetUid,
            searchableText = flattenNoteText(note),
            attachmentsJson = buildAttachmentsJson(note),
            isSynced = true,
            pendingDelete = false,
            lastModifiedLocal = now,
            updatedAt = now,
            updatedAtServer = now
        )
        val createdRemote = firestoreManager.addNote(targetUid, prepared, context)
        val localId = noteDao.insertNote(createdRemote)
        return createdRemote.copy(id = localId)
    }

    suspend fun readNoteFromFirestore(
        remoteUuid: String,
        userId: String? = currentUserId
    ): NoteEntity? {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        require(targetUid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            "Se requiere un usuario autenticado para leer notas de Firestore"
        }
        val remoteEntity = firestoreManager.readNote(targetUid, remoteUuid) ?: return null
        val localNote = noteDao.getNoteByRemoteUuidAndUser(remoteUuid, targetUid)
            ?: noteDao.getNoteByRemoteUuid(remoteUuid)
        applyRemoteNoteToLocal(targetUid, remoteUuid, remoteEntity, localNote)
        return noteDao.getNoteByRemoteUuidAndUser(remoteUuid, targetUid) ?: remoteEntity
    }

    suspend fun readAllNotesFromFirestore(
        userId: String? = currentUserId
    ): List<NoteEntity> {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        require(targetUid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            "Se requiere un usuario autenticado para listar notas de Firestore"
        }
        val remoteMaps = firestoreManager.fetchNotesOnce(targetUid)
        processRemoteNotesList(targetUid, remoteMaps)
        return remoteMaps.map { it.toNoteEntity(fallbackUserId = targetUid) }
    }

    suspend fun updateNoteInFirestore(
        note: NoteEntity,
        userId: String? = currentUserId
    ): NoteEntity {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        require(targetUid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            "Se requiere un usuario autenticado para actualizar notas en Firestore"
        }
        val now = System.currentTimeMillis()
        val prepared = note.copy(
            userId = targetUid,
            searchableText = flattenNoteText(note),
            attachmentsJson = buildAttachmentsJson(note),
            isSynced = true,
            lastModifiedLocal = now,
            updatedAt = now,
            updatedAtServer = now
        )
        val updatedRemote = firestoreManager.updateNote(targetUid, prepared, context)
        if (updatedRemote.id > 0L) {
            noteDao.updateNote(updatedRemote)
        } else {
            val existing = noteDao.getNoteByRemoteUuidAndUser(updatedRemote.remoteUuid, targetUid)
            if (existing != null) {
                noteDao.updateNote(updatedRemote.copy(id = existing.id))
            }
        }
        return updatedRemote
    }

    suspend fun deleteNoteFromFirestore(
        remoteUuid: String,
        userId: String? = currentUserId
    ): Boolean {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        require(targetUid.isNotBlank() && targetUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            "Se requiere un usuario autenticado para eliminar notas en Firestore"
        }
        val deleted = firestoreManager.deleteNote(targetUid, remoteUuid, context)
        if (deleted) {
            noteDao.getNoteByRemoteUuidAndUser(remoteUuid, targetUid)?.let {
                noteDao.deleteNoteById(it.id)
            }
        }
        return deleted
    }

    private fun syncToCloud(note: NoteEntity) {
        val userId = currentUserId ?: _activeUserId.value
        if (userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return

        OfflineSyncWorker.enqueueWhenConnected(context)
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

        if (context != null && !firestoreManager.checkNetworkAvailable(context)) {
            firestoreManager.setSyncState(SyncState.OFFLINE)
            OfflineSyncWorker.enqueueWhenConnected(context)
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

            val trackingUuid = updatedNote.remoteUuid
            inFlightNoteUuids.add(trackingUuid)
            var isModified = false
            var hasFailedAttachmentUpload = false

            coroutineScope {
                val needsMainPdfUpload = updatedNote.pdfPath.isNotBlank() && updatedNote.remoteUrl.isBlank()
                val mainPdfDeferred = if (needsMainPdfUpload) {
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

                val needsMainImageUpload = updatedNote.imageUri != null &&
                    updatedNote.remoteImageUrl.isBlank() &&
                    !updatedNote.imageUri.toString().startsWith("http")
                val mainImageDeferred = if (needsMainImageUpload) {
                    val imgUriStr = updatedNote.imageUri.toString()
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
                                    if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else null
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
                                    if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else null
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
                                    if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else null
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
                                    if (!url.isNullOrEmpty()) block.copy(remoteUrl = url) else null
                                }
                            } else null
                        }
                        else -> null
                    }
                }

                if (mainPdfDeferred != null) {
                    val mainPdfRemoteUrl = mainPdfDeferred.await()
                    if (!mainPdfRemoteUrl.isNullOrEmpty()) {
                        updatedNote = updatedNote.copy(remoteUrl = mainPdfRemoteUrl)
                        isModified = true
                    } else {
                        hasFailedAttachmentUpload = true
                    }
                }

                if (mainImageDeferred != null) {
                    val mainImageRemoteUrl = mainImageDeferred.await()
                    if (!mainImageRemoteUrl.isNullOrEmpty()) {
                        updatedNote = updatedNote.copy(remoteImageUrl = mainImageRemoteUrl)
                        isModified = true
                    } else {
                        hasFailedAttachmentUpload = true
                    }
                }

                val updatedBlocks = blocks.mapIndexed { index, originalBlock ->
                    val deferred = blockUploadDeferreds[index]
                    if (deferred != null) {
                        val uploadedBlock = deferred.await()
                        if (uploadedBlock != null) {
                            if (uploadedBlock != originalBlock) {
                                isModified = true
                            }
                            uploadedBlock
                        } else {
                            hasFailedAttachmentUpload = true
                            originalBlock
                        }
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
            } finally {
                inFlightNoteUuids.remove(trackingUuid)
            }

            if (success) {
                val serverTs = System.currentTimeMillis()
                if (expectedLocalTimestamp > 0L) {
                    noteDao.markNoteSyncedIfUnchanged(
                        remoteUuid = updatedNote.remoteUuid,
                        expectedLocalTimestamp = expectedLocalTimestamp,
                        serverTimestamp = serverTs
                    )
                } else {
                    noteDao.markNoteSyncedById(updatedNote.id, serverTs)
                }
            } else {
                OfflineSyncWorker.enqueueWhenConnected(context)
            }
        } catch (e: Exception) {
            note.remoteUuid.takeIf { it.isNotBlank() }?.let { inFlightNoteUuids.remove(it) }
            Log.e("NoteRepository", "Error in pushSingleNoteInternal", e)
            OfflineSyncWorker.enqueueWhenConnected(context)
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

    private fun registerNetworkMonitor(userId: String) {
        val appContext = context?.applicationContext ?: return
        unregisterNetworkMonitor()
        try {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val activeUid = currentUserId?.takeIf { it.isNotBlank() } ?: userId
                    if (activeUid.isNotBlank() && activeUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                        syncPendingNotes(activeUid, forcePushAll = false)
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        val activeUid = currentUserId?.takeIf { it.isNotBlank() } ?: userId
                        if (activeUid.isNotBlank() && activeUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                            syncPendingNotes(activeUid, forcePushAll = false)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    if (!firestoreManager.checkNetworkAvailable(appContext)) {
                        firestoreManager.setSyncState(SyncState.OFFLINE)
                    }
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            Log.w("NoteRepository", "No se pudo registrar NetworkCallback para sincronización automática", e)
        }
    }

    private fun unregisterNetworkMonitor() {
        val appContext = context?.applicationContext ?: return
        val callback = networkCallback ?: return
        networkCallback = null
        try {
            val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    fun startListeningToCloud(userId: String) {
        if (userId.isBlank() || userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        this.currentUserId = userId
        this._activeUserId.value = userId

        if (listeningUserId == userId && cloudSyncJob?.isActive == true) {
            syncPendingNotes(userId, forcePushAll = false)
            return
        }

        stopListeningToCloud(resetToAnonymous = false)
        this.currentUserId = userId
        this._activeUserId.value = userId
        this.listeningUserId = userId
        registerNetworkMonitor(userId)

        cloudSyncJob = syncScope.launch {
            // 1. Migrar notas y recordatorios creados sin cuenta ('LOCAL_ANONYMOUS') al UID autenticado
            noteDao.claimAnonymousNotes(userId)
            reminderDao?.claimAnonymousReminders(userId)

            // 2. Empujar cambios locales pendientes en paralelo sin bloquear el listener en tiempo real
            syncScope.launch {
                syncPendingNotesSuspending(userId, forcePushAll = false)
            }

            // 3. Conectar inmediatamente el listener de Recordatorios en tiempo real desde Firestore
            if (reminderDao != null) {
                remindersSyncJob?.cancel()
                remindersSyncJob = syncScope.launch {
                    firestoreManager.getRemindersFlow(userId, context)
                        .retryWhen { cause, attempt ->
                            if (cause is CancellationException) {
                                false
                            } else {
                                delay(((attempt + 1).coerceAtMost(5)) * 2000L)
                                true
                            }
                        }
                        .catch { e ->
                            Log.w("NoteRepository", "Listener de Recordatorios interrumpido: ${e.localizedMessage}")
                        }
                        .collect { remoteRemindersMap ->
                            processRemoteRemindersList(userId, remoteRemindersMap)
                        }
                }
            }

            // 4. Conectar inmediatamente el listener de Notas en tiempo real desde Firestore
            firestoreManager.getNotesFlow(userId, context)
                .retryWhen { cause, attempt ->
                    if (cause is CancellationException) {
                        false
                    } else {
                        Log.w("NoteRepository", "Reintentando listener de Firestore tras error: ${cause.localizedMessage}")
                        delay(((attempt + 1).coerceAtMost(5)) * 2000L)
                        true
                    }
                }
                .catch { e ->
                    Log.w("NoteRepository", "Listener de Firestore interrumpido: ${e.localizedMessage}")
                }
                .collect { remoteNotesMap ->
                    processRemoteNotesList(userId, remoteNotesMap)
                }
        }
    }

    internal suspend fun processRemoteRemindersList(
        userId: String,
        remoteRemindersMap: List<Map<String, Any>>
    ) {
        val rDao = reminderDao ?: return
        remoteReminderMutex.withLock {
            val remoteUuids = mutableSetOf<String>()

            remoteRemindersMap.forEach { map ->
                val remoteUuid = (map["remoteUuid"] as? String)?.takeIf { it.isNotBlank() } ?: return@forEach
                remoteUuids.add(remoteUuid)

                if (remoteUuid in inFlightReminderUuids) {
                    return@forEach
                }

                val serverTs = (map["updatedAt"] as? Timestamp)?.toDate()?.time
                    ?: (map["updatedAtServer"] as? Number)?.toLong()
                    ?: (map["updatedAt"] as? Number)?.toLong()
                    ?: 0L
                val remoteLocalTs = (map["lastModifiedLocal"] as? Number)?.toLong() ?: 0L
                val remoteUpdatedAt = maxOf(serverTs, remoteLocalTs)

                val localRem = rDao.getReminderByRemoteUuidAndUser(remoteUuid, userId)
                    ?: rDao.getReminderByRemoteUuid(remoteUuid)

                if (localRem != null && localRem.pendingDelete) {
                    syncScope.launch {
                        if (firestoreManager.deleteReminder(userId, remoteUuid)) {
                            rDao.deleteReminderById(localRem.id)
                        }
                    }
                    return@forEach
                }

                var remoteEntity = map.toReminderEntity(fallbackUserId = userId)
                // Reconcile linkedNoteId across devices using linkedNoteUuid if available
                if (remoteEntity.linkedNoteUuid.isNotBlank()) {
                    val linkedNote = noteDao.getNoteByRemoteUuidAndUser(remoteEntity.linkedNoteUuid, userId)
                        ?: noteDao.getNoteByRemoteUuid(remoteEntity.linkedNoteUuid)
                    if (linkedNote != null) {
                        remoteEntity = remoteEntity.copy(
                            linkedNoteId = linkedNote.id,
                            linkedNoteTitle = linkedNote.title.ifBlank { remoteEntity.linkedNoteTitle }
                        )
                    }
                }

                if (localRem != null && !localRem.isSynced) {
                    if (localRem.lastModifiedLocal > remoteUpdatedAt) {
                        syncReminderToCloud(localRem)
                    } else {
                        val merged = remoteEntity.copy(id = localRem.id, userId = userId, isSynced = true, pendingDelete = false)
                        rDao.updateReminder(merged)
                        context?.let { ReminderScheduler.scheduleStandaloneReminder(it, merged) }
                    }
                    return@forEach
                }

                if (localRem == null) {
                    val newId = rDao.insertReminder(remoteEntity.copy(userId = userId, isSynced = true, pendingDelete = false))
                    context?.let { ReminderScheduler.scheduleStandaloneReminder(it, remoteEntity.copy(id = newId)) }
                } else if (remoteUpdatedAt >= localRem.updatedAt ||
                    localRem.title != remoteEntity.title ||
                    localRem.scheduledAt != remoteEntity.scheduledAt ||
                    localRem.endTimeMillis != remoteEntity.endTimeMillis ||
                    localRem.isCompleted != remoteEntity.isCompleted ||
                    localRem.isAllDay != remoteEntity.isAllDay ||
                    localRem.priority != remoteEntity.priority ||
                    localRem.listName != remoteEntity.listName ||
                    localRem.subtasksJson != remoteEntity.subtasksJson
                ) {
                    val updated = remoteEntity.copy(id = localRem.id, userId = userId, isSynced = true, pendingDelete = false)
                    rDao.updateReminder(updated)
                    context?.let { ReminderScheduler.scheduleStandaloneReminder(it, updated) }
                }
            }

            // Reconciliar eliminaciones remotas de recordatorios
            val localUserReminders = rDao.getRemindersForUserList(userId)
            for (localRem in localUserReminders) {
                if (localRem.userId == userId &&
                    localRem.isSynced &&
                    !localRem.pendingDelete &&
                    localRem.remoteUuid.isNotBlank() &&
                    localRem.remoteUuid !in remoteUuids &&
                    localRem.remoteUuid !in inFlightReminderUuids
                ) {
                    context?.let { ReminderScheduler.cancelStandaloneReminder(it, localRem.id) }
                    rDao.deleteReminderById(localRem.id)
                }
            }
        }
    }

    internal suspend fun processRemoteNotesList(
        userId: String,
        remoteNotesMap: List<Map<String, Any>>
    ) {
        remoteProcessMutex.withLock {
            val existingFolders = try {
                folderDao.getAllFoldersList().map { it.name.lowercase() }.toMutableSet()
            } catch (_: Exception) {
                mutableSetOf()
            }

            val remoteUuids = mutableSetOf<String>()

            remoteNotesMap.forEach { map ->
                val remoteUuid = (map["remoteUuid"] as? String)?.takeIf { it.isNotBlank() } ?: return@forEach
                remoteUuids.add(remoteUuid)

                // Si esta nota está siendo subida activamente en este instante desde este dispositivo,
                // evitamos procesar su eco parcial hasta que termine la subida.
                if (remoteUuid in inFlightNoteUuids) {
                    return@forEach
                }

                val serverTs = (map["updatedAt"] as? Timestamp)?.toDate()?.time
                    ?: (map["updatedAtServer"] as? Number)?.toLong()
                    ?: (map["updatedAt"] as? Number)?.toLong()
                    ?: 0L
                val remoteLocalTs = (map["lastModifiedLocal"] as? Number)?.toLong() ?: 0L
                val remoteUpdatedAt = maxOf(serverTs, remoteLocalTs)

                val remoteFolder = (map["folder"] as? String)?.trim().orEmpty()
                if (remoteFolder.isNotEmpty() &&
                    !remoteFolder.equals("Notas", ignoreCase = true) &&
                    !existingFolders.contains(remoteFolder.lowercase())
                ) {
                    try {
                        folderDao.insertFolder(FolderEntity(name = remoteFolder))
                        existingFolders.add(remoteFolder.lowercase())
                    } catch (_: Exception) {
                    }
                }

                val localNote = noteDao.getNoteByRemoteUuidAndUser(remoteUuid, userId)
                    ?: noteDao.getNoteByRemoteUuid(remoteUuid)

                // CASO A: La nota está marcada localmente como pendingDelete -> El borrado local gana
                if (localNote != null && localNote.pendingDelete) {
                    syncScope.launch {
                        if (firestoreManager.deleteNote(userId, remoteUuid, context)) {
                            noteDao.deleteNoteById(localNote.id)
                        }
                    }
                    return@forEach
                }

                val remoteEntity = map.toNoteEntity(fallbackUserId = userId)

                // CASO B: La nota local tiene `isSynced == false`
                if (localNote != null && !localNote.isSynced) {
                    val hasDivergentContent = hasNoteContentDiverged(localNote, remoteEntity)

                    if (!hasDivergentContent && !hasRemoteNoteChanged(localNote, remoteEntity, localNote.updatedAt)) {
                        // El servidor ya tiene exactamente nuestros cambios locales: marcar como sincronizada sin re-subir
                        val resolvedServerTs = if (serverTs > 0L) serverTs else System.currentTimeMillis()
                        noteDao.markNoteSyncedIfUnchanged(
                            remoteUuid = remoteUuid,
                            expectedLocalTimestamp = localNote.lastModifiedLocal,
                            serverTimestamp = resolvedServerTs
                        )
                    } else if (localNote.updatedAtServer > 0L &&
                        remoteUpdatedAt > localNote.updatedAtServer &&
                        hasDivergentContent
                    ) {
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

                        applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                    } else if (localNote.lastModifiedLocal >= remoteUpdatedAt) {
                        syncToCloud(localNote)
                    } else {
                        applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                    }
                    return@forEach
                }

                // CASO C: Nota nueva en el dispositivo (`localNote == null`) o nota local sincronizada (`isSynced == true`)
                // que presenta cambios en Firestore
                if (localNote == null || hasRemoteNoteChanged(localNote, remoteEntity, remoteUpdatedAt)) {
                    applyRemoteNoteToLocal(userId, remoteUuid, remoteEntity, localNote)
                }
            }

            // CASO D: Reconciliación de eliminaciones remotas (notas previamente sincronizadas localmente
            // que fueron eliminadas en Firestore desde otro dispositivo)
            val localUserNotes = noteDao.getAllNotesForUserList(userId)
            for (localNote in localUserNotes) {
                if (localNote.userId == userId &&
                    localNote.isSynced &&
                    !localNote.pendingDelete &&
                    localNote.syncEnabled &&
                    localNote.remoteUuid.isNotBlank() &&
                    localNote.remoteUuid !in remoteUuids &&
                    localNote.remoteUuid !in inFlightNoteUuids
                ) {
                    noteDao.deleteNoteById(localNote.id)
                }
            }
        }
    }

    private fun hasNoteContentDiverged(
        localNote: NoteEntity,
        remoteEntity: NoteEntity
    ): Boolean {
        val mergedRemoteContent = preserveLocalBlockPaths(remoteEntity.content, localNote.content)
        return localNote.title != remoteEntity.title ||
            localNote.content != mergedRemoteContent ||
            localNote.checklistData != remoteEntity.checklistData ||
            localNote.drawingData != remoteEntity.drawingData ||
            localNote.mathEvaluations != remoteEntity.mathEvaluations ||
            localNote.pdfAnnotationData != remoteEntity.pdfAnnotationData
    }

    private fun hasRemoteNoteChanged(
        localNote: NoteEntity,
        remoteEntity: NoteEntity,
        remoteUpdatedAt: Long
    ): Boolean {
        val mergedContent = preserveLocalBlockPaths(remoteEntity.content, localNote.content)
        val preservedPdfPath = if (localNote.pdfPath.isNotBlank() && File(localNote.pdfPath).exists()) {
            localNote.pdfPath
        } else {
            remoteEntity.pdfPath
        }
        return remoteUpdatedAt > localNote.updatedAt ||
            localNote.userId != remoteEntity.userId ||
            localNote.title != remoteEntity.title ||
            localNote.content != mergedContent ||
            localNote.noteType != remoteEntity.noteType ||
            localNote.colorIndex != remoteEntity.colorIndex ||
            localNote.paperStyle != remoteEntity.paperStyle ||
            localNote.isPinned != remoteEntity.isPinned ||
            localNote.isArchived != remoteEntity.isArchived ||
            localNote.isTrashed != remoteEntity.isTrashed ||
            localNote.isLocked != remoteEntity.isLocked ||
            localNote.lockPin != remoteEntity.lockPin ||
            localNote.tags != remoteEntity.tags ||
            localNote.folder != remoteEntity.folder ||
            localNote.checklistData != remoteEntity.checklistData ||
            localNote.drawingData != remoteEntity.drawingData ||
            localNote.mathEvaluations != remoteEntity.mathEvaluations ||
            localNote.pdfPath != preservedPdfPath ||
            localNote.pdfAnnotationData != remoteEntity.pdfAnnotationData ||
            localNote.reminderTimestamp != remoteEntity.reminderTimestamp ||
            localNote.isReminderDone != remoteEntity.isReminderDone ||
            localNote.remoteUrl != remoteEntity.remoteUrl ||
            localNote.remoteImageUrl != remoteEntity.remoteImageUrl
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
        unregisterNetworkMonitor()
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        remindersSyncJob?.cancel()
        remindersSyncJob = null
        listeningUserId = null
        firestoreManager.clearListeners()
        currentUserId = null
        if (resetToAnonymous) {
            _activeUserId.value = NoteEntity.LOCAL_ANONYMOUS_USER_ID
        }
    }
}

