package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.mutableLongStateOf
import com.example.util.sanitizeNoteSnippet
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.TagFilterRow
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.viewmodel.NavTab
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val sortSheetState = rememberModalBottomSheetState()
    val actionSheetState = rememberModalBottomSheetState()
    var showSortSheet by remember { mutableStateOf(false) }

    var selectedNoteForAction by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToUnlock by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToSetPin by remember { mutableStateOf<NoteEntity?>(null) }

    // Batch Action Dialog States
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchFolderSheet by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
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
                        onMoreOptionsClick = { showSortSheet = true },
                        isSelectionMode = uiState.isSelectionMode,
                        selectedCount = uiState.selectedNoteIds.size,
                        totalCount = currentListIds.size,
                        onExitSelectionMode = { viewModel.exitSelectionMode() },
                        onSelectAll = { viewModel.selectAllNotes(currentListIds) },
                        onDeselectAll = { viewModel.deselectAllNotes() }
                    )

                    if (uiState.activeTab == NavTab.NOTES && !uiState.isSelectionMode) {
                        TagFilterRow(
                            tags = uiState.availableTags,
                            selectedTag = uiState.selectedTag,
                            onTagSelected = { viewModel.selectTag(it) },
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
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
                                        columns = StaggeredGridCells.Fixed(2),
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
                                                    onLongClick = { viewModel.enterSelectionMode(note.id) },
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
                                                onLongClick = { viewModel.enterSelectionMode(note.id) },
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
                        modifier = Modifier.align(Alignment.BottomCenter)
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
