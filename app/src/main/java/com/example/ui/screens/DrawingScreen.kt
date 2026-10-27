package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import com.example.ui.components.NoteJsHighlighterIcon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteBlock
import com.example.model.NoteType
import com.example.model.PaperStyle
import com.example.ui.components.PaperBackground
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.FileManager
import com.example.util.NoteBlockSerializer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.viewmodel.NotesViewModel
import org.json.JSONArray
import org.json.JSONObject

// 1. Estructura de Datos
data class Stroke(
    val points: List<Offset>,
    val color: Color = Color.Yellow,
    val strokeWidth: Float = 8f,
    val isHighlighter: Boolean = false
)

fun serializeStrokesToJson(strokes: List<Stroke>): String {
    val array = JSONArray()
    strokes.forEach { stroke ->
        val strokeObject = JSONObject().apply {
            put("color", stroke.color.value.toLong())
            put("strokeWidth", stroke.strokeWidth)
            put("isHighlighter", stroke.isHighlighter)

            val pointsArray = JSONArray()
            stroke.points.forEach { pt ->
                val pointObject = JSONObject().apply {
                    put("x", pt.x)
                    put("y", pt.y)
                }
                pointsArray.put(pointObject)
            }
            put("points", pointsArray)
        }
        array.put(strokeObject)
    }
    return array.toString()
}

