package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.VideoLinkDetector

/**
 * Aperçu vidéo en ligne « Aurora Glass » (Feed / Détail / Profil) : miniature
 * rayon 20, scrim palette, pastille fournisseur sur scrim, bouton lecture en
 * dégradé de marque avec lueur --fab-glow. La lecture s'ouvre dans le viewer.
 */
@Composable
fun VideoLinkCard(
    videoUrl: String,
    onClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val provider = VideoLinkDetector.providerLabel(videoUrl)
    val thumbnailUrl = VideoLinkDetector.thumbnailUrl(videoUrl)
    val isShort = VideoLinkDetector.isYouTubeShorts(videoUrl)

    val aspectRatio = if (isShort) {
        9f / 16f
    } else {
        16f / 9f
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(palette.bg1)
            .testTag("video_link_card")
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(videoUrl) }
                } else {
                    Modifier
                }
            )
    ) {
        if (thumbnailUrl != null) {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = strings.lot3.videoPreviewDescFormat.format(provider),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(HubGlassStrong),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                },
                error = {
                    VideoThumbnailFallback(
                        provider = provider,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            VideoThumbnailFallback(
                provider = provider,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Scrim de lisibilité (palette --scrim).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = palette.scrim.alpha * 0.45f))
        )

        // Pastille fournisseur.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(palette.scrim)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = HubWhite,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = provider,
                    color = HubWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Bouton lecture — dégradé de marque + lueur FAB de la maquette.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (isShort) 62.dp else 58.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Transparent,
                    spotColor = palette.fabGlow
                )
                .clip(CircleShape)
                .background(brush = hubPrimaryGradient()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = strings.lot3.playVideoDesc,
                tint = Color.White,
                modifier = Modifier.size(if (isShort) 34.dp else 32.dp)
            )
        }

        // Pastille « Shorts » pour le format vertical.
        if (isShort) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(palette.scrim)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = strings.lot3.shortsLabel,
                    color = HubWhite,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun VideoThumbnailFallback(
    provider: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    Box(
        modifier = modifier.background(HubGlassStrong),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = HubSecondary,
                modifier = Modifier.size(42.dp)
            )
            Text(
                text = provider,
                color = HubWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = strings.lot3.previewUnavailableTitle,
                color = HubMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
