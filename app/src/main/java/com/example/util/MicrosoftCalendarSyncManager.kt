package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.auth.microsoft.MicrosoftAuthManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.microsoft.MicrosoftCalendarEvent
import com.example.data.microsoft.MicrosoftGraphClient
import com.example.model.ConflictRecordType
import com.example.model.ConflictResolutionChoice
import com.example.model.SyncConflictItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

data class MicrosoftCalendarSyncResult(
    val success: Boolean,
    val exportedCount: Int = 0,
    val importedCount: Int = 0,
    val updatedCount: Int = 0,
    val accountName: String = "",
    val message: String = "",
    val conflicts: List<SyncConflictItem> = emptyList()
)

/**
 * Gestor de sincronización bidireccional entre los Recordatorios/Eventos de Note JS
 * y el Calendario de Microsoft Outlook / Office 365 a través de Microsoft Graph API.
 * Incluye detección y resolución manual de conflictos entre Google y Microsoft.
 */
object MicrosoftCalendarSyncManager {

    private const val TAG = "MicrosoftCalendarSync"
    private const val PREFS_NAME = "microsoft_calendar_sync_prefs"
    private const val MAP_PREFS_NAME = "notejs_ms_event_map"

    const val KEY_MS_CAL_SYNC_ENABLED = "key_ms_cal_sync_enabled"
    const val KEY_MS_CAL_SYNC_REMINDERS = "key_ms_cal_sync_reminders"
    const val KEY_MS_CAL_IMPORT_EVENTS = "key_ms_cal_import_events"
    const val KEY_MS_CAL_LAST_SYNC_TIME = "key_ms_cal_last_sync_time"

