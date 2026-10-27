package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DEFAULT_NOTE_JS_ACCENT_HEX
import com.example.ui.theme.DefaultNoteJsAmber
import com.example.ui.theme.NoteJsAccentPresets
import com.example.ui.theme.colorToHex
import com.example.ui.theme.parseNoteJsAccentHex

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconColorPaletteDialog(
    currentHex: String,
    isDark: Boolean,
    onColorSelected: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val activeColor = parseNoteJsAccentHex(currentHex)
    val isDefaultSelected = currentHex.trim().equals(DEFAULT_NOTE_JS_ACCENT_HEX, ignoreCase = true)

    val initialHsv = remember {
        FloatArray(3).also { hsv ->
            AndroidColor.colorToHSV(activeColor.toArgb(), hsv)
        }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    LaunchedEffect(currentHex) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(parseNoteJsAccentHex(currentHex).toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val sheetBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    val cardBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val primaryText = if (isDark) Color.White else Color(0xFF111827)
    val secondaryText = if (isDark) Color(0xFF98989D) else Color(0xFF6B7280)
    val onActiveColor = if (activeColor.luminance() > 0.45f) Color.Black else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .border(
                    BorderStroke(1.dp, activeColor.copy(alpha = 0.35f)),
                    RoundedCornerShape(28.dp)
                )
                .testTag("dialog_icon_color_palette"),
            shape = RoundedCornerShape(28.dp),
            color = sheetBg,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Encabezado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Color de los iconos",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Personaliza el color de todos los iconos de la app",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = secondaryText
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = primaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Vista previa en tiempo real de los iconos
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VISTA PREVIA EN TIEMPO REAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = secondaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val previewIcons = listOf(
                                Icons.Default.Home to "Inicio",
                                Icons.Default.Notifications to "Alertas",
                                Icons.Default.AddCircle to "Crear",
                                Icons.Default.Folder to "Carpetas",
                                Icons.Default.Search to "Buscar",
                                Icons.Default.Settings to "Ajustes"
                            )
                            previewIcons.forEach { (iconVec, label) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(activeColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVec,
                                            contentDescription = label,
                                            tint = activeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = secondaryText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Botón dedicado: Color predeterminado (Amarillo Note JS)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetDefault()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDefaultSelected) {
                        DefaultNoteJsAmber.copy(alpha = 0.16f)
                    } else {
                        cardBg
                    },
                    border = BorderStroke(
                        width = if (isDefaultSelected) 1.5.dp else 1.dp,
                        color = if (isDefaultSelected) DefaultNoteJsAmber else DefaultNoteJsAmber.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_default_icon_color")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(DefaultNoteJsAmber)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDefaultSelected) Icons.Default.Check else Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Color predeterminado",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Amarillo original de la app (#EAB308)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isDefaultSelected) DefaultNoteJsAmber
                                    else DefaultNoteJsAmber.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isDefaultSelected) "Activo" else "Restaurar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (isDefaultSelected) Color.Black else DefaultNoteJsAmber
                            )
                        }
                    }
                }

                // Colores rápidos de la paleta (incluyendo Violeta, Morado, Azul, etc.)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COLORES DE LA PALETA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.7.sp
                        ),
                        color = secondaryText
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 4
                    ) {
                        NoteJsAccentPresets.forEach { preset ->
                            val isSelected = currentHex.trim().equals(preset.hex, ignoreCase = true)
                            val checkTint = if (preset.color.luminance() > 0.45f) Color.Black else Color.White

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onColorSelected(preset.hex)
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag("preset_color_${preset.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(preset.color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) primaryText else Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = checkTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) preset.color else primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Selector libre de espectro cromático (Paleta de colores 2D + Barra de tono)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECTOR PERSONALIZADO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.7.sp
                            ),
                            color = secondaryText
                        )
                        Text(
                            text = currentHex.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = activeColor
                        )
                    }

                    // Cuadro 2D de Saturación y Brillo
                    val pureHueColor = remember(hue) {
                        Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, primaryText.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .pointerInput(hue) {
                                detectTapGestures { offset ->
                                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                                    val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    saturation = s
                                    value = v
                                    val newColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, s, v)))
                                    onColorSelected(colorToHex(newColor))
                                }
                            }
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                    saturation = s
                                    value = v
                                    val newColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, s, v)))
                                    onColorSelected(colorToHex(newColor))
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, pureHueColor)
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )
                            val cx = saturation * size.width
                            val cy = (1f - value) * size.height
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.5f),
                                radius = 11.5.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }

                    // Barra de Matiz (Arcoíris de tonos: Rojo, Amarillo, Verde, Cian, Azul, Violeta, Rosa)
                    val hueColors = remember {
                        listOf(
                            Color(0xFFFF0000),
                            Color(0xFFFFFF00),
                            Color(0xFF00FF00),
                            Color(0xFF00FFFF),
                            Color(0xFF0000FF),
                            Color(0xFFFF00FF),
                            Color(0xFFFF0000)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(CircleShape)
                            .border(1.dp, primaryText.copy(alpha = 0.15f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val h = ((offset.x / size.width).coerceIn(0f, 1f)) * 360f
                                    hue = h
                                    val s = if (saturation < 0.15f) 0.85f else saturation
                                    val v = if (value < 0.25f) 0.95f else value
                                    saturation = s
                                    value = v
                                    val newColor = Color(AndroidColor.HSVToColor(floatArrayOf(h, s, v)))
                                    onColorSelected(colorToHex(newColor))
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val h = ((change.position.x / size.width).coerceIn(0f, 1f)) * 360f
                                    hue = h
                                    val newColor = Color(AndroidColor.HSVToColor(floatArrayOf(h, saturation, value)))
                                    onColorSelected(colorToHex(newColor))
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.horizontalGradient(hueColors))
                            val thumbX = (hue / 360f).coerceIn(0f, 1f) * size.width
                            drawCircle(
                                color = Color.White,
                                radius = 11.dp.toPx(),
                                center = Offset(thumbX, size.height / 2f),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }

                // Botón de confirmación
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor,
                        contentColor = onActiveColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_icon_color")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listo",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}
