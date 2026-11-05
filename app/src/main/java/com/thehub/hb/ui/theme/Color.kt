package com.thehub.hb.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Palette de couleurs adaptable selon le mode de thème (Sombre, Clair, Effet Glass).
 *
 * Les champs d'origine sont conservés à l'identique (aucune rupture d'API) ;
 * les champs ajoutés portent les tokens EXACTS de la maquette « Aurora Glass »
 * (assets/glass.css, thèmes dark et light).
 *
 * Rappels de conversion CSS → Compose :
 *  - rgba(255,255,255,.13)  → Color(0x21FFFFFF)   (alpha .13 × 255 = 33 = 0x21)
 *  - color-mix(in srgb, X n%, transparent) → X.copy(alpha = n / 100f)
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

    // ---- Tokens Aurora Glass ajoutés (aucun champ existant retiré) ----
    // Fond écran : stops du dégradé 160° de .screen
    val bg0: Color = Color.Unspecified,
    val bg1: Color = Color.Unspecified,
    val bg2: Color = Color.Unspecified,
    // Trois lueurs de l'aurore (violet haut-droite, bleu gauche, rose bas-centre)
    val glow1: Color = Color.Unspecified,
    val glow2: Color = Color.Unspecified,
    val glow3: Color = Color.Unspecified,
    // Surfaces de verre : --glass, --glass-strong, --glass-soft
    val glass: Color = Color.Unspecified,
    val glassStrong: Color = Color.Unspecified,
    val glassSoft: Color = Color.Unspecified,
    // Contours : --stroke, --stroke-soft, --stroke-strong
    val stroke: Color = Color.Unspecified,
    val strokeSoft: Color = Color.Unspecified,
    val strokeStrong: Color = Color.Unspecified,
    // Reflet interne haut : --hl
    val highlight: Color = Color.Unspecified,
    // Textes : --text, --text-2, --text-3
    val textPrimary: Color = Color.Unspecified,
    val textSecondary: Color = Color.Unspecified,
    val textTertiary: Color = Color.Unspecified,
    // Accents UI : --accent, --accent-2
    val accent: Color = Color.Unspecified,
    val accent2: Color = Color.Unspecified,
    // Sémantiques manquantes : --warning, --info
    val warning: Color = Color.Unspecified,
    val info: Color = Color.Unspecified,
    // Surfaces utilitaires : --chip, --field, --scrim
    val chip: Color = Color.Unspecified,
    val field: Color = Color.Unspecified,
    val scrim: Color = Color.Unspecified,
    // Ombres : --shadow-sm, --fab-glow
    val shadowColor: Color = Color.Unspecified,
    val fabGlow: Color = Color.Unspecified,

    val isGlass: Boolean = false,
    val isLight: Boolean = false
)

// ---------------------------------------------------------------------------
// Constantes Aurora — valeurs EXACTES de glass.css
// ---------------------------------------------------------------------------

// Fond « aurore » sombre : stops du dégradé 160° de .screen (#06060F → #0E0B28 55 % → #120F33)
val AuroraNightTop = Color(0xFF06060F)     // --bg0
val AuroraNightBase = Color(0xFF0E0B28)    // --bg1
val AuroraNightBottom = Color(0xFF120F33)  // --bg2

// Fond « verre lait » clair : #EEF0FF → #FDF3FF 55 % → #EAF4FF
val AuroraLightTop = Color(0xFFEEF0FF)     // --bg0 clair
val AuroraLightMid = Color(0xFFFDF3FF)     // --bg1 clair
val AuroraLightBottom = Color(0xFFEAF4FF)  // --bg2 clair

// Lueurs de l'aurore (teintes pures ; les alphas exacts sont portés par les palettes)
val AuroraGlowViolet = Color(0xFF8B5CF6)   // --glow1
val AuroraGlowBlue = Color(0xFF3B82F6)     // --glow2
val AuroraGlowPink = Color(0xFFEC4899)     // --glow3

// Dégradé de marque --grad-accent : linear-gradient(135deg, #8B5CF6, #3B82F6)
val HubViolet = Color(0xFF8B5CF6)
val HubBlue = Color(0xFF3B82F6)

