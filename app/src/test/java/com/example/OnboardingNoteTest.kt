package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.NoteBlock
import com.example.model.TextStyleType
import com.example.util.AppPreferences
import com.example.util.NoteBlockSerializer
import com.example.util.OnboardingNoteHelper
import kotlinx.coroutines.runBlocking
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
        assertEquals("¡Bienvenido a Note JS! 🚀", note.title)
        assertTrue(note.isPinned)
        assertEquals("👋", note.icon)
    }

    @Test
    fun testWelcomeNoteBlocksStructureAndSerialization() {
        val note = OnboardingNoteHelper.createWelcomeNote()
        val blocks = NoteBlockSerializer.deserialize(note.content)

        assertTrue("Blocks should not be empty", blocks.isNotEmpty())

        val callouts = blocks.filterIsInstance<NoteBlock.CalloutBox>()
        assertTrue("Should contain at least one CalloutBox", callouts.isNotEmpty())
        assertEquals("💡", callouts.first().emoji)
        
        val subtitles = blocks.filterIsInstance<NoteBlock.Text>().filter { it.style == TextStyleType.SUBTITLE }
        assertEquals(4, subtitles.size)

        val checklists = blocks.filterIsInstance<NoteBlock.Checklist>()
        assertEquals(2, checklists.size)

        val mathBlocks = blocks.filterIsInstance<NoteBlock.Math>()
        assertEquals(1, mathBlocks.size)
    }

    @Test
    fun testAppPreferencesFirstLaunchFlag() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Default should be true
        assertTrue(AppPreferences.getIsFirstLaunch(context))

        // Mark completed
        AppPreferences.setFirstLaunchCompleted(context)
        assertFalse(AppPreferences.getIsFirstLaunch(context))
    }
}
