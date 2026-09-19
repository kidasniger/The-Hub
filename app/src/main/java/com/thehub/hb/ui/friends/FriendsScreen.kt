package com.thehub.hb.ui.friends

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("friends_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("friends_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = "Amis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = HubWhite,
                edgePadding = 16.dp,
                divider = { HorizontalDivider(color = HubBorder) }
            ) {
                Tab(
                    selected = uiState.selectedTab == FriendsTab.FRIENDS,
                    onClick = { viewModel.selectTab(FriendsTab.FRIENDS) },
                    text = { Text("Mes amis") }
                )
                Tab(
                    selected = uiState.selectedTab == FriendsTab.CONNECTIONS,
                    onClick = { viewModel.selectTab(FriendsTab.CONNECTIONS) },
                    text = { Text("Abonnés / abonnements") }
                )
                Tab(
                    selected = uiState.selectedTab == FriendsTab.DISCOVER,
                    onClick = { viewModel.selectTab(FriendsTab.DISCOVER) },
                    text = { Text("Trouver des amis") }
                )
            }

            when (uiState.selectedTab) {
                FriendsTab.FRIENDS -> {
                    if (uiState.isLoading) {
                        LoadingState(modifier = Modifier.weight(1f))
                    } else if (uiState.users.isEmpty()) {
                        EmptyFriendsState(
                            text = "Pas encore d'amis en commun.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        UserList(
                            users = uiState.users,
                            currentUserId = viewModel.currentUserId,
                            followingMap = uiState.followingMap,
                            actionLoadingMap = uiState.actionLoadingMap,
                            onUserClick = onUserClick,
                            onToggleFollow = viewModel::toggleFollowUser
                        )
                    }
                }

                FriendsTab.CONNECTIONS -> {
                    ConnectionSwitcher(
                        selected = uiState.connectionsTab,
                        onSelected = viewModel::selectConnectionsTab
                    )

                    val users = when (uiState.connectionsTab) {
                        ConnectionsTab.FOLLOWERS -> uiState.followers
                        ConnectionsTab.FOLLOWING -> uiState.following
                    }

                    if (uiState.isLoading) {
                        LoadingState(modifier = Modifier.weight(1f))
                    } else if (users.isEmpty()) {
                        EmptyFriendsState(
                            text = if (uiState.connectionsTab == ConnectionsTab.FOLLOWERS) {
                                "Aucun abonné pour le moment."
                            } else {
                                "Vous ne suivez encore personne."
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        UserList(
                            users = users,
                            currentUserId = viewModel.currentUserId,
                            followingMap = uiState.followingMap,
                            actionLoadingMap = uiState.actionLoadingMap,
                            onUserClick = onUserClick,
                            onToggleFollow = viewModel::toggleFollowUser
                        )
                    }
                }

                FriendsTab.DISCOVER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        HubTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = "Trouver des amis par nom ou pseudo...",
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
                                .testTag("friends_discover_search_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        when {
                            uiState.isSearchLoading -> LoadingState(modifier = Modifier.weight(1f))
                            uiState.searchQuery.isBlank() -> EmptyFriendsState(
                                text = "Recherchez un nom ou un pseudo pour trouver de nouvelles personnes.",
                                modifier = Modifier.weight(1f)
                            )
                            uiState.searchResults.isEmpty() -> EmptyFriendsState(
                                text = "Aucun utilisateur trouvé.",
                                modifier = Modifier.weight(1f)
                            )
                            else -> UserList(
                                users = uiState.searchResults,
                                currentUserId = viewModel.currentUserId,
                                followingMap = uiState.followingMap,
                                actionLoadingMap = uiState.actionLoadingMap,
                                onUserClick = onUserClick,
                                onToggleFollow = viewModel::toggleFollowUser,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionSwitcher(
    selected: ConnectionsTab,
    onSelected: (ConnectionsTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(HubSurface)
            .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ConnectionsTab.entries.forEach { tab ->
            val selectedTab = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selectedTab) HubSurfaceElevated else HubSurface)
                    .clickable { onSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (tab == ConnectionsTab.FOLLOWERS) "Abonnés" else "Abonnements",
                    color = if (selectedTab) HubWhite else HubSecondary,
                    fontWeight = if (selectedTab) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun UserList(
    users: List<User>,
    currentUserId: String?,
    followingMap: Map<String, Boolean>,
    actionLoadingMap: Map<String, Boolean>,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.uid }) { user ->
            FriendUserRow(
                user = user,
                isMe = user.uid == currentUserId,
                isFollowing = followingMap[user.uid] == true,
                isActionLoading = actionLoadingMap[user.uid] == true,
                onUserClick = { onUserClick(user.uid) },
                onToggleFollow = { onToggleFollow(user.uid) }
            )
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = HubWhite,
            strokeWidth = 2.dp,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun EmptyFriendsState(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("friends_empty_state"),
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
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HubSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FriendUserRow(
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
            .clip(MaterialTheme.shapes.medium)
            .background(HubSurface)
            .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
            .clickable(onClick = onUserClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("friend_row_${user.uid}"),
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
            Text(
                text = "@${liveUser.username}",
                fontSize = 13.sp,
                color = HubSecondary,
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
                        .testTag("friend_follow_button_${user.uid}")
                )
            }
        }
    }
}
