package com.example.util

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CalendarEvent
import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RecurrenceRule
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

data class GoogleCalendarAccountInfo(
    val calendarId: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int = 0xFF4285F4.toInt(),
    val isPrimary: Boolean = false
) {
    val isGoogleAccount: Boolean
        get() = accountType.equals("com.google", ignoreCase = true) ||
            accountName.endsWith("@gmail.com", ignoreCase = true)

    val summaryLabel: String
        get() = if (accountName.isNotBlank() && !displayName.equals(accountName, ignoreCase = true)) {
            "$displayName ($accountName)"
        } else {
            displayName.ifBlank { accountName.ifBlank { "Google Calendar" } }
        }
}

data class GoogleCalendarSyncResult(
    val success: Boolean,
    val exportedCount: Int = 0,
    val importedCount: Int = 0,
    val updatedCount: Int = 0,
    val calendarName: String = "",
    val accountName: String = "",
    val message: String = ""
)

/**
 * Gestor de sincronización bidireccional entre los Recordatorios y Eventos de Note JS
 * y Google Calendar (a través de [CalendarContract] y el adaptador nativo de sincronización de Google).
 */
object GoogleCalendarSyncManager {

    private const val TAG = "GoogleCalendarSync"
    private const val PREFS_NAME = "aura_notes_prefs"
    private const val MAP_PREFS_NAME = "notejs_gcal_event_map"

    const val KEY_GCAL_SYNC_ENABLED = "key_gcal_sync_enabled"
    const val KEY_GCAL_SYNC_REMINDERS = "key_gcal_sync_reminders"
    const val KEY_GCAL_IMPORT_EVENTS = "key_gcal_import_events"
    const val KEY_GCAL_SELECTED_CAL_ID = "key_gcal_selected_cal_id"
    const val KEY_GCAL_SELECTED_CAL_NAME = "key_gcal_selected_cal_name"
    const val KEY_GCAL_SELECTED_ACCOUNT_NAME = "key_gcal_selected_account_name"
    const val KEY_GCAL_LAST_SYNC_TIME = "key_gcal_last_sync_time"

    private const val CUSTOM_URI_PREFIX = "notejs://calendar/"
    private const val DESCRIPTION_MARKER_PREFIX = "[NoteJS:"

