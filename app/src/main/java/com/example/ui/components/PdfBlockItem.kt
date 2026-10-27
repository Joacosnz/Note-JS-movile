package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.model.NoteBlock
import com.example.model.PdfBlock
import com.example.model.PdfDrawingStroke
import com.example.model.SignatureStamp
import com.example.util.PdfHelper
import com.example.util.PdfModifierUtil
import com.example.util.PdfRenderUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PdfTool {
    NONE, PEN, HIGHLIGHTER, ERASER
}

@Composable
private fun PdfInlinePageView(
    file: File,
    page: Int,
    block: NoteBlock.Pdf,
    onClick: () -> Unit
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(page, file.absolutePath, file.exists(), file.length()) {
        if (file.exists() && file.length() > 0) {
            pageBitmap = PdfHelper.renderPdfPage(file, page)
        }
    }

    val strokes = remember(block.annotationsPerPage, page) {
        block.annotationsPerPage[page] ?: emptyList()
    }
    val signatures = remember(block.signaturesPerPage, page) {
        block.signaturesPerPage[page] ?: emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (pageBitmap != null) {
            val img = pageBitmap!!.asImageBitmap()
            val aspectRatio = (img.width.toFloat() / img.height.toFloat()).coerceAtLeast(0.2f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            ) {
                Image(
                    bitmap = img,
                    contentDescription = "Página ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Capa estática de trazos dibujados (Anotaciones superpuestas sobre el Bitmap de la página)
                if (strokes.isNotEmpty()) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        strokes.forEach { stroke ->
                            if (stroke.points.size > 1) {
                                val path = Path()
                                path.moveTo(
                                    stroke.points.first().first * size.width,
                                    stroke.points.first().second * size.height
                                )
                                for (i in 1 until stroke.points.size) {
                                    path.lineTo(
                                        stroke.points[i].first * size.width,
                                        stroke.points[i].second * size.height
                                    )
                                }
                                val colorHexInt = runCatching { android.graphics.Color.parseColor(stroke.colorHex) }
                                    .getOrDefault(android.graphics.Color.BLACK)
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
                }

                // Capa estática de firmas superpuestas
                if (signatures.isNotEmpty()) {
                    signatures.forEach { stamp ->
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stampWidth = stamp.widthRatio * size.width
                            val stampHeight = stamp.heightRatio * size.height
                            val stampX = stamp.normalizedX * size.width
                            val stampY = stamp.normalizedY * size.height

                            if (stamp.points.isNotEmpty()) {
                                val path = Path()
                                path.moveTo(
                                    stampX + stamp.points.first().first * stampWidth,
                                    stampY + stamp.points.first().second * stampHeight
                                )
                                for (i in 1 until stamp.points.size) {
                                    path.lineTo(
                                        stampX + stamp.points[i].first * stampWidth,
                                        stampY + stamp.points[i].second * stampHeight
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(
                                        width = 4f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfInlineBlock(
    block: NoteBlock.Pdf,
    onPageChange: (newPageIndex: Int) -> Unit = {},
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onToggleExpand: () -> Unit = {},
    onDelete: () -> Unit = {},
    onOpenFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val file = remember(block.filePath) { File(block.filePath) }

    val pageCount = block.pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    // Sincronización continua de la página actual
    LaunchedEffect(pagerState.currentPage) {
        block.currentPageIndex = pagerState.currentPage
        onPageChange(pagerState.currentPage)
    }

    var isFullScreen by remember { mutableStateOf(false) }
    var currentTool by remember { mutableStateOf(PdfTool.NONE) }
    var selectedPenColor by remember { mutableStateOf("#FF000080") }
    var selectedHighlighterColor by remember { mutableStateOf("#66FFFF00") }
    var penStrokeWidth by remember { mutableFloatStateOf(4f) }
    var highlighterStrokeWidth by remember { mutableFloatStateOf(18f) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showPagesGrid by remember { mutableStateOf(false) }
    var isProcessingPdf by remember { mutableStateOf(false) }

    val currentPage = pagerState.currentPage

    var strokes by remember(currentPage, isFullScreen) {
        mutableStateOf(block.annotationsPerPage[currentPage] ?: emptyList())
    }
    var signatures by remember(currentPage, isFullScreen) {
        mutableStateOf(block.signaturesPerPage[currentPage] ?: emptyList())
    }
    var currentPath by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    var fullScreenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFullScreenLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentPage, file.absolutePath, file.exists(), file.length(), isFullScreen) {
        if (isFullScreen) {
            isFullScreenLoading = true
            fullScreenBitmap = withContext(Dispatchers.IO) {
                PdfRenderUtil.renderPageToBitmap(file, currentPage)
            }
            strokes = block.annotationsPerPage[currentPage] ?: emptyList()
            signatures = block.signaturesPerPage[currentPage] ?: emptyList()
            isFullScreenLoading = false
        }
    }

    val saveStrokes = {
        val map = block.annotationsPerPage.toMutableMap()
        map[currentPage] = strokes
        block.annotationsPerPage = map
    }

    val saveSignatures = {
        val map = block.signaturesPerPage.toMutableMap()
        map[currentPage] = signatures
        block.signaturesPerPage = map
    }

    val handleOpenFullScreen = {
        if (onOpenFullScreen != null) {
            onOpenFullScreen()
        } else {
            isFullScreen = true
        }
    }

    // --- BLOQUE INLINE (Tarjeta dentro de la Nota) ---
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("embedded_pdf_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Cabecera limpia del archivo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = block.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pág. ${pagerState.currentPage + 1} /$pageCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Botón para Expandir / Abrir Pantalla Completa
                IconButton(onClick = { handleOpenFullScreen() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Pantalla completa / Dibujar",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Botón de Eliminar
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar PDF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Carrusel táctil con HorizontalPager y carga perezosa por página
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) { page ->
                PdfInlinePageView(
                    file = file,
                    page = page,
                    block = block,
                    onClick = { handleOpenFullScreen() }
                )
            }
        }
    }

    // --- PANTALLA COMPLETA CON HERRAMIENTAS DE DIBUJO ---
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = true
            )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = block.fileName,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Pág. ${pagerState.currentPage + 1} de ${block.pageCount}",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, "Anterior")
                            }
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < block.pageCount - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = pagerState.currentPage < block.pageCount - 1
                            ) {
                                Icon(Icons.Default.ChevronRight, "Siguiente")
                            }
                            IconButton(onClick = { showPagesGrid = true }) {
                                Icon(Icons.Default.GridView, "Ver páginas")
                            }
                            IconButton(
                                onClick = {
                                    isProcessingPdf = true
                                    coroutineScope.launch {
                                        PdfModifierUtil.flattenAndSharePdf(
                                            context = context,
                                            originalFile = file,
                                            strokesMap = block.annotationsPerPage,
                                            signaturesMap = block.signaturesPerPage
                                        )
                                        isProcessingPdf = false
                                    }
                                },
                                enabled = !isProcessingPdf,
                                modifier = Modifier.testTag("btn_pdf_share")
                            ) {
                                Icon(Icons.Default.Share, "Compartir")
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("pdf_markup_palette"),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tool Selector Chips Row
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    FilterChip(
                                        selected = currentTool == PdfTool.NONE,
                                        onClick = {
                                            currentTool = PdfTool.NONE
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        label = { Text("Navegar") },
                                        leadingIcon = {
                                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = currentTool == PdfTool.PEN,
                                        onClick = {
                                            currentTool = PdfTool.PEN
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        label = { Text("Pluma") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = currentTool == PdfTool.HIGHLIGHTER,
                                        onClick = {
                                            currentTool = PdfTool.HIGHLIGHTER
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        label = { Text("Resaltador") },
                                        leadingIcon = {
                                            Icon(NoteJsHighlighterIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = currentTool == PdfTool.ERASER,
                                        onClick = {
                                            currentTool = PdfTool.ERASER
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        label = { Text("Borrador") },
                                        leadingIcon = {
                                            Icon(Icons.Default.AutoFixNormal, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            showSignatureDialog = true
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        label = { Text("Firma") },
                                        leadingIcon = {
                                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("btn_insert_signature")
                                    )
                                }
                            }

                            // Colors Palette & Thickness Slider
                            if (currentTool == PdfTool.PEN || currentTool == PdfTool.HIGHLIGHTER) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isPen = currentTool == PdfTool.PEN
                                    val colors = if (isPen) {
                                        listOf("#FF000080", "#E11D48", "#2563EB", "#16A34A", "#D97706", "#000000")
                                    } else {
                                        listOf("#66FFFF00", "#6600FF00", "#6600FFFF", "#66FF00FF", "#66FF8800")
                                    }
                                    val selectedColor = if (isPen) selectedPenColor else selectedHighlighterColor

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(colors, key = { it }) { colorHex ->
                                            val colorInt = runCatching { android.graphics.Color.parseColor(colorHex) }.getOrDefault(android.graphics.Color.BLACK)
                                            val isColorSelected = selectedColor.equals(colorHex, ignoreCase = true)

                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorInt))
                                                    .border(
                                                        width = if (isColorSelected) 3.dp else 1.dp,
                                                        color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        if (isPen) selectedPenColor = colorHex else selectedHighlighterColor = colorHex
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Grosor",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = if (isPen) penStrokeWidth else highlighterStrokeWidth,
                                            onValueChange = {
                                                if (isPen) penStrokeWidth = it else highlighterStrokeWidth = it
                                            },
                                            valueRange = if (isPen) 2f..24f else 10f..40f,
                                            modifier = Modifier.width(90.dp)
                                        )
                                    }
                                }
                            } else if (currentTool == PdfTool.ERASER) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Arrastra sobre cualquier trazo para borrarlo",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            strokes = emptyList()
                                            saveStrokes()
                                            signatures = emptyList()
                                            saveSignatures()
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Borrar todo", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isFullScreenLoading -> {
                            CircularProgressIndicator(modifier = Modifier.padding(32.dp), color = Color.White)
                        }
                        fullScreenBitmap != null -> {
                            val img = fullScreenBitmap!!.asImageBitmap()
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

                                    Canvas(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(currentTool, selectedPenColor, selectedHighlighterColor, penStrokeWidth, highlighterStrokeWidth) {
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
                                                            val isHighlight = currentTool == PdfTool.HIGHLIGHTER
                                                            val newStroke = PdfDrawingStroke(
                                                                points = currentPath,
                                                                colorHex = if (isHighlight) selectedHighlighterColor else selectedPenColor,
                                                                strokeWidth = if (isHighlight) highlighterStrokeWidth else penStrokeWidth,
                                                                isHighlighter = isHighlight
                                                            )
                                                            strokes = strokes + newStroke
                                                            saveStrokes()
                                                            currentPath = emptyList()
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        val activeIsHighlight = currentTool == PdfTool.HIGHLIGHTER
                                        val drawStrokes = strokes + if (currentPath.isNotEmpty()) {
                                            listOf(
                                                PdfDrawingStroke(
                                                    points = currentPath,
                                                    colorHex = if (activeIsHighlight) selectedHighlighterColor else selectedPenColor,
                                                    strokeWidth = if (activeIsHighlight) highlighterStrokeWidth else penStrokeWidth,
                                                    isHighlighter = activeIsHighlight
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

                                        signatures.forEach { stamp ->
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
                        else -> {
                            Text("Error al cargar la página", Modifier.padding(24.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGOS Y MODALES ---

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
                                    normalizedX = 0.5f - 0.35f / 2,
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

    // ModalBottomSheet: Gestor de cuadrícula de páginas
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
                    items((0 until block.pageCount).toList(), key = { index -> "${block.id}_page_$index" }) { index ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(0.7f)
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(index)
                                    }
                                    showPagesGrid = false
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == pagerState.currentPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                PdfPageThumbnail(file = file, pageIndex = index)

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
    var bitmap by remember(file, pageIndex) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, pageIndex) {
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

@Composable
fun PdfBlockItem(
    block: NoteBlock.Pdf,
    onPageChange: (newPageIndex: Int) -> Unit = {},
    onToggleExpand: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onOpenFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PdfInlineBlock(
        block = block,
        onPageChange = onPageChange,
        onToggleExpand = onToggleExpand,
        onDelete = onDeleteBlock,
        onOpenFullScreen = onOpenFullScreen,
        modifier = modifier
    )
}
