package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal val WORD_COUNT_REGEX = Regex("""\s+""")
internal val EMPTY_HIGHLIGHT_REGEX = Regex("""==\s*==|====|===""")
internal val MALFORMED_HIGHLIGHT_1_REGEX = Regex("""==([^=\n]+)=(?!=)""")
internal val MALFORMED_HIGHLIGHT_2_REGEX = Regex("""(?<!=)=([^=\n]+)==""")
internal val MALFORMED_HIGHLIGHT_3_REGEX = Regex("""(?<!=)=([^=\n]+)=(?![=])""")
internal val HIGHLIGHT_REGEX = Regex("""==([^=\n]+)==""")
internal val HASHTAG_REGEX = Regex("""#([a-zA-Z0-9_áéíóúÁÉÍÓÚñÑ]+)""")
internal val NOTE_LINK_REGEX = Regex("""\[([^\]]+)\]\(notejs://note/([^\)]+)\)""")
internal val WEB_LINK_REGEX = Regex(
    """\[([^\]]+)\]\(((?:https?://|www\.|(?:[a-zA-Z0-9-]+\.)+(?:com|org|net|edu|gov|io|dev|app|ai|co|es|mx|ar|cl|pe|me|info|tv|gg|xyz|ly|gl|be)\b)[^\s\)]*)\)""",
    RegexOption.IGNORE_CASE
)
internal val RAW_URL_VISUAL_REGEX = Regex(
    """(?<![/@\w])(?:\bhttps?://[^\s<>"{}|\\^`\[\]\)]+|\bwww\.[^\s<>"{}|\\^`\[\]\)]+|\b(?:[a-zA-Z0-9-]+\.)+(?:com|org|net|edu|gov|io|dev|app|ai|co|es|mx|ar|cl|pe|me|info|tv|gg|xyz|ly|gl|be)\b(?:/[^\s<>"{}|\\^`\[\]\)]*)?)""",
    RegexOption.IGNORE_CASE
)
internal val INLINE_MATH_REGEX = Regex("""\[math:((?:(?!\[math:)[^\[\]]|\[[^\]]*\])+)\]""")
internal val EMPTY_INLINE_MATH_REGEX = Regex("""\[math:\s*\]""")

internal fun normalizeDetectedWebUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim().trimEnd('.', ',', ';', ':', '!', '?')
    return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

internal fun sanitizeHighlightTags(text: String): String {
    var result = text
    // 0. Eliminar etiquetas de ecuación vacías [math:]
    result = result.replace(EMPTY_INLINE_MATH_REGEX, "")

    // 1. Eliminar etiquetas vacías o triples como ==== o ===
    result = result.replace(EMPTY_HIGHLIGHT_REGEX, "")

    // 2. Corregir etiqueta rota al final: ==palabra=
    result = result.replace(MALFORMED_HIGHLIGHT_1_REGEX) {
        it.groupValues[1]
    }

    // 3. Corregir etiqueta rota al inicio: =palabra==
    result = result.replace(MALFORMED_HIGHLIGHT_2_REGEX) {
        it.groupValues[1]
    }

    // 4. Corregir etiquetas únicas: =palabra=
    result = result.replace(MALFORMED_HIGHLIGHT_3_REGEX) {
        it.groupValues[1]
    }

    return result
}

