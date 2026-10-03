package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import com.example.data.drive.DriveBackupWorker
import com.example.data.drive.DriveRestoreUtil
import com.example.data.drive.GoogleDriveManager
import com.example.data.drive.triggerRestart
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.ui.components.SortBottomSheet
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import kotlinx.coroutines.launch
import com.example.ui.theme.AppleNotesAmber
import com.example.ui.theme.AppleNotesAmberDark
import com.example.util.ThemeMode
import com.example.viewmodel.NotesViewModel
import com.example.viewmodel.SortOption
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val amber = if (isDark) AppleNotesAmberDark else AppleNotesAmber

    val driveManager = remember { GoogleDriveManager(context) }
    var isDriveConnected by remember { mutableStateOf(driveManager.getSignedInAccount() != null) }
    
    val coroutineScope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }
    var isFirebaseConnected by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

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
                    
                    account.idToken?.let { idToken ->
                        coroutineScope.launch {
                            val uid = authManager.signInWithGoogleIdToken(idToken)
                            if (uid != null) {
                                isFirebaseConnected = true
                                viewModel.repository.startListeningToCloud(uid)
                                Toast.makeText(context, "Sesión iniciada correctamente (Respaldo y Sincronización)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Respaldo activado. Error al sincronizar con Firebase.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } ?: run {
                        Toast.makeText(context, "Respaldo activado. No se pudo iniciar sincronización (token ausente).", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Error API de Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            // Bloque agregado para atrapar fallos silenciosos (RESULT_CANCELED)
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
                Toast.makeText(context, "Operación cancelada", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Toast.makeText(context, "Fallo de conexión. Código de error: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val sortSheetState = rememberModalBottomSheetState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaperDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showConfirmEmptyTrashDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    var newPinInput by remember(uiState.defaultLockPin) { mutableStateOf(uiState.defaultLockPin) }

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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            fontSize = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECCIÓN: CUENTA DE GOOGLE (SINCRONIZACIÓN Y COPIA DE SEGURIDAD)
            item {
                SettingsSectionHeader("CUENTA DE GOOGLE")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!isDriveConnected || !isFirebaseConnected) {
                            Column {
                                Text(
                                    text = "Inicia sesión con Google para sincronizar tus notas en tiempo real y respaldarlas automáticamente en Google Drive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        googleDriveLauncher.launch(driveManager.getGoogleSignInClient().signInIntent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_sign_in_google_unified")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = amber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Iniciar sesión con Google")
                                }
                            }
                        } else {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Sincronización y Respaldo activados",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 15.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = driveManager.getSignedInAccount()?.email ?: "Cuenta vinculada",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            driveManager.getGoogleSignInClient().signOut().addOnCompleteListener {
                                                authManager.signOut()
                                                isDriveConnected = false
                                                isFirebaseConnected = false
                                                Toast.makeText(context, "Cuenta de Google desvinculada", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("btn_disconnect_google_drive")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Desvincular cuenta",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                FilledTonalButton(
                                    onClick = { showRestoreDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_restore_backup")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Restaurar Copia de Seguridad")
                                }
                            }
                        }
                    }
                }
            }

            // SECCIÓN 1: APARIENCIA
            item {
                SettingsSectionHeader("APARIENCIA")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = "Tema de la aplicación",
                            subtitle = uiState.themeMode.title,
                            iconTint = amber,
                            onClick = { showThemeDialog = true },
                            testTag = "settings_row_theme"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsSwitchRow(
                            icon = Icons.Default.Description,
                            title = "Vista en cuadrícula por defecto",
                            subtitle = if (uiState.isGridView) "Galería (Cuadrícula)" else "Lista compacta",
                            checked = uiState.isGridView,
                            iconTint = amber,
                            onCheckedChange = { viewModel.setGridViewDefault(it) },
                            testTag = "settings_switch_grid"
                        )
                    }
                }
            }

            // SECCIÓN 2: NOTAS Y FORMATO
            item {
                SettingsSectionHeader("NOTAS Y FORMATO")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val paperName = when (uiState.defaultPaperStyle) {
                            "RULED" -> "Rayado"
                            "GRID" -> "Cuadrícula"
                            "DOTS" -> "Puntos"
                            else -> "En blanco"
                        }

                        SettingsRow(
                            icon = Icons.Default.Description,
                            title = "Estilo de papel por defecto",
                            subtitle = paperName,
                            iconTint = amber,
                            onClick = { showPaperDialog = true },
                            testTag = "settings_row_paper"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsRow(
                            icon = Icons.AutoMirrored.Filled.Sort,
                            title = "Ordenación por defecto",
                            subtitle = uiState.sortOption.title,
                            iconTint = amber,
                            onClick = { showSortDialog = true },
                            testTag = "settings_row_sort"
                        )
                    }
                }
            }

            // SECCIÓN 3: SEGURIDAD
            item {
                SettingsSectionHeader("SEGURIDAD Y PRIVACIDAD")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = "PIN por defecto de notas protegidas",
                            subtitle = "Actual: ${"•".repeat(uiState.defaultLockPin.length)}",
                            iconTint = amber,
                            onClick = {
                                newPinInput = uiState.defaultLockPin
                                showPinDialog = true
                            },
                            testTag = "settings_row_pin"
                        )
                    }
                }
            }

            // SECCIÓN 5: GESTIÓN DE DATOS
            item {
                SettingsSectionHeader("GESTIÓN DE DATOS")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Share,
                            title = "Exportar copia de seguridad",
                            subtitle = "Guardar todas las notas en formato JSON",
                            iconTint = amber,
                            onClick = {
                                shareBackupJson(context, viewModel)
                            },
                            testTag = "settings_row_export"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Vaciar papelera de reciclaje",
                            subtitle = "${uiState.trashedCount} notas eliminadas",
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showConfirmEmptyTrashDialog = true },
                            testTag = "settings_row_empty_trash"
                        )
                    }
                }
            }

            // SECCIÓN 6: ACERCA DE
            item {
                SettingsSectionHeader("ACERCA DE")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(amber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = amber
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "AuraNotes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Versión 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Diseñado con Jetpack Compose y la experiencia inspirada en Apple Notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
            title = { Text("Seleccionar Tema") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO: PAPEL
    if (showPaperDialog) {
        AlertDialog(
            onDismissRequest = { showPaperDialog = false },
            title = { Text("Estilo de Papel por Defecto") },
            text = {
                Column {
                    val styles = listOf(
                        "BLANK" to "En blanco",
                        "RULED" to "Rayado",
                        "GRID" to "Cuadrícula",
                        "DOTS" to "Puntos"
                    )
                    styles.forEach { (code, name) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultPaperStyle(code)
                                    showPaperDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = uiState.defaultPaperStyle == code,
                                onClick = {
                                    viewModel.setDefaultPaperStyle(code)
                                    showPaperDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaperDialog = false }) {
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
            title = { Text("PIN por Defecto") },
            text = {
                Column {
                    Text("Establece el PIN predeterminado para bloquear notas:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("PIN de seguridad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPinInput.isNotBlank()) {
                        viewModel.setDefaultLockPin(newPinInput)
                        showPinDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: CONFIRMAR VACIAR PAPELERA
    if (showConfirmEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmptyTrashDialog = false },
            title = { Text("Vaciar Papelera") },
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente todas las notas en la papelera?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showConfirmEmptyTrashDialog = false
                }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmptyTrashDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO: RESTAURAR COPIA DE SEGURIDAD
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restaurar Copia de Seguridad") },
            text = {
                Text("Esta acción reemplazará los datos actuales con la copia de seguridad guardada en Google Drive. La aplicación se reiniciará para aplicar los cambios. ¿Deseas continuar?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        val account = driveManager.getSignedInAccount()
                        if (account != null) {
                            Toast.makeText(context, "Iniciando restauración desde Google Drive...", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                val driveService = driveManager.getDriveService(account)
                                val success = DriveRestoreUtil.restoreBackup(
                                    context = context,
                                    driveService = driveService,
                                    dbName = DriveBackupWorker.DB_NAME
                                )
                                if (success) {
                                    Toast.makeText(context, "Restauración exitosa. Reiniciando...", Toast.LENGTH_SHORT).show()
                                    context.triggerRestart()
                                } else {
                                    Toast.makeText(context, "Error al restaurar o no se encontró la copia de seguridad", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Error: No se encontró una cuenta vinculada de Google Drive", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Restaurar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    iconTint: Color,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint
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
    context.startActivity(Intent.createChooser(sendIntent, "Exportar copia de seguridad de AuraNotes"))
}
