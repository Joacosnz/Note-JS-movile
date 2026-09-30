package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.model.ChecklistItem
import com.example.model.NoteBlock
import com.example.model.NoteType
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.util.MathEvaluator
import com.example.util.NoteBlockSerializer
import com.example.util.PdfRenderUtil
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class NavTab {
    NOTES,
    ARCHIVED,
    TRASH,
    REMINDERS
}

enum class SortOption(val title: String) {
    MODIFIED_DESC("Última modificación (recientes)"),
    MODIFIED_ASC("Última modificación (antiguas)"),
    CREATED_DESC("Fecha de creación (nuevas)"),
    CREATED_ASC("Fecha de creación (antiguas)"),
    TITLE_ASC("Título (A - Z)"),
    TITLE_DESC("Título (Z - A)")
}

sealed class Screen {
    data object Home : Screen()
    data object Folders : Screen()
    data class Editor(val noteId: Long = 0, val initialType: NoteType = NoteType.TEXT) : Screen()
    data class Drawing(val noteId: Long = 0) : Screen()
    data class PdfViewer(val noteId: Long = 0, val pdfPath: String = "") : Screen()
}

data class NotesUiState(
    val activeNotes: List<NoteEntity> = emptyList(),
    val archivedNotes: List<NoteEntity> = emptyList(),
    val trashedNotes: List<NoteEntity> = emptyList(),
    val remindersNotes: List<NoteEntity> = emptyList(),
    val availableTags: List<String> = listOf("Todas"),
    val availableFolders: List<String> = listOf("Notas"),
    val customFolders: List<FolderEntity> = emptyList(),
    val selectedFolder: String? = null,
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val sortOption: SortOption = SortOption.MODIFIED_DESC,
    val activeTab: NavTab = NavTab.NOTES,
    val unlockedNoteIds: Set<Long> = emptySet(),
    val snackbarMessage: String? = null,
    val pendingUndoNote: NoteEntity? = null,
    val isSelectionMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val allActiveNotesCount: Int = 0,
    val remindersCount: Int = 0,
    val archivedCount: Int = 0,
    val trashedCount: Int = 0,
    val folderNoteCounts: Map<String, Int> = emptyMap(),
    val tagNoteCounts: Map<String, Int> = emptyMap()
)

private data class RawData(
    val active: List<NoteEntity>,
    val archived: List<NoteEntity>,
    val trashed: List<NoteEntity>,
    val folders: List<FolderEntity>
)

private data class FilterState(
    val query: String,
    val tag: String?,
    val folder: String?,
    val isGrid: Boolean,
    val sort: SortOption,
    val tab: NavTab
)

private data class ExtraState(
    val unlocked: Set<Long>,
    val snackbar: String?,
    val undoNote: NoteEntity?
)

