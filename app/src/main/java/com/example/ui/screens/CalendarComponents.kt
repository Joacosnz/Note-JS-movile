package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CalendarCategoryPresets
import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RecurrenceRule
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.ReminderSubtask
import com.example.data.local.entity.ReminderSubtaskSerializer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Niveles semánticos de densidad para la vista mensual con soporte de Pinch-to-Zoom.
 */
enum class MonthZoomLevel(
    val level: Int,
    val label: String,
    val cellHeight: Dp
) {
    COMPACTO(0, "Compacto", 56.dp),
    APILADO(1, "Apilado", 76.dp),
    DETALLES(2, "Detalles", 104.dp),
    EXPANDIDO(3, "Expandido", 142.dp);

    companion object {
        fun fromFloat(zoomFactor: Float): MonthZoomLevel {
            return when {
                zoomFactor < 1.25f -> COMPACTO
                zoomFactor < 1.85f -> APILADO
                zoomFactor < 2.55f -> DETALLES
                else -> EXPANDIDO
            }
        }
    }
}

/**
 * Proyecta todos los recordatorios y eventos (incluyendo reglas de recurrencia) sobre un día específico.
 */
fun filterItemsForDay(
    items: List<ReminderEntity>,
    dayStartMillis: Long,
    includeCompleted: Boolean,
    includeEvents: Boolean,
    selectedCategory: String?
): List<ReminderEntity> {
    val (startOfDay, endOfDay) = getDayBounds(dayStartMillis)

    return items.mapNotNull { item ->
        if (!includeCompleted && item.isCompleted) return@mapNotNull null
        if (!includeEvents && item.isEvent) return@mapNotNull null
        if (selectedCategory != null && !item.listName.equals(selectedCategory, ignoreCase = true)) {
            return@mapNotNull null
        }
        item.projectOntoDay(startOfDay, endOfDay)
    }.sortedWith(
        compareBy<ReminderEntity> { !it.isAllDay }
            .thenBy { it.scheduledAt }
            .thenByDescending { it.priority }
    )
}

fun getDayBounds(timeMillis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return start to cal.timeInMillis
}

fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

// ============================================================================
// 1. VISTA DIARIA (DAY VIEW) - Timeline 24h + Cabecera "Todo el día"
// ============================================================================

