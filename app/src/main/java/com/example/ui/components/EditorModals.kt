package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.AudioBlock
import com.example.model.ImageBlock
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.PaperStyle
import com.example.model.PdfBlock
import com.example.ui.theme.NoteJsAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class ActiveHashtagInfo(
    val targetId: String,
    val textFieldValue: TextFieldValue,
    val hashIndex: Int,
    val rawQuery: String
)

internal data class SmartTagItem(
    val tag: String,
    val count: Int,
    val isNew: Boolean
)

@Composable
internal fun SmartTagsSuggestionRow(
    tags: List<SmartTagItem>,
    amberColor: Color,
    isDark: Boolean,
    onSelectTag: (SmartTagItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.92f) else Color(0xFFF2F2F7).copy(alpha = 0.92f),
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tag,
                contentDescription = null,
                tint = amberColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tags, key = { "${it.tag}_${it.isNew}" }) { item ->
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTag(item)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (item.isNew) {
                            amberColor.copy(alpha = 0.18f)
                        } else {
                            if (isDark) Color(0xFF3A3A3C) else Color.White
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (item.isNew) amberColor.copy(alpha = 0.5f)
                            else if (isDark) Color.White.copy(alpha = 0.12f)
                            else Color.Black.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (item.isNew) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = amberColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Crear #${item.tag}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = amberColor
                                    )
                                )
                            } else {
                                Text(
                                    text = "#${item.tag}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                )
                                if (item.count > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = amberColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "${item.count}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = amberColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoteLinkSuggestionMenu(
    linkQuery: String,
    notes: List<NoteEntity>,
    amberColor: Color,
    isDark: Boolean,
    onNoteSelected: (noteId: Long, noteTitle: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.95f) else Color(0xFFF2F2F7).copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Vincular nota",
                        tint = amberColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vincular nota",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = amberColor
                        )
                    )
                    if (linkQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "([[ $linkQuery)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() }
                )
            }

            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (notes.isEmpty()) {
                Text(
                    text = if (linkQuery.isBlank()) "Sin notas disponibles para vincular" else "No se encontraron notas con \"$linkQuery\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        val displayTitle = note.title.ifBlank { "Nota sin título" }
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNoteSelected(note.id, displayTitle)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = amberColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    ),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                if (note.folder.isNotBlank()) {
                                    Text(
                                        text = note.folder,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorFontSizeCarouselSheet(
    showFontSizeCarouselSheet: Boolean,
    customFontSizeSp: Int,
    isPaperDark: Boolean,
    isDark: Boolean,
    amber: Color,
    onFontSizeChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (showFontSizeCarouselSheet) {
        val fontSizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val initialItemIndex = (customFontSizeSp - 1).coerceIn(0, 149)
        val verticalPickerState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)
        val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = verticalPickerState)
        val coroutineScope = rememberCoroutineScope()
        val haptics = LocalHapticFeedback.current

        LaunchedEffect(verticalPickerState) {
            snapshotFlow { verticalPickerState.firstVisibleItemIndex }
                .collect { firstIndex ->
                    val newSize = (firstIndex + 1).coerceIn(1, 150)
                    if (newSize != customFontSizeSp) {
                        onFontSizeChange(newSize)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        }

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = fontSizeSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = if (isPaperDark) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tamaño de texto",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isPaperDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = amber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$customFontSizeSp sp",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = amber
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.50f)
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = amber.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, amber)
                    ) {}

                    LazyColumn(
                        state = verticalPickerState,
                        flingBehavior = snapFlingBehavior,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 60.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(150) { index ->
                            val number = index + 1
                            val isSelected = number == customFontSizeSp

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clickable {
                                        onFontSizeChange(number)
                                        coroutineScope.launch {
                                            verticalPickerState.animateScrollToItem(index)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$number",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = if (isSelected) 30.sp else 20.sp
                                    ),
                                    color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(12, 14, 17, 20, 24, 32, 48, 64).forEach { quickSize ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (customFontSizeSp == quickSize) amber.copy(alpha = 0.35f) else if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onFontSizeChange(quickSize)
                                    coroutineScope.launch {
                                        verticalPickerState.animateScrollToItem((quickSize - 1).coerceIn(0, 149))
                                    }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$quickSize",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (customFontSizeSp == quickSize) amber else if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorStatsBottomSheet(
    showStatsDialog: Boolean,
    blocks: List<NoteBlock>,
    title: String,
    createdAt: Long,
    updatedAt: Long,
    isPaperDark: Boolean,
    isDark: Boolean,
    amber: Color,
    onDismissRequest: () -> Unit
) {
    if (showStatsDialog) {
        val statsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val textBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Text>() }
        val checklistBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Checklist>() }
        val tableBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Table>() }
        val audioBlocks = remember(blocks) { blocks.filterIsInstance<AudioBlock>() }
        val pdfBlocks = remember(blocks) { blocks.filterIsInstance<PdfBlock>() }

        val plainTextSnippet = remember(blocks, title) {
            val contentStr = blocks.joinToString(" ") {
                when (it) {
                    is NoteBlock.Text -> it.content
                    is NoteBlock.Checklist -> it.content
                    is NoteBlock.SimpleList -> it.content
                    is NoteBlock.CalloutBox -> it.content
                    is NoteBlock.Drawing -> ""
                    is NoteBlock.Table -> it.matrix.joinToString(" ") { row -> row.joinToString(" ") { cell -> cell.text } }
                    is PdfBlock -> it.fileName
                    is ImageBlock -> "[Imagen]"
                    is NoteBlock.Image -> it.fileName
                    is AudioBlock -> it.transcription
                    is NoteBlock.Math -> it.latexCode
                }
            }
            "$title $contentStr"
        }

        val wordCount = remember(plainTextSnippet) {
            plainTextSnippet.split(WORD_COUNT_REGEX).filter { it.isNotBlank() }.size
        }
        val charCount = remember(plainTextSnippet) { plainTextSnippet.length }
        val readTimeMin = remember(wordCount) { (wordCount / 200).coerceAtLeast(1) }

        val createdFormatted = remember(createdAt) {
            SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES")).format(Date(createdAt))
        }
        val updatedFormatted = remember(updatedAt) {
            SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES")).format(Date(updatedAt))
        }

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = statsSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (isPaperDark) Color(0xFF141416) else Color(0xFFF8FAFC),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Información y estadísticas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isPaperDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassContainer(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isDark = isPaperDark
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$wordCount",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = amber
                            )
                            Text(
                                text = "Palabras",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LiquidGlassContainer(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isDark = isPaperDark
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$charCount",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = amber
                            )
                            Text(
                                text = "Caracteres",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LiquidGlassContainer(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isDark = isPaperDark
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "~$readTimeMin min",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = amber
                            )
                            Text(
                                text = "Lectura",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LiquidGlassContainer(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    isDark = isDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Detalles del contenido",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Párrafos de texto:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${textBlocks.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }

                        if (checklistBlocks.isNotEmpty()) {
                            val doneCount = checklistBlocks.count { it.isChecked }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tareas completadas:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$doneCount de ${checklistBlocks.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = amber)
                            }
                        }

                        if (tableBlocks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tablas:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${tableBlocks.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (audioBlocks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Grabaciones de audio:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${audioBlocks.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (pdfBlocks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Adjuntos PDF:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${pdfBlocks.size}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Creada:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(createdFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modificada:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(updatedFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cerrar",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = amber
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorColorPickerDialog(
    showColorDialog: Boolean,
    colorIndex: Int,
    isDark: Boolean,
    amber: Color,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    KeepColorPickerBottomSheet(
        show = showColorDialog,
        selectedColorIndex = colorIndex,
        isDark = isDark,
        onColorSelected = onColorSelected,
        onDismissRequest = onDismissRequest
    )
}

@Composable
internal fun EditorPaperStyleDialog(
    showPaperDialog: Boolean,
    paperStyle: PaperStyle,
    amber: Color,
    onStyleSelected: (PaperStyle) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Estilo de papel") },
            text = {
                Column(modifier = Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
                    PaperStyle.entries.forEach { style ->
                        TextButton(
                            onClick = {
                                onStyleSelected(style)
                                onDismissRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (style) {
                                    PaperStyle.BLANK -> "Liso"
                                    PaperStyle.RULED -> "Rayado"
                                    PaperStyle.GRID -> "Cuadriculado"
                                    PaperStyle.DOTS -> "Punteado"
                                },
                                color = if (paperStyle == style) amber else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorReminderBottomSheet(
    showReminderDialog: Boolean,
    reminderTimestamp: Long?,
    isDark: Boolean,
    amber: Color,
    onSaveReminder: (selectedTimestamp: Long, offsets: List<Int>) -> Unit,
    onDeleteReminder: () -> Unit,
    onDismissRequest: () -> Unit
) {
    if (showReminderDialog) {
        val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val haptics = LocalHapticFeedback.current

        val dayOptions = remember {
            List(60) { offset ->
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, offset)
                }
            }
        }
        val hourOptions = remember { (0..23).toList() }
        val minuteOptions = remember { (0..59).toList() }

        val initialCal = remember(reminderTimestamp) {
            Calendar.getInstance().apply {
                if (reminderTimestamp != null && reminderTimestamp > System.currentTimeMillis()) {
                    timeInMillis = reminderTimestamp
                } else {
                    add(Calendar.HOUR_OF_DAY, 1)
                    set(Calendar.MINUTE, 0)
                }
            }
        }

        var selectedDayIndex by remember {
            val initialDay = dayOptions.indexOfFirst {
                it.get(Calendar.YEAR) == initialCal.get(Calendar.YEAR) &&
                it.get(Calendar.DAY_OF_YEAR) == initialCal.get(Calendar.DAY_OF_YEAR)
            }
            mutableIntStateOf(if (initialDay >= 0) initialDay else 0)
        }

        var selectedHourIndex by remember {
            mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23))
        }

        var selectedMinuteIndex by remember {
            mutableIntStateOf(initialCal.get(Calendar.MINUTE).coerceIn(0, 59))
        }

        var selectedReminderOffsets by remember { mutableStateOf(setOf(0, 15)) }

        val reminderCoroutineScope = rememberCoroutineScope()

        val dayPickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedDayIndex)
        val hourPickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedHourIndex)
        val minutePickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinuteIndex)

        val daySnap = rememberSnapFlingBehavior(lazyListState = dayPickerState)
        val hourSnap = rememberSnapFlingBehavior(lazyListState = hourPickerState)
        val minuteSnap = rememberSnapFlingBehavior(lazyListState = minutePickerState)

        LaunchedEffect(dayPickerState) {
            snapshotFlow { dayPickerState.firstVisibleItemIndex }
                .collect { index ->
                    if (index in dayOptions.indices && index != selectedDayIndex) {
                        selectedDayIndex = index
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        }

        LaunchedEffect(hourPickerState) {
            snapshotFlow { hourPickerState.firstVisibleItemIndex }
                .collect { index ->
                    if (index in hourOptions.indices && index != selectedHourIndex) {
                        selectedHourIndex = index
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        }

        LaunchedEffect(minutePickerState) {
            snapshotFlow { minutePickerState.firstVisibleItemIndex }
                .collect { index ->
                    if (index in minuteOptions.indices && index != selectedMinuteIndex) {
                        selectedMinuteIndex = index
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        }

        val selectedTimestamp = remember(selectedDayIndex, selectedHourIndex, selectedMinuteIndex) {
            Calendar.getInstance().apply {
                val base = dayOptions.getOrElse(selectedDayIndex) { Calendar.getInstance() }
                set(Calendar.YEAR, base.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, base.get(Calendar.DAY_OF_YEAR))
                set(Calendar.HOUR_OF_DAY, hourOptions.getOrElse(selectedHourIndex) { 0 })
                set(Calendar.MINUTE, minuteOptions.getOrElse(selectedMinuteIndex) { 0 })
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        val formattedSelectedDateTime = remember(selectedTimestamp) {
            val sdf = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es-ES"))
            val formatted = sdf.format(Date(selectedTimestamp))
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }
        }

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = reminderSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Programar recordatorio",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = amber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formattedSelectedDateTime,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = amber
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedDayIndex = 0
                                selectedHourIndex = 18
                                selectedMinuteIndex = 0
                                reminderCoroutineScope.launch {
                                    dayPickerState.animateScrollToItem(0)
                                    hourPickerState.animateScrollToItem(18)
                                    minutePickerState.animateScrollToItem(0)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hoy 18:00",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val targetDay = if (dayOptions.size > 1) 1 else 0
                                selectedDayIndex = targetDay
                                selectedHourIndex = 9
                                selectedMinuteIndex = 0
                                reminderCoroutineScope.launch {
                                    dayPickerState.animateScrollToItem(targetDay)
                                    hourPickerState.animateScrollToItem(9)
                                    minutePickerState.animateScrollToItem(0)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Mañana 09:00",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DÍA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(0.50f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "HORA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(0.25f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MIN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(0.25f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = amber.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, amber)
                    ) {}

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyColumn(
                            state = dayPickerState,
                            flingBehavior = daySnap,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier
                                .weight(0.50f)
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(dayOptions) { idx, dayCal ->
                                val isSelected = idx == selectedDayIndex
                                val dayText = remember(dayCal) {
                                    val now = Calendar.getInstance()
                                    if (dayCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                        dayCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                                        "Hoy"
                                    } else {
                                        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                        if (dayCal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                                            dayCal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)) {
                                            "Mañana"
                                        } else {
                                            val sdf = SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("es-ES"))
                                            sdf.format(dayCal.time)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clickable {
                                            selectedDayIndex = idx
                                            reminderCoroutineScope.launch {
                                                dayPickerState.animateScrollToItem(idx)
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isSelected) 17.sp else 14.sp
                                        ),
                                        color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )

                        LazyColumn(
                            state = hourPickerState,
                            flingBehavior = hourSnap,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(hourOptions) { idx, hour ->
                                val isSelected = idx == selectedHourIndex
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clickable {
                                            selectedHourIndex = idx
                                            reminderCoroutineScope.launch {
                                                hourPickerState.animateScrollToItem(idx)
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", hour),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isSelected) 20.sp else 15.sp
                                        ),
                                        color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }

                        Text(
                            text = ":",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = amber,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        LazyColumn(
                            state = minutePickerState,
                            flingBehavior = minuteSnap,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 55.dp),
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(minuteOptions) { idx, minute ->
                                val isSelected = idx == selectedMinuteIndex
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clickable {
                                            selectedMinuteIndex = idx
                                            reminderCoroutineScope.launch {
                                                minutePickerState.animateScrollToItem(idx)
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d", minute),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = if (isSelected) 20.sp else 15.sp
                                        ),
                                        color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Avisar antes",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(REMINDER_OFFSET_OPTIONS) { option ->
                            val isSelected = selectedReminderOffsets.contains(option.minutes)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedReminderOffsets = if (isSelected) {
                                        if (selectedReminderOffsets.size > 1) selectedReminderOffsets - option.minutes else selectedReminderOffsets
                                    } else {
                                        selectedReminderOffsets + option.minutes
                                    }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                label = { Text(option.label, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amber,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = amber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSaveReminder(selectedTimestamp, selectedReminderOffsets.toList())
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Guardar recordatorio",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.Black
                        )
                    }
                }

                if (reminderTimestamp != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            onDeleteReminder()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Eliminar recordatorio",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EditorScannedPdfNameDialog(
    pdfUri: Uri?,
    scannedPdfName: String,
    onNameChange: (String) -> Unit,
    onConfirm: (Uri, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (pdfUri != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Nombrar documento escaneado") },
            text = {
                OutlinedTextField(
                    value = scannedPdfName,
                    onValueChange = onNameChange,
                    label = { Text("Nombre del archivo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm(pdfUri, scannedPdfName)
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

