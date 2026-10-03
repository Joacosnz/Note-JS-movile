package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.ImageBlock
import com.example.model.NoteBlock
import java.io.File

@Composable
fun ImageBlockItem(
    block: ImageBlock,
    onBlockChange: (NoteBlock) -> Unit = {},
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Usamos localUri si está presente; de lo contrario, remoteUrl
            val imageSource = block.localUri.ifEmpty { block.remoteUrl }

            AsyncImage(
                model = imageSource,
                contentDescription = "Imagen adjunta",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
            
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
    val imageSource = if (block.filePath.isNotBlank()) File(block.filePath) else block.remoteUrl
    ImageBlockItem(
        block = ImageBlock(id = block.id, localUri = imageSource.toString(), remoteUrl = block.remoteUrl),
        onRemove = onDeleteBlock,
        modifier = modifier
    )
}
