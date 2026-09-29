package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Campo de búsqueda estilo cápsula ergonómico.
 *
 * Cumple con las especificaciones de diseño:
 * 1. Geometría y Contenedor:
 *    - Forma: Cápsula completamente redondeada (CircleShape).
 *    - Dimensiones: Altura de 52.dp, ocupando el ancho completo con márgenes laterales de 16.dp.
 *    - Fondo: Modo claro: blanco puro (Color.White), Modo oscuro: gris oscuro elegante (#1C1C1E).
 *    - Sombra y elevación: Sombra suave de 3.dp y sutil borde BorderStroke(1.dp, Color(0xFFE5E5EA)) en modo claro.
 * 2. Ícono de Búsqueda:
 *    - Extremo izquierdo con padding inicial de 18.dp.
 *    - Ícono de lupa delineado (Icons.Outlined.Search) en tono azul marino/índigo (Color(0xFF1E3A8A)) en modo claro y azul brillante (#60A5FA) en modo oscuro.
 *    - Tamaño: 22.dp.
 * 3. Campo de Texto y Placeholder:
 *    - Tipografía: Sans-serif limpia de peso regular, tamaño 16.sp.
 *    - Placeholder: "Buscar en notas..." en gris medio neutro (Color(0xFF6B7280)).
 *    - Comportamiento: BasicTextField de una sola línea, cursor en color azul a juego, botón de limpieza en el extremo derecho cuando hay texto, testTag "search_bar_input".
 */
@Composable
fun AppleSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar en notas...",
) {
    val isDark = isSystemInDarkTheme()

    // Colores según especificaciones
    val backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val searchIconColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1E3A8A)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val placeholderColor = Color(0xFF6B7280)
    val borderStroke = if (!isDark) BorderStroke(1.dp, Color(0xFFE5E5EA)) else null

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp)
            .shadow(elevation = 3.dp, shape = CircleShape),
        shape = CircleShape,
        color = backgroundColor,
        border = borderStroke,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Padding inicial de 18.dp antes del ícono de búsqueda
            Spacer(modifier = Modifier.width(18.dp))

            // Ícono de búsqueda
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = searchIconColor,
                modifier = Modifier.size(22.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Campo de texto y placeholder
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
                    onValueChange = onQueryChange,
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

            // Botón de limpieza si hay texto escrito
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
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
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Apple Search Bar - Light")
@Composable
private fun AppleSearchBarLightPreview() {
    AppleSearchBar(
        query = "",
        onQueryChange = {},
    )
}

@Preview(showBackground = true, name = "Apple Search Bar - Light with text")
@Composable
private fun AppleSearchBarLightWithTextPreview() {
    AppleSearchBar(
        query = "Nota de reunión",
        onQueryChange = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Apple Search Bar - Dark")
@Composable
private fun AppleSearchBarDarkPreview() {
    AppleSearchBar(
        query = "Ideas",
        onQueryChange = {},
    )
}