private data class SelectionState(
    val isSelectionMode: Boolean,
    val selectedIds: Set<Long>
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(true)
    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)
    private val _activeTab = MutableStateFlow(NavTab.NOTES)
    private val _unlockedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _pendingUndoNote = MutableStateFlow<NoteEntity?>(null)
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao(), database.folderDao())
    }

    private val rawNotesFlow = combine(
        repository.activeNotes,
        repository.archivedNotes,
        repository.trashedNotes
    ) { active, archived, trashed ->
        Triple(active, archived, trashed)
    }

    private val rawDataFlow = combine(
        rawNotesFlow,
        repository.folders
    ) { notesTriple, folders ->
        RawData(notesTriple.first, notesTriple.second, notesTriple.third, folders)
    }

    private val filterFlow1 = combine(
        _searchQuery,
        _selectedTag,
        _selectedFolder
    ) { query, tag, folder ->
        Triple(query, tag, folder)
    }

    private val filterFlow2 = combine(
        _isGridView,
        _sortOption,
        _activeTab
    ) { isGrid, sort, tab ->
        Triple(isGrid, sort, tab)
    }

    private val filterFlow = combine(
        filterFlow1,
        filterFlow2
    ) { t1, t2 ->
        FilterState(
            query = t1.first,
            tag = t1.second,
            folder = t1.third,
            isGrid = t2.first,
            sort = t2.second,
            tab = t2.third
        )
    }

    private val extraStateFlow = combine(
        _unlockedNoteIds,
        _snackbarMessage,
        _pendingUndoNote
    ) { unlocked, snackbar, undoNote ->
        ExtraState(unlocked, snackbar, undoNote)
    }

    private val selectionStateFlow = combine(
        _isSelectionMode,
        _selectedNoteIds
    ) { isSel, ids ->
        SelectionState(isSel, ids)
    }

    val uiState: StateFlow<NotesUiState> = combine(
        rawDataFlow,
        filterFlow,
        extraStateFlow,
        selectionStateFlow
    ) { raw, filter, extra, selection ->
        val tagsSet = linkedSetOf<String>()
        val foldersSet = linkedSetOf("Notas")
        (raw.active + raw.archived + raw.trashed).forEach { note ->
            extractTagsFromNote(note.title, note.content, note.tags).forEach {
                tagsSet.add(it)
            }
            if (note.folder.isNotBlank()) {
                foldersSet.add(note.folder)
            }
        }
        raw.folders.forEach { foldersSet.add(it.name) }

        val baseActive = if (filter.folder != null) {
            raw.active.filter { it.folder.equals(filter.folder, ignoreCase = true) }
        } else {
            raw.active
        }

        fun filterAndSort(list: List<NoteEntity>): List<NoteEntity> {
            var filtered = list
            if (filter.query.isNotBlank()) {
                val q = filter.query.trim().lowercase(Locale.getDefault())
                filtered = filtered.filter {
                    it.title.lowercase(Locale.getDefault()).contains(q) ||
                            it.content.lowercase(Locale.getDefault()).contains(q) ||
                            it.tags.lowercase(Locale.getDefault()).contains(q)
                }
            }
            if (filter.tag != null && filter.tag != "Todas") {
                val t = filter.tag.lowercase(Locale.getDefault())
                filtered = filtered.filter { note ->
                    extractTagsFromNote(note.title, note.content, note.tags).map { it.lowercase(Locale.getDefault()) }.contains(t)
                }
            }
            return when (filter.sort) {
                SortOption.MODIFIED_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                SortOption.MODIFIED_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.updatedAt })
                SortOption.CREATED_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAt })
                SortOption.CREATED_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.createdAt })
                SortOption.TITLE_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
                SortOption.TITLE_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.title.lowercase(Locale.getDefault()) })
            }
        }

        val remindersList = raw.active.filter { it.reminderTimestamp != null && !it.isReminderDone }
            .sortedBy { it.reminderTimestamp }

        val folderCounts = mutableMapOf<String, Int>()
        foldersSet.forEach { fName ->
            folderCounts[fName] = raw.active.count { it.folder.equals(fName, ignoreCase = true) }
        }

        val tagCounts = mutableMapOf<String, Int>()
        tagsSet.forEach { tag ->
            tagCounts[tag] = raw.active.count { note ->
                extractTagsFromNote(note.title, note.content, note.tags).contains(tag)
            }
        }

        NotesUiState(
            activeNotes = filterAndSort(baseActive),
            archivedNotes = filterAndSort(raw.archived),
            trashedNotes = filterAndSort(raw.trashed),
            remindersNotes = filterAndSort(remindersList),
            availableTags = listOf("Todas") + tagsSet.toList(),
            availableFolders = foldersSet.toList(),
            customFolders = raw.folders,
            selectedFolder = filter.folder,
            selectedTag = filter.tag,
            searchQuery = filter.query,
            isGridView = filter.isGrid,
            sortOption = filter.sort,
            activeTab = filter.tab,
            unlockedNoteIds = extra.unlocked,
            snackbarMessage = extra.snackbar,
            pendingUndoNote = extra.undoNote,
            isSelectionMode = selection.isSelectionMode,
            selectedNoteIds = selection.selectedIds,
            allActiveNotesCount = raw.active.size,
            remindersCount = remindersList.size,
            archivedCount = raw.archived.size,
            trashedCount = raw.trashed.size,
            folderNoteCounts = folderCounts,
            tagNoteCounts = tagCounts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    fun navigateTo(screen: Screen) {
        currentScreen.value = screen
    }

    fun navigateBack() {
        currentScreen.value = Screen.Home
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
        _selectedTag.value = null
        _activeTab.value = NavTab.NOTES
        currentScreen.value = Screen.Home
    }

    fun selectTagAndNavigate(tag: String) {
        _selectedFolder.value = null
        _selectedTag.value = tag
        _activeTab.value = NavTab.NOTES
        currentScreen.value = Screen.Home
    }

    fun selectTab(tab: NavTab) {
        _selectedFolder.value = null
        _selectedTag.value = null
        _activeTab.value = tab
        exitSelectionMode()
        currentScreen.value = Screen.Home
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertFolder(FolderEntity(name = trimmed))
            _snackbarMessage.value = "Carpeta '$trimmed' creada"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (tag == "Todas") null else tag
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setActiveTab(tab: NavTab) {
        _selectedFolder.value = null
        _activeTab.value = tab
        exitSelectionMode()
    }

    fun enterSelectionMode(initialNoteId: Long? = null) {
        _isSelectionMode.value = true
        if (initialNoteId != null) {
            _selectedNoteIds.value = setOf(initialNoteId)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedNoteIds.value = emptySet()
    }

    fun toggleNoteSelection(noteId: Long) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (current.contains(noteId)) {
            current - noteId
        } else {
            current + noteId
        }
    }

    fun selectAllNotes(noteIds: List<Long>) {
        _selectedNoteIds.value = noteIds.toSet()
    }

    fun deselectAllNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun batchMoveToFolder(folder: String) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateFolderBatch(ids, folder)
            _snackbarMessage.value = "${ids.size} notas movidas a $folder"
            exitSelectionMode()
        }
    }

    fun batchAddTag(tag: String) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty() || tag.isBlank()) return
        viewModelScope.launch {
            repository.addTagBatch(ids, tag)
            _snackbarMessage.value = "Etiqueta #${tag.trim()} aplicada a ${ids.size} notas"
            exitSelectionMode()
        }
    }

    fun batchDelete(isTrashTab: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            if (isTrashTab) {
                repository.deletePermanentlyBatch(ids)
                _snackbarMessage.value = "${ids.size} notas eliminadas definitivamente"
            } else {
                repository.moveToTrashBatch(ids)
                _snackbarMessage.value = "${ids.size} notas enviadas a la papelera"
            }
            exitSelectionMode()
        }
    }

    fun unlockNote(noteId: Long, pinInput: String, correctPin: String): Boolean {
        if (pinInput == correctPin) {
            _unlockedNoteIds.value = _unlockedNoteIds.value + noteId
            return true
        }
        return false
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note)
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            _pendingUndoNote.value = note
            _snackbarMessage.value = if (note.isArchived) "Nota desarchivada" else "Nota archivada"
            repository.toggleArchive(note)
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            _pendingUndoNote.value = note
            _snackbarMessage.value = "Nota enviada a la papelera"
            repository.moveToTrash(note)
        }
    }

    fun restoreFromTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreFromTrash(note)
            _snackbarMessage.value = "Nota restaurada"
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.delete(note)
            _snackbarMessage.value = "Nota eliminada definitivamente"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            _snackbarMessage.value = "Papelera vaciada"
        }
    }

    fun undoLastAction() {
        val note = _pendingUndoNote.value ?: return
        viewModelScope.launch {
            repository.update(note)
            _pendingUndoNote.value = null
            _snackbarMessage.value = "Acción deshecha"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    suspend fun getNote(id: Long): NoteEntity? {
        return repository.getNoteById(id)
    }

    fun setReminder(context: Context, noteId: Long, timestamp: Long, title: String, content: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            if (note != null) {
                val updated = note.copy(reminderTimestamp = timestamp, isReminderDone = false, updatedAt = System.currentTimeMillis())
                repository.update(updated)
                ReminderScheduler.scheduleReminder(context, noteId, timestamp, title, content)
                _snackbarMessage.value = "Recordatorio programado"
            }
        }
    }

    fun cancelReminder(context: Context, noteId: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            if (note != null) {
                val updated = note.copy(reminderTimestamp = null, isReminderDone = false, updatedAt = System.currentTimeMillis())
                repository.update(updated)
                ReminderScheduler.cancelReminder(context, noteId)
                _snackbarMessage.value = "Recordatorio cancelado"
            }
        }
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        noteType: NoteType = NoteType.TEXT,
        colorIndex: Int = 0,
        paperStyle: String = "BLANK",
        isPinned: Boolean = false,
        isArchived: Boolean = false,
        tags: String = "",
        folder: String = "Notas",
        checklistItems: List<ChecklistItem> = emptyList(),
        drawingData: String = "",
        pdfPath: String = "",
        pdfAnnotationData: String = "",
        isLocked: Boolean = false,
        lockPin: String = "",
        reminderTimestamp: Long? = null,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val jsonChecklist = if (checklistItems.isNotEmpty()) {
                val array = JSONArray()
                checklistItems.forEach { item ->
                    val obj = JSONObject()
                    obj.put("id", item.id)
                    obj.put("text", item.text)
                    obj.put("isChecked", item.isChecked)
                    array.put(obj)
                }
                array.toString()
            } else ""

            val processedContent = if (noteType == NoteType.MATH) {
                MathEvaluator.processMathInText(content)
            } else {
                content
            }

            val plainTextForTags = extractNotePlainText(title, processedContent)
            val extractedTags = hashtagRegex.findAll(plainTextForTags).map { it.value.removePrefix("#") }.toList()
            val paramTags = tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
            val finalTagsString = (extractedTags + paramTags).distinct().joinToString(",")

            val existing = if (id > 0) repository.getNoteById(id) else null
            val finalReminderTimestamp = reminderTimestamp ?: existing?.reminderTimestamp

            val finalPdfPath = if (pdfPath.isNotEmpty()) pdfPath else (existing?.pdfPath ?: "")
            val finalPdfAnnotationData = if (pdfAnnotationData.isNotEmpty()) pdfAnnotationData else (existing?.pdfAnnotationData ?: "")

            val entity = if (existing != null) {
                existing.copy(
                    title = title,
                    content = processedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = finalTagsString,
                    folder = folder,
                    checklistData = jsonChecklist,
                    drawingData = if (drawingData.isNotEmpty()) drawingData else existing.drawingData,
                    pdfPath = finalPdfPath,
                    pdfAnnotationData = finalPdfAnnotationData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    reminderTimestamp = finalReminderTimestamp,
                    updatedAt = now
                )
            } else {
                NoteEntity(
                    title = title,
                    content = processedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = finalTagsString,
                    folder = folder,
                    checklistData = jsonChecklist,
                    drawingData = drawingData,
                    pdfPath = finalPdfPath,
                    pdfAnnotationData = finalPdfAnnotationData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    reminderTimestamp = finalReminderTimestamp,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val savedId = repository.insert(entity)
            onSaved(if (id > 0) id else savedId)
        }
    }

    fun savePdfAnnotationData(noteId: Long, pdfAnnotationData: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val existing = repository.getNoteById(noteId)
            if (existing != null) {
                val updated = existing.copy(
                    pdfAnnotationData = pdfAnnotationData,
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(updated)
                _snackbarMessage.value = "Anotaciones guardadas"
                onSaved()
            } else {
                onSaved()
            }
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val duplicate = note.copy(
                id = 0,
                title = if (note.title.isNotBlank()) "${note.title} (Copia)" else "Copia de nota",
                reminderTimestamp = null,
                isReminderDone = false,
                createdAt = now,
                updatedAt = now
            )
            repository.insert(duplicate)
            _snackbarMessage.value = "Nota duplicada"
        }
    }


    fun insertPdfBlock(uri: Uri, onBlockCreated: (PdfBlock) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val pdfsDir = File(context.filesDir, "pdfs")
                if (!pdfsDir.exists()) pdfsDir.mkdirs()
                val destFile = File(pdfsDir, "${UUID.randomUUID()}.pdf")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val pageCount = PdfRenderUtil.getPageCount(destFile)
                val fileName = getFileNameFromUri(context, uri) ?: "Documento.pdf"

                val fileSize = destFile.length()
                val fileSizeFormatted = if (fileSize < 1024 * 1024) {
                    "${fileSize / 1024} KB"
                } else {
                    String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0))
                }

                val indexedText = withContext(Dispatchers.IO) {
                    PdfRenderUtil.extractTextFromPdf(destFile)
                }

                val pdfBlock = PdfBlock(
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    fileSizeFormatted = fileSizeFormatted,
                    pageCount = pageCount,
                    indexedText = indexedText
                )


                onBlockCreated(pdfBlock)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Error al adjuntar PDF"
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
            if (name != null && name!!.contains("/")) {
                name = name!!.substring(name!!.lastIndexOf('/') + 1)
            }
        }
        return name
    }
}

