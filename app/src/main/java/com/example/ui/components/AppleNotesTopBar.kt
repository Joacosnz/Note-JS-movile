package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme

@Composable
fun NoteJsTopBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    totalCount: Int = 0,
    onExitSelectionMode: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
    isGridView: Boolean = true,
    onToggleGridView: (() -> Unit)? = null,
    availableFolders: List<String> = emptyList(),
    selectedFolder: String? = null,
    onSelectFolder: ((String?) -> Unit)? = null,
    onOpenFolderSheet: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        if (isSelectionMode) {
            // Selection mode header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onExitSelectionMode,
                    modifier = Modifier.testTag("action_batch_cancel")
                ) {
                    Text(
                        text = "Listo",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = amber
                    )
                }

                Text(
                    text = if (selectedCount == 0) "Seleccionar notas" else "$selectedCount seleccionadas",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = {
                        if (selectedCount == totalCount && totalCount > 0) {
                            onDeselectAll()
                        } else {
                            onSelectAll()
                        }
                    },
                    modifier = Modifier.testTag("action_batch_select_all")
                ) {
                    Text(
                        text = if (selectedCount == totalCount && totalCount > 0) "Deseleccionar" else "Seleccionar todas",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = amber
                    )
                }
            }
        } else {
            // Note JS Large Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onOpenFolderSheet != null || onSelectFolder != null) {
                        IconButton(
                            onClick = { onOpenFolderSheet?.invoke() ?: onSelectFolder?.invoke(null) },
                            modifier = Modifier.testTag("btn_folder_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Seleccionar carpeta",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (onToggleGridView != null) {
                        IconButton(
                            onClick = onToggleGridView,
                            modifier = Modifier.testTag("btn_toggle_grid_view")
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = if (isGridView) "Ver como lista" else "Ver como cuadrícula",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("btn_open_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ajustes",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Note JS Search Bar
            Spacer(modifier = Modifier.height(10.dp))
            NoteJsSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholderText = "Buscar por título o contenido...",
            )
        }
    }
}
