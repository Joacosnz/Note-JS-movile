package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.NoteBlock
import com.example.model.TextStyleType
import com.example.util.NoteBlockSerializer
import com.example.util.OnboardingNoteHelper
import com.example.util.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OnboardingNoteTest {

    @Test
    fun testWelcomeNoteCreationAndProperties() {
        val note = OnboardingNoteHelper.createWelcomeNote()

        assertNotNull(note)
        assertTrue(note.title.contains("Bienvenido a Note JS"))
        assertTrue(note.isPinned)
        assertEquals(1, note.colorIndex)
        assertEquals("Notas", note.folder)
        assertTrue(note.tags.contains("Bienvenida"))
        assertTrue(note.tags.contains("Guía"))
        assertTrue(note.tags.contains("NoteJS"))
        assertFalse(note.isArchived)
        assertFalse(note.isTrashed)
        assertFalse(note.isLocked)
    }

    @Test
    fun testWelcomeNoteBlocksStructureAndSerialization() {
        val note = OnboardingNoteHelper.createWelcomeNote()
        val blocks = NoteBlockSerializer.deserialize(note.content)

        assertTrue("Blocks should not be empty", blocks.isNotEmpty())

        // Check for presence of CalloutBox
        val callouts = blocks.filterIsInstance<NoteBlock.CalloutBox>()
        assertTrue("Should contain at least one CalloutBox", callouts.isNotEmpty())

        // Check for Title text block
        val titleBlock = blocks.filterIsInstance<NoteBlock.Text>().firstOrNull { it.style == TextStyleType.TITLE }
        assertNotNull("Should contain a TITLE text block", titleBlock)
        assertTrue(titleBlock!!.content.contains("Dossier Completo de Capacidades"))

        // Check for collapsible H2 Subtitles
        val subtitles = blocks.filterIsInstance<NoteBlock.Text>().filter { it.style == TextStyleType.SUBTITLE }
        assertTrue("Should contain multiple H2 subtitles", subtitles.size >= 5)

        // Check for interactive Checklists
        val checklists = blocks.filterIsInstance<NoteBlock.Checklist>()
        assertTrue("Should contain multiple checklist items", checklists.size >= 10)

        // Check for Technical Table
        val tables = blocks.filterIsInstance<NoteBlock.Table>()
        assertEquals("Should contain 1 summary table", 1, tables.size)
        val table = tables.first()
        assertEquals(6, table.rows)
        assertEquals(3, table.cols)
        assertTrue(table.hasHeaderRow)
        assertEquals("Componente", table.matrix[0][0].text)
        assertEquals("Tecnología / Motor", table.matrix[0][1].text)
        assertEquals("Estado", table.matrix[0][2].text)
    }

    @Test
    fun testSettingsManagerFirstRunFlag() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settingsManager = SettingsManager(context)

        // Reset or test default
        settingsManager.setFirstRunNoteGenerated(false)
        assertFalse(settingsManager.isFirstRunNoteGenerated())

        // Mark generated
        settingsManager.setFirstRunNoteGenerated(true)
        assertTrue(settingsManager.isFirstRunNoteGenerated())
    }
}
