package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PdfHelper
import java.io.File

@Composable
fun EmbeddedPdfCard(
    pdfPath: String,
    pdfAnnotationData: String,
    onOpenFullScreen: () -> Unit,
    onUnlinkPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = remember(pdfPath) { File(pdfPath) }
    var pageCount by remember { mutableIntStateOf(0) }
    var thumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumbnail by remember { mutableStateOf(true) }

    val hasAnnotations = remember(pdfAnnotationData) {
        pdfAnnotationData.isNotBlank() && pdfAnnotationData.contains("points")
    }

    LaunchedEffect(pdfPath) {
        if (file.exists()) {
            pageCount = PdfHelper.getPageCount(file)
            isLoadingThumbnail = true
            thumbnailBitmap = PdfHelper.renderPdfPage(file, 0, targetWidth = 360)
            isLoadingThumbnail = false
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .testTag("embedded_pdf_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Cabecera: Ícono rojo de PDF, Nombre de archivo, Cantidad de páginas y Badge de anotaciones
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Documento PDF",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (file.exists()) file.name else "Documento PDF",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (pageCount > 0) "$pageCount páginas" else "Documento adjunto",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (hasAnnotations) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Con anotaciones",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón desvincular / quitar PDF
                IconButton(
                    onClick = onUnlinkPdf,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Desvincular PDF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Miniatura previa de la primera página
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .clickable { onOpenFullScreen() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoadingThumbnail) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                } else if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Vista previa de PDF",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "Vista previa de la primera página",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de acción principal destacado: "Abrir en vista grande"
            Button(
                onClick = onOpenFullScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_pdf_full_screen"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Abrir en vista grande",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
