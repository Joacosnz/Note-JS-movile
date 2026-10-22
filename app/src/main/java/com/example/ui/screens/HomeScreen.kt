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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import com.example.ui.components.NoteJsBottomToolbar
import com.example.ui.components.NoteJsNoteRow
import com.example.ui.components.NoteJsTopBar
import com.example.ui.components.ReorderableGroupedNotesCard
import com.example.ui.components.BatchActionBottomBar
import com.example.ui.components.BatchDeleteDialog
import com.example.ui.components.BatchFolderBottomSheet
import com.example.ui.components.BatchTagDialog
import com.example.ui.components.FolderTreeBottomSheet
import com.example.ui.components.KeepColorPickerBottomSheet
import com.example.ui.components.NoteActionBottomSheet
import com.example.ui.components.NoteCard
import com.example.ui.components.NoteContentPreview
import com.example.ui.components.NoteReminderBottomSheet
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.TagFilterRow
import com.example.ui.utils.BiometricAuthManager
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
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
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val sortSheetState = rememberModalBottomSheetState()
    val actionSheetState = rememberModalBottomSheetState()
    var showSortSheet by remember { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }

    var selectedNoteForAction by remember { mutableStateOf<NoteEntity?>(null) }
    var noteForReminder by remember { mutableStateOf<NoteEntity?>(null) }
    var showQuickReminderSheet by remember { mutableStateOf(false) }

    // Batch Action Dialog States
    var showBatchColorSheet by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchFolderSheet by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var previewNote by remember { mutableStateOf<NoteEntity?>(null) }
    var scannedPdfUri by remember { mutableStateOf<Uri?>(null) }

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

    LaunchedEffect(uiState.activeTab, uiState.selectedFolder, uiState.selectedTag) {
        if (uiState.activeTab != NavTab.NOTES ||
            uiState.selectedFolder != null ||
            (uiState.selectedTag != null &&
                !uiState.selectedTag.equals("🔒 Privado", ignoreCase = true) &&
                !uiState.selectedTag.equals("🔒 Privadas", ignoreCase = true) &&
                !uiState.selectedTag.equals("Bloqueadas", ignoreCase = true))
        ) {
            viewModel.closePrivateFolder()
        }
    }

    val isLockedTabSelected = uiState.selectedTag.equals("🔒 Privado", ignoreCase = true) ||
        uiState.selectedTag.equals("🔒 Privadas", ignoreCase = true) ||
        uiState.selectedTag.equals("Bloqueadas", ignoreCase = true) ||
        uiState.selectedTag.equals("Privado", ignoreCase = true)
    val currentTitle = when {
        uiState.selectedFolder != null &&
            !uiState.selectedFolder.equals("Todas", ignoreCase = true) &&
            !uiState.selectedFolder.equals("Todas las notas", ignoreCase = true) -> uiState.selectedFolder!!
        isLockedTabSelected -> "Notas Privadas"
        uiState.selectedTag != null &&
            !uiState.selectedTag.equals("Todas", ignoreCase = true) -> "#${uiState.selectedTag}"
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
        NavTab.PDF_LIBRARY -> uiState.activeNotes
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
                    NoteJsTopBar(
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
                        onToggleGridView = { viewModel.toggleViewMode() },
                        availableFolders = uiState.availableFolders,
                        selectedFolder = uiState.selectedFolder,
                        onSelectFolder = { viewModel.selectFolder(it) },
                        onOpenFolderSheet = { showFolderSheet = true }
                    )

                    if (!uiState.isSelectionMode) {
                        if (uiState.searchQuery.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${currentList.size} ${if (currentList.size == 1) "nota encontrada" else "notas encontradas"}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = amber
                                    )
                                )
                                TextButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Limpiar búsqueda",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        } else if (uiState.activeTab == NavTab.NOTES) {
                            val userFolders = uiState.availableFolders.filter {
                                !it.equals("Notas", ignoreCase = true) && !it.equals("Todas las notas", ignoreCase = true)
                            }
                            TagFilterRow(
                                folders = userFolders,
                                tags = uiState.availableTags.filter { it != "Todas" },
                                selectedFolder = uiState.selectedFolder,
                                selectedTag = uiState.selectedTag,
                                onFolderSelected = { viewModel.selectFolder(it) },
                                onTagSelected = { tag ->
                                    if (tag.equals("🔒 Privado", ignoreCase = true) || tag.equals("🔒 Privadas", ignoreCase = true) || tag.equals("Bloqueadas", ignoreCase = true)) {
                                        BiometricAuthManager.authenticateWithBiometrics(
                                            context = context,
                                            onSuccess = { viewModel.openPrivateFolder() },
                                            onError = { _ -> }
                                        )
                                    } else {
                                        viewModel.selectTag(tag)
                                    }
                                },
                                onClearFilters = {
                                    viewModel.selectFolder(null)
                                    viewModel.selectTag(null)
                                },
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
                                        .testTag("notejs_more_options")
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
                                val isFiltered = uiState.searchQuery.isNotBlank() || uiState.selectedTag != null || uiState.selectedFolder != null
                                NoteJsEmptyState(
                                    message = if (isLockedTabSelected)
                                        "Sin notas privadas"
                                    else if (uiState.searchQuery.isNotBlank())
                                        "Sin resultados para \"${uiState.searchQuery}\""
                                    else if (isFiltered)
                                        "No se encontraron notas"
                                    else
                                        "Sin notas",
                                    submessage = if (isLockedTabSelected)
                                        "Las notas que protejas con PIN aparecerán aquí."
                                    else if (uiState.searchQuery.isNotBlank())
                                        "Intenta buscar por otro título, palabra clave o contenido."
                                    else if (isFiltered)
                                        "Intenta con otra búsqueda, carpeta o etiqueta."
                                    else
                                        "Toca '+' para crear tu primera nota."
                                )
                            } else {
                                if (uiState.isGridView) {
                                    // Intelligent Adaptive Grid (Note JS style)
                                    LazyVerticalStaggeredGrid(
                                        columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalItemSpacing = 12.dp,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (pinned.isNotEmpty()) {
                                            item(span = StaggeredGridItemSpan.FullLine) {
                                                NoteJsSectionHeader("FIJADAS")
                                            }
                                            items(pinned, key = { it.id }) { note ->
                                                NoteCard(
                                                    note = note,
                                                    isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                    onClick = {
                                                        openNoteOrUnlock(note, viewModel)
                                                    },
                                                    onLongClick = { previewNote = note },
                                                    onTogglePin = { viewModel.togglePin(note) },
                                                    isSelectionMode = uiState.isSelectionMode,
                                                    isSelected = uiState.selectedNoteIds.contains(note.id),
                                                    onSelectToggle = { viewModel.toggleNoteSelection(note.id) },
                                                    modifier = Modifier
                                                        .animateItem(placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                                        .fillMaxWidth()
                                                )
                                            }

                                            if (others.isNotEmpty()) {
                                                item(span = StaggeredGridItemSpan.FullLine) {
                                                    NoteJsSectionHeader("NOTAS")
                                                }
                                            }
                                        }

                                        items(others, key = { it.id }) { note ->
                                            NoteCard(
                                                note = note,
                                                isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                onClick = {
                                                    openNoteOrUnlock(note, viewModel)
                                                },
                                                onLongClick = { previewNote = note },
                                                onTogglePin = { viewModel.togglePin(note) },
                                                isSelectionMode = uiState.isSelectionMode,
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                onSelectToggle = { viewModel.toggleNoteSelection(note.id) },
                                                modifier = Modifier
                                                    .animateItem(placementSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                                    .fillMaxWidth()
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
                                            item { NoteJsSectionHeader("FIJADAS") }
                                            item {
                                                ReorderableGroupedNotesCard(
                                                    notes = pinned,
                                                    unlockedNoteIds = uiState.unlockedNoteIds,
                                                    selectedNoteIds = uiState.selectedNoteIds,
                                                    isSelectionMode = uiState.isSelectionMode,
                                                    onNoteClick = { note -> openNoteOrUnlock(note, viewModel) },
                                                    onNoteLongClick = { note -> previewNote = note },
                                                    onSelectToggle = { noteId -> viewModel.toggleNoteSelection(noteId) },
                                                    onReorder = { reorderedPinned ->
                                                        val fullOrder = reorderedPinned.map { it.id } + others.map { it.id }
                                                        viewModel.reorderNotes(fullOrder)
                                                    }
                                                )
                                            }
                                            item { Spacer(modifier = Modifier.height(16.dp)) }
                                        }

                                        if (others.isNotEmpty()) {
                                            if (pinned.isNotEmpty()) {
                                                item {
                                                    NoteJsSectionHeader("NOTAS")
                                                }
                                            }
                                            item {
                                                ReorderableGroupedNotesCard(
                                                    notes = others,
                                                    unlockedNoteIds = uiState.unlockedNoteIds,
                                                    selectedNoteIds = uiState.selectedNoteIds,
                                                    isSelectionMode = uiState.isSelectionMode,
                                                    onNoteClick = { note -> openNoteOrUnlock(note, viewModel) },
                                                    onNoteLongClick = { note -> previewNote = note },
                                                    onSelectToggle = { noteId -> viewModel.toggleNoteSelection(noteId) },
                                                    onReorder = { reorderedOthers ->
                                                        val fullOrder = pinned.map { it.id } + reorderedOthers.map { it.id }
                                                        viewModel.reorderNotes(fullOrder)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        NavTab.REMINDERS -> {
                            if (uiState.remindersNotes.isEmpty()) {
                                NoteJsEmptyState("No hay recordatorios pendientes", "Las notas con recordatorios programados aparecerán aquí.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF1C1C1E),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                uiState.remindersNotes.forEachIndexed { index, note ->
                                                    NoteJsNoteRow(
                                                        note = note,
                                                        isUnlocked = uiState.unlockedNoteIds.contains(note.id),
                                                        isLastItem = index == uiState.remindersNotes.size - 1,
                                                        onClick = {
                                                            openNoteOrUnlock(note, viewModel)
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
                                NoteJsEmptyState("No hay notas archivadas", "Las notas que archives aparecerán aquí.")
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFF1C1C1E),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                uiState.archivedNotes.forEachIndexed { index, note ->
                                                    NoteJsNoteRow(
                                                        note = note,
                                                        isUnlocked = true,
                                                        isLastItem = index == uiState.archivedNotes.size - 1,
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
                                    NoteJsEmptyState("Papelera vacía", "No hay notas eliminadas.")
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = Color(0xFF1C1C1E),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column {
                                                    uiState.trashedNotes.forEachIndexed { index, note ->
                                                        NoteJsNoteRow(
                                                            note = note,
                                                            isUnlocked = true,
                                                            isLastItem = index == uiState.trashedNotes.size - 1,
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
                        NavTab.PDF_LIBRARY -> {
                            LaunchedEffect(Unit) {
                                viewModel.navigateTo(Screen.PdfLibrary)
                            }
                        }
                    }
                }

                // Bottom Toolbar or Floating Batch Action Bar
                if (uiState.isSelectionMode) {
                    BatchActionBottomBar(
                        onColorClick = { showBatchColorSheet = true },
                        onTagClick = { showBatchTagDialog = true },
                        onFolderClick = { showBatchFolderSheet = true },
                        onDeleteClick = { showBatchDeleteDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                    NoteJsBottomToolbar(
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
                            viewModel.navigateTo(Screen.PdfLibrary)
                        },
                        onScanDocument = {
                            val activity = context.findActivity()
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
                                        homeScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                                    .addOnFailureListener {
                                        viewModel.navigateTo(Screen.Editor(0L, openDocumentScanner = true))
                                    }
                            } else {
                                viewModel.navigateTo(Screen.Editor(0L, openDocumentScanner = true))
                            }
                        },
                        onReminder = {
                            showQuickReminderSheet = true
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
                            previewNote = null
                            openNoteOrUnlock(currentNote, viewModel)
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
                            viewModel.toggleLockNote(currentNote)
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

    if (showFolderSheet) {
        FolderTreeBottomSheet(
            uiState = uiState,
            onSelectFolder = { folder ->
                viewModel.setFolderFilter(folder)
                showFolderSheet = false
            },
            onSelectNote = { note ->
                openNoteOrUnlock(note, viewModel)
                showFolderSheet = false
            },
            onSelectPrivateFolder = {
                showFolderSheet = false
                BiometricAuthManager.authenticateWithBiometrics(
                    context = context,
                    onSuccess = { viewModel.openPrivateFolder() },
                    onError = { _ -> }
                )
            },
            onDismiss = { showFolderSheet = false }
        )
    }

    if (showBatchColorSheet) {
        KeepColorPickerBottomSheet(
            show = showBatchColorSheet,
            selectedColorIndex = 0,
            isDark = isDark,
            onColorSelected = { selectedColor ->
                viewModel.batchUpdateColor(selectedColor)
                showBatchColorSheet = false
            },
            onDismissRequest = { showBatchColorSheet = false }
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
                viewModel.toggleLockNote(note)
                selectedNoteForAction = null
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
                noteForReminder = null
            },
            onCancelReminder = {
                viewModel.cancelReminder(context, note.id)
                noteForReminder = null
            }
        )
    }

    if (showQuickReminderSheet) {
        val dummyQuickNote = remember {
            NoteEntity(
                title = "Recordatorio rápido",
                content = ""
            )
        }
        NoteReminderBottomSheet(
            note = dummyQuickNote,
            onDismiss = { showQuickReminderSheet = false },
            onSaveReminder = { timestamp, updatedTitle, updatedContent, offsets ->
                val finalTitle = updatedTitle.ifBlank { "Recordatorio rápido" }
                viewModel.saveNote(
                    title = finalTitle,
                    content = updatedContent,
                    reminderTimestamp = timestamp,
                    onSaved = { savedId ->
                        viewModel.setReminder(context, savedId, timestamp, finalTitle, updatedContent, offsets)
                    }
                )
                showQuickReminderSheet = false
            },
            onCancelReminder = {
                showQuickReminderSheet = false
            }
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
    viewModel: NotesViewModel
) {
    if (note.noteType == "DRAWING") {
        viewModel.navigateTo(Screen.Drawing(note.id))
    } else {
        viewModel.navigateTo(Screen.Editor(note.id, NoteType.valueOf(note.noteType)))
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
            val title = if (note.isLocked) "Nota Protegida" else note.title.ifBlank { "Sin Título" }
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
            
            Spacer(modifier = Modifier.height(12.dp))

            if (note.isLocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Nota Protegida",
                        tint = if (isDark) com.example.ui.theme.NoteJsAmberDark else com.example.ui.theme.NoteJsAmber,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "Nota protegida",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                        color = textColor.copy(alpha = 0.9f)
                    )
                }
            } else {
                NoteContentPreview(
                    content = note.content,
                    textColor = textColor,
                    maxBlocks = 4
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
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

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
private fun NoteJsSectionHeader(title: String) {
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
private fun NoteJsEmptyState(message: String, submessage: String) {
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



