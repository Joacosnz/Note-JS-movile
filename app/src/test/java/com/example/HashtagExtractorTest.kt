package com.example

import com.example.util.HashtagExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HashtagExtractorTest {

    @Test
    fun `extractHashtags detects Spanish hashtags with accents and special characters`() {
        val text = "Nota de #trabajo sobre #diseño y #reunión_2026 con música #español"
        val tags = HashtagExtractor.extractHashtags(text)

        assertEquals(setOf("trabajo", "diseño", "reunión_2026", "español"), tags)
    }

    @Test
    fun `extractHashtags handles multiple text inputs and nulls`() {
        val title = "Reunión #urgente"
        val content = "Revisar proyecto #trabajo_2026 y #diseño"
        val tags = HashtagExtractor.extractHashtags(title, null, content, "")

        assertEquals(setOf("urgente", "trabajo_2026", "diseño"), tags)
    }

    @Test
    fun `combineTags merges manual tags and extracted hashtags without duplicates`() {
        val manual = "  #ideas , trabajo , #diseño  "
        val content = "Inspiración para #diseño y #proyecto"

        val result = HashtagExtractor.combineTags(manual, content)

        assertEquals("ideas,trabajo,diseño,proyecto", result)
    }

    @Test
    fun `noteMatchesTag matches target tag correctly`() {
        val noteTags = "trabajo,diseño"
        val title = "Nota de #reunión_2026"
        val content = "Detalles importantes"

        assertTrue(HashtagExtractor.noteMatchesTag(noteTags, title, content, "Todas"))
        assertTrue(HashtagExtractor.noteMatchesTag(noteTags, title, content, "#trabajo"))
        assertTrue(HashtagExtractor.noteMatchesTag(noteTags, title, content, "DISEÑO"))
        assertTrue(HashtagExtractor.noteMatchesTag(noteTags, title, content, "reunión_2026"))
        assertFalse(HashtagExtractor.noteMatchesTag(noteTags, title, content, "personal"))
    }
}
