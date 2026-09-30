package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.util.PdfHelper
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.hypot

enum class MarkupTool {
    HAND,
    PEN,
    HIGHLIGHTER,
    ERASER
}

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DrawingPoint>,
    val colorHex: String,
    val strokeWidthDp: Float = 5f,
    val isHighlighter: Boolean = false
)

data class DigitalSignature(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DrawingPoint>,
    var normalizedX: Float = 0.25f,
    var normalizedY: Float = 0.72f,
    var scale: Float = 1.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    noteId: Long,
    pdfPath: String,
    viewModel: NotesViewModel
) {
    val haptic = LocalHapticFeedback.current

    val file = remember(pdfPath) { File(pdfPath) }
    var pageCount by remember { mutableIntStateOf(1) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Color palette options
    val colorOptions = remember {
        listOf(
            "#2563EB", // Azul
            "#000000", // Negro
            "#DC2626", // Rojo
            "#16A34A", // Verde
            "#EA580C", // Naranja
            "#9333EA", // Púrpura
            "#CA8A04"  // Amarillo
        )
    }

    var activeTool by remember { mutableStateOf(MarkupTool.HAND) }
    var selectedColorHex by remember { mutableStateOf("#2563EB") }
    var penStrokeWidth by remember { mutableFloatStateOf(5f) }
    var highlighterStrokeWidth by remember { mutableFloatStateOf(24f) }

    // Scale and Offset for Zoom & Pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Reset zoom and pan on page change
    LaunchedEffect(currentPageIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Annotations per page
    var pageStrokesMap by remember { mutableStateOf<Map<Int, List<DrawingStroke>>>(emptyMap()) }
    var pageSignaturesMap by remember { mutableStateOf<Map<Int, List<DigitalSignature>>>(emptyMap()) }

    // Undo / Redo stacks per page
    var undoMap by remember { mutableStateOf<Map<Int, List<List<DrawingStroke>>>>(emptyMap()) }
    var redoMap by remember { mutableStateOf<Map<Int, List<List<DrawingStroke>>>>(emptyMap()) }

    var currentPathPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var showSignatureDialog by remember { mutableStateOf(false) }

    // Load PDF page count and initial annotations from DB
    LaunchedEffect(pdfPath, noteId) {
        if (file.exists()) {
            pageCount = PdfHelper.getPageCount(file).coerceAtLeast(1)
            val note = viewModel.getNote(noteId)
            if (note != null && note.pdfAnnotationData.isNotBlank()) {
                val (loadedStrokes, loadedSigs) = deserializeAnnotations(note.pdfAnnotationData)
                pageStrokesMap = loadedStrokes
                pageSignaturesMap = loadedSigs
            }
        }
    }

    // Load Bitmap for currentPageIndex
    LaunchedEffect(currentPageIndex, pdfPath) {
        if (file.exists()) {
            isLoading = true
            pageBitmap = PdfHelper.renderPdfPage(file, currentPageIndex, targetWidth = 1080)
            isLoading = false
        }
    }

    val currentStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()
    val currentSignatures = pageSignaturesMap[currentPageIndex] ?: emptyList()

    fun updateStrokesForCurrentPage(newStrokes: List<DrawingStroke>) {
        val oldStrokes = pageStrokesMap[currentPageIndex] ?: emptyList()

        // Push old state to undo stack
        val currentUndoList = undoMap[currentPageIndex] ?: emptyList()
        undoMap = undoMap + (currentPageIndex to (currentUndoList + listOf(oldStrokes)))
        // Clear redo stack on new action
        redoMap = redoMap + (currentPageIndex to emptyList())

        pageStrokesMap = pageStrokesMap + (currentPageIndex to newStrokes)
    }

    fun undoAction() {
        val undoList = undoMap[currentPageIndex] ?: emptyList()
        if (undoList.isNotEmpty()) {
            val previousState = undoList.last()
            val newUndoList = undoList.dropLast(1)
            undoMap = undoMap + (currentPageIndex to newUndoList)

            val currentRedoList = redoMap[currentPageIndex] ?: emptyList()
            redoMap = redoMap + (currentPageIndex to (currentRedoList + listOf(currentStrokes)))

            pageStrokesMap = pageStrokesMap + (currentPageIndex to previousState)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun redoAction() {
        val redoList = redoMap[currentPageIndex] ?: emptyList()
        if (redoList.isNotEmpty()) {
            val nextState = redoList.last()
            val newRedoList = redoList.dropLast(1)
            redoMap = redoMap + (currentPageIndex to newRedoList)

            val currentUndoList = undoMap[currentPageIndex] ?: emptyList()
            undoMap = undoMap + (currentPageIndex to (currentUndoList + listOf(currentStrokes)))

            pageStrokesMap = pageStrokesMap + (currentPageIndex to nextState)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun clearCurrentPage() {
        if (currentStrokes.isNotEmpty()) {
            updateStrokesForCurrentPage(emptyList())
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun saveAndExit() {
        val serializedJson = serializeAnnotations(pageStrokesMap, pageSignaturesMap)
        viewModel.savePdfAnnotationData(noteId, serializedJson) {
            viewModel.navigateTo(Screen.Editor(noteId = noteId))
        }
    }

    BackHandler {
        saveAndExit()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Página ${currentPageIndex + 1} de $pageCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                currentPageIndex--
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Página anterior")
                    }

                    Text(
                        text = "${currentPageIndex + 1} / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentPageIndex < pageCount - 1) {
                                currentPageIndex++
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        enabled = currentPageIndex < pageCount - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Página siguiente")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            scale = (scale - 0.5f).coerceAtLeast(1f)
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        enabled = scale > 1f
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Alejar")
                    }

                    Text(
                        text = "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            scale = (scale + 0.5f).coerceAtMost(5f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        enabled = scale < 5f
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Acercar")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = { saveAndExit() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_done_pdf_markup")
                    ) {
                        Text("Listo", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                    if (activeTool == MarkupTool.PEN || activeTool == MarkupTool.HIGHLIGHTER) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorOptions.forEach { hex ->
                                    val colorInt = runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(android.graphics.Color.BLUE)
                                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 28.dp else 22.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedColorHex = hex
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val activeWidth = if (activeTool == MarkupTool.HIGHLIGHTER) highlighterStrokeWidth else penStrokeWidth
                                Slider(
                                    value = activeWidth,
                                    onValueChange = { newValue ->
                                        if (activeTool == MarkupTool.HIGHLIGHTER) {
                                            highlighterStrokeWidth = newValue
                                        } else {
                                            penStrokeWidth = newValue
                                        }
                                    },
                                    valueRange = if (activeTool == MarkupTool.HIGHLIGHTER) 12f..40f else 2f..20f,
                                    modifier = Modifier.width(100.dp)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            IconButton(
                                onClick = {
                                    activeTool = MarkupTool.HAND
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTool == MarkupTool.HAND) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PanTool,
                                    contentDescription = "Modo Mano",
                                    tint = if (activeTool == MarkupTool.HAND) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = {
                                    activeTool = MarkupTool.PEN
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTool == MarkupTool.PEN) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Lápiz",
                                    tint = if (activeTool == MarkupTool.PEN) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = {
                                    activeTool = MarkupTool.HIGHLIGHTER
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTool == MarkupTool.HIGHLIGHTER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Highlight,
                                    contentDescription = "Resaltador",
                                    tint = if (activeTool == MarkupTool.HIGHLIGHTER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = {
                                    showSignatureDialog = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.testTag("btn_add_signature")
                            ) {
                                Icon(
                                    Icons.Default.Draw,
                                    contentDescription = "Firma Digital",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = {
                                    activeTool = MarkupTool.ERASER
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTool == MarkupTool.ERASER) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(
                                    Icons.Default.AutoFixNormal,
                                    contentDescription = "Borrador",
                                    tint = if (activeTool == MarkupTool.ERASER) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = { undoAction() },
                                enabled = (undoMap[currentPageIndex] ?: emptyList()).isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                            }
                        }

                        item {
                            IconButton(
                                onClick = { redoAction() },
                                enabled = (redoMap[currentPageIndex] ?: emptyList()).isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
                            }
                        }

                        item {
                            IconButton(
                                onClick = { clearCurrentPage() },
                                enabled = currentStrokes.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Limpiar Página",
                                    tint = MaterialTheme.colorScheme.error
                                )
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
                .background(Color(0xFF2B2B2B)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                pageBitmap != null -> {
                    val imgBitmap = pageBitmap!!.asImageBitmap()
                    val aspectRatio = imgBitmap.width.toFloat() / imgBitmap.height.toFloat()

                    // Paper styled container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .pointerInput(activeTool) {
                                if (activeTool == MarkupTool.HAND) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale

                                        val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                                        if (newScale > 1f) {
                                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .aspectRatio(aspectRatio)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .shadow(elevation = 12.dp, shape = RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .testTag("pdf_markup_canvas")
                        ) {
                            val density = LocalDensity.current
                            val canvasWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }.coerceAtLeast(1f)
                            val canvasHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }.coerceAtLeast(1f)

                            // PDF Page Image
                            Image(
                                bitmap = imgBitmap,
                                contentDescription = "Página $currentPageIndex",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )

                            // Interactive Markup Canvas
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(activeTool, scale, selectedColorHex, penStrokeWidth, highlighterStrokeWidth, currentStrokes, currentPageIndex, pageCount) {
                                        if (activeTool == MarkupTool.HAND) {
                                            var totalDragX = 0f
                                            detectDragGestures(
                                                onDragStart = {
                                                    totalDragX = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    totalDragX += dragAmount.x
                                                    val maxOffsetX = (size.width * (scale - 1f)) / 2f + size.width * 0.5f
                                                    val maxOffsetY = (size.height * (scale - 1f)) / 2f + size.height * 0.5f

                                                    offsetX = (offsetX + dragAmount.x).coerceIn(-maxOffsetX, maxOffsetX)
                                                    offsetY = (offsetY + dragAmount.y).coerceIn(-maxOffsetY, maxOffsetY)
                                                },
                                                onDragEnd = {
                                                    // Cambio de página al arrastrar horizontalmente en modo mano
                                                    if (totalDragX < -100f && currentPageIndex < pageCount - 1) {
                                                        currentPageIndex++
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    } else if (totalDragX > 100f && currentPageIndex > 0) {
                                                        currentPageIndex--
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            )
                                        } else {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                                    val normY = (offset.y / size.height).coerceIn(0f, 1f)

                                                    if (activeTool == MarkupTool.ERASER) {
                                                        val filtered = currentStrokes.filterNot { stroke ->
                                                            stroke.points.any { p ->
                                                                hypot(p.x - normX, p.y - normY) < (0.03f / scale)
                                                            }
                                                        }
                                                        if (filtered.size != currentStrokes.size) {
                                                            updateStrokesForCurrentPage(filtered)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    } else {
                                                        currentPathPoints = listOf(DrawingPoint(normX, normY))
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                                    val normY = (change.position.y / size.height).coerceIn(0f, 1f)

                                                    if (activeTool == MarkupTool.ERASER) {
                                                        val filtered = currentStrokes.filterNot { stroke ->
                                                            stroke.points.any { p ->
                                                                hypot(p.x - normX, p.y - normY) < (0.03f / scale)
                                                            }
                                                        }
                                                        if (filtered.size != currentStrokes.size) {
                                                            updateStrokesForCurrentPage(filtered)
                                                        }
                                                    } else {
                                                        currentPathPoints = currentPathPoints + DrawingPoint(normX, normY)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (activeTool != MarkupTool.ERASER && currentPathPoints.isNotEmpty()) {
                                                        val isHighlight = activeTool == MarkupTool.HIGHLIGHTER
                                                        val newStroke = DrawingStroke(
                                                            points = currentPathPoints,
                                                            colorHex = if (isHighlight) "#FFFF00" else selectedColorHex,
                                                            strokeWidthDp = if (isHighlight) highlighterStrokeWidth else penStrokeWidth,
                                                            isHighlighter = isHighlight
                                                        )
                                                        updateStrokesForCurrentPage(currentStrokes + newStroke)
                                                        currentPathPoints = emptyList()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            )
                                        }
                                    }
                            ) {
                                val isHighlighting = activeTool == MarkupTool.HIGHLIGHTER
                                val activePathStroke = if (currentPathPoints.isNotEmpty()) {
                                    listOf(
                                        DrawingStroke(
                                            points = currentPathPoints,
                                            colorHex = if (isHighlighting) "#FFFF00" else selectedColorHex,
                                            strokeWidthDp = if (isHighlighting) highlighterStrokeWidth else penStrokeWidth,
                                            isHighlighter = isHighlighting
                                        )
                                    )
                                } else emptyList()

                                val allStrokesToDraw = currentStrokes + activePathStroke

                                allStrokesToDraw.forEach { stroke ->
                                    if (stroke.points.size > 1) {
                                        val path = Path()
                                        path.moveTo(
                                            stroke.points.first().x * size.width,
                                            stroke.points.first().y * size.height
                                        )
                                        for (i in 1 until stroke.points.size) {
                                            path.lineTo(
                                                stroke.points[i].x * size.width,
                                                stroke.points[i].y * size.height
                                            )
                                        }

                                        val parseColorInt = runCatching {
                                            android.graphics.Color.parseColor(stroke.colorHex)
                                        }.getOrDefault(android.graphics.Color.BLUE)

                                        drawPath(
                                            path = path,
                                            color = Color(parseColorInt),
                                            style = Stroke(
                                                width = stroke.strokeWidthDp,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            ),
                                            blendMode = if (stroke.isHighlighter) BlendMode.Multiply else BlendMode.SrcOver,
                                            alpha = if (stroke.isHighlighter) 0.38f else 1.0f
                                        )
                                    }
                                }
                            }

                            // Interactive Signatures Layer
                            currentSignatures.forEach { sig ->
                                SignatureStampItem(
                                    sig = sig,
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                    density = density,
                                    onUpdateSignature = { updatedSig ->
                                        val updated = currentSignatures.map { s ->
                                            if (s.id == updatedSig.id) updatedSig else s
                                        }
                                        pageSignaturesMap = pageSignaturesMap + (currentPageIndex to updated)
                                    },
                                    onDeleteSignature = {
                                        val updated = currentSignatures.filterNot { it.id == sig.id }
                                        pageSignaturesMap = pageSignaturesMap + (currentPageIndex to updated)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text("Error al cargar documento PDF", color = Color.White)
                }
            }
        }
    }

    // Digital Signature Modal Dialog
    if (showSignatureDialog) {
        Dialog(onDismissRequest = { showSignatureDialog = false }) {
            var signaturePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Firma Digital Manuscrita",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showSignatureDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Signature Pad Box with guideline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            signaturePoints = signaturePoints + DrawingPoint(
                                                offset.x / size.width,
                                                offset.y / size.height
                                            )
                                        },
                                        onDrag = { change, _ ->
                                            signaturePoints = signaturePoints + DrawingPoint(
                                                change.position.x / size.width,
                                                change.position.y / size.height
                                            )
                                        }
                                    )
                                }
                        ) {
                            // Horizontal guide line
                            val guideY = size.height * 0.75f
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.4f),
                                start = Offset(20f, guideY),
                                end = Offset(size.width - 20f, guideY),
                                strokeWidth = 2f
                            )

                            if (signaturePoints.isNotEmpty()) {
                                val path = Path()
                                path.moveTo(
                                    signaturePoints.first().x * size.width,
                                    signaturePoints.first().y * size.height
                                )
                                for (i in 1 until signaturePoints.size) {
                                    path.lineTo(
                                        signaturePoints[i].x * size.width,
                                        signaturePoints[i].y * size.height
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }

                        Text(
                            text = "Línea de firma ___________________________",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { signaturePoints = emptyList() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar")
                        }

                        Button(
                            onClick = {
                                if (signaturePoints.isNotEmpty()) {
                                    val newSig = DigitalSignature(
                                        points = signaturePoints,
                                        normalizedX = 0.30f,
                                        normalizedY = 0.70f,
                                        scale = 1.0f
                                    )
                                    pageSignaturesMap = pageSignaturesMap + (currentPageIndex to (currentSignatures + newSig))
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                showSignatureDialog = false
                            },
                            enabled = signaturePoints.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Estampar firma")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignatureStampItem(
    sig: DigitalSignature,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    density: Density,
    onUpdateSignature: (DigitalSignature) -> Unit,
    onDeleteSignature: () -> Unit
) {
    var localNormX by remember(sig.id, sig.normalizedX) { mutableFloatStateOf(sig.normalizedX) }
    var localNormY by remember(sig.id, sig.normalizedY) { mutableFloatStateOf(sig.normalizedY) }
    var localScale by remember(sig.id, sig.scale) { mutableFloatStateOf(sig.scale) }

    val sigWidthPx = (0.40f * canvasWidthPx * localScale).coerceAtLeast(50f)
    val sigHeightPx = (0.16f * canvasHeightPx * localScale).coerceAtLeast(30f)
    val sigLeftPx = localNormX * canvasWidthPx
    val sigTopPx = localNormY * canvasHeightPx

    val sigLeftDp = with(density) { sigLeftPx.toDp() }
    val sigTopDp = with(density) { sigTopPx.toDp() }
    val sigWidthDp = with(density) { sigWidthPx.toDp() }
    val sigHeightDp = with(density) { sigHeightPx.toDp() }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .offset(x = sigLeftDp, y = sigTopDp)
            .size(width = sigWidthDp, height = sigHeightDp)
            .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
            .background(Color(0xFF2563EB).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .pointerInput(sig.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localNormX = (localNormX + dragAmount.x / canvasWidthPx).coerceIn(0f, 0.90f)
                        localNormY = (localNormY + dragAmount.y / canvasHeightPx).coerceIn(0f, 0.90f)
                        onUpdateSignature(sig.copy(normalizedX = localNormX, normalizedY = localNormY, scale = localScale))
                    }
                )
            }
    ) {
        // Draw Signature Vector Path
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            if (sig.points.isNotEmpty()) {
                val path = Path()
                path.moveTo(
                    sig.points.first().x * size.width,
                    sig.points.first().y * size.height
                )
                for (i in 1 until sig.points.size) {
                    path.lineTo(
                        sig.points[i].x * size.width,
                        sig.points[i].y * size.height
                    )
                }

                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Botón de eliminar firma (X roja)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFDC2626))
                .clickable {
                    onDeleteSignature()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Eliminar firma",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Tirador de redimensionado (esquina inferior derecha)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp, y = 8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB))
                .pointerInput(sig.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount.x + dragAmount.y) / 180f
                        localScale = (localScale + delta).coerceIn(0.3f, 4.0f)
                        onUpdateSignature(sig.copy(normalizedX = localNormX, normalizedY = localNormY, scale = localScale))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Redimensionar firma",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

private fun serializeAnnotations(
    pageStrokesMap: Map<Int, List<DrawingStroke>>,
    pageSignaturesMap: Map<Int, List<DigitalSignature>>
): String {
    return try {
        val root = JSONObject()
        val pagesObj = JSONObject()

        val allPageIndices = (pageStrokesMap.keys + pageSignaturesMap.keys).distinct()
        for (pageIdx in allPageIndices) {
            val pageObj = JSONObject()

            val strokesArray = JSONArray()
            pageStrokesMap[pageIdx]?.forEach { stroke ->
                val strokeObj = JSONObject()
                strokeObj.put("id", stroke.id)
                strokeObj.put("colorHex", stroke.colorHex)
                strokeObj.put("strokeWidthDp", stroke.strokeWidthDp.toDouble())
                strokeObj.put("isHighlighter", stroke.isHighlighter)

                val pointsArr = JSONArray()
                stroke.points.forEach { pt ->
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    pointsArr.put(ptObj)
                }
                strokeObj.put("points", pointsArr)
                strokesArray.put(strokeObj)
            }
            pageObj.put("strokes", strokesArray)

            val sigsArray = JSONArray()
            pageSignaturesMap[pageIdx]?.forEach { sig ->
                val sigObj = JSONObject()
                sigObj.put("id", sig.id)
                sigObj.put("normalizedX", sig.normalizedX.toDouble())
                sigObj.put("normalizedY", sig.normalizedY.toDouble())
                sigObj.put("scale", sig.scale.toDouble())

                val pointsArr = JSONArray()
                sig.points.forEach { pt ->
                    val ptObj = JSONObject()
                    ptObj.put("x", pt.x.toDouble())
                    ptObj.put("y", pt.y.toDouble())
                    pointsArr.put(ptObj)
                }
                sigObj.put("points", pointsArr)
                sigsArray.put(sigObj)
            }
            pageObj.put("signatures", sigsArray)

            pagesObj.put(pageIdx.toString(), pageObj)
        }

        root.put("pages", pagesObj)
        root.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

private fun deserializeAnnotations(
    jsonStr: String
): Pair<Map<Int, List<DrawingStroke>>, Map<Int, List<DigitalSignature>>> {
    val strokesMap = mutableMapOf<Int, List<DrawingStroke>>()
    val sigsMap = mutableMapOf<Int, List<DigitalSignature>>()

    if (jsonStr.isBlank()) return Pair(strokesMap, sigsMap)

    try {
        val root = JSONObject(jsonStr)
        val pagesObj = root.optJSONObject("pages") ?: return Pair(strokesMap, sigsMap)

        val keys = pagesObj.keys()
        while (keys.hasNext()) {
            val keyStr = keys.next()
            val pageIdx = keyStr.toIntOrNull() ?: continue
            val pageObj = pagesObj.getJSONObject(keyStr)

            val strokesList = mutableListOf<DrawingStroke>()
            val strokesArr = pageObj.optJSONArray("strokes")
            if (strokesArr != null) {
                for (i in 0 until strokesArr.length()) {
                    val sObj = strokesArr.getJSONObject(i)
                    val id = sObj.optString("id", UUID.randomUUID().toString())
                    val colorHex = sObj.optString("colorHex", "#2563EB")
                    val strokeWidthDp = sObj.optDouble("strokeWidthDp", 5.0).toFloat()
                    val isHighlighter = sObj.optBoolean("isHighlighter", false)

                    val ptsList = mutableListOf<DrawingPoint>()
                    val ptsArr = sObj.optJSONArray("points")
                    if (ptsArr != null) {
                        for (j in 0 until ptsArr.length()) {
                            val pObj = ptsArr.getJSONObject(j)
                            ptsList.add(DrawingPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                        }
                    }
                    strokesList.add(DrawingStroke(id, ptsList, colorHex, strokeWidthDp, isHighlighter))
                }
            }
            strokesMap[pageIdx] = strokesList

            val sigsList = mutableListOf<DigitalSignature>()
            val sigsArr = pageObj.optJSONArray("signatures")
            if (sigsArr != null) {
                for (i in 0 until sigsArr.length()) {
                    val sigObj = sigsArr.getJSONObject(i)
                    val id = sigObj.optString("id", UUID.randomUUID().toString())
                    val normX = sigObj.optDouble("normalizedX", 0.25).toFloat()
                    val normY = sigObj.optDouble("normalizedY", 0.72).toFloat()
                    val scale = sigObj.optDouble("scale", 1.0).toFloat()

                    val ptsList = mutableListOf<DrawingPoint>()
                    val ptsArr = sigObj.optJSONArray("points")
                    if (ptsArr != null) {
                        for (j in 0 until ptsArr.length()) {
                            val pObj = ptsArr.getJSONObject(j)
                            ptsList.add(DrawingPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                        }
                    }
                    sigsList.add(DigitalSignature(id, ptsList, normX, normY, scale))
                }
            }
            sigsMap[pageIdx] = sigsList
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return Pair(strokesMap, sigsMap)
}
