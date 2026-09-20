package com.thehub.hb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.VideoLinkDetector

/**
 * Inline video preview used by Feed/Profile/Post Detail.
 *
 * The video itself is not played inside the card. The card renders a thumbnail
 * and opens the full VideoViewer only when the user taps it.
 */
@Composable
fun VideoLinkCard(
    videoUrl: String,
    onClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            .background(Color.Black)
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
                contentDescription = "Aperçu vidéo $provider",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(HubSurfaceElevated),
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

        // Subtle readability scrim.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f))
        )

        // Provider label.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.68f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.VideoLibrary,
                    contentDescription = null,
                    tint = HubWhite,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = provider,
                    color = HubWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // Main play button.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (isShort) 62.dp else 58.dp)
                .clip(CircleShape)
                .background(
                    HubViolet.copy(alpha = 0.92f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Lire la vidéo",
                tint = Color.White,
                modifier = Modifier.size(if (isShort) 34.dp else 32.dp)
            )
        }

        // Short label helps the viewer understand the vertical format.
        if (isShort) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Shorts",
                    color = HubWhite,
                    fontSize = 11.sp,
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
    Box(
        modifier = modifier
            .background(HubSurfaceElevated)
            .then(
                Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                text = "Aperçu indisponible",
                color = HubMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
