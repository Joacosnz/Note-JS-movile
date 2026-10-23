@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.ui.components.RichLinkPreviewList
import com.example.ui.components.RichLinkQuickPreviewDialog
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onInlineMathClick
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.onWebLinkClick
import com.example.ui.components.sanitizeTextFieldValue
import com.example.util.MathEvaluator
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChecklistBlockItem(
    block: NoteBlock.Checklist,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleCheck: () -> Unit,
    onNext: () -> Unit,
    onNextWithText: (remainingText: String) -> Unit = {},
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
    customFontSizeSp: Int,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isHighlightActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }
    var linkModeRefreshTrigger by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("block_checklist_$index"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (block.isChecked) amber else if (isPaperDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .background(if (block.isChecked) amber else Color.Transparent)
                    .clickable {
                        onToggleCheck()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (block.isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completado",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = tfv,
                onValueChange = { newValue ->
                    val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, tfv)
                    val effective = solved ?: newValue
                    val sanitized = sanitizeTextFieldValue(effective)
                    evaluateNoteLinkTrigger(block.id, sanitized)
                    if (sanitized.text.contains('\n')) {
                        val newlineIndex = sanitized.text.indexOf('\n')
                        val safeNL = newlineIndex.coerceIn(0, sanitized.text.length)
                        val firstPart = sanitized.text.substring(0, safeNL)
                        val remainingText = sanitized.text.substring((safeNL + 1).coerceIn(0, sanitized.text.length))

                        if (block.content.isEmpty() && firstPart.isEmpty() && remainingText.isEmpty()) {
                            onNext()
                        } else {
                            onValueChange(TextFieldValue(firstPart, TextRange(firstPart.length)))
                            onNextWithText(remainingText)
                        }
                    } else {
                        onValueChange(sanitized)
                        coroutineScope.launch {
                            val layout = textLayoutResult
                            val cursorOffset = sanitized.selection.max
                            if (layout != null && (cursorOffset in 0..layout.layoutInput.text.length)) {
                                val cursorRect = layout.getCursorRect(cursorOffset)
                                val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                bringIntoViewRequester.bringIntoView(expandedRect)
                            } else {
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                },
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier
                    .weight(1f)
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
                                if (layout != null && (cursorOffset in 0..layout.layoutInput.text.length)) {
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
                    fontSize = customFontSizeSp.sp,
                    fontWeight = if (isBoldActive) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal,
                    color = if (block.isChecked) paperHintColor else paperTextColor,
                    background = if (isHighlightActive) amber.copy(alpha = 0.35f) else Color.Transparent,
                    textDecoration = when {
                        block.isChecked -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    }
                ),
                cursorBrush = SolidColor(amber),
                visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        Text(
                            text = "Elemento de lista de tareas...",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = paperHintColor.copy(alpha = 0.5f))
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onNext() }
                )
            )
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = paperHintColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        RichLinkPreviewList(
            text = tfv.text,
            amber = amber,
            isDark = isPaperDark,
            externalRefreshTrigger = linkModeRefreshTrigger,
            modifier = Modifier.padding(start = 36.dp),
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
}

