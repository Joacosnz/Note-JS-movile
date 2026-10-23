package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.AuthResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.SignatureEntity
import com.example.data.remote.FirebaseStorageManager
import com.example.data.remote.FirestoreManager
import com.example.data.repository.NoteRepository
import com.example.ui.components.contentSnippet
import com.example.ui.screens.DrawingPoint
import com.example.util.SignatureHelper
import com.example.data.drive.DriveSyncManager
import com.example.data.drive.DriveSyncProgress
import com.example.data.drive.DriveSyncResult
import com.example.data.drive.GoogleDriveManager
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.example.model.ChecklistItem
import com.example.model.NoteBlock
import com.example.model.NoteColors
import com.example.model.NoteType
import com.example.model.PdfBlock
import com.example.model.AudioBlock
import com.example.model.SyncState
import com.example.util.FileManager
import com.example.util.MathEvaluator
import com.example.util.NoteBlockSerializer
import com.example.util.PdfModifierUtil
import com.example.util.PdfRenderUtil
import com.example.util.PdfTextExtractor
import com.example.util.ReminderScheduler
import com.example.util.SettingsManager
import com.example.util.ThemeMode
import com.example.util.PaperThemeMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
    REMINDERS,
    PDF_LIBRARY
}

enum class SortOption(val title: String) {
    MODIFIED_DESC("Última modificación (recientes)"),
    MODIFIED_ASC("Última modificación (antiguas)"),
    CREATED_DESC("Fecha de creación (nuevas)"),
    CREATED_ASC("Fecha de creación (antiguas)"),
    TITLE_ASC("Título (A - Z)"),
    TITLE_DESC("Título (Z - A)"),
    MANUAL("Prioridad manual")
}

sealed class Screen {
    data object Home : Screen()
    data object Folders : Screen()
    data class Editor(
        val noteId: Long = 0,
        val initialType: NoteType = NoteType.TEXT,
        val openVoiceDictation: Boolean = false,
        val openDocumentScanner: Boolean = false,
        val initialSharedText: String? = null,
        val initialSharedTitle: String? = null
    ) : Screen()
    data class Drawing(val noteId: Long = 0) : Screen()
    data class PdfViewer(val noteId: Long = 0, val pdfPath: String = "") : Screen()
    data object PdfLibrary : Screen()
    data object Settings : Screen()
}

