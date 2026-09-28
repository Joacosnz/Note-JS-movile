package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.model.ChecklistItem
import com.example.model.NoteType
import com.example.util.MathEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class NavTab {
    NOTES,
    ARCHIVED,
    TRASH
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
    data class Editor(val noteId: Long = 0, val initialType: NoteType = NoteType.TEXT) : Screen()
    data class Drawing(val noteId: Long = 0) : Screen()
}

data class NotesUiState(
    val activeNotes: List<NoteEntity> = emptyList(),
    val archivedNotes: List<NoteEntity> = emptyList(),
    val trashedNotes: List<NoteEntity> = emptyList(),
    val availableTags: List<String> = listOf("Todas", "Ideas", "Trabajo", "Personal", "Estudio"),
    val selectedTag: String? = null,
    val searchQuery: String = "",
    val isGridView: Boolean = true,
    val sortOption: SortOption = SortOption.MODIFIED_DESC,
    val activeTab: NavTab = NavTab.NOTES,
    val unlockedNoteIds: Set<Long> = emptySet(),
    val snackbarMessage: String? = null,
    val pendingUndoNote: NoteEntity? = null
)

private data class RawNotes(
    val active: List<NoteEntity>,
    val archived: List<NoteEntity>,
    val trashed: List<NoteEntity>
)

private data class FilterState(
    val query: String,
    val tag: String?,
    val isGrid: Boolean,
    val sort: SortOption,
    val tab: NavTab
)

private data class ExtraState(
    val unlocked: Set<Long>,
    val snackbar: String?,
    val undoNote: NoteEntity?
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(true)
    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)
    private val _activeTab = MutableStateFlow(NavTab.NOTES)
    private val _unlockedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _pendingUndoNote = MutableStateFlow<NoteEntity?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao())
        viewModelScope.launch {
            repository.preseedIfEmpty()
        }
    }

    private val rawNotesFlow = combine(
        repository.activeNotes,
        repository.archivedNotes,
        repository.trashedNotes
    ) { active, archived, trashed ->
        RawNotes(active, archived, trashed)
    }

    private val filterFlow = combine(
        _searchQuery,
        _selectedTag,
        _isGridView,
        _sortOption,
        _activeTab
    ) { query, tag, isGrid, sort, tab ->
        FilterState(query, tag, isGrid, sort, tab)
    }

    private val extraStateFlow = combine(
        _unlockedNoteIds,
        _snackbarMessage,
        _pendingUndoNote
    ) { unlocked, snackbar, undoNote ->
        ExtraState(unlocked, snackbar, undoNote)
    }

    val uiState: StateFlow<NotesUiState> = combine(
        rawNotesFlow,
        filterFlow,
        extraStateFlow
    ) { raw, filter, extra ->
        val tagsSet = linkedSetOf("Todas", "Ideas", "Trabajo", "Personal", "Estudio")
        (raw.active + raw.archived + raw.trashed).forEach { note ->
            note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                tagsSet.add(it)
            }
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
                    note.tags.split(",").map { it.trim().lowercase(Locale.getDefault()) }.contains(t)
                }
            }
            return when (filter.sort) {
                SortOption.MODIFIED_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                SortOption.MODIFIED_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.updatedAt })
                SortOption.CREATED_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAt })
                SortOption.CREATED_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.createdAt })
                SortOption.TITLE_ASC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
                SortOption.TITLE_DESC -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.title.lowercase(Locale.getDefault()) })
                else -> filtered
            }
        }

        NotesUiState(
            activeNotes = filterAndSort(raw.active),
            archivedNotes = filterAndSort(raw.archived),
            trashedNotes = filterAndSort(raw.trashed),
            availableTags = tagsSet.toList(),
            selectedTag = filter.tag,
            searchQuery = filter.query,
            isGridView = filter.isGrid,
            sortOption = filter.sort,
            activeTab = filter.tab,
            unlockedNoteIds = extra.unlocked,
            snackbarMessage = extra.snackbar,
            pendingUndoNote = extra.undoNote
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
        _activeTab.value = tab
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
        checklistItems: List<ChecklistItem> = emptyList(),
        drawingData: String = "",
        isLocked: Boolean = false,
        lockPin: String = "",
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

            val entity = if (id > 0) {
                val existing = repository.getNoteById(id)
                existing?.copy(
                    title = title,
                    content = processedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = tags,
                    checklistData = jsonChecklist,
                    drawingData = if (drawingData.isNotEmpty()) drawingData else existing.drawingData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    updatedAt = now
                ) ?: NoteEntity(
                    id = id,
                    title = title,
                    content = processedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = tags,
                    checklistData = jsonChecklist,
                    drawingData = drawingData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    createdAt = now,
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
                    tags = tags,
                    checklistData = jsonChecklist,
                    drawingData = drawingData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    createdAt = now,
                    updatedAt = now
                )
            }

            val savedId = repository.insert(entity)
            onSaved(if (id > 0) id else savedId)
        }
    }

    fun duplicateNote(note: NoteEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val duplicate = note.copy(
                id = 0,
                title = if (note.title.isNotBlank()) "${note.title} (Copia)" else "Copia de nota",
                createdAt = now,
                updatedAt = now
            )
            repository.insert(duplicate)
            _snackbarMessage.value = "Nota duplicada"
        }
    }
}
