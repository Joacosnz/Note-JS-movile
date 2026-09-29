package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object PdfRenderUtil {

    // Obtiene la cantidad total de páginas de un PDF
    fun getPageCount(file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            1
        }
    }

    // Renderiza una página específica en un Bitmap nítido en segundo plano
    suspend fun renderPageToBitmap(file: File, pageIndex: Int, targetWidthPx: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }
            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeightPx = (targetWidthPx * aspectRatio).toInt()
            
            val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
            // Fondo blanco antes de dibujar para PDFs con fondo transparente
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Método simulado/básico para extraer texto legible para el motor de búsqueda.
    // Sin una librería de terceros como PDFBox o iText, PdfRenderer nativo no extrae texto.
    // Se extrae información básica de metadatos o contenido genérico simulado si no hay librería.
    fun extractTextFromPdf(file: File): String {
        return "Contenido indexado del PDF: ${file.name}"
    }
}
