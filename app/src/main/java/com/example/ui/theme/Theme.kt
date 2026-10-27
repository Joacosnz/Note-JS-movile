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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isAppInDarkTheme(): Boolean = LocalIsDarkTheme.current

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accentDark = NoteJsAmberDark
    val accentLight = NoteJsAmber
    val onAccentDark = if (accentDark.luminance() > 0.45f) Color.Black else Color.White
    val onAccentLight = if (accentLight.luminance() > 0.45f) Color.Black else Color.White

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentDark,
            onPrimary = onAccentDark,
            primaryContainer = accentDark.copy(alpha = 0.22f),
            onPrimaryContainer = accentDark,
            secondary = accentDark,
            background = NoteJsSystemBgDark,
            surface = NoteJsCardBgDark,
            surfaceVariant = NoteJsSecondaryCardDark,
            surfaceTint = Color.Transparent,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = NoteJsTextSecondaryDark,
            outline = NoteJsSeparatorDark,
            outlineVariant = NoteJsSeparatorDark
        )
    } else {
        lightColorScheme(
            primary = accentLight,
            onPrimary = onAccentLight,
            primaryContainer = accentLight.copy(alpha = 0.18f),
            onPrimaryContainer = accentLight,
            secondary = accentLight,
            background = NoteJsSystemBgLight,
            surface = NoteJsCardBgLight,
            surfaceVariant = NoteJsSearchBarLight,
            surfaceTint = Color.Transparent,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = NoteJsTextSecondaryLight,
            outline = NoteJsSeparatorLight,
            outlineVariant = NoteJsSeparatorLight
        )
    }

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