data class NotesUiState(
    val activeNotes: List<NoteEntity> = emptyList(),
    val allActiveNotes: List<NoteEntity> = emptyList(),
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
    val tagNoteCounts: Map<String, Int> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paperThemeMode: PaperThemeMode = PaperThemeMode.AUTO,
    val defaultPaperStyle: String = "BLANK",
    val geminiApiKey: String = "",
    val defaultLockPin: String = "1234",
    val isViewingPrivateFolder: Boolean = false,
    val isPrivateUnlocked: Boolean = isViewingPrivateFolder
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
    val tab: NavTab,
    val isViewingPrivateFolder: Boolean
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

private data class SettingsState(
    val themeMode: ThemeMode,
    val paperThemeMode: PaperThemeMode,
    val defaultPaperStyle: String,
    val geminiApiKey: String,
    val defaultLockPin: String
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    val repository: NoteRepository
    val settingsManager = SettingsManager(application)
    val currentScreen = MutableStateFlow<Screen>(Screen.Home)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _isGridView = MutableStateFlow(true)
    private val _sortOption = MutableStateFlow(SortOption.MODIFIED_DESC)
    private val _activeTab = MutableStateFlow(NavTab.NOTES)
    private val _isViewingPrivateFolder = MutableStateFlow(false)
    val isViewingPrivateFolder: StateFlow<Boolean> = _isViewingPrivateFolder.asStateFlow()
    private val _unlockedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _pendingUndoNote = MutableStateFlow<NoteEntity?>(null)
    val firestoreManager = FirestoreManager(application.applicationContext)
    val syncState: StateFlow<SyncState> = firestoreManager.syncState
    val authManager = AuthManager(application)
    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUserFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = authManager.currentUser
    )
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    val pendingSyncCount: StateFlow<Int>

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _pdfLastModified = MutableStateFlow<Long>(System.currentTimeMillis())
    val pdfLastModified: StateFlow<Long> = _pdfLastModified.asStateFlow()

    private val _driveSyncProgress = MutableStateFlow<DriveSyncProgress>(DriveSyncProgress.Idle)
    val driveSyncProgress: StateFlow<DriveSyncProgress> = _driveSyncProgress.asStateFlow()

    private val _lastDriveSyncTime = MutableStateFlow<Long>(0L)
    val lastDriveSyncTime: StateFlow<Long> = _lastDriveSyncTime.asStateFlow()

    val savedSignatures: StateFlow<List<SignatureEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        val firebaseStorageManager = FirebaseStorageManager()
        repository = NoteRepository(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            signatureDao = database.signatureDao(),
            firestoreManager = firestoreManager,
            firebaseStorageManager = firebaseStorageManager,
            context = application.applicationContext
        )

        pendingSyncCount = repository.pendingSyncCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        viewModelScope.launch {
            authManager.currentUserFlow.collectLatest { user ->
                if (user != null) {
                    repository.startListeningToCloud(user.uid)
                } else {
                    val driveAccount = try {
                        GoogleDriveManager(application).getSignedInAccount()
                    } catch (_: Exception) {
                        null
                    }
                    val idToken = driveAccount?.idToken
                    if (!idToken.isNullOrBlank()) {
                        val fbUid = authManager.signInWithGoogleIdToken(idToken)
                        if (!fbUid.isNullOrBlank()) {
                            repository.startListeningToCloud(fbUid)
                        } else {
                            repository.stopListeningToCloud(resetToAnonymous = true)
                        }
                    } else {
                        repository.stopListeningToCloud(resetToAnonymous = true)
                    }
                }
            }
        }
        _isGridView.value = settingsManager.isGridViewDefault()
        _sortOption.value = settingsManager.getDefaultSortOption()
        _lastDriveSyncTime.value = DriveSyncManager.getLastSyncTime(application)
        savedSignatures = repository.signatures
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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

    private data class FilterPart2(
        val isGrid: Boolean,
        val sort: SortOption,
        val tab: NavTab,
        val viewingPrivate: Boolean
    )

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
        _activeTab,
        _isViewingPrivateFolder
    ) { isGrid, sort, tab, viewingPrivate ->
        FilterPart2(isGrid, sort, tab, viewingPrivate)
    }

    private val filterFlow = combine(
        filterFlow1,
        filterFlow2
    ) { t1, t2 ->
        FilterState(
            query = t1.first,
            tag = t1.second,
            folder = t1.third,
            isGrid = t2.isGrid,
            sort = t2.sort,
            tab = t2.tab,
            isViewingPrivateFolder = t2.viewingPrivate
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

    private val settingsStateFlow = combine(
        settingsManager.themeMode,
        settingsManager.paperThemeMode,
        settingsManager.defaultPaperStyle,
        settingsManager.geminiApiKey,
        settingsManager.defaultLockPin
    ) { mode, paperTheme, paperStyle, apiKey, pin ->
        SettingsState(mode, paperTheme, paperStyle, apiKey, pin)
    }

    val uiState: StateFlow<NotesUiState> = combine(
        rawDataFlow,
        filterFlow,
        extraStateFlow,
        selectionStateFlow,
        settingsStateFlow
    ) { raw, filter, extra, selection, settings ->
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

        val baseActive = if (filter.folder != null && filter.query.isBlank()) {
            raw.active.filter { it.folder.equals(filter.folder, ignoreCase = true) }
        } else {
            raw.active
        }

        val isLockedFilter = filter.tag.equals("🔒 Privado", ignoreCase = true) ||
                filter.tag.equals("🔒 Privadas", ignoreCase = true) ||
                filter.tag.equals("Bloqueadas", ignoreCase = true) ||
                filter.tag.equals("Privado", ignoreCase = true) ||
                filter.isViewingPrivateFolder

        fun filterAndSort(list: List<NoteEntity>): List<NoteEntity> {
            var filtered = if (isLockedFilter) {
                list.filter { it.isLocked }
            } else {
                list.filter { !it.isLocked }
            }
            if (filter.query.isNotBlank()) {
                val q = filter.query.trim().lowercase(Locale.getDefault())
                filtered = filtered.filter { note ->
                    note.title.lowercase(Locale.getDefault()).contains(q) ||
                            note.contentSnippet.lowercase(Locale.getDefault()).contains(q) ||
                            note.content.lowercase(Locale.getDefault()).contains(q) ||
                            note.searchableText.lowercase(Locale.getDefault()).contains(q) ||
                            note.tags.lowercase(Locale.getDefault()).contains(q) ||
                            (note.pdfExtractedText?.lowercase(Locale.getDefault())?.contains(q) == true)
                }
            }
            if (filter.tag != null && filter.tag != "Todas" && !isLockedFilter && filter.query.isBlank()) {
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
                SortOption.MANUAL -> {
                    val manualOrder = settingsManager.getManualNoteOrder()
                    val orderMap = manualOrder.withIndex().associate { it.value to it.index }
                    filtered.sortedWith(
                        compareByDescending<NoteEntity> { it.isPinned }
                            .thenBy { orderMap[it.id] ?: Int.MAX_VALUE }
                            .thenByDescending { it.updatedAt }
                    )
                }
            }
        }

        val remindersList = raw.active.filter { it.reminderTimestamp != null && !it.isReminderDone && !it.isLocked }
            .sortedBy { it.reminderTimestamp }

        val folderCounts = mutableMapOf<String, Int>()
        foldersSet.forEach { fName ->
            folderCounts[fName] = raw.active.count { !it.isLocked && it.folder.equals(fName, ignoreCase = true) }
        }

        val tagCounts = mutableMapOf<String, Int>()
        tagCounts["🔒 Privado"] = raw.active.count { it.isLocked }
        tagCounts["🔒 Privadas"] = raw.active.count { it.isLocked }
        tagsSet.forEach { tag ->
            if (!tag.equals("🔒 Privado", ignoreCase = true) && !tag.equals("🔒 Privadas", ignoreCase = true) && !tag.equals("Bloqueadas", ignoreCase = true)) {
                tagCounts[tag] = raw.active.count { note ->
                    !note.isLocked && extractTagsFromNote(note.title, note.content, note.tags).contains(tag)
                }
            }
        }

        val userTags = tagsSet.filter { !it.equals("🔒 Privado", ignoreCase = true) && !it.equals("🔒 Privadas", ignoreCase = true) && !it.equals("Bloqueadas", ignoreCase = true) }

        NotesUiState(
            activeNotes = filterAndSort(baseActive),
            allActiveNotes = raw.active,
            archivedNotes = filterAndSort(raw.archived),
            trashedNotes = filterAndSort(raw.trashed),
            remindersNotes = filterAndSort(remindersList),
            availableTags = listOf("Todas", "🔒 Privado") + userTags,
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
            allActiveNotesCount = raw.active.count { !it.isLocked },
            remindersCount = remindersList.size,
            archivedCount = raw.archived.size,
            trashedCount = raw.trashed.size,
            folderNoteCounts = folderCounts,
            tagNoteCounts = tagCounts,
            themeMode = settings.themeMode,
            paperThemeMode = settings.paperThemeMode,
            defaultPaperStyle = settings.defaultPaperStyle,
            geminiApiKey = settings.geminiApiKey,
            defaultLockPin = settings.defaultLockPin,
            isViewingPrivateFolder = filter.isViewingPrivateFolder,
            isPrivateUnlocked = filter.isViewingPrivateFolder
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesUiState()
    )

    private data class NavigationSnapshot(
        val screen: Screen = Screen.Home,
        val activeTab: NavTab = NavTab.NOTES,
        val selectedFolder: String? = null,
        val selectedTag: String? = null,
        val isViewingPrivateFolder: Boolean = false
    ) {
        fun isRootHome(): Boolean {
            return screen == Screen.Home &&
                activeTab == NavTab.NOTES &&
                selectedFolder == null &&
                selectedTag == null &&
                !isViewingPrivateFolder
        }
    }

    private val navigationHistory = ArrayDeque<NavigationSnapshot>()
    private val _canNavigateBack = MutableStateFlow(false)
    val canNavigateBack = _canNavigateBack.asStateFlow()
    private val _isNavigatingBack = MutableStateFlow(false)
    val isNavigatingBack = _isNavigatingBack.asStateFlow()

    private fun updateCanNavigateBack() {
        val snapshot = currentNavigationSnapshot()
        _canNavigateBack.value = navigationHistory.isNotEmpty() ||
            !snapshot.isRootHome() ||
            _isSelectionMode.value ||
            _searchQuery.value.isNotBlank()
    }

    private fun currentNavigationSnapshot(): NavigationSnapshot {
        val normFolder = _selectedFolder.value?.takeIf {
            it.isNotBlank() &&
                !it.equals("Todas", ignoreCase = true) &&
                !it.equals("Todas las notas", ignoreCase = true) &&
                !it.equals("Notas", ignoreCase = true)
        }
        val normTag = _selectedTag.value?.takeIf {
            it.isNotBlank() && !it.equals("Todas", ignoreCase = true)
        }
        return NavigationSnapshot(
            screen = currentScreen.value,
            activeTab = _activeTab.value,
            selectedFolder = normFolder,
            selectedTag = normTag,
            isViewingPrivateFolder = _isViewingPrivateFolder.value
        )
    }

    private fun transitionToSnapshot(target: NavigationSnapshot, recordHistory: Boolean = true) {
        val normalizedTarget = target.copy(
            selectedFolder = target.selectedFolder?.takeIf {
                it.isNotBlank() &&
                    !it.equals("Todas", ignoreCase = true) &&
                    !it.equals("Todas las notas", ignoreCase = true) &&
                    !it.equals("Notas", ignoreCase = true)
            },
            selectedTag = target.selectedTag?.takeIf {
                it.isNotBlank() && !it.equals("Todas", ignoreCase = true)
            }
        )
        val current = currentNavigationSnapshot()
        if (current == normalizedTarget) {
            updateCanNavigateBack()
            return
        }

        _isNavigatingBack.value = !recordHistory

        if (recordHistory) {
            if (navigationHistory.lastOrNull() != current) {
                navigationHistory.addLast(current)
                while (navigationHistory.size > 50) {
                    navigationHistory.removeFirst()
                }
            }
        }

        _selectedFolder.value = normalizedTarget.selectedFolder
        _selectedTag.value = normalizedTarget.selectedTag
        _isViewingPrivateFolder.value = normalizedTarget.isViewingPrivateFolder
        if (normalizedTarget.isViewingPrivateFolder) {
            val privateIds = uiState.value.allActiveNotes.filter { it.isLocked }.map { it.id }.toSet()
            _unlockedNoteIds.value = _unlockedNoteIds.value + privateIds
        } else {
            _unlockedNoteIds.value = emptySet()
        }
        _activeTab.value = normalizedTarget.activeTab
        exitSelectionMode()
        currentScreen.value = normalizedTarget.screen
        updateCanNavigateBack()
    }

    fun navigateTo(screen: Screen) {
        val current = currentNavigationSnapshot()
        transitionToSnapshot(current.copy(screen = screen))
    }

    fun navigateBack(): Boolean {
        if (currentScreen.value == Screen.Home) {
            if (_isSelectionMode.value) {
                exitSelectionMode()
                return true
            }
            if (_searchQuery.value.isNotBlank()) {
                _searchQuery.value = ""
                updateCanNavigateBack()
                return true
            }
            if (currentNavigationSnapshot().isRootHome()) {
                navigationHistory.clear()
                updateCanNavigateBack()
                return false
            }
        }

        val current = currentNavigationSnapshot()
        while (navigationHistory.isNotEmpty()) {
            val previous = navigationHistory.removeLast()
            if (previous != current) {
                transitionToSnapshot(previous, recordHistory = false)
                if (previous.isRootHome()) {
                    navigationHistory.clear()
                    updateCanNavigateBack()
                }
                return true
            }
        }

        if (!current.isRootHome()) {
            transitionToSnapshot(NavigationSnapshot(), recordHistory = false)
            navigationHistory.clear()
            updateCanNavigateBack()
            return true
        }

        updateCanNavigateBack()
        return false
    }

    fun openPrivateFolder() {
        transitionToSnapshot(
            NavigationSnapshot(
                screen = Screen.Home,
                activeTab = NavTab.NOTES,
                selectedFolder = null,
                selectedTag = "🔒 Privado",
                isViewingPrivateFolder = true
            )
        )
    }

    fun closePrivateFolder() {
        _isViewingPrivateFolder.value = false
        if (_selectedTag.value.equals("🔒 Privado", ignoreCase = true) ||
            _selectedTag.value.equals("🔒 Privadas", ignoreCase = true) ||
            _selectedTag.value.equals("Bloqueadas", ignoreCase = true)
        ) {
            _selectedTag.value = null
        }
        _unlockedNoteIds.value = emptySet()
    }

    fun toggleLockNote(note: NoteEntity) {
        viewModelScope.launch {
            val updatedLock = !note.isLocked
            saveNote(
                id = note.id,
                title = note.title,
                content = note.content,
                noteType = NoteType.valueOf(note.noteType),
                colorIndex = note.colorIndex,
                paperStyle = note.paperStyle,
                isPinned = note.isPinned,
                isArchived = note.isArchived,
                tags = note.tags,
                folder = note.folder,
                isLocked = updatedLock,
                lockPin = ""
            )
            val msg = if (updatedLock) "Nota movida a Privado" else "Nota quitada de Privado"
            _snackbarMessage.value = msg
        }
    }

    fun selectFolder(folder: String?) {
        transitionToSnapshot(
            NavigationSnapshot(
                screen = Screen.Home,
                activeTab = NavTab.NOTES,
                selectedFolder = folder,
                selectedTag = null,
                isViewingPrivateFolder = false
            )
        )
    }

    fun setFolderFilter(folder: String?) {
        selectFolder(folder)
    }

    fun selectTagAndNavigate(tag: String) {
        if (tag.equals("🔒 Privado", ignoreCase = true) || tag.equals("🔒 Privadas", ignoreCase = true) || tag.equals("Bloqueadas", ignoreCase = true)) {
            openPrivateFolder()
        } else {
            transitionToSnapshot(
                NavigationSnapshot(
                    screen = Screen.Home,
                    activeTab = NavTab.NOTES,
                    selectedFolder = null,
                    selectedTag = tag,
                    isViewingPrivateFolder = false
                )
            )
        }
    }

    fun selectTab(tab: NavTab) {
        if (tab == NavTab.PDF_LIBRARY) {
            navigateTo(Screen.PdfLibrary)
            return
        }
        transitionToSnapshot(
            NavigationSnapshot(
                screen = Screen.Home,
                activeTab = tab,
                selectedFolder = null,
                selectedTag = null,
                isViewingPrivateFolder = false
            )
        )
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.insertFolder(FolderEntity(name = trimmed))
            _snackbarMessage.value = "Carpeta '$trimmed' creada"
        }
    }

    fun renameFolder(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == oldName) return
        viewModelScope.launch {
            repository.renameFolder(oldName, trimmed)
            if (_selectedFolder.value.equals(oldName, ignoreCase = true)) {
                _selectedFolder.value = trimmed
            }
            _snackbarMessage.value = "Carpeta renombrada a '$trimmed'"
        }
    }

    fun deleteFolder(folderName: String) {
        viewModelScope.launch {
            repository.deleteFolderByName(folderName)
            if (_selectedFolder.value.equals(folderName, ignoreCase = true)) {
                _selectedFolder.value = null
            }
            _snackbarMessage.value = "Carpeta '$folderName' eliminada"
        }
    }

    fun renameTag(oldTag: String, newTag: String) {
        val trimmedOld = oldTag.trim().removePrefix("#")
        val trimmedNew = newTag.trim().removePrefix("#")
        if (trimmedNew.isBlank() || trimmedNew == trimmedOld) return
        viewModelScope.launch {
            repository.renameTag(trimmedOld, trimmedNew)
            if (_selectedTag.value.equals(trimmedOld, ignoreCase = true)) {
                _selectedTag.value = trimmedNew
            }
            _snackbarMessage.value = "Etiqueta cambiada a '#$trimmedNew'"
        }
    }

    fun deleteTag(tagName: String) {
        val trimmed = tagName.trim().removePrefix("#")
        viewModelScope.launch {
            repository.deleteTagByName(trimmed)
            if (_selectedTag.value.equals(trimmed, ignoreCase = true)) {
                _selectedTag.value = null
            }
            _snackbarMessage.value = "Etiqueta '#$trimmed' eliminada"
        }
    }

    fun reorderNotes(orderedNoteIds: List<Long>) {
        val currentManual = settingsManager.getManualNoteOrder().toMutableList()
        val updated = mutableListOf<Long>()
        updated.addAll(orderedNoteIds)
        currentManual.forEach { id ->
            if (!updated.contains(id)) {
                updated.add(id)
            }
        }
        settingsManager.setManualNoteOrder(updated)
        _sortOption.value = SortOption.MANUAL
        settingsManager.setDefaultSortOption(SortOption.MANUAL)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateCanNavigateBack()
    }

    fun selectTag(tag: String?) {
        if (tag == "Todas" || tag == null) {
            transitionToSnapshot(
                NavigationSnapshot(
                    screen = Screen.Home,
                    activeTab = NavTab.NOTES,
                    selectedFolder = null,
                    selectedTag = null,
                    isViewingPrivateFolder = false
                )
            )
        } else if (tag.equals("🔒 Privado", ignoreCase = true) || tag.equals("🔒 Privadas", ignoreCase = true) || tag.equals("Bloqueadas", ignoreCase = true)) {
            openPrivateFolder()
        } else {
            transitionToSnapshot(
                NavigationSnapshot(
                    screen = Screen.Home,
                    activeTab = NavTab.NOTES,
                    selectedFolder = null,
                    selectedTag = tag,
                    isViewingPrivateFolder = false
                )
            )
        }
    }

    fun toggleViewMode() {
        val newMode = !_isGridView.value
        _isGridView.value = newMode
        settingsManager.setGridViewDefault(newMode)
    }

    fun setGridViewDefault(isGrid: Boolean) {
        _isGridView.value = isGrid
        settingsManager.setGridViewDefault(isGrid)
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        settingsManager.setDefaultSortOption(option)
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
    }

    fun setPaperThemeMode(mode: PaperThemeMode) {
        settingsManager.setPaperThemeMode(mode)
    }

    fun setDefaultPaperStyle(style: String) {
        settingsManager.setDefaultPaperStyle(style)
    }

    fun setDefaultSortOption(option: SortOption) {
        _sortOption.value = option
        settingsManager.setDefaultSortOption(option)
    }

    fun setGeminiApiKey(key: String) {
        settingsManager.setGeminiApiKey(key)
        _snackbarMessage.value = "Clave API de Gemini guardada"
    }

    fun setDefaultLockPin(pin: String) {
        settingsManager.setDefaultLockPin(pin)
        _snackbarMessage.value = "PIN por defecto actualizado"
    }

    fun setActiveTab(tab: NavTab) {
        selectTab(tab)
    }

    fun enterSelectionMode(initialNoteId: Long? = null) {
        _isSelectionMode.value = true
        if (initialNoteId != null) {
            _selectedNoteIds.value = setOf(initialNoteId)
        }
        updateCanNavigateBack()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedNoteIds.value = emptySet()
        updateCanNavigateBack()
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

    fun batchUpdateColor(colorIndex: Int) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateColorBatch(ids, colorIndex)
            val colorName = NoteColors.options.firstOrNull { it.id == colorIndex }?.name ?: "Color"
            _snackbarMessage.value = "$colorName aplicado a ${ids.size} notas"
            exitSelectionMode()
        }
    }

    fun updateNoteColor(noteId: Long, colorIndex: Int) {
        viewModelScope.launch {
            repository.updateNoteColor(noteId, colorIndex)
        }
    }

    private fun cleanupNoteFiles(note: NoteEntity) {
        if (note.drawingData.isNotBlank()) {
            FileManager.deleteFile(note.drawingData)
        }
        val blocks = NoteBlockSerializer.deserialize(note.content)
        for (block in blocks) {
            if (block is NoteBlock.Drawing) {
                FileManager.deleteFile(block.filePath)
                if (block.pathData.isNotBlank()) {
                    FileManager.deleteFile(block.pathData)
                }
            }
        }
    }

    fun batchDelete(isTrashTab: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            if (isTrashTab) {
                for (id in ids) {
                    val note = repository.getNoteById(id)
                    note?.let { cleanupNoteFiles(it) }
                }
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

    fun unlockNoteBiometrically(noteId: Long) {
        _unlockedNoteIds.value = _unlockedNoteIds.value + noteId
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
            cleanupNoteFiles(note)
            repository.deletePermanently(note)
            _snackbarMessage.value = "Nota eliminada definitivamente"
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val trashed = uiState.value.trashedNotes
            for (note in trashed) {
                cleanupNoteFiles(note)
            }
            repository.emptyTrash(trashed)
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

    fun setReminder(
        context: Context,
        noteId: Long,
        timestamp: Long,
        title: String,
        content: String,
        offsetsMinutes: List<Int> = listOf(0, 15)
    ) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            if (note != null) {
                val updated = note.copy(
                    title = title,
                    content = content,
                    reminderTimestamp = timestamp,
                    isReminderDone = false,
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(updated)
                ReminderScheduler.scheduleReminder(context, noteId, timestamp, title, content, offsetsMinutes)
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

    private val keepImageUri: Uri = Uri.parse("aura_notes://keep_image_uri")
    private val keepReminderTimestamp: Long = Long.MIN_VALUE
    private val keepPdfPath: String = "\u0000KEEP_PDF_PATH\u0000"
    private val keepPdfAnnotationData: String = "\u0000KEEP_PDF_ANNOTATION_DATA\u0000"
    private val keepDrawingData: String = "\u0000KEEP_DRAWING_DATA\u0000"

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
        drawingData: String = keepDrawingData,
        pdfPath: String = keepPdfPath,
        pdfAnnotationData: String = keepPdfAnnotationData,
        isLocked: Boolean = false,
        lockPin: String = "",
        reminderTimestamp: Long? = keepReminderTimestamp,
        imageUri: Uri? = keepImageUri,
        syncEnabled: Boolean? = null,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            // 1. Asegurar la persistencia de los permisos de lectura del URI
            if (imageUri != null && imageUri !== keepImageUri) {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(imageUri, takeFlags)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            
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
                if (content.trim().startsWith("[") || content.trim().startsWith("{")) {
                    try {
                        val deserialized = NoteBlockSerializer.deserialize(content)
                        val updated = deserialized.map { blk ->
                            if (blk is NoteBlock.Text) {
                                blk.copy(content = MathEvaluator.processMathInText(blk.content))
                            } else blk
                        }
                        NoteBlockSerializer.serialize(updated)
                    } catch (_: Exception) {
                        content
                    }
                } else {
                    MathEvaluator.processMathInText(content)
                }
            } else {
                content
            }

            val plainTextForTags = extractNotePlainText(title, processedContent)
            val extractedTags = hashtagRegex.findAll(plainTextForTags).map { it.value.removePrefix("#") }.toList()
            val paramTags = tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
            val finalTagsString = (extractedTags + paramTags).distinct().joinToString(",")

            val existing = if (id > 0) repository.getNoteById(id) else null

            // --- MERGE DE SINCRONIZACIÓN ---
            // Evita que al guardar la nota se sobrescriban las URLs remotas o rutas locales 
            // que se hayan actualizado en segundo plano por el proceso de sincronización.
            val finalProcessedContent = if (existing != null && processedContent.isNotBlank() && existing.content.isNotBlank()) {
                try {
                    val incomingBlocks = NoteBlockSerializer.deserialize(processedContent)
                    val existingBlocks = NoteBlockSerializer.deserialize(existing.content)
                    val existingBlocksMap = existingBlocks.associateBy { it.id }

                    val mergedBlocks = incomingBlocks.map { incoming ->
                        val exist = existingBlocksMap[incoming.id]
                        if (exist != null) {
                            when (incoming) {
                                is NoteBlock.ImageBlock -> {
                                    val e = exist as? NoteBlock.ImageBlock
                                    if (e != null) {
                                        val bestRemoteUrl = e.remoteUrl.ifBlank { incoming.remoteUrl }
                                        val bestLocalUri = if (e.localUri.isNotBlank() && File(e.localUri).exists()) e.localUri else incoming.localUri
                                        incoming.copy(remoteUrl = bestRemoteUrl, localUri = bestLocalUri)
                                    } else incoming
                                }
                                is NoteBlock.Image -> {
                                    val e = exist as? NoteBlock.Image
                                    if (e != null) {
                                        val bestRemoteUrl = e.remoteUrl.ifBlank { incoming.remoteUrl }
                                        val bestFilePath = if (e.filePath.isNotBlank() && File(e.filePath).exists()) e.filePath else incoming.filePath
                                        incoming.copy(remoteUrl = bestRemoteUrl, filePath = bestFilePath)
                                    } else incoming
                                }
                                is AudioBlock -> {
                                    val e = exist as? AudioBlock
                                    if (e != null) {
                                        val bestRemoteUrl = e.remoteUrl.ifBlank { incoming.remoteUrl }
                                        val bestFilePath = if (e.filePath.isNotBlank() && File(e.filePath).exists()) e.filePath else incoming.filePath
                                        incoming.copy(remoteUrl = bestRemoteUrl, filePath = bestFilePath)
                                    } else incoming
                                }
                                is PdfBlock -> {
                                    val e = exist as? PdfBlock
                                    if (e != null) {
                                        val bestRemoteUrl = e.remoteUrl.ifBlank { incoming.remoteUrl }
                                        val bestFilePath = if (e.filePath.isNotBlank() && File(e.filePath).exists()) e.filePath else incoming.filePath
                                        incoming.copy(remoteUrl = bestRemoteUrl, filePath = bestFilePath)
                                    } else incoming
                                }
                                else -> incoming
                            }
                        } else {
                            incoming
                        }
                    }
                    NoteBlockSerializer.serialize(mergedBlocks)
                } catch (e: Exception) {
                    processedContent
                }
            } else {
                processedContent
            }

            val finalReminderTimestamp = if (reminderTimestamp == keepReminderTimestamp) {
                existing?.reminderTimestamp
            } else {
                reminderTimestamp
            }

            val finalPdfPath = if (pdfPath == keepPdfPath) {
                existing?.pdfPath ?: ""
            } else {
                pdfPath
            }

            val finalPdfAnnotationData = if (pdfAnnotationData == keepPdfAnnotationData) {
                existing?.pdfAnnotationData ?: ""
            } else {
                pdfAnnotationData
            }

            val finalDrawingData = if (drawingData == keepDrawingData) {
                existing?.drawingData ?: ""
            } else {
                drawingData
            }

            val finalImageUri = if (imageUri === keepImageUri) {
                existing?.imageUri
            } else {
                imageUri
            }

            val blocksPdfText = try {
                NoteBlockSerializer.deserialize(finalProcessedContent)
                    .filterIsInstance<PdfBlock>()
                    .mapNotNull { block ->
                        if (block.indexedText.isNotBlank()) block.indexedText
                        else if (block.filePath.isNotBlank()) {
                            val f = File(block.filePath)
                            if (f.exists()) PdfTextExtractor.extractTextFromPdf(context, f) else null
                        } else null
                    }.joinToString("\n")
            } catch (_: Exception) { "" }

            val extractedPdfText = if (blocksPdfText.isNotBlank()) {
                blocksPdfText.take(30000)
            } else if (finalPdfPath.isNotEmpty()) {
                existing?.pdfExtractedText ?: withContext(Dispatchers.IO) {
                    val pdfFile = File(finalPdfPath)
                    if (pdfFile.exists()) PdfTextExtractor.extractTextFromPdf(context, pdfFile) else null
                }
            } else {
                existing?.pdfExtractedText
            }

            val finalSyncEnabled = syncEnabled ?: existing?.syncEnabled ?: true

            val entity = if (existing != null) {
                existing.copy(
                    title = title,
                    content = finalProcessedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = finalTagsString,
                    folder = folder,
                    checklistData = jsonChecklist,
                    drawingData = finalDrawingData,
                    pdfPath = finalPdfPath,
                    pdfAnnotationData = finalPdfAnnotationData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    reminderTimestamp = finalReminderTimestamp,
                    updatedAt = now,
                    imageUri = finalImageUri,
                    pdfExtractedText = extractedPdfText,
                    syncEnabled = finalSyncEnabled
                )
            } else {
                NoteEntity(
                    title = title,
                    content = finalProcessedContent,
                    noteType = noteType.name,
                    colorIndex = colorIndex,
                    paperStyle = paperStyle,
                    isPinned = isPinned,
                    isArchived = isArchived,
                    tags = finalTagsString,
                    folder = folder,
                    checklistData = jsonChecklist,
                    drawingData = finalDrawingData,
                    pdfPath = finalPdfPath,
                    pdfAnnotationData = finalPdfAnnotationData,
                    isLocked = isLocked,
                    lockPin = lockPin,
                    reminderTimestamp = finalReminderTimestamp,
                    createdAt = now,
                    updatedAt = now,
                    imageUri = finalImageUri,
                    pdfExtractedText = extractedPdfText,
                    syncEnabled = finalSyncEnabled
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


    fun insertPdfBlock(
        uri: Uri,
        customFileName: String? = null,
        onBlockCreated: (PdfBlock) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                
                val pdfBlock = withContext(Dispatchers.IO) {
                    val pdfsDir = File(context.filesDir, "pdfs")
                    if (!pdfsDir.exists()) pdfsDir.mkdirs()

                    val formattedCustomName = if (!customFileName.isNullOrBlank()) {
                        val trimmed = customFileName.trim()
                        if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed else "$trimmed.pdf"
                    } else null

                    val localFileName = formattedCustomName ?: "${UUID.randomUUID()}.pdf"
                    val destFile = File(pdfsDir, localFileName)

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val pageCount = PdfRenderUtil.getPageCount(destFile)
                    val fileName = formattedCustomName ?: (getFileNameFromUri(context, uri) ?: "Documento.pdf")

                    val fileSize = destFile.length()
                    val fileSizeFormatted = if (fileSize < 1024 * 1024) {
                        "${fileSize / 1024} KB"
                    } else {
                        String.format(Locale.getDefault(), "%.1f MB", fileSize / (1024.0 * 1024.0))
                    }

                    val indexedText = PdfTextExtractor.extractTextFromPdf(context, destFile) ?: ""

                    PdfBlock(
                        filePath = destFile.absolutePath,
                        fileName = fileName,
                        fileSizeFormatted = fileSizeFormatted,
                        pageCount = pageCount,
                        indexedText = indexedText
                    )
                }

                onBlockCreated(pdfBlock)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Error al adjuntar PDF"
            }
        }
    }

    fun createNoteWithPdf(uri: Uri, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                
                val insertedId = withContext(Dispatchers.IO) {
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

                    val indexedText = PdfTextExtractor.extractTextFromPdf(context, destFile) ?: ""

                    val pdfBlock = PdfBlock(
                        filePath = destFile.absolutePath,
                        fileName = fileName,
                        fileSizeFormatted = fileSizeFormatted,
                        pageCount = pageCount,
                        indexedText = indexedText
                    )

                    val serializedBlocks = NoteBlockSerializer.serialize(listOf(pdfBlock))
                    val now = System.currentTimeMillis()
                    val titleName = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                        fileName.dropLast(4)
                    } else fileName

                    val newNote = NoteEntity(
                        id = 0,
                        title = titleName,
                        content = serializedBlocks,
                        noteType = NoteType.TEXT.name,
                        pdfPath = destFile.absolutePath,
                        pdfExtractedText = indexedText,
                        createdAt = now,
                        updatedAt = now
                    )
                    repository.insert(newNote)
                }
                
                _snackbarMessage.value = "PDF cargado correctamente"
                onCreated(insertedId)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Error al cargar PDF"
            }
        }
    }

    fun savePdfToDownloads(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri("external")

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "Escaneo_Aura_${System.currentTimeMillis()}.pdf")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/AuraNotes"
                        )
                    }
                }

                val newUri = contentResolver.insert(collection, contentValues)
                if (newUri != null) {
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        contentResolver.openOutputStream(newUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    _snackbarMessage.value = "Guardado en Descargas/Note JS"
                } else {
                    _snackbarMessage.value = "Error al guardar en Descargas"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "Error al guardar en Descargas"
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

    fun rotatePdfPage(context: Context, file: File, pageIndex: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = PdfModifierUtil.rotatePage(context, file, pageIndex)
            if (success) {
                _pdfLastModified.value = System.currentTimeMillis()
                _snackbarMessage.value = "Página rotada 90°"
            } else {
                _snackbarMessage.value = "Error al rotar la página"
            }
            onResult(success)
        }
    }

    fun deletePdfPage(context: Context, file: File, pageIndex: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = PdfModifierUtil.deletePage(context, file, pageIndex)
            if (success) {
                _pdfLastModified.value = System.currentTimeMillis()
                _snackbarMessage.value = "Página eliminada"
            } else {
                _snackbarMessage.value = "Error al eliminar la página"
            }
            onResult(success)
        }
    }

    fun insertPdfPage(context: Context, file: File, pageIndex: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = PdfModifierUtil.insertBlankPage(context, file, pageIndex)
            if (success) {
                _pdfLastModified.value = System.currentTimeMillis()
                _snackbarMessage.value = "Página en blanco insertada"
            } else {
                _snackbarMessage.value = "Error al insertar la página"
            }
            onResult(success)
        }
    }

    fun saveSignatureToVault(points: List<DrawingPoint>) {
        if (points.isEmpty()) return
        viewModelScope.launch {
            val json = SignatureHelper.serializePoints(points)
            val entity = SignatureEntity(
                pointsJson = json,
                createdAt = System.currentTimeMillis()
            )
            repository.insertSignature(entity)
        }
    }

    fun deleteSignatureFromVault(signatureId: Long) {
        viewModelScope.launch {
            repository.deleteSignatureById(signatureId)
        }
    }

    fun stopListeningToCloud() {
        repository.stopListeningToCloud(resetToAnonymous = true)
    }

    /**
     * Inicia sesión con Google mediante la API moderna Credential Manager vinculada a Firebase Auth,
     * reclamando las notas anónimas ('LOCAL_ANONYMOUS') y drenando la cola de sincronización offline.
     */
    fun signInWithCredentialManager(
        activity: Activity,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (_isSigningIn.value) return
        viewModelScope.launch {
            _isSigningIn.value = true
            when (val result = authManager.signInWithCredentialManager(activity)) {
                is AuthResult.Success -> {
                    repository.startListeningToCloud(result.user.uid)
                    val msg = "Sesión iniciada como ${result.user.displayName ?: result.user.email ?: "Usuario"}"
                    _snackbarMessage.value = msg
                    _isSigningIn.value = false
                    onComplete(true, msg)
                }
                is AuthResult.Cancelled -> {
                    _isSigningIn.value = false
                    onComplete(false, "Inicio de sesión cancelado")
                }
                is AuthResult.Error -> {
                    _snackbarMessage.value = result.message
                    _isSigningIn.value = false
                    onComplete(false, result.message)
                }
            }
        }
    }

    /**
     * Cierra la sesión en Firebase Auth y Credential Manager, aislando las notas de la cuenta anterior.
     */
    fun signOutAccount(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.stopListeningToCloud(resetToAnonymous = true)
            authManager.signOutAsync()
            _snackbarMessage.value = "Sesión cerrada"
            onComplete()
        }
    }

    private fun resolveEffectiveCloudUserId(): String? {
        val fbUid = currentUser.value?.uid ?: authManager.currentUser?.uid
        if (!fbUid.isNullOrBlank()) return fbUid
        return null
    }

    /**
     * Fuerza el drenado inmediato de la cola de sincronización offline y la descarga de notas remotas desde Firestore.
     */
    fun syncPendingNotesNow() {
        viewModelScope.launch(Dispatchers.IO) {
            var uid = resolveEffectiveCloudUserId()
            if (uid == null) {
                val driveAccount = try {
                    GoogleDriveManager(getApplication()).getSignedInAccount()
                } catch (_: Exception) {
                    null
                }
                val idToken = driveAccount?.idToken
                if (!idToken.isNullOrBlank()) {
                    uid = authManager.signInWithGoogleIdToken(idToken)
                }
            }

            if (uid != null) {
                firestoreManager.setSyncState(SyncState.SYNCING)
                repository.syncPendingNotesSuspending(uid, forcePushAll = true)
                repository.startListeningToCloud(uid)
                firestoreManager.setSyncState(SyncState.SYNCED)
                _snackbarMessage.value = "Sincronización en la nube completada"
            } else {
                _snackbarMessage.value = "Inicia sesión con Google para sincronizar tus notas en la nube"
            }
        }
    }

    fun resetDriveSyncProgress() {
        _driveSyncProgress.value = DriveSyncProgress.Idle
    }

    /**
     * Sincroniza manualmente todas las notas y archivos PDF locales con Google Drive y Note JS Cloud (Firestore).
     */
    fun syncNotesAndPdfsToDrive(
        onAuthRequired: (Intent) -> Unit = {},
        onCompleted: (DriveSyncResult) -> Unit = {}
    ) {
        val context = getApplication<Application>()
        val driveManager = GoogleDriveManager(context)
        val account = try {
            driveManager.getSignedInAccount()
        } catch (_: Exception) {
            null
        }
        val effectiveUid = resolveEffectiveCloudUserId()

        if (account == null && effectiveUid == null) {
            val signInIntent = driveManager.getGoogleSignInClient().signInIntent
            onAuthRequired(signInIntent)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _driveSyncProgress.value = DriveSyncProgress.Syncing(
                phase = "Iniciando sincronización...",
                currentItemName = "Conectando con Note JS Cloud...",
                current = 0,
                total = 1,
                percent = 0.05f
            )

            var activeUid = effectiveUid
            if (activeUid == null && account?.idToken != null) {
                activeUid = authManager.signInWithGoogleIdToken(account.idToken!!)
            }

            if (activeUid != null) {
                repository.syncPendingNotesSuspending(activeUid, forcePushAll = true)
            }

            val notes = repository.getAllNotes()
            val pdfFiles = DriveSyncManager.getLocalPdfFiles(context)

            try {
                val driveService = account?.let {
                    try {
                        driveManager.getDriveService(it)
                    } catch (_: Exception) {
                        null
                    }
                }

                val result = DriveSyncManager.syncNotesAndPdfs(
                    context = context,
                    driveService = driveService,
                    notes = notes,
                    pdfFiles = pdfFiles
                ) { progress ->
                    _driveSyncProgress.value = progress
                }

                if (result.success && activeUid != null) {
                    repository.syncPendingNotesSuspending(activeUid, forcePushAll = true)
                    firestoreManager.setSyncState(SyncState.SYNCED)
                    _lastDriveSyncTime.value = System.currentTimeMillis()
                }

                withContext(Dispatchers.Main) {
                    onCompleted(result)
                }
            } catch (e: Exception) {
                val fallbackResult = DriveSyncManager.syncNotesAndPdfs(
                    context = context,
                    driveService = null,
                    notes = notes,
                    pdfFiles = pdfFiles
                ) { progress ->
                    _driveSyncProgress.value = progress
                }
                if (fallbackResult.success && activeUid != null) {
                    repository.syncPendingNotesSuspending(activeUid, forcePushAll = true)
                    firestoreManager.setSyncState(SyncState.SYNCED)
                    _lastDriveSyncTime.value = System.currentTimeMillis()
                }
                withContext(Dispatchers.Main) {
                    onCompleted(fallbackResult)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListeningToCloud()
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
                        if (block.indexedText.isNotBlank()) {
                            append(block.indexedText).append(" ")
                        }
                    }
                    is NoteBlock.ImageBlock -> {}
                    is NoteBlock.Image -> {
                        append(block.fileName).append(" ")
                    }
                    is AudioBlock -> {
                        if (block.transcription.isNotBlank()) {
                            append(block.transcription).append(" ")
                        }
                    }
                    is NoteBlock.Math -> {
                        if (block.latexCode.isNotBlank()) {
                            append(block.latexCode).append(" ")
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
