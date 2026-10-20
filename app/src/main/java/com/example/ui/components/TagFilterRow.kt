package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme

sealed class FilterChipType {
    data object All : FilterChipType()
    data object Locked : FilterChipType()
    data class Folder(val name: String) : FilterChipType()
    data class Tag(val name: String) : FilterChipType()
}

@Composable
fun TagFilterRow(
    folders: List<String> = emptyList(),
    tags: List<String> = emptyList(),
    selectedFolder: String? = null,
    selectedTag: String? = null,
    onFolderSelected: (String) -> Unit = {},
    onTagSelected: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSortClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

    // Construct order: [Todas] -> [🔒 Privadas] -> [Folders...] -> [Tags...]
    val chipItems = mutableListOf<FilterChipType>()
    chipItems.add(FilterChipType.All)
    chipItems.add(FilterChipType.Locked)
    folders.filter { it.isNotBlank() && !it.equals("Notas", ignoreCase = true) && !it.equals("Todas las notas", ignoreCase = true) }
        .distinct()
        .forEach { chipItems.add(FilterChipType.Folder(it)) }
    tags.filter {
        it.isNotBlank() &&
            it != "Todas" &&
            !it.equals("🔒 Privado", ignoreCase = true) &&
            !it.equals("🔒 Privadas", ignoreCase = true) &&
            !it.equals("Bloqueadas", ignoreCase = true) &&
            !it.equals("Privado", ignoreCase = true) &&
            !it.equals("Privadas", ignoreCase = true)
    }
        .distinct()
        .forEach { chipItems.add(FilterChipType.Tag(it)) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onSortClick != null) {
            IconButton(
                onClick = onSortClick,
                modifier = Modifier
                    .padding(start = 16.dp, end = 4.dp)
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

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = if (onSortClick != null) 0.dp else 16.dp,
                end = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = chipItems,
                key = { item ->
                    when (item) {
                        is FilterChipType.All -> "chip_all"
                        is FilterChipType.Locked -> "chip_locked"
                        is FilterChipType.Folder -> "chip_folder_${item.name}"
                        is FilterChipType.Tag -> "chip_tag_${item.name}"
                    }
                }
            ) { item ->
                val isSelected = when (item) {
                    is FilterChipType.All -> selectedFolder == null && (selectedTag == null || selectedTag == "Todas")
                    is FilterChipType.Locked -> selectedFolder == null && (
                        selectedTag.equals("🔒 Privado", ignoreCase = true) ||
                            selectedTag.equals("🔒 Privadas", ignoreCase = true) ||
                            selectedTag.equals("Bloqueadas", ignoreCase = true) ||
                            selectedTag.equals("Privado", ignoreCase = true) ||
                            selectedTag.equals("Privadas", ignoreCase = true)
                    )
                    is FilterChipType.Folder -> selectedFolder.equals(item.name, ignoreCase = true)
                    is FilterChipType.Tag -> selectedTag.equals(item.name, ignoreCase = true)
                }

                val onClick = {
                    when (item) {
                        is FilterChipType.All -> onClearFilters()
                        is FilterChipType.Locked -> onTagSelected("🔒 Privado")
                        is FilterChipType.Folder -> onFolderSelected(item.name)
                        is FilterChipType.Tag -> onTagSelected(item.name)
                    }
                }

                val labelText = when (item) {
                    is FilterChipType.All -> "Todas"
                    is FilterChipType.Locked -> "Privado"
                    is FilterChipType.Folder -> item.name
                    is FilterChipType.Tag -> item.name.removePrefix("#")
                }

                val leadingIcon: (@Composable () -> Unit)? = when (item) {
                    is FilterChipType.All -> null
                    is FilterChipType.Locked -> {
                        {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is FilterChipType.Folder -> {
                        {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    is FilterChipType.Tag -> {
                        {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                FilterChip(
                    selected = isSelected,
                    onClick = onClick,
                    label = {
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = leadingIcon,
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag(
                        when (item) {
                            is FilterChipType.All -> "tag_chip_Todas"
                            is FilterChipType.Locked -> "tag_chip_Privadas"
                            is FilterChipType.Folder -> "folder_chip_${item.name}"
                            is FilterChipType.Tag -> "tag_chip_${item.name}"
                        }
                    )
                )
            }
        }
    }
}