internal fun sanitizeTextFieldValue(tfv: TextFieldValue): TextFieldValue {
    var rawText = tfv.text
    var cursorStart = tfv.selection.start.coerceIn(0, rawText.length)
    var cursorEnd = tfv.selection.end.coerceIn(0, rawText.length)

    // Si el usuario presionó Backspace justo al final de un bloque [math:...], se habrá borrado el ']' de cierre,
    // dejando un "[math:..." sin cerrar justo antes del cursor. Lo eliminamos de forma atómica y limpia.
    if (cursorStart == cursorEnd && rawText.contains("[math:")) {
        val validRanges = INLINE_MATH_REGEX.findAll(rawText).map { it.range }.toList()
        var searchIdx = 0
        while (true) {
            val idx = rawText.indexOf("[math:", searchIdx)
            if (idx == -1) break
            val isValid = validRanges.any { it.first == idx }
            if (!isValid && cursorStart > idx) {
                val lineEnd = rawText.indexOf('\n', idx).let { if (it == -1) rawText.length else it }
                val nextMath = rawText.indexOf("[math:", idx + 6).let { if (it == -1) rawText.length else it }
                val removeEnd = cursorStart.coerceAtMost(minOf(lineEnd, nextMath))
                if (removeEnd > idx) {
                    rawText = rawText.removeRange(idx, removeEnd)
                    cursorStart = idx
                    cursorEnd = idx
                    break
                }
            }
            searchIdx = idx + 6
        }
    }

    val sanitized = sanitizeHighlightTags(rawText)
    val start = cursorStart.coerceIn(0, sanitized.length)
    val end = cursorEnd.coerceIn(0, sanitized.length)
    val coercedRange = if (start <= end) TextRange(start, end) else TextRange(end, start)
    if (sanitized == tfv.text && tfv.selection == coercedRange) {
        return tfv
    }
    return tfv.copy(
        text = sanitized,
        selection = coercedRange
    )
}

internal enum class SyntaxType {
    HIGHLIGHT,
    NOTE_LINK,
    WEB_LINK,
    INLINE_MATH
}

internal data class SyntaxSpan(
    val type: SyntaxType,
    val rawStart: Int,
    val rawEnd: Int,
    val prefixLength: Int,
    val visualText: String,
    val suffixLength: Int,
    val mathTokens: List<InlineMathToken> = emptyList()
) {
    val visualLength: Int get() = visualText.length
    val rawLength: Int get() = rawEnd - rawStart
    val hiddenLength: Int get() = rawLength - visualLength
}

internal class SyntaxOffsetMapping(
    private val spans: List<SyntaxSpan>,
    private val originalLength: Int,
    private val transformedLength: Int
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalLength)
        var totalHiddenBefore = 0
        for (span in spans) {
            if (clamped <= span.rawStart) {
                break
            } else if (clamped < span.rawEnd) {
                val visualStart = span.rawStart - totalHiddenBefore
                val rawInnerLen = (span.rawLength - span.prefixLength - span.suffixLength).coerceAtLeast(1)
                val rawInnerOffset = (clamped - (span.rawStart + span.prefixLength)).coerceIn(0, rawInnerLen)
                val rel = if (rawInnerLen == span.visualLength) {
                    rawInnerOffset.coerceIn(0, span.visualLength)
                } else {
                    ((rawInnerOffset.toFloat() / rawInnerLen.toFloat()) * span.visualLength)
                        .toInt()
                        .coerceIn(0, span.visualLength)
                }
                return (visualStart + rel).coerceIn(0, transformedLength)
            } else {
                totalHiddenBefore += span.hiddenLength
            }
        }
        return (clamped - totalHiddenBefore).coerceIn(0, transformedLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedLength)
        var totalHidden = 0
        for (span in spans) {
            val visualStart = span.rawStart - totalHidden
            val visualEnd = visualStart + span.visualLength

            if (clamped <= visualStart) {
                break
            } else if (clamped < visualEnd) {
                val offsetInsideVisual = clamped - visualStart
                val rawInnerLen = (span.rawLength - span.prefixLength - span.suffixLength).coerceAtLeast(0)
                val rawInnerOffset = if (rawInnerLen == span.visualLength || span.visualLength == 0) {
                    offsetInsideVisual.coerceIn(0, rawInnerLen)
                } else {
                    ((offsetInsideVisual.toFloat() / span.visualLength.toFloat()) * rawInnerLen)
                        .toInt()
                        .coerceIn(0, rawInnerLen)
                }
                return (span.rawStart + span.prefixLength + rawInnerOffset).coerceIn(0, originalLength)
            } else {
                // Cuando clamped == visualEnd, suma el hiddenLength completo del span para que el cursor
                // quede situado en span.rawEnd (FUERA de la etiqueta de cierre ']', '==' o ')').
                totalHidden += span.hiddenLength
            }
        }
        return (clamped + totalHidden).coerceIn(0, originalLength)
    }
}

