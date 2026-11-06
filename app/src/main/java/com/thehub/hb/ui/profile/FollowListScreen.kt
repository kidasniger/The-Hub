package com.thehub.hb.ui.profile

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Liste abonnés / abonnements — maquette 19 : appbar retour + titre + recherche,
 * rangées verre (avatar 42, nom vérifié, bouton xs dégradé ou verre).
 */
@Composable
fun FollowListScreen(
    viewModel: FollowListViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var showSearch by remember { mutableStateOf(false) }
    val title = if (uiState.type == FollowListType.FOLLOWERS) {
        strings.lot5.followListFollowersTitle
    } else {
        strings.lot5.followListFollowingTitle
    }
    val subtitle = if (uiState.type == FollowListType.FOLLOWERS) {
        strings.lot5.followListSubFollowersFormat.format(uiState.users.size)
    } else {
        strings.lot5.followListSubFollowingFormat.format(uiState.users.size)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("follow_list_screen")
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
                    .testTag("follow_list_back_button"),
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
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
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
                        showSearch = !showSearch
                        if (!showSearch) {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = strings.searchPlaceholder,
                    tint = HubWhite,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Champ de filtre (verre .input) affiché à la demande.
        if (showSearch) {
            FollowListSearchField(
                query = uiState.searchQuery,
                onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 12.dp)
                    .testTag("follow_list_search_input")
            )
        }

        when {
            uiState.isLoading -> FollowListSkeleton()

            uiState.filteredUsers.isEmpty() -> {
                val message = if (uiState.searchQuery.isNotBlank()) {
                    strings.lot5.followListNoResultFormat.format(uiState.searchQuery)
                } else if (uiState.type == FollowListType.FOLLOWERS) {
                    strings.lot5.followListEmptyFollowers
                } else {
                    strings.lot5.followListEmptyFollowing
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HubEmptyState(
                        title = if (uiState.searchQuery.isNotBlank()) {
                            strings.lot5.searchNoResultTitle
                        } else {
                            title
                        },
                        description = message,
                        icon = Icons.Outlined.PeopleAlt,
                        actionLabel = if (uiState.searchQuery.isNotBlank()) {
                            strings.lot4.searchClearDesc
                        } else {
                            null
                        },
                        onAction = if (uiState.searchQuery.isNotBlank()) {
                            { viewModel.onSearchQueryChanged("") }
                        } else {
                            null
                        }
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 2.dp,
                        bottom = 20.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        GlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            variant = GlassVariant.Card,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                uiState.filteredUsers.forEachIndexed { index, user ->
                                    FollowUserRow(
                                        user = user,
                                        isMe = user.uid == viewModel.currentUserId,
                                        isFollowing = uiState.followingMap[user.uid] == true,
                                        isActionLoading =
                                            uiState.actionLoadingMap[user.uid] == true,
                                        showTopDivider = index > 0,
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
    }
}

/** Champ de filtre .input — icône accent, effacement, clavier recherche. */
@Composable
private fun FollowListSearchField(
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
            .fillMaxWidth()
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
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text(
                    text = strings.searchPlaceholder,
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

/** Rangée de la maquette : avatar 42 + nom vérifié + @pseudo + bouton xs. */
@Composable
private fun FollowUserRow(
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
                .testTag("follow_user_row_${user.uid}"),
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
                FollowListActionButton(
                    text = if (isFollowing) {
                        strings.lot5.followListUnfollowBtn
                    } else {
                        strings.lot5.followListSubscribeBtn
                    },
                    isFollowing = isFollowing,
                    isActionLoading = isActionLoading,
                    onClick = onToggleFollow,
                    modifier = Modifier.testTag("follow_button_${user.uid}")
                )
            }
        }
    }
}

/** Bouton xs de la maquette : dégradé (« S'abonner ») ou verre (« Se désabonner »). */
@Composable
private fun FollowListActionButton(
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

/** État « chargement » : rangées squelettes en verre. */
@Composable
private fun FollowListSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
