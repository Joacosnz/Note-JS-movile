package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"
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

                scheduleAlarm(alarmManager, triggerTime, pendingIntent)
            }
        }
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
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
}
