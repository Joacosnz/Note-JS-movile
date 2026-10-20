package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
internal fun EditorTopAppBar(
    amber: Color,
    editorBgColor: Color,
    isPaperDark: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isPinned: Boolean,
    isLocked: Boolean,
    syncEnabled: Boolean,
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
    onToggleSync: () -> Unit,
    onOpenStats: () -> Unit,
    onToggleArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    TopAppBar(
        modifier = modifier,
        title = {},
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

@Composable
internal fun FormatSectionTitle(
    title: String,
    isPaperDark: Boolean = false
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = if (isPaperDark) Color(0xFF94A3B8) else Color(0xFF64748B),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 12.dp)
    )
}

@Composable
internal fun FormatOptionItem(
    icon: ImageVector,
    text: String,
    isPaperDark: Boolean = false,
    amberColor: Color = Color(0xFFFFB800),
    onClick: () -> Unit
) {
    val bgColor = if (isPaperDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val contentColor = if (isPaperDark) Color.White else Color(0xFF0F172A)
    val iconTint = amberColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = contentColor
            )
        )
    }
}

@Composable
internal fun EditorAccessoryBar(
    amber: Color,
    isPaperDark: Boolean,
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isHighlightActive: Boolean,
    customFontSizeSp: Int,
    onFormatBody: () -> Unit,
    onFormatTitle: () -> Unit,
    onFormatSubtitle: () -> Unit,
    onFormatChecklist: () -> Unit,
    onFormatBulletList: () -> Unit,
    onFormatNumberedList: () -> Unit,
    onFormatCallout: () -> Unit,
    onFormatTable: () -> Unit,
    onFormatDrawing: () -> Unit,
    onFormatPdf: () -> Unit,
    onFormatScanner: () -> Unit,
    onFormatMath: () -> Unit = {},
    onAddImage: () -> Unit,
    onStartAudioDictation: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleHighlight: () -> Unit,
    onOpenFontSizeRuler: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var isInsertMenuExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isInsertMenuExpanded) 45f else 0f,
        animationSpec = spring(),
        label = "insert_menu_rotation"
    )

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
    ) {
        // Backdrop transparente cuando el menú está expandido
        if (isInsertMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isInsertMenuExpanded = false }
            )
        }

        // Contenedor Único de Cristal Líquido que se expande hacia arriba
        LiquidGlassContainer(
            isDark = isPaperDark,
            shape = RoundedCornerShape(28.dp),
            glassAlpha = 0.88f,
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .testTag("liquid_glass_accessory_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sección Expandida Superior
                AnimatedVisibility(
                    visible = isInsertMenuExpanded,
                    enter = fadeIn(animationSpec = tween(150)) + expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ),
                    exit = fadeOut(animationSpec = tween(100)) + shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(100)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Insertar contenido",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = if (isPaperDark) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
                            )

                            FormatSectionTitle("Básico", isPaperDark = isPaperDark)
                            FormatOptionItem(
                                icon = Icons.AutoMirrored.Rounded.Subject,
                                text = "Texto normal (Cuerpo)",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatBody()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.Title,
                                text = "Título grande",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatTitle()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.FormatSize,
                                text = "Subtítulo",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatSubtitle()
                                isInsertMenuExpanded = false
                            }

                            FormatSectionTitle("Listas", isPaperDark = isPaperDark)
                            FormatOptionItem(
                                icon = Icons.Rounded.CheckBox,
                                text = "Lista de tareas (Checklist)",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatChecklist()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                                text = "Lista con viñetas",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatBulletList()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.FormatListNumbered,
                                text = "Lista numerada",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatNumberedList()
                                isInsertMenuExpanded = false
                            }

                            FormatSectionTitle("Elementos Avanzados", isPaperDark = isPaperDark)
                            FormatOptionItem(
                                icon = Icons.Rounded.Info,
                                text = "Cuadro destacado",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatCallout()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.TableChart,
                                text = "Tabla",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatTable()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.Draw,
                                text = "Boceto a mano (Dibujo)",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatDrawing()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Rounded.PictureAsPdf,
                                text = "Adjuntar PDF",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatPdf()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Default.DocumentScanner,
                                text = "Escanear documento (Modo Escáner Pro)",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatScanner()
                                isInsertMenuExpanded = false
                            }

                            FormatOptionItem(
                                icon = Icons.Default.Functions,
                                text = "Ecuación (KaTeX)",
                                isPaperDark = isPaperDark,
                                amberColor = amber
                            ) {
                                onFormatMath()
                                isInsertMenuExpanded = false
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            color = if (isPaperDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f),
                            thickness = 0.8.dp
                        )
                    }
                }

                // Fila Inferior de Botones de la Barra de Accesorios
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Botón "Insertar" (+)
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isInsertMenuExpanded = !isInsertMenuExpanded
                        },
                        modifier = Modifier.testTag("btn_insert_menu")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Insertar",
                                tint = amber,
                                modifier = Modifier
                                    .size(28.dp)
                                    .rotate(rotation)
                            )
                        }
                    }

                    // 2. Subir Imagen
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddImage()
                        },
                        modifier = Modifier.testTag("btn_add_image")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Subir imagen",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 3. Micrófono (Dictado de voz)
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStartAudioDictation()
                        },
                        modifier = Modifier.testTag("btn_add_audio")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictado de voz",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 4. Negrita
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleBold()
                        },
                        modifier = Modifier.testTag("btn_format_bold")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isBoldActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatBold,
                                contentDescription = "Negrita",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 5. Cursiva
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleItalic()
                        },
                        modifier = Modifier.testTag("btn_format_italic")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isItalicActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatItalic,
                                contentDescription = "Cursiva",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 6. Resaltar selección
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleHighlight()
                        },
                        modifier = Modifier.testTag("btn_format_highlight")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isHighlightActive) amber.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Brush,
                                contentDescription = "Resaltar",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 7. Tamaño de texto (Icono de regla)
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenFontSizeRuler()
                        },
                        modifier = Modifier.testTag("btn_font_size_ruler")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (customFontSizeSp != 17) amber.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Straighten,
                                contentDescription = "Tamaño de texto",
                                tint = amber,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
