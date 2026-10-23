package com.example.ui.components.editor

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AudioBlock
import com.example.model.ImageBlock
import com.example.model.NoteBlock
import com.example.model.PdfBlock
import com.example.model.TextStyleType
import com.example.ui.components.AudioBlockItem
import com.example.ui.components.ImageBlockItem
import com.example.ui.components.InlineMathBlockItem
import com.example.ui.components.PdfBlockItem
import com.example.ui.components.RichLinkPreviewList
import com.example.ui.components.RichLinkQuickPreviewDialog
import com.example.ui.components.TableBlockItem
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onInlineMathClick
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.onWebLinkClick
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.screens.Stroke
import com.example.ui.screens.deserializeStrokesFromJson
import com.example.util.FileManager
import com.example.util.MathEvaluator
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorBlockList(
    blocks: List<NoteBlock>,
    listState: LazyListState,
    visibleBlocksWithIndex: List<Pair<Int, NoteBlock>>,
    h2SectionBlockIds: Set<String>,
    focusRequesters: MutableMap<String, FocusRequester>,
    blockTextFieldValues: MutableMap<String, TextFieldValue>,
    targetFocusBlockId: String?,
    focusedBlockId: String?,
    onFocusedBlockIdChange: (String?) -> Unit,
    onRequestFocus: (String) -> Unit = {},
    formattedHeaderDate: String,
    reminderTimestamp: Long?,
    onReminderClick: () -> Unit,
    onDeleteReminder: () -> Unit,
    titleTextFieldValue: TextFieldValue,
    onTitleValueChange: (TextFieldValue) -> Unit,
    onTitleNext: () -> Unit,
    onNoteLinkClick: (Long) -> Unit,
    onInlineMathClick: (targetBlockId: String, code: String, isEditing: Boolean, range: TextRange?) -> Unit,
    onUpdateBlock: (Int, NoteBlock) -> Unit,
    onRemoveBlock: (Int) -> Unit,
    onInsertBlockAt: (Int, NoteBlock) -> Unit,
    onDrawingClick: () -> Unit,
    onExportTablePdf: (NoteBlock.Table) -> Unit,
    onOpenPdfFullScreen: (String) -> Unit,
    attachedImageUri: Uri?,
    onRemoveAttachedImage: () -> Unit,
    onAddBodyBlockBelow: () -> Unit,
    evaluateNoteLinkTrigger: (String, TextFieldValue) -> Unit,
    amber: Color,
    paperTextColor: Color,
    paperHintColor: Color,
    isPaperDark: Boolean,
    customFontSizeSp: Int,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isHighlightActive: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 0.dp,
            bottom = 90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header date & reminder & title
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedHeaderDate,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = paperHintColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (reminderTimestamp != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_chip")
                        .clickable { onReminderClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A365D).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF1A365D).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF1A365D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val formattedReminder = remember(reminderTimestamp) {
                                val sdf = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
                                sdf.format(Date(reminderTimestamp))
                            }
                            Text(
                                text = "⏰ Recordatorio: $formattedReminder",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF1A365D)
                            )
                        }

                        TextButton(
                            onClick = onDeleteReminder
                        ) {
                            Text(text = "Eliminar", color = Color(0xFF1A365D), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title input
            val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
            var titleTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            var titlePreviewingWebUrl by remember { mutableStateOf<String?>(null) }
            var titleLinkModeRefreshTrigger by remember { mutableIntStateOf(0) }
            BasicTextField(
                value = titleTextFieldValue,
                onValueChange = { newValue ->
                    val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newValue, titleTextFieldValue)
                    val finalValue = com.example.ui.components.sanitizeTextFieldValue(solved ?: newValue)
                    onTitleValueChange(finalValue)
                    evaluateNoteLinkTrigger("TITLE", finalValue)
                    coroutineScope.launch {
                        val layout = titleTextLayoutResult
                        if (layout != null) {
                            val cursorOffset = finalValue.selection.max
                            if (cursorOffset in 0..layout.layoutInput.text.length) {
                                val cursorRect = layout.getCursorRect(cursorOffset)
                                val expandedRect = cursorRect.copy(bottom = cursorRect.bottom + 40f)
                                titleBringIntoViewRequester.bringIntoView(expandedRect)
                            } else {
                                titleBringIntoViewRequester.bringIntoView()
                            }
                        } else {
                            titleBringIntoViewRequester.bringIntoView()
                        }
                    }
                },
                onTextLayout = { titleTextLayoutResult = it },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = paperTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp
                ),
                cursorBrush = SolidColor(amber),
                visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onTitleNext() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onNoteLinkClick(titleTextLayoutResult) { linkedId ->
                        onNoteLinkClick(linkedId)
                    }
                    .onWebLinkClick(titleTextLayoutResult) { webUrl ->
                        titlePreviewingWebUrl = webUrl
                    }
                    .onInlineMathClick(titleTextLayoutResult) { code, start, end ->
                        onInlineMathClick("TITLE", code, true, TextRange(start, end))
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocusedBlockIdChange("TITLE")
                        }
                    }
                    .bringIntoViewRequester(titleBringIntoViewRequester)
                    .testTag("notejs_editor_title"),
                decorationBox = { innerTextField ->
                    if (titleTextFieldValue.text.isEmpty()) {
                        Text(
                            text = "Título",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = paperHintColor.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            com.example.ui.components.InlineMathPreviewRow(
                text = titleTextFieldValue.text,
                isDark = isPaperDark,
                amber = amber,
                onEditInlineMath = { code, start, end ->
                    onInlineMathClick("TITLE", code, true, TextRange(start, end))
                }
            )

            RichLinkPreviewList(
                text = titleTextFieldValue.text,
                amber = amber,
                isDark = isPaperDark,
                externalRefreshTrigger = titleLinkModeRefreshTrigger,
                onConvertToInlineLink = { link, cleanTitle ->
                    val raw = titleTextFieldValue.text
                    val safeStart = link.startIndex.coerceIn(0, raw.length)
                    val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                    val markdownLink = "[$cleanTitle](${link.url})"
                    val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                    onTitleValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
                }
            )

            titlePreviewingWebUrl?.let { targetUrl ->
                RichLinkQuickPreviewDialog(
                    url = targetUrl,
                    blockText = titleTextFieldValue.text,
                    amber = amber,
                    isDark = isPaperDark,
                    onDismiss = { titlePreviewingWebUrl = null },
                    onModeChanged = { titleLinkModeRefreshTrigger++ },
                    onConvertRawUrlToInlineTitle = { link, cleanTitle ->
                        val raw = titleTextFieldValue.text
                        val safeStart = link.startIndex.coerceIn(0, raw.length)
                        val safeEnd = link.endIndex.coerceIn(safeStart, raw.length)
                        val markdownLink = "[$cleanTitle](${link.url})"
                        val updated = raw.substring(0, safeStart) + markdownLink + raw.substring(safeEnd)
                        onTitleValueChange(TextFieldValue(updated, TextRange(safeStart + markdownLink.length)))
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Mixed-Content Blocks Items
        itemsIndexed(visibleBlocksWithIndex, key = { _, pair -> pair.second.id }) { _, (index, block) ->
            val focusRequester = remember(block.id) { FocusRequester() }
            focusRequesters[block.id] = focusRequester

            val blockIndent = if (block.id in h2SectionBlockIds) 28.dp else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = blockIndent)
            ) {
                when (block) {
                    is NoteBlock.Text -> {
                        val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                        TextBlockItem(
                            block = block,
                            index = index,
                            tfv = tfv,
                            onValueChange = { sanitized ->
                                blockTextFieldValues[block.id] = sanitized
                                val trimmed = sanitized.text.trim()
                                if (trimmed == "$$" || trimmed == "/math" || trimmed == "/equation") {
                                    // Notion trigger: typing $$ or /math converts text block into a Notion Block Equation
                                    onUpdateBlock(index, NoteBlock.Math(id = block.id, latexCode = ""))
                                    onRequestFocus(block.id)
                                } else if (sanitized.text != block.content) {
                                    val nextStyle = if (block.content.isEmpty() && sanitized.text.isEmpty()) TextStyleType.BODY else block.style
                                    onUpdateBlock(index, block.copy(content = sanitized.text, style = nextStyle))
                                }
                            },
                            onEnterHeading = { headingText, remainingText ->
                                blockTextFieldValues[block.id] = TextFieldValue(headingText, TextRange(headingText.length))
                                onUpdateBlock(index, block.copy(content = headingText, isCollapsed = false))

                                val nextBlock = blocks.getOrNull(index + 1)
                                if (nextBlock is NoteBlock.Text && nextBlock.style == TextStyleType.BODY && nextBlock.isIndented) {
                                    val updatedContent = if (remainingText.isNotEmpty()) {
                                        if (nextBlock.content.isEmpty()) remainingText else "$remainingText\n${nextBlock.content}"
                                    } else nextBlock.content
                                    onUpdateBlock(index + 1, nextBlock.copy(content = updatedContent))
                                    blockTextFieldValues[nextBlock.id] = TextFieldValue(updatedContent, TextRange(remainingText.length))
                                    onRequestFocus(nextBlock.id)
                                } else {
                                    val newBodyId = UUID.randomUUID().toString()
                                    val newBodyBlock = NoteBlock.Text(id = newBodyId, content = remainingText, style = TextStyleType.BODY, isIndented = true)
                                    onInsertBlockAt(index + 1, newBodyBlock)
                                    blockTextFieldValues[newBodyId] = TextFieldValue(remainingText, TextRange(remainingText.length))
                                    onRequestFocus(newBodyId)
                                }
                            },
                            onDoubleNewlineIndented = { trimmedText ->
                                onUpdateBlock(index, block.copy(content = trimmedText))
                                blockTextFieldValues[block.id] = TextFieldValue(trimmedText, TextRange(trimmedText.length))

                                val newBodyId = UUID.randomUUID().toString()
                                val newBodyBlock = NoteBlock.Text(id = newBodyId, content = "", style = TextStyleType.BODY, isIndented = false)
                                onInsertBlockAt(index + 1, newBodyBlock)
                                blockTextFieldValues[newBodyId] = TextFieldValue("", TextRange(0))
                                onRequestFocus(newBodyId)
                            },
                            onToggleCollapse = {
                                onUpdateBlock(index, block.copy(isCollapsed = !block.isCollapsed))
                            },
                            onEmptyHeadingConvert = {
                                val updated = block.copy(style = TextStyleType.BODY, isCollapsed = false, isIndented = false)
                                onUpdateBlock(index, updated)
                                blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                onRequestFocus(block.id)
                            },
                            onNoteLinkClick = onNoteLinkClick,
                            onInlineMathClick = { code, start, end ->
                                onInlineMathClick(block.id, code, true, TextRange(start, end))
                            },
                            evaluateNoteLinkTrigger = evaluateNoteLinkTrigger,
                            focusRequester = focusRequester,
                            onFocusChanged = { isFocused ->
                                if (isFocused) onFocusedBlockIdChange(block.id)
                            },
                            amber = amber,
                            paperTextColor = paperTextColor,
                            paperHintColor = paperHintColor,
                            isPaperDark = isPaperDark,
                            customFontSizeSp = customFontSizeSp,
                            isBoldActive = isBoldActive,
                            isItalicActive = isItalicActive,
                            isHighlightActive = isHighlightActive,
                            showEmptyPlaceholder = blocks.size <= 1 && title.isEmpty()
                        )
                    }

                    is NoteBlock.Checklist -> {
                        val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                        ChecklistBlockItem(
                            block = block,
                            index = index,
                            tfv = tfv,
                            onValueChange = { sanitized ->
                                blockTextFieldValues[block.id] = sanitized
                                if (sanitized.text != block.content) {
                                    onUpdateBlock(index, block.copy(content = sanitized.text))
                                }
                            },
                            onToggleCheck = {
                                val newChecked = !block.isChecked
                                onUpdateBlock(index, block.copy(isChecked = newChecked, isReminderDone = newChecked))
                            },
                            onScheduleReminder = { timestamp, priority ->
                                onUpdateBlock(
                                    index,
                                    block.copy(
                                        reminderTimestamp = timestamp,
                                        priority = priority,
                                        isReminderDone = if (timestamp == null) false else block.isChecked
                                    )
                                )
                            },
                            onNext = {
                                if (block.content.isBlank()) {
                                    onUpdateBlock(index, NoteBlock.Text(id = block.id, content = ""))
                                    blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(block.id)
                                } else {
                                    val newId = UUID.randomUUID().toString()
                                    onInsertBlockAt(index + 1, NoteBlock.Checklist(id = newId, content = ""))
                                    blockTextFieldValues[newId] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(newId)
                                }
                            },
                            onNextWithText = { remainingText ->
                                if (block.content.isBlank() && remainingText.isBlank()) {
                                    onUpdateBlock(index, NoteBlock.Text(id = block.id, content = ""))
                                    blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(block.id)
                                } else {
                                    val newId = UUID.randomUUID().toString()
                                    onInsertBlockAt(index + 1, NoteBlock.Checklist(id = newId, content = remainingText))
                                    blockTextFieldValues[newId] = TextFieldValue(remainingText, TextRange(0))
                                    onRequestFocus(newId)
                                }
                            },
                            onDelete = { onRemoveBlock(index) },
                            onNoteLinkClick = onNoteLinkClick,
                            onInlineMathClick = { code, start, end ->
                                onInlineMathClick(block.id, code, true, TextRange(start, end))
                            },
                            evaluateNoteLinkTrigger = evaluateNoteLinkTrigger,
                            focusRequester = focusRequester,
                            onFocusChanged = { isFocused ->
                                if (isFocused) onFocusedBlockIdChange(block.id)
                            },
                            amber = amber,
                            paperTextColor = paperTextColor,
                            paperHintColor = paperHintColor,
                            isPaperDark = isPaperDark,
                            customFontSizeSp = customFontSizeSp,
                            isBoldActive = isBoldActive,
                            isItalicActive = isItalicActive,
                            isHighlightActive = isHighlightActive
                        )
                    }

                    is NoteBlock.SimpleList -> {
                        val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                        SimpleListBlockItem(
                            block = block,
                            index = index,
                            tfv = tfv,
                            onValueChange = { sanitized ->
                                blockTextFieldValues[block.id] = sanitized
                                if (sanitized.text != block.content) {
                                    onUpdateBlock(index, block.copy(content = sanitized.text))
                                }
                            },
                            onNext = {
                                if (block.content.isBlank()) {
                                    onUpdateBlock(index, NoteBlock.Text(id = block.id, content = ""))
                                    blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(block.id)
                                } else {
                                    val nextNumber = if (block.isNumbered) block.number + 1 else 1
                                    val newId = UUID.randomUUID().toString()
                                    onInsertBlockAt(index + 1, NoteBlock.SimpleList(id = newId, isNumbered = block.isNumbered, number = nextNumber, content = ""))
                                    blockTextFieldValues[newId] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(newId)
                                }
                            },
                            onNextWithText = { remainingText ->
                                if (block.content.isBlank() && remainingText.isBlank()) {
                                    onUpdateBlock(index, NoteBlock.Text(id = block.id, content = ""))
                                    blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                    onRequestFocus(block.id)
                                } else {
                                    val nextNumber = if (block.isNumbered) block.number + 1 else 1
                                    val newId = UUID.randomUUID().toString()
                                    onInsertBlockAt(index + 1, NoteBlock.SimpleList(id = newId, isNumbered = block.isNumbered, number = nextNumber, content = remainingText))
                                    blockTextFieldValues[newId] = TextFieldValue(remainingText, TextRange(0))
                                    onRequestFocus(newId)
                                }
                            },
                            onNoteLinkClick = onNoteLinkClick,
                            onInlineMathClick = { code, start, end ->
                                onInlineMathClick(block.id, code, true, TextRange(start, end))
                            },
                            evaluateNoteLinkTrigger = evaluateNoteLinkTrigger,
                            focusRequester = focusRequester,
                            onFocusChanged = { isFocused ->
                                if (isFocused) onFocusedBlockIdChange(block.id)
                            },
                            amber = amber,
                            paperTextColor = paperTextColor,
                            paperHintColor = paperHintColor,
                            isPaperDark = isPaperDark,
                            customFontSizeSp = customFontSizeSp,
                            isBoldActive = isBoldActive,
                            isItalicActive = isItalicActive,
                            isHighlightActive = isHighlightActive
                        )
                    }

                    is NoteBlock.CalloutBox -> {
                        val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                        CalloutBlockItem(
                            block = block,
                            index = index,
                            tfv = tfv,
                            onValueChange = { sanitized ->
                                blockTextFieldValues[block.id] = sanitized
                                if (sanitized.text != block.content) {
                                    onUpdateBlock(index, block.copy(content = sanitized.text))
                                }
                            },
                            onColorChanged = { hex ->
                                onUpdateBlock(index, block.copy(backgroundColorHex = hex))
                            },
                            onDelete = { onRemoveBlock(index) },
                            onNoteLinkClick = onNoteLinkClick,
                            onInlineMathClick = { code, start, end ->
                                onInlineMathClick(block.id, code, true, TextRange(start, end))
                            },
                            evaluateNoteLinkTrigger = evaluateNoteLinkTrigger,
                            focusRequester = focusRequester,
                            onFocusChanged = { isFocused ->
                                if (isFocused) onFocusedBlockIdChange(block.id)
                            },
                            amber = amber,
                            paperTextColor = paperTextColor,
                            paperHintColor = paperHintColor,
                            isPaperDark = isPaperDark
                        )
                    }

                    is NoteBlock.Drawing -> {
                        DrawingBlockItem(
                            block = block,
                            amber = amber,
                            isPaperDark = isPaperDark,
                            paperTextColor = paperTextColor,
                            onDrawingClick = onDrawingClick
                        )
                    }

                    is NoteBlock.Table -> {
                        TableBlockItem(
                            block = block,
                            onUpdateBlock = { updatedTable ->
                                onUpdateBlock(index, updatedTable)
                            },
                            onDeleteBlock = {
                                onRemoveBlock(index)
                            },
                            isPaperDark = isPaperDark,
                            onExportTablePdf = { onExportTablePdf(block) },
                            modifier = Modifier.testTag("table_block_$index")
                        )
                    }

                    is PdfBlock -> {
                        PdfBlockItem(
                            block = block,
                            onPageChange = { newIndex ->
                                onUpdateBlock(index, block.copy(currentPageIndex = newIndex))
                            },
                            onToggleExpand = {
                                onUpdateBlock(index, block.copy(isExpanded = !block.isExpanded))
                            },
                            onDeleteBlock = {
                                onRemoveBlock(index)
                            },
                            onOpenFullScreen = {
                                onOpenPdfFullScreen(block.filePath)
                            },
                            modifier = Modifier.testTag("block_pdf_$index")
                        )
                    }

                    is AudioBlock -> {
                        AudioBlockItem(
                            block = block,
                            onToggleTranscription = {
                                onUpdateBlock(index, block.copy(isTranscriptionVisible = !block.isTranscriptionVisible))
                            },
                            onDeleteBlock = {
                                val file = File(block.filePath)
                                if (file.exists()) file.delete()
                                onRemoveBlock(index)
                            },
                            modifier = Modifier.testTag("block_audio_$index")
                        )
                    }

                    is ImageBlock -> {
                        ImageBlockItem(
                            block = block,
                            onRemove = { onRemoveBlock(index) },
                            modifier = Modifier.testTag("block_image_$index")
                        )
                    }

                    is NoteBlock.Image -> {
                        ImageBlockItem(
                            block = block,
                            onDeleteBlock = { onRemoveBlock(index) },
                            modifier = Modifier.testTag("block_image_$index")
                        )
                    }

                    is NoteBlock.Math -> {
                        NotionMathBlockItem(
                            block = block,
                            onUpdateBlock = { updatedMath ->
                                onUpdateBlock(index, updatedMath)
                            },
                            onDeleteBlock = {
                                onRemoveBlock(index)
                            },
                            isDark = isPaperDark,
                            amber = amber,
                            modifier = Modifier.testTag("block_math_$index")
                        )
                    }
                }
            }
        }

        attachedImageUri?.let { uri ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Imagen adjunta",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = onRemoveAttachedImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar imagen",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

                        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 350.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onAddBodyBlockBelow()
                    }
            )
        }
    }
}

@Composable
private fun DrawingBlockItem(
    block: NoteBlock.Drawing,
    amber: Color,
    isPaperDark: Boolean,
    paperTextColor: Color,
    onDrawingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strokesState = produceState<List<Stroke>>(initialValue = emptyList(), key1 = block.filePath, key2 = block.pathData) {
        value = withContext(Dispatchers.IO) {
            val pathOrData = block.filePath.ifBlank { block.pathData }
            val json = FileManager.readDrawingData(pathOrData)
            deserializeStrokesFromJson(json)
        }
    }
    val strokes = strokesState.value

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(vertical = 6.dp)
            .clickable { onDrawingClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isPaperDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA),
        border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
    ) {
        if (strokes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gesture, contentDescription = null, tint = amber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Boceto a mano alzada (Toca para editar)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = paperTextColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    strokes.forEach { stroke ->
                        if (stroke.points.isNotEmpty()) {
                            val path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = if (stroke.isHighlighter) stroke.color.copy(alpha = 0.35f) else stroke.color,
                                style = DrawScopeStroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
