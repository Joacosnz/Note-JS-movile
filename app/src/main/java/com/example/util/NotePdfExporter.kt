package com.example.util

import android.content.Context
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.model.NoteType
import com.example.model.TextStyleType
import com.example.ui.components.MathFormatter
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de Exportación a PDF para Note JS utilizando la biblioteca PdfBox-Android.
 * Soporta la exportación de notas completas y hojas de cálculo (tablas y matrices numéricas/fórmulas)
 * con paginación automática, tipografía monoespaciada para fórmulas matemáticas y cuadrículas
 * de cálculo estilizadas.
 */
object NotePdfExporter {

    private const val MARGIN = 45f
    private val PAGE_RECT = PDRectangle.A4
    private val PAGE_WIDTH = PAGE_RECT.width   // ~595.28 pt
    private val PAGE_HEIGHT = PAGE_RECT.height // ~841.89 pt
    private val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2f)

    @Volatile
    private var isInitialized = false

    private fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    isInitialized = true
                }
            }
        }
    }

    /**
     * Sanitiza el texto para compatibilidad con codificación WinAnsi (ISO-8859-1) de fuentes estándar en PDFBox.
     * Evita caídas por caracteres Unicode no mapeados en fuentes Type1.
     */
    fun sanitizeForPdfBox(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder()
        for (ch in text) {
            val code = ch.code
            if (code in 32..126 || code in 160..255) {
                sb.append(ch)
            } else when (ch) {
                '\n', '\r', '\t' -> sb.append(' ')
                '•' -> sb.append("*")
                '—', '–' -> sb.append("-")
                '“', '”', '«', '»' -> sb.append("\"")
                '‘', '’' -> sb.append("'")
                '∑' -> sb.append("SUM")
                '√' -> sb.append("SQRT")
                '≠' -> sb.append("!=")
                '≤' -> sb.append("<=")
                '≥' -> sb.append(">=")
                '±' -> sb.append("+/-")
                '×' -> sb.append("x")
                '÷' -> sb.append("/")
                '→', '⇒' -> sb.append("->")
                '☑' -> sb.append("[X]")
                '☐' -> sb.append("[ ]")
                '²' -> sb.append("^2")
                '³' -> sb.append("^3")
                '¹' -> sb.append("^1")
                '⁰' -> sb.append("^0")
                '⁴' -> sb.append("^4")
                '⁵' -> sb.append("^5")
                '⁶' -> sb.append("^6")
                '⁷' -> sb.append("^7")
                '⁸' -> sb.append("^8")
                '⁹' -> sb.append("^9")
                else -> sb.append(' ')
            }
        }
        return sb.toString()
    }

    /**
     * Exporta la nota actual como documento PDF utilizando PdfBox-Android.
     */
    suspend fun exportNote(
        context: Context,
        title: String,
        blocks: List<NoteBlock>
    ): File = exportWithPdfBox(context, title, blocks, NoteType.TEXT)

    /**
     * Exporta la nota completa o hoja de cálculo con PdfBox-Android.
     */
    suspend fun exportWithPdfBox(
        context: Context,
        title: String,
        blocks: List<NoteBlock>,
        noteType: NoteType = NoteType.TEXT
    ): File = withContext(Dispatchers.IO) {
        ensureInitialized(context)

        val document = PDDocument()
        val pages = mutableListOf<PDPage>()
        val streams = mutableListOf<PDPageContentStream>()

        var currentPageNumber = 1
        var currentPage = PDPage(PAGE_RECT)
        document.addPage(currentPage)
        pages.add(currentPage)

        var contentStream = PDPageContentStream(document, currentPage)
        streams.add(contentStream)

        // Coordenada Y en PDFBox: 0 está en la base de la página. El margen superior empieza en PAGE_HEIGHT - MARGIN.
        var currentY = PAGE_HEIGHT - MARGIN

        val fontTitle: PDFont = PDType1Font.HELVETICA_BOLD
        val fontHeader: PDFont = PDType1Font.HELVETICA_BOLD
        val fontRegular: PDFont = PDType1Font.HELVETICA
        val fontMonospace: PDFont = PDType1Font.COURIER
        val fontMonospaceBold: PDFont = PDType1Font.COURIER_BOLD

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY - requiredHeight < MARGIN + 35f) {
                // Cerrar stream actual y crear nueva página
                contentStream.close()

                currentPageNumber++
                currentPage = PDPage(PAGE_RECT)
                document.addPage(currentPage)
                pages.add(currentPage)

                contentStream = PDPageContentStream(document, currentPage)
                streams.add(contentStream)
                currentY = PAGE_HEIGHT - MARGIN

                // Encabezado sutil en páginas secundarias
                contentStream.beginText()
                contentStream.setFont(fontRegular, 8f)
                contentStream.setNonStrokingColor(140, 140, 140)
                contentStream.newLineAtOffset(MARGIN, PAGE_HEIGHT - 30f)
                contentStream.showText(sanitizeForPdfBox(title.ifBlank { "Nota" }))
                contentStream.endText()

                // Línea separadora
                contentStream.setStrokingColor(220, 220, 220)
                contentStream.setLineWidth(0.5f)
                contentStream.moveTo(MARGIN, PAGE_HEIGHT - 34f)
                contentStream.lineTo(PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 34f)
                contentStream.stroke()
            }
        }

        // --- ENCABEZADO PRINCIPAL DE LA PRIMERA PÁGINA ---
        val displayTitle = sanitizeForPdfBox(title.ifBlank { "Nota sin título" })
        checkPageBreak(50f)

        // Barra decorativa ámbar superior
        contentStream.setNonStrokingColor(255, 184, 0)
        contentStream.addRect(MARGIN, currentY - 4f, 40f, 4f)
        contentStream.fill()
        currentY -= 14f

        // Título principal
        contentStream.beginText()
        contentStream.setFont(fontTitle, 22f)
        contentStream.setNonStrokingColor(25, 25, 25)
        contentStream.newLineAtOffset(MARGIN, currentY - 18f)
        contentStream.showText(displayTitle)
        contentStream.endText()
        currentY -= 26f

        // Subtítulo con fecha y tipo de documento
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val typeBadge = if (noteType == NoteType.MATH) "Hoja de Cálculo / Matemáticas" else "Documento de Notas"
        contentStream.beginText()
        contentStream.setFont(fontRegular, 9f)
        contentStream.setNonStrokingColor(120, 120, 120)
        contentStream.newLineAtOffset(MARGIN, currentY - 8f)
        contentStream.showText("Exportado con PdfBox  |  $dateStr  |  $typeBadge")
        contentStream.endText()
        currentY -= 16f

        // Línea divisoria suave
        contentStream.setStrokingColor(225, 225, 230)
        contentStream.setLineWidth(0.8f)
        contentStream.moveTo(MARGIN, currentY)
        contentStream.lineTo(PAGE_WIDTH - MARGIN, currentY)
        contentStream.stroke()
        currentY -= 18f

        // Función auxiliar para texto multilínea
        fun drawWrappedText(
            text: String,
            font: PDFont,
            fontSize: Float,
            colorR: Int,
            colorG: Int,
            colorB: Int,
            indentX: Float = 0f,
            lineSpacing: Float = 4f
        ) {
            val safeText = sanitizeForPdfBox(cleanMarkdownSyntax(text))
            if (safeText.isBlank()) return

            val maxLineWidth = USABLE_WIDTH - indentX
            val words = safeText.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (w in words) {
                if (w.isEmpty()) continue
                val candidate = if (currentLine.isEmpty()) w else "${currentLine} $w"
                val widthPt = (font.getStringWidth(candidate) / 1000f) * fontSize
                if (widthPt > maxLineWidth && currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(w)
                } else {
                    currentLine = StringBuilder(candidate)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }

            val totalHeight = lines.size * (fontSize + lineSpacing)
            checkPageBreak(totalHeight)

            for (l in lines) {
                contentStream.beginText()
                contentStream.setFont(font, fontSize)
                contentStream.setNonStrokingColor(colorR, colorG, colorB)
                contentStream.newLineAtOffset(MARGIN + indentX, currentY - fontSize)
                contentStream.showText(l)
                contentStream.endText()
                currentY -= (fontSize + lineSpacing)
            }
        }

        // --- PROCESAMIENTO DE BLOQUES ---
        for (block in blocks) {
            when (block) {
                is NoteBlock.Text -> {
                    val (font, size, r, g, b, extra) = when (block.style) {
                        TextStyleType.TITLE -> Tuple6(fontHeader, 16f, 30, 30, 30, 10f)
                        TextStyleType.SUBTITLE -> Tuple6(fontHeader, 13f, 70, 70, 70, 8f)
                        TextStyleType.BODY -> Tuple6(fontRegular, 11f, 40, 40, 40, 6f)
                    }
                    drawWrappedText(block.content, font, size, r, g, b)
                    currentY -= extra
                }

                is NoteBlock.Checklist -> {
                    checkPageBreak(18f)
                    val isChecked = block.isChecked
                    val mark = if (isChecked) "[X]" else "[ ]"

                    contentStream.beginText()
                    contentStream.setFont(fontMonospaceBold, 10f)
                    if (isChecked) {
                        contentStream.setNonStrokingColor(16, 185, 129) // Verde
                    } else {
                        contentStream.setNonStrokingColor(130, 130, 130)
                    }
                    contentStream.newLineAtOffset(MARGIN, currentY - 10f)
                    contentStream.showText(mark)
                    contentStream.endText()

                    val textColor = if (isChecked) Triple(140, 140, 140) else Triple(40, 40, 40)
                    drawWrappedText(
                        block.content.ifBlank { "Tarea" },
                        fontRegular,
                        10.5f,
                        textColor.first,
                        textColor.second,
                        textColor.third,
                        indentX = 24f,
                        lineSpacing = 3f
                    )
                    currentY -= 4f
                }

                is NoteBlock.SimpleList -> {
                    checkPageBreak(16f)
                    val bullet = if (block.isNumbered) "${block.number}." else "*"
                    contentStream.beginText()
                    contentStream.setFont(fontMonospaceBold, 10f)
                    contentStream.setNonStrokingColor(255, 184, 0) // Ámbar
                    contentStream.newLineAtOffset(MARGIN + 4f, currentY - 10f)
                    contentStream.showText(bullet)
                    contentStream.endText()

                    drawWrappedText(
                        block.content,
                        fontRegular,
                        10.5f,
                        40,
                        40,
                        40,
                        indentX = 20f,
                        lineSpacing = 3f
                    )
                    currentY -= 4f
                }

                is NoteBlock.CalloutBox -> {
                    val text = cleanMarkdownSyntax(block.content).ifBlank { "Nota destacada" }
                    val sanitized = sanitizeForPdfBox(text)
                    val boxWidth = USABLE_WIDTH
                    val boxHeight = 36f

                    checkPageBreak(boxHeight + 8f)

                    // Fondo de la caja
                    contentStream.setNonStrokingColor(245, 245, 248)
                    contentStream.addRect(MARGIN, currentY - boxHeight, boxWidth, boxHeight)
                    contentStream.fill()

                    // Borde lateral ámbar
                    contentStream.setNonStrokingColor(255, 184, 0)
                    contentStream.addRect(MARGIN, currentY - boxHeight, 4f, boxHeight)
                    contentStream.fill()

                    contentStream.beginText()
                    contentStream.setFont(fontRegular, 10f)
                    contentStream.setNonStrokingColor(50, 50, 50)
                    contentStream.newLineAtOffset(MARGIN + 12f, currentY - 22f)
                    contentStream.showText(sanitized.take(80))
                    contentStream.endText()

                    currentY -= (boxHeight + 10f)
                }

                is NoteBlock.Math -> {
                    val rawLatex = block.latexCode.ifBlank { "0" }
                    val formatted = MathFormatter.formatLatexToUnicode(rawLatex)
                    val safeLatex = sanitizeForPdfBox(formatted)
                    val cardHeight = 38f

                    checkPageBreak(cardHeight + 12f)

                    // Caja de fondo para la expresión matemática
                    contentStream.setNonStrokingColor(248, 249, 250)
                    contentStream.addRect(MARGIN, currentY - cardHeight, USABLE_WIDTH, cardHeight)
                    contentStream.fill()

                    // Borde exterior
                    contentStream.setStrokingColor(255, 184, 0)
                    contentStream.setLineWidth(1f)
                    contentStream.addRect(MARGIN, currentY - cardHeight, USABLE_WIDTH, cardHeight)
                    contentStream.stroke()

                    // Etiqueta "MATH / FÓRMULA"
                    contentStream.beginText()
                    contentStream.setFont(fontHeader, 8f)
                    contentStream.setNonStrokingColor(180, 83, 9)
                    contentStream.newLineAtOffset(MARGIN + 10f, currentY - 12f)
                    contentStream.showText("FÓRMULA / EXPRESIÓN MATEMÁTICA")
                    contentStream.endText()

                    // Texto monoespaciado alineado
                    contentStream.beginText()
                    contentStream.setFont(fontMonospaceBold, 12f)
                    contentStream.setNonStrokingColor(20, 20, 20)
                    contentStream.newLineAtOffset(MARGIN + 12f, currentY - 28f)
                    contentStream.showText(safeLatex.take(65))
                    contentStream.endText()

                    currentY -= (cardHeight + 12f)
                }

                is NoteBlock.Table -> {
                    // Renderizado de tabla estructurada con PdfBox
                    val rows = block.rows.coerceAtLeast(1)
                    val cols = block.cols.coerceAtLeast(1)
                    val cellHeight = 22f
                    val totalTableHeight = (rows * cellHeight) + 10f

                    checkPageBreak(totalTableHeight.coerceAtMost(250f))

                    val colWidth = USABLE_WIDTH / cols

                    for (r in 0 until rows) {
                        checkPageBreak(cellHeight + 4f)
                        val rowY = currentY - cellHeight
                        val isHeaderRow = (r == 0 && block.hasHeaderRow)
                        val isZebra = (block.hasZebraStripes && r % 2 == 1 && !isHeaderRow)

                        for (c in 0 until cols) {
                            val colX = MARGIN + (c * colWidth)

                            // Fondo de celda
                            if (isHeaderRow) {
                                contentStream.setNonStrokingColor(255, 237, 180) // Ámbar claro encabezado
                                contentStream.addRect(colX, rowY, colWidth, cellHeight)
                                contentStream.fill()
                            } else if (isZebra) {
                                contentStream.setNonStrokingColor(245, 245, 247)
                                contentStream.addRect(colX, rowY, colWidth, cellHeight)
                                contentStream.fill()
                            }

                            // Borde de celda
                            contentStream.setStrokingColor(210, 210, 215)
                            contentStream.setLineWidth(0.5f)
                            contentStream.addRect(colX, rowY, colWidth, cellHeight)
                            contentStream.stroke()

                            // Texto de celda
                            val rawText = block.matrix.getOrNull(r)?.getOrNull(c)?.text ?: ""
                            val cellText = sanitizeForPdfBox(rawText).trim()
                            if (cellText.isNotEmpty()) {
                                contentStream.beginText()
                                contentStream.setFont(if (isHeaderRow) fontMonospaceBold else fontMonospace, 9.5f)
                                contentStream.setNonStrokingColor(30, 30, 30)
                                contentStream.newLineAtOffset(colX + 5f, rowY + 6.5f)
                                val maxChars = (colWidth / 6.5f).toInt().coerceAtLeast(3)
                                contentStream.showText(cellText.take(maxChars))
                                contentStream.endText()
                            }
                        }
                        currentY -= cellHeight
                    }
                    currentY -= 12f
                }

                is NoteBlock.Drawing -> {
                    checkPageBreak(24f)
                    drawBadge(contentStream, fontRegular, "Dibujo / Boceto a mano alzada", currentY, 100, 116, 139)
                    currentY -= 28f
                }

                is AudioBlock -> {
                    checkPageBreak(24f)
                    val durStr = String.format(Locale.getDefault(), "%02d:%02d", block.durationMs / 60000, (block.durationMs % 60000) / 1000)
                    drawBadge(contentStream, fontRegular, "Grabación de Audio [$durStr]", currentY, 147, 51, 234)
                    currentY -= 28f
                }

                is NoteBlock.Image -> {
                    checkPageBreak(24f)
                    val name = block.fileName.ifBlank { "Imagen adjunta" }
                    drawBadge(contentStream, fontRegular, "Imagen: $name", currentY, 59, 130, 246)
                    currentY -= 28f
                }

                is NoteBlock.Pdf -> {
                    checkPageBreak(24f)
                    val name = block.fileName.ifBlank { "Documento PDF" }
                    drawBadge(contentStream, fontRegular, "PDF Adjunto: $name", currentY, 239, 68, 68)
                    currentY -= 28f
                }

                else -> {}
            }
        }

        // --- PIE DE PÁGINA EN TODAS LAS PÁGINAS ---
        contentStream.close()

        val totalPages = pages.size
        for (i in 0 until totalPages) {
            val p = pages[i]
            val footerStream = PDPageContentStream(document, p, PDPageContentStream.AppendMode.APPEND, true, true)

            // Línea separadora
            footerStream.setStrokingColor(230, 230, 235)
            footerStream.setLineWidth(0.5f)
            footerStream.moveTo(MARGIN, MARGIN - 10f)
            footerStream.lineTo(PAGE_WIDTH - MARGIN, MARGIN - 10f)
            footerStream.stroke()

            // Texto pie de página
            footerStream.beginText()
            footerStream.setFont(fontRegular, 8f)
            footerStream.setNonStrokingColor(150, 150, 150)
            footerStream.newLineAtOffset(MARGIN, MARGIN - 22f)
            footerStream.showText("Note JS  |  Página ${i + 1} de $totalPages")
            footerStream.endText()

            footerStream.close()
        }

        // Guardar archivo PDF generado en caché
        val outputDir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val safeFileName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(25).ifEmpty { "Nota" }
        val pdfFile = File(outputDir, "${safeFileName}_pdfbox_${System.currentTimeMillis()}.pdf")

        document.save(pdfFile)
        document.close()

        pdfFile
    }

    /**
     * Exporta exclusivamente una tabla como hoja de cálculo formal a PDF con PdfBox-Android.
     */
    suspend fun exportCalculationSheet(
        context: Context,
        title: String,
        table: NoteBlock.Table
    ): File = withContext(Dispatchers.IO) {
        ensureInitialized(context)

        val document = PDDocument()
        val pages = mutableListOf<PDPage>()

        var currentPageNumber = 1
        var currentPage = PDPage(PAGE_RECT)
        document.addPage(currentPage)
        pages.add(currentPage)

        var stream = PDPageContentStream(document, currentPage)
        var currentY = PAGE_HEIGHT - MARGIN

        val fontTitle: PDFont = PDType1Font.HELVETICA_BOLD
        val fontHeader: PDFont = PDType1Font.COURIER_BOLD
        val fontCell: PDFont = PDType1Font.COURIER
        val fontMeta: PDFont = PDType1Font.HELVETICA

        val rows = table.rows.coerceAtLeast(1)
        val cols = table.cols.coerceAtLeast(1)
        val colWidth = USABLE_WIDTH / cols
        val cellHeight = 24f

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY - requiredHeight < MARGIN + 40f) {
                stream.close()

                currentPageNumber++
                currentPage = PDPage(PAGE_RECT)
                document.addPage(currentPage)
                pages.add(currentPage)

                stream = PDPageContentStream(document, currentPage)
                currentY = PAGE_HEIGHT - MARGIN

                // Encabezado de página secundaria
                stream.beginText()
                stream.setFont(fontMeta, 8f)
                stream.setNonStrokingColor(130, 130, 130)
                stream.newLineAtOffset(MARGIN, PAGE_HEIGHT - 30f)
                stream.showText("Hoja de Cálculo: ${sanitizeForPdfBox(title.ifBlank { "Cálculo" })}")
                stream.endText()

                stream.setStrokingColor(220, 220, 220)
                stream.setLineWidth(0.5f)
                stream.moveTo(MARGIN, PAGE_HEIGHT - 34f)
                stream.lineTo(PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 34f)
                stream.stroke()
            }
        }

        // Título de la Hoja de Cálculo
        stream.setNonStrokingColor(255, 184, 0)
        stream.addRect(MARGIN, currentY - 4f, 40f, 4f)
        stream.fill()
        currentY -= 14f

        stream.beginText()
        stream.setFont(fontTitle, 20f)
        stream.setNonStrokingColor(20, 20, 20)
        stream.newLineAtOffset(MARGIN, currentY - 18f)
        stream.showText(sanitizeForPdfBox(title.ifBlank { "Hoja de Cálculo" }))
        stream.endText()
        currentY -= 26f

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        stream.beginText()
        stream.setFont(fontMeta, 9f)
        stream.setNonStrokingColor(110, 110, 110)
        stream.newLineAtOffset(MARGIN, currentY - 8f)
        stream.showText("Hoja de Cálculo ($rows filas x $cols columnas)  |  $dateStr  |  PdfBox")
        stream.endText()
        currentY -= 20f

        // Dibujar filas y columnas
        for (r in 0 until rows) {
            checkPageBreak(cellHeight + 4f)
            val rowY = currentY - cellHeight
            val isHeader = (r == 0 && table.hasHeaderRow)
            val isZebra = (table.hasZebraStripes && r % 2 == 1 && !isHeader)

            for (c in 0 until cols) {
                val colX = MARGIN + (c * colWidth)

                if (isHeader) {
                    stream.setNonStrokingColor(254, 243, 199) // Ámbar suave
                    stream.addRect(colX, rowY, colWidth, cellHeight)
                    stream.fill()
                } else if (isZebra) {
                    stream.setNonStrokingColor(249, 250, 251)
                    stream.addRect(colX, rowY, colWidth, cellHeight)
                    stream.fill()
                }

                // Bordes nítidos
                stream.setStrokingColor(209, 213, 219)
                stream.setLineWidth(0.6f)
                stream.addRect(colX, rowY, colWidth, cellHeight)
                stream.stroke()

                val rawText = table.matrix.getOrNull(r)?.getOrNull(c)?.text ?: ""
                val cellText = sanitizeForPdfBox(rawText).trim()
                if (cellText.isNotEmpty()) {
                    stream.beginText()
                    stream.setFont(if (isHeader) fontHeader else fontCell, 10f)
                    stream.setNonStrokingColor(17, 24, 39)
                    stream.newLineAtOffset(colX + 6f, rowY + 7f)
                    val maxLen = (colWidth / 6.8f).toInt().coerceAtLeast(3)
                    stream.showText(cellText.take(maxLen))
                    stream.endText()
                }
            }
            currentY -= cellHeight
        }

        stream.close()

        // Numerar páginas
        val totalPages = pages.size
        for (i in 0 until totalPages) {
            val p = pages[i]
            val footerStream = PDPageContentStream(document, p, PDPageContentStream.AppendMode.APPEND, true, true)
            footerStream.setStrokingColor(229, 231, 235)
            footerStream.setLineWidth(0.5f)
            footerStream.moveTo(MARGIN, MARGIN - 10f)
            footerStream.lineTo(PAGE_WIDTH - MARGIN, MARGIN - 10f)
            footerStream.stroke()

            footerStream.beginText()
            footerStream.setFont(fontMeta, 8f)
            footerStream.setNonStrokingColor(156, 163, 175)
            footerStream.newLineAtOffset(MARGIN, MARGIN - 22f)
            footerStream.showText("Note JS  |  Hoja de Cálculo  |  Página ${i + 1} de $totalPages")
            footerStream.endText()
            footerStream.close()
        }

        val outputDir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val safeFileName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(25).ifEmpty { "Calculo" }
        val pdfFile = File(outputDir, "${safeFileName}_hoja_calculo_${System.currentTimeMillis()}.pdf")

        document.save(pdfFile)
        document.close()

        pdfFile
    }

    private fun drawBadge(
        stream: PDPageContentStream,
        font: PDFont,
        label: String,
        y: Float,
        r: Int,
        g: Int,
        b: Int
    ) {
        val safe = sanitizeForPdfBox(label)
        stream.beginText()
        stream.setFont(font, 9.5f)
        stream.setNonStrokingColor(r, g, b)
        stream.newLineAtOffset(MARGIN + 4f, y - 10f)
        stream.showText("[ $safe ]")
        stream.endText()
    }

    private data class Tuple6<A, B, C, D, E, F>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
        val sixth: F
    )
}
