package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class InlineMathTokenKind {
    NORMAL,
    VARIABLE,
    NUMBER,
    OPERATOR,
    SUPERSCRIPT,
    SUBSCRIPT,
    FRAC_NUM,
    FRAC_SLASH,
    FRAC_DEN,
    RADICAL,
    RADICAND
}

data class InlineMathToken(
    val text: String,
    val kind: InlineMathTokenKind
)

sealed class MathAstNode {
    data class Plain(val text: String) : MathAstNode()
    data class Fraction(val numerator: List<MathAstNode>, val denominator: List<MathAstNode>) : MathAstNode()
    data class Sqrt(val index: List<MathAstNode>?, val radicand: List<MathAstNode>) : MathAstNode()
    data class Scripts(
        val base: List<MathAstNode>,
        val subscript: List<MathAstNode>?,
        val superscript: List<MathAstNode>?
    ) : MathAstNode()
    data class Accent(val inner: List<MathAstNode>, val type: String) : MathAstNode()
    data class Matrix(val envType: String, val rows: List<List<List<MathAstNode>>>) : MathAstNode()
}

object MathFormatter {

    private val SUPERSCRIPT_MAP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 'a' to 'ᵃ',
        'b' to 'ᵇ', 'c' to 'ᶜ', 'd' to 'ᵈ', 'e' to 'ᵉ', 'k' to 'ᵏ',
        'm' to 'ᵐ', 'p' to 'ᵖ', 'r' to 'ʳ', 't' to 'ᵗ'
    )

    private val SUBSCRIPT_MAP = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'h' to 'ₕ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ',
        's' to 'ₛ', 't' to 'ₜ', 'i' to 'ᵢ', 'j' to 'ⱼ'
    )

    internal val SYMBOL_MAP = listOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\varepsilon" to "ε", "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η",
        "\\vartheta" to "ϑ", "\\theta" to "θ", "\\iota" to "ι", "\\kappa" to "κ",
        "\\lambda" to "λ", "\\mu" to "μ", "\\nu" to "ν", "\\xi" to "ξ",
        "\\pi" to "π", "\\rho" to "ρ", "\\sigma" to "σ", "\\tau" to "τ",
        "\\upsilon" to "υ", "\\varphi" to "φ", "\\phi" to "φ", "\\chi" to "χ",
        "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
        "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω",
        "\\pm" to "±", "\\mp" to "∓", "\\times" to "×", "\\div" to "÷",
        "\\cdot" to "·", "\\bullet" to "•", "\\circ" to "∘", "\\star" to "⋆",
        "\\leq" to "≤", "\\le" to "≤", "\\geq" to "≥", "\\ge" to "≥",
        "\\neq" to "≠", "\\ne" to "≠", "\\approx" to "≈", "\\equiv" to "≡",
        "\\sim" to "∼", "\\propto" to "∝",
        "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫", "\\oint" to "∮",
        "\\infty" to "∞", "\\partial" to "∂", "\\nabla" to "∇",
        "\\forall" to "∀", "\\exists" to "∃", "\\emptyset" to "∅",
        "\\in" to "∈", "\\notin" to "∉", "\\subseteq" to "⊆", "\\subset" to "⊂",
        "\\supseteq" to "⊇", "\\supset" to "⊃", "\\cap" to "∩", "\\cup" to "∪",
        "\\Leftrightarrow" to "⇔", "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐",
        "\\leftrightarrow" to "↔", "\\rightarrow" to "→", "\\leftarrow" to "←",
        "\\to" to "→", "\\iff" to "⇔", "\\implies" to "⇒",
        "\\lim" to "lim", "\\ln" to "ln", "\\log" to "log",
        "\\arcsin" to "arcsin", "\\arccos" to "arccos", "\\arctan" to "arctan",
        "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan",
        "\\sec" to "sec", "\\csc" to "csc", "\\cot" to "cot",
        "\\left" to "", "\\right" to "",
        "\\quad" to " ", "\\qquad" to "  ", "\\," to " ", "\\;" to " ", "\\!" to ""
    )

    fun toSuperscript(s: String): String {
        return s.map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    fun toSubscript(s: String): String {
        return s.map { SUBSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    private fun extractGroup(input: String, startIndex: Int, openChar: Char = '{', closeChar: Char = '}'): Pair<String, Int>? {
        var i = startIndex
        while (i < input.length && input[i].isWhitespace()) i++
        if (i >= input.length || input[i] != openChar) return null
        val contentStart = i + 1
        var depth = 1
        i++
        while (i < input.length && depth > 0) {
            when (input[i]) {
                openChar -> depth++
                closeChar -> depth--
            }
            i++
        }
        if (depth != 0) {
            return input.substring(contentStart) to input.length
        }
        return input.substring(contentStart, i - 1) to i
    }

    private fun extractSingleTokenOrGroup(input: String, startIndex: Int): Pair<String, Int> {
        var i = startIndex
        while (i < input.length && input[i].isWhitespace()) i++
        if (i >= input.length) return "" to i
        if (input[i] == '{') {
            return extractGroup(input, i, '{', '}') ?: ("" to i)
        }
        if (input[i] == '\\') {
            val cmdStart = i
            i++
            while (i < input.length && input[i].isLetter()) i++
            return input.substring(cmdStart, i) to i
        }
        return input[i].toString() to (i + 1)
    }

    fun replaceSymbolsPlain(raw: String): String {
        var text = raw
        text = text.replace(Regex("""\\text\s*\{([^{}]*)\}""")) { it.groupValues[1] }
        for ((latexSym, uniSym) in SYMBOL_MAP) {
            text = text.replace(latexSym, uniSym)
        }
        text = text.replace(Regex("""[{}]"""), "")
        text = text.replace(Regex("""\\([a-zA-Z]+)""")) { it.groupValues[1] }
        return text
    }

    /**
     * Parses a LaTeX expression into styled inline tokens suitable for AnnotatedString
     * with BaselineShift so fractions (\frac{a}{b}), exponents (x^2), subscripts (x_i),
     * and roots (\sqrt{x}) render natively inside a single-line or multi-line text field.
     */
    fun parseInlineStyledTokens(latex: String): List<InlineMathToken> {
        val cleaned = latex.trim().let {
            when {
                it.startsWith("$$") && it.endsWith("$$") && it.length >= 4 -> it.substring(2, it.length - 2).trim()
                it.startsWith("$") && it.endsWith("$") && it.length >= 2 -> it.substring(1, it.length - 1).trim()
                else -> it
            }
        }
        if (cleaned.isEmpty()) return emptyList()

        val nodes = parseLatexAst(cleaned)
        val tokens = mutableListOf<InlineMathToken>()
        flattenAstToInlineTokens(nodes, tokens, null)
        return tokens
    }

    private fun flattenAstToInlineTokens(
        nodes: List<MathAstNode>,
        out: MutableList<InlineMathToken>,
        overrideKind: InlineMathTokenKind?
    ) {
        for (node in nodes) {
            when (node) {
                is MathAstNode.Plain -> {
                    val formatted = formatPlainSpacing(node.text)
                    if (formatted.isNotEmpty()) {
                        out.add(InlineMathToken(formatted, overrideKind ?: InlineMathTokenKind.NORMAL))
                    }
                }
                is MathAstNode.Fraction -> {
                    val numStr = astListToPlainText(node.numerator).trim()
                    val denStr = astListToPlainText(node.denominator).trim()
                    val safeNum = if (numStr.length > 2 && (numStr.contains('+') || numStr.contains('-'))) "($numStr)" else numStr
                    val safeDen = if (denStr.length > 2 && (denStr.contains('+') || denStr.contains('-'))) "($denStr)" else denStr
                    out.add(InlineMathToken(safeNum.ifEmpty { "□" }, InlineMathTokenKind.FRAC_NUM))
                    out.add(InlineMathToken("⁄", InlineMathTokenKind.FRAC_SLASH))
                    out.add(InlineMathToken(safeDen.ifEmpty { "□" }, InlineMathTokenKind.FRAC_DEN))
                }
                is MathAstNode.Sqrt -> {
                    if (!node.index.isNullOrEmpty()) {
                        val idxStr = astListToPlainText(node.index).trim()
                        if (idxStr.isNotEmpty()) {
                            out.add(InlineMathToken(idxStr, InlineMathTokenKind.SUPERSCRIPT))
                        }
                    }
                    out.add(InlineMathToken("√", InlineMathTokenKind.RADICAL))
                    val radStr = astListToPlainText(node.radicand).trim()
                    out.add(InlineMathToken(radStr.ifEmpty { "□" }, InlineMathTokenKind.RADICAND))
                }
                is MathAstNode.Scripts -> {
                    flattenAstToInlineTokens(node.base, out, overrideKind)
                    if (!node.subscript.isNullOrEmpty()) {
                        val subStr = astListToPlainText(node.subscript).trim()
                        if (subStr.isNotEmpty()) {
                            out.add(InlineMathToken(subStr, InlineMathTokenKind.SUBSCRIPT))
                        }
                    }
                    if (!node.superscript.isNullOrEmpty()) {
                        val supStr = astListToPlainText(node.superscript).trim()
                        if (supStr.isNotEmpty()) {
                            out.add(InlineMathToken(supStr, InlineMathTokenKind.SUPERSCRIPT))
                        }
                    }
                }
                is MathAstNode.Accent -> {
                    val innerStr = astListToPlainText(node.inner).trim()
                    val combined = when (node.type) {
                        "vec" -> "$innerStr\u20D7"
                        "hat" -> "$innerStr\u0302"
                        "bar", "overline" -> innerStr
                        else -> innerStr
                    }
                    out.add(
                        InlineMathToken(
                            combined,
                            if (node.type == "bar" || node.type == "overline") InlineMathTokenKind.RADICAND else (overrideKind ?: InlineMathTokenKind.NORMAL)
                        )
                    )
                }
                is MathAstNode.Matrix -> {
                    val rowsText = node.rows.joinToString("; ") { row ->
                        row.joinToString(", ") { cell -> astListToPlainText(cell).trim() }
                    }
                    val (open, close) = when (node.envType) {
                        "pmatrix" -> "(" to ")"
                        "vmatrix" -> "|" to "|"
                        "cases" -> "{ " to ""
                        else -> "[" to "]"
                    }
                    out.add(InlineMathToken("$open$rowsText$close", overrideKind ?: InlineMathTokenKind.NORMAL))
                }
            }
        }
    }

    private fun astListToPlainText(nodes: List<MathAstNode>): String {
        val sb = StringBuilder()
        for (node in nodes) {
            when (node) {
                is MathAstNode.Plain -> sb.append(node.text)
                is MathAstNode.Fraction -> {
                    val n = astListToPlainText(node.numerator).trim()
                    val d = astListToPlainText(node.denominator).trim()
                    sb.append("$n⁄$d")
                }
                is MathAstNode.Sqrt -> {
                    val idx = node.index?.let { toSuperscript(astListToPlainText(it).trim()) } ?: ""
                    val rad = astListToPlainText(node.radicand).trim()
                    sb.append("${idx}√($rad)")
                }
                is MathAstNode.Scripts -> {
                    sb.append(astListToPlainText(node.base))
                    node.subscript?.let { sb.append(toSubscript(astListToPlainText(it).trim())) }
                    node.superscript?.let { sb.append(toSuperscript(astListToPlainText(it).trim())) }
                }
                is MathAstNode.Accent -> {
                    sb.append(astListToPlainText(node.inner))
                }
                is MathAstNode.Matrix -> {
                    val r = node.rows.joinToString("; ") { row ->
                        row.joinToString(", ") { cell -> astListToPlainText(cell).trim() }
                    }
                    sb.append("[$r]")
                }
            }
        }
        return sb.toString()
    }

    private fun formatPlainSpacing(raw: String): String {
        return raw.replace(
            Regex("""(?<=[a-zA-Z0-9α-ωΑ-Ω⁰¹²³⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉)\]])\s*([=+\-×·÷±≤≥≠≈→⇒⇔])\s*(?=[a-zA-Z0-9α-ωΑ-Ω(\[√])""")
        ) { match ->
            " ${match.groupValues[1]} "
        }.replace(Regex("""\s+"""), " ")
    }

    /**
     * Parses a LaTeX math string into a structured 2D AST (`List<MathAstNode>`).
     */
    fun parseLatexAst(latex: String): List<MathAstNode> {
        val input = latex.trim()
        if (input.isEmpty()) return emptyList()

        val nodes = mutableListOf<MathAstNode>()
        val plainBuffer = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plainBuffer.isNotEmpty()) {
                val replaced = replaceSymbolsPlain(plainBuffer.toString())
                if (replaced.isNotEmpty()) {
                    nodes.add(MathAstNode.Plain(replaced))
                }
                plainBuffer.clear()
            }
        }

        while (i < input.length) {
            val ch = input[i]
            when {
                input.startsWith("\\begin{", i) -> {
                    val envGroup = extractGroup(input, i + 6, '{', '}')
                    if (envGroup != null) {
                        val envName = envGroup.first.trim()
                        val endTag = "\\end{$envName}"
                        val contentStart = envGroup.second
                        val endIdx = input.indexOf(endTag, contentStart)
                        val body = if (endIdx != -1) input.substring(contentStart, endIdx) else input.substring(contentStart)
                        i = if (endIdx != -1) endIdx + endTag.length else input.length
                        flushPlain()
                        val rawRows = body.trim().split(Regex("""\\\\""")).filter { it.isNotBlank() }
                        val parsedRows = rawRows.map { rowStr ->
                            rowStr.split("&").map { cellStr -> parseLatexAst(cellStr.trim()) }
                        }
                        nodes.add(MathAstNode.Matrix(envName, parsedRows))
                    } else {
                        plainBuffer.append(ch)
                        i++
                    }
                }
                input.startsWith("\\frac", i) || input.startsWith("\\dfrac", i) || input.startsWith("\\tfrac", i) -> {
                    val cmdLen = if (input.startsWith("\\frac", i)) 5 else 6
                    val numGroup = extractGroup(input, i + cmdLen, '{', '}')
                    if (numGroup != null) {
                        val denGroup = extractGroup(input, numGroup.second, '{', '}')
                        if (denGroup != null) {
                            flushPlain()
                            val numNodes = parseLatexAst(numGroup.first)
                            val denNodes = parseLatexAst(denGroup.first)
                            nodes.add(MathAstNode.Fraction(numNodes, denNodes))
                            i = denGroup.second
                        } else {
                            // Incomplete denominator while user is typing (e.g. \frac{a})
                            flushPlain()
                            val numNodes = parseLatexAst(numGroup.first)
                            nodes.add(MathAstNode.Fraction(numNodes, listOf(MathAstNode.Plain("□"))))
                            i = numGroup.second
                        }
                    } else {
                        plainBuffer.append(ch)
                        i++
                    }
                }
                input.startsWith("\\sqrt", i) -> {
                    var cursor = i + 5
                    while (cursor < input.length && input[cursor].isWhitespace()) cursor++
                    val indexGroup = if (cursor < input.length && input[cursor] == '[') {
                        extractGroup(input, cursor, '[', ']')
                    } else null
                    if (indexGroup != null) {
                        cursor = indexGroup.second
                    }
                    val radGroup = extractGroup(input, cursor, '{', '}')
                    if (radGroup != null) {
                        flushPlain()
                        val idxNodes = indexGroup?.first?.let { parseLatexAst(it) }
                        val radNodes = parseLatexAst(radGroup.first)
                        nodes.add(MathAstNode.Sqrt(idxNodes, radNodes))
                        i = radGroup.second
                    } else {
                        plainBuffer.append(ch)
                        i++
                    }
                }
                input.startsWith("\\vec", i) || input.startsWith("\\hat", i) || input.startsWith("\\bar", i) || input.startsWith("\\overline", i) -> {
                    val cmdEnd = input.indexOf('{', i).takeIf { it != -1 } ?: (i + 4)
                    val accentName = input.substring(i + 1, cmdEnd).trim()
                    val grp = extractGroup(input, cmdEnd, '{', '}')
                    if (grp != null) {
                        flushPlain()
                        nodes.add(MathAstNode.Accent(parseLatexAst(grp.first), accentName))
                        i = grp.second
                    } else {
                        plainBuffer.append(ch)
                        i++
                    }
                }
                ch == '^' || ch == '_' -> {
                    // Extract base from the last character/node
                    val baseNodes: List<MathAstNode> = if (plainBuffer.isNotEmpty()) {
                        // Check if plainBuffer ends with a LaTeX command like \sum, \int, \lim, \pi
                        val bufStr = plainBuffer.toString()
                        val trailingCmdMatch = Regex("""(\\[a-zA-Z]+|[a-zA-Z0-9α-ωΑ-Ω∑∏∫∮)])$""").find(bufStr)
                        if (trailingCmdMatch != null) {
                            val matchStart = trailingCmdMatch.range.first
                            val prefix = bufStr.substring(0, matchStart)
                            val baseToken = trailingCmdMatch.value
                            plainBuffer.clear()
                            if (prefix.isNotEmpty()) {
                                val rep = replaceSymbolsPlain(prefix)
                                if (rep.isNotEmpty()) nodes.add(MathAstNode.Plain(rep))
                            }
                            listOf(MathAstNode.Plain(replaceSymbolsPlain(baseToken)))
                        } else {
                            flushPlain()
                            if (nodes.isNotEmpty()) listOf(nodes.removeAt(nodes.lastIndex)) else listOf(MathAstNode.Plain(""))
                        }
                    } else if (nodes.isNotEmpty()) {
                        listOf(nodes.removeAt(nodes.lastIndex))
                    } else {
                        listOf(MathAstNode.Plain(""))
                    }

                    var subNodes: List<MathAstNode>? = null
                    var supNodes: List<MathAstNode>? = null
                    var cursor = i
                    while (cursor < input.length && (input[cursor] == '^' || input[cursor] == '_')) {
                        val isSup = input[cursor] == '^'
                        val (scriptText, nextPos) = extractSingleTokenOrGroup(input, cursor + 1)
                        val parsedScript = parseLatexAst(scriptText)
                        if (isSup) supNodes = parsedScript else subNodes = parsedScript
                        cursor = nextPos
                        while (cursor < input.length && input[cursor].isWhitespace()) cursor++
                    }
                    nodes.add(MathAstNode.Scripts(baseNodes, subNodes, supNodes))
                    i = cursor
                }
                else -> {
                    plainBuffer.append(ch)
                    i++
                }
            }
        }
        flushPlain()
        return nodes
    }

    /**
     * Converts a LaTeX math expression into a clean, human-readable Unicode string
     * for plain-text exports (such as PDF text fallback or sharing).
     */
    fun formatLatexToUnicode(latex: String): String {
        if (latex.isBlank()) return ""
        return try {
            val tokens = parseInlineStyledTokens(latex)
            val sb = StringBuilder()
            for (token in tokens) {
                when (token.kind) {
                    InlineMathTokenKind.SUPERSCRIPT -> sb.append(toSuperscript(token.text))
                    InlineMathTokenKind.SUBSCRIPT -> sb.append(toSubscript(token.text))
                    InlineMathTokenKind.FRAC_NUM -> sb.append(toSuperscript(token.text))
                    InlineMathTokenKind.FRAC_SLASH -> sb.append("⁄")
                    InlineMathTokenKind.FRAC_DEN -> sb.append(toSubscript(token.text))
                    else -> sb.append(token.text)
                }
            }
            sb.toString().replace(Regex("""\s+"""), " ").trim()
        } catch (e: Exception) {
            latex.trim()
        }
    }
}

