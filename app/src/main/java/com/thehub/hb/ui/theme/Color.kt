package com.thehub.hb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
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

// Palette Premium Dark — The Hub
val HubDarkPalette = HubPalette(
    black = Color(0xFF0B0B0F),
    surfaceDark = Color(0xFF0B0B0F),
    card = Color(0xFF17171C),
    surfaceElevated = Color(0xFF1E1E22),

    border = Color(0xFF25252D),
    borderLight = Color.White.copy(alpha = 0.06f),

    darkGray = Color(0xFF25252D),
    muted = Color(0xFF9CA3AF),
    secondary = Color(0xFF9CA3AF),

    lightGray = Color(0xFFEDEDED),
    white = Color(0xFFFFFFFF),

    error = Color(0xFFFF5A5A),
    success = Color(0xFF22C55E),

    isGlass = false,
    isLight = false
)

// Couleurs de marque et tokens globaux
val HubViolet = Color(0xFF8B5CF6)
val HubBlue = Color(0xFF3B82F6)

val HubBackground = Color(0xFF0B0B0F)
val HubSurface = Color(0xFF17171C)
val HubSurfaceElevatedColor = Color(0xFF1E1E22)
val HubOutline = Color(0xFF25252D)

val HubTextPrimary = Color(0xFFEDEDED)
val HubTextSecondary = Color(0xFF9CA3AF)

val HubNavigationSurface = Color(0xE61E1E22) // #1E1E22 à ~90% d'opacité

fun hubPrimaryGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            HubViolet,
            HubBlue
        )
    )
}

// Palette Claire : Toile lumineuse épurée, cartes blanches, texte sombre contrasté
val HubLightPalette = HubPalette(
    black = Color(0xFFF6F7F9), // Fond principal
    surfaceDark = Color(0xFFFFFFFF), // Surface élevée
    card = Color(0xFFFFFFFF), // Cartes de contenu
    surfaceElevated = Color(0xFFECEEF2),
    border = Color(0xFFE2E4E9),
    borderLight = Color(0xFFEAEBED),
    darkGray = Color(0xFFCFD3DA),
    muted = Color(0xFF868B96),
    secondary = Color(0xFF555B67),
    lightGray = Color(0xFF333742),
    white = Color(0xFF111418), // Texte et icônes principaux contrastés
    error = Color(0xFFE53935),
    success = Color(0xFF2E7D32),
    isGlass = false,
    isLight = true
)

// Palette Glass : Effet verre dépoli aéro avec reflets translucides et lueur moderne
val HubGlassPalette = HubPalette(
    black = Color(0xFF090D18), // Fond saphir nuit cosmique
    surfaceDark = Color(0xFF0F172A),
    card = Color(0x2EFFFFFF), // 18% verre dépoli translucide
    surfaceElevated = Color(0x40FFFFFF), // 25% verre dépoli surélevé
    border = Color(0x38FFFFFF), // 22% bordure translucide lumineuse
    borderLight = Color(0x4DFFFFFF), // 30% reflet de lumière sur les contours
    darkGray = Color(0x507C94B2),
    muted = Color(0xFF8DA2BC),
    secondary = Color(0xFFB4C8DF),
    lightGray = Color(0xFFDCE8F7),
    white = Color(0xFFFFFFFF), // Texte et icônes d'un blanc pur éclatant
    error = Color(0xFFFF6B6B),
    success = Color(0xFF4ECCA3),
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
