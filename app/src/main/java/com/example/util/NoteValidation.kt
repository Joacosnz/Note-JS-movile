package com.example.util

import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock

/**
 * Determina si un bloque de nota contiene información real.
 */
fun NoteBlock.hasContent(): Boolean {
    return when (this) {
        is NoteBlock.Text -> content.isNotBlank()
        is NoteBlock.Checklist -> content.isNotBlank() || isChecked
        is NoteBlock.Table -> matrix.any { row -> row.any { cell -> cell.text.isNotBlank() } }
        else -> true // PdfBlock, AudioBlock, DrawingBlock, CalloutBox, SimpleList, etc.
    }
}

/**
 * Valida si una lista de bloques contiene información real en al menos uno de ellos.
 */
fun List<NoteBlock>.hasContent(): Boolean {
    return any { it.hasContent() }
}

/**
 * Valida si un título y lista de bloques representan una nota válida para guardar o sincronizar.
 */
fun isValidNoteForSync(title: String, blocks: List<NoteBlock>): Boolean {
    return title.isNotBlank() || blocks.hasContent()
}

/**
 * Función de extensión para [NoteEntity] que determina si la nota es válida para sincronización o guardado.
 * La nota debe sincronizarse SI el título no está en blanco O SI al menos uno de sus bloques contiene información real.
 */
fun NoteEntity.isValidForSync(
    blocks: List<NoteBlock> = NoteBlockSerializer.deserialize(this.content)
): Boolean {
    return isValidNoteForSync(this.title, blocks)
}
