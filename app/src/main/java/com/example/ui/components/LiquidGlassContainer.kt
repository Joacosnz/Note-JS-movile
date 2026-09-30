package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contenedor Liquid Glass (Vidrio Líquido) reutilizable para Jetpack Compose.
 *
 * Sigue la anatomía óptica de 5 capas:
 * 1. Desenfoque/Difusión y tinte translúcido reactivo.
 * 2. Adaptabilidad a modo claro y oscuro (isSystemInDarkTheme).
 * 3. Borde refractivo especular con degradado lineal.
 * 4. Brillo superficial y luminancia interior (Sheen).
 * 5. Sombra ambiental de dispersión suave.
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 10.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    glassAlpha: Float = 0.85f,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val glassBgColor = if (isDark) {
        Color(0xFF1E1E22).copy(alpha = glassAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = glassAlpha)
    }

    val specularBorderBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.10f),
                Color(0xFF3F3F46).copy(alpha = 0.50f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.40f),
                Color(0xFFCBD5E1).copy(alpha = 0.60f)
            )
        )
    }

    val sheenBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.20f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.50f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.03f)
            )
        )
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(glassBgColor)
            .border(width = borderWidth, brush = specularBorderBrush, shape = shape),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier.background(sheenBrush),
            contentAlignment = contentAlignment,
            content = content
        )
    }
}