    private val syncMutex = Mutex()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun mapPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)

    fun isSyncEnabled(context: Context): Boolean {
        val authManager = MicrosoftAuthManager(context)
        return authManager.isSignedIn() && prefs(context).getBoolean(KEY_MS_CAL_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MS_CAL_SYNC_ENABLED, enabled).apply()
    }

    fun isSyncRemindersEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MS_CAL_SYNC_REMINDERS, true)

    fun setSyncRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MS_CAL_SYNC_REMINDERS, enabled).apply()
    }

    fun isImportEventsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MS_CAL_IMPORT_EVENTS, true)

    fun setImportEventsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MS_CAL_IMPORT_EVENTS, enabled).apply()
    }

    fun getLastSyncTime(context: Context): Long =
        prefs(context).getLong(KEY_MS_CAL_LAST_SYNC_TIME, 0L)

    fun getAccountLabel(context: Context): String {
        val account = MicrosoftAuthManager(context).currentAccount.value
        return account?.summaryLabel ?: "Microsoft Outlook"
    }

    /**
     * Realiza una sincronización bidireccional completa con Microsoft Outlook Calendar:
     * 1. Obtiene los eventos remotos actuales de Outlook para comparar posibles conflictos con Google/Local.
     * 2. Exporta recordatorios y eventos locales hacia Outlook Calendar (o registra conflicto si ambos cambiaron).
     * 3. Importa eventos remotos de Outlook Calendar en Note JS.
     */
    suspend fun syncBidirectionalWithOutlookCalendar(context: Context): MicrosoftCalendarSyncResult =
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                val authManager = MicrosoftAuthManager(context)
                val account = authManager.currentAccount.value
                if (account == null || account.accessToken.isBlank()) {
                    return@withContext MicrosoftCalendarSyncResult(
                        success = false,
                        message = "Inicia sesión con tu cuenta de Microsoft (Outlook/Office 365) para sincronizar el calendario."
                    )
                }

                val client = MicrosoftGraphClient(context)
                val db = AppDatabase.getDatabase(context)
                val eventMap = mapPrefs(context)
                val activeUid = account.userId.ifBlank { NoteEntity.LOCAL_ANONYMOUS_USER_ID }
                val lastSyncTime = getLastSyncTime(context)

                var exported = 0
                var imported = 0
                var updated = 0
                val detectedConflicts = mutableListOf<SyncConflictItem>()

                try {
                    val syncReminders = isSyncRemindersEnabled(context)
                    val importEvents = isImportEventsEnabled(context)

                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -30)
                    val windowStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 120)
                    val windowEnd = cal.timeInMillis

                    val remoteEvents = client.listCalendarEvents(windowStart, windowEnd)
                    val remoteById = remoteEvents.associateBy { it.id }

                    // 1. EXPORTAR: Recordatorios locales como eventos de Outlook
                    if (syncReminders) {
                        val reminders: List<ReminderEntity> = db.reminderDao().getRemindersForUserList(activeUid)
                        for (rem in reminders) {
                            if (rem.scheduledAt <= 0L || rem.isCompleted) continue
                            val mapKey = "rem_${rem.id}"
                            val existingGraphId = eventMap.getString(mapKey, null)

                            val duration = if (rem.endTimeMillis > rem.scheduledAt) rem.endTimeMillis - rem.scheduledAt else 3600_000L
                            val endAt = rem.scheduledAt + duration
                            val bodyNotes: String = if (rem.notes.isNotBlank()) rem.notes else "Recordatorio creado en Note JS"

                            if (existingGraphId.isNullOrBlank()) {
                                val res = client.createCalendarEvent(
                                    subject = "[NoteJS] ${rem.title}",
                                    bodyContent = bodyNotes,
                                    startMillis = rem.scheduledAt,
                                    endMillis = endAt,
                                    isAllDay = rem.isAllDay
                                )
                                res.onSuccess { newId ->
                                    eventMap.edit().putString(mapKey, newId).apply()
                                    exported++
                                }
                            } else {
                                val remoteMatch = remoteById[existingGraphId]
                                val cleanRemoteSubject = remoteMatch?.subject?.removePrefix("[NoteJS]")?.trim() ?: ""
                                val remoteBody = remoteMatch?.body?.trim() ?: ""

                                val titleDiffers = remoteMatch != null && !cleanRemoteSubject.equals(rem.title.trim(), ignoreCase = false)
                                val bodyDiffers = remoteMatch != null && remoteBody.isNotBlank() &&
                                    remoteBody != "Recordatorio creado en Note JS" &&
                                    !remoteBody.equals(rem.notes.trim(), ignoreCase = false)

                                if (remoteMatch != null && (titleDiffers || bodyDiffers) && rem.updatedAt > lastSyncTime && lastSyncTime > 0L) {
                                    detectedConflicts.add(
                                        SyncConflictItem(
                                            id = "conflict_rem_${rem.id}",
                                            localId = rem.id,
                                            remoteUuid = rem.remoteUuid,
                                            recordType = ConflictRecordType.REMINDER,
                                            googleTitle = rem.title,
                                            googleContent = rem.notes,
                                            googleUpdatedAt = rem.updatedAt,
                                            microsoftTitle = cleanRemoteSubject.ifBlank { remoteMatch.subject },
                                            microsoftContent = remoteBody,
                                            microsoftUpdatedAt = System.currentTimeMillis(),
                                            googleEventStartMillis = rem.scheduledAt,
                                            microsoftEventStartMillis = remoteMatch.startMillis,
                                            microsoftGraphEventId = existingGraphId
                                        )
                                    )
                                } else {
                                    val res = client.updateCalendarEvent(
                                        eventId = existingGraphId,
                                        subject = "[NoteJS] ${rem.title}",
                                        bodyContent = bodyNotes,
                                        startMillis = rem.scheduledAt,
                                        endMillis = endAt,
                                        isAllDay = rem.isAllDay
                                    )
                                    if (res.isSuccess) {
                                        updated++
                                    }
                                }
                            }
                        }
                    }

                    // 2. EXPORTAR: Eventos del calendario interno de Note JS
                    val internalEvents: List<CalendarEvent> = db.calendarEventDao().getCalendarEventsForUserList(activeUid)
                    for (ev in internalEvents) {
                        val mapKey = "event_${ev.id}"
                        val existingGraphId = eventMap.getString(mapKey, null)
                        val endAt = if (ev.endTimestamp > ev.timestamp) ev.endTimestamp else ev.timestamp + 3600_000L
                        val desc: String = if (ev.description.isNotBlank()) ev.description else "Evento de Note JS"

                        if (existingGraphId.isNullOrBlank()) {
                            val res = client.createCalendarEvent(
                                subject = ev.title,
                                bodyContent = desc,
                                startMillis = ev.timestamp,
                                endMillis = endAt,
                                isAllDay = ev.isAllDay
                            )
                            res.onSuccess { newId ->
                                eventMap.edit().putString(mapKey, newId).apply()
                                exported++
                            }
                        } else {
                            val remoteMatch = remoteById[existingGraphId]
                            val cleanRemoteSubject = remoteMatch?.subject?.trim() ?: ""
                            val remoteBody = remoteMatch?.body?.trim() ?: ""

                            val titleDiffers = remoteMatch != null && !cleanRemoteSubject.equals(ev.title.trim(), ignoreCase = false)
                            val bodyDiffers = remoteMatch != null && remoteBody.isNotBlank() &&
                                remoteBody != "Evento de Note JS" &&
                                !remoteBody.equals(ev.description.trim(), ignoreCase = false)

                            if (remoteMatch != null && (titleDiffers || bodyDiffers) && ev.updatedAt > lastSyncTime && lastSyncTime > 0L) {
                                detectedConflicts.add(
                                    SyncConflictItem(
                                        id = "conflict_ev_${ev.id}",
                                        localId = ev.id,
                                        remoteUuid = ev.remoteUuid,
                                        recordType = ConflictRecordType.CALENDAR_EVENT,
                                        googleTitle = ev.title,
                                        googleContent = ev.description,
                                        googleUpdatedAt = ev.updatedAt,
                                        microsoftTitle = cleanRemoteSubject,
                                        microsoftContent = remoteBody,
                                        microsoftUpdatedAt = System.currentTimeMillis(),
                                        googleEventStartMillis = ev.timestamp,
                                        microsoftEventStartMillis = remoteMatch.startMillis,
                                        microsoftGraphEventId = existingGraphId
                                    )
                                )
                            } else {
                                val res = client.updateCalendarEvent(
                                    eventId = existingGraphId,
                                    subject = ev.title,
                                    bodyContent = desc,
                                    startMillis = ev.timestamp,
                                    endMillis = endAt,
                                    isAllDay = ev.isAllDay
                                )
                                if (res.isSuccess) updated++
                            }
                        }
                    }

                    // 3. IMPORTAR: Traer eventos remotos de Outlook Calendar a la base de datos de Note JS
                    if (importEvents) {
                        val currentEvents = db.calendarEventDao().getCalendarEventsForUserList(activeUid)

                        for (rev in remoteEvents) {
                            if (rev.categories.contains("NoteJS") || rev.subject.startsWith("[NoteJS]")) {
                                continue
                            }

                            val cleanTitle = rev.subject.trim()
                            val existingLocal = currentEvents.firstOrNull {
                                it.title.equals(cleanTitle, ignoreCase = true) && it.timestamp == rev.startMillis
                            }
                            if (existingLocal == null) {
                                val newEvent = CalendarEvent(
                                    title = cleanTitle,
                                    description = rev.body,
                                    timestamp = rev.startMillis,
                                    endTimestamp = rev.endMillis,
                                    isAllDay = rev.isAllDay,
                                    listName = "Outlook",
                                    colorHex = 0xFF0078D4, // Azul oficial de Outlook
                                    userId = activeUid
                                )
                                db.calendarEventDao().insertCalendarEvent(newEvent)
                                imported++
                            } else if (rev.body.isNotBlank() &&
                                existingLocal.description.isNotBlank() &&
                                !rev.body.trim().equals(existingLocal.description.trim(), ignoreCase = false)
                            ) {
                                // Mismo evento en ambos calendarios pero con descripción modificada
                                detectedConflicts.add(
                                    SyncConflictItem(
                                        id = "conflict_import_ev_${existingLocal.id}",
                                        localId = existingLocal.id,
                                        remoteUuid = existingLocal.remoteUuid,
                                        recordType = ConflictRecordType.CALENDAR_EVENT,
                                        googleTitle = existingLocal.title,
                                        googleContent = existingLocal.description,
                                        googleUpdatedAt = existingLocal.updatedAt,
                                        microsoftTitle = cleanTitle,
                                        microsoftContent = rev.body,
                                        microsoftUpdatedAt = System.currentTimeMillis(),
                                        googleEventStartMillis = existingLocal.timestamp,
                                        microsoftEventStartMillis = rev.startMillis,
                                        microsoftGraphEventId = rev.id
                                    )
                                )
                            }
                        }
                    }

                    val now = System.currentTimeMillis()
                    prefs(context).edit().putLong(KEY_MS_CAL_LAST_SYNC_TIME, now).apply()

                    val summary = buildString {
                        append("Outlook sincronizado: ")
                        if (exported > 0) append("$exported subidos ")
                        if (imported > 0) append("$imported importados ")
                        if (updated > 0) append("$updated actualizados ")
                        if (detectedConflicts.isNotEmpty()) append("(${detectedConflicts.size} conflictos por resolver) ")
                        if (exported == 0 && imported == 0 && updated == 0 && detectedConflicts.isEmpty()) append("Al día")
                    }

                    MicrosoftCalendarSyncResult(
                        success = true,
                        exportedCount = exported,
                        importedCount = imported,
                        updatedCount = updated,
                        accountName = account.email,
                        message = summary.trim(),
                        conflicts = detectedConflicts
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error en la sincronización con Microsoft Calendar", e)
                    MicrosoftCalendarSyncResult(
                        success = false,
                        message = "Error sincronizando con Outlook: ${e.localizedMessage ?: "Fallo de conexión"}"
                    )
                }
            }
        }

    /**
     * Aplica la resolución elegida por el usuario para un conflicto de recordatorio o evento de calendario.
     */
    suspend fun resolveCalendarConflict(
        context: Context,
        conflict: SyncConflictItem,
        choice: ConflictResolutionChoice
    ) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val client = MicrosoftGraphClient(context)
        val now = System.currentTimeMillis()

        when (conflict.recordType) {
            ConflictRecordType.REMINDER -> {
                val localRem = db.reminderDao().getReminderById(conflict.localId)
                    ?: db.reminderDao().getReminderByRemoteUuid(conflict.remoteUuid)
                    ?: return@withContext

                when (choice) {
                    ConflictResolutionChoice.KEEP_GOOGLE -> {
                        // Mantener versión local/Google y actualizar Microsoft Outlook
                        val graphId = conflict.microsoftGraphEventId
                        if (!graphId.isNullOrBlank()) {
                            val endAt = if (localRem.endTimeMillis > localRem.scheduledAt) localRem.endTimeMillis else localRem.scheduledAt + 3600_000L
                            client.updateCalendarEvent(
                                eventId = graphId,
                                subject = "[NoteJS] ${conflict.googleTitle}",
                                bodyContent = conflict.googleContent.ifBlank { "Recordatorio creado en Note JS" },
                                startMillis = localRem.scheduledAt,
                                endMillis = endAt,
                                isAllDay = localRem.isAllDay
                            )
                        }
                    }
                    ConflictResolutionChoice.KEEP_MICROSOFT -> {
                        // Actualizar versión local/Google con los datos de Microsoft
                        val updated = localRem.copy(
                            title = conflict.microsoftTitle,
                            notes = conflict.microsoftContent,
                            scheduledAt = if (conflict.microsoftEventStartMillis > 0L) conflict.microsoftEventStartMillis else localRem.scheduledAt,
                            updatedAt = now,
                            lastModifiedLocal = now,
                            isSynced = false
                        )
                        db.reminderDao().updateReminder(updated)
                    }
                    ConflictResolutionChoice.KEEP_BOTH -> {
                        // Conservar el original de Google y crear una copia con la versión de Microsoft
                        val copyRem = localRem.copy(
                            id = 0L,
                            remoteUuid = UUID.randomUUID().toString(),
                            title = "${conflict.microsoftTitle} (Microsoft)",
                            notes = conflict.microsoftContent,
                            scheduledAt = if (conflict.microsoftEventStartMillis > 0L) conflict.microsoftEventStartMillis else localRem.scheduledAt,
                            createdAt = now,
                            updatedAt = now,
                            lastModifiedLocal = now,
                            isSynced = false
                        )
                        db.reminderDao().insertReminder(copyRem)
                    }
                }
            }

            ConflictRecordType.CALENDAR_EVENT -> {
                val localEv = db.calendarEventDao().getCalendarEventById(conflict.localId)
                    ?: db.calendarEventDao().getCalendarEventByRemoteUuid(conflict.remoteUuid)
                    ?: return@withContext

                when (choice) {
                    ConflictResolutionChoice.KEEP_GOOGLE -> {
                        val graphId = conflict.microsoftGraphEventId
                        if (!graphId.isNullOrBlank()) {
                            val endAt = if (localEv.endTimestamp > localEv.timestamp) localEv.endTimestamp else localEv.timestamp + 3600_000L
                            client.updateCalendarEvent(
                                eventId = graphId,
                                subject = conflict.googleTitle,
                                bodyContent = conflict.googleContent.ifBlank { "Evento de Note JS" },
                                startMillis = localEv.timestamp,
                                endMillis = endAt,
                                isAllDay = localEv.isAllDay
                            )
                        }
                    }
                    ConflictResolutionChoice.KEEP_MICROSOFT -> {
                        val updated = localEv.copy(
                            title = conflict.microsoftTitle,
                            description = conflict.microsoftContent,
                            timestamp = if (conflict.microsoftEventStartMillis > 0L) conflict.microsoftEventStartMillis else localEv.timestamp,
                            updatedAt = now,
                            lastModifiedLocal = now,
                            isSynced = false
                        )
                        db.calendarEventDao().updateCalendarEvent(updated)
                    }
                    ConflictResolutionChoice.KEEP_BOTH -> {
                        val copyEv = localEv.copy(
                            id = 0L,
                            remoteUuid = UUID.randomUUID().toString(),
                            title = "${conflict.microsoftTitle} (Outlook)",
                            description = conflict.microsoftContent,
                            timestamp = if (conflict.microsoftEventStartMillis > 0L) conflict.microsoftEventStartMillis else localEv.timestamp,
                            createdAt = now,
                            updatedAt = now,
                            lastModifiedLocal = now,
                            isSynced = false
                        )
                        db.calendarEventDao().insertCalendarEvent(copyEv)
                    }
                }
            }

            ConflictRecordType.NOTE -> {
                // Manejado en el repositorio de notas
            }
        }
    }
}
