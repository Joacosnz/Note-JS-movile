package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CalendarCategoryPresets
import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.ReminderEntity
import com.example.model.UnifiedReminderItem
import com.example.ui.components.editor.ChecklistItemReminderDialog
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.ReminderViewModel
import com.example.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val title: String, val icon: ImageVector) {
    DAY("Día", Icons.Default.CalendarViewDay),
    MULTI_DAY("3 Días", Icons.Default.ViewColumn),
    WEEK("Semana", Icons.Default.CalendarViewWeek),
    MONTH("Mes", Icons.Default.CalendarMonth),
    SMART_LISTS("Listas", Icons.Default.Checklist)
}

enum class SmartListTab(val title: String, val badgeColor: Long) {
    HOY("Hoy", 0xFF007AFF),
    PROGRAMADOS("Programados", 0xFFFF3B30),
    TODOS("Todos", 0xFF8E8E93),
    PRIORIDAD("Prioridad", 0xFFFF9500),
    COMPLETADOS("Completados", 0xFF34C759)
}

@Composable
fun RemindersScreen(
    viewModel: NotesViewModel,
    reminderViewModel: ReminderViewModel = viewModel(),
    initialOpenReminderId: Long = 0L,
    initialCreateNewReminder: Boolean = false,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val notesCalendarReminders by viewModel.calendarReminders.collectAsStateWithLifecycle()
    val dedicatedCalendarItems by reminderViewModel.allCalendarItems.collectAsStateWithLifecycle()
    val calendarReminders = remember(notesCalendarReminders, dedicatedCalendarItems) {
        val merged = linkedMapOf<String, ReminderEntity>()
        dedicatedCalendarItems.forEach { item ->
            val key = item.remoteUuid.ifBlank { "d_${item.id}" }
            merged[key] = item
        }
        notesCalendarReminders.forEach { item ->
            val key = item.remoteUuid.ifBlank { "n_${item.id}" }
            val existing = merged[key]
            if (existing == null || item.lastModifiedLocal > existing.lastModifiedLocal) {
                merged[key] = item
            }
        }
        merged.values.sortedWith(compareBy<ReminderEntity> { it.scheduledAt }.thenByDescending { it.priority })
    }
    val checklistReminders by viewModel.unifiedReminders.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.DAY) }
    var selectedSmartTab by rememberSaveable { mutableStateOf(SmartListTab.HOY) }
    var selectedDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    var showCompleted by rememberSaveable { mutableStateOf(true) }
    var showEvents by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var showHybridModal by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ReminderEntity?>(null) }
    var modalInitialScheduledAt by remember { mutableLongStateOf(System.currentTimeMillis() + 3600_000L) }
    var modalInitialIsAllDay by remember { mutableStateOf(false) }
    var modalInitialType by remember { mutableStateOf(CalendarItemType.REMINDER) }

    var itemForEditChecklistReminder by remember { mutableStateOf<UnifiedReminderItem?>(null) }

    LaunchedEffect(initialCreateNewReminder, initialOpenReminderId, calendarReminders) {
        if (initialCreateNewReminder || initialOpenReminderId == -2L) {
            editingItem = null
            modalInitialScheduledAt = System.currentTimeMillis() + 3600_000L
            modalInitialType = CalendarItemType.REMINDER
            showHybridModal = true
        } else if (initialOpenReminderId > 0L && calendarReminders.isNotEmpty()) {
            val target = calendarReminders.firstOrNull { it.id == initialOpenReminderId }
            if (target != null) {
                selectedDateMillis = target.scheduledAt
                editingItem = target
                showHybridModal = true
            }
        }
    }

    if (showBackButton) {
        BackHandler {
            viewModel.navigateBack()
        }
    }

    val searchFilteredReminders = remember(calendarReminders, searchQuery) {
        if (searchQuery.isBlank()) {
            calendarReminders
        } else {
            val q = searchQuery.trim()
            calendarReminders.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.notes.contains(q, ignoreCase = true) ||
                    it.location.contains(q, ignoreCase = true) ||
                    it.listName.contains(q, ignoreCase = true) ||
                    it.linkedNoteTitle.contains(q, ignoreCase = true)
            }
        }
    }

    val (_, todayEnd) = remember { getDayBounds(System.currentTimeMillis()) }
    val pendingTodayCount = remember(calendarReminders, todayEnd) {
        calendarReminders.count { !it.isCompleted && it.scheduledAt <= todayEnd }
    }
    val totalPendingCount = remember(calendarReminders) {
        calendarReminders.count { !it.isCompleted }
    }

    val headerDateTitle = remember(selectedDateMillis, viewMode) {
        val pattern = when (viewMode) {
            CalendarViewMode.DAY -> "EEE d 'de' MMM yyyy"
            CalendarViewMode.MULTI_DAY, CalendarViewMode.WEEK -> "MMMM yyyy"
            CalendarViewMode.MONTH -> "MMMM yyyy"
            CalendarViewMode.SMART_LISTS -> "Listas Inteligentes"
        }
        if (viewMode == CalendarViewMode.SMART_LISTS) {
            "Listas y Categorías"
        } else {
            SimpleDateFormat(pattern, Locale("es", "ES"))
                .format(Date(selectedDateMillis))
                .replaceFirstChar { it.titlecase(Locale("es", "ES")) }
        }
    }

    val shiftSelectedDate: (Int) -> Unit = { direction ->
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        when (viewMode) {
            CalendarViewMode.DAY -> cal.add(Calendar.DAY_OF_YEAR, direction)
            CalendarViewMode.MULTI_DAY -> cal.add(Calendar.DAY_OF_YEAR, direction * 3)
            CalendarViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, direction)
            CalendarViewMode.MONTH -> cal.add(Calendar.MONTH, direction)
            CalendarViewMode.SMART_LISTS -> cal.add(Calendar.DAY_OF_YEAR, direction)
        }
        selectedDateMillis = cal.timeInMillis
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("reminders_calendar_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val baseCal = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        val nowCal = Calendar.getInstance()
                        set(Calendar.HOUR_OF_DAY, (nowCal.get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23))
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    editingItem = null
                    modalInitialScheduledAt = baseCal.timeInMillis
                    modalInitialIsAllDay = false
                    modalInitialType = CalendarItemType.REMINDER
                    showHybridModal = true
                },
                containerColor = amber,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("fab_new_calendar_item")
                    .padding(bottom = if (showBackButton) 16.dp else 84.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo recordatorio o evento"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. BARRA SUPERIOR (Título + Controles de fecha Hoy/< /> + Búsqueda)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
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
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerDateTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$pendingTodayCount para hoy • $totalPendingCount pendientes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (viewMode != CalendarViewMode.SMART_LISTS) {
                    IconButton(
                        onClick = { shiftSelectedDate(-1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Periodo anterior"
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = amber.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, amber.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clickable { selectedDateMillis = System.currentTimeMillis() }
                            .testTag("btn_calendar_today")
                    ) {
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = amber,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = { shiftSelectedDate(1) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Periodo siguiente"
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) searchQuery = ""
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Buscar en calendario",
                        tint = if (isSearchVisible) amber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isSearchVisible) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar recordatorios, eventos, notas o lugares...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            // 2. SELECTOR DE ESCALA TEMPORAL (Día | 3 Días | Semana | Mes | Listas)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CalendarViewMode.entries.forEach { mode ->
                        val active = viewMode == mode
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) amber else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewMode = mode
                                }
                                .testTag("tab_calendar_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. FILTROS RÁPIDOS DE CAPAS (Eventos, Completados y Listas de Categoría)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = showEvents,
                        onClick = { showEvents = !showEvents },
                        label = { Text("Eventos", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = showCompleted,
                        onClick = { showCompleted = !showCompleted },
                        label = { Text("Completados", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 4. CONTENIDO PRINCIPAL SEGÚN LA VISTA SELECCIONADA
            when (viewMode) {
                CalendarViewMode.DAY -> {
                    val dayItems = remember(
                        searchFilteredReminders,
                        selectedDateMillis,
                        showCompleted,
                        showEvents,
                        selectedCategory
                    ) {
                        filterItemsForDay(
                            items = searchFilteredReminders,
                            dayStartMillis = selectedDateMillis,
                            includeCompleted = showCompleted,
                            includeEvents = showEvents,
                            selectedCategory = selectedCategory
                        )
                    }

                    DayTimelineView(
                        selectedDateMillis = selectedDateMillis,
                        items = dayItems,
                        amber = amber,
                        onSlotClick = { hour, minute, isAllDay ->
                            val slotCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDateMillis
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                            }
                            editingItem = null
                            modalInitialScheduledAt = slotCal.timeInMillis
                            modalInitialIsAllDay = isAllDay
                            modalInitialType = CalendarItemType.REMINDER
                            showHybridModal = true
                        },
                        onItemClick = { item ->
                            editingItem = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            showHybridModal = true
                        },
                        onToggleReminderCompleted = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val real = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            reminderViewModel.toggleCalendarItemCompleted(real)
                            viewModel.toggleCalendarReminderCompleted(real)
                        },
                        onQuickReschedule = { item, newScheduledAt ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val real = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            val duration = if (real.endTimeMillis > real.scheduledAt) {
                                real.endTimeMillis - real.scheduledAt
                            } else 0L
                            val newEnd = if (duration > 0L) newScheduledAt + duration else 0L
                            reminderViewModel.rescheduleCalendarItem(
                                item = real,
                                newStartMillis = newScheduledAt,
                                newEndMillis = newEnd
                            )
                            viewModel.rescheduleCalendarItem(
                                reminder = real,
                                newStartMillis = newScheduledAt,
                                newEndMillis = newEnd
                            )
                        },
                        onOpenLinkedNote = { noteId ->
                            viewModel.navigateTo(Screen.Editor(noteId = noteId))
                        }
                    )
                }

                CalendarViewMode.MULTI_DAY -> {
                    MultiDayTimelineView(
                        startDateMillis = selectedDateMillis,
                        daysCount = 3,
                        allItems = searchFilteredReminders,
                        includeCompleted = showCompleted,
                        includeEvents = showEvents,
                        selectedCategory = selectedCategory,
                        amber = amber,
                        onSelectDay = { dayMs ->
                            selectedDateMillis = dayMs
                            viewMode = CalendarViewMode.DAY
                        },
                        onSlotClick = { dayMs, hour ->
                            val slotCal = Calendar.getInstance().apply {
                                timeInMillis = dayMs
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            selectedDateMillis = dayMs
                            editingItem = null
                            modalInitialScheduledAt = slotCal.timeInMillis
                            modalInitialIsAllDay = false
                            modalInitialType = CalendarItemType.REMINDER
                            showHybridModal = true
                        },
                        onItemClick = { item ->
                            editingItem = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            showHybridModal = true
                        },
                        onToggleReminderCompleted = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val real = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            reminderViewModel.toggleCalendarItemCompleted(real)
                            viewModel.toggleCalendarReminderCompleted(real)
                        }
                    )
                }

                CalendarViewMode.WEEK -> {
                    MultiDayTimelineView(
                        startDateMillis = selectedDateMillis,
                        daysCount = 7,
                        allItems = searchFilteredReminders,
                        includeCompleted = showCompleted,
                        includeEvents = showEvents,
                        selectedCategory = selectedCategory,
                        amber = amber,
                        onSelectDay = { dayMs ->
                            selectedDateMillis = dayMs
                            viewMode = CalendarViewMode.DAY
                        },
                        onSlotClick = { dayMs, hour ->
                            val slotCal = Calendar.getInstance().apply {
                                timeInMillis = dayMs
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            selectedDateMillis = dayMs
                            editingItem = null
                            modalInitialScheduledAt = slotCal.timeInMillis
                            modalInitialIsAllDay = false
                            modalInitialType = CalendarItemType.REMINDER
                            showHybridModal = true
                        },
                        onItemClick = { item ->
                            editingItem = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            showHybridModal = true
                        },
                        onToggleReminderCompleted = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val real = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            reminderViewModel.toggleCalendarItemCompleted(real)
                            viewModel.toggleCalendarReminderCompleted(real)
                        }
                    )
                }

                CalendarViewMode.MONTH -> {
                    MonthPinchZoomView(
                        selectedDateMillis = selectedDateMillis,
                        allItems = searchFilteredReminders,
                        includeCompleted = showCompleted,
                        includeEvents = showEvents,
                        selectedCategory = selectedCategory,
                        amber = amber,
                        onSelectDate = { dayMs ->
                            selectedDateMillis = dayMs
                        },
                        onCreateAtDate = { dayMs ->
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = dayMs
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            editingItem = null
                            modalInitialScheduledAt = cal.timeInMillis
                            modalInitialIsAllDay = false
                            modalInitialType = CalendarItemType.REMINDER
                            showHybridModal = true
                        },
                        onItemClick = { item ->
                            editingItem = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            showHybridModal = true
                        },
                        onToggleReminderCompleted = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val real = calendarReminders.firstOrNull { it.id == item.id } ?: item
                            reminderViewModel.toggleCalendarItemCompleted(real)
                            viewModel.toggleCalendarReminderCompleted(real)
                        },
                        onOpenLinkedNote = { noteId ->
                            viewModel.navigateTo(Screen.Editor(noteId = noteId))
                        }
                    )
                }

                CalendarViewMode.SMART_LISTS -> {
                    SmartListsView(
                        allReminders = searchFilteredReminders,
                        checklistReminders = checklistReminders,
                        selectedSmartTab = selectedSmartTab,
                        selectedCategory = selectedCategory,
                        showEvents = showEvents,
                        amber = amber,
                        onSelectSmartTab = { selectedSmartTab = it },
                        onItemClick = { item ->
                            editingItem = item
                            showHybridModal = true
                        },
                        onToggleCompleted = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            reminderViewModel.toggleCalendarItemCompleted(item)
                            viewModel.toggleCalendarReminderCompleted(item)
                        },
                        onOpenLinkedNote = { noteId ->
                            viewModel.navigateTo(Screen.Editor(noteId = noteId))
                        },
                        onToggleChecklistReminder = { unified ->
                            viewModel.toggleUnifiedReminder(unified)
                        },
                        onEditChecklistReminder = { unified ->
                            itemForEditChecklistReminder = unified
                        }
                    )
                }
            }
        }
    }

    val allTags = remember(calendarReminders) {
        calendarReminders.flatMap { it.tags }.toSet().toList().sorted()
    }

    // Modal Híbrido de Creación / Edición ([ Recordatorio | Evento ])
    if (showHybridModal) {
        HybridCalendarItemSheet(
            existingItem = editingItem,
            initialScheduledAt = modalInitialScheduledAt,
            initialIsAllDay = modalInitialIsAllDay,
            initialItemType = modalInitialType,
            availableNotes = uiState.activeNotes,
            allTags = allTags,
            amber = amber,
            onDismiss = {
                showHybridModal = false
                editingItem = null
            },
            onSave = { entityToSave ->
                reminderViewModel.saveCalendarItem(item = entityToSave)
                viewModel.saveCalendarItem(item = entityToSave)
                showHybridModal = false
                editingItem = null
            },
            onDelete = { toDelete ->
                reminderViewModel.deleteCalendarItem(toDelete)
                viewModel.deleteCalendarItem(toDelete)
                showHybridModal = false
                editingItem = null
            },
            onOpenLinkedNote = { noteId ->
                showHybridModal = false
                editingItem = null
                viewModel.navigateTo(Screen.Editor(noteId = noteId))
            }
        )
    }

    itemForEditChecklistReminder?.let { targetItem ->
        ChecklistItemReminderDialog(
            initialTimestamp = targetItem.reminderTimestamp,
            initialPriority = targetItem.priority,
            itemTitle = targetItem.title,
            amber = amber,
            onDismiss = { itemForEditChecklistReminder = null },
            onSave = { newTs, newPrio ->
                if (targetItem.checkItemId != null) {
                    viewModel.setChecklistItemReminder(
                        noteId = targetItem.noteId,
                        itemId = targetItem.checkItemId,
                        timestamp = newTs,
                        priority = newPrio
                    )
                } else {
                    viewModel.setReminder(
                        context = context,
                        noteId = targetItem.noteId,
                        timestamp = newTs,
                        title = targetItem.noteTitle,
                        content = ""
                    )
                }
                itemForEditChecklistReminder = null
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
                itemForEditChecklistReminder = null
            }
        )
    }
}

