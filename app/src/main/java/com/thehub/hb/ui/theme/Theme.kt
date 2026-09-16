package com.thehub.hb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun createDarkColorScheme(palette: HubPalette) = darkColorScheme(
    primary = palette.white,
    onPrimary = palette.black,
    primaryContainer = palette.surfaceElevated,
    onPrimaryContainer = palette.white,
    secondary = palette.lightGray,
    onSecondary = palette.black,
    background = palette.surfaceDark,
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
    outline = palette.border,
    outlineVariant = palette.darkGray,
    error = palette.error,
    onError = palette.white
)

private fun createLightColorScheme(palette: HubPalette) = lightColorScheme(
    primary = palette.white,
    onPrimary = palette.surfaceDark,
    primaryContainer = palette.surfaceElevated,
    onPrimaryContainer = palette.white,
    secondary = palette.secondary,
    onSecondary = palette.surfaceDark,
    background = palette.black,
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
    outline = palette.border,
    outlineVariant = palette.darkGray,
    error = palette.error,
    onError = palette.surfaceDark
)

@Composable
fun TheHubTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val effectiveMode = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) AppThemeMode.DARK else AppThemeMode.LIGHT
        else -> themeMode
    }

    val palette = when (effectiveMode) {
        AppThemeMode.LIGHT -> HubLightPalette
        AppThemeMode.GLASS -> HubGlassPalette
        else -> HubDarkPalette
    }

    val colorScheme = if (palette.isLight) {
        createLightColorScheme(palette)
    } else {
        createDarkColorScheme(palette)
    }

    CompositionLocalProvider(LocalHubColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

