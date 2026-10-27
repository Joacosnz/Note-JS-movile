package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

const val DEFAULT_NOTE_JS_ACCENT_HEX = "#EAB308"
val DefaultNoteJsAmber = Color(0xFFEAB308)

data class NoteJsAccentPreset(
    val name: String,
    val hex: String,
    val color: Color
)

val NoteJsAccentPresets = listOf(
    NoteJsAccentPreset("Amarillo", DEFAULT_NOTE_JS_ACCENT_HEX, DefaultNoteJsAmber),
    NoteJsAccentPreset("Violeta", "#8B5CF6", Color(0xFF8B5CF6)),
    NoteJsAccentPreset("Morado", "#AF52DE", Color(0xFFAF52DE)),
    NoteJsAccentPreset("Índigo", "#5856D6", Color(0xFF5856D6)),
    NoteJsAccentPreset("Azul", "#007AFF", Color(0xFF007AFF)),
    NoteJsAccentPreset("Cian", "#06B6D4", Color(0xFF06B6D4)),
    NoteJsAccentPreset("Menta", "#00C7BE", Color(0xFF00C7BE)),
    NoteJsAccentPreset("Verde", "#34C759", Color(0xFF34C759)),
    NoteJsAccentPreset("Naranja", "#FF9500", Color(0xFFFF9500)),
    NoteJsAccentPreset("Rosa", "#FF2D55", Color(0xFFFF2D55)),
    NoteJsAccentPreset("Coral", "#FB7185", Color(0xFFFB7185)),
    NoteJsAccentPreset("Rojo", "#EF4444", Color(0xFFEF4444))
)

private var _noteJsAccentColor by mutableStateOf(DefaultNoteJsAmber)

// Note JS iconic primary tint (reactive across the entire app)
val NoteJsAmber: Color
    get() = _noteJsAccentColor

val NoteJsAmberDark: Color
    get() = _noteJsAccentColor

fun setNoteJsAccentColor(color: Color) {
    _noteJsAccentColor = color
}

fun parseNoteJsAccentHex(hex: String): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val parsed = clean.toLong(16)
        Color(0xFF000000L or (parsed and 0xFFFFFFL))
    } catch (_: Exception) {
        DefaultNoteJsAmber
    }
}

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

fun getNoteJsAccentColorName(hex: String): String {
    val normalized = "#" + hex.trim().removePrefix("#").uppercase()
    if (normalized.equals(DEFAULT_NOTE_JS_ACCENT_HEX, ignoreCase = true)) {
        return "Predeterminado"
    }
    val match = NoteJsAccentPresets.firstOrNull { it.hex.equals(normalized, ignoreCase = true) }
    return match?.name ?: normalized
}

// Note JS iOS system surfaces
val NoteJsSystemBgLight = Color(0xFFF2F2F7)
val NoteJsSystemBgDark = Color(0xFF000000)

val NoteJsCardBgLight = Color(0xFFFFFFFF)
val NoteJsCardBgDark = Color(0xFF1C1C1E)

val NoteJsSecondaryCardDark = Color(0xFF2C2C2E)
val NoteJsSearchBarLight = Color(0xFFE5E5EA)
val NoteJsSearchBarDark = Color(0xFF1C1C1E)

val NoteJsSeparatorLight = Color(0xFFE5E5EA)
val NoteJsSeparatorDark = Color(0xFF38383A)

val NoteJsTextSecondaryLight = Color(0xFF8E8E93)
val NoteJsTextSecondaryDark = Color(0xFF98989D)

// Note JS 5 calibrated highlight colors (from Note JS specifications)
val NoteJsHighlightPurple = Color(0xFFAF52DE)
val NoteJsHighlightPink = Color(0xFFFF2D55)
val NoteJsHighlightOrange = Color(0xFFFF9500)
val NoteJsHighlightMint = Color(0xFF00C7BE)
val NoteJsHighlightBlue = Color(0xFF007AFF)
