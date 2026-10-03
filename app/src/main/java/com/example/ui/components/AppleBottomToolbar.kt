package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Barra de Navegación Inferior Flotante con paradigma de diseño "Liquid Glass".
 * Al mantener presionado el botón "+", la barra se expande hacia arriba mostrando
 * las opciones integradas en la misma barra ("Nota de Voz", "Leer PDF").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleBottomToolbar(
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier,
    isHomeActive: Boolean = true,
    isFoldersActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onFoldersClick: () -> Unit = {},
    onVoiceNote: () -> Unit = {},
    onReadPdf: () -> Unit = {},
    onScanDocument: () -> Unit = {},
    onReminder: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme()
    val haptic = LocalHapticFeedback.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    val activeColor = Color(0xFFEAB308)
    val inactiveColor = Color(0xFF8E8E93)
    val backgroundColor = if (isDark) Color(0xFF18181B).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)

    // Bisel Vitrocerámico (Borde con gradiente especular)
    val specularBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.35f),
                Color.Transparent,
                Color(0xFF27272A).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.25f),
                Color(0xFFCBD5E1).copy(alpha = 0.40f)
            )
        )
    }

    val rotation by animateFloatAsState(
        targetValue = if (isMenuExpanded) 45f else 0f,
        animationSpec = spring(),
        label = "plus_icon_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // Backdrop clicable a pantalla completa cuando la barra está expandida
        if (isMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isMenuExpanded = false }
            )
        }

        // Barra con padding de barra de navegación
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .testTag("nav_bar_liquid_glass"),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .border(width = 1.dp, brush = specularBorder, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = backgroundColor,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .width(230.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Sección Expandida Superior
                    AnimatedVisibility(
                        visible = isMenuExpanded,
                        enter = fadeIn(tween(180)) + expandVertically(
                            expandFrom = Alignment.Bottom,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = fadeOut(tween(120)) + shrinkVertically(
                            shrinkTowards = Alignment.Bottom,
                            animationSpec = tween(120)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Opción 0: Recordatorio
                            QuickActionMenuItem(
                                icon = Icons.Default.Notifications,
                                label = "Recordatorio",
                                accentColor = activeColor,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMenuExpanded = false
                                    onReminder()
                                },
                                testTag = "btn_quick_reminder"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )

                            // Opción 1: Nota de Voz
                            QuickActionMenuItem(
                                icon = Icons.Default.Mic,
                                label = "Nota de Voz",
                                accentColor = activeColor,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMenuExpanded = false
                                    onVoiceNote()
                                },
                                testTag = "btn_quick_voice_note"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )

                            // Opción 2: Leer PDF
                            QuickActionMenuItem(
                                icon = Icons.Default.PictureAsPdf,
                                label = "Leer PDF",
                                accentColor = activeColor,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMenuExpanded = false
                                    onReadPdf()
                                },
                                testTag = "btn_quick_read_pdf"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )

                            // Opción 3: Escanear Documento
                            QuickActionMenuItem(
                                icon = Icons.Default.DocumentScanner,
                                label = "Escanear Documento",
                                accentColor = activeColor,
                                textColor = if (isDark) Color.White else Color(0xFF1C1C1E),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isMenuExpanded = false
                                    onScanDocument()
                                },
                                testTag = "btn_quick_scan_document"
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f),
                                thickness = 0.8.dp
                            )
                        }
                    }

                    // Fila Principal de Navegación Inferior
                    Row(
                        modifier = Modifier
                            .height(58.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 1. Inicio (Casita - Izquierda)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isMenuExpanded) isMenuExpanded = false
                                    onHomeClick()
                                }
                                .testTag("home_nav_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Inicio",
                                    tint = if (isHomeActive) activeColor else inactiveColor,
                                    modifier = Modifier.size(24.dp),
                                )

                                if (isHomeActive) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(activeColor),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 2. Crear Nota / Mantener presionado para expandir la barra (+ Centro)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isMenuExpanded) {
                                            isMenuExpanded = false
                                        } else {
                                            onNewNote()
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isMenuExpanded = !isMenuExpanded
                                    }
                                )
                                .testTag("btn_create_note_direct"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Crear nota o expandir opciones",
                                tint = activeColor,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rotation),
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 3. Biblioteca / Carpetas (Derecha)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isMenuExpanded) isMenuExpanded = false
                                    onFoldersClick()
                                }
                                .testTag("folders_nav_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Biblioteca / Carpetas",
                                    tint = if (isFoldersActive) activeColor else inactiveColor,
                                    modifier = Modifier.size(24.dp),
                                )

                                if (isFoldersActive) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(activeColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionMenuItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}

@Preview(showBackground = true, name = "Floating Bottom Toolbar Liquid Glass - Light")
@Composable
private fun AppleBottomToolbarLightPreview() {
    AppleBottomToolbar(
        onNewNote = {},
        isHomeActive = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Floating Bottom Toolbar Liquid Glass - Dark")
@Composable
private fun AppleBottomToolbarDarkPreview() {
    AppleBottomToolbar(
        onNewNote = {},
        isHomeActive = true,
    )
}
