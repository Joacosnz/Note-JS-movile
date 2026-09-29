package com.example

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openNoteId = intent.getLongExtra("EXTRA_OPEN_NOTE_ID", 0L)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesApp(initialOpenNoteId = openNoteId)
                }
            }
        }
    }
}

@Composable
fun NotesApp(initialOpenNoteId: Long = 0L, viewModel: NotesViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    LaunchedEffect(initialOpenNoteId) {
        if (initialOpenNoteId > 0L) {
            viewModel.navigateTo(Screen.Editor(noteId = initialOpenNoteId))
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
                    viewModel = viewModel
                )
            }
            is Screen.Drawing -> {
                DrawingScreen(
                    noteId = screen.noteId,
                    viewModel = viewModel
                )
            }
        }
    }
}
