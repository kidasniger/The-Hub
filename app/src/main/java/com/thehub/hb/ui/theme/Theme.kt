package com.thehub.hb.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

// Formes globales Material 3 : arrondies et cohérentes dans les quatre modes.
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
    background = if (palette.isGlass) Color.Transparent else palette.black,
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
    primary = HubViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = HubBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF172554),
    tertiary = HubViolet,
    onTertiary = Color.White,
    background = palette.black,
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
    outline = palette.border,
    outlineVariant = palette.darkGray,
    error = palette.error,
    onError = Color.White
)

@Composable
fun TheHubTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
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

/**
 * Arrière-plan global du thème.
 * Glass ajoute plusieurs halos colorés derrière les surfaces translucides ;
 * sombre et clair restent très sobres pour préserver la lisibilité.
 */
@Composable
fun HubThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalHubColors.current
    val brush = when {
        palette.isGlass -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF08101F),
                Color(0xFF111B31),
                Color(0xFF0A0D18)
            )
        )
        palette.isLight -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF08080B),
                Color(0xFF0D0D13)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}
