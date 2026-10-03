package com.example.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioBlock
import com.example.util.GeminiAudioTranscriber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

@Composable
fun AudioBlockItem(
    block: AudioBlock,
    onToggleTranscription: () -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    val maxPosition = block.durationMs.toFloat().coerceAtLeast(1f)

    var isTranscribingState by remember(block.id, block.isTranscribing) { mutableStateOf(block.isTranscribing) }
    var transcriptionState by remember(block.id, block.transcription) { mutableStateOf(block.transcription) }

    LaunchedEffect(block.id, block.filePath) {
        if (isTranscribingState && transcriptionState.isBlank()) {
            try {
                val file = File(block.filePath)
                if (file.exists()) {
                    val result = GeminiAudioTranscriber.transcribeAudio(file)
                    block.transcription = result
                    transcriptionState = result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                block.isTranscribing = false
                isTranscribingState = false
            }
        }
    }

    // Manage MediaPlayer lifecycle
    DisposableEffect(block.filePath, block.remoteUrl) {
        val file = File(block.filePath)
        try {
            if (file.exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosition = 0f
                        it.seekTo(0)
                    }
                }
            } else if (block.remoteUrl.isNotBlank()) {
                // Fallback: si el archivo aún no se descargó o la ruta local se corrompió, hacer streaming desde Firebase
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(block.remoteUrl)
                    prepareAsync() // Usar prepareAsync para URLs remotas
                    setOnPreparedListener {
                        // Listo para reproducir
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPosition = 0f
                        it.seekTo(0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Update progress periodically
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPosition = player.currentPosition.toFloat()
                }
            }
            delay(50)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("audio_block_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // A. Fila del Reproductor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Ícono de micrófono azul índigo pulido
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A365D).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Nota de voz",
                        tint = Color(0xFF1A365D),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))

                // Botón Play/Pause
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Barra de progreso interactiva
                Slider(
                    value = currentPosition,
                    onValueChange = {
                        currentPosition = it
                    },
                    onValueChangeFinished = {
                        mediaPlayer?.let { player ->
                            player.seekTo(currentPosition.toInt())
                        }
                    },
                    valueRange = 0f..maxPosition,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Texto con la duración formateada
                val displayPositionMs = if (isPlaying) currentPosition.toLong() else block.durationMs
                val minutes = displayPositionMs / 60000
                val seconds = (displayPositionMs % 60000) / 1000
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Botón Menú / Eliminar
                IconButton(
                    onClick = onDeleteBlock,
                    modifier = Modifier.size(32.dp).padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar nota de voz",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B. Sección "Leer Transcripción"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { 
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleTranscription()
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .testTag("btn_toggle_transcription"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTranscribingState) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transcribiendo audio...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = if (block.isTranscriptionVisible) "Ocultar transcripción ▲" else "Leer transcripción ▼",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = block.isTranscriptionVisible && !isTranscribingState,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = transcriptionState.ifBlank { "No se pudo transcribir el audio." },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    if (transcriptionState.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(transcriptionState))
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
