package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.DragHandle
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
    isLastItem: Boolean = false,
    showDivider: Boolean = !isLastItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isDragging: Boolean = false,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current
    val isLockedNote = note.isLocked && !isUnlocked

    val isCustomColor = note.colorIndex != 0
    val rowBg = if (isDragging) {
        if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    } else if (isCustomColor) {
        NoteColors.getColor(note.colorIndex, isDark)
    } else {
        Color.Transparent
    }

    val titleColor = if (isCustomColor) {
        NoteColors.getContentColor(note.colorIndex, isDark)
    } else {
        Color.White
    }

    val snippetColor = if (isCustomColor) {
        NoteColors.getSecondaryTextColor(note.colorIndex, isDark)
    } else {
        Color.Gray
    }

    val timeColor = if (isCustomColor) {
        NoteColors.getSecondaryTextColor(note.colorIndex, isDark)
    } else {
        Color.DarkGray
    }

    val iconPrefix = if (!note.icon.isNullOrBlank()) "${note.icon} " else ""
    val displayTitle = if (isLockedNote) "Nota Protegida" else if (note.title.isBlank()) "${iconPrefix}Nota sin título" else "$iconPrefix${note.title}"

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
                Text(
                    text = displayTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = note.contentSnippet,
                    fontSize = 14.sp,
                    color = snippetColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (note.reminderTimestamp != null) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (note.isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Nota Protegida",
                        tint = amber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = note.time,
                    fontSize = 12.sp,
                    color = timeColor
                )

                if (showDragHandle && !isSelectionMode) {
                    Box(
                        modifier = dragHandleModifier
                            .size(28.dp)
                            .testTag("drag_handle_${note.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Arrastrar para reordenar",
                            tint = if (isDragging) amber else Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (!isLastItem && showDivider && !isDragging) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = Color.DarkGray.copy(alpha = 0.3f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: NoteEntity,
    modifier: Modifier = Modifier,
    isLastItem: Boolean = false,
    isUnlocked: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    showDivider: Boolean = !isLastItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    isDragging: Boolean = false,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    NoteJsNoteRow(
        note = note,
        isUnlocked = isUnlocked,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        isLastItem = isLastItem,
        showDivider = showDivider,
        isSelectionMode = isSelectionMode,
        isSelected = isSelected,
        onSelectToggle = onSelectToggle,
        isDragging = isDragging,
        showDragHandle = showDragHandle,
        dragHandleModifier = dragHandleModifier
    )
}

val NoteEntity.contentSnippet: String
    get() {
        if (isLocked) return "Toca para ingresar PIN"
        val blocks = NoteBlockSerializer.deserialize(content)
        val checklistBlocks = blocks.filterIsInstance<NoteBlock.Checklist>()
        if (checklistBlocks.isNotEmpty()) {
            val completedCount = checklistBlocks.count { it.isChecked }
            return "☑️ $completedCount/${checklistBlocks.size} tareas completadas"
        }
        val raw = blocks.filter { it !is NoteBlock.Drawing }.joinToString(" ") { block ->
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
                is NoteBlock.Math -> "∑ ${block.latexCode.ifBlank { "Fórmula" }}"
            }
        }
        return sanitizeNoteSnippet(raw).ifBlank { "Sin contenido adicional" }
    }

val NoteEntity.time: String
    get() {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt
        return when {
            diff < (24 * 60 * 60 * 1000) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updatedAt))
            diff < (48 * 60 * 60 * 1000) -> "ayer"
            diff < (7 * 24 * 60 * 60 * 1000) -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(updatedAt))
            else -> SimpleDateFormat("d/M/yy", Locale.getDefault()).format(Date(updatedAt))
        }
    }

