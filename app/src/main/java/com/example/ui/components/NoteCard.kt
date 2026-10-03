package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.PaperStyle
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.ui.theme.AppleCardBgDark
import com.example.ui.theme.AppleCardBgLight
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.ui.theme.AppleSeparatorDark
import com.example.ui.theme.AppleSeparatorLight
import com.example.util.NoteBlockSerializer
import com.example.util.sanitizeNoteSnippet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val haptics = LocalHapticFeedback.current

    val isCustomColor = note.colorIndex != 0
    val cardBg = if (isCustomColor) {
        NoteColors.getColor(note.colorIndex, isDark)
    } else {
        if (isDark) AppleCardBgDark else AppleCardBgLight
    }
    val cardBorder = if (isCustomColor) {
        NoteColors.getBorderColor(note.colorIndex, isDark)
    } else {
        if (isDark) AppleSeparatorDark else AppleSeparatorLight
    }

    val paperStyle = runCatching { PaperStyle.valueOf(note.paperStyle) }.getOrDefault(PaperStyle.BLANK)
    val isLockedNote = note.isLocked && !isUnlocked

    val blocks = remember(note.content) { NoteBlockSerializer.deserialize(note.content) }
    val checklistBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Checklist>() }
    val completedCount = checklistBlocks.count { it.isChecked }
    val totalChecklist = checklistBlocks.size
    val hasDrawing = remember(blocks) { blocks.any { it is NoteBlock.Drawing } || note.drawingData.isNotBlank() }
    val hasCallout = remember(blocks) { blocks.any { it is NoteBlock.CalloutBox } }

    val snippetText = remember(blocks) {
        val raw = blocks.filter { it !is NoteBlock.Drawing }.joinToString(" ") { block ->
            when (block) {
                is NoteBlock.Text -> block.content
                is NoteBlock.Checklist -> block.content
                is NoteBlock.SimpleList -> block.content
                is NoteBlock.CalloutBox -> block.content
                is NoteBlock.Drawing -> ""
                is NoteBlock.Table -> block.matrix.joinToString(" ") { row -> row.joinToString(" ") { it.text } }
                is PdfBlock -> "[PDF: ${block.fileName}]"
                is NoteBlock.ImageBlock -> "[Imagen]"
                is NoteBlock.Image -> "[Imagen: ${block.fileName}]"
                is AudioBlock -> "[Audio: ${String.format(Locale.getDefault(), "%02d:%02d", block.durationMs / 60000, (block.durationMs % 60000) / 1000)}]"
            }
        }
        sanitizeNoteSnippet(raw)
    }

    val dateFormatted = remember(note.updatedAt) {
        val now = System.currentTimeMillis()
        val diff = now - note.updatedAt
        val sdf = if (diff < 24 * 60 * 60 * 1000) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault())
        }
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
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
                    if (isSelectionMode) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectToggle()
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                }
            )
            .testTag("apple_card_${note.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.6.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            PaperBackground(
                paperStyle = paperStyle,
                isDark = isDark,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelectionMode) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = note.title.ifBlank { "Nota sin título" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasDrawing) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        if (hasCallout) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        if (note.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 4.dp)
                            )
                        }

                        if (note.isPinned && !isSelectionMode) {
                            IconButton(
                                onClick = onTogglePin,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Nota fijada",
                                    tint = amber,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (isLockedNote) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Nota protegida con código",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))

                    if (totalChecklist > 0) {
                        Text(
                            text = "☑️ $completedCount/$totalChecklist tareas completadas",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = amber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        checklistBlocks.take(3).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (item.isChecked) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = sanitizeNoteSnippet(item.content),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (snippetText.isNotBlank()) {
                        Text(
                            text = snippetText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (note.tags.isNotBlank()) {
                        Text(
                            text = "#${note.tags.split(",").firstOrNull()?.trim() ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = amber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
