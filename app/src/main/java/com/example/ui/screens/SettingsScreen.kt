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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.data.drive.BackupResult
import com.example.data.drive.DriveBackupWorker
import com.example.data.drive.DriveRestoreUtil
import com.example.data.drive.GoogleDriveManager
import com.example.data.drive.RestoreResult
import com.example.data.drive.triggerRestart
import com.example.ui.components.DriveSyncDialog
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.SortBottomSheet
import com.example.ui.theme.NoteJsAmber
import com.example.ui.theme.NoteJsAmberDark
import com.example.ui.theme.isAppInDarkTheme
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
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) NoteJsAmberDark else NoteJsAmber

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

    var newPinInput by remember(uiState.defaultLockPin) { mutableStateOf(uiState.defaultLockPin) }
    var isPinVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isFirebaseConnected, isDriveConnected, pendingSyncCount) {
        if ((isFirebaseConnected || isDriveConnected) && pendingSyncCount > 0) {
            viewModel.syncPendingNotesNow()
        }
    }

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
                    if (!isFirebaseConnected && !isDriveConnected) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconBadge(
                                    icon = Icons.Default.CloudUpload,
                                    badgeColor = Color(0xFF4285F4)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sincronización Offline-First y Respaldo",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (pendingSyncCount > 0)
                                            "$pendingSyncCount notas locales listas para vincular a tu cuenta"
                                        else
                                            "Modo local activo • Tus notas están seguras en Room",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Inicia sesión con Google para activar la sincronización en tiempo real con Firebase Firestore y Cloud Storage, aislando tus notas por cuenta.",
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
                                text = "Continuar con Google",
                                onSuccess = { user ->
                                    isFirebaseConnected = true
                                    viewModel.repository.startListeningToCloud(user.uid)
                                    val displayName = user.displayName ?: user.email ?: "Usuario"
                                    Toast.makeText(context, "Sesión iniciada como $displayName", Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onCancelled = {
                                    Toast.makeText(context, "Inicio de sesión cancelado. Selecciona una cuenta de Google para continuar.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_sign_in_google_unified")
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
                            // Fila de estado de la cuenta / Perfil
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
                                                    text = statusBadgeText,
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
                                            try {
                                                driveManager.getGoogleSignInClient().signOut().addOnCompleteListener {
                                                    isDriveConnected = false
                                                    isFirebaseConnected = false
                                                    Toast.makeText(context, "Cuenta de Note JS desvinculada", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {
                                                isDriveConnected = false
                                                isFirebaseConnected = false
                                                Toast.makeText(context, "Cuenta de Note JS desvinculada", Toast.LENGTH_SHORT).show()
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

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Fila de cola Offline-First (Room <-> Firestore + Cloud Storage)
                            SettingsRow(
                                icon = Icons.Default.CloudSync,
                                badgeColor = statusBadgeColor,
                                title = "Sincronización Cloud (Firestore)",
                                subtitle = if (pendingSyncCount > 0)
                                    "$pendingSyncCount cambios locales pendientes • Toca para sincronizar ahora"
                                else
                                    "Base de datos local Room sincronizada en tiempo real",
                                onClick = {
                                    viewModel.syncPendingNotesNow()
                                },
                                testTag = "btn_sync_firestore_now"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Fila de sincronización manual de Notas y PDFs con Google Drive
                            SettingsRow(
                                icon = Icons.Default.CloudSync,
                                badgeColor = Color(0xFF34A853),
                                title = "Sincronizar Notas y PDFs",
                                subtitle = "Subir notas y PDFs a tu carpeta 'Note JS' en Drive",
                                onClick = { showManualDriveSyncDialog = true },
                                testTag = "btn_manual_drive_sync"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Única fila de opciones de Note JS Cloud
                            SettingsRow(
                                icon = Icons.Default.CloudUpload,
                                badgeColor = Color(0xFF4285F4),
                                title = "Copia de seguridad en Drive",
                                subtitle = "Respaldar o recuperar notas",
                                onClick = { showDriveOptions = true },
                                testTag = "btn_drive_options"
                            )
                        }
                    }
                }
            }

            // SECCIÓN: APARIENCIA
            item {
                SettingsSectionHeader("APARIENCIA")
                SettingsGroupCard {
                    SettingsRow(
                        icon = Icons.Default.Palette,
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

                    SettingsSwitchRow(
                        icon = Icons.Default.GridView,
                        badgeColor = NoteJsAmber,
                        title = "Vista en cuadrícula por defecto",
                        subtitle = if (uiState.isGridView) "Galería (Cuadrícula)" else "Lista compacta",
                        checked = uiState.isGridView,
                        onCheckedChange = { viewModel.setGridViewDefault(it) },
                        testTag = "settings_switch_grid"
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

            // SECCIÓN: SEGURIDAD Y PRIVACIDAD
            item {
                SettingsSectionHeader("SEGURIDAD Y PRIVACIDAD")
                SettingsGroupCard {
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        badgeColor = Color(0xFF5856D6),
                        title = "Carpeta Privada",
                        valueText = "Biometría / PIN",
                        subtitle = "Protegida con la huella o el PIN del sistema",
                        onClick = {
                            Toast.makeText(context, "La Carpeta Privada utiliza la seguridad biométrica o el PIN de tu dispositivo", Toast.LENGTH_SHORT).show()
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
                        icon = Icons.Default.Share,
                        badgeColor = Color(0xFF34C759),
                        title = "Exportar copia de seguridad",
                        subtitle = "Guardar todas las notas en formato JSON",
                        onClick = { shareBackupJson(context, viewModel) },
                        testTag = "settings_row_export"
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsRow(
                        icon = Icons.Default.DeleteSweep,
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
                                .background(amber.copy(alpha = 0.15f))
                                .border(1.dp, amber.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp,
                                    color = amber
                                )
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
                                text = "Versión 1.0.0",
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
