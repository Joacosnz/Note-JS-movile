package com.example.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import com.example.util.PdfHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val size: Long,
    val dateModified: Long,
    val path: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfLibraryScreen(
    viewModel: NotesViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isAppInDarkTheme()
    val amberColor = if (isDark) NoteJsAmberDark else NoteJsAmber

    var pdfList by remember { mutableStateOf<List<PdfItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    // Función para escanear archivos PDF con ContentResolver y MediaStore.Files
    fun scanPdfFiles() {
        scope.launch {
            isLoading = true
            val items = withContext(Dispatchers.IO) {
                queryPdfFiles(context)
            }
            pdfList = items
            isLoading = false
        }
    }

    // Comprobar estado inicial del permiso según la versión de Android
    fun checkPermissionAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // En Android 13+ MediaStore.Files se consulta directamente sin requerir READ_EXTERNAL_STORAGE
            hasPermission = true
            scanPdfFiles()
        } else {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            hasPermission = isGranted
            if (isGranted) {
                scanPdfFiles()
            }
        }
    }

    // Launcher para pedir permiso en Android 12 o inferior
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (hasPermission) {
            scanPdfFiles()
        }
    }

    // Launcher del selector nativo de archivos
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.navigateTo(Screen.PdfViewer(noteId = 0L, pdfPath = it.toString()))
        }
    }

    LaunchedEffect(Unit) {
        checkPermissionAndScan()
    }

    BackHandler {
        viewModel.navigateTo(Screen.Home)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Archivos PDF",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) }) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Abrir selector de PDF"
                        )
                    }
                    IconButton(onClick = { checkPermissionAndScan() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = amberColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Buscando archivos PDF en el dispositivo...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                !hasPermission && pdfList.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = amberColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = amberColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Permiso de Almacenamiento Requerido",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Para escanear y mostrar los archivos PDF almacenados en tu dispositivo, concede el acceso de lectura de archivos.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) },
                            colors = ButtonDefaults.buttonColors(containerColor = amberColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Conceder permiso",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Seleccionar PDF directamente")
                        }
                    }
                }

                pdfList.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron archivos PDF automáticamente",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Puedes seleccionar cualquier PDF desde tu almacenamiento interno o descargas.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                            colors = ButtonDefaults.buttonColors(containerColor = amberColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Seleccionar PDF",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 145.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${pdfList.size} documento(s) en la biblioteca",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Abrir otro", fontSize = 12.sp)
                                }
                            }
                        }

                        items(
                            items = pdfList,
                            key = { it.id.toString() + it.path }
                        ) { pdf ->
                            PdfGridItemCard(
                                pdf = pdf,
                                amberColor = amberColor,
                                onClick = {
                                    val targetPath = if (pdf.path.isNotBlank() && File(pdf.path).exists()) {
                                        pdf.path
                                    } else {
                                        pdf.uri.toString()
                                    }
                                    viewModel.navigateTo(Screen.PdfViewer(noteId = 0L, pdfPath = targetPath))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfGridItemCard(
    pdf: PdfItem,
    amberColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    var coverBitmap by remember(pdf.uri, pdf.path) { mutableStateOf<Bitmap?>(null) }
    var isLoadingCover by remember(pdf.uri, pdf.path) { mutableStateOf(true) }

    LaunchedEffect(pdf.uri, pdf.path) {
        val bmp = PdfHelper.renderPdfCover(context, pdf.uri, pdf.path, targetWidth = 450)
        coverBitmap = bmp
        isLoadingCover = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start
    ) {
        // 1. Portada en formato rectangular tipo libro / documento (Aspect ratio vertical A4 ~ 0.72)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.30f)
                ),
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF222226) else Color(0xFFF3F3F7),
            border = BorderStroke(
                0.5.dp,
                if (isDark) Color(0xFF33333A) else Color(0xFFD6D6DC)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap!!.asImageBitmap(),
                        contentDescription = "Portada de ${pdf.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isLoadingCover) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = amberColor
                    )
                } else {
                    // Placeholder visual cuando la página no puede ser renderizada
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        )
                    }
                }

                // Insignia flotante roja "PDF" en la esquina superior izquierda
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 6.dp, topStart = 10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "PDF",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Nombre del PDF DEBAJO de la portada rectangular
        Text(
            text = pdf.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. Metadatos de tamaño y fecha debajo del nombre
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatFileSize(pdf.size),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pdf.dateModified > 0) {
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(pdf.dateModified).substringBefore(" "),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Consulta MediaStore.Files y la carpeta privada pdfs en búsqueda de archivos PDF.
 */
private fun queryPdfFiles(context: Context): List<PdfItem> {
    val list = mutableListOf<PdfItem>()
    val seenPaths = mutableSetOf<String>()

    try {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Documento.pdf"
                val size = cursor.getLong(sizeCol)
                val dateModified = cursor.getLong(dateCol)
                val path = cursor.getString(dataCol) ?: ""

                val contentUri = ContentUris.withAppendedId(collection, id)
                if (path.isBlank() || seenPaths.add(path)) {
                    list.add(
                        PdfItem(
                            id = id,
                            name = name,
                            uri = contentUri,
                            size = size,
                            dateModified = dateModified,
                            path = path
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Incluir archivos PDF guardados localmente en la carpeta de la app (context.filesDir/pdfs)
    try {
        val pdfsDir = File(context.filesDir, "pdfs")
        if (pdfsDir.exists()) {
            pdfsDir.listFiles { _, name -> name.lowercase().endsWith(".pdf") }?.forEach { file ->
                if (seenPaths.add(file.absolutePath)) {
                    list.add(
                        PdfItem(
                            id = file.hashCode().toLong(),
                            name = file.name,
                            uri = Uri.fromFile(file),
                            size = file.length(),
                            dateModified = file.lastModified() / 1000,
                            path = file.absolutePath
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return list
}

private fun formatFileSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = sizeInBytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}

private fun formatDate(timeSeconds: Long): String {
    if (timeSeconds <= 0) return ""
    val date = Date(timeSeconds * 1000)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(date)
}
