package com.example.data.drive

import android.content.Context
import android.util.Log
import com.example.data.local.entity.NoteEntity
import com.example.model.AudioBlock
import com.example.model.NoteBlock
import com.example.util.NoteBlockSerializer
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resultado de la sincronización manual de Notas y PDFs con Google Drive.
 */
data class DriveSyncResult(
    val success: Boolean,
    val notesSynced: Int,
    val pdfsSynced: Int,
    val totalItems: Int,
    val message: String,
    val error: Throwable? = null
)

/**
 * Estado y progreso en tiempo real de la sincronización con Google Drive.
 */
sealed class DriveSyncProgress {
    object Idle : DriveSyncProgress()
    data class Syncing(
        val phase: String,
        val currentItemName: String,
        val current: Int,
        val total: Int,
        val percent: Float
    ) : DriveSyncProgress()
    data class Completed(val result: DriveSyncResult) : DriveSyncProgress()
}

/**
 * Gestor encargado de sincronizar de forma manual las notas locales y los archivos PDF
 * con la cuenta de Google Drive del usuario, organizándolos en carpetas dedicadas.
 */
object DriveSyncManager {

    private const val TAG = "DriveSyncManager"
    private const val ROOT_FOLDER_NAME = "Note JS"
    private const val NOTES_FOLDER_NAME = "Notes"
    private const val PDFS_FOLDER_NAME = "PDFs"
    private const val PREFS_NAME = "notejs_drive_sync_prefs"
    private const val KEY_LAST_SYNC = "last_drive_sync_time"