/**
 * 100% Native Jetpack Compose 2D Mathematical Formula Renderer.
 * Renders true vertical stacked fractions (\frac{a}{b}), radicals with vinculum overbars (\sqrt{x}),
 * superscripts/subscripts, big operators (\sum, \int, \lim), and 2D matrices without WebView.
 */
@Composable
fun NativeMathView(
    expression: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    fontSize: TextUnit = 18.sp,
    color: Color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF111827),
    textAlign: TextAlign = TextAlign.Center
) {
    val lines = remember(expression) {
        val trimmed = expression.trim().let {
            when {
                it.startsWith("$$") && it.endsWith("$$") && it.length >= 4 -> it.substring(2, it.length - 2).trim()
                it.startsWith("$") && it.endsWith("$") && it.length >= 2 -> it.substring(1, it.length - 1).trim()
                else -> it
            }
        }
        if (trimmed.contains("\\begin{")) {
            listOf(MathFormatter.parseLatexAst(trimmed))
        } else {
            trimmed.split(Regex("""\\\\\s*"""))
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(trimmed) }
                .map { MathFormatter.parseLatexAst(it) }
        }
    }

    val horizontalAlign = when (textAlign) {
        TextAlign.Start, TextAlign.Left -> Alignment.Start
        TextAlign.End, TextAlign.Right -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val boxAlignment = when (textAlign) {
        TextAlign.Start, TextAlign.Left -> Alignment.CenterStart
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(
        modifier = modifier,
        contentAlignment = boxAlignment
    ) {
        Column(
            horizontalAlignment = horizontalAlign,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            lines.forEach { astNodes ->
                MathAstRow(
                    nodes = astNodes,
                    fontSize = fontSize,
                    color = color
                )
            }
        }
    }
}

@Composable
internal fun MathAstRow(
    nodes: List<MathAstNode>,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        nodes.forEach { node ->
            MathAstNodeView(
                node = node,
                fontSize = fontSize,
                color = color
            )
        }
    }
}

