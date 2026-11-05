package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.LocalHubColors

/**
 * Huit dégradés d'avatar de la maquette (.av-g1 … .av-g8 de glass.css),
 * attribués de façon déterministe selon le nom (hash stable de String).
 */
private val HubAvatarGradients = listOf(
    Color(0xFF8B5CF6) to Color(0xFF6366F1), // .av-g1
    Color(0xFF3B82F6) to Color(0xFF06B6D4), // .av-g2
    Color(0xFFEC4899) to Color(0xFFF97316), // .av-g3
    Color(0xFF10B981) to Color(0xFF34D399), // .av-g4
    Color(0xFFF59E0B) to Color(0xFFEF4444), // .av-g5
    Color(0xFF6366F1) to Color(0xFFEC4899), // .av-g6
    Color(0xFF0EA5E9) to Color(0xFF8B5CF6), // .av-g7
    Color(0xFF14B8A6) to Color(0xFF22C55E)  // .av-g8
)

/**
 * Avatar « Aurora Glass » : dégradé 135° de la palette .av-gN, initiale blanche 800,
 * reflet interne haut (inset 0 1px 0 rgba(255,255,255,.35)), ombre douce
 * (0 4px 12px rgba(0,0,0,.25)) et point de présence vert (.on-dot) optionnel.
 *
 * Signature d'origine conservée ; seul paramètre ajouté : [isOnline] (défaut null,
 * aucune rupture pour les appelants existants).
 */
@Composable
fun UserAvatar(
    name: String,
    photoUrl: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    isOnline: Boolean? = null
) {
    val palette = LocalHubColors.current
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val gradient = HubAvatarGradients[((name.hashCode() % 8) + 8) % 8]

    @Composable
    fun DefaultAvatarContent() {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    // inset 0 1px 0 rgba(255,255,255,.35)
                    drawRect(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(0f, 0f),
                        size = Size(this.size.width, 1.dp.toPx())
                    )
                }
                .background(
                    brush = Brush.linearGradient(
                        listOf(gradient.first, gradient.second)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    val remoteImageUnavailable = if (!photoUrl.isNullOrBlank()) {
        rememberRemoteImageUnavailable(photoUrl)
    } else {
        false
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank() && !remoteImageUnavailable) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = "Avatar de $name",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    DefaultAvatarContent()
                },
                success = {
                    SubcomposeAsyncImageContent()
                },
                error = {
                    DefaultAvatarContent()
                }
            )
        } else {
            DefaultAvatarContent()
        }

        // Point de présence .on-dot : 26 % de l'avatar (min 10 dp), bordure du fond.
        if (isOnline == true) {
            val dotSize = (size.value * 0.26f).dp.coerceAtLeast(10.dp)
            val dotBorderColor = if (palette.isLight) Color(0xFFFDFCFF) else Color(0xFF0B0A1C)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(HubSuccess)
                    .border(2.5.dp, dotBorderColor, CircleShape)
            )
        }
    }
}
