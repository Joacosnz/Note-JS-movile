package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import com.example.ui.utils.BiometricAuthManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.NoteEntity
import com.example.ui.components.NoteJsBottomToolbar
import com.example.ui.components.NoteLockDialog
import com.example.ui.components.NoteReminderBottomSheet
import com.example.viewmodel.NavTab
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen

@Composable
fun FoldersScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // State for creating new Folder
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // State for CRUD actions on Folder
    var targetFolderForCrud by remember { mutableStateOf<String?>(null) }
    var showFolderOptionsDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var renameFolderNewName by remember { mutableStateOf("") }
    var showDeleteFolderConfirmDialog by remember { mutableStateOf(false) }

    // State for CRUD actions on Tag
    var targetTagForCrud by remember { mutableStateOf<String?>(null) }
    var showTagOptionsDialog by remember { mutableStateOf(false) }
    var showRenameTagDialog by remember { mutableStateOf(false) }
    var renameTagNewName by remember { mutableStateOf("") }
    var showDeleteTagConfirmDialog by remember { mutableStateOf(false) }

    var noteForReminder by remember { mutableStateOf<NoteEntity?>(null) }
    var showQuickReminderSheet by remember { mutableStateOf(false) }
    var showPrivateFolderPinDialog by remember { mutableStateOf(false) }

    BackHandler {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.navigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_screen")
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header with Title and "+ Nueva carpeta"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Administración",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                newFolderName = ""
                                showNewFolderDialog = true
                            },
                            modifier = Modifier.testTag("btn_new_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nueva carpeta",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.navigateTo(Screen.Settings)
                            },
                            modifier = Modifier.testTag("btn_settings_folders")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECCIÓN CARPETAS
                    item {
                        Text(
                            text = "CARPETAS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                val allFolderNames = linkedSetOf<String>()
                                uiState.customFolders.forEach { allFolderNames.add(it.name) }
                                uiState.activeNotes.forEach {
                                    if (it.folder.isNotBlank() &&
                                        !it.folder.equals("Notas", ignoreCase = true) &&
                                        !it.folder.equals("Todas las notas", ignoreCase = true)) {
                                        allFolderNames.add(it.folder)
                                    }
                                }

                                if (allFolderNames.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No hay carpetas creadas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    allFolderNames.forEachIndexed { index, folderName ->
                                        val count = uiState.folderNoteCounts[folderName]
                                            ?: uiState.activeNotes.count { it.folder.equals(folderName, ignoreCase = true) }
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 48.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                        FolderRowItem(
                                            icon = Icons.Default.Folder,
                                            title = folderName,
                                            count = count,
                                            tint = MaterialTheme.colorScheme.primary,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                targetFolderForCrud = folderName
                                                showFolderOptionsDialog = true
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                targetFolderForCrud = folderName
                                                showFolderOptionsDialog = true
                                            },
                                            testTag = "folder_item_$folderName"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN ETIQUETAS
                    val tagsList = uiState.availableTags.filter {
                        it != "Todas" &&
                            !it.equals("🔒 Privado", ignoreCase = true) &&
                            !it.equals("🔒 Privadas", ignoreCase = true) &&
                            !it.equals("Bloqueadas", ignoreCase = true) &&
                            !it.equals("Privado", ignoreCase = true)
                    }
                    if (tagsList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ETIQUETAS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    tagsList.forEachIndexed { index, tag ->
                                        val count = uiState.tagNoteCounts[tag] ?: 0
                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 48.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        }
                                        FolderRowItem(
                                            icon = Icons.Default.Tag,
                                            title = "#$tag",
                                            count = count,
                                            tint = MaterialTheme.colorScheme.primary,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                targetTagForCrud = tag
                                                showTagOptionsDialog = true
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                targetTagForCrud = tag
                                                showTagOptionsDialog = true
                                            },
                                            testTag = "tag_folder_$tag"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ACCESOS RÁPIDOS
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ACCESOS RÁPIDOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                FolderRowItem(
                                    icon = Icons.Default.Lock,
                                    title = "🔒 Privado",
                                    count = uiState.tagNoteCounts["🔒 Privado"] ?: uiState.tagNoteCounts["🔒 Privadas"] ?: 0,
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (uiState.isBiometricLockEnabled) {
                                            BiometricAuthManager.authenticateWithBiometrics(
                                                context = context,
                                                title = "Acceso a Carpeta Privada",
                                                subtitle = "Verifica tu identidad con huella dactilar o reconocimiento facial",
                                                onSuccess = {
                                                    viewModel.openPrivateFolder()
                                                },
                                                onFallbackToPin = {
                                                    showPrivateFolderPinDialog = true
                                                },
                                                onError = { _ -> }
                                            )
                                        } else {
                                            showPrivateFolderPinDialog = true
                                        }
                                    },
                                    testTag = "folder_private_notes",
                                    showTrailingIcon = false
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                FolderRowItem(
                                    icon = Icons.Default.Notifications,
                                    title = "Calendario y Recordatorios",
                                    count = uiState.remindersCount,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateTo(Screen.Reminders())
                                    },
                                    testTag = "folder_reminders",
                                    showTrailingIcon = true
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                FolderRowItem(
                                    icon = Icons.Default.Archive,
                                    title = "Archivadas",
                                    count = uiState.archivedCount,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectTab(NavTab.ARCHIVED)
                                    },
                                    testTag = "folder_archive",
                                    showTrailingIcon = false
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                FolderRowItem(
                                    icon = Icons.Default.Delete,
                                    title = "Papelera / Eliminadas",
                                    count = uiState.trashedCount,
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectTab(NavTab.TRASH)
                                    },
                                    testTag = "folder_trash",
                                    showTrailingIcon = false
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                FolderRowItem(
                                    icon = Icons.Default.PictureAsPdf,
                                    title = "📄 Biblioteca PDF",
                                    count = 0,
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateTo(Screen.PdfLibrary)
                                    },
                                    testTag = "folder_pdf_library",
                                    showTrailingIcon = false
                                )
                            }
                        }
                    }
                }
            }
        }

        NoteJsBottomToolbar(
            onNewNote = {
                viewModel.navigateTo(Screen.Editor())
            },
            isHomeActive = false,
            isFoldersActive = true,
            isRemindersActive = false,
            onHomeClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.selectFolder(null)
            },
            onFoldersClick = {},
            onRemindersClick = { viewModel.navigateTo(Screen.Reminders()) },
            onVoiceNote = {
                viewModel.navigateTo(Screen.Editor(0L, openVoiceDictation = true))
            },
            onReadPdf = {
                viewModel.navigateTo(Screen.PdfLibrary)
            },
            onReminder = {
                viewModel.navigateTo(
                    Screen.Reminders(
                        initialCreateNewReminder = true,
                        triggerTimestamp = System.currentTimeMillis()
                    )
                )
            },
            modifier = Modifier.align(Alignment.BottomCenter)
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

    // --- DIÁLOGOS CRUD PARA CARPETAS ---

    // 1. Diálogo de opciones para carpeta
    if (showFolderOptionsDialog && targetFolderForCrud != null) {
        val folderName = targetFolderForCrud!!
        AlertDialog(
            onDismissRequest = { showFolderOptionsDialog = false },
            title = { Text("Carpeta: $folderName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showFolderOptionsDialog = false
                            viewModel.selectFolder(folderName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver notas de la carpeta", modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showFolderOptionsDialog = false
                            renameFolderNewName = folderName
                            showRenameFolderDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar nombre", modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showFolderOptionsDialog = false
                            showDeleteFolderConfirmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar carpeta", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFolderOptionsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. Diálogo renombrar carpeta
    if (showRenameFolderDialog && targetFolderForCrud != null) {
        val oldFolderName = targetFolderForCrud!!
        AlertDialog(
            onDismissRequest = { showRenameFolderDialog = false },
            title = { Text("Renombrar carpeta") },
            text = {
                OutlinedTextField(
                    value = renameFolderNewName,
                    onValueChange = { renameFolderNewName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameFolderNewName.isNotBlank()) {
                            viewModel.renameFolder(oldFolderName, renameFolderNewName.trim())
                            showRenameFolderDialog = false
                            targetFolderForCrud = null
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 3. Diálogo confirmar eliminar carpeta
    if (showDeleteFolderConfirmDialog && targetFolderForCrud != null) {
        val folderName = targetFolderForCrud!!
        AlertDialog(
            onDismissRequest = { showDeleteFolderConfirmDialog = false },
            title = { Text("¿Eliminar carpeta '$folderName'?") },
            text = {
                Text("Las notas pertenecientes a esta carpeta no se eliminarán, pero pasarán a no tener carpeta asignada.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folderName)
                        showDeleteFolderConfirmDialog = false
                        targetFolderForCrud = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- DIÁLOGOS CRUD PARA ETIQUETAS ---

    // 1. Diálogo de opciones para etiqueta
    if (showTagOptionsDialog && targetTagForCrud != null) {
        val tagName = targetTagForCrud!!
        AlertDialog(
            onDismissRequest = { showTagOptionsDialog = false },
            title = { Text("Etiqueta: #$tagName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showTagOptionsDialog = false
                            viewModel.selectTagAndNavigate(tagName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver notas con #$tagName", modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showTagOptionsDialog = false
                            renameTagNewName = tagName
                            showRenameTagDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar nombre", modifier = Modifier.weight(1f))
                    }

                    TextButton(
                        onClick = {
                            showTagOptionsDialog = false
                            showDeleteTagConfirmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar etiqueta", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTagOptionsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. Diálogo renombrar etiqueta
    if (showRenameTagDialog && targetTagForCrud != null) {
        val oldTagName = targetTagForCrud!!
        AlertDialog(
            onDismissRequest = { showRenameTagDialog = false },
            title = { Text("Renombrar etiqueta") },
            text = {
                OutlinedTextField(
                    value = renameTagNewName,
                    onValueChange = { renameTagNewName = it },
                    label = { Text("Nuevo nombre (#)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameTagNewName.isNotBlank()) {
                            viewModel.renameTag(oldTagName, renameTagNewName.trim())
                            showRenameTagDialog = false
                            targetTagForCrud = null
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameTagDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 3. Diálogo confirmar eliminar etiqueta
    if (showDeleteTagConfirmDialog && targetTagForCrud != null) {
        val tagName = targetTagForCrud!!
        AlertDialog(
            onDismissRequest = { showDeleteTagConfirmDialog = false },
            title = { Text("¿Eliminar etiqueta '#$tagName'?") },
            text = {
                Text("Esta etiqueta se eliminará de todas las notas que la contengan. Ninguna nota será eliminada.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tagName)
                        showDeleteTagConfirmDialog = false
                        targetTagForCrud = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTagConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo Nueva carpeta
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Nueva carpeta") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nombre de la carpeta") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_folder_name")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_new_folder")
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPrivateFolderPinDialog) {
        NoteLockDialog(
            isSettingPin = false,
            expectedPin = uiState.defaultLockPin,
            title = "Acceso a Carpeta Privada",
            subtitle = "Usa tu huella dactilar, reconocimiento facial o ingresa tu PIN de seguridad",
            autoTriggerBiometric = false,
            isBiometricEnabled = uiState.isBiometricLockEnabled,
            onConfirm = { _ ->
                showPrivateFolderPinDialog = false
                viewModel.openPrivateFolder()
            },
            onUnlockSuccess = {
                showPrivateFolderPinDialog = false
                viewModel.openPrivateFolder()
            },
            onDismiss = {
                showPrivateFolderPinDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRowItem(
    icon: ImageVector,
    title: String,
    count: Int,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    testTag: String,
    showTrailingIcon: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (showTrailingIcon) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