fun deserializeStrokesFromJson(json: String): List<Stroke> {
    return runCatching {
        if (json.isBlank()) return emptyList()
        val array = JSONArray(json)
        val result = mutableListOf<Stroke>()

        for (i in 0 until array.length()) {
            val strokeObj = array.getJSONObject(i)
            val colorLong = strokeObj.getLong("color")
            val strokeWidth = strokeObj.optDouble("strokeWidth", strokeObj.optDouble("width", 8.0)).toFloat()
            val isHighlighter = strokeObj.optBoolean("isHighlighter", false)
            val pointsArr = strokeObj.getJSONArray("points")

            val pts = mutableListOf<Offset>()
            for (j in 0 until pointsArr.length()) {
                val pObj = pointsArr.getJSONObject(j)
                pts.add(Offset(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
            }

            result.add(
                Stroke(
                    points = pts,
                    color = Color(colorLong.toULong()),
                    strokeWidth = strokeWidth,
                    isHighlighter = isHighlighter
                )
            )
        }
        result.toList()
    }.getOrDefault(emptyList())
}

enum class DrawingTool(val title: String) {
    PEN("Bolígrafo"),
    HIGHLIGHTER("Resaltador"),
    PENCIL("Lápiz"),
    ERASER("Borrador")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current

    var title by remember { mutableStateOf("Boceto") }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color.Yellow) }
    var strokeWidth by remember { mutableFloatStateOf(8f) }

    // 2. Manejo de Estados
    val strokes = remember { mutableStateListOf<Stroke>() }
    val undoneStrokes = remember { mutableStateListOf<Stroke>() }
    var currentPoints by remember { mutableStateOf(emptyList<Offset>()) }

    val notejsPalette = remember {
        listOf(
            Color.Yellow,
            Color(0xFF000000),
            Color(0xFF007AFF),
            Color(0xFFFF2D55),
            Color(0xFF34C759),
            Color(0xFFFF9500),
            Color(0xFFAF52DE),
            Color(0xFF5856D6),
            Color(0xFFFFFFFF)
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                if (it.title.isNotBlank()) title = it.title
                paperStyle = runCatching { PaperStyle.valueOf(it.paperStyle) }.getOrDefault(PaperStyle.BLANK)
                
                val rawSource = if (it.drawingData.isNotBlank() && it.drawingData != "PRESET_SKETCH") {
                    it.drawingData
                } else {
                    val blocks = NoteBlockSerializer.deserialize(it.content)
                    val drawingBlock = blocks.filterIsInstance<NoteBlock.Drawing>().firstOrNull()
                    drawingBlock?.filePath?.ifBlank { drawingBlock.pathData } ?: ""
                }

                if (rawSource.isNotBlank()) {
                    strokes.clear()
                    val jsonContent = withContext(Dispatchers.IO) {
                        FileManager.readDrawingData(rawSource)
                    }
                    if (jsonContent.isNotBlank()) {
                        strokes.addAll(deserializeStrokesFromJson(jsonContent))
                    }
                }
            }
        }
    }

    fun saveAndExit() {
        coroutineScope.launch {
            val json = serializeStrokesToJson(strokes)
            val filePath = if (strokes.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    FileManager.saveDrawingData(context, noteId.toString(), "main", json)
                }
            } else ""

            val entity = viewModel.getNote(noteId)
            val currentBlocks = entity?.let { NoteBlockSerializer.deserialize(it.content) } ?: emptyList()
            val hasDrawingBlock = currentBlocks.any { it is NoteBlock.Drawing }

            val updatedContent = if (hasDrawingBlock) {
                val newBlocks = currentBlocks.map { block ->
                    if (block is NoteBlock.Drawing) {
                        block.copy(filePath = filePath)
                    } else block
                }
                NoteBlockSerializer.serialize(newBlocks)
            } else {
                if (strokes.isNotEmpty()) "Boceto de Note JS (${strokes.size} trazos)" else ""
            }

            viewModel.saveNote(
                id = noteId,
                title = title,
                content = updatedContent,
                noteType = NoteType.DRAWING,
                paperStyle = paperStyle.name,
                drawingData = filePath
            )
            viewModel.navigateBack()
        }
    }

    fun saveAndExitToText() {
        coroutineScope.launch {
            val json = serializeStrokesToJson(strokes)
            val filePath = if (strokes.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    FileManager.saveDrawingData(context, noteId.toString(), "main", json)
                }
            } else ""

            val entity = viewModel.getNote(noteId)
            val currentBlocks = entity?.let { NoteBlockSerializer.deserialize(it.content) } ?: emptyList()
            val hasDrawingBlock = currentBlocks.any { it is NoteBlock.Drawing }

            val updatedContent = if (hasDrawingBlock) {
                val newBlocks = currentBlocks.map { block ->
                    if (block is NoteBlock.Drawing) {
                        block.copy(filePath = filePath)
                    } else block
                }
                NoteBlockSerializer.serialize(newBlocks)
            } else {
                if (strokes.isNotEmpty()) "Boceto de Note JS (${strokes.size} trazos)" else ""
            }

            viewModel.saveNote(
                id = noteId,
                title = title,
                content = updatedContent,
                noteType = NoteType.TEXT,
                paperStyle = paperStyle.name,
                drawingData = filePath
            )
            viewModel.navigateBack()
        }
    }

    BackHandler {
        saveAndExit()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { saveAndExit() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Notas",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = amber
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                val last = strokes.removeAt(strokes.lastIndex)
                                undoneStrokes.add(last)
                            }
                        },
                        enabled = strokes.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            tint = if (strokes.isNotEmpty()) amber else Color.Gray.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (undoneStrokes.isNotEmpty()) {
                                val last = undoneStrokes.removeAt(undoneStrokes.lastIndex)
                                strokes.add(last)
                            }
                        },
                        enabled = undoneStrokes.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            tint = if (undoneStrokes.isNotEmpty()) amber else Color.Gray.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = {
                            strokes.clear()
                            undoneStrokes.clear()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar lienzo", tint = amber)
                    }

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            saveAndExitToText()
                        },
                        modifier = Modifier.testTag("drawing_exit_to_text")
                    ) {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = amber
                        )
                    }

                    TextButton(onClick = { saveAndExit() }) {
                        Text(
                            text = "Listo",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = amber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawingTool.entries.forEach { tool ->
                            val isSelected = currentTool == tool
                            val icon = when (tool) {
                                DrawingTool.PEN -> Icons.Default.Create
                                DrawingTool.HIGHLIGHTER -> NoteJsHighlighterIcon
                                DrawingTool.PENCIL -> Icons.Default.Edit
                                DrawingTool.ERASER -> Icons.Default.Clear
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentTool = tool
                                    when (tool) {
                                        DrawingTool.HIGHLIGHTER -> strokeWidth = 24f
                                        DrawingTool.PEN -> strokeWidth = 8f
                                        DrawingTool.PENCIL -> strokeWidth = 4f
                                        DrawingTool.ERASER -> strokeWidth = 28f
                                    }
                                },
                                label = { Text(tool.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        modifier = Modifier.size(16.dp),
                                        contentDescription = null
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentTool != DrawingTool.ERASER) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(notejsPalette, key = { it.value.toLong() }) { color ->
                                    val isSelected = selectedColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) amber else Color.Gray.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Slider(
                                value = strokeWidth,
                                onValueChange = { strokeWidth = it },
                                valueRange = 2f..40f,
                                modifier = Modifier.width(90.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Papel:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PaperStyle.entries.forEach { style ->
                            FilterChip(
                                selected = paperStyle == style,
                                onClick = { paperStyle = style },
                                label = { Text(style.title, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF))
        ) {
            PaperBackground(
                paperStyle = paperStyle,
                isDark = isDark,
                modifier = Modifier.fillMaxSize()
            )

            val canvasBgColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

            // 3. Gestos y Captura
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("notejs_drawing_canvas")
                    .pointerInput(currentTool, selectedColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    val isEraser = currentTool == DrawingTool.ERASER
                                    val isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                                    val strokeColor = if (isEraser) canvasBgColor else selectedColor
                                    strokes.add(
                                        Stroke(
                                            points = currentPoints,
                                            color = strokeColor,
                                            strokeWidth = strokeWidth,
                                            isHighlighter = isHighlighter
                                        )
                                    )
                                    undoneStrokes.clear()
                                    currentPoints = emptyList()
                                }
                            },
                            onDragCancel = {
                                if (currentPoints.isNotEmpty()) {
                                    val isEraser = currentTool == DrawingTool.ERASER
                                    val isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                                    val strokeColor = if (isEraser) canvasBgColor else selectedColor
                                    strokes.add(
                                        Stroke(
                                            points = currentPoints,
                                            color = strokeColor,
                                            strokeWidth = strokeWidth,
                                            isHighlighter = isHighlighter
                                        )
                                    )
                                    undoneStrokes.clear()
                                    currentPoints = emptyList()
                                }
                            }
                        )
                    }
            ) {
                // 4. Renderizado Vectorial (Función auxiliar)
                fun drawStrokePath(points: List<Offset>, color: Color, strokeWidth: Float, isHighlighter: Boolean) {
                    if (points.isEmpty()) return

                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = if (isHighlighter) color.copy(alpha = 0.35f) else color,
                        style = DrawScopeStroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Dibuja los trazos finalizados
                strokes.forEach { stroke ->
                    drawStrokePath(
                        points = stroke.points,
                        color = stroke.color,
                        strokeWidth = stroke.strokeWidth,
                        isHighlighter = stroke.isHighlighter
                    )
                }

                // Dibuja el trazo temporal en tiempo real
                if (currentPoints.isNotEmpty()) {
                    val isEraser = currentTool == DrawingTool.ERASER
                    val isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                    val strokeColor = if (isEraser) canvasBgColor else selectedColor

                    drawStrokePath(
                        points = currentPoints,
                        color = strokeColor,
                        strokeWidth = strokeWidth,
                        isHighlighter = isHighlighter
                    )
                }
            }
        }
    }
}
