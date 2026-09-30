package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    TableColorOption("Rojo", "#EF4444"),
    TableColorOption("Azul", "#3B82F6"),
    TableColorOption("Verde", "#10B981"),
    TableColorOption("Amarillo", "#F59E0B"),
    TableColorOption("Naranja", "#F97316"),
    TableColorOption("Morado", "#8B5CF6"),
    TableColorOption("Turquesa", "#06B6D4"),
    TableColorOption("Rosa", "#EC4899"),
    TableColorOption("Gris", "#6B7280")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableBlockItem(
    block: NoteBlock.Table,
    onUpdateBlock: (NoteBlock.Table) -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingTable by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = amberColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isEditingTable) "Editando Tabla (${block.rows}×${block.cols})" else "Tabla (${block.rows}×${block.cols})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (block.hasHeaderRow) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = amberColor.copy(alpha = 0.15f)
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
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingTable) {
                        TextButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isEditingTable = false
                            }
                        ) {
                            Text("Listo", color = amberColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

            // Structure & Dimension Controls (ONLY shown when isEditingTable is TRUE)
            if (isEditingTable) {
                val activeCell = selectedCell ?: Pair(0, 0)
                val (r, c) = activeCell

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
                                text = "Estructura [Fila ${r + 1}, Col ${c + 1}]",
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
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowAbove(r))
                                }
                            ) {
                                Text("+ Fila ↑", fontSize = 11.sp, color = amberColor)
                            }
                            TextButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addRowBelow(r))
                                }
                            ) {
                                Text("+ Fila ↓", fontSize = 11.sp, color = amberColor)
                            }
                            if (block.rows > 1) {
                                TextButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.deleteRow(r))
                                        selectedCell = null
                                    }
                                ) {
                                    Text("- Fila", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(1.dp)
                                    .padding(horizontal = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            TextButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnLeft(c))
                                }
                            ) {
                                Text("+ Col ←", fontSize = 11.sp, color = amberColor)
                            }
                            TextButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.addColumnRight(c))
                                }
                            ) {
                                Text("+ Col →", fontSize = 11.sp, color = amberColor)
                            }
                            if (block.cols > 1) {
                                TextButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateBlock(block.deleteColumn(c))
                                        selectedCell = null
                                    }
                                ) {
                                    Text("- Col", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Table Grid
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (block.cols > 3) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (rIndex in 0 until block.rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (cIndex in 0 until block.cols) {
                                val cell = block.matrix.getOrNull(rIndex)?.getOrNull(cIndex) ?: TableCell()
                                val isHeader = block.hasHeaderRow && rIndex == 0

                                val baseBg = if (!cell.backgroundColorHex.isNullOrBlank()) {
                                    runCatching { Color(android.graphics.Color.parseColor(cell.backgroundColorHex)) }
                                        .getOrDefault(Color.Transparent)
                                } else if (isHeader) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    Color.Transparent
                                }

                                val isSelected = selectedCell == Pair(rIndex, cIndex)

                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (block.cols > 3) Modifier.widthIn(min = 110.dp)
                                            else Modifier.weight(1f)
                                        )
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(baseBg)
                                        .border(
                                            width = if (isSelected && isEditingTable) 2.dp else 0.5.dp,
                                            color = if (isSelected && isEditingTable) amberColor
                                            else if (isHeader) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedCell = Pair(rIndex, cIndex)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = cell.text,
                                        onValueChange = { newText ->
                                            onUpdateBlock(block.updateCellText(rIndex, cIndex, newText))
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp,
                                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        cursorBrush = SolidColor(amberColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    selectedCell = Pair(rIndex, cIndex)
                                                }
                                            },
                                        decorationBox = { innerTextField ->
                                            if (cell.text.isEmpty() && !isSelected) {
                                                Text(
                                                    text = if (isHeader) "Encabezado" else "",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
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
            selectedCell = selectedCell,
            colorScope = colorScope,
            isEditingTable = isEditingTable,
            onToggleEditingTable = { isEditingTable = !isEditingTable },
            onColorScopeChange = { colorScope = it },
            onUpdateBlock = onUpdateBlock,
            onDeleteBlock = {
                showOptionsSheet = false
                onDeleteBlock()
            },
            onDismiss = { showOptionsSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableOptionsBottomSheet(
    block: NoteBlock.Table,
    selectedCell: Pair<Int, Int>?,
    colorScope: NoteBlock.TableColorScope,
    isEditingTable: Boolean,
    onToggleEditingTable: () -> Unit,
    onColorScopeChange: (NoteBlock.TableColorScope) -> Unit,
    onUpdateBlock: (NoteBlock.Table) -> Unit,
    onDeleteBlock: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Opciones de Tabla",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Mode Toggle Switch / Button
            OutlinedButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleEditingTable()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = amberColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditingTable) "Finalizar edición de estructura" else "Editar estructura (Filas y Columnas)",
                    color = amberColor,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 1: Color Customization
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Personalizar Colores",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Scope Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val scopes = listOf(
                        NoteBlock.TableColorScope.CELL to "Celda",
                        NoteBlock.TableColorScope.ROW to "Fila",
                        NoteBlock.TableColorScope.COLUMN to "Columna",
                        NoteBlock.TableColorScope.TABLE to "Toda la tabla"
                    )

                    scopes.forEach { (scope, label) ->
                        FilterChip(
                            selected = colorScope == scope,
                            onClick = { onColorScopeChange(scope) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = amberColor.copy(alpha = 0.2f),
                                selectedLabelColor = amberColor
                            )
                        )
                    }
                }

                // Color Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tableColorPalette.forEach { colorOpt ->
                        val (r, c) = selectedCell ?: Pair(0, 0)
                        val isSelectedColor = when (colorScope) {
                            NoteBlock.TableColorScope.CELL -> block.matrix.getOrNull(r)?.getOrNull(c)?.backgroundColorHex == colorOpt.hex
                            NoteBlock.TableColorScope.ROW -> block.matrix.getOrNull(r)?.firstOrNull()?.backgroundColorHex == colorOpt.hex
                            NoteBlock.TableColorScope.COLUMN -> block.matrix.firstOrNull()?.getOrNull(c)?.backgroundColorHex == colorOpt.hex
                            NoteBlock.TableColorScope.TABLE -> block.matrix.firstOrNull()?.firstOrNull()?.backgroundColorHex == colorOpt.hex
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colorOpt.hex != null) Color(android.graphics.Color.parseColor(colorOpt.hex))
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelectedColor) 2.5.dp else 1.dp,
                                    color = if (isSelectedColor) amberColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateBlock(block.applyColor(r, c, colorOpt.hex, colorScope))
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
                            } else if (isSelectedColor) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 2: Format & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fila de Encabezado",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Aplica formato negrita y tono distintivo a la primera fila",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = block.hasHeaderRow,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateBlock(block.toggleHeaderRow())
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 3: Structure & Clearing Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Acciones de Estructura",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val r = selectedCell?.first ?: (block.rows - 1)
                            onUpdateBlock(block.addRowBelow(r))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Fila", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val c = selectedCell?.second ?: (block.cols - 1)
                            onUpdateBlock(block.addColumnRight(c))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Columna", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateBlock(block.clearTable())
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vaciar", fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section 4: Delete Table
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
