package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        noteId: Long,
        timestamp: Long,
        title: String,
        content: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NoteReminderReceiver::class.java).apply {
            putExtra("EXTRA_NOTE_ID", noteId)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_CONTENT", content)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            }
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    timestamp,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NoteReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
