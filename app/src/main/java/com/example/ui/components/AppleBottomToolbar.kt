package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteType
import com.example.ui.theme.AppleCardBgDark
import com.example.ui.theme.AppleCardBgLight
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark

@Composable
fun AppleBottomToolbar(
    noteCount: Int,
    onNewNote: (NoteType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val bg = if (isDark) AppleCardBgDark.copy(alpha = 0.95f) else AppleCardBgLight.copy(alpha = 0.95f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = bg,
        shadowElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left action buttons: Checklist, Math Note, Drawing
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNewNote(NoteType.CHECKLIST) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("apple_toolbar_checklist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Nueva lista",
                            tint = amber,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNewNote(NoteType.MATH) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("apple_toolbar_math")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Nueva Math Note",
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNewNote(NoteType.DRAWING) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("apple_toolbar_drawing")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = "Nuevo boceto",
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center note count
                Text(
                    text = "$noteCount ${if (noteCount == 1) "nota" else "notas"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Right: Apple Notes Iconic Compose Button (Square with pencil)
                IconButton(
                    onClick = { onNewNote(NoteType.TEXT) },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("apple_compose_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Escribir nota",
                        tint = amber,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
