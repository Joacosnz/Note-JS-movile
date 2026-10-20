package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NoteBlockSerializer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ReminderOffsetOption(
    val minutes: Int,
    val label: String
)

val REMINDER_OFFSET_OPTIONS = listOf(
    ReminderOffsetOption(0, "Al momento"),
    ReminderOffsetOption(5, "5 min antes"),
    ReminderOffsetOption(15, "15 min antes"),
    ReminderOffsetOption(30, "30 min antes"),
    ReminderOffsetOption(60, "1 hora antes"),
    ReminderOffsetOption(120, "2 horas antes"),
    ReminderOffsetOption(1440, "1 día antes")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReminderBottomSheet(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSaveReminder: (timestamp: Long, updatedTitle: String, updatedContent: String, offsetsMinutes: List<Int>) -> Unit,
    onCancelReminder: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var titleText by remember(note) { mutableStateOf(note.title) }
    var contentText by remember(note) { mutableStateOf(getPlainTextFromNote(note.content)) }
    var selectedOffsets by remember { mutableStateOf(setOf(0, 15)) }

    val dayOptions = remember {
        List(60) { offset ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
        }
    }
    val hourOptions = remember { (0..23).toList() }
    val minuteOptions = remember { (0..59).toList() }

    val initialCal = remember(note.reminderTimestamp) {
        Calendar.getInstance().apply {
            if (note.reminderTimestamp != null && note.reminderTimestamp > System.currentTimeMillis()) {
                timeInMillis = note.reminderTimestamp
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    var selectedDayIndex by remember {
        val initialDay = dayOptions.indexOfFirst {
            it.get(Calendar.YEAR) == initialCal.get(Calendar.YEAR) &&
                    it.get(Calendar.DAY_OF_YEAR) == initialCal.get(Calendar.DAY_OF_YEAR)
        }
        mutableIntStateOf(if (initialDay >= 0) initialDay else 0)
    }

    var selectedHourIndex by remember {
        mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23))
    }

    var selectedMinuteIndex by remember {
        mutableIntStateOf(initialCal.get(Calendar.MINUTE).coerceIn(0, 59))
    }

    val dayPickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedDayIndex)
    val hourPickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedHourIndex)
    val minutePickerState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMinuteIndex)

    val daySnap = rememberSnapFlingBehavior(lazyListState = dayPickerState)
    val hourSnap = rememberSnapFlingBehavior(lazyListState = hourPickerState)
    val minuteSnap = rememberSnapFlingBehavior(lazyListState = minutePickerState)

    LaunchedEffect(dayPickerState) {
        snapshotFlow { dayPickerState.firstVisibleItemIndex }
            .collect { index ->
                if (index in dayOptions.indices && index != selectedDayIndex) {
                    selectedDayIndex = index
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    LaunchedEffect(hourPickerState) {
        snapshotFlow { hourPickerState.firstVisibleItemIndex }
            .collect { index ->
                if (index in hourOptions.indices && index != selectedHourIndex) {
                    selectedHourIndex = index
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    LaunchedEffect(minutePickerState) {
        snapshotFlow { minutePickerState.firstVisibleItemIndex }
            .collect { index ->
                if (index in minuteOptions.indices && index != selectedMinuteIndex) {
                    selectedMinuteIndex = index
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    }

    val selectedTimestamp = remember(selectedDayIndex, selectedHourIndex, selectedMinuteIndex) {
        Calendar.getInstance().apply {
            val base = dayOptions.getOrElse(selectedDayIndex) { Calendar.getInstance() }
            set(Calendar.YEAR, base.get(Calendar.YEAR))
            set(Calendar.DAY_OF_YEAR, base.get(Calendar.DAY_OF_YEAR))
            set(Calendar.HOUR_OF_DAY, hourOptions.getOrElse(selectedHourIndex) { 0 })
            set(Calendar.MINUTE, minuteOptions.getOrElse(selectedMinuteIndex) { 0 })
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val formattedSelectedDateTime = remember(selectedTimestamp) {
        val sdf = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale.forLanguageTag("es-ES"))
        val formatted = sdf.format(Date(selectedTimestamp))
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-ES")) else it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (note.reminderTimestamp != null) "Editar recordatorio" else "Programar recordatorio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Text Fields for Note Title & Content
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Título de la nota") },
                placeholder = { Text("Añadir título...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = amber,
                    focusedLabelColor = amber
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = contentText,
                onValueChange = { contentText = it },
                label = { Text("Contenido de la nota") },
                placeholder = { Text("Detalle o recordatorio...") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = amber,
                    focusedLabelColor = amber
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = amber.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, amber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = amber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedSelectedDateTime,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = amber
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedDayIndex = 0
                            selectedHourIndex = 18
                            selectedMinuteIndex = 0
                            coroutineScope.launch {
                                dayPickerState.animateScrollToItem(0)
                                hourPickerState.animateScrollToItem(18)
                                minutePickerState.animateScrollToItem(0)
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hoy 18:00",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val targetDay = if (dayOptions.size > 1) 1 else 0
                            selectedDayIndex = targetDay
                            selectedHourIndex = 9
                            selectedMinuteIndex = 0
                            coroutineScope.launch {
                                dayPickerState.animateScrollToItem(targetDay)
                                hourPickerState.animateScrollToItem(9)
                                minutePickerState.animateScrollToItem(0)
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mañana 09:00",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advance Notice / Offset Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Avisar antes",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(REMINDER_OFFSET_OPTIONS) { option ->
                        val isSelected = selectedOffsets.contains(option.minutes)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedOffsets = if (isSelected) {
                                    if (selectedOffsets.size > 1) selectedOffsets - option.minutes else selectedOffsets
                                } else {
                                    selectedOffsets + option.minutes
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            label = { Text(option.label, fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = amber,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DÍA",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(0.50f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "HORA",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(0.25f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "MIN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(0.25f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = amber.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, amber)
                ) {}

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyColumn(
                        state = dayPickerState,
                        flingBehavior = daySnap,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 50.dp),
                        modifier = Modifier
                            .weight(0.50f)
                            .fillMaxHeight()
                    ) {
                        itemsIndexed(dayOptions) { idx, dayCal ->
                            val isSelected = idx == selectedDayIndex
                            val dayText = remember(dayCal) {
                                val now = Calendar.getInstance()
                                if (dayCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                    dayCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                                ) {
                                    "Hoy"
                                } else {
                                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    if (dayCal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                                        dayCal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)
                                    ) {
                                        "Mañana"
                                    } else {
                                        val sdf = SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("es-ES"))
                                        sdf.format(dayCal.time)
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clickable {
                                        selectedDayIndex = idx
                                        coroutineScope.launch {
                                            dayPickerState.animateScrollToItem(idx)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isSelected) 16.sp else 14.sp
                                    ),
                                    color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(110.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    LazyColumn(
                        state = hourPickerState,
                        flingBehavior = hourSnap,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 50.dp),
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                    ) {
                        itemsIndexed(hourOptions) { idx, hour ->
                            val isSelected = idx == selectedHourIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clickable {
                                        selectedHourIndex = idx
                                        coroutineScope.launch {
                                            hourPickerState.animateScrollToItem(idx)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", hour),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isSelected) 18.sp else 15.sp
                                    ),
                                    color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = amber,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    LazyColumn(
                        state = minutePickerState,
                        flingBehavior = minuteSnap,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(vertical = 50.dp),
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight()
                    ) {
                        itemsIndexed(minuteOptions) { idx, minute ->
                            val isSelected = idx == selectedMinuteIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clickable {
                                        selectedMinuteIndex = idx
                                        coroutineScope.launch {
                                            minutePickerState.animateScrollToItem(idx)
                                        }
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", minute),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isSelected) 18.sp else 15.sp
                                    ),
                                    color = if (isSelected) amber else if (isDark) Color.White.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val updatedContent = updateNoteContentWithText(note.content, contentText)
                    onSaveReminder(selectedTimestamp, titleText, updatedContent, selectedOffsets.toList())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = amber,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Guardar recordatorio",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            if (note.reminderTimestamp != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCancelReminder()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Eliminar recordatorio",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getPlainTextFromNote(content: String): String {
    val blocks = NoteBlockSerializer.deserialize(content)
    if (blocks.isEmpty()) return content
    val textBlocks = blocks.filterIsInstance<NoteBlock.Text>()
    if (textBlocks.isNotEmpty()) {
        return textBlocks.joinToString("\n") { it.content }
    }
    return blocks.mapNotNull { block ->
        when (block) {
            is NoteBlock.Text -> block.content
            is NoteBlock.Checklist -> block.content
            is NoteBlock.SimpleList -> block.content
            is NoteBlock.CalloutBox -> block.content
            else -> null
        }
    }.filter { it.isNotBlank() }.joinToString("\n")
}

private fun updateNoteContentWithText(originalContent: String, newText: String): String {
    val blocks = NoteBlockSerializer.deserialize(originalContent).toMutableList()
    if (blocks.isEmpty()) {
        return NoteBlockSerializer.serialize(listOf(NoteBlock.Text(content = newText)))
    }

    val textIndex = blocks.indexOfFirst { it is NoteBlock.Text }
    if (textIndex >= 0) {
        val currentText = blocks[textIndex] as NoteBlock.Text
        blocks[textIndex] = currentText.copy(content = newText)
    } else {
        blocks.add(0, NoteBlock.Text(content = newText))
    }
    return NoteBlockSerializer.serialize(blocks)
}
