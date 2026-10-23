@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onInlineMathClick
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.sanitizeTextFieldValue
import com.example.util.MathEvaluator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.TextRange
import com.example.ui.components.RichLinkPreviewList
import com.example.ui.components.RichLinkQuickPreviewDialog
import com.example.ui.components.onWebLinkClick

@Composable
fun CalloutBlockItem(
    block: NoteBlock.CalloutBox,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onColorChanged: (String?) -> Unit,
    onDelete: () -> Unit,
    onNoteLinkClick: (Long) -> Unit,
    onInlineMathClick: (code: String, start: Int, end: Int) -> Unit,
    evaluateNoteLinkTrigger: (String, TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    amber: Color,
    paperTextColor: Color,
    paperHintColor: Color,
    isPaperDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showCalloutMenu by remember { mutableStateOf(value = false) }
    var showCalloutColorPicker by remember { mutableStateOf(value = false) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }
    var linkModeRefreshTrigger by remember { mutableIntStateOf(0) }

    val calloutBg = if (!block.backgroundColorHex.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(block.backgroundColorHex)) }.getOrDefault(
            if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        )
    } else {
        if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("block_callout_$index")
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showCalloutMenu = true
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            color = calloutBg,
            border = BorderStroke(1.dp, amber.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(amber, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = tfv,
                    onValueChange = { newValue ->
                        val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, tfv)
                        val effective = solved ?: newValue
                        val sanitized = sanitizeTextFieldValue(effective)
                        evaluateNoteLinkTrigger(block.id, sanitized)
                        onValueChange(sanitized)
                        coroutineScope.launch {
                            val layout = textLayoutResult
                            val cursorOffset = sanitized.selection.max
                            if ((layout != null) && (cursorOffset in 0..layout.layoutInput.text.length)) {
                                val cursorRect = layout.getCursorRect(cursorOffset)
                                val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                bringIntoViewRequester.bringIntoView(expandedRect)
                            } else {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onNoteLinkClick(textLayoutResult) { linkedId -> onNoteLinkClick(linkedId) }
                        .onWebLinkClick(textLayoutResult) { webUrl -> previewingWebUrl = webUrl }
                        .onInlineMathClick(textLayoutResult) { code, start, end -> onInlineMathClick(code, start, end) }
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            onFocusChanged(focusState.isFocused)
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    delay(100.milliseconds)
                                    val layout = textLayoutResult
                                    val cursorOffset = tfv.selection.max
                                    if ((layout != null) && (cursorOffset in 0..layout.layoutInput.text.length)) {
                                        val cursorRect = layout.getCursorRect(cursorOffset)
                                        val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                        bringIntoViewRequester.bringIntoView(expandedRect)
                                    } else {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        color = paperTextColor,
                        fontStyle = FontStyle.Italic
                    ),
                    cursorBrush = SolidColor(amber),
                    visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                    decorationBox = { innerTextField ->
                        if (block.content.isEmpty()) {
                            Text(
                                text = "Cuadro destacado / Nota importante...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    color = paperHintColor.copy(alpha = 0.5f),
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                        innerTextField()
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    )
                )
            }
        }

        RichLinkPreviewList(
            text = tfv.text,
            amber = amber,
            isDark = isPaperDark,
            externalRefreshTrigger = linkModeRefreshTrigger,
            onConvertToInlineLink = { link, cleanTitle ->
                val raw = tfv.text
                val safeStart = link.startIndex.coerceIn(0, raw.length)
                val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                val markdownLink = "[$cleanTitle](${link.url})"
                val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                onValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
            }
        )

        previewingWebUrl?.let { targetUrl ->
            RichLinkQuickPreviewDialog(
                url = targetUrl,
                blockText = tfv.text,
                amber = amber,
                isDark = isPaperDark,
                onDismiss = { previewingWebUrl = null },
                onModeChanged = { linkModeRefreshTrigger++ },
                onConvertRawUrlToInlineTitle = { link, cleanTitle ->
                    val raw = tfv.text
                    val safeStart = link.startIndex.coerceIn(0, raw.length)
                    val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                    val markdownLink = "[$cleanTitle](${link.url})"
                    val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                    onValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
                }
            )
        }
    }

    if (showCalloutMenu) {
        AlertDialog(
            onDismissRequest = { showCalloutMenu = false },
            title = { Text("Opciones de cuadro destacado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showCalloutMenu = false
                            showCalloutColorPicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎨 Cambiar color de resalte", color = amber)
                    }
                    TextButton(
                        onClick = {
                            showCalloutMenu = false
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_delete_callout")
                    ) {
                        Text("🗑️ Eliminar cuadro destacado", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalloutMenu = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showCalloutColorPicker) {
        AlertDialog(
            onDismissRequest = { showCalloutColorPicker = false },
            title = { Text("Seleccionar color de resalte") },
            text = {
                val calloutPalette = listOf(
                    "Gris neutro" to null,
                    "Amarillo pastel" to "#FEF3C7",
                    "Azul claro" to "#DBEAFE",
                    "Verde menta" to "#D1FAE5",
                    "Rosa pastel" to "#FCE7F3"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    calloutPalette.forEach { (name, hex) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorChanged(hex)
                                    showCalloutColorPicker = false
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(12.dp),
                                color = if (hex != null) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalloutColorPicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
