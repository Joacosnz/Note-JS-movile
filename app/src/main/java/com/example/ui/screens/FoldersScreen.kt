package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppleBottomToolbar
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
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    BackHandler {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        viewModel.navigateTo(Screen.Home)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_screen"),
        bottomBar = {
            AppleBottomToolbar(
                onNewNote = {
                    viewModel.navigateTo(Screen.Editor())
                },
                isHomeActive = false,
                isFoldersActive = true,
                onHomeClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.navigateTo(Screen.Home)
                },
                onFoldersClick = {}
            )
        },
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
                    text = "Carpetas",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

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
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sección Principal
                item {
                    Text(
                        text = "EN MI TELÉFONO",
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
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Todas las notas
                            FolderRowItem(
                                icon = Icons.Default.Folder,
                                title = "Todas las notas",
                                count = uiState.allActiveNotesCount,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectFolder(null)
                                },
                                testTag = "folder_all_notes"
                            )

                            // Default folders or custom folders
                            val allFolderNames = linkedSetOf<String>()
                            uiState.customFolders.forEach { allFolderNames.add(it.name) }
                            uiState.activeNotes.forEach { 
                                if (it.folder.isNotBlank() && 
                                    !it.folder.equals("Notas", ignoreCase = true) && 
                                    !it.folder.equals("Todas las notas", ignoreCase = true)) {
                                    allFolderNames.add(it.folder)
                                }
                            }

                            allFolderNames.forEach { folderName ->
                                val count = uiState.folderNoteCounts[folderName] ?: uiState.activeNotes.count { it.folder.equals(folderName, ignoreCase = true) }
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 48.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                FolderRowItem(
                                    icon = Icons.Default.Folder,
                                    title = folderName,
                                    count = count,
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectFolder(folderName)
                                    },
                                    testTag = "folder_item_$folderName"
                                )
                            }
                        }
                    }
                }

                // Sección de Etiquetas (si hay etiquetas detectadas)
                if (uiState.availableTags.size > 1) {
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
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                val tagsList = uiState.availableTags.filter { it != "Todas" }
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
                                        icon = Icons.Default.Label,
                                        title = "#$tag",
                                        count = count,
                                        tint = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.selectTagAndNavigate(tag)
                                        },
                                        testTag = "tag_folder_$tag"
                                    )
                                }
                            }
                        }
                    }
                }

                // Sección Secundaria
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
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            FolderRowItem(
                                icon = Icons.Default.Notifications,
                                title = "Con recordatorio",
                                count = uiState.remindersCount,
                                tint = MaterialTheme.colorScheme.secondary,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectTab(NavTab.REMINDERS)
                                },
                                testTag = "folder_reminders"
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
                                testTag = "folder_archive"
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
                                testTag = "folder_trash"
                            )
                        }
                    }
                }
            }
        }
    }

    // New Folder Dialog
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
}

@Composable
private fun FolderRowItem(
    icon: ImageVector,
    title: String,
    count: Int,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
