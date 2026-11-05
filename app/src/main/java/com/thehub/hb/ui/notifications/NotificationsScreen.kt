package com.thehub.hb.ui.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.RelativeTime

/**
 * Écran 15 « Notifications » — maquette Aurora Glass.
 *
 * App bar : titre 19/800 + sous-titre « N nouvelles », bouton « tout marquer
 * comme lu » (.iconbtn, ✓✓) ; contrôle segmenté .seg (Toutes / Non lues /
 * Activité / Messages) ; liste par période : titre de section 12/800
 * majuscules puis carte de verre r22 contenant les rangées séparées par des
 * dividers (avatar 40, texte 12.5/600 avec nom en ExtraBold, citation 11 en
 * text-3, heure à droite, pastille non-lue dégradé 8 dp lueur violette).
 * Chargement = squelettes, vide = HubEmptyState par filtre.
 */
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
    val strings = LocalHubStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("notifications_screen")
    ) {
        // ---- App bar : titre + sous-titre + tout marquer comme lu ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.notificationsTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite
                )
                if (uiState.unreadCount > 0) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = if (uiState.unreadCount == 1) {
                            strings.lot4.notifNewOne
                        } else {
                            strings.lot4.notifNewFormat.format(uiState.unreadCount)
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubTextTertiary
                    )
                }
            }
            if (uiState.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubGlassSoft)
                        .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                        .clickable { viewModel.onMarkAllAsRead() }
                        .testTag("mark_all_read_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = strings.lot4.markAllReadDesc,
                        tint = HubWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ---- Contrôle segmenté .seg ----
        NotificationFilterBar(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = viewModel::setFilter
        )

        // ---- Contenu ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    NotificationsLoadingSkeleton()
                }

                uiState.visibleNotifications.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("empty_notifications"),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyNotifications(filter = uiState.selectedFilter)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("notifications_list"),
                        contentPadding = PaddingValues(
                            top = 4.dp, bottom = 96.dp
                        )
                    ) {
                        if (uiState.groupedNotifications.isNotEmpty()) {
                            uiState.groupedNotifications.forEach { (periodTitle, itemsInPeriod) ->
                                item(key = "header_$periodTitle") {
                                    PeriodHeader(title = periodTitle)
                                }

                                item(key = "card_$periodTitle") {
                                    GlassSurface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp),
                                        shape = RoundedCornerShape(22.dp),
                                        variant = GlassVariant.Card
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            itemsInPeriod.forEachIndexed { index, notification ->
                                                if (index > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp)
                                                            .height(1.dp)
                                                            .background(HubStrokeSoft)
                                                    )
                                                }
                                                NotificationRow(
                                                    notification = notification,
                                                    onClick = {
                                                        viewModel.onNotificationClicked(notification)
                                                        when (notification.type) {
                                                            NotificationItem.TYPE_LIKE -> {
                                                                notification.postId?.let(onPostClick)
                                                            }
                                                            NotificationItem.TYPE_COMMENT,
                                                            NotificationItem.TYPE_LIKE_COMMENT,
                                                            NotificationItem.TYPE_REPLY_COMMENT,
                                                            NotificationItem.TYPE_ANNOUNCEMENT -> {
                                                                notification.postId?.let(onOpenComments)
                                                            }
                                                            NotificationItem.TYPE_FOLLOW -> {
                                                                onUserClick(
                                                                    notification.actorId,
                                                                    notification.actorUsername
                                                                )
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
                                    }
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
                                            NotificationItem.TYPE_LIKE -> {
                                                notification.postId?.let(onPostClick)
                                            }
                                            NotificationItem.TYPE_COMMENT,
                                            NotificationItem.TYPE_LIKE_COMMENT,
                                            NotificationItem.TYPE_REPLY_COMMENT -> {
                                                notification.postId?.let(onOpenComments)
                                            }
                                            NotificationItem.TYPE_FOLLOW -> {
                                                onUserClick(
                                                    notification.actorId,
                                                    notification.actorUsername
                                                )
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

/** Titre de période .section-title — 12/800, majuscules, text-3. */
@Composable
private fun PeriodHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = HubTextTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
    )
}

/** Rangée de notification — avatar 40, texte, heure, pastille non-lue. */
@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val strings = LocalHubStrings.current
    val actor = rememberLiveUser(
        userId = notification.actorId,
        fallbackUsername = notification.actorUsername,
        fallbackDisplayName = notification.actorDisplayName,
        fallbackPhotoUrl = notification.actorPhotoUrl
    )

    // Texte principal et citation selon le type (données du ViewModel).
    val quoteText = when (notification.type) {
        NotificationItem.TYPE_COMMENT,
        NotificationItem.TYPE_REPLY_COMMENT,
        NotificationItem.TYPE_MESSAGE ->
            notification.commentText?.takeIf { it.isNotBlank() }
                ?.let { strings.lot4.notifQuoteFormat.format(it) }
        else -> null
    }
    val actionText = when (notification.type) {
        NotificationItem.TYPE_LIKE -> strings.lot4.notifLikedPost
        NotificationItem.TYPE_LIKE_COMMENT -> strings.lot4.notifLikedComment
        NotificationItem.TYPE_COMMENT -> strings.lot4.notifCommented
        NotificationItem.TYPE_REPLY_COMMENT -> strings.lot4.notifReply
        NotificationItem.TYPE_FOLLOW -> strings.lot4.notifFollowed
        NotificationItem.TYPE_MESSAGE -> strings.lot4.notifMessage
        NotificationItem.TYPE_ANNOUNCEMENT ->
            notification.commentText?.takeIf { it.isNotBlank() }
                ?.let { "· $it" }
                ?: strings.lot4.notifAnnouncement
        else -> strings.lot4.notifInteracted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("notification_item_${notification.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(
            name = actor.effectiveName,
            photoUrl = actor.photoUrl,
            size = 40.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            color = HubWhite
                        )
                    ) {
                        append(actor.effectiveName)
                    }
                    append(" ")
                    append(actionText)
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                color = HubWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (quoteText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = quoteText,
                    fontSize = 11.sp,
                    color = HubTextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Colonne droite : heure + pastille non-lue dégradé avec lueur.
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = RelativeTime.format(notification.createdAt),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HubTextTertiary
            )
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = HubViolet.copy(alpha = 0.8f)
                        )
                        .clip(CircleShape)
                        .background(brush = hubPrimaryGradient())
                        .testTag("unread_dot_${notification.id}")
                )
            }
        }
    }
}

