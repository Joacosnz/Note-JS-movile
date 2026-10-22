package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

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

    fun toSuperscript(s: String): String {
        return s.map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    fun toSubscript(s: String): String {
        return s.map { SUBSCRIPT_MAP[it] ?: it }.joinToString("")
    }

    /**
     * Converts a LaTeX math expression into a clean, human-readable Unicode string
     * suitable for monospaced rendering without external WebView or network dependencies.
     */
    fun formatLatexToUnicode(latex: String): String {
        if (latex.isBlank()) return ""
        return try {
            var text = latex.trim()

            // Support Notion-style multi-line equations (\\)
            text = text.replace(Regex("""\\\\\s*"""), "\n")

            // Strip surrounding dollar signs
            if (text.startsWith("$$") && text.endsWith("$$") && text.length >= 4) {
                text = text.substring(2, text.length - 2).trim()
            } else if (text.startsWith("$") && text.endsWith("$") && text.length >= 2) {
                text = text.substring(1, text.length - 1).trim()
            }

        // 1. Fractions: \frac{num}{den} -> num / den
        val fracRegex = Regex("""\\frac\s*\{([^{}]+)\}\s*\{([^{}]+)\}""")
        while (fracRegex.containsMatchIn(text)) {
            text = text.replace(fracRegex) { match ->
                val num = match.groupValues[1].trim()
                val den = match.groupValues[2].trim()
                val safeNum = if (num.length > 1 && (num.contains("+") || num.contains("-"))) "($num)" else num
                val safeDen = if (den.length > 1 && (den.contains("+") || den.contains("-"))) "($den)" else den
                "$safeNum / $safeDen"
            }
        }

        // 2. Square roots: \sqrt[n]{x} -> ⁿ√(x), \sqrt{x} -> √(x)
        text = text.replace(Regex("""\\sqrt\s*\[([^{}]+)\]\s*\{([^{}]+)\}""")) {
            val root = toSuperscript(it.groupValues[1].trim())
            val inner = it.groupValues[2].trim()
            "${root}√($inner)"
        }
        text = text.replace(Regex("""\\sqrt\s*\{([^{}]+)\}""")) {
            val inner = it.groupValues[1].trim()
            if (inner.length == 1) "√$inner" else "√($inner)"
        }

        // 3. Exponents with braces: x^{2} or x^{-1}
        text = text.replace(Regex("""\^\{([^{}]+)\}""")) {
            toSuperscript(it.groupValues[1].trim())
        }
        // Exponents single char: x^2, x^n
        text = text.replace(Regex("""\^([a-zA-Z0-9+\-])""")) {
            toSuperscript(it.groupValues[1])
        }

        // 4. Subscripts with braces: x_{1} or x_{max}
        text = text.replace(Regex("""_\{([^{}]+)\}""")) {
            toSubscript(it.groupValues[1].trim())
        }
        // Subscripts single char: x_1, x_i
        text = text.replace(Regex("""_([a-zA-Z0-9+\-])""")) {
            toSubscript(it.groupValues[1])
        }

        // 5. Greek letters & symbols
        val symbolMap = listOf(
            "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
            "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
            "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
            "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
            "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
            "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
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
            "\\in" to "∈", "\\notin" to "∉", "\\subset" to "⊂", "\\subseteq" to "⊆",
            "\\supset" to "⊃", "\\supseteq" to "⊇", "\\cap" to "∩", "\\cup" to "∪",
            "\\to" to "→", "\\rightarrow" to "→", "\\leftarrow" to "←",
            "\\Rightarrow" to "⇒", "\\Leftarrow" to "⇐", "\\Leftrightarrow" to "⇔",
            "\\iff" to "⇔", "\\implies" to "⇒",
            "\\lim" to "lim", "\\ln" to "ln", "\\log" to "log",
            "\\sin" to "sin", "\\cos" to "cos", "\\tan" to "tan",
            "\\sec" to "sec", "\\csc" to "csc", "\\cot" to "cot",
            "\\arcsin" to "arcsin", "\\arccos" to "arccos", "\\arctan" to "arctan",
            "\\left" to "", "\\right" to "",
            "\\quad" to " ", "\\qquad" to "  ", "\\," to " ", "\\;" to " ", "\\!" to ""
        )

        for ((latexSym, uniSym) in symbolMap) {
            text = text.replace(latexSym, uniSym)
        }

        // 6. Text blocks: \text{foo} -> foo
        text = text.replace(Regex("""\\text\s*\{([^{}]+)\}""")) { it.groupValues[1] }

        // 7. Cleanup remaining stray backslashes or curly braces
        text = text.replace(Regex("""[{}]"""), "")
        text = text.replace(Regex("""\\([a-zA-Z]+)""")) { it.groupValues[1] }

        // 8. Normalize spacing for linear formulas and operators to ensure consistent alignment
        text = text.replace(Regex("""(?<=[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿⁱˣʸᵃᵇᶜᵈᵉᵏᵐᵖʳᵗ₀₁₂₃₄₅₆₇₈₉₊₋₌₎⁾])\s*([=+\-×·÷±≤≥≠≈→⇒])\s*(?=[a-zA-Z0-9⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼ⁿⁱˣʸᵃᵇᶜᵈᵉᵏᵐᵖʳᵗ₀₁₂₃₄₅₆₇₈₉₊₋₌₍⁽√])""")) { match ->
            " ${match.groupValues[1]} "
        }

        text = text.replace(Regex("""\s+"""), " ")
        text.trim()
    } catch (e: Exception) {
        latex.trim()
    }
}
}

/**
 * 100% Native Jetpack Compose mathematical formula renderer.
 * Completely replaces fragile Android WebView engines to ensure instant rendering,
 * zero network delay, perfect monospace character alignment, and zero Chromium crashes.
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
    val formatted = remember(expression) {
        MathFormatter.formatLatexToUnicode(expression)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatted,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            color = color,
            textAlign = textAlign,
            letterSpacing = 0.04.sp
        )
    }
}
