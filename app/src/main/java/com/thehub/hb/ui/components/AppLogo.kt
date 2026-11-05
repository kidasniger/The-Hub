package com.thehub.hb.ui.components

/*
 * Marque de l'application « Aurora Glass » — design-system.html (.logo .mark)
 * et écran Splash (01-splash.html) :
 *   carré arrondi au dégradé de marque 135° #8B5CF6 → #3B82F6 (rayon ≈ 1/3 du côté),
 *   glyphe « H » blanc (trait 2.6, extrémités rondes) centré à ~55 % du côté,
 *   reflet radial haut (radial-gradient 70 % 45 % at 30 % 10 %, blanc .55 → transparent 60 %),
 *   lueur violette portée (0 8px 20px rgba(124,92,255,.5), inset 0 1px 0 blanc .35).
 *
 * animated = flottement vertical de la maquette (.float : 5 s ease-in-out, ±9 px
 * à l'échelle 112 px, proportionnel à la taille réelle).
 */

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubViolet

/**
 * Logo animé « Aurora Glass » de The Hub.
 * @param animated true = flottement continu (Splash, chargement) ; false = marque statique (header, badges).
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    animated: Boolean = true,
) {
    val cornerShape = RoundedCornerShape(size / 3)

    val floatTransition = rememberInfiniteTransition(label = "hubLogoFloat")
    val floatPhase = if (animated) {
        floatTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hubLogoFloatPhase"
        ).value
    } else 0f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                // .float de la maquette : -9 px pour un logo de 112 px, proportionnel ici.
                translationY = -floatPhase * 9.dp.toPx() * (this.size.width / 112.dp.toPx())
            }
            .shadow(
                elevation = 10.dp,
                shape = cornerShape,
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = Color(0x807C5CFF) // 0 8px 20px rgba(124,92,255,.5)
            )
            .clip(cornerShape)
            .background(
                brush = Brush.linearGradient(listOf(HubViolet, HubBlue))
            )
            .drawBehind {
                // inset 0 1px 0 rgba(255,255,255,.35)
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(this.size.width, 1.dp.toPx())
                )
                // Reflet radial : 70 % × 45 % at 30 % 10 %, blanc .55 → transparent 60 %.
                withTransform({
                    scale(
                        scaleX = this@drawBehind.size.width * 0.70f,
                        scaleY = this@drawBehind.size.height * 0.45f,
                        pivot = Offset(
                            this@drawBehind.size.width * 0.30f,
                            this@drawBehind.size.height * 0.10f
                        )
                    )
                }) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.55f),
                            0.60f to Color.Transparent,
                            1f to Color.Transparent,
                            center = Offset.Zero,
                            radius = 1f
                        ),
                        radius = 1f,
                        center = Offset.Zero
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Glyphe H : M6.5 4v16 M17.5 4v16 M6.5 12h11 — viewBox 24, trait 2.6 ;
            // le glyphe occupe 22/36 du côté de la marque (écran 06-feed).
            val glyphBox = this.size.width * (22f / 36f)
            val offset = (this.size.width - glyphBox) / 2f
            val scale = glyphBox / 24f
            val stroke = 2.6f * scale
            fun gx(x: Float): Float = offset + x * scale
            fun gy(y: Float): Float = offset + y * scale

            drawLine(
                color = Color.White,
                start = Offset(gx(6.5f), gy(4f)),
                end = Offset(gx(6.5f), gy(20f)),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(gx(17.5f), gy(4f)),
                end = Offset(gx(17.5f), gy(20f)),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(gx(6.5f), gy(12f)),
                end = Offset(gx(17.5f), gy(12f)),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
