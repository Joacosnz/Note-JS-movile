import com.example.model.NoteBlock
import com.example.util.NoteBlockSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TableBlockTest {

    @Test
    fun testTableInitialization() {
        val table = NoteBlock.Table(rows = 3, cols = 4)
        assertEquals(3, table.rows)
        assertEquals(4, table.cols)
        assertEquals(3, table.matrix.size)
        assertEquals(4, table.matrix[0].size)
        assertFalse(table.hasHeaderRow)
    }

    @Test
    fun testCellTextUpdate() {
        var table = NoteBlock.Table(rows = 2, cols = 2)
        table = table.updateCellText(0, 1, "Hola")
        assertEquals("Hola", table.matrix[0][1].text)
        assertEquals("", table.matrix[0][0].text)
    }

    @Test
    fun testAddAndDeleteRow() {
        var table = NoteBlock.Table(rows = 2, cols = 2)
        table = table.updateCellText(0, 0, "Fila 0")
        table = table.updateCellText(1, 0, "Fila 1")

        // Add row below row 0 (so index 1 becomes new row)
        table = table.addRowBelow(0)
        assertEquals(3, table.rows)
        assertEquals("Fila 0", table.matrix[0][0].text)
        assertEquals("", table.matrix[1][0].text)
        assertEquals("Fila 1", table.matrix[2][0].text)

        // Delete row at index 1
        table = table.deleteRow(1)
        assertEquals(2, table.rows)
        assertEquals("Fila 0", table.matrix[0][0].text)
        assertEquals("Fila 1", table.matrix[1][0].text)
    }

    @Test
    fun testAddAndDeleteColumn() {
        var table = NoteBlock.Table(rows = 2, cols = 2)
        table = table.updateCellText(0, 0, "C0")
        table = table.updateCellText(0, 1, "C1")

        // Add column right of index 0
        table = table.addColumnRight(0)
        assertEquals(3, table.cols)
        assertEquals("C0", table.matrix[0][0].text)
        assertEquals("", table.matrix[0][1].text)
        assertEquals("C1", table.matrix[0][2].text)

        // Delete new column at index 1
        table = table.deleteColumn(1)
        assertEquals(2, table.cols)
        assertEquals("C0", table.matrix[0][0].text)
        assertEquals("C1", table.matrix[0][1].text)
    }

    @Test
    fun testApplyColors() {
        var table = NoteBlock.Table(rows = 2, cols = 2)

        // Apply color to single cell
        table = table.applyColor(0, 0, "#EF4444", NoteBlock.TableColorScope.CELL)
        assertEquals("#EF4444", table.matrix[0][0].backgroundColorHex)
        assertNull(table.matrix[0][1].backgroundColorHex)

        // Apply color to row 1
        table = table.applyColor(1, 0, "#3B82F6", NoteBlock.TableColorScope.ROW)
        assertEquals("#3B82F6", table.matrix[1][0].backgroundColorHex)
        assertEquals("#3B82F6", table.matrix[1][1].backgroundColorHex)

        // Apply color to column 1
        table = table.applyColor(0, 1, "#10B981", NoteBlock.TableColorScope.COLUMN)
        assertEquals("#10B981", table.matrix[0][1].backgroundColorHex)
        assertEquals("#10B981", table.matrix[1][1].backgroundColorHex)
    }

    @Test
    fun testApplyColorToMultipleCellsAndClear() {
        var table = NoteBlock.Table(rows = 2, cols = 2)
        val selected = setOf(Pair(0, 0), Pair(1, 1))

        table = table.applyColorToCells(selected, "#EF4444")
        assertEquals("#EF4444", table.matrix[0][0].backgroundColorHex)
        assertEquals("#EF4444", table.matrix[1][1].backgroundColorHex)
        assertNull(table.matrix[0][1].backgroundColorHex)

        table = table.updateCellText(0, 0, "A")
        table = table.updateCellText(1, 1, "B")
        table = table.clearCells(selected)
        assertEquals("", table.matrix[0][0].text)
        assertEquals("", table.matrix[1][1].text)
    }

    @Test
    fun testToggleHeaderRowAndSerialization() {
        var table = NoteBlock.Table(rows = 2, cols = 2)
        assertFalse(table.hasHeaderRow)

        table = table.toggleHeaderRow()
        assertTrue(table.hasHeaderRow)

        val json = NoteBlockSerializer.serialize(listOf(table))
        val deserialized = NoteBlockSerializer.deserialize(json)

        assertEquals(1, deserialized.size)
        val deserializedTable = deserialized[0] as NoteBlock.Table
        assertTrue(deserializedTable.hasHeaderRow)
        assertEquals(2, deserializedTable.rows)
        assertEquals(2, deserializedTable.cols)
    }
}