// Palette Dark glass (glass.css html[data-theme="dark"]) — tokens exacts
val HubDarkPalette = HubPalette(
    black = AuroraNightTop,                 // fond le plus profond
    surfaceDark = AuroraNightBase,
    card = Color(0x10FFFFFF),               // --glass rgba(255,255,255,.062)
    surfaceElevated = Color(0x1AFFFFFF),    // --glass-strong rgba(255,255,255,.10)

    border = Color(0x21FFFFFF),             // --stroke rgba(255,255,255,.13)
    borderLight = Color(0x38FFFFFF),        // --stroke-strong rgba(255,255,255,.22)

    darkGray = Color(0xFF241E4E),
    muted = Color(0x6BE4E1FF),              // --text-3 rgba(228,225,255,.42)
    secondary = Color(0xADE4E1FF),          // --text-2 rgba(228,225,255,.68)

    lightGray = Color(0xFFEDEBFA),
    white = Color(0xFFF5F3FF),              // --text #F5F3FF

    error = Color(0xFFFF5A7E),              // --danger #FF5A7E
    success = Color(0xFF34D399),            // --success #34D399

    bg0 = AuroraNightTop,
    bg1 = AuroraNightBase,
    bg2 = AuroraNightBottom,
    glow1 = Color(0x858B5CF6),              // rgba(139,92,246,.52)
    glow2 = Color(0x6B3B82F6),              // rgba(59,130,246,.42)
    glow3 = Color(0x2EEC4899),              // rgba(236,72,153,.18)

    glass = Color(0x10FFFFFF),
    glassStrong = Color(0x1AFFFFFF),
    glassSoft = Color(0x0AFFFFFF),          // --glass-soft rgba(255,255,255,.038)

    stroke = Color(0x21FFFFFF),
    strokeSoft = Color(0x13FFFFFF),         // rgba(255,255,255,.075)
    strokeStrong = Color(0x38FFFFFF),

    highlight = Color(0x29FFFFFF),          // --hl rgba(255,255,255,.16)

    textPrimary = Color(0xFFF5F3FF),
    textSecondary = Color(0xADE4E1FF),
    textTertiary = Color(0x6BE4E1FF),

    accent = Color(0xFFA78BFA),             // --accent #A78BFA
    accent2 = Color(0xFF60A5FA),            // --accent-2 #60A5FA

    warning = Color(0xFFFBBF24),            // --warning #FBBF24
    info = Color(0xFF60A5FA),               // --info #60A5FA

    chip = Color(0x12FFFFFF),               // --chip rgba(255,255,255,.07)
    field = Color(0x0EFFFFFF),              // --field rgba(255,255,255,.055)
    scrim = Color(0x9E04030E),              // --scrim rgba(4,3,14,.62)

    shadowColor = Color(0x6102020C),        // --shadow-sm 0 10px 26px rgba(2,2,12,.38)
    fabGlow = Color(0x807C5CFF),            // --fab-glow 0 14px 34px rgba(124,92,255,.5)

    isGlass = false,
    isLight = false
)

// Palette Glass — mêmes tokens que Dark glass, drapeau glass actif
val HubGlassPalette = HubDarkPalette.copy(isGlass = true)

// Palette Light glass (glass.css html[data-theme="light"]) — tokens exacts
val HubLightPalette = HubPalette(
    black = AuroraLightTop,
    surfaceDark = AuroraLightMid,
    card = Color(0x8FFFFFFF),               // --glass rgba(255,255,255,.56)
    surfaceElevated = Color(0xBDFFFFFF),    // --glass-strong rgba(255,255,255,.74)

    border = Color(0xD9FFFFFF),             // --stroke rgba(255,255,255,.85)
    borderLight = Color(0x428174FF),        // --stroke-strong rgba(129,116,255,.26)

    darkGray = Color(0xFFE2DEF4),
    muted = Color(0x66262056),              // --text-3 rgba(38,32,86,.4)
    secondary = Color(0xA8262056),          // --text-2 rgba(38,32,86,.66)

    lightGray = Color(0xFF3B3663),
    white = Color(0xFF241F52),              // --text #241F52

    error = Color(0xFFE5326B),              // --danger clair
    success = Color(0xFF0EA573),            // --success clair

    bg0 = AuroraLightTop,
    bg1 = AuroraLightMid,
    bg2 = AuroraLightBottom,
    glow1 = Color(0x73A78BFA),              // rgba(167,139,250,.45)
    glow2 = Color(0x6B60A5FA),              // rgba(96,165,250,.42)
    glow3 = Color(0x38F472B2),              // rgba(244,114,182,.22)

    glass = Color(0x8FFFFFFF),
    glassStrong = Color(0xBDFFFFFF),
    glassSoft = Color(0x61FFFFFF),          // rgba(255,255,255,.38)

    stroke = Color(0xD9FFFFFF),
    strokeSoft = Color(0x248174FF),         // rgba(129,116,255,.14)
    strokeStrong = Color(0x428174FF),

    highlight = Color(0xE6FFFFFF),          // --hl rgba(255,255,255,.9)

    textPrimary = Color(0xFF241F52),
    textSecondary = Color(0xA8262056),
    textTertiary = Color(0x66262056),

    accent = Color(0xFF7C4DFF),             // --accent clair #7C4DFF
    accent2 = Color(0xFF2563EB),            // --accent-2 clair #2563EB

    warning = Color(0xFFD97706),            // --warning clair
    info = Color(0xFF2563EB),               // --info clair

    chip = Color(0x99FFFFFF),               // --chip rgba(255,255,255,.6)
    field = Color(0x8CFFFFFF),              // --field rgba(255,255,255,.55)
    scrim = Color(0x59262056),              // --scrim rgba(38,32,86,.35)

    shadowColor = Color(0x246052DC),        // --shadow-sm clair rgba(96,82,220,.14)
    fabGlow = Color(0x667C5CFF),            // --fab-glow clair rgba(124,92,255,.4)

    isGlass = false,
    isLight = true
)

