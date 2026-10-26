package com.example.model

import java.util.UUID

enum class TextStyleType {
    TITLE,
    SUBTITLE,
    BODY
}

data class TableCell(
    var text: String = "",
    var backgroundColorHex: String? = null // Ej: "#EF4444" (Rojo), null para transparente
)

data class PdfDrawingStroke(
    val points: List<Pair<Float, Float>>, // Coordenadas normalizadas (0.0f a 1.0f) relativas al ancho y alto de la página
    val colorHex: String,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false   // true = trazo semitransparente para resaltar texto
)

data class SignatureStamp(
    val id: String = UUID.randomUUID().toString(),
    val points: List<Pair<Float, Float>>,
    var normalizedX: Float,               // Posición X en la página (0f a 1f)
    var normalizedY: Float,               // Posición Y en la página (0f a 1f)
    var widthRatio: Float = 0.35f,        // Ancho relativo a la página
    var heightRatio: Float = 0.15f
)

typealias PdfBlock = NoteBlock.Pdf
typealias ImageBlock = NoteBlock.ImageBlock
typealias AudioBlock = NoteBlock.Audio

sealed interface NoteBlock {
    val id: String

    data class Audio(
        override val id: String = UUID.randomUUID().toString(),
        val filePath: String,                   // Ruta absoluta local en filesDir
        var transcription: String? = "",        // Texto transcrito
        var remoteUrl: String = "",             // URL de descarga de Firebase Cloud Storage
        val durationMs: Long = 0L,              // Duración total en milisegundos
        var isTranscribing: Boolean = false,    // true mientras se procesa el audio
        var isTranscriptionVisible: Boolean = false // Controla si se muestra u oculta el texto
    ) : NoteBlock

    data class Pdf(
        override val id: String = UUID.randomUUID().toString(),
        val filePath: String,              // Ruta local absoluta en filesDir
        var remoteUrl: String = "",        // URL de descarga de Firebase Cloud Storage
        val fileName: String,              // Nombre del archivo (ej. "Contrato.pdf")
        val fileSizeFormatted: String = "",// Format: "2.4 MB"
        val pageCount: Int,                // Total de páginas del PDF
        var currentPageIndex: Int = 0,     // Índice de página visible (inicia en 0)
        var isExpanded: Boolean = true,    // true = ver página grande, false = tarjeta compacta
        var annotationsPerPage: MutableMap<Int, List<PdfDrawingStroke>> = mutableMapOf(),
        var signaturesPerPage: MutableMap<Int, List<SignatureStamp>> = mutableMapOf(),
        var indexedText: String = ""       // Texto extraído del documento para el motor de búsqueda
    ) : NoteBlock

    data class ImageBlock(
        override val id: String = UUID.randomUUID().toString(),
        val localUri: String = "",
        val remoteUrl: String = ""
    ) : NoteBlock

