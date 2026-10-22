package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.NoteBlock
import com.example.ui.components.editor.NotionMathBlockItem

/**
 * Componente puente para mantener compatibilidad hacia atrás con referencias existentes,
 * delegando completamente a la implementación de ecuación en bloque estilo Notion.
 */
@Composable
fun MathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val amber = Color(0xFFFFB800)
    NotionMathBlockItem(
        block = block,
        onUpdateBlock = onUpdateBlock,
        onDeleteBlock = onDeleteBlock,
        isDark = isDark,
        amber = amber,
        modifier = modifier
    )
}
