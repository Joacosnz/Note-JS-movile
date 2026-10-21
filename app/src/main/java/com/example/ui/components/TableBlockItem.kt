package com.example.ui.components

import com.example.util.MathEvaluator
import com.example.ui.theme.isAppInDarkTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.model.TableCell

private val amberColor = Color(0xFFFFB300)

data class TableColorOption(
    val name: String,
    val hex: String?
)

val tableColorPalette = listOf(
    TableColorOption("Sin color", null),
    TableColorOption("Rojo Coral", "#EF4444"),
    TableColorOption("Azul Cielo", "#3B82F6"),
    TableColorOption("Verde Menta", "#10B981"),
    TableColorOption("Amarillo Sol", "#F59E0B"),
    TableColorOption("Naranja Ámbar", "#F97316"),
    TableColorOption("Morado Lavanda", "#8B5CF6"),
    TableColorOption("Turquesa", "#06B6D4"),
    TableColorOption("Rosa Pastel", "#EC4899"),
    TableColorOption("Gris Elegante", "#6B7280")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableBlockItem(
    block: NoteBlock.Table,
    onUpdateBlock: (NoteBlock.Table) -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier,
    isPaperDark: Boolean = isAppInDarkTheme(),
    onExportTablePdf: (() -> Unit)? = null
) {
    val cellTextColor = if (isPaperDark) Color.White else Color(0xFF1C1C1E)
    val cellHintColor = if (isPaperDark) Color.LightGray else Color.DarkGray
    var isEditingTable by remember { mutableStateOf(false) }
    var selectedCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(setOf(Pair(0, 0))) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var colorScope by remember { mutableStateOf(NoteBlock.TableColorScope.CELL) }
    val haptics = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("table_block"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isEditingTable) 1.5.dp else 1.dp,
            color = if (isEditingTable) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TableChart,
                        contentDescription = null,
                        tint = amberColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isEditingTable) "Editando Tabla (${block.rows}×${block.cols})" else "Tabla (${block.rows}×${block.cols})",
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (block.hasHeaderRow) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Encabezado",
                                fontSize = 10.sp,
                                color = amberColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (block.hasZebraStripes) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Zebra",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingTable) {
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isEditingTable = false
                            },
                            shape = RoundedCornerShape(50),
                            color = amberColor,
                            contentColor = Color.Black
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Listo",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Listo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isEditingTable = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Estructura",
                                tint = amberColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (onExportTablePdf != null) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExportTablePdf()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Exportar Hoja de Cálculo a PDF",
                                    tint = amberColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showOptionsSheet = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones de Tabla",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Structure Controls (ONLY shown when isEditingTable is TRUE)
            if (isEditingTable) {
                val primaryCell = selectedCells.firstOrNull() ?: Pair(0, 0)
                val (r, c) = primaryCell

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estructura [${selectedCells.size} celdas sel]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = amberColor
                            )
                            IconButton(
                                onClick = { showOptionsSheet = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Colores",
                                    tint = amberColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowAbove(r))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, amberColor.copy(alpha = 0.6f))
                            ) {
                                Text("+ Fila ↑", fontSize = 11.sp, color = amberColor, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowBelow(r))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, amberColor.copy(alpha = 0.6f))
                            ) {
                                Text("+ Fila ↓", fontSize = 11.sp, color = amberColor, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                            }

                            if (block.rows > 1) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.deleteRow(r))
                                        selectedCells = setOf(Pair(0, 0))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                ) {
                                    Text("- Fila", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnLeft(c))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, amberColor.copy(alpha = 0.6f))
                            ) {
                                Text("+ Col ←", fontSize = 11.sp, color = amberColor, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnRight(c))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, amberColor.copy(alpha = 0.6f))
                            ) {
                                Text("+ Col →", fontSize = 11.sp, color = amberColor, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                            }

                            if (block.cols > 1) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.deleteColumn(c))
                                        selectedCells = setOf(Pair(0, 0))
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                ) {
                                    Text("- Col", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, maxLines = 1, softWrap = false, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Table Grid Container
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (rIndex in 0 until block.rows) {
                        Row(
                            modifier = Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (cIndex in 0 until block.cols) {
                                val cell = block.matrix.getOrNull(rIndex)?.getOrNull(cIndex) ?: TableCell()
                                val isHeader = block.hasHeaderRow && rIndex == 0
                                val isZebra = block.hasZebraStripes && rIndex % 2 == 1 && !isHeader

                                val baseBg = if (!cell.backgroundColorHex.isNullOrBlank()) {
                                    runCatching { Color(android.graphics.Color.parseColor(cell.backgroundColorHex)) }
                                        .getOrDefault(Color.Transparent)
                                } else if (isHeader) {
                                    amberColor.copy(alpha = 0.22f)
                                } else if (isZebra) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                } else {
                                    Color.Transparent
                                }

                                val isSelected = selectedCells.contains(Pair(rIndex, cIndex))
                                val cellShape = RoundedCornerShape(4.dp)

                                Box(
                                    modifier = Modifier
                                        .widthIn(min = 120.dp, max = 250.dp)
                                        .fillMaxHeight()
                                        .clip(cellShape)
                                        .background(baseBg)
                                        .border(
                                            width = if (isSelected && isEditingTable) 2.dp else 0.5.dp,
                                            color = if (isSelected && isEditingTable) amberColor
                                            else if (isHeader) amberColor.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            shape = cellShape
                                        )
                                        .clickable {
                                            selectedCells = if (selectedCells.contains(Pair(rIndex, cIndex)) && selectedCells.size > 1) {
                                                selectedCells - Pair(rIndex, cIndex)
                                            } else {
                                                selectedCells + Pair(rIndex, cIndex)
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = cell.text,
                                        onValueChange = { newText ->
                                            val solvedText = if (newText.length > cell.text.length && newText.endsWith("=")) {
                                                MathEvaluator.trySolveInlineEquation(newText, newText.length)?.newText ?: newText
                                            } else {
                                                newText
                                            }
                                            onUpdateBlock(block.updateCellText(rIndex, cIndex, solvedText))
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = cellTextColor,
                                            fontSize = 14.sp,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                        cursorBrush = SolidColor(amberColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    selectedCells = setOf(Pair(rIndex, cIndex))
                                                }
                                            },
                                        decorationBox = { innerTextField ->
                                            if (cell.text.isEmpty() && !isSelected) {
                                                Text(
                                                    text = if (isHeader) "Encabezado" else "",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = cellHintColor.copy(alpha = 0.5f),
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptionsSheet) {
        TableOptionsBottomSheet(
            block = block,
            selectedCells = selectedCells,
            onSelectCellsChange = { selectedCells = it },
            colorScope = colorScope,
            isEditingTable = isEditingTable,
            onToggleEditingTable = { isEditingTable = !isEditingTable },
            onColorScopeChange = { colorScope = it },
            onUpdateBlock = onUpdateBlock,
            onDeleteBlock = {
                showOptionsSheet = false
                onDeleteBlock()
            },
            onExportPdf = onExportTablePdf,
            onDismiss = { showOptionsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOptionsBottomSheet(
    block: NoteBlock.Table,
    selectedCells: Set<Pair<Int, Int>>,
    onSelectCellsChange: (Set<Pair<Int, Int>>) -> Unit,
    colorScope: NoteBlock.TableColorScope,
    isEditingTable: Boolean,
    onToggleEditingTable: () -> Unit,
    onColorScopeChange: (NoteBlock.TableColorScope) -> Unit,
    onUpdateBlock: (NoteBlock.Table) -> Unit,
    onDeleteBlock: () -> Unit,
    onExportPdf: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val haptics = LocalHapticFeedback.current

    val primaryCell = selectedCells.firstOrNull() ?: Pair(0, 0)
    val (r, c) = primaryCell

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(amberColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = amberColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Opciones de Tabla",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${block.rows} Filas × ${block.cols} Columnas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = amberColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${block.rows}×${block.cols}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = amberColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Segmented Tabs Navigation
            val tabs = listOf("Estilo y Colores", "Estructura", "Formato y Datos")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Estilo y Colores Tab
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. COLOR SWATCHES PROMINENTLY AT THE TOP
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Elige un Color (${selectedCells.size} sel):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tableColorPalette.forEach { colorOpt ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (colorOpt.hex != null) Color(android.graphics.Color.parseColor(colorOpt.hex))
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                when (colorScope) {
                                                    NoteBlock.TableColorScope.CELL -> {
                                                        onUpdateBlock(block.applyColorToCells(selectedCells, colorOpt.hex))
                                                    }
                                                    NoteBlock.TableColorScope.ROW -> {
                                                        onUpdateBlock(block.applyColor(r, c, colorOpt.hex, NoteBlock.TableColorScope.ROW))
                                                    }
                                                    NoteBlock.TableColorScope.COLUMN -> {
                                                        onUpdateBlock(block.applyColor(r, c, colorOpt.hex, NoteBlock.TableColorScope.COLUMN))
                                                    }
                                                    NoteBlock.TableColorScope.TABLE -> {
                                                        onUpdateBlock(block.applyColor(r, c, colorOpt.hex, NoteBlock.TableColorScope.TABLE))
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (colorOpt.hex == null) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Sin color",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // 2. SCROLLABLE CHIP BAR FOR SELECTION SCOPE & BULK ACTIONS
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = colorScope == NoteBlock.TableColorScope.CELL,
                                onClick = { onColorScopeChange(NoteBlock.TableColorScope.CELL) },
                                label = { Text("Celdas (${selectedCells.size}) 🔲", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amberColor.copy(alpha = 0.2f),
                                    selectedLabelColor = amberColor
                                )
                            )

                            FilterChip(
                                selected = colorScope == NoteBlock.TableColorScope.ROW,
                                onClick = {
                                    onColorScopeChange(NoteBlock.TableColorScope.ROW)
                                    val rowCells = mutableSetOf<Pair<Int, Int>>()
                                    for (cIdx in 0 until block.cols) {
                                        rowCells.add(Pair(r, cIdx))
                                    }
                                    onSelectCellsChange(selectedCells + rowCells)
                                },
                                label = { Text("Toda Fila ${r + 1} ↔️", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amberColor.copy(alpha = 0.2f),
                                    selectedLabelColor = amberColor
                                )
                            )

                            FilterChip(
                                selected = colorScope == NoteBlock.TableColorScope.COLUMN,
                                onClick = {
                                    onColorScopeChange(NoteBlock.TableColorScope.COLUMN)
                                    val colCells = mutableSetOf<Pair<Int, Int>>()
                                    for (rIdx in 0 until block.rows) {
                                        colCells.add(Pair(rIdx, c))
                                    }
                                    onSelectCellsChange(selectedCells + colCells)
                                },
                                label = { Text("Toda Col ${c + 1} ↕️", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amberColor.copy(alpha = 0.2f),
                                    selectedLabelColor = amberColor
                                )
                            )

                            FilterChip(
                                selected = colorScope == NoteBlock.TableColorScope.TABLE,
                                onClick = {
                                    onColorScopeChange(NoteBlock.TableColorScope.TABLE)
                                    val all = mutableSetOf<Pair<Int, Int>>()
                                    for (rIdx in 0 until block.rows) {
                                        for (cIdx in 0 until block.cols) {
                                            all.add(Pair(rIdx, cIdx))
                                        }
                                    }
                                    onSelectCellsChange(all)
                                },
                                label = { Text("Toda Tabla 📊", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = amberColor.copy(alpha = 0.2f),
                                    selectedLabelColor = amberColor
                                )
                            )

                            FilterChip(
                                selected = false,
                                onClick = { onSelectCellsChange(setOf(Pair(0, 0))) },
                                label = { Text("Limpiar ✖", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                            )
                        }

                        // 3. COMPACT INTERACTIVE CELL MAP
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Mapa de Selección Libre (Toca celdas para añadir):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )

                            // Render interactive matrix grid for multi-selection as clean cells with uniform height and text truncation
                            val gridHorizontalScroll = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                                    .then(if (block.cols > 8) Modifier.horizontalScroll(gridHorizontalScroll) else Modifier)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (rIdx in 0 until block.rows) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                for (cIdx in 0 until block.cols) {
                                                    val cell = block.matrix.getOrNull(rIdx)?.getOrNull(cIdx)
                                                    val cellPair = Pair(rIdx, cIdx)
                                                    val isSelected = selectedCells.contains(cellPair)

                                                    val bg = if (isSelected) {
                                                        if (cell != null && !cell.backgroundColorHex.isNullOrBlank()) {
                                                            runCatching { Color(android.graphics.Color.parseColor(cell.backgroundColorHex)) }
                                                                .getOrDefault(amberColor)
                                                        } else {
                                                            amberColor
                                                        }
                                                    } else {
                                                        if (cell != null && !cell.backgroundColorHex.isNullOrBlank()) {
                                                            runCatching { Color(android.graphics.Color.parseColor(cell.backgroundColorHex)).copy(alpha = 0.35f) }
                                                                .getOrDefault(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .height(28.dp)
                                                            .widthIn(min = 28.dp, max = 56.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(bg)
                                                            .border(
                                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                                color = if (isSelected) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .clickable {
                                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                val nextSelection = if (selectedCells.contains(cellPair) && selectedCells.size > 1) {
                                                                    selectedCells - cellPair
                                                                } else {
                                                                    selectedCells + cellPair
                                                                }
                                                                onSelectCellsChange(nextSelection)
                                                            }
                                                            .padding(horizontal = 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Seleccionada",
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                            if (!cell?.text.isNullOrBlank()) {
                                                                Text(
                                                                    text = cell.text,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    softWrap = false,
                                                                    textAlign = TextAlign.Center
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
                    }
                }

                1 -> {
                    // Estructura Tab (Matriz Visual Grid 6x6)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = null,
                                tint = amberColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Redimensionar Matriz Táctil (Grid 6×6)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val gridRows = 6
                                val gridCols = 6

                                for (rIdx in 0 until gridRows) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (cIdx in 0 until gridCols) {
                                            val isHighlighted = rIdx < block.rows && cIdx < block.cols

                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isHighlighted) amberColor.copy(alpha = 0.85f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                    .border(
                                                        width = if (isHighlighted) 1.5.dp else 0.5.dp,
                                                        color = if (isHighlighted) amberColor else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val targetRows = rIdx + 1
                                                        val targetCols = cIdx + 1
                                                        val updatedBlock = block.copy(rows = targetRows, cols = targetCols)
                                                        updatedBlock.ensureMatrixSize()
                                                        onUpdateBlock(updatedBlock)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Text(
                            text = "Acciones en Fila ${r + 1} / Columna ${c + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = amberColor
                        )

                        // Row 1: Add Row buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowAbove(r))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Fila ↑", fontSize = 12.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowBelow(r))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Fila ↓", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        // Row 2: Add Column buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnLeft(c))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Col ←", fontSize = 12.sp, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnRight(c))
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Col →", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        // Row 3: Delete Row & Delete Column buttons (grouped in a Row with equal .weight(1f))
                        if (block.rows > 1 || block.cols > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (block.rows > 1) {
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateBlock(block.deleteRow(r))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("- Fila", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                                if (block.cols > 1) {
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onUpdateBlock(block.deleteColumn(c))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("- Col", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Formato y Datos Tab
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Visual Cards for Header & Zebra Style Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Card 1: Header Row Toggle Card
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.toggleHeaderRow())
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (block.hasHeaderRow) amberColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (block.hasHeaderRow) 2.dp else 1.dp,
                                    color = if (block.hasHeaderRow) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (block.hasHeaderRow) amberColor else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewHeadline,
                                            contentDescription = null,
                                            tint = if (block.hasHeaderRow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Encabezado",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (block.hasHeaderRow) amberColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (block.hasHeaderRow) "Activado" else "Desactivado",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Card 2: Zebra Stripes Toggle Card
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.toggleZebraStripes())
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (block.hasZebraStripes) amberColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (block.hasZebraStripes) 2.dp else 1.dp,
                                    color = if (block.hasZebraStripes) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (block.hasZebraStripes) amberColor else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewStream,
                                            contentDescription = null,
                                            tint = if (block.hasZebraStripes) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Filas Zebra",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (block.hasZebraStripes) amberColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (block.hasZebraStripes) "Activado" else "Desactivado",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        if (onExportPdf != null) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExportPdf()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = amberColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Exportar Hoja de Cálculo a PDF (PdfBox)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onUpdateBlock(block.clearCells(selectedCells)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Vaciar Texto de Celdas Seleccionadas (${selectedCells.size})", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = onDeleteBlock,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eliminar tabla completa",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
