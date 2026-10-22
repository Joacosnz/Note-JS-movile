package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteColors
import com.example.model.NoteThemeColor

/**
 * Selector de colores y fondos idéntico a Google Keep.
 * Presenta los 12 tonos icónicos de Keep en una fila horizontal con desplazamiento,
 * opción de restablecer/sin color, icono de selección (Checkmark) y respuesta háptica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepColorPickerBottomSheet(
    show: Boolean,
    selectedColorIndex: Int,
    isDark: Boolean,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!show) return

    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // Auto-scroll al color seleccionado al abrir
    LaunchedEffect(show, selectedColorIndex) {
        val targetIdx = selectedColorIndex.coerceIn(0, NoteColors.options.lastIndex)
        listState.scrollToItem(targetIdx)
    }

    val selectedTheme = remember(selectedColorIndex) {
        NoteColors.options.firstOrNull { it.id == selectedColorIndex } ?: NoteColors.options[0]
    }

    val sheetBgColor = if (isDark) Color(0xFF28292C) else Color(0xFFFFFFFF)
    val onSurfaceColor = if (isDark) Color(0xFFE8EAED) else Color(0xFF202124)
    val subtitleColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetBgColor,
        tonalElevation = 6.dp,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("keep_color_picker_bottom_sheet")
        ) {
            // Cabecera estilo Google Keep
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = onSurfaceColor
                    )
                )

                Text(
                    text = selectedTheme.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = subtitleColor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de swatches horizontales idéntica a Google Keep
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keep_color_swatches_row"),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(NoteColors.options, key = { it.id }) { option ->
                    val isSelected = option.id == selectedColorIndex
                    KeepColorSwatchItem(
                        option = option,
                        isSelected = isSelected,
                        isDark = isDark,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onColorSelected(option.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeepColorSwatchItem(
    option: NoteThemeColor,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDefault = option.id == 0
    val swatchColor = if (isDefault) {
        if (isDark) Color(0xFF202124) else Color(0xFFFFFFFF)
    } else {
        if (isDark) option.darkBg else option.lightBg
    }

    // Color del borde exterior
    val borderColor = when {
        isSelected -> {
            if (isDark) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
        }
        isDefault -> {
            if (isDark) Color(0xFF5F6368) else Color(0xFFDADCE0)
        }
        else -> {
            if (isDark) option.darkBorder else option.lightBorder
        }
    }

    val borderWidth = if (isSelected) 2.5.dp else 1.dp

    // Color del checkmark
    val checkmarkTint = if (isDark) {
        Color.White
    } else {
        Color(0xFF202124)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = option.name }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("keep_swatch_${option.id}"),
        contentAlignment = Alignment.Center
    ) {
        // Círculo del swatch
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDefault) {
                // Icono sin color / reset
                if (!isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.FormatColorReset,
                        contentDescription = "Sin color",
                        tint = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = checkmarkTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado: ${option.name}",
                        tint = checkmarkTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
