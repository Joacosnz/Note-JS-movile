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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.viewmodel.NavTab

@Composable
fun AppleFoldersDrawer(
    activeTab: NavTab,
    notesCount: Int,
    archivedCount: Int,
    trashCount: Int,
    onSelectTab: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

    ModalDrawerSheet(
        modifier = modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Carpetas",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    FolderRowItem(
                        icon = Icons.Default.Folder,
                        title = "Notas",
                        count = notesCount,
                        isSelected = activeTab == NavTab.NOTES,
                        tint = amber,
                        onClick = { onSelectTab(NavTab.NOTES) },
                        testTag = "folder_notes"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    FolderRowItem(
                        icon = Icons.Default.Archive,
                        title = "Archivadas",
                        count = archivedCount,
                        isSelected = activeTab == NavTab.ARCHIVED,
                        tint = amber,
                        onClick = { onSelectTab(NavTab.ARCHIVED) },
                        testTag = "folder_archive"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    FolderRowItem(
                        icon = Icons.Default.Delete,
                        title = "Eliminadas recientemente",
                        count = trashCount,
                        isSelected = activeTab == NavTab.TRASH,
                        tint = amber,
                        onClick = { onSelectTab(NavTab.TRASH) },
                        testTag = "folder_trash"
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRowItem(
    icon: ImageVector,
    title: String,
    count: Int,
    isSelected: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
