package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.Reminder
import com.example.data.local.entity.ReminderEntity
import com.example.data.remote.OfflineSyncWorker
import com.example.util.ReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val EXTRA_OPEN_CALENDAR_KEY = ActionParameters.Key<Boolean>("EXTRA_OPEN_CALENDAR")
val EXTRA_OPEN_REMINDER_ID_KEY = ActionParameters.Key<Long>("EXTRA_OPEN_REMINDER_ID")

val PARAM_REMINDER_ID = ActionParameters.Key<Long>("PARAM_REMINDER_ID")
val PARAM_REMINDER_UUID = ActionParameters.Key<String>("PARAM_REMINDER_UUID")

data class WidgetCalendarItem(
    val id: Long,
    val remoteUuid: String,
    val title: String,
    val scheduledAt: Long,
    val endTimeMillis: Long = 0L,
    val isAllDay: Boolean = false,
    val isCompleted: Boolean = false,
    val isEvent: Boolean = false,
    val priority: Int = 0,
    val listName: String = "Personal",
    val location: String = ""
)

/**
 * Jetpack Glance AppWidget para la pantalla de inicio mostrando los próximos
 * recordatorios y eventos de calendario, con interacción directa para completar tareas.
 */
class CalendarRemindersWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val items = withContext(Dispatchers.IO) {
            loadUpcomingCalendarItems(context)
        }

        provideContent {
            GlanceTheme {
                CalendarRemindersContent(items = items)
            }
        }
    }

    private suspend fun loadUpcomingCalendarItems(context: Context): List<WidgetCalendarItem> {
        return try {
            val db = AppDatabase.getDatabase(context)
            val authUid = FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID

            val remindersList = db.reminderDao().getRemindersForUserList(authUid)
            val reminderItemsList = db.reminderDao().getReminderItemsForUserList(authUid)
            val calendarEventsList = db.calendarEventDao().getCalendarEventsForUserList(authUid)

            val merged = linkedMapOf<String, WidgetCalendarItem>()

            // 1. Entidades unificadas de tabla 'reminders'
            remindersList.filter { !it.pendingDelete }.forEach { entity ->
                val key = entity.remoteUuid.ifBlank { "rem_${entity.id}" }
                merged[key] = WidgetCalendarItem(
                    id = entity.id,
                    remoteUuid = entity.remoteUuid,
                    title = entity.title,
                    scheduledAt = entity.scheduledAt,
                    endTimeMillis = entity.endTimeMillis,
                    isAllDay = entity.isAllDay,
                    isCompleted = entity.isCompleted,
                    isEvent = entity.isEvent,
                    priority = entity.priority,
                    listName = entity.listName,
                    location = entity.location
                )
            }

            // 2. Entidades de tabla 'reminder_items'
            reminderItemsList.filter { !it.pendingDelete }.forEach { item ->
                val key = item.remoteUuid.ifBlank { "item_${item.id}" }
                if (!merged.containsKey(key)) {
                    merged[key] = WidgetCalendarItem(
                        id = item.id,
                        remoteUuid = item.remoteUuid,
                        title = item.title,
                        scheduledAt = item.timestamp,
                        endTimeMillis = 0L,
                        isAllDay = false,
                        isCompleted = item.isCompleted,
                        isEvent = false,
                        priority = item.priority,
                        listName = "Recordatorios",
                        location = ""
                    )
                }
            }

            // 3. Entidades de tabla 'calendar_events'
            calendarEventsList.filter { !it.pendingDelete }.forEach { event ->
                val key = event.remoteUuid.ifBlank { "event_${event.id}" }
                if (!merged.containsKey(key)) {
                    merged[key] = WidgetCalendarItem(
                        id = event.id,
                        remoteUuid = event.remoteUuid,
                        title = event.title,
                        scheduledAt = event.timestamp,
                        endTimeMillis = event.timestamp + 3600_000L,
                        isAllDay = false,
                        isCompleted = event.isCompleted,
                        isEvent = true,
                        priority = 0,
                        listName = "Eventos",
                        location = ""
                    )
                }
            }

            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calToday.timeInMillis
            val now = System.currentTimeMillis()

            // Filtramos elementos relevantes para el widget:
            // - Recordatorios pendientes (incluso atrasados para que no se pierdan)
            // - Recordatorios completados hoy (para poder verlos o desmarcarlos)
            // - Eventos de hoy o futuros
            merged.values.filter { item ->
                if (item.isEvent) {
                    val eventEnd = if (item.endTimeMillis > item.scheduledAt) item.endTimeMillis else item.scheduledAt
                    eventEnd >= startOfToday
                } else {
                    !item.isCompleted || (item.scheduledAt >= startOfToday - 86400000L)
                }
            }.sortedWith(
                compareBy<WidgetCalendarItem> { item ->
                    // Priorizamos pendientes sobre completados
                    if (item.isCompleted) 1 else 0
                }.thenBy { item ->
                    // Si es recordatorio pendiente atrasado, darle alta prioridad
                    val isOverdue = !item.isCompleted && !item.isEvent && item.scheduledAt < now
                    if (isOverdue) 0 else 1
                }.thenBy { item ->
                    item.scheduledAt
                }.thenByDescending { item ->
                    item.priority
                }
            ).take(20)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

/**
 * Receiver para el widget de Calendario y Recordatorios en Android.
 */
class CalendarRemindersWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarRemindersWidget()
}

/**
 * Callback de acción de Glance para completar o desmarcar recordatorios directamente desde la pantalla de inicio.
 */
class ToggleWidgetReminderAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val reminderId = parameters[PARAM_REMINDER_ID] ?: return
        val reminderUuid = parameters[PARAM_REMINDER_UUID] ?: ""

        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminderDao = db.reminderDao()
                val now = System.currentTimeMillis()

                // 1. Buscar en tabla unificada 'reminders'
                val existingUnified = if (reminderId > 0L) {
                    reminderDao.getReminderById(reminderId)
                } else if (reminderUuid.isNotBlank()) {
                    reminderDao.getReminderByRemoteUuid(reminderUuid)
                } else null

                if (existingUnified != null) {
                    val newCompleted = !existingUnified.isCompleted
                    val updated = existingUnified.copy(
                        isCompleted = newCompleted,
                        completedAt = if (newCompleted) now else 0L,
                        updatedAt = now,
                        lastModifiedLocal = now,
                        isSynced = false
                    )
                    reminderDao.updateReminder(updated)

                    // Actualizar alarma de notificación
                    if (newCompleted) {
                        ReminderScheduler.cancelStandaloneReminder(context, updated.id)
                    } else {
                        ReminderScheduler.scheduleStandaloneReminder(context, updated)
                    }
                }

                // 2. Buscar en tabla 'reminder_items'
                val existingItem = if (reminderId > 0L) {
                    reminderDao.getReminderItemById(reminderId)
                } else if (reminderUuid.isNotBlank()) {
                    reminderDao.getReminderItemByRemoteUuid(reminderUuid)
                } else null

                if (existingItem != null) {
                    val newCompleted = !existingItem.isCompleted
                    val updatedItem = existingItem.copy(
                        isCompleted = newCompleted,
                        completedAt = if (newCompleted) now else 0L,
                        updatedAt = now,
                        lastModifiedLocal = now,
                        isSynced = false
                    )
                    reminderDao.updateReminderItem(updatedItem)

                    if (newCompleted) {
                        ReminderScheduler.cancelStandaloneReminder(context, existingItem.id)
                    }
                }

                // Sincronizar en segundo plano si hay usuario autenticado
                val authUid = FirebaseAuth.getInstance().currentUser?.uid
                if (authUid != null && authUid.isNotBlank() && authUid != NoteEntity.LOCAL_ANONYMOUS_USER_ID) {
                    OfflineSyncWorker.enqueueWhenConnected(context)
                }
            } catch (_: Exception) {
            }
        }

        // Actualizar el widget de inmediato para respuesta visual instantánea
        CalendarRemindersWidget().update(context, glanceId)
        CalendarRemindersWidget().updateAll(context)
    }
}

