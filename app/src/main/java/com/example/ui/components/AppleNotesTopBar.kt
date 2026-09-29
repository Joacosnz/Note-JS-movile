package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onFoldersClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    folderName: String = "Carpetas",
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        // Navigation bar header row (< Carpetas  ... [ViewMode])
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Folders back chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onFoldersClick)
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .testTag("apple_folders_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = amber,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("apple_toggle_view"),
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Ver lista" else "Ver galería",
                        tint = amber,
                        modifier = Modifier.size(20.dp),
                    )
                }

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
