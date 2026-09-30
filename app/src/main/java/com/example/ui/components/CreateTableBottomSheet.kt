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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val amberColor = Color(0xFFFFB300)

data class VisualTableTemplate(
    val title: String,
    val rows: Int,
    val cols: Int,
    val hasHeader: Boolean,
    val hasZebra: Boolean
)

val visualTemplates = listOf(
    VisualTableTemplate("Estándar", 3, 3, hasHeader = false, hasZebra = false),
    VisualTableTemplate("Encabezado", 4, 3, hasHeader = true, hasZebra = false),
    VisualTableTemplate("Filas Zebra", 5, 3, hasHeader = true, hasZebra = true),
    VisualTableTemplate("Compacta", 2, 2, hasHeader = true, hasZebra = false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTableBottomSheet(
    initialRows: Int = 3,
    initialCols: Int = 3,
    onCreateTable: (rows: Int, cols: Int, hasHeaderRow: Boolean, hasZebraStripes: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rows by remember { mutableIntStateOf(initialRows.coerceIn(1, 10)) }
    var cols by remember { mutableIntStateOf(initialCols.coerceIn(1, 6)) }
    var hasHeaderRow by remember { mutableStateOf(true) }
    var hasZebraStripes by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("dialog_create_table")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Header Banner
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(amberColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = amberColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Nueva Tabla",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$rows Filas × $cols Columnas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = amberColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = amberColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, amberColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$rows×$cols",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = amberColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // 1. VISUAL LIVE PREVIEW CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val previewRows = rows.coerceAtMost(5)
                    val previewCols = cols.coerceAtMost(5)

                    for (r in 0 until previewRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(previewCols) {
                                val isHeader = hasHeaderRow && r == 0
                                val isZebra = hasZebraStripes && r % 2 == 1 && !isHeader

                                val bg = if (isHeader) amberColor.copy(alpha = 0.35f)
                                else if (isZebra) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(bg)
                                        .border(
                                            width = if (isHeader) 1.dp else 0.5.dp,
                                            color = if (isHeader) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isHeader) {
                                        Box(
                                            modifier = Modifier
                                                .width(18.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(amberColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. VISUAL TEMPLATE CARDS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Plantillas Visuales",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    visualTemplates.forEach { template ->
                        val isSelected = rows == template.rows && cols == template.cols &&
                                hasHeaderRow == template.hasHeader && hasZebraStripes == template.hasZebra

                        Surface(
                            modifier = Modifier
                                .width(105.dp)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    rows = template.rows
                                    cols = template.cols
                                    hasHeaderRow = template.hasHeader
                                    hasZebraStripes = template.hasZebra
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) amberColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Graphic Representation
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    repeat(3) { rIdx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            repeat(3) {
                                                val isHdr = template.hasHeader && rIdx == 0
                                                val isZb = template.hasZebra && rIdx % 2 == 1 && !isHdr
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(
                                                            if (isHdr) amberColor
                                                            else if (isZb) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                            else MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = template.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) amberColor else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3. VISUAL MATRIX GRID SELECTOR (6x6)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                        text = "Ajuste Táctil de Matriz (Grid 6×6)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val gridRows = 6
                        val gridCols = 6

                        for (r in 0 until gridRows) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (c in 0 until gridCols) {
                                    val isHighlighted = r < rows && c < cols

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isHighlighted) amberColor.copy(alpha = 0.85f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = if (isHighlighted) 1.5.dp else 0.5.dp,
                                                color = if (isHighlighted) amberColor else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                rows = r + 1
                                                cols = c + 1
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. VISUAL STYLE CARDS (Header / Zebra Toggles)
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
                            hasHeaderRow = !hasHeaderRow
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasHeaderRow) amberColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = if (hasHeaderRow) 2.dp else 1.dp,
                        color = if (hasHeaderRow) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                .background(if (hasHeaderRow) amberColor else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewHeadline,
                                contentDescription = null,
                                tint = if (hasHeaderRow) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Encabezado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasHeaderRow) amberColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasHeaderRow) "Activado" else "Desactivado",
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
                            hasZebraStripes = !hasZebraStripes
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasZebraStripes) amberColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = if (hasZebraStripes) 2.dp else 1.dp,
                        color = if (hasZebraStripes) amberColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                .background(if (hasZebraStripes) amberColor else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewStream,
                                contentDescription = null,
                                tint = if (hasZebraStripes) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Filas Zebra",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasZebraStripes) amberColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasZebraStripes) "Activado" else "Desactivado",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. STEPPER NUMERIC CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Filas: $rows", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { if (rows > 1) rows-- },
                        enabled = rows > 1,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { if (rows < 10) rows++ },
                        enabled = rows < 10,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Columnas: $cols", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { if (cols > 1) cols-- },
                        enabled = cols > 1,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { if (cols < 6) cols++ },
                        enabled = cols < 6,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 6. ACTION CTAS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCreateTable(rows, cols, hasHeaderRow, hasZebraStripes)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Crear Tabla", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
