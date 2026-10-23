package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NoteBlockSerializer
import com.example.util.WebLinkMetadataFetcher
import com.example.util.cleanMarkdownSyntax
import java.util.Locale

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLayoutResult

@Composable
fun NoteContentPreview(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    maxBlocks: Int = 3
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val blocks = remember(content) { NoteBlockSerializer.deserialize(content) }
    var previewingWebUrl by remember { mutableStateOf<String?>(null) }

    if (blocks.isEmpty()) {
        Text(
            text = "Sin contenido",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.6f)
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.take(maxBlocks).forEach { block ->
            when (block) {
                is NoteBlock.Table -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tabla (${block.rows}x${block.cols})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            val previewRow = block.matrix.firstOrNull()?.joinToString(" | ") { it.text.trim() } ?: ""
                            if (previewRow.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewRow,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                is NoteBlock.Text -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, isDark, amber) {
                            buildNoteRichAnnotatedString(block.content, isDark, amber)
                        }
                        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier.onWebLinkClick(layoutResult) { webUrl ->
                                previewingWebUrl = webUrl
                            }
                        )
                    }
                    val firstLink = remember(block.content) { WebLinkMetadataFetcher.extractWebLinks(block.content).firstOrNull() }
                    if (firstLink != null) {
                        CompactRichLinkNoteCardPreview(
                            url = firstLink.url,
                            inlineTitle = firstLink.inlineTitle,
                            isDark = isDark,
                            amber = amber,
                            modifier = Modifier
                                .padding(top = 4.dp)
                        )
                    }
                }

                is NoteBlock.CalloutBox -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, isDark, amber) {
                            buildNoteRichAnnotatedString(block.content, isDark, amber)
                        }
                        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier.onWebLinkClick(layoutResult) { webUrl ->
                                previewingWebUrl = webUrl
                            }
                        )
                    }
                    val firstLink = remember(block.content) { WebLinkMetadataFetcher.extractWebLinks(block.content).firstOrNull() }
                    if (firstLink != null) {
                        CompactRichLinkNoteCardPreview(
                            url = firstLink.url,
                            inlineTitle = firstLink.inlineTitle,
                            isDark = isDark,
                            amber = amber,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                is NoteBlock.SimpleList -> {
                    if (block.content.isNotBlank()) {
                        val annotated = remember(block.content, isDark, amber) {
                            buildNoteRichAnnotatedString(block.content, isDark, amber)
                        }
                        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { layoutResult = it },
                            modifier = Modifier.onWebLinkClick(layoutResult) { webUrl ->
                                previewingWebUrl = webUrl
                            }
                        )
                    }
                    val firstLink = remember(block.content) { WebLinkMetadataFetcher.extractWebLinks(block.content).firstOrNull() }
                    if (firstLink != null) {
                        CompactRichLinkNoteCardPreview(
                            url = firstLink.url,
                            inlineTitle = firstLink.inlineTitle,
                            isDark = isDark,
                            amber = amber,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                is NoteBlock.Checklist -> {
                    val annotated = remember(block.content, isDark, amber) {
                        buildNoteRichAnnotatedString(block.content, isDark, amber)
                    }
                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (block.isChecked) "☑ " else "☐ ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (block.isChecked) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                        )
                        if (annotated.text.isNotBlank()) {
                            Text(
                                text = annotated,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (block.isChecked) TextDecoration.LineThrough else null
                                ),
                                color = if (block.isChecked) textColor.copy(alpha = 0.6f) else textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { layoutResult = it },
                                modifier = Modifier.onWebLinkClick(layoutResult) { webUrl ->
                                    previewingWebUrl = webUrl
                                }
                            )
                        } else {
                            Text(
                                text = "Tarea sin texto",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (block.isChecked) TextDecoration.LineThrough else null
                                ),
                                color = if (block.isChecked) textColor.copy(alpha = 0.6f) else textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    val firstLink = remember(block.content) { WebLinkMetadataFetcher.extractWebLinks(block.content).firstOrNull() }
                    if (firstLink != null) {
                        CompactRichLinkNoteCardPreview(
                            url = firstLink.url,
                            inlineTitle = firstLink.inlineTitle,
                            isDark = isDark,
                            amber = amber,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                is NoteBlock.Pdf -> {
                    MediaChip(
                        icon = Icons.Rounded.PictureAsPdf,
                        iconTint = MaterialTheme.colorScheme.error,
                        fileName = block.fileName.ifBlank { "Documento PDF" },
                        textColor = textColor
                    )
                }

                is AudioBlock -> {
                    val durationText = if (block.durationMs > 0) {
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            block.durationMs / 60000,
                            (block.durationMs % 60000) / 1000
                        )
                    } else ""
                    val audioLabel = if (durationText.isNotBlank()) "Nota de voz ($durationText)" else "Nota de voz"
                    val fileName = block.filePath.substringAfterLast("/").ifBlank { audioLabel }
                    MediaChip(
                        icon = Icons.Rounded.Mic,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        fileName = fileName,
                        textColor = textColor
                    )
                }

                is NoteBlock.ImageBlock -> {
                    val fileName = block.localUri.substringAfterLast("/").ifBlank { "Imagen" }
                    MediaChip(
                        icon = Icons.Rounded.Image,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        fileName = fileName,
                        textColor = textColor
                    )
                }

                is NoteBlock.Image -> {
                    val fileName = block.fileName.ifBlank { block.filePath.substringAfterLast("/") }.ifBlank { "Imagen" }
                    MediaChip(
                        icon = Icons.Rounded.Image,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        fileName = fileName,
                        textColor = textColor
                    )
                }

                is NoteBlock.Drawing -> {
                    MediaChip(
                        icon = Icons.Rounded.Brush,
                        iconTint = MaterialTheme.colorScheme.primary,
                        fileName = "Dibujo / Boceto",
                        textColor = textColor
                    )
                }

                is NoteBlock.Math -> {
                    MediaChip(
                        icon = Icons.Default.Functions,
                        iconTint = MaterialTheme.colorScheme.primary,
                        fileName = block.latexCode.ifBlank { "Fórmula Matemática" },
                        textColor = textColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaChip(
    icon: ImageVector,
    iconTint: Color,
    fileName: String,
    textColor: Color,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
