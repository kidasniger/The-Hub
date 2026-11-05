// Build validation: notification subsystem restored from v1.0.180.
// The Hub shared UI primitives.
// Android stability pass: push, badges, sign-in.
package com.thehub.hb.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubChip
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Dimensions « Aurora Glass » — valeurs de glass.css :
 * --r-xs 10 · --r-sm 14 · --r-md 18 · --r-lg 24 · --r-xl 30 · --r-2xl 36 ;
 * .pad 18 px de gouttière d'écran.
 */
object HubDimens {
    val RadiusXs = 10.dp
    val RadiusSm = 14.dp
    val RadiusMd = 18.dp
    val RadiusLg = 24.dp
    val RadiusXl = 30.dp
    val Radius2xl = 36.dp
    val ScreenHorizontal = 18.dp
    val CompactHorizontal = 16.dp
    val SectionGap = 24.dp
    val CardRadius = 30.dp   // .post { border-radius: var(--r-xl) }
    val SmallRadius = 14.dp
    val TouchTarget = 48.dp
}

/**
 * Chip de la maquette (.chip / .chip.on) : pilule 999, 8×14, 12.5/600 ;
 * sélectionnée → dégradé de marque + lueur.
 */
@Composable
fun HubPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = shape,
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = HubFabGlow
                        )
                        .clip(shape)
                        .background(brush = hubPrimaryGradient(), shape = shape)
                } else {
                    Modifier
                        .clip(shape)
                        .background(HubChip, shape)
                        .border(1.dp, HubStrokeSoft, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingContent?.invoke()
        Text(
            text = text,
            color = if (selected) Color.White else HubSecondary,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** En-tête de section (.appbar .title 19/800/-0.3 + action accent). */
@Composable
fun HubSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HubWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = HubMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = HubAccent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HubAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** État vide de la maquette (.empty) : icône dans une carte de verre 76 dp rayon 26. */
@Composable
fun HubEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassSurface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(26.dp),
            variant = GlassVariant.Card
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HubAccent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = HubWhite,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            color = HubMuted,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(22.dp))
            HubButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.width(220.dp)
            )
        }
    }
}

/** Bannière .banner.danger : fond danger 11 %, contour danger 32 %, rayon --r-md 18. */
@Composable
fun HubErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(HubError.copy(alpha = 0.11f))
            .border(1.dp, HubError.copy(alpha = 0.32f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = HubError,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = message,
            color = HubWhite,
            fontSize = 12.5.sp,
            lineHeight = 19.sp
        )
    }
}

/**
 * Squelette .skel : fond --glass-strong, rayon 12, balayage lumineux
 * (shimmer horizontal traduisant l'animation translateX de la maquette).
 */
@Composable
fun HubSkeletonLine(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 14.dp,
    widthFraction: Float = 0.7f
) {
    val transition = rememberInfiniteTransition(label = "hub_skeleton")
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hub_skeleton_sweep"
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(HubGlassStrong)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = sweep * size.width
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            HubWhite.copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/** Nom vérifié — couleurs mises à jour automatiquement via les tokens Aurora Glass. */
@Composable
fun HubVerifiedUserName(
    name: String,
    isVerified: Boolean,
    verificationType: String?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    color: Color = HubWhite,
    weight: FontWeight = FontWeight.Bold,
    badgeSize: androidx.compose.ui.unit.Dp = 15.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = name,
            color = color,
            fontSize = fontSize,
            fontWeight = weight
        )
        if (verificationType == "admin") {
            OfficialVerificationBadge(size = badgeSize)
        }
    }
}
