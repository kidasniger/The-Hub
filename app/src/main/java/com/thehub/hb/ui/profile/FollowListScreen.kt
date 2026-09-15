package com.thehub.hb.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun FollowListScreen(
    viewModel: FollowListViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val title = if (uiState.type == FollowListType.FOLLOWERS) "Abonnés" else "Abonnements"

    Scaffold(
        containerColor = HubBlack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("follow_list_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("follow_list_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Search Bar Filter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                HubTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = "Rechercher...",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = HubSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    tint = HubSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("follow_list_search_input")
                )
            }

            HorizontalDivider(color = HubBorder, thickness = 1.dp)

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                uiState.filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(HubSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = HubSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    "Aucun résultat trouvé pour \"${uiState.searchQuery}\""
                                } else if (uiState.type == FollowListType.FOLLOWERS) {
                                    "Aucun abonné pour l'instant"
                                } else {
                                    "Ne suit personne pour l'instant"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = HubSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.filteredUsers,
                            key = { it.uid }
                        ) { user ->
                            val isMe = user.uid == viewModel.currentUserId
                            val isFollowing = uiState.followingMap[user.uid] == true
                            val isActionLoading = uiState.actionLoadingMap[user.uid] == true

                            FollowUserRow(
                                user = user,
                                isMe = isMe,
                                isFollowing = isFollowing,
                                isActionLoading = isActionLoading,
                                onUserClick = { onUserClick(user.uid) },
                                onToggleFollow = { viewModel.toggleFollowUser(user.uid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(
    user: User,
    isMe: Boolean,
    isFollowing: Boolean,
    isActionLoading: Boolean,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val liveUser = rememberLiveUser(
        userId = user.uid,
        fallbackUsername = user.username,
        fallbackDisplayName = user.displayName,
        fallbackPhotoUrl = user.photoUrl
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HubCard)
            .clickable(onClick = onUserClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("follow_user_row_${user.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = liveUser.effectiveName,
            photoUrl = liveUser.photoUrl,
            size = 46.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = liveUser.effectiveName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!isMe) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.width(110.dp)) {
                HubButton(
                    text = if (isFollowing) "Abonné" else "Suivre",
                    onClick = onToggleFollow,
                    isLoading = isActionLoading,
                    enabled = !isActionLoading,
                    variant = if (isFollowing) HubButtonVariant.Secondary else HubButtonVariant.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("follow_button_${user.uid}")
                )
            }
        }
    }
}
