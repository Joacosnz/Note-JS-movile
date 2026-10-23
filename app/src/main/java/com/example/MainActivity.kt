package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfLibraryScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ThemeMode
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen

class MainActivity : FragmentActivity() {

    companion object {
        const val ACTION_QUICK_NOTE = "com.example.ACTION_QUICK_NOTE"
        const val ACTION_QUICK_VOICE = "com.example.ACTION_QUICK_VOICE"
        const val ACTION_QUICK_SCAN = "com.example.ACTION_QUICK_SCAN"
    }

    private var openNoteIdState by mutableLongStateOf(0L)
    private var createNewNoteState by mutableStateOf(false)
    private var openVoiceDictationState by mutableStateOf(false)
    private var openDocumentScannerState by mutableStateOf(false)
    private var openPdfUriState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleQuickActionIntent(intent)
        setContent {
            NotesApp(
                initialOpenNoteId = openNoteIdState,
                initialCreateNewNote = createNewNoteState,
                initialOpenVoiceDictation = openVoiceDictationState,
                initialOpenDocumentScanner = openDocumentScannerState,
                initialOpenPdfUri = openPdfUriState,
                onNoteOpened = {
                    openNoteIdState = 0L
                    createNewNoteState = false
                    openVoiceDictationState = false
                    openDocumentScannerState = false
                    openPdfUriState = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickActionIntent(intent)
    }

    private fun handleQuickActionIntent(intent: Intent?) {
        if (intent == null) return
        
        val newNoteId = intent.getLongExtra("EXTRA_OPEN_NOTE_ID", 0L)
        val createNew = intent.getBooleanExtra("EXTRA_CREATE_NEW_NOTE", false)
        val action = intent.action
        val type = intent.type
        val data = intent.data

        if (action == Intent.ACTION_VIEW) {
            if (data != null && (type == "application/pdf" || data.toString().lowercase().endsWith(".pdf") || intent.scheme == "content" || intent.scheme == "file")) {
                openPdfUriState = data.toString()
                openNoteIdState = 0L
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                intent.action = null
                return
            }
        }

        when {
            newNoteId > 0L -> {
                openNoteIdState = newNoteId
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
            }
            createNew || action == ACTION_QUICK_NOTE || action == "com.example.ACTION_NEW_NOTE" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
            }
            action == ACTION_QUICK_VOICE || action == "com.example.ACTION_VOICE_NOTE" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = true
                openDocumentScannerState = false
                openPdfUriState = null
            }
            action == ACTION_QUICK_SCAN || action == "com.example.ACTION_SCAN_DOCUMENT" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openDocumentScannerState = true
                openVoiceDictationState = false
                openPdfUriState = null
            }
        }
        
        if (action == ACTION_QUICK_NOTE || action == ACTION_QUICK_VOICE || action == ACTION_QUICK_SCAN || action == "com.example.ACTION_NEW_NOTE" || action == "com.example.ACTION_VOICE_NOTE" || action == "com.example.ACTION_SCAN_DOCUMENT") {
            intent.action = null
        }
    }
}

@Composable
fun NotesApp(
    initialOpenNoteId: Long = 0L,
    initialCreateNewNote: Boolean = false,
    initialOpenVoiceDictation: Boolean = false,
    initialOpenDocumentScanner: Boolean = false,
    initialOpenPdfUri: String? = null,
    onNoteOpened: () -> Unit = {},
    viewModel: NotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canNavigateBack by viewModel.canNavigateBack.collectAsStateWithLifecycle()
    val isNavigatingBack by viewModel.isNavigatingBack.collectAsStateWithLifecycle()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    // Root BackHandler manages system back press across all screens and tabs,
    // popping the navigation history step-by-step until reaching the root Home screen.
    // Child BackHandlers (e.g., Editor auto-save, Drawing save, or modal sheets)
    // registered deeper in the composition hierarchy automatically take priority when enabled.
    BackHandler(enabled = true) {
        if (canNavigateBack && viewModel.navigateBack()) {
            return@BackHandler
        }
        if (!viewModel.navigateBack()) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Presione de nuevo para salir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LaunchedEffect(initialOpenNoteId, initialCreateNewNote, initialOpenVoiceDictation, initialOpenDocumentScanner, initialOpenPdfUri) {
                if (!initialOpenPdfUri.isNullOrEmpty()) {
                    viewModel.navigateTo(Screen.PdfViewer(noteId = 0L, pdfPath = initialOpenPdfUri))
                    onNoteOpened()
                } else if (initialOpenNoteId > 0L) {
                    viewModel.navigateTo(Screen.Editor(noteId = initialOpenNoteId))
                    onNoteOpened()
                } else if (initialCreateNewNote || initialOpenVoiceDictation || initialOpenDocumentScanner) {
                    viewModel.navigateTo(
                        Screen.Editor(
                            noteId = 0L,
                            openVoiceDictation = initialOpenVoiceDictation,
                            openDocumentScanner = initialOpenDocumentScanner
                        )
                    )
                    onNoteOpened()
                }
            }

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (isNavigatingBack) {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    is Screen.Home -> {
                        HomeScreen(viewModel = viewModel)
                    }
                    is Screen.Folders -> {
                        FoldersScreen(viewModel = viewModel)
                    }
                    is Screen.Editor -> {
                        NoteEditorScreen(
                            noteId = screen.noteId,
                            initialType = screen.initialType,
                            initialOpenVoiceDictation = screen.openVoiceDictation,
                            initialOpenDocumentScanner = screen.openDocumentScanner,
                            viewModel = viewModel
                        )
                    }
                    is Screen.Drawing -> {
                        DrawingScreen(
                            noteId = screen.noteId,
                            viewModel = viewModel
                        )
                    }
                    is Screen.PdfViewer -> {
                        PdfViewerScreen(
                            noteId = screen.noteId,
                            pdfPath = screen.pdfPath,
                            viewModel = viewModel
                        )
                    }
                    is Screen.PdfLibrary -> {
                        PdfLibraryScreen(
                            viewModel = viewModel
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

