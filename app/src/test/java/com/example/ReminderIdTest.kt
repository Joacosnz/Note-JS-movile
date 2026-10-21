package com.example

import com.example.util.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderIdTest {

    @Test
    fun testGetReminderRequestCodeNeverOverflows() {
        val testIds = listOf(
            0L,
            1L,
            42L,
            1729500000000L, // current timestamp
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            -123456789L
        )
        val testOffsets = listOf(0, 5, 15, 30, 60, 120, 720, 1440)

        for (id in testIds) {
            for (offset in testOffsets) {
                val code = ReminderScheduler.getReminderRequestCode(id, offset)
                // Must be >= 0 and within safe bounds (< 3.3 million)
                assertTrue("Code $code must be non-negative for id=$id and offset=$offset", code >= 0)
                assertTrue("Code $code must be within safe Int bounds (< 3,300,000)", code < 3_300_000)
            }
        }
    }

    @Test
    fun testDeterministicIdBetweenScheduleAndCancel() {
        val noteId = 1729500000000L
        val offset = 15

        val code1 = ReminderScheduler.getReminderRequestCode(noteId, offset)
        val code2 = ReminderScheduler.getReminderRequestCode(noteId, offset)

        assertEquals("Codes must match deterministically", code1, code2)
    }

    @Test
    fun testDistinctOffsetsForSameNote() {
        val noteId = 9876543210L
        val code0 = ReminderScheduler.getReminderRequestCode(noteId, 0)
        val code15 = ReminderScheduler.getReminderRequestCode(noteId, 15)

        assertTrue("Different offsets must yield distinct request codes", code0 != code15)
    }
}
