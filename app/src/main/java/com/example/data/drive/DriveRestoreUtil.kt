package com.example.data.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Utilidad responsable de buscar y restaurar la copia de seguridad de la base de datos
 * desde la carpeta oculta appDataFolder de Google Drive.
 */
object DriveRestoreUtil {

    private const val TAG = "DriveRestoreUtil"
    private const val BACKUP_FILE_NAME = "backup.zip"

    /**
     * Descarga y restaura el archivo de copia de seguridad (backup.zip) desde Google Drive.
     *
     * @param context Contexto de la aplicación.
     * @param driveService Cliente autenticado de la API de Google Drive.
     * @param dbName Nombre de la base de datos Room a restaurar.
     * @return true si la restauración fue exitosa, false en caso contrario.
     */
    suspend fun restoreBackup(
        context: Context,
        driveService: Drive,
        dbName: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando copia de seguridad en Google Drive (appDataFolder)...")

            // 1. Consulta a la API de Drive para buscar el archivo backup.zip en appDataFolder
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME'")
                .execute()

            val files = fileList.files
            if (files.isNullOrEmpty()) {
                Log.w(TAG, "No se encontró ningún archivo $BACKUP_FILE_NAME en Drive.")
                return@withContext false
            }

            val fileId = files[0].id
            Log.d(TAG, "Archivo de respaldo encontrado con ID: $fileId. Descargando...")

            // 2. Descargar a un archivo temporal en context.cacheDir
            val tempFile = File(context.cacheDir, "temp_restore_backup.zip")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            FileOutputStream(tempFile).use { outputStream ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            }

            // 3. Descomprimir el .zip iterando sobre ZipInputStream
            val dbParentFolder = context.getDatabasePath(dbName).parentFile
                ?: File(context.applicationInfo.dataDir, "databases")

            if (!dbParentFolder.exists()) {
                dbParentFolder.mkdirs()
            }

            tempFile.inputStream().use { fileIn ->
                ZipInputStream(fileIn).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val targetFile = File(dbParentFolder, entry.name)
                            Log.d(TAG, "Restaurando archivo de BD: ${targetFile.absolutePath}")
                            FileOutputStream(targetFile).use { outStream ->
                                zipIn.copyTo(outStream)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Limpieza del archivo temporal
            if (tempFile.exists()) {
                tempFile.delete()
            }

            Log.d(TAG, "Copia de seguridad restaurada exitosamente.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al restaurar la copia de seguridad desde Google Drive", e)
            false
        }
    }
}

/**
 * Función de extensión de Context para reiniciar la aplicación explícitamente hacia MainActivity
 * y forzar la recarga de Room con los nuevos archivos de la base de datos.
 */
fun Context.triggerRestart() {
    val intent = Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(intent)
    Runtime.getRuntime().exit(0)
}
