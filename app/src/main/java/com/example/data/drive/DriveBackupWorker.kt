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
        private const val BACKUP_FILE_NAME = "backup.zip"
        private const val TAG = "DriveBackupWorker"

        /**
         * Localiza los archivos de la base de datos Room, los comprime en un archivo .zip temporal
         * y los sube a la carpeta oculta appDataFolder de Note JS Cloud.
         *
         * @param context Contexto de la aplicación.
         * @param driveService Instancia autenticada de la API de Note JS Cloud.
         * @param dbName Nombre de la base de datos Room (ej. "auranotes_v3.db").
         * @return un BackupResult indicando el éxito, necesidad de autorización o un error.
         */
        suspend fun performBackup(
            context: Context,
            driveService: Drive,
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

                // 2. Crear archivo ZIP temporal en context.cacheDir llamado backup.zip
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

                    // 3. Buscar si el backup ya existe en la carpeta oculta appDataFolder
                    val fileList = driveService.files().list()
                        .setSpaces("appDataFolder")
                        .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                        .execute()

                    val existingFilesList = fileList.files
                    val fileContent = FileContent("application/zip", tempZipFile)

                    if (!existingFilesList.isNullOrEmpty()) {
                        // Update
                        val fileId = existingFilesList[0].id
                        val updateMetadata = DriveFile().apply {
                            name = BACKUP_FILE_NAME
                            mimeType = "application/zip"
                        }
                        driveService.files().update(fileId, updateMetadata, fileContent).execute()
                        Log.d(TAG, "Archivo $BACKUP_FILE_NAME actualizado correctamente en appDataFolder de Drive.")
                    } else {
                        // Create
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
                    // 4. Borrar el archivo ZIP temporal local tras la subida
                    if (tempZipFile.exists()) {
                        val deleted = tempZipFile.delete()
                        if (deleted) {
                            Log.d(TAG, "Archivo temporal $BACKUP_FILE_NAME eliminado localmente.")
                        }
                    }
                }
            } catch (e: GoogleJsonResponseException) {
                val reason = e.details?.message ?: e.statusMessage ?: e.localizedMessage
                val errorMsg = "Drive HTTP ${e.statusCode}: $reason"
                Log.e(TAG, errorMsg, e)

                if (e.statusCode == 401 || e.statusCode == 403) {
                    try {
                        val account = GoogleSignIn.getLastSignedInAccount(context)
                        if (account?.account != null) {
                            val scopes = listOf(DriveScopes.DRIVE_APPDATA)
                            val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
                            credential.selectedAccount = account.account
                            credential.token?.let { token ->
                                GoogleAuthUtil.clearToken(context, token)
                                Log.d(TAG, "Token OAuth limpiado tras error ${e.statusCode}.")
                            }
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Error al intentar limpiar el token OAuth", ex)
                    }
                    val signInIntent = GoogleDriveManager(context).getGoogleSignInClient().signInIntent
                    BackupResult.AuthRequired(signInIntent)
                } else {
                    BackupResult.Error(e, errorMsg)
                }
            } catch (e: UserRecoverableAuthIOException) {
                Log.w(TAG, "Se requiere autorización del usuario para acceder a Drive", e)
                BackupResult.AuthRequired(e.intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error detallado en subida a Drive", e)
                BackupResult.Error(e, "Error al subir a Drive: ${e.localizedMessage ?: e.javaClass.simpleName}")
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
