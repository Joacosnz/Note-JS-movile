package com.example

import com.example.widget.EXTRA_OPEN_CALENDAR_KEY
import com.example.widget.EXTRA_OPEN_REMINDER_ID_KEY
import com.example.widget.PARAM_REMINDER_ID
import com.example.widget.PARAM_REMINDER_UUID
import com.example.widget.WidgetCalendarItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRemindersWidgetTest {

    @Test
    fun testActionParameterKeys() {
        assertEquals("EXTRA_OPEN_CALENDAR", EXTRA_OPEN_CALENDAR_KEY.name)
        assertEquals("EXTRA_OPEN_REMINDER_ID", EXTRA_OPEN_REMINDER_ID_KEY.name)
        assertEquals("PARAM_REMINDER_ID", PARAM_REMINDER_ID.name)
        assertEquals("PARAM_REMINDER_UUID", PARAM_REMINDER_UUID.name)
    }

    @Test
    fun testWidgetCalendarItemSortingAndPriorities() {
        val now = System.currentTimeMillis()

        val pendingReminder = WidgetCalendarItem(
            id = 1L,
            remoteUuid = "uuid-1",
            title = "Pagar servicios",
            scheduledAt = now + 3600_000L,
            isCompleted = false,
            isEvent = false,
            priority = 2
        )

        val completedReminder = WidgetCalendarItem(
            id = 2L,
            remoteUuid = "uuid-2",
            title = "Comprar café",
            scheduledAt = now - 1800_000L,
            isCompleted = true,
            isEvent = false,
            priority = 1
        )

        val calendarEvent = WidgetCalendarItem(
            id = 3L,
            remoteUuid = "uuid-3",
            title = "Reunión de sprint",
            scheduledAt = now + 7200_000L,
            endTimeMillis = now + 10800_000L,
            isCompleted = false,
            isEvent = true,
            priority = 0
        )

        val items = listOf(completedReminder, calendarEvent, pendingReminder)

        val sorted = items.sortedWith(
            compareBy<WidgetCalendarItem> { item ->
                if (item.isCompleted) 1 else 0
            }.thenBy { item ->
                item.scheduledAt
            }
        )

        // Pending items must be before completed items
        assertFalse(sorted[0].isCompleted)
        assertFalse(sorted[1].isCompleted)
        assertTrue(sorted[2].isCompleted)
        assertEquals(1L, sorted[0].id)
        assertEquals(3L, sorted[1].id)
        assertEquals(2L, sorted[2].id)
    }

    @Test
    fun testEventDurationCalculation() {
        val start = System.currentTimeMillis()
        val end = start + 5400_000L // 1.5 hours
        val event = WidgetCalendarItem(
            id = 10L,
            remoteUuid = "ev-10",
            title = "Consulta médica",
            scheduledAt = start,
            endTimeMillis = end,
            isEvent = true
        )

        assertTrue(event.isEvent)
        assertTrue(event.endTimeMillis > event.scheduledAt)
        assertEquals(5400_000L, event.endTimeMillis - event.scheduledAt)
    }
}
