package com.example.ui.components.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SyncState
import com.example.ui.components.LiquidGlassContainer

@Composable
internal fun MenuGridItem(
    icon: ImageVector,
    label: String,
    amber: Color,
    isPaperDark: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(amber.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = amber,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPaperDark) Color.White else Color(0xFF1C1C1E),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    amber: Color,
    editorBgColor: Color,
    isPaperDark: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isPinned: Boolean,
    isLocked: Boolean,
    syncEnabled: Boolean,
    syncState: SyncState = SyncState.SYNCED,
    isArchived: Boolean,
    reminderTimestamp: Long?,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReminderClick: () -> Unit,
    onShareClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenPaperStyle: () -> Unit,
    onOpenColorPicker: () -> Unit = {},
    onToggleSync: () -> Unit,
    onOpenStats: () -> Unit,
    onToggleArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    TopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Nota bloqueada",
                        tint = amber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                val syncIcon = when (syncState) {
                    SyncState.SYNCED -> Icons.Default.CloudDone
                    SyncState.SYNCING -> Icons.Default.CloudSync
                    SyncState.OFFLINE -> Icons.Default.CloudOff
                }
                val syncTint = when (syncState) {
                    SyncState.SYNCED -> amber.copy(alpha = 0.7f)
                    SyncState.SYNCING -> amber
                    SyncState.OFFLINE -> if (isPaperDark) Color(0xFFEF4444) else Color(0xFFDC2626)
                }

                if (syncState == SyncState.SYNCING) {
                    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = "Sincronizando...",
                        tint = syncTint,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = rotation
                                this.alpha = alpha
                            }
                    )
                } else {
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = when (syncState) {
                            SyncState.SYNCED -> "Sincronizado"
                            SyncState.OFFLINE -> "Sin conexión"
                            else -> ""
                        },
                        tint = syncTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("notejs_editor_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = amber
                )
            }
        },
        actions = {
            // Deshacer
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("btn_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Deshacer",
                    tint = if (canUndo) amber else amber.copy(alpha = 0.38f)
                )
            }

            // Rehacer
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("btn_redo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Rehacer",
                    tint = if (canRedo) amber else amber.copy(alpha = 0.38f)
                )
            }

            // Google Keep Color Palette
            IconButton(
                onClick = onOpenColorPicker,
                modifier = Modifier.testTag("btn_editor_color_palette")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Color de la nota",
                    tint = amber
                )
            }

            Box {
                IconButton(
                    onClick = { onShowMenuChange(true) },
                    modifier = Modifier.testTag("btn_more_options")
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "Opciones",
                        tint = if (showMenu) Color.Transparent else amber
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { onShowMenuChange(false) },
                    offset = DpOffset(x = (-16).dp, y = 4.dp),
                    containerColor = Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .background(Color.Transparent)
                        .shadow(0.dp)
                ) {
                    LiquidGlassContainer(
                        shape = RoundedCornerShape(24.dp),
                        isDark = isPaperDark,
                        glassAlpha = 0.85f,
                        elevation = 16.dp,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Fila 1: Acciones Rápidas
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MenuGridItem(
                                    icon = if (reminderTimestamp != null) Icons.Default.Notifications else Icons.Outlined.NotificationsActive,
                                    label = "Recordatorio",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onReminderClick()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = Icons.Default.Share,
                                    label = "Compartir",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onShareClick()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = Icons.Default.PictureAsPdf,
                                    label = "PDF",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onExportPdfClick()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = if (isPaperDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Fila 2: Ajustes de la nota
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MenuGridItem(
                                    icon = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                    label = if (isPinned) "Desanclar" else "Fijar",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onTogglePin()
                                        onShowMenuChange(false)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = Icons.Outlined.Palette,
                                    label = "Color",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onOpenColorPicker()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = if (isLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                                    label = if (isLocked) "Desbloquear" else "Bloquear",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onToggleLock()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = Icons.Default.GridView,
                                    label = "Papel",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onOpenPaperStyle()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = if (isPaperDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Ítem de sincronización en la nube
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sincronizar en la nube",
                                        color = if (isPaperDark) Color.White else Color(0xFF1C1C1E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (syncEnabled) Icons.Default.Cloud else Icons.Default.CloudOff,
                                        contentDescription = "Sincronización en la nube",
                                        tint = amber
                                    )
                                },
                                onClick = {
                                    onShowMenuChange(false)
                                    onToggleSync()
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )

                            HorizontalDivider(
                                color = if (isPaperDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Fila 3: Información y Utilidades
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MenuGridItem(
                                    icon = Icons.Default.Info,
                                    label = "Info",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        onOpenStats()
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = if (isArchived) Icons.Default.Unarchive else Icons.Outlined.Archive,
                                    label = if (isArchived) "Desarchivar" else "Archivar",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onToggleArchive()
                                        onShowMenuChange(false)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                                MenuGridItem(
                                    icon = Icons.Default.Close,
                                    label = "Cerrar",
                                    amber = amber,
                                    isPaperDark = isPaperDark,
                                    onClick = {
                                        onShowMenuChange(false)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = editorBgColor
        )
    )
}
