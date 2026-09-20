package com.thehub.hb.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.RelativeTime

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onPostClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onUserClick: (String, String) -> Unit,
    onOpenMessenger: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("notifications_screen")
    ) {
        // Top App Bar
        NotificationsHeader(
            unreadCount = uiState.unreadCount,
            onMarkAllAsRead = { viewModel.onMarkAllAsRead() }
        )

        HorizontalDivider(color = HubBorder, thickness = 1.dp)

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("notifications_loading_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                uiState.notifications.isEmpty() -> {
                    EmptyNotifications()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("notifications_list"),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.groupedNotifications.isNotEmpty()) {
                            uiState.groupedNotifications.forEach { (periodTitle, itemsInPeriod) ->
                                item(key = "header_$periodTitle") {
                                    PeriodHeader(title = periodTitle)
                                }

                                items(
                                    items = itemsInPeriod,
                                    key = { it.id }
                                ) { notification ->
                                    NotificationRow(
                                        notification = notification,
                                        onClick = {
                                            viewModel.onNotificationClicked(notification)
                                            when (notification.type) {
                                                NotificationItem.TYPE_LIKE, NotificationItem.TYPE_COMMENT,
                                                NotificationItem.TYPE_LIKE_COMMENT, NotificationItem.TYPE_REPLY_COMMENT -> {
                                                    notification.postId?.let { postId ->
                                                        onOpenComments(postId)
                                                    }
                                                }
                                                NotificationItem.TYPE_FOLLOW -> {
                                                    onUserClick(notification.actorId, notification.actorUsername)
                                                }
                                                NotificationItem.TYPE_MESSAGE -> {
                                                    notification.conversationId
                                                        ?.takeIf { it.isNotBlank() }
                                                        ?.let(onOpenChat)
                                                        ?: onOpenMessenger()
                                                }
                                                else -> {
                                                    notification.postId?.let { onPostClick(it) }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.notifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationRow(
                                    notification = notification,
                                    onClick = {
                                        viewModel.onNotificationClicked(notification)
                                        when (notification.type) {
                                            NotificationItem.TYPE_LIKE, NotificationItem.TYPE_COMMENT,
                                            NotificationItem.TYPE_LIKE_COMMENT, NotificationItem.TYPE_REPLY_COMMENT -> {
                                                notification.postId?.let { postId ->
                                                    onPostClick(postId)
                                                }
                                            }
                                            NotificationItem.TYPE_FOLLOW -> {
                                                onUserClick(notification.actorId, notification.actorUsername)
                                            }
                                            NotificationItem.TYPE_MESSAGE -> {
                                                onOpenMessenger()
                                            }
                                            else -> {
                                                notification.postId?.let { onPostClick(it) }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsHeader(
    unreadCount: Int,
    onMarkAllAsRead: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Notifications",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )

            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(HubWhite)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else "$unreadCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubBlack
                    )
                }
            }
        }

        if (unreadCount > 0) {
            TextButton(
                onClick = onMarkAllAsRead,
                modifier = Modifier.testTag("mark_all_read_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Tout lire",
                    fontSize = 13.sp,
                    color = HubSecondary
                )
            }
        }
    }
}

@Composable
private fun PeriodHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = HubSecondary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val actor = rememberLiveUser(
        userId = notification.actorId,
        fallbackUsername = notification.actorUsername,
        fallbackPhotoUrl = notification.actorPhotoUrl
    )
    val backgroundColor = if (!notification.isRead) HubCard else HubBlack
    val borderColor = if (!notification.isRead) HubBorder else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("notification_item_${notification.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with Action Badge
        Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatar(
                name = actor.effectiveName,
                photoUrl = actor.photoUrl,
                size = 46.dp
            )

            val badgeInfo = getBadgeForType(notification.type)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(badgeInfo.color)
                    .border(1.5.dp, HubBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeInfo.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text & Timestamp
        Column(modifier = Modifier.weight(1f)) {
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HubWhite)) {
                    append(actor.effectiveName)
                }

                val actionText = when (notification.type) {
                    NotificationItem.TYPE_LIKE -> " a aimé votre publication."
                    NotificationItem.TYPE_LIKE_COMMENT -> " a aimé votre commentaire."
                    NotificationItem.TYPE_COMMENT -> {
                        if (!notification.commentText.isNullOrBlank()) {
                            " a commenté : « ${notification.commentText} »"
                        } else {
                            " a commenté votre publication."
                        }
                    }
                    NotificationItem.TYPE_REPLY_COMMENT -> {
                        if (!notification.commentText.isNullOrBlank()) {
                            " a répondu à votre commentaire : « ${notification.commentText} »"
                        } else {
                            " a répondu à votre commentaire."
                        }
                    }
                    NotificationItem.TYPE_FOLLOW -> " a commencé à vous suivre."
                    NotificationItem.TYPE_MESSAGE -> {
                        if (!notification.commentText.isNullOrBlank()) {
                            " vous a envoyé un message : « ${notification.commentText} »"
                        } else {
                            " vous a envoyé un message."
                        }
                    }
                    else -> " a interagi avec vous."
                }

                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = HubWhite.copy(alpha = 0.9f))) {
                    append(actionText)
                }
            }

            Text(
                text = annotatedText,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = RelativeTime.format(notification.createdAt),
                fontSize = 12.sp,
                color = HubSecondary
            )
        }

        // Unread Blue/White Indicator Dot
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HubWhite)
                    .testTag("unread_dot_${notification.id}")
            )
        }
    }
}

private data class NotificationBadge(
    val icon: ImageVector,
    val color: Color
)

private fun getBadgeForType(type: String): NotificationBadge {
    return when (type) {
        NotificationItem.TYPE_LIKE, NotificationItem.TYPE_LIKE_COMMENT -> NotificationBadge(
            icon = Icons.Default.Favorite,
            color = Color(0xFFE91E63) // Vibrant Rose / Red
        )
        NotificationItem.TYPE_COMMENT, NotificationItem.TYPE_REPLY_COMMENT -> NotificationBadge(
            icon = Icons.Default.ChatBubble,
            color = Color(0xFF2196F3) // Blue
        )
        NotificationItem.TYPE_FOLLOW -> NotificationBadge(
            icon = Icons.Default.PersonAdd,
            color = Color(0xFF9C27B0) // Purple
        )
        NotificationItem.TYPE_MESSAGE -> NotificationBadge(
            icon = Icons.Default.Mail,
            color = Color(0xFF00B0FF) // Cyan
        )
        else -> NotificationBadge(
            icon = Icons.Default.Notifications,
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
private fun EmptyNotifications() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("empty_notifications"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Toutes vos notifications sont à jour",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Les mentions J'aime, commentaires, nouveaux abonnements et messages apparaîtront ici.",
                fontSize = 13.sp,
                color = HubSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
