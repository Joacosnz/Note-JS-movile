package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.model.PdfBlock
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NoteBlockSerializer
import com.example.util.cleanMarkdownSyntax
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente principal de cuadrícula escalonada (Staggered Grid) estilo Google Keep.
 * Organiza las notas en secciones "FIJADAS" y "OTRAS", adaptando el contenido
 * de manera dinámica según checklists, imágenes, grabaciones de voz y paleta de colores.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeepNotesGrid(
    notes: List<NoteEntity>,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
    unlockedNoteIds: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    selectedNoteIds: Set<Long> = emptySet(),
    onSelectToggle: (Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp)
) {
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val otherNotes = remember(notes) { notes.filter { !it.isPinned } }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier
            .fillMaxSize()
            .testTag("keep_notes_grid")
    ) {
        // 1. Sección de notas fijadas
        if (pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                KeepSectionHeader(title = "FIJADAS")
            }
            items(pinnedNotes, key = { it.id }) { note ->
                KeepNoteCard(
                    note = note,
                    isUnlocked = unlockedNoteIds.contains(note.id),
                    onClick = { onNoteClick(note) },
                    onLongClick = { onNoteLongClick(note) },
                    onTogglePin = { onTogglePin(note) },
                    isSelectionMode = isSelectionMode,
                    isSelected = selectedNoteIds.contains(note.id),
                    onSelectToggle = { onSelectToggle(note.id) },
                    modifier = Modifier
                        .animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        .fillMaxWidth()
                )
            }

            if (otherNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    KeepSectionHeader(title = "OTRAS")
                }
            }
        }

        // 2. Sección del resto de notas
        items(otherNotes, key = { it.id }) { note ->
            KeepNoteCard(
                note = note,
                isUnlocked = unlockedNoteIds.contains(note.id),
                onClick = { onNoteClick(note) },
                onLongClick = { onNoteLongClick(note) },
                onTogglePin = { onTogglePin(note) },
                isSelectionMode = isSelectionMode,
                isSelected = selectedNoteIds.contains(note.id),
                onSelectToggle = { onSelectToggle(note.id) },
                modifier = Modifier
                    .animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * Título de sección estilo Google Keep (pequeño, mayúsculas, espaciado elegante).
 */
@Composable
fun KeepSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = modifier.padding(start = 6.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Tarjeta individual estilo Google Keep con soporte de color de fondo,
 * checklists visuales, chips de recordatorio y etiquetas.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KeepNoteCard(
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

    // Determinación de color de fondo estilo Google Keep
    val backgroundColor = if (isLockedNote) {
        if (isDark) Color(0xFF1E1F22) else Color(0xFFF1F3F4)
    } else {
        NoteColors.getColor(note.colorIndex, isDark)
    }

    val borderColor = if (isLockedNote) {
        if (isDark) Color(0xFF383A40) else Color(0xFFE0E0E0)
    } else {
        NoteColors.getBorderColor(note.colorIndex, isDark)
    }

    val titleColor = NoteColors.getContentColor(note.colorIndex, isDark)
    val bodyColor = NoteColors.getSecondaryTextColor(note.colorIndex, isDark)

    // Deserializar bloques si contiene JSON
    val blocks = remember(note.content) {
        if (note.content.trim().startsWith("[")) {
            try {
                NoteBlockSerializer.deserialize(note.content)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val checklistItems = remember(blocks) {
        blocks.filterIsInstance<NoteBlock.Checklist>()
    }

    val hasAudio = remember(blocks) { blocks.any { it is AudioBlock } }
    val hasPdf = remember(blocks) { blocks.any { it is PdfBlock } || note.pdfExtractedText != null }
    val hasDrawing = remember(blocks) { blocks.any { it is NoteBlock.Drawing } }
    val hasWebLinks = remember(blocks) { blocks.any { it is NoteBlock.Text && (it.content.contains("http://") || it.content.contains("https://")) } }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onSelectToggle() else onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isSelectionMode) onSelectToggle() else onLongClick()
                }
            )
            .testTag("keep_note_card_${note.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, if (isSelected) amber else borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Cabecera: Título y botón de Fijar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 19.sp
                            ),
                            color = titleColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                    } else if (isLockedNote) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nota protegida",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                color = titleColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Botón de Fijar o Selector de Selección
                    if (isSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = if (isSelected) "Seleccionada" else "No seleccionada",
                            tint = if (isSelected) amber else bodyColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (!isLockedNote) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("btn_pin_note_${note.id}")
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (note.isPinned) "Desfijar" else "Fijar",
                                tint = if (note.isPinned) amber else bodyColor.copy(alpha = 0.45f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Contenido
                if (isLockedNote) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toca para desbloquear con PIN o huella",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = bodyColor.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                } else if (checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        checklistItems.take(5).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (item.isChecked) amber else bodyColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.content.ifBlank { "Elemento de lista" },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                    ),
                                    color = if (item.isChecked) bodyColor.copy(alpha = 0.5f) else titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (checklistItems.size > 5) {
                            Text(
                                text = "+ ${checklistItems.size - 5} elementos más...",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = bodyColor.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                            )
                        }
                    }
                } else {
                    val rawSnippet = remember(note.content, blocks) {
                        if (blocks.isNotEmpty()) {
                            blocks.filterIsInstance<NoteBlock.Text>()
                                .joinToString("\n") { it.content }
                                .take(250)
                        } else {
                            cleanMarkdownSyntax(note.content).take(250)
                        }
                    }

                    if (rawSnippet.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rawSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 16.5.sp
                            ),
                            color = bodyColor,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Fila inferior de Badges e Indicadores (Google Keep style pills)
                val hasBadges = hasAudio || hasPdf || hasDrawing || hasWebLinks || note.reminderTimestamp != null || note.tags.isNotBlank() || note.folder.isNotBlank()
                if (hasBadges && !isLockedNote) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge de Recordatorio
                        if (note.reminderTimestamp != null) {
                            val timeFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
                            val formattedDate = timeFormat.format(Date(note.reminderTimestamp))
                            KeepBadgeChip(
                                icon = Icons.Default.Notifications,
                                label = formattedDate,
                                tint = amber,
                                isDark = isDark
                            )
                        }

                        // Badge de Carpeta
                        if (note.folder.isNotBlank()) {
                            KeepBadgeChip(
                                icon = null,
                                label = note.folder,
                                tint = MaterialTheme.colorScheme.primary,
                                isDark = isDark
                            )
                        }

                        // Badges de Etiquetas (#tag)
                        if (note.tags.isNotBlank()) {
                            note.tags.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .take(2)
                                .forEach { tag ->
                                    KeepBadgeChip(
                                        icon = null,
                                        label = if (tag.startsWith("#")) tag else "#$tag",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        isDark = isDark
                                    )
                                }
                        }

                        // Badges de multimedia
                        if (hasAudio) {
                            KeepMiniIconBadge(icon = Icons.Rounded.Mic, tint = amber, isDark = isDark)
                        }
                        if (hasPdf) {
                            KeepMiniIconBadge(icon = Icons.Rounded.PictureAsPdf, tint = Color(0xFFE53935), isDark = isDark)
                        }
                        if (hasDrawing) {
                            KeepMiniIconBadge(icon = Icons.Rounded.Brush, tint = Color(0xFF8E24AA), isDark = isDark)
                        }
                        if (hasWebLinks) {
                            KeepMiniIconBadge(icon = Icons.Rounded.Link, tint = Color(0xFF1E88E5), isDark = isDark)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chip para metadatos de notas estilo Google Keep (redondeado, compacto).
 */
@Composable
fun KeepBadgeChip(
    icon: ImageVector?,
    label: String,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0x33000000) else Color(0x1F000000),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isDark) Color(0xFFE3E3E3) else Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Indicador icono compacto para adjuntos multimedia.
 */
@Composable
fun KeepMiniIconBadge(
    icon: ImageVector,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = if (isDark) Color(0x33000000) else Color(0x1A000000),
        modifier = modifier.size(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
