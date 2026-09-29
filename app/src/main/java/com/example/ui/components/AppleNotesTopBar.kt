package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
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
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark

@Composable
fun AppleNotesTopBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedCount: Int = 0,
    totalCount: Int = 0,
    onExitSelectionMode: () -> Unit = {},
    onSelectAll: () -> Unit = {},
    onDeselectAll: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

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
            // Normal header (minimalist: only More Options on the right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onMoreOptionsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("apple_more_options"),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Más opciones",
                        tint = amber,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Apple Large Title
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // Apple Search Bar
            Spacer(modifier = Modifier.height(10.dp))
            AppleSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholderText = "Buscar en notas...",
            )
        }
    }
}
