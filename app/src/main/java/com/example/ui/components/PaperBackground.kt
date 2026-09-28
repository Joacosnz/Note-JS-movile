package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.model.PaperStyle

@Composable
fun PaperBackground(
    paperStyle: PaperStyle,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    if (paperStyle == PaperStyle.BLANK) return

    val lineColor = if (isDark) Color(0x25FFFFFF) else Color(0x1F000000)
    val dotColor = if (isDark) Color(0x35FFFFFF) else Color(0x2B000000)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (paperStyle) {
            PaperStyle.RULED -> {
                val step = 32f * density
                var y = step
                while (y < height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f * density
                    )
                    y += step
                }
            }
            PaperStyle.GRID -> {
                val step = 28f * density
                var x = step
                while (x < width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f * density
                    )
                    x += step
                }
                var y = step
                while (y < height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f * density
                    )
                    y += step
                }
            }
            PaperStyle.DOTS -> {
                val step = 26f * density
                var x = step
                while (x < width) {
                    var y = step
                    while (y < height) {
                        drawCircle(
                            color = dotColor,
                            radius = 1.6f * density,
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }
            PaperStyle.BLANK -> Unit
        }
    }
}
