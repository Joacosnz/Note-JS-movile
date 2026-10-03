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
        val rawTitle = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: ""
        val rawContent = intent.getStringExtra(ReminderScheduler.EXTRA_CONTENT) ?: ""
        val offsetMinutes = intent.getIntExtra(ReminderScheduler.EXTRA_OFFSET, 0)
        val originalTime = intent.getLongExtra(ReminderScheduler.EXTRA_ORIGINAL_TIME, 0L)

        // Title formatting: if offset > 0 -> "En $offset min: $title", else original title
        val baseTitle = if (rawTitle.isNotBlank()) rawTitle else "Nota sin título"
        val notificationTitle = if (offsetMinutes > 0) {
            "En $offsetMinutes min: $baseTitle"
        } else {
            baseTitle
        }

        // Extract excerpt if JSON content
        val excerpt = if (rawContent.trim().startsWith("[")) {
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
                    }
                }.filter { it.isNotBlank() }
                lines.joinToString("\n")
            }.getOrDefault(rawContent)
        } else {
            rawContent
        }

        // Body text formatting: "Hora programada: HH:mm hs" + excerpt if available
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = if (originalTime > 0L) timeFormat.format(Date(originalTime)) else ""
        val scheduledTimeLine = if (formattedTime.isNotBlank()) "Hora programada: $formattedTime hs" else ""

        val bodyText = when {
            scheduledTimeLine.isNotBlank() && excerpt.isNotBlank() -> "$scheduledTimeLine\n$excerpt"
            scheduledTimeLine.isNotBlank() -> scheduledTimeLine
            excerpt.isNotBlank() -> excerpt
            else -> "Tienes un recordatorio pendiente."
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "note_reminders_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Notas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordatorios de notas programados"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_NOTE_ID", noteId)
        }

        val requestCode = (noteId.toInt() * 100) + offsetMinutes

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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (noteId.toInt() * 100) + offsetMinutes
        notificationManager.notify(notificationId, notification)
    }
}
