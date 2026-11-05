package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubHighlight
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubShadowColor
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Variantes du bouton « Aurora Glass » (design-system.html) :
 *  - Primary   : .btn-primary — dégradé de marque 135°, lueur --fab-glow,
 *                grad-line haute (inset 18 px, blanc .55 dégradé horizontal) ;
 *  - Secondary : .btn-glass   — verre, contour --stroke, ombre douce, reflet interne ;
 *  - Danger    : .btn-danger  — fond danger 13 %, contour danger 38 %, texte danger ;
 *  - Ghost     : .btn-ghost   — transparent, texte --accent.
 *
 * Métriques maquette : rayon --r-md 18, hauteur ≈ 50 (padding 15 × 2 + ligne ~20),
 * texte 14.5/700/0.1, écart icône/texte 8 px (.btn { gap: 8px }).
 * Signatures publiques et testTags conservés ; paramètres ajoutés
 * [leadingIcon] / [trailingIcon] avec valeur par défaut null (aucune rupture).
 */
enum class HubButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Danger
}

// Métriques du bouton issues de glass.css (.btn)
private val HubButtonShape = RoundedCornerShape(18.dp)
private val HubButtonHeight = 50.dp

@Composable
fun HubButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HubButtonVariant = HubButtonVariant.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String = "hub_button"
) {
    when (variant) {
        HubButtonVariant.Primary -> {
            // Bouton signature de la maquette : dégradé violet → bleu + lueur + grad-line.
            val active = enabled && !isLoading
            val brush = if (active) {
                hubPrimaryGradient()
            } else {
                Brush.verticalGradient(
                    listOf(HubSurfaceElevated, HubSurfaceElevated)
                )
            }
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(HubButtonHeight)
                    .shadow(
                        elevation = 14.dp,
                        shape = HubButtonShape,
                        clip = false,
                        ambientColor = Color.Transparent,
                        spotColor = HubFabGlow
                    )
                    .background(brush = brush, shape = HubButtonShape)
                    .drawBehind {
                        // .btn .grad-line : inset 18 px, 1 px, dégradé horizontal
                        // transparent → blanc .55 → transparent.
                        val inset = 18.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(inset, 0f),
                            size = Size(size.width - 2 * inset, 1.dp.toPx())
                        )
                    }
                    .testTag(testTag),
                shape = HubButtonShape,
                enabled = active,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = HubMuted
                )
            ) {
                HubButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }
        HubButtonVariant.Secondary -> {
            // .btn-glass : verre + contour --stroke + ombre douce + reflet interne.
            val active = enabled && !isLoading
            // Capture hors lambda : HubHighlight est un getter @Composable.
            val highlight = HubHighlight
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(HubButtonHeight)
                    .shadow(
                        elevation = 10.dp,
                        shape = HubButtonShape,
                        clip = false,
                        ambientColor = HubShadowColor,
                        spotColor = HubShadowColor
                    )
                    .background(HubGlass, HubButtonShape)
                    .drawBehind {
                        // inset 0 1px 0 var(--hl)
                        drawRect(
                            color = highlight,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, 1.dp.toPx())
                        )
                    }
                    .border(1.dp, HubStroke, HubButtonShape)
                    .testTag(testTag),
                shape = HubButtonShape,
                enabled = active,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = HubWhite,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = if (isLoading) HubWhite else HubMuted
                )
            ) {
                HubButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }
        HubButtonVariant.Ghost -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .height(HubButtonHeight)
                    .testTag(testTag),
                shape = HubButtonShape,
                enabled = enabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = HubAccent,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = if (isLoading) HubAccent else HubMuted
                )
            ) {
                HubButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }
        HubButtonVariant.Danger -> {
            // .btn-danger : fond danger 13 %, contour danger 38 %, texte danger.
            val active = enabled && !isLoading
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(HubButtonHeight)
                    .background(HubError.copy(alpha = 0.13f), HubButtonShape)
                    .border(1.dp, HubError.copy(alpha = 0.38f), HubButtonShape)
                    .testTag(testTag),
                shape = HubButtonShape,
                enabled = active,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = HubError,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = if (isLoading) HubError else HubMuted
                )
            ) {
                HubButtonContent(
                    text = text,
                    isLoading = isLoading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }
    }
}

/**
 * Contenu commun des variantes : icône optionnelle — texte 14.5/700/0.1 —
 * icône optionnelle, écart 8 px (.btn { gap: 8px }). En chargement, la signature
 * animée de marque remplace le contenu (état « chargement » du lot États).
 */
@Composable
private fun HubButtonContent(
    text: String,
    isLoading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?
) {
    if (isLoading) {
        AppLogo(
            size = 26.dp,
            animated = true
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            )
            trailingIcon?.invoke()
        }
    }
}
