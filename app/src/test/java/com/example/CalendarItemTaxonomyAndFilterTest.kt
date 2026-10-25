package com.example

import com.example.data.local.entity.CalendarItemType
import com.example.data.local.entity.ReminderEntity
import com.example.ui.screens.MonthZoomLevel
import com.example.ui.screens.filterItemsForDay
import com.example.ui.screens.getDayBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarItemTaxonomyAndFilterTest {

    @Test
    fun testMonthZoomLevelMapping() {
        assertEquals(MonthZoomLevel.COMPACTO, MonthZoomLevel.fromFloat(1.0f))
        assertEquals(MonthZoomLevel.COMPACTO, MonthZoomLevel.fromFloat(1.24f))
        assertEquals(MonthZoomLevel.APILADO, MonthZoomLevel.fromFloat(1.30f))
        assertEquals(MonthZoomLevel.APILADO, MonthZoomLevel.fromFloat(1.80f))
        assertEquals(MonthZoomLevel.DETALLES, MonthZoomLevel.fromFloat(2.0f))
        assertEquals(MonthZoomLevel.DETALLES, MonthZoomLevel.fromFloat(2.50f))
        assertEquals(MonthZoomLevel.EXPANDIDO, MonthZoomLevel.fromFloat(2.70f))
        assertEquals(MonthZoomLevel.EXPANDIDO, MonthZoomLevel.fromFloat(3.2f))
    }

    @Test
    fun testFilterItemsForDayDistinguishesEventsAndReminders() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayMillis = cal.timeInMillis

        val reminder = ReminderEntity(
            id = 1L,
            title = "Comprar insumos",
            scheduledAt = dayMillis,
            itemType = CalendarItemType.REMINDER.code,
            isCompleted = false,
            priority = 2
        )

        val calendarEvent = ReminderEntity(
            id = 2L,
            title = "Reunión de equipo",
            scheduledAt = dayMillis + 3600_000L,
            endTimeMillis = dayMillis + 7200_000L,
            itemType = CalendarItemType.EVENT.code,
            isCompleted = false,
            listName = "Trabajo"
        )

        val items = listOf(reminder, calendarEvent)

        // Both included
        val filteredAll = filterItemsForDay(
            items = items,
            dayStartMillis = dayMillis,
            includeCompleted = true,
            includeEvents = true,
            selectedCategory = null
        )
        assertEquals(2, filteredAll.size)

        // Reminders only
        val filteredRemindersOnly = filterItemsForDay(
            items = items,
            dayStartMillis = dayMillis,
            includeCompleted = true,
            includeEvents = false,
            selectedCategory = null
        )
        assertEquals(1, filteredRemindersOnly.size)
        assertEquals("Comprar insumos", filteredRemindersOnly.first().title)
        assertTrue(filteredRemindersOnly.first().isReminder)

        // Category filter
        val filteredCategory = filterItemsForDay(
            items = items,
            dayStartMillis = dayMillis,
            includeCompleted = true,
            includeEvents = true,
            selectedCategory = "Trabajo"
        )
        assertEquals(1, filteredCategory.size)
        assertEquals("Reunión de equipo", filteredCategory.first().title)
        assertTrue(filteredCategory.first().isEvent)
    }

    @Test
    fun testDayBoundsCalculation() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 24, 15, 30, 45)
        }
        val (start, end) = getDayBounds(cal.timeInMillis)

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))

        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
    }
}
