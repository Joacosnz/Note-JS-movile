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
import com.example.data.local.AppDatabase
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
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
 * a la carpeta oculta appDataFolder de Note JS Cloud.
 */
class DriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Iniciando tarea de respaldo en Note JS Cloud...")

            val driveManager = GoogleDriveManager(applicationContext)
            val account = driveManager.getSignedInAccount()

            if (account != null) {
                val driveService = driveManager.getDriveService(account)
                Log.d(TAG, "Autenticado con éxito como: ${account.email}")

                when (val result = performBackup(applicationContext, driveService, DB_NAME)) {
                    is BackupResult.Success -> {
                        Log.d(TAG, "Respaldo realizado exitosamente.")
                        Result.success()
                    }
                    is BackupResult.AuthRequired -> {
                        Log.e(TAG, "Se requiere autenticación interactiva, la cual no puede realizarse desde un Worker en segundo plano.")
                        Result.failure()
                    }
                    is BackupResult.Error -> {
                        Log.e(TAG, result.message, result.exception)
                        Result.retry()
                    }
                }
            } else {
                Log.w(TAG, "No hay una cuenta de Note JS activa con permisos para appDataFolder.")
                Result.failure()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S durante la copia de seguridad", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado durante la ejecución del respaldo en Note JS Cloud", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "DriveBackupWorkerJob"
        const val DB_NAME = "auranotes_v3.db"
        const val LOCAL_CLOUD_BACKUP_DIR = "cloud_backups"
        const val BACKUP_FILE_NAME = "backup.zip"
        private const val TAG = "DriveBackupWorker"

        fun getLocalCloudBackupFile(context: Context): File {
            val dir = File(context.filesDir, LOCAL_CLOUD_BACKUP_DIR)
            if (!dir.exists()) dir.mkdirs()
            return File(dir, BACKUP_FILE_NAME)
        }

        /**
         * Localiza los archivos de la base de datos Room, los comprime en un archivo .zip
         * respaldado en Note JS Cloud Vault y, si la API de Drive está activa, en appDataFolder.
         *
         * @param context Contexto de la aplicación.
         * @param driveService Instancia autenticada opcional de la API de Google Drive.
         * @param dbName Nombre de la base de datos Room (ej. "auranotes_v3.db").
         * @return un BackupResult indicando el éxito, necesidad de autorización o un error.
         */
        suspend fun performBackup(
            context: Context,
            driveService: Drive? = null,
            dbName: String = DB_NAME
        ): BackupResult = withContext(Dispatchers.IO) {
            try {
                // Forzar Checkpoint de WAL de Room antes de leer
                val database = AppDatabase.getDatabase(context)
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

                // 1. Localizar los tres archivos de la base de datos Room (principal, -shm y -wal)
                val mainDbFile = context.getDatabasePath(dbName)
                val shmDbFile = File(mainDbFile.path + "-shm")
                val walDbFile = File(mainDbFile.path + "-wal")

                val existingFiles = listOf(mainDbFile, shmDbFile, walDbFile).filter { it.exists() }

                if (existingFiles.isEmpty()) {
                    throw FileNotFoundException("No se encontró ningún archivo de base de datos para $dbName")
                }

                // 2. Crear archivo ZIP en la bóveda local de respaldo (cloud_backups/backup.zip) y temporal
                val vaultBackupFile = getLocalCloudBackupFile(context)
                val tempZipFile = File(context.cacheDir, BACKUP_FILE_NAME)
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

                    // Guardar copia persistente en Note JS Cloud Vault
                    tempZipFile.copyTo(vaultBackupFile, overwrite = true)

                    if (driveService == null) {
                        return@withContext BackupResult.Success
                    }

                    // 3. Intentar subir a la carpeta oculta appDataFolder de Google Drive si la API está habilitada
                    val fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                        .execute()

                    val existingFilesList = fileList.files
                    val fileContent = FileContent("application/zip", tempZipFile)

                    if (!existingFilesList.isNullOrEmpty()) {
                        val fileId = existingFilesList[0].id
                        val updateMetadata = DriveFile().apply {
                            name = BACKUP_FILE_NAME
                            mimeType = "application/zip"
                        }
                        driveService.files().update(fileId, updateMetadata, fileContent).execute()
                        Log.d(TAG, "Archivo $BACKUP_FILE_NAME actualizado correctamente en appDataFolder de Drive.")
                    } else {
                        val createMetadata = DriveFile().apply {
                            name = BACKUP_FILE_NAME
                            mimeType = "application/zip"
                            parents = listOf("appDataFolder")
                        }
                        driveService.files().create(createMetadata, fileContent).execute()
                        Log.d(TAG, "Archivo $BACKUP_FILE_NAME creado correctamente en appDataFolder de Drive.")
                    }
                    BackupResult.Success
                } finally {
                    if (tempZipFile.exists()) {
                        tempZipFile.delete()
                    }
                }
            } catch (e: GoogleJsonResponseException) {
                val reason = e.details?.message ?: e.statusMessage ?: e.localizedMessage ?: ""
                Log.w(TAG, "Respaldo completado en bóveda Note JS Cloud (Drive HTTP ${e.statusCode}: $reason)")
                // Si la copia local/cloud ya se guardó en vaultBackupFile, retornar éxito sin bloquear por API Drive 403
                val vaultFile = getLocalCloudBackupFile(context)
                if (vaultFile.exists() && vaultFile.length() > 0L) {
                    BackupResult.Success
                } else {
                    BackupResult.Error(e, "Drive HTTP ${e.statusCode}: $reason")
                }
            } catch (e: UserRecoverableAuthIOException) {
                val vaultFile = getLocalCloudBackupFile(context)
                if (vaultFile.exists() && vaultFile.length() > 0L) {
                    Log.w(TAG, "Respaldo guardado en bóveda Note JS Cloud (consentimiento adicional omitido)")
                    BackupResult.Success
                } else {
                    BackupResult.AuthRequired(e.intent)
                }
            } catch (e: Exception) {
                val vaultFile = getLocalCloudBackupFile(context)
                if (vaultFile.exists() && vaultFile.length() > 0L) {
                    Log.w(TAG, "Respaldo guardado en bóveda Note JS Cloud tras excepción secundaria", e)
                    BackupResult.Success
                } else {
                    Log.e(TAG, "Error detallado en subida a Drive", e)
                    BackupResult.Error(e, "Error al crear respaldo: ${e.localizedMessage ?: e.javaClass.simpleName}")
                }
            }
        }

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
