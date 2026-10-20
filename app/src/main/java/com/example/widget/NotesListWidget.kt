package com.example.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.util.NoteBlockSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val EXTRA_OPEN_NOTE_ID_KEY = ActionParameters.Key<Long>("EXTRA_OPEN_NOTE_ID")
val EXTRA_CREATE_NEW_NOTE_KEY = ActionParameters.Key<Boolean>("EXTRA_CREATE_NEW_NOTE")

class NotesListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = try {
            withContext(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(context).noteDao()
                dao.getActiveNotesList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                NotesListContent(notes = notes)
            }
        }
    }
}

class NotesListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesListWidget()
}

@Composable
private fun NotesListContent(notes: List<NoteEntity>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Note JS",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            Button(
                text = "+ Crear Nota",
                onClick = actionStartActivity<MainActivity>(
                    actionParametersOf(EXTRA_CREATE_NEW_NOTE_KEY to true)
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        if (notes.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay notas disponibles",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(items = notes, itemId = { note -> note.id }) { note ->
                    NoteRowItem(note = note)
                }
            }
        }
    }
}

@Composable
private fun NoteRowItem(note: NoteEntity) {
    val displayTitle = note.title.ifBlank { "Sin título" }
    val previewText = getNotePreviewText(note.content)

    val context = LocalContext.current
    val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val theme = NoteColors.options.firstOrNull { it.id == note.colorIndex } ?: NoteColors.options[0]
    val cardBgColor = if (note.colorIndex == 0) null else if (isDark) theme.darkBg else theme.lightBg

    val cardModifier = if (cardBgColor != null) {
        GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .cornerRadius(10.dp)
            .background(cardBgColor)
    } else {
        GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .cornerRadius(10.dp)
            .background(GlanceTheme.colors.surfaceVariant)
    }

    Column(
        modifier = cardModifier
            .clickable(
                actionStartActivity<MainActivity>(
                    actionParametersOf(EXTRA_OPEN_NOTE_ID_KEY to note.id)
                )
            )
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.isPinned) {
                Text(
                    text = "📌 ",
                    style = TextStyle(fontSize = 12.sp)
                )
            }
            Text(
                text = displayTitle,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurface
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        Text(
            text = previewText,
            style = TextStyle(
                fontSize = 12.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            maxLines = 2
        )
    }
}

private fun getNotePreviewText(contentJson: String): String {
    val blocks = NoteBlockSerializer.deserialize(contentJson)
    val textParts = mutableListOf<String>()

    for (block in blocks) {
        when (block) {
            is NoteBlock.Text -> if (block.content.isNotBlank()) textParts.add(block.content)
            is NoteBlock.Checklist -> if (block.content.isNotBlank()) {
                val prefix = if (block.isChecked) "☑ " else "☐ "
                textParts.add(prefix + block.content)
            }
            is NoteBlock.SimpleList -> if (block.content.isNotBlank()) textParts.add("• " + block.content)
            is NoteBlock.CalloutBox -> if (block.content.isNotBlank()) textParts.add(block.content)
            else -> {}
        }
    }

    return if (textParts.isEmpty()) "Nota sin contenido de texto" else textParts.take(3).joinToString(" • ")
}
