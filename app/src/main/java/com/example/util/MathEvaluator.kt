package com.example.util

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.text.DecimalFormat
import java.util.Stack
import kotlin.math.pow
import kotlin.math.sqrt

object MathEvaluator {

    private val df = DecimalFormat("#.####")

    /**
     * Evaluates a mathematical expression string, such as:
     * "45 * 12", "150 + 350", "1450 * 0.21", "sqrt(144) + 6^2", "15% * 200",
     * or solves a linear equation like "2x + 5 = 15" -> "x = 5"
     */
    fun evaluate(expression: String): String? {
        try {
            var expr = expression.trim()
            if (expr.isEmpty()) return null

            // If expression is an equation with a variable, solve it as a linear equation:
            if (expr.contains("=") && !expr.endsWith("=") && !expr.contains("==")) {
                val eqSolution = solveLinearEquation(expr)
                if (eqSolution != null) return eqSolution
            }

            if (expr.endsWith("=")) {
                expr = expr.dropLast(1).trim()
            }
            if (expr.isEmpty()) return null

            if (expr.contains("=") && !expr.contains("==")) {
                val eqSolution = solveLinearEquation(expr)
                if (eqSolution != null) return eqSolution
            }

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
     * Solves a single-variable linear equation such as:
     * "2x + 5 = 15" -> "x = 5"
     * "3x - 9 = 0" -> "x = 3"
     * "x + 4 = 10" -> "x = 6"
     * "2x + 4 = x + 10" -> "x = 6"
     * "5x = 20" -> "x = 4"
     */
    fun solveLinearEquation(equation: String): String? {
        try {
            val parts = equation.split("=")
            if (parts.size != 2) return null
            val leftSide = parts[0].trim()
            val rightSide = parts[1].trim()
            if (leftSide.isEmpty() || rightSide.isEmpty()) return null

            val varMatch = Regex("[a-zA-Z]").findAll(leftSide + rightSide)
                .map { it.value }
                .filter { it.lowercase() !in listOf("s", "q", "r", "t", "d", "e") } // avoid keywords like sqrt or de
                .distinct()
                .toList()

            val varName = if (varMatch.size == 1) {
                varMatch[0]
            } else if (varMatch.any { it.equals("x", ignoreCase = true) }) {
                "x"
            } else if (varMatch.isNotEmpty()) {
                varMatch.first()
            } else {
                return null
            }

            val h0 = evalExprWithVar(leftSide, varName, 0.0) - evalExprWithVar(rightSide, varName, 0.0)
            val h1 = evalExprWithVar(leftSide, varName, 1.0) - evalExprWithVar(rightSide, varName, 1.0)
            val h2 = evalExprWithVar(leftSide, varName, 2.0) - evalExprWithVar(rightSide, varName, 2.0)

            if (h0.isNaN() || h1.isNaN() || h2.isNaN() || h0.isInfinite() || h1.isInfinite() || h2.isInfinite()) {
                return null
            }

            val a = h1 - h0
            val b = h0

            // Linearity check: h(2) must equal 2*a + b
            if (kotlin.math.abs(h2 - (2.0 * a + b)) > 1e-4) {
                return null
            }

            if (kotlin.math.abs(a) < 1e-9) {
                return if (kotlin.math.abs(b) < 1e-9) {
                    "Infinitas soluciones"
                } else {
                    "Sin solución"
                }
            }

            val solution = -b / a
            val formatted = df.format(solution)
            return "$varName = $formatted"
        } catch (_: Exception) {
            return null
        }
    }

    private fun evalExprWithVar(expr: String, varName: String, value: Double): Double {
        var substituted = expr.trim()
        val coeffRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*$varName", RegexOption.IGNORE_CASE)
        substituted = substituted.replace(coeffRegex) {
            val coeff = it.groupValues[1]
            "$coeff * ($value)"
        }
        val standaloneRegex = Regex("(?<![a-zA-Z0-9_.])$varName(?![a-zA-Z0-9_])", RegexOption.IGNORE_CASE)
        substituted = substituted.replace(standaloneRegex, "($value)")
        return evalInfix(substituted)
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
            } else if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("->") && !trimmed.contains("=>")) {
                val eqResult = solveLinearEquation(trimmed)
                if (eqResult != null) {
                    "$trimmed  ->  $eqResult"
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

    data class AutoSolveResult(
        val newText: String,
        val newCursorPosition: Int
    )

    /**
     * Searches backwards from [cursorPosition] on the current line for a simple arithmetic
     * expression ending in '=', evaluates it, and returns the updated text with the solution
     * appended after '='.
     *
     * Supports basic arithmetic: addition (+), subtraction (-), multiplication (*, x, ×),
     * and division (/, ÷), including decimals, percentages, and parentheses.
     */
    fun trySolveInlineEquation(fullText: String, cursorPosition: Int): AutoSolveResult? {
        if (cursorPosition <= 0 || cursorPosition > fullText.length) return null
        val textBeforeCursor = fullText.substring(0, cursorPosition)

        // Must end with '='
        if (!textBeforeCursor.endsWith("=")) return null

        // Ignore double equals or comparison/assignment operators
        if (textBeforeCursor.endsWith("==") ||
            textBeforeCursor.endsWith("!=") ||
            textBeforeCursor.endsWith("<=") ||
            textBeforeCursor.endsWith(">=") ||
            textBeforeCursor.endsWith("+=") ||
            textBeforeCursor.endsWith("-=") ||
            textBeforeCursor.endsWith("*=") ||
            textBeforeCursor.endsWith("/=") ||
            textBeforeCursor.endsWith("=>") ||
            textBeforeCursor.endsWith("->")
        ) {
            return null
        }

        val beforeEqual = textBeforeCursor.dropLast(1)
        val trimmedBeforeEqual = beforeEqual.trimEnd()
        if (trimmedBeforeEqual.isEmpty()) return null

        // Restrict extraction to the current line
        val lastNewline = trimmedBeforeEqual.lastIndexOf('\n')
        val lineStartIndex = if (lastNewline == -1) 0 else lastNewline + 1
        val currentLine = trimmedBeforeEqual.substring(lineStartIndex)
        if (currentLine.isBlank()) return null

        // Find where the arithmetic expression begins on this line
        val exprStartInLine = findArithmeticExpressionStart(currentLine)
        val rawCandidateWithSpaces = currentLine.substring(exprStartInLine)
        val rawCandidate = rawCandidateWithSpaces.trimStart()
        if (rawCandidate.isEmpty()) return null

        val leadingSpacesCount = rawCandidateWithSpaces.length - rawCandidate.length
        val actualStartInLine = exprStartInLine + leadingSpacesCount
        val actualStartInFullText = lineStartIndex + actualStartInLine

        // Must be a valid arithmetic equation with operations between numbers
        if (!isSimpleArithmeticExpression(rawCandidate)) return null

        val normalized = normalizeExpression(rawCandidate)
        val mathResult = evaluate(normalized) ?: return null

        // Must be a valid numerical result
        if (mathResult.toDoubleOrNull() == null) return null

        val textBefore = fullText.substring(0, actualStartInFullText)
        val textAfter = fullText.substring(cursorPosition)

        val replacement = "$rawCandidate = $mathResult"
        val newText = textBefore + replacement + textAfter
        val newCursor = textBefore.length + replacement.length

        return AutoSolveResult(newText, newCursor)
    }

    /**
     * Solves a simple math expression when the user types '=' in a normal text box.
     * Prevents triggering on backspace or deletions.
     */
    fun tryAutoSolveEquationOnEqual(
        newValue: TextFieldValue,
        oldValue: TextFieldValue? = null
    ): TextFieldValue? {
        val cursor = newValue.selection.end
        if (cursor <= 0 || cursor > newValue.text.length) return null
        if (newValue.text[cursor - 1] != '=') return null

        // Ensure this '=' was just entered (not a result of backspacing/deleting text)
        if (oldValue != null) {
            val isInsert = newValue.text.length > oldValue.text.length
            val wasSelectionReplacement = !oldValue.selection.collapsed
            if (!isInsert && !wasSelectionReplacement) {
                return null
            }
        }

        val result = trySolveInlineEquation(newValue.text, cursor) ?: return null
        return TextFieldValue(
            text = result.newText,
            selection = TextRange(result.newCursorPosition)
        )
    }

    private fun findArithmeticExpressionStart(line: String): Int {
        var idx = line.length - 1
        var parenBalance = 0

        while (idx >= 0) {
            val c = line[idx]
            when {
                c.isDigit() -> idx--
                c == '.' -> idx--
                c == ',' -> {
                    val prevIsDigit = idx > 0 && line[idx - 1].isDigit()
                    val nextIsDigit = idx + 1 < line.length && line[idx + 1].isDigit()
                    if (prevIsDigit || nextIsDigit) {
                        idx--
                    } else {
                        break
                    }
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '%' ||
                c == '×' || c == '÷' || c == '·' || c == '\u2212' -> {
                    idx--
                }
                c == ')' -> {
                    parenBalance++
                    idx--
                }
                c == '(' -> {
                    if (parenBalance > 0) {
                        parenBalance--
                        idx--
                    } else {
                        break
                    }
                }
                c == ' ' || c == '\t' -> idx--
                c == 'x' || c == 'X' -> {
                    val rightSlice = line.substring(idx + 1).trimStart()
                    val rightHasOperand = rightSlice.firstOrNull()?.let { it.isDigit() || it == '(' || it == '-' || it == '+' } == true
                    val leftSlice = line.substring(0, idx).trimEnd()
                    val leftHasOperand = leftSlice.lastOrNull()?.let { it.isDigit() || it == ')' } == true
                    if (rightHasOperand && leftHasOperand) {
                        idx--
                    } else {
                        break
                    }
                }
                else -> break
            }
        }
        return idx + 1
    }

    private fun isSimpleArithmeticExpression(candidate: String): Boolean {
        val s = candidate.trim()
        if (s.isEmpty()) return false

        var openCount = 0
        for (c in s) {
            if (c == '(') openCount++
            else if (c == ')') openCount--
            if (openCount < 0) return false
        }
        if (openCount != 0) return false

        if (s.startsWith("*") || s.startsWith("/") || s.startsWith("^") || s.startsWith("%") || s.startsWith("×") || s.startsWith("÷")) {
            return false
        }
        if (s.endsWith("+") || s.endsWith("-") || s.endsWith("*") || s.endsWith("/") || s.endsWith("^") || s.endsWith("×") || s.endsWith("÷")) {
            return false
        }

        val hasTwoOperandsWithOp = Regex("""(?:\d|\))\s*[+\-*/xX×÷^·\u2212]\s*(?:\d|\(|[-+]\d)""").containsMatchIn(s)
        val hasPercentageOp = Regex("""\d+\s*%""").containsMatchIn(s)

        return hasTwoOperandsWithOp || hasPercentageOp
    }

    private fun normalizeExpression(expr: String): String {
        var s = expr
        s = s.replace('\u2212', '-')
        s = s.replace('×', '*').replace('·', '*')
        s = s.replace(Regex("""(?<=\d|\s)[xX](?=\d|\s)"""), "*")
        s = s.replace('÷', '/')
        s = s.replace(Regex("""(\d+),(\d+)"""), "$1.$2")
        return s
    }
}
