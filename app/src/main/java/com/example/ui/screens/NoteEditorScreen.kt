package com.example.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.TableChart
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.NoteType
import com.example.model.PaperStyle
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.model.TableCell
import com.example.model.TextStyleType
import com.example.ui.components.NoteLockDialog
import com.example.ui.components.PaperBackground
import com.example.ui.components.AudioRecorderDialog
import com.example.ui.components.AudioBlockItem
import com.example.ui.components.PdfBlockItem
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.util.NoteBlockSerializer
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val haptics = LocalHapticFeedback.current

    var title by remember { mutableStateOf("") }
    val blocks = remember { mutableStateListOf<NoteBlock>() }
    var noteType by remember { mutableStateOf(initialType) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }
    var isPinned by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var lockPin by remember { mutableStateOf("") }
    var reminderTimestamp by remember { mutableStateOf<Long?>(null) }

    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var updatedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    var targetFocusBlockId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.insertPdfBlock(it) { newBlock ->
                if (!blocks.any { b -> b.id == newBlock.id }) {
                    blocks.add(newBlock)
                }
            }
        }
    }


    LaunchedEffect(targetFocusBlockId) {
        targetFocusBlockId?.let { id ->
            try {
                focusRequesters[id]?.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors for blocks that don't have focusRequester attached
            }
            targetFocusBlockId = null
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var createTableRows by remember { mutableIntStateOf(3) }
    var createTableCols by remember { mutableIntStateOf(3) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showReminderDialog = true
        }
    }

    val isCustomColor = colorIndex != 0
    val editorBgColor = if (isCustomColor) {
        NoteColors.getColor(colorIndex, isDark)
    } else {
        MaterialTheme.colorScheme.background
    }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val entity = viewModel.getNote(noteId)
            entity?.let {
                title = it.title
                noteType = runCatching { NoteType.valueOf(it.noteType) }.getOrDefault(NoteType.TEXT)
                colorIndex = it.colorIndex
                paperStyle = runCatching { PaperStyle.valueOf(it.paperStyle) }.getOrDefault(PaperStyle.BLANK)
                isPinned = it.isPinned
                isArchived = it.isArchived
                isLocked = it.isLocked
                lockPin = it.lockPin
                reminderTimestamp = it.reminderTimestamp
                createdAt = it.createdAt
                updatedAt = it.updatedAt

                val deserialized = NoteBlockSerializer.deserialize(it.content)
                blocks.clear()
                blocks.addAll(deserialized)
            }
        } else {
            if (blocks.isEmpty()) {
                blocks.add(NoteBlock.Text())
            }
        }
    }

    fun openDateTimePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        val chosenTimestamp = calendar.timeInMillis
                        reminderTimestamp = chosenTimestamp
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        if (noteId > 0) {
                            viewModel.setReminder(context, noteId, chosenTimestamp, title, NoteBlockSerializer.serialize(blocks))
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun saveAndExit(navigateBack: Boolean = true) {
        val finalTitle = title.trim()
        val serializedContent = NoteBlockSerializer.serialize(blocks)
        val isEmptyNote = finalTitle.isEmpty() && blocks.all { it is NoteBlock.Text && it.content.isEmpty() }

        if (noteId == 0L && isEmptyNote) {
            if (navigateBack) viewModel.navigateBack()
            return
        }

        viewModel.saveNote(
            id = noteId,
            title = finalTitle,
            content = serializedContent,
            noteType = noteType,
            colorIndex = colorIndex,
            paperStyle = paperStyle.name,
            isPinned = isPinned,
            isArchived = isArchived,
            checklistItems = emptyList(),
            isLocked = isLocked,
            lockPin = lockPin,
            reminderTimestamp = reminderTimestamp,
            onSaved = { savedId ->
                if (reminderTimestamp != null) {
                    viewModel.setReminder(context, savedId, reminderTimestamp!!, finalTitle, serializedContent)
                }
            }
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
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val permission = Manifest.permission.POST_NOTIFICATIONS
                                if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                                    showReminderDialog = true
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            } else {
                                showReminderDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_set_reminder")
                    ) {
                        Icon(
                            imageVector = if (reminderTimestamp != null) Icons.Default.Notifications else Icons.Outlined.NotificationsActive,
                            contentDescription = "Programar recordatorio",
                            tint = amber
                        )
                    }

                    IconButton(
                        onClick = {
                            val plainText = blocks.joinToString("\n") {
                                when (it) {
                                    is NoteBlock.Text -> it.content
                                    is NoteBlock.Checklist -> "${if (it.isChecked) "☑" else "☐"} ${it.content}"
                                    is NoteBlock.SimpleList -> "${it.content}"
                                    is NoteBlock.CalloutBox -> it.content
                                    is NoteBlock.Drawing -> "[Boceto]"
                                    is NoteBlock.Table -> it.matrix.joinToString(" | ") { row -> row.joinToString(" | ") { cell -> cell.text } }
                                    is PdfBlock -> "[PDF: ${it.fileName}]"
                                    is AudioBlock -> "[Audio: ${String.format(Locale.getDefault(), "%02d:%02d", it.durationMs / 60000, (it.durationMs % 60000) / 1000)}]"
                                }
                            }
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "$title\n\n$plainText")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir nota"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = amber)
                    }

                    IconButton(
                        onClick = {
                            saveAndExit(navigateBack = false)
                            viewModel.navigateTo(Screen.Drawing(noteId))
                        }
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = "Dibujar", tint = amber)
                    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.08f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.85f)
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.Transparent,
                                        Color(0xFF3F3F46).copy(alpha = 0.60f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color.White.copy(alpha = 0.30f),
                                        Color(0xFFCBD5E1).copy(alpha = 0.45f)
                                    )
                                )
                            },
                            shape = CircleShape
                        )
                        .testTag("liquid_glass_accessory_bar"),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Tipografía / Formato (Aa) -> Inserta o convierte en Cuadro / Texto / Lista
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFormatSheet = true
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "Aa",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = amber
                                )
                            }
                        }

                        // 2. Lista de Tareas / Checklist (✓=)
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                blocks.add(NoteBlock.Checklist())
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "Añadir checklist",
                                    tint = amber
                                )
                            }
                        }

                        // 3. Boceto a Mano / Dibujo (〰️)
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                blocks.add(NoteBlock.Drawing())
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gesture,
                                    contentDescription = "Añadir dibujo",
                                    tint = amber
                                )
                            }
                        }

                        // 4. Tabla interactiva
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                createTableRows = 3
                                createTableCols = 3
                                showCreateTableDialog = true
                            },
                            modifier = Modifier.testTag("btn_add_table")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = "Añadir tabla",
                                    tint = amber
                                )
                            }
                        }

                        // 5. Adjuntar PDF
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier.testTag("btn_add_pdf")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Adjuntar PDF",
                                    tint = amber
                                )
                            }
                        }

                        // 6. Grabar Nota de Voz
                        var showAudioRecorder by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showAudioRecorder = true
                            },
                            modifier = Modifier.testTag("btn_add_audio")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Grabar audio",
                                    tint = amber
                                )
                            }
                        }
                        
                        if (showAudioRecorder) {
                            AudioRecorderDialog(
                                onDismiss = { showAudioRecorder = false },
                                onAudioRecorded = { audioBlock ->
                                    blocks.add(audioBlock)
                                    showAudioRecorder = false
                                }
                            )
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
            PaperBackground(
                paperStyle = paperStyle,
                isDark = isDark,
                modifier = Modifier.fillMaxSize()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header date & reminder
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedHeaderDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    if (reminderTimestamp != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_chip")
                                .clickable { showReminderDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A365D).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF1A365D).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF1A365D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val formattedReminder = remember(reminderTimestamp) {
                                        val sdf = SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
                                        sdf.format(Date(reminderTimestamp!!))
                                    }
                                    Text(
                                        text = "⏰ Recordatorio: $formattedReminder",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFF1A365D)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        reminderTimestamp = null
                                        if (noteId > 0) {
                                            viewModel.cancelReminder(context, noteId)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                ) {
                                    Text(text = "Eliminar", color = Color(0xFF1A365D), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title input
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
                        visualTransformation = remember(isDark) { hashtagVisualTransformation(isDark) },
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

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Mixed-Content Blocks Items
                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    val focusRequester = remember(block.id) { FocusRequester() }
                    focusRequesters[block.id] = focusRequester

                    when (block) {
                        is NoteBlock.Text -> {
                            val textStyle = when (block.style) {
                                TextStyleType.TITLE -> MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                TextStyleType.SUBTITLE -> MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                TextStyleType.BODY -> MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp)
                            }
                            BasicTextField(
                                value = block.content,
                                onValueChange = { newText ->
                                    blocks[index] = block.copy(content = newText)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .focusRequester(focusRequester)
                                    .testTag("block_text_$index"),
                                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(amber),
                                visualTransformation = remember(isDark) { hashtagVisualTransformation(isDark) },
                                decorationBox = { innerTextField ->
                                    if (block.content.isEmpty()) {
                                        Text(
                                            text = when (block.style) {
                                                TextStyleType.TITLE -> "Título de sección..."
                                                TextStyleType.SUBTITLE -> "Subtítulo..."
                                                TextStyleType.BODY -> "Escribe algo..."
                                            },
                                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        )
                                    }
                                    innerTextField()
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val newId = UUID.randomUUID().toString()
                                        blocks.add(index + 1, NoteBlock.Text(id = newId))
                                        targetFocusBlockId = newId
                                    }
                                )
                            )
                        }
                        is NoteBlock.Checklist -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("block_checklist_$index"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (block.isChecked) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                                        .background(if (block.isChecked) amber else Color.Transparent)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            blocks[index] = block.copy(isChecked = !block.isChecked)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (block.isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completado",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                BasicTextField(
                                    value = block.content,
                                    onValueChange = { newText ->
                                        blocks[index] = block.copy(content = newText)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        color = if (block.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (block.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    cursorBrush = SolidColor(amber),
                                    visualTransformation = remember(isDark) { hashtagVisualTransformation(isDark) },
                                    decorationBox = { innerTextField ->
                                        if (block.content.isEmpty()) {
                                            Text(
                                                text = "Elemento de lista de tareas...",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            )
                                        }
                                        innerTextField()
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (block.content.isBlank()) {
                                                blocks[index] = NoteBlock.Text(id = block.id, content = "")
                                            } else {
                                                val newId = UUID.randomUUID().toString()
                                                blocks.add(index + 1, NoteBlock.Checklist(id = newId))
                                                targetFocusBlockId = newId
                                            }
                                        }
                                    )
                                )
                                IconButton(
                                    onClick = { blocks.removeAt(index) },
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
                        is NoteBlock.SimpleList -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("block_simple_list_$index"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (block.isNumbered) "${block.number}." else "•",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = amber, fontSize = 17.sp),
                                    modifier = Modifier.width(24.dp),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = block.content,
                                    onValueChange = { newText ->
                                        blocks[index] = block.copy(content = newText)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface),
                                    cursorBrush = SolidColor(amber),
                                    visualTransformation = remember(isDark) { hashtagVisualTransformation(isDark) },
                                    decorationBox = { innerTextField ->
                                        if (block.content.isEmpty()) {
                                            Text(
                                                text = "Elemento de lista...",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                            )
                                        }
                                        innerTextField()
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (block.content.isBlank()) {
                                                blocks[index] = NoteBlock.Text(id = block.id, content = "")
                                            } else {
                                                val nextNumber = if (block.isNumbered) block.number + 1 else 1
                                                val newId = UUID.randomUUID().toString()
                                                blocks.add(index + 1, NoteBlock.SimpleList(id = newId, isNumbered = block.isNumbered, number = nextNumber))
                                                targetFocusBlockId = newId
                                            }
                                        }
                                    )
                                )
                            }
                        }
                        is NoteBlock.CalloutBox -> {
                            var showCalloutMenu by remember { mutableStateOf(false) }
                            var showCalloutColorPicker by remember { mutableStateOf(false) }

                            val calloutBg = if (!block.backgroundColorHex.isNullOrBlank()) {
                                runCatching { Color(android.graphics.Color.parseColor(block.backgroundColorHex)) }.getOrDefault(
                                    if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                                )
                            } else {
                                if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("block_callout_$index")
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showCalloutMenu = true
                                        }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = calloutBg,
                                border = BorderStroke(1.dp, amber.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(36.dp)
                                            .background(amber, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    BasicTextField(
                                        value = block.content,
                                        onValueChange = { newText ->
                                            blocks[index] = block.copy(content = newText)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontStyle = FontStyle.Italic
                                        ),
                                        cursorBrush = SolidColor(amber),
                                        visualTransformation = remember(isDark) { hashtagVisualTransformation(isDark) },
                                        decorationBox = { innerTextField ->
                                            if (block.content.isEmpty()) {
                                                Text(
                                                    text = "Cuadro destacado / Nota importante...",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 17.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        fontStyle = FontStyle.Italic
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val newId = UUID.randomUUID().toString()
                                                blocks.add(index + 1, NoteBlock.Text(id = newId))
                                                targetFocusBlockId = newId
                                            }
                                        )
                                    )
                                }
                            }

                            if (showCalloutMenu) {
                                AlertDialog(
                                    onDismissRequest = { showCalloutMenu = false },
                                    title = { Text("Opciones de cuadro destacado") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    showCalloutMenu = false
                                                    showCalloutColorPicker = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🎨 Cambiar color de resalte", color = amber)
                                            }
                                            TextButton(
                                                onClick = {
                                                    showCalloutMenu = false
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    blocks.removeAt(index)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("btn_delete_callout")
                                            ) {
                                                Text("🗑️ Eliminar cuadro destacado", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showCalloutMenu = false }) {
                                            Text("Cerrar")
                                        }
                                    }
                                )
                            }

                            if (showCalloutColorPicker) {
                                AlertDialog(
                                    onDismissRequest = { showCalloutColorPicker = false },
                                    title = { Text("Seleccionar color de resalte") },
                                    text = {
                                        val calloutPalette = listOf(
                                            "Gris neutro" to null,
                                            "Amarillo pastel" to "#FEF3C7",
                                            "Azul claro" to "#DBEAFE",
                                            "Verde menta" to "#D1FAE5",
                                            "Rosa pastel" to "#FCE7F3"
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            calloutPalette.forEach { (name, hex) ->
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            block.backgroundColorHex = hex
                                                            blocks[index] = block.copy(backgroundColorHex = hex)
                                                            showCalloutColorPicker = false
                                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = name,
                                                        modifier = Modifier.padding(12.dp),
                                                        color = if (hex != null) Color.Black else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showCalloutColorPicker = false }) {
                                            Text("Cancelar")
                                        }
                                    }
                                )
                            }
                        }
                        is NoteBlock.Drawing -> {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        saveAndExit(navigateBack = false)
                                        viewModel.navigateTo(Screen.Drawing(noteId))
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA),
                                border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Gesture, contentDescription = null, tint = amber)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Boceto a mano alzada (Toca para editar)",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        is NoteBlock.Table -> {
                            var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                            var showTableMenu by remember { mutableStateOf(false) }
                            var isEditingTable by remember { mutableStateOf(false) }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .focusRequester(focusRequester)
                                    .testTag("table_block")
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showTableMenu = true
                                        }
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    if (isEditingTable) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val insertIndex = selectedCell?.first?.let { it + 1 } ?: block.matrix.size
                                                        val newRow = mutableListOf<TableCell>()
                                                        for (c in 0 until block.cols) {
                                                            newRow.add(TableCell())
                                                        }
                                                        block.matrix.add(insertIndex.coerceIn(0, block.matrix.size), newRow)
                                                        block.rows = block.matrix.size
                                                        blocks[index] = block.copy(rows = block.rows, matrix = block.matrix)
                                                    }
                                                ) {
                                                    Text("+ Fila", fontSize = 12.sp, color = amber)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val removeIndex = selectedCell?.first ?: (block.matrix.size - 1)
                                                        if (block.matrix.size > 1 && removeIndex in block.matrix.indices) {
                                                            block.matrix.removeAt(removeIndex)
                                                            block.rows = block.matrix.size
                                                            selectedCell = null
                                                            blocks[index] = block.copy(rows = block.rows, matrix = block.matrix)
                                                        }
                                                    }
                                                ) {
                                                    Text("- Fila", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val insertCol = selectedCell?.second?.let { it + 1 } ?: block.cols
                                                        block.matrix.forEach { row ->
                                                            row.add(insertCol.coerceIn(0, row.size), TableCell())
                                                        }
                                                        block.cols = block.matrix.firstOrNull()?.size ?: block.cols
                                                        blocks[index] = block.copy(cols = block.cols, matrix = block.matrix)
                                                    }
                                                ) {
                                                    Text("+ Col", fontSize = 12.sp, color = amber)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        val removeCol = selectedCell?.second ?: (block.cols - 1)
                                                        if (block.cols > 1) {
                                                            block.matrix.forEach { row ->
                                                                if (removeCol in row.indices) {
                                                                    row.removeAt(removeCol)
                                                                }
                                                            }
                                                            block.cols = block.matrix.firstOrNull()?.size ?: block.cols
                                                            selectedCell = null
                                                            blocks[index] = block.copy(cols = block.cols, matrix = block.matrix)
                                                        }
                                                    }
                                                ) {
                                                    Text("- Col", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            TextButton(onClick = { isEditingTable = false }) {
                                                Text("Listo", fontSize = 12.sp, color = amber, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    for (r in 0 until block.rows) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            for (c in 0 until block.cols) {
                                                val cell = block.matrix.getOrNull(r)?.getOrNull(c) ?: TableCell()
                                                val cellBg = if (!cell.backgroundColorHex.isNullOrBlank()) {
                                                    runCatching { Color(android.graphics.Color.parseColor(cell.backgroundColorHex)) }.getOrDefault(Color.Transparent)
                                                } else {
                                                    Color.Transparent
                                                }
                                                val isSelected = selectedCell == Pair(r, c)

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(2.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(cellBg)
                                                        .border(
                                                            width = if (isSelected) 2.dp else 0.5.dp,
                                                            color = if (isSelected) amber else MaterialTheme.colorScheme.outlineVariant,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .clickable {
                                                            selectedCell = Pair(r, c)
                                                        }
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    BasicTextField(
                                                        value = cell.text,
                                                        onValueChange = { newTxt ->
                                                            cell.text = newTxt
                                                            blocks[index] = block.copy(matrix = block.matrix)
                                                        },
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 15.sp
                                                        ),
                                                        cursorBrush = SolidColor(amber),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { selectedCell = Pair(r, c) },
                                                        decorationBox = { innerTextField ->
                                                            if (cell.text.isEmpty()) {
                                                                Text(
                                                                    text = "",
                                                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 15.sp)
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (r < block.rows - 1) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                    }

                                    if (isEditingTable && selectedCell != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Celda [${selectedCell!!.first + 1}, ${selectedCell!!.second + 1}]", style = MaterialTheme.typography.bodySmall, color = amber)
                                            TextButton(onClick = { selectedCell = null }) {
                                                Text("Deseleccionar", fontSize = 11.sp)
                                            }
                                        }
                                        val palette = listOf(
                                            "Rojo" to "#EF4444",
                                            "Azul" to "#3B82F6",
                                            "Verde" to "#10B981",
                                            "Amarillo" to "#F59E0B",
                                            "Naranja" to "#F97316",
                                            "Morado" to "#8B5CF6",
                                            "Sin color" to null
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(palette.size) { idx ->
                                                val (cName, hex) = palette[idx]
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        val (r, c) = selectedCell!!
                                                        block.matrix[r][c].backgroundColorHex = hex
                                                        blocks[index] = block.copy(matrix = block.matrix)
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text("$cName (Celda)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), fontSize = 10.sp, color = if (hex != null) Color.White else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(palette.size) { idx ->
                                                val (cName, hex) = palette[idx]
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        val (r, _) = selectedCell!!
                                                        block.matrix[r].forEach { cellItem ->
                                                            cellItem.backgroundColorHex = hex
                                                        }
                                                        blocks[index] = block.copy(matrix = block.matrix)
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text("$cName (Fila)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), fontSize = 10.sp, color = if (hex != null) Color.White else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(palette.size) { idx ->
                                                val (cName, hex) = palette[idx]
                                                Surface(
                                                    modifier = Modifier.clickable {
                                                        val (_, c) = selectedCell!!
                                                        block.matrix.forEach { rowItem ->
                                                            if (c in rowItem.indices) {
                                                                rowItem[c].backgroundColorHex = hex
                                                            }
                                                        }
                                                        blocks[index] = block.copy(matrix = block.matrix)
                                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (hex != null) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text("$cName (Col)", modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), fontSize = 10.sp, color = if (hex != null) Color.White else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (showTableMenu) {
                                AlertDialog(
                                    onDismissRequest = { showTableMenu = false },
                                    modifier = Modifier.testTag("menu_edit_table"),
                                    title = { Text("Opciones de tabla") },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    showTableMenu = false
                                                    isEditingTable = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🎨 Editar tabla (Tamaño y Colores)", color = amber)
                                            }
                                            TextButton(
                                                onClick = {
                                                    showTableMenu = false
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    blocks.removeAt(index)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("🗑️ Eliminar tabla completa", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { showTableMenu = false }) {
                                            Text("Cerrar")
                                        }
                                    }
                                )
                            }
                        }
                        is PdfBlock -> {
                            PdfBlockItem(
                                block = block,
                                onPageChange = { newIndex ->
                                    blocks[index] = block.copy(currentPageIndex = newIndex)
                                },
                                onToggleExpand = {
                                    blocks[index] = block.copy(isExpanded = !block.isExpanded)
                                },
                                onDeleteBlock = {
                                    blocks.removeAt(index)
                                },
                                modifier = Modifier.testTag("block_pdf_$index")
                            )
                        }
                        is AudioBlock -> {
                            AudioBlockItem(
                                block = block,
                                onToggleTranscription = {
                                    blocks[index] = block.copy(isTranscriptionVisible = !block.isTranscriptionVisible)
                                },
                                onDeleteBlock = {
                                    val file = File(block.filePath)
                                    if (file.exists()) file.delete()
                                    blocks.removeAt(index)
                                },
                                modifier = Modifier.testTag("block_audio_$index")
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Format Popover (Aa) - Liquid Glass Anchored Popover
    if (showFormatSheet) {
        Popup(
            onDismissRequest = { showFormatSheet = false },
            alignment = Alignment.BottomStart,
            offset = IntOffset(x = 40, y = -75)
        ) {
            AnimatedVisibility(
                visible = showFormatSheet,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .width(230.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isDark) Color(0xFF1E1E22).copy(alpha = 0.90f)
                            else Color.White.copy(alpha = 0.90f)
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isDark) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color(0xFF3F3F46).copy(alpha = 0.50f)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.90f),
                                        Color.White.copy(alpha = 0.25f),
                                        Color(0xFFCBD5E1).copy(alpha = 0.40f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(20.dp)
                        ),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FormatOptionItem(
                            icon = "📝",
                            text = "Texto normal (Cuerpo)",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.Text(id = newId, style = TextStyleType.BODY))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "𝐇₁",
                            text = "Título grande",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.Text(id = newId, style = TextStyleType.TITLE))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "𝐇₂",
                            text = "Subtítulo",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.Text(id = newId, style = TextStyleType.SUBTITLE))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "•",
                            text = "Lista con viñetas",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.SimpleList(id = newId, isNumbered = false))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "1.",
                            text = "Lista numerada",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.SimpleList(id = newId, isNumbered = true))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "▢",
                            text = "Cuadro destacado",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            val newId = UUID.randomUUID().toString()
                            blocks.add(NoteBlock.CalloutBox(id = newId))
                            targetFocusBlockId = newId
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        FormatOptionItem(
                            icon = "▦",
                            text = "Tabla",
                            isDark = isDark
                        ) {
                            showFormatSheet = false
                            createTableRows = 3
                            createTableCols = 3
                            showCreateTableDialog = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            }
        }
    }

    if (showCreateTableDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTableDialog = false },
            modifier = Modifier.testTag("dialog_create_table"),
            title = { Text("Crear tabla") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filas: $createTableRows", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (createTableRows > 1) createTableRows-- },
                                enabled = createTableRows > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos filas")
                            }
                            Text("$createTableRows", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(
                                onClick = { if (createTableRows < 10) createTableRows++ },
                                enabled = createTableRows < 10
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más filas")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Columnas: $createTableCols", style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (createTableCols > 1) createTableCols-- },
                                enabled = createTableCols > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos columnas")
                            }
                            Text("$createTableCols", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(
                                onClick = { if (createTableCols < 6) createTableCols++ },
                                enabled = createTableCols < 6
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Más columnas")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateTableDialog = false
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val newId = UUID.randomUUID().toString()
                        blocks.add(NoteBlock.Table(id = newId, rows = createTableRows, cols = createTableCols))
                        targetFocusBlockId = newId
                    }
                ) {
                    Text("Crear tabla", color = amber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTableDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Programar recordatorio") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showReminderDialog = false
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                if (timeInMillis <= System.currentTimeMillis()) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            val chosenTimestamp = cal.timeInMillis
                            reminderTimestamp = chosenTimestamp
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (noteId > 0) {
                                viewModel.setReminder(context, noteId, chosenTimestamp, title, NoteBlockSerializer.serialize(blocks))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hoy a las 18:00")
                    }

                    TextButton(
                        onClick = {
                            showReminderDialog = false
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val chosenTimestamp = cal.timeInMillis
                            reminderTimestamp = chosenTimestamp
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (noteId > 0) {
                                viewModel.setReminder(context, noteId, chosenTimestamp, title, NoteBlockSerializer.serialize(blocks))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mañana a las 09:00")
                    }

                    TextButton(
                        onClick = {
                            showReminderDialog = false
                            openDateTimePicker()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Elegir fecha y hora...")
                    }

                    if (reminderTimestamp != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        TextButton(
                            onClick = {
                                showReminderDialog = false
                                reminderTimestamp = null
                                if (noteId > 0) {
                                    viewModel.cancelReminder(context, noteId)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Eliminar recordatorio", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Color de nota") },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(6) { idx ->
                        val col = NoteColors.getColor(idx, isDark)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (colorIndex == idx) 2.dp else 1.dp,
                                    color = if (colorIndex == idx) amber else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorIndex = idx
                                    showColorDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = { showPaperDialog = false },
            title = { Text("Estilo de papel") },
            text = {
                Column {
                    PaperStyle.entries.forEach { style ->
                        TextButton(
                            onClick = {
                                paperStyle = style
                                showPaperDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = when (style) {
                                    PaperStyle.BLANK -> "Liso"
                                    PaperStyle.RULED -> "Rayado"
                                    PaperStyle.GRID -> "Cuadriculado"
                                    PaperStyle.DOTS -> "Punteado"
                                },
                                color = if (paperStyle == style) amber else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaperDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

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

    if (showStatsDialog) {
        val plainTextSnippet = blocks.joinToString(" ") {
            when (it) {
                is NoteBlock.Text -> it.content
                is NoteBlock.Checklist -> it.content
                is NoteBlock.SimpleList -> it.content
                is NoteBlock.CalloutBox -> it.content
                is NoteBlock.Drawing -> ""
                is NoteBlock.Table -> it.matrix.joinToString(" ") { row -> row.joinToString(" ") { cell -> cell.text } }
                is PdfBlock -> it.fileName
                is AudioBlock -> it.transcription
            }
        }
        val wordCount = plainTextSnippet.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val charCount = plainTextSnippet.length
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Información de la nota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Palabras: $wordCount")
                    Text("Caracteres: $charCount")
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    Text("Creada: ${sdf.format(Date(createdAt))}")
                    Text("Modificada: ${sdf.format(Date(updatedAt))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun FormatOptionItem(
    icon: String,
    text: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 15.sp,
                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF334155),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = if (isDark) Color.White else Color(0xFF0F172A)
        )
    }
}

private fun hashtagVisualTransformation(isDark: Boolean): VisualTransformation {
    val lightIndigo = Color(0xFF1A365D)
    val darkIndigo = Color(0xFF60A5FA)
    val tagColor = if (isDark) darkIndigo else lightIndigo

    return VisualTransformation { text ->
        val fullText = text.text
        val annotatedString = buildAnnotatedString {
            val hashtagRegex = Regex("""#([a-zA-Z0-9_áéíóúÁÉÍÓÚñÑ]+)""")
            val matches = hashtagRegex.findAll(fullText).toList()
            var lastIndex = 0

            for (match in matches) {
                val start = match.range.first
                val end = match.range.last + 1
                if (start > lastIndex) {
                    append(fullText.substring(lastIndex, start))
                }
                withStyle(
                    SpanStyle(
                        color = tagColor,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(fullText.substring(start, end))
                }
                lastIndex = end
            }
            if (lastIndex < fullText.length) {
                append(fullText.substring(lastIndex))
            }
        }
        TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