    /**
     * Obtiene la marca de tiempo de la última sincronización manual exitosa.
     */
    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    /**
     * Guarda la marca de tiempo de sincronización.
     */
    fun setLastSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    /**
     * Recupera todos los archivos PDF almacenados localmente en la app.
     */
    fun getLocalPdfFiles(context: Context): List<File> {
        val pdfList = mutableListOf<File>()
        try {
            val pdfsDir = File(context.filesDir, "pdfs")
            if (pdfsDir.exists() && pdfsDir.isDirectory) {
                pdfsDir.listFiles { _, name -> name.lowercase().endsWith(".pdf") }?.let {
                    pdfList.addAll(it)
                }
            }

            // También incluir PDFs en cache/shared_pdfs si existen
            val sharedDir = File(context.cacheDir, "shared_pdfs")
            if (sharedDir.exists() && sharedDir.isDirectory) {
                sharedDir.listFiles { _, name -> name.lowercase().endsWith(".pdf") }?.let { files ->
                    for (file in files) {
                        if (pdfList.none { it.name == file.name }) {
                            pdfList.add(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listando PDFs locales", e)
        }
        return pdfList
    }

    /**
     * Busca o crea una carpeta en Google Drive por nombre y padre opcional.
     */
    private suspend fun getOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentId: String? = null
    ): String = withContext(Dispatchers.IO) {
        val escapedName = folderName.replace("'", "\\'")
        val query = if (parentId == null) {
            "name = '$escapedName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        } else {
            "name = '$escapedName' and '$parentId' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        }

        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val existingFiles = fileList.files
        if (!existingFiles.isNullOrEmpty()) {
            return@withContext existingFiles[0].id
        }

        // Crear la carpeta si no existe
        val folderMetadata = DriveFile().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
            if (parentId != null) {
                parents = listOf(parentId)
            }
        }

        val created = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()

        Log.d(TAG, "Carpeta '$folderName' creada en Drive con ID: ${created.id}")
        return@withContext created.id
    }

    /**
     * Sube o actualiza un archivo en Google Drive dentro de una carpeta padre específica.
     */
    private suspend fun uploadOrUpdateFile(
        driveService: Drive,
        folderId: String,
        fileName: String,
        mimeType: String,
        content: com.google.api.client.http.AbstractInputStreamContent
    ): String = withContext(Dispatchers.IO) {
        val escapedName = fileName.replace("'", "\\'")
        val query = "name = '$escapedName' and '$folderId' in parents and trashed = false"

        val fileList = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val existingFiles = fileList.files
        if (!existingFiles.isNullOrEmpty()) {
            val fileId = existingFiles[0].id
            val updateMetadata = DriveFile().apply {
                name = fileName
            }
            driveService.files().update(fileId, updateMetadata, content)
                .setFields("id")
                .execute()
            Log.d(TAG, "Archivo '$fileName' actualizado en Drive (ID: $fileId)")
            return@withContext fileId
        } else {
            val createMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
            }
            val created = driveService.files().create(createMetadata, content)
                .setFields("id")
                .execute()
            Log.d(TAG, "Archivo '$fileName' creado en Drive (ID: ${created.id})")
            return@withContext created.id
        }
    }

    /**
     * Sincroniza todas las notas y archivos PDF locales hacia Google Drive.
     */
    suspend fun syncNotesAndPdfs(
        context: Context,
        driveService: Drive,
        notes: List<NoteEntity>,
        pdfFiles: List<File>,
        onProgress: (DriveSyncProgress) -> Unit = {}
    ): DriveSyncResult = withContext(Dispatchers.IO) {
        val totalItems = (notes.size + pdfFiles.size).coerceAtLeast(1)
        var currentProgressIndex = 0
        var syncedNotesCount = 0
        var syncedPdfsCount = 0

        try {
            // Fase 1: Creación de estructura de carpetas
            onProgress(
                DriveSyncProgress.Syncing(
                    phase = "Preparando carpetas en Google Drive...",
                    currentItemName = ROOT_FOLDER_NAME,
                    current = 0,
                    total = totalItems,
                    percent = 0.05f
                )
            )

            val rootFolderId = getOrCreateFolder(driveService, ROOT_FOLDER_NAME)
            val notesFolderId = getOrCreateFolder(driveService, NOTES_FOLDER_NAME, rootFolderId)
            val pdfsFolderId = getOrCreateFolder(driveService, PDFS_FOLDER_NAME, rootFolderId)

            // Fase 2: Sincronizar Notas a la carpeta "Notes"
            val jsonNotesArray = JSONArray()

            for (note in notes) {
                currentProgressIndex++
                val noteTitle = note.title.ifBlank { "Nota ${note.id}" }
                val percent = (currentProgressIndex.toFloat() / totalItems.toFloat()) * 0.90f

                onProgress(
                    DriveSyncProgress.Syncing(
                        phase = "Sincronizando notas a Drive...",
                        currentItemName = noteTitle,
                        current = currentProgressIndex,
                        total = totalItems,
                        percent = percent
                    )
                )

                // 2a. Convertir nota a documento Markdown limpio
                val markdownContent = formatNoteToMarkdown(note)
                val safeFileName = sanitizeFileName(noteTitle) + ".md"
                val mdContent = ByteArrayContent.fromString("text/markdown", markdownContent)

                uploadOrUpdateFile(
                    driveService = driveService,
                    folderId = notesFolderId,
                    fileName = safeFileName,
                    mimeType = "text/markdown",
                    content = mdContent
                )
                syncedNotesCount++

                // Añadir al manifiesto JSON
                jsonNotesArray.put(noteToJson(note))
            }

            // Subir manifiesto general de notas (JSON estructurado completo)
            try {
                val manifestJson = JSONObject().apply {
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("notesCount", notes.size)
                    put("notes", jsonNotesArray)
                }.toString(2)

                val manifestContent = ByteArrayContent.fromString("application/json", manifestJson)
                uploadOrUpdateFile(
                    driveService = driveService,
                    folderId = notesFolderId,
                    fileName = "notes_manifest.json",
                    mimeType = "application/json",
                    content = manifestContent
                )
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo subir notes_manifest.json", e)
            }

            // Fase 3: Sincronizar Archivos PDF a la carpeta "PDFs"
            for (pdfFile in pdfFiles) {
                currentProgressIndex++
                val percent = (currentProgressIndex.toFloat() / totalItems.toFloat()) * 0.95f

                onProgress(
                    DriveSyncProgress.Syncing(
                        phase = "Sincronizando PDFs a Drive...",
                        currentItemName = pdfFile.name,
                        current = currentProgressIndex,
                        total = totalItems,
                        percent = percent
                    )
                )

                if (pdfFile.exists() && pdfFile.length() > 0) {
                    val fileContent = FileContent("application/pdf", pdfFile)
                    uploadOrUpdateFile(
                        driveService = driveService,
                        folderId = pdfsFolderId,
                        fileName = pdfFile.name,
                        mimeType = "application/pdf",
                        content = fileContent
                    )
                    syncedPdfsCount++
                }
            }

            // Fase 4: Respaldo de base de datos Room en appDataFolder
            try {
                DriveBackupWorker.performBackup(context, driveService, DriveBackupWorker.DB_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "Error secundario al respaldar appDataFolder", e)
            }

            // Guardar marca de tiempo
            val now = System.currentTimeMillis()
            setLastSyncTime(context, now)

            val successResult = DriveSyncResult(
                success = true,
                notesSynced = syncedNotesCount,
                pdfsSynced = syncedPdfsCount,
                totalItems = syncedNotesCount + syncedPdfsCount,
                message = "Sincronización completada exitosamente. $syncedNotesCount notas y $syncedPdfsCount PDFs sincronizados en la carpeta '$ROOT_FOLDER_NAME' de tu Google Drive."
            )

            onProgress(DriveSyncProgress.Completed(successResult))
            return@withContext successResult

        } catch (e: GoogleJsonResponseException) {
            val errorMsg = "Error en Google Drive (HTTP ${e.statusCode}): ${e.details?.message ?: e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            val errResult = DriveSyncResult(
                success = false,
                notesSynced = syncedNotesCount,
                pdfsSynced = syncedPdfsCount,
                totalItems = totalItems,
                message = errorMsg,
                error = e
            )
            onProgress(DriveSyncProgress.Completed(errResult))
            return@withContext errResult
        } catch (e: Exception) {
            val errorMsg = "Error durante la sincronización: ${e.localizedMessage ?: e.javaClass.simpleName}"
            Log.e(TAG, errorMsg, e)
            val errResult = DriveSyncResult(
                success = false,
                notesSynced = syncedNotesCount,
                pdfsSynced = syncedPdfsCount,
                totalItems = totalItems,
                message = errorMsg,
                error = e
            )
            onProgress(DriveSyncProgress.Completed(errResult))
            return@withContext errResult
        }
    }

    /**
     * Limpia caracteres no permitidos en nombres de archivos.
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[/\\\\?%*:|\"<>]"), "_")
            .replace("\n", " ")
            .trim()
            .take(60)
            .ifBlank { "Nota" }
    }

    /**
     * Da formato Markdown legible y estructurado a una nota para Drive.
     */
    private fun formatNoteToMarkdown(note: NoteEntity): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(note.updatedAt))

        val sb = StringBuilder()
        sb.append("# ").append(note.title.ifBlank { "Sin título" }).append("\n\n")
        sb.append("> **Fecha:** ").append(dateStr).append("\n")
        if (note.folder.isNotBlank()) {
            sb.append("> **Carpeta:** ").append(note.folder).append("\n")
        }
        if (note.tags.isNotBlank()) {
            sb.append("> **Etiquetas:** ").append(note.tags).append("\n")
        }
        if (note.isPinned) {
            sb.append("> **Fijada:** Sí\n")
        }
        sb.append("\n---\n\n")

        val blocks = NoteBlockSerializer.deserialize(note.content)
        if (blocks.isEmpty() && note.content.isNotBlank()) {
            sb.append(note.content).append("\n")
        } else {
            for (block in blocks) {
                when (block) {
                    is NoteBlock.Text -> {
                        if (block.content.isNotBlank()) {
                            sb.append(block.content).append("\n\n")
                        }
                    }
                    is NoteBlock.Checklist -> {
                        val check = if (block.isChecked) "[x]" else "[ ]"
                        sb.append("- ").append(check).append(" ").append(block.content).append("\n")
                    }
                    is NoteBlock.SimpleList -> {
                        sb.append("- ").append(block.content).append("\n")
                    }
                    is NoteBlock.CalloutBox -> {
                        sb.append("> 💡 **Nota:** ").append(block.content).append("\n\n")
                    }
                    is NoteBlock.Table -> {
                        sb.append(formatTableToMarkdown(block)).append("\n\n")
                    }
                    is NoteBlock.Math -> {
                        sb.append("$$\n").append(block.latexCode).append("\n$$\n\n")
                    }
                    is NoteBlock.Pdf -> {
                        sb.append("📄 **Documento PDF adjunto:** ").append(block.fileName).append("\n\n")
                    }
                    is AudioBlock -> {
                        sb.append("🎙️ **Audio adjunto:** ").append(block.filePath).append("\n")
                        if (block.transcription.isNotBlank()) {
                            sb.append("*Transcripción:* ").append(block.transcription).append("\n\n")
                        }
                    }
                    is NoteBlock.Image -> {
                        sb.append("🖼️ **Imagen adjunta:** ").append(block.fileName).append("\n\n")
                    }
                    is NoteBlock.ImageBlock -> {
                        sb.append("🖼️ **Imagen:** ").append("\n\n")
                    }
                    is NoteBlock.Drawing -> {
                        sb.append("✏️ **Dibujo / Apunte a mano incluido**\n\n")
                    }
                }
            }
        }

        return sb.toString()
    }

