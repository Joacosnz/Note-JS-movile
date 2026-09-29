package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.NoteType
import com.example.model.PaperStyle
import com.example.ui.components.PaperBackground
import com.example.ui.theme.AppleHighlightBlue
import com.example.ui.theme.AppleHighlightMint
import com.example.ui.theme.AppleHighlightOrange
import com.example.ui.theme.AppleHighlightPink
import com.example.ui.theme.AppleHighlightPurple
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.viewmodel.NotesViewModel
import org.json.JSONArray
import org.json.JSONObject

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
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val haptics = LocalHapticFeedback.current

    var title by remember { mutableStateOf("Boceto") }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var currentTool by remember { mutableStateOf(DrawingTool.PEN) }
    var selectedColor by remember { mutableStateOf(if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    val undoneStrokes = remember { mutableStateListOf<DrawingStroke>() }
    var currentPoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    val applePalette = remember {
        listOf(
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

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                if (it.title.isNotBlank()) title = it.title
                paperStyle = runCatching { PaperStyle.valueOf(it.paperStyle) }.getOrDefault(PaperStyle.BLANK)
                if (it.drawingData.isNotBlank() && it.drawingData != "PRESET_SKETCH") {
                    runCatching {
                        val array = JSONArray(it.drawingData)
                        strokes.clear()
                        for (i in 0 until array.length()) {
                            val strokeObj = array.getJSONObject(i)
                            val pointsArr = strokeObj.getJSONArray("points")
                            val pts = mutableListOf<DrawingPoint>()
                            for (j in 0 until pointsArr.length()) {
                                val pObj = pointsArr.getJSONObject(j)
                                pts.add(DrawingPoint(pObj.getDouble("x").toFloat(), pObj.getDouble("y").toFloat()))
                            }
                            strokes.add(
                                DrawingStroke(
                                    points = pts,
                                    colorHex = strokeObj.getLong("color"),
                                    strokeWidth = strokeObj.getDouble("width").toFloat(),
                                    isHighlighter = strokeObj.optBoolean("isHighlighter", false)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun serializeStrokes(): String {
        val array = JSONArray()
        strokes.forEach { s ->
            val obj = JSONObject()
            obj.put("color", s.colorHex)
            obj.put("width", s.strokeWidth)
            obj.put("isHighlighter", s.isHighlighter)
            val pArr = JSONArray()
            s.points.forEach { pt ->
                val pObj = JSONObject()
                pObj.put("x", pt.x)
                pObj.put("y", pt.y)
                pArr.put(pObj)
            }
            obj.put("points", pArr)
            array.put(obj)
        }
        return array.toString()
    }

    fun saveAndExit() {
        val json = serializeStrokes()
        viewModel.saveNote(
            id = noteId,
            title = title,
            content = "Boceto de Apple Notes (${strokes.size} trazos)",
            noteType = NoteType.DRAWING,
            paperStyle = paperStyle.name,
            drawingData = json
        )
        viewModel.navigateBack()
    }

    fun saveAndExitToText() {
        val json = serializeStrokes()
        viewModel.saveNote(
            id = noteId,
            title = title,
            content = if (strokes.isNotEmpty()) "Boceto de Apple Notes (${strokes.size} trazos)" else "",
            noteType = NoteType.TEXT,
            paperStyle = paperStyle.name,
            drawingData = json
        )
        viewModel.navigateBack()
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
            // Apple Markup Floating Tool Palette
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Apple Tools Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DrawingTool.entries.forEach { tool ->
                            val isSelected = currentTool == tool
                            val icon = when (tool) {
                                DrawingTool.PEN -> Icons.Default.Create
                                DrawingTool.HIGHLIGHTER -> Icons.Default.Highlight
                                DrawingTool.PENCIL -> Icons.Default.Edit
                                DrawingTool.ERASER -> Icons.Default.Clear
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentTool = tool
                                    when (tool) {
                                        DrawingTool.HIGHLIGHTER -> strokeWidth = 24f
                                        DrawingTool.PEN -> strokeWidth = 6f
                                        DrawingTool.PENCIL -> strokeWidth = 4f
                                        DrawingTool.ERASER -> strokeWidth = 28f
                                    }
                                },
                                label = { Text(tool.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
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
                                items(applePalette) { color ->
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

                    // Apple Notes Lines & Grids quick toggle
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
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("apple_drawing_canvas")
                    .pointerInput(currentTool, selectedColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(DrawingPoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + DrawingPoint(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                if (currentPoints.isNotEmpty()) {
                                    val isEraser = currentTool == DrawingTool.ERASER
                                    val isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                                    val strokeColor = if (isEraser) canvasBgColor else selectedColor
                                    val colorLong = strokeColor.value.toLong()
                                    strokes.add(
                                        DrawingStroke(
                                            points = currentPoints,
                                            colorHex = colorLong,
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
                strokes.forEach { stroke ->
                    if (stroke.points.size > 1) {
                        val path = Path()
                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        val color = Color(stroke.colorHex.toULong())
                        drawPath(
                            path = path,
                            color = if (stroke.isHighlighter) color.copy(alpha = 0.35f) else color,
                            style = Stroke(
                                width = stroke.strokeWidth * density,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                if (currentPoints.size > 1) {
                    val path = Path()
                    path.moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        path.lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                    val isEraser = currentTool == DrawingTool.ERASER
                    val isHighlighter = currentTool == DrawingTool.HIGHLIGHTER
                    val color = if (isEraser) canvasBgColor else selectedColor

                    drawPath(
                        path = path,
                        color = if (isHighlighter) color.copy(alpha = 0.35f) else color,
                        style = Stroke(
                            width = strokeWidth * density,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
