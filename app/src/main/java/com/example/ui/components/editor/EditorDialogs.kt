package com.example.ui.components.editor

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.model.PaperStyle
import com.example.ui.components.CreateTableBottomSheet
import com.example.ui.components.EditorColorPickerDialog
import com.example.ui.components.EditorFontSizeCarouselSheet
import com.example.ui.components.EditorPaperStyleDialog
import com.example.ui.components.EditorReminderBottomSheet
import com.example.ui.components.EditorScannedPdfNameDialog
import com.example.ui.components.EditorStatsBottomSheet
import com.example.ui.components.InsertInlineMathDialog

@Composable
fun EditorDialogs(
    showFontSizeCarouselSheet: Boolean,
    customFontSizeSp: Int,
    onFontSizeChange: (Int) -> Unit,
    onDismissFontSizeCarouselSheet: () -> Unit,
    showCreateTableDialog: Boolean,
    createTableRows: Int,
    createTableCols: Int,
    onCreateTable: (rows: Int, cols: Int, header: Boolean, zebra: Boolean) -> Unit,
    onDismissCreateTable: () -> Unit,
    showReminderDialog: Boolean,
    reminderTimestamp: Long?,
    onSaveReminder: (Long, List<Int>) -> Unit,
    onDeleteReminder: () -> Unit,
    onDismissReminder: () -> Unit,
    showColorDialog: Boolean,
    colorIndex: Int,
    onColorSelected: (Int) -> Unit,
    onDismissColorDialog: () -> Unit,
    showPaperDialog: Boolean,
    paperStyle: PaperStyle,
    onStyleSelected: (PaperStyle) -> Unit,
    onDismissPaperDialog: () -> Unit,
    showStatsDialog: Boolean,
    blocks: List<NoteBlock>,
    title: String,
    createdAt: Long,
    updatedAt: Long,
    onDismissStatsDialog: () -> Unit,
    pendingScannedPdfUri: Uri?,
    scannedPdfName: String,
    onScannedPdfNameChange: (String) -> Unit,
    onConfirmScannedPdf: (Uri, String) -> Unit,
    onDismissScannedPdf: () -> Unit,
    showInlineMathDialog: Boolean,
    inlineMathDialogInitialCode: String,
    inlineMathDialogIsEditing: Boolean,
    onDismissInlineMath: () -> Unit,
    onConfirmInlineMath: (String) -> Unit,
    onDeleteInlineMath: (() -> Unit)?,
    showExportPdfChoiceDialog: Boolean,
    onDismissExportPdfChoice: () -> Unit,
    onExportCompletePdf: () -> Unit,
    onExportCalculationSheetPdf: () -> Unit,
    isDark: Boolean,
    isPaperDark: Boolean,
    amber: Color
) {
    EditorFontSizeCarouselSheet(
        showFontSizeCarouselSheet = showFontSizeCarouselSheet,
        customFontSizeSp = customFontSizeSp,
        isPaperDark = isPaperDark,
        isDark = isDark,
        amber = amber,
        onFontSizeChange = onFontSizeChange,
        onDismissRequest = onDismissFontSizeCarouselSheet
    )

    if (showCreateTableDialog) {
        CreateTableBottomSheet(
            initialRows = createTableRows,
            initialCols = createTableCols,
            onCreateTable = onCreateTable,
            onDismiss = onDismissCreateTable
        )
    }

    EditorReminderBottomSheet(
        showReminderDialog = showReminderDialog,
        reminderTimestamp = reminderTimestamp,
        isDark = isDark,
        amber = amber,
        onSaveReminder = onSaveReminder,
        onDeleteReminder = onDeleteReminder,
        onDismissRequest = onDismissReminder
    )

    EditorColorPickerDialog(
        showColorDialog = showColorDialog,
        colorIndex = colorIndex,
        isDark = isDark,
        amber = amber,
        onColorSelected = onColorSelected,
        onDismissRequest = onDismissColorDialog
    )

    EditorPaperStyleDialog(
        showPaperDialog = showPaperDialog,
        paperStyle = paperStyle,
        amber = amber,
        onStyleSelected = onStyleSelected,
        onDismissRequest = onDismissPaperDialog
    )

    EditorStatsBottomSheet(
        showStatsDialog = showStatsDialog,
        blocks = blocks,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPaperDark = isPaperDark,
        isDark = isDark,
        amber = amber,
        onDismissRequest = onDismissStatsDialog
    )

    EditorScannedPdfNameDialog(
        pdfUri = pendingScannedPdfUri,
        scannedPdfName = scannedPdfName,
        onNameChange = onScannedPdfNameChange,
        onConfirm = onConfirmScannedPdf,
        onDismissRequest = onDismissScannedPdf
    )

    if (showInlineMathDialog) {
        InsertInlineMathDialog(
            initialCode = inlineMathDialogInitialCode,
            isEditing = inlineMathDialogIsEditing,
            isDark = isPaperDark,
            onDismiss = onDismissInlineMath,
            onConfirm = onConfirmInlineMath,
            onDelete = onDeleteInlineMath
        )
    }

    if (showExportPdfChoiceDialog) {
        ExportPdfChoiceDialog(
            amber = amber,
            onDismiss = onDismissExportPdfChoice,
            onExportComplete = onExportCompletePdf,
            onExportCalculationSheet = onExportCalculationSheetPdf
        )
    }
}

@Composable
fun ExportPdfChoiceDialog(
    amber: Color,
    onDismiss: () -> Unit,
    onExportComplete: () -> Unit,
    onExportCalculationSheet: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Exportar PDF (PdfBox)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "¿Qué formato de documento PDF deseas generar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, amber.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportComplete() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = amber)
                        Column {
                            Text("Documento Completo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Exporta toda la nota con texto, fórmulas y cuadrículas.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, amber.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportCalculationSheet() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = amber)
                        Column {
                            Text("Hoja de Cálculo / Tabla", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Exporta exclusivamente la cuadrícula con formato de cálculo.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
