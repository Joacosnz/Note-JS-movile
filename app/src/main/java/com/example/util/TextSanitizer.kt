package com.example.util

/**
 * Sanitizes preview text and snippets for list views and cards,
 * removing any hyphen (-), bullet (•), or bullet-like symbols
 * to ensure clean, continuous text presentation.
 */
fun sanitizeNoteSnippet(text: String): String {
    if (text.isBlank()) return ""
    return text
        .replace(Regex("[•\\-*⁃–—]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}
