package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MathAutoSolveTest {

    @Test
    fun testSimpleAddition() {
        val input = "5 + 5="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("5 + 5 = 10", res?.newText)
        assertEquals(10, res?.newCursorPosition)
    }

    @Test
    fun testAdditionWithSpaces() {
        val input = "12 + 8 ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("12 + 8 = 20", res?.newText)
        assertEquals(11, res?.newCursorPosition)
    }

    @Test
    fun testSimpleSubtraction() {
        val input = "100 - 35 ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("100 - 35 = 65", res?.newText)
        assertEquals(13, res?.newCursorPosition)
    }

    @Test
    fun testMultiplicationAsterisk() {
        val input = "12 * 4 ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("12 * 4 = 48", res?.newText)
        assertEquals(11, res?.newCursorPosition)
    }

    @Test
    fun testMultiplicationX() {
        val input1 = "6 x 7 ="
        val res1 = MathEvaluator.trySolveInlineEquation(input1, input1.length)
        assertNotNull(res1)
        assertEquals("6 x 7 = 42", res1?.newText)

        val input2 = "8 × 5 ="
        val res2 = MathEvaluator.trySolveInlineEquation(input2, input2.length)
        assertNotNull(res2)
        assertEquals("8 × 5 = 40", res2?.newText)
    }

    @Test
    fun testSimpleDivision() {
        val input1 = "100 / 4 ="
        val res1 = MathEvaluator.trySolveInlineEquation(input1, input1.length)
        assertNotNull(res1)
        assertEquals("100 / 4 = 25", res1?.newText)

        val input2 = "100 ÷ 5 ="
        val res2 = MathEvaluator.trySolveInlineEquation(input2, input2.length)
        assertNotNull(res2)
        assertEquals("100 ÷ 5 = 20", res2?.newText)
    }

    @Test
    fun testDecimalsDotAndComma() {
        val input1 = "10.5 + 2.5 ="
        val res1 = MathEvaluator.trySolveInlineEquation(input1, input1.length)
        assertNotNull(res1)
        assertEquals("10.5 + 2.5 = 13", res1?.newText)

        val input2 = "10,5 + 2,5 ="
        val res2 = MathEvaluator.trySolveInlineEquation(input2, input2.length)
        assertNotNull(res2)
        assertEquals("10,5 + 2,5 = 13", res2?.newText)
    }

    @Test
    fun testPrecedingTextOnSameLine() {
        val input = "Total compra: 15 + 25 ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("Total compra: 15 + 25 = 40", res?.newText)
        assertEquals("Total compra: 15 + 25 = 40".length, res?.newCursorPosition)
    }

    @Test
    fun testMultilineText() {
        val input = "Primera línea de notas\nPresupuesto: 50 * 3 ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("Primera línea de notas\nPresupuesto: 50 * 3 = 150", res?.newText)
    }

    @Test
    fun testParenthesesAndOrderOfOperations() {
        val input1 = "(10 + 5) * 2 ="
        val res1 = MathEvaluator.trySolveInlineEquation(input1, input1.length)
        assertNotNull(res1)
        assertEquals("(10 + 5) * 2 = 30", res1?.newText)

        val input2 = "10 + 5 * 2 ="
        val res2 = MathEvaluator.trySolveInlineEquation(input2, input2.length)
        assertNotNull(res2)
        assertEquals("10 + 5 * 2 = 20", res2?.newText)
    }

    @Test
    fun testPercentages() {
        val input = "200 * 15% ="
        val res = MathEvaluator.trySolveInlineEquation(input, input.length)
        assertNotNull(res)
        assertEquals("200 * 15% = 30", res?.newText)
    }

    @Test
    fun testTextFieldValueAutoSolveTrigger() {
        val oldTfv = TextFieldValue("5 + 5 ")
        val newTfv = TextFieldValue("5 + 5 =", TextRange(7))

        val solved = MathEvaluator.tryAutoSolveEquationOnEqual(newTfv, oldTfv)
        assertNotNull(solved)
        assertEquals("5 + 5 = 10", solved?.text)
        assertEquals(10, solved?.selection?.end)
    }

    @Test
    fun testDoNotTriggerOnBackspaceOrDeletion() {
        val oldTfv = TextFieldValue("5 + 5 = 10")
        // User pressed backspace, leaving "5 + 5 ="
        val backspacedTfv = TextFieldValue("5 + 5 =", TextRange(7))

        val solved = MathEvaluator.tryAutoSolveEquationOnEqual(backspacedTfv, oldTfv)
        assertNull(solved)
    }

    @Test
    fun testDoNotTriggerOnVariableAssignmentOrSingleNumber() {
        val input1 = "x ="
        assertNull(MathEvaluator.trySolveInlineEquation(input1, input1.length))

        val input2 = "Total ="
        assertNull(MathEvaluator.trySolveInlineEquation(input2, input2.length))

        val input3 = "123 ="
        assertNull(MathEvaluator.trySolveInlineEquation(input3, input3.length))
    }

    @Test
    fun testDoNotTriggerOnDoubleEqualsOrComparison() {
        val input1 = "5 + 5 =="
        assertNull(MathEvaluator.trySolveInlineEquation(input1, input1.length))

        val input2 = "a != b="
        assertNull(MathEvaluator.trySolveInlineEquation(input2, input2.length))
    }

    @Test
    fun testDoNotTriggerOnDivisionByZero() {
        val input = "10 / 0 ="
        assertNull(MathEvaluator.trySolveInlineEquation(input, input.length))
    }
}
