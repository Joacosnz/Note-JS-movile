package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Barra de Navegación Inferior Flotante con paradigma de diseño "Liquid Glass" (Cápsula flotante compacta centrada).
 */
@Composable
fun AppleBottomToolbar(
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier,
    isHomeActive: Boolean = true,
    isFoldersActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onFoldersClick: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    // Colores Liquid Glass
    val backgroundColor = if (isDark) Color(0xFF141416).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.82f)
    val activeColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1A365D)
    val inactiveColor = Color(0xFF8E8E93)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .testTag("nav_bar_liquid_glass"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .height(62.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .border(width = 1.dp, brush = specularBorder, shape = CircleShape),
            shape = CircleShape,
            color = backgroundColor,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Inicio (Casita - Izquierda)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

                // 2. Crear Nota Directa (+ en Círculo - Centro)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNewNote()
                        }
                        .testTag("btn_create_note_direct"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Crear nota",
                        tint = activeColor,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 3. Biblioteca / Carpetas (Derecha)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
