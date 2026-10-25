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
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false,
    val reminderTimestamp: Long? = null,
    val isReminderDone: Boolean = false,
    val priority: Int = 0 // 0=Ninguna, 1=Baja, 2=Media, 3=Urgente
)

/**
 * Modelo unificado que representa un recordatorio de lista o de nota general con enlace profundo a la nota de origen.
 */
data class UnifiedReminderItem(
    val id: String, // ID único: "${noteId}_${checkItemId}" o "${noteId}_note"
    val noteId: Long,
    val noteTitle: String,
    val noteColorIndex: Int,
    val noteFolder: String,
    val checkItemId: String? = null, // null si corresponde al recordatorio de la nota completa
    val title: String,
    val isCompleted: Boolean,
    val reminderTimestamp: Long,
    val priority: Int = 0
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
            lightBorder = Color(0xFFE0E0E0),
            darkBg = Color(0xFF202124),
            darkBorder = Color(0xFF5F6368),
            chipColor = Color.Transparent
        ),
        NoteThemeColor(
            id = 1,
            name = "Coral",
            lightBg = Color(0xFFF28B82),
            lightBorder = Color(0xFFF28B82),
            darkBg = Color(0xFF5C2B29),
            darkBorder = Color(0xFF753634),
            chipColor = Color(0xFFF28B82)
        ),
        NoteThemeColor(
            id = 2,
            name = "Melocotón",
            lightBg = Color(0xFFFBBC04),
            lightBorder = Color(0xFFFBBC04),
            darkBg = Color(0xFF614A19),
            darkBorder = Color(0xFF7C5F20),
            chipColor = Color(0xFFFBBC04)
        ),
        NoteThemeColor(
            id = 3,
            name = "Arena",
            lightBg = Color(0xFFFFF475),
            lightBorder = Color(0xFFFFF475),
            darkBg = Color(0xFF5B4E28),
            darkBorder = Color(0xFF746433),
            chipColor = Color(0xFFFFF475)
        ),
        NoteThemeColor(
            id = 4,
            name = "Menta",
            lightBg = Color(0xFFCCFF90),
            lightBorder = Color(0xFFCCFF90),
            darkBg = Color(0xFF345920),
            darkBorder = Color(0xFF43732A),
            chipColor = Color(0xFFCCFF90)
        ),
        NoteThemeColor(
            id = 5,
            name = "Salvia",
            lightBg = Color(0xFFA7FFEB),
            lightBorder = Color(0xFFA7FFEB),
            darkBg = Color(0xFF16504B),
            darkBorder = Color(0xFF1E6C65),
            chipColor = Color(0xFFA7FFEB)
        ),
        NoteThemeColor(
            id = 6,
            name = "Niebla",
            lightBg = Color(0xFFCBF0F8),
            lightBorder = Color(0xFFCBF0F8),
            darkBg = Color(0xFF2D555E),
            darkBorder = Color(0xFF3B6E7A),
            chipColor = Color(0xFFCBF0F8)
        ),
        NoteThemeColor(
            id = 7,
            name = "Tormenta",
            lightBg = Color(0xFFAECBFA),
            lightBorder = Color(0xFFAECBFA),
            darkBg = Color(0xFF1E3A5F),
            darkBorder = Color(0xFF284C7C),
            chipColor = Color(0xFFAECBFA)
        ),
        NoteThemeColor(
            id = 8,
            name = "Atardecer",
            lightBg = Color(0xFFD7AEFB),
            lightBorder = Color(0xFFD7AEFB),
            darkBg = Color(0xFF42275E),
            darkBorder = Color(0xFF56337B),
            chipColor = Color(0xFFD7AEFB)
        ),
        NoteThemeColor(
            id = 9,
            name = "Flor",
            lightBg = Color(0xFFFDCFE8),
            lightBorder = Color(0xFFFDCFE8),
            darkBg = Color(0xFF5B2245),
            darkBorder = Color(0xFF762C5A),
            chipColor = Color(0xFFFDCFE8)
        ),
        NoteThemeColor(
            id = 10,
            name = "Arcilla",
            lightBg = Color(0xFFE6C9A8),
            lightBorder = Color(0xFFE6C9A8),
            darkBg = Color(0xFF442F19),
            darkBorder = Color(0xFF593E21),
            chipColor = Color(0xFFE6C9A8)
        ),
        NoteThemeColor(
            id = 11,
            name = "Tiza",
            lightBg = Color(0xFFE8EAED),
            lightBorder = Color(0xFFE8EAED),
            darkBg = Color(0xFF3C3F41),
            darkBorder = Color(0xFF4F5356),
            chipColor = Color(0xFFE8EAED)
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

    fun getContentColor(id: Int, isDark: Boolean): Color {
        return if (isDark) Color(0xFFE8EAED) else Color(0xFF202124)
    }

    fun getSecondaryTextColor(id: Int, isDark: Boolean): Color {
        return if (id == 0) {
            if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
        } else {
            if (isDark) Color(0xFFBDC1C6) else Color(0xFF3C4043)
        }
    }

    fun isCustomColor(id: Int): Boolean = id > 0
}

/**
 * Typealias for 'Note' referencing the Room persistence entity [com.example.data.local.entity.NoteEntity].
 */
typealias Note = com.example.data.local.entity.NoteEntity
