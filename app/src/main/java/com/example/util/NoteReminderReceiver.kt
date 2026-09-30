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

class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra("EXTRA_NOTE_ID", 0L)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Recordatorio de Nota"
        val rawContent = intent.getStringExtra("EXTRA_CONTENT") ?: "Tienes un recordatorio pendiente."

        val displayContent = if (rawContent.trim().startsWith("[")) {
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
                        is AudioBlock -> "[Audio]"
                    }
                }.filter { it.isNotBlank() }
                val text = lines.joinToString("\n")
                if (text.isBlank()) "Tienes un recordatorio pendiente." else text
            }.getOrDefault(rawContent)
        } else {
            rawContent
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

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "Nota sin título" })
            .setContentText(displayContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }
}