@Composable
fun DayTimelineView(
    selectedDateMillis: Long,
    items: List<ReminderEntity>,
    amber: Color,
    onSlotClick: (hour: Int, minute: Int, isAllDay: Boolean) -> Unit,
    onItemClick: (ReminderEntity) -> Unit,
    onToggleReminderCompleted: (ReminderEntity) -> Unit,
    onQuickReschedule: (ReminderEntity, Long) -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDayItems = remember(items) { items.filter { it.isAllDay } }
    val timedItems = remember(items) { items.filter { !it.isAllDay } }
    var hourHeightDp by remember { mutableFloatStateOf(68f) }
    val hourHeight = hourHeightDp.dp
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedDateMillis) {
        val firstHour = timedItems.minOfOrNull {
            Calendar.getInstance().apply { timeInMillis = it.scheduledAt }.get(Calendar.HOUR_OF_DAY)
        } ?: 8
        val targetHour = max(0, firstHour - 1)
        scrollState.scrollTo(targetHour * 140)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Cabecera Fija: "TODO EL DÍA"
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TODO EL DÍA (${allDayItems.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+ Añadir todo el día",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = amber,
                        modifier = Modifier
                            .clickable { onSlotClick(9, 0, true) }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                            .testTag("btn_add_all_day")
                    )
                }

                if (allDayItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allDayItems.forEach { item ->
                            CalendarItemTaxonomyCard(
                                item = item,
                                compact = true,
                                onItemClick = { onItemClick(item) },
                                onToggleCompleted = { onToggleReminderCompleted(item) },
                                onOpenLinkedNote = onOpenLinkedNote
                            )
                        }
                    }
                }
            }
        }

        // Barra de control de Zoom en Vista Diaria
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Línea horaria diaria",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { hourHeightDp = (hourHeightDp - 14f).coerceAtLeast(42f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_day_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Reducir zoom",
                        modifier = Modifier.size(16.dp),
                        tint = amber
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = amber.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clickable { hourHeightDp = 68f }
                        .testTag("btn_day_zoom_reset")
                ) {
                    Text(
                        text = "${(hourHeightDp / 68f * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = amber,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = { hourHeightDp = (hourHeightDp + 14f).coerceAtMost(140f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_day_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar zoom",
                        modifier = Modifier.size(16.dp),
                        tint = amber
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Cuadrícula horaria vertical (00:00 - 23:00) con soporte de pinch-to-zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            hourHeightDp = (hourHeightDp * zoom).coerceIn(42f, 140f)
                        }
                    }
                }
                .verticalScroll(scrollState)
                .padding(bottom = 88.dp)
        ) {
            val totalHeight = hourHeight * 24
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                // Columna izquierda de etiquetas de hora
                Column(
                    modifier = Modifier
                        .width(58.dp)
                        .fillMaxHeight()
                ) {
                    for (hour in 0..23) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight)
                                .padding(end = 8.dp, top = 4.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:00", hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Lienzo de horas + bloques posicionados
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (hour in 0..23) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(hourHeight)
                                    .clickable { onSlotClick(hour, 0, false) }
                                    .testTag("day_slot_hour_$hour")
                            ) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                            }
                        }
                    }

                    // Agrupamos por hora para manejar colisiones en columnas paralelas
                    val itemsByHourBucket = remember(timedItems) {
                        timedItems.groupBy { item ->
                            Calendar.getInstance().apply { timeInMillis = item.scheduledAt }
                                .get(Calendar.HOUR_OF_DAY)
                        }
                    }

                    itemsByHourBucket.forEach { (hour, bucketItems) ->
                        val count = bucketItems.size
                        bucketItems.forEachIndexed { index, item ->
                            val cal = Calendar.getInstance().apply { timeInMillis = item.scheduledAt }
                            val minute = cal.get(Calendar.MINUTE)
                            val topOffset = hourHeight * (hour + (minute / 60f))
                            val durationHours = if (item.isEvent && item.endTimeMillis > item.scheduledAt) {
                                ((item.endTimeMillis - item.scheduledAt).toFloat() / (60 * 60 * 1000f)).coerceIn(0.65f, 6f)
                            } else {
                                0.82f
                            }
                            val cardHeight = (hourHeight * durationHours).coerceAtLeast(46.dp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topOffset)
                                    .heightIn(min = 46.dp, max = cardHeight)
                            ) {
                                if (index > 0) {
                                    Spacer(modifier = Modifier.weight(index.toFloat()))
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    CalendarItemTaxonomyCard(
                                        item = item,
                                        compact = count > 1,
                                        showQuickShiftControls = count == 1,
                                        onItemClick = { onItemClick(item) },
                                        onToggleCompleted = { onToggleReminderCompleted(item) },
                                        onQuickShiftHour = { deltaHours ->
                                            val newStart = item.scheduledAt + deltaHours * 3600_000L
                                            onQuickReschedule(item, newStart)
                                        },
                                        onOpenLinkedNote = onOpenLinkedNote
                                    )
                                }
                                val remaining = count - 1 - index
                                if (remaining > 0) {
                                    Spacer(modifier = Modifier.weight(remaining.toFloat()))
                                }
                            }
                        }
                    }

                    // Indicador rojo de hora actual en vivo (si es Hoy)
                    if (isSameCalendarDay(selectedDateMillis, System.currentTimeMillis())) {
                        val nowCal = Calendar.getInstance()
                        val nowFraction = nowCal.get(Calendar.HOUR_OF_DAY) + (nowCal.get(Calendar.MINUTE) / 60f)
                        val nowOffset = hourHeight * nowFraction
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = nowOffset - 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color(0xFFFF3B30))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. VISTA MULTI-DÍA (3 DÍAS) Y SEMANA (7 DÍAS)
// ============================================================================

