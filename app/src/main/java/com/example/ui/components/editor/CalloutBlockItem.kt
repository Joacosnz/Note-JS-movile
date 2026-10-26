@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.border
import androidx.compose.ui.text.TextRange
import com.example.ui.components.RichLinkPreviewList
import com.example.ui.components.RichLinkQuickPreviewDialog
import com.example.ui.components.onWebLinkClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalloutBlockItem(
    block: NoteBlock.CalloutBox,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onBlockUpdated: (NoteBlock.CalloutBox) -> Unit,
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
    var showCalloutMenu by remember { mutableStateOf(false) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }
    var linkModeRefreshTrigger by remember { mutableIntStateOf(0) }

    val calloutBg = if (!block.backgroundColorHex.isNullOrBlank()) {
        runCatching { Color(android.graphics.Color.parseColor(block.backgroundColorHex)) }.getOrDefault(
            if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        )
    } else {
        if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    }

    val calloutFontFamily = when (block.fontType) {
        "Serif" -> FontFamily.Serif
        "Mono" -> FontFamily.Monospace
        "Cursiva" -> FontFamily.Cursive
        else -> FontFamily.Default
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
            color = calloutBg.copy(alpha = 0.15f), // Soft background tint
            border = BorderStroke(1.dp, calloutBg.copy(alpha = 0.4f)) // Subtle border
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Border/Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp) // Minimum height
                        .background(calloutBg, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Emoji (if present)
                if (block.emoji != null) {
                    Text(
                        text = block.emoji!!,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                    )
                }

                // Text
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
                        fontFamily = calloutFontFamily,
                        fontStyle = if (block.fontType == "Estándar") FontStyle.Italic else FontStyle.Normal
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
                                    fontFamily = calloutFontFamily,
                                    fontStyle = if (block.fontType == "Estándar") FontStyle.Italic else FontStyle.Normal
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
            },
            onInsertWebClip = { clipText ->
                val current = tfv.text.trimEnd()
                val updated = if (current.isEmpty()) clipText else "$current\n$clipText"
                onValueChange(TextFieldValue(updated, TextRange(updated.length)))
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
                },
                onInsertWebClip = { clipText ->
                    val current = tfv.text.trimEnd()
                    val updated = if (current.isEmpty()) clipText else "$current\n$clipText"
                    onValueChange(TextFieldValue(updated, TextRange(updated.length)))
                }
            )
        }
    }

    if (showCalloutMenu) {
        Dialog(onDismissRequest = { showCalloutMenu = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2C2C2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Opciones de cuadro destacado",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )

                    // Section 1: Emoji
                var showEmojiDialog by remember { mutableStateOf(false) }

                if (showEmojiDialog) {
                    com.example.ui.components.NativeEmojiInputDialog(
                        onDismiss = { showEmojiDialog = false },
                        onEmojiSelected = { emoji ->
                            onBlockUpdated(block.copy(emoji = emoji))
                            showEmojiDialog = false
                        },
                        onRemoveIcon = {
                            onBlockUpdated(block.copy(emoji = null))
                            showEmojiDialog = false
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Cambiar emoji / ícono",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { showEmojiDialog = true }) {
                        Text(
                            text = block.emoji ?: "🚫 Elegir",
                            fontSize = 24.sp
                        )
                    }
                    }

                    // Section 2: Font
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "🔤 Cambiar tipografía",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        val fonts = listOf("Estándar", "Serif", "Mono", "Cursiva")
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fonts.forEach { font ->
                                val isSelected = block.fontType == font
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onBlockUpdated(block.copy(fontType = font))
                                    }
                                ) {
                                    Text(
                                        text = font,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Color Palette
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "🎨 Color de resalte y fondo",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.SemiBold
                        )
                        val colors = listOf(
                            "#FDE047", // Amarillo
                            "#93C5FD", // Azul
                            "#86EFAC", // Verde
                            "#FCA5A5", // Rojo
                            "#D8B4FE", // Morado
                            "#D1D5DB"  // Gris
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            colors.forEach { hex ->
                                val isSelected = block.backgroundColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                        .then(if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                        .clip(CircleShape)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBlockUpdated(block.copy(backgroundColorHex = hex))
                                        }
                                )
                            }
                        }
                    }

                    // Section 4 & 5: Delete and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    showCalloutMenu = false
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDelete()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFE57373)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eliminar cuadro destacado",
                                color = Color(0xFFE57373),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        TextButton(onClick = { showCalloutMenu = false }) {
                            Text("Cerrar", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