/** Contrôle segmenté .seg — quatre filtres du ViewModel. */
@Composable
private fun NotificationFilterBar(
    selectedFilter: NotificationFilter,
    onFilterSelected: (NotificationFilter) -> Unit
) {
    val strings = LocalHubStrings.current
    val filters = listOf(
        NotificationFilter.ALL to strings.lot4.filterAllNotif,
        NotificationFilter.UNREAD to strings.lot4.filterUnread,
        NotificationFilter.ACTIVITY to strings.lot4.filterActivity,
        NotificationFilter.MESSAGES to strings.lot4.filterMessages
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HubGlassSoft)
            .border(1.dp, HubStrokeSoft, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .testTag("notification_filter_bar")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            filters.forEach { (filter, label) ->
                val selected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) HubGlassStrong else Color.Transparent)
                        .border(
                            1.dp,
                            if (selected) HubStroke else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                        .testTag("notification_filter_${filter.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) HubWhite else HubTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** État vide — HubEmptyState dont le titre dépend du filtre actif. */
@Composable
private fun EmptyNotifications(filter: NotificationFilter) {
    val strings = LocalHubStrings.current
    HubEmptyState(
        title = when (filter) {
            NotificationFilter.ALL -> strings.lot4.notifEmptyAllTitle
            NotificationFilter.UNREAD -> strings.lot4.notifEmptyUnreadTitle
            NotificationFilter.ACTIVITY -> strings.lot4.notifEmptyActivityTitle
            NotificationFilter.MESSAGES -> strings.lot4.notifEmptyMessagesTitle
        },
        description = strings.lot4.notifEmptyDesc,
        icon = Icons.Outlined.Notifications
    )
}

/** Chargement : deux cartes de verre à rangées squelettes. */
@Composable
private fun NotificationsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("notifications_loading_indicator"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HubSkeletonLine(height = 12.dp, widthFraction = 0.3f, modifier = Modifier.padding(start = 8.dp))
        repeat(2) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                variant = GlassVariant.Card
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(3) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(HubGlassStrong)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                HubSkeletonLine(height = 12.dp, widthFraction = 0.85f)
                                Spacer(modifier = Modifier.height(6.dp))
                                HubSkeletonLine(height = 10.dp, widthFraction = 0.55f)
                            }
                        }
                    }
                }
            }
        }
    }
}
