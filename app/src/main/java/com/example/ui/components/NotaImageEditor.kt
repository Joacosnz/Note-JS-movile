package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

@Composable
fun NotaImageEditor(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    // Estados locales inicializados
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    // Estados para previsualización de imagen y archivo PDF consolidado
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Implementación del interceptor para el autoguardado al retroceder
    BackHandler {
        // Autoguardado condicional si hay algún dato ingresado
        if (title.isNotBlank() || content.isNotBlank() || selectedImageUri != null || selectedPdfUri != null) {
            viewModel.saveNote(
                title = title,
                content = content,
                imageUri = selectedImageUri,
                pdfPath = selectedPdfUri?.toString() ?: ""
            )
        }
        // Retornamos a la pantalla anterior incondicionalmente
        onNavigateBack()
    }

    // Selector moderno sin requerir permisos de almacenamiento
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> 
            // Si el usuario cancela, 'uri' será null y el estado se actualizará de forma segura
            if (uri != null) {
                selectedImageUri = uri 
            }
        }
    )

    // Configuración del escáner de documentos
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(
                RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .build()
    }
    val scanner = remember(scannerOptions) {
        GmsDocumentScanning.getClient(scannerOptions)
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pages?.firstOrNull()?.imageUri?.let { uri ->
                selectedImageUri = uri
            }
            scanningResult?.pdf?.uri?.let { pdfUri ->
                selectedPdfUri = pdfUri
            }
        }
    }

    // Estructura de la interfaz en orden vertical
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // 1. Campo de texto para el título
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 2. Campo de texto para el contenido
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Contenido") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 3. Fila con botones para seleccionar imagen o escanear documento
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    // Lanzamos el Picker filtrando exclusivamente por imágenes
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text("Galería")
            }
            
            val context = LocalContext.current
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        scanner.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al iniciar escáner", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Error: No se pudo obtener la Activity.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Escanear")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Renderizado del PDF generado si no es nulo
        selectedPdfUri?.let { pdfUri ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Generado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "PDF Consolidado",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = pdfUri.lastPathSegment ?: "Documento.pdf",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { selectedPdfUri = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar PDF"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Renderizado de Coil si el Uri de la imagen no es nulo
        selectedImageUri?.let { uri ->
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            var isExtracting by remember { mutableStateOf(false) }
            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset += pan * scale
                        }
                    }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Imagen seleccionada para la nota",
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )

                // Botón FAB superpuesto en la esquina inferior derecha
                SmallFloatingActionButton(
                    onClick = {
                        if (isExtracting) return@SmallFloatingActionButton
                        
                        isExtracting = true
                        extractTextFromUri(
                            context = context,
                            uri = uri,
                            onResult = { extractedText ->
                                isExtracting = false
                                if (extractedText.isNotBlank()) {
                                    content = if (content.isEmpty()) extractedText else "$content\n\n$extractedText"
                                    Toast.makeText(context, "Texto extraído con éxito", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se encontró texto en la imagen", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onError = {
                                isExtracting = false
                                Toast.makeText(context, "Error al extraer texto", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Extraer texto de la imagen"
                        )
                    }
                }
            }

            // Botón condicional para abrir el PDF generado por el escáner
            selectedPdfUri?.let { pdfUri ->
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        viewModel.navigateTo(Screen.PdfViewer(noteId = 0L, pdfPath = pdfUri.toString()))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Ver documento PDF",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Ver documento PDF")
                }
            }
        }
    }
}

private fun extractTextFromUri(
    context: Context,
    uri: Uri,
    onResult: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    } catch (e: IOException) {
        e.printStackTrace()
        onError(e)
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