internal fun buildNoteRichAnnotatedString(
    fullText: String,
    isDark: Boolean,
    amber: Color
): androidx.compose.ui.text.AnnotatedString {
    return buildNoteRichTextPair(fullText, isDark, amber).first
}

private fun buildNoteRichTextPair(
    fullText: String,
    isDark: Boolean,
    amber: Color
): Pair<androidx.compose.ui.text.AnnotatedString, List<SyntaxSpan>> {
    val lightIndigo = Color(0xFF1A365D)
    val darkIndigo = Color(0xFF60A5FA)
    val tagColor = if (isDark) darkIndigo else lightIndigo
    val webLinkColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val webLinkBgColor = webLinkColor.copy(alpha = if (isDark) 0.14f else 0.10f)
    val mathColor = if (isDark) amber else Color(0xFFB45309)
    val mathBgColor = amber.copy(alpha = if (isDark) 0.16f else 0.14f)

    val highlightSpans = HIGHLIGHT_REGEX.findAll(fullText).map { match ->
        val rawStart = match.range.first
        val rawEnd = match.range.last + 1
        val innerText = match.groupValues[1]
        SyntaxSpan(
            type = SyntaxType.HIGHLIGHT,
            rawStart = rawStart,
            rawEnd = rawEnd,
            prefixLength = 2,
            visualText = innerText,
            suffixLength = 2
        )
    }

    val noteLinkSpans = NOTE_LINK_REGEX.findAll(fullText).map { match ->
        val rawStart = match.range.first
        val rawEnd = match.range.last + 1
        val titleText = match.groupValues[1]
        val fullMatchLen = match.value.length
        val prefixLen = 1 // '['
        val visualLen = titleText.length
        val suffixLen = fullMatchLen - prefixLen - visualLen // "](notejs://note/id)"
        SyntaxSpan(
            type = SyntaxType.NOTE_LINK,
            rawStart = rawStart,
            rawEnd = rawEnd,
            prefixLength = prefixLen,
            visualText = titleText,
            suffixLength = suffixLen
        )
    }

    val webLinkSpans = WEB_LINK_REGEX.findAll(fullText).map { match ->
        val rawStart = match.range.first
        val rawEnd = match.range.last + 1
        val titleText = match.groupValues[1]
        val fullMatchLen = match.value.length
        val prefixLen = 1 // '['
        val visualLen = titleText.length
        val suffixLen = fullMatchLen - prefixLen - visualLen // "](https://...)"
        SyntaxSpan(
            type = SyntaxType.WEB_LINK,
            rawStart = rawStart,
            rawEnd = rawEnd,
            prefixLength = prefixLen,
            visualText = titleText,
            suffixLength = suffixLen
        )
    }

    val mathSpans = INLINE_MATH_REGEX.findAll(fullText).mapNotNull { match ->
        val code = match.groupValues[1]
        if (code.isBlank()) return@mapNotNull null
        val rawStart = match.range.first
        val rawEnd = match.range.last + 1
        val prefixLen = 6 // "[math:"
        val suffixLen = 1 // "]"
        val parsedTokens = MathFormatter.parseInlineStyledTokens(code)
        val formattedVisual = if (parsedTokens.isNotEmpty()) {
            "\u2009" + parsedTokens.joinToString("") { it.text } + "\u2009"
        } else {
            MathFormatter.formatLatexToUnicode(code).ifBlank { code }
        }
        SyntaxSpan(
            type = SyntaxType.INLINE_MATH,
            rawStart = rawStart,
            rawEnd = rawEnd,
            prefixLength = prefixLen,
            visualText = formattedVisual,
            suffixLength = suffixLen,
            mathTokens = parsedTokens
        )
    }.toList()

    val allSpans = (highlightSpans + noteLinkSpans + webLinkSpans + mathSpans)
        .sortedBy { it.rawStart }
        .fold(mutableListOf<SyntaxSpan>()) { acc, span ->
            if (acc.isEmpty() || span.rawStart >= acc.last().rawEnd) {
                acc.add(span)
            }
            acc
        }

    val occupiedVisualRanges = mutableListOf<IntRange>()

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (span in allSpans) {
            if (span.rawStart > lastIndex) {
                val safeLast = lastIndex.coerceIn(0, fullText.length)
                val safeStart = span.rawStart.coerceIn(0, fullText.length)
                append(fullText.substring(safeLast, safeStart))
            }

            val visualStart = length
            append(span.visualText)
            val visualEnd = length
            occupiedVisualRanges.add(visualStart until visualEnd)

            when (span.type) {
                SyntaxType.HIGHLIGHT -> {
                    addStyle(
                        SpanStyle(background = amber.copy(alpha = 0.35f)),
                        visualStart,
                        visualEnd
                    )
                }
                SyntaxType.NOTE_LINK -> {
                    addStyle(
                        SpanStyle(
                            color = amber,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        visualStart,
                        visualEnd
                    )
                    val matchStr = fullText.substring(span.rawStart, span.rawEnd)
                    val noteIdStr = NOTE_LINK_REGEX.find(matchStr)?.groupValues?.getOrNull(2)
                    if (noteIdStr != null) {
                        addStringAnnotation(
                            tag = "NOTE_LINK",
                            annotation = noteIdStr,
                            start = visualStart,
                            end = visualEnd
                        )
                    }
                }
                SyntaxType.WEB_LINK -> {
                    addStyle(
                        SpanStyle(
                            color = webLinkColor,
                            background = webLinkBgColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        visualStart,
                        visualEnd
                    )
                    val matchStr = fullText.substring(span.rawStart, span.rawEnd)
                    val webUrl = WEB_LINK_REGEX.find(matchStr)?.groupValues?.getOrNull(2)
                    if (!webUrl.isNullOrBlank()) {
                        val normalized = normalizeDetectedWebUrl(webUrl)
                        addStringAnnotation(
                            tag = "WEB_LINK",
                            annotation = normalized,
                            start = visualStart,
                            end = visualEnd
                        )
                        addStringAnnotation(
                            tag = "URL",
                            annotation = normalized,
                            start = visualStart,
                            end = visualEnd
                        )
                    }
                }
                SyntaxType.INLINE_MATH -> {
                    val matchStr = fullText.substring(span.rawStart, span.rawEnd)
                    val latexCodeStr = INLINE_MATH_REGEX.find(matchStr)?.groupValues?.getOrNull(1) ?: span.visualText
                    addStringAnnotation(
                        tag = "INLINE_MATH",
                        annotation = "$latexCodeStr|${span.rawStart}|${span.rawEnd}",
                        start = visualStart,
                        end = visualEnd
                    )
                    addStyle(
                        SpanStyle(
                            color = mathColor,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            background = mathBgColor
                        ),
                        visualStart,
                        visualEnd
                    )
                    // Apply fine-grained mathematical typography (fractions with raised numerator & lowered denominator,
                    // superscripts, subscripts, radicals) across each parsed token.
                    if (span.mathTokens.isNotEmpty()) {
                        var tokenCursor = visualStart + 1 // skip leading '\u2009'
                        for (token in span.mathTokens) {
                            val tStart = tokenCursor.coerceIn(visualStart, visualEnd)
                            val tEnd = (tokenCursor + token.text.length).coerceIn(tStart, visualEnd)
                            if (tEnd > tStart) {
                                val tokenStyle = when (token.kind) {
                                    InlineMathTokenKind.FRAC_NUM -> SpanStyle(
                                        fontSize = 0.82.em,
                                        baselineShift = BaselineShift(0.32f),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic
                                    )
                                    InlineMathTokenKind.FRAC_SLASH -> SpanStyle(
                                        fontSize = 0.96.em,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    )
                                    InlineMathTokenKind.FRAC_DEN -> SpanStyle(
                                        fontSize = 0.82.em,
                                        baselineShift = BaselineShift(-0.18f),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic
                                    )
                                    InlineMathTokenKind.SUPERSCRIPT -> SpanStyle(
                                        fontSize = 0.75.em,
                                        baselineShift = BaselineShift(0.36f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Serif
                                    )
                                    InlineMathTokenKind.SUBSCRIPT -> SpanStyle(
                                        fontSize = 0.75.em,
                                        baselineShift = BaselineShift(-0.20f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Serif
                                    )
                                    InlineMathTokenKind.RADICAL -> SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    )
                                    InlineMathTokenKind.RADICAND -> SpanStyle(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic
                                    )
                                    else -> null
                                }
                                if (tokenStyle != null) {
                                    addStyle(tokenStyle, tStart, tEnd)
                                }
                            }
                            tokenCursor += token.text.length
                        }
                    }
                }
            }

            lastIndex = span.rawEnd
        }
        if (lastIndex < fullText.length) {
            val safeLast = lastIndex.coerceIn(0, fullText.length)
            append(fullText.substring(safeLast))
        }

        // Aplicar estilo de hashtags y URLs crudas sobre el texto visual
        val visualString = toAnnotatedString().text
        for (m in HASHTAG_REGEX.findAll(visualString)) {
            if (occupiedVisualRanges.any { m.range.first < it.last + 1 && m.range.last >= it.first }) {
                continue
            }
            addStyle(
                SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                m.range.first,
                m.range.last + 1
            )
        }
        for (m in RAW_URL_VISUAL_REGEX.findAll(visualString)) {
            if (occupiedVisualRanges.any { m.range.first < it.last + 1 && m.range.last >= it.first }) {
                continue
            }
            val cleanedRaw = m.value.trimEnd('.', ',', ';', '!', '?', ')', ']')
            if (cleanedRaw.length >= 5 && cleanedRaw.contains('.')) {
                val vStart = m.range.first
                val vEnd = (vStart + cleanedRaw.length).coerceAtMost(visualString.length)
                val normalized = normalizeDetectedWebUrl(cleanedRaw)
                addStyle(
                    SpanStyle(
                        color = webLinkColor,
                        background = webLinkBgColor,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    ),
                    vStart,
                    vEnd
                )
                addStringAnnotation(
                    tag = "WEB_LINK",
                    annotation = normalized,
                    start = vStart,
                    end = vEnd
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = normalized,
                    start = vStart,
                    end = vEnd
                )
            }
        }
    }

    return annotatedString to allSpans
}

internal fun markdownVisualTransformation(isDark: Boolean, amber: Color): VisualTransformation {
    return VisualTransformation { text ->
        val fullText = text.text
        val (annotatedString, allSpans) = buildNoteRichTextPair(fullText, isDark, amber)

        val offsetMapping = if (allSpans.isEmpty()) {
            OffsetMapping.Identity
        } else {
            SyntaxOffsetMapping(
                spans = allSpans,
                originalLength = fullText.length,
                transformedLength = annotatedString.length
            )
        }
        TransformedText(annotatedString, offsetMapping)
    }
}

internal fun Modifier.onNoteLinkClick(
    textLayoutResult: TextLayoutResult?,
    onNavigateToNote: (Long) -> Unit
): Modifier = this.pointerInput(textLayoutResult) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null) {
            val layout = textLayoutResult ?: return@awaitEachGesture
            if (layout.layoutInput.text.isEmpty()) return@awaitEachGesture
            val offset = layout.getOffsetForPosition(up.position)
            val startQuery = (offset - 1).coerceAtLeast(0)
            val annotations = layout.layoutInput.text.getStringAnnotations("NOTE_LINK", startQuery, offset)
            val noteId = annotations.firstOrNull()?.item?.toLongOrNull()
            if (noteId != null) {
                up.consume()
                onNavigateToNote(noteId)
            }
        }
    }
}

