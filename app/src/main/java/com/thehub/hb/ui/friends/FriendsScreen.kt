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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Écran 20 — Amis : appbar (retour + titre/sous-titre + recherche), segmenté
 * Amis / Abonnés / Abonnements, sections en cartes verre, rangées avatar 42.
 */
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("friends_screen")
    ) {
        // .appbar — retour verre + titre + sous-titre + recherche verre.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HubGlass)
                    .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateBack)
                    .testTag("friends_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = HubWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.lot5.friendsTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite
                )
                Text(
                    text = strings.lot5.friendsSubtitleFormat.format(uiState.users.size),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubTextTertiary,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HubGlass)
                    .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                    .clickable {
                        if (uiState.selectedTab == FriendsTab.DISCOVER) {
                            viewModel.onSearchQueryChanged("")
                            viewModel.selectTab(FriendsTab.FRIENDS)
                            focusManager.clearFocus()
                        } else {
                            viewModel.selectTab(FriendsTab.DISCOVER)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.selectedTab == FriendsTab.DISCOVER) {
                        Icons.Outlined.Close
                    } else {
                        Icons.Outlined.Search
                    },
                    contentDescription = strings.lot5.friendsSearchDesc,
                    tint = HubWhite,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        when (uiState.selectedTab) {
            FriendsTab.DISCOVER -> {
                // Recherche d'amis : champ verre + résultats.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .imePadding()
                ) {
                    FriendsSearchField(
                        query = uiState.searchQuery,
                        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friends_discover_search_input")
                    )

                    Spacer(Modifier.height(12.dp))

                    when {
                        uiState.isSearchLoading -> FriendsSkeletonList()

                        uiState.searchQuery.isBlank() -> FriendsEmptyState(
                            text = strings.lot5.friendsSearchHint
                        )

                        uiState.searchResults.isEmpty() -> FriendsEmptyState(
                            text = strings.lot5.friendsNoResult
                        )

                        else -> FriendsCardList(
                            users = uiState.searchResults,
                            currentUserId = viewModel.currentUserId,
                            followingMap = uiState.followingMap,
                            actionLoadingMap = uiState.actionLoadingMap,
                            onUserClick = onUserClick,
                            onToggleFollow = viewModel::toggleFollowUser
                        )
                    }
                }
            }

            else -> {
                // Segmenté .seg — Amis · N / Abonnés · N / Abonnements · N.
                FriendsSegment(
                    selectedTab = uiState.selectedTab,
                    selectedConnectionsTab = uiState.connectionsTab,
                    friendsCount = uiState.users.size,
                    followersCount = uiState.followers.size,
                    followingCount = uiState.following.size,
                    onSelectFriends = { viewModel.selectTab(FriendsTab.FRIENDS) },
                    onSelectFollowers = {
                        viewModel.selectTab(FriendsTab.CONNECTIONS)
                        viewModel.selectConnectionsTab(ConnectionsTab.FOLLOWERS)
                    },
                    onSelectFollowing = {
                        viewModel.selectTab(FriendsTab.CONNECTIONS)
                        viewModel.selectConnectionsTab(ConnectionsTab.FOLLOWING)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                )

                when (uiState.selectedTab) {
                    FriendsTab.FRIENDS -> {
                        if (uiState.isLoading) {
                            FriendsSkeletonList()
                        } else if (uiState.users.isEmpty()) {
                            FriendsEmptyState(text = strings.lot5.friendsEmpty)
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                FriendsSectionTitle(text = strings.lot5.friendsSectionTitle)
                                FriendsCardList(
                                    users = uiState.users,
                                    currentUserId = viewModel.currentUserId,
                                    followingMap = uiState.followingMap,
                                    actionLoadingMap = uiState.actionLoadingMap,
                                    onUserClick = onUserClick,
                                    onToggleFollow = viewModel::toggleFollowUser
                                )
                            }
                        }
                    }

                    FriendsTab.CONNECTIONS -> {
                        val users = when (uiState.connectionsTab) {
                            ConnectionsTab.FOLLOWERS -> uiState.followers
                            ConnectionsTab.FOLLOWING -> uiState.following
                        }

                        if (uiState.isLoading) {
                            FriendsSkeletonList()
                        } else if (users.isEmpty()) {
                            FriendsEmptyState(
                                text = if (uiState.connectionsTab == ConnectionsTab.FOLLOWERS) {
                                    strings.lot5.friendsEmptyFollowers
                                } else {
                                    strings.lot5.friendsEmptyFollowing
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                FriendsSectionTitle(
                                    text = if (uiState.connectionsTab == ConnectionsTab.FOLLOWERS) {
                                        strings.lot5.friendsTabFollowers
                                    } else {
                                        strings.lot5.friendsTabFollowing
                                    }
                                )
                                FriendsCardList(
                                    users = users,
                                    currentUserId = viewModel.currentUserId,
                                    followingMap = uiState.followingMap,
                                    actionLoadingMap = uiState.actionLoadingMap,
                                    onUserClick = onUserClick,
                                    onToggleFollow = viewModel::toggleFollowUser
                                )
                            }
                        }
                    }

                    FriendsTab.DISCOVER -> Unit
                }
            }
        }
    }
}

