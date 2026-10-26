package com.example.ui.screens

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.isActive
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.io.FileOutputStream
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Job
import com.example.util.getKeyboardLanguage
import com.example.util.MathEvaluator
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
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
import com.example.ui.components.InlineMathBlockItem
import com.example.ui.components.InsertInlineMathDialog
import com.example.ui.components.onInlineMathClick
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.model.SyncState
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
import com.example.ui.components.editor.EditorTopBar
import com.example.ui.components.editor.EditorTopBar
import com.example.ui.components.editor.EditorBlockList
import com.example.ui.components.editor.EditorDialogs
import com.example.ui.components.SmartTagItem
import com.example.ui.components.SmartTagsSuggestionRow
import com.example.ui.components.NoteLinkSuggestionMenu
import com.example.ui.components.markdownVisualTransformation
import com.example.ui.components.onNoteLinkClick
import com.example.ui.components.sanitizeTextFieldValue
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.util.FileManager
import com.example.util.GeminiAudioTranscriber
import com.example.util.ImageHelper
import com.example.util.NoteBlockSerializer
import com.example.util.NotePdfExporter
import com.example.util.WebLinkMetadataFetcher
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
    initialOpenDocumentScanner: Boolean = false,
    initialSharedText: String? = null,
    initialSharedTitle: String? = null
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

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
    var noteIcon by remember { mutableStateOf<String?>(null) }
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

    var showInlineMathDialog by remember { mutableStateOf(false) }
    var inlineMathDialogInitialCode by remember { mutableStateOf("") }
    var inlineMathDialogTargetBlockId by remember { mutableStateOf<String?>(null) }
    var inlineMathDialogIsEditing by remember { mutableStateOf(false) }
    var inlineMathDialogRange by remember { mutableStateOf<TextRange?>(null) }

    var showWebLinkDialog by remember { mutableStateOf(false) }
    var webLinkInitialText by remember { mutableStateOf("") }
    var webLinkTargetBlockId by remember { mutableStateOf<String?>(null) }
    var webLinkSelectionRange by remember { mutableStateOf<TextRange?>(null) }

    var showExportPdfChoiceDialog by remember { mutableStateOf(false) }

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

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim()

            if (!spokenText.isNullOrBlank()) {
                saveSnapshot()
                val targetId = focusedBlockId
                if (targetId == "TITLE") {
                    val current = titleTextFieldValue.text
                    val updated = if (current.isBlank()) spokenText else "$current $spokenText"
                    titleTextFieldValue = TextFieldValue(updated, TextRange(updated.length))
                    title = updated
                } else if (targetId != null) {
                    val currentTfv = blockTextFieldValues[targetId] ?: TextFieldValue("")
                    val currentText = currentTfv.text
                    val updated = if (currentText.isBlank()) spokenText else "$currentText $spokenText"
                    blockTextFieldValues[targetId] = TextFieldValue(updated, TextRange(updated.length))
                    val idx = blocks.indexOfFirst { it.id == targetId }
                    if (idx != -1) {
                        when (val b = blocks[idx]) {
                            is NoteBlock.Text -> blocks[idx] = b.copy(content = updated)
                            is NoteBlock.Checklist -> blocks[idx] = b.copy(content = updated)
                            is NoteBlock.SimpleList -> blocks[idx] = b.copy(content = updated)
                            is NoteBlock.CalloutBox -> blocks[idx] = b.copy(content = updated)
                            else -> {}
                        }
                    }
                } else {
                    val newId = UUID.randomUUID().toString()
                    blocks.add(NoteBlock.Text(id = newId, content = spokenText))
                    blockTextFieldValues[newId] = TextFieldValue(spokenText, TextRange(spokenText.length))
                    focusedBlockId = newId
                }
                if (title.isBlank()) {
                    title = "Nota de dictado"
                    titleTextFieldValue = TextFieldValue(title, TextRange(title.length))
                }
            }
        }
    }

    fun launchAndroidVoiceDictation() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora para dictar tu nota...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "El dictado por voz de Android no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar el dictado por voz", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        launchAndroidVoiceDictation()
    }

    fun startVoiceDictationWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            launchAndroidVoiceDictation()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var hasHandledInitialVoiceDictation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialOpenVoiceDictation) {
        if (initialOpenVoiceDictation && !hasHandledInitialVoiceDictation) {
            hasHandledInitialVoiceDictation = true
            startVoiceDictationWithPermission()
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
            val removed = blocks.removeAt(index)
            if (removed is NoteBlock.Drawing) {
                FileManager.deleteFile(removed.filePath)
                if (removed.pathData.isNotBlank()) {
                    FileManager.deleteFile(removed.pathData)
                }
            }
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

    LaunchedEffect(noteId, initialSharedText, initialSharedTitle) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                title = it.title
                noteIcon = it.icon
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
            noteType = initialType
            if (initialOpenVoiceDictation && title.isBlank()) {
                title = "Nota de voz rápida"
                titleTextFieldValue = TextFieldValue(title, TextRange(title.length))
            } else if (!initialSharedTitle.isNullOrBlank() && title.isBlank()) {
                title = initialSharedTitle
                titleTextFieldValue = TextFieldValue(title, TextRange(title.length))
            } else if (!initialSharedText.isNullOrBlank()) {
                title = ""
                titleTextFieldValue = TextFieldValue("")
            }
            val sharedContent = initialSharedText?.trim() ?: ""
            if (sharedContent.isNotEmpty()) {
                val initialId = UUID.randomUUID().toString()
                blocks.clear()
                blockTextFieldValues.clear()
                blocks.add(NoteBlock.Text(id = initialId, content = sharedContent))
                blockTextFieldValues[initialId] = TextFieldValue(sharedContent, TextRange(sharedContent.length))
                targetFocusBlockId = initialId
                focusedBlockId = initialId
            } else if (blocks.isEmpty()) {
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
            icon = noteIcon,
            onSaved = { savedId ->
                if (reminderTimestamp != null) {
                    viewModel.setReminder(context, savedId, reminderTimestamp!!, finalTitle, serializedContent)
                }
                for (blk in blocks) {
                    if (blk is NoteBlock.Checklist) {
                        if (blk.reminderTimestamp != null && !blk.isChecked && blk.reminderTimestamp!! > System.currentTimeMillis()) {
                            com.example.util.ReminderScheduler.scheduleItemReminder(
                                context = context,
                                noteId = savedId,
                                itemId = blk.id,
                                timestamp = blk.reminderTimestamp!!,
                                title = finalTitle.ifBlank { "Recordatorio de lista" },
                                content = blk.content.ifBlank { "Elemento de lista" }
                            )
                        } else if (blk.isChecked || blk.reminderTimestamp == null) {
                            com.example.util.ReminderScheduler.cancelItemReminder(context, savedId, blk.id)
                        }
                    }
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
            EditorTopBar(
                amber = amber,
                editorBgColor = editorBgColor,
                isPaperDark = isPaperDark,
                canUndo = undoHistory.isNotEmpty(),
                canRedo = redoHistory.isNotEmpty(),
                isPinned = isPinned,
                isLocked = isLocked,
                syncEnabled = syncEnabled,
                syncState = syncState,
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
                    val hasTable = blocks.any { it is NoteBlock.Table }
                    if (hasTable || noteType == NoteType.MATH) {
                        showExportPdfChoiceDialog = true
                    } else {
                        val targetLabel = "nota"
                        Toast.makeText(context, "Generando PDF con PdfBox...", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            try {
                                val pdfFile = NotePdfExporter.exportWithPdfBox(context, title, blocks, noteType)
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
                                context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF ($targetLabel)"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "Error al generar el PDF: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                onOpenColorPicker = { showColorDialog = true },
                onToggleSync = {
                    syncEnabled = !syncEnabled
                    saveAndExit(navigateBack = false)
                },
                onOpenStats = { showStatsDialog = true },
                onToggleArchive = { isArchived = !isArchived }
            )
        },
        floatingActionButton = {},
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

            EditorBlockList(
                blocks = blocks,
                listState = lazyListState,
                visibleBlocksWithIndex = visibleBlocksWithIndex,
                h2SectionBlockIds = h2SectionBlockIds,
                focusRequesters = focusRequesters,
                blockTextFieldValues = blockTextFieldValues,
                targetFocusBlockId = targetFocusBlockId,
                focusedBlockId = focusedBlockId,
                onFocusedBlockIdChange = { focusedBlockId = it },
                onRequestFocus = { id ->
                    targetFocusBlockId = id
                    focusedBlockId = id
                },
                formattedHeaderDate = formattedHeaderDate,
                reminderTimestamp = reminderTimestamp,
                onReminderClick = { showReminderDialog = true },
                onDeleteReminder = {
                    reminderTimestamp = null
                    if (noteId > 0) {
                        viewModel.cancelReminder(context, noteId)
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        noteIcon = noteIcon,
        onNoteIconChange = { noteIcon = it },
        titleTextFieldValue = titleTextFieldValue,
        onTitleValueChange = { newValue ->
            titleTextFieldValue = newValue
            title = newValue.text
        },
                onTitleNext = {
                    if (blocks.isEmpty()) {
                        ensureTextNoteExists()
                    }
                    blocks.firstOrNull()?.id?.let { firstId ->
                        targetFocusBlockId = firstId
                        focusedBlockId = firstId
                    }
                },
                onNoteLinkClick = { linkedId ->
                    viewModel.navigateTo(Screen.Editor(noteId = linkedId))
                },
                onInlineMathClick = { targetBlockId, code, isEditing, range ->
                    inlineMathDialogTargetBlockId = targetBlockId
                    inlineMathDialogInitialCode = code
                    inlineMathDialogIsEditing = isEditing
                    inlineMathDialogRange = range
                    showInlineMathDialog = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onUpdateBlock = { idx, updatedBlock ->
                    blocks[idx] = updatedBlock
                },
                onRemoveBlock = { idx ->
                    removeBlock(idx)
                },
                onInsertBlockAt = { idx, newBlock ->
                    blocks.add(idx, newBlock)
                },
                onDrawingClick = {
                    saveAndExit(navigateBack = false)
                    viewModel.navigateTo(Screen.Drawing(noteId))
                },
                onExportTablePdf = { tableBlock ->
                    Toast.makeText(context, "Exportando hoja de cálculo con PdfBox...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        try {
                            val pdfFile = NotePdfExporter.exportCalculationSheet(context, title, tableBlock)
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
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Hoja de Cálculo"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error al generar el PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenPdfFullScreen = { pdfPath ->
                    saveAndExit(navigateBack = false)
                    viewModel.navigateTo(Screen.PdfViewer(noteId = noteId, pdfPath = pdfPath))
                },
                attachedImageUri = attachedImageUri,
                onRemoveAttachedImage = { attachedImageUri = null },
                onAddBodyBlockBelow = {
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
                },
                evaluateNoteLinkTrigger = { blockId, tfv ->
                    evaluateNoteLinkTrigger(blockId, tfv)
                },
                amber = amber,
                paperTextColor = paperTextColor,
                paperHintColor = paperHintColor,
                isPaperDark = isPaperDark,
                customFontSizeSp = customFontSizeSp,
                isBoldActive = isBoldActive,
                isItalicActive = isItalicActive,
                isHighlightActive = isHighlightActive,
                title = title
            )

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
                            onFormatInlineMath = {
                                ensureTextNoteExists()
                                val targetId = focusedBlockId ?: blocks.firstOrNull { it is NoteBlock.Text }?.id
                                inlineMathDialogTargetBlockId = targetId
                                inlineMathDialogInitialCode = ""
                                inlineMathDialogIsEditing = false
                                val currentTfv = if (targetId == "TITLE") titleTextFieldValue else (targetId?.let { blockTextFieldValues[it] })
                                inlineMathDialogRange = currentTfv?.selection
                                showInlineMathDialog = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onFormatWebLink = {
                                ensureTextNoteExists()
                                val targetId = focusedBlockId ?: blocks.firstOrNull {
                                    it is NoteBlock.Text || it is NoteBlock.Checklist || it is NoteBlock.SimpleList || it is NoteBlock.CalloutBox
                                }?.id
                                webLinkTargetBlockId = targetId
                                val currentTfv = if (targetId == "TITLE") titleTextFieldValue else (targetId?.let { blockTextFieldValues[it] })
                                val sel = currentTfv?.selection
                                webLinkSelectionRange = sel
                                webLinkInitialText = if (currentTfv != null && sel != null && !sel.collapsed) {
                                    val start = sel.min.coerceIn(0, currentTfv.text.length)
                                    val end = sel.max.coerceIn(start, currentTfv.text.length)
                                    currentTfv.text.substring(start, end)
                                } else {
                                    ""
                                }
                                showWebLinkDialog = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onAddImage = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onStartAudioDictation = {
                                startVoiceDictationWithPermission()
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



    EditorDialogs(
        showFontSizeCarouselSheet = showFontSizeCarouselSheet,
        customFontSizeSp = customFontSizeSp,
        onFontSizeChange = { customFontSizeSp = it },
        onDismissFontSizeCarouselSheet = { showFontSizeCarouselSheet = false },
        showCreateTableDialog = showCreateTableDialog,
        createTableRows = createTableRows,
        createTableCols = createTableCols,
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
        onDismissCreateTable = { showCreateTableDialog = false },
        showReminderDialog = showReminderDialog,
        reminderTimestamp = reminderTimestamp,
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
        onDismissReminder = { showReminderDialog = false },
        showColorDialog = showColorDialog,
        colorIndex = colorIndex,
        onColorSelected = { newColor ->
            colorIndex = newColor
            if (noteId > 0) {
                viewModel.updateNoteColor(noteId, newColor)
            }
        },
        onDismissColorDialog = { showColorDialog = false },
        showPaperDialog = showPaperDialog,
        paperStyle = paperStyle,
        onStyleSelected = { paperStyle = it },
        onDismissPaperDialog = { showPaperDialog = false },
        showStatsDialog = showStatsDialog,
        blocks = blocks,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        onDismissStatsDialog = { showStatsDialog = false },
        pendingScannedPdfUri = pendingScannedPdfUri,
        scannedPdfName = scannedPdfName,
        onScannedPdfNameChange = { scannedPdfName = it },
        onConfirmScannedPdf = { uri, name ->
            viewModel.insertPdfBlock(uri, customFileName = name) { newBlock ->
                if (!blocks.any { b -> b.id == newBlock.id }) {
                    blocks.add(newBlock)
                }
            }
            pendingScannedPdfUriString = null
        },
        onDismissScannedPdf = { pendingScannedPdfUriString = null },
        showInlineMathDialog = showInlineMathDialog,
        inlineMathDialogInitialCode = inlineMathDialogInitialCode,
        inlineMathDialogIsEditing = inlineMathDialogIsEditing,
        onDismissInlineMath = { showInlineMathDialog = false },
        onConfirmInlineMath = { newLatexCode ->
            val targetId = inlineMathDialogTargetBlockId
            if (targetId != null) {
                val currentTfv = if (targetId == "TITLE") titleTextFieldValue else (blockTextFieldValues[targetId] ?: TextFieldValue(""))
                val text = currentTfv.text
                val selection = currentTfv.selection
                val mathTag = "[math:$newLatexCode]"
                val newText: String
                val newSelection: TextRange

                if (inlineMathDialogIsEditing && inlineMathDialogRange != null) {
                    val range = inlineMathDialogRange!!
                    val start = range.min.coerceIn(0, text.length)
                    val end = range.max.coerceIn(start, text.length)
                    val suffix = if (end < text.length && text[end].isWhitespace()) "" else " "
                    val replacement = "$mathTag$suffix"
                    newText = text.substring(0, start) + replacement + text.substring(end)
                    newSelection = TextRange(start + replacement.length)
                } else if (selection.min != selection.max) {
                    val start = selection.min.coerceIn(0, text.length)
                    val end = selection.max.coerceIn(start, text.length)
                    val suffix = if (end < text.length && text[end].isWhitespace()) "" else " "
                    val replacement = "$mathTag$suffix"
                    newText = text.substring(0, start) + replacement + text.substring(end)
                    newSelection = TextRange(start + replacement.length)
                } else {
                    val cursor = selection.min.coerceIn(0, text.length)
                    val prefix = if (cursor > 0 && !text[cursor - 1].isWhitespace()) " " else ""
                    val suffix = if (cursor < text.length && text[cursor].isWhitespace()) "" else " "
                    val insertStr = "$prefix$mathTag$suffix"
                    newText = text.substring(0, cursor) + insertStr + text.substring(cursor)
                    newSelection = TextRange(cursor + insertStr.length)
                }

                if (targetId == "TITLE") {
                    titleTextFieldValue = TextFieldValue(newText, newSelection)
                    title = newText
                } else {
                    blockTextFieldValues[targetId] = TextFieldValue(newText, newSelection)
                    val realIdx = blocks.indexOfFirst { it.id == targetId }
                    if (realIdx != -1) {
                        when (val block = blocks[realIdx]) {
                            is NoteBlock.Text -> blocks[realIdx] = block.copy(content = newText)
                            is NoteBlock.Checklist -> blocks[realIdx] = block.copy(content = newText)
                            is NoteBlock.SimpleList -> blocks[realIdx] = block.copy(content = newText)
                            is NoteBlock.CalloutBox -> blocks[realIdx] = block.copy(content = newText)
                            else -> {}
                        }
                    }
                    targetFocusBlockId = targetId
                    focusedBlockId = targetId
                }
            }
            showInlineMathDialog = false
        },
        onDeleteInlineMath = if (inlineMathDialogIsEditing && inlineMathDialogRange != null) {
            {
                val targetId = inlineMathDialogTargetBlockId
                val range = inlineMathDialogRange
                if (targetId != null && range != null) {
                    val currentTfv = if (targetId == "TITLE") titleTextFieldValue else (blockTextFieldValues[targetId] ?: TextFieldValue(""))
                    val text = currentTfv.text
                    val start = range.min.coerceIn(0, text.length)
                    val end = range.max.coerceIn(start, text.length)
                    val newText = text.substring(0, start) + text.substring(end)
                    val newSelection = TextRange(start)
                    if (targetId == "TITLE") {
                        titleTextFieldValue = TextFieldValue(newText, newSelection)
                        title = newText
                    } else {
                        blockTextFieldValues[targetId] = TextFieldValue(newText, newSelection)
                        val realIdx = blocks.indexOfFirst { it.id == targetId }
                        if (realIdx != -1) {
                            when (val block = blocks[realIdx]) {
                                is NoteBlock.Text -> blocks[realIdx] = block.copy(content = newText)
                                is NoteBlock.Checklist -> blocks[realIdx] = block.copy(content = newText)
                                is NoteBlock.SimpleList -> blocks[realIdx] = block.copy(content = newText)
                                is NoteBlock.CalloutBox -> blocks[realIdx] = block.copy(content = newText)
                                else -> {}
                            }
                        }
                    }
                }
                showInlineMathDialog = false
            }
        } else null,
        showWebLinkDialog = showWebLinkDialog,
        webLinkInitialText = webLinkInitialText,
        onDismissWebLink = { showWebLinkDialog = false },
        onConfirmWebLink = { url, customTitle, cardMode ->
            saveSnapshot()
            WebLinkMetadataFetcher.setDisplayMode(context, url, cardMode)
            val linkMarkdown = if (!customTitle.isNullOrBlank()) {
                val safeTitle = customTitle.trim().replace("[", "(").replace("]", ")")
                "[$safeTitle]($url)"
            } else {
                url
            }

            val targetId = webLinkTargetBlockId ?: focusedBlockId ?: blocks.firstOrNull {
                it is NoteBlock.Text || it is NoteBlock.Checklist || it is NoteBlock.SimpleList || it is NoteBlock.CalloutBox
            }?.id

            if (targetId != null && targetId != "TITLE") {
                val currentTfv = blockTextFieldValues[targetId] ?: TextFieldValue("")
                val text = currentTfv.text
                val range = webLinkSelectionRange ?: currentTfv.selection
                val newText: String
                val newSelection: TextRange
                if (range.min != range.max) {
                    val start = range.min.coerceIn(0, text.length)
                    val end = range.max.coerceIn(start, text.length)
                    newText = text.substring(0, start) + linkMarkdown + text.substring(end)
                    newSelection = TextRange(start + linkMarkdown.length)
                } else {
                    val cursor = range.min.coerceIn(0, text.length)
                    val prefix = if (cursor > 0 && !text[cursor - 1].isWhitespace()) " " else ""
                    val suffix = if (cursor < text.length && !text[cursor].isWhitespace()) " " else ""
                    val insertStr = "$prefix$linkMarkdown$suffix"
                    newText = text.substring(0, cursor) + insertStr + text.substring(cursor)
                    newSelection = TextRange(cursor + insertStr.length)
                }
                blockTextFieldValues[targetId] = TextFieldValue(newText, newSelection)
                val realIdx = blocks.indexOfFirst { it.id == targetId }
                if (realIdx != -1) {
                    when (val block = blocks[realIdx]) {
                        is NoteBlock.Text -> blocks[realIdx] = block.copy(content = newText)
                        is NoteBlock.Checklist -> blocks[realIdx] = block.copy(content = newText)
                        is NoteBlock.SimpleList -> blocks[realIdx] = block.copy(content = newText)
                        is NoteBlock.CalloutBox -> blocks[realIdx] = block.copy(content = newText)
                        else -> {}
                    }
                }
                targetFocusBlockId = targetId
                focusedBlockId = targetId
            } else {
                val newBlockId = UUID.randomUUID().toString()
                blocks.add(NoteBlock.Text(id = newBlockId, content = linkMarkdown))
                blockTextFieldValues[newBlockId] = TextFieldValue(linkMarkdown, TextRange(linkMarkdown.length))
                targetFocusBlockId = newBlockId
                focusedBlockId = newBlockId
            }
            showWebLinkDialog = false
        },
        showExportPdfChoiceDialog = showExportPdfChoiceDialog,
        onDismissExportPdfChoice = { showExportPdfChoiceDialog = false },
        onExportCompletePdf = {
            showExportPdfChoiceDialog = false
            Toast.makeText(context, "Generando PDF con PdfBox...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val pdfFile = NotePdfExporter.exportWithPdfBox(context, title, blocks, noteType)
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
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF (Nota)"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onExportCalculationSheetPdf = {
            showExportPdfChoiceDialog = false
            val tableBlock = blocks.filterIsInstance<NoteBlock.Table>().firstOrNull()
            Toast.makeText(context, "Generando PDF de hoja de cálculo con PdfBox...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                try {
                    val pdfFile = if (tableBlock != null) {
                        NotePdfExporter.exportCalculationSheet(context, title, tableBlock)
                    } else {
                        NotePdfExporter.exportWithPdfBox(context, title, blocks, noteType)
                    }
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
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF (Hoja de Cálculo)"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        isDark = isDark,
        isPaperDark = isPaperDark,
        amber = amber
    )


}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