val LocalHubColors = staticCompositionLocalOf { HubDarkPalette }

// ---------------------------------------------------------------------------
// Accesseurs dynamiques — champs d'origine (inchangés)
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Accesseurs dynamiques — nouveaux tokens Aurora Glass
// ---------------------------------------------------------------------------

val HubBg0: Color
    @Composable get() = LocalHubColors.current.bg0

val HubBg1: Color
    @Composable get() = LocalHubColors.current.bg1

val HubBg2: Color
    @Composable get() = LocalHubColors.current.bg2

val HubGlow1: Color
    @Composable get() = LocalHubColors.current.glow1

val HubGlow2: Color
    @Composable get() = LocalHubColors.current.glow2

val HubGlow3: Color
    @Composable get() = LocalHubColors.current.glow3

val HubGlass: Color
    @Composable get() = LocalHubColors.current.glass

val HubGlassStrong: Color
    @Composable get() = LocalHubColors.current.glassStrong

val HubGlassSoft: Color
    @Composable get() = LocalHubColors.current.glassSoft

val HubStroke: Color
    @Composable get() = LocalHubColors.current.stroke

val HubStrokeSoft: Color
    @Composable get() = LocalHubColors.current.strokeSoft

val HubStrokeStrong: Color
    @Composable get() = LocalHubColors.current.strokeStrong

val HubHighlight: Color
    @Composable get() = LocalHubColors.current.highlight

val HubTextPrimary: Color
    @Composable get() = LocalHubColors.current.textPrimary

val HubTextSecondary: Color
    @Composable get() = LocalHubColors.current.textSecondary

val HubTextTertiary: Color
    @Composable get() = LocalHubColors.current.textTertiary

val HubAccent: Color
    @Composable get() = LocalHubColors.current.accent

val HubAccent2: Color
    @Composable get() = LocalHubColors.current.accent2

val HubWarning: Color
    @Composable get() = LocalHubColors.current.warning

val HubInfo: Color
    @Composable get() = LocalHubColors.current.info

val HubChip: Color
    @Composable get() = LocalHubColors.current.chip

val HubField: Color
    @Composable get() = LocalHubColors.current.field

val HubScrim: Color
    @Composable get() = LocalHubColors.current.scrim

val HubShadowColor: Color
    @Composable get() = LocalHubColors.current.shadowColor

val HubFabGlow: Color
    @Composable get() = LocalHubColors.current.fabGlow

// Couleurs dynamiques existantes (Material) — inchangées
val HubBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val HubSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val HubSurfaceElevatedColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val HubOutline: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val HubTextPrimaryFromScheme: Color
    @Composable get() = MaterialTheme.colorScheme.onBackground

val HubNavigationSurface: Color
    @Composable get() = if (LocalHubColors.current.isLight) {
        Color(0xBDFFFFFF)
    } else {
        Color(0x10FFFFFF)
    }

/**
 * Dégradé de marque de la maquette : linear-gradient(135deg, #8B5CF6, #3B82F6).
 * Brush.linearGradient par défaut part du coin haut-gauche vers le bas-droite,
 * soit exactement 135° en CSS.
 */
fun hubPrimaryGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            HubViolet,
            HubBlue
        )
    )
}