    data class Image(
        override val id: String = UUID.randomUUID().toString(),
        val filePath: String,              // Ruta local absoluta
        var remoteUrl: String = "",        // URL de descarga de Firebase Cloud Storage
        val fileName: String = ""          // Nombre del archivo de imagen
    ) : NoteBlock

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        val style: TextStyleType = TextStyleType.BODY,
        var isCollapsed: Boolean = false,
        var isIndented: Boolean = true
    ) : NoteBlock

    data class Checklist(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var isChecked: Boolean = false,
        var reminderTimestamp: Long? = null,
        var isReminderDone: Boolean = false,
        var priority: Int = 0
    ) : NoteBlock

    data class SimpleList(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        val isNumbered: Boolean = false,
        val number: Int = 1
    ) : NoteBlock

    data class CalloutBox(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var backgroundColorHex: String? = null,
        var fontType: String = "Estándar",
        var emoji: String? = "💡"
    ) : NoteBlock

    data class Drawing(
        override val id: String = UUID.randomUUID().toString(),
        val filePath: String = "",
        val pathData: String = ""
    ) : NoteBlock

    enum class TableColorScope {
        CELL,
        ROW,
        COLUMN,
        TABLE
    }

    data class Table(
        override val id: String = UUID.randomUUID().toString(),
        var rows: Int = 2,
        var cols: Int = 2,
        var matrix: MutableList<MutableList<TableCell>> = mutableListOf(),
        var hasHeaderRow: Boolean = false,
        var hasZebraStripes: Boolean = false
    ) : NoteBlock {
        init {
            ensureMatrixSize()
        }

        fun ensureMatrixSize() {
            if (rows < 1) rows = 1
            if (cols < 1) cols = 1
            while (matrix.size < rows) {
                val rowList = mutableListOf<TableCell>()
                for (c in 0 until cols) {
                    rowList.add(TableCell())
                }
                matrix.add(rowList)
            }
            if (matrix.size > rows) {
                matrix = matrix.take(rows).toMutableList()
            }
            for (r in 0 until matrix.size) {
                while (matrix[r].size < cols) {
                    matrix[r].add(TableCell())
                }
                if (matrix[r].size > cols) {
                    matrix[r] = matrix[r].take(cols).toMutableList()
                }
            }
        }

        private fun deepCopyMatrix(): MutableList<MutableList<TableCell>> {
            return matrix.map { row ->
                row.map { cell -> TableCell(text = cell.text, backgroundColorHex = cell.backgroundColorHex) }.toMutableList()
            }.toMutableList()
        }

        fun updateCellText(r: Int, c: Int, text: String): Table {
            val newMatrix = deepCopyMatrix()
            if (r in newMatrix.indices && c in newMatrix[r].indices) {
                newMatrix[r][c].text = text
            }
            return copy(matrix = newMatrix)
        }

        fun applyColor(r: Int, c: Int, colorHex: String?, scope: TableColorScope): Table {
            val newMatrix = deepCopyMatrix()
            when (scope) {
                TableColorScope.CELL -> {
                    if (r in newMatrix.indices && c in newMatrix[r].indices) {
                        newMatrix[r][c].backgroundColorHex = colorHex
                    }
                }
                TableColorScope.ROW -> {
                    if (r in newMatrix.indices) {
                        newMatrix[r].forEach { it.backgroundColorHex = colorHex }
                    }
                }
                TableColorScope.COLUMN -> {
                    newMatrix.forEach { row ->
                        if (c in row.indices) {
                            row[c].backgroundColorHex = colorHex
                        }
                    }
                }
                TableColorScope.TABLE -> {
                    newMatrix.forEach { row ->
                        row.forEach { cell ->
                            cell.backgroundColorHex = colorHex
                        }
                    }
                }
            }
            return copy(matrix = newMatrix)
        }

        fun applyColorToCells(cells: Set<Pair<Int, Int>>, colorHex: String?): Table {
            val newMatrix = deepCopyMatrix()
            cells.forEach { (r, c) ->
                if (r in newMatrix.indices && c in newMatrix[r].indices) {
                    newMatrix[r][c].backgroundColorHex = colorHex
                }
            }
            return copy(matrix = newMatrix)
        }

        fun addRowAbove(targetRow: Int): Table {
            val newMatrix = deepCopyMatrix()
            val insertIndex = targetRow.coerceIn(0, newMatrix.size)
            val newRow = MutableList(cols) { TableCell() }
            newMatrix.add(insertIndex, newRow)
            return copy(rows = newMatrix.size, matrix = newMatrix)
        }

        fun addRowBelow(targetRow: Int): Table {
            val newMatrix = deepCopyMatrix()
            val insertIndex = (targetRow + 1).coerceIn(0, newMatrix.size)
            val newRow = MutableList(cols) { TableCell() }
            newMatrix.add(insertIndex, newRow)
            return copy(rows = newMatrix.size, matrix = newMatrix)
        }

        fun deleteRow(targetRow: Int): Table {
            if (rows <= 1) return this
            val newMatrix = deepCopyMatrix()
            if (targetRow in newMatrix.indices) {
                newMatrix.removeAt(targetRow)
            }
            return copy(rows = newMatrix.size, matrix = newMatrix)
        }

        fun addColumnLeft(targetCol: Int): Table {
            val newMatrix = deepCopyMatrix()
            val insertIndex = targetCol.coerceIn(0, cols)
            newMatrix.forEach { row ->
                row.add(insertIndex.coerceIn(0, row.size), TableCell())
            }
            val newCols = newMatrix.firstOrNull()?.size ?: cols
            return copy(cols = newCols, matrix = newMatrix)
        }

        fun addColumnRight(targetCol: Int): Table {
            val newMatrix = deepCopyMatrix()
            val insertIndex = (targetCol + 1).coerceIn(0, cols)
            newMatrix.forEach { row ->
                row.add(insertIndex.coerceIn(0, row.size), TableCell())
            }
            val newCols = newMatrix.firstOrNull()?.size ?: cols
            return copy(cols = newCols, matrix = newMatrix)
        }

        fun deleteColumn(targetCol: Int): Table {
            if (cols <= 1) return this
            val newMatrix = deepCopyMatrix()
            newMatrix.forEach { row ->
                if (targetCol in row.indices) {
                    row.removeAt(targetCol)
                }
            }
            val newCols = newMatrix.firstOrNull()?.size ?: cols
            return copy(cols = newCols, matrix = newMatrix)
        }

        fun clearCell(r: Int, c: Int): Table {
            return updateCellText(r, c, "")
        }

        fun clearCells(cells: Set<Pair<Int, Int>>): Table {
            val newMatrix = deepCopyMatrix()
            cells.forEach { (r, c) ->
                if (r in newMatrix.indices && c in newMatrix[r].indices) {
                    newMatrix[r][c].text = ""
                }
            }
            return copy(matrix = newMatrix)
        }

        fun clearTable(): Table {
            val newMatrix = deepCopyMatrix()
            newMatrix.forEach { row ->
                row.forEach { cell ->
                    cell.text = ""
                }
            }
            return copy(matrix = newMatrix)
        }

        fun toggleHeaderRow(): Table {
            return copy(hasHeaderRow = !hasHeaderRow)
        }

        fun toggleZebraStripes(): Table {
            return copy(hasZebraStripes = !hasZebraStripes)
        }
    }

    data class Math(
        override val id: String = UUID.randomUUID().toString(),
        var latexCode: String = ""
    ) : NoteBlock
}
