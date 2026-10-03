package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ThemeMode
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private var openNoteIdState by mutableLongStateOf(0L)
    private var createNewNoteState by mutableStateOf(false)
    private var openVoiceDictationState by mutableStateOf(false)
    private var openDocumentScannerState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            NotesApp(
                initialOpenNoteId = openNoteIdState,
                initialCreateNewNote = createNewNoteState,
                initialOpenVoiceDictation = openVoiceDictationState,
                initialOpenDocumentScanner = openDocumentScannerState,
                onNoteOpened = {
                    openNoteIdState = 0L
                    createNewNoteState = false
                    openVoiceDictationState = false
                    openDocumentScannerState = false
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val newNoteId = intent.getLongExtra("EXTRA_OPEN_NOTE_ID", 0L)
        val createNew = intent.getBooleanExtra("EXTRA_CREATE_NEW_NOTE", false)
        val action = intent.action

        when {
            newNoteId > 0L -> {
                openNoteIdState = newNoteId
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
            }
            createNew || action == "com.example.ACTION_NEW_NOTE" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = false
                openDocumentScannerState = false
            }
            action == "com.example.ACTION_VOICE_NOTE" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = true
                openDocumentScannerState = false
            }
            action == "com.example.ACTION_SCAN_DOCUMENT" -> {
                openNoteIdState = 0L
                createNewNoteState = true
                openDocumentScannerState = true
                openVoiceDictationState = false
            }
        }
    }
}

@Composable
fun NotesApp(
    initialOpenNoteId: Long = 0L,
    initialCreateNewNote: Boolean = false,
    initialOpenVoiceDictation: Boolean = false,
    initialOpenDocumentScanner: Boolean = false,
    onNoteOpened: () -> Unit = {},
    viewModel: NotesViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LaunchedEffect(initialOpenNoteId, initialCreateNewNote, initialOpenVoiceDictation, initialOpenDocumentScanner) {
                if (initialOpenNoteId > 0L) {
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
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
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
                    is Screen.Settings -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
