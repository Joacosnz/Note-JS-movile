package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

import androidx.compose.ui.window.Popup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.derivedStateOf
import com.example.ui.components.LiquidGlassContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.model.NoteBlock
import com.example.model.ImageBlock
import com.example.model.NoteColors
import com.example.model.NoteType
import com.example.model.PaperStyle
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.model.TableCell
import com.example.model.TextStyleType
import com.example.ui.components.NoteLockDialog
import com.example.ui.components.PaperBackground
import com.example.ui.components.AudioRecorderDialog
import com.example.ui.components.AudioBlockItem
import com.example.ui.components.PdfBlockItem
import com.example.ui.components.TableBlockItem
import com.example.ui.components.ImageBlockItem
import com.example.ui.components.CreateTableBottomSheet
import com.example.ui.components.REMINDER_OFFSET_OPTIONS
import androidx.core.content.FileProvider
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.util.ImageHelper
import com.example.util.NoteBlockSerializer
import com.example.util.NotePdfExporter
import com.example.util.isValidNoteForSync
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class ActiveHashtagInfo(
    val targetId: String,
    val textFieldValue: TextFieldValue,
    val hashIndex: Int,
    val rawQuery: String
)

private data class SmartTagItem(
    val tag: String,
    val count: Int,
    val isNew: Boolean
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    initialType: NoteType,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    initialOpenVoiceDictation: Boolean = false,
    initialOpenDocumentScanner: Boolean = false
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var titleTextFieldValue by remember { mutableStateOf(TextFieldValue(title)) }
    val blocks = remember { mutableStateListOf<NoteBlock>() }
    var noteType by remember { mutableStateOf(initialType) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var lockPin by remember { mutableStateOf("") }
    var reminderTimestamp by remember { mutableStateOf<Long?>(null) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }

    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var updatedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var targetFocusBlockId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val blockTextFieldValues = remember { mutableStateMapOf<String, TextFieldValue>() }
    val lazyListState = rememberLazyListState()

    val activeHashtagInfo by remember {
        derivedStateOf {
            val currentFocusId = focusedBlockId ?: return@derivedStateOf null
            val tfv = if (currentFocusId == "TITLE") {
                titleTextFieldValue
            } else {
                blockTextFieldValues[currentFocusId]
            } ?: return@derivedStateOf null

            val cursorIndex = tfv.selection.max
            val text = tfv.text

            if (cursorIndex < 0 || cursorIndex > text.length) return@derivedStateOf null
            val textBeforeCursor = text.substring(0, cursorIndex)
            val lastHashIndex = textBeforeCursor.lastIndexOf('#')
            if (lastHashIndex == -1) return@derivedStateOf null

            val queryCandidate = textBeforeCursor.substring(lastHashIndex + 1)
            if (queryCandidate.any { it.isWhitespace() || it == '\n' || it == ',' || it == '.' || it == '!' || it == '?' }) {
                return@derivedStateOf null
            }

            if (lastHashIndex > 0) {
                val prevChar = textBeforeCursor[lastHashIndex - 1]
                if (!prevChar.isWhitespace() && prevChar != '\n') {
                    return@derivedStateOf null
                }
            }

            ActiveHashtagInfo(
                targetId = currentFocusId,
                textFieldValue = tfv,
                hashIndex = lastHashIndex,
                rawQuery = queryCandidate
            )
        }
    }

    val suggestedSmartTags by remember(activeHashtagInfo?.rawQuery, uiState.availableTags, uiState.tagNoteCounts) {
        derivedStateOf {
            val info = activeHashtagInfo ?: return@derivedStateOf emptyList<SmartTagItem>()
            val query = info.rawQuery.trim().lowercase(Locale.getDefault())

            val allTags = uiState.availableTags
                .filter { it != "Todas" && it.isNotBlank() }
                .distinct()

            val items = mutableListOf<SmartTagItem>()

            if (query.isEmpty()) {
                val sortedByUsage = allTags.sortedByDescending { uiState.tagNoteCounts[it] ?: 0 }
                sortedByUsage.forEach { tag ->
                    val count = uiState.tagNoteCounts[tag] ?: 0
                    items.add(SmartTagItem(tag = tag, count = count, isNew = false))
                }
            } else {
                val matching = allTags.filter { it.lowercase(Locale.getDefault()).contains(query) }
                    .sortedWith(
                        compareByDescending<String> { it.lowercase(Locale.getDefault()).startsWith(query) }
                            .thenByDescending { uiState.tagNoteCounts[it] ?: 0 }
                            .thenBy { it }
                    )

                matching.forEach { tag ->
                    val count = uiState.tagNoteCounts[tag] ?: 0
                    items.add(SmartTagItem(tag = tag, count = count, isNew = false))
                }

                val exactMatchExists = allTags.any { it.equals(query, ignoreCase = true) }
                if (!exactMatchExists && info.rawQuery.isNotBlank()) {
                    items.add(0, SmartTagItem(tag = info.rawQuery, count = 0, isNew = true))
                }
            }

            items
        }
    }

    fun applySmartTagSelection(item: SmartTagItem) {
        val info = activeHashtagInfo ?: return
        val tfv = info.textFieldValue
        val text = tfv.text
        val cursorIndex = tfv.selection.max.coerceIn(0, text.length)

        val cleanTag = item.tag.trim().removePrefix("#")
        if (cleanTag.isEmpty()) return

        val replacement = "#$cleanTag "
        val beforeHash = text.substring(0, info.hashIndex)
        val afterCursor = text.substring(cursorIndex)

        val newText = beforeHash + replacement + afterCursor
        val newCursor = info.hashIndex + replacement.length
        val newTfv = TextFieldValue(text = newText, selection = TextRange(newCursor))

        if (info.targetId == "TITLE") {
            titleTextFieldValue = newTfv
            title = newText
        } else {
            blockTextFieldValues[info.targetId] = newTfv
            val idx = blocks.indexOfFirst { it.id == info.targetId }
            if (idx != -1) {
                when (val b = blocks[idx]) {
                    is NoteBlock.Text -> blocks[idx] = b.copy(content = newText)
                    is NoteBlock.Checklist -> blocks[idx] = b.copy(content = newText)
                    is NoteBlock.SimpleList -> blocks[idx] = b.copy(content = newText)
                    is NoteBlock.CalloutBox -> blocks[idx] = b.copy(content = newText)
                    else -> {}
                }
            }
        }
    }

    val hiddenBlockIds by remember {
        derivedStateOf {
            val hidden = mutableSetOf<String>()
            var isCollapsing = false
            for (b in blocks) {
                if (b is NoteBlock.Text) {
                    if (b.style == TextStyleType.TITLE) {
                        isCollapsing = false
                    } else if (b.style == TextStyleType.SUBTITLE) {
                        isCollapsing = b.isCollapsed
                        continue
                    } else if (b.style == TextStyleType.BODY && !b.isIndented) {
                        isCollapsing = false
                    }
                }
                if (isCollapsing) {
                    hidden.add(b.id)
                }
            }
            hidden
        }
    }

    val visibleBlocksWithIndex by remember {
        derivedStateOf {
            blocks.mapIndexed { index, block -> index to block }
                .filter { (_, block) -> block.id !in hiddenBlockIds }
        }
    }

    val h2SectionBlockIds by remember {
        derivedStateOf {
            val h2Ids = mutableSetOf<String>()
            var inH2Section = false
            for (b in blocks) {
                if (b is NoteBlock.Text) {
                    if (b.style == TextStyleType.TITLE) {
                        inH2Section = false
                    } else if (b.style == TextStyleType.SUBTITLE) {
                        inH2Section = true
                        continue
                    } else if (b.style == TextStyleType.BODY && !b.isIndented) {
                        inH2Section = false
                    }
                }
                if (inH2Section) {
                    h2Ids.add(b.id)
                }
            }
            h2Ids
        }
    }

    fun toggleOrInsertChecklist() {
        val targetIndex = if (focusedBlockId != null) {
            blocks.indexOfFirst { it.id == focusedBlockId }
        } else -1

        if (targetIndex != -1) {
            val currentBlock = blocks[targetIndex]
            when (currentBlock) {
                is NoteBlock.Text -> {
                    val checklistBlock = NoteBlock.Checklist(
                        id = currentBlock.id,
                        content = currentBlock.content,
                        isChecked = false
                    )
                    blocks[targetIndex] = checklistBlock
                    targetFocusBlockId = currentBlock.id
                }
                is NoteBlock.Checklist -> {
                    val textBlock = NoteBlock.Text(
                        id = currentBlock.id,
                        content = currentBlock.content,
                        style = TextStyleType.BODY
                    )
                    blocks[targetIndex] = textBlock
                    targetFocusBlockId = currentBlock.id
                }
                is NoteBlock.SimpleList -> {
                    val checklistBlock = NoteBlock.Checklist(
                        id = currentBlock.id,
                        content = currentBlock.content,
                        isChecked = false
                    )
                    blocks[targetIndex] = checklistBlock
                    targetFocusBlockId = currentBlock.id
                }
                is NoteBlock.CalloutBox -> {
                    val checklistBlock = NoteBlock.Checklist(
                        id = currentBlock.id,
                        content = currentBlock.content,
                        isChecked = false
                    )
                    blocks[targetIndex] = checklistBlock
                    targetFocusBlockId = currentBlock.id
                }
                else -> {
                    val newId = UUID.randomUUID().toString()
                    blocks.add(targetIndex + 1, NoteBlock.Checklist(id = newId))
                    targetFocusBlockId = newId
                    focusedBlockId = newId
                }
            }
        } else {
            if (blocks.isEmpty()) {
                val newId = UUID.randomUUID().toString()
                blocks.add(NoteBlock.Checklist(id = newId))
                targetFocusBlockId = newId
                focusedBlockId = newId
            } else {
                val lastIndex = blocks.lastIndex
                val lastBlock = blocks[lastIndex]
                when (lastBlock) {
                    is NoteBlock.Text -> {
                        blocks[lastIndex] = NoteBlock.Checklist(
                            id = lastBlock.id,
                            content = lastBlock.content,
                            isChecked = false
                        )
                        targetFocusBlockId = lastBlock.id
                        focusedBlockId = lastBlock.id
                    }
                    is NoteBlock.Checklist -> {
                        val textBlock = NoteBlock.Text(
                            id = lastBlock.id,
                            content = lastBlock.content,
                            style = TextStyleType.BODY
                        )
                        blocks[lastIndex] = textBlock
                        targetFocusBlockId = lastBlock.id
                        focusedBlockId = lastBlock.id
                    }
                    is NoteBlock.SimpleList -> {
                        blocks[lastIndex] = NoteBlock.Checklist(
                            id = lastBlock.id,
                            content = lastBlock.content,
                            isChecked = false
                        )
                        targetFocusBlockId = lastBlock.id
                        focusedBlockId = lastBlock.id
                    }
                    is NoteBlock.CalloutBox -> {
                        blocks[lastIndex] = NoteBlock.Checklist(
                            id = lastBlock.id,
                            content = lastBlock.content,
                            isChecked = false
                        )
                        targetFocusBlockId = lastBlock.id
                        focusedBlockId = lastBlock.id
                    }
                    else -> {
                        val newId = UUID.randomUUID().toString()
                        blocks.add(NoteBlock.Checklist(id = newId))
                        targetFocusBlockId = newId
                        focusedBlockId = newId
                    }
                }
            }
        }
    }

    fun transformOrInsertBlock(createBlock: (id: String, existingContent: String) -> NoteBlock) {
        val targetIndex = if (focusedBlockId != null) {
            blocks.indexOfFirst { it.id == focusedBlockId }
        } else -1

        if (targetIndex != -1) {
            val currentBlock = blocks[targetIndex]
            val existingContent = when (currentBlock) {
                is NoteBlock.Text -> currentBlock.content
                is NoteBlock.Checklist -> currentBlock.content
                is NoteBlock.SimpleList -> currentBlock.content
                is NoteBlock.CalloutBox -> currentBlock.content
                else -> ""
            }
            val newBlock = createBlock(currentBlock.id, existingContent)
            blocks[targetIndex] = newBlock
            targetFocusBlockId = newBlock.id
            focusedBlockId = newBlock.id
        } else if (blocks.isNotEmpty()) {
            val lastIndex = blocks.lastIndex
            val lastBlock = blocks[lastIndex]
            val existingContent = when (lastBlock) {
                is NoteBlock.Text -> lastBlock.content
                is NoteBlock.Checklist -> lastBlock.content
                is NoteBlock.SimpleList -> lastBlock.content
                is NoteBlock.CalloutBox -> lastBlock.content
                else -> ""
            }
            val newBlock = createBlock(lastBlock.id, existingContent)
            blocks[lastIndex] = newBlock
            targetFocusBlockId = newBlock.id
            focusedBlockId = newBlock.id
        } else {
            val newId = UUID.randomUUID().toString()
            val newBlock = createBlock(newId, "")
            blocks.add(newBlock)
            targetFocusBlockId = newBlock.id
            focusedBlockId = newBlock.id
        }
    }

    fun normalizeHeadingBlocks(rawBlocks: List<NoteBlock>): List<NoteBlock> {
        val normalized = mutableListOf<NoteBlock>()
        for (block in rawBlocks) {
            if (block is NoteBlock.Text && (block.style == TextStyleType.SUBTITLE || block.style == TextStyleType.TITLE) && block.content.contains('\n')) {
                val lines = block.content.split('\n')
                val headingText = lines.firstOrNull() ?: ""
                val remainingText = lines.drop(1).joinToString("\n")
                normalized.add(block.copy(content = headingText))
                if (remainingText.isNotEmpty()) {
                    normalized.add(NoteBlock.Text(content = remainingText, style = TextStyleType.BODY, isIndented = true))
                }
            } else {
                normalized.add(block)
            }
        }

        val merged = mutableListOf<NoteBlock>()
        for (b in normalized) {
            val last = merged.lastOrNull()
            if (last is NoteBlock.Text && b is NoteBlock.Text && last.style == TextStyleType.BODY && b.style == TextStyleType.BODY && last.isIndented == b.isIndented) {
                val combined = if (last.content.isEmpty()) b.content else if (b.content.isEmpty()) last.content else "${last.content}\n${b.content}"
                merged[merged.lastIndex] = last.copy(content = combined)
            } else {
                merged.add(b)
            }
        }
        return merged
    }

    fun applyHeadingStyleToCurrentBlock(headingStyle: TextStyleType) {
        val targetIndex = if (focusedBlockId != null) {
            blocks.indexOfFirst { it.id == focusedBlockId }
        } else -1

        if (targetIndex != -1) {
            val currentBlock = blocks[targetIndex]
            if (currentBlock is NoteBlock.Text) {
                val tfv = blockTextFieldValues[currentBlock.id] ?: TextFieldValue(currentBlock.content)
                val text = tfv.text
                val cursor = tfv.selection.max.coerceIn(0, text.length)

                if (!text.contains('\n')) {
                    val updatedBlock = currentBlock.copy(style = headingStyle, isCollapsed = false)
                    blocks[targetIndex] = updatedBlock
                    targetFocusBlockId = currentBlock.id
                    focusedBlockId = currentBlock.id
                } else {
                    val lines = text.split('\n')
                    var charCount = 0
                    var targetLineIndex = lines.lastIndex
                    for ((i, line) in lines.withIndex()) {
                        val lineEnd = charCount + line.length
                        if (cursor <= lineEnd) {
                            targetLineIndex = i
                            break
                        }
                        charCount = lineEnd + 1
                    }

                    val headingText = lines[targetLineIndex]
                    val textAbove = lines.subList(0, targetLineIndex).joinToString("\n")
                    val textBelow = lines.subList(targetLineIndex + 1, lines.size).joinToString("\n")

                    if (textAbove.isNotEmpty() || targetLineIndex > 0) {
                        blocks[targetIndex] = NoteBlock.Text(id = currentBlock.id, content = textAbove, style = TextStyleType.BODY)
                        blockTextFieldValues[currentBlock.id] = TextFieldValue(textAbove)

                        val headingId = UUID.randomUUID().toString()
                        val headingBlock = NoteBlock.Text(id = headingId, content = headingText, style = headingStyle, isCollapsed = false)
                        blocks.add(targetIndex + 1, headingBlock)
                        blockTextFieldValues[headingId] = TextFieldValue(headingText, TextRange(headingText.length))
                        targetFocusBlockId = headingId
                        focusedBlockId = headingId

                        if (textBelow.isNotEmpty() || targetLineIndex < lines.lastIndex) {
                            val belowId = UUID.randomUUID().toString()
                            val belowBlock = NoteBlock.Text(id = belowId, content = textBelow, style = TextStyleType.BODY)
                            blocks.add(targetIndex + 2, belowBlock)
                            blockTextFieldValues[belowId] = TextFieldValue(belowBlock.content)
                        }
                    } else {
                        blocks[targetIndex] = currentBlock.copy(content = headingText, style = headingStyle, isCollapsed = false)
                        blockTextFieldValues[currentBlock.id] = TextFieldValue(headingText, TextRange(headingText.length))
                        targetFocusBlockId = currentBlock.id
                        focusedBlockId = currentBlock.id

                        if (textBelow.isNotEmpty() || targetLineIndex < lines.lastIndex) {
                            val belowId = UUID.randomUUID().toString()
                            val belowBlock = NoteBlock.Text(id = belowId, content = textBelow, style = TextStyleType.BODY)
                            blocks.add(targetIndex + 1, belowBlock)
                            blockTextFieldValues[belowId] = TextFieldValue(belowBlock.content)
                        }
                    }
                }
            } else {
                transformOrInsertBlock { id, content ->
                    NoteBlock.Text(id = id, content = content, style = headingStyle)
                }
            }
        } else {
            val newId = UUID.randomUUID().toString()
            blocks.add(NoteBlock.Text(id = newId, content = "", style = headingStyle))
            targetFocusBlockId = newId
            focusedBlockId = newId
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.insertPdfBlock(it) { newBlock ->
                if (!blocks.any { b -> b.id == newBlock.id }) {
                    blocks.add(newBlock)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val copiedFile = ImageHelper.copyImageFromUri(context, uri)
                val finalUri = copiedFile?.absolutePath ?: uri.toString()
                blocks.add(
                    ImageBlock(
                        localUri = finalUri
                    )
                )
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(15)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL) // Habilita filtros (B/N) y recorte manual
            .build()
    }
    val scanner = remember(scannerOptions) {
        GmsDocumentScanning.getClient(scannerOptions)
    }
    var pendingScannedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var scannedPdfName by remember { mutableStateOf("CamNotes Scanner") }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanningResult?.pdf != null) {
                val pdfUri = scanningResult.pdf!!.uri
                scannedPdfName = "CamNotes Scanner"
                pendingScannedPdfUri = pdfUri
            }
        }
    }

    var triggerScanner by remember { mutableStateOf(initialOpenDocumentScanner) }

    LaunchedEffect(triggerScanner) {
        if (triggerScanner) {
            val activity = context.findActivity()
            if (activity != null) {
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al iniciar el escáner", Toast.LENGTH_SHORT).show()
                    }
            }
            triggerScanner = false
        }
    }


    // =========================================================================================
    // REGLA CRÍTICA DE FUNCIONAMIENTO DEL EDITOR (PROHIBIDO MODIFICAR)
    // 1. El campo de texto de las notas es continuo y único: al pulsar Enter se inserta un '\n'
    //    dentro del mismo bloque (ImeAction.Default) y NO se crea otra casilla de texto.
    // 2. Si se borra una checklist u otro elemento y 'blocks' queda vacío, ensureTextNoteExists()
    //    garantiza que SIEMPRE exista un campo de texto activo para escribir.
    // 3. Al tocar en cualquier parte vacía del papel de fondo (área inferior), se enfoca o crea
    //    un campo de texto para escribir como en una hoja de papel real.
    // =========================================================================================
    fun ensureTextNoteExists() {
        if (blocks.isEmpty()) {
            val newId = UUID.randomUUID().toString()
            blocks.add(NoteBlock.Text(id = newId))
            targetFocusBlockId = newId
            focusedBlockId = newId
        }
    }

    fun removeBlock(index: Int) {
        if (index in blocks.indices) {
            blocks.removeAt(index)
            ensureTextNoteExists()
        }
    }

    LaunchedEffect(blocks.size) {
        if (blocks.isEmpty()) {
            ensureTextNoteExists()
        }
    }

    LaunchedEffect(targetFocusBlockId) {
        targetFocusBlockId?.let { id ->
            val blockIndex = blocks.indexOfFirst { it.id == id }
            if (blockIndex != -1) {
                // The LazyColumn has 1 item before the blocks (Header + Title), so blockIndex + 1
                try {
                    lazyListState.animateScrollToItem(blockIndex + 1)
                } catch (_: Exception) {
                    // Ignore scroll errors
                }
            }
            
            var retries = 0
            while (focusRequesters[id] == null && retries < 10) {
                delay(50)
                retries++
            }
            try {
                focusRequesters[id]?.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors for blocks that don't have focusRequester attached
            }
            targetFocusBlockId = null
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var createTableRows by remember { mutableIntStateOf(3) }
    var createTableCols by remember { mutableIntStateOf(3) }

    var showVoiceDictation by remember { mutableStateOf(initialOpenVoiceDictation) }

    var isBoldActive by remember { mutableStateOf(false) }
    var isItalicActive by remember { mutableStateOf(false) }
    var isHighlightActive by remember { mutableStateOf(false) }
    var customFontSizeSp by remember { mutableIntStateOf(17) }
    var showFontSizeCarouselSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReminderDialog = true
        }
    }

    val isCustomColor = colorIndex != 0
    val editorBgColor = if (isCustomColor) {
        NoteColors.getColor(colorIndex, isDark)
    } else {
        MaterialTheme.colorScheme.background
    }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                title = it.title
                titleTextFieldValue = TextFieldValue(it.title, TextRange(it.title.length))
                noteType = runCatching { NoteType.valueOf(it.noteType) }.getOrDefault(NoteType.TEXT)
                colorIndex = it.colorIndex
                paperStyle = runCatching { PaperStyle.valueOf(it.paperStyle) }.getOrDefault(PaperStyle.BLANK)
                isPinned = it.isPinned
                isArchived = it.isArchived
                isLocked = it.isLocked
                lockPin = it.lockPin
                reminderTimestamp = it.reminderTimestamp
                attachedImageUri = it.imageUri
                createdAt = it.createdAt
                updatedAt = it.updatedAt

                val deserialized = NoteBlockSerializer.deserialize(it.content)
                blocks.clear()
                blocks.addAll(normalizeHeadingBlocks(deserialized))
            }
        } else {
            if (initialOpenVoiceDictation && title.isBlank()) {
                title = "Nota de voz rápida"
                titleTextFieldValue = TextFieldValue(title, TextRange(title.length))
            }
            if (blocks.isEmpty()) {
                val initialId = UUID.randomUUID().toString()
                if (initialType == NoteType.CHECKLIST) {
                    blocks.add(NoteBlock.Checklist(id = initialId))
                } else {
                    blocks.add(NoteBlock.Text(id = initialId))
                }
                targetFocusBlockId = initialId
                focusedBlockId = initialId
            }
        }
    }



    fun saveAndExit(navigateBack: Boolean = true) {
        val finalTitle = title.trim()
        val serializedContent = NoteBlockSerializer.serialize(blocks)
        val isEmptyNote = !isValidNoteForSync(finalTitle, blocks)

        if (noteId == 0L && isEmptyNote) {
            if (navigateBack) viewModel.navigateBack()
            return
        }

        viewModel.saveNote(
            id = noteId,
            title = finalTitle,
            content = serializedContent,
            noteType = noteType,
            colorIndex = colorIndex,
            paperStyle = paperStyle.name,
            isPinned = isPinned,
            isArchived = isArchived,
            checklistItems = emptyList(),
            isLocked = isLocked,
            lockPin = lockPin,
            reminderTimestamp = reminderTimestamp,
            imageUri = attachedImageUri,
            onSaved = { savedId ->
                if (reminderTimestamp != null) {
                    viewModel.setReminder(context, savedId, reminderTimestamp!!, finalTitle, serializedContent)
                }
            }
        )
        if (navigateBack) viewModel.navigateBack()
    }

    BackHandler {
        saveAndExit(navigateBack = true)
    }

    val formattedHeaderDate = remember(updatedAt) {
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
        sdf.format(Date(updatedAt))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { saveAndExit(navigateBack = true) },
                        modifier = Modifier.testTag("apple_editor_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = amber
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null) {
                                scanner.getStartScanIntent(activity)
                                    .addOnSuccessListener { intentSender ->
                                        scannerLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error al iniciar el escáner", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, "No se pudo obtener la actividad", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_document_scanner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Escanear documento",
                            tint = amber
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("btn_add_image")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Subir imagen",
                            tint = amber
                        )
                    }

                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val permission = Manifest.permission.POST_NOTIFICATIONS
                                if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                                    showReminderDialog = true
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            } else {
                                showReminderDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_set_reminder")
                    ) {
                        Icon(
                            imageVector = if (reminderTimestamp != null) Icons.Default.Notifications else Icons.Outlined.NotificationsActive,
                            contentDescription = "Programar recordatorio",
                            tint = amber
                        )
                    }

                    IconButton(
                        onClick = {
                            val plainText = blocks.joinToString("\n") {
                                when (it) {
                                    is NoteBlock.Text -> it.content
                                    is NoteBlock.Checklist -> "${if (it.isChecked) "☑" else "☐"} ${it.content}"
                                    is NoteBlock.SimpleList -> it.content
                                    is NoteBlock.CalloutBox -> it.content
                                    is NoteBlock.Drawing -> "[Boceto]"
                                    is NoteBlock.Table -> it.matrix.joinToString(" | ") { row -> row.joinToString(" | ") { cell -> cell.text } }
                                    is PdfBlock -> "[PDF: ${it.fileName}]"
                                    is ImageBlock -> "[Imagen]"
                                    is NoteBlock.Image -> "[Imagen: ${it.fileName}]"
                                    is AudioBlock -> "[Audio: ${String.format(Locale.getDefault(), "%02d:%02d", it.durationMs / 60000, (it.durationMs % 60000) / 1000)}]"
                                }
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$title\n\n$plainText")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir nota"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = amber)
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Generando PDF...", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                try {
                                    val pdfFile = NotePdfExporter.exportNote(context, title, blocks)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        pdfFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "Error al generar el PDF: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_export_pdf")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar PDF",
                            tint = amber
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showVoiceDictation = true
                        },
                        modifier = Modifier.testTag("btn_add_audio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Dictado de voz",
                            tint = amber
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("btn_more_options")
                        ) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Opciones",
                                tint = if (showMenu) Color.Transparent else amber
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(x = 16.dp, y = (-42).dp), // Ajustado para centrar mejor sobre el botón original
                            containerColor = Color.Transparent,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .background(Color.Transparent)
                                .shadow(0.dp)
                        ) {
                            LiquidGlassContainer(
                                shape = CircleShape, // Menos cuadrado
                                isDark = isDark,
                                glassAlpha = 0.65f, // Más translúcido
                                elevation = 12.dp,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 0. Cerrar menú (simula el botón original dentro del vidrio)
                                    IconButton(
                                        onClick = {
                                            showMenu = false
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "Cerrar opciones",
                                            tint = amber
                                        )
                                    }

                                    // 1. Fijar / Desanclar
                                    IconButton(
                                        onClick = {
                                            isPinned = !isPinned
                                            showMenu = false
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = if (isPinned) "Desanclar nota" else "Fijar nota",
                                            tint = amber
                                        )
                                    }

                                    // 2. Bloquear con PIN
                                    IconButton(
                                        onClick = {
                                            showMenu = false
                                            if (isLocked) {
                                                isLocked = false
                                                lockPin = ""
                                            } else {
                                                showLockDialog = true
                                            }
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                                            contentDescription = if (isLocked) "Desbloquear" else "Bloquear con PIN",
                                            tint = amber
                                        )
                                    }

                                    // 3. Líneas y cuadrículas (Estilo de papel)
                                    IconButton(
                                        onClick = {
                                            showMenu = false
                                            showPaperDialog = true
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridView,
                                            contentDescription = "Líneas y cuadrículas",
                                            tint = amber
                                        )
                                    }

                                    // 4. Información y estadísticas
                                    IconButton(
                                        onClick = {
                                            showMenu = false
                                            showStatsDialog = true
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Información y estadísticas",
                                            tint = amber
                                        )
                                    }

                                    // 5. Archivar / Desarchivar
                                    IconButton(
                                        onClick = {
                                            isArchived = !isArchived
                                            showMenu = false
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Outlined.Archive,
                                            contentDescription = if (isArchived) "Desarchivar" else "Archivar nota",
                                            tint = amber
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorBgColor
                )
            )
        },
        containerColor = editorBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            PaperBackground(
                paperStyle = paperStyle,
                isDark = isDark,
                modifier = Modifier.fillMaxSize()
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 0.dp,
                    bottom = 90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header date & reminder
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedHeaderDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    if (reminderTimestamp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_chip")
                                .clickable { showReminderDialog = true },
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
                                        sdf.format(Date(reminderTimestamp!!))
                                    }
                                    Text(
                                        text = "⏰ Recordatorio: $formattedReminder",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF1A365D)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        reminderTimestamp = null
                                        if (noteId > 0) {
                                            viewModel.cancelReminder(context, noteId)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
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
                    BasicTextField(
                        value = titleTextFieldValue,
                        onValueChange = { newValue ->
                            titleTextFieldValue = newValue
                            title = newValue.text
                            coroutineScope.launch {
                                val layout = titleTextLayoutResult
                                if (layout != null) {
                                    val cursorOffset = newValue.selection.max
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
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        cursorBrush = SolidColor(amber),
                        visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedBlockId = "TITLE"
                                }
                            }
                            .bringIntoViewRequester(titleBringIntoViewRequester)
                            .testTag("apple_editor_title"),
                        decorationBox = { innerTextField ->
                            if (titleTextFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Título",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    )

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
                            // PROHIBIDO MODIFICAR: El texto debe ser una casilla entera continua.
                            // ImeAction.Default permite saltos de línea (\n) sin crear nuevos bloques.
                            val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
                            var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

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
                                lineHeight = (baseFontSize.value * 1.4f).sp
                            )
                            val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                            if (block.style == TextStyleType.SUBTITLE) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                if (realIdx != -1) {
                                                    blocks[realIdx] = block.copy(isCollapsed = !block.isCollapsed)
                                                }
                                            }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (block.isCollapsed) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = if (block.isCollapsed) "Expandir sección" else "Colapsar sección",
                                            tint = amber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    BasicTextField(
                                        value = tfv,
                                        onValueChange = { newValue ->
                                            val sanitized = sanitizeTextFieldValue(newValue)
                                            if (sanitized.text.contains('\n')) {
                                                val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                if (block.content.isEmpty()) {
                                                    if (realIdx != -1) {
                                                        val updated = block.copy(style = TextStyleType.BODY, isCollapsed = false)
                                                        blocks[realIdx] = updated
                                                        blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                                        targetFocusBlockId = block.id
                                                        focusedBlockId = block.id
                                                    }
                                                } else {
                                                    val newlineIndex = sanitized.text.indexOf('\n')
                                                    val headingText = sanitized.text.substring(0, newlineIndex)
                                                    val remainingText = sanitized.text.substring(newlineIndex + 1)

                                                    blockTextFieldValues[block.id] = TextFieldValue(headingText, TextRange(headingText.length))
                                                    if (realIdx != -1) {
                                                        blocks[realIdx] = block.copy(content = headingText, isCollapsed = false)

                                                        val nextBlock = blocks.getOrNull(realIdx + 1)
                                                        if (nextBlock is NoteBlock.Text && nextBlock.style == TextStyleType.BODY && nextBlock.isIndented) {
                                                            val updatedContent = if (remainingText.isNotEmpty()) {
                                                                if (nextBlock.content.isEmpty()) remainingText else "$remainingText\n${nextBlock.content}"
                                                            } else nextBlock.content
                                                            blocks[realIdx + 1] = nextBlock.copy(content = updatedContent)
                                                            blockTextFieldValues[nextBlock.id] = TextFieldValue(updatedContent, TextRange(remainingText.length))
                                                            targetFocusBlockId = nextBlock.id
                                                            focusedBlockId = nextBlock.id
                                                        } else {
                                                            val newBodyId = UUID.randomUUID().toString()
                                                            val newBodyBlock = NoteBlock.Text(id = newBodyId, content = remainingText, style = TextStyleType.BODY, isIndented = true)
                                                            blocks.add(realIdx + 1, newBodyBlock)
                                                            blockTextFieldValues[newBodyId] = TextFieldValue(remainingText, TextRange(remainingText.length))
                                                            targetFocusBlockId = newBodyId
                                                            focusedBlockId = newBodyId
                                                        }
                                                    }
                                                }
                                            } else {
                                                blockTextFieldValues[block.id] = sanitized
                                                if (sanitized.text != block.content) {
                                                    val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                    if (realIdx != -1) {
                                                        val nextStyle = if (block.content.isEmpty() && sanitized.text.isEmpty()) TextStyleType.BODY else block.style
                                                        blocks[realIdx] = block.copy(content = sanitized.text, style = nextStyle)
                                                    }
                                                }
                                                coroutineScope.launch {
                                                    val layout = textLayoutResult
                                                    val cursorOffset = sanitized.selection.max
                                                    if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedBlockId = block.id
                                                    coroutineScope.launch {
                                                        delay(100)
                                                        val layout = textLayoutResult
                                                        val currentTfv = blockTextFieldValues[block.id]
                                                        val cursorOffset = currentTfv?.selection?.max ?: 0
                                                        if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                Text(
                                                    text = "Subtítulo...",
                                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                )
                                            }
                                            innerTextField()
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                                    )
                                }
                            } else {
                                BasicTextField(
                                    value = tfv,
                                        onValueChange = { newValue ->
                                            val sanitized = sanitizeTextFieldValue(newValue)
                                            if ((block.style == TextStyleType.TITLE) && sanitized.text.contains('\n')) {
                                                val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                if (block.content.isEmpty()) {
                                                    if (realIdx != -1) {
                                                        val updated = block.copy(style = TextStyleType.BODY, isCollapsed = false, isIndented = false)
                                                        blocks[realIdx] = updated
                                                        blockTextFieldValues[block.id] = TextFieldValue("", TextRange(0))
                                                        targetFocusBlockId = block.id
                                                        focusedBlockId = block.id
                                                    }
                                                } else {
                                                    val newlineIndex = sanitized.text.indexOf('\n')
                                                    val headingText = sanitized.text.substring(0, newlineIndex)
                                                    val remainingText = sanitized.text.substring(newlineIndex + 1)

                                                    blockTextFieldValues[block.id] = TextFieldValue(headingText, TextRange(headingText.length))
                                                    if (realIdx != -1) {
                                                        blocks[realIdx] = block.copy(content = headingText, isCollapsed = false)

                                                        val newBodyId = UUID.randomUUID().toString()
                                                        val newBodyBlock = NoteBlock.Text(id = newBodyId, content = remainingText, style = TextStyleType.BODY, isIndented = true)
                                                        blocks.add(realIdx + 1, newBodyBlock)
                                                        blockTextFieldValues[newBodyId] = TextFieldValue(remainingText, TextRange(remainingText.length))
                                                        targetFocusBlockId = newBodyId
                                                        focusedBlockId = newBodyId
                                                    }
                                                }
                                            } else if (block.isIndented && sanitized.text.endsWith("\n\n")) {
                                                val trimmedText = sanitized.text.dropLast(2)
                                                val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                if (realIdx != -1) {
                                                    blocks[realIdx] = block.copy(content = trimmedText)
                                                    blockTextFieldValues[block.id] = TextFieldValue(trimmedText, TextRange(trimmedText.length))

                                                    val newBodyId = UUID.randomUUID().toString()
                                                    val newBodyBlock = NoteBlock.Text(id = newBodyId, content = "", style = TextStyleType.BODY, isIndented = false)
                                                    blocks.add(realIdx + 1, newBodyBlock)
                                                    blockTextFieldValues[newBodyId] = TextFieldValue("", TextRange(0))
                                                    targetFocusBlockId = newBodyId
                                                    focusedBlockId = newBodyId
                                                }
                                            } else {
                                                blockTextFieldValues[block.id] = sanitized
                                                if (sanitized.text != block.content) {
                                                    val realIdx = blocks.indexOfFirst { it.id == block.id }
                                                    if (realIdx != -1) {
                                                        blocks[realIdx] = block.copy(content = sanitized.text)
                                                    }
                                                }
                                                coroutineScope.launch {
                                                    val layout = textLayoutResult
                                                    val cursorOffset = sanitized.selection.max
                                                    if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedBlockId = block.id
                                                    coroutineScope.launch {
                                                        delay(100)
                                                        val layout = textLayoutResult
                                                        val currentTfv = blockTextFieldValues[block.id]
                                                        val cursorOffset = currentTfv?.selection?.max ?: 0
                                                        if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                val showPlaceholder = when (block.style) {
                                                    TextStyleType.TITLE -> "Título de sección..."
                                                    else -> if (blocks.size <= 1 && title.isEmpty()) "Escribe algo..." else ""
                                                }
                                                if (showPlaceholder.isNotEmpty()) {
                                                    Text(
                                                        text = showPlaceholder,
                                                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                    )
                                                }
                                            }
                                            innerTextField()
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                                    )
                            }
                        }
                        is NoteBlock.Checklist -> {
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
                                        .border(2.dp, if (block.isChecked) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                                        .background(if (block.isChecked) amber else Color.Transparent)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            blocks[index] = block.copy(isChecked = !block.isChecked)
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
                                val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                                val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
                                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                BasicTextField(
                                    value = tfv,
                                    onValueChange = { newValue ->
                                        val sanitized = sanitizeTextFieldValue(newValue)
                                        blockTextFieldValues[block.id] = sanitized
                                        if (sanitized.text != block.content) {
                                            blocks[index] = block.copy(content = sanitized.text)
                                        }
                                        coroutineScope.launch {
                                            val layout = textLayoutResult
                                            val cursorOffset = sanitized.selection.max
                                            if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        .weight(1f)
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedBlockId = block.id
                                                coroutineScope.launch {
                                                    delay(100)
                                                    val layout = textLayoutResult
                                                    val currentTfv = blockTextFieldValues[block.id]
                                                    val cursorOffset = currentTfv?.selection?.max ?: 0
                                                    if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        color = if (block.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        background = if (isHighlightActive) amber.copy(alpha = 0.35f) else Color.Transparent,
                                        textDecoration = when {
                                            block.isChecked -> TextDecoration.LineThrough
                                            else -> TextDecoration.None
                                        }
                                    ),
                                    cursorBrush = SolidColor(amber),
                                    visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                                    decorationBox = { innerTextField ->
                                        if (block.content.isEmpty()) {
                                            Text(
                                                text = "Elemento de lista de tareas...",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            )
                                        }
                                        innerTextField()
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (block.content.isBlank()) {
                                                blocks[index] = NoteBlock.Text(id = block.id, content = "")
                                            } else {
                                                val newId = UUID.randomUUID().toString()
                                                blocks.add(index + 1, NoteBlock.Checklist(id = newId))
                                                targetFocusBlockId = newId
                                            }
                                        }
                                    )
                                )
                                IconButton(
                                    onClick = { removeBlock(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        is NoteBlock.SimpleList -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("block_simple_list_$index"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (block.isNumbered) "${block.number}." else "•",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = amber, fontSize = 17.sp),
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                                val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
                                var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                BasicTextField(
                                    value = tfv,
                                    onValueChange = { newValue ->
                                        val sanitized = sanitizeTextFieldValue(newValue)
                                        blockTextFieldValues[block.id] = sanitized
                                        if (sanitized.text != block.content) {
                                            blocks[index] = block.copy(content = sanitized.text)
                                        }
                                        coroutineScope.launch {
                                            val layout = textLayoutResult
                                            val cursorOffset = sanitized.selection.max
                                            if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        .weight(1f)
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedBlockId = block.id
                                                coroutineScope.launch {
                                                    delay(100)
                                                    val layout = textLayoutResult
                                                    val currentTfv = blockTextFieldValues[block.id]
                                                    val cursorOffset = currentTfv?.selection?.max ?: 0
                                                    if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                        background = if (isHighlightActive) amber.copy(alpha = 0.35f) else Color.Transparent,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(amber),
                                    visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                                    decorationBox = { innerTextField ->
                                        if (block.content.isEmpty()) {
                                            Text(
                                                text = "Elemento de lista...",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            )
                                        }
                                        innerTextField()
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (block.content.isBlank()) {
                                                blocks[index] = NoteBlock.Text(id = block.id, content = "")
                                            } else {
                                                val nextNumber = if (block.isNumbered) block.number + 1 else 1
                                                val newId = UUID.randomUUID().toString()
                                                blocks.add(index + 1, NoteBlock.SimpleList(id = newId, isNumbered = block.isNumbered, number = nextNumber))
                                                targetFocusBlockId = newId
                                            }
                                        }
                                    )
                                )
                            }
                        }
                        is NoteBlock.CalloutBox -> {
                            var showCalloutMenu by remember { mutableStateOf(false) }
                            var showCalloutColorPicker by remember { mutableStateOf(false) }

                            val calloutBg = if (!block.backgroundColorHex.isNullOrBlank()) {
                                runCatching { Color(android.graphics.Color.parseColor(block.backgroundColorHex)) }.getOrDefault(
                                    if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                                )
                            } else {
                                if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                            }

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
                                    val tfv = blockTextFieldValues[block.id] ?: TextFieldValue(block.content)
                                    val bringIntoViewRequester = remember(block.id) { BringIntoViewRequester() }
                                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                    BasicTextField(
                                        value = tfv,
                                        onValueChange = { newValue ->
                                            val sanitized = sanitizeTextFieldValue(newValue)
                                            blockTextFieldValues[block.id] = sanitized
                                            if (sanitized.text != block.content) {
                                                blocks[index] = block.copy(content = sanitized.text)
                                            }
                                            coroutineScope.launch {
                                                val layout = textLayoutResult
                                                val cursorOffset = sanitized.selection.max
                                                if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedBlockId = block.id
                                                    coroutineScope.launch {
                                                        delay(100)
                                                        val layout = textLayoutResult
                                                        val currentTfv = blockTextFieldValues[block.id]
                                                        val cursorOffset = currentTfv?.selection?.max ?: 0
                                                        if (layout != null && cursorOffset in 0..layout.layoutInput.text.length) {
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontStyle = FontStyle.Italic
                                        ),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isDark) { markdownVisualTransformation(isDark, amber) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                Text(
                                                    text = "Cuadro destacado / Nota importante...",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 17.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
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
                                                    removeBlock(index)
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
                                                            block.backgroundColorHex = hex
                                                            blocks[index] = block.copy(backgroundColorHex = hex)
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
                        is NoteBlock.Drawing -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        saveAndExit(navigateBack = false)
                                        viewModel.navigateTo(Screen.Drawing(noteId))
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA),
                                border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
                            ) {
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        is NoteBlock.Table -> {
                            TableBlockItem(
                                block = block,
                                onUpdateBlock = { updatedTable ->
                                    blocks[index] = updatedTable
                                },
                                onDeleteBlock = {
                                    removeBlock(index)
                                },
                                modifier = Modifier.testTag("table_block_$index")
                            )
                        }
                        is PdfBlock -> {
                            PdfBlockItem(
                                block = block,
                                onPageChange = { newIndex ->
                                    blocks[index] = block.copy(currentPageIndex = newIndex)
                                },
                                onToggleExpand = {
                                    blocks[index] = block.copy(isExpanded = !block.isExpanded)
                                },
                                onDeleteBlock = {
                                    removeBlock(index)
                                },
                                onOpenFullScreen = {
                                    saveAndExit(navigateBack = false)
                                    viewModel.navigateTo(Screen.PdfViewer(noteId = noteId, pdfPath = block.filePath))
                                },
                                modifier = Modifier.testTag("block_pdf_$index")
                            )
                        }
                        is AudioBlock -> {
                            AudioBlockItem(
                                block = block,
                                onToggleTranscription = {
                                    blocks[index] = block.copy(isTranscriptionVisible = !block.isTranscriptionVisible)
                                },
                                onDeleteBlock = {
                                    val file = File(block.filePath)
                                    if (file.exists()) file.delete()
                                    removeBlock(index)
                                },
                                modifier = Modifier.testTag("block_audio_$index")
                            )
                        }
                        is ImageBlock -> {
                            ImageBlockItem(
                                block = block,
                                onRemove = { removeBlock(index) },
                                modifier = Modifier.testTag("block_image_$index")
                            )
                        }
                        is NoteBlock.Image -> {
                            ImageBlockItem(
                                block = block,
                                onDeleteBlock = { removeBlock(index) },
                                modifier = Modifier.testTag("block_image_$index")
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
                                    onClick = { attachedImageUri = null },
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
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val lastBlock = blocks.lastOrNull()
                                if (lastBlock is NoteBlock.Text && lastBlock.style == TextStyleType.BODY && !lastBlock.isIndented) {
                                    targetFocusBlockId = lastBlock.id
                                    focusedBlockId = lastBlock.id
                                } else {
                                    val newId = UUID.randomUUID().toString()
                                    blocks.add(NoteBlock.Text(id = newId, style = TextStyleType.BODY, isIndented = false))
                                    targetFocusBlockId = newId
                                    focusedBlockId = newId
                                }
                            }
                    )
                }
            }

            // Floating Accessory Bar (barra inferior flotante)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                AnimatedVisibility(
                    visible = activeHashtagInfo != null && suggestedSmartTags.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SmartTagsSuggestionRow(
                        tags = suggestedSmartTags,
                        amberColor = amber,
                        isDark = isDark,
                        onSelectTag = { item ->
                            applySmartTagSelection(item)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.10f),
                                spotColor = Color.Black.copy(alpha = 0.15f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.88f)
                                else Color.White.copy(alpha = 0.88f)
                            )
                            .border(
                                width = 1.dp,
                                brush = if (isDark) {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.40f),
                                            Color.Transparent,
                                            Color(0xFF3F3F46).copy(alpha = 0.60f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.90f),
                                            Color.White.copy(alpha = 0.30f),
                                            Color(0xFFCBD5E1).copy(alpha = 0.45f)
                                        )
                                    )
                                },
                                shape = CircleShape
                            )
                            .testTag("liquid_glass_accessory_bar"),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Botón "Insertar" (+) -> Abre menú de inserción (desplegable desde abajo)
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showFormatSheet = true
                                },
                                modifier = Modifier.testTag("btn_insert_menu")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = "Insertar",
                                        tint = amber,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // 2. Negrita
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isBoldActive = !isBoldActive
                                },
                                modifier = Modifier.testTag("btn_format_bold")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isBoldActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatBold,
                                        contentDescription = "Negrita",
                                        tint = amber,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // 3. Cursiva
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isItalicActive = !isItalicActive
                                },
                                modifier = Modifier.testTag("btn_format_italic")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isItalicActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatItalic,
                                        contentDescription = "Cursiva",
                                        tint = amber,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // 4. Resaltar selección
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val currentId = focusedBlockId
                                    if (currentId != null) {
                                        val tfv = blockTextFieldValues[currentId]
                                        if (tfv != null && !tfv.selection.collapsed) {
                                            val start = tfv.selection.min
                                            val end = tfv.selection.max
                                            val text = tfv.text
                                            val selectedText = text.substring(start, end)

                                            val (newText, newSelection) = if (selectedText.startsWith("==") && selectedText.endsWith("==") && selectedText.length >= 4) {
                                                // Quitar resaltado
                                                val unhighlighted = selectedText.substring(2, selectedText.length - 2)
                                                val updated = text.substring(0, start) + unhighlighted + text.substring(end)
                                                val sel = TextRange(start, start + unhighlighted.length)
                                                Pair(updated, sel)
                                            } else {
                                                // Aplicar resaltado
                                                val highlighted = "==$selectedText=="
                                                val updated = text.substring(0, start) + highlighted + text.substring(end)
                                                val sel = TextRange(start, start + highlighted.length)
                                                Pair(updated, sel)
                                            }

                                            val newTfv = tfv.copy(text = newText, selection = newSelection)
                                            blockTextFieldValues[currentId] = newTfv

                                            // Actualizar el modelo
                                            val idx = blocks.indexOfFirst { it.id == currentId }
                                            if (idx != -1) {
                                                val b = blocks[idx]
                                                if (b is NoteBlock.Text) blocks[idx] = b.copy(content = newText)
                                                else if (b is NoteBlock.Checklist) blocks[idx] = b.copy(content = newText)
                                                else if (b is NoteBlock.SimpleList) blocks[idx] = b.copy(content = newText)
                                                else if (b is NoteBlock.CalloutBox) blocks[idx] = b.copy(content = newText)
                                            }
                                        } else {
                                            isHighlightActive = !isHighlightActive
                                        }
                                    } else {
                                        isHighlightActive = !isHighlightActive
                                    }
                                },
                                modifier = Modifier.testTag("btn_format_highlight")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isHighlightActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Highlight,
                                        contentDescription = "Resaltar",
                                        tint = amber,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // 5. Tamaño de texto (Icono de regla -> abre carrusel de tamaños 1 a 150)
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showFontSizeCarouselSheet = true
                                },
                                modifier = Modifier.testTag("btn_font_size_ruler")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (customFontSizeSp != 17) amber.copy(alpha = 0.25f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Straighten,
                                        contentDescription = "Tamaño de texto",
                                        tint = amber,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Insert Bottom Sheet (Menú desplegable desde abajo hacia arriba)
    if (showFormatSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showFormatSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.Black,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Insertar contenido",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                FormatSectionTitle("Básico")
                FormatOptionItem(
                    icon = Icons.AutoMirrored.Rounded.Subject,
                    text = "Texto normal (Cuerpo)"
                ) {
                    showFormatSheet = false
                    transformOrInsertBlock { id, content ->
                        NoteBlock.Text(id = id, content = content, style = TextStyleType.BODY, isIndented = false)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.Title,
                    text = "Título grande"
                ) {
                    showFormatSheet = false
                    applyHeadingStyleToCurrentBlock(TextStyleType.TITLE)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.FormatSize,
                    text = "Subtítulo"
                ) {
                    showFormatSheet = false
                    applyHeadingStyleToCurrentBlock(TextStyleType.SUBTITLE)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatSectionTitle("Listas")
                FormatOptionItem(
                    icon = Icons.Rounded.CheckBox,
                    text = "Lista de tareas (Checklist)"
                ) {
                    showFormatSheet = false
                    toggleOrInsertChecklist()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    text = "Lista con viñetas"
                ) {
                    showFormatSheet = false
                    transformOrInsertBlock { id, content ->
                        NoteBlock.SimpleList(id = id, content = content, isNumbered = false)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.FormatListNumbered,
                    text = "Lista numerada"
                ) {
                    showFormatSheet = false
                    transformOrInsertBlock { id, content ->
                        NoteBlock.SimpleList(id = id, content = content, isNumbered = true)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatSectionTitle("Elementos Avanzados")
                FormatOptionItem(
                    icon = Icons.Rounded.Info,
                    text = "Cuadro destacado"
                ) {
                    showFormatSheet = false
                    transformOrInsertBlock { id, content ->
                        NoteBlock.CalloutBox(id = id, content = content)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.TableChart,
                    text = "Tabla"
                ) {
                    showFormatSheet = false
                    createTableRows = 3
                    createTableCols = 3
                    showCreateTableDialog = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.Draw,
                    text = "Boceto a mano (Dibujo)"
                ) {
                    showFormatSheet = false
                    blocks.add(NoteBlock.Drawing())
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Rounded.PictureAsPdf,
                    text = "Adjuntar PDF"
                ) {
                    showFormatSheet = false
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                FormatOptionItem(
                    icon = Icons.Default.DocumentScanner,
                    text = "Escanear documento (Modo Escáner Pro)"
                ) {
                    showFormatSheet = false
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val activity = (context as? Activity) ?: context.findActivity()
                    if (activity != null) {
                        scanner.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al iniciar el escáner", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "No se pudo obtener la actividad", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showVoiceDictation) {
        val voiceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showVoiceDictation = false },
            sheetState = voiceSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            VoiceNoteScreen(
                onRecordingFinished = { transcribedText, _, _ ->
                    if (transcribedText.isNotBlank()) {
                        val newTextBlockId = UUID.randomUUID().toString()
                        val textBlock = NoteBlock.Text(
                            id = newTextBlockId,
                            content = transcribedText
                        )
                        
                        val targetIndex = if (focusedBlockId != null) {
                            blocks.indexOfFirst { it.id == focusedBlockId }
                        } else -1

                        if (targetIndex != -1) {
                            val currentBlock = blocks[targetIndex]
                            val isEmptyText = currentBlock is NoteBlock.Text && currentBlock.content.isEmpty()
                            if (isEmptyText) {
                                blocks[targetIndex] = textBlock
                            } else {
                                blocks.add(targetIndex + 1, textBlock)
                            }
                        } else {
                            blocks.add(textBlock)
                        }
                        targetFocusBlockId = newTextBlockId
                        focusedBlockId = newTextBlockId
                    }
                },
                onDismiss = { showVoiceDictation = false }
            )
        }
    }

    // Carousel Vertical Font Size Bottom Sheet (1 a 150 sp - Rodillo/Rueda vertical)
    if (showFontSizeCarouselSheet) {
        val fontSizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val initialItemIndex = (customFontSizeSp - 1).coerceIn(0, 149)
        val verticalPickerState = rememberLazyListState(initialFirstVisibleItemIndex = initialItemIndex)
        val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = verticalPickerState)
        val coroutineScope = rememberCoroutineScope()

        // Sincronizar customFontSizeSp con la posición exacta del rodillo en tiempo real
        LaunchedEffect(verticalPickerState) {
            snapshotFlow { verticalPickerState.firstVisibleItemIndex }
                .collect { firstIndex ->
                    val newSize = (firstIndex + 1).coerceIn(1, 150)
                    if (newSize != customFontSizeSp) {
                        customFontSizeSp = newSize
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        }

        ModalBottomSheet(
            onDismissRequest = { showFontSizeCarouselSheet = false },
            sheetState = fontSizeSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tamaño de texto",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Muestra digital del tamaño actual
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

                // Rodillo / Rueda Selectora Vertical (180dp alto: 60dp arriba, 60dp centro, 60dp abajo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.50f)
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Cápsula seleccionada central en la rueda
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
                                        customFontSizeSp = number
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

                // Accesos rápidos comunes abajo
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
                                    customFontSizeSp = quickSize
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

    if (showCreateTableDialog) {
        CreateTableBottomSheet(
            initialRows = createTableRows,
            initialCols = createTableCols,
            onCreateTable = { r, c, header, zebra ->
                showCreateTableDialog = false
                val newId = UUID.randomUUID().toString()
                blocks.add(
                    NoteBlock.Table(
                        id = newId,
                        rows = r,
                        cols = c,
                        hasHeaderRow = header,
                        hasZebraStripes = zebra
                    )
                )
                targetFocusBlockId = newId
            },
            onDismiss = { showCreateTableDialog = false }
        )
    }

    if (showReminderDialog) {
        val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                if (reminderTimestamp != null && reminderTimestamp!! > System.currentTimeMillis()) {
                    timeInMillis = reminderTimestamp!!
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
            onDismissRequest = { showReminderDialog = false },
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
                    .padding(bottom = 24.dp),
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
                            reminderTimestamp = selectedTimestamp
                            if (noteId > 0) {
                                viewModel.setReminder(context, noteId, selectedTimestamp, title, NoteBlockSerializer.serialize(blocks), selectedReminderOffsets.toList())
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showReminderDialog = false
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
                            reminderTimestamp = null
                            if (noteId > 0) {
                                viewModel.cancelReminder(context, noteId)
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showReminderDialog = false
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

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Color de nota") },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(6) { idx ->
                        val col = NoteColors.getColor(idx, isDark)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (colorIndex == idx) 2.dp else 1.dp,
                                    color = if (colorIndex == idx) amber else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorIndex = idx
                                    showColorDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = { showPaperDialog = false },
            title = { Text("Estilo de papel") },
            text = {
                Column {
                    PaperStyle.entries.forEach { style ->
                        TextButton(
                            onClick = {
                                paperStyle = style
                                showPaperDialog = false
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
                TextButton(onClick = { showPaperDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showLockDialog) {
        NoteLockDialog(
            isSettingPin = true,
            onConfirm = { pin ->
                isLocked = true
                lockPin = pin
                showLockDialog = false
            },
            onDismiss = { showLockDialog = false }
        )
    }

    if (showStatsDialog) {
        val statsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val textBlocks = blocks.filterIsInstance<NoteBlock.Text>()
        val checklistBlocks = blocks.filterIsInstance<NoteBlock.Checklist>()
        val tableBlocks = blocks.filterIsInstance<NoteBlock.Table>()
        val audioBlocks = blocks.filterIsInstance<AudioBlock>()
        val pdfBlocks = blocks.filterIsInstance<PdfBlock>()

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
                }
            }
            "$title $contentStr"
        }

        val wordCount = remember(plainTextSnippet) {
            plainTextSnippet.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
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
            onDismissRequest = { showStatsDialog = false },
            sheetState = statsSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = if (isDark) Color(0xFF141416) else Color(0xFFF8FAFC),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Información y estadísticas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassContainer(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isDark = isDark
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
                        isDark = isDark
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
                        isDark = isDark
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
                    onClick = { showStatsDialog = false },
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

    if (pendingScannedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { pendingScannedPdfUri = null },
            title = { Text("Nombrar documento escaneado") },
            text = {
                OutlinedTextField(
                    value = scannedPdfName,
                    onValueChange = { scannedPdfName = it },
                    label = { Text("Nombre del archivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingScannedPdfUri
                        if (uri != null) {
                            viewModel.insertPdfBlock(uri, customFileName = scannedPdfName) { newBlock ->
                                if (!blocks.any { b -> b.id == newBlock.id }) {
                                    blocks.add(newBlock)
                                }
                            }
                        }
                        pendingScannedPdfUri = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingScannedPdfUri = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FormatSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = Color(0xFF94A3B8),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 12.dp)
    )
}

@Composable
fun FormatOptionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val bgColor = Color(0xFF1C1C1E)
    val contentColor = Color.White
    val iconTint = AppleNotesAmber

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = contentColor
        )
    }
}

private data class HighlightSpan(val rawStart: Int, val rawEnd: Int) {
    val rawLength: Int get() = rawEnd - rawStart
    val innerLength: Int get() = rawLength - 4
}

private fun sanitizeHighlightTags(text: String): String {
    var result = text
    // 1. Eliminar etiquetas vacías o triples como ==== o ===
    result = result.replace(Regex("""==\s*==|====|==="""), "")

    // 2. Corregir etiqueta rota al final: ==palabra=
    result = result.replace(Regex("""==([^=\n]+)=(?!=)""")) {
        it.groupValues[1]
    }

    // 3. Corregir etiqueta rota al inicio: =palabra==
    result = result.replace(Regex("""(?<!=)=([^=\n]+)==""")) {
        it.groupValues[1]
    }

    // 4. Corregir etiquetas únicas: =palabra=
    result = result.replace(Regex("""(?<!=)=([^=\n]+)=(?![=])""")) {
        it.groupValues[1]
    }

    return result
}

private fun sanitizeTextFieldValue(tfv: TextFieldValue): TextFieldValue {
    val rawText = tfv.text
    val sanitized = sanitizeHighlightTags(rawText)
    if (sanitized == rawText) {
        return tfv
    }
    val newCursor = tfv.selection.start.coerceIn(0, sanitized.length)
    return tfv.copy(
        text = sanitized,
        selection = TextRange(newCursor)
    )
}

private class HighlightOffsetMapping(private val spans: List<HighlightSpan>) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var totalRemoved = 0
        for (span in spans) {
            if (offset <= span.rawStart) {
                break
            } else if (offset <= span.rawStart + 2) {
                totalRemoved += (offset - span.rawStart)
            } else if (offset <= span.rawEnd - 2) {
                totalRemoved += 2
            } else if (offset <= span.rawEnd) {
                totalRemoved += 2 + (offset - (span.rawEnd - 2))
            } else {
                totalRemoved += 4
            }
        }
        return offset - totalRemoved
    }

    override fun transformedToOriginal(offset: Int): Int {
        var totalAdded = 0
        for (span in spans) {
            val visualStart = span.rawStart - totalAdded
            val visualEnd = visualStart + span.innerLength

            if (offset <= visualStart) {
                break
            } else if (offset <= visualEnd) {
                totalAdded += 2
            } else {
                totalAdded += 4
            }
        }
        return offset + totalAdded
    }
}

private fun markdownVisualTransformation(isDark: Boolean, amber: Color): VisualTransformation {
    val lightIndigo = Color(0xFF1A365D)
    val darkIndigo = Color(0xFF60A5FA)
    val tagColor = if (isDark) darkIndigo else lightIndigo

    return VisualTransformation { text ->
        val fullText = text.text
        val highlightRegex = Regex("""==([^=\n]+)==""")
        val highlightMatches = highlightRegex.findAll(fullText).toList()

        val spans = highlightMatches.map {
            HighlightSpan(it.range.first, it.range.last + 1)
        }

        val annotatedString = buildAnnotatedString {
            var lastIndex = 0
            for (match in highlightMatches) {
                val start = match.range.first
                val end = match.range.last + 1

                if (start > lastIndex) {
                    append(fullText.substring(lastIndex, start))
                }

                val innerText = match.groupValues[1]
                val visualStart = length
                append(innerText)
                val visualEnd = length

                addStyle(
                    SpanStyle(background = amber.copy(alpha = 0.35f)),
                    visualStart,
                    visualEnd
                )

                lastIndex = end
            }
            if (lastIndex < fullText.length) {
                append(fullText.substring(lastIndex))
            }

            // Aplicar estilo de hashtags sobre el texto visual
            val visualString = toAnnotatedString().text
            val hashtagRegex = Regex("""#([a-zA-Z0-9_áéíóúÁÉÍÓÚñÑ]+)""")
            for (m in hashtagRegex.findAll(visualString)) {
                addStyle(
                    SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                    m.range.first,
                    m.range.last + 1
                )
            }
        }

        val offsetMapping = if (spans.isEmpty()) OffsetMapping.Identity else HighlightOffsetMapping(spans)
        TransformedText(annotatedString, offsetMapping)
    }
}

@Composable
private fun SmartTagsSuggestionRow(
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
