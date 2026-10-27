package com.example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NoteLockDialog
import com.example.ui.screens.DrawingScreen
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfLibraryScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.utils.BiometricAuthManager
import com.example.util.LauncherIconManager
import com.example.util.ThemeMode
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.Screen

class MainActivity : FragmentActivity() {

    companion object {
        const val ACTION_QUICK_NOTE = "com.example.ACTION_QUICK_NOTE"
        const val ACTION_QUICK_VOICE = "com.example.ACTION_QUICK_VOICE"
        const val ACTION_QUICK_SCAN = "com.example.ACTION_QUICK_SCAN"
        const val ACTION_CREATE_REMINDER = "com.example.notejs.CREATE_REMINDER"
    }

    private var openNoteIdState by mutableLongStateOf(0L)
    private var openReminderIdState by mutableLongStateOf(0L)
    private var createNewReminderState by mutableStateOf(false)
    private var createNewNoteState by mutableStateOf(false)
    private var openVoiceDictationState by mutableStateOf(false)
    private var openDocumentScannerState by mutableStateOf(false)
    private var openPdfUriState by mutableStateOf<String?>(null)
    private var sharedTextState by mutableStateOf<String?>(null)
    private var sharedTitleState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleQuickActionIntent(intent)
        setContent {
            NotesApp(
                initialOpenNoteId = openNoteIdState,
                initialOpenReminderId = openReminderIdState,
                initialCreateNewReminder = createNewReminderState,
                initialCreateNewNote = createNewNoteState,
                initialOpenVoiceDictation = openVoiceDictationState,
                initialOpenDocumentScanner = openDocumentScannerState,
                initialOpenPdfUri = openPdfUriState,
                initialSharedText = sharedTextState,
                initialSharedTitle = sharedTitleState,
                onNoteOpened = {
                    openNoteIdState = 0L
                    openReminderIdState = 0L
                    createNewReminderState = false
                    createNewNoteState = false
                    openVoiceDictationState = false
                    openDocumentScannerState = false
                    openPdfUriState = null
                    sharedTextState = null
                    sharedTitleState = null
                }
            )
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        LauncherIconManager.syncLauncherIcon(this, newConfig = newConfig)
    }

    override fun onResume() {
        super.onResume()
        LauncherIconManager.onMainActivityResumed(this, resources.configuration)
    }

    override fun onPause() {
        super.onPause()
        LauncherIconManager.onMainActivityPausedOrStopped(this, resources.configuration)
    }

    override fun onStop() {
        super.onStop()
        LauncherIconManager.onMainActivityPausedOrStopped(this, resources.configuration)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickActionIntent(intent)
    }

    private fun handleQuickActionIntent(intent: Intent?) {
        if (intent == null) return
        
        val newNoteId = intent.getLongExtra("EXTRA_OPEN_NOTE_ID", 0L)
        val reminderId = intent.getLongExtra("EXTRA_OPEN_REMINDER_ID", 0L)
        val openCalendar = intent.getBooleanExtra("EXTRA_OPEN_CALENDAR", false)
        val createNew = intent.getBooleanExtra("EXTRA_CREATE_NEW_NOTE", false)
        val action = intent.action
        val type = intent.type
        val data = intent.data

        if (action == Intent.ACTION_VIEW) {
            if (data != null && (type == "application/pdf" || data.toString().lowercase().endsWith(".pdf") || intent.scheme == "content" || intent.scheme == "file")) {
                openPdfUriState = data.toString()
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                sharedTextState = null
                sharedTitleState = null
                intent.action = null
                return
            }
        }

        if (action == Intent.ACTION_PROCESS_TEXT) {
            val selectedText = (intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT_READONLY))
                ?.toString()?.trim()
            if (!selectedText.isNullOrEmpty()) {
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = selectedText
                sharedTitleState = null
                intent.action = null
                return
            }
        }

        if (action == Intent.ACTION_SEND && type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
            val sharedSubject = (intent.getStringExtra(Intent.EXTRA_SUBJECT)
                ?: intent.getStringExtra(Intent.EXTRA_TITLE))?.trim()
            val fullContent = when {
                !sharedSubject.isNullOrEmpty() && !sharedText.isNullOrEmpty() && !sharedText.contains(sharedSubject) ->
                    "$sharedSubject\n$sharedText"
                !sharedText.isNullOrEmpty() -> sharedText
                !sharedSubject.isNullOrEmpty() -> sharedSubject
                else -> null
            }
            if (!fullContent.isNullOrEmpty()) {
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewNoteState = true
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = fullContent
                sharedTitleState = null
                intent.action = null
                return
            }
        }