    private val itemSyncMutex = Mutex()
    private val fullSyncMutex = Mutex()

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun isSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GCAL_SYNC_ENABLED, false)
    }

    fun setSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GCAL_SYNC_ENABLED, enabled).apply()
        if (enabled) {
            GoogleCalendarSyncWorker.schedulePeriodicSync(context)
        } else {
            GoogleCalendarSyncWorker.cancelPeriodicSync(context)
        }
    }

    fun isSyncRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GCAL_SYNC_REMINDERS, true)
    }

    fun setSyncRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GCAL_SYNC_REMINDERS, enabled).apply()
    }

    fun isImportEventsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_GCAL_IMPORT_EVENTS, true)
    }

    fun setImportEventsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_GCAL_IMPORT_EVENTS, enabled).apply()
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_GCAL_LAST_SYNC_TIME, 0L)
    }

    private fun setLastSyncTime(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_GCAL_LAST_SYNC_TIME, timestamp).apply()
    }

    fun getSelectedCalendarInfoFromPrefs(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val calName = prefs.getString(KEY_GCAL_SELECTED_CAL_NAME, "") ?: ""
        val accName = prefs.getString(KEY_GCAL_SELECTED_ACCOUNT_NAME, "") ?: ""
        return calName to accName
    }

    fun setSelectedCalendar(context: Context, info: GoogleCalendarAccountInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_GCAL_SELECTED_CAL_ID, info.calendarId)
            .putString(KEY_GCAL_SELECTED_CAL_NAME, info.displayName)
            .putString(KEY_GCAL_SELECTED_ACCOUNT_NAME, info.accountName)
            .apply()
    }

    /**
     * Obtiene todos los calendarios disponibles en el dispositivo con permisos de escritura,
     * priorizando cuentas de Google (`com.google`) y el calendario principal del usuario.
     */
    fun getAvailableCalendars(context: Context): List<GoogleCalendarAccountInfo> {
        if (!hasCalendarPermissions(context)) return emptyList()

        val result = mutableListOf<GoogleCalendarAccountInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val ownerIdx = cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                val accessIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (cursor.moveToNext()) {
                    val calId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val accessLevel = if (accessIdx >= 0) cursor.getInt(accessIdx) else CalendarContract.Calendars.CAL_ACCESS_OWNER
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR && accessLevel != 0) {
                        continue
                    }

                    val displayName = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val accountName = if (accNameIdx >= 0) cursor.getString(accNameIdx).orEmpty() else ""
                    val accountType = if (accTypeIdx >= 0) cursor.getString(accTypeIdx).orEmpty() else ""
                    val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF4285F4.toInt()
                    val ownerAccount = if (ownerIdx >= 0) cursor.getString(ownerIdx).orEmpty() else ""
                    val isPrimaryFlag = if (primaryIdx >= 0) cursor.getInt(primaryIdx) == 1 else false
                    val isPrimary = isPrimaryFlag || (accountName.isNotBlank() && accountName.equals(ownerAccount, ignoreCase = true))

                    result.add(
                        GoogleCalendarAccountInfo(
                            calendarId = calId,
                            displayName = displayName.ifBlank { accountName.ifBlank { "Google Calendar" } },
                            accountName = accountName,
                            accountType = accountType,
                            color = color,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando calendarios del dispositivo", e)
        }

        val preferredEmail = resolveSignedInGoogleEmail(context)

        return result.sortedWith(
            compareByDescending<GoogleCalendarAccountInfo> {
                preferredEmail.isNotBlank() && it.accountName.equals(preferredEmail, ignoreCase = true) && it.isPrimary
            }
                .thenByDescending {
                    preferredEmail.isNotBlank() && it.accountName.equals(preferredEmail, ignoreCase = true)
                }
                .thenByDescending { it.isGoogleAccount && it.isPrimary }
                .thenByDescending { it.isGoogleAccount }
                .thenByDescending { it.isPrimary }
        )
    }

    /**
     * Resuelve el calendario de Google activo para sincronizar. Si no hay ninguno en el dispositivo
     * (por ejemplo, en un emulador sin cuenta configurada), crea un calendario local de respaldo
     * para que CalendarContract funcione sin errores.
     */
    fun resolveTargetCalendar(context: Context): GoogleCalendarAccountInfo? {
        if (!hasCalendarPermissions(context)) return null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCalId = prefs.getLong(KEY_GCAL_SELECTED_CAL_ID, -1L)
        val available = getAvailableCalendars(context)

        if (savedCalId > 0L) {
            val matched = available.firstOrNull { it.calendarId == savedCalId }
            if (matched != null) {
                return matched
            }
        }

        if (available.isNotEmpty()) {
            val best = available.first()
            setSelectedCalendar(context, best)
            return best
        }

        return ensureFallbackCalendarExists(context)?.also {
            setSelectedCalendar(context, it)
        }
    }

    private fun ensureFallbackCalendarExists(context: Context): GoogleCalendarAccountInfo? {
        if (!hasCalendarPermissions(context)) return null
        val email = resolveSignedInGoogleEmail(context).ifBlank { "usuario@gmail.com" }
        val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Calendars.ACCOUNT_NAME, email)
                put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                put(CalendarContract.Calendars.NAME, "Note JS - Google Calendar")
                put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Google Calendar ($email)")
                put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF4285F4.toInt())
                put(
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CAL_ACCESS_OWNER
                )
                put(CalendarContract.Calendars.OWNER_ACCOUNT, email)
                put(CalendarContract.Calendars.VISIBLE, 1)
                put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            }

            val syncUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, email)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                .build()

            val insertedUri = context.contentResolver.insert(syncUri, values)
            val newId = insertedUri?.let { ContentUris.parseId(it) } ?: -1L
            if (newId > 0L) {
                GoogleCalendarAccountInfo(
                    calendarId = newId,
                    displayName = "Google Calendar",
                    accountName = email,
                    accountType = accountType,
                    color = 0xFF4285F4.toInt(),
                    isPrimary = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo crear calendario local de respaldo: ${e.message}")
            null
        }
    }

    private fun resolveSignedInGoogleEmail(context: Context): String {
        try {
            val fbEmail = FirebaseAuth.getInstance().currentUser?.email
            if (!fbEmail.isNullOrBlank()) return fbEmail
        } catch (_: Exception) {}

        try {
            val gsaEmail = GoogleSignIn.getLastSignedInAccount(context)?.email
            if (!gsaEmail.isNullOrBlank()) return gsaEmail
        } catch (_: Exception) {}

        return ""
    }

    /**
     * Sincroniza automáticamente un [ReminderEntity] (Evento o Recordatorio) hacia Google Calendar
     * cuando se crea, edita, reprograma o marca como completado en Note JS.
     */
    suspend fun syncItemToGoogleCalendar(
        context: Context,
        item: ReminderEntity,
        forceEvenIfDisabled: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) return@withContext null
        if (!forceEvenIfDisabled && !isSyncEnabled(context)) return@withContext null
        if (item.isReminder && !forceEvenIfDisabled && !isSyncRemindersEnabled(context)) {
            return@withContext null
        }

        itemSyncMutex.withLock {
            val targetCal = resolveTargetCalendar(context) ?: return@withContext null
            upsertEventInProvider(context, targetCal, item).also { eventId ->
                if (eventId != null && eventId > 0L) {
                    requestGoogleSyncAdapterRefresh(targetCal)
                }
            }
        }
    }

    /**
     * Elimina de Google Calendar un evento o recordatorio que fue eliminado en Note JS.
     */
    suspend fun deleteItemFromGoogleCalendar(
        context: Context,
        remoteUuid: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (remoteUuid.isBlank() || !hasCalendarPermissions(context)) return@withContext false

        itemSyncMutex.withLock {
            val eventId = findGoogleEventIdForUuid(context, remoteUuid) ?: return@withContext false
            try {
                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                val rows = context.contentResolver.delete(deleteUri, null, null)
                removeMappedGoogleEventId(context, remoteUuid)
                resolveTargetCalendar(context)?.let { requestGoogleSyncAdapterRefresh(it) }
                rows > 0
            } catch (e: Exception) {
                Log.w(TAG, "Error eliminando evento de Google Calendar ($remoteUuid): ${e.message}")
                false
            }
        }
    }

    /**
     * Ejecuta una sincronización bidireccional completa entre Note JS y Google Calendar:
     * 1. Exporta/actualiza todos los Eventos y Recordatorios activos de Note JS en Google Calendar.
     * 2. Importa eventos desde Google Calendar hacia la base de datos local de Note JS (Room).
     */
    suspend fun syncBidirectionalWithGoogleCalendar(
        context: Context
    ): GoogleCalendarSyncResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions(context)) {
            return@withContext GoogleCalendarSyncResult(
                success = false,
                message = "Concede permisos de Calendario para sincronizar con Google Calendar"
            )
        }

        fullSyncMutex.withLock {
            try {
                val targetCal = resolveTargetCalendar(context)
                    ?: return@withContext GoogleCalendarSyncResult(
                        success = false,
                        message = "No se encontró ninguna cuenta de Google Calendar en el dispositivo"
                    )

                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val calendarEventDao = db.calendarEventDao()
                val activeUid = resolveCurrentUserId()

                val includeReminders = isSyncRemindersEnabled(context)
                val importEvents = isImportEventsEnabled(context)

                // 1. Reunir todos los elementos activos en Note JS
                val unifiedItems = reminderDao.getRemindersForUserList(activeUid)
                val standaloneReminders = reminderDao.getReminderItemsForUserList(activeUid)
                val standaloneEvents = calendarEventDao.getCalendarEventsForUserList(activeUid)

                val byUuid = linkedMapOf<String, ReminderEntity>()
                unifiedItems.filter { !it.pendingDelete }.forEach { entity ->
                    val key = entity.remoteUuid.ifBlank { "u_${entity.id}" }
                    byUuid[key] = entity
                }
                standaloneReminders.filter { !it.pendingDelete }.forEach { rem ->
                    val key = rem.remoteUuid.ifBlank { "r_${rem.id}" }
                    val existing = byUuid[key]
                    if (existing == null || rem.lastModifiedLocal > existing.lastModifiedLocal) {
                        byUuid[key] = rem.toReminderEntity().copy(id = existing?.id ?: rem.id)
                    }
                }
                standaloneEvents.filter { !it.pendingDelete }.forEach { ev ->
                    val key = ev.remoteUuid.ifBlank { "e_${ev.id}" }
                    val existing = byUuid[key]
                    if (existing == null || ev.lastModifiedLocal > existing.lastModifiedLocal) {
                        byUuid[key] = ev.toReminderEntity().copy(id = existing?.id ?: ev.id)
                    }
                }

                // También incluir notas con recordatorio legacy si aún no tienen entidad
                val noteDao = db.noteDao()
                val allNotes = noteDao.getAllNotesList()
                val linkedNoteIds = byUuid.values.mapNotNull { it.linkedNoteId }.toSet()
                for (note in allNotes) {
                    val ts = note.reminderTimestamp
                    if (!note.isTrashed && !note.pendingDelete && ts != null && note.id !in linkedNoteIds) {
                        val promotedUuid = "rem_note_${note.id}"
                        if (!byUuid.containsKey(promotedUuid)) {
                            byUuid[promotedUuid] = ReminderEntity(
                                id = 0L,
                                remoteUuid = promotedUuid,
                                userId = activeUid,
                                itemType = CalendarItemType.REMINDER.code,
                                title = note.title.ifBlank { "Recordatorio de nota" },
                                notes = "",
                                scheduledAt = ts,
                                isCompleted = note.isReminderDone,
                                linkedNoteId = note.id,
                                linkedNoteUuid = note.remoteUuid,
                                linkedNoteTitle = note.title.ifBlank { "Nota vinculada" }
                            )
                        }
                    }
                }

                var exportedCount = 0
                val syncedGoogleIds = mutableSetOf<Long>()

                // 2. Empujar elementos de Note JS hacia Google Calendar
                for (item in byUuid.values) {
                    if (item.isReminder && !includeReminders) continue
                    val gcalId = itemSyncMutex.withLock {
                        upsertEventInProvider(context, targetCal, item)
                    }
                    if (gcalId != null && gcalId > 0L) {
                        exportedCount++
                        syncedGoogleIds.add(gcalId)
                    }
                }

                var importedCount = 0
                var updatedFromGoogleCount = 0

                // 3. Importar eventos y cambios desde Google Calendar hacia Note JS
                if (importEvents) {
                    val now = System.currentTimeMillis()
                    val windowStart = now - 30L * 24L * 3600_000L // Últimos 30 días
                    val windowEnd = now + 365L * 24L * 3600_000L // Próximo año

                    val providerEvents = queryGoogleCalendarEventsInWindow(
                        context = context,
                        calendarId = targetCal.calendarId,
                        startMillis = windowStart,
                        endMillis = windowEnd
                    )

                    for (gEvent in providerEvents) {
                        val existingUuid = gEvent.extractedUuid
                            ?: getUuidByGoogleEventId(context, gEvent.eventId)
                            ?: "gcal_${gEvent.eventId}"

                        val localUnified = reminderDao.getReminderByRemoteUuid(existingUuid)
                        val localCalEv = calendarEventDao.getCalendarEventByRemoteUuid(existingUuid)
                        val localRemItem = reminderDao.getReminderItemByRemoteUuid(existingUuid)

                        if (localUnified == null && localCalEv == null && localRemItem == null) {
                            // Es un evento nuevo proveniente de Google Calendar -> Importar a Note JS
                            val isRem = gEvent.isReminderStyle
                            val cleanTitle = gEvent.cleanTitle.ifBlank { "Evento de Google Calendar" }
                            val newEntity = ReminderEntity(
                                id = 0L,
                                remoteUuid = existingUuid,
                                userId = activeUid,
                                itemType = if (isRem) CalendarItemType.REMINDER.code else CalendarItemType.EVENT.code,
                                title = cleanTitle,
                                notes = gEvent.cleanDescription,
                                location = gEvent.location,
                                scheduledAt = gEvent.startMillis,
                                endTimeMillis = if (isRem) 0L else gEvent.endMillis,
                                isAllDay = gEvent.isAllDay,
                                isCompleted = gEvent.isCompleted,
                                listName = "Google Calendar",
                                tags = listOf("#GoogleCalendar"),
                                colorHex = 0xFF4285F4L,
                                alertOffsetMinutes = 15,
                                isSynced = false,
                                pendingDelete = false,
                                createdAt = now,
                                updatedAt = now,
                                lastModifiedLocal = now
                            )

                            val insertedUnifiedId = reminderDao.insertReminder(newEntity)
                            val savedUnified = newEntity.copy(id = insertedUnifiedId)
                            if (isRem) {
                                reminderDao.insertReminderItem(Reminder.fromReminderEntity(savedUnified).copy(id = 0L))
                            } else {
                                calendarEventDao.insertCalendarEvent(CalendarEvent.fromReminderEntity(savedUnified).copy(id = 0L))
                            }
                            ReminderScheduler.scheduleStandaloneReminder(context, savedUnified)
                            saveMappedGoogleEventId(context, existingUuid, gEvent.eventId)
                            importedCount++
                        } else if (gEvent.eventId !in syncedGoogleIds) {
                            // Ya existía localmente: verificar si cambió en Google Calendar
                            val base = localUnified
                                ?: localCalEv?.toReminderEntity()
                                ?: localRemItem?.toReminderEntity()
                            if (base != null) {
                                val cleanTitle = gEvent.cleanTitle.ifBlank { base.title }
                                val titleChanged = cleanTitle != base.title
                                val timeChanged = kotlin.math.abs(gEvent.startMillis - base.scheduledAt) > 60_000L
                                val locChanged = gEvent.location.isNotBlank() && gEvent.location != base.location

                                if (titleChanged || timeChanged || locChanged) {
                                    val updatedEntity = base.copy(
                                        title = cleanTitle,
                                        scheduledAt = gEvent.startMillis,
                                        endTimeMillis = if (base.isEvent) gEvent.endMillis else base.endTimeMillis,
                                        location = gEvent.location.ifBlank { base.location },
                                        isAllDay = gEvent.isAllDay,
                                        updatedAt = now,
                                        lastModifiedLocal = now,
                                        isSynced = false
                                    )
                                    if (localUnified != null) {
                                        reminderDao.updateReminder(updatedEntity.copy(id = localUnified.id))
                                    }
                                    if (localCalEv != null) {
                                        calendarEventDao.updateCalendarEvent(
                                            CalendarEvent.fromReminderEntity(updatedEntity).copy(id = localCalEv.id)
                                        )
                                    }
                                    if (localRemItem != null) {
                                        reminderDao.updateReminderItem(
                                            Reminder.fromReminderEntity(updatedEntity).copy(id = localRemItem.id)
                                        )
                                    }
                                    ReminderScheduler.scheduleStandaloneReminder(context, updatedEntity)
                                    updatedFromGoogleCount++
                                }
                            }
                        }
                    }
                }

                // 4. Solicitar al SyncAdapter de Google que sincronice con la nube de Google Calendar
                requestGoogleSyncAdapterRefresh(targetCal)

                val syncTime = System.currentTimeMillis()
                setLastSyncTime(context, syncTime)

                try {
                    com.example.widget.CalendarRemindersWidget().updateAll(context)
                } catch (_: Exception) {}

                val summaryMsg = buildString {
                    append("Sincronizado con Google Calendar")
                    if (targetCal.accountName.isNotBlank()) {
                        append(" (${targetCal.accountName})")
                    }
                    append(" • $exportedCount enviados")
                    if (importedCount > 0) append(", $importedCount importados")
                    if (updatedFromGoogleCount > 0) append(", $updatedFromGoogleCount actualizados")
                }

                GoogleCalendarSyncResult(
                    success = true,
                    exportedCount = exportedCount,
                    importedCount = importedCount,
                    updatedCount = updatedFromGoogleCount,
                    calendarName = targetCal.displayName,
                    accountName = targetCal.accountName,
                    message = summaryMsg
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error en sincronización bidireccional con Google Calendar", e)
                GoogleCalendarSyncResult(
                    success = false,
                    message = "Error al sincronizar con Google Calendar: ${e.localizedMessage ?: "Intenta nuevamente"}"
                )
            }
        }
    }

    /**
     * Abre el evento o recordatorio directamente en la aplicación oficial de Google Calendar.
     */
    fun openInGoogleCalendarApp(context: Context, item: ReminderEntity) {
        try {
            val existingEventId = if (hasCalendarPermissions(context)) {
                findGoogleEventIdForUuid(context, item.remoteUuid)
            } else null

            if (existingEventId != null && existingEventId > 0L) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.scheduledAt)
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.effectiveEndMillis)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
                return
            }

            // Si aún no está insertado o no tiene permisos directos, abrir el editor de Google Calendar prellenado
            val insertIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, item.title)
                putExtra(CalendarContract.Events.DESCRIPTION, buildEventDescription(item, includeMarker = false))
                putExtra(CalendarContract.Events.EVENT_LOCATION, item.location)
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, item.isAllDay)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.scheduledAt)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.effectiveEndMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(insertIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No se encontró la aplicación de Calendario en este dispositivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // --- MÉTODOS INTERNOS DEL PROVEEDOR DE CALENDARIO ---

    private fun upsertEventInProvider(
        context: Context,
        targetCal: GoogleCalendarAccountInfo,
        item: ReminderEntity
    ): Long? {
        val resolver = context.contentResolver
        val uuid = item.remoteUuid.ifBlank { "local_${item.id}" }
        val existingEventId = findGoogleEventIdForUuid(context, uuid)

        val values = buildEventContentValues(targetCal.calendarId, item, uuid)

        val finalEventId: Long = try {
            if (existingEventId != null && existingEventId > 0L) {
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                val updatedRows = resolver.update(updateUri, values, null, null)
                if (updatedRows > 0) {
                    existingEventId
                } else {
                    val inserted = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    inserted?.let { ContentUris.parseId(it) } ?: -1L
                }
            } else {
                val inserted = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                inserted?.let { ContentUris.parseId(it) } ?: -1L
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error insertando/actualizando evento en CalendarContract", e)
            -1L
        }

        if (finalEventId <= 0L) return null

        saveMappedGoogleEventId(context, uuid, finalEventId)
        syncEventReminderAlarmInProvider(resolver, finalEventId, item)
        return finalEventId
    }

    private fun buildEventContentValues(
        calendarId: Long,
        item: ReminderEntity,
        uuid: String
    ): ContentValues {
        val baseTitle = item.title.trim()
            .removePrefix("✓ ")
            .removePrefix("📌 ")
            .trim()
            .ifBlank { if (item.isEvent) "Evento" else "Recordatorio" }

        val formattedTitle = when {
            item.isCompleted -> "✓ $baseTitle"
            item.isReminder -> "📌 $baseTitle"
            else -> baseTitle
        }

        val rrule = mapRecurrenceRuleToRRule(RecurrenceRule.fromCode(item.recurrenceRule))

        val startMs: Long
        val endMs: Long
        val timeZoneId: String

        if (item.isAllDay) {
            // CalendarContract exige medianoche UTC para eventos de día completo
            val localCal = Calendar.getInstance().apply { timeInMillis = item.scheduledAt }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0
                )
            }
            startMs = utcCal.timeInMillis
            endMs = startMs + 86_400_000L
            timeZoneId = "UTC"
        } else {
            startMs = item.scheduledAt
            endMs = item.effectiveEndMillis
            timeZoneId = TimeZone.getDefault().id
        }

        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, formattedTitle)
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(item, includeMarker = true))
            put(CalendarContract.Events.EVENT_LOCATION, item.location)
            put(CalendarContract.Events.ALL_DAY, if (item.isAllDay) 1 else 0)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZoneId)
            put(CalendarContract.Events.CUSTOM_APP_URI, "$CUSTOM_URI_PREFIX$uuid")
            put(CalendarContract.Events.HAS_ALARM, if (item.isCompleted) 0 else 1)
            put(
                CalendarContract.Events.AVAILABILITY,
                if (item.isEvent) CalendarContract.Events.AVAILABILITY_BUSY
                else CalendarContract.Events.AVAILABILITY_FREE
            )

            if (rrule != null) {
                val durationMinutes = ((endMs - startMs) / 60_000L).coerceAtLeast(30L)
                put(CalendarContract.Events.RRULE, rrule)
                put(CalendarContract.Events.DURATION, "PT${durationMinutes}M")
                putNull(CalendarContract.Events.DTEND)
            } else {
                put(CalendarContract.Events.DTEND, endMs)
                putNull(CalendarContract.Events.RRULE)
                putNull(CalendarContract.Events.DURATION)
            }
        }
    }

    private fun syncEventReminderAlarmInProvider(
        resolver: ContentResolver,
        eventId: Long,
        item: ReminderEntity
    ) {
        try {
            resolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID} = ?",
                arrayOf(eventId.toString())
            )
            if (!item.isCompleted) {
                val reminderValues = ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, item.alertOffsetMinutes.coerceAtLeast(0))
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo configurar alerta en CalendarContract.Reminders: ${e.message}")
        }
    }

    private fun buildEventDescription(item: ReminderEntity, includeMarker: Boolean): String {
        val parts = mutableListOf<String>()
        if (item.notes.isNotBlank()) {
            parts.add(item.notes.trim())
        }
        val subs = item.subtasks
        if (subs.isNotEmpty()) {
            val subText = buildString {
                append("Subtareas:\n")
                subs.forEach { sub ->
                    append(if (sub.isCompleted) "☑ " else "☐ ")
                    append(sub.title)
                    append("\n")
                }
            }.trimEnd()
            parts.add(subText)
        }
        if (item.linkedNoteTitle.isNotBlank()) {
            parts.add("Nota vinculada en Note JS: ${item.linkedNoteTitle}")
        }
        if (includeMarker && item.remoteUuid.isNotBlank()) {
            parts.add("$DESCRIPTION_MARKER_PREFIX${item.remoteUuid}]")
        }
        return parts.joinToString("\n\n")
    }

    private fun mapRecurrenceRuleToRRule(rule: RecurrenceRule): String? {
        return when (rule) {
            RecurrenceRule.NONE -> null
            RecurrenceRule.DAILY -> "FREQ=DAILY"
            RecurrenceRule.WEEKDAYS -> "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
            RecurrenceRule.WEEKLY -> "FREQ=WEEKLY"
            RecurrenceRule.MONTHLY -> "FREQ=MONTHLY"
            RecurrenceRule.YEARLY -> "FREQ=YEARLY"
        }
    }

    private data class ProviderEventRecord(
        val eventId: Long,
        val rawTitle: String,
        val rawDescription: String,
        val location: String,
        val startMillis: Long,
        val endMillis: Long,
        val isAllDay: Boolean,
        val customAppUri: String
    ) {
        val isCompleted: Boolean
            get() = rawTitle.trimStart().startsWith("✓")

        val isReminderStyle: Boolean
            get() = rawTitle.trimStart().startsWith("📌") ||
                rawTitle.contains("[Recordatorio]", ignoreCase = true)

        val cleanTitle: String
            get() = rawTitle.trim()
                .removePrefix("✓ ")
                .removePrefix("✓")
                .removePrefix("📌 ")
                .removePrefix("📌")
                .trim()

        val extractedUuid: String?
            get() {
                if (customAppUri.startsWith(CUSTOM_URI_PREFIX)) {
                    return customAppUri.removePrefix(CUSTOM_URI_PREFIX).takeIf { it.isNotBlank() }
                }
                val markerStart = rawDescription.indexOf(DESCRIPTION_MARKER_PREFIX)
                if (markerStart >= 0) {
                    val after = rawDescription.substring(markerStart + DESCRIPTION_MARKER_PREFIX.length)
                    val endBracket = after.indexOf(']')
                    if (endBracket > 0) {
                        return after.substring(0, endBracket).trim().takeIf { it.isNotBlank() }
                    }
                }
                return null
            }

        val cleanDescription: String
            get() {
                var desc = rawDescription
                val markerIdx = desc.indexOf(DESCRIPTION_MARKER_PREFIX)
                if (markerIdx >= 0) {
                    desc = desc.substring(0, markerIdx).trim()
                }
                return desc
            }
    }

    private fun queryGoogleCalendarEventsInWindow(
        context: Context,
        calendarId: Long,
        startMillis: Long,
        endMillis: Long
    ): List<ProviderEventRecord> {
        val records = mutableListOf<ProviderEventRecord>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.DELETED
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
            "${CalendarContract.Events.DTSTART} >= ? AND " +
            "${CalendarContract.Events.DTSTART} <= ? AND " +
            "${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(
            calendarId.toString(),
            startMillis.toString(),
            endMillis.toString()
        )

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val uriIdx = cursor.getColumnIndex(CalendarContract.Events.CUSTOM_APP_URI)

                while (cursor.moveToNext()) {
                    val eventId = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else ""
                    if (title.isBlank()) continue

                    val desc = if (descIdx >= 0) cursor.getString(descIdx).orEmpty() else ""
                    val loc = if (locIdx >= 0) cursor.getString(locIdx).orEmpty() else ""
                    val rawStart = if (startIdx >= 0) cursor.getLong(startIdx) else continue
                    val rawEnd = if (endIdx >= 0 && !cursor.isNull(endIdx)) cursor.getLong(endIdx) else rawStart + 3_600_000L
                    val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val customUri = if (uriIdx >= 0) cursor.getString(uriIdx).orEmpty() else ""

                    val normalizedStart = if (isAllDay) {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = rawStart
                        }
                        Calendar.getInstance().apply {
                            set(
                                utcCal.get(Calendar.YEAR),
                                utcCal.get(Calendar.MONTH),
                                utcCal.get(Calendar.DAY_OF_MONTH),
                                9,
                                0,
                                0
                            )
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    } else {
                        rawStart
                    }

                    val normalizedEnd = if (isAllDay) {
                        normalizedStart + 3_600_000L
                    } else if (rawEnd > normalizedStart) {
                        rawEnd
                    } else {
                        normalizedStart + 3_600_000L
                    }

                    records.add(
                        ProviderEventRecord(
                            eventId = eventId,
                            rawTitle = title,
                            rawDescription = desc,
                            location = loc,
                            startMillis = normalizedStart,
                            endMillis = normalizedEnd,
                            isAllDay = isAllDay,
                            customAppUri = customUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo eventos de Google Calendar", e)
        }
        return records
    }

    private fun findGoogleEventIdForUuid(context: Context, remoteUuid: String): Long? {
        if (remoteUuid.isBlank()) return null

        if (remoteUuid.startsWith("gcal_")) {
            val parsed = remoteUuid.removePrefix("gcal_").toLongOrNull()
            if (parsed != null && parsed > 0L && verifyEventExists(context, parsed)) {
                return parsed
            }
        }

        val mapped = getMappedGoogleEventId(context, remoteUuid)
        if (mapped != null && mapped > 0L && verifyEventExists(context, mapped)) {
            return mapped
        }

        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.CUSTOM_APP_URI} = ? AND ${CalendarContract.Events.DELETED} = 0"
        val args = arrayOf("$CUSTOM_URI_PREFIX$remoteUuid")

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val foundId = cursor.getLong(0)
                    if (foundId > 0L) {
                        saveMappedGoogleEventId(context, remoteUuid, foundId)
                        return foundId
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun verifyEventExists(context: Context, eventId: Long): Boolean {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val projection = arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DELETED)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val deletedIdx = cursor.getColumnIndex(CalendarContract.Events.DELETED)
                    val isDeleted = if (deletedIdx >= 0) cursor.getInt(deletedIdx) == 1 else false
                    !isDeleted
                } else {
                    false
                }
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getMappedGoogleEventId(context: Context, remoteUuid: String): Long? {
        val prefs = context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getLong("uuid_$remoteUuid", -1L)
        return id.takeIf { it > 0L }
    }

    private fun getUuidByGoogleEventId(context: Context, eventId: Long): String? {
        val prefs = context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("ev_$eventId", null)?.takeIf { it.isNotBlank() }
    }

    private fun saveMappedGoogleEventId(context: Context, remoteUuid: String, eventId: Long) {
        if (remoteUuid.isBlank() || eventId <= 0L) return
        val prefs = context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("uuid_$remoteUuid", eventId)
            .putString("ev_$eventId", remoteUuid)
            .apply()
    }

    private fun removeMappedGoogleEventId(context: Context, remoteUuid: String) {
        val prefs = context.getSharedPreferences(MAP_PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getLong("uuid_$remoteUuid", -1L)
        val editor = prefs.edit().remove("uuid_$remoteUuid")
        if (existingId > 0L) {
            editor.remove("ev_$existingId")
        }
        editor.apply()
    }

    private fun requestGoogleSyncAdapterRefresh(calendar: GoogleCalendarAccountInfo) {
        if (calendar.accountName.isBlank() || calendar.accountType.isBlank()) return
        if (calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) return
        try {
            val account = Account(calendar.accountName, calendar.accountType)
            val extras = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }
            ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
        } catch (e: Exception) {
            Log.d(TAG, "No se pudo solicitar requestSync inmediato: ${e.message}")
        }
    }

    private fun resolveCurrentUserId(): String {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID
        } catch (_: Exception) {
            NoteEntity.LOCAL_ANONYMOUS_USER_ID
        }
    }
}
