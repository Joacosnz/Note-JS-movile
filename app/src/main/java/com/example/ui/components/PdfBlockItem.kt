package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.PdfBlock
import com.example.model.PdfDrawingStroke
import com.example.model.SignatureStamp
import com.example.util.PdfRenderUtil
import java.io.File
import java.util.UUID

enum class PdfTool {
    NONE, PEN, HIGHLIGHTER, ERASER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfBlockItem(
    block: PdfBlock,
    onPageChange: (newPageIndex: Int) -> Unit,
    onToggleExpand: () -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val file = remember(block.filePath) { File(block.filePath) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var isFullScreen by remember { mutableStateOf(false) }
    var currentTool by remember { mutableStateOf(PdfTool.NONE) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showPagesGrid by remember { mutableStateOf(false) }

    // Estado reactivo para la página actual
    var strokes by remember { mutableStateOf(block.annotationsPerPage[block.currentPageIndex] ?: emptyList()) }
    var signatures by remember { mutableStateOf(block.signaturesPerPage[block.currentPageIndex] ?: emptyList()) }
    var currentPath by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    LaunchedEffect(block.currentPageIndex, block.isExpanded, isFullScreen) {
        if (block.isExpanded || isFullScreen) {
            isLoading = true
            pageBitmap = PdfRenderUtil.renderPageToBitmap(file, block.currentPageIndex)
            strokes = block.annotationsPerPage[block.currentPageIndex] ?: emptyList()
            signatures = block.signaturesPerPage[block.currentPageIndex] ?: emptyList()
            isLoading = false
        }
    }

    val saveStrokes = {
        val map = block.annotationsPerPage.toMutableMap()
        map[block.currentPageIndex] = strokes
        block.annotationsPerPage = map
    }
    
    val saveSignatures = {
        val map = block.signaturesPerPage.toMutableMap()
        map[block.currentPageIndex] = signatures
        block.signaturesPerPage = map
    }

    // --- VISTA INLINE (Tarjeta) ---
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Cabecera
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (block.fileSizeFormatted.isNotEmpty()) {
                        Text(
                            text = "${block.fileSizeFormatted} • ${block.pageCount} pág",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botón de Pantalla Completa
                IconButton(onClick = { isFullScreen = true }) {
                    Icon(Icons.Default.Fullscreen, "Pantalla Completa")
                }

                // Menú desplegable para opciones
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones del PDF")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Ver todas las páginas") },
                            onClick = {
                                showMenu = false
                                showPagesGrid = true
                            },
                            modifier = Modifier.testTag("btn_pdf_pages_grid")
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir PDF anotado") },
                            onClick = {
                                showMenu = false
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
                            }
                        )
                    }
                }
                
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (block.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Alternar"
                    )
                }
                IconButton(onClick = onDeleteBlock) {
                    Icon(Icons.Default.DeleteOutline, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }

            // Cuerpo del PDF Inline (Solo lectura)
            if (block.isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { isFullScreen = true }, // Tap para expandir
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    } else if (pageBitmap != null) {
                        val img = pageBitmap!!.asImageBitmap()
                        Image(
                            bitmap = img,
                            contentDescription = "Página PDF",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                        
                        // Capa estática de trazos
                        Canvas(modifier = Modifier.matchParentSize()) {
                            strokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = Path()
                                    path.moveTo(stroke.points.first().first * size.width, stroke.points.first().second * size.height)
                                    for (i in 1 until stroke.points.size) {
                                        path.lineTo(stroke.points[i].first * size.width, stroke.points[i].second * size.height)
                                    }
                                    val colorHexInt = runCatching { android.graphics.Color.parseColor(stroke.colorHex) }.getOrDefault(android.graphics.Color.BLACK)
                                    drawPath(
                                        path = path,
                                        color = Color(colorHexInt),
                                        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                        blendMode = if (stroke.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver,
                                        alpha = if (stroke.isHighlighter) 0.4f else 1f
                                    )
                                }
                            }
                        }

                        // Capa estática de firmas
                        signatures.forEach { stamp ->
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val stampWidth = stamp.widthRatio * size.width
                                val stampHeight = stamp.heightRatio * size.height
                                val stampX = stamp.normalizedX * size.width
                                val stampY = stamp.normalizedY * size.height
                                
                                if (stamp.points.isNotEmpty()) {
                                    val path = Path()
                                    path.moveTo(stampX + stamp.points.first().first * stampWidth, stampY + stamp.points.first().second * stampHeight)
                                    for (i in 1 until stamp.points.size) {
                                        path.lineTo(stampX + stamp.points[i].first * stampWidth, stampY + stamp.points[i].second * stampHeight)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Error al cargar la página", Modifier.padding(24.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Controles de navegación inline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { 
                            if (block.currentPageIndex > 0) {
                                onPageChange(block.currentPageIndex - 1)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        enabled = block.currentPageIndex > 0
                    ) { Text("← Ant") }

                    Text(
                        text = "Pág. ${block.currentPageIndex + 1} de ${block.pageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    TextButton(
                        onClick = { 
                            if (block.currentPageIndex < block.pageCount - 1) {
                                onPageChange(block.currentPageIndex + 1)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        enabled = block.currentPageIndex < block.pageCount - 1
                    ) { Text("Sig →") }
                }
            }
        }
    }

    // --- VISTA PANTALLA COMPLETA (Interactiva) ---
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false, 
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(block.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text("Pág. ${block.currentPageIndex + 1} de ${block.pageCount}", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { isFullScreen = false }) {
                                Icon(Icons.Default.Close, "Cerrar")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { 
                                    if (block.currentPageIndex > 0) {
                                        onPageChange(block.currentPageIndex - 1)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = block.currentPageIndex > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, "Anterior")
                            }
                            IconButton(
                                onClick = { 
                                    if (block.currentPageIndex < block.pageCount - 1) {
                                        onPageChange(block.currentPageIndex + 1)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = block.currentPageIndex < block.pageCount - 1
                            ) {
                                Icon(Icons.Default.ChevronRight, "Siguiente")
                            }
                            IconButton(onClick = { showPagesGrid = true }) {
                                Icon(Icons.Default.GridView, "Ver páginas")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(
                                onClick = { 
                                    currentTool = if (currentTool == PdfTool.PEN) PdfTool.NONE else PdfTool.PEN 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (currentTool == PdfTool.PEN) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) { Icon(Icons.Default.Edit, "Pluma") }

                            IconButton(
                                onClick = { 
                                    currentTool = if (currentTool == PdfTool.HIGHLIGHTER) PdfTool.NONE else PdfTool.HIGHLIGHTER
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (currentTool == PdfTool.HIGHLIGHTER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) { Icon(Icons.Default.Brush, "Resaltador") }
                            
                            IconButton(
                                onClick = { 
                                    currentTool = if (currentTool == PdfTool.ERASER) PdfTool.NONE else PdfTool.ERASER 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (currentTool == PdfTool.ERASER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) { Icon(Icons.Default.AutoFixNormal, "Borrador") }

                            IconButton(
                                onClick = { 
                                    showSignatureDialog = true 
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.testTag("btn_insert_signature")
                            ) { Icon(Icons.Default.Draw, "Firmar") }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF1E1E1E)), // Fondo oscuro de visor PDF clásico
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.padding(32.dp), color = Color.White)
                        }
                        pageBitmap != null -> {
                            val img = pageBitmap!!.asImageBitmap()
                            val aspectRatio = img.width.toFloat() / img.height.toFloat()
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(aspectRatio)
                                        .background(Color.White)
                                        .testTag("pdf_markup_canvas")
                                ) {
                                    Image(
                                        bitmap = img,
                                        contentDescription = "Página PDF",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                    
                                    // Capa interactiva de Canvas para dibujar
                                    Canvas(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerInput(currentTool) {
                                            if (currentTool == PdfTool.NONE) return@pointerInput
                                            
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    if (currentTool == PdfTool.ERASER) {
                                                        val normX = offset.x / size.width
                                                        val normY = offset.y / size.height
                                                        val updated = strokes.filterNot { stroke ->
                                                            stroke.points.any { p ->
                                                                val dx = p.first - normX
                                                                val dy = p.second - normY
                                                                (dx * dx + dy * dy) < 0.002f
                                                            }
                                                        }
                                                        if (updated.size != strokes.size) {
                                                            strokes = updated
                                                            saveStrokes()
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    } else {
                                                        val p = Pair(offset.x / size.width, offset.y / size.height)
                                                        currentPath = listOf(p)
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    if (currentTool == PdfTool.ERASER) {
                                                        val normX = change.position.x / size.width
                                                        val normY = change.position.y / size.height
                                                        val updated = strokes.filterNot { stroke ->
                                                            stroke.points.any { p ->
                                                                val dx = p.first - normX
                                                                val dy = p.second - normY
                                                                (dx * dx + dy * dy) < 0.002f
                                                            }
                                                        }
                                                        if (updated.size != strokes.size) {
                                                            strokes = updated
                                                            saveStrokes()
                                                        }
                                                    } else {
                                                        val p = Pair(change.position.x / size.width, change.position.y / size.height)
                                                        currentPath = currentPath + p
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (currentTool != PdfTool.ERASER && currentPath.isNotEmpty()) {
                                                        val newStroke = PdfDrawingStroke(
                                                            points = currentPath,
                                                            colorHex = if (currentTool == PdfTool.HIGHLIGHTER) "#66FFFF00" else "#FF000080",
                                                            strokeWidth = if (currentTool == PdfTool.HIGHLIGHTER) 16f else 4f,
                                                            isHighlighter = currentTool == PdfTool.HIGHLIGHTER
                                                        )
                                                        strokes = strokes + newStroke
                                                        saveStrokes()
                                                        currentPath = emptyList()
                                                    }
                                                }
                                            )
                                        }
                                    ) {
                                        val drawStrokes = strokes + if (currentPath.isNotEmpty()) {
                                            listOf(
                                                PdfDrawingStroke(
                                                    points = currentPath,
                                                    colorHex = if (currentTool == PdfTool.HIGHLIGHTER) "#66FFFF00" else "#FF000080",
                                                    strokeWidth = if (currentTool == PdfTool.HIGHLIGHTER) 16f else 4f,
                                                    isHighlighter = currentTool == PdfTool.HIGHLIGHTER
                                                )
                                            )
                                        } else emptyList()

                                        drawStrokes.forEach { stroke ->
                                            if (stroke.points.size > 1) {
                                                val path = Path()
                                                path.moveTo(stroke.points.first().first * size.width, stroke.points.first().second * size.height)
                                                for (i in 1 until stroke.points.size) {
                                                    path.lineTo(stroke.points[i].first * size.width, stroke.points[i].second * size.height)
                                                }
                                                val colorHexInt = runCatching { android.graphics.Color.parseColor(stroke.colorHex) }.getOrDefault(android.graphics.Color.BLACK)
                                                drawPath(
                                                    path = path,
                                                    color = Color(colorHexInt),
                                                    style = Stroke(
                                                        width = stroke.strokeWidth,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    ),
                                                    blendMode = if (stroke.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver,
                                                    alpha = if (stroke.isHighlighter) 0.4f else 1f
                                                )
                                            }
                                        }
                                    }

                                    // Superposición de firmas interactivas (Movimiento y Zoom)
                                    for (stamp in signatures) {
                                        Box(
                                            modifier = Modifier.matchParentSize()
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .pointerInput(stamp.id) {
                                                        detectTransformGestures { _, pan, zoom, _ ->
                                                            val updated = signatures.map { s ->
                                                                if (s.id == stamp.id) {
                                                                    s.copy(
                                                                        normalizedX = s.normalizedX + pan.x / size.width,
                                                                        normalizedY = s.normalizedY + pan.y / size.height,
                                                                        widthRatio = s.widthRatio * zoom,
                                                                        heightRatio = s.heightRatio * zoom
                                                                    )
                                                                } else s
                                                            }
                                                            signatures = updated
                                                            saveSignatures()
                                                        }
                                                    }
                                            ) {
                                                val stampWidth = stamp.widthRatio * size.width
                                                val stampHeight = stamp.heightRatio * size.height
                                                val stampX = stamp.normalizedX * size.width
                                                val stampY = stamp.normalizedY * size.height
                                                
                                                if (stamp.points.isNotEmpty()) {
                                                    val path = Path()
                                                    path.moveTo(stampX + stamp.points.first().first * stampWidth, stampY + stamp.points.first().second * stampHeight)
                                                    for (i in 1 until stamp.points.size) {
                                                        path.lineTo(stampX + stamp.points[i].first * stampWidth, stampY + stamp.points[i].second * stampHeight)
                                                    }
                                                    drawPath(
                                                        path = path,
                                                        color = Color.Black,
                                                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                    )
                                                    // Dibujar recuadro de selección sutil para indicar que se puede mover
                                                    drawRect(
                                                        color = Color.Gray.copy(alpha = 0.3f),
                                                        topLeft = Offset(stampX, stampY),
                                                        size = Size(stampWidth, stampHeight),
                                                        style = Stroke(width = 2f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                         else -> {
                            Text("Error al cargar la página", Modifier.padding(24.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS COMPARTIDOS ---
    
    // Diálogo para crear firma
    if (showSignatureDialog) {
        Dialog(onDismissRequest = { showSignatureDialog = false }) {
            var signPath by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dibuja tu firma", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F4F6))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            signPath = signPath + Pair(offset.x / size.width, offset.y / size.height)
                                        },
                                        onDrag = { change, _ ->
                                            signPath = signPath + Pair(change.position.x / size.width, change.position.y / size.height)
                                        }
                                    )
                                }
                        ) {
                            if (signPath.isNotEmpty()) {
                                val path = Path()
                                path.moveTo(signPath.first().first * size.width, signPath.first().second * size.height)
                                for (i in 1 until signPath.size) {
                                    path.lineTo(signPath[i].first * size.width, signPath[i].second * size.height)
                                }
                                drawPath(path, Color.Black, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { signPath = emptyList() }) { Text("Limpiar") }
                        Button(onClick = {
                            if (signPath.isNotEmpty()) {
                                val newStamp = SignatureStamp(
                                    points = signPath,
                                    normalizedX = 0.5f - 0.35f / 2, // Centrado
                                    normalizedY = 0.5f - 0.15f / 2
                                )
                                signatures = signatures + newStamp
                                saveSignatures()
                            }
                            showSignatureDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) { Text("Insertar") }
                    }
                }
            }
        }
    }

    // ModalBottomSheet: Gestor de miniaturas (Page Manager)
    if (showPagesGrid) {
        ModalBottomSheet(
            onDismissRequest = { showPagesGrid = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Páginas del documento", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items((0 until block.pageCount).toList()) { index ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(0.7f)
                                .clickable {
                                    onPageChange(index)
                                    showPagesGrid = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == block.currentPageIndex) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Miniatura real cargada asíncronamente
                                PdfPageThumbnail(file = file, pageIndex = index)
                                
                                // Indicador oscuro en la parte inferior para mostrar el número de página
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .align(Alignment.BottomCenter)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Pág ${index + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }

                                val hasAnnotations = block.annotationsPerPage[index]?.isNotEmpty() == true
                                val hasSignatures = block.signaturesPerPage[index]?.isNotEmpty() == true
                                if (hasAnnotations || hasSignatures) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Anotado",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPageThumbnail(file: File, pageIndex: Int, modifier: Modifier = Modifier) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, pageIndex) {
        // Genera la miniatura rápidamente reduciendo la resolución (ancho = 300px)
        bitmap = PdfRenderUtil.renderPageToBitmap(file, pageIndex, targetWidthPx = 300)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Miniatura pág ${pageIndex + 1}",
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
