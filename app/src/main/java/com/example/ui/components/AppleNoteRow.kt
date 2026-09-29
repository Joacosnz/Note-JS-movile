package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteColors
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.util.sanitizeNoteSnippet
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppleNoteRow(
    note: NoteEntity,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val isLockedNote = note.isLocked && !isUnlocked

    val rowBg = if (note.colorIndex != 0) {
        NoteColors.getColor(note.colorIndex, isDark).copy(alpha = if (isDark) 0.25f else 0.4f)
    } else {
        Color.Transparent
    }

    val formattedDate = remember(note.updatedAt) {
        val now = System.currentTimeMillis()
        val diff = now - note.updatedAt
        when {
            diff < (24 * 60 * 60 * 1000) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            diff < (48 * 60 * 60 * 1000) -> "ayer"
            diff < (7 * 24 * 60 * 60 * 1000) -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(note.updatedAt))
            else -> SimpleDateFormat("d/M/yy", Locale.getDefault()).format(Date(note.updatedAt))
        }
    }

    val snippetText = remember(note.content, note.checklistData, note.noteType) {
        val raw = when {
            isLockedNote -> "Nota bloqueada"
            note.noteType == "CHECKLIST" && note.checklistData.isNotBlank() -> {
                runCatching {
                    val arr = JSONArray(note.checklistData)
                    val items = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        items.add(o.getString("text"))
                    }
                    items.joinToString(", ")
                }.getOrDefault("Lista de tareas")
            }
            note.noteType == "DRAWING" -> "Dibujo / Boceto a mano alzada"
            note.noteType == "MATH" -> note.content.ifBlank { "Math Note con cálculo reactivo" }
            else -> note.content
        }
        sanitizeNoteSnippet(raw)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable(onClick = onClick)
            .testTag("apple_note_row_${note.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.title.ifBlank { "Nota sin título" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Nota fijada",
                            tint = amber,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (note.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Nota protegida",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Date + snippet line (Iconic Apple Notes structure)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = snippetText.ifBlank { "Sin texto adicional" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Apple folder / tags label
                val tagList = note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (tagList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tagList.take(2).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = amber
                            )
                        }
                    }
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
