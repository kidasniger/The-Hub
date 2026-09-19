package com.thehub.hb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Définition des formes globales Material 3
val HubShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(44.dp),
    extraLarge = RoundedCornerShape(44.dp)
)

private fun createDarkColorScheme(palette: HubPalette) = darkColorScheme(
    primary = HubViolet,
    onPrimary = Color.White,
    primaryContainer = palette.surfaceElevated,
    onPrimaryContainer = palette.white,
    secondary = HubBlue,
    onSecondary = Color.White,
    tertiary = HubViolet,
    onTertiary = Color.White,
    background = palette.black,
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
    outline = palette.border,
    outlineVariant = palette.borderLight,
    error = palette.error,
    onError = Color.White
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

    CompositionLocalProvider(
        LocalHubColors provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HubShapes,
            content = content
        )
    }
}
