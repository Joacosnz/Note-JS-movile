package com.example.ui.components

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.model.ImageBlock
import com.example.model.NoteBlock
import java.io.File
import java.net.URI

@Composable
fun ImageBlockItem(
    block: ImageBlock,
    onBlockChange: (NoteBlock) -> Unit = {},
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel = remember(block.localUri, block.remoteUrl) {
        val localPath = block.localUri
        val remoteUrl = block.remoteUrl

        val localFile = when {
            localPath.startsWith("/") -> File(localPath)
            localPath.startsWith("file://") -> File(URI(localPath))
            else -> null
        }

        when {
            // 1. Si ya se descargó en el almacenamiento interno privado (Dispositivo B post-sync)
            localFile != null && localFile.exists() -> localFile
            
            // 2. Si es una URI de la galería original (Dispositivo A)
            localPath.startsWith("content://") -> {
                try {
                    // Verificamos si la imagen sigue siendo accesible en este teléfono
                    val stream = context.contentResolver.openInputStream(Uri.parse(localPath))
                    if (stream != null) {
                        stream.close()
                        localPath // Es accesible, la usamos
                    } else {
                        // Stream null -> la imagen no está en este dispositivo, cargar nube
                        remoteUrl.ifBlank { localPath }
                    }
                } catch (e: Exception) {
                    // No es accesible (estamos en el Dispositivo B o se borró de la galería). Fallback a la nube.
                    remoteUrl.ifBlank { localPath }
                }
            }
            
            // 3. Fallback general a la nube si hay URL remota
            remoteUrl.isNotBlank() -> remoteUrl
            
            // 4. Último recurso (intentar cargar lo que haya)
            else -> localPath
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            var isLoading by remember { mutableStateOf(true) }
            var isError by remember { mutableStateOf(false) }

            AsyncImage(
                model = imageModel,
                contentDescription = "Imagen adjunta",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 350.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    isLoading = state is AsyncImagePainter.State.Loading
                    isError = state is AsyncImagePainter.State.Error
                    if (isError) {
                        val errorState = state as AsyncImagePainter.State.Error
                        Log.e("ImageBlockItem", "Error loading image: ${errorState.result.throwable.message}")
                    }
                }
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isError) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Close, // Un ícono de error
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se pudo cargar la imagen\n(Quizás aún se está subiendo)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Botón (X) en la esquina superior para eliminar la imagen
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar imagen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ImageBlockItem(
    block: NoteBlock.Image,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImageBlockItem(
        block = ImageBlock(id = block.id, localUri = block.filePath, remoteUrl = block.remoteUrl),
        onRemove = onDeleteBlock,
        modifier = modifier
    )
}
