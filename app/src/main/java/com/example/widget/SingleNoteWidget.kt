package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
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

val EXTRA_CONFIG_APPWIDGET_ID_KEY = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)

class SingleNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val note = try {
            withContext(Dispatchers.IO) {
                val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
                val selectedNoteId = WidgetPrefManager.getSelectedNoteId(context, appWidgetId)

                val dao = AppDatabase.getDatabase(context).noteDao()
                if (selectedNoteId > 0L) {
                    dao.getNoteById(selectedNoteId) ?: dao.getLatestActiveNote()
                } else {
                    dao.getLatestActiveNote()
                }
            }
        } catch (_: Exception) {
            null
        }

        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            AppWidgetManager.INVALID_APPWIDGET_ID
        }

        provideContent {
            GlanceTheme {
                SingleNoteContent(
                    context = context,
                    appWidgetId = appWidgetId,
                    note = note
                )
            }
        }
    }
}

class SingleNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleNoteWidget()
}

@Composable
private fun SingleNoteContent(
    context: Context,
    appWidgetId: Int,
    note: NoteEntity?
) {
    val configComponentName = ComponentName(context, SingleNoteWidgetConfigActivity::class.java)
    val configAction = actionStartActivity(
        configComponentName,
        actionParametersOf(EXTRA_CONFIG_APPWIDGET_ID_KEY to appWidgetId)
    )

    if (note == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No hay nota seleccionada",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Button(
                    text = "📌 Seleccionar Nota",
                    onClick = configAction
                )
            }
        }
        return
    }

    val glanceContext = LocalContext.current
    val isDark = (glanceContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val theme = NoteColors.options.firstOrNull { it.id == note.colorIndex } ?: NoteColors.options[0]
    val cardBgColor = if (note.colorIndex == 0) null else if (isDark) theme.darkBg else theme.lightBg

    val containerModifier = if (cardBgColor != null) {
        GlanceModifier
            .fillMaxSize()
            .background(cardBgColor)
            .padding(12.dp)
    } else {
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
    }

    val blocks = NoteBlockSerializer.deserialize(note.content)
    val displayTitle = note.title.ifBlank { "Sin título" }

    Column(
        modifier = containerModifier
    ) {
        // Header with Title & Button to switch note
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(EXTRA_OPEN_NOTE_ID_KEY to note.id)
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📌 $displayTitle",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
            }

            Button(
                text = "⚙️ Cambiar",
                onClick = configAction
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Note Blocks
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            items(items = blocks, itemId = { block -> block.id.hashCode().toLong() }) { block ->
                when (block) {
                    is NoteBlock.Text -> {
                        if (block.content.isNotBlank()) {
                            Text(
                                text = block.content,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = GlanceTheme.colors.onSurface
                                ),
                                modifier = GlanceModifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    is NoteBlock.Checklist -> {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckBox(
                                checked = block.isChecked,
                                onCheckedChange = actionRunCallback<ToggleChecklistAction>(
                                    actionParametersOf(
                                        ToggleChecklistAction.NOTE_ID_KEY to note.id,
                                        ToggleChecklistAction.BLOCK_ID_KEY to block.id
                                    )
                                ),
                                text = block.content
                            )
                        }
                    }

                    is NoteBlock.SimpleList -> {
                        if (block.content.isNotBlank()) {
                            Text(
                                text = "• ${block.content}",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = GlanceTheme.colors.onSurface
                                ),
                                modifier = GlanceModifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }
                    }

                    is NoteBlock.CalloutBox -> {
                        if (block.content.isNotBlank()) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(GlanceTheme.colors.surfaceVariant)
                                    .cornerRadius(6.dp)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = block.content,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
