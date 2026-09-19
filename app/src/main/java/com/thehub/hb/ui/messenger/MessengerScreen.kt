package com.thehub.hb.ui.messenger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.RelativeTime

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("messenger_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("messenger_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = HubWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Messages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                }

                IconButton(
                    onClick = onNewMessageClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HubSurfaceElevated)
                        .testTag("new_message_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouveau message",
                        tint = HubWhite
                    )
                }
            }

            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                HubTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = "Rechercher des conversations...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = HubMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer la recherche",
                                    tint = HubMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    minHeight = 44,
                    testTag = "messenger_search_input"
                )
            }

            // Content
            when (val state = uiState) {
                is MessengerUiState.Loading -> {
                    MessengerLoadingSkeleton()
                }

                is MessengerUiState.Success -> {
                    if (state.filteredConversations.isEmpty()) {
                        MessengerEmptyState(
                            isSearch = searchQuery.isNotBlank(),
                            onNewMessageClick = onNewMessageClick
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("conversations_list")
                        ) {
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
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                is MessengerUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = HubMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Floating Action Button for quick new message
        FloatingActionButton(
            onClick = onNewMessageClick,
            containerColor = HubWhite,
            contentColor = HubBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("messenger_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Écrire un message",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_item_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = displayName,
            photoUrl = liveUser.photoUrl,
            size = 52.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = HubWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = RelativeTime.formatConversationDate(conversation.lastMessageAt),
                    fontSize = 12.sp,
                    color = if (hasUnread) HubWhite else HubMuted,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val preview = conversation.lastMessageText.ifBlank { "Nouvelle conversation" }
                Text(
                    text = preview,
                    fontSize = 14.sp,
                    color = if (hasUnread) HubWhite else HubMuted,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(HubWhite)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = HubBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessengerEmptyState(
    isSearch: Boolean,
    onNewMessageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("messenger_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(HubSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = HubSecondary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearch) "Aucun résultat trouvé" else "Aucune conversation",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearch) {
                "Vérifiez l'orthographe ou commencez un nouveau message."
            } else {
                "Envoyez un message privé avec du texte et des photos à vos contacts."
            },
            fontSize = 14.sp,
            color = HubMuted,
            lineHeight = 20.sp
        )

        if (!isSearch) {
            Spacer(modifier = Modifier.height(24.dp))
            HubButton(
                text = "Démarrer une conversation",
                onClick = onNewMessageClick,
                modifier = Modifier.width(220.dp),
                testTag = "start_conversation_button"
            )
        }
    }
}

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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HubSurfaceElevated)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HubSurfaceElevated)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(HubSurface)
                            .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
                    )
                }
            }
        }
    }
}
