package com.example.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Extractor de texto PDF ("Deep Search") utilizando PDFBox for Android.
 */
object PdfTextExtractor {

    /**
     * Extrae hasta 30,000 caracteres de un archivo PDF a partir de un [pdfUri].
     * Ejecutado en [Dispatchers.IO].
     */
    suspend fun extractTextFromPdf(
        context: Context,
        pdfUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var document: PDDocument? = null
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(pdfUri) ?: return@withContext null
            document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            if (text.isNullOrBlank()) null else text.take(30000)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document?.close()
        }
    }

    /**
     * Sobrecarga para extraer hasta 30,000 caracteres de un archivo PDF a partir de un [File].
     * Ejecutado en [Dispatchers.IO].
     */
    suspend fun extractTextFromPdf(
        context: Context,
        pdfFile: File
    ): String? = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var document: PDDocument? = null
        try {
            if (!pdfFile.exists() || pdfFile.length() == 0L) return@withContext null
            document = PDDocument.load(pdfFile)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            if (text.isNullOrBlank()) null else text.take(30000)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document?.close()
        }
    }
}
