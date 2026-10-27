package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.data.drive.BackupFrequency
import com.example.data.drive.BackupResult
import com.example.data.drive.BackupSnapshotInfo
import com.example.data.drive.DriveBackupWorker
import com.example.data.drive.DriveRestoreUtil
import com.example.data.drive.GoogleDriveManager
import com.example.data.drive.RestoreMode
import com.example.data.drive.RestoreResult
import com.example.data.drive.triggerRestart
import com.example.ui.components.DriveSyncDialog
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.MicrosoftLogoIcon
import com.example.ui.components.MicrosoftSignInButton
import com.example.ui.components.IconColorPaletteDialog
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.SyncConflictResolutionDialog
import com.example.ui.components.WhatsAppStyleBackupCard
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.getNoteJsAccentColorName
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.utils.BiometricAuthManager
import com.example.util.GoogleCalendarAccountInfo
import com.example.util.GoogleCalendarSyncManager
import com.example.util.ThemeMode
import com.example.util.PaperThemeMode
import com.example.viewmodel.NotesViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSigningIn by viewModel.isSigningIn.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val anonymousNotesCount by viewModel.anonymousNotesCount.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isManualSyncing by viewModel.isManualSyncing.collectAsStateWithLifecycle()
    val isGoogleCalendarSyncEnabled by viewModel.isGoogleCalendarSyncEnabled.collectAsStateWithLifecycle()
    val isGoogleCalendarSyncing by viewModel.isGoogleCalendarSyncing.collectAsStateWithLifecycle()
    val lastGoogleCalendarSyncTime by viewModel.lastGoogleCalendarSyncTime.collectAsStateWithLifecycle()
    val googleCalendarAccountLabel by viewModel.googleCalendarAccountLabel.collectAsStateWithLifecycle()
    val syncRemindersToGoogleCalendar by viewModel.syncRemindersToGoogleCalendar.collectAsStateWithLifecycle()
    val importGoogleCalendarEvents by viewModel.importGoogleCalendarEvents.collectAsStateWithLifecycle()

    // Estados de cuenta y sincronización de Microsoft Graph API
    val microsoftAccount by viewModel.microsoftAccount.collectAsStateWithLifecycle()
    val isMicrosoftCalendarSyncEnabled by viewModel.isMicrosoftCalendarSyncEnabled.collectAsStateWithLifecycle()
    val isMicrosoftCalendarSyncing by viewModel.isMicrosoftCalendarSyncing.collectAsStateWithLifecycle()
    val lastMicrosoftCalendarSyncTime by viewModel.lastMicrosoftCalendarSyncTime.collectAsStateWithLifecycle()
    val microsoftCalendarAccountLabel by viewModel.microsoftCalendarAccountLabel.collectAsStateWithLifecycle()
    val syncRemindersToMicrosoftCalendar by viewModel.syncRemindersToMicrosoftCalendar.collectAsStateWithLifecycle()
    val importMicrosoftCalendarEvents by viewModel.importMicrosoftCalendarEvents.collectAsStateWithLifecycle()
    val pendingSyncConflicts by viewModel.pendingSyncConflicts.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

    LaunchedEffect(Unit) {
        viewModel.refreshGoogleCalendarState()
        viewModel.refreshMicrosoftCalendarState()
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.READ_CALENDAR] == true &&
            permissions[android.Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            viewModel.setGoogleCalendarSyncEnabled(true) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Se requieren permisos de Calendario para sincronizar con Google Calendar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val driveManager = remember { GoogleDriveManager(context) }
    var isDriveConnected by remember {
        mutableStateOf(
            try {
                driveManager.getSignedInAccount() != null
            } catch (_: Exception) {
                false
            }
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }
    var isFirebaseConnected by remember(currentUser) {
        mutableStateOf(
            currentUser != null || try {
                FirebaseAuth.getInstance().currentUser != null
            } catch (_: Exception) {
                false
            }
        )
    }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Permisos concedidos. Por favor, intenta de nuevo.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permisos denegados para Note JS Cloud", Toast.LENGTH_SHORT).show()
        }
    }

    val googleDriveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    isDriveConnected = true
                    DriveBackupWorker.schedule(context)
                    Toast.makeText(
                        context,
                        "Copia de seguridad en Google Drive conectada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Error API de Note JS: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
                Toast.makeText(context, "Operación cancelada", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Toast.makeText(
                    context,
                    "Fallo de conexión. Código de error: ${e.statusCode}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val sortSheetState = rememberModalBottomSheetState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaperThemeDialog by remember { mutableStateOf(false) }
    var showIconColorDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmEmptyTrashDialog by remember { mutableStateOf(false) }
    var showDriveOptions by remember { mutableStateOf(false) }
    var showManualDriveSyncDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var isUploadingBackup by remember { mutableStateOf(false) }
    var isRestoringBackup by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf("") }

    var backupFrequency by remember { mutableStateOf(DriveBackupWorker.getBackupFrequency(context)) }
    var scheduledBackupHour by remember { mutableStateOf(DriveBackupWorker.getScheduledHour(context)) }
    var scheduledBackupMinute by remember { mutableStateOf(DriveBackupWorker.getScheduledMinute(context)) }
    var backupWifiOnly by remember { mutableStateOf(DriveBackupWorker.isWifiOnly(context)) }
    var backupIncludePdfs by remember { mutableStateOf(DriveBackupWorker.isIncludePdfs(context)) }
    var lastLocalBackupTime by remember { mutableStateOf(DriveBackupWorker.getLastLocalBackupTime(context)) }
    var lastCloudBackupTime by remember { mutableStateOf(DriveBackupWorker.getLastCloudBackupTime(context)) }
    var lastBackupSize by remember { mutableStateOf(DriveBackupWorker.getLastBackupSize(context)) }
    val initialCounts = remember { DriveBackupWorker.getLastBackupCounts(context) }
    var lastBackupNotesCount by remember { mutableStateOf(initialCounts.first) }
    var lastBackupRemindersCount by remember { mutableStateOf(initialCounts.second) }
    var backupHistoryList by remember { mutableStateOf(DriveBackupWorker.loadLocalBackupHistory(context)) }

    fun refreshBackupState() {
        backupFrequency = DriveBackupWorker.getBackupFrequency(context)
        scheduledBackupHour = DriveBackupWorker.getScheduledHour(context)
        scheduledBackupMinute = DriveBackupWorker.getScheduledMinute(context)
        backupWifiOnly = DriveBackupWorker.isWifiOnly(context)
        backupIncludePdfs = DriveBackupWorker.isIncludePdfs(context)
        lastLocalBackupTime = DriveBackupWorker.getLastLocalBackupTime(context)
        lastCloudBackupTime = DriveBackupWorker.getLastCloudBackupTime(context)
        lastBackupSize = DriveBackupWorker.getLastBackupSize(context)
        val counts = DriveBackupWorker.getLastBackupCounts(context)
        lastBackupNotesCount = if (counts.first > 0) counts.first else (uiState.activeNotes.size + uiState.archivedNotes.size)
        lastBackupRemindersCount = if (counts.second > 0) counts.second else uiState.remindersCount
        coroutineScope.launch {
            val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
            val driveService = account?.let {
                try { driveManager.getDriveService(it) } catch (_: Exception) { null }
            }
            backupHistoryList = DriveRestoreUtil.fetchAllAvailableBackups(context, driveService)
        }
    }

    LaunchedEffect(isFirebaseConnected, isDriveConnected, uiState.allActiveNotesCount, uiState.remindersCount) {
        if (isFirebaseConnected || isDriveConnected) {
            DriveBackupWorker.scheduleWithPreferences(context)
            refreshBackupState()
        } else {
            DriveBackupWorker.cancel(context)
        }
    }

    var newPinInput by remember(uiState.defaultLockPin) { mutableStateOf(uiState.defaultLockPin) }
    var isPinVisible by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("settings_btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                            contentDescription = "Volver",
                            tint = amber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // SECCIÓN: CUENTA DE NOTE JS
            item {
                SettingsSectionHeader("CUENTA DE NOTE JS")
                SettingsGroupCard {
                    val isGoogleConnected = isFirebaseConnected || isDriveConnected
                    val isMsConnected = microsoftAccount != null && !microsoftAccount?.accessToken.isNullOrBlank()

                    if (!isGoogleConnected && !isMsConnected) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconBadge(
                                    icon = Icons.Default.AccountCircle,
                                    badgeColor = Color(0xFF34A853)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Sesión Anónima Activa",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 16.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF34A853).copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Offline-First",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color(0xFF34A853),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (anonymousNotesCount > 0)
                                            "$anonymousNotesCount notas en sesión anónima listas para vincular"
                                        else
                                            "Acceso inmediato sin registro • Guardado seguro en Room",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Puedes crear y editar notas sin conexión ni cuenta. Vincula tu cuenta de Google o Microsoft para respaldar en Google Drive / OneDrive y sincronizar tu calendario de Google y Outlook.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            GoogleSignInButton(
                                authManager = authManager,
                                isLoading = isSigningIn,
                                text = "Vincular cuenta de Google",
                                onSuccess = { user ->
                                    isFirebaseConnected = true
                                    viewModel.repository.startListeningToCloud(user.uid)
                                    val displayName = user.displayName ?: user.email ?: "Usuario"
                                    Toast.makeText(
                                        context,
                                        "Cuenta vinculada como $displayName. Sincronizando notas...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onCancelled = {
                                    Toast.makeText(context, "Vinculación cancelada. Tus notas siguen seguras en tu sesión anónima.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_sign_in_google_unified")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            MicrosoftSignInButton(
                                authManager = viewModel.microsoftAuthManager,
                                text = "Vincular cuenta de Microsoft (Outlook / 365)",
                                onSuccess = { msUser ->
                                    Toast.makeText(
                                        context,
                                        "Microsoft conectado: ${msUser.summaryLabel}. Sincronizando...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    viewModel.refreshMicrosoftCalendarState()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onCancelled = {
                                    Toast.makeText(context, "Vinculación de Microsoft cancelada.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_sign_in_microsoft_unified")
                            )
                        }
                    } else {
                        val fbUser = currentUser ?: try {
                            FirebaseAuth.getInstance().currentUser
                        } catch (_: Exception) {
                            null
                        }
                        val googleAccount = driveManager.getSignedInAccount()
                        val displayName = fbUser?.displayName?.ifBlank { null }
                            ?: googleAccount?.displayName?.ifBlank { null }
                            ?: "Usuario"
                        val email = fbUser?.email?.ifBlank { null }
                            ?: googleAccount?.email?.ifBlank { null }
                            ?: "Sin correo electrónico"
                        val photoUrl = fbUser?.photoUrl ?: googleAccount?.photoUrl

                        val statusBadgeText = when {
                            syncState == com.example.model.SyncState.SYNCING -> "Sincronizando"
                            pendingSyncCount > 0 -> "Pendientes: $pendingSyncCount"
                            syncState == com.example.model.SyncState.OFFLINE -> "Offline"
                            else -> "Sincronizado"
                        }
                        val statusBadgeColor = when {
                            syncState == com.example.model.SyncState.SYNCING -> Color(0xFF4285F4)
                            pendingSyncCount > 0 || syncState == com.example.model.SyncState.OFFLINE -> Color(0xFFFF9500)
                            else -> Color(0xFF34A853)
                        }

                        Column {
                            // Fila de estado de la cuenta de Google
                            if (isGoogleConnected) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        if (photoUrl != null) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Foto de perfil",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Foto de perfil",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(statusBadgeColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Google",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = statusBadgeColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = email,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.signOutAccount {
                                                DriveBackupWorker.cancel(context)
                                                try {
                                                    driveManager.getGoogleSignInClient().signOut().addOnCompleteListener {
                                                        isDriveConnected = false
                                                        isFirebaseConnected = false
                                                        Toast.makeText(context, "Cuenta de Google desvinculada", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (_: Exception) {
                                                    isDriveConnected = false
                                                    isFirebaseConnected = false
                                                    Toast.makeText(context, "Cuenta de Google desvinculada", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_disconnect_google_drive")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Desvincular cuenta",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Fila de estado de la cuenta de Microsoft (Outlook / Office 365)
                            if (isMsConnected) {
                                if (isGoogleConnected) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 60.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF0078D4).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MicrosoftLogoIcon(modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = microsoftAccount?.displayName?.ifBlank { "Usuario Microsoft" } ?: "Usuario Microsoft",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFF0078D4).copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Microsoft 365",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = Color(0xFF0078D4),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = microsoftAccount?.email?.ifBlank { "Outlook & OneDrive" } ?: "Outlook & OneDrive",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.signOutMicrosoft {
                                                Toast.makeText(context, "Cuenta de Microsoft desvinculada", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_disconnect_microsoft")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Desvincular Microsoft",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Botón secundario para vincular el otro proveedor
                            if (isGoogleConnected && !isMsConnected) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 60.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    MicrosoftSignInButton(
                                        authManager = viewModel.microsoftAuthManager,
                                        text = "+ Vincular cuenta de Microsoft (Outlook / 365)",
                                        onSuccess = {
                                            Toast.makeText(context, "Microsoft vinculado con éxito", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            } else if (isMsConnected && !isGoogleConnected) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 60.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    GoogleSignInButton(
                                        authManager = authManager,
                                        isLoading = isSigningIn,
                                        text = "+ Vincular cuenta de Google",
                                        onSuccess = { user ->
                                            isFirebaseConnected = true
                                            viewModel.repository.startListeningToCloud(user.uid)
                                            Toast.makeText(context, "Cuenta de Google vinculada", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Panel unificado de Sincronización Instantánea en la Nube
                            val isSyncingNow = isManualSyncing || syncState == com.example.model.SyncState.SYNCING
                            SettingsRow(
                                icon = Icons.Default.CloudSync,
                                badgeColor = statusBadgeColor,
                                title = "Sincronización Instantánea en la Nube",
                                subtitle = when {
                                    isSyncingNow -> "Sincronizando cambios entre dispositivos..."
                                    pendingSyncCount > 0 -> "$pendingSyncCount cambios locales pendientes • Toca para sincronizar"
                                    syncState == com.example.model.SyncState.OFFLINE -> "Sin conexión • Se sincronizará automáticamente al conectar"
                                    else -> "Tus notas se sincronizan al instante en todos tus dispositivos"
                                },
                                valueText = if (isSyncingNow) "Sincronizando..." else "Sincronizar",
                                onClick = {
                                    viewModel.syncPendingNotesNow { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                testTag = "btn_sync_firestore_now"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Menú desplegable unificado de Copia de Seguridad (visible únicamente al iniciar sesión en Google)
                            WhatsAppStyleBackupCard(
                                accentColor = amber,
                                isDark = isDark,
                                backupFrequency = backupFrequency,
                                scheduledHour = scheduledBackupHour,
                                scheduledMinute = scheduledBackupMinute,
                                wifiOnly = backupWifiOnly,
                                includePdfs = backupIncludePdfs,
                                lastLocalBackupTime = lastLocalBackupTime,
                                lastCloudBackupTime = lastCloudBackupTime,
                                lastBackupSize = lastBackupSize,
                                lastBackupNotesCount = lastBackupNotesCount,
                                lastBackupRemindersCount = lastBackupRemindersCount,
                                accountEmail = email,
                                isBackingUp = isUploadingBackup,
                                isRestoring = isRestoringBackup,
                                backupStatusText = backupStatusText,
                                backupHistory = backupHistoryList,
                                onBackupNowClick = {
                                    isUploadingBackup = true
                                    backupStatusText = "Comprimiendo notas, recordatorios y subiendo copia..."
                                    coroutineScope.launch {
                                        val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
                                        val driveService = account?.let {
                                            try { driveManager.getDriveService(it) } catch (_: Exception) { null }
                                        }
                                        val result = DriveBackupWorker.performBackup(
                                            context = context,
                                            driveService = driveService,
                                            dbName = DriveBackupWorker.DB_NAME,
                                            triggerType = "MANUAL"
                                        )
                                        if (isFirebaseConnected) {
                                            viewModel.syncPendingNotesNow()
                                        }
                                        refreshBackupState()
                                        isUploadingBackup = false
                                        backupStatusText = ""
                                        when (result) {
                                            is BackupResult.Success -> {
                                                Toast.makeText(
                                                    context,
                                                    "Copia de seguridad guardada exitosamente",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            is BackupResult.AuthRequired -> {
                                                authLauncher.launch(result.intent)
                                            }
                                            is BackupResult.Error -> {
                                                Toast.makeText(
                                                    context,
                                                    result.message,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                onFrequencySelected = { freq ->
                                    DriveBackupWorker.setBackupFrequency(context, freq)
                                    backupFrequency = freq
                                    val msg = when (freq) {
                                        BackupFrequency.DAILY -> "Copia automática programada diariamente"
                                        BackupFrequency.WEEKLY -> "Copia automática programada semanalmente"
                                        BackupFrequency.MONTHLY -> "Copia automática programada una vez al mes"
                                        BackupFrequency.MANUAL_ONLY -> "Copia configurada solo en modo manual"
                                        BackupFrequency.NEVER -> "Copias automáticas desactivadas"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onScheduledTimeSelected = { hour, minute ->
                                    DriveBackupWorker.setScheduledTime(context, hour, minute)
                                    scheduledBackupHour = hour
                                    scheduledBackupMinute = minute
                                    val timeFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
                                    Toast.makeText(
                                        context,
                                        "Horario de copia automática fijado a las $timeFormatted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onWifiOnlyChanged = { wifiOnly ->
                                    DriveBackupWorker.setWifiOnly(context, wifiOnly)
                                    backupWifiOnly = wifiOnly
                                },
                                onIncludePdfsChanged = { include ->
                                    DriveBackupWorker.setIncludePdfs(context, include)
                                    backupIncludePdfs = include
                                },
                                onRestoreSnapshot = { snapshot, mode ->
                                    isRestoringBackup = true
                                    backupStatusText = "Restaurando copia del historial..."
                                    coroutineScope.launch {
                                        val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
                                        val driveService = account?.let {
                                            try { driveManager.getDriveService(it) } catch (_: Exception) { null }
                                        }
                                        val result = DriveRestoreUtil.restoreSpecificBackup(
                                            context = context,
                                            snapshot = snapshot,
                                            mode = mode,
                                            driveService = driveService,
                                            dbName = DriveBackupWorker.DB_NAME
                                        )
                                        isRestoringBackup = false
                                        backupStatusText = ""
                                        when (result) {
                                            is RestoreResult.Success -> {
                                                if (mode == RestoreMode.REPLACE_ALL) {
                                                    Toast.makeText(
                                                        context,
                                                        "Base de datos restaurada. Reiniciando...",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    context.triggerRestart()
                                                } else {
                                                    if (isFirebaseConnected) {
                                                        viewModel.syncPendingNotesNow()
                                                    }
                                                    refreshBackupState()
                                                    Toast.makeText(
                                                        context,
                                                        "Copia restaurada y combinada exitosamente (${snapshot.notesCount} notas, ${snapshot.remindersCount} recordatorios)",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                            is RestoreResult.NotFound -> {
                                                Toast.makeText(
                                                    context,
                                                    "No se encontró el archivo de esta copia de seguridad",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            is RestoreResult.AuthRequired -> {
                                                authLauncher.launch(result.intent)
                                            }
                                            is RestoreResult.Error -> {
                                                Toast.makeText(
                                                    context,
                                                    result.message,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                onDeleteSnapshot = { snapshot ->
                                    coroutineScope.launch {
                                        val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
                                        val driveService = account?.let {
                                            try { driveManager.getDriveService(it) } catch (_: Exception) { null }
                                        }
                                        val deleted = DriveRestoreUtil.deleteBackupSnapshot(context, snapshot, driveService)
                                        refreshBackupState()
                                        if (deleted) {
                                            Toast.makeText(
                                                context,
                                                "Copia anterior eliminada",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onExportJsonClick = {
                                    shareBackupJson(context, viewModel)
                                },
                                onOpenDriveSyncClick = {
                                    showManualDriveSyncDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // SECCIÓN: PERSONALIZACIÓN
            item {
                SettingsSectionHeader("PERSONALIZACIÓN")
                SettingsGroupCard {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        badgeColor = Color(0xFFAF52DE),
                        title = "Tema de la aplicación",
                        valueText = uiState.themeMode.title,
                        onClick = { showThemeDialog = true },
                        testTag = "settings_row_theme"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.Description,
                        badgeColor = Color(0xFF34C759),
                        title = "Color de la hoja",
                        valueText = uiState.paperThemeMode.title,
                        onClick = { showPaperThemeDialog = true },
                        testTag = "settings_row_paper_theme"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.Palette,
                        badgeColor = amber,
                        title = "Color de los iconos",
                        subtitle = "Cambia el color de los iconos en toda la app",
                        valueText = getNoteJsAccentColorName(uiState.accentColorHex),
                        onClick = { showIconColorDialog = true },
                        testTag = "settings_row_icon_color"
                    )
                }
            }

            // SECCIÓN: NOTAS Y FORMATO
            item {
                SettingsSectionHeader("NOTAS Y FORMATO")
                SettingsGroupCard {
                    val paperName = when (uiState.defaultPaperStyle) {
                        "RULED" -> "Rayado"
                        "GRID" -> "Cuadrícula"
                        "DOTS" -> "Puntos"
                        else -> "En blanco"
                    }

                    SettingsSwitchRow(
                        icon = Icons.Default.GridView,
                        badgeColor = amber,
                        title = "Vista en cuadrícula por defecto",
                        subtitle = if (uiState.isGridView) "Galería (Cuadrícula)" else "Lista compacta",
                        checked = uiState.isGridView,
                        onCheckedChange = { viewModel.setGridViewDefault(it) },
                        testTag = "settings_switch_grid"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.Article,
                        badgeColor = Color(0xFFFF9500),
                        title = "Estilo de papel por defecto",
                        valueText = paperName,
                        onClick = { showPaperDialog = true },
                        testTag = "settings_row_paper"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        badgeColor = Color(0xFF00C7BE),
                        title = "Ordenación por defecto",
                        valueText = uiState.sortOption.title,
                        onClick = { showSortDialog = true },
                        testTag = "settings_row_sort"
                    )
                }
            }

            // SECCIÓN: RECORDATORIOS Y CALENDARIO
            item {
                SettingsSectionHeader("RECORDATORIOS Y CALENDARIO")
                SettingsGroupCard {
                    var showWorkdaysDialog by remember { mutableStateOf(false) }
                    var showGoogleCalendarPickerDialog by remember { mutableStateOf(false) }

                    SettingsSwitchRow(
                        icon = Icons.Default.EventAvailable,
                        badgeColor = Color(0xFF4285F4),
                        title = "Sincronizar con Google Calendar",
                        subtitle = if (isGoogleCalendarSyncEnabled) {
                            "Vinculado con ${googleCalendarAccountLabel.ifBlank { "Google Calendar" }}"
                        } else {
                            "Sincroniza tus recordatorios y eventos con Google Calendar"
                        },
                        checked = isGoogleCalendarSyncEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (GoogleCalendarSyncManager.hasCalendarPermissions(context)) {
                                    viewModel.setGoogleCalendarSyncEnabled(true) { _, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    calendarPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.READ_CALENDAR,
                                            android.Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                }
                            } else {
                                viewModel.setGoogleCalendarSyncEnabled(false) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        testTag = "settings_switch_google_calendar_sync"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    if (isGoogleCalendarSyncEnabled) {
                        SettingsRow(
                            icon = Icons.Default.AccountCircle,
                            badgeColor = Color(0xFF34A853),
                            title = "Calendario de Google vinculado",
                            subtitle = "Toca para elegir la cuenta o calendario de destino",
                            valueText = googleCalendarAccountLabel.ifBlank { "Principal" },
                            onClick = { showGoogleCalendarPickerDialog = true },
                            testTag = "settings_row_google_calendar_account"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.NotificationsActive,
                            badgeColor = Color(0xFFFF9500),
                            title = "Sincronizar recordatorios",
                            subtitle = "Incluir tus recordatorios además de los eventos en Google Calendar",
                            checked = syncRemindersToGoogleCalendar,
                            onCheckedChange = { viewModel.setSyncRemindersToGoogleCalendar(it) },
                            testTag = "settings_switch_gcal_reminders"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.EventRepeat,
                            badgeColor = Color(0xFF5856D6),
                            title = "Importar eventos de Google Calendar",
                            subtitle = "Mostrar los eventos de tu Google Calendar dentro de Note JS",
                            checked = importGoogleCalendarEvents,
                            onCheckedChange = { viewModel.setImportGoogleCalendarEvents(it) },
                            testTag = "settings_switch_gcal_import"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        val lastGCalSyncFormatted = remember(lastGoogleCalendarSyncTime) {
                            if (lastGoogleCalendarSyncTime <= 0L) {
                                "Listo para sincronizar"
                            } else {
                                val sdf = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("es", "ES"))
                                "Última sincronización: ${sdf.format(java.util.Date(lastGoogleCalendarSyncTime))}"
                            }
                        }

                        SettingsRow(
                            icon = Icons.Default.Sync,
                            badgeColor = Color(0xFF4285F4),
                            title = "Sincronizar con Google Calendar ahora",
                            subtitle = if (isGoogleCalendarSyncing) {
                                "Sincronizando eventos y recordatorios..."
                            } else {
                                lastGCalSyncFormatted
                            },
                            valueText = if (isGoogleCalendarSyncing) "Sincronizando..." else "Sincronizar",
                            onClick = {
                                viewModel.syncWithGoogleCalendarNow { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            testTag = "btn_sync_google_calendar_now"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // --- SECCIÓN: MICROSOFT OUTLOOK / OFFICE 365 CALENDAR ---
                    SettingsSwitchRow(
                        icon = Icons.Default.EventAvailable,
                        badgeColor = Color(0xFF0078D4),
                        title = "Sincronizar con Microsoft Outlook",
                        subtitle = if (isMicrosoftCalendarSyncEnabled) {
                            "Vinculado con ${microsoftCalendarAccountLabel.ifBlank { "Outlook / Office 365" }}"
                        } else {
                            "Sincroniza tus recordatorios y eventos con Outlook y Microsoft 365"
                        },
                        checked = isMicrosoftCalendarSyncEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setMicrosoftCalendarSyncEnabled(enabled) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        testTag = "settings_switch_microsoft_calendar_sync"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    if (isMicrosoftCalendarSyncEnabled) {
                        SettingsRow(
                            icon = Icons.Default.AccountCircle,
                            badgeColor = Color(0xFF0078D4),
                            title = "Cuenta de Outlook vinculada",
                            subtitle = "Eventos sincronizados mediante Microsoft Graph API",
                            valueText = microsoftCalendarAccountLabel.ifBlank { "Microsoft 365" },
                            onClick = {
                                Toast.makeText(context, "Sincronizado con: $microsoftCalendarAccountLabel", Toast.LENGTH_SHORT).show()
                            },
                            testTag = "settings_row_ms_calendar_account"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.NotificationsActive,
                            badgeColor = Color(0xFFFF9500),
                            title = "Sincronizar recordatorios con Outlook",
                            subtitle = "Incluir tus recordatorios además de los eventos en el calendario",
                            checked = syncRemindersToMicrosoftCalendar,
                            onCheckedChange = { viewModel.setSyncRemindersToMicrosoftCalendar(it) },
                            testTag = "settings_switch_ms_reminders"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.EventRepeat,
                            badgeColor = Color(0xFF0078D4),
                            title = "Importar eventos de Outlook Calendar",
                            subtitle = "Mostrar los eventos de tu calendario de Microsoft dentro de Note JS",
                            checked = importMicrosoftCalendarEvents,
                            onCheckedChange = { viewModel.setImportMicrosoftCalendarEvents(it) },
                            testTag = "settings_switch_ms_import"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        val lastMsSyncFormatted = remember(lastMicrosoftCalendarSyncTime) {
                            if (lastMicrosoftCalendarSyncTime <= 0L) {
                                "Listo para sincronizar"
                            } else {
                                val sdf = java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("es", "ES"))
                                "Última sincronización: ${sdf.format(java.util.Date(lastMicrosoftCalendarSyncTime))}"
                            }
                        }

                        SettingsRow(
                            icon = Icons.Default.Sync,
                            badgeColor = Color(0xFF0078D4),
                            title = "Sincronizar con Outlook Calendar ahora",
                            subtitle = if (isMicrosoftCalendarSyncing) {
                                "Sincronizando eventos y recordatorios con Microsoft..."
                            } else {
                                lastMsSyncFormatted
                            },
                            valueText = if (isMicrosoftCalendarSyncing) "Sincronizando..." else "Sincronizar",
                            onClick = {
                                viewModel.syncWithMicrosoftCalendarNow { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            testTag = "btn_sync_microsoft_calendar_now"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    val workdaysLabel = remember(uiState.workdays) {
                        if (uiState.workdays.isEmpty()) "Ninguno"
                        else if (uiState.workdays == setOf(1, 2, 3, 4, 5)) "Lunes a Viernes"
                        else if (uiState.workdays == setOf(1, 2, 3, 4, 5, 6)) "Lunes a Sábado"
                        else if (uiState.workdays == setOf(6, 7)) "Fines de semana"
                        else if (uiState.workdays.size == 7) "Todos los días"
                        else "${uiState.workdays.size} días"
                    }

                    SettingsRow(
                        icon = Icons.Default.CalendarMonth,
                        badgeColor = Color(0xFF007AFF),
                        title = "Días laborales",
                        subtitle = "Usados para repetición de recordatorios",
                        valueText = workdaysLabel,
                        onClick = { showWorkdaysDialog = true },
                        testTag = "settings_row_workdays"
                    )

                    if (showGoogleCalendarPickerDialog) {
                        val availableCalendars = remember {
                            viewModel.getAvailableGoogleCalendars()
                        }
                        AlertDialog(
                            onDismissRequest = { showGoogleCalendarPickerDialog = false },
                            title = { Text("Seleccionar Google Calendar") },
                            text = {
                                if (availableCalendars.isEmpty()) {
                                    Text(
                                        text = "No se encontraron calendarios adicionales. Tus eventos y recordatorios se sincronizarán con el calendario principal del dispositivo.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        availableCalendars.forEach { calInfo ->
                                            val isSelected = googleCalendarAccountLabel == calInfo.summaryLabel ||
                                                googleCalendarAccountLabel == calInfo.displayName
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) amber.copy(alpha = 0.16f) else Color.Transparent,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.selectGoogleCalendarAccount(calInfo) { _, msg ->
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                        showGoogleCalendarPickerDialog = false
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = null,
                                                        colors = RadioButtonDefaults.colors(selectedColor = amber)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = calInfo.displayName,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = FontWeight.SemiBold
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (calInfo.accountName.isNotBlank()) {
                                                            Text(
                                                                text = calInfo.accountName,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showGoogleCalendarPickerDialog = false }) {
                                    Text("Cerrar")
                                }
                            }
                        )
                    }

                    if (showWorkdaysDialog) {
                        var tempWorkdays by remember { mutableStateOf(uiState.workdays) }
                        val daysMap = listOf(
                            1 to "Lunes", 2 to "Martes", 3 to "Miércoles", 
                            4 to "Jueves", 5 to "Viernes", 6 to "Sábado", 7 to "Domingo"
                        )
                        
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showWorkdaysDialog = false },
                            title = { Text("Días laborales") },
                            text = {
                                Column {
                                    daysMap.forEach { (dayInt, dayName) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    tempWorkdays = if (tempWorkdays.contains(dayInt)) {
                                                        tempWorkdays - dayInt
                                                    } else {
                                                        tempWorkdays + dayInt
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.Checkbox(
                                                checked = tempWorkdays.contains(dayInt),
                                                onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(dayName)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.setWorkdays(tempWorkdays)
                                        showWorkdaysDialog = false
                                    }
                                ) {
                                    Text("Guardar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showWorkdaysDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }
            }

            // SECCIÓN: SEGURIDAD Y PRIVACIDAD
            item {
                val biometricStatusLabel = remember(context) {
                    BiometricAuthManager.getBiometricStatusDescription(context)
                }
                SettingsSectionHeader("SEGURIDAD Y PRIVACIDAD")
                SettingsGroupCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Fingerprint,
                        badgeColor = Color(0xFF5856D6),
                        title = "Desbloqueo Biométrico (Huella / Rostro)",
                        subtitle = biometricStatusLabel,
                        checked = uiState.isBiometricLockEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setBiometricLockEnabled(enabled)
                        },
                        testTag = "settings_switch_biometric_lock"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsSwitchRow(
                        icon = Icons.Default.Security,
                        badgeColor = Color(0xFF007AFF),
                        title = "Bloquear Note JS al abrir",
                        subtitle = if (uiState.isAppBiometricLockEnabled)
                            "Solicita huella, rostro o PIN al iniciar la app"
                        else
                            "Acceso directo al abrir la aplicación",
                        checked = uiState.isAppBiometricLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && uiState.isBiometricLockEnabled && BiometricAuthManager.isBiometricAvailable(context)) {
                                BiometricAuthManager.authenticateWithBiometrics(
                                    context = context,
                                    title = "Activar Bloqueo de Aplicación",
                                    subtitle = "Confirma tu huella dactilar o reconocimiento facial",
                                    onSuccess = {
                                        viewModel.setAppBiometricLockEnabled(true)
                                    },
                                    onFallbackToPin = {
                                        viewModel.setAppBiometricLockEnabled(true)
                                    },
                                    onError = { _ -> }
                                )
                            } else {
                                viewModel.setAppBiometricLockEnabled(enabled)
                            }
                        },
                        testTag = "settings_switch_app_biometric_lock"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.Lock,
                        badgeColor = Color(0xFF34C759),
                        title = "Verificar BiometricPrompt ahora",
                        valueText = "Huella / Rostro",
                        subtitle = "Probar autenticación biométrica en este dispositivo",
                        onClick = {
                            BiometricAuthManager.authenticateWithBiometrics(
                                context = context,
                                title = "Verificación Biométrica",
                                subtitle = "Escanea tu huella dactilar o rostro para verificar BiometricPrompt",
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Autenticación biométrica verificada correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFallbackToPin = {
                                    showPinDialog = true
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        testTag = "settings_row_test_biometric"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.Key,
                        badgeColor = Color(0xFFFF9500),
                        title = "PIN de Seguridad de Respaldo",
                        valueText = "Configurar",
                        subtitle = "Usado para desbloquear notas si cancelas la huella o rostro",
                        onClick = {
                            showPinDialog = true
                        },
                        testTag = "settings_row_pin"
                    )
                }
            }

            // SECCIÓN: GESTIÓN DE DATOS
            item {
                SettingsSectionHeader("GESTIÓN DE DATOS")
                SettingsGroupCard {
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        badgeColor = Color(0xFFFF3B30),
                        title = "Vaciar papelera de reciclaje",
                        subtitle = "${uiState.trashedCount} notas eliminadas",
                        textColor = if (uiState.trashedCount > 0) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface,
                        onClick = { showConfirmEmptyTrashDialog = true },
                        testTag = "settings_row_empty_trash"
                    )
                }
            }

            // SECCIÓN: ACERCA DE
            item {
                SettingsSectionHeader("ACERCA DE")
                SettingsGroupCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (isDark) R.drawable.ic_launcher_dark_background else R.drawable.ic_launcher_background
                                ),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Image(
                                painter = painterResource(
                                    id = if (isDark) R.drawable.ic_launcher_dark_foreground else R.drawable.ic_launcher_foreground
                                ),
                                contentDescription = "Logo de Note JS",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(1.5f),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Note JS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Versión 1.7.10",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Hecho con amor❤️ y mate🧉",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // DIÁLOGO: TEMA
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Tema de la aplicación",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = uiState.themeMode == mode
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Default.Palette
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                        }

                        Surface(
                            onClick = {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) amber.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isSelected) amber else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = amber
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showThemeDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO: COLOR DE LA HOJA
    if (showPaperThemeDialog) {
        AlertDialog(
            onDismissRequest = { showPaperThemeDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Color de la hoja",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaperThemeMode.entries.forEach { mode ->
                        val isSelected = uiState.paperThemeMode == mode
                        Surface(
                            onClick = {
                                viewModel.setPaperThemeMode(mode)
                                showPaperThemeDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) amber.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isSelected) amber else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setPaperThemeMode(mode)
                                        showPaperThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = amber
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showPaperThemeDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO: PALETA DE COLORES DE ICONOS
    if (showIconColorDialog) {
        IconColorPaletteDialog(
            currentHex = uiState.accentColorHex,
            isDark = isDark,
            onColorSelected = { hex ->
                viewModel.setAccentColorHex(hex)
            },
            onResetDefault = {
                viewModel.resetAccentColorToDefault()
                Toast.makeText(
                    context,
                    "Color predeterminado restaurado (#EAB308)",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showIconColorDialog = false }
        )
    }

    // DIÁLOGO: PAPEL
    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = { showPaperDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Estilo de Papel por Defecto",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val styles = listOf(
                        "BLANK" to "En blanco",
                        "RULED" to "Rayado",
                        "GRID" to "Cuadrícula",
                        "DOTS" to "Puntos"
                    )
                    styles.forEach { (code, name) ->
                        val isSelected = uiState.defaultPaperStyle == code
                        Surface(
                            onClick = {
                                viewModel.setDefaultPaperStyle(code)
                                showPaperDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) amber.copy(alpha = 0.12f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 15.sp
                                    ),
                                    color = if (isSelected) amber else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setDefaultPaperStyle(code)
                                        showPaperDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = amber
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showPaperDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // SHEET: ORDEN
    if (showSortDialog) {
        SortBottomSheet(
            sheetState = sortSheetState,
            currentOption = uiState.sortOption,
            onOptionSelected = { option ->
                viewModel.setDefaultSortOption(option)
            },
            onDismiss = { showSortDialog = false }
        )
    }

    // DIÁLOGO: CAMBIAR PIN
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "PIN por Defecto",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Establece el PIN predeterminado para bloquear y desbloquear notas protegidas:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("PIN de seguridad") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Icon(
                                    imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPinVisible) "Ocultar PIN" else "Mostrar PIN",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = amber,
                            focusedLabelColor = amber
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.isNotBlank()) {
                            viewModel.setDefaultLockPin(newPinInput)
                            showPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = amber, contentColor = Color.Black)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPinDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: CONFIRMAR VACIAR PAPELERA
    if (showConfirmEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmptyTrashDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Vaciar Papelera",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar permanentemente todas las notas en la papelera? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showConfirmEmptyTrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = amber, contentColor = Color.Black)
                ) {
                    Text("Vaciar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmEmptyTrashDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: OPCIONES DE NOTE JS CLOUD
    if (showDriveOptions) {
        AlertDialog(
            onDismissRequest = { showDriveOptions = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Note JS Cloud",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selecciona la acción que deseas realizar con tus copias de seguridad:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            showDriveOptions = false
                            showManualDriveSyncDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_option_sync_notes_pdfs"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34A853),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sincronizar Notas y PDFs",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            showDriveOptions = false
                            showUploadDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_option_upload_backup"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subir copia a Drive",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            showDriveOptions = false
                            showRestoreDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("btn_option_restore_backup"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restaurar desde Drive",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showDriveOptions = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: SINCRONIZACIÓN MANUAL DE NOTAS Y PDFS CON GOOGLE DRIVE
    if (showManualDriveSyncDialog) {
        DriveSyncDialog(
            viewModel = viewModel,
            onDismiss = { showManualDriveSyncDialog = false },
            onAuthRequired = { intent -> authLauncher.launch(intent) }
        )
    }

    // DIÁLOGO: SUBIR COPIA DE SEGURIDAD
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUploadingBackup) showUploadDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Subir Copia de Seguridad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Deseas comprimir y subir una copia de seguridad actualizada de todas tus notas a Note JS Cloud?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isUploadingBackup) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = amber,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Subiendo copia de seguridad...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploadingBackup,
                    onClick = {
                        val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
                        if (account != null || isFirebaseConnected) {
                            isUploadingBackup = true
                            coroutineScope.launch {
                                val driveService = account?.let {
                                    try { driveManager.getDriveService(it) } catch (_: Exception) { null }
                                }
                                val result = DriveBackupWorker.performBackup(
                                    context = context,
                                    driveService = driveService,
                                    dbName = DriveBackupWorker.DB_NAME
                                )
                                viewModel.syncPendingNotesNow()
                                isUploadingBackup = false
                                showUploadDialog = false
                                when (result) {
                                    is BackupResult.Success -> {
                                        Toast.makeText(
                                            context,
                                            "Copia de seguridad subida con éxito a Note JS Cloud",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    is BackupResult.AuthRequired -> {
                                        authLauncher.launch(result.intent)
                                    }
                                    is BackupResult.Error -> {
                                        Toast.makeText(
                                            context,
                                            result.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            showUploadDialog = false
                            Toast.makeText(
                                context,
                                "Error: No se encontró una cuenta vinculada de Note JS Cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = amber, contentColor = Color.Black)
                ) {
                    Text("Subir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isUploadingBackup,
                    onClick = { showUploadDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: RESTAURAR COPIA DE SEGURIDAD
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoringBackup) showRestoreDialog = false },
            modifier = Modifier.border(BorderStroke(1.dp, amber.copy(alpha = 0.3f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Restaurar Copia de Seguridad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Esta acción reemplazará los datos actuales con la copia de seguridad guardada en Note JS Cloud. La aplicación se reiniciará para aplicar los cambios. ¿Deseas continuar?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRestoringBackup) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = amber,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Restaurando copia de seguridad...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isRestoringBackup,
                    onClick = {
                        val account = try { driveManager.getSignedInAccount() } catch (_: Exception) { null }
                        if (account != null || isFirebaseConnected) {
                            isRestoringBackup = true
                            coroutineScope.launch {
                                val driveService = account?.let {
                                    try { driveManager.getDriveService(it) } catch (_: Exception) { null }
                                }
                                val result = DriveRestoreUtil.restoreBackup(
                                    context = context,
                                    driveService = driveService,
                                    dbName = DriveBackupWorker.DB_NAME
                                )
                                when (result) {
                                    is RestoreResult.Success -> {
                                        val firebaseUid = authManager.currentUser?.uid
                                        if (firebaseUid != null) {
                                            viewModel.repository.pullRemoteNotesSuspending(firebaseUid)
                                        }
                                        isRestoringBackup = false
                                        showRestoreDialog = false
                                        Toast.makeText(
                                            context,
                                            "Restauración exitosa. Reiniciando...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        context.triggerRestart()
                                    }
                                    is RestoreResult.NotFound -> {
                                        val firebaseUid = authManager.currentUser?.uid
                                        if (firebaseUid != null) {
                                            val pulledCount = viewModel.repository.pullRemoteNotesSuspending(firebaseUid)
                                            isRestoringBackup = false
                                            showRestoreDialog = false
                                            if (pulledCount > 0) {
                                                Toast.makeText(
                                                    context,
                                                    "Se sincronizaron $pulledCount notas desde Note JS Cloud.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "No se encontró ninguna copia ni notas remotas en Note JS Cloud.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            isRestoringBackup = false
                                            showRestoreDialog = false
                                            Toast.makeText(
                                                context,
                                                "No se encontró ninguna copia de seguridad en Note JS Cloud.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    is RestoreResult.AuthRequired -> {
                                        isRestoringBackup = false
                                        showRestoreDialog = false
                                        authLauncher.launch(result.intent)
                                    }
                                    is RestoreResult.Error -> {
                                        val firebaseUid = authManager.currentUser?.uid
                                        if (firebaseUid != null) {
                                            val pulledCount = viewModel.repository.pullRemoteNotesSuspending(firebaseUid)
                                            isRestoringBackup = false
                                            showRestoreDialog = false
                                            if (pulledCount > 0) {
                                                Toast.makeText(
                                                    context,
                                                    "Se sincronizaron $pulledCount notas desde Note JS Cloud.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                        }
                                        isRestoringBackup = false
                                        showRestoreDialog = false
                                        Toast.makeText(
                                            context,
                                            result.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            showRestoreDialog = false
                            Toast.makeText(
                                context,
                                "Error: No se encontró una cuenta vinculada de Note JS Cloud",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = amber, contentColor = Color.Black)
                ) {
                    Text("Restaurar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isRestoringBackup,
                    onClick = { showRestoreDialog = false },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingSyncConflicts.isNotEmpty()) {
        SyncConflictResolutionDialog(
            conflicts = pendingSyncConflicts,
            accentColor = amber,
            onResolveConflict = { conflict, choice, applyToAll ->
                viewModel.resolveSyncConflict(conflict, choice, applyToAll)
            },
            onDismiss = {
                viewModel.dismissSyncConflicts()
            }
        )
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.6.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    valueText: String = "",
    subtitle: String = "",
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            IconBadge(icon = icon, badgeColor = badgeColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(0.8f, fill = false)
        ) {
            if (valueText.isNotBlank()) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            IconBadge(icon = icon, badgeColor = badgeColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NoteJsAmber,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

private fun shareBackupJson(context: Context, viewModel: NotesViewModel) {
    val activeNotes = viewModel.uiState.value.activeNotes
    val archivedNotes = viewModel.uiState.value.archivedNotes
    val all = activeNotes + archivedNotes

    if (all.isEmpty()) {
        Toast.makeText(context, "No hay notas para exportar", Toast.LENGTH_SHORT).show()
        return
    }

    val jsonString = buildString {
        append("[\n")
        all.forEachIndexed { index, note ->
            append("  {\n")
            append("    \"id\": ${note.id},\n")
            append("    \"title\": \"${note.title.replace("\"", "\\\"")}\",\n")
            append("    \"folder\": \"${note.folder.replace("\"", "\\\"")}\",\n")
            append("    \"content\": \"${note.content.replace("\"", "\\\"").replace("\n", "\\n")}\"\n")
            append("  }${if (index < all.size - 1) "," else ""}\n")
        }
        append("]")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, jsonString)
        type = "application/json"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Exportar copia de seguridad de Note JS"))
}
