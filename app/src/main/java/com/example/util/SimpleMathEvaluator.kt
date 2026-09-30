package com.example.util

object SimpleMathEvaluator {
    // Evalúa expresiones básicas como "1500 * 3 + 200" o "(50 + 10) / 2"
    fun evaluate(expression: String): Double? {
        return try {
            val clean = expression.replace(" ", "")
            parseExpression(clean, intArrayOf(0))
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpression(expr: String, pos: IntArray): Double {
        var x = parseTerm(expr, pos)
        while (pos[0] < expr.length) {
            val op = expr[pos[0]]
            if (op != '+' && op != '-') break
            pos[0]++
            val y = parseTerm(expr, pos)
            if (op == '+') x += y else x -= y
        }
        return x
    }

    private fun parseTerm(expr: String, pos: IntArray): Double {
        var x = parseFactor(expr, pos)
        while (pos[0] < expr.length) {
            val op = expr[pos[0]]
            if (op != '*' && op != '/') break
            pos[0]++
            val y = parseFactor(expr, pos)
            if (op == '*') x *= y else x /= y
        }
        return x
    }

    private fun parseFactor(expr: String, pos: IntArray): Double {
        if (pos[0] < expr.length && expr[pos[0]] == '(') {
            pos[0]++
            val result = parseExpression(expr, pos)
            if (pos[0] < expr.length && expr[pos[0]] == ')') pos[0]++
            return result
        }
        val start = pos[0]
        while (pos[0] < expr.length && (expr[pos[0]].isDigit() || expr[pos[0]] == '.')) {
            pos[0]++
        }
        return expr.substring(start, pos[0]).toDouble()
    }
}
