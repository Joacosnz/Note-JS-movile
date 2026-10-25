package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.entity.ReminderEntity

object ReminderScheduler {
    const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"
    const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
    const val EXTRA_ITEM_TYPE = "EXTRA_ITEM_TYPE"
    const val EXTRA_TITLE = "EXTRA_TITLE"
    const val EXTRA_CONTENT = "EXTRA_CONTENT"
    const val EXTRA_OFFSET = "EXTRA_OFFSET"
    const val EXTRA_ORIGINAL_TIME = "EXTRA_ORIGINAL_TIME"

    val ALL_POSSIBLE_OFFSETS = listOf(1440, 720, 120, 60, 30, 15, 5, 0)

    /**
     * Genera un identificador acotado y seguro para PendingIntent y notificaciones.
     * La máscara `and 0x7FFF` limita el hash a 15 bits positivos (máximo 32767).
     * Multiplicado por 100 y sumando el offset (< 100), el valor máximo es ~3.27 millones,
     * garantizando total inmunidad ante Integer Overflow y evitando números negativos.
     */
    fun getReminderRequestCode(noteId: Long, offsetMinutes: Int): Int {
        return ((noteId.hashCode() and 0x7FFF) * 100) + (offsetMinutes % 100)
    }

    fun getStandaloneReminderRequestCode(reminderId: Long, offsetMinutes: Int = 0): Int {
        return 5_000_000 + ((reminderId.hashCode() and 0x7FFF) * 100) + (offsetMinutes.coerceAtLeast(0) % 100)
    }

    fun scheduleStandaloneReminder(
        context: Context,
        reminder: ReminderEntity
    ) {
        cancelStandaloneReminder(context, reminder.id)
        if (reminder.isCompleted || reminder.pendingDelete || reminder.alertOffsetMinutes < 0) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()
        val offsetMins = reminder.alertOffsetMinutes.coerceAtLeast(0)
        val triggerTime = reminder.scheduledAt - (offsetMins * 60_000L)

        if (triggerTime > currentTime) {
            val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_NOTE_ID, reminder.linkedNoteId ?: 0L)
                putExtra(EXTRA_ITEM_TYPE, reminder.itemType)
                putExtra(EXTRA_TITLE, reminder.title)
                putExtra(
                    EXTRA_CONTENT,
                    buildString {
                        if (reminder.listName.isNotBlank()) append("[${reminder.listName}] ")
                        if (reminder.location.isNotBlank()) append("📍 ${reminder.location} ")
                        if (reminder.notes.isNotBlank()) append(reminder.notes)
                    }.trim()
                )
                putExtra(EXTRA_OFFSET, offsetMins)
                putExtra(EXTRA_ORIGINAL_TIME, reminder.scheduledAt)
            }
            val requestCode = getStandaloneReminderRequestCode(reminder.id, offsetMins)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(context, alarmManager, triggerTime, pendingIntent, requestCode)
        }
    }

    fun cancelStandaloneReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (offsetMinutes in ALL_POSSIBLE_OFFSETS + listOf(10)) {
            val requestCode = getStandaloneReminderRequestCode(reminderId, offsetMinutes)
            val intent = Intent(context, NoteReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleReminder(
        context: Context,
        noteId: Long,
        timestamp: Long,
        title: String,
        content: String,
        offsetsMinutes: List<Int> = listOf(0, 15)
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        cancelReminder(context, noteId)

        val activeOffsets = if (offsetsMinutes.isEmpty()) listOf(0) else offsetsMinutes.distinct()

        for (offsetMinutes in activeOffsets) {
            val triggerTime = timestamp - (offsetMinutes * 60 * 1000L)
            if (triggerTime > currentTime) {
                val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                    putExtra(EXTRA_NOTE_ID, noteId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_CONTENT, content)
                    putExtra(EXTRA_OFFSET, offsetMinutes)
                    putExtra(EXTRA_ORIGINAL_TIME, timestamp)
                }
                val requestCode = getReminderRequestCode(noteId, offsetMinutes)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarm(context, alarmManager, triggerTime, pendingIntent, requestCode)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent,
        requestCode: Int = 0
    ) {
        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    try {
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    } catch (_: Exception) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                }
            } else {
                try {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } catch (_: Exception) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        } else {
            try {
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (_: Exception) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (_: Exception) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (offsetMinutes in ALL_POSSIBLE_OFFSETS) {
            val requestCode = getReminderRequestCode(noteId, offsetMinutes)
            val intent = Intent(context, NoteReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun getItemReminderRequestCode(noteId: Long, itemId: String): Int {
        return ((noteId.hashCode() xor itemId.hashCode()) and 0x7FFF) * 100
    }

    fun scheduleItemReminder(
        context: Context,
        noteId: Long,
        itemId: String,
        timestamp: Long,
        title: String,
        content: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()

        cancelItemReminder(context, noteId, itemId)

        if (timestamp > currentTime) {
            val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_OFFSET, 0)
                putExtra(EXTRA_ORIGINAL_TIME, timestamp)
            }
            val requestCode = getItemReminderRequestCode(noteId, itemId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(context, alarmManager, timestamp, pendingIntent, requestCode)
        }
    }

    fun cancelItemReminder(context: Context, noteId: Long, itemId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = getItemReminderRequestCode(noteId, itemId)
        val intent = Intent(context, NoteReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    suspend fun computeNextOccurrenceMillis(
        context: Context,
        reminder: ReminderEntity
    ): Long? {
        val workdays = WorkdayPreferences.getWorkdays(context)
        return reminder.computeNextOccurrenceMillis(workdays)
    }
}
