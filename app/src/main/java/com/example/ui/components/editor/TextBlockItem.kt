@file:OptIn(ExperimentalFoundationApi::class)

package com.example.ui.components.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.model.TextStyleType
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
fun TextBlockItem(
    block: NoteBlock.Text,
    index: Int,
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onEnterHeading: (headingText: String, remainingText: String) -> Unit,
    onDoubleNewlineIndented: (trimmedText: String) -> Unit,
    onToggleCollapse: () -> Unit,
    onEmptyHeadingConvert: () -> Unit,
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
    showEmptyPlaceholder: Boolean,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }
    var linkModeRefreshTrigger by remember { mutableIntStateOf(0) }

    val baseFontSize = if (customFontSizeSp != 17) customFontSizeSp.sp else when (block.style) {
        TextStyleType.TITLE -> 22.sp
        TextStyleType.SUBTITLE -> 18.sp
        TextStyleType.BODY -> 17.sp
    }
    val baseFontWeight = if (isBoldActive) FontWeight.Bold else when (block.style) {
        TextStyleType.TITLE -> FontWeight.Bold
        TextStyleType.SUBTITLE -> FontWeight.SemiBold
        TextStyleType.BODY -> FontWeight.Normal
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = baseFontSize,
        fontWeight = baseFontWeight,
        fontStyle = if (isItalicActive) FontStyle.Italic else FontStyle.Normal,
        background = if (isHighlightActive) amber.copy(alpha = 0.35f) else Color.Transparent,
        lineHeight = (baseFontSize.value * 1.4f).sp,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        if (block.style == TextStyleType.SUBTITLE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(CircleShape)
                        .clickable { onToggleCollapse() }
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (block.isCollapsed) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (block.isCollapsed) "Expandir sección" else "Colapsar sección",
                        tint = amber,
                        modifier = Modifier.size(20.dp),
                    )
                }
                BasicTextField(
                    value = tfv,
                    onValueChange = { newValue ->
                        val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, tfv)
                        val effective = solved ?: newValue
                        val sanitized = sanitizeTextFieldValue(effective)
                        evaluateNoteLinkTrigger(block.id, sanitized)
                        if (sanitized.text.contains('\n')) {
                            if (block.content.isEmpty()) {
                                onEmptyHeadingConvert()
                            } else {
                                val newlineIndex = sanitized.text.indexOf('\n')
                                val safeNL = newlineIndex.coerceIn(0, sanitized.text.length)
                                val headingText = sanitized.text.substring(0, safeNL)
                                val remainingText = sanitized.text.substring((safeNL + 1).coerceIn(0, sanitized.text.length))
                                onEnterHeading(headingText, remainingText)
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
                        }
                        .testTag("block_text_$index"),
                    textStyle = textStyle.copy(color = paperTextColor),
                    cursorBrush = SolidColor(amber),
                    visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                    decorationBox = { innerTextField ->
                        if (block.content.isEmpty()) {
                            Text(
                                text = "Subtítulo...",
                                style = textStyle.copy(color = paperHintColor.copy(alpha = 0.5f)),
                            )
                        }
                        innerTextField()
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                )
            }
        } else {
            BasicTextField(
                value = tfv,
                onValueChange = { newValue ->
                    val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, tfv)
                    val effective = solved ?: newValue
                    val sanitized = sanitizeTextFieldValue(effective)
                    evaluateNoteLinkTrigger(block.id, sanitized)
                    if ((block.style == TextStyleType.TITLE) && sanitized.text.contains('\n')) {
                        if (block.content.isEmpty()) {
                            onEmptyHeadingConvert()
                        } else {
                            val newlineIndex = sanitized.text.indexOf('\n')
                            val safeNL = newlineIndex.coerceIn(0, sanitized.text.length)
                            val headingText = sanitized.text.substring(0, safeNL)
                            val remainingText = sanitized.text.substring((safeNL + 1).coerceIn(0, sanitized.text.length))
                            onEnterHeading(headingText, remainingText)
                        }
                    } else if (block.isIndented && sanitized.text.endsWith("\n\n")) {
                        val trimmedText = sanitized.text.dropLast(2)
                        onDoubleNewlineIndented(trimmedText)
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
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
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
                    }
                    .testTag("block_text_$index"),
                textStyle = textStyle.copy(color = paperTextColor),
                cursorBrush = SolidColor(amber),
                visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        val showPlaceholder = when (block.style) {
                            TextStyleType.TITLE -> "Título de sección..."
                            else -> if (showEmptyPlaceholder) "Escribe algo..." else ""
                        }
                        if (showPlaceholder.isNotEmpty()) {
                            Text(
                                text = showPlaceholder,
                                style = textStyle.copy(color = paperHintColor.copy(alpha = 0.5f)),
                            )
                        }
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            )
        }

        com.example.ui.components.InlineMathPreviewRow(
            text = tfv.text,
            isDark = isPaperDark,
            amber = amber,
            onEditInlineMath = { code, start, end ->
                onInlineMathClick(code, start, end)
            }
        )

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
}

