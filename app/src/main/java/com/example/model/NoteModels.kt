package com.example.model

import androidx.compose.ui.graphics.Color

enum class NoteType(val label: String) {
    TEXT("Texto"),
    CHECKLIST("Lista"),
    DRAWING("Dibujo"),
    MATH("Cálculo")
}

enum class PaperStyle(val title: String) {
    BLANK("Liso"),
    RULED("Pautado"),
    GRID("Cuadrícula"),
    DOTS("Puntos")
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
)

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val colorHex: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class NoteThemeColor(
    val id: Int,
    val name: String,
    val lightBg: Color,
    val lightBorder: Color,
    val darkBg: Color,
    val darkBorder: Color,
    val chipColor: Color
)

object NoteColors {
    val options = listOf(
        NoteThemeColor(
            id = 0,
            name = "Predeterminado",
            lightBg = Color(0xFFFFFFFF),
            lightBorder = Color(0xFFE2E8F0),
            darkBg = Color(0xFF1E293B),
            darkBorder = Color(0xFF334155),
            chipColor = Color(0xFF94A3B8)
        ),
        NoteThemeColor(
            id = 1,
            name = "Coral",
            lightBg = Color(0xFFFFECE8),
            lightBorder = Color(0xFFFFCDD2),
            darkBg = Color(0xFF4A1E22),
            darkBorder = Color(0xFF6B2D33),
            chipColor = Color(0xFFEF5350)
        ),
        NoteThemeColor(
            id = 2,
            name = "Ámbar",
            lightBg = Color(0xFFFFF8E1),
            lightBorder = Color(0xFFFFECB3),
            darkBg = Color(0xFF423819),
            darkBorder = Color(0xFF615324),
            chipColor = Color(0xFFFFB300)
        ),
        NoteThemeColor(
            id = 3,
            name = "Menta",
            lightBg = Color(0xFFE8F5E9),
            lightBorder = Color(0xFFC8E6C9),
            darkBg = Color(0xFF1C3D2B),
            darkBorder = Color(0xFF28563D),
            chipColor = Color(0xFF66BB6A)
        ),
        NoteThemeColor(
            id = 4,
            name = "Cielo",
            lightBg = Color(0xFFE1F5FE),
            lightBorder = Color(0xFFB3E5FC),
            darkBg = Color(0xFF16374D),
            darkBorder = Color(0xFF214E6D),
            chipColor = Color(0xFF29B6F6)
        ),
        NoteThemeColor(
            id = 5,
            name = "Lavanda",
            lightBg = Color(0xFFF3E5F5),
            lightBorder = Color(0xFFE1BEE7),
            darkBg = Color(0xFF3A1E43),
            darkBorder = Color(0xFF542C61),
            chipColor = Color(0xFFAB47BC)
        ),
        NoteThemeColor(
            id = 6,
            name = "Rosa",
            lightBg = Color(0xFFFCE4EC),
            lightBorder = Color(0xFFF8BBD0),
            darkBg = Color(0xFF451928),
            darkBorder = Color(0xFF63253B),
            chipColor = Color(0xFFEC407A)
        ),
        NoteThemeColor(
            id = 7,
            name = "Arena",
            lightBg = Color(0xFFEFEBE9),
            lightBorder = Color(0xFFD7CCC8),
            darkBg = Color(0xFF332D2B),
            darkBorder = Color(0xFF4E4542),
            chipColor = Color(0xFF8D6E63)
        ),
        NoteThemeColor(
            id = 8,
            name = "Esmeralda",
            lightBg = Color(0xFFE0F2F1),
            lightBorder = Color(0xFFB2DFDB),
            darkBg = Color(0xFF133935),
            darkBorder = Color(0xFF1E524D),
            chipColor = Color(0xFF26A69A)
        )
    )

    fun getColor(id: Int, isDark: Boolean): Color {
        val theme = options.firstOrNull { it.id == id } ?: options[0]
        return if (isDark) theme.darkBg else theme.lightBg
    }

    fun getBorderColor(id: Int, isDark: Boolean): Color {
        val theme = options.firstOrNull { it.id == id } ?: options[0]
        return if (isDark) theme.darkBorder else theme.lightBorder
    }
}