/** Segmenté .seg — fond verre doux, segment actif verre fort + liseré. */
@Composable
private fun FriendsSegment(
    selectedTab: FriendsTab,
    selectedConnectionsTab: ConnectionsTab,
    friendsCount: Int,
    followersCount: Int,
    followingCount: Int,
    onSelectFriends: () -> Unit,
    onSelectFollowers: () -> Unit,
    onSelectFollowing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(HubGlassStrong.copy(alpha = 0.45f))
            .border(1.dp, HubStrokeSoft, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FriendsSegmentButton(
            text = strings.lot5.friendsTabFriends + " · $friendsCount",
            selected = selectedTab == FriendsTab.FRIENDS,
            onClick = onSelectFriends,
            modifier = Modifier.weight(1f)
        )
        FriendsSegmentButton(
            text = strings.lot5.friendsTabFollowers + " · $followersCount",
            selected = selectedTab == FriendsTab.CONNECTIONS &&
                selectedConnectionsTab == ConnectionsTab.FOLLOWERS,
            onClick = onSelectFollowers,
            modifier = Modifier.weight(1f)
        )
        FriendsSegmentButton(
            text = strings.lot5.friendsTabFollowing + " · $followingCount",
            selected = selectedTab == FriendsTab.CONNECTIONS &&
                selectedConnectionsTab == ConnectionsTab.FOLLOWING,
            onClick = onSelectFollowing,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Segment du contrôleur — actif : verre fort + texte plein. */
@Composable
private fun FriendsSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(HubGlassStrong, shape)
                        .border(1.dp, HubStroke, shape)
                } else {
                    Modifier.background(androidx.compose.ui.graphics.Color.Transparent)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) HubWhite else HubTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Titre de section .section-title — 12 / 800 / interlettre 1.2. */
@Composable
private fun FriendsSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = HubTextTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 10.dp, bottom = 8.dp)
    )
}

/** Liste en carte verre unique — rangées avatar 42 + bouton xs. */
@Composable
private fun FriendsCardList(
    users: List<User>,
    currentUserId: String?,
    followingMap: Map<String, Boolean>,
    actionLoadingMap: Map<String, Boolean>,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GlassSurface(
                shape = RoundedCornerShape(22.dp),
                variant = GlassVariant.Card,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    users.forEachIndexed { index, user ->
                        FriendUserRow(
                            user = user,
                            isMe = user.uid == currentUserId,
                            isFollowing = followingMap[user.uid] == true,
                            isActionLoading = actionLoadingMap[user.uid] == true,
                            showTopDivider = index > 0,
                            onUserClick = { onUserClick(user.uid) },
                            onToggleFollow = { onToggleFollow(user.uid) }
                        )
                    }
                }
            }
        }
    }
}

/** Rangée ami : avatar 42, nom vérifié + @pseudo, bouton verre/dégradé xs. */
@Composable
private fun FriendUserRow(
    user: User,
    isMe: Boolean,
    isFollowing: Boolean,
    isActionLoading: Boolean,
    showTopDivider: Boolean,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val strings = LocalHubStrings.current
    val liveUser = rememberLiveUser(
        userId = user.uid,
        fallbackUsername = user.username,
        fallbackDisplayName = user.displayName,
        fallbackPhotoUrl = user.photoUrl
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(1.dp)
                    .background(HubStrokeSoft)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onUserClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("friend_row_${user.uid}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                name = liveUser.effectiveName,
                photoUrl = liveUser.photoUrl,
                size = 42.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                HubVerifiedUserName(
                    name = liveUser.effectiveName,
                    isVerified = liveUser.isVerified,
                    verificationType = liveUser.verificationType,
                    fontSize = 13.5.sp,
                    color = HubWhite,
                    badgeSize = 14.dp
                )
                Text(
                    text = "@${liveUser.username}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = HubTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (!isMe) {
                FriendsActionButton(
                    text = if (isFollowing) {
                        strings.lot5.followListUnfollowBtn
                    } else {
                        strings.lot5.followListSubscribeBtn
                    },
                    isFollowing = isFollowing,
                    isActionLoading = isActionLoading,
                    onClick = onToggleFollow,
                    modifier = Modifier.testTag("friend_follow_button_${user.uid}")
                )
            }
        }
    }
}

/** Champ de recherche .input — icône accent, effacement. */
@Composable
private fun FriendsSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHubColors.current
    val strings = LocalHubStrings.current
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(palette.field, shape)
            .border(1.dp, palette.strokeSoft, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(17.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HubWhite
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text(
                    text = strings.lot5.friendsSearchPlaceholder,
                    fontSize = 14.sp,
                    color = HubTextTertiary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.lot4.searchClearDesc,
                tint = HubTextTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChanged("") }
            )
        }
    }
}

/** Bouton xs : dégradé (« S'abonner ») ou verre (« Se désabonner »). */
@Composable
private fun FriendsActionButton(
    text: String,
    isFollowing: Boolean,
    isActionLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isFollowing) {
                    Modifier
                        .background(HubGlassStrong, shape)
                        .border(1.dp, HubStroke, shape)
                } else {
                    Modifier.background(brush = hubPrimaryGradient(), shape = shape)
                }
            )
            .clickable(enabled = !isActionLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )
    }
}

/** État vide : HubEmptyState (icône verre). */
@Composable
private fun FriendsEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("friends_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        HubEmptyState(
            title = LocalHubStrings.current.lot5.friendsTitle,
            description = text,
            icon = Icons.Outlined.PeopleAlt
        )
    }
}

/** État « chargement » : rangées squelettes verre. */
@Composable
private fun FriendsSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        GlassSurface(
            shape = RoundedCornerShape(22.dp),
            variant = GlassVariant.Card,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                repeat(4) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(HubGlassStrong)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            HubSkeletonLine(height = 13.dp, widthFraction = 0.45f)
                            Spacer(Modifier.height(6.dp))
                            HubSkeletonLine(height = 11.dp, widthFraction = 0.3f)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}
