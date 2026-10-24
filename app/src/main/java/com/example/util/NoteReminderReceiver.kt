package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.model.PdfBlock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(ReminderScheduler.EXTRA_NOTE_ID, 0L)
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0L)
        val itemType = intent.getStringExtra(ReminderScheduler.EXTRA_ITEM_TYPE) ?: "REMINDER"
        val rawTitle = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: ""
        val rawContent = intent.getStringExtra(ReminderScheduler.EXTRA_CONTENT) ?: ""
        val offsetMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_OFFSET, 0)
        val originalTime = intent.getLongExtra(ReminderScheduler.EXTRA_ORIGINAL_TIME, 0L)

        val defaultTitle = if (itemType.equals("EVENT", ignoreCase = true)) "Evento de calendario" else "Recordatorio"
        val baseTitle = if (rawTitle.isNotBlank()) rawTitle else defaultTitle
        val notificationTitle = if (offsetMinutes > 0) {
            "En $offsetMinutes min: $baseTitle"
        } else {
            baseTitle
        }

        // Extract excerpt if JSON content
        val excerpt = if (rawContent.trim().startsWith("[") && rawContent.contains("\"type\"")) {
            runCatching {
                val deserialized = NoteBlockSerializer.deserialize(rawContent)
                val lines = deserialized.map { block ->
                    when (block) {
                        is NoteBlock.Text -> block.content
                        is NoteBlock.Checklist -> "${if (block.isChecked) "☑" else "☐"} ${block.content}"
                        is NoteBlock.SimpleList -> block.content
                        is NoteBlock.CalloutBox -> block.content
                        is NoteBlock.Drawing -> "[Boceto]"
                        is NoteBlock.Table -> block.matrix.joinToString(" | ") { row -> row.joinToString(" | ") { cell -> cell.text } }
                        is PdfBlock -> "[PDF: ${block.fileName}]"
                        is NoteBlock.ImageBlock -> "[Imagen]"
                        is NoteBlock.Image -> "[Imagen: ${block.fileName}]"
                        is AudioBlock -> "[Audio]"
                        is NoteBlock.Math -> "[Fórmula Math: ${block.latexCode}]"
                    }
                }.filter { it.isNotBlank() }
                lines.joinToString("\n")
            }.getOrDefault(rawContent)
        } else {
            rawContent
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = if (originalTime > 0L) timeFormat.format(Date(originalTime)) else ""
        val scheduledTimeLine = if (formattedTime.isNotBlank()) "Hora programada: $formattedTime hs" else ""

        val bodyText = when {
            scheduledTimeLine.isNotBlank() && excerpt.isNotBlank() -> "$scheduledTimeLine\n$excerpt"
            scheduledTimeLine.isNotBlank() -> scheduledTimeLine
            excerpt.isNotBlank() -> excerpt
            else -> "Tienes un recordatorio pendiente en tu calendario."
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "note_reminders_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios y Calendario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordatorios y eventos de calendario"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (noteId > 0L) {
                putExtra("EXTRA_OPEN_NOTE_ID", noteId)
            }
            if (reminderId > 0L) {
                putExtra("EXTRA_OPEN_REMINDER_ID", reminderId)
            }
        }

        val requestCode = if (reminderId > 0L) {
            ReminderScheduler.getStandaloneReminderRequestCode(reminderId, offsetMinutes)
        } else {
            ReminderScheduler.getReminderRequestCode(noteId, offsetMinutes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notificationTitle)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                if (itemType.equals("EVENT", ignoreCase = true)) NotificationCompat.CATEGORY_EVENT
                else NotificationCompat.CATEGORY_REMINDER
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(requestCode, notification)
    }
}
