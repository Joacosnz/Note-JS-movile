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

data class PdfBlock(
    override val id: String = UUID.randomUUID().toString(),
    val filePath: String,              // Ruta local absoluta en filesDir
    val fileName: String,              // Nombre del archivo (ej. "Contrato.pdf")
    val fileSizeFormatted: String = "",// Format: "2.4 MB"
    val pageCount: Int,                // Total de páginas del PDF
    var currentPageIndex: Int = 0,     // Índice de página visible (inicia en 0)
    var isExpanded: Boolean = true,    // true = ver página grande, false = tarjeta compacta
    var annotationsPerPage: MutableMap<Int, List<PdfDrawingStroke>> = mutableMapOf(),
    var signaturesPerPage: MutableMap<Int, List<SignatureStamp>> = mutableMapOf(),
    var indexedText: String = ""       // Texto extraído del documento para el motor de búsqueda
) : NoteBlock

data class AudioBlock(
    override val id: String = UUID.randomUUID().toString(),
    val filePath: String,                   // Ruta absoluta local en filesDir
    val durationMs: Long,                   // Duración total en milisegundos
    var transcription: String = "",         // Texto transcrito
    var isTranscribing: Boolean = false,    // true mientras se procesa el audio
    var isTranscriptionVisible: Boolean = false // Controla si se muestra u oculta el texto
) : NoteBlock

sealed interface NoteBlock {
    val id: String

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        val style: TextStyleType = TextStyleType.BODY
    ) : NoteBlock

    data class Checklist(
        override val id: String = UUID.randomUUID().toString(),
        var content: String = "",
        var isChecked: Boolean = false
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
        var backgroundColorHex: String? = null
    ) : NoteBlock

    data class Drawing(
        override val id: String = UUID.randomUUID().toString(),
        val pathData: String = ""
    ) : NoteBlock

    data class Table(
        override val id: String = UUID.randomUUID().toString(),
        var rows: Int = 2,
        var cols: Int = 2,
        var matrix: MutableList<MutableList<TableCell>> = mutableListOf()
    ) : NoteBlock {
        init {
            if (matrix.isEmpty()) {
                for (r in 0 until rows) {
                    val rowList = mutableListOf<TableCell>()
                    for (c in 0 until cols) {
                        rowList.add(TableCell())
                    }
                    matrix.add(rowList)
                }
            }
        }
    }
}
