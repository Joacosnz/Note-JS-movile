package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

internal val WORD_COUNT_REGEX = Regex("""\s+""")
internal val EMPTY_HIGHLIGHT_REGEX = Regex("""==\s*==|====|===""")
internal val MALFORMED_HIGHLIGHT_1_REGEX = Regex("""==([^=\n]+)=(?!=)""")
internal val MALFORMED_HIGHLIGHT_2_REGEX = Regex("""(?<!=)=([^=\n]+)==""")
internal val MALFORMED_HIGHLIGHT_3_REGEX = Regex("""(?<!=)=([^=\n]+)=(?![=])""")
internal val HIGHLIGHT_REGEX = Regex("""==([^=\n]+)==""")
internal val HASHTAG_REGEX = Regex("""#([a-zA-Z0-9_áéíóúÁÉÍÓÚñÑ]+)""")
internal val NOTE_LINK_REGEX = Regex("""\[([^\]]+)\]\(notejs://note/([^\)]+)\)""")

internal fun sanitizeHighlightTags(text: String): String {
    var result = text
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
    val rawText = tfv.text
    val sanitized = sanitizeHighlightTags(rawText)
    val start = tfv.selection.start.coerceIn(0, sanitized.length)
    val end = tfv.selection.end.coerceIn(0, sanitized.length)
    val coercedRange = if (start <= end) TextRange(start, end) else TextRange(end, start)
    if (sanitized == rawText && tfv.selection == coercedRange) {
        return tfv
    }
    return tfv.copy(
        text = sanitized,
        selection = coercedRange
    )
}

internal enum class SyntaxType {
    HIGHLIGHT,
    NOTE_LINK
}

internal data class SyntaxSpan(
    val type: SyntaxType,
    val rawStart: Int,
    val rawEnd: Int,
    val prefixLength: Int,
    val visualText: String,
    val suffixLength: Int
) {
    val visualLength: Int get() = visualText.length
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
            } else if (clamped <= span.rawStart + span.prefixLength) {
                val visualStart = span.rawStart - totalHiddenBefore
                return visualStart.coerceIn(0, transformedLength)
            } else if (clamped <= span.rawStart + span.prefixLength + span.visualLength) {
                val hiddenInThisSpan = span.prefixLength
                return (clamped - totalHiddenBefore - hiddenInThisSpan).coerceIn(0, transformedLength)
            } else if (clamped <= span.rawEnd) {
                val visualEnd = span.rawStart + span.prefixLength + span.visualLength - totalHiddenBefore - span.prefixLength
                return visualEnd.coerceIn(0, transformedLength)
            } else {
                totalHiddenBefore += (span.prefixLength + span.suffixLength)
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
                return (span.rawStart + span.prefixLength + offsetInsideVisual).coerceIn(0, originalLength)
            } else {
                totalHidden += (span.prefixLength + span.suffixLength)
            }
        }
        return (clamped + totalHidden).coerceIn(0, originalLength)
    }
}

internal fun markdownVisualTransformation(isDark: Boolean, amber: Color): VisualTransformation {
    val lightIndigo = Color(0xFF1A365D)
    val darkIndigo = Color(0xFF60A5FA)
    val tagColor = if (isDark) darkIndigo else lightIndigo

    return VisualTransformation { text ->
        val fullText = text.text

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

        val allSpans = (highlightSpans + noteLinkSpans)
            .sortedBy { it.rawStart }
            .fold(mutableListOf<SyntaxSpan>()) { acc, span ->
                if (acc.isEmpty() || span.rawStart >= acc.last().rawEnd) {
                    acc.add(span)
                }
                acc
            }

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
                }

                lastIndex = span.rawEnd
            }
            if (lastIndex < fullText.length) {
                val safeLast = lastIndex.coerceIn(0, fullText.length)
                append(fullText.substring(safeLast))
            }

            // Aplicar estilo de hashtags sobre el texto visual
            val visualString = toAnnotatedString().text
            for (m in HASHTAG_REGEX.findAll(visualString)) {
                addStyle(
                    SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                    m.range.first,
                    m.range.last + 1
                )
            }
        }

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
            val offset = layout.getOffsetForPosition(up.position)
            val annotations = layout.layoutInput.text.getStringAnnotations("NOTE_LINK", offset, offset)
            val noteId = annotations.firstOrNull()?.item?.toLongOrNull()
            if (noteId != null) {
                up.consume()
                onNavigateToNote(noteId)
            }
        }
    }
}
