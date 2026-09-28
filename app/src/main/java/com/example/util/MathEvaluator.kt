package com.example.util

import java.text.DecimalFormat
import java.util.Stack
import kotlin.math.pow
import kotlin.math.sqrt

object MathEvaluator {

    private val df = DecimalFormat("#.####")

    /**
     * Evaluates a mathematical expression string, such as:
     * "45 * 12", "150 + 350", "1450 * 0.21", "sqrt(144) + 6^2", "15% * 200"
     */
    fun evaluate(expression: String): String? {
        try {
            var expr = expression.trim()
            if (expr.endsWith("=")) {
                expr = expr.dropLast(1).trim()
            }
            if (expr.isEmpty()) return null

            // Handle percentages like "15% de 200" -> "0.15 * 200"
            expr = expr.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*%\\s*de\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)) {
                val p = it.groupValues[1].toDoubleOrNull() ?: 0.0
                val v = it.groupValues[2].toDoubleOrNull() ?: 0.0
                ((p / 100.0) * v).toString()
            }

            // Replace single percentages: "20%" -> "0.20"
            expr = expr.replace(Regex("(\\d+(?:\\.\\d+)?)\\s*%")) {
                val p = it.groupValues[1].toDoubleOrNull() ?: 0.0
                (p / 100.0).toString()
            }

            // Replace sqrt(x)
            expr = expr.replace(Regex("sqrt\\s*\\(\\s*(\\d+(?:\\.\\d+)?)\\s*\\)", RegexOption.IGNORE_CASE)) {
                val v = it.groupValues[1].toDoubleOrNull() ?: 0.0
                sqrt(v).toString()
            }

            val result = evalInfix(expr)
            return if (result.isNaN() || result.isInfinite()) null else df.format(result)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Scans note content and finds all expressions ending with '='
     * that haven't been completed yet, or computes live statistics.
     */
    fun processMathInText(text: String): String {
        val lines = text.lines()
        val processed = lines.map { line ->
            val trimmed = line.trim()
            if (trimmed.endsWith("=") && !trimmed.contains("==")) {
                val candidateExpr = trimmed.substringBeforeLast("=").trim()
                val mathResult = evaluate(candidateExpr)
                if (mathResult != null) {
                    "$trimmed $mathResult"
                } else {
                    line
                }
            } else {
                line
            }
        }
        return processed.joinToString("\n")
    }

    private fun evalInfix(expr: String): Double {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return 0.0

        val values = Stack<Double>()
        val ops = Stack<Char>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.toDoubleOrNull() != null -> {
                    values.push(token.toDouble())
                }
                token == "(" -> {
                    ops.push('(')
                }
                token == ")" -> {
                    while (ops.isNotEmpty() && ops.peek() != '(') {
                        applyOp(ops.pop(), values)
                    }
                    if (ops.isNotEmpty()) ops.pop()
                }
                isOp(token[0]) -> {
                    val op = token[0]
                    while (ops.isNotEmpty() && precedence(ops.peek()) >= precedence(op)) {
                        applyOp(ops.pop(), values)
                    }
                    ops.push(op)
                }
            }
            i++
        }

        while (ops.isNotEmpty()) {
            applyOp(ops.pop(), values)
        }

        return if (values.isNotEmpty()) values.pop() else 0.0
    }

    private fun isOp(c: Char): Boolean = c == '+' || c == '-' || c == '*' || c == '/' || c == '^'

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> -1
    }

    private fun applyOp(op: Char, values: Stack<Double>) {
        if (values.size < 2) return
        val b = values.pop()
        val a = values.pop()
        val res = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> if (b != 0.0) a / b else Double.NaN
            '^' -> a.pow(b)
            else -> 0.0
        }
        values.push(res)
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var buffer = StringBuilder()
        var idx = 0

        while (idx < expr.length) {
            val c = expr[idx]
            when {
                c.isWhitespace() -> {
                    if (buffer.isNotEmpty()) {
                        tokens.add(buffer.toString())
                        buffer = StringBuilder()
                    }
                }
                c.isDigit() || c == '.' -> {
                    buffer.append(c)
                }
                c == '(' || c == ')' || isOp(c) -> {
                    if (buffer.isNotEmpty()) {
                        tokens.add(buffer.toString())
                        buffer = StringBuilder()
                    }
                    // Handle unary minus: if '-' is preceded by '(' or start of tokens
                    if (c == '-' && (tokens.isEmpty() || tokens.last() == "(" || isOp(tokens.last().first()))) {
                        buffer.append('-')
                    } else {
                        tokens.add(c.toString())
                    }
                }
            }
            idx++
        }
        if (buffer.isNotEmpty()) {
            tokens.add(buffer.toString())
        }
        return tokens
    }
}
