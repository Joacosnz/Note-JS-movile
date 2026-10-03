package com.example.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseStorageManager
import com.example.data.remote.FirestoreManager
import com.example.data.repository.NoteRepository
import com.example.model.NoteBlock
import com.example.util.NoteBlockSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToggleChecklistAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NOTE_ID_KEY] ?: return
        val blockId = parameters[BLOCK_ID_KEY] ?: return

        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val repository = NoteRepository(
                noteDao = database.noteDao(),
                folderDao = database.folderDao(),
                signatureDao = database.signatureDao(),
                firestoreManager = FirestoreManager(),
                firebaseStorageManager = FirebaseStorageManager(),
                context = context.applicationContext
            )

            val note = repository.getNoteById(noteId) ?: return@withContext

            val blocks = NoteBlockSerializer.deserialize(note.content).map { block ->
                if (block is NoteBlock.Checklist && block.id == blockId) {
                    block.copy(isChecked = !block.isChecked)
                } else {
                    block
                }
            }

            val updatedNote = note.copy(
                content = NoteBlockSerializer.serialize(blocks),
                updatedAt = System.currentTimeMillis()
            )

            repository.update(updatedNote)
        }

        // Update active Glance widgets
        SingleNoteWidget().updateAll(context)
        NotesListWidget().updateAll(context)
    }

    companion object {
        val NOTE_ID_KEY = ActionParameters.Key<Long>("noteId")
        val BLOCK_ID_KEY = ActionParameters.Key<String>("blockId")
    }
}
