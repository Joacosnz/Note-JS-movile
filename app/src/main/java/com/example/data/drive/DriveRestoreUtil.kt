package com.example.data.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.google.api.services.drive.Drive
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Utilidad responsable de listar, descargar y restaurar copias de seguridad (tanto la última
 * como cualquier copia anterior del historial local o de Google Drive) al estilo WhatsApp.
 */
object DriveRestoreUtil {

    private const val TAG = "DriveRestoreUtil"
    private const val BACKUP_FILE_NAME = "backup.zip"

    /**
     * Obtiene la lista unificada de copias de seguridad disponibles (locales + Google Drive),
     * ordenadas de más reciente a más antigua.
     */
    suspend fun fetchAllAvailableBackups(
        context: Context,
        driveService: Drive? = null
    ): List<BackupSnapshotInfo> = withContext(Dispatchers.IO) {
        val localList = DriveBackupWorker.loadLocalBackupHistory(context).toMutableList()

        if (driveService != null) {
            try {
                val fileList = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("trashed = false and name contains 'backup' and name contains '.zip'")
                    .setFields("files(id, name, size, modifiedTime, description)")
                    .execute()

                val remoteFiles = fileList.files.orEmpty()
                for (rf in remoteFiles) {
                    val rName = rf.name ?: continue
                    if (rName == BACKUP_FILE_NAME && localList.isNotEmpty()) continue
                    val alreadyIndex = localList.indexOfFirst { it.fileName == rName }
                    if (alreadyIndex >= 0) {
                        val existing = localList[alreadyIndex]
                        if (existing.driveFileId.isNullOrBlank()) {
                            localList[alreadyIndex] = existing.copy(
                                isUploadedToCloud = true,
                                driveFileId = rf.id
                            )
                        }
                    } else {
                        val metaMap = parseDescriptionMeta(rf.description)
                        val ts = metaMap["ts"]?.toLongOrNull()
                            ?: rName.removePrefix("backup_").removeSuffix(".zip").toLongOrNull()
                            ?: rf.modifiedTime?.value
                            ?: System.currentTimeMillis()
                        val notesCount = metaMap["notes"]?.toIntOrNull() ?: 0
                        val remindersCount = metaMap["reminders"]?.toIntOrNull() ?: 0
                        val pdfsCount = metaMap["pdfs"]?.toIntOrNull() ?: 0
                        val trigger = metaMap["trigger"] ?: "CLOUD"

                        localList.add(
                            BackupSnapshotInfo(
                                id = rName.removeSuffix(".zip"),
                                fileName = rName,
                                timestamp = ts,
                                sizeBytes = rf.getSize()?.toLong() ?: 0L,
                                notesCount = notesCount,
                                remindersCount = remindersCount,
                                pdfsCount = pdfsCount,
                                triggerType = trigger,
                                accountEmail = DriveBackupWorker.getLastBackupAccount(context)
                                    .ifBlank { "Google Drive" },
                                isUploadedToCloud = true,
                                driveFileId = rf.id
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo consultar el historial remoto en Drive; mostrando copias locales", e)
            }
        }

        localList.sortedByDescending { it.timestamp }
    }

    private fun parseDescriptionMeta(description: String?): Map<String, String> {
        if (description.isNullOrBlank()) return emptyMap()
        return description.split(";")
            .mapNotNull { part ->
                val idx = part.indexOf(':')
                if (idx > 0 && idx < part.length - 1) {
                    part.substring(0, idx).trim() to part.substring(idx + 1).trim()
                } else null
            }
            .toMap()
    }

    /**
     * Elimina una copia de seguridad específica del historial local y, si aplica, de Google Drive.
     */
    suspend fun deleteBackupSnapshot(
        context: Context,
        snapshot: BackupSnapshotInfo,
        driveService: Drive? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val historyFile = File(DriveBackupWorker.getHistoryDir(context), snapshot.fileName)
            if (historyFile.exists()) {
                historyFile.delete()
            }
            if (snapshot.fileName == BACKUP_FILE_NAME) {
                val mainVault = DriveBackupWorker.getLocalCloudBackupFile(context)
                if (mainVault.exists()) mainVault.delete()
            }

            val updated = DriveBackupWorker.loadLocalBackupHistory(context)
                .filterNot { it.id == snapshot.id || it.fileName == snapshot.fileName }
            DriveBackupWorker.saveLocalBackupHistory(context, updated)

            if (driveService != null && !snapshot.driveFileId.isNullOrBlank()) {
                try {
                    driveService.files().delete(snapshot.driveFileId).execute()
                } catch (e: Exception) {
                    Log.w(TAG, "Aviso al borrar copia en Drive (${snapshot.driveFileId})", e)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando copia de seguridad ${snapshot.fileName}", e)
            false
        }
    }

    /**
     * Restaura una copia de seguridad específica seleccionada por el usuario desde el historial
     * de copias anteriores, permitiendo modo MERGE (sin perder notas nuevas) o REPLACE_ALL.
     */
    suspend fun restoreSpecificBackup(
        context: Context,
        snapshot: BackupSnapshotInfo,
        mode: RestoreMode = RestoreMode.MERGE,
        driveService: Drive? = null,
        dbName: String = DriveBackupWorker.DB_NAME
    ): RestoreResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_restore_${snapshot.id}.zip")
        if (tempFile.exists()) tempFile.delete()

        try {
            val historyFile = File(DriveBackupWorker.getHistoryDir(context), snapshot.fileName)
            val mainVaultFile = DriveBackupWorker.getLocalCloudBackupFile(context)

            var resolvedZipReady = false

            if (historyFile.exists() && historyFile.length() > 0L) {
                historyFile.copyTo(tempFile, overwrite = true)
                resolvedZipReady = true
            } else if (snapshot.fileName == BACKUP_FILE_NAME && mainVaultFile.exists() && mainVaultFile.length() > 0L) {
                mainVaultFile.copyTo(tempFile, overwrite = true)
                resolvedZipReady = true
            } else if (driveService != null) {
                try {
                    val fileIdToDownload = if (!snapshot.driveFileId.isNullOrBlank()) {
                        snapshot.driveFileId
                    } else {
                        val fileList = driveService.files().list()
                            .setSpaces("appDataFolder")
                            .setQ("name='${snapshot.fileName}' and trashed=false")
                            .execute()
                        fileList.files?.firstOrNull()?.id
                    }

                    if (!fileIdToDownload.isNullOrBlank()) {
                        FileOutputStream(tempFile).use { out ->
                            driveService.files().get(fileIdToDownload).executeMediaAndDownloadTo(out)
                        }
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            tempFile.copyTo(historyFile, overwrite = true)
                            resolvedZipReady = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallo descargando copia específica desde Drive", e)
                }
            }

            if (!resolvedZipReady && mainVaultFile.exists() && mainVaultFile.length() > 0L) {
                mainVaultFile.copyTo(tempFile, overwrite = true)
                resolvedZipReady = true
            }

            if (!resolvedZipReady || !tempFile.exists() || tempFile.length() == 0L) {
                return@withContext RestoreResult.NotFound
            }

            if (mode == RestoreMode.MERGE) {
                val mergedSuccess = tryRestoreFromStructuredJson(context, tempFile)
                if (mergedSuccess) {
                    tempFile.delete()
                    return@withContext RestoreResult.Success
                }
            }

            // Modo REPLACE_ALL o respaldo legado sin snapshot_data.json:
            extractDatabaseFilesFromZip(context, tempFile, dbName)
            tempFile.delete()
            RestoreResult.Success
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            Log.e(TAG, "Error restaurando copia específica ${snapshot.fileName}", e)
            RestoreResult.Error(e, "Error al restaurar: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    /**
     * Lee `snapshot_data.json` dentro del archivo ZIP y restaura todas las notas, recordatorios,
     * eventos y carpetas en la base de datos Room activa sin borrar notas creadas posteriormente.
     */
    private suspend fun tryRestoreFromStructuredJson(context: Context, zipFile: File): Boolean {
        return try {
            val jsonText = ZipFile(zipFile).use { zf ->
                val entry = zf.getEntry(DriveBackupWorker.SNAPSHOT_JSON_ENTRY) ?: return false
                zf.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
            }
            if (jsonText.isBlank()) return false

            val root = JSONObject(jsonText)
            val database = AppDatabase.getDatabase(context)
            val noteDao = database.noteDao()
            val reminderDao = database.reminderDao()
            val folderDao = database.folderDao()

            val activeUserId = try {
                FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID
            } catch (_: Exception) {
                NoteEntity.LOCAL_ANONYMOUS_USER_ID
            }

            // 1. Restaurar carpetas
            val foldersArr = root.optJSONArray("folders")
            if (foldersArr != null) {
                for (i in 0 until foldersArr.length()) {
                    val fObj = foldersArr.optJSONObject(i) ?: continue
                    val name = fObj.optString("name", "").trim()
                    if (name.isNotBlank()) {
                        folderDao.insertFolder(
                            FolderEntity(
                                name = name,
                                createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            // 2. Restaurar notas (combinando por remoteUuid o título/fecha)
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val nObj = notesArr.optJSONObject(i) ?: continue
                    val remoteUuid = nObj.optString("remoteUuid", "").ifBlank { UUID.randomUUID().toString() }
                    val existing = noteDao.getNoteByRemoteUuid(remoteUuid)

                    val imageUriStr = nObj.optString("imageUri", "")
                    val reminderTs = if (nObj.has("reminderTimestamp") && !nObj.isNull("reminderTimestamp")) {
                        nObj.optLong("reminderTimestamp")
                    } else null

                    val restoredNote = NoteEntity(
                        id = existing?.id ?: 0L,
                        remoteUuid = remoteUuid,
                        userId = activeUserId,
                        isSynced = false,
                        pendingDelete = false,
                        lastModifiedLocal = System.currentTimeMillis(),
                        updatedAtServer = 0L,
                        title = nObj.optString("title", ""),
                        content = nObj.optString("content", ""),
                        noteType = nObj.optString("noteType", "TEXT"),
                        colorIndex = nObj.optInt("colorIndex", 0),
                        paperStyle = nObj.optString("paperStyle", "BLANK"),
                        isPinned = nObj.optBoolean("isPinned", false),
                        isArchived = nObj.optBoolean("isArchived", false),
                        isTrashed = nObj.optBoolean("isTrashed", false),
                        isLocked = nObj.optBoolean("isLocked", false),
                        lockPin = nObj.optString("lockPin", ""),
                        tags = nObj.optString("tags", ""),
                        folder = nObj.optString("folder", "Notas"),
                        checklistData = nObj.optString("checklistData", ""),
                        drawingData = nObj.optString("drawingData", ""),
                        mathEvaluations = nObj.optString("mathEvaluations", ""),
                        pdfPath = nObj.optString("pdfPath", ""),
                        pdfAnnotationData = nObj.optString("pdfAnnotationData", ""),
                        reminderTimestamp = reminderTs,
                        isReminderDone = nObj.optBoolean("isReminderDone", false),
                        createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis()),
                        imageUri = if (imageUriStr.isNotBlank()) Uri.parse(imageUriStr) else null,
                        remoteImageUrl = nObj.optString("remoteImageUrl", ""),
                        pdfExtractedText = nObj.optString("pdfExtractedText", "").ifBlank { null },
                        remoteUrl = nObj.optString("remoteUrl", ""),
                        attachmentsJson = nObj.optString("attachmentsJson", "[]"),
                        searchableText = nObj.optString("searchableText", ""),
                        syncEnabled = nObj.optBoolean("syncEnabled", true),
                        icon = nObj.optString("icon", "").ifBlank { null }
                    )

                    if (existing == null || existing.isTrashed || existing.pendingDelete || restoredNote.updatedAt >= existing.updatedAt) {
                        noteDao.insertNote(restoredNote)
                    }
                }
            }

            // 3. Restaurar recordatorios y eventos de calendario
            val remindersArr = root.optJSONArray("reminders")
            if (remindersArr != null) {
                for (i in 0 until remindersArr.length()) {
                    val rObj = remindersArr.optJSONObject(i) ?: continue
                    val remoteUuid = rObj.optString("remoteUuid", "").ifBlank { UUID.randomUUID().toString() }
                    val existingRem = reminderDao.getReminderByRemoteUuid(remoteUuid)

                    val tagsList = mutableListOf<String>()
                    val tagsJson = rObj.optJSONArray("tags")
                    if (tagsJson != null) {
                        for (t in 0 until tagsJson.length()) {
                            val tagStr = tagsJson.optString(t, "").trim()
                            if (tagStr.isNotBlank()) tagsList.add(tagStr)
                        }
                    }

                    val linkedNoteId = if (rObj.has("linkedNoteId") && !rObj.isNull("linkedNoteId")) {
                        rObj.optLong("linkedNoteId")
                    } else null

                    val restoredReminder = ReminderEntity(
                        id = existingRem?.id ?: 0L,
                        remoteUuid = remoteUuid,
                        userId = activeUserId,
                        itemType = rObj.optString("itemType", "REMINDER"),
                        title = rObj.optString("title", "Recordatorio"),
                        notes = rObj.optString("notes", ""),
                        location = rObj.optString("location", ""),
                        scheduledAt = rObj.optLong("scheduledAt", System.currentTimeMillis()),
                        endTimeMillis = rObj.optLong("endTimeMillis", 0L),
                        isAllDay = rObj.optBoolean("isAllDay", false),
                        isCompleted = rObj.optBoolean("isCompleted", false),
                        completedAt = rObj.optLong("completedAt", 0L),
                        priority = rObj.optInt("priority", 0),
                        listName = rObj.optString("listName", "Personal"),
                        tags = tagsList,
                        colorHex = rObj.optLong("colorHex", 0xFF007AFFL),
                        recurrenceRule = rObj.optString("recurrenceRule", "NONE"),
                        subtasksJson = rObj.optString("subtasksJson", "[]"),
                        linkedNoteId = linkedNoteId,
                        linkedNoteUuid = rObj.optString("linkedNoteUuid", ""),
                        linkedNoteTitle = rObj.optString("linkedNoteTitle", ""),
                        alertOffsetMinutes = rObj.optInt("alertOffsetMinutes", 0),
                        isSynced = false,
                        pendingDelete = false,
                        createdAt = rObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = rObj.optLong("updatedAt", System.currentTimeMillis()),
                        lastModifiedLocal = System.currentTimeMillis(),
                        updatedAtServer = 0L
                    )

                    if (existingRem == null || existingRem.pendingDelete || restoredReminder.updatedAt >= existingRem.updatedAt) {
                        reminderDao.insertReminder(restoredReminder)
                    }
                }
            }

            true
        } catch (e: Exception) {
            Log.w(TAG, "Restauración JSON estructurada no aplicable, usando extracción SQLite directa", e)
            false
        }
    }

    private fun extractDatabaseFilesFromZip(context: Context, zipFile: File, dbName: String) {
        AppDatabase.closeDatabase()

        val dbParentFolder = context.getDatabasePath(dbName).parentFile
            ?: File(context.applicationInfo.dataDir, "databases")

        if (!dbParentFolder.exists()) {
            dbParentFolder.mkdirs()
        }

        zipFile.inputStream().use { fileIn ->
            ZipInputStream(fileIn).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name != DriveBackupWorker.SNAPSHOT_JSON_ENTRY) {
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
    }

    /**
     * Descarga y restaura el archivo de copia de seguridad más reciente (backup.zip)
     * desde Note JS Cloud / Google Drive o desde el historial local.
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
                    val latestHistory = DriveBackupWorker.loadLocalBackupHistory(context).firstOrNull()
                    val histFile = latestHistory?.let { File(DriveBackupWorker.getHistoryDir(context), it.fileName) }
                    if (histFile != null && histFile.exists() && histFile.length() > 0L) {
                        histFile.copyTo(tempFile, overwrite = true)
                    } else {
                        Log.w(TAG, "No se encontró ningún archivo $BACKUP_FILE_NAME en Drive ni en la bóveda local.")
                        return@withContext RestoreResult.NotFound
                    }
                }
            }

            extractDatabaseFilesFromZip(context, tempFile, dbName)

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
