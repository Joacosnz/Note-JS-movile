package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    /**
     * Renderiza una página específica del PDF en un Bitmap nítido con fondo blanco.
     */
    suspend fun renderPdfPage(file: File, pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                return@withContext null
            }
            page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } finally {
            runCatching { page?.close() }
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }

    /**
     * Obtiene el conteo total de páginas de un archivo PDF.
     */
    fun getPageCount(file: File): Int {
        if (!file.exists()) return 0
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Throwable) {
            1
        }
    }

    /**
     * Copia un archivo PDF desde un Uri del almacenamiento externo al directorio privado filesDir/pdfs.
     */
    fun copyPdfFromUri(context: Context, uri: Uri, targetName: String): File? {
        return try {
            val pdfsDir = File(context.filesDir, "pdfs")
            if (!pdfsDir.exists()) pdfsDir.mkdirs()

            val destFile = File(pdfsDir, targetName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) destFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Genera un contrato de prueba de 2 páginas con cláusulas y un área demarcada de firma digital.
     */
    fun createSamplePdf(context: Context): File? {
        val pdfsDir = File(context.filesDir, "pdfs")
        if (!pdfsDir.exists()) pdfsDir.mkdirs()
        val pdfFile = File(pdfsDir, "contrato_ejemplo.pdf")

        return try {
            val document = PdfDocument()

            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points

            // --- PÁGINA 1 ---
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = document.startPage(pageInfo1)
            val canvas1 = page1.canvas

            val paintTitle = Paint().apply {
                color = Color.rgb(26, 35, 126) // Indigo / Azul
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val paintSubtitle = Paint().apply {
                color = Color.rgb(66, 66, 66)
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val paintBody = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 10f
                isAntiAlias = true
            }

            val paintLine = Paint().apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
                isAntiAlias = true
            }

            var y = 50f
            canvas1.drawText("CONTRATO DE PRESTACIÓN DE SERVICIOS", 40f, y, paintTitle)
            y += 20f
            canvas1.drawText("ACUERDO MARCO Y CONDICIONES GENERALES", 40f, y, paintSubtitle)
            y += 15f
            canvas1.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paintLine)

            y += 35f
            canvas1.drawText("PRIMERA: OBJETO DEL CONTRATO", 40f, y, paintSubtitle)
            y += 20f
            canvas1.drawText("El presente contrato tiene por objeto regular la prestación de servicios profesionales de", 40f, y, paintBody)
            y += 15f
            canvas1.drawText("desarrollo de software y diseño de experiencia de usuario (UX/UI) entre las partes suscritos.", 40f, y, paintBody)

            y += 35f
            canvas1.drawText("SEGUNDA: CONDICIONES Y HONORARIOS", 40f, y, paintSubtitle)
            y += 20f
            canvas1.drawText("1. Los honorarios pactados se liquidarán de acuerdo al cumplimiento de los entregables.", 40f, y, paintBody)
            y += 15f
            canvas1.drawText("2. Todo ajuste o modificación sobre el alcance inicial requerirá aprobación previa por escrito.", 40f, y, paintBody)
            y += 15f
            canvas1.drawText("3. Los pagos se realizarán dentro de los 15 días posteriores a la facturación correspondientes.", 40f, y, paintBody)

            y += 35f
            canvas1.drawText("TERCERA: CONFIDENCIALIDAD", 40f, y, paintSubtitle)
            y += 20f
            canvas1.drawText("Ambas partes se obligan a mantener estricta confidencialidad sobre toda información técnica,", 40f, y, paintBody)
            y += 15f
            canvas1.drawText("comercial o de negocio compartida durante la vigencia de este contrato.", 40f, y, paintBody)

            // Número de página
            canvas1.drawText("Página 1 de 2", (pageWidth - 100).toFloat(), (pageHeight - 30).toFloat(), paintBody)

            document.finishPage(page1)

            // --- PÁGINA 2 ---
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = document.startPage(pageInfo2)
            val canvas2 = page2.canvas

            y = 50f
            canvas2.drawText("CUARTA: VIGENCIA Y RESCISIÓN", 40f, y, paintSubtitle)
            y += 20f
            canvas2.drawText("El contrato tendrá una vigencia inicial de 12 meses renovables automáticamente salvo", 40f, y, paintBody)
            y += 15f
            canvas2.drawText("notificación en contrario con 30 días de anticipación por cualquiera de las partes.", 40f, y, paintBody)

            y += 35f
            canvas2.drawText("QUINTA: JURISDICCIÓN Y LEY APLICABLE", 40f, y, paintSubtitle)
            y += 20f
            canvas2.drawText("Para cualquier controversia derivada del presente acuerdo, las partes se someten a la", 40f, y, paintBody)
            y += 15f
            canvas2.drawText("jurisdicción de los tribunales ordinarios de la ciudad correspondiente.", 40f, y, paintBody)

            // Área demarcada de firma digital
            y += 80f
            val rectBox = RectF(60f, y, (pageWidth - 60).toFloat(), y + 140f)
            val paintBox = Paint().apply {
                color = Color.rgb(220, 230, 242) // Azul suave
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val paintBoxBorder = Paint().apply {
                color = Color.rgb(25, 118, 210) // Azul primario
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }

            canvas2.drawRoundRect(rectBox, 12f, 12f, paintBox)
            canvas2.drawRoundRect(rectBox, 12f, 12f, paintBoxBorder)

            val paintSigHeader = Paint().apply {
                color = Color.rgb(25, 118, 210)
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas2.drawText("ÁREA RESERVADA PARA FIRMA DIGITAL", 80f, y + 30f, paintSigHeader)

            // Línea guía para la firma
            val paintSigLine = Paint().apply {
                color = Color.rgb(100, 100, 100)
                strokeWidth = 1.5f
                isAntiAlias = true
            }
            canvas2.drawLine(80f, y + 100f, (pageWidth - 80).toFloat(), y + 100f, paintSigLine)

            val paintSigHint = Paint().apply {
                color = Color.rgb(120, 120, 120)
                textSize = 9f
                isAntiAlias = true
            }
            canvas2.drawText("Firma Digital Manuscrita del Cliente / Aceptante", 80f, y + 120f, paintSigHint)

            // Número de página
            canvas2.drawText("Página 2 de 2", (pageWidth - 100).toFloat(), (pageHeight - 30).toFloat(), paintBody)

            document.finishPage(page2)

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            runCatching { document.close() }

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    pdfFile.writeText("%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
                }
                pdfFile
            } catch (_: Exception) {
                null
            }
        }
    }
}
