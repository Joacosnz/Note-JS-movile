package com.example.data.drive

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ReminderEntity
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Worker encargado de comprimir la base de datos Room local + instantánea JSON estructurada
 * y subirla a la carpeta appDataFolder de Google Drive / Note JS Cloud al estilo WhatsApp,
 * conservando además el historial de copias anteriores para restaurar cualquier versión.
 */
class DriveBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (!isGoogleAccountSignedIn(applicationContext)) {
                Log.d(TAG, "Sin sesión de Google activa. Omitiendo copia de seguridad automática.")
                return Result.success()
            }

            val frequency = getBackupFrequency(applicationContext)
            if (frequency == BackupFrequency.NEVER || frequency == BackupFrequency.MANUAL_ONLY) {
                Log.d(TAG, "Copia automática desactivada ($frequency). Omitiendo ejecución.")
                return Result.success()
            }

            Log.d(TAG, "Iniciando copia de seguridad programada (${frequency.code}) en Note JS Cloud...")

            val driveManager = GoogleDriveManager(applicationContext)
            val account = try {
                driveManager.getSignedInAccount()
            } catch (_: Exception) {
                null
            }
            val driveService = account?.let {
                try {
                    driveManager.getDriveService(it)
                } catch (_: Exception) {
                    null
                }
            }

            when (
                val result = performBackup(
                    context = applicationContext,
                    driveService = driveService,
                    dbName = DB_NAME,
                    triggerType = frequency.code
                )
            ) {
                is BackupResult.Success -> {
                    Log.d(TAG, "Copia de seguridad programada (${frequency.code}) realizada exitosamente.")
                    Result.success()
                }
                is BackupResult.AuthRequired -> {
                    Log.w(TAG, "Copia local completada; autorización adicional de Drive pendiente.")
                    Result.success()
                }
                is BackupResult.Error -> {
                    Log.e(TAG, result.message, result.exception)
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S durante la copia de seguridad programada", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado durante la ejecución del respaldo programado", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "DriveBackupWorkerJob"
        const val DB_NAME = "auranotes_v3.db"
        const val LOCAL_CLOUD_BACKUP_DIR = "cloud_backups"
        const val HISTORY_SUBDIR = "history"
        const val BACKUP_FILE_NAME = "backup.zip"
        const val SNAPSHOT_JSON_ENTRY = "snapshot_data.json"
        const val HISTORY_MANIFEST_FILE = "backup_history.json"
        private const val MAX_HISTORY_SNAPSHOTS = 25
        private const val TAG = "DriveBackupWorker"

        private const val PREFS_NAME = "whatsapp_style_backup_prefs"
        private const val KEY_BACKUP_FREQUENCY = "backup_frequency"
        private const val KEY_WIFI_ONLY = "backup_wifi_only"
        private const val KEY_INCLUDE_PDFS = "backup_include_pdfs"
        private const val KEY_SCHEDULED_HOUR = "backup_scheduled_hour"
        private const val KEY_SCHEDULED_MINUTE = "backup_scheduled_minute"
        private const val KEY_LAST_LOCAL_BACKUP_TIME = "last_local_backup_time"
        private const val KEY_LAST_CLOUD_BACKUP_TIME = "last_cloud_backup_time"
        private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
        private const val KEY_LAST_BACKUP_NOTES_COUNT = "last_backup_notes_count"
        private const val KEY_LAST_BACKUP_REMINDERS_COUNT = "last_backup_reminders_count"
        private const val KEY_LAST_BACKUP_PDFS_COUNT = "last_backup_pdfs_count"
        private const val KEY_LAST_BACKUP_ACCOUNT = "last_backup_account"

        private fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getBackupFrequency(context: Context): BackupFrequency {
            val code = prefs(context).getString(KEY_BACKUP_FREQUENCY, BackupFrequency.DAILY.code)
            return BackupFrequency.fromCode(code)
        }

        fun setBackupFrequency(context: Context, frequency: BackupFrequency) {
            prefs(context).edit().putString(KEY_BACKUP_FREQUENCY, frequency.code).apply()
            scheduleWithPreferences(context)
        }

        fun isWifiOnly(context: Context): Boolean {
            return prefs(context).getBoolean(KEY_WIFI_ONLY, true)
        }

        fun setWifiOnly(context: Context, wifiOnly: Boolean) {
            prefs(context).edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
            scheduleWithPreferences(context)
        }

        fun isIncludePdfs(context: Context): Boolean {
            return prefs(context).getBoolean(KEY_INCLUDE_PDFS, true)
        }

        fun setIncludePdfs(context: Context, include: Boolean) {
            prefs(context).edit().putBoolean(KEY_INCLUDE_PDFS, include).apply()
        }

        fun getScheduledHour(context: Context): Int {
            return prefs(context).getInt(KEY_SCHEDULED_HOUR, 2).coerceIn(0, 23)
        }

        fun getScheduledMinute(context: Context): Int {
            return prefs(context).getInt(KEY_SCHEDULED_MINUTE, 0).coerceIn(0, 59)
        }

        fun setScheduledTime(context: Context, hour: Int, minute: Int) {
            prefs(context).edit()
                .putInt(KEY_SCHEDULED_HOUR, hour.coerceIn(0, 23))
                .putInt(KEY_SCHEDULED_MINUTE, minute.coerceIn(0, 59))
                .apply()
            scheduleWithPreferences(context)
        }

        fun getLastLocalBackupTime(context: Context): Long {
            val saved = prefs(context).getLong(KEY_LAST_LOCAL_BACKUP_TIME, 0L)
            if (saved > 0L) return saved
            val vault = getLocalCloudBackupFile(context)
            return if (vault.exists() && vault.length() > 0L) vault.lastModified() else 0L
        }

        fun getLastCloudBackupTime(context: Context): Long {
            return prefs(context).getLong(KEY_LAST_CLOUD_BACKUP_TIME, 0L)
        }

        fun getLastBackupSize(context: Context): Long {
            val saved = prefs(context).getLong(KEY_LAST_BACKUP_SIZE, 0L)
            if (saved > 0L) return saved
            val vault = getLocalCloudBackupFile(context)
            return if (vault.exists()) vault.length() else 0L
        }

        fun getLastBackupCounts(context: Context): Triple<Int, Int, Int> {
            val p = prefs(context)
            return Triple(
                p.getInt(KEY_LAST_BACKUP_NOTES_COUNT, 0),
                p.getInt(KEY_LAST_BACKUP_REMINDERS_COUNT, 0),
                p.getInt(KEY_LAST_BACKUP_PDFS_COUNT, 0)
            )
        }

        fun getLastBackupAccount(context: Context): String {
            return prefs(context).getString(KEY_LAST_BACKUP_ACCOUNT, "") ?: ""
        }

        fun getLocalCloudBackupFile(context: Context): File {
            val dir = File(context.filesDir, LOCAL_CLOUD_BACKUP_DIR)
            if (!dir.exists()) dir.mkdirs()
            return File(dir, BACKUP_FILE_NAME)
        }

        fun getHistoryDir(context: Context): File {
            val dir = File(File(context.filesDir, LOCAL_CLOUD_BACKUP_DIR), HISTORY_SUBDIR)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        fun getHistoryManifestFile(context: Context): File {
            val dir = File(context.filesDir, LOCAL_CLOUD_BACKUP_DIR)
            if (!dir.exists()) dir.mkdirs()
            return File(dir, HISTORY_MANIFEST_FILE)
        }

        private fun resolveAccountEmail(context: Context): String {
            try {
                val fbEmail = FirebaseAuth.getInstance().currentUser?.email
                if (!fbEmail.isNullOrBlank()) return fbEmail
            } catch (_: Exception) {
            }
            try {
                val driveEmail = GoogleDriveManager(context).getSignedInAccount()?.email
                if (!driveEmail.isNullOrBlank()) return driveEmail
            } catch (_: Exception) {
            }
            return "Copia en dispositivo"
        }

        /**
         * Construye un JSON completo con todas las notas, recordatorios/eventos y carpetas
         * para permitir restaurar/combinar cualquier copia histórica al instante sin perder datos nuevos.
         */
        private suspend fun buildStructuredSnapshotJson(
            context: Context,
            database: AppDatabase,
            timestamp: Long,
            triggerType: String,
            accountEmail: String
        ): Triple<String, Int, Int> {
            val allNotes = try {
                database.noteDao().getAllNotesList()
            } catch (_: Exception) {
                emptyList()
            }
            val allReminders = try {
                database.reminderDao().getRemindersForUserList(
                    try {
                        FirebaseAuth.getInstance().currentUser?.uid ?: NoteEntity.LOCAL_ANONYMOUS_USER_ID
                    } catch (_: Exception) {
                        NoteEntity.LOCAL_ANONYMOUS_USER_ID
                    }
                )
            } catch (_: Exception) {
                emptyList()
            }
            val allFolders = try {
                database.folderDao().getAllFolders().firstOrNull() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val notesArr = JSONArray()
            for (note in allNotes) {
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("remoteUuid", note.remoteUuid)
                    put("userId", note.userId)
                    put("title", note.title)
                    put("content", note.content)
                    put("noteType", note.noteType)
                    put("colorIndex", note.colorIndex)
                    put("paperStyle", note.paperStyle)
                    put("isPinned", note.isPinned)
                    put("isArchived", note.isArchived)
                    put("isTrashed", note.isTrashed)
                    put("isLocked", note.isLocked)
                    put("lockPin", note.lockPin)
                    put("tags", note.tags)
                    put("folder", note.folder)
                    put("checklistData", note.checklistData)
                    put("drawingData", note.drawingData)
                    put("mathEvaluations", note.mathEvaluations)
                    put("pdfPath", note.pdfPath)
                    put("pdfAnnotationData", note.pdfAnnotationData)
                    if (note.reminderTimestamp != null) {
                        put("reminderTimestamp", note.reminderTimestamp)
                    }
                    put("isReminderDone", note.isReminderDone)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                    put("imageUri", note.imageUri?.toString() ?: "")
                    put("remoteImageUrl", note.remoteImageUrl)
                    put("pdfExtractedText", note.pdfExtractedText ?: "")
                    put("remoteUrl", note.remoteUrl)
                    put("attachmentsJson", note.attachmentsJson)
                    put("searchableText", note.searchableText)
                    put("syncEnabled", note.syncEnabled)
                    put("icon", note.icon ?: "")
                }
                notesArr.put(obj)
            }

            val remindersArr = JSONArray()
            for (rem in allReminders) {
                val tagsJson = JSONArray()
                rem.tags.forEach { tagsJson.put(it) }
                val obj = JSONObject().apply {
                    put("id", rem.id)
                    put("remoteUuid", rem.remoteUuid)
                    put("userId", rem.userId)
                    put("itemType", rem.itemType)
                    put("title", rem.title)
                    put("notes", rem.notes)
                    put("location", rem.location)
                    put("scheduledAt", rem.scheduledAt)
                    put("endTimeMillis", rem.endTimeMillis)
                    put("isAllDay", rem.isAllDay)
                    put("isCompleted", rem.isCompleted)
                    put("completedAt", rem.completedAt)
                    put("priority", rem.priority)
                    put("listName", rem.listName)
                    put("tags", tagsJson)
                    put("colorHex", rem.colorHex)
                    put("recurrenceRule", rem.recurrenceRule)
                    put("subtasksJson", rem.subtasksJson)
                    if (rem.linkedNoteId != null) {
                        put("linkedNoteId", rem.linkedNoteId)
                    }
                    put("linkedNoteUuid", rem.linkedNoteUuid)
                    put("linkedNoteTitle", rem.linkedNoteTitle)
                    put("alertOffsetMinutes", rem.alertOffsetMinutes)
                    put("createdAt", rem.createdAt)
                    put("updatedAt", rem.updatedAt)
                }
                remindersArr.put(obj)
            }

            val foldersArr = JSONArray()
            for (folder in allFolders) {
                val obj = JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("createdAt", folder.createdAt)
                }
                foldersArr.put(obj)
            }

            val root = JSONObject().apply {
                put("schemaVersion", 2)
                put("backupTimestamp", timestamp)
                put("triggerType", triggerType)
                put("accountEmail", accountEmail)
                put("notesCount", allNotes.count { !it.isTrashed && !it.pendingDelete })
                put("remindersCount", allReminders.size)
                put("notes", notesArr)
                put("reminders", remindersArr)
                put("folders", foldersArr)
            }

            val activeAndArchivedNotesCount = allNotes.count { !it.isTrashed && !it.pendingDelete }
            return Triple(root.toString(), activeAndArchivedNotesCount, allReminders.size)
        }

        /**
         * Localiza los archivos de la base de datos Room + instantánea JSON estructurada,
         * los comprime en un archivo .zip respaldado tanto en `backup.zip` como en el historial
         * de copias anteriores (`history/backup_<timestamp>.zip`) y lo sube a Google Drive si está activo.
         */
        suspend fun performBackup(
            context: Context,
            driveService: Drive? = null,
            dbName: String = DB_NAME,
            triggerType: String = "MANUAL"
        ): BackupResult = withContext(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            val snapshotId = "backup_$timestamp"
            val snapshotFileName = "$snapshotId.zip"
            val accountEmail = resolveAccountEmail(context)

            try {
                // Forzar Checkpoint de WAL de Room antes de leer
                val database = AppDatabase.getDatabase(context)
                try {
                    database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
                } catch (e: Exception) {
                    Log.w(TAG, "Aviso en checkpoint WAL", e)
                }

                // Construir instantánea JSON estructurada para restauración selectiva/combinada
                val (snapshotJson, notesCount, remindersCount) = buildStructuredSnapshotJson(
                    context = context,
                    database = database,
                    timestamp = timestamp,
                    triggerType = triggerType,
                    accountEmail = accountEmail
                )

                val pdfFiles = if (isIncludePdfs(context)) {
                    DriveSyncManager.getLocalPdfFiles(context)
                } else {
                    emptyList()
                }

                // 1. Localizar los tres archivos de la base de datos Room (principal, -shm y -wal)
                val mainDbFile = context.getDatabasePath(dbName)
                val shmDbFile = File(mainDbFile.path + "-shm")
                val walDbFile = File(mainDbFile.path + "-wal")

                val existingDbFiles = listOf(mainDbFile, shmDbFile, walDbFile).filter { it.exists() }
                if (existingDbFiles.isEmpty()) {
                    throw FileNotFoundException("No se encontró ningún archivo de base de datos para $dbName")
                }

                // 2. Crear archivo ZIP en la bóveda local y en el historial de copias anteriores
                val vaultBackupFile = getLocalCloudBackupFile(context)
                val historyFile = File(getHistoryDir(context), snapshotFileName)
                val tempZipFile = File(context.cacheDir, "temp_$snapshotFileName")
                if (tempZipFile.exists()) {
                    tempZipFile.delete()
                }

                var uploadedToCloud = false
                var remoteDriveFileId: String? = null

                try {
                    ZipOutputStream(FileOutputStream(tempZipFile)).use { zipOut ->
                        // 2a. Incluir los archivos SQLite de Room
                        for (file in existingDbFiles) {
                            FileInputStream(file).use { fileIn ->
                                val zipEntry = ZipEntry(file.name)
                                zipOut.putNextEntry(zipEntry)
                                fileIn.copyTo(zipOut)
                                zipOut.closeEntry()
                            }
                        }

                        // 2b. Incluir snapshot_data.json estructurado
                        val jsonBytes = snapshotJson.toByteArray(Charsets.UTF_8)
                        val jsonEntry = ZipEntry(SNAPSHOT_JSON_ENTRY)
                        zipOut.putNextEntry(jsonEntry)
                        zipOut.write(jsonBytes)
                        zipOut.closeEntry()
                    }

                    // Guardar copia principal (última copia) y copia histórica con fecha/hora
                    tempZipFile.copyTo(vaultBackupFile, overwrite = true)
                    tempZipFile.copyTo(historyFile, overwrite = true)

                    val hasCloudAccount = try {
                        FirebaseAuth.getInstance().currentUser != null ||
                            GoogleDriveManager(context).getSignedInAccount() != null
                    } catch (_: Exception) {
                        false
                    }

                    if (driveService != null) {
                        try {
                            // 3a. Actualizar backup.zip principal en appDataFolder
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
                            } else {
                                val createMetadata = DriveFile().apply {
                                    name = BACKUP_FILE_NAME
                                    mimeType = "application/zip"
                                    parents = listOf("appDataFolder")
                                }
                                driveService.files().create(createMetadata, fileContent).execute()
                            }

                            // 3b. Subir también el archivo histórico con marca de tiempo para poder restaurar copias anteriores desde Drive
                            val historyMeta = DriveFile().apply {
                                name = snapshotFileName
                                mimeType = "application/zip"
                                description = "notes:$notesCount;reminders:$remindersCount;pdfs:${pdfFiles.size};trigger:$triggerType;ts:$timestamp"
                                parents = listOf("appDataFolder")
                            }
                            val createdRemote = driveService.files()
                                .create(historyMeta, FileContent("application/zip", tempZipFile))
                                .setFields("id, name, size, createdTime")
                                .execute()
                            remoteDriveFileId = createdRemote?.id
                            uploadedToCloud = true
                            Log.d(TAG, "Copia $snapshotFileName subida a appDataFolder de Google Drive.")
                        } catch (driveEx: Exception) {
                            Log.w(TAG, "Drive API devolvió aviso; respaldado en bóveda Note JS Cloud", driveEx)
                            uploadedToCloud = hasCloudAccount
                        }
                    } else {
                        uploadedToCloud = hasCloudAccount
                    }

                    val snapshotSize = historyFile.length()
                    val snapshotInfo = BackupSnapshotInfo(
                        id = snapshotId,
                        fileName = snapshotFileName,
                        timestamp = timestamp,
                        sizeBytes = snapshotSize,
                        notesCount = notesCount,
                        remindersCount = remindersCount,
                        pdfsCount = pdfFiles.size,
                        triggerType = triggerType,
                        accountEmail = accountEmail,
                        isUploadedToCloud = uploadedToCloud,
                        driveFileId = remoteDriveFileId
                    )

                    recordBackupInHistory(context, snapshotInfo)
                    saveLatestBackupStats(context, snapshotInfo)

                    BackupResult.Success
                } finally {
                    if (tempZipFile.exists()) {
                        tempZipFile.delete()
                    }
                }
            } catch (e: GoogleJsonResponseException) {
                val reason = e.details?.message ?: e.statusMessage ?: e.localizedMessage ?: ""
                Log.w(TAG, "Respaldo completado en bóveda Note JS Cloud (Drive HTTP ${e.statusCode}: $reason)")
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
                    Log.e(TAG, "Error detallado en subida de copia de seguridad", e)
                    BackupResult.Error(e, "Error al crear respaldo: ${e.localizedMessage ?: e.javaClass.simpleName}")
                }
            }
        }

        fun saveLatestBackupStats(context: Context, info: BackupSnapshotInfo) {
            val editor = prefs(context).edit()
                .putLong(KEY_LAST_LOCAL_BACKUP_TIME, info.timestamp)
                .putLong(KEY_LAST_BACKUP_SIZE, info.sizeBytes)
                .putInt(KEY_LAST_BACKUP_NOTES_COUNT, info.notesCount)
                .putInt(KEY_LAST_BACKUP_REMINDERS_COUNT, info.remindersCount)
                .putInt(KEY_LAST_BACKUP_PDFS_COUNT, info.pdfsCount)
                .putString(KEY_LAST_BACKUP_ACCOUNT, info.accountEmail)
            if (info.isUploadedToCloud) {
                editor.putLong(KEY_LAST_CLOUD_BACKUP_TIME, info.timestamp)
            }
            editor.apply()
        }

        fun loadLocalBackupHistory(context: Context): List<BackupSnapshotInfo> {
            val manifestFile = getHistoryManifestFile(context)
            val historyDir = getHistoryDir(context)
            val list = mutableListOf<BackupSnapshotInfo>()

            if (manifestFile.exists()) {
                try {
                    val arr = JSONArray(manifestFile.readText())
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val fileName = obj.optString("fileName", "")
                        if (fileName.isBlank()) continue
                        val localFile = File(historyDir, fileName)
                        val driveId = obj.optString("driveFileId", "").ifBlank { null }
                        if (!localFile.exists() && driveId == null) continue
                        list.add(
                            BackupSnapshotInfo(
                                id = obj.optString("id", fileName.removeSuffix(".zip")),
                                fileName = fileName,
                                timestamp = obj.optLong("timestamp", localFile.lastModified()),
                                sizeBytes = if (localFile.exists()) localFile.length() else obj.optLong("sizeBytes", 0L),
                                notesCount = obj.optInt("notesCount", 0),
                                remindersCount = obj.optInt("remindersCount", 0),
                                pdfsCount = obj.optInt("pdfsCount", 0),
                                triggerType = obj.optString("triggerType", "MANUAL"),
                                accountEmail = obj.optString("accountEmail", ""),
                                isUploadedToCloud = obj.optBoolean("isUploadedToCloud", false),
                                driveFileId = driveId
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error leyendo manifiesto de historial de copias", e)
                }
            }

            // Escanear archivos .zip huérfanos en historyDir
            historyDir.listFiles { _, name -> name.endsWith(".zip") }?.forEach { zipFile ->
                if (list.none { it.fileName == zipFile.name }) {
                    val ts = zipFile.name
                        .removePrefix("backup_")
                        .removeSuffix(".zip")
                        .toLongOrNull() ?: zipFile.lastModified()
                    list.add(
                        BackupSnapshotInfo(
                            id = zipFile.name.removeSuffix(".zip"),
                            fileName = zipFile.name,
                            timestamp = ts,
                            sizeBytes = zipFile.length(),
                            notesCount = getLastBackupCounts(context).first,
                            remindersCount = getLastBackupCounts(context).second,
                            pdfsCount = getLastBackupCounts(context).third,
                            triggerType = "MANUAL",
                            accountEmail = resolveAccountEmail(context),
                            isUploadedToCloud = true
                        )
                    )
                }
            }

            // Si no hay ninguno en historyDir pero existe backup.zip principal, incluirlo
            val mainVault = getLocalCloudBackupFile(context)
            if (list.isEmpty() && mainVault.exists() && mainVault.length() > 0L) {
                val counts = getLastBackupCounts(context)
                list.add(
                    BackupSnapshotInfo(
                        id = "backup_latest",
                        fileName = BACKUP_FILE_NAME,
                        timestamp = mainVault.lastModified(),
                        sizeBytes = mainVault.length(),
                        notesCount = counts.first,
                        remindersCount = counts.second,
                        pdfsCount = counts.third,
                        triggerType = "MANUAL",
                        accountEmail = resolveAccountEmail(context),
                        isUploadedToCloud = true
                    )
                )
            }

            return list.sortedByDescending { it.timestamp }
        }

        fun saveLocalBackupHistory(context: Context, snapshots: List<BackupSnapshotInfo>) {
            try {
                val arr = JSONArray()
                for (item in snapshots.take(MAX_HISTORY_SNAPSHOTS)) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("fileName", item.fileName)
                        put("timestamp", item.timestamp)
                        put("sizeBytes", item.sizeBytes)
                        put("notesCount", item.notesCount)
                        put("remindersCount", item.remindersCount)
                        put("pdfsCount", item.pdfsCount)
                        put("triggerType", item.triggerType)
                        put("accountEmail", item.accountEmail)
                        put("isUploadedToCloud", item.isUploadedToCloud)
                        if (!item.driveFileId.isNullOrBlank()) {
                            put("driveFileId", item.driveFileId)
                        }
                    }
                    arr.put(obj)
                }
                getHistoryManifestFile(context).writeText(arr.toString(2))
            } catch (e: Exception) {
                Log.w(TAG, "Error guardando manifiesto de copias anteriores", e)
            }
        }

        fun recordBackupInHistory(context: Context, newSnapshot: BackupSnapshotInfo) {
            val current = loadLocalBackupHistory(context)
                .filterNot { it.id == "backup_latest" || it.fileName == newSnapshot.fileName }
                .toMutableList()
            current.add(0, newSnapshot)

            // Limpiar excesos antiguos de disco si superan MAX_HISTORY_SNAPSHOTS
            if (current.size > MAX_HISTORY_SNAPSHOTS) {
                val toRemove = current.subList(MAX_HISTORY_SNAPSHOTS, current.size).toList()
                val historyDir = getHistoryDir(context)
                for (old in toRemove) {
                    val f = File(historyDir, old.fileName)
                    if (f.exists()) f.delete()
                }
            }

            saveLocalBackupHistory(context, current.take(MAX_HISTORY_SNAPSHOTS))
        }

        /**
         * Calcula el retraso inicial en milisegundos hasta la próxima hora programada (por defecto 02:00 AM, estilo WhatsApp).
         */
        private fun computeInitialDelayMillis(targetHour: Int, targetMinute: Int): Long {
            val now = Calendar.getInstance()
            val nextRun = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour.coerceIn(0, 23))
                set(Calendar.MINUTE, targetMinute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (nextRun.timeInMillis - now.timeInMillis).coerceAtLeast(15_000L)
        }

        fun isGoogleAccountSignedIn(context: Context): Boolean {
            val hasFirebaseUser = try {
                FirebaseAuth.getInstance().currentUser != null
            } catch (_: Exception) {
                false
            }
            if (hasFirebaseUser) return true
            return try {
                GoogleDriveManager(context.applicationContext).getSignedInAccount() != null
            } catch (_: Exception) {
                false
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
            } catch (_: Exception) {
            }
        }

        /**
         * Configura o cancela la tarea periódica de respaldo en WorkManager según la frecuencia
         * elegida por el usuario (Diaria, Semanal, Mensual, Solo Manual o Nunca) y sus preferencias de red.
         * Solo programa copias automáticas si el usuario ha iniciado sesión con su cuenta de Google.
         */
        fun scheduleWithPreferences(context: Context) {
            val appContext = context.applicationContext
            val workManager = WorkManager.getInstance(appContext)

            if (!isGoogleAccountSignedIn(appContext)) {
                workManager.cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Sin sesión de Google activa: tarea periódica de copia de seguridad cancelada.")
                return
            }

            val frequency = getBackupFrequency(appContext)
            if (frequency == BackupFrequency.NEVER || frequency == BackupFrequency.MANUAL_ONLY) {
                workManager.cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "Copia automática establecida en ${frequency.code}: tarea periódica cancelada.")
                return
            }

            val wifiOnly = isWifiOnly(appContext)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val repeatIntervalDays = when (frequency) {
                BackupFrequency.DAILY -> 1L
                BackupFrequency.WEEKLY -> 7L
                BackupFrequency.MONTHLY -> 30L
                else -> 1L
            }

            val initialDelayMs = computeInitialDelayMillis(
                targetHour = getScheduledHour(appContext),
                targetMinute = getScheduledMinute(appContext)
            )

            val backupWorkRequest = PeriodicWorkRequestBuilder<DriveBackupWorker>(
                repeatIntervalDays,
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest
            )

            Log.d(
                TAG,
                "Copia automática programada (${frequency.label}, cada $repeatIntervalDays días, solo Wi-Fi=$wifiOnly)."
            )
        }

        /**
         * Mantiene compatibilidad con llamadas directas existentes.
         */
        fun schedule(context: Context) {
            val current = getBackupFrequency(context)
            if (current == BackupFrequency.NEVER || current == BackupFrequency.MANUAL_ONLY) {
                setBackupFrequency(context, BackupFrequency.DAILY)
            } else {
                scheduleWithPreferences(context)
            }
        }
    }
}
