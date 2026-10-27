package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AllInbox
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.NotesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderTreeBottomSheet(
    uiState: NotesUiState,
    onSelectFolder: (String?) -> Unit,
    onSelectNote: (NoteEntity) -> Unit,
    onSelectPrivateFolder: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

    // Agrupa las notas por carpeta
    val notesByFolder = remember(uiState.allActiveNotes) {
        uiState.allActiveNotes.groupBy { it.folder }
    }

    // Estado para rastrear qué carpetas están expandidas
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    val isAllSelected = uiState.selectedFolder.isNullOrBlank() ||
            uiState.selectedFolder.equals("Todas", ignoreCase = true) ||
            uiState.selectedFolder.equals("Todas las notas", ignoreCase = true)

    val userFolders = remember(uiState.availableFolders) {
        uiState.availableFolders.filter {
            !it.equals("Todas las notas", ignoreCase = true) &&
                    !it.equals("Todas", ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Explorador de Carpetas",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("folder_tree_lazy_column")
            ) {
                // Item estático inicial para "Todas las notas"
                item {
                    val totalNotesCount = uiState.allActiveNotes.count { !it.isLocked }
                    val isNormalAllSelected = isAllSelected && !uiState.isViewingPrivateFolder
                    Surface(
                        onClick = {
                            onSelectFolder(null)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isNormalAllSelected) amber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AllInbox,
                                contentDescription = "Todas las notas",
                                tint = if (isNormalAllSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Todas las notas",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isNormalAllSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isNormalAllSelected) amber else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            BadgeCount(count = totalNotesCount, isSelected = isNormalAllSelected, amber = amber)
                            if (isNormalAllSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Seleccionado",
                                    tint = amber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Item estático para "🔒 Carpeta Privada"
                item {
                    val privateNotesCount = uiState.allActiveNotes.count { it.isLocked }
                    val isPrivateSelected = uiState.isViewingPrivateFolder
                    Surface(
                        onClick = {
                            onSelectPrivateFolder()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPrivateSelected) amber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Carpeta Privada",
                                tint = if (isPrivateSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "🔒 Privado",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isPrivateSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isPrivateSelected) amber else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            BadgeCount(count = privateNotesCount, isSelected = isPrivateSelected, amber = amber)
                            if (isPrivateSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Seleccionado",
                                    tint = amber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Iteración sobre carpetas disponibles
                items(userFolders, key = { it }) { folder ->
                    val isFolderSelected = uiState.selectedFolder != null &&
                            uiState.selectedFolder.equals(folder, ignoreCase = true)
                    val folderNotes = notesByFolder[folder] ?: emptyList()
                    val isExpanded = expandedFolders.contains(folder)

                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 90f else 0f,
                        label = "arrowRotation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        // A) Header de la Carpeta (Row)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isFolderSelected) amber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Flecha animada a la izquierda
                                IconButton(
                                    onClick = {
                                        expandedFolders = if (isExpanded) {
                                            expandedFolders - folder
                                        } else {
                                            expandedFolders + folder
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = if (isExpanded) "Colapsar $folder" else "Expandir $folder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectFolder(folder)
                                            onDismiss()
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = if (isFolderSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = folder,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isFolderSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isFolderSelected) amber else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BadgeCount(count = folderNotes.size, isSelected = isFolderSelected, amber = amber)
                                    if (isFolderSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Seleccionado",
                                            tint = amber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // B) Contenido Expandido (Notas)
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 6.dp)
                            ) {
                                if (folderNotes.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 48.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Carpeta vacía",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                } else {
                                    folderNotes.forEach { note ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onSelectNote(note)
                                                    onDismiss()
                                                }
                                                .padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val isPdf = note.pdfPath.isNotBlank() || note.noteType.equals("PDF", ignoreCase = true)
                                            Icon(
                                                imageVector = if (isPdf) Icons.Rounded.PictureAsPdf else Icons.AutoMirrored.Rounded.Article,
                                                contentDescription = null,
                                                tint = if (isPdf) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            val displayTitle = if (note.isLocked) "Nota Protegida" else note.title.ifBlank { "Sin título" }
                                            Text(
                                                text = displayTitle,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BadgeCount(
    count: Int,
    isSelected: Boolean,
    amber: Color
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) amber.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
