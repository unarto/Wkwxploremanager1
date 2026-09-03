package com.wakwau.xplore.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkXPloreColors.primary,
    onPrimary = DarkXPloreColors.onPrimary,
    primaryContainer = DarkXPloreColors.primaryContainer,
    onPrimaryContainer = DarkXPloreColors.onPrimaryContainer,
    secondary = DarkXPloreColors.secondary,
    onSecondary = DarkXPloreColors.onSecondary,
    tertiary = DarkXPloreColors.checkMarkSelected,
    background = DarkXPloreColors.background,
    onBackground = DarkXPloreColors.onBackground,
    surface = DarkXPloreColors.surface,
    onSurface = DarkXPloreColors.onSurface,
    surfaceVariant = DarkXPloreColors.surfaceElevated,
    onSurfaceVariant = DarkXPloreColors.textSecondary,
    outline = DarkXPloreColors.border,
    error = Color(0xFFE53935),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightXPloreColors.primary,
    onPrimary = LightXPloreColors.onPrimary,
    primaryContainer = LightXPloreColors.primaryContainer,
    onPrimaryContainer = LightXPloreColors.onPrimaryContainer,
    secondary = LightXPloreColors.secondary,
    onSecondary = LightXPloreColors.onSecondary,
    tertiary = LightXPloreColors.checkMarkSelected,
    background = LightXPloreColors.background,
    onBackground = LightXPloreColors.onBackground,
    surface = LightXPloreColors.surface,
    onSurface = LightXPloreColors.onSurface,
    surfaceVariant = LightXPloreColors.surfaceElevated,
    onSurfaceVariant = LightXPloreColors.textSecondary,
    outline = LightXPloreColors.border,
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun WKWXploreTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val xploreColors = if (darkTheme) DarkXPloreColors else LightXPloreColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = xploreColors.topBarContainer.toArgb()
                window.navigationBarColor = xploreColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalXPloreColors provides xploreColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
