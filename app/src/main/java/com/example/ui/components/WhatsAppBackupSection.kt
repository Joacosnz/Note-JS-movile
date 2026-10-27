package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.drive.BackupFrequency
import com.example.data.drive.BackupSnapshotInfo
import com.example.data.drive.RestoreMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val WhatsAppGreen = Color(0xFF25D366)
private val WhatsAppDarkGreen = Color(0xFF128C7E)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatsAppStyleBackupCard(
    accentColor: Color,
    isDark: Boolean,
    backupFrequency: BackupFrequency,
    scheduledHour: Int,
    scheduledMinute: Int,
    wifiOnly: Boolean,
    includePdfs: Boolean,
    lastLocalBackupTime: Long,
    lastCloudBackupTime: Long,
    lastBackupSize: Long,
    lastBackupNotesCount: Int,
    lastBackupRemindersCount: Int,
    accountEmail: String,
    isBackingUp: Boolean,
    isRestoring: Boolean,
    backupStatusText: String,
    backupHistory: List<BackupSnapshotInfo>,
    onBackupNowClick: () -> Unit,
    onFrequencySelected: (BackupFrequency) -> Unit,
    onScheduledTimeSelected: (Int, Int) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit,
    onIncludePdfsChanged: (Boolean) -> Unit,
    onRestoreSnapshot: (BackupSnapshotInfo, RestoreMode) -> Unit,
    onDeleteSnapshot: (BackupSnapshotInfo) -> Unit,
    onExportJsonClick: () -> Unit,
    onOpenDriveSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var showTimeDropdown by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var snapshotToRestore by remember { mutableStateOf<BackupSnapshotInfo?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDropdownExpanded) 180f else 0f,
        label = "backup_dropdown_arrow"
    )

    val formattedLocalTime = remember(lastLocalBackupTime) {
        formatBackupTimestamp(lastLocalBackupTime)
    }
    val formattedCloudTime = remember(lastCloudBackupTime) {
        if (lastCloudBackupTime > 0L) formatBackupTimestamp(lastCloudBackupTime) else "Pendiente de subir"
    }
    val formattedSize = remember(lastBackupSize) {
        formatBytes(lastBackupSize)
    }
    val formattedScheduleTime = remember(scheduledHour, scheduledMinute) {
        String.format(Locale.getDefault(), "%02d:%02d", scheduledHour, scheduledMinute)
    }

    val collapsedSubtitle = remember(
        lastCloudBackupTime,
        lastLocalBackupTime,
        backupFrequency,
        formattedScheduleTime,
        isBackingUp,
        isRestoring
    ) {
        when {
            isBackingUp -> "Subiendo copia de seguridad a Google Drive..."
            isRestoring -> "Restaurando copia de seguridad..."
            lastCloudBackupTime > 0L -> "Última en Drive: $formattedCloudTime • ${backupFrequency.shortLabel}"
            lastLocalBackupTime > 0L -> "Última copia: $formattedLocalTime • ${backupFrequency.shortLabel}"
            backupFrequency == BackupFrequency.DAILY ||
                backupFrequency == BackupFrequency.WEEKLY ||
                backupFrequency == BackupFrequency.MONTHLY ->
                "Automática: ${backupFrequency.label} ($formattedScheduleTime)"
            else -> "Toca para desplegar opciones de respaldo y restauración"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("whatsapp_backup_card")
    ) {
        // Fila principal desplegable (Menú unificado de Copia de Seguridad)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDropdownExpanded = !isDropdownExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("btn_toggle_backup_dropdown"),
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
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = "Copia de seguridad",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Copia de seguridad",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = collapsedSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(WhatsAppGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = backupFrequency.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = WhatsAppGreen
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isDropdownExpanded) "Contraer menú de copia de seguridad" else "Desplegar menú de copia de seguridad",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }

        // Contenido desplegable unificado con todas las opciones de copia de seguridad
        AnimatedVisibility(
            visible = isDropdownExpanded || isBackingUp || isRestoring,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isDark) 0.03f else 0.015f
                        )
                    )
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Bloque superior: Resumen estilo WhatsApp + Botones Guardar ahora y Restaurar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Guarda y restaura tus notas, listas, dibujos, recordatorios y PDFs en Google Drive. Puedes programar copias automáticas diarias, semanales o mensuales y restaurar cualquier copia anterior.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cuadro de resumen de la última copia
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.04f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            BackupInfoStatRow(
                                icon = Icons.Default.CloudDone,
                                label = "Google Drive:",
                                value = formattedCloudTime
                            )
                            BackupInfoStatRow(
                                icon = Icons.Default.Storage,
                                label = "Local:",
                                value = formattedLocalTime
                            )
                            BackupInfoStatRow(
                                icon = Icons.Default.FolderZip,
                                label = "Tamaño:",
                                value = if (lastBackupSize > 0L) {
                                    "$formattedSize ($lastBackupNotesCount notas • $lastBackupRemindersCount recordatorios)"
                                } else {
                                    "Sin respaldos previos"
                                }
                            )
                            if (accountEmail.isNotBlank()) {
                                BackupInfoStatRow(
                                    icon = Icons.Default.AccountCircle,
                                    label = "Cuenta:",
                                    value = accountEmail
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isBackingUp || isRestoring) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = WhatsAppGreen,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = backupStatusText.ifBlank {
                                        if (isRestoring) "Restaurando copia de seguridad..."
                                        else "Creando y subiendo copia de seguridad..."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = WhatsAppGreen,
                                trackColor = WhatsAppGreen.copy(alpha = 0.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones principales: GUARDAR AHORA y RESTAURAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onBackupNowClick,
                            enabled = !isBackingUp && !isRestoring,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .heightIn(min = 46.dp)
                                .testTag("btn_whatsapp_backup_now")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBackingUp) "Guardando..." else "Guardar ahora",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showHistoryDialog = true },
                            enabled = !isBackingUp && !isRestoring,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                accentColor.copy(alpha = 0.55f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("btn_open_backup_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (backupHistory.isNotEmpty()) {
                                    "Restaurar (${backupHistory.size})"
                                } else {
                                    "Restaurar"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 1: Frecuencia de copia automática con DropdownMenu integrado
                Box(modifier = Modifier.fillMaxWidth()) {
                    BackupSettingRow(
                        icon = Icons.Default.EventRepeat,
                        badgeColor = WhatsAppDarkGreen,
                        title = "Guardar en Google Drive",
                        subtitle = when (backupFrequency) {
                            BackupFrequency.DAILY -> "Se sube una copia automática todos los días"
                            BackupFrequency.WEEKLY -> "Se sube una copia automática una vez por semana"
                            BackupFrequency.MONTHLY -> "Se sube una copia automática una vez al mes"
                            BackupFrequency.MANUAL_ONLY -> "Solo cuando tocas el botón \"Guardar ahora\""
                            BackupFrequency.NEVER -> "Copias automáticas desactivadas"
                        },
                        valueText = backupFrequency.shortLabel,
                        isDropdownIndicator = true,
                        onClick = { showFrequencyDropdown = true },
                        testTag = "row_backup_frequency"
                    )

                    DropdownMenu(
                        expanded = showFrequencyDropdown,
                        onDismissRequest = { showFrequencyDropdown = false },
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF252528) else Color.White)
                    ) {
                        BackupFrequency.entries.forEach { freq ->
                            val isSelected = backupFrequency == freq
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = freq.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) WhatsAppGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = freq.description,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = WhatsAppGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showFrequencyDropdown = false
                                    onFrequencySelected(freq)
                                },
                                modifier = Modifier.testTag("option_backup_freq_${freq.code}")
                            )
                        }
                    }
                }

                // Fila 2: Horario programado con DropdownMenu integrado (si es Diaria, Semanal o Mensual)
                if (backupFrequency == BackupFrequency.DAILY ||
                    backupFrequency == BackupFrequency.WEEKLY ||
                    backupFrequency == BackupFrequency.MONTHLY
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    val presetHours = remember {
                        listOf(
                            Triple(2, 0, "02:00 AM (Recomendado)"),
                            Triple(4, 0, "04:00 AM (Madrugada)"),
                            Triple(9, 0, "09:00 AM (Mañana)"),
                            Triple(14, 0, "02:00 PM (Mediodía)"),
                            Triple(20, 0, "08:00 PM (Tarde / Noche)"),
                            Triple(23, 0, "11:00 PM (Antes de dormir)")
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        BackupSettingRow(
                            icon = Icons.Default.AccessTime,
                            badgeColor = Color(0xFF007AFF),
                            title = "Horario de copia automática",
                            subtitle = "Hora preferida para subir la copia en segundo plano",
                            valueText = formattedScheduleTime,
                            isDropdownIndicator = true,
                            onClick = { showTimeDropdown = true },
                            testTag = "row_backup_schedule_time"
                        )

                        DropdownMenu(
                            expanded = showTimeDropdown,
                            onDismissRequest = { showTimeDropdown = false },
                            modifier = Modifier
                                .background(if (isDark) Color(0xFF252528) else Color.White)
                        ) {
                            presetHours.forEach { (h, m, desc) ->
                                val isSelected = scheduledHour == h && scheduledMinute == m
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        showTimeDropdown = false
                                        onScheduledTimeSelected(h, m)
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 3: Copias anteriores (Restaurar historial)
                BackupSettingRow(
                    icon = Icons.Default.History,
                    badgeColor = Color(0xFF5856D6),
                    title = "Copias de seguridad anteriores",
                    subtitle = if (backupHistory.isEmpty()) {
                        "Aún no hay copias guardadas en el historial"
                    } else {
                        "${backupHistory.size} copias disponibles • Toca para elegir cuál restaurar"
                    },
                    valueText = "Ver historial",
                    onClick = { showHistoryDialog = true },
                    testTag = "row_previous_backups"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 4: Solo con Wi-Fi
                BackupSwitchRow(
                    icon = Icons.Default.Wifi,
                    badgeColor = Color(0xFFFF9500),
                    title = "Subir solo por Wi-Fi",
                    subtitle = if (wifiOnly) {
                        "Las copias automáticas esperan conexión Wi-Fi"
                    } else {
                        "Se permite usar datos móviles si no hay Wi-Fi"
                    },
                    checked = wifiOnly,
                    accentColor = accentColor,
                    onCheckedChange = onWifiOnlyChanged,
                    testTag = "switch_backup_wifi_only"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 5: Incluir documentos PDF y adjuntos
                BackupSwitchRow(
                    icon = Icons.Default.PictureAsPdf,
                    badgeColor = Color(0xFFFF3B30),
                    title = "Incluir documentos PDF y adjuntos",
                    subtitle = "Respalda también tus archivos PDF y firmas guardadas",
                    checked = includePdfs,
                    accentColor = accentColor,
                    onCheckedChange = onIncludePdfsChanged,
                    testTag = "switch_backup_include_pdfs"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 6: Sincronizar carpetas de Notas y PDFs en Google Drive
                BackupSettingRow(
                    icon = Icons.Default.CloudSync,
                    badgeColor = Color(0xFF4285F4),
                    title = "Sincronizar Notas y PDFs con Drive",
                    subtitle = "Subir o descargar documentos PDF y notas en tu Google Drive",
                    valueText = "Gestionar",
                    onClick = onOpenDriveSyncClick,
                    testTag = "row_backup_drive_pdfs_sync"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Fila 7: Exportar copia de seguridad en formato JSON (unificado aquí)
                BackupSettingRow(
                    icon = Icons.Default.Share,
                    badgeColor = Color(0xFF34C759),
                    title = "Exportar copia de seguridad (JSON)",
                    subtitle = "Compartir o guardar todas tus notas en un archivo JSON externo",
                    valueText = "Exportar",
                    onClick = onExportJsonClick,
                    testTag = "settings_row_export"
                )
            }
        }
    }

    // DIÁLOGO: HISTORIAL DE COPIAS DE SEGURIDAD ANTERIORES
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            modifier = Modifier.border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                RoundedCornerShape(24.dp)
            ),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Copias anteriores",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Selecciona cualquier copia para restaurarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                if (backupHistory.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Aún no tienes copias de seguridad guardadas.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Presiona \"Guardar ahora\" para crear tu primera copia de seguridad y poder restaurarla cuando quieras.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                showHistoryDialog = false
                                onBackupNowClick()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear copia ahora", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = backupHistory,
                            key = { _, item -> item.id }
                        ) { index, snapshot ->
                            val isLatest = index == 0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isDark) 0.07f else 0.04f
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isLatest) WhatsAppGreen.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("backup_history_item_${snapshot.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = formatBackupTimestamp(snapshot.timestamp),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isLatest) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(WhatsAppGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "Más reciente",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = WhatsAppGreen
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteSnapshot(snapshot) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Eliminar esta copia",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        SmallMetaBadge(
                                            text = snapshot.triggerBadgeLabel,
                                            color = accentColor
                                        )
                                        SmallMetaBadge(
                                            text = if (snapshot.isUploadedToCloud) "Drive / Nube ✓" else "Local",
                                            color = if (snapshot.isUploadedToCloud) WhatsAppGreen else Color(0xFF007AFF)
                                        )
                                        SmallMetaBadge(
                                            text = snapshot.formattedSize,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = buildString {
                                            append("${snapshot.notesCount} notas • ${snapshot.remindersCount} recordatorios")
                                            if (snapshot.pdfsCount > 0) {
                                                append(" • ${snapshot.pdfsCount} PDFs")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (snapshot.accountEmail.isNotBlank()) {
                                        Text(
                                            text = snapshot.accountEmail,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Button(
                                        onClick = {
                                            snapshotToRestore = snapshot
                                            showHistoryDialog = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 38.dp)
                                            .testTag("btn_restore_snapshot_${snapshot.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Restaurar esta copia",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // DIÁLOGO: CONFIRMAR RESTAURACIÓN DE COPIA SELECCIONADA (COMBINAR O REEMPLAZAR)
    snapshotToRestore?.let { targetSnapshot ->
        AlertDialog(
            onDismissRequest = { snapshotToRestore = null },
            modifier = Modifier.border(
                BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                RoundedCornerShape(24.dp)
            ),
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            title = {
                Text(
                    text = "Restaurar copia de seguridad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = WhatsAppGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Copia del ${formatBackupTimestamp(targetSnapshot.timestamp)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${targetSnapshot.notesCount} notas • ${targetSnapshot.remindersCount} recordatorios • ${targetSnapshot.formattedSize}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "¿Cómo deseas restaurar esta copia de seguridad?",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    RestoreMode.entries.forEach { mode ->
                        val isSelected = selectedRestoreMode == mode
                        Surface(
                            onClick = { selectedRestoreMode = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.14f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val snap = targetSnapshot
                        val mode = selectedRestoreMode
                        snapshotToRestore = null
                        onRestoreSnapshot(snap, mode)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Restaurar ahora", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { snapshotToRestore = null },
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
private fun SmallMetaBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp
            ),
            color = color
        )
    }
}

@Composable
private fun BackupInfoStatRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BackupSettingRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    valueText: String,
    isDropdownIndicator: Boolean = false,
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
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.5.sp
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (valueText.isNotBlank()) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = if (isDropdownIndicator) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.ArrowForwardIos
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(if (isDropdownIndicator) 18.dp else 12.dp)
            )
        }
    }
}

@Composable
private fun BackupSwitchRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
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
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.5.sp
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
                        maxLines = 2,
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
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

private fun formatBackupTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Nunca"
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    return when {
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR) -> {
            "Hoy, ${timeFormat.format(Date(timestamp))}"
        }
        now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Ayer, ${timeFormat.format(Date(timestamp))}"
        }
        else -> {
            val fullFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "ES"))
            fullFormat.format(Date(timestamp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    return if (kb < 1024.0) {
        String.format(Locale.US, "%.1f KB", kb)
    } else {
        val mb = kb / 1024.0
        String.format(Locale.US, "%.2f MB", mb)
    }
}
