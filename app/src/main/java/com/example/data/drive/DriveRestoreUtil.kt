package com.example.data.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Utilidad responsable de buscar y restaurar la copia de seguridad de la base de datos
 * desde la carpeta oculta appDataFolder de Note JS Cloud.
 */
object DriveRestoreUtil {

    private const val TAG = "DriveRestoreUtil"
    private const val BACKUP_FILE_NAME = "backup.zip"

    /**
     * Descarga y restaura el archivo de copia de seguridad (backup.zip) desde Note JS Cloud
     * (intentando primero Google Drive y con respaldo automático desde la bóveda local de sincronización).
     *
     * @param context Contexto de la aplicación.
     * @param driveService Cliente autenticado opcional de la API de Note JS Cloud.
     * @param dbName Nombre de la base de datos Room a restaurar.
     * @return [RestoreResult] indicando el resultado de la operación.
     */
    suspend fun restoreBackup(
        context: Context,
        driveService: Drive? = null,
        dbName: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_restore_backup.zip")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            var downloadedFromDrive = false

            if (driveService != null) {
                try {
                    Log.d(TAG, "Buscando copia de seguridad en Note JS Cloud (appDataFolder)...")
                    var fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                        .execute()

                    var files = fileList.files

                    if (files.isNullOrEmpty()) {
                        try {
                            fileList = driveService.files().list()
                                .setSpaces("drive")
                                .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                                .execute()
                            files = fileList.files
                        } catch (e: Exception) {
                            Log.w(TAG, "Búsqueda secundaria en 'drive' no disponible", e)
                        }
                    }

                    if (!files.isNullOrEmpty()) {
                        val fileId = files[0].id
                        FileOutputStream(tempFile).use { outputStream ->
                            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                        }
                        downloadedFromDrive = tempFile.exists() && tempFile.length() > 0L
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Drive API no disponible durante restauración; usando bóveda Note JS Cloud", e)
                }
            }

            if (!downloadedFromDrive) {
                val vaultFile = DriveBackupWorker.getLocalCloudBackupFile(context)
                if (vaultFile.exists() && vaultFile.length() > 0L) {
                    vaultFile.copyTo(tempFile, overwrite = true)
                } else {
                    Log.w(TAG, "No se encontró ningún archivo $BACKUP_FILE_NAME en Drive ni en la bóveda local.")
                    return@withContext RestoreResult.NotFound
                }
            }

            // Cerrar conexiones activas de la base de datos Room antes de sobreescribirla
            AppDatabase.closeDatabase()

            val dbParentFolder = context.getDatabasePath(dbName).parentFile
                ?: File(context.applicationInfo.dataDir, "databases")

            if (!dbParentFolder.exists()) {
                dbParentFolder.mkdirs()
            }

            // Descomprimir el .zip iterando sobre ZipInputStream
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

            if (tempFile.exists()) {
                tempFile.delete()
            }

            Log.d(TAG, "Copia de seguridad restaurada exitosamente.")
            RestoreResult.Success
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Log.e(TAG, "Error al restaurar la copia de seguridad desde Note JS Cloud", e)
            RestoreResult.Error(e, "Error al restaurar: ${e.localizedMessage ?: e.javaClass.simpleName}")
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
