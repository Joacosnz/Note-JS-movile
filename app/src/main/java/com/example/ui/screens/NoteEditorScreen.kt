package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChecklistItem
import com.example.model.NoteColors
import com.example.model.NoteType
import com.example.model.PaperStyle
import com.example.ui.components.AppleFormatSheet
import com.example.ui.components.NoteLockDialog
import com.example.ui.components.PaperBackground
import com.example.ui.theme.AppleHighlightBlue
import com.example.ui.theme.AppleHighlightMint
import com.example.ui.theme.AppleHighlightOrange
import com.example.ui.theme.AppleHighlightPink
import com.example.ui.theme.AppleHighlightPurple
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.util.MathEvaluator
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    initialType: NoteType,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf(initialType) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var lockPin by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }
    var newChecklistText by remember { mutableStateOf("") }

    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var updatedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var showMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val formatSheetState = rememberModalBottomSheetState()
    var showFormatSheet by remember { mutableStateOf(false) }

    val isCustomColor = colorIndex != 0
    val editorBgColor = if (isCustomColor) {
        NoteColors.getColor(colorIndex, isDark)
    } else {
        MaterialTheme.colorScheme.background
    }
    val bottomBarBgColor = if (isCustomColor) {
        NoteColors.getColor(colorIndex, isDark)
    } else {
        MaterialTheme.colorScheme.surface
    }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                title = it.title
                content = it.content
                noteType = runCatching { NoteType.valueOf(it.noteType) }.getOrDefault(NoteType.TEXT)
                colorIndex = it.colorIndex
                paperStyle = runCatching { PaperStyle.valueOf(it.paperStyle) }.getOrDefault(PaperStyle.BLANK)
                isPinned = it.isPinned
                isArchived = it.isArchived
                isLocked = it.isLocked
                lockPin = it.lockPin
                tagsText = it.tags
                createdAt = it.createdAt
                updatedAt = it.updatedAt

                if (it.checklistData.isNotBlank()) {
                    runCatching {
                        val arr = JSONArray(it.checklistData)
                        checklistItems.clear()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            checklistItems.add(
                                ChecklistItem(
                                    id = o.getString("id"),
                                    text = o.getString("text"),
                                    isChecked = o.optBoolean("isChecked", false)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun saveAndExit(navigateBack: Boolean = true) {
        val finalTitle = title.trim()
        val finalContent = content.trim()
        val hasChecklist = checklistItems.isNotEmpty()

        if (noteId == 0L && finalTitle.isEmpty() && finalContent.isEmpty() && !hasChecklist) {
            if (navigateBack) viewModel.navigateBack()
            return
        }

        viewModel.saveNote(
            id = noteId,
            title = finalTitle,
            content = finalContent,
            noteType = noteType,
            colorIndex = colorIndex,
            paperStyle = paperStyle.name,
            isPinned = isPinned,
            isArchived = isArchived,
            tags = tagsText.trim(),
            checklistItems = checklistItems.toList(),
            isLocked = isLocked,
            lockPin = lockPin
        )
        if (navigateBack) viewModel.navigateBack()
    }

    BackHandler {
        saveAndExit(navigateBack = true)
    }

    val formattedHeaderDate = remember(updatedAt) {
        val sdf = SimpleDateFormat("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-ES"))
        sdf.format(Date(updatedAt))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { saveAndExit(navigateBack = true) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("apple_editor_back")
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
                    // Share icon
                    IconButton(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir nota"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = amber)
                    }

                    // Markup / Drawing button
                    IconButton(
                        onClick = {
                            saveAndExit(navigateBack = false)
                            viewModel.navigateTo(Screen.Drawing(noteId))
                        }
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = "Dibujar", tint = amber)
                    }

                    // Apple Notes More menu (...)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = amber)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isPinned) "Desanclar nota" else "Fijar nota") },
                                onClick = {
                                    isPinned = !isPinned
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Color de la nota...") },
                                onClick = {
                                    showMenu = false
                                    showColorDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (isLocked) "Eliminar bloqueo con PIN" else "Bloquear nota con PIN") },
                                onClick = {
                                    showMenu = false
                                    if (isLocked) {
                                        isLocked = false
                                        lockPin = ""
                                    } else {
                                        showLockDialog = true
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Líneas y cuadrículas...") },
                                onClick = {
                                    showMenu = false
                                    showPaperDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Información y estadísticas") },
                                onClick = {
                                    showMenu = false
                                    showStatsDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (isArchived) "Desarchivar" else "Archivar nota") },
                                onClick = {
                                    isArchived = !isArchived
                                    showMenu = false
                                }
                            )
                        }
                    }

                    // Apple "Listo" (Done) button in bold amber text
                    TextButton(
                        onClick = { saveAndExit(navigateBack = true) },
                        modifier = Modifier.testTag("apple_editor_done")
                    ) {
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
                    containerColor = editorBgColor
                )
            )
        },
        bottomBar = {
            // Apple Notes Docked Keyboard Toolbar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                color = bottomBarBgColor,
                shadowElevation = 4.dp
            ) {
                Column {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aa Format button
                        IconButton(onClick = { showFormatSheet = true }) {
                            Text(
                                text = "Aa",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = amber
                            )
                        }

                        // Checklist circle button
                        IconButton(
                            onClick = {
                                noteType = NoteType.CHECKLIST
                                if (checklistItems.isEmpty() && content.isNotBlank()) {
                                    content.lines().filter { it.isNotBlank() }.forEach { line ->
                                        checklistItems.add(
                                            ChecklistItem(
                                                id = UUID.randomUUID().toString(),
                                                text = line.trim()
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = "Lista de tareas",
                                tint = if (noteType == NoteType.CHECKLIST) amber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Math Notes button
                        IconButton(
                            onClick = {
                                noteType = NoteType.MATH
                                content = MathEvaluator.processMathInText(content)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Math Notes",
                                tint = if (noteType == NoteType.MATH) amber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Drawing sketch
                        IconButton(
                            onClick = {
                                saveAndExit(navigateBack = false)
                                viewModel.navigateTo(Screen.Drawing(noteId))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = "Boceto",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Apple 5-color Highlighter button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                AppleHighlightPurple,
                                AppleHighlightPink,
                                AppleHighlightOrange,
                                AppleHighlightMint,
                                AppleHighlightBlue
                            ).forEach { hlColor ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(hlColor)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = editorBgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Paper Style (Ruled, Grid, Dots)
            PaperBackground(
                paperStyle = paperStyle,
                isDark = isDark,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Apple Notes Centered Date Header
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formattedHeaderDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Apple Notes Large Title
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    cursorBrush = SolidColor(amber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apple_editor_title"),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "Título",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                )

                // Optional Tags input
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = amber,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(amber),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerField ->
                        if (tagsText.isEmpty()) {
                            Text(
                                text = "Agregar etiquetas (ej. #Ideas, #Trabajo)...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerField()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Body Section
                when (noteType) {
                    NoteType.CHECKLIST -> {
                        // Apple Notes Circular Checklists
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newChecklistText.isNotBlank()) {
                                        checklistItems.add(
                                            ChecklistItem(
                                                id = UUID.randomUUID().toString(),
                                                text = newChecklistText.trim()
                                            )
                                        )
                                        newChecklistText = ""
                                    }
                                }),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(amber),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("apple_checklist_input"),
                                decorationBox = { innerField ->
                                    if (newChecklistText.isEmpty()) {
                                        Text(
                                            text = "Nuevo elemento...",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerField()
                                }
                            )
                            if (newChecklistText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        checklistItems.add(
                                            ChecklistItem(
                                                id = UUID.randomUUID().toString(),
                                                text = newChecklistText.trim()
                                            )
                                        )
                                        newChecklistText = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Añadir", tint = amber)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(checklistItems, key = { _, item -> item.id }) { index, item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    // Apple circular checkbox toggle
                                    IconButton(
                                        onClick = {
                                            checklistItems[index] = item.copy(isChecked = !item.isChecked)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        if (item.isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Completado",
                                                tint = amber,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Circle,
                                                contentDescription = "Incompleto",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    BasicTextField(
                                        value = item.text,
                                        onValueChange = { updated ->
                                            checklistItems[index] = item.copy(text = updated)
                                        },
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 17.sp,
                                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        cursorBrush = SolidColor(amber),
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { checklistItems.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    NoteType.MATH -> {
                        // Apple Math Notes UI
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = amber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Math Notes reactivo (ej. 25 * 4 =)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        content = MathEvaluator.processMathInText(content)
                                    }
                                ) {
                                    Text("Calcular", color = amber, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(amber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("apple_editor_body"),
                            decorationBox = { innerField ->
                                if (content.isEmpty()) {
                                    Text(
                                        text = "Escribe tus cálculos aquí...",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                innerField()
                            }
                        )
                    }

                    else -> {
                        // Standard Apple Rich Text Note
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(amber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("apple_editor_body"),
                            decorationBox = { innerField ->
                                if (content.isEmpty()) {
                                    Text(
                                        text = "Escribe aquí...",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                                innerField()
                            }
                        )
                    }
                }
            }
        }
    }

    // Format Sheet (Aa)
    if (showFormatSheet) {
        AppleFormatSheet(
            sheetState = formatSheetState,
            onApplyStyle = { styleType ->
                when (styleType) {
                    "TITLE" -> title = if (title.isBlank()) "Título" else title
                    "HEADING" -> content += "\n\nEncabezado\n"
                    "SUBHEADING" -> content += "\n\nSubencabezado\n"
                    "BODY" -> content += "\n"
                    "MONOSPACE" -> content += "\n\nCódigo\n"
                }
                showFormatSheet = false
            },
            onApplyHighlight = { _ ->
                showFormatSheet = false
            },
            onDismiss = { showFormatSheet = false }
        )
    }

    // Color Selector Dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Color de la nota") },
            text = {
                Column {
                    Text(
                        text = "Selecciona un color personalizado para esta nota:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NoteColors.options) { option ->
                            val isSelected = colorIndex == option.id
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(option.chipColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) amber else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        colorIndex = option.id
                                        showColorDialog = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cerrar", color = amber)
                }
            }
        )
    }

    // Paper Lines & Grid dialog (Apple Notes: Lines & Grids)
    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = { showPaperDialog = false },
            title = { Text("Líneas y cuadrículas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elige un estilo de fondo para el lienzo de la nota:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PaperStyle.entries.forEach { style ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (paperStyle == style) amber.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    paperStyle = style
                                    showPaperDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = style.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (paperStyle == style) amber else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaperDialog = false }) {
                    Text("Cerrar", color = amber)
                }
            }
        )
    }

    // Set PIN Dialog
    if (showLockDialog) {
        NoteLockDialog(
            isSettingPin = true,
            onConfirm = { pin ->
                isLocked = true
                lockPin = pin
                showLockDialog = false
            },
            onDismiss = { showLockDialog = false }
        )
    }

    // Forensic Stats Dialog
    if (showStatsDialog) {
        val wordCount = if (content.isNotBlank()) content.split(Regex("\\s+")).count { it.isNotBlank() } else 0
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Información de la nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Palabras: $wordCount")
                    Text("Caracteres: ${content.length}")
                    Text("Tipo: ${noteType.label}")
                    Text("Fondo: ${paperStyle.title}")
                    Text("Protegida con PIN: ${if (isLocked) "Sí" else "No"}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Aceptar", color = amber)
                }
            }
        )
    }
}
