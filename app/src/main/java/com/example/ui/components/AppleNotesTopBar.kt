package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.ui.theme.AppleSearchBarDark
import com.example.ui.theme.AppleSearchBarLight

@Composable
fun AppleNotesTopBar(
    title: String,
    folderName: String = "Carpetas",
    query: String,
    onQueryChange: (String) -> Unit,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onFoldersClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val searchBarBg = if (isDark) AppleSearchBarDark else AppleSearchBarLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Navigation bar header row (< Carpetas  ... [ViewMode])
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folders back chevron
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onFoldersClick)
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .testTag("apple_folders_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = amber
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleView,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("apple_toggle_view")
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                        contentDescription = if (isGridView) "Ver lista" else "Ver galería",
                        tint = amber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onMoreOptionsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("apple_more_options")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Más opciones",
                        tint = amber,
                        modifier = Modifier.size(24.dp)
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
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Apple iOS Rounded Search Bar
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = searchBarBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(amber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apple_search_input")
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Borrar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
