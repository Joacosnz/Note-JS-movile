package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra de Búsqueda con paradigma de diseño "Liquid Glass" (Matriz Vitrocerámica + Metal Amorfo BMG).
 */
@Composable
fun AppleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar en notas...",
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    // Colores Liquid Glass
    val backgroundColor = if (isDark) Color(0xFF141416).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.82f)
    val searchIconColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1A365D)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val placeholderColor = Color(0xFF71717A) // Gris mineral neutro

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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .border(width = 1.dp, brush = specularBorder, shape = CircleShape)
            .testTag("search_bar_liquid_glass"),
        shape = CircleShape,
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Padding inicial de 18.dp
            Spacer(modifier = Modifier.width(18.dp))

            // Ícono de Búsqueda (Lupa) en azul índigo pulido (aleación amorfa)
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = searchIconColor,
                modifier = Modifier.size(22.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Campo de entrada (BasicTextField)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            color = placeholderColor,
                        ),
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor,
                    ),
                    cursorBrush = SolidColor(searchIconColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_input"),
                )
            }

            // Botón de limpieza animado con respuesta háptica snap-back
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onQueryChange("")
                    },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = placeholderColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (query.isEmpty()) {
                Spacer(modifier = Modifier.width(18.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Apple Search Bar Liquid Glass - Light")
@Composable
private fun AppleSearchBarLightPreview() {
    AppleSearchBar(
        query = "",
        onQueryChange = {},
    )
}

@Preview(showBackground = true, name = "Apple Search Bar Liquid Glass - Light with text")
@Composable
private fun AppleSearchBarLightWithTextPreview() {
    AppleSearchBar(
        query = "Investigación BMG",
        onQueryChange = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Apple Search Bar Liquid Glass - Dark")
@Composable
private fun AppleSearchBarDarkPreview() {
    AppleSearchBar(
        query = "Ceramic Shield",
        onQueryChange = {},
    )
}
