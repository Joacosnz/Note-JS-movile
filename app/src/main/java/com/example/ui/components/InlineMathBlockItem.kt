package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.NoteBlock
import com.example.ui.components.editor.NotionMathBlockItem

/**
 * Componente puente para mantener compatibilidad hacia atrás,
 * delegando completamente a la implementación de ecuación en bloque estilo Notion.
 */
@Composable
fun InlineMathBlockItem(
    block: NoteBlock.Math,
    onUpdateBlock: (NoteBlock.Math) -> Unit,
    onDeleteBlock: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
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
