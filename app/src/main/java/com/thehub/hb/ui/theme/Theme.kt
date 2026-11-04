package com.thehub.hb.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind

// Formes globales Material 3 : arrondies et cohérentes dans les quatre modes.
val HubShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private fun createDarkColorScheme(palette: HubPalette) = darkColorScheme(
    primary = HubViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF35256B),
    onPrimaryContainer = palette.white,
    secondary = HubBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1B2C5E),
    onSecondaryContainer = palette.white,
    tertiary = HubViolet,
    onTertiary = Color.White,
    background = Color.Transparent, // le fond aurora est peint par HubThemeBackground
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
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
    background = Color.Transparent, // le fond pastel est peint par HubThemeBackground
    onBackground = palette.white,
    surface = palette.card,
    onSurface = palette.white,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.secondary,
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
 * Arrière-plan global « Aurora Glass », identique à la maquette :
 * - nuit : base indigo profond + halos radiaux violet / bleu / rose
 * - jour : dégradé crée-violet très doux + orbes pastel
 */
@Composable
fun HubThemeBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalHubColors.current
    val isLight = palette.isLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Base : dégradé vertical
                if (isLight) {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(AuroraLightTop, AuroraLightBottom)
                        )
                    )
                } else {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(AuroraNightTop, AuroraNightBase, AuroraNightBottom)
                        )
                    )
                }

                val w = size.width
                val h = size.height

                if (isLight) {
                    // Orbes pastel : violet en haut à gauche, bleu en bas à droite
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowViolet.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.10f, h * 0.04f),
                            radius = w * 0.75f
                        ),
                        radius = w * 0.75f,
                        center = Offset(w * 0.10f, h * 0.04f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowBlue.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(w * 1.02f, h * 0.38f),
                            radius = w * 0.62f
                        ),
                        radius = w * 0.62f,
                        center = Offset(w * 1.02f, h * 0.38f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowPink.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.72f, h * 1.05f),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.72f, h * 1.05f)
                    )
                } else {
                    // Nuit aurora : halo violet dominant + bleu + touche rose
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowViolet.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.06f, h * 0.00f),
                            radius = w * 0.95f
                        ),
                        radius = w * 0.95f,
                        center = Offset(w * 0.06f, h * 0.00f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowBlue.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(w * 1.05f, h * 0.32f),
                            radius = w * 0.85f
                        ),
                        radius = w * 0.85f,
                        center = Offset(w * 1.05f, h * 0.32f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraGlowPink.copy(alpha = 0.13f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.78f, h * 1.04f),
                            radius = w * 0.60f
                        ),
                        radius = w * 0.60f,
                        center = Offset(w * 0.78f, h * 1.04f)
                    )
                    // Lueur blanche discrète en haut, comme le reflet de la maquette
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.25f, h * -0.05f),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.25f, h * -0.05f)
                    )
                }
            }
    ) {
        content()
    }
}