@Composable
fun MultiDayTimelineView(
    startDateMillis: Long,
    daysCount: Int,
    allItems: List<ReminderEntity>,
    includeCompleted: Boolean,
    includeEvents: Boolean,
    selectedCategory: String?,
    amber: Color,
    onSelectDay: (Long) -> Unit,
    onSlotClick: (dayMillis: Long, hour: Int) -> Unit,
    onItemClick: (ReminderEntity) -> Unit,
    onToggleReminderCompleted: (ReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayMillisList = remember(startDateMillis, daysCount) {
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (daysCount == 7) {
            baseCal.firstDayOfWeek = Calendar.MONDAY
            val dow = baseCal.get(Calendar.DAY_OF_WEEK)
            val diff = if (dow == Calendar.SUNDAY) -6 else (Calendar.MONDAY - dow)
            baseCal.add(Calendar.DAY_OF_YEAR, diff)
        }
        List(daysCount) { idx ->
            (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, idx)
            }.timeInMillis
        }
    }

    val dayItemsMap = remember(dayMillisList, allItems, includeCompleted, includeEvents, selectedCategory) {
        dayMillisList.associateWith { dayMs ->
            filterItemsForDay(allItems, dayMs, includeCompleted, includeEvents, selectedCategory)
        }
    }

    val dayNameFormat = remember { SimpleDateFormat("EEE", Locale("es", "ES")) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale("es", "ES")) }
    var hourHeightDp by remember(daysCount) { mutableFloatStateOf(if (daysCount == 7) 54f else 62f) }
    val hourHeight = hourHeightDp.dp
    val defaultBase = remember(daysCount) { if (daysCount == 7) 54f else 62f }
    val scrollState = rememberScrollState()

    LaunchedEffect(startDateMillis, daysCount) {
        scrollState.scrollTo(8 * 120)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Cabecera de columnas de días
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayMillisList.forEach { dayMs ->
                        val isToday = isSameCalendarDay(dayMs, System.currentTimeMillis())
                        val isSelected = isSameCalendarDay(dayMs, startDateMillis)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDay(dayMs) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = dayNameFormat.format(Date(dayMs)).uppercase(Locale("es", "ES")).take(3),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isToday) amber else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday -> amber
                                            isSelected -> amber.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumFormat.format(Date(dayMs)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 13.sp,
                                    color = if (isToday) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Fila "Todo el día" multi-columna
                val anyAllDay = dayItemsMap.values.any { list -> list.any { it.isAllDay } }
                if (anyAllDay) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 46.dp, end = 6.dp, top = 4.dp, bottom = 6.dp)
                    ) {
                        dayMillisList.forEach { dayMs ->
                            val dayAllDay = dayItemsMap[dayMs]?.filter { it.isAllDay }.orEmpty()
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                dayAllDay.take(2).forEach { item ->
                                    MiniMultiDayChip(
                                        item = item,
                                        onClick = { onItemClick(item) },
                                        onToggle = { onToggleReminderCompleted(item) }
                                    )
                                }
                                if (dayAllDay.size > 2) {
                                    Text(
                                        text = "+${dayAllDay.size - 2}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Barra de control de Zoom en Multi-Día / Semana
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (daysCount == 7) "Semana" else "3 Días",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { hourHeightDp = (hourHeightDp - 12f).coerceAtLeast(38f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_multiday_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Reducir zoom",
                        modifier = Modifier.size(16.dp),
                        tint = amber
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = amber.copy(alpha = 0.16f),
                    modifier = Modifier
                        .clickable { hourHeightDp = defaultBase }
                        .testTag("btn_multiday_zoom_reset")
                ) {
                    Text(
                        text = "${(hourHeightDp / defaultBase * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = amber,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = { hourHeightDp = (hourHeightDp + 12f).coerceAtMost(120f) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_multiday_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar zoom",
                        modifier = Modifier.size(16.dp),
                        tint = amber
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // Cuadrícula horaria sincronizada multi-día con soporte de pinch-to-zoom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(daysCount) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            hourHeightDp = (hourHeightDp * zoom).coerceIn(38f, 120f)
                        }
                    }
                }
                .verticalScroll(scrollState)
                .padding(bottom = 88.dp)
        ) {
            val totalHeight = hourHeight * 24
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalHeight)
            ) {
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .fillMaxHeight()
                ) {
                    for (hour in 0..23) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight)
                                .padding(end = 6.dp, top = 2.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", hour),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 6.dp)
                ) {
                    dayMillisList.forEach { dayMs ->
                        val timedForDay = dayItemsMap[dayMs]?.filter { !it.isAllDay }.orEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(
                                    width = 0.25.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (hour in 0..23) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(hourHeight)
                                            .clickable { onSlotClick(dayMs, hour) }
                                    ) {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        )
                                    }
                                }
                            }

                            timedForDay.forEach { item ->
                                val cal = Calendar.getInstance().apply { timeInMillis = item.scheduledAt }
                                val hour = cal.get(Calendar.HOUR_OF_DAY)
                                val minute = cal.get(Calendar.MINUTE)
                                val topOffset = hourHeight * (hour + minute / 60f)
                                val durationHours = if (item.isEvent && item.endTimeMillis > item.scheduledAt) {
                                    ((item.endTimeMillis - item.scheduledAt).toFloat() / 3600_000f).coerceIn(0.75f, 5f)
                                } else {
                                    0.85f
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = topOffset)
                                        .heightIn(min = 36.dp, max = hourHeight * durationHours)
                                        .padding(horizontal = 1.5.dp, vertical = 1.dp)
                                ) {
                                    MiniMultiDayChip(
                                        item = item,
                                        onClick = { onItemClick(item) },
                                        onToggle = { onToggleReminderCompleted(item) }
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

@Composable
private fun MiniMultiDayChip(
    item: ReminderEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val itemColor = Color(item.colorHex)

    if (item.isEvent) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = itemColor.copy(alpha = 0.22f),
            border = BorderStroke(0.5.dp, itemColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(itemColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = item.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (item.isCompleted) 0.25f else 0.55f),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (item.isCompleted) 0.55f else 1f)
                .clickable { onClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, itemColor, CircleShape)
                        .background(if (item.isCompleted) itemColor else Color.Transparent)
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = item.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// 3. VISTA MENSUAL CON PINCH-TO-ZOOM (4 NIVELES DE DENSIDAD SEMÁNTICA)
// ============================================================================

@Composable
fun MonthPinchZoomView(
    selectedDateMillis: Long,
    allItems: List<ReminderEntity>,
    includeCompleted: Boolean,
    includeEvents: Boolean,
    selectedCategory: String?,
    amber: Color,
    onSelectDate: (Long) -> Unit,
    onCreateAtDate: (Long) -> Unit,
    onItemClick: (ReminderEntity) -> Unit,
    onToggleReminderCompleted: (ReminderEntity) -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomScale by remember { mutableFloatStateOf(1.55f) }
    val currentZoomLevel = remember(zoomScale) { MonthZoomLevel.fromFloat(zoomScale) }
    val animatedCellHeight by animateDpAsState(
        targetValue = currentZoomLevel.cellHeight,
        label = "month_cell_height"
    )

    val monthDays = remember(selectedDateMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetMonth = cal.get(Calendar.MONTH)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = if (dow == Calendar.SUNDAY) 6 else (dow - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, -leadingEmpty)

        List(42) {
            val dayMs = cal.timeInMillis
            val inMonth = cal.get(Calendar.MONTH) == targetMonth
            val dayNum = cal.get(Calendar.DAY_OF_MONTH)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            Triple(dayMs, dayNum, inMonth)
        }
    }

    val itemsByDay = remember(monthDays, allItems, includeCompleted, includeEvents, selectedCategory) {
        monthDays.associate { (dayMs, _, _) ->
            dayMs to filterItemsForDay(allItems, dayMs, includeCompleted, includeEvents, selectedCategory)
        }
    }

    val selectedDayItems = remember(selectedDateMillis, allItems, includeCompleted, includeEvents, selectedCategory) {
        filterItemsForDay(allItems, selectedDateMillis, includeCompleted, includeEvents, selectedCategory)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        zoomScale = (zoomScale * zoom).coerceIn(1.0f, 3.2f)
                    }
                }
            }
    ) {
        // Barra de control de densidad (Pinch-to-Zoom + Selector directo accesible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val prevIdx = (currentZoomLevel.ordinal - 1).coerceAtLeast(0)
                        val prevLevel = MonthZoomLevel.entries[prevIdx]
                        zoomScale = when (prevLevel) {
                            MonthZoomLevel.COMPACTO -> 1.0f
                            MonthZoomLevel.APILADO -> 1.5f
                            MonthZoomLevel.DETALLES -> 2.1f
                            MonthZoomLevel.EXPANDIDO -> 2.9f
                        }
                    },
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("btn_month_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Reducir zoom",
                        modifier = Modifier.size(15.dp),
                        tint = amber
                    )
                }
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = currentZoomLevel.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        val nextIdx = (currentZoomLevel.ordinal + 1).coerceAtMost(MonthZoomLevel.entries.size - 1)
                        val nextLevel = MonthZoomLevel.entries[nextIdx]
                        zoomScale = when (nextLevel) {
                            MonthZoomLevel.COMPACTO -> 1.0f
                            MonthZoomLevel.APILADO -> 1.5f
                            MonthZoomLevel.DETALLES -> 2.1f
                            MonthZoomLevel.EXPANDIDO -> 2.9f
                        }
                    },
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("btn_month_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar zoom",
                        modifier = Modifier.size(15.dp),
                        tint = amber
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthZoomLevel.entries.forEach { level ->
                    val active = currentZoomLevel == level
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) amber.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (active) BorderStroke(1.dp, amber) else null,
                        modifier = Modifier
                            .clickable {
                                zoomScale = when (level) {
                                    MonthZoomLevel.COMPACTO -> 1.0f
                                    MonthZoomLevel.APILADO -> 1.5f
                                    MonthZoomLevel.DETALLES -> 2.1f
                                    MonthZoomLevel.EXPANDIDO -> 2.9f
                                }
                            }
                            .testTag("btn_month_zoom_${level.name.lowercase()}")
                    ) {
                        Text(
                            text = level.label,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Cabecera de días de la semana (LUN - DOM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM").forEach { dow ->
                Text(
                    text = dow,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(6) { weekIndex ->
                val weekSlice = monthDays.subList(weekIndex * 7, (weekIndex + 1) * 7)
                if (weekIndex == 5 && weekSlice.none { it.third }) return@items

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(animatedCellHeight)
                        .padding(horizontal = 4.dp)
                ) {
                    weekSlice.forEach { (dayMs, dayNum, inCurrentMonth) ->
                        val dayItems = itemsByDay[dayMs].orEmpty()
                        val isToday = isSameCalendarDay(dayMs, System.currentTimeMillis())
                        val isSelected = isSameCalendarDay(dayMs, selectedDateMillis)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> amber.copy(alpha = 0.14f)
                                        inCurrentMonth -> MaterialTheme.colorScheme.surface
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.4.dp,
                                    color = if (isSelected) amber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDate(dayMs) }
                                .padding(4.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isToday) amber else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isToday -> Color.Black
                                                inCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            }
                                        )
                                    }
                                    if (dayItems.isNotEmpty() && currentZoomLevel == MonthZoomLevel.COMPACTO) {
                                        Text(
                                            text = "${dayItems.size}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                when (currentZoomLevel) {
                                    MonthZoomLevel.COMPACTO -> {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            dayItems.take(5).forEach { item ->
                                                val c = Color(item.colorHex)
                                                if (item.isEvent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(c)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .border(1.2.dp, c, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    MonthZoomLevel.APILADO -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                            dayItems.take(4).forEach { item ->
                                                val c = Color(item.colorHex)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(if (item.isEvent) 1f else 0.82f)
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(
                                                            if (item.isCompleted) c.copy(alpha = 0.35f) else c
                                                        )
                                                )
                                            }
                                            if (dayItems.size > 4) {
                                                Text(
                                                    text = "+${dayItems.size - 4} más",
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    MonthZoomLevel.DETALLES -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dayItems.take(3).forEach { item ->
                                                val c = Color(item.colorHex)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (item.isEvent) c.copy(alpha = 0.22f) else Color.Transparent
                                                        )
                                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                                ) {
                                                    if (!item.isEvent) {
                                                        Text(
                                                            text = if (item.isCompleted) "● " else "○ ",
                                                            fontSize = 8.sp,
                                                            color = c,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 8.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            if (dayItems.size > 3) {
                                                Text(
                                                    text = "+${dayItems.size - 3}",
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    MonthZoomLevel.EXPANDIDO -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            dayItems.take(4).forEach { item ->
                                                val c = Color(item.colorHex)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(
                                                            if (item.isEvent) c.copy(alpha = 0.22f)
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                        .clickable {
                                                            if (item.isEvent) onItemClick(item)
                                                            else onToggleReminderCompleted(item)
                                                        }
                                                        .padding(horizontal = 3.dp, vertical = 2.dp)
                                                ) {
                                                    if (!item.isEvent) {
                                                        Text(
                                                            text = if (item.isCompleted) "✓ " else "○ ",
                                                            fontSize = 9.sp,
                                                            color = c,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = item.title,
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            if (dayItems.size > 4) {
                                                Text(
                                                    text = "+${dayItems.size - 4} más",
                                                    fontSize = 8.5.sp,
                                                    color = amber
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

            // Panel de Agenda del Día Seleccionado debajo del mes
            item {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                val dayTitleFormat = remember {
                    SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dayTitleFormat.format(Date(selectedDateMillis))
                                .replaceFirstChar { it.titlecase(Locale("es", "ES")) },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = { onCreateAtDate(selectedDateMillis) }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir aquí", color = amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (selectedDayItems.isEmpty()) {
                        Text(
                            text = "Sin eventos ni recordatorios para este día.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedDayItems.forEach { item ->
                                CalendarItemTaxonomyCard(
                                    item = item,
                                    compact = false,
                                    onItemClick = { onItemClick(item) },
                                    onToggleCompleted = { onToggleReminderCompleted(item) },
                                    onOpenLinkedNote = onOpenLinkedNote
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. COMPONENTE DE TAXONOMÍA VISUAL (EVENTO = BLOQUE SÓLIDO, RECORDATORIO = ○)
// ============================================================================

@Composable
fun CalendarItemTaxonomyCard(
    item: ReminderEntity,
    compact: Boolean = false,
    showQuickShiftControls: Boolean = false,
    onItemClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onQuickShiftHour: ((Int) -> Unit)? = null,
    onOpenLinkedNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor = Color(item.colorHex)
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val subtasks = remember(item.subtasksJson) { item.subtasks }

    if (item.isEvent) {
        // EVENTO DE CALENDARIO: Bloque rectangular coloreado con barra lateral sólida
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = itemColor.copy(alpha = 0.16f),
            border = BorderStroke(0.8.dp, itemColor.copy(alpha = 0.45f)),
            modifier = modifier
                .fillMaxWidth()
                .clickable { onItemClick() }
                .testTag("calendar_event_card_${item.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (compact) 24.dp else 36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(itemColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = itemColor.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = if (item.tags.isNotEmpty()) item.tags.joinToString(" ") else item.listName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = itemColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    val timeLabel = if (item.isAllDay) {
                        "Todo el día"
                    } else {
                        val startStr = timeFormat.format(Date(item.scheduledAt))
                        val endStr = if (item.endTimeMillis > item.scheduledAt) {
                            " - ${timeFormat.format(Date(item.endTimeMillis))}"
                        } else ""
                        "$startStr$endStr"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.location.isNotBlank() && !compact) {
                            Text(
                                text = "📍 ${item.location}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.linkedNoteId != null && item.linkedNoteId > 0L) {
                            Text(
                                text = "🔗 ${item.linkedNoteTitle.ifBlank { "Nota" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = itemColor,
                                modifier = Modifier.clickable { onOpenLinkedNote(item.linkedNoteId) }
                            )
                        }
                    }
                }

                if (showQuickShiftControls && onQuickShiftHour != null && !item.isAllDay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-1h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQuickShiftHour(-1) }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                        Text(
                            text = "+1h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQuickShiftHour(1) }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    } else {
        // RECORDATORIO: Círculo abierto interactivo ○ + texto limpio con tachado al completar
        val isOverdue = !item.isCompleted && !item.isAllDay && item.scheduledAt < System.currentTimeMillis()
        val circleBg by animateColorAsState(
            targetValue = if (item.isCompleted) itemColor else Color.Transparent,
            label = "reminder_circle_bg"
        )

        val cardBgColor = if (item.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = cardBgColor,
            border = BorderStroke(
                width = 0.8.dp,
                color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (item.isCompleted) 0.3f else 0.55f)
            ),
            modifier = modifier
                .fillMaxWidth()
                .alpha(if (item.isCompleted) 0.6f else 1f)
                .clickable { onItemClick() }
                .testTag("calendar_reminder_card_${item.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(2.dp, itemColor, CircleShape)
                        .background(circleBg)
                        .clickable { onToggleCompleted() }
                        .testTag("btn_toggle_calendar_reminder_${item.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (item.priority > 0) {
                            val prioLabel = when (item.priority) {
                                1 -> "Baja"
                                2 -> "Media"
                                3 -> "Urgente"
                                else -> ""
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (item.priority) {
                                    3 -> Color(0xFFFF3B30).copy(alpha = 0.2f)
                                    2 -> Color(0xFFFF9500).copy(alpha = 0.2f)
                                    else -> Color(0xFF34C759).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = prioLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.priority) {
                                        3 -> Color(0xFFFF3B30)
                                        2 -> Color(0xFFFF9500)
                                        else -> Color(0xFF34C759)
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeText = if (item.isAllDay) "Todo el día" else timeFormat.format(Date(item.scheduledAt))
                        Text(
                            text = if (isOverdue) "Vencido • $timeText" else timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (item.tags.isNotEmpty()) "• ${item.tags.joinToString(" ")}" else "• ${item.listName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = itemColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (subtasks.isNotEmpty()) {
                            val doneCount = subtasks.count { it.isCompleted }
                            Text(
                                text = "☑ $doneCount/${subtasks.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (item.linkedNoteId != null && item.linkedNoteId > 0L) {
                            Text(
                                text = "🔗 ${item.linkedNoteTitle.ifBlank { "Nota" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = itemColor,
                                modifier = Modifier.clickable { onOpenLinkedNote(item.linkedNoteId) }
                            )
                        }
                    }
                }

                if (showQuickShiftControls && onQuickShiftHour != null && !item.isAllDay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "-1h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQuickShiftHour(-1) }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                        Text(
                            text = "+1h",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onQuickShiftHour(1) }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. MODAL HÍBRIDO DE CREACIÓN / EDICIÓN ([ Recordatorio | Evento ])
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HybridCalendarItemSheet(
    existingItem: ReminderEntity?,
    initialScheduledAt: Long,
    initialIsAllDay: Boolean,
    initialItemType: CalendarItemType = CalendarItemType.REMINDER,
    availableNotes: List<NoteEntity>,
    allTags: List<String> = emptyList(),
    amber: Color,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit,
    onDelete: ((ReminderEntity) -> Unit)? = null,
    onOpenLinkedNote: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var itemType by remember(existingItem) {
        mutableStateOf(
            if (existingItem != null) CalendarItemType.fromCode(existingItem.itemType)
            else initialItemType
        )
    }
    var title by remember(existingItem) { mutableStateOf(existingItem?.title ?: "") }
    var notes by remember(existingItem) { mutableStateOf(existingItem?.notes ?: "") }
    var location by remember(existingItem) { mutableStateOf(existingItem?.location ?: "") }
    var scheduledAt by remember(existingItem, initialScheduledAt) {
        mutableLongStateOf(existingItem?.scheduledAt ?: initialScheduledAt)
    }
    var endTimeMillis by remember(existingItem, initialScheduledAt) {
        mutableLongStateOf(
            existingItem?.endTimeMillis?.takeIf { it > 0L } ?: (initialScheduledAt + 3600_000L)
        )
    }
    var isAllDay by remember(existingItem, initialIsAllDay) {
        mutableStateOf(existingItem?.isAllDay ?: initialIsAllDay)
    }
    var priority by remember(existingItem) { mutableIntStateOf(existingItem?.priority ?: 0) }
    var listName by remember(existingItem) {
        mutableStateOf(existingItem?.listName ?: "Personal")
    }
    var tags by remember(existingItem) {
        mutableStateOf(existingItem?.tags ?: emptyList())
    }
    var colorHex by remember(existingItem) {
        mutableLongStateOf(existingItem?.colorHex ?: CalendarCategoryPresets.colorForList("Personal"))
    }
    var recurrenceRule by remember(existingItem) {
        mutableStateOf(RecurrenceRule.fromCode(existingItem?.recurrenceRule))
    }
    var alertOffsetMinutes by remember(existingItem) {
        mutableIntStateOf(existingItem?.alertOffsetMinutes ?: 0)
    }

    val subtasks = remember(existingItem) {
        mutableStateListOf<ReminderSubtask>().apply {
            if (existingItem != null) {
                addAll(existingItem.subtasks)
            }
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    var selectedLinkedNote by remember(existingItem, availableNotes) {
        mutableStateOf(
            availableNotes.firstOrNull {
                (existingItem?.linkedNoteId != null && it.id == existingItem.linkedNoteId) ||
                    (existingItem?.linkedNoteUuid?.isNotBlank() == true && it.remoteUuid == existingItem.linkedNoteUuid)
            }
        )
    }
    var showNotePickerDialog by remember { mutableStateOf(false) }
    var noteSearchQuery by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("EEE d MMM yyyy", Locale("es", "ES")) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingItem == null) "Nuevo en Calendario" else "Editar elemento",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                if (existingItem != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(existingItem) },
                        modifier = Modifier.testTag("btn_delete_calendar_item")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control Segmentado: [ ○ Recordatorio | 🗓️ Evento ]
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    CalendarItemType.entries.forEach { type ->
                        val active = itemType == type
                        val label = if (type == CalendarItemType.REMINDER) "○ Recordatorio" else "🗓️ Evento"
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) amber else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { itemType = type }
                                .testTag("segment_type_${type.name.lowercase()}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = {
                    Text(if (itemType == CalendarItemType.REMINDER) "Título del recordatorio" else "Título del evento")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_calendar_item_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (itemType == CalendarItemType.EVENT) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación o enlace de reunión (opcional)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas o detalles adicionales") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Switch "Todo el día" + Selectores de Fecha y Hora
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(colorHex),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Todo el día",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(colorHex))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val updated = Calendar.getInstance().apply {
                                            timeInMillis = scheduledAt
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }.timeInMillis
                                        val delta = updated - scheduledAt
                                        scheduledAt = updated
                                        endTimeMillis += delta
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(dateFormat.format(Date(scheduledAt)), fontSize = 12.sp)
                        }

                        if (!isAllDay) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = scheduledAt }
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            scheduledAt = Calendar.getInstance().apply {
                                                timeInMillis = scheduledAt
                                                set(Calendar.HOUR_OF_DAY, h)
                                                set(Calendar.MINUTE, min)
                                                set(Calendar.SECOND, 0)
                                            }.timeInMillis
                                            if (endTimeMillis <= scheduledAt) {
                                                endTimeMillis = scheduledAt + 3600_000L
                                            }
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("Inicio: ${timeFormat.format(Date(scheduledAt))}", fontSize = 12.sp)
                            }
                        }
                    }

                    if (!isAllDay && itemType == CalendarItemType.EVENT) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hora de finalización",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = endTimeMillis }
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            val candidate = Calendar.getInstance().apply {
                                                timeInMillis = scheduledAt
                                                set(Calendar.HOUR_OF_DAY, h)
                                                set(Calendar.MINUTE, min)
                                                set(Calendar.SECOND, 0)
                                            }.timeInMillis
                                            endTimeMillis = if (candidate > scheduledAt) candidate else scheduledAt + 1800_000L
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Fin: ${timeFormat.format(Date(endTimeMillis))}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Etiquetas (Hashtags)
            Text(
                text = "ETIQUETAS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            var showAddTagDialog by remember { mutableStateOf(false) }
            var newTagText by remember { mutableStateOf("") }
            
            // Unificamos las etiquetas disponibles globalmente con las seleccionadas localmente (por si el usuario acaba de agregar una nueva)
            val displayedTags = remember(allTags, tags) {
                (allTags + tags).distinct().sorted()
            }

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                displayedTags.forEach { tag ->
                    val isSelected = tags.contains(tag)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            tags = if (isSelected) tags - tag else tags + tag
                            if (colorHex == 0xFF007AFFL && tags.isNotEmpty()) {
                                colorHex = CalendarCategoryPresets.colorForList(tags.first())
                            }
                        },
                        label = { Text(tag, fontSize = 12.sp) }
                    )
                }
                
                // Botón + para agregar nueva etiqueta
                FilterChip(
                    selected = false,
                    onClick = { showAddTagDialog = true },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir", fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            if (showAddTagDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { 
                        showAddTagDialog = false
                        newTagText = "" 
                    },
                    title = { Text("Nueva etiqueta") },
                    text = {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            placeholder = { Text("ej. video para YouTube") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val trimmed = newTagText.trim()
                                if (trimmed.isNotEmpty()) {
                                    val finalTag = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
                                    if (!tags.contains(finalTag)) {
                                        tags = tags + finalTag
                                    }
                                    if (colorHex == 0xFF007AFFL) {
                                        colorHex = CalendarCategoryPresets.colorForList(finalTag)
                                    }
                                }
                                showAddTagDialog = false
                                newTagText = ""
                            }
                        ) {
                            Text("Añadir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            showAddTagDialog = false
                            newTagText = "" 
                        }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prioridad
            Text(
                text = "PRIORIDAD",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "Ninguna",
                    1 to "Baja",
                    2 to "Media",
                    3 to "Urgente"
                ).forEach { (prioVal, prioLabel) ->
                    val active = priority == prioVal
                    FilterChip(
                        selected = active,
                        onClick = { priority = prioVal },
                        label = { Text(prioLabel, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Repetición (Recurrencia)
            Text(
                text = "REPETIR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecurrenceRule.entries.toList()) { rule ->
                    FilterChip(
                        selected = recurrenceRule == rule,
                        onClick = { recurrenceRule = rule },
                        label = { Text(rule.label, fontSize = 12.sp) },
                        leadingIcon = if (rule != RecurrenceRule.NONE) {
                            { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Aviso / Alerta
            Text(
                text = "ALERTA DE NOTIFICACIÓN",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val offsets = listOf(
                    0 to "A la hora",
                    5 to "5 min antes",
                    15 to "15 min antes",
                    30 to "30 min antes",
                    60 to "1 h antes",
                    1440 to "1 día antes"
                )
                items(offsets) { (minOffset, label) ->
                    FilterChip(
                        selected = alertOffsetMinutes == minOffset,
                        onClick = { alertOffsetMinutes = minOffset },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            // Subtareas (para Recordatorios)
            if (itemType == CalendarItemType.REMINDER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SUBTAREAS (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                subtasks.forEachIndexed { idx, sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(colorHex), CircleShape)
                                .background(if (sub.isCompleted) Color(colorHex) else Color.Transparent)
                                .clickable {
                                    subtasks[idx] = sub.copy(isCompleted = !sub.isCompleted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sub.isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { subtasks.removeAt(idx) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar subtarea", modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Añadir subtarea...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val clean = newSubtaskTitle.trim()
                            if (clean.isNotEmpty()) {
                                subtasks.add(ReminderSubtask(title = clean))
                                newSubtaskTitle = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar subtarea", tint = Color(colorHex))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // VINCULACIÓN OPCIONAL CON UNA NOTA
            Text(
                text = "VINCULAR CON UNA NOTA (OPCIONAL)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val linked = selectedLinkedNote
                    if (linked != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onOpenLinkedNote?.invoke(linked.id)
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(colorHex),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = linked.title.ifBlank { "Nota sin título" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Toca para abrir nota • Carpeta: ${linked.folder}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(colorHex)
                                )
                            }
                        }
                        Row {
                            TextButton(onClick = { showNotePickerDialog = true }) {
                                Text("Cambiar", fontSize = 12.sp)
                            }
                            IconButton(onClick = { selectedLinkedNote = null }) {
                                Icon(Icons.Default.LinkOff, contentDescription = "Desvincular nota")
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNotePickerDialog = true }
                                .testTag("btn_pick_linked_note")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vincular una nota existente a este ${if (itemType == CalendarItemType.REMINDER) "recordatorio" else "evento"}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Elegir",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(colorHex)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        val finalSubtasks = subtasks.toList().let { list ->
                            val pending = newSubtaskTitle.trim()
                            if (pending.isNotEmpty()) list + ReminderSubtask(title = pending) else list
                        }
                        val base = existingItem ?: ReminderEntity(
                            title = "",
                            scheduledAt = scheduledAt
                        )
                        val entityToSave = base.copy(
                            itemType = itemType.code,
                            title = title.ifBlank {
                                if (itemType == CalendarItemType.EVENT) "Nuevo evento" else "Nuevo recordatorio"
                            },
                            notes = notes,
                            location = location,
                            scheduledAt = scheduledAt,
                            endTimeMillis = if (itemType == CalendarItemType.EVENT && !isAllDay) endTimeMillis else 0L,
                            isAllDay = isAllDay,
                            priority = priority,
                            listName = listName,
                            tags = tags,
                            colorHex = colorHex,
                            recurrenceRule = recurrenceRule.code,
                            subtasksJson = ReminderSubtaskSerializer.serialize(finalSubtasks),
                            linkedNoteId = selectedLinkedNote?.id,
                            linkedNoteUuid = selectedLinkedNote?.remoteUuid ?: "",
                            linkedNoteTitle = selectedLinkedNote?.title ?: "",
                            alertOffsetMinutes = alertOffsetMinutes,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(entityToSave)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("btn_save_calendar_item")
                ) {
                    Text(
                        text = if (existingItem == null) "Crear" else "Guardar cambios",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showNotePickerDialog) {
        val filteredNotes = remember(availableNotes, noteSearchQuery) {
            if (noteSearchQuery.isBlank()) availableNotes
            else availableNotes.filter {
                it.title.contains(noteSearchQuery, ignoreCase = true) ||
                    it.content.contains(noteSearchQuery, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showNotePickerDialog = false },
            title = { Text("Vincular una nota") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = noteSearchQuery,
                        onValueChange = { noteSearchQuery = it },
                        placeholder = { Text("Buscar nota por título...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (filteredNotes.isEmpty()) {
                        Text(
                            text = "No hay notas disponibles que coincidan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(filteredNotes, key = { it.id }) { note ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedLinkedNote?.id == note.id) {
                                        amber.copy(alpha = 0.18f)
                                    } else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLinkedNote = note
                                            showNotePickerDialog = false
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = amber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title.ifBlank { "Nota sin título" },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = note.folder,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotePickerDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
