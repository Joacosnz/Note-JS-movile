package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.example.ui.theme.isAppInDarkTheme
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.util.NoteBlockSerializer
import com.example.util.sanitizeNoteSnippet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteJsNoteRow(
    note: NoteEntity,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current
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

    val blocks = remember(note.content) { NoteBlockSerializer.deserialize(note.content) }
    val checklistBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Checklist>() }
    val completedCount = checklistBlocks.count { isChecked -> isChecked.isChecked }
    val totalChecklist = checklistBlocks.size

    val snippetText = remember(blocks, isLockedNote) {
        val raw = if (isLockedNote) {
            "Toca para ingresar PIN"
        } else if (totalChecklist > 0) {
            "☑️ $completedCount/$totalChecklist tareas completadas"
        } else {
            blocks.filter { it !is NoteBlock.Drawing }.joinToString(" ") { block ->
                 when (block) {
                    is NoteBlock.Text -> block.content
                    is NoteBlock.Checklist -> block.content
                    is NoteBlock.SimpleList -> block.content
                    is NoteBlock.CalloutBox -> block.content
                    is NoteBlock.Drawing -> "[Boceto]"
                    is NoteBlock.Table -> block.matrix.joinToString(" ") { row -> row.joinToString(" ") { it.text } }
                    is PdfBlock -> "[PDF: ${block.fileName}]"
                    is NoteBlock.ImageBlock -> "[Imagen]"
                    is NoteBlock.Image -> "[Imagen: ${block.fileName}]"
                    is AudioBlock -> "[Audio: ${String.format(Locale.getDefault(), "%02d:%02d", block.durationMs / 60000, (block.durationMs % 60000) / 1000)}]"
                    is NoteBlock.Math -> "[Fórmula Math: ${block.latexCode}]"
                }
            }
        }
        sanitizeNoteSnippet(raw)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectToggle()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                }
            )
            .testTag("notejs_note_row_${note.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 12.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayTitle = if (isLockedNote) "Nota Protegida" else note.title.ifBlank { "Nota sin título" }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (note.reminderTimestamp != null) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Nota Protegida",
                                tint = amber,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = snippetText.ifBlank { "Sin contenido adicional" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
