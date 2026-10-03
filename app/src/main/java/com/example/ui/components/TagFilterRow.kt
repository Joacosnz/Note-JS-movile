package com.example.ui.components

import com.example.ui.theme.isAppInDarkTheme
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
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark

@Composable
fun TagFilterRow(
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSortClick: (() -> Unit)? = null
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

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

        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = if (onSortClick != null) 0.dp else 16.dp,
                end = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                val isSelected = (selectedTag == null && tag == "Todas") || (selectedTag == tag)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTagSelected(tag) },
                    label = {
                        Text(
                            text = if (tag == "Todas") "Todas" else "#$tag",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("tag_chip_$tag")
                )
            }
        }
    }
}