private val hashtagRegex = Regex("""#([a-zA-Z0-9_áéíóúÁÉÍÓÚñÑ]+)""")

private fun extractNotePlainText(title: String, content: String): String {
    return buildString {
        append(title)
        append(" ")
        try {
            val blocksList = NoteBlockSerializer.deserialize(content)
            blocksList.forEach { block ->
                when (block) {
                    is NoteBlock.Text -> append(block.content).append(" ")
                    is NoteBlock.Checklist -> append(block.content).append(" ")
                    is NoteBlock.SimpleList -> append(block.content).append(" ")
                    is NoteBlock.CalloutBox -> append(block.content).append(" ")
                    is NoteBlock.Drawing -> {}
                    is NoteBlock.Table -> {
                        block.matrix.forEach { row ->
                            row.forEach { cell ->
                                append(cell.text).append(" ")
                            }
                        }
                    }
                    is PdfBlock -> {
                        append(block.fileName).append(" ")
                    }
                    is AudioBlock -> {
                        if (block.transcription.isNotBlank()) {
                            append(block.transcription).append(" ")
                        }
                    }
                }
            }
        } catch (_: Exception) {
            append(content)
        }
    }
}

private fun extractTagsFromNote(title: String, content: String, legacyTags: String): List<String> {
    val plainText = extractNotePlainText(title, content)
    val tagsSet = linkedSetOf<String>()
    hashtagRegex.findAll(plainText).map { it.value.removePrefix("#") }.forEach {
        tagsSet.add(it)
    }
    legacyTags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }.forEach {
        tagsSet.add(it)
    }
    return tagsSet.toList()
}
