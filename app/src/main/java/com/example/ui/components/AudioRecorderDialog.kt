package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.model.AudioBlock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import java.util.UUID

@Composable
fun AudioRecorderDialog(
    onDismiss: () -> Unit,
    onAudioRecorded: (AudioBlock) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (hasPermission) {
        AudioRecorderContent(
            context = context,
            onDismiss = onDismiss,
            onAudioRecorded = onAudioRecorded
        )
    }
}

@Composable
private fun AudioRecorderContent(
    context: Context,
    onDismiss: () -> Unit,
    onAudioRecorded: (AudioBlock) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) audioDir.mkdirs()

        onDispose {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isActive) {
                delay(1000)
                recordingDuration += 1000L
            }
        }
    }

    Dialog(onDismissRequest = {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                audioFile?.delete()
            } catch (e: Exception) {
            }
        }
        onDismiss()
    }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "Grabando nota de voz..." else "Presiona para grabar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))

                val infiniteTransition = rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = pulseAlpha))
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isRecording) {
                                // Stop recording
                                isRecording = false
                                try {
                                    mediaRecorder?.stop()
                                    mediaRecorder?.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                val finalFile = audioFile
                                if (finalFile != null) {
                                    val block = AudioBlock(
                                        filePath = finalFile.absolutePath,
                                        durationMs = recordingDuration,
                                        transcription = "",
                                        isTranscribing = true,
                                        isTranscriptionVisible = false
                                    )
                                    onAudioRecorded(block)
                                } else {
                                    onDismiss()
                                }
                            } else {
                                // Start recording
                                audioFile = File(context.filesDir, "audio/${UUID.randomUUID()}.m4a")
                                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setOutputFile(audioFile?.absolutePath)
                                    try {
                                        prepare()
                                        start()
                                        isRecording = true
                                        recordingDuration = 0L
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Detener grabación" else "Iniciar grabación",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isRecording) {
                    val minutes = recordingDuration / 60000
                    val seconds = (recordingDuration % 60000) / 1000
                    Text(
                        text = String.format(Locale.getDefault(), "🔴 %02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        if (isRecording) {
                            try {
                                mediaRecorder?.stop()
                                mediaRecorder?.release()
                                audioFile?.delete()
                            } catch (_: Exception) {
                            }
                        }
                        onDismiss()
                    }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}