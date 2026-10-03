package com.example

import com.example.model.NoteBlock
import com.example.model.TextStyleType
import com.example.util.NoteBlockSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollapsibleSubtitleTest {

    @Test
    fun testSerializationAndDeserializationWithIsCollapsed() {
        val blocks = listOf(
            NoteBlock.Text(id = "1", content = "Título Principal", style = TextStyleType.TITLE),
            NoteBlock.Text(id = "2", content = "Subtítulo 1", style = TextStyleType.SUBTITLE, isCollapsed = true),
            NoteBlock.Text(id = "3", content = "Texto dentro de Subtítulo 1", style = TextStyleType.BODY),
            NoteBlock.Text(id = "4", content = "Subtítulo 2", style = TextStyleType.SUBTITLE, isCollapsed = false),
            NoteBlock.Text(id = "5", content = "Texto dentro de Subtítulo 2", style = TextStyleType.BODY)
        )

        val serialized = NoteBlockSerializer.serialize(blocks)
        val deserialized = NoteBlockSerializer.deserialize(serialized)

        assertEquals(5, deserialized.size)

        val sub1 = deserialized[1] as NoteBlock.Text
        assertEquals("Subtítulo 1", sub1.content)
        assertEquals(TextStyleType.SUBTITLE, sub1.style)
        assertTrue(sub1.isCollapsed)

        val sub2 = deserialized[3] as NoteBlock.Text
        assertEquals("Subtítulo 2", sub2.content)
        assertEquals(TextStyleType.SUBTITLE, sub2.style)
        assertFalse(sub2.isCollapsed)
    }

    @Test
    fun testCollapsingSectionLogic() {
        val blocks = listOf(
            NoteBlock.Text(id = "1", content = "Título", style = TextStyleType.TITLE),
            NoteBlock.Text(id = "2", content = "Subtítulo Colapsado", style = TextStyleType.SUBTITLE, isCollapsed = true),
            NoteBlock.Text(id = "3", content = "Texto oculto 1", style = TextStyleType.BODY),
            NoteBlock.Checklist(id = "4", content = "Tarea oculta", isChecked = false),
            NoteBlock.Text(id = "5", content = "Subtítulo Visible", style = TextStyleType.SUBTITLE, isCollapsed = false),
            NoteBlock.Text(id = "6", content = "Texto visible", style = TextStyleType.BODY)
        )

        val hiddenBlockIds = mutableSetOf<String>()
        var isCollapsing = false
        for (b in blocks) {
            if (b is NoteBlock.Text) {
                if (b.style == TextStyleType.TITLE) {
                    isCollapsing = false
                } else if (b.style == TextStyleType.SUBTITLE) {
                    isCollapsing = b.isCollapsed
                    continue
                }
            }
            if (isCollapsing) {
                hiddenBlockIds.add(b.id)
            }
        }

        assertTrue(hiddenBlockIds.contains("3"))
        assertTrue(hiddenBlockIds.contains("4"))
        assertFalse(hiddenBlockIds.contains("1"))
        assertFalse(hiddenBlockIds.contains("2"))
        assertFalse(hiddenBlockIds.contains("5"))
        assertFalse(hiddenBlockIds.contains("6"))

        val visibleBlocks = blocks.filter { it.id !in hiddenBlockIds }
        assertEquals(4, visibleBlocks.size)
        assertEquals(listOf("1", "2", "5", "6"), visibleBlocks.map { it.id })
    }

    @Test
    fun testNormalizeHeadingBlocks() {
        val rawBlocks = listOf(
            NoteBlock.Text(id = "1", content = "Texto\n#Pruebas\nMaterias\nalgebra\ningles", style = TextStyleType.SUBTITLE)
        )

        val normalized = mutableListOf<NoteBlock>()
        for (block in rawBlocks) {
            if (block is NoteBlock.Text && (block.style == TextStyleType.SUBTITLE || block.style == TextStyleType.TITLE) && block.content.contains('\n')) {
                val lines = block.content.split('\n')
                val headingText = lines.firstOrNull() ?: ""
                val remainingText = lines.drop(1).joinToString("\n")
                normalized.add(block.copy(content = headingText))
                if (remainingText.isNotEmpty()) {
                    normalized.add(NoteBlock.Text(content = remainingText, style = TextStyleType.BODY))
                }
            } else {
                normalized.add(block)
            }
        }

        assertEquals(2, normalized.size)
        val heading = normalized[0] as NoteBlock.Text
        assertEquals("Texto", heading.content)
        assertEquals(TextStyleType.SUBTITLE, heading.style)

        val body = normalized[1] as NoteBlock.Text
        assertEquals("#Pruebas\nMaterias\nalgebra\ningles", body.content)
        assertEquals(TextStyleType.BODY, body.style)
    }

    @Test
    fun testExitingH2SectionWithIsIndented() {
        val blocks = listOf(
            NoteBlock.Text(id = "1", content = "hola", style = TextStyleType.SUBTITLE, isCollapsed = true),
            NoteBlock.Text(id = "2", content = "logica", style = TextStyleType.BODY, isIndented = true),
            NoteBlock.Text(id = "3", content = "Texto fuera de H2", style = TextStyleType.BODY, isIndented = false)
        )

        val hiddenBlockIds = mutableSetOf<String>()
        var isCollapsing = false
        for (b in blocks) {
            if (b is NoteBlock.Text) {
                if (b.style == TextStyleType.TITLE) {
                    isCollapsing = false
                } else if (b.style == TextStyleType.SUBTITLE) {
                    isCollapsing = b.isCollapsed
                    continue
                } else if (b.style == TextStyleType.BODY && !b.isIndented) {
                    isCollapsing = false
                }
            }
            if (isCollapsing) {
                hiddenBlockIds.add(b.id)
            }
        }

        assertTrue(hiddenBlockIds.contains("2"))
        assertFalse(hiddenBlockIds.contains("1"))
        assertFalse(hiddenBlockIds.contains("3"))

        val visibleBlocks = blocks.filter { it.id !in hiddenBlockIds }
        assertEquals(2, visibleBlocks.size)
        assertEquals(listOf("1", "3"), visibleBlocks.map { it.id })
    }
}