@Composable
private fun SmartListsView(
    allReminders: List<ReminderEntity>,
    checklistReminders: List<UnifiedReminderItem>,
    selectedSmartTab: SmartListTab,
    selectedCategory: String?,
    showEvents: Boolean,
    amber: Color,
    onSelectSmartTab: (SmartListTab) -> Unit,
    onItemClick: (ReminderEntity) -> Unit,
    onToggleCompleted: (ReminderEntity) -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    onToggleChecklistReminder: (UnifiedReminderItem) -> Unit,
    onEditChecklistReminder: (UnifiedReminderItem) -> Unit
) {
    val (_, todayEnd) = remember { getDayBounds(System.currentTimeMillis()) }

    val baseFiltered = remember(allReminders, selectedCategory, showEvents) {
        allReminders.filter { item ->
            (showEvents || !item.isEvent) &&
                (selectedCategory == null || item.listName.equals(selectedCategory, ignoreCase = true))
        }
    }

    val hoyList = remember(baseFiltered, todayEnd) {
        baseFiltered.filter { !it.isCompleted && it.scheduledAt <= todayEnd }.sortedBy { it.scheduledAt }
    }
    val programadosList = remember(baseFiltered, todayEnd) {
        baseFiltered.filter { !it.isCompleted && it.scheduledAt > todayEnd }.sortedBy { it.scheduledAt }
    }
    val todosList = remember(baseFiltered) {
        baseFiltered.sortedWith(compareBy<ReminderEntity> { it.isCompleted }.thenBy { it.scheduledAt })
    }
    val prioridadList = remember(baseFiltered) {
        baseFiltered.filter { !it.isCompleted && it.priority > 0 }
            .sortedWith(compareByDescending<ReminderEntity> { it.priority }.thenBy { it.scheduledAt })
    }
    val completadosList = remember(baseFiltered) {
        baseFiltered.filter { it.isCompleted }.sortedByDescending {
            if (it.completedAt > 0L) it.completedAt else it.scheduledAt
        }
    }

    val displayedItems = when (selectedSmartTab) {
        SmartListTab.HOY -> hoyList
        SmartListTab.PROGRAMADOS -> programadosList
        SmartListTab.TODOS -> todosList
        SmartListTab.PRIORIDAD -> prioridadList
        SmartListTab.COMPLETADOS -> completadosList
    }

    val standaloneChecklistItems = remember(checklistReminders) {
        checklistReminders.filter { it.checkItemId != null }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(SmartListTab.entries.toList()) { tab ->
                val active = selectedSmartTab == tab
                val count = when (tab) {
                    SmartListTab.HOY -> hoyList.size
                    SmartListTab.PROGRAMADOS -> programadosList.size
                    SmartListTab.TODOS -> todosList.size
                    SmartListTab.PRIORIDAD -> prioridadList.size
                    SmartListTab.COMPLETADOS -> completadosList.size
                }
                val tabColor = Color(tab.badgeColor)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (active) tabColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (active) 1.4.dp else 0.7.dp,
                        color = if (active) tabColor else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .width(122.dp)
                        .clickable { onSelectSmartTab(tab) }
                        .testTag("smart_list_card_${tab.name.lowercase()}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(tabColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        SmartListTab.HOY -> Icons.Default.CalendarViewDay
                                        SmartListTab.PROGRAMADOS -> Icons.Default.Schedule
                                        SmartListTab.TODOS -> Icons.Default.Checklist
                                        SmartListTab.PRIORIDAD -> Icons.Default.Flag
                                        SmartListTab.COMPLETADOS -> Icons.Default.TaskAlt
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) tabColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (displayedItems.isEmpty() && standaloneChecklistItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(amber.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Sin elementos en ${selectedSmartTab.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pulsa el botón + para crear un recordatorio o evento independiente, o vincularlo con cualquiera de tus notas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedItems, key = { "cal_${it.id}" }) { item ->
                    CalendarItemTaxonomyCard(
                        item = item,
                        compact = false,
                        onItemClick = { onItemClick(item) },
                        onToggleCompleted = { onToggleCompleted(item) },
                        onOpenLinkedNote = onOpenLinkedNote
                    )
                }

                if (standaloneChecklistItems.isNotEmpty() && selectedSmartTab == SmartListTab.TODOS) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "TAREAS PROGRAMADAS EN CHECKLISTS DE NOTAS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(standaloneChecklistItems, key = { it.id }) { checkItem ->
                        ChecklistUnifiedReminderRow(
                            item = checkItem,
                            amber = amber,
                            onToggleCompleted = { onToggleChecklistReminder(checkItem) },
                            onNavigateToNote = { onOpenLinkedNote(checkItem.noteId) },
                            onEditReminder = { onEditChecklistReminder(checkItem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistUnifiedReminderRow(
    item: UnifiedReminderItem,
    amber: Color,
    onToggleCompleted: () -> Unit,
    onNavigateToNote: () -> Unit,
    onEditReminder: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("EEE d MMM, HH:mm", Locale("es", "ES")) }
    val formattedTime = remember(item.reminderTimestamp) { sdf.format(Date(item.reminderTimestamp)) }
    val checkboxBg by animateColorAsState(
        targetValue = if (item.isCompleted) amber else Color.Transparent,
        label = "chk_bg"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, amber, CircleShape)
                    .background(checkboxBg)
                    .clickable { onToggleCompleted() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🔗 ${item.noteTitle}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = amber,
                        modifier = Modifier.clickable { onNavigateToNote() }
                    )
                }
            }
            IconButton(onClick = onEditReminder, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.EditCalendar, contentDescription = "Editar", modifier = Modifier.size(16.dp))
            }
        }
    }
}
