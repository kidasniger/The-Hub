package com.thehub.hb.ui.messenger

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubAccent2
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.RelativeTime

/**
 * Écran 11 « Messagerie » — maquette Aurora Glass.
 *
 * App bar verre : retour + titre 19/800 + sous-titre « N conversations non lues » ;
 * champ de recherche .input ; rangée d'accès rapide (cercle pointillé « Nouveau » +
 * avatars 48 dp des conversations récentes) ; séparateur ; titre de section
 * « Récentes » ; rangées .row (avatar 48, titre 13.5/700, aperçu 11.5, heure 10,
 * coche de lecture accent ou badge non-lu dégradé) ; FAB 58/20 dégradé avion.
 * Chargement = squelettes, vide = HubEmptyState, erreur = bandeau .banner.danger.
 */
@Composable
fun MessengerScreen(
    viewModel: MessengerViewModel,
    onBack: () -> Unit,
    onConversationClick: (String) -> Unit,
    onNewMessageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val strings = LocalHubStrings.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .testTag("messenger_screen")
    ) {
        // ---- .appbar : iconbtn retour + titre + sous-titre non-lues ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassIconButton(
                onClick = onBack,
                modifier = Modifier.testTag("messenger_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = HubWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.lot4.messengerTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite
                )
                when (val state = uiState) {
                    is MessengerUiState.Success -> {
                        val unreadConv = state.conversations.count {
                            it.getUnreadCountFor(state.currentUserId) > 0
                        }
                        if (unreadConv > 0) {
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = if (unreadConv == 1) {
                                    strings.lot4.messengerUnreadOne
                                } else {
                                    strings.lot4.messengerUnreadFormat.format(unreadConv)
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HubTextTertiary
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }

        // ---- Recherche .input ----
        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
            HubTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = strings.lot4.messengerSearchPlaceholder,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = HubTextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = strings.lot4.searchClearDesc,
                            tint = HubTextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.onSearchQueryChanged("") }
                        )
                    }
                } else null,
                minHeight = 46,
                testTag = "messenger_search_input"
            )
        }

        when (val state = uiState) {
            is MessengerUiState.Loading -> {
                MessengerLoadingSkeleton()
            }

            is MessengerUiState.Success -> {
                if (state.filteredConversations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        MessengerEmptyState(
                            isSearch = searchQuery.isNotBlank(),
                            onNewMessageClick = onNewMessageClick
                        )
                    }
                } else {
                    MessengerSuccessContent(
                        state = state,
                        onConversationClick = onConversationClick,
                        onNewMessageClick = onNewMessageClick
                    )
                }
            }

            is MessengerUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = HubWhite.copy(alpha = 0.68f),
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Contenu succès : accès rapide + « Récentes » + liste + FAB. */
@Composable
private fun MessengerSuccessContent(
    state: MessengerUiState.Success,
    onConversationClick: (String) -> Unit,
    onNewMessageClick: () -> Unit
) {
    val strings = LocalHubStrings.current
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("conversations_list"),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // ---- Accès rapide : cercle pointillé « Nouveau » + avatars récents ----
            item(key = "quick_access") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(
                                            HubAccent.copy(alpha = 0.22f),
                                            HubAccent2.copy(alpha = 0.22f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(
                                            HubAccent.copy(alpha = 0.6f),
                                            HubAccent2.copy(alpha = 0.6f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(onClick = onNewMessageClick)
                                .testTag("new_message_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.lot4.newChatDesc,
                                tint = HubAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = strings.lot4.newChatLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HubTextTertiary
                        )
                    }
                    state.filteredConversations.take(4).forEach { conversation ->
                        val otherInfo = conversation.getOtherParticipantInfo(state.currentUserId)
                        val otherUid = conversation.getOtherParticipantId(state.currentUserId)
                        val liveUser = rememberLiveUser(
                            userId = otherUid,
                            fallbackUsername = otherInfo.username,
                            fallbackDisplayName = otherInfo.displayName,
                            fallbackPhotoUrl = otherInfo.photoUrl
                        )
                        UserAvatar(
                            name = liveUser.effectiveName,
                            photoUrl = liveUser.photoUrl,
                            size = 48.dp,
                            modifier = Modifier
                                .clickable { onConversationClick(conversation.id) }
                        )
                    }
                }
            }

            // ---- Séparateur + titre de section « Récentes » ----
            item(key = "divider") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .height(1.dp)
                        .background(HubStrokeSoft)
                )
            }
            item(key = "section_title") {
                Text(
                    text = strings.lot4.sectionRecent.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = HubTextTertiary,
                    modifier = Modifier.padding(start = 26.dp, top = 6.dp, bottom = 8.dp)
                )
            }

            // ---- Rangées de conversation .row ----
            items(
                items = state.filteredConversations,
                key = { it.id }
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    currentUserId = state.currentUserId,
                    onClick = { onConversationClick(conversation.id) }
                )
            }
        }

        // ---- FAB .fab 58/20 dégradé + avion ----
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 30.dp)
                .navigationBarsPadding()
                .size(58.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(20.dp),
                    clip = false,
                    ambientColor = Color.Transparent,
                    spotColor = HubFabGlow
                )
                .clip(RoundedCornerShape(20.dp))
                .background(brush = hubPrimaryGradient())
                .clickable(onClick = onNewMessageClick)
                .testTag("messenger_fab"),
            contentAlignment = Alignment.Center
        ) {
            // Reflet interne haut 1 dp rgba(255,255,255,.35)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.35f))
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = strings.lot4.messengerFabDesc,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Rangée de conversation .row — avatar 48, titre, aperçu, heure, état de lecture. */
@Composable
private fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val strings = LocalHubStrings.current
    val otherInfo = conversation.getOtherParticipantInfo(currentUserId)
    val otherUid = conversation.getOtherParticipantId(currentUserId)
    val liveUser = rememberLiveUser(
        userId = otherUid,
        fallbackUsername = otherInfo.username,
        fallbackDisplayName = otherInfo.displayName,
        fallbackPhotoUrl = otherInfo.photoUrl
    )
    val unreadCount = conversation.getUnreadCountFor(currentUserId)
    val hasUnread = unreadCount > 0
    val displayName = liveUser.effectiveName
    val sentByMe = conversation.lastMessageSenderId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
            .testTag("conversation_item_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(
            name = displayName,
            photoUrl = liveUser.photoUrl,
            size = 48.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            HubVerifiedUserName(
                name = displayName,
                isVerified = liveUser.isVerified,
                verificationType = liveUser.verificationType,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 13.5.sp,
                color = HubWhite,
                weight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
                badgeSize = 15.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.lastMessageText.ifBlank {
                    strings.lot4.emptyConversationPreview
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasUnread) HubWhite.copy(alpha = 0.68f) else HubTextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Colonne droite : heure + coche de lecture ou badge non-lu.
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = RelativeTime.formatConversationDate(conversation.lastMessageAt),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasUnread) HubWhite.copy(alpha = 0.68f) else HubTextTertiary
            )
            when {
                hasUnread -> {
                    // Badge .bdg : dégradé de marque, pilule 19 dp, lueur violette.
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(10.dp),
                                clip = false,
                                ambientColor = Color.Transparent,
                                spotColor = Color(0xFF7C5CFF).copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush = hubPrimaryGradient())
                            .padding(horizontal = 6.dp)
                            .height(19.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                sentByMe -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = strings.lot4.readReceiptDesc,
                        tint = HubAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/** État vide .empty — icône bulle dans carte de verre + action. */
@Composable
private fun MessengerEmptyState(
    isSearch: Boolean,
    onNewMessageClick: () -> Unit
) {
    val strings = LocalHubStrings.current
    HubEmptyState(
        title = if (isSearch) strings.lot4.searchNoMatchTitle else strings.lot4.emptyConvTitle,
        description = if (isSearch) strings.lot4.searchNoMatchDesc else strings.lot4.emptyConvDesc,
        icon = Icons.Outlined.ChatBubbleOutline,
        actionLabel = if (isSearch) null else strings.lot4.startConversationCta,
        onAction = if (isSearch) null else onNewMessageClick,
        modifier = Modifier.testTag("messenger_empty_state")
    )
}

/** Chargement : squelettes de rangées (avatar + deux lignes .skel). */
@Composable
private fun MessengerLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .testTag("messenger_skeleton")
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HubGlassStrong)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    HubSkeletonLine(height = 13.dp, widthFraction = 0.45f)
                    Spacer(modifier = Modifier.height(8.dp))
                    HubSkeletonLine(height = 11.dp, widthFraction = 0.72f)
                }
            }
        }
    }
}

/**
 * Bouton icône .iconbtn de la maquette : 40 dp, rayon 14, fond --glass,
 * contour --stroke (même motif que les écrans du lot 3).
 */
@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HubGlass)
            .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
