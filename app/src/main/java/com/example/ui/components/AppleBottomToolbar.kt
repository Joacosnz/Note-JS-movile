package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteType
import kotlinx.coroutines.launch

/**
 * Barra flotante inferior en forma de píldora/cápsula.
 *
 * Características principales:
 * 1. Estética y Contenedor Flotante:
 *    - Forma: Cápsula totalmente redondeada (CircleShape).
 *    - Disposición: Centrada inferior con márgenes (32.dp horizontal, 12.dp vertical) y navigationBarsPadding().
 *    - Dimensiones: Altura fija de 60.dp.
 *    - Fondo: Blanco puro en modo claro (Color.White), gris oscuro de iOS (#1C1C1E) en modo oscuro.
 *    - Sombra/Elevación: 8.dp para flotar limpiamente sobre el contenido.
 * 2. Estructura de los 3 Íconos (Distribución Equidistante):
 *    - Botón Inicio (Casita - Izquierda): Ícono con indicador azul horizontal activo cuando isHomeActive = true.
 *    - Botón Crear Nota (+ en círculo - Centro): Ícono destacado de 32.dp que abre ModalBottomSheet con 4 opciones.
 *    - Botón Biblioteca / Carpetas (Derecha): Ícono de carpetas que abre el drawer de biblioteca.
 * 3. Código y Accesibilidad:
 *    - Modifier.testTag("floating_bottom_bar"), home_nav_button, create_note_button, folders_nav_button.
 *    - Touch target mínimo de 48.dp por elemento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleBottomToolbar(
    onNewNote: (NoteType) -> Unit,
    modifier: Modifier = Modifier,
    noteCount: Int = 0,
    isHomeActive: Boolean = true,
    isFoldersActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onFoldersClick: () -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()

    // Colores según especificaciones
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val activeColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A)
    val inactiveColor = Color(0xFF8E8E93)

    var showCreationSheet by remember { mutableStateOf(value = false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 12.dp)
            .testTag("floating_bottom_bar"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(elevation = 8.dp, shape = CircleShape),
            shape = CircleShape,
            color = backgroundColor,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Botón Inicio (Casita - Izquierda)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onHomeClick)
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
                                    .width(14.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(activeColor),
                            )
                        }
                    }
                }

                // 2. Botón Crear Nota (+ en círculo - Centro)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { showCreationSheet = true }
                        .testTag("create_note_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Crear nota",
                        tint = activeColor,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 3. Botón Biblioteca / Carpetas (Derecha)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFoldersClick)
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
                                    .width(14.dp)
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

    // ModalBottomSheet de Creación de Notas
    if (showCreationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreationSheet = false },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text = if (noteCount > 0) "Crear nueva nota ($noteCount)" else "Crear nueva nota",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // Option 1: Nota de texto
                CreationSheetOption(
                    icon = Icons.Default.EditNote,
                    title = "Nota de texto",
                    subtitle = "Abre el editor para una nota estándar en blanco",
                    tint = activeColor,
                    testTag = "create_option_text",
                ) {
                    scope.launch {
                        sheetState.hide()
                        showCreationSheet = false
                        onNewNote(NoteType.TEXT)
                    }
                }

                // Option 2: Lista de tareas
                CreationSheetOption(
                    icon = Icons.Default.Checklist,
                    title = "Lista de tareas",
                    subtitle = "Nueva lista con casillas circulares interactivas",
                    tint = activeColor,
                    testTag = "create_option_checklist",
                ) {
                    scope.launch {
                        sheetState.hide()
                        showCreationSheet = false
                        onNewNote(NoteType.CHECKLIST)
                    }
                }

                // Option 3: Math Note
                CreationSheetOption(
                    icon = Icons.Default.Calculate,
                    title = "Math Note",
                    subtitle = "Nota con cálculo reactivo en tiempo real",
                    tint = activeColor,
                    testTag = "create_option_math",
                ) {
                    scope.launch {
                        sheetState.hide()
                        showCreationSheet = false
                        onNewNote(NoteType.MATH)
                    }
                }

                // Option 4: Boceto a mano
                CreationSheetOption(
                    icon = Icons.Default.Gesture,
                    title = "Boceto a mano",
                    subtitle = "Abre el lienzo de dibujo vectorial",
                    tint = activeColor,
                    testTag = "create_option_drawing",
                ) {
                    scope.launch {
                        sheetState.hide()
                        showCreationSheet = false
                        onNewNote(NoteType.DRAWING)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CreationSheetOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Floating Bottom Toolbar - Light")
@Composable
private fun AppleBottomToolbarLightPreview() {
    AppleBottomToolbar(
        onNewNote = {},
        isHomeActive = true,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Floating Bottom Toolbar - Dark")
@Composable
private fun AppleBottomToolbarDarkPreview() {
    AppleBottomToolbar(
        onNewNote = {},
        isHomeActive = true,
    )
}
