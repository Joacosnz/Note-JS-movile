package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private var _highlighterIcon: ImageVector? = null

/**
 * Ícono vectorial oficial de resaltador (Highlighter Marker) estilo punta biselada con trazo de texto.
 */
val HighlighterIcon: ImageVector
    get() {
        if (_highlighterIcon != null) {
            return _highlighterIcon!!
        }
        _highlighterIcon = ImageVector.Builder(
            name = "Highlighter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Tapa / Extremo superior del marcador
            path(fill = SolidColor(Color.White)) {
                moveTo(18.8f, 3.2f)
                curveTo(19.6f, 2.4f, 20.9f, 2.4f, 21.7f, 3.2f)
                curveTo(22.5f, 4.0f, 22.5f, 5.3f, 21.7f, 6.1f)
                lineTo(20.2f, 7.6f)
                lineTo(17.3f, 4.7f)
                close()
            }
            // Cuerpo ancho del resaltador (barrel)
            path(fill = SolidColor(Color.White)) {
                moveTo(16.3f, 5.7f)
                lineTo(19.2f, 8.6f)
                lineTo(14.7f, 13.1f)
                lineTo(11.8f, 10.2f)
                close()
            }
            // Anillo o cuello de agarre
            path(fill = SolidColor(Color.White)) {
                moveTo(10.8f, 11.2f)
                lineTo(13.7f, 14.1f)
                lineTo(12.5f, 15.3f)
                lineTo(9.6f, 12.4f)
                close()
            }
            // Punta biselada característica del resaltador
            path(fill = SolidColor(Color.White)) {
                moveTo(8.8f, 13.2f)
                lineTo(11.7f, 16.1f)
                lineTo(7.7f, 18.5f)
                lineTo(4.5f, 18.5f)
                lineTo(4.5f, 15.3f)
                close()
            }
            // Trazo ancho de resaltado bajo la punta
            path(fill = SolidColor(Color.White)) {
                moveTo(2.0f, 20.5f)
                curveTo(2.0f, 19.7f, 2.7f, 19.0f, 3.5f, 19.0f)
                lineTo(14.0f, 19.0f)
                curveTo(14.8f, 19.0f, 15.5f, 19.7f, 15.5f, 20.5f)
                curveTo(15.5f, 21.3f, 14.8f, 22.0f, 14.0f, 22.0f)
                lineTo(3.5f, 22.0f)
                curveTo(2.7f, 22.0f, 2.0f, 21.3f, 2.0f, 20.5f)
                close()
            }
        }.build()
        return _highlighterIcon!!
    }

val NoteJsHighlighterIcon: ImageVector get() = HighlighterIcon
