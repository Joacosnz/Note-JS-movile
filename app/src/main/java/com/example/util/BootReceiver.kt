package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.model.NoteBlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val notes = db.noteDao().getAllNotesList()
                    val now = System.currentTimeMillis()

                    for (note in notes) {
                        if (note.isLocked) continue

                        // 1. Recordatorio a nivel de Nota
                        if (note.reminderTimestamp != null && !note.isReminderDone && note.reminderTimestamp > now) {
                            ReminderScheduler.scheduleReminder(
                                context = context,
                                noteId = note.id,
                                timestamp = note.reminderTimestamp,
                                title = note.title.ifBlank { "Recordatorio" },
                                content = note.content
                            )
                        }

                        // 2. Recordatorios de checklist blocks
                        if (note.content.isNotBlank() && note.content.trim().startsWith("[")) {
                            try {
                                val blocks = NoteBlockSerializer.deserialize(note.content)
                                for (block in blocks) {
                                    if (block is NoteBlock.Checklist &&
                                        block.reminderTimestamp != null &&
                                        !block.isChecked &&
                                        !block.isReminderDone &&
                                        block.reminderTimestamp!! > now
                                    ) {
                                        ReminderScheduler.scheduleItemReminder(
                                            context = context,
                                            noteId = note.id,
                                            itemId = block.id,
                                            timestamp = block.reminderTimestamp!!,
                                            title = note.title.ifBlank { "Recordatorio de lista" },
                                            content = block.content.ifBlank { "Elemento de lista" }
                                        )
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