    private fun formatTableToMarkdown(table: NoteBlock.Table): String {
        val matrix = table.matrix
        if (matrix.isEmpty() || matrix[0].isEmpty()) return ""
        val sb = StringBuilder()
        // Header
        val firstRow = matrix[0]
        sb.append("| ").append(firstRow.joinToString(" | ") { it.text.replace("|", "\\|").ifBlank { " " } }).append(" |\n")
        sb.append("| ").append(firstRow.joinToString(" | ") { "---" }).append(" |\n")
        // Rows
        for (i in 1 until matrix.size) {
            val row = matrix[i]
            sb.append("| ").append(row.joinToString(" | ") { it.text.replace("|", "\\|").ifBlank { " " } }).append(" |\n")
        }
        return sb.toString()
    }

    private fun noteToJson(note: NoteEntity): JSONObject {
        return JSONObject().apply {
            put("id", note.id)
            put("remoteUuid", note.remoteUuid)
            put("title", note.title)
            put("content", note.content)
            put("folder", note.folder)
            put("tags", note.tags)
            put("colorIndex", note.colorIndex)
            put("isPinned", note.isPinned)
            put("isArchived", note.isArchived)
            put("isTrashed", note.isTrashed)
            put("createdAt", note.createdAt)
            put("updatedAt", note.updatedAt)
            put("pdfPath", note.pdfPath ?: "")
        }
    }
}
