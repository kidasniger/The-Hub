package com.thehub.hb.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thehub.hb.ui.components.AuroraBackground

// Formes globales Material 3 — rayons EXACTS de la maquette glass.css :
// --r-xs 10 · --r-sm 14 · --r-md 18 · --r-lg 24 · --r-xl 30 (px ≡ dp).
val HubShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private fun createDarkColorScheme(palette: HubPalette) = darkColorScheme(
    primary = HubViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF35256B),
    onPrimaryContainer = palette.textPrimary,
    secondary = HubBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B2C5E),
    onSecondaryContainer = palette.textPrimary,
    tertiary = HubViolet,
    onTertiary = Color.White,
    background = Color.Transparent, // le fond aurora est peint une seule fois par HubThemeBackground
    onBackground = palette.textPrimary,
    surface = palette.card,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.textSecondary,
    surfaceContainer = palette.card,
    surfaceContainerHigh = palette.surfaceElevated,
    surfaceContainerHighest = palette.surfaceElevated,
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
    background = Color.Transparent, // le fond pastel est peint une seule fois par HubThemeBackground
    onBackground = palette.textPrimary,
    surface = palette.card,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.textSecondary,
    surfaceContainer = palette.card,
    surfaceContainerHigh = palette.surfaceElevated,
    surfaceContainerHighest = palette.surfaceElevated,
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

    // AppThemeMode.DARK / AppThemeMode.GLASS → « Dark glass » de la maquette ;
    // AppThemeMode.LIGHT → « Light glass » ; les palettes portent les tokens exacts.
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
 * Arrière-plan global « Aurora Glass », conforme à la maquette (.screen de glass.css) :
 * dégradé de base 160° (#06060F → #0E0B28 55 % → #120F33), trois lueurs radiales
 * elliptiques (violet haut-droite, bleu gauche, rose bas-centre) et grain subtil.
 * Le délégué [AuroraBackground] porte l'implémentation ; la signature publique
 * reste inchangée pour tous les appelants existants.
 */
@Composable
fun HubThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AuroraBackground(modifier = modifier, content = content)
}
