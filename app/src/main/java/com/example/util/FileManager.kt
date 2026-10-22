package com.example.util

import android.content.Context
import android.util.Log
import java.io.File

object FileManager {
    private const val TAG = "FileManager"
    private const val DRAWINGS_DIR = "drawings"

    /**
     * Guarda los datos JSON de un dibujo en el almacenamiento interno privado (filesDir/drawings).
     * Devuelve la ruta absoluta del archivo guardado.
     */
    fun saveDrawingData(
        context: Context,
        noteId: String,
        blockId: String,
        data: String
    ): String {
        if (data.isBlank()) return ""
        return try {
            val dir = File(context.filesDir, DRAWINGS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val safeBlockId = blockId.ifBlank { "main" }
            val fileName = "drawing_${noteId}_${safeBlockId}.json"
            val file = File(dir, fileName)
            file.writeText(data)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando datos de dibujo en archivo local", e)
            ""
        }
    }

    /**
     * Lee el contenido de un archivo de dibujo desde su filePath.
     * Si la cadena ingresada ya es JSON directo (compatibilidad con notas anteriores), se retorna directamente.
     */
    fun readDrawingData(filePathOrData: String): String {
        if (filePathOrData.isBlank()) return ""
        val trimmed = filePathOrData.trim()
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            // Datos legacy almacenados directamente como JSON
            return filePathOrData
        }
        return try {
            val file = File(filePathOrData)
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo archivo de dibujo en $filePathOrData", e)
            ""
        }
    }

    /**
     * Elimina un archivo físico si existe y no es un JSON inline directo.
     */
    fun deleteFile(filePath: String): Boolean {
        if (filePath.isBlank()) return false
        val trimmed = filePath.trim()
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return false // No es una ruta de archivo
        }
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar archivo: $filePath", e)
            false
        }
    }

    /**
     * Elimina todos los archivos de dibujos asociados a un ID de nota específico.
     */
    fun deleteDrawingsForNote(context: Context, noteId: String) {
        try {
            val dir = File(context.filesDir, DRAWINGS_DIR)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { _, name ->
                    name.startsWith("drawing_${noteId}_") || name == "drawing_${noteId}.json"
                }?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar dibujos de la nota $noteId", e)
        }
    }
}