internal fun Modifier.onWebLinkClick(
    textLayoutResult: TextLayoutResult?,
    onOpenWebLink: (String) -> Unit
): Modifier = this.pointerInput(textLayoutResult) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null) {
            val layout = textLayoutResult ?: return@awaitEachGesture
            if (layout.layoutInput.text.isEmpty()) return@awaitEachGesture
            val offset = layout.getOffsetForPosition(up.position)
            val line = layout.getLineForOffset(offset.coerceIn(0, layout.layoutInput.text.length))
            val lineLeft = layout.getLineLeft(line)
            val lineRight = layout.getLineRight(line)
            if (up.position.x < lineLeft - 12f || up.position.x > lineRight + 12f) {
                return@awaitEachGesture
            }
            val startQuery = (offset - 1).coerceAtLeast(0)
            val annotations = layout.layoutInput.text
                .getStringAnnotations("WEB_LINK", startQuery, offset)
                .ifEmpty { layout.layoutInput.text.getStringAnnotations("URL", startQuery, offset) }
            val webUrl = annotations.firstOrNull()?.item
            if (!webUrl.isNullOrBlank()) {
                up.consume()
                onOpenWebLink(webUrl)
            }
        }
    }
}

internal fun Modifier.onInlineMathClick(
    textLayoutResult: TextLayoutResult?,
    onEditInlineMath: (code: String, rawStart: Int, rawEnd: Int) -> Unit
): Modifier = this.pointerInput(textLayoutResult) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial)
        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        if (up != null) {
            val layout = textLayoutResult ?: return@awaitEachGesture
            if (layout.layoutInput.text.isEmpty()) return@awaitEachGesture
            val offset = layout.getOffsetForPosition(up.position)
            val line = layout.getLineForOffset(offset.coerceIn(0, layout.layoutInput.text.length))
            val lineLeft = layout.getLineLeft(line)
            val lineRight = layout.getLineRight(line)
            // Evitar abrir el editor si el usuario tocó el espacio vacío a la derecha de la línea
            if (up.position.x < lineLeft - 8f || up.position.x > lineRight + 8f) {
                return@awaitEachGesture
            }
            val startQuery = (offset - 1).coerceAtLeast(0)
            val annotations = layout.layoutInput.text.getStringAnnotations("INLINE_MATH", startQuery, offset)
            val itemStr = annotations.firstOrNull()?.item
            if (itemStr != null) {
                val lastPipe = itemStr.lastIndexOf('|')
                val secondLastPipe = if (lastPipe > 0) itemStr.lastIndexOf('|', lastPipe - 1) else -1
                if (lastPipe != -1 && secondLastPipe != -1) {
                    val code = itemStr.substring(0, secondLastPipe)
                    val start = itemStr.substring(secondLastPipe + 1, lastPipe).toIntOrNull() ?: 0
                    val end = itemStr.substring(lastPipe + 1).toIntOrNull() ?: 0
                    up.consume()
                    onEditInlineMath(code, start, end)
                }
            }
        }
    }
}

