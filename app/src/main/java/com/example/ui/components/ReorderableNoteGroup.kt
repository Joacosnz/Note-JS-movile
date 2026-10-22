package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.entity.NoteEntity
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme

/**
 * Contenedor de lista agrupada con soporte interactivo de reordenación arrastrar y soltar (Drag and Drop).
 * Permite a los usuarios priorizar sus notas de forma manual con respuesta háptica y animaciones fluidas.
 */
@Composable
fun ReorderableGroupedNotesCard(
    notes: List<NoteEntity>,
    unlockedNoteIds: Set<Long>,
    selectedNoteIds: Set<Long>,
    isSelectionMode: Boolean,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    onSelectToggle: (Long) -> Unit,
    onReorder: (List<NoteEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current

    val currentNotes = remember(notes) { mutableStateListOf<NoteEntity>().apply { addAll(notes) } }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(170f) }

    LaunchedEffect(notes) {
        if (draggedIndex == null) {
            currentNotes.clear()
            currentNotes.addAll(notes)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1C1C1E),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            currentNotes.forEachIndexed { index, note ->
                val isBeingDragged = draggedIndex == index
                val itemZIndex = if (isBeingDragged) 10f else 1f
                val translationY = if (isBeingDragged) dragOffsetY else 0f
                val scale = if (isBeingDragged) 1.02f else 1f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(itemZIndex)
                        .graphicsLayer {
                            this.translationY = translationY
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                        .onGloballyPositioned { coordinates ->
                            if (coordinates.size.height > 0) {
                                itemHeightPx = coordinates.size.height.toFloat()
                            }
                        }
                ) {
                    val rowModifier = if (isBeingDragged) {
                        Modifier
                            .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black, spotColor = amber)
                            .clip(RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }

                    val dragModifier = if (!isSelectionMode) {
                        Modifier.pointerInput(note.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedIndex = index
                                    dragOffsetY = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    val activeIdx = draggedIndex ?: return@detectDragGestures
                                    val threshold = if (itemHeightPx > 0) itemHeightPx * 0.65f else 110f

                                    if (dragOffsetY > threshold && activeIdx < currentNotes.size - 1) {
                                        // Mover hacia abajo
                                        val movedItem = currentNotes.removeAt(activeIdx)
                                        currentNotes.add(activeIdx + 1, movedItem)
                                        draggedIndex = activeIdx + 1
                                        dragOffsetY -= itemHeightPx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (dragOffsetY < -threshold && activeIdx > 0) {
                                        // Mover hacia arriba
                                        val movedItem = currentNotes.removeAt(activeIdx)
                                        currentNotes.add(activeIdx - 1, movedItem)
                                        draggedIndex = activeIdx - 1
                                        dragOffsetY += itemHeightPx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onReorder(currentNotes.toList())
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    NoteJsNoteRow(
                        note = note,
                        isUnlocked = unlockedNoteIds.contains(note.id),
                        isLastItem = index == currentNotes.size - 1,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedNoteIds.contains(note.id),
                        onSelectToggle = { onSelectToggle(note.id) },
                        isDragging = isBeingDragged,
                        showDragHandle = currentNotes.size > 1 && !isSelectionMode,
                        dragHandleModifier = dragModifier,
                        modifier = rowModifier
                    )
                }
            }
        }
    }
}