@Composable
private fun CalendarRemindersContent(items: List<WidgetCalendarItem>) {
    val pendingCount = items.count { !it.isCompleted }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, d MMM", Locale("es", "ES"))
    val todayHeader = dateFormat.format(Date()).replaceFirstChar { it.titlecase(Locale("es", "ES")) }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(12.dp)
    ) {
        // --- Cabecera del Widget ---
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_calendar),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier.size(20.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Agenda y Recordatorios",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Text(
                    text = "$todayHeader • $pendingCount pendientes",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }

            Button(
                text = "+ Añadir",
                onClick = actionStartActivity<MainActivity>(
                    actionParametersOf(EXTRA_OPEN_CALENDAR_KEY to true)
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // --- Lista o Estado Vacío ---
        if (items.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(EXTRA_OPEN_CALENDAR_KEY to true)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.padding(16.dp)
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_event),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                        modifier = GlanceModifier.size(28.dp)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "Sin eventos ni recordatorios próximos",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Toca para abrir el calendario",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(items = items, itemId = { item -> item.id }) { item ->
                    WidgetCalendarItemRow(
                        item = item,
                        timeFormat = timeFormat,
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCalendarItemRow(
    item: WidgetCalendarItem,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat
) {
    val now = System.currentTimeMillis()
    val isOverdue = !item.isCompleted && !item.isEvent && !item.isAllDay && item.scheduledAt < now

    val calItem = Calendar.getInstance().apply { timeInMillis = item.scheduledAt }
    val calToday = Calendar.getInstance()
    val isToday = calItem.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
        calItem.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

    val timeLabel = when {
        item.isAllDay -> "Todo el día"
        item.isEvent && item.endTimeMillis > item.scheduledAt -> {
            val start = timeFormat.format(Date(item.scheduledAt))
            val end = timeFormat.format(Date(item.endTimeMillis))
            "$start - $end"
        }
        isToday -> "Hoy ${timeFormat.format(Date(item.scheduledAt))}"
        else -> "${dateFormat.format(Date(item.scheduledAt))} ${timeFormat.format(Date(item.scheduledAt))}"
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Check interactivo para Recordatorio o Icono para Evento
        if (item.isEvent) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_event),
                contentDescription = "Evento de calendario",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(EXTRA_OPEN_REMINDER_ID_KEY to item.id)
                        )
                    )
            )
        } else {
            val checkIcon = if (item.isCompleted) {
                R.drawable.ic_widget_check_done
            } else {
                R.drawable.ic_widget_check_circle
            }
            Image(
                provider = ImageProvider(checkIcon),
                contentDescription = if (item.isCompleted) "Completado" else "Marcar como completado",
                modifier = GlanceModifier
                    .size(26.dp)
                    .clickable(
                        actionRunCallback<ToggleWidgetReminderAction>(
                            actionParametersOf(
                                PARAM_REMINDER_ID to item.id,
                                PARAM_REMINDER_UUID to item.remoteUuid
                            )
                        )
                    )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Contenido del elemento (tocar abre la app en el recordatorio)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(
                    actionStartActivity<MainActivity>(
                        actionParametersOf(EXTRA_OPEN_REMINDER_ID_KEY to item.id)
                    )
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.priority > 0) {
                    Text(
                        text = "!".repeat(item.priority) + " ",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GlanceTheme.colors.error
                        )
                    )
                }
                Text(
                    text = item.title,
                    maxLines = 1,
                    style = TextStyle(
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            val subtitleText = when {
                isOverdue -> "⚠️ Vencido • $timeLabel"
                item.location.isNotBlank() -> "$timeLabel • 📍 ${item.location}"
                else -> "$timeLabel • ${item.listName}"
            }

            Text(
                text = subtitleText,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = if (isOverdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}
