package com.example.data.drive

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker encargado de comprimir la base de datos Room local y subirla
 * a la carpeta oculta appDataFolder de Google Drive.
 */
class DriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando tarea de respaldo en Google Drive...")

            val driveManager = GoogleDriveManager(applicationContext)
            val account = driveManager.getSignedInAccount()

            if (account != null) {
                val driveService = driveManager.getDriveService(account)
                Log.d(TAG, "Autenticado con éxito como: ${account.email}")

                performBackup(driveService, DB_NAME)
                Log.d(TAG, "Respaldo realizado exitosamente.")
                Result.success()
            } else {
                Log.w(TAG, "No hay una cuenta de Google activa con permisos para appDataFolder.")
                Result.failure()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S durante la copia de seguridad", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado durante la ejecución del respaldo en Google Drive", e)
            Result.retry()
        }
    }

    /**
     * Localiza los archivos de la base de datos Room, los comprime en un archivo .zip temporal
     * y los sube a la carpeta oculta appDataFolder de Google Drive.
     *
     * @param driveService Instancia autenticada de la API de Google Drive.
     * @param dbName Nombre de la base de datos Room (ej. "auranotes_v3.db").
     */
    suspend fun performBackup(driveService: Drive, dbName: String) = withContext(Dispatchers.IO) {
        // 1. Localizar los tres archivos de la base de datos Room (principal, -shm y -wal)
        val mainDbFile = applicationContext.getDatabasePath(dbName)
        val shmDbFile = File(mainDbFile.path + "-shm")
        val walDbFile = File(mainDbFile.path + "-wal")

        val existingFiles = listOf(mainDbFile, shmDbFile, walDbFile).filter { it.exists() }

        if (existingFiles.isEmpty()) {
            throw FileNotFoundException("No se encontró ningún archivo de base de datos para $dbName")
        }

        // 2. Crear archivo ZIP temporal en applicationContext.cacheDir llamado backup.zip
        val tempZipFile = File(applicationContext.cacheDir, BACKUP_FILE_NAME)
        if (tempZipFile.exists()) {
            tempZipFile.delete()
        }

        try {
            ZipOutputStream(FileOutputStream(tempZipFile)).use { zipOut ->
                for (file in existingFiles) {
                    FileInputStream(file).use { fileIn ->
                        val zipEntry = ZipEntry(file.name)
                        zipOut.putNextEntry(zipEntry)
                        fileIn.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }

            // 3. Preparar metadata (nombre "backup.zip", parents = "appDataFolder") y subir
            val fileMetadata = DriveFile().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }

            val fileContent = FileContent("application/zip", tempZipFile)

            driveService.files().create(fileMetadata, fileContent).execute()
            Log.d(TAG, "Archivo $BACKUP_FILE_NAME subido correctamente a appDataFolder.")

        } finally {
            // 4. Borrar el archivo ZIP temporal local tras la subida
            if (tempZipFile.exists()) {
                val deleted = tempZipFile.delete()
                if (deleted) {
                    Log.d(TAG, "Archivo temporal $BACKUP_FILE_NAME eliminado localmente.")
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "DriveBackupWorkerJob"
        const val DB_NAME = "auranotes_v3.db"
        private const val BACKUP_FILE_NAME = "backup.zip"
        private const val TAG = "DriveBackupWorker"

        /**
         * Configura y encola la ejecución periódica de respaldo (cada 24 horas)
         * bajo restricciones de red no medida (WiFi) y dispositivo en carga.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()

            val backupWorkRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backupWorkRequest
            )

            Log.d(TAG, "Tarea de respaldo periódico programada correctamente en WorkManager.")
        }
    }
}
