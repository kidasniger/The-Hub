package com.thehub.hb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs adaptable selon le mode de thème (Sombre, Clair, Effet Glass).
 */
data class HubPalette(
    val black: Color,
    val surfaceDark: Color,
    val card: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderLight: Color,
    val darkGray: Color,
    val muted: Color,
    val secondary: Color,
    val lightGray: Color,
    val white: Color,
    val error: Color,
    val success: Color,
    val isGlass: Boolean = false,
    val isLight: Boolean = false
)

// Palette Aurora Glass — The Hub (conforme à la maquette glassmorphism)
// Nuit aurora : base indigo profond avec halos violet/bleu, cartes translucides.
val AuroraNightBase = Color(0xFF0E0B28)
val AuroraNightTop = Color(0xFF171045)
val AuroraNightBottom = Color(0xFF0B0920)
val AuroraGlowViolet = Color(0xFF8B5CF6)
val AuroraGlowBlue = Color(0xFF3B82F6)
val AuroraGlowPink = Color(0xFFEC4899)

// Jour « verre lait » : dégradé crée/violet très doux avec orbes pastel.
val AuroraLightTop = Color(0xFFEEF0FF)
val AuroraLightBottom = Color(0xFFFDF3FF)

// Palette Aurora Glass sombre (défaut, identique à la maquette Dark glass)
val HubDarkPalette = HubPalette(
    black = AuroraNightBase,
    surfaceDark = Color(0xFF140F33),
    card = Color(0x16FFFFFF),        // verre ~9 % — translucide sur l'aurora
    surfaceElevated = Color(0x23FFFFFF), // verre ~14 %

    border = Color(0x29FFFFFF),      // liseré ~16 %
    borderLight = Color(0x52FFFFFF), // reflet haut de carte ~32 %

    darkGray = Color(0xFF241E4E),
    muted = Color(0xFFA8A3CC),
    secondary = Color(0xFFCFCBEF),

    lightGray = Color(0xFFEDEBFA),
    white = Color(0xFFFFFFFF),

    error = Color(0xFFFF6B81),
    success = Color(0xFF34D399),

    isGlass = false,
    isLight = false
)

// Couleurs de marque et tokens globaux
val HubViolet = Color(0xFF8B5CF6)
val HubBlue = Color(0xFF3B82F6)

val HubBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val HubSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val HubSurfaceElevatedColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val HubOutline: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val HubTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val HubTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val HubNavigationSurface: Color
    @Composable get() = if (LocalHubColors.current.isLight) {
        Color(0xE6FFFFFF)
    } else {
        Color(0xE61E1E22)
    }

fun hubPrimaryGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            HubViolet,
            HubBlue
        )
    )
}

// Palette Claire « Light glass » — fond lait, cartes blanches translucides
val HubLightPalette = HubPalette(
    black = Color(0xFFF2F1FC),
    surfaceDark = Color(0xFFF2F1FC),
    card = Color(0xC9FFFFFF),        // blanc ~79 % sur dégradé pastel
    surfaceElevated = Color(0xE6FFFFFF),

    border = Color(0xFFE2DEF4),
    borderLight = Color(0xFFFFFFFF),

    darkGray = Color(0xFFE2DEF4),
    muted = Color(0xFF6E6A96),
    secondary = Color(0xFF565180),

    lightGray = Color(0xFF3B3663),
    white = Color(0xFF241F52),       // « white » = couleur de texte principale en clair

    error = Color(0xFFE11D48),
    success = Color(0xFF059669),

    isGlass = false,
    isLight = true
)

// Palette Glass : aurora nuit maximale — verre fin, halos bien visibles
val HubGlassPalette = HubPalette(
    black = Color(0xB30E0B28), // base nuit ~70 % : laisse transparaître les halos
    surfaceDark = Color(0x80140F33),
    card = Color(0x12FFFFFF),        // verre ~7 %
    surfaceElevated = Color(0x1FFFFFFF), // verre ~12 %

    border = Color(0x2EFFFFFF),
    borderLight = Color(0x59FFFFFF),

    darkGray = Color(0x3A8C86C4),
    muted = Color(0xFFB4AFD6),
    secondary = Color(0xFFD9D5F2),

    lightGray = Color(0xFFEDEBFA),
    white = Color(0xFFFFFFFF),

    error = Color(0xFFFF7A8E),
    success = Color(0xFF4ADE80),

    isGlass = true,
    isLight = false
)

val LocalHubColors = staticCompositionLocalOf { HubDarkPalette }

// Couleurs dynamiques accessibles dans tous les Composables existants
val HubBlack: Color
    @Composable get() = LocalHubColors.current.black

val HubSurfaceDark: Color
    @Composable get() = LocalHubColors.current.surfaceDark

val HubCard: Color
    @Composable get() = LocalHubColors.current.card

val HubSurfaceElevated: Color
    @Composable get() = LocalHubColors.current.surfaceElevated

val HubBorder: Color
    @Composable get() = LocalHubColors.current.border

val HubBorderLight: Color
    @Composable get() = LocalHubColors.current.borderLight

val HubDarkGray: Color
    @Composable get() = LocalHubColors.current.darkGray

val HubMuted: Color
    @Composable get() = LocalHubColors.current.muted

val HubSecondary: Color
    @Composable get() = LocalHubColors.current.secondary

val HubLightGray: Color
    @Composable get() = LocalHubColors.current.lightGray

val HubWhite: Color
    @Composable get() = LocalHubColors.current.white

val HubError: Color
    @Composable get() = LocalHubColors.current.error

val HubSuccess: Color
    @Composable get() = LocalHubColors.current.success
