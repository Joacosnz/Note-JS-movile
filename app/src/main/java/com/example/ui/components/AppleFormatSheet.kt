package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleHighlightBlue
import com.example.ui.theme.AppleHighlightMint
import com.example.ui.theme.AppleHighlightOrange
import com.example.ui.theme.AppleHighlightPink
import com.example.ui.theme.AppleHighlightPurple
import com.example.ui.theme.AppleNotesAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleFormatSheet(
    sheetState: SheetState,
    onApplyStyle: (String) -> Unit,
    onApplyHighlight: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val highlightColors = listOf(
        AppleHighlightPurple,
        AppleHighlightPink,
        AppleHighlightOrange,
        AppleHighlightMint,
        AppleHighlightBlue
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Formato de texto",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Text styles
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    FormatRow(
                        title = "Título",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        onClick = {
                            onApplyStyle("TITLE")
                            onDismiss()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    FormatRow(
                        title = "Encabezado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        onClick = {
                            onApplyStyle("HEADING")
                            onDismiss()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    FormatRow(
                        title = "Subencabezado",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        onClick = {
                            onApplyStyle("SUBHEADING")
                            onDismiss()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    FormatRow(
                        title = "Cuerpo de texto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        onClick = {
                            onApplyStyle("BODY")
                            onDismiss()
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    FormatRow(
                        title = "Monoespaciado (SF Mono)",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        onClick = {
                            onApplyStyle("MONOSPACE")
                            onDismiss()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Apple 5-color Highlighter row (from Apple Notes specification)
            Text(
                text = "Resaltador cromático Apple Notes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                highlightColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                onApplyHighlight(color)
                                onDismiss()
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatRow(
    title: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    fontFamily: FontFamily? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
