package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RecurrenceRule
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderSubtaskSerializer
import com.example.data.remote.FirestoreManager
import com.example.data.repository.ReminderRepository
import com.example.model.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Estado de interfaz para [ReminderViewModel] con recordatorios, eventos de calendario
 * y estado de sincronización en tiempo real con Firebase Firestore.
 */
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val allCalendarItems: List<ReminderEntity> = emptyList(),
    val pendingRemindersCount: Int = 0,
    val completedRemindersCount: Int = 0,
    val syncState: SyncState = SyncState.SYNCED,
    val isSyncing: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel dedicado a la gestión de [Reminder], [CalendarEvent] y [ReminderEntity] en Room,
 * además de la sincronización bidireccional en tiempo real con Firebase Firestore.
 */
class ReminderViewModel @JvmOverloads constructor(
    application: Application,
    customRepository: ReminderRepository? = null
) : AndroidViewModel(application) {

    val firestoreManager: FirestoreManager = FirestoreManager(application.applicationContext)
    val authManager: AuthManager = AuthManager(application)
    val repository: ReminderRepository

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val syncState: StateFlow<SyncState> = firestoreManager.syncState

    /**
     * Flujo reactivo de recordatorios (`Reminder`) almacenados en Room y sincronizados con Firestore.
     */
    val reminders: StateFlow<List<Reminder>>

    /**
     * Flujo reactivo de eventos de calendario (`CalendarEvent`) almacenados en Room y sincronizados con Firestore.
     */
    val calendarEvents: StateFlow<List<CalendarEvent>>

    /**
     * Flujo reactivo unificado de elementos de calendario (`ReminderEntity`) para las vistas Día, 3 Días, Semana, Mes y Listas.
     */
    val allCalendarItems: StateFlow<List<ReminderEntity>>

    /**
     * Estado consolidado de UI para observadores Compose.
     */
    val uiState: StateFlow<ReminderUiState>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = customRepository ?: ReminderRepository(
            reminderDao = database.reminderDao(),
            calendarEventDao = database.calendarEventDao(),
            firestoreManager = firestoreManager,
            noteDao = database.noteDao(),
            context = application.applicationContext
        )

        reminders = repository.reminders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        calendarEvents = repository.calendarEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCalendarItems = repository.calendarItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        uiState = combine(
            reminders,
            calendarEvents,
            allCalendarItems,
            combine(syncState, _isSyncing, _snackbarMessage) { state, syncing, msg ->
                Triple(state, syncing, msg)
            }
        ) { remList, eventList, calendarList, syncTriple ->
            ReminderUiState(
                reminders = remList,
                calendarEvents = eventList,
                allCalendarItems = calendarList,
                pendingRemindersCount = remList.count { !it.isCompleted },
                completedRemindersCount = remList.count { it.isCompleted },
                syncState = syncTriple.first,
                isSyncing = syncTriple.second,
                snackbarMessage = syncTriple.third
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderUiState()
        )

        // Observar automáticamente la sesión de Firebase Auth para activar/detener la sincronización bidireccional
        viewModelScope.launch {
            authManager.currentUserFlow.collectLatest { user ->
                if (user != null && user.uid.isNotBlank()) {
                    repository.startListeningToFirestore(user.uid)
                } else {
                    repository.stopListeningToFirestore(resetToAnonymous = true)
                }
            }
        }
    }

    // --- CONSULTAS REACTIVAS ESPECÍFICAS ---

    fun observeRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        repository.observeRemindersByLinkedNoteId(noteId)

    fun observeEventsForNote(noteId: Long): Flow<List<CalendarEvent>> =
        repository.observeCalendarEventsByLinkedNoteId(noteId)

    fun observeEventsInRange(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>> =
        repository.observeCalendarEventsInRange(startMillis, endMillis)

    // --- INSERCIÓN, ACTUALIZACIÓN Y GESTIÓN DE RECORDATORIOS (`Reminder`) ---

    fun insertReminder(
        title: String,
        description: String = "",
        timestamp: Long,
        linkedNoteId: Long? = null,
        isCompleted: Boolean = false,
        priority: Int = 0,
        listName: String = "Personal",
        colorHex: Long = 0xFF007AFFL,
        recurrenceRule: String = RecurrenceRule.NONE.code,
        subtasksJson: String = "[]",
        alertOffsetMinutes: Int = 0,
        onInserted: (Long) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            id = 0L,
            title = title.trim().ifBlank { "Nuevo recordatorio" },
            description = description,
            timestamp = timestamp,
            linkedNoteId = linkedNoteId,
            isCompleted = isCompleted,
            priority = priority,
            listName = listName,
            colorHex = colorHex,
            recurrenceRule = recurrenceRule,
            subtasksJson = subtasksJson,
            alertOffsetMinutes = alertOffsetMinutes,
            completedAt = if (isCompleted) now else 0L,
            createdAt = now,
            updatedAt = now,
            lastModifiedLocal = now
        )
        insertReminder(reminder, onInserted)
    }

    fun insertReminder(
        reminder: Reminder,
        onInserted: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.insertReminder(reminder)
            _snackbarMessage.value = "Recordatorio guardado y sincronizado"
            onInserted(id)
        }
    }

    fun updateReminder(
        reminder: Reminder,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            _snackbarMessage.value = "Recordatorio actualizado"
            onUpdated()
        }
    }

    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val newCompleted = !reminder.isCompleted
            val now = System.currentTimeMillis()
            val updated = reminder.copy(
                isCompleted = newCompleted,
                completedAt = if (newCompleted) now else 0L,
                updatedAt = now,
                lastModifiedLocal = now
            )
            repository.updateReminder(updated)

            // Si tiene regla de recurrencia y se marcó como completado, generar la siguiente ocurrencia
            val entityView = reminder.toReminderEntity()
            if (newCompleted && RecurrenceRule.fromCode(reminder.recurrenceRule) != RecurrenceRule.NONE) {
                val nextStart = entityView.computeNextOccurrenceMillis()
                if (nextStart != null) {
                    val resetSubtasks = entityView.subtasks.map { it.copy(isCompleted = false) }
                    val nextReminder = reminder.copy(
                        id = 0L,
                        remoteUuid = UUID.randomUUID().toString(),
                        timestamp = nextStart,
                        isCompleted = false,
                        completedAt = 0L,
                        subtasksJson = ReminderSubtaskSerializer.serialize(resetSubtasks),
                        createdAt = now,
                        updatedAt = now,
                        lastModifiedLocal = now
                    )
                    repository.insertReminder(nextReminder)
                }
            }
        }
    }

    fun linkNoteToReminder(
        reminder: Reminder,
        noteId: Long?,
        noteTitle: String = "",
        noteUuid: String = ""
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = reminder.copy(
                linkedNoteId = noteId,
                linkedNoteTitle = if (noteId != null) noteTitle else "",
                linkedNoteUuid = if (noteId != null) noteUuid else "",
                updatedAt = now,
                lastModifiedLocal = now
            )
            repository.updateReminder(updated)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            _snackbarMessage.value = "Recordatorio eliminado"
        }
    }

    fun deleteReminderById(id: Long) {
        viewModelScope.launch {
            repository.deleteReminderById(id)
            _snackbarMessage.value = "Recordatorio eliminado"
        }
    }

    // --- INSERCIÓN, ACTUALIZACIÓN Y GESTIÓN DE EVENTOS DE CALENDARIO (`CalendarEvent`) ---

    fun insertCalendarEvent(
        title: String,
        description: String = "",
        timestamp: Long,
        endTimestamp: Long = timestamp + 3_600_000L,
        isAllDay: Boolean = false,
        location: String = "",
        linkedNoteId: Long? = null,
        isCompleted: Boolean = false,
        listName: String = "Personal",
        colorHex: Long = 0xFF007AFFL,
        recurrenceRule: String = RecurrenceRule.NONE.code,
        alertOffsetMinutes: Int = 0,
        onInserted: (Long) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val event = CalendarEvent(
            id = 0L,
            title = title.trim().ifBlank { "Nuevo evento" },
            description = description,
            timestamp = timestamp,
            endTimestamp = if (endTimestamp > timestamp) endTimestamp else timestamp + 3_600_000L,
            isAllDay = isAllDay,
            location = location,
            linkedNoteId = linkedNoteId,
            isCompleted = isCompleted,
            listName = listName,
            colorHex = colorHex,
            recurrenceRule = recurrenceRule,
            alertOffsetMinutes = alertOffsetMinutes,
            createdAt = now,
            updatedAt = now,
            lastModifiedLocal = now
        )
        insertCalendarEvent(event, onInserted)
    }

    fun insertCalendarEvent(
        event: CalendarEvent,
        onInserted: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.insertCalendarEvent(event)
            _snackbarMessage.value = "Evento guardado y sincronizado"
            onInserted(id)
        }
    }

    fun updateCalendarEvent(
        event: CalendarEvent,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateCalendarEvent(event)
            _snackbarMessage.value = "Evento actualizado"
            onUpdated()
        }
    }

    fun toggleCalendarEventCompleted(event: CalendarEvent) {
        viewModelScope.launch {
            repository.toggleCalendarEventCompleted(event)
        }
    }

    fun deleteCalendarEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
            _snackbarMessage.value = "Evento eliminado"
        }
    }

    fun deleteCalendarEventById(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEventById(id)
            _snackbarMessage.value = "Evento eliminado"
        }
    }

    // --- OPERACIONES UNIFICADAS PARA EL CALENDARIO (`ReminderEntity`) ---

    fun saveCalendarItem(
        item: ReminderEntity,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val savedId = if (item.id > 0L) {
                repository.updateCalendarItem(item)
                item.id
            } else {
                repository.insertCalendarItem(item.copy(id = 0L))
            }
            _snackbarMessage.value = if (item.isEvent) {
                "Evento guardado y sincronizado"
            } else {
                "Recordatorio guardado y sincronizado"
            }
            onSaved(savedId)
        }
    }

    fun toggleCalendarItemCompleted(item: ReminderEntity) {
        if (item.isEvent) {
            toggleCalendarEventCompleted(CalendarEvent.fromReminderEntity(item))
        } else {
            toggleReminderCompleted(Reminder.fromReminderEntity(item))
        }
    }

    fun toggleSubtask(item: ReminderEntity, subtaskId: String) {
        viewModelScope.launch {
            val updatedSubtasks = item.subtasks.map { sub ->
                if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
            }
            val updated = item.copy(
                subtasksJson = ReminderSubtaskSerializer.serialize(updatedSubtasks),
                updatedAt = System.currentTimeMillis(),
                lastModifiedLocal = System.currentTimeMillis()
            )
            if (updated.id > 0L) {
                repository.updateCalendarItem(updated)
            } else {
                repository.insertCalendarItem(updated.copy(id = 0L, remoteUuid = UUID.randomUUID().toString()))
            }
        }
    }

    fun rescheduleCalendarItem(
        item: ReminderEntity,
        newStartMillis: Long,
        newEndMillis: Long = 0L,
        isAllDay: Boolean = item.isAllDay
    ) {
        viewModelScope.launch {
            val resolvedEnd = when {
                newEndMillis > newStartMillis -> newEndMillis
                item.isEvent && item.endTimeMillis > item.scheduledAt -> {
                    val duration = item.endTimeMillis - item.scheduledAt
                    newStartMillis + duration
                }
                item.isEvent -> newStartMillis + 3_600_000L
                else -> 0L
            }
            val now = System.currentTimeMillis()
            val updated = item.copy(
                scheduledAt = newStartMillis,
                endTimeMillis = resolvedEnd,
                isAllDay = isAllDay,
                updatedAt = now,
                lastModifiedLocal = now
            )
            if (updated.id > 0L) {
                repository.updateCalendarItem(updated)
            } else {
                repository.insertCalendarItem(updated.copy(id = 0L, remoteUuid = UUID.randomUUID().toString()))
            }
            _snackbarMessage.value = "Horario actualizado en el calendario"
        }
    }

    fun deleteCalendarItem(item: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteCalendarItem(item)
            _snackbarMessage.value = if (item.isEvent) "Evento eliminado" else "Recordatorio eliminado"
        }
    }

    // --- CONTROL DE SINCRONIZACIÓN BIDIRECCIONAL CON FIRESTORE ---

    fun startRealtimeSync(userId: String) {
        repository.startListeningToFirestore(userId)
    }

    fun stopRealtimeSync() {
        repository.stopListeningToFirestore(resetToAnonymous = true)
    }

    fun syncWithFirestore(
        forcePushAll: Boolean = true,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (_isSyncing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val uid = authManager.currentUser?.uid
                ?: repository.activeUserId.value.takeIf { it != NoteEntity.LOCAL_ANONYMOUS_USER_ID }

            if (uid.isNullOrBlank()) {
                val msg = "Inicia sesión con Google para sincronizar tus recordatorios en la nube"
                _snackbarMessage.value = msg
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, msg)
                }
                return@launch
            }

            _isSyncing.value = true
            try {
                val appContext = getApplication<Application>()
                if (!firestoreManager.checkNetworkAvailable(appContext)) {
                    firestoreManager.setSyncState(SyncState.OFFLINE)
                    val offlineMsg = "Sin conexión • Los recordatorios se sincronizarán al reconectar"
                    _snackbarMessage.value = offlineMsg
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, offlineMsg)
                    }
                    return@launch
                }

                firestoreManager.setSyncState(SyncState.SYNCING)
                repository.startListeningToFirestore(uid)
                repository.syncBidirectionalSuspending(uid, forcePushAll = forcePushAll)
                firestoreManager.setSyncState(SyncState.SYNCED)

                val okMsg = "Recordatorios y calendario sincronizados con Firestore"
                _snackbarMessage.value = okMsg
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, okMsg)
                }
            } catch (e: Exception) {
                val errMsg = "Error al sincronizar recordatorios: ${e.localizedMessage ?: "Intenta de nuevo"}"
                _snackbarMessage.value = errMsg
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, errMsg)
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListeningToFirestore(resetToAnonymous = false)
    }

    companion object {
        fun provideFactory(
            application: Application,
            customRepository: ReminderRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
                    return ReminderViewModel(application, customRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
