package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteColors
import com.example.model.PaperStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteActionBottomSheet(
    note: NoteEntity,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onDuplicate: () -> Unit,
    onColorChanged: (Int) -> Unit,
    onPaperStyleChanged: (PaperStyle) -> Unit,
    onLockToggle: () -> Unit,
    onShare: () -> Unit,
    onReminder: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            val displayTitle = if (note.isLocked) "Nota Protegida" else note.title.ifBlank { "Opciones de nota" }
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isDark = com.example.ui.theme.isAppInDarkTheme()
            val selectedTheme = NoteColors.options.firstOrNull { it.id == note.colorIndex } ?: NoteColors.options[0]

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedTheme.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(NoteColors.options, key = { it.id }) { option ->
                    KeepColorSwatchItem(
                        option = option,
                        isSelected = note.colorIndex == option.id,
                        isDark = isDark,
                        onClick = { onColorChanged(option.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paper Style selector row
            Text(
                text = "Estilo de lienzo / Papel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaperStyle.entries.forEach { style ->
                    val isSelected = note.paperStyle == style.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPaperStyleChanged(style) },
                        label = { Text(style.title) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Action Items
            ActionRowItem(
                icon = Icons.Default.PushPin,
                text = if (note.isPinned) "Desanclar nota" else "Fijar al inicio",
                onClick = {
                    onTogglePin()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                text = if (note.isArchived) "Desarchivar nota" else "Archivar nota",
                onClick = {
                    onArchive()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = Icons.Default.ContentCopy,
                text = "Duplicar nota",
                onClick = {
                    onDuplicate()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                text = if (note.isLocked) "Quitar de Carpeta Privada" else "Mover a Carpeta Privada",
                onClick = {
                    onLockToggle()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = Icons.Default.Notifications,
                text = if (note.reminderTimestamp != null) "Editar recordatorio" else "Programar recordatorio",
                onClick = {
                    onReminder()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = Icons.Default.Share,
                text = "Compartir texto",
                onClick = {
                    onShare()
                    onDismiss()
                }
            )

            ActionRowItem(
                icon = Icons.Default.Delete,
                text = "Enviar a la papelera",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    onTrash()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
