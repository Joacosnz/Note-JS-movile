package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.data.local.dao.CalendarEventDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import com.example.data.remote.FirestoreManager
import com.example.data.remote.OfflineSyncWorker
import com.example.data.remote.mapper.toReminderEntity
import com.example.model.SyncState
import com.example.util.GoogleCalendarSyncManager
import com.example.util.ReminderScheduler
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositorio Offline-First para la gestión de [Reminder], [CalendarEvent] y [ReminderEntity]
 * en Room con sincronización bidireccional en tiempo real hacia Firebase Firestore (`/users/{userId}/reminders`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val calendarEventDao: CalendarEventDao,
    private val firestoreManager: FirestoreManager,
    private val noteDao: NoteDao? = null,
    private val context: Context? = null
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val syncMutex = Mutex()
    private val remoteReconcileMutex = Mutex()
    private val inFlightUuids: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private var realtimeJob: Job? = null
    private var listeningUserId: String? = null

    private val _activeUserId = MutableStateFlow(resolveCurrentAuthUid())
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    val syncState: StateFlow<SyncState> = firestoreManager.syncState

    private fun notifyWidgetsUpdate() {
        context?.let { ctx ->
            syncScope.launch {
                try {
                    com.example.widget.CalendarRemindersWidget().updateAll(ctx)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Flujo reactivo de recordatorios (`Reminder`) desde Room para el usuario activo.
     */
    val reminders: Flow<List<Reminder>> = _activeUserId.flatMapLatest { uid ->
        combine(
            reminderDao.observeReminderItemsForUser(uid),
            reminderDao.observeRemindersForUser(uid)
        ) { items, unifiedEntities ->
            val byUuid = linkedMapOf<String, Reminder>()
            items.forEach { item ->
                val key = item.remoteUuid.ifBlank { "rem_item_${item.id}" }
                byUuid[key] = item
            }
            unifiedEntities.filter { it.isReminder && !it.pendingDelete }.forEach { entity ->
                val key = entity.remoteUuid.ifBlank { "rem_ent_${entity.id}" }
                if (!byUuid.containsKey(key)) {
                    byUuid[key] = Reminder.fromReminderEntity(entity)
                }
            }
            byUuid.values.sortedWith(compareBy<Reminder> { it.timestamp }.thenByDescending { it.priority })
        }
    }

    /**
     * Flujo reactivo de eventos de calendario (`CalendarEvent`) desde Room para el usuario activo.
     */
    val calendarEvents: Flow<List<CalendarEvent>> = _activeUserId.flatMapLatest { uid ->
        combine(
            calendarEventDao.observeCalendarEventsForUser(uid),
            reminderDao.observeRemindersForUser(uid)
        ) { events, unifiedEntities ->
            val byUuid = linkedMapOf<String, CalendarEvent>()
            events.forEach { ev ->
                val key = ev.remoteUuid.ifBlank { "cal_ev_${ev.id}" }
                byUuid[key] = ev
            }
            unifiedEntities.filter { it.isEvent && !it.pendingDelete }.forEach { entity ->
                val key = entity.remoteUuid.ifBlank { "cal_ent_${entity.id}" }
                if (!byUuid.containsKey(key)) {
                    byUuid[key] = CalendarEvent.fromReminderEntity(entity)
                }
            }
            byUuid.values.sortedBy { it.timestamp }
        }
    }

    /**
     * Flujo reactivo unificado de todos los elementos de calendario (recordatorios + eventos) en Room.
     */
    val calendarItems: Flow<List<ReminderEntity>> = _activeUserId.flatMapLatest { uid ->
        combine(
            reminderDao.observeRemindersForUser(uid),
            reminderDao.observeReminderItemsForUser(uid),
            calendarEventDao.observeCalendarEventsForUser(uid)
        ) { unifiedList, reminderItems, calendarEvents ->
            val byUuid = linkedMapOf<String, ReminderEntity>()
            unifiedList.forEach { entity ->
                val key = entity.remoteUuid.ifBlank { "u_${entity.id}" }
                byUuid[key] = entity
            }
            reminderItems.forEach { rem ->
                val key = rem.remoteUuid.ifBlank { "r_${rem.id}" }
                val existing = byUuid[key]
                if (existing == null || rem.lastModifiedLocal > existing.lastModifiedLocal) {
                    byUuid[key] = rem.toReminderEntity().copy(id = existing?.id ?: rem.id)
                }
            }
            calendarEvents.forEach { ev ->
                val key = ev.remoteUuid.ifBlank { "e_${ev.id}" }
                val existing = byUuid[key]
                if (existing == null || ev.lastModifiedLocal > existing.lastModifiedLocal) {
                    byUuid[key] = ev.toReminderEntity().copy(id = existing?.id ?: ev.id)
                }
            }
            byUuid.values.sortedWith(compareBy<ReminderEntity> { it.scheduledAt }.thenByDescending { it.priority })
        }
    }

    fun observeRemindersByLinkedNoteId(noteId: Long): Flow<List<Reminder>> =
        reminderDao.observeReminderItemsByLinkedNoteId(noteId)

    fun observeCalendarEventsByLinkedNoteId(noteId: Long): Flow<List<CalendarEvent>> =
        calendarEventDao.observeCalendarEventsByLinkedNoteId(noteId)

    fun observeCalendarEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>> =
        calendarEventDao.observeCalendarEventsInRange(startMillis, endMillis)

    suspend fun getReminderById(id: Long): Reminder? {
        reminderDao.getReminderItemById(id)?.let { return it }
        return reminderDao.getReminderById(id)?.takeIf { it.isReminder }?.let { Reminder.fromReminderEntity(it) }
    }

    suspend fun getCalendarEventById(id: Long): CalendarEvent? {
        calendarEventDao.getCalendarEventById(id)?.let { return it }
        return reminderDao.getReminderById(id)?.takeIf { it.isEvent }?.let { CalendarEvent.fromReminderEntity(it) }
    }

    suspend fun getCalendarItemById(id: Long): ReminderEntity? =
        reminderDao.getReminderById(id)

    // --- INSERCIÓN, ACTUALIZACIÓN Y ELIMINACIÓN DE RECORDATORIOS (`Reminder`) ---

    suspend fun insertReminder(reminder: Reminder): Long {
        val now = System.currentTimeMillis()
        val effectiveUid = resolveEffectiveUserId(reminder.userId)
        val ensuredUuid = reminder.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val (validNoteId, resolvedNoteUuid, resolvedNoteTitle) = resolveLinkedNoteMetadata(
            linkedNoteId = reminder.linkedNoteId,
            fallbackUuid = reminder.linkedNoteUuid,
            fallbackTitle = reminder.linkedNoteTitle
        )

        val toSave = reminder.copy(
            id = 0L,
            title = reminder.title.trim().ifBlank { "Nuevo recordatorio" },
            remoteUuid = ensuredUuid,
            userId = effectiveUid,
            linkedNoteId = validNoteId,
            linkedNoteUuid = resolvedNoteUuid,
            linkedNoteTitle = resolvedNoteTitle,
            isSynced = false,
            pendingDelete = false,
            createdAt = if (reminder.createdAt > 0L) reminder.createdAt else now,
            updatedAt = now,
            lastModifiedLocal = now
        )

        val itemId = reminderDao.insertReminderItem(toSave)
        val savedReminder = toSave.copy(id = itemId)

        // Espejar en tabla unificada `reminders` para compatibilidad con vistas de calendario y alarmas
        val existingUnified = reminderDao.getReminderByRemoteUuid(ensuredUuid)
        val unifiedToSave = savedReminder.toReminderEntity().copy(id = existingUnified?.id ?: 0L)
        val unifiedId = if (existingUnified != null) {
            reminderDao.updateReminder(unifiedToSave)
            existingUnified.id
        } else {
            reminderDao.insertReminder(unifiedToSave)
        }
        val scheduledEntity = unifiedToSave.copy(id = unifiedId)
        context?.let { ctx ->
            ReminderScheduler.scheduleStandaloneReminder(ctx, scheduledEntity)
            syncScope.launch {
                GoogleCalendarSyncManager.syncItemToGoogleCalendar(ctx, scheduledEntity)
            }
        }

        syncItemToFirestoreAsync(scheduledEntity)
        notifyWidgetsUpdate()
        return itemId
    }

    suspend fun updateReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        val effectiveUid = resolveEffectiveUserId(reminder.userId)
        val ensuredUuid = reminder.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val (validNoteId, resolvedNoteUuid, resolvedNoteTitle) = resolveLinkedNoteMetadata(
            linkedNoteId = reminder.linkedNoteId,
            fallbackUuid = reminder.linkedNoteUuid,
            fallbackTitle = reminder.linkedNoteTitle
        )

        val existingItem = if (reminder.id > 0L) {
            reminderDao.getReminderItemById(reminder.id)
        } else {
            reminderDao.getReminderItemByRemoteUuid(ensuredUuid)
        }

        val toUpdate = reminder.copy(
            id = existingItem?.id ?: reminder.id,
            title = reminder.title.trim().ifBlank { "Recordatorio" },
            remoteUuid = ensuredUuid,
            userId = effectiveUid,
            linkedNoteId = validNoteId,
            linkedNoteUuid = resolvedNoteUuid,
            linkedNoteTitle = resolvedNoteTitle,
            isSynced = false,
            pendingDelete = false,
            updatedAt = now,
            lastModifiedLocal = now
        )

        if (toUpdate.id > 0L && existingItem != null) {
            reminderDao.updateReminderItem(toUpdate)
        } else {
            reminderDao.insertReminderItem(toUpdate.copy(id = 0L))
        }

        val existingUnified = reminderDao.getReminderByRemoteUuid(ensuredUuid)
            ?: (if (reminder.id > 0L) reminderDao.getReminderById(reminder.id) else null)
        val unifiedEntity = toUpdate.toReminderEntity().copy(id = existingUnified?.id ?: 0L)
        val unifiedId = if (existingUnified != null) {
            reminderDao.updateReminder(unifiedEntity)
            existingUnified.id
        } else {
            reminderDao.insertReminder(unifiedEntity)
        }

        val scheduledEntity = unifiedEntity.copy(id = unifiedId)
        context?.let { ctx ->
            ReminderScheduler.scheduleStandaloneReminder(ctx, scheduledEntity)
            syncScope.launch {
                GoogleCalendarSyncManager.syncItemToGoogleCalendar(ctx, scheduledEntity)
            }
        }
        syncItemToFirestoreAsync(scheduledEntity)
        notifyWidgetsUpdate()
    }

    suspend fun toggleReminderCompleted(reminder: Reminder) {
        val now = System.currentTimeMillis()
        val newCompleted = !reminder.isCompleted
        val updated = reminder.copy(
            isCompleted = newCompleted,
            completedAt = if (newCompleted) now else 0L,
            updatedAt = now,
            lastModifiedLocal = now
        )
        updateReminder(updated)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        val targetUuid = reminder.remoteUuid
        val existingUnified = if (targetUuid.isNotBlank()) {
            reminderDao.getReminderByRemoteUuid(targetUuid)
        } else {
            reminderDao.getReminderById(reminder.id)
        }
        existingUnified?.let {
            context?.let { ctx -> ReminderScheduler.cancelStandaloneReminder(ctx, it.id) }
        }
        val uuidForGCal = targetUuid.ifBlank { existingUnified?.remoteUuid.orEmpty() }
        if (uuidForGCal.isNotBlank()) {
            context?.let { ctx ->
                syncScope.launch {
                    GoogleCalendarSyncManager.deleteItemFromGoogleCalendar(ctx, uuidForGCal)
                }
            }
        }

        val uid = _activeUserId.value
        if (uid.isNotBlank() && uid != NoteEntity.LOCAL_ANONYMOUS_USER_ID && targetUuid.isNotBlank()) {
            if (reminder.id > 0L) {
                reminderDao.markReminderItemPendingDelete(reminder.id)
            }
            existingUnified?.let { reminderDao.markReminderPendingDelete(it.id) }
            syncBidirectional(uid)
        } else {
            if (reminder.id > 0L) {
                reminderDao.deleteReminderItemById(reminder.id)
            }
            if (targetUuid.isNotBlank()) {
                reminderDao.deleteReminderItemByRemoteUuid(targetUuid)
            }
            existingUnified?.let { reminderDao.deleteReminderById(it.id) }
        }
        notifyWidgetsUpdate()
    }

    suspend fun deleteReminderById(id: Long) {
        val reminder = getReminderById(id)
        if (reminder != null) {
            deleteReminder(reminder)
        } else {
            reminderDao.deleteReminderItemById(id)
            reminderDao.deleteReminderById(id)
        }
    }

    // --- INSERCIÓN, ACTUALIZACIÓN Y ELIMINACIÓN DE EVENTOS DE CALENDARIO (`CalendarEvent`) ---

    suspend fun insertCalendarEvent(event: CalendarEvent): Long {
        val now = System.currentTimeMillis()
        val effectiveUid = resolveEffectiveUserId(event.userId)
        val ensuredUuid = event.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val (validNoteId, resolvedNoteUuid, resolvedNoteTitle) = resolveLinkedNoteMetadata(
            linkedNoteId = event.linkedNoteId,
            fallbackUuid = event.linkedNoteUuid,
            fallbackTitle = event.linkedNoteTitle
        )

        val toSave = event.copy(
            id = 0L,
            title = event.title.trim().ifBlank { "Nuevo evento" },
            endTimestamp = if (event.endTimestamp > event.timestamp) event.endTimestamp else event.timestamp + 3_600_000L,
            remoteUuid = ensuredUuid,
            userId = effectiveUid,
            linkedNoteId = validNoteId,
            linkedNoteUuid = resolvedNoteUuid,
            linkedNoteTitle = resolvedNoteTitle,
            isSynced = false,
            pendingDelete = false,
            createdAt = if (event.createdAt > 0L) event.createdAt else now,
            updatedAt = now,
            lastModifiedLocal = now
        )

        val eventId = calendarEventDao.insertCalendarEvent(toSave)
        val savedEvent = toSave.copy(id = eventId)

        val existingUnified = reminderDao.getReminderByRemoteUuid(ensuredUuid)
        val unifiedToSave = savedEvent.toReminderEntity().copy(id = existingUnified?.id ?: 0L)
        val unifiedId = if (existingUnified != null) {
            reminderDao.updateReminder(unifiedToSave)
            existingUnified.id
        } else {
            reminderDao.insertReminder(unifiedToSave)
        }
        val scheduledEntity = unifiedToSave.copy(id = unifiedId)
        context?.let { ctx ->
            ReminderScheduler.scheduleStandaloneReminder(ctx, scheduledEntity)
            syncScope.launch {
                GoogleCalendarSyncManager.syncItemToGoogleCalendar(ctx, scheduledEntity)
            }
        }

        syncItemToFirestoreAsync(scheduledEntity)
        notifyWidgetsUpdate()
        return eventId
    }

    suspend fun updateCalendarEvent(event: CalendarEvent) {
        val now = System.currentTimeMillis()
        val effectiveUid = resolveEffectiveUserId(event.userId)
        val ensuredUuid = event.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        val (validNoteId, resolvedNoteUuid, resolvedNoteTitle) = resolveLinkedNoteMetadata(
            linkedNoteId = event.linkedNoteId,
            fallbackUuid = event.linkedNoteUuid,
            fallbackTitle = event.linkedNoteTitle
        )

        val existingEvent = if (event.id > 0L) {
            calendarEventDao.getCalendarEventById(event.id)
        } else {
            calendarEventDao.getCalendarEventByRemoteUuid(ensuredUuid)
        }

        val toUpdate = event.copy(
            id = existingEvent?.id ?: event.id,
            title = event.title.trim().ifBlank { "Evento" },
            endTimestamp = if (event.endTimestamp > event.timestamp) event.endTimestamp else event.timestamp + 3_600_000L,
            remoteUuid = ensuredUuid,
            userId = effectiveUid,
            linkedNoteId = validNoteId,
            linkedNoteUuid = resolvedNoteUuid,
            linkedNoteTitle = resolvedNoteTitle,
            isSynced = false,
            pendingDelete = false,
            updatedAt = now,
            lastModifiedLocal = now
        )

        if (toUpdate.id > 0L && existingEvent != null) {
            calendarEventDao.updateCalendarEvent(toUpdate)
        } else {
            calendarEventDao.insertCalendarEvent(toUpdate.copy(id = 0L))
        }

        val existingUnified = reminderDao.getReminderByRemoteUuid(ensuredUuid)
            ?: (if (event.id > 0L) reminderDao.getReminderById(event.id) else null)
        val unifiedEntity = toUpdate.toReminderEntity().copy(id = existingUnified?.id ?: 0L)
        val unifiedId = if (existingUnified != null) {
            reminderDao.updateReminder(unifiedEntity)
            existingUnified.id
        } else {
            reminderDao.insertReminder(unifiedEntity)
        }

        val scheduledEntity = unifiedEntity.copy(id = unifiedId)
        context?.let { ctx ->
            ReminderScheduler.scheduleStandaloneReminder(ctx, scheduledEntity)
            syncScope.launch {
                GoogleCalendarSyncManager.syncItemToGoogleCalendar(ctx, scheduledEntity)
            }
        }
        syncItemToFirestoreAsync(scheduledEntity)
        notifyWidgetsUpdate()
    }

    suspend fun toggleCalendarEventCompleted(event: CalendarEvent) {
        val now = System.currentTimeMillis()
        val updated = event.copy(
            isCompleted = !event.isCompleted,
            updatedAt = now,
            lastModifiedLocal = now
        )
        updateCalendarEvent(updated)
    }

    suspend fun deleteCalendarEvent(event: CalendarEvent) {
        val targetUuid = event.remoteUuid
        val existingUnified = if (targetUuid.isNotBlank()) {
            reminderDao.getReminderByRemoteUuid(targetUuid)
        } else {
            reminderDao.getReminderById(event.id)
        }
        existingUnified?.let {
            context?.let { ctx -> ReminderScheduler.cancelStandaloneReminder(ctx, it.id) }
        }
        val uuidForGCal = targetUuid.ifBlank { existingUnified?.remoteUuid.orEmpty() }
        if (uuidForGCal.isNotBlank()) {
            context?.let { ctx ->
                syncScope.launch {
                    GoogleCalendarSyncManager.deleteItemFromGoogleCalendar(ctx, uuidForGCal)
                }
            }
        }

        val uid = _activeUserId.value
        if (uid.isNotBlank() && uid != NoteEntity.LOCAL_ANONYMOUS_USER_ID && targetUuid.isNotBlank()) {
            if (event.id > 0L) {
                calendarEventDao.markCalendarEventPendingDelete(event.id)
            }
            existingUnified?.let { reminderDao.markReminderPendingDelete(it.id) }
            syncBidirectional(uid)
        } else {
            if (event.id > 0L) {
                calendarEventDao.deleteCalendarEventById(event.id)
            }
            if (targetUuid.isNotBlank()) {
                calendarEventDao.deleteCalendarEventByRemoteUuid(targetUuid)
            }
            existingUnified?.let { reminderDao.deleteReminderById(it.id) }
        }
        notifyWidgetsUpdate()
    }

    suspend fun deleteCalendarEventById(id: Long) {
        val event = getCalendarEventById(id)
        if (event != null) {
            deleteCalendarEvent(event)
        } else {
            calendarEventDao.deleteCalendarEventById(id)
        }
    }

    // --- OPERACIONES CON REMINDERENTITY UNIFICADO ---

    suspend fun insertCalendarItem(item: ReminderEntity): Long {
        return if (item.isEvent) {
            insertCalendarEvent(CalendarEvent.fromReminderEntity(item))
        } else {
            insertReminder(Reminder.fromReminderEntity(item))
        }
    }

    suspend fun updateCalendarItem(item: ReminderEntity) {
        if (item.isEvent) {
            updateCalendarEvent(CalendarEvent.fromReminderEntity(item))
        } else {
            updateReminder(Reminder.fromReminderEntity(item))
        }
    }

    suspend fun deleteCalendarItem(item: ReminderEntity) {
        if (item.isEvent) {
            deleteCalendarEvent(CalendarEvent.fromReminderEntity(item))
        } else {
            deleteReminder(Reminder.fromReminderEntity(item))
        }
    }

    // --- SINCRONIZACIÓN BIDIRECCIONAL EN TIEMPO REAL CON FIRESTORE ---

    fun startListeningToFirestore(userId: String) {
        if (userId.isBlank() || userId == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        _activeUserId.value = userId

        if (listeningUserId == userId && realtimeJob?.isActive == true) {
            syncBidirectional(userId, forcePushAll = false)
            return
        }

        stopListeningToFirestore(resetToAnonymous = false)
        _activeUserId.value = userId
        listeningUserId = userId

        realtimeJob = syncScope.launch {
            // 1. Reclamar elementos creados en modo local anónimo
            reminderDao.claimAnonymousReminders(userId)
            reminderDao.claimAnonymousReminderItems(userId)
            calendarEventDao.claimAnonymousCalendarEvents(userId)

            // 2. Sincronizar cambios locales pendientes hacia Firestore
            syncScope.launch {
                syncBidirectionalSuspending(userId, forcePushAll = false)
            }

            // 3. Escuchar cambios en tiempo real desde Firestore (/users/{userId}/reminders)
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
                    Log.w("ReminderRepository", "Listener de Firestore interrumpido: ${e.localizedMessage}")
                }
                .collect { remoteList ->
                    reconcileRemoteItems(userId, remoteList)
                }
        }
    }

    fun stopListeningToFirestore(resetToAnonymous: Boolean = false) {
        realtimeJob?.cancel()
        realtimeJob = null
        listeningUserId = null
        if (resetToAnonymous) {
            _activeUserId.value = NoteEntity.LOCAL_ANONYMOUS_USER_ID
        }
    }

    fun syncBidirectional(userId: String? = null, forcePushAll: Boolean = false) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid.isBlank() || targetUid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        OfflineSyncWorker.enqueueWhenConnected(context)
        syncScope.launch {
            syncBidirectionalSuspending(targetUid, forcePushAll = forcePushAll)
        }
    }

    suspend fun syncBidirectionalSuspending(
        userId: String? = null,
        forcePushAll: Boolean = false
    ) {
        val targetUid = userId?.takeIf { it.isNotBlank() } ?: _activeUserId.value
        if (targetUid.isBlank() || targetUid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return

        syncMutex.withLock {
            try {
                reminderDao.claimAnonymousReminders(targetUid)
                reminderDao.claimAnonymousReminderItems(targetUid)
                calendarEventDao.claimAnonymousCalendarEvents(targetUid)

                // 1. Procesar eliminaciones pendientes en las 3 tablas
                val pendingUnifiedDeletes = reminderDao.getPendingDeleteReminders(targetUid)
                val pendingItemDeletes = reminderDao.getPendingDeleteReminderItems(targetUid)
                val pendingEventDeletes = calendarEventDao.getPendingDeleteCalendarEvents(targetUid)

                val uuidsToDelete = linkedSetOf<String>()
                pendingUnifiedDeletes.mapNotNullTo(uuidsToDelete) { it.remoteUuid.takeIf { u -> u.isNotBlank() } }
                pendingItemDeletes.mapNotNullTo(uuidsToDelete) { it.remoteUuid.takeIf { u -> u.isNotBlank() } }
                pendingEventDeletes.mapNotNullTo(uuidsToDelete) { it.remoteUuid.takeIf { u -> u.isNotBlank() } }

                for (uuid in uuidsToDelete) {
                    val deletedRemotely = firestoreManager.deleteReminder(targetUid, uuid)
                    if (deletedRemotely) {
                        reminderDao.deleteReminderItemByRemoteUuid(uuid)
                        calendarEventDao.deleteCalendarEventByRemoteUuid(uuid)
                        reminderDao.getReminderByRemoteUuid(uuid)?.let { reminderDao.deleteReminderById(it.id) }
                    } else {
                        OfflineSyncWorker.enqueueWhenConnected(context)
                    }
                }

                // 2. Empujar inserciones y actualizaciones locales no sincronizadas
                val unsyncedItems = reminderDao.getUnsyncedReminderItems(targetUid)
                for (item in unsyncedItems) {
                    pushSingleEntityToFirestore(targetUid, item.toReminderEntity())
                }

                val unsyncedEvents = calendarEventDao.getUnsyncedCalendarEvents(targetUid)
                for (event in unsyncedEvents) {
                    pushSingleEntityToFirestore(targetUid, event.toReminderEntity())
                }

                val unsyncedUnified = reminderDao.getUnsyncedReminders(targetUid)
                for (unified in unsyncedUnified) {
                    pushSingleEntityToFirestore(targetUid, unified)
                }

                // 3. Descargar y reconciliar desde Firestore
                val remoteItems = firestoreManager.fetchRemindersOnce(targetUid)
                reconcileRemoteItems(targetUid, remoteItems)

                // 4. Si es sincronización manual forzada, empujar todos los elementos activos
                if (forcePushAll) {
                    val allUnified = reminderDao.getRemindersForUserList(targetUid).filter { !it.pendingDelete }
                    for (rem in allUnified) {
                        pushSingleEntityToFirestore(targetUid, rem)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ReminderRepository", "Error en sincronización bidireccional de recordatorios", e)
                OfflineSyncWorker.enqueueWhenConnected(context)
            }
        }
    }

    private fun syncItemToFirestoreAsync(entity: ReminderEntity) {
        val uid = _activeUserId.value
        if (uid.isBlank() || uid == NoteEntity.LOCAL_ANONYMOUS_USER_ID) return
        OfflineSyncWorker.enqueueWhenConnected(context)
        syncScope.launch {
            pushSingleEntityToFirestore(uid, entity)
        }
    }

    private suspend fun pushSingleEntityToFirestore(userId: String, entity: ReminderEntity) {
        if (context != null && !firestoreManager.checkNetworkAvailable(context)) {
            firestoreManager.setSyncState(SyncState.OFFLINE)
            OfflineSyncWorker.enqueueWhenConnected(context)
            return
        }

        val trackingUuid = entity.remoteUuid.ifBlank { UUID.randomUUID().toString() }
        inFlightUuids.add(trackingUuid)
        try {
            val expectedLocalTs = entity.lastModifiedLocal
            val normalized = entity.copy(remoteUuid = trackingUuid, userId = userId)
            val pushed = firestoreManager.pushReminder(userId, normalized, context)
            if (pushed) {
                val serverTs = System.currentTimeMillis()
                reminderDao.markReminderSyncedIfUnchanged(trackingUuid, expectedLocalTs, serverTs)
                reminderDao.markReminderItemSyncedIfUnchanged(trackingUuid, expectedLocalTs, serverTs)
                calendarEventDao.markCalendarEventSyncedIfUnchanged(trackingUuid, expectedLocalTs, serverTs)
            } else {
                OfflineSyncWorker.enqueueWhenConnected(context)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("ReminderRepository", "Error al subir recordatorio/evento a Firestore", e)
            OfflineSyncWorker.enqueueWhenConnected(context)
        } finally {
            inFlightUuids.remove(trackingUuid)
        }
    }

    internal suspend fun reconcileRemoteItems(
        userId: String,
        remoteMaps: List<Map<String, Any>>
    ) {
        remoteReconcileMutex.withLock {
            val remoteUuids = mutableSetOf<String>()

            for (map in remoteMaps) {
                val remoteUuid = (map["remoteUuid"] as? String)?.takeIf { it.isNotBlank() } ?: continue
                remoteUuids.add(remoteUuid)

                if (remoteUuid in inFlightUuids) continue

                val serverTs = (map["updatedAt"] as? Timestamp)?.toDate()?.time
                    ?: (map["updatedAtServer"] as? Number)?.toLong()
                    ?: (map["updatedAt"] as? Number)?.toLong()
                    ?: 0L
                val remoteLocalTs = (map["lastModifiedLocal"] as? Number)?.toLong() ?: 0L
                val remoteUpdatedAt = maxOf(serverTs, remoteLocalTs)

                val localUnified = reminderDao.getReminderByRemoteUuidAndUser(remoteUuid, userId)
                    ?: reminderDao.getReminderByRemoteUuid(remoteUuid)
                val localRemItem = reminderDao.getReminderItemByRemoteUuidAndUser(remoteUuid, userId)
                    ?: reminderDao.getReminderItemByRemoteUuid(remoteUuid)
                val localCalEvent = calendarEventDao.getCalendarEventByRemoteUuidAndUser(remoteUuid, userId)
                    ?: calendarEventDao.getCalendarEventByRemoteUuid(remoteUuid)

                val isPendingDeleteLocally = (localUnified?.pendingDelete == true) ||
                    (localRemItem?.pendingDelete == true) ||
                    (localCalEvent?.pendingDelete == true)

                if (isPendingDeleteLocally) {
                    syncScope.launch {
                        if (firestoreManager.deleteReminder(userId, remoteUuid)) {
                            reminderDao.deleteReminderItemByRemoteUuid(remoteUuid)
                            calendarEventDao.deleteCalendarEventByRemoteUuid(remoteUuid)
                            localUnified?.let { reminderDao.deleteReminderById(it.id) }
                        }
                    }
                    continue
                }

                var remoteEntity = map.toReminderEntity(fallbackUserId = userId)
                val (validNoteId, resolvedNoteUuid, resolvedNoteTitle) = resolveLinkedNoteMetadata(
                    linkedNoteId = remoteEntity.linkedNoteId,
                    fallbackUuid = remoteEntity.linkedNoteUuid,
                    fallbackTitle = remoteEntity.linkedNoteTitle,
                    userId = userId
                )
                remoteEntity = remoteEntity.copy(
                    userId = userId,
                    linkedNoteId = validNoteId,
                    linkedNoteUuid = resolvedNoteUuid,
                    linkedNoteTitle = resolvedNoteTitle,
                    isSynced = true,
                    pendingDelete = false
                )

                // Verificar si hay cambios locales más recientes sin sincronizar
                val localLastModified = maxOf(
                    localUnified?.lastModifiedLocal ?: 0L,
                    localRemItem?.lastModifiedLocal ?: 0L,
                    localCalEvent?.lastModifiedLocal ?: 0L
                )
                val hasUnsyncedLocal = (localUnified != null && !localUnified.isSynced) ||
                    (localRemItem != null && !localRemItem.isSynced) ||
                    (localCalEvent != null && !localCalEvent.isSynced)

                if (hasUnsyncedLocal && localLastModified > remoteUpdatedAt) {
                    val newestLocal = localUnified
                        ?: localRemItem?.toReminderEntity()
                        ?: localCalEvent?.toReminderEntity()
                    if (newestLocal != null) {
                        syncItemToFirestoreAsync(newestLocal)
                    }
                    continue
                }

                // Persistir en tabla unificada `reminders`
                val savedUnifiedId = if (localUnified == null) {
                    reminderDao.insertReminder(remoteEntity)
                } else {
                    reminderDao.updateReminder(remoteEntity.copy(id = localUnified.id))
                    localUnified.id
                }
                context?.let {
                    ReminderScheduler.scheduleStandaloneReminder(it, remoteEntity.copy(id = savedUnifiedId))
                }

                // Persistir en tabla específica (`reminder_items` o `calendar_events`)
                if (remoteEntity.isEvent) {
                    if (localRemItem != null) {
                        reminderDao.deleteReminderItemById(localRemItem.id)
                    }
                    val eventToSave = CalendarEvent.fromReminderEntity(remoteEntity).copy(
                        id = localCalEvent?.id ?: 0L,
                        linkedNoteId = validNoteId,
                        isSynced = true,
                        pendingDelete = false
                    )
                    if (localCalEvent == null) {
                        calendarEventDao.insertCalendarEvent(eventToSave)
                    } else {
                        calendarEventDao.updateCalendarEvent(eventToSave)
                    }
                } else {
                    if (localCalEvent != null) {
                        calendarEventDao.deleteCalendarEventById(localCalEvent.id)
                    }
                    val reminderToSave = Reminder.fromReminderEntity(remoteEntity).copy(
                        id = localRemItem?.id ?: 0L,
                        linkedNoteId = validNoteId,
                        isSynced = true,
                        pendingDelete = false
                    )
                    if (localRemItem == null) {
                        reminderDao.insertReminderItem(reminderToSave)
                    } else {
                        reminderDao.updateReminderItem(reminderToSave)
                    }
                }
            }

            // Reconciliar eliminaciones remotas (elementos previamente sincronizados que ya no existen en Firestore)
            val localUnifiedList = reminderDao.getRemindersForUserList(userId)
            for (local in localUnifiedList) {
                if (local.userId == userId &&
                    local.isSynced &&
                    !local.pendingDelete &&
                    local.remoteUuid.isNotBlank() &&
                    local.remoteUuid !in remoteUuids &&
                    local.remoteUuid !in inFlightUuids
                ) {
                    context?.let { ReminderScheduler.cancelStandaloneReminder(it, local.id) }
                    reminderDao.deleteReminderById(local.id)
                    reminderDao.deleteReminderItemByRemoteUuid(local.remoteUuid)
                    calendarEventDao.deleteCalendarEventByRemoteUuid(local.remoteUuid)
                }
            }

            val localReminderItems = reminderDao.getReminderItemsForUserList(userId)
            for (item in localReminderItems) {
                if (item.userId == userId &&
                    item.isSynced &&
                    !item.pendingDelete &&
                    item.remoteUuid.isNotBlank() &&
                    item.remoteUuid !in remoteUuids &&
                    item.remoteUuid !in inFlightUuids
                ) {
                    reminderDao.deleteReminderItemById(item.id)
                }
            }

            val localCalendarEvents = calendarEventDao.getCalendarEventsForUserList(userId)
            for (ev in localCalendarEvents) {
                if (ev.userId == userId &&
                    ev.isSynced &&
                    !ev.pendingDelete &&
                    ev.remoteUuid.isNotBlank() &&
                    ev.remoteUuid !in remoteUuids &&
                    ev.remoteUuid !in inFlightUuids
                ) {
                    calendarEventDao.deleteCalendarEventById(ev.id)
                }
            }
        }
    }

    private suspend fun resolveLinkedNoteMetadata(
        linkedNoteId: Long?,
        fallbackUuid: String,
        fallbackTitle: String,
        userId: String = _activeUserId.value
    ): Triple<Long?, String, String> {
        val nDao = noteDao ?: return Triple(linkedNoteId, fallbackUuid, fallbackTitle)
        if (linkedNoteId != null && linkedNoteId > 0L) {
            val noteById = nDao.getNoteById(linkedNoteId)
            if (noteById != null) {
                return Triple(
                    noteById.id,
                    noteById.remoteUuid.ifBlank { fallbackUuid },
                    noteById.title.ifBlank { fallbackTitle }
                )
            }
        }
        if (fallbackUuid.isNotBlank()) {
            val noteByUuid = nDao.getNoteByRemoteUuidAndUser(fallbackUuid, userId)
                ?: nDao.getNoteByRemoteUuid(fallbackUuid)
            if (noteByUuid != null) {
                return Triple(
                    noteByUuid.id,
                    noteByUuid.remoteUuid,
                    noteByUuid.title.ifBlank { fallbackTitle }
                )
            }
        }
        // Si el linkedNoteId no existe en la tabla `notes`, devolvemos null para respetar la ForeignKey
        return Triple(null, fallbackUuid, fallbackTitle)
    }

    private fun resolveEffectiveUserId(candidateUserId: String): String {
        if (candidateUserId.isNotBlank() && candidateUserId != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
            return candidateUserId
        }
        return _activeUserId.value
    }

    companion object {
        private fun resolveCurrentAuthUid(): String {
            return try {
                FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID
            } catch (_: Exception) {
                NoteEntity.LOCAL_ANONYMOUS_USER_ID
            }
        }
    }
}
