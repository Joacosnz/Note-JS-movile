package com.example.ui.components

import androidx.compose.foundation.Canvas
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.PaperStyle

@Composable
fun PaperBackground(
    paperStyle: PaperStyle,
    modifier: Modifier = Modifier,
    lineHeightPx: Float = -1f,
    startYOffsetPx: Float = 0f,
    headerMarginPx: Float = 0f,
    isDark: Boolean = isAppInDarkTheme()
) {
    if (paperStyle == PaperStyle.BLANK) return

    val lineColor = if (isDark) Color(0x25FFFFFF) else Color(0x1F000000)
    val dotColor = if (isDark) Color(0x35FFFFFF) else Color(0x2B000000)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Use the exact line height of the text for the step, or fallback to a default
        val step = if (lineHeightPx > 0f) lineHeightPx else (32f * density)
        val headerMargin = if (headerMarginPx > 0f) headerMarginPx else 120.dp.toPx()

        when (paperStyle) {
            PaperStyle.RULED -> {
                var y = headerMargin + startYOffsetPx
                while (y < height) {
                    if (y >= headerMargin) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f * density
                        )
                    }
                    y += step
                }
            }
            PaperStyle.GRID -> {
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, headerMargin),
                        end = Offset(x, height),
                        strokeWidth = 1f * density
                    )
                    x += step
                }
                var y = headerMargin + startYOffsetPx
                while (y < height) {
                    if (y >= headerMargin) {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f * density
                        )
                    }
                    y += step
                }
            }
            PaperStyle.DOTS -> {
                var x = step / 2f
                while (x < width) {
                    var y = headerMargin + startYOffsetPx
                    while (y < height) {
                        if (y >= headerMargin) {
                            drawCircle(
                                color = dotColor,
                                radius = 1.6f * density,
                                center = Offset(x, y)
                            )
                        }
                        y += step
                    }
                    x += step
                }
            }
            PaperStyle.BLANK -> Unit
        }
    }
}
