package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.VoiceNoteViewModel

@Composable
fun VoiceNoteScreen(
    onRecordingFinished: (String, String, Long) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit = {},
    viewModel: VoiceNoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val transcription by viewModel.transcription.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    // Manejador de permisos de grabación de audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Iniciar automáticamente la escucha al abrir la pantalla de nota de voz
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nueva Nota de Voz",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Contenedor del texto transcrito ajustado a menor altura
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = transcription.ifEmpty { "Presiona el micrófono y comienza a hablar..." },
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                style = MaterialTheme.typography.bodyLarge,
                color = if (transcription.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para Grabar / Detener
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    viewModel.stopListening()
                } else {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        viewModel.startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            containerColor = if (isListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isListening) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(
                imageVector = if (isListening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                contentDescription = if (isListening) "Pausar dictado" else "Iniciar dictado",
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Indicador de estado visual
        Text(
            text = if (isListening) "Escuchando..." else "Pausado",
            color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                viewModel.cancelRecording()
                onDismiss()
            }) {
                Text("Cancelar")
            }
            
            Button(
                onClick = {
                    viewModel.stopListening()
                    onRecordingFinished(transcription, "", 0L)
                    onDismiss()
                }
            ) {
                Text("Insertar")
            }
        }
    }
}
