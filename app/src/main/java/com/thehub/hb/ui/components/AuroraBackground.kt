package com.thehub.hb.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import com.thehub.hb.ui.theme.LocalHubColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Fond « Aurora Glass » de la maquette (.screen de assets/glass.css), peint UNE SEULE
 * fois derrière toute la navigation (appelé par HubThemeBackground) :
 *
 *  1. dégradé de base à 160° : --bg0 → --bg1 (55 %) → --bg2 ;
 *  2. trois lueurs radiales ELLIPTIQUES empilées au-dessus du dégradé :
 *     - --glow1 violet : 120 % × 78 % centré à 88 % / -12 % (haut-droite), fondu à 56 % ;
 *     - --glow2 bleu : 105 % × 70 % centré à -14 % / 22 % (gauche), fondu à 52 % ;
 *     - --glow3 rose : 90 % × 60 % centré à 50 % / 112 % (bas-centre), fondu à 55 % ;
 *  3. grain subtil (SVG feTurbulence de la maquette → bruit statique aléatoire
 *     dessiné en mosaïque à alpha 5 %).
 *
 * Une version sombre (« Dark glass ») et une version claire (« Light glass »)
 * existent, pilotées par LocalHubColors.isLight.
 *
 * Technique : les ellipses radiales sont dessinées avec withTransform { scale(...) }
 * autour d'un cercle unitaire — aucune API non stable, fonctionne sur minSdk 24.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalHubColors.current
    val bg0 = palette.bg0
    val bg1 = palette.bg1
    val bg2 = palette.bg2
    val glow1 = palette.glow1
    val glow2 = palette.glow2
    val glow3 = palette.glow3

    // Grain statique déterministe (même rendu à chaque démarrage), 256 × 256 px,
    // mosaïqué pour couvrir l'écran ; alpha 5 % comme la maquette.
    val grainImage = remember {
        val tile = 256
        val pixels = IntArray(tile * tile)
        val rnd = Random(20261102)
        for (i in pixels.indices) {
            val v = 128 + rnd.nextInt(from = -60, until = 60)
            pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bmp = Bitmap.createBitmap(tile, tile, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, tile, 0, 0, tile, tile)
        bmp.asImageBitmap()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Dégradé de base à 160°.
                // Direction CSS 160° (0° = vers le haut, sens horaire) : (sin 160°, -cos 160°).
                val rad = Math.toRadians(160.0)
                val dirX = Math.sin(rad).toFloat()      // ≈ 0.342 (vers la droite)
                val dirY = (-Math.cos(rad)).toFloat()   // ≈ 0.940 (vers le bas)
                val length = size.width * abs(dirX) + size.height * abs(dirY)
                val center = Offset(size.width / 2f, size.height / 2f)
                drawRect(
                    brush = Brush.linearGradient(
                        0f to bg0,
                        0.55f to bg1,
                        1f to bg2,
                        start = Offset(
                            center.x - dirX * length / 2f,
                            center.y - dirY * length / 2f
                        ),
                        end = Offset(
                            center.x + dirX * length / 2f,
                            center.y + dirY * length / 2f
                        )
                    )
                )

                // 2. Lueurs — mêmes géométries et fondus que .screen.
                drawAuroraGlow(
                    center = Offset(size.width * 0.88f, -size.height * 0.12f),
                    radiusX = size.width * 1.20f,
                    radiusY = size.height * 0.78f,
                    color = glow1,
                    fadeStop = 0.56f
                )
                drawAuroraGlow(
                    center = Offset(-size.width * 0.14f, size.height * 0.22f),
                    radiusX = size.width * 1.05f,
                    radiusY = size.height * 0.70f,
                    color = glow2,
                    fadeStop = 0.52f
                )
                drawAuroraGlow(
                    center = Offset(size.width * 0.50f, size.height * 1.12f),
                    radiusX = size.width * 0.90f,
                    radiusY = size.height * 0.60f,
                    color = glow3,
                    fadeStop = 0.55f
                )

                // 3. Grain en mosaïque, alpha 5 % (mix-blend overlay de la maquette
                // approximé en SrcOver pour rester stable sur minSdk 24).
                val tw = grainImage.width
                val th = grainImage.height
                val cols = ceil(size.width / tw).toInt() + 1
                val rows = ceil(size.height / th).toInt() + 1
                var col = 0
                while (col < cols) {
                    var row = 0
                    while (row < rows) {
                        drawImage(
                            image = grainImage,
                            dstOffset = IntOffset(col * tw, row * th),
                            alpha = 0.05f,
                            filterQuality = FilterQuality.None
                        )
                        row++
                    }
                    col++
                }
            }
    ) {
        content()
    }
}

/**
 * Lueur radiale elliptique : cercle unitaire muni d'un dégradé radial dont les
 * stops reproduisent le fondu CSS (couleur pleine au centre, transparent à
 * [fadeStop]), étiré par une transformation d'échelle autour du centre de la lueur.
 */
private fun DrawScope.drawAuroraGlow(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    color: Color,
    fadeStop: Float
) {
    withTransform({ scale(radiusX, radiusY, pivot = center) }) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                fadeStop to Color.Transparent,
                1f to Color.Transparent,
                center = Offset.Zero,
                radius = 1f
            ),
            radius = 1f,
            center = Offset.Zero
        )
    }
}
