package com.example.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppInDarkTheme(): Boolean = LocalIsDarkTheme.current

private val DarkColorScheme = darkColorScheme(
    primary = AppleNotesAmberDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3D2800),
    onPrimaryContainer = AppleNotesAmberDark,
    secondary = AppleNotesAmberDark,
    background = AppleSystemBgDark,
    surface = AppleCardBgDark,
    surfaceVariant = AppleSecondaryCardDark,
    surfaceTint = Color.Transparent,
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
    surfaceTint = Color.Transparent,
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    val window = context.window
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                    break
                }
                context = context.baseContext
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
