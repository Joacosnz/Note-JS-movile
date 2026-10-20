package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.PaperStyle
import com.example.model.PdfBlock
import com.example.ui.theme.NoteJsCardBgDark
import com.example.ui.theme.NoteJsCardBgLight
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.NoteJsSeparatorDark
import com.example.ui.theme.NoteJsSeparatorLight
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NoteBlockSerializer
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
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current

    val isLockedNote = note.isLocked && !isUnlocked

    val isCustomColor = note.colorIndex != 0
    val cardBg = if (isLockedNote) {
        if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    } else if (isCustomColor) {
        NoteColors.getColor(note.colorIndex, isDark)
    } else {
        if (isDark) NoteJsCardBgDark else NoteJsCardBgLight
    }
    val cardBorder = if (isLockedNote) {
        amber.copy(alpha = 0.3f)
    } else if (isCustomColor) {
        NoteColors.getBorderColor(note.colorIndex, isDark)
    } else {
        if (isDark) NoteJsSeparatorDark else NoteJsSeparatorLight
    }

    val paperStyle = runCatching { PaperStyle.valueOf(note.paperStyle) }.getOrDefault(PaperStyle.BLANK)

    val syncIcon = if (note.syncEnabled) Icons.Rounded.Cloud else Icons.Rounded.CloudOff
    val syncTint = if (note.syncEnabled) Color(0xFFFFD54F) else Color.Gray.copy(alpha = 0.5f)

    val blocks = remember(note.content) { NoteBlockSerializer.deserialize(note.content) }
    val previewBlocks = remember(blocks) {
        blocks.filter { block ->
            when (block) {
                is NoteBlock.Text -> block.content.isNotBlank()
                is NoteBlock.Checklist -> block.content.isNotBlank()
                is NoteBlock.SimpleList -> block.content.isNotBlank()
                is NoteBlock.CalloutBox -> block.content.isNotBlank()
                else -> true
            }
        }.take(2)
    }

    val hasDrawing = remember(blocks) { blocks.any { it is NoteBlock.Drawing } || note.drawingData.isNotBlank() }
    val hasCallout = remember(blocks) { blocks.any { it is NoteBlock.CalloutBox } }

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
            .testTag("notejs_card_${note.id}"),
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
                        val displayTitle = if (isLockedNote) "Nota Protegida" else note.title.ifBlank { "Nota sin título" }
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasDrawing && !isLockedNote) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        if (hasCallout && !isLockedNote) {
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
                                contentDescription = "Nota Protegida",
                                tint = amber,
                                modifier = Modifier
                                    .size(18.dp)
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

                        Icon(
                            imageVector = syncIcon,
                            contentDescription = if (note.syncEnabled) "Sincronización activada" else "Sincronización desactivada",
                            tint = syncTint,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp)
                        )
                    }
                }

                if (isLockedNote) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Nota Protegida",
                            tint = amber,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Toca para ingresar el PIN",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (previewBlocks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        previewBlocks.forEach { block ->
                            CompactNoteBlockItem(
                                block = block,
                                amber = amber
                            )
                        }
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

                    if (note.tags.isNotBlank() && !isLockedNote) {
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

@Composable
private fun CompactNoteBlockItem(
    block: NoteBlock,
    amber: Color,
    modifier: Modifier = Modifier
) {
    when (block) {
        is NoteBlock.Table -> {
            val firstCellText = block.matrix.firstOrNull()?.firstOrNull()?.text?.trim().orEmpty()
            val label = if (firstCellText.isNotBlank()) {
                "Tabla (${block.rows}x${block.cols}) • $firstCellText"
            } else {
                "Tabla (${block.rows}x${block.cols})"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        is NoteBlock.Checklist -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (block.isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (block.isChecked) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = block.content.ifBlank { "Tarea sin texto" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (block.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        is NoteBlock.Text -> {
            if (block.content.isNotBlank()) {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }
        }

        is NoteBlock.CalloutBox -> {
            if (block.content.isNotBlank()) {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }
        }

        is NoteBlock.SimpleList -> {
            if (block.content.isNotBlank()) {
                val prefix = if (block.isNumbered) "${block.number}. " else "• "
                Text(
                    text = "$prefix${block.content}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = modifier
                )
            }
        }

        is PdfBlock -> {
            CompactMediaItem(
                icon = Icons.Rounded.PictureAsPdf,
                fileName = block.fileName.ifBlank { "Documento PDF" },
                modifier = modifier
            )
        }

        is AudioBlock -> {
            val durationText = if (block.durationMs > 0) {
                String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    block.durationMs / 60000,
                    (block.durationMs % 60000) / 1000
                )
            } else ""
            val defaultLabel = if (durationText.isNotBlank()) "Nota de voz ($durationText)" else "Nota de voz"
            val name = block.filePath.substringAfterLast("/").ifBlank { defaultLabel }
            CompactMediaItem(
                icon = Icons.Rounded.Mic,
                fileName = name,
                modifier = modifier
            )
        }

        is NoteBlock.ImageBlock -> {
            val name = block.localUri.substringAfterLast("/").ifBlank { "Imagen" }
            CompactMediaItem(
                icon = Icons.Rounded.Image,
                fileName = name,
                modifier = modifier
            )
        }

        is NoteBlock.Image -> {
            val name = block.fileName.ifBlank { block.filePath.substringAfterLast("/") }.ifBlank { "Imagen" }
            CompactMediaItem(
                icon = Icons.Rounded.Image,
                fileName = name,
                modifier = modifier
            )
        }

        is NoteBlock.Drawing -> {
            CompactMediaItem(
                icon = Icons.Rounded.Brush,
                fileName = "Dibujo / Boceto",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun CompactMediaItem(
    icon: ImageVector,
    fileName: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
