package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLinearEquationSolving() {
    val res1 = com.example.util.MathEvaluator.solveLinearEquation("2x + 5 = 15")
    assertEquals("x = 5", res1)

    val res2 = com.example.util.MathEvaluator.solveLinearEquation("3x - 9 = 0")
    assertEquals("x = 3", res2)

    val res3 = com.example.util.MathEvaluator.solveLinearEquation("x + 4 = 10")
    assertEquals("x = 6", res3)

    val res4 = com.example.util.MathEvaluator.solveLinearEquation("2x + 4 = x + 10")
    assertEquals("x = 6", res4)

    val res5 = com.example.util.MathEvaluator.evaluate("5x = 20")
    assertEquals("x = 4", res5)
  }
}
