package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.NoteType
import com.example.model.UnifiedReminderItem
import com.example.ui.components.editor.ChecklistItemReminderDialog
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReminderFilterTab(val title: String) {
    HOY("Hoy"),
    PROGRAMADOS("Programados"),
    COMPLETADOS("Hechos"),
    TODOS("Todos")
}

@Composable
fun RemindersScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.unifiedReminders.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(ReminderFilterTab.HOY) }
    var itemForEditReminder by remember { mutableStateOf<UnifiedReminderItem?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    val now = System.currentTimeMillis()
    val calendarTodayStart = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val calendarTodayEnd = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // Clasificación de recordatorios (ordenados por fecha)
    val hoyList = remember(reminders, calendarTodayEnd) {
        reminders.filter {
            !it.isCompleted && it.reminderTimestamp <= calendarTodayEnd
        }.sortedBy { it.reminderTimestamp }
    }
    val programadosList = remember(reminders, calendarTodayEnd) {
        reminders.filter {
            !it.isCompleted && it.reminderTimestamp > calendarTodayEnd
        }.sortedBy { it.reminderTimestamp }
    }
    val completadosList = remember(reminders) {
        reminders.filter { it.isCompleted }.sortedByDescending { it.reminderTimestamp }
    }
    val todosList = remember(reminders) {
        reminders.sortedBy { it.reminderTimestamp }
    }

    // Auto-selección inicial inteligente
    var tabHasBeenSetManually by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(reminders) {
        if (!tabHasBeenSetManually && reminders.isNotEmpty()) {
            if (hoyList.isNotEmpty()) {
                selectedTab = ReminderFilterTab.HOY
            } else if (programadosList.isNotEmpty()) {
                selectedTab = ReminderFilterTab.PROGRAMADOS
            } else if (todosList.isNotEmpty()) {
                selectedTab = ReminderFilterTab.TODOS
            }
        }
    }

    val displayedList = when (selectedTab) {
        ReminderFilterTab.HOY -> hoyList
        ReminderFilterTab.PROGRAMADOS -> programadosList
        ReminderFilterTab.COMPLETADOS -> completadosList
        ReminderFilterTab.TODOS -> todosList
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.navigateTo(Screen.Editor(noteId = 0L, initialType = NoteType.CHECKLIST))
                },
                containerColor = amber,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("fab_new_checklist_note")
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva lista de tareas"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.navigateBack()
                    },
                    modifier = Modifier.testTag("btn_reminders_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = amber
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recordatorios y Tareas",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${hoyList.size} para hoy • ${reminders.count { !it.isCompleted }} pendientes en total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(amber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Barra de pestañas tipo pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderFilterTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val count = when (tab) {
                        ReminderFilterTab.HOY -> hoyList.size
                        ReminderFilterTab.PROGRAMADOS -> programadosList.size
                        ReminderFilterTab.COMPLETADOS -> completadosList.size
                        ReminderFilterTab.TODOS -> todosList.size
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tabHasBeenSetManually = true
                            selectedTab = tab
                        },
                        label = {
                            Text(
                                text = "${tab.title} ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = amber.copy(alpha = 0.20f),
                            selectedLabelColor = amber
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Lista reactiva de recordatorios
            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(amber.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    ReminderFilterTab.COMPLETADOS -> Icons.Default.TaskAlt
                                    else -> Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = amber,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedTab) {
                                ReminderFilterTab.HOY -> "¡Todo al día por hoy!"
                                ReminderFilterTab.PROGRAMADOS -> "Sin tareas futuras programadas"
                                ReminderFilterTab.COMPLETADOS -> "No hay tareas completadas"
                                ReminderFilterTab.TODOS -> "Sin recordatorios creados"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "En tus notas, pulsa el icono del reloj ⏰ junto a cualquier elemento de checklist para programarle fecha y hora.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedList, key = { it.id }) { item ->
                        ReminderCardItem(
                            item = item,
                            amber = amber,
                            onToggleCompleted = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleUnifiedReminder(item)
                            },
                            onNavigateToNote = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.navigateTo(Screen.Editor(noteId = item.noteId))
                            },
                            onEditReminder = {
                                itemForEditReminder = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal de edición de recordatorio para un elemento
    itemForEditReminder?.let { targetItem ->
        ChecklistItemReminderDialog(
            initialTimestamp = targetItem.reminderTimestamp,
            initialPriority = targetItem.priority,
            itemTitle = targetItem.title,
            amber = amber,
            onDismiss = { itemForEditReminder = null },
            onSave = { newTs, newPrio ->
                if (targetItem.checkItemId != null) {
                    viewModel.setChecklistItemReminder(
                        noteId = targetItem.noteId,
                        itemId = targetItem.checkItemId,
                        timestamp = newTs,
                        priority = newPrio
                    )
                } else {
                    // Note-level reminder
                    viewModel.setReminder(
                        context = context,
                        noteId = targetItem.noteId,
                        timestamp = newTs,
                        title = targetItem.noteTitle,
                        content = ""
                    )
                }
                itemForEditReminder = null
            },
            onRemove = {
                if (targetItem.checkItemId != null) {
                    viewModel.setChecklistItemReminder(
                        noteId = targetItem.noteId,
                        itemId = targetItem.checkItemId,
                        timestamp = null,
                        priority = 0
                    )
                } else {
                    viewModel.cancelReminder(context, targetItem.noteId)
                }
                itemForEditReminder = null
            }
        )
    }
}

@Composable
private fun ReminderCardItem(
    item: UnifiedReminderItem,
    amber: Color,
    onToggleCompleted: () -> Unit,
    onNavigateToNote: () -> Unit,
    onEditReminder: () -> Unit
) {
    val isOverdue = item.reminderTimestamp < System.currentTimeMillis() && !item.isCompleted
    val sdf = remember(item.reminderTimestamp) {
        SimpleDateFormat("EEE d MMM, HH:mm", Locale("es", "ES"))
    }
    val formattedTime = remember(item.reminderTimestamp) {
        sdf.format(Date(item.reminderTimestamp))
    }

    val statusColor = when {
        item.isCompleted -> Color(0xFF10B981)
        isOverdue -> Color(0xFFEF4444)
        item.priority == 3 -> Color(0xFFEF4444)
        item.priority == 2 -> Color(0xFFF59E0B)
        else -> amber
    }

    val checkboxBg by animateColorAsState(
        targetValue = if (item.isCompleted) amber else Color.Transparent,
        label = "checkbox_bg"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Checkbox circular
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (item.isCompleted) amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .background(checkboxBg)
                        .clickable { onToggleCompleted() }
                        .testTag("btn_toggle_reminder_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Contenido de la tarea
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                            fontSize = 16.sp,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Indicador de fecha, hora y prioridad
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isOverdue) "Vencido: $formattedTime" else formattedTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = statusColor
                        )

                        if (item.priority > 0) {
                            val prioText = when (item.priority) {
                                3 -> "Urgente"
                                2 -> "Media"
                                1 -> "Baja"
                                else -> ""
                            }
                            Text(
                                text = "• $prioText",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }
                    }
                }

                // Botón para editar recordatorio
                IconButton(
                    onClick = onEditReminder,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_edit_reminder_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Editar fecha/hora",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ENLACE BIDIRECCIONAL A LA NOTA (Deep Link Chip)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToNote() }
                    .testTag("btn_deeplink_note_${item.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nota: ${item.noteTitle}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Abrir nota ➔",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = amber
                    )
                }
            }
        }
    }
}
