package com.example.util

import java.util.Locale

object HashtagExtractor {
    // Detecta palabras precedidas por # (ej. #trabajo, #diseño, #reunión_2026)
    private val HASHTAG_REGEX = Regex("""#([a-zA-Z0-9_\u00C0-\u017E]+)""")

    // Extrae todos los hashtags sin el símbolo '#'
    fun extractHashtags(vararg texts: String?): Set<String> {
        val tags = linkedSetOf<String>()
        texts.forEach { text ->
            if (!text.isNullOrBlank()) {
                HASHTAG_REGEX.findAll(text).forEach { matchResult ->
                    val tag = matchResult.groupValues[1].trim()
                    if (tag.isNotEmpty()) {
                        tags.add(tag)
                    }
                }
            }
        }
        return tags
    }

    // Combina etiquetas manuales y hashtags detectados en un string separado por comas
    fun combineTags(manualTags: String, vararg contents: String?): String {
        val extracted = extractHashtags(*contents)
        val manual = manualTags.split(",")
            .map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotEmpty() }
        return (manual + extracted).distinct().joinToString(",")
    }

    // Comprueba si una nota coincide con una etiqueta seleccionada
    fun noteMatchesTag(noteTags: String, noteTitle: String, noteContent: String, targetTag: String): Boolean {
        if (targetTag.equals("Todas", ignoreCase = true)) return true
        val cleanTarget = targetTag.trim().removePrefix("#").lowercase(Locale.getDefault())
        if (cleanTarget.isEmpty()) return true

        val extracted = extractHashtags(noteTitle, noteContent).map { it.lowercase(Locale.getDefault()) }
        val stored = noteTags.split(",")
            .map { it.trim().removePrefix("#").lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }

        return extracted.contains(cleanTarget) || stored.contains(cleanTarget)
    }
}
