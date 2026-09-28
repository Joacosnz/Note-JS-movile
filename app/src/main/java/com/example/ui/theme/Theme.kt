package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppleNotesAmberDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3D2800),
    onPrimaryContainer = AppleNotesAmberDark,
    secondary = AppleNotesAmberDark,
    background = AppleSystemBgDark,
    surface = AppleCardBgDark,
    surfaceVariant = AppleSecondaryCardDark,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = AppleTextSecondaryDark,
    outline = AppleSeparatorDark,
    outlineVariant = AppleSeparatorDark
)

private val LightColorScheme = lightColorScheme(
    primary = AppleNotesAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = AppleNotesAmber,
    background = AppleSystemBgLight,
    surface = AppleCardBgLight,
    surfaceVariant = AppleSearchBarLight,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = AppleTextSecondaryLight,
    outline = AppleSeparatorLight,
    outlineVariant = AppleSeparatorLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
