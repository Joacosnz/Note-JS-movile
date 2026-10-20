package com.example.util

@Deprecated("Usa MathEvaluator en su lugar.", ReplaceWith("MathEvaluator.evaluate(expression)?.toDoubleOrNull()"))
object SimpleMathEvaluator {
    /**
     * Delegado de compatibilidad hacia MathEvaluator para mantener un único motor de evaluación matemática.
     */
    fun evaluate(expression: String): Double? {
        return MathEvaluator.evaluate(expression)?.toDoubleOrNull()
    }
}
