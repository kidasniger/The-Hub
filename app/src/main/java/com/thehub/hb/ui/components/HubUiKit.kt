// The Hub shared UI primitives.
// Android stability pass: push, badges, sign-in.
package com.thehub.hb.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

object HubDimens {
    val ScreenHorizontal = 20.dp
    val CompactHorizontal = 16.dp
    val SectionGap = 24.dp
    val CardRadius = 20.dp
    val SmallRadius = 14.dp
    val TouchTarget = 48.dp
}

@Composable
fun HubPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HubWhite else HubSurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) HubWhite else HubBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingContent?.invoke()
        Text(
            text = text,
            color = if (selected) Color(0xFF09090B) else HubSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

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
                fontWeight = FontWeight.Bold
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
                    color = HubSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

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
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated)
                .border(1.dp, HubBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HubSecondary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = HubWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            color = HubMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
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

@Composable
fun HubErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HubError.copy(alpha = 0.10f))
            .border(1.dp, HubError.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = HubError,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            color = HubWhite,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun HubSkeletonLine(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 14.dp,
    widthFraction: Float = 0.7f
) {
    val transition = rememberInfiniteTransition(label = "hub_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hub_skeleton_alpha"
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(HubWhite.copy(alpha = alpha))
    )
}

@Composable
fun HubCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(HubWhite)
            .border(1.dp, HubBlack, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color(0xFF09090B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
