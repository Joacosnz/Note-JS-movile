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
     * Descarga y restaura el archivo de copia de seguridad (backup.zip) desde Note JS Cloud.
     *
     * @param context Contexto de la aplicación.
     * @param driveService Cliente autenticado de la API de Note JS Cloud.
     * @param dbName Nombre de la base de datos Room a restaurar.
     * @return [RestoreResult] indicando el resultado de la operación.
     */
    suspend fun restoreBackup(
        context: Context,
        driveService: Drive,
        dbName: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Buscando copia de seguridad en Note JS Cloud (appDataFolder)...")

            // 1. Consulta a la API de Drive para buscar el archivo backup.zip en appDataFolder
            var fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                .execute()

            var files = fileList.files

            // Compatibilidad hacia atrás: si no está en appDataFolder, intentar buscar en la raíz (drive)
            if (files.isNullOrEmpty()) {
                Log.d(TAG, "No se encontró en appDataFolder. Buscando en la raíz de Drive...")
                try {
                    fileList = driveService.files().list()
                        .setSpaces("drive")
                        .setQ("name='$BACKUP_FILE_NAME' and trashed=false")
                        .execute()
                    files = fileList.files
                } catch (e: Exception) {
                    Log.w(TAG, "Búsqueda secundaria en 'drive' no disponible o sin permiso", e)
                }
            }

            if (files.isNullOrEmpty()) {
                Log.w(TAG, "No se encontró ningún archivo $BACKUP_FILE_NAME en Drive.")
                return@withContext RestoreResult.NotFound
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

            // 3. Cerrar conexiones activas de la base de datos Room antes de sobreescribirla
            AppDatabase.closeDatabase()

            val dbParentFolder = context.getDatabasePath(dbName).parentFile
                ?: File(context.applicationInfo.dataDir, "databases")

            if (!dbParentFolder.exists()) {
                dbParentFolder.mkdirs()
            }

            // 4. Descomprimir el .zip iterando sobre ZipInputStream
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
            RestoreResult.Success
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
                RestoreResult.AuthRequired(signInIntent)
            } else {
                RestoreResult.Error(e, errorMsg)
            }
        } catch (e: UserRecoverableAuthIOException) {
            Log.w(TAG, "Se requiere autorización del usuario para acceder a Drive", e)
            RestoreResult.AuthRequired(e.intent)
        } catch (e: Exception) {
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
