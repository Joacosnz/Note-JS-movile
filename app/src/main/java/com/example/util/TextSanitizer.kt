package com.example.util

/**
 * Cleans internal markdown formatting tags like [math:code], ==highlight==,
 * and [title](url) to display pure, human-readable text in previews and cards.
 */
fun cleanMarkdownSyntax(text: String): String {
    if (text.isBlank()) return ""
    return text
        .replace(Regex("""\[math:(.+?)\]""")) { it.groupValues[1].trim() }
        .replace(Regex("""==([^=\n]+)==""")) { it.groupValues[1] }
        .replace(Regex("""\[([^\]]+)\]\(notejs://note/[^\)]+\)""")) { it.groupValues[1] }
        .replace(Regex("""\[([^\]]+)\]\((?:https?://|www\.)[^\s\)]+\)""", RegexOption.IGNORE_CASE)) { it.groupValues[1] }
}

/**
 * Sanitizes preview text and snippets for list views and cards,
 * removing any hyphen (-), bullet (•), or bullet-like symbols
 * and internal formatting tags to ensure clean, continuous text presentation.
 */
fun sanitizeNoteSnippet(text: String): String {
    if (text.isBlank()) return ""
    val cleaned = cleanMarkdownSyntax(text)
    return cleaned
        .replace(Regex("[•\\-*⁃–—]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

