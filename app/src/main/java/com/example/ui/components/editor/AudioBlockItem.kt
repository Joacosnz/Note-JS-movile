package com.example.ui.components.editor

import android.net.Uri
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.NoteBlock
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.GeminiAudioTranscriber
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale

@Composable
fun AudioBlockItem(
    block: NoteBlock.Audio,
    onToggleTranscription: () -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(block.durationMs) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    DisposableEffect(block.filePath, block.remoteUrl) {
        val file = File(block.filePath)
        val mediaItem = when {
            file.exists() -> MediaItem.fromUri(Uri.fromFile(file))
            block.remoteUrl.isNotBlank() -> MediaItem.fromUri(block.remoteUrl)
            else -> null
        }

        if (mediaItem != null) {
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    if (player.duration > 0) {
                        totalDuration = player.duration
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    currentPosition = 0L
                    player.seekTo(0)
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.stop()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            delay(100)
        }
    }

    var isTranscribingState by remember(block.id, block.isTranscribing) { mutableStateOf(block.isTranscribing) }
    var transcriptionState by remember(block.id, block.transcription) { mutableStateOf(block.transcription ?: "") }

    LaunchedEffect(block.id, block.filePath) {
        if (transcriptionState.isBlank() || isTranscribingState) {
            isTranscribingState = true
            try {
                val file = File(block.filePath)
                if (file.exists()) {
                    val result = GeminiAudioTranscriber.transcribeAudio(file = file, context = context)
                    if (result.isNotBlank()) {
                        block.transcription = result
                        transcriptionState = result
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                block.isTranscribing = false
                isTranscribingState = false
            }
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
            val isDark = isAppInDarkTheme()
            val micColor = if (isDark) NoteJsAmberDark else Color(0xFF1A365D)

            // UI del Reproductor: Row principal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(micColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Nota de voz",
                        tint = micColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
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

                Spacer(modifier = Modifier.width(8.dp))

                val maxPosition = (if (totalDuration > 0) totalDuration else block.durationMs).coerceAtLeast(1L).toFloat()
                var isSeeking by remember { mutableStateOf(false) }
                var seekValue by remember { mutableFloatStateOf(0f) }

                val sliderValue = if (isSeeking) seekValue else currentPosition.toFloat().coerceIn(0f, maxPosition)

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isSeeking = true
                        seekValue = it
                    },
                    onValueChangeFinished = {
                        player.seekTo(seekValue.toLong())
                        currentPosition = seekValue.toLong()
                        isSeeking = false
                    },
                    valueRange = 0f..maxPosition,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                val displayMs = if (isPlaying || isSeeking) sliderValue.toLong() else totalDuration
                val minutes = displayMs / 60000
                val seconds = (displayMs % 60000) / 1000
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val nextSpeed = when (currentSpeed) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        currentSpeed = nextSpeed
                        player.setPlaybackSpeed(nextSpeed)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = when (currentSpeed) {
                            1.5f -> "1.5x"
                            2.0f -> "2x"
                            else -> "1x"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        player.stop()
                        val file = File(block.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                        onDeleteBlock()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar nota de voz",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Componente de transcripción (estilo cursiva / fondo gris) si la transcripción no está vacía
            val currentTranscription = block.transcription?.takeIf { it.isNotBlank() }
                ?: transcriptionState.takeIf { it.isNotBlank() }

            if (!currentTranscription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SelectionContainer {
                            Text(
                                text = currentTranscription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentTranscription))
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar transcripción", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            } else if (isTranscribingState) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transcribiendo audio...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
