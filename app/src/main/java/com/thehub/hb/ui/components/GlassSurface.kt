package com.thehub.hb.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.thehub.hb.ui.theme.LocalHubColors

/**
 * Variantes de surface de verre de la maquette :
 *  - [GlassVariant.Card]   : .glass        — verre 6 %, flou 22 px, contour --stroke, reflet interne, ombre douce ;
 *  - [GlassVariant.Strong] : verre renforcé --glass-strong (cartes interactives, navbar) ;
 *  - [GlassVariant.Soft]   : .glass-soft   — verre 3.8 %, contour --stroke-soft, sans reflet ni ombre ;
 *  - [GlassVariant.Pill]   : .glass en pilule (linkback, composeur) — rayon 100 %, sans ombre.
 */
enum class GlassVariant {
    Card,
    Strong,
    Soft,
    Pill
}

/**
 * Surface de verre réutilisable « Aurora Glass ».
 *
 * - Android 12+ (API 31) : flou RÉEL du calque arrière via Modifier.blur
 *   (RenderEffect 22 dp), technique prescrite par le README de la maquette
 *   (« Modifier.blur() sur un calque arrière + carte alpha 6 % ») : la surface
 *   duplique le dégradé d'aurore derrière sa teinte translucide.
 * - Android < 12 : repli TRANSLUCIDE sans flou — la teinte est renforcée par
 *   un mélange vers --bg1 (lerp 65 %) pour préserver la lisibilité.
 *
 * Chaque surface porte : contour 1 dp --stroke, reflet interne haut de 1 dp
 * (--hl) et ombre douce --shadow-sm (variantes Card/Strong uniquement).
 *
 * Aucune dépendance nouvelle : Compose stable uniquement, minSdk 24 respecté.
 *
 * @param shape       forme de la surface (défaut : rayon --r-lg 24 dp, pilule pour Pill)
 * @param variant     Card / Strong / Soft / Pill
 * @param blurEnabled false pour forcer le repli translucide sans flou (test, perf)
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    variant: GlassVariant = GlassVariant.Card,
    blurEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = LocalHubColors.current
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useBlur = blurEnabled && canBlur

    val resolvedShape: Shape = remember(shape, variant) {
        shape ?: when (variant) {
            GlassVariant.Pill -> RoundedCornerShape(percent = 50)
            else -> RoundedCornerShape(24.dp) // --r-lg
        }
    }

    val tint = when (variant) {
        GlassVariant.Card -> palette.glass
        GlassVariant.Strong -> palette.glassStrong
        GlassVariant.Soft -> palette.glassSoft
        GlassVariant.Pill -> palette.glass
    }
    val strokeColor = when (variant) {
        GlassVariant.Soft -> palette.strokeSoft
        else -> palette.stroke
    }
    val shadowEnabled = variant == GlassVariant.Card || variant == GlassVariant.Strong
    val highlightEnabled = variant != GlassVariant.Soft

    // Repli translucide sans flou : on renforce la teinte vers le fond bg1.
    val fallbackTint = remember(tint, palette.bg1) {
        lerp(tint, palette.bg1, 0.65f)
    }

    Box(
        modifier = modifier
            .then(
                if (shadowEnabled) {
                    Modifier.shadow(
                        elevation = 10.dp, // --shadow-sm 0 10px 26px
                        shape = resolvedShape,
                        clip = false,
                        ambientColor = palette.shadowColor,
                        spotColor = palette.shadowColor
                    )
                } else {
                    Modifier
                }
            )
            .clip(resolvedShape)
    ) {
        if (useBlur) {
            // Calque arrière flouté (RenderEffect via Modifier.blur, API 31+) :
            // copie du dégradé d'aurore, comme si la surface « regardait » le fond.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(22.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(palette.bg0, palette.bg1, palette.bg2)
                        )
                    )
            )
        }
        // Teinte de verre + reflet interne haut de 1 dp + contour 1 dp.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (useBlur) tint else fallbackTint
                )
                .then(
                    if (highlightEnabled) {
                        Modifier.drawBehind {
                            drawRect(
                                color = palette.highlight,
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, 1.dp.toPx())
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .border(
                    width = 1.dp,
                    color = strokeColor,
                    shape = resolvedShape
                )
        )
        content()
    }
}

/**
 * Carte de verre standard (r-lg 24 dp, variante Card) — raccourci réutilisable
 * par tous les écrans « Aurora Glass ».
 */
@Composable
fun HubGlassCard(
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.Card,
    blurEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = null,
        variant = variant,
        blurEnabled = blurEnabled,
        content = content
    )
}