        when {
            newNoteId > 0L -> {
                openNoteIdState = newNoteId
                openReminderIdState = 0L
                createNewReminderState = false
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
            action == ACTION_CREATE_REMINDER || action == "com.example.ACTION_CREATE_REMINDER" || action == "com.example.CREATE_REMINDER" -> {
                openNoteIdState = 0L
                openReminderIdState = -2L
                createNewReminderState = true
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
            reminderId != 0L || openCalendar -> {
                openReminderIdState = if (reminderId > 0L) reminderId else -1L
                openNoteIdState = 0L
                createNewReminderState = false
                createNewNoteState = false
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
            createNew || action == ACTION_QUICK_NOTE || action == "com.example.ACTION_NEW_NOTE" -> {
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewReminderState = false
                createNewNoteState = true
                openVoiceDictationState = false
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
            action == ACTION_QUICK_VOICE || action == "com.example.ACTION_VOICE_NOTE" -> {
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewReminderState = false
                createNewNoteState = true
                openVoiceDictationState = true
                openDocumentScannerState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
            action == ACTION_QUICK_SCAN || action == "com.example.ACTION_SCAN_DOCUMENT" -> {
                openNoteIdState = 0L
                openReminderIdState = 0L
                createNewReminderState = false
                createNewNoteState = true
                openDocumentScannerState = true
                openVoiceDictationState = false
                openPdfUriState = null
                sharedTextState = null
                sharedTitleState = null
            }
        }
        
        if (action == ACTION_QUICK_NOTE || action == ACTION_QUICK_VOICE || action == ACTION_QUICK_SCAN || action == ACTION_CREATE_REMINDER || action == "com.example.ACTION_NEW_NOTE" || action == "com.example.ACTION_VOICE_NOTE" || action == "com.example.ACTION_SCAN_DOCUMENT" || action == "com.example.CREATE_REMINDER" || action == "com.example.ACTION_CREATE_REMINDER") {
            intent.action = null
        }
    }
}

@Composable
fun NotesApp(
    initialOpenNoteId: Long = 0L,
    initialOpenReminderId: Long = 0L,
    initialCreateNewReminder: Boolean = false,
    initialCreateNewNote: Boolean = false,
    initialOpenVoiceDictation: Boolean = false,
    initialOpenDocumentScanner: Boolean = false,
    initialOpenPdfUri: String? = null,
    initialSharedText: String? = null,
    initialSharedTitle: String? = null,
    onNoteOpened: () -> Unit = {},
    viewModel: NotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canNavigateBack by viewModel.canNavigateBack.collectAsStateWithLifecycle()
    val isNavigatingBack by viewModel.isNavigatingBack.collectAsStateWithLifecycle()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var isSessionBiometricallyUnlocked by rememberSaveable { mutableStateOf(false) }

    val isSystemDark = isSystemInDarkTheme()
    LaunchedEffect(isSystemDark) {
        LauncherIconManager.syncLauncherIcon(context, isDarkOverride = isSystemDark)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { _ -> }

        LaunchedEffect(Unit) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
            if (uiState.isAppBiometricLockEnabled && !isSessionBiometricallyUnlocked) {
                BiometricAppLockScreen(
                    isDark = isDarkTheme,
                    expectedPin = uiState.defaultLockPin,
                    isBiometricEnabled = uiState.isBiometricLockEnabled,
                    onUnlocked = {
                        isSessionBiometricallyUnlocked = true
                    }
                )
                return@Surface
            }

            var pendingOpenReminderId by rememberSaveable { mutableLongStateOf(0L) }

            LaunchedEffect(
                initialOpenNoteId,
                initialOpenReminderId,
                initialCreateNewReminder,
                initialCreateNewNote,
                initialOpenVoiceDictation,
                initialOpenDocumentScanner,
                initialOpenPdfUri,
                initialSharedText,
                initialSharedTitle
            ) {
                if (!initialOpenPdfUri.isNullOrEmpty()) {
                    viewModel.navigateTo(Screen.PdfViewer(noteId = 0L, pdfPath = initialOpenPdfUri))
                    onNoteOpened()
                } else if (initialOpenNoteId > 0L) {
                    viewModel.navigateTo(Screen.Editor(noteId = initialOpenNoteId))
                    onNoteOpened()
                } else if (initialCreateNewReminder || initialOpenReminderId == -2L) {
                    pendingOpenReminderId = -2L
                    viewModel.navigateTo(Screen.Reminders(initialCreateNewReminder = true))
                    onNoteOpened()
                } else if (initialOpenReminderId != 0L) {
                    if (initialOpenReminderId > 0L) {
                        pendingOpenReminderId = initialOpenReminderId
                    }
                    viewModel.navigateTo(Screen.Reminders(initialOpenReminderId = pendingOpenReminderId))
                    onNoteOpened()
                } else if (initialCreateNewNote || initialOpenVoiceDictation || initialOpenDocumentScanner || !initialSharedText.isNullOrEmpty() || !initialSharedTitle.isNullOrEmpty()) {
                    viewModel.navigateTo(
                        Screen.Editor(
                            noteId = 0L,
                            openVoiceDictation = initialOpenVoiceDictation,
                            openDocumentScanner = initialOpenDocumentScanner,
                            initialSharedText = initialSharedText,
                            initialSharedTitle = initialSharedTitle
                        )
                    )
                    onNoteOpened()
                }
            }

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState is Screen.Reminders && initialState is Screen.Reminders) {
                        fadeIn(animationSpec = tween(200)).togetherWith(
                            fadeOut(animationSpec = tween(200))
                        )
                    } else if (targetState is Screen.Reminders) {
                        // Recordatorios / Calendario: desplazamiento vertical de abajo hacia arriba
                        (slideInVertically(
                            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                            initialOffsetY = { fullHeight -> fullHeight }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 240)
                        )).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                                targetOffsetY = { fullHeight -> -fullHeight / 4 }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    } else if (targetState is Screen.Folders) {
                        // Carpetas: desplazamiento horizontal desde la derecha hacia la izquierda
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth / 3 }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    } else if (targetState is Screen.Home) {
                        // Página principal (Inicio): desplazamiento horizontal hacia la derecha desde la izquierda
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> fullWidth / 3 }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    } else if (initialState is Screen.Reminders) {
                        (slideInVertically(
                            animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                            initialOffsetY = { fullHeight -> -fullHeight / 4 }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 240)
                        )).togetherWith(
                            slideOutVertically(
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                                targetOffsetY = { fullHeight -> fullHeight }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    } else if (isNavigatingBack) {
                        (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                fadeIn(animationSpec = tween(180))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width } +
                                    fadeOut(animationSpec = tween(180))
                        )
                    } else {
                        (slideInHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> width } +
                                fadeIn(animationSpec = tween(180))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(240, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                    fadeOut(animationSpec = tween(180))
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
                            initialSharedText = screen.initialSharedText,
                            initialSharedTitle = screen.initialSharedTitle,
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
                    is Screen.Reminders -> {
                        RemindersScreen(
                            viewModel = viewModel,
                            initialOpenReminderId = if (screen.initialOpenReminderId != 0L) screen.initialOpenReminderId else pendingOpenReminderId,
                            initialCreateNewReminder = screen.initialCreateNewReminder || (pendingOpenReminderId == -2L),
                            creationTriggerTimestamp = screen.triggerTimestamp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricAppLockScreen(
    isDark: Boolean,
    expectedPin: String,
    isBiometricEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber
    var showPinDialog by remember { mutableStateOf(false) }

    val triggerBiometricPrompt = {
        if (isBiometricEnabled) {
            BiometricAuthManager.authenticateWithBiometrics(
                context = context,
                title = "Desbloquear Note JS",
                subtitle = "Usa tu huella dactilar o reconocimiento facial para acceder a tus notas",
                onSuccess = onUnlocked,
                onFallbackToPin = {
                    showPinDialog = true
                },
                onError = { _ -> }
            )
        } else {
            showPinDialog = true
        }
    }

    LaunchedEffect(Unit) {
        triggerBiometricPrompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("biometric_app_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(amber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Protección Biométrica",
                    tint = amber,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Note JS Protegido",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tus notas están aseguradas. Verifica tu identidad con huella dactilar, desbloqueo facial o tu PIN de respaldo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { triggerBiometricPrompt() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = amber,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_app_lock_biometric")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Desbloquear con Huella o Rostro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showPinDialog = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_app_lock_pin")
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ingresar PIN de Respaldo",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showPinDialog) {
        NoteLockDialog(
            isSettingPin = false,
            expectedPin = expectedPin,
            title = "Desbloquear Note JS",
            subtitle = "Ingresa tu PIN de seguridad o toca el ícono de huella/rostro",
            autoTriggerBiometric = false,
            isBiometricEnabled = isBiometricEnabled,
            onConfirm = { _ ->
                showPinDialog = false
                onUnlocked()
            },
            onUnlockSuccess = {
                showPinDialog = false
                onUnlocked()
            },
            onDismiss = {
                showPinDialog = false
            }
        )
    }
}

