package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.FileProvider
import com.example.model.PdfDrawingStroke
import com.example.model.SignatureStamp
import com.example.ui.screens.DigitalSignature
import com.example.ui.screens.DrawingStroke
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utilidad para manipulación estructural y aplanado de archivos PDF mediante PDFBox para Android.
 */
object PdfModifierUtil {

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
     * Rota la página especificada por [pageIndex] 90 grados en sentido horario.
     */
    suspend fun rotatePage(
        context: Context,
        file: File,
        pageIndex: Int
    ): Boolean = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        if (!file.exists() || pageIndex < 0) return@withContext false

        var doc: PDDocument? = null
        val tempFile = File.createTempFile("pdf_rotate_", ".pdf", context.cacheDir)
        try {
            doc = PDDocument.load(file)
            if (pageIndex >= doc.numberOfPages) return@withContext false

            val page = doc.getPage(pageIndex)
            page.rotation = (page.rotation + 90) % 360

            doc.save(tempFile)
            doc.close()
            doc = null

            tempFile.copyTo(file, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                doc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Elimina la página en [pageIndex]. No permite eliminar si el documento tiene sólo 1 página.
     */
    suspend fun deletePage(
        context: Context,
        file: File,
        pageIndex: Int
    ): Boolean = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        if (!file.exists() || pageIndex < 0) return@withContext false

        var doc: PDDocument? = null
        val tempFile = File.createTempFile("pdf_delete_", ".pdf", context.cacheDir)
        try {
            doc = PDDocument.load(file)
            if (doc.numberOfPages <= 1 || pageIndex >= doc.numberOfPages) return@withContext false

            doc.removePage(pageIndex)

            doc.save(tempFile)
            doc.close()
            doc = null

            tempFile.copyTo(file, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                doc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Inserta una página en blanco de tamaño A4 justo después de la página en [pageIndex].
     */
    suspend fun insertBlankPage(
        context: Context,
        file: File,
        pageIndex: Int
    ): Boolean = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        if (!file.exists() || pageIndex < 0) return@withContext false

        var doc: PDDocument? = null
        val tempFile = File.createTempFile("pdf_insert_", ".pdf", context.cacheDir)
        try {
            doc = PDDocument.load(file)
            if (pageIndex >= doc.numberOfPages) return@withContext false

            val newPage = PDPage(PDRectangle.A4)
            val insertPosition = pageIndex + 1

            if (insertPosition < doc.numberOfPages) {
                val targetPage = doc.getPage(insertPosition)
                doc.pages.insertBefore(newPage, targetPage)
            } else {
                doc.addPage(newPage)
            }

            doc.save(tempFile)
            doc.close()
            doc = null

            tempFile.copyTo(file, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                doc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Aplanar (flatten) trazos de dibujo y firmas en las páginas del PDF físico y abrir el menú de compartir.
     */
    suspend fun flattenAndSharePdf(
        context: Context,
        originalFile: File,
        strokesMap: Map<Int, List<PdfDrawingStroke>>,
        signaturesMap: Map<Int, List<SignatureStamp>>
    ): Boolean = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        if (!originalFile.exists()) return@withContext false

        var doc: PDDocument? = null
        val sharedFile = File(context.cacheDir, "shared_doc.pdf")

        try {
            doc = PDDocument.load(originalFile)
            val pageCount = doc.numberOfPages

            val allPageIndices = (strokesMap.keys + signaturesMap.keys).distinct()

            for (pageIndex in allPageIndices) {
                if (pageIndex !in 0 until pageCount) continue

                val strokes = strokesMap[pageIndex] ?: emptyList()
                val signatures = signaturesMap[pageIndex] ?: emptyList()

                if (strokes.isEmpty() && signatures.isEmpty()) continue

                val page = doc.getPage(pageIndex)
                val mediaBox = page.mediaBox
                val pageWidth = mediaBox.width
                val pageHeight = mediaBox.height

                val contentStream = PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )

                // 1. Dibujar trazos (strokes)
                for (stroke in strokes) {
                    if (stroke.points.size <= 1) continue

                    val colorInt = runCatching { Color.parseColor(stroke.colorHex) }
                        .getOrDefault(Color.BLACK)
                    val r = Color.red(colorInt)
                    val g = Color.green(colorInt)
                    val b = Color.blue(colorInt)
                    val alpha = Color.alpha(colorInt) / 255f

                    contentStream.setStrokingColor(r, g, b)
                    val lineWidth = stroke.strokeWidth.coerceAtLeast(0.5f)
                    contentStream.setLineWidth(lineWidth)

                    val gs = PDExtendedGraphicsState()
                    gs.strokingAlphaConstant = if (stroke.isHighlighter) 0.4f else alpha
                    contentStream.setGraphicsStateParameters(gs)

                    val (firstNormX, firstNormY) = stroke.points.first()
                    val startX = firstNormX * pageWidth
                    val startY = pageHeight - (firstNormY * pageHeight)
                    contentStream.moveTo(startX, startY)

                    for (i in 1 until stroke.points.size) {
                        val (normX, normY) = stroke.points[i]
                        val x = normX * pageWidth
                        val y = pageHeight - (normY * pageHeight)
                        contentStream.lineTo(x, y)
                    }
                    contentStream.stroke()
                }

                // 2. Dibujar firmas (signatures)
                for (stamp in signatures) {
                    if (stamp.points.size <= 1) continue

                    contentStream.setStrokingColor(0, 0, 0)
                    val lineWidth = 2f
                    contentStream.setLineWidth(lineWidth)

                    val gs = PDExtendedGraphicsState()
                    gs.strokingAlphaConstant = 1.0f
                    contentStream.setGraphicsStateParameters(gs)

                    val stampWidth = stamp.widthRatio * pageWidth
                    val stampHeight = stamp.heightRatio * pageHeight
                    val stampX = stamp.normalizedX * pageWidth
                    val stampYCompose = stamp.normalizedY * pageHeight

                    val (firstPtX, firstPtY) = stamp.points.first()
                    val startX = stampX + (firstPtX * stampWidth)
                    val startY = pageHeight - (stampYCompose + (firstPtY * stampHeight))
                    contentStream.moveTo(startX, startY)

                    for (i in 1 until stamp.points.size) {
                        val (ptX, ptY) = stamp.points[i]
                        val x = stampX + (ptX * stampWidth)
                        val y = pageHeight - (stampYCompose + (ptY * stampHeight))
                        contentStream.lineTo(x, y)
                    }
                    contentStream.stroke()
                }

                contentStream.close()
            }

            doc.save(sharedFile)
            doc.close()
            doc = null

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                sharedFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                doc?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmName("flattenAndSharePdfUiModels")
    suspend fun flattenAndSharePdf(
        context: Context,
        originalFile: File,
        strokesMap: Map<Int, List<DrawingStroke>>,
        signaturesMap: Map<Int, List<DigitalSignature>>
    ): Boolean {
        val modelStrokes = strokesMap.mapValues { (_, strokes) ->
            strokes.map { s ->
                PdfDrawingStroke(
                    points = s.points.map { Pair(it.x, it.y) },
                    colorHex = s.colorHex,
                    strokeWidth = s.strokeWidthDp,
                    isHighlighter = s.isHighlighter
                )
            }
        }
        val modelSigs = signaturesMap.mapValues { (_, sigs) ->
            sigs.map { sig ->
                SignatureStamp(
                    id = sig.id,
                    points = sig.points.map { Pair(it.x, it.y) },
                    normalizedX = sig.normalizedX,
                    normalizedY = sig.normalizedY,
                    widthRatio = 0.35f * sig.scale,
                    heightRatio = 0.15f * sig.scale
                )
            }
        }
        return flattenAndSharePdf(context, originalFile, modelStrokes, modelSigs)
    }
}
