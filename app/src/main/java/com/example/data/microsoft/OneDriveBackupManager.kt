package com.example.data.microsoft

import android.content.Context
import android.util.Log
import com.example.auth.microsoft.MicrosoftAuthManager
import com.example.data.drive.BackupResult
import com.example.data.drive.BackupSnapshotInfo
import com.example.data.drive.DriveBackupWorker
import com.example.data.drive.DriveRestoreUtil
import com.example.data.drive.RestoreMode
import com.example.data.drive.RestoreResult
import com.example.data.local.AppDatabase
import com.example.model.ConflictRecordType
import com.example.model.ConflictResolutionChoice
import com.example.model.SyncConflictItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile

/**
 * Administrador de copias de seguridad de Note JS en Microsoft OneDrive utilizando Microsoft Graph API.
 * Permite guardar, listar, restaurar, detectar conflictos con Google y respaldar en la nube de Microsoft (Outlook / Office 365).
 */
class OneDriveBackupManager(private val context: Context) {

    private val authManager = MicrosoftAuthManager(context)
    private val client = MicrosoftGraphClient(context)

    fun isSignedIn(): Boolean = authManager.isSignedIn()

    /**
     * Realiza un respaldo completo en OneDrive:
     * 1. Genera el archivo ZIP con la base de datos y/o snapshot JSON estructurado localmente.
     * 2. Lo sube a la carpeta /NoteJS_Backups/ en la cuenta de OneDrive del usuario.
     */
    suspend fun performOneDriveBackup(
        triggerType: String = "MANUAL"
    ): BackupResult = withContext(Dispatchers.IO) {
        val account = authManager.currentAccount.value
        if (account == null || account.accessToken.isBlank()) {
            return@withContext BackupResult.Error(
                IllegalStateException("No hay una cuenta de Microsoft vinculada para subir a OneDrive."),
                "No hay una cuenta de Microsoft vinculada para subir a OneDrive."
            )
        }

        try {
            val localResult = DriveBackupWorker.performBackup(
                context = context,
                driveService = null,
                triggerType = triggerType
            )

            if (localResult is BackupResult.Error) {
                return@withContext localResult
            }

            val localZipFile = DriveBackupWorker.getLocalCloudBackupFile(context)
            if (!localZipFile.exists() || localZipFile.length() == 0L) {
                return@withContext BackupResult.Error(
                    IllegalStateException("No se pudo generar el archivo de respaldo local."),
                    "No se pudo generar el archivo de respaldo local."
                )
            }

            val fileBytes = localZipFile.readBytes()
            val timestamp = System.currentTimeMillis()
            val timestampFileName = "backup_$timestamp.zip"

            val uploadHistRes = client.uploadBackupFile(timestampFileName, fileBytes)
            val uploadMainRes = client.uploadBackupFile("backup.zip", fileBytes)

            if (uploadMainRes.isSuccess || uploadHistRes.isSuccess) {
                val now = System.currentTimeMillis()
                val counts = DriveBackupWorker.getLastBackupCounts(context)
                val notesCount = counts.first
                val remindersCount = counts.second
                val pdfsCount = counts.third
                val accountEmail = account.email.ifBlank { account.displayName }

                val newSnapshot = BackupSnapshotInfo(
                    id = timestamp.toString(),
                    fileName = timestampFileName,
                    timestamp = now,
                    sizeBytes = fileBytes.size.toLong(),
                    notesCount = notesCount,
                    remindersCount = remindersCount,
                    pdfsCount = pdfsCount,
                    isUploadedToCloud = true,
                    driveFileId = uploadHistRes.getOrNull()?.id ?: uploadMainRes.getOrNull()?.id,
                    triggerType = triggerType,
                    accountEmail = "$accountEmail (OneDrive)"
                )
                DriveBackupWorker.recordBackupInHistory(context, newSnapshot)
                DriveBackupWorker.saveLatestBackupStats(context, newSnapshot)

                BackupResult.Success
            } else {
                val errMsg = uploadMainRes.exceptionOrNull()?.message
                    ?: uploadHistRes.exceptionOrNull()?.message
                    ?: "Fallo de conexión con Microsoft OneDrive"
                BackupResult.Error(Exception(errMsg), "Error al subir a OneDrive: $errMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante copia a OneDrive", e)
            BackupResult.Error(e, "Error inesperado en respaldo a OneDrive: ${e.localizedMessage}")
        }
    }

    /**
     * Obtiene el listado de copias de seguridad disponibles en OneDrive.
     */
    suspend fun fetchOneDriveBackups(): List<BackupSnapshotInfo> = withContext(Dispatchers.IO) {
        if (!authManager.isSignedIn()) return@withContext emptyList()

        try {
            val remoteItems = client.listBackupFiles()
            val localHistory = DriveBackupWorker.loadLocalBackupHistory(context)

            val resultList = mutableListOf<BackupSnapshotInfo>()

            for (item in remoteItems) {
                val matchedLocal = localHistory.firstOrNull { it.fileName == item.name || it.driveFileId == item.id }
                if (matchedLocal != null) {
                    resultList.add(
                        matchedLocal.copy(
                            isUploadedToCloud = true,
                            driveFileId = item.id,
                            sizeBytes = if (matchedLocal.sizeBytes > 0L) matchedLocal.sizeBytes else item.sizeBytes
                        )
                    )
                } else {
                    val ts = item.name.removePrefix("backup_").removeSuffix(".zip").toLongOrNull()
                        ?: item.modifiedTimeMillis

                    resultList.add(
                        BackupSnapshotInfo(
                            id = item.id,
                            fileName = item.name,
                            timestamp = ts,
                            sizeBytes = item.sizeBytes,
                            notesCount = 0,
                            remindersCount = 0,
                            pdfsCount = 0,
                            isUploadedToCloud = true,
                            driveFileId = item.id,
                            triggerType = "ONEDRIVE"
                        )
                    )
                }
            }
            resultList.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error listando copias de OneDrive", e)
            emptyList()
        }
    }

    /**
     * Compara las notas del respaldo más reciente en OneDrive contra las notas actuales (Google / Local)
     * y devuelve la lista de conflictos si una misma nota fue editada con contenido distinto en ambas plataformas.
     */
    suspend fun checkAndDetectNoteConflicts(): List<SyncConflictItem> = withContext(Dispatchers.IO) {
        if (!authManager.isSignedIn()) return@withContext emptyList()
        val tempFile = File(context.cacheDir, "temp_onedrive_conflict_check.zip")
        if (tempFile.exists()) tempFile.delete()

        try {
            val remoteFiles = client.listBackupFiles()
            val latestMain = remoteFiles.firstOrNull { it.name.equals("backup.zip", ignoreCase = true) }
                ?: remoteFiles.firstOrNull()
                ?: return@withContext emptyList()

            val bytes = client.downloadBackupFile(latestMain.id) ?: return@withContext emptyList()
            FileOutputStream(tempFile).use { it.write(bytes) }

            val jsonText = ZipFile(tempFile).use { zf ->
                val entry = zf.getEntry(DriveBackupWorker.SNAPSHOT_JSON_ENTRY) ?: return@withContext emptyList()
                zf.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
            }
            if (jsonText.isBlank()) return@withContext emptyList()

            val root = JSONObject(jsonText)
            val notesArr = root.optJSONArray("notes") ?: return@withContext emptyList()

            val db = AppDatabase.getDatabase(context)
            val localNotes = db.noteDao().getAllNotesList().filter { !it.isTrashed && !it.pendingDelete }
            val localByUuid = localNotes.associateBy { it.remoteUuid }

            val conflicts = mutableListOf<SyncConflictItem>()
            for (i in 0 until notesArr.length()) {
                val msNoteObj = notesArr.optJSONObject(i) ?: continue
                if (msNoteObj.optBoolean("isTrashed", false)) continue

                val uuid = msNoteObj.optString("remoteUuid", "")
                if (uuid.isBlank()) continue

                val localMatch = localByUuid[uuid] ?: continue
                val msTitle = msNoteObj.optString("title", "")
                val msContent = msNoteObj.optString("content", "")
                val msUpdatedAt = msNoteObj.optLong("updatedAt", latestMain.modifiedTimeMillis)

                val titleChanged = !localMatch.title.trim().equals(msTitle.trim(), ignoreCase = false)
                val contentChanged = !localMatch.content.trim().equals(msContent.trim(), ignoreCase = false)

                if (titleChanged || contentChanged) {
                    conflicts.add(
                        SyncConflictItem(
                            id = "conflict_note_$uuid",
                            localId = localMatch.id,
                            remoteUuid = uuid,
                            recordType = ConflictRecordType.NOTE,
                            googleTitle = localMatch.title,
                            googleContent = localMatch.content,
                            googleUpdatedAt = localMatch.updatedAt,
                            microsoftTitle = msTitle,
                            microsoftContent = msContent,
                            microsoftUpdatedAt = msUpdatedAt
                        )
                    )
                }
            }
            conflicts
        } catch (e: Exception) {
            Log.w(TAG, "Aviso al verificar conflictos con OneDrive", e)
            emptyList()
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * Resuelve manualmente un conflicto de nota entre Google y Microsoft según la elección del usuario.
     */
    suspend fun resolveNoteConflict(
        conflict: SyncConflictItem,
        choice: ConflictResolutionChoice
    ) = withContext(Dispatchers.IO) {
        if (conflict.recordType != ConflictRecordType.NOTE) return@withContext
        val db = AppDatabase.getDatabase(context)
        val existingNote = db.noteDao().getNoteById(conflict.localId)
            ?: db.noteDao().getNoteByRemoteUuid(conflict.remoteUuid)
            ?: return@withContext
        val now = System.currentTimeMillis()

        when (choice) {
            ConflictResolutionChoice.KEEP_GOOGLE -> {
                // La versión de Google/Local ya está en la base de datos; marcamos como actualizada para sincronizar
                db.noteDao().updateNote(
                    existingNote.copy(
                        updatedAt = now,
                        lastModifiedLocal = now,
                        isSynced = false
                    )
                )
            }
            ConflictResolutionChoice.KEEP_MICROSOFT -> {
                // Sobrescribimos con la versión proveniente de Microsoft OneDrive
                db.noteDao().updateNote(
                    existingNote.copy(
                        title = conflict.microsoftTitle,
                        content = conflict.microsoftContent,
                        updatedAt = now,
                        lastModifiedLocal = now,
                        isSynced = false
                    )
                )
            }
            ConflictResolutionChoice.KEEP_BOTH -> {
                // Mantenemos la de Google y creamos una nota adicional con la versión de Microsoft
                val copyNote = existingNote.copy(
                    id = 0L,
                    remoteUuid = UUID.randomUUID().toString(),
                    title = "${conflict.microsoftTitle.ifBlank { "Nota" }} (Versión Microsoft)",
                    content = conflict.microsoftContent,
                    createdAt = now,
                    updatedAt = now,
                    lastModifiedLocal = now,
                    isSynced = false
                )
                db.noteDao().insertNote(copyNote)
            }
        }
    }

    /**
     * Descarga y restaura una copia de seguridad específica desde OneDrive.
     */
    suspend fun restoreOneDriveBackup(
        snapshot: BackupSnapshotInfo,
        mode: RestoreMode = RestoreMode.MERGE
    ): RestoreResult = withContext(Dispatchers.IO) {
        val fileId = snapshot.driveFileId?.ifBlank { null } ?: snapshot.id
        val tempFile = File(context.cacheDir, "temp_onedrive_restore_${snapshot.id}.zip")
        if (tempFile.exists()) tempFile.delete()

        try {
            val bytes = client.downloadBackupFile(fileId)
                ?: return@withContext RestoreResult.NotFound

            FileOutputStream(tempFile).use { it.write(bytes) }

            val fakeSnapshot = snapshot.copy(fileName = tempFile.name)
            val result = DriveRestoreUtil.restoreSpecificBackup(
                context = context,
                snapshot = fakeSnapshot,
                mode = mode,
                driveService = null
            )
            tempFile.delete()
            result
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "Error restaurando respaldo de OneDrive", e)
            RestoreResult.Error(e, "Error al restaurar desde OneDrive: ${e.localizedMessage}")
        }
    }

    /**
     * Elimina un archivo de copia de seguridad en OneDrive.
     */
    suspend fun deleteOneDriveBackup(snapshot: BackupSnapshotInfo): Boolean = withContext(Dispatchers.IO) {
        val fileId = snapshot.driveFileId?.ifBlank { null } ?: snapshot.id
        client.deleteBackupFile(fileId)
    }

    companion object {
        private const val TAG = "OneDriveBackupManager"
    }
}