internal data class DetectedInlineMathItem(
    val code: String,
    val rawStart: Int,
    val rawEnd: Int
)

/**
 * Muestra chips interactivos con renderizado matemático 2D real (fracciones verticales, raíces con barra
 * superior, integrales, sumatorias, matrices) para las ecuaciones `[math:...]` presentes en el bloque.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InlineMathPreviewRow(
    text: String,
    isDark: Boolean,
    amber: Color,
    onEditInlineMath: (code: String, rawStart: Int, rawEnd: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val mathItems = remember(text) {
        INLINE_MATH_REGEX.findAll(text).mapNotNull { match ->
            val code = match.groupValues[1].trim()
            if (code.isEmpty()) null
            else DetectedInlineMathItem(
                code = code,
                rawStart = match.range.first,
                rawEnd = match.range.last + 1
            )
        }.toList()
    }

    if (mathItems.isEmpty()) return

    val cardBg = if (isDark) Color(0xFF1F1F24) else Color(0xFFF7F5F0)
    val borderColor = amber.copy(alpha = if (isDark) 0.42f else 0.50f)
    val mathColor = if (isDark) amber else Color(0xFFB45309)

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        mathItems.forEach { item ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onEditInlineMath(item.code, item.rawStart, item.rawEnd)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "Ecuación",
                        tint = amber.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    NativeMathView(
                        expression = item.code,
                        isDark = isDark,
                        fontSize = 15.sp,
                        color = mathColor,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ecuación",
                        tint = mathColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun buildInlineMathMap(
    text: String,
    onEditInlineMath: (code: String, rawStart: Int, rawEnd: Int) -> Unit
): Map<String, InlineTextContent> {
    return emptyMap()
}
