package com.example.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.util.getKeyboardLanguage
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

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
import com.example.util.PaperThemeMode
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.derivedStateOf
import com.example.ui.components.LiquidGlassContainer
import com.example.ui.components.InlineMathView
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
import androidx.compose.ui.platform.LocalDensity
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
import com.example.ui.components.PaperBackground
import com.example.ui.components.AudioBlockItem
import com.example.ui.components.PdfBlockItem
import com.example.ui.components.TableBlockItem
import com.example.ui.components.ImageBlockItem
import com.example.ui.components.MathBlockItem
import com.example.ui.components.CreateTableBottomSheet
import com.example.ui.components.REMINDER_OFFSET_OPTIONS
import androidx.core.content.FileProvider
import com.example.ui.components.ActiveHashtagInfo
import com.example.ui.components.EditorColorPickerDialog
import com.example.ui.components.EditorFontSizeCarouselSheet
import com.example.ui.components.EditorAccessoryBar
import com.example.ui.components.EditorPaperStyleDialog
import com.example.ui.components.EditorReminderBottomSheet
import com.example.ui.components.EditorScannedPdfNameDialog
import com.example.ui.components.EditorStatsBottomSheet
import com.example.ui.components.EditorTopAppBar
import com.example.ui.components.SmartTagItem
import com.example.ui.components.SmartTagsSuggestionRow
import com.example.ui.components.NoteLinkSuggestionMenu
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.sanitizeTextFieldValue
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.util.GeminiAudioTranscriber
import com.example.util.ImageHelper
import com.example.util.NoteBlockSerializer
import com.example.util.NotePdfExporter
import com.example.util.isValidNoteForSync
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    val paperTheme = uiState.paperThemeMode
    val isPaperDark = when (paperTheme) {
        PaperThemeMode.DARK -> true
        PaperThemeMode.LIGHT -> false
        PaperThemeMode.AUTO -> isDark
    }
    val amber = if (isPaperDark) NoteJsAmberDark else NoteJsAmber

    val paperTextColor = if (isPaperDark) Color.White else Color(0xFF1C1C1E)
    val paperHintColor = if (isPaperDark) Color.LightGray else Color.DarkGray

    var title by remember { mutableStateOf("") }
    var titleTextFieldValue by remember { mutableStateOf(TextFieldValue(title)) }
    val blocks = remember { mutableStateListOf<NoteBlock>() }
    val undoHistory = remember { mutableStateListOf<List<NoteBlock>>() }
    val redoHistory = remember { mutableStateListOf<List<NoteBlock>>() }

    fun saveSnapshot() {
        undoHistory.add(blocks.toList())
        if (undoHistory.size > 30) {
            undoHistory.removeAt(0)
        }
        redoHistory.clear()
    }

    var noteType by remember { mutableStateOf(initialType) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var lockPin by remember { mutableStateOf("") }
    var syncEnabled by remember { mutableStateOf(true) }
    var reminderTimestamp by remember { mutableStateOf<Long?>(null) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }

    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var updatedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var targetFocusBlockId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val blockTextFieldValues = remember { mutableStateMapOf<String, TextFieldValue>() }
    val lazyListState = rememberLazyListState()

    var showNoteLinker by remember { mutableStateOf(false) }
    var linkQuery by remember { mutableStateOf("") }
    var activeNoteLinkerBlockId by remember { mutableStateOf<String?>(null) }

    fun evaluateNoteLinkTrigger(blockId: String, tfv: TextFieldValue) {
        val text = tfv.text
        val cursorIndex = tfv.selection.end.coerceIn(0, text.length)
        val textBeforeCursor = text.substring(0, cursorIndex)
        val triggerRegex = Regex("""\[\[([^\]\n]*)$""")
        val match = triggerRegex.find(textBeforeCursor)
        if (match != null) {
            showNoteLinker = true
            linkQuery = match.groupValues[1]
            activeNoteLinkerBlockId = blockId
        } else {
            if (activeNoteLinkerBlockId == blockId) {
                showNoteLinker = false
                linkQuery = ""
                activeNoteLinkerBlockId = null
            }
        }
    }

    val allActiveNotes = uiState.allActiveNotes
    val matchingNotesForLinker = remember(linkQuery, allActiveNotes, noteId) {
        allActiveNotes.filter { note ->
            note.id != noteId && !note.isLocked && !note.isTrashed &&
            (linkQuery.isBlank() || note.title.contains(linkQuery, ignoreCase = true) || note.content.contains(linkQuery, ignoreCase = true))
        }.take(10)
    }

    fun insertNoteLink(selectedNoteId: Long, selectedNoteTitle: String) {
        val targetId = activeNoteLinkerBlockId ?: focusedBlockId ?: return
        val currentTfv = if (targetId == "TITLE") titleTextFieldValue else blockTextFieldValues[targetId] ?: return
        val cursorIdx = currentTfv.selection.end.coerceIn(0, currentTfv.text.length)
        val textBeforeCursor = currentTfv.text.substring(0, cursorIdx)
        val textAfterCursor = currentTfv.text.substring(cursorIdx)

        val triggerIndex = textBeforeCursor.lastIndexOf("[[")
        if (triggerIndex != -1) {
            val markdownLink = "[$selectedNoteTitle](notejs://note/$selectedNoteId) "
            val newTextBefore = textBeforeCursor.substring(0, triggerIndex) + markdownLink
            val newFullText = newTextBefore + textAfterCursor
            val newCursorPos = newTextBefore.length
            val newTfv = TextFieldValue(text = newFullText, selection = TextRange(newCursorPos))

            if (targetId == "TITLE") {
                titleTextFieldValue = newTfv
                title = newFullText
            } else {
                blockTextFieldValues[targetId] = newTfv
                val realIdx = blocks.indexOfFirst { it.id == targetId }
                if (realIdx != -1) {
                    val block = blocks[realIdx]
                    if (block is NoteBlock.Text) {
                        blocks[realIdx] = block.copy(content = newFullText)
                    } else if (block is NoteBlock.Checklist) {
                        blocks[realIdx] = block.copy(content = newFullText)
                    } else if (block is NoteBlock.SimpleList) {
                        blocks[realIdx] = block.copy(content = newFullText)
                    } else if (block is NoteBlock.CalloutBox) {
                        blocks[realIdx] = block.copy(content = newFullText)
                    }
                }
            }
        }
        showNoteLinker = false
        linkQuery = ""
        activeNoteLinkerBlockId = null
    }

    val activeHashtagInfo by remember {
        derivedStateOf {
            val currentFocusId = focusedBlockId ?: return@derivedStateOf null
            val tfv = if (currentFocusId == "TITLE") {
                titleTextFieldValue
            } else {
                blockTextFieldValues[currentFocusId]
            } ?: return@derivedStateOf null

            val text = tfv.text
            val cursorIndex = tfv.selection.max.coerceIn(0, text.length)
            val textBeforeCursor = text.substring(0, cursorIndex)
            val lastHashIndex = textBeforeCursor.lastIndexOf('#')
            if (lastHashIndex == -1) return@derivedStateOf null

            val safeHashIdx = (lastHashIndex + 1).coerceIn(0, textBeforeCursor.length)
            val queryCandidate = textBeforeCursor.substring(safeHashIdx)
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
        val safeHashIndex = info.hashIndex.coerceIn(0, text.length)
        val safeCursorIndex = cursorIndex.coerceIn(0, text.length)
        val beforeHash = text.substring(0, safeHashIndex)
        val afterCursor = text.substring(safeCursorIndex)

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
        saveSnapshot()
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
        saveSnapshot()
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
        saveSnapshot()
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
                    saveSnapshot()
                    blocks.add(newBlock)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            saveSnapshot()
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

    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull() ?: ""
            
            @Suppress("DEPRECATION")
            val audioUri: Uri? = data?.data ?: data?.getParcelableExtra("android.speech.extra.AUDIO_URI")
            
            if (text.isNotBlank()) {
                coroutineScope.launch(Dispatchers.IO) {
                    var localPath = ""
                    var durationMs = 0L
                    
                    if (audioUri != null) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(audioUri)
                            val audioDir = File(context.filesDir, "audio")
                            if (!audioDir.exists()) audioDir.mkdirs()
                            val destFile = File(audioDir, "audio_${UUID.randomUUID()}.amr")
                            inputStream?.use { input ->
                                destFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            localPath = destFile.absolutePath
                            
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(localPath)
                                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                retriever.release()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        saveSnapshot()
                        val newId = UUID.randomUUID().toString()
                        
                        val newBlock = if (localPath.isNotBlank()) {
                            AudioBlock(
                                id = newId,
                                filePath = localPath,
                                durationMs = durationMs,
                                transcription = text,
                                isTranscribing = false,
                                isTranscriptionVisible = true
                            )
                        } else {
                            NoteBlock.Text(id = newId, content = text)
                        }
                        
                        val targetIndex = if (focusedBlockId != null) {
                            blocks.indexOfFirst { it.id == focusedBlockId }
                        } else -1

                        if (targetIndex != -1) {
                            val currentBlock = blocks[targetIndex]
                            val isEmptyText = currentBlock is NoteBlock.Text && currentBlock.content.isEmpty()
                            if (isEmptyText) {
                                blocks[targetIndex] = newBlock
                            } else {
                                blocks.add(targetIndex + 1, newBlock)
                            }
                        } else {
                            blocks.add(newBlock)
                        }
                        targetFocusBlockId = newId
                        focusedBlockId = newId
                        // The note logic handles saveAndExit inside NoteEditorScreen down below.
                        // We will need to launch a ViewModel method if saveAndExit is out of scope here.
                        // Actually, let's just trigger a re-render. Since we updated `blocks`, 
                        // the BackHandler/Save logic will pick it up, or we can just leave it to auto-save.
                    }
                }
            }
        }
    }

    var triggerVoiceDictation by remember { mutableStateOf(initialOpenVoiceDictation) }
    
    LaunchedEffect(triggerVoiceDictation) {
        if (triggerVoiceDictation) {
            triggerVoiceDictation = false
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val lang = getKeyboardLanguage(context)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
                putExtra("android.speech.extra.GET_AUDIO", true)
            }
            try {
                voiceRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "El reconocimiento de voz no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingScannedPdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingScannedPdfUri = pendingScannedPdfUriString?.let { Uri.parse(it) }
    var scannedPdfName by rememberSaveable { mutableStateOf("CamNotes Scanner") }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanningResult?.pdf != null) {
                val pdfUri = scanningResult.pdf!!.uri
                scannedPdfName = "CamNotes Scanner"
                pendingScannedPdfUriString = pdfUri.toString()
            }
        }
    }

    fun startDocumentScanner() {
        val activity = (context as? Activity) ?: context.findActivity()
        if (activity != null) {
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(15)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(activity)
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

    var triggerScanner by remember { mutableStateOf(initialOpenDocumentScanner) }

    LaunchedEffect(triggerScanner) {
        if (triggerScanner) {
            startDocumentScanner()
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
            saveSnapshot()
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
    var showStatsDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var createTableRows by remember { mutableIntStateOf(3) }
    var createTableCols by remember { mutableIntStateOf(3) }


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

    val editorBgColor = NoteColors.getColor(colorIndex, isPaperDark)

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
                syncEnabled = it.syncEnabled
                reminderTimestamp = it.reminderTimestamp
                attachedImageUri = it.imageUri
                createdAt = it.createdAt
                updatedAt = it.updatedAt

                val deserialized = NoteBlockSerializer.deserialize(it.content)
                blocks.clear()
                blocks.addAll(normalizeHeadingBlocks(deserialized))
                undoHistory.clear()
                redoHistory.clear()
            }
        } else {
            undoHistory.clear()
            redoHistory.clear()
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
            syncEnabled = syncEnabled,
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
            EditorTopAppBar(
                amber = amber,
                editorBgColor = editorBgColor,
                isPaperDark = isPaperDark,
                canUndo = undoHistory.isNotEmpty(),
                canRedo = redoHistory.isNotEmpty(),
                isPinned = isPinned,
                isLocked = isLocked,
                syncEnabled = syncEnabled,
                isArchived = isArchived,
                reminderTimestamp = reminderTimestamp,
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
                onBack = { saveAndExit(navigateBack = true) },
                onUndo = {
                    if (undoHistory.isNotEmpty()) {
                        val currentSnapshot = blocks.toList()
                        redoHistory.add(currentSnapshot)
                        if (redoHistory.size > 30) {
                            redoHistory.removeAt(0)
                        }
                        val previousSnapshot = undoHistory.removeAt(undoHistory.lastIndex)
                        blocks.clear()
                        blocks.addAll(normalizeHeadingBlocks(previousSnapshot))
                        blockTextFieldValues.clear()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onRedo = {
                    if (redoHistory.isNotEmpty()) {
                        val currentSnapshot = blocks.toList()
                        undoHistory.add(currentSnapshot)
                        if (undoHistory.size > 30) {
                            undoHistory.removeAt(0)
                        }
                        val nextSnapshot = redoHistory.removeAt(redoHistory.lastIndex)
                        blocks.clear()
                        blocks.addAll(normalizeHeadingBlocks(nextSnapshot))
                        blockTextFieldValues.clear()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onReminderClick = {
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
                onShareClick = {
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
                            is NoteBlock.Math -> "[Fórmula Math: ${it.latexCode}]"
                        }
                    }
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "$title\n\n$plainText")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir nota"))
                },
                onExportPdfClick = {
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
                onTogglePin = { isPinned = !isPinned },
                onToggleLock = {
                    isLocked = !isLocked
                    lockPin = ""
                    val msg = if (isLocked) "Nota movida a Privado" else "Nota quitada de Privado"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onOpenPaperStyle = { showPaperDialog = true },
                onToggleSync = {
                    syncEnabled = !syncEnabled
                    saveAndExit(navigateBack = false)
                },
                onOpenStats = { showStatsDialog = true },
                onToggleArchive = { isArchived = !isArchived }
            )
        },
        containerColor = editorBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            val localDensity = LocalDensity.current
            val editorLineHeight = (customFontSizeSp * 1.4f).sp
            val lineHeightPx = with(localDensity) { editorLineHeight.toPx() }
            
            // Cálculo exacto del margen superior basado en la estructura del Header (Fecha + Recordatorio + Título + Spacers + Padding)
            val reminderHeight = if (reminderTimestamp != null) 44.dp else 0.dp
            val headerMarginPx = with(localDensity) {
                120.dp.toPx().coerceAtLeast(
                    (6.dp + 14.dp + 12.dp + 10.dp + 4.dp + reminderHeight).toPx() + (12.sp.toPx() * 1.4f) + (28.sp.toPx() * 1.4f)
                )
            }
            // El offset del renglón es la altura exacta de una línea de texto para que cada renglón quede situado justo debajo de la línea de escritura
            val startYOffsetPx = lineHeightPx

            PaperBackground(
                paperStyle = paperStyle,
                modifier = Modifier.fillMaxSize(),
                lineHeightPx = lineHeightPx,
                startYOffsetPx = startYOffsetPx,
                headerMarginPx = headerMarginPx,
                isDark = isPaperDark
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
                            evaluateNoteLinkTrigger("TITLE", newValue)
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
                            onNext = {
                                if (blocks.isEmpty()) {
                                    ensureTextNoteExists()
                                }
                                blocks.firstOrNull()?.id?.let { firstId ->
                                    targetFocusBlockId = firstId
                                    focusedBlockId = firstId
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onNoteLinkClick(titleTextLayoutResult) { linkedId ->
                                viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedBlockId = "TITLE"
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
                                            evaluateNoteLinkTrigger(block.id, sanitized)
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
                                                    val safeNL = newlineIndex.coerceIn(0, sanitized.text.length)
                                                    val headingText = sanitized.text.substring(0, safeNL)
                                                    val remainingText = sanitized.text.substring((safeNL + 1).coerceIn(0, sanitized.text.length))

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
                                            .onNoteLinkClick(textLayoutResult) { linkedId ->
                                                viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                                            }
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
                                        textStyle = textStyle.copy(color = paperTextColor),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                Text(
                                                    text = "Subtítulo...",
                                                    style = textStyle.copy(color = paperHintColor.copy(alpha = 0.5f))
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
                            } else {
                                BasicTextField(
                                    value = tfv,
                                        onValueChange = { newValue ->
                                            val sanitized = sanitizeTextFieldValue(newValue)
                                            evaluateNoteLinkTrigger(block.id, sanitized)
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
                                                    val safeNL = newlineIndex.coerceIn(0, sanitized.text.length)
                                                    val headingText = sanitized.text.substring(0, safeNL)
                                                    val remainingText = sanitized.text.substring((safeNL + 1).coerceIn(0, sanitized.text.length))

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
                                            .onNoteLinkClick(textLayoutResult) { linkedId ->
                                                viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                                            }
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
                                        textStyle = textStyle.copy(color = paperTextColor),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                val showPlaceholder = when (block.style) {
                                                    TextStyleType.TITLE -> "Título de sección..."
                                                    else -> if (blocks.size <= 1 && title.isEmpty()) "Escribe algo..." else ""
                                                }
                                                if (showPlaceholder.isNotEmpty()) {
                                                    Text(
                                                        text = showPlaceholder,
                                                        style = textStyle.copy(color = paperHintColor.copy(alpha = 0.5f))
                                                    )
                                                }
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
                                        .border(2.dp, if (block.isChecked) amber else if (isPaperDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.35f), CircleShape)
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
                                        evaluateNoteLinkTrigger(block.id, sanitized)
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
                                        .onNoteLinkClick(textLayoutResult) { linkedId ->
                                            viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                                        }
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
                                        tint = paperHintColor.copy(alpha = 0.6f),
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
                                        evaluateNoteLinkTrigger(block.id, sanitized)
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
                                        .onNoteLinkClick(textLayoutResult) { linkedId ->
                                            viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                                        }
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
                                        color = paperTextColor
                                    ),
                                    cursorBrush = SolidColor(amber),
                                    visualTransformation = remember(isPaperDark) { markdownVisualTransformation(isPaperDark, amber) },
                                    decorationBox = { innerTextField ->
                                        if (block.content.isEmpty()) {
                                            Text(
                                                text = "Elemento de lista...",
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
                                    if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                                )
                            } else {
                                if (isPaperDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
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
                                            evaluateNoteLinkTrigger(block.id, sanitized)
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
                                            .onNoteLinkClick(textLayoutResult) { linkedId ->
                                                viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                                            }
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
                                color = if (isPaperDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA),
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
                                            color = paperTextColor
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
                                isPaperDark = isPaperDark,
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
                        is NoteBlock.Math -> {
                            MathBlockItem(
                                block = block,
                                onUpdateBlock = { updatedMath ->
                                    blocks[index] = updatedMath
                                },
                                onDeleteBlock = {
                                    removeBlock(index)
                                },
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
                                    saveSnapshot()
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
                    visible = showNoteLinker,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NoteLinkSuggestionMenu(
                        linkQuery = linkQuery,
                        notes = matchingNotesForLinker,
                        amberColor = amber,
                        isDark = isPaperDark,
                        onNoteSelected = { selectedNoteId, selectedNoteTitle ->
                            insertNoteLink(selectedNoteId, selectedNoteTitle)
                        },
                        onDismiss = {
                            showNoteLinker = false
                            linkQuery = ""
                            activeNoteLinkerBlockId = null
                        }
                    )
                }

                AnimatedVisibility(
                    visible = activeHashtagInfo != null && suggestedSmartTags.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SmartTagsSuggestionRow(
                        tags = suggestedSmartTags,
                        amberColor = amber,
                        isDark = isPaperDark,
                        onSelectTag = { item ->
                            applySmartTagSelection(item)
                        }
                    )
                }

                AnimatedVisibility(
                    visible = focusedBlockId != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EditorAccessoryBar(
                            amber = amber,
                            isPaperDark = isPaperDark,
                            isBoldActive = isBoldActive,
                            isItalicActive = isItalicActive,
                            isHighlightActive = isHighlightActive,
                            customFontSizeSp = customFontSizeSp,
                            onFormatBody = {
                                transformOrInsertBlock { id, content ->
                                    NoteBlock.Text(id = id, content = content, style = TextStyleType.BODY, isIndented = false)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatTitle = {
                                applyHeadingStyleToCurrentBlock(TextStyleType.TITLE)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatSubtitle = {
                                applyHeadingStyleToCurrentBlock(TextStyleType.SUBTITLE)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatChecklist = {
                                toggleOrInsertChecklist()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatBulletList = {
                                transformOrInsertBlock { id, content ->
                                    NoteBlock.SimpleList(id = id, content = content, isNumbered = false)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatNumberedList = {
                                transformOrInsertBlock { id, content ->
                                    NoteBlock.SimpleList(id = id, content = content, isNumbered = true)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatCallout = {
                                transformOrInsertBlock { id, content ->
                                    NoteBlock.CalloutBox(id = id, content = content)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatTable = {
                                createTableRows = 3
                                createTableCols = 3
                                showCreateTableDialog = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatDrawing = {
                                saveSnapshot()
                                blocks.add(NoteBlock.Drawing())
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatPdf = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatScanner = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                startDocumentScanner()
                            },
                            onFormatMath = {
                                transformOrInsertBlock { id, content ->
                                    NoteBlock.Math(id = id, latexCode = content)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onAddImage = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onStartAudioDictation = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                val lang = getKeyboardLanguage(context)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
                                putExtra("android.speech.extra.GET_AUDIO", true)
                            }
                            try {
                                voiceRecognitionLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "El reconocimiento de voz no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onToggleBold = { isBoldActive = !isBoldActive },
                        onToggleItalic = { isItalicActive = !isItalicActive },
                        onToggleHighlight = {
                            val currentId = focusedBlockId
                            if (currentId != null) {
                                val tfv = blockTextFieldValues[currentId]
                                if (tfv != null && !tfv.selection.collapsed) {
                                    val text = tfv.text
                                    val start = tfv.selection.min.coerceIn(0, text.length)
                                    val end = tfv.selection.max.coerceIn(start, text.length)
                                    val selectedText = text.substring(start, end)

                                    val (newText, newSelection) = if (selectedText.startsWith("==") && selectedText.endsWith("==") && selectedText.length >= 4) {
                                        val unhighlighted = selectedText.substring(2, (selectedText.length - 2).coerceIn(2, selectedText.length))
                                        val updated = text.substring(0, start) + unhighlighted + text.substring(end.coerceIn(0, text.length))
                                        val sel = TextRange(start, start + unhighlighted.length)
                                        Pair(updated, sel)
                                    } else {
                                        val highlighted = "==$selectedText=="
                                        val updated = text.substring(0, start) + highlighted + text.substring(end.coerceIn(0, text.length))
                                        val sel = TextRange(start, start + highlighted.length)
                                        Pair(updated, sel)
                                    }

                                    val newTfv = tfv.copy(text = newText, selection = newSelection)
                                    blockTextFieldValues[currentId] = newTfv

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
                        onOpenFontSizeRuler = { showFontSizeCarouselSheet = true }
                    )
                }
                }
            }
        }
    }

    // Carousel Vertical Font Size Bottom Sheet (1 a 150 sp - Rodillo/Rueda vertical)
    EditorFontSizeCarouselSheet(
        showFontSizeCarouselSheet = showFontSizeCarouselSheet,
        customFontSizeSp = customFontSizeSp,
        isPaperDark = isPaperDark,
        isDark = isDark,
        amber = amber,
        onFontSizeChange = { customFontSizeSp = it },
        onDismissRequest = { showFontSizeCarouselSheet = false }
    )

    if (showCreateTableDialog) {
        CreateTableBottomSheet(
            initialRows = createTableRows,
            initialCols = createTableCols,
            onCreateTable = { r, c, header, zebra ->
                showCreateTableDialog = false
                saveSnapshot()
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

    EditorReminderBottomSheet(
        showReminderDialog = showReminderDialog,
        reminderTimestamp = reminderTimestamp,
        isDark = isDark,
        amber = amber,
        onSaveReminder = { selectedTimestamp, offsets ->
            reminderTimestamp = selectedTimestamp
            if (noteId > 0) {
                viewModel.setReminder(
                    context,
                    noteId,
                    selectedTimestamp,
                    title,
                    NoteBlockSerializer.serialize(blocks),
                    offsets
                )
            }
        },
        onDeleteReminder = {
            reminderTimestamp = null
            if (noteId > 0) {
                viewModel.cancelReminder(context, noteId)
            }
        },
        onDismissRequest = { showReminderDialog = false }
    )

    EditorColorPickerDialog(
        showColorDialog = showColorDialog,
        colorIndex = colorIndex,
        isDark = isDark,
        amber = amber,
        onColorSelected = { colorIndex = it },
        onDismissRequest = { showColorDialog = false }
    )

    EditorPaperStyleDialog(
        showPaperDialog = showPaperDialog,
        paperStyle = paperStyle,
        amber = amber,
        onStyleSelected = { paperStyle = it },
        onDismissRequest = { showPaperDialog = false }
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
        onDismissRequest = { showStatsDialog = false }
    )

    EditorScannedPdfNameDialog(
        pdfUri = pendingScannedPdfUri,
        scannedPdfName = scannedPdfName,
        onNameChange = { scannedPdfName = it },
        onConfirm = { uri, name ->
            viewModel.insertPdfBlock(uri, customFileName = name) { newBlock ->
                if (!blocks.any { b -> b.id == newBlock.id }) {
                    blocks.add(newBlock)
                }
            }
            pendingScannedPdfUriString = null
        },
        onDismissRequest = { pendingScannedPdfUriString = null }
    )
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
