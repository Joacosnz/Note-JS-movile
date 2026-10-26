package com.example.util

import com.example.data.local.entity.NoteEntity
import com.example.model.NoteBlock
import com.example.model.TextStyleType

object OnboardingNoteHelper {

    fun createWelcomeNote(): NoteEntity {
        val blocks = mutableListOf<NoteBlock>()

        blocks.add(
            NoteBlock.Text(
                content = "1. Un editor por bloques",
                style = TextStyleType.SUBTITLE
            )
        )
        blocks.add(
            NoteBlock.Text(
                content = "Note JS no es un bloc de notas común. Cada párrafo, ecuación o lista es un bloque independiente que puedes ordenar como quieras.",
                style = TextStyleType.BODY
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Toca esta casilla para marcar la tarea como completada.",
                isChecked = false
            )
        )
        blocks.add(
            NoteBlock.Checklist(
                content = "Puedes crear listas de compras o hábitos diarios.",
                isChecked = false
            )
        )

        blocks.add(
            NoteBlock.Text(
                content = "2. Cuadros Destacados",
                style = TextStyleType.SUBTITLE
            )
        )
        blocks.add(
            NoteBlock.CalloutBox(
                content = "Mantén presionado o toca las opciones de este bloque para cambiar su color, tipografía o ícono.",
                emoji = "💡",
                backgroundColorHex = "#FFD700" // Amarillo / gold
            )
        )

        blocks.add(
            NoteBlock.Text(
                content = "3. Matemáticas en tiempo real",
                style = TextStyleType.SUBTITLE
            )
        )
        blocks.add(
            NoteBlock.Text(
                content = "Escribe fórmulas directamente en el texto encerrándolas entre signos de dólar, por ejemplo: La fórmula general es \$x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}\$ y se renderizará automáticamente.",
                style = TextStyleType.BODY
            )
        )
        blocks.add(
            NoteBlock.Math(
                latexCode = "\\begin{bmatrix} 1 & 2 & 3 \\\\ 4 & 5 & 6 \\\\ 7 & 8 & 9 \\end{bmatrix}"
            )
        )

        blocks.add(
            NoteBlock.Text(
                content = "4. Productividad Pura",
                style = TextStyleType.SUBTITLE
            )
        )
        blocks.add(
            NoteBlock.Text(
                content = "• Usa el ícono del micrófono abajo para dictar texto sin teclear.\n• Al crear recordatorios, usa #hashtags como #Trabajo o #Estudio para organizar automáticamente tus listas inteligentes.",
                style = TextStyleType.BODY
            )
        )

        val serializedContent = NoteBlockSerializer.serialize(blocks)

        return NoteEntity(
            title = "¡Bienvenido a Note JS! 🚀",
            content = serializedContent,
            icon = "👋",
            noteType = "TEXT",
            isPinned = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastModifiedLocal = System.currentTimeMillis()
        )
    }
}