@Composable
private fun MathAstNodeView(
    node: MathAstNode,
    fontSize: TextUnit,
    color: Color
) {
    when (node) {
        is MathAstNode.Plain -> {
            val formatted = remember(node.text) {
                node.text.replace(
                    Regex("""(?<=[a-zA-Z0-9α-ωΑ-Ω)\]])\s*([=+\-×·÷±≤≥≠≈→⇒⇔:])\s*(?=[a-zA-Z0-9α-ωΑ-Ω(\[√])""")
                ) { " ${it.groupValues[1]} " }
            }
            Text(
                text = formatted,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                color = color,
                letterSpacing = 0.02.sp
            )
        }

        is MathAstNode.Fraction -> {
            val subFontSize = (fontSize.value * 0.82f).coerceAtLeast(11f).sp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .width(IntrinsicSize.Max)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                ) {
                    MathAstRow(
                        nodes = node.numerator.ifEmpty { listOf(MathAstNode.Plain("□")) },
                        fontSize = subFontSize,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(1.5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
                Spacer(modifier = Modifier.height(1.5.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                ) {
                    MathAstRow(
                        nodes = node.denominator.ifEmpty { listOf(MathAstNode.Plain("□")) },
                        fontSize = subFontSize,
                        color = color
                    )
                }
            }
        }

        is MathAstNode.Sqrt -> {
            val idxFontSize = (fontSize.value * 0.62f).coerceAtLeast(9f).sp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                if (!node.index.isNullOrEmpty()) {
                    Box(modifier = Modifier.padding(bottom = (fontSize.value * 0.45f).dp)) {
                        MathAstRow(
                            nodes = node.index,
                            fontSize = idxFontSize,
                            color = color
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .drawBehind {
                            val strokeW = 1.6.dp.toPx()
                            val w = size.width
                            val h = size.height
                            val radW = 10.dp.toPx()
                            val path = Path().apply {
                                moveTo(0f, h * 0.58f)
                                lineTo(radW * 0.32f, h * 0.50f)
                                lineTo(radW * 0.62f, h - strokeW)
                                lineTo(radW, strokeW)
                                lineTo(w, strokeW)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = strokeW,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        .padding(start = 11.dp, top = 3.dp, end = 2.dp, bottom = 1.dp)
                ) {
                    MathAstRow(
                        nodes = node.radicand.ifEmpty { listOf(MathAstNode.Plain("□")) },
                        fontSize = fontSize,
                        color = color
                    )
                }
            }
        }

        is MathAstNode.Scripts -> {
            val baseText = (node.base.singleOrNull() as? MathAstNode.Plain)?.text?.trim()
            val isBigOp = baseText in setOf("∑", "∏", "∫", "∮", "lim")
            val scriptSize = (fontSize.value * 0.68f).coerceAtLeast(10f).sp

            if (isBigOp && baseText in setOf("∑", "∏", "lim")) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 3.dp)
                ) {
                    if (!node.superscript.isNullOrEmpty()) {
                        MathAstRow(nodes = node.superscript, fontSize = scriptSize, color = color)
                    }
                    Text(
                        text = baseText ?: "",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (baseText == "lim") fontSize else (fontSize.value * 1.25f).sp,
                        color = color
                    )
                    if (!node.subscript.isNullOrEmpty()) {
                        MathAstRow(nodes = node.subscript, fontSize = scriptSize, color = color)
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MathAstRow(
                        nodes = node.base,
                        fontSize = if (isBigOp) (fontSize.value * 1.3f).sp else fontSize,
                        color = color
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(start = 1.dp)
                    ) {
                        if (!node.superscript.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.padding(
                                    bottom = if (node.subscript.isNullOrEmpty()) (fontSize.value * 0.35f).dp else 0.dp
                                )
                            ) {
                                MathAstRow(
                                    nodes = node.superscript,
                                    fontSize = scriptSize,
                                    color = color
                                )
                            }
                        }
                        if (!node.subscript.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.padding(
                                    top = if (node.superscript.isNullOrEmpty()) (fontSize.value * 0.35f).dp else 0.dp
                                )
                            ) {
                                MathAstRow(
                                    nodes = node.subscript,
                                    fontSize = scriptSize,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }

        is MathAstNode.Accent -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                if (node.type == "overline" || node.type == "bar") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.3.dp)
                            .background(color)
                    )
                } else if (node.type == "vec") {
                    Text(
                        text = "→",
                        fontSize = (fontSize.value * 0.65f).sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                } else if (node.type == "hat") {
                    Text(
                        text = "^",
                        fontSize = (fontSize.value * 0.70f).sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                MathAstRow(
                    nodes = node.inner,
                    fontSize = fontSize,
                    color = color
                )
            }
        }

        is MathAstNode.Matrix -> {
            val cellSize = (fontSize.value * 0.86f).coerceAtLeast(12f).sp
            val (leftDelim, rightDelim) = when (node.envType) {
                "pmatrix" -> "(" to ")"
                "bmatrix" -> "[" to "]"
                "vmatrix" -> "|" to "|"
                "cases" -> "{" to ""
                else -> "" to ""
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (leftDelim.isNotEmpty()) {
                    Text(
                        text = leftDelim,
                        fontFamily = FontFamily.Serif,
                        fontSize = (fontSize.value * (1.1f + 0.35f * (node.rows.size - 1).coerceAtLeast(0))).sp,
                        fontWeight = FontWeight.Light,
                        color = color
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    node.rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { cell ->
                                MathAstRow(
                                    nodes = cell,
                                    fontSize = cellSize,
                                    color = color
                                )
                            }
                        }
                    }
                }
                if (rightDelim.isNotEmpty()) {
                    Text(
                        text = rightDelim,
                        fontFamily = FontFamily.Serif,
                        fontSize = (fontSize.value * (1.1f + 0.35f * (node.rows.size - 1).coerceAtLeast(0))).sp,
                        fontWeight = FontWeight.Light,
                        color = color
                    )
                }
            }
        }
    }
}
