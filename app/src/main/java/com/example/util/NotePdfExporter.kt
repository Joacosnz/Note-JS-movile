package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.PathParser
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.model.TextStyleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Motor de Exportación Inteligente para Note JS.
 * Convierte una lista de NoteBlock en un archivo PDF paginado calculando
 * la altura de cada bloque antes de dibujarlo y forzando saltos de página
 * para evitar que los elementos se corten por la mitad.
 */
object NotePdfExporter {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 50f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2f) // 495f

    suspend fun exportNote(
        context: Context,
        title: String,
        blocks: List<NoteBlock>
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        // Configuración de TextPaints y Paints
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.DEFAULT
            color = Color.BLACK
        }

        val metadataPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT
            color = Color.GRAY
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.BLACK
        }

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFFF3F4F6.toInt() // Gris claro
        }

        val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = 0xFFD1D5DB.toInt()
        }

        // Variables de estado
        var pageNumber = 1
        var currentY = MARGIN

        var pageInfo = PdfDocument.PageInfo.Builder(
            PAGE_WIDTH.toInt(),
            PAGE_HEIGHT.toInt(),
            pageNumber
        ).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun checkPageBreak(requiredHeight: Float) {
            if (currentY + requiredHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(),
                    PAGE_HEIGHT.toInt(),
                    pageNumber
                ).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN
            }
        }

        // Renderizar Título de la Nota
        if (title.isNotBlank()) {
            val titleLayout = StaticLayout.Builder.obtain(
                title,
                0,
                title.length,
                titlePaint,
                USABLE_WIDTH.toInt()
            ).build()
            val titleRequiredHeight = titleLayout.height.toFloat() + 15f
            checkPageBreak(titleRequiredHeight)

            canvas.save()
            canvas.translate(MARGIN, currentY)
            titleLayout.draw(canvas)
            canvas.restore()

            currentY += titleRequiredHeight
        }

        // Procesar bloques de la nota
        for (block in blocks) {
            when (block) {
                is NoteBlock.Text -> {
                    val paintToUse = when (block.style) {
                        TextStyleType.TITLE -> sectionTitlePaint
                        TextStyleType.SUBTITLE -> subtitlePaint
                        TextStyleType.BODY -> bodyPaint
                    }
                    val textContent = block.content.ifEmpty { " " }
                    val layout = StaticLayout.Builder.obtain(
                        textContent,
                        0,
                        textContent.length,
                        paintToUse,
                        USABLE_WIDTH.toInt()
                    ).build()

                    val blockHeight = layout.height.toFloat() + 10f
                    checkPageBreak(blockHeight)

                    canvas.save()
                    canvas.translate(MARGIN, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += blockHeight
                }

                is NoteBlock.Checklist -> {
                    val text = "${if (block.isChecked) "☑" else "☐"} ${block.content.ifEmpty { " " }}"
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        USABLE_WIDTH.toInt()
                    ).build()

                    val blockHeight = layout.height.toFloat() + 10f
                    checkPageBreak(blockHeight)

                    canvas.save()
                    canvas.translate(MARGIN, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += blockHeight
                }

                is NoteBlock.SimpleList -> {
                    val prefix = if (block.isNumbered) "${block.number}." else "•"
                    val text = "$prefix ${block.content.ifEmpty { " " }}"
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        USABLE_WIDTH.toInt()
                    ).build()

                    val blockHeight = layout.height.toFloat() + 10f
                    checkPageBreak(blockHeight)

                    canvas.save()
                    canvas.translate(MARGIN, currentY)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += blockHeight
                }

                is NoteBlock.CalloutBox -> {
                    val text = block.content.ifEmpty { " " }
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        (USABLE_WIDTH - 20f).toInt()
                    ).build()

                    val blockHeight = layout.height.toFloat() + 10f
                    checkPageBreak(blockHeight)

                    val bgRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + layout.height.toFloat() + 8f)
                    canvas.drawRoundRect(bgRect, 6f, 6f, cardBgPaint)
                    canvas.drawRoundRect(bgRect, 6f, 6f, tableBorderPaint)

                    canvas.save()
                    canvas.translate(MARGIN + 10f, currentY + 4f)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += blockHeight + 8f
                }

                is NoteBlock.Drawing -> {
                    if (block.pathData.isNotBlank()) {
                        val path = try {
                            PathParser.createPathFromPathData(block.pathData)
                        } catch (_: Exception) {
                            null
                        }

                        if (path != null) {
                            val bounds = RectF()
                            path.computeBounds(bounds, true)

                            val pathWidth = bounds.width().coerceAtLeast(1f)
                            val pathHeight = bounds.height().coerceAtLeast(1f)

                            val maxProjectedHeight = 200f
                            val widthScale = if (pathWidth > USABLE_WIDTH) USABLE_WIDTH / pathWidth else 1f
                            val heightScale = if (pathHeight * widthScale > maxProjectedHeight) maxProjectedHeight / pathHeight else widthScale
                            val scale = minOf(widthScale, heightScale)

                            val projectedWidth = pathWidth * scale
                            val projectedHeight = pathHeight * scale

                            checkPageBreak(projectedHeight + 10f)

                            canvas.save()
                            val offsetX = MARGIN + (USABLE_WIDTH - projectedWidth) / 2f
                            canvas.translate(offsetX - (bounds.left * scale), currentY - (bounds.top * scale))
                            canvas.scale(scale, scale)
                            canvas.drawPath(path, strokePaint)
                            canvas.restore()

                            currentY += projectedHeight + 10f
                        }
                    }
                }

                is NoteBlock.Pdf -> {
                    val cardHeight = 50f
                    checkPageBreak(60f)

                    val cardRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + cardHeight)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, tableBorderPaint)

                    val text = "📄 PDF: ${block.fileName} (${block.pageCount} pgs)"
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        (USABLE_WIDTH - 20f).toInt()
                    ).build()

                    canvas.save()
                    canvas.translate(MARGIN + 10f, currentY + (cardHeight - layout.height) / 2f)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += 60f
                }

                is NoteBlock.ImageBlock -> {
                    val cardHeight = 50f
                    checkPageBreak(60f)

                    val cardRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + cardHeight)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, tableBorderPaint)

                    val text = "🖼️ Imagen"
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        (USABLE_WIDTH - 20f).toInt()
                    ).build()

                    canvas.save()
                    canvas.translate(MARGIN + 10f, currentY + (cardHeight - layout.height) / 2f)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += 60f
                }

                is NoteBlock.Image -> {
                    val cardHeight = 50f
                    checkPageBreak(60f)

                    val cardRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + cardHeight)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, tableBorderPaint)

                    val text = "🖼️ Imagen: ${block.fileName}"
                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        (USABLE_WIDTH - 20f).toInt()
                    ).build()

                    canvas.save()
                    canvas.translate(MARGIN + 10f, currentY + (cardHeight - layout.height) / 2f)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += 60f
                }

                is AudioBlock -> {
                    val cardHeight = 50f
                    checkPageBreak(60f)

                    val cardRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + cardHeight)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, tableBorderPaint)

                    val minutes = block.durationMs / 60000
                    val seconds = (block.durationMs % 60000) / 1000
                    val durationText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    val snippet = if (block.transcription.isNotBlank()) " - ${block.transcription.take(30)}..." else ""
                    val text = "🎵 Audio: $durationText$snippet"

                    val layout = StaticLayout.Builder.obtain(
                        text,
                        0,
                        text.length,
                        bodyPaint,
                        (USABLE_WIDTH - 20f).toInt()
                    ).build()

                    canvas.save()
                    canvas.translate(MARGIN + 10f, currentY + (cardHeight - layout.height) / 2f)
                    layout.draw(canvas)
                    canvas.restore()

                    currentY += 60f
                }

                is NoteBlock.Table -> {
                    val rows = block.rows.coerceAtLeast(1)
                    val cols = block.cols.coerceAtLeast(1)
                    val rowHeight = 20f
                    val approxHeight = (rows * rowHeight) + 10f

                    checkPageBreak(approxHeight)

                    val colWidth = USABLE_WIDTH / cols
                    val tableHeight = rows * rowHeight
                    val tableRect = RectF(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + tableHeight)

                    canvas.drawRoundRect(tableRect, 4f, 4f, cardBgPaint)
                    canvas.drawRect(tableRect, tableBorderPaint)

                    for (r in 0 until rows) {
                        val rowY = currentY + (r * rowHeight)
                        if (r > 0) {
                            canvas.drawLine(MARGIN, rowY, MARGIN + USABLE_WIDTH, rowY, tableBorderPaint)
                        }
                        for (c in 0 until cols) {
                            val colX = MARGIN + (c * colWidth)
                            if (c > 0 && r == 0) {
                                canvas.drawLine(colX, currentY, colX, currentY + tableHeight, tableBorderPaint)
                            }
                            val cellText = block.matrix.getOrNull(r)?.getOrNull(c)?.text ?: ""
                            if (cellText.isNotBlank()) {
                                val snippet = if (cellText.length > 15) cellText.take(13) + ".." else cellText
                                val cellLayout = StaticLayout.Builder.obtain(
                                    snippet,
                                    0,
                                    snippet.length,
                                    metadataPaint,
                                    (colWidth - 6f).coerceAtLeast(10f).toInt()
                                ).build()

                                canvas.save()
                                canvas.translate(colX + 3f, rowY + 3f)
                                cellLayout.draw(canvas)
                                canvas.restore()
                            }
                        }
                    }

                    currentY += approxHeight
                }
            }
        }

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(30).ifEmpty { "Nota" }
        val pdfFile = File(outputDir, "${safeTitle}_${System.currentTimeMillis()}.pdf")

        pdfFile.outputStream().use { out ->
            document.writeTo(out)
        }
        document.close()

        pdfFile
    }
}
