package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.mutableLongStateOf
import com.example.model.NoteBlock
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.util.NoteBlockSerializer
import com.example.util.sanitizeNoteSnippet
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.content.ContextWrapper
import com.google.mlkit.vision.documentscanner.*
import com.example.ui.components.findActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteType
import com.example.ui.components.AppleBottomToolbar
import com.example.ui.components.AppleNoteRow
import com.example.ui.components.AppleNotesTopBar
import com.example.ui.components.BatchActionBottomBar
import com.example.ui.components.BatchDeleteDialog
import com.example.ui.components.BatchFolderBottomSheet
import com.example.ui.components.BatchTagDialog
import com.example.ui.components.NoteActionBottomSheet
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteLockDialog
import com.example.ui.components.NoteReminderBottomSheet
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.TagFilterRow
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.viewmodel.NavTab
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val sortSheetState = rememberModalBottomSheetState()
    val actionSheetState = rememberModalBottomSheetState()
    var showSortSheet by remember { mutableStateOf(false) }

    var selectedNoteForAction by remember { mutableStateOf<NoteEntity?>(null) }
    var noteForReminder by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToUnlock by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToSetPin by remember { mutableStateOf<NoteEntity?>(null) }

    // Batch Action Dialog States
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchFolderSheet by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var previewNote by remember { mutableStateOf<NoteEntity?>(null) }
    var scannedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.createNoteWithPdf(it) { newNoteId ->
                viewModel.navigateTo(Screen.Editor(noteId = newNoteId))
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(15)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember(scannerOptions) {
        GmsDocumentScanning.getClient(scannerOptions)
    }
    val homeScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.uri?.let { pdfUri ->
                scannedPdfUri = pdfUri
            }
        }
    }

    BackHandler(enabled = previewNote != null) {
        previewNote = null
    }

    BackHandler(enabled = previewNote == null) {
        if (uiState.isSelectionMode) {
            viewModel.exitSelectionMode()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Presione de nuevo para salir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (uiState.pendingUndoNote != null) "Deshacer" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastAction()
            }
            viewModel.clearSnackbar()
        }
    }

    val currentTitle = when {
        uiState.selectedFolder != null -> uiState.selectedFolder!!
        uiState.activeTab == NavTab.NOTES -> "Notas"
        uiState.activeTab == NavTab.REMINDERS -> "Con recordatorio"
        uiState.activeTab == NavTab.ARCHIVED -> "Archivadas"
        uiState.activeTab == NavTab.TRASH -> "Papelera"
        else -> "Notas"
    }

    val currentList = when (uiState.activeTab) {
        NavTab.NOTES -> uiState.activeNotes
        NavTab.REMINDERS -> uiState.remindersNotes
        NavTab.ARCHIVED -> uiState.archivedNotes
        NavTab.TRASH -> uiState.trashedNotes
    }
    val currentListIds = currentList.map { it.id }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    AppleNotesTopBar(
                        title = currentTitle,
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        isSelectionMode = uiState.isSelectionMode,
                        selectedCount = uiState.selectedNoteIds.size,
                        totalCount = currentListIds.size,
                        onExitSelectionMode = { viewModel.exitSelectionMode() },
                        onSelectAll = { viewModel.selectAllNotes(currentListIds) },
                        onDeselectAll = { viewModel.deselectAllNotes() },
                        onOpenSettings = { viewModel.navigateTo(Screen.Settings) },
                        isGridView = uiState.isGridView,
                        onToggleGridView = { viewModel.toggleViewMode() }
                    )

                    if (!uiState.isSelectionMode) {
                        if (uiState.activeTab == NavTab.NOTES) {
                            TagFilterRow(
                                tags = uiState.availableTags,
                                selectedTag = uiState.selectedTag,
                                onTagSelected = { viewModel.selectTag(it) },
                                onSortClick = { showSortSheet = true },
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showSortSheet = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("apple_more_options")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Ordenar y filtrar notas",
                                        tint = amber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Content Box
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (uiState.activeTab) {
                        NavTab.NOTES -> {
                            val pinned = uiState.activeNotes.filter { it.isPinned }
                            val others = uiState.activeNotes.filter { !it.isPinned }

                            if (uiState.activeNotes.isEmpty()) {
                                AppleEmptyState(
                                    message = if (uiState.searchQuery.isNotBlank() || uiState.selectedTag != null)
                                        "No se encontraron notas"
                                    else
                                        "Sin notas",
                                    submessage = if (uiState.searchQuery.isNotBlank() || uiState.selectedTag != null)
                                        "Intenta con otra búsqueda o etiqueta."
                                    else
                                        "Toca '+' para crear tu primera nota."
                                )
                            } else {
                                if (uiState.isGridView) {
                                    // iOS Gallery / Grid View
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalItemSpacing = 12.dp,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (pinned.isNotEmpty()) {
                                            item(span = StaggeredGridItemSpan.FullLine) {
                                                AppleSectionHeader("FIJADAS")
                                            }
                                            items(pinned, key = { it.id }) { note ->
                                                NoteCard(
                                                    note = note,
                                                    isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                    onClick = {
                                                        openNoteOrUnlock(note, uiState.unlockedNoteIds, viewModel) {
                                                            noteToUnlock = note
                                                        }
                                                    },
                                                    onLongClick = { previewNote = note },
                                                    onTogglePin = { viewModel.togglePin(note) },
                                                    isSelectionMode = uiState.isSelectionMode,
                                                    isSelected = uiState.selectedNoteIds.contains(note.id),
                                                    onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
                                                )
                                            }

                                            if (others.isNotEmpty()) {
                                                item(span = StaggeredGridItemSpan.FullLine) {
                                                    AppleSectionHeader("NOTAS")
                                                }
                                            }
                                        }

                                        items(others, key = { it.id }) { note ->
                                            NoteCard(
                                                note = note,
                                                isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                onClick = {
                                                    openNoteOrUnlock(note, uiState.unlockedNoteIds, viewModel) {
                                                        noteToUnlock = note
                                                    }
                                                },
                                                onLongClick = { previewNote = note },
                                                onTogglePin = { viewModel.togglePin(note) },
                                                isSelectionMode = uiState.isSelectionMode,
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
                                            )
                                        }
                                    }
                                } else {
                                    // Iconic iOS Inset Grouped Table View
                                    LazyColumn(
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (pinned.isNotEmpty()) {
                                            item { AppleSectionHeader("FIJADAS") }
                                            item {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        pinned.forEachIndexed { index, note ->
                                                            AppleNoteRow(
                                                                note = note,
                                                                isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                                showDivider = index < pinned.size - 1,
                                                                onClick = {
                                                                    openNoteOrUnlock(note, uiState.unlockedNoteIds, viewModel) {
                                                                        noteToUnlock = note
                                                                    }
                                                                },
                                                                onLongClick = { previewNote = note },
                                                                isSelectionMode = uiState.isSelectionMode,
                                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                                onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            item { Spacer(modifier = Modifier.height(16.dp)) }
                                        }

                                        if (others.isNotEmpty()) {
                                            item {
                                                AppleSectionHeader(if (pinned.isNotEmpty()) "NOTAS" else "ANTERIORES")
                                            }
                                            item {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column {
                                                        others.forEachIndexed { index, note ->
                                                            AppleNoteRow(
                                                                note = note,
                                                                isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                                showDivider = index < others.size - 1,
                                                                onClick = {
                                                                    openNoteOrUnlock(note, uiState.unlockedNoteIds, viewModel) {
                                                                        noteToUnlock = note
                                                                    }
                                                                },
                                                                onLongClick = { previewNote = note },
                                                                isSelectionMode = uiState.isSelectionMode,
                                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                                onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
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

                        NavTab.REMINDERS -> {
                            if (uiState.remindersNotes.isEmpty()) {
                                AppleEmptyState("No hay recordatorios pendientes", "Las notas con recordatorios programados aparecerán aquí.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                uiState.remindersNotes.forEachIndexed { index, note ->
                                                    AppleNoteRow(
                                                        note = note,
                                                        isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                        showDivider = index < uiState.remindersNotes.size - 1,
                                                        onClick = {
                                                            openNoteOrUnlock(note, uiState.unlockedNoteIds, viewModel) {
                                                                noteToUnlock = note
                                                            }
                                                        },
                                                        onLongClick = { previewNote = note },
                                                        isSelectionMode = uiState.isSelectionMode,
                                                        isSelected = uiState.selectedNoteIds.contains(note.id),
                                                        onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        NavTab.ARCHIVED -> {
                            if (uiState.archivedNotes.isEmpty()) {
                                AppleEmptyState("No hay notas archivadas", "Las notas que archives aparecerán aquí.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                uiState.archivedNotes.forEachIndexed { index, note ->
                                                    AppleNoteRow(
                                                        note = note,
                                                        isUnlocked = true,
                                                        showDivider = index < uiState.archivedNotes.size - 1,
                                                        onClick = { selectedNoteForAction = note },
                                                        onLongClick = { previewNote = note },
                                                        isSelectionMode = uiState.isSelectionMode,
                                                        isSelected = uiState.selectedNoteIds.contains(note.id),
                                                        onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        NavTab.TRASH -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (uiState.trashedNotes.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${uiState.trashedNotes.size} notas en papelera",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { viewModel.emptyTrash() },
                                            modifier = Modifier.testTag("empty_trash_button")
                                        ) {
                                            Text("Vaciar", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                if (uiState.trashedNotes.isEmpty()) {
                                    AppleEmptyState("Papelera vacía", "No hay notas eliminadas.")
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column {
                                                    uiState.trashedNotes.forEachIndexed { index, note ->
                                                        AppleNoteRow(
                                                            note = note,
                                                            isUnlocked = true,
                                                            showDivider = index < uiState.trashedNotes.size - 1,
                                                            onClick = { selectedNoteForAction = note },
                                                            onLongClick = { previewNote = note },
                                                            isSelectionMode = uiState.isSelectionMode,
                                                            isSelected = uiState.selectedNoteIds.contains(note.id),
                                                            onSelectToggle = { viewModel.toggleNoteSelection(note.id) }
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

                // Bottom Toolbar or Floating Batch Action Bar
                if (uiState.isSelectionMode) {
                    BatchActionBottomBar(
                        onTagClick = { showBatchTagDialog = true },
                        onFolderClick = { showBatchFolderSheet = true },
                        onDeleteClick = { showBatchDeleteDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                    AppleBottomToolbar(
                        isHomeActive = true,
                        isFoldersActive = false,
                        onHomeClick = {
                            viewModel.selectFolder(null)
                            viewModel.setActiveTab(NavTab.NOTES)
                        },
                        onFoldersClick = { viewModel.navigateTo(Screen.Folders) },
                        onNewNote = {
                            viewModel.navigateTo(Screen.Editor(0L))
                        },
                        onVoiceNote = {
                            viewModel.navigateTo(Screen.Editor(0L, openVoiceDictation = true))
                        },
                        onReadPdf = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        },
                        onScanDocument = {
                            scanner.getStartScanIntent(context.findActivity()!!).addOnSuccessListener {
                                homeScannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                            }
                        },
                        onReminder = {
                            noteForReminder = NoteEntity()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = previewNote != null,
            enter = scaleIn(initialScale = 0.85f, animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            val currentNote = previewNote ?: return@AnimatedVisibility

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { previewNote = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 110.dp, bottom = 85.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    FloatingNoteCard(
                        note = currentNote,
                        onClick = {
                            val id = currentNote.id
                            previewNote = null
                            openNoteOrUnlock(currentNote, uiState.unlockedNoteIds, viewModel) {
                                noteToUnlock = currentNote
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    QuickActionsCard(
                        note = currentNote,
                        onPin = { 
                            viewModel.togglePin(currentNote)
                            previewNote = null 
                        },
                        onReminder = {
                            noteForReminder = currentNote
                            previewNote = null
                        },
                        onShare = { 
                            shareNote(context, currentNote)
                            previewNote = null
                        },
                        onLock = {
                            noteToSetPin = currentNote
                            previewNote = null
                        },
                        onDelete = { 
                            viewModel.moveToTrash(currentNote)
                            previewNote = null
                        }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            sheetState = sortSheetState,
            currentOption = uiState.sortOption,
            onOptionSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showBatchTagDialog) {
        BatchTagDialog(
            availableTags = uiState.availableTags,
            onDismiss = { showBatchTagDialog = false },
            onApplyTag = { tag ->
                viewModel.batchAddTag(tag)
                showBatchTagDialog = false
            }
        )
    }

    if (showBatchFolderSheet) {
        BatchFolderBottomSheet(
            folders = uiState.availableFolders,
            onDismiss = { showBatchFolderSheet = false },
            onSelectFolder = { folder ->
                viewModel.batchMoveToFolder(folder)
                showBatchFolderSheet = false
            }
        )
    }

    if (showBatchDeleteDialog) {
        BatchDeleteDialog(
            count = uiState.selectedNoteIds.size,
            isTrashTab = uiState.activeTab == NavTab.TRASH,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                viewModel.batchDelete(uiState.activeTab == NavTab.TRASH)
                showBatchDeleteDialog = false
            }
        )
    }

    selectedNoteForAction?.let { note ->
        NoteActionBottomSheet(
            note = note,
            sheetState = actionSheetState,
            onDismiss = { selectedNoteForAction = null },
            onTogglePin = { viewModel.togglePin(note) },
            onArchive = { viewModel.archiveNote(note) },
            onTrash = { viewModel.moveToTrash(note) },
            onDuplicate = { viewModel.duplicateNote(note) },
            onColorChanged = { newColor ->
                viewModel.saveNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    noteType = NoteType.valueOf(note.noteType),
                    colorIndex = newColor,
                    paperStyle = note.paperStyle,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    tags = note.tags,
                    folder = note.folder,
                    isLocked = note.isLocked,
                    lockPin = note.lockPin
                )
            },
            onPaperStyleChanged = { newPaper ->
                viewModel.saveNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    noteType = NoteType.valueOf(note.noteType),
                    colorIndex = note.colorIndex,
                    paperStyle = newPaper.name,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    tags = note.tags,
                    folder = note.folder,
                    isLocked = note.isLocked,
                    lockPin = note.lockPin
                )
            },
            onLockToggle = {
                if (note.isLocked) {
                    viewModel.saveNote(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        noteType = NoteType.valueOf(note.noteType),
                        colorIndex = note.colorIndex,
                        paperStyle = note.paperStyle,
                        isPinned = note.isPinned,
                        isArchived = note.isArchived,
                        tags = note.tags,
                        folder = note.folder,
                        isLocked = false,
                        lockPin = ""
                    )
                } else {
                    noteToSetPin = note
                }
            },
            onShare = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir nota"))
            },
            onReminder = {
                noteForReminder = note
                selectedNoteForAction = null
            }
        )
    }

    noteForReminder?.let { note ->
        NoteReminderBottomSheet(
            note = note,
            onDismiss = { noteForReminder = null },
            onSaveReminder = { timestamp, updatedTitle, updatedContent, offsets ->
                viewModel.setReminder(context, note.id, timestamp, updatedTitle, updatedContent, offsets)
            },
            onCancelReminder = {
                viewModel.cancelReminder(context, note.id)
            }
        )
    }

    noteToSetPin?.let { note ->
        NoteLockDialog(
            isSettingPin = true,
            onConfirm = { pin ->
                viewModel.saveNote(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    noteType = NoteType.valueOf(note.noteType),
                    colorIndex = note.colorIndex,
                    paperStyle = note.paperStyle,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    tags = note.tags,
                    folder = note.folder,
                    isLocked = true,
                    lockPin = pin
                )
                noteToSetPin = null
            },
            onDismiss = { noteToSetPin = null }
        )
    }

    noteToUnlock?.let { note ->
        NoteLockDialog(
            isSettingPin = false,
            onConfirm = { pinInput ->
                if (viewModel.unlockNote(note.id, pinInput, note.lockPin)) {
                    val targetScreen = if (note.noteType == "DRAWING") {
                        Screen.Drawing(note.id)
                    } else {
                        Screen.Editor(note.id, NoteType.valueOf(note.noteType))
                    }
                    viewModel.navigateTo(targetScreen)
                    noteToUnlock = null
                }
            },
            onDismiss = { noteToUnlock = null }
        )
    }

    if (scannedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { scannedPdfUri = null },
            title = { Text("Documento Escaneado") },
            text = { Text("¿Qué deseas hacer con el documento escaneado?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createNoteWithPdf(scannedPdfUri!!) { id ->
                            viewModel.navigateTo(Screen.Editor(noteId = id))
                        }
                        scannedPdfUri = null
                    }
                ) {
                    Text("Crear Nota")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.savePdfToDownloads(context, scannedPdfUri!!)
                        scannedPdfUri = null
                    }
                ) {
                    Text("Guardar en dispositivo")
                }
            }
        )
    }
}

private fun openNoteOrUnlock(
    note: NoteEntity,
    unlockedIds: Set<Long>,
    viewModel: NotesViewModel,
    onNeedsUnlock: () -> Unit
) {
    if (note.isLocked && !unlockedIds.contains(note.id)) {
        onNeedsUnlock()
    } else {
        if (note.noteType == "DRAWING") {
            viewModel.navigateTo(Screen.Drawing(note.id))
        } else {
            viewModel.navigateTo(Screen.Editor(note.id, NoteType.valueOf(note.noteType)))
        }
    }
}

@Composable
fun FloatingNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val bg = if (isDark) androidx.compose.ui.graphics.Color(0xFF1C1C1E) else androidx.compose.ui.graphics.Color(0xFFF2F2F7)
    val textColor = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
    val secondaryText = if (isDark) Color.Gray else Color.DarkGray

    Surface(
        modifier = modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val title = note.title.ifBlank { "Sin Título" }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(note.updatedAt))
            
            Text(
                text = "$dateStr • ${note.folder}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = secondaryText
            )
            
            val blocks = remember(note.content) { NoteBlockSerializer.deserialize(note.content) }
            val checklistBlocks = remember(blocks) { blocks.filterIsInstance<NoteBlock.Checklist>() }
            val completedCount = checklistBlocks.count { it.isChecked }
            val totalChecklist = checklistBlocks.size

            val snippetText = remember(blocks) {
                val raw = blocks.filter { it !is NoteBlock.Drawing }.joinToString(" ") { block ->
                    when (block) {
                        is NoteBlock.Text -> block.content
                        is NoteBlock.Checklist -> block.content
                        is NoteBlock.SimpleList -> block.content
                        is NoteBlock.CalloutBox -> block.content
                        is NoteBlock.Drawing -> ""
                        is NoteBlock.Table -> block.matrix.joinToString(" ") { row -> row.joinToString(" ") { it.text } }
                        is PdfBlock -> "[PDF: ${block.fileName}]"
                        is NoteBlock.ImageBlock -> "[Imagen]"
                        is NoteBlock.Image -> "[Imagen: ${block.fileName}]"
                        is AudioBlock -> "[Audio: ${String.format(Locale.getDefault(), "%02d:%02d", block.durationMs / 60000, (block.durationMs % 60000) / 1000)}]"
                    }
                }
                sanitizeNoteSnippet(raw)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            if (totalChecklist > 0) {
                Text(
                    text = "☑️ $completedCount/$totalChecklist tareas completadas",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                checklistBlocks.take(12).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isChecked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                            ),
                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (totalChecklist > 12) {
                    Text(
                        text = "+ ${totalChecklist - 12} más...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                    )
                }
            } else {
                val snippet = snippetText.ifBlank { "Sin contenido" }
                Text(
                    text = if (note.isLocked) "Contenido protegido" else snippet,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                    color = textColor.copy(alpha = 0.9f),
                    maxLines = 16,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    note: NoteEntity,
    onPin: () -> Unit,
    onReminder: () -> Unit,
    onShare: () -> Unit,
    onLock: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

    Row(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionCircle(icon = Icons.Default.PushPin, color = amber, onClick = onPin)
        QuickActionCircle(icon = Icons.Default.Notifications, color = amber, onClick = onReminder)
        QuickActionCircle(icon = Icons.Default.Share, color = amber, onClick = onShare)
        QuickActionCircle(icon = Icons.Default.Lock, color = amber, onClick = onLock)
        QuickActionCircle(icon = Icons.Default.Delete, color = MaterialTheme.colorScheme.error, onClick = onDelete)
    }
}

@Composable
private fun QuickActionCircle(
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val circleBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(circleBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
    }
}

fun shareNote(context: Context, note: NoteEntity) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}


@Composable
private fun AppleSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun AppleEmptyState(message: String, submessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = submessage,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



