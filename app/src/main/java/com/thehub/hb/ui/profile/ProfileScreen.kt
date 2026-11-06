package com.thehub.hb.ui.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.ui.components.BlockUserConfirmationDialog
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.OfficialVerificationBadge
import com.thehub.hb.ui.components.ReportBottomSheet
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.HubBlue
import com.thehub.hb.ui.theme.HubViolet
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Onglets de présentation de la maquette : Publications / Médias / J'aime. */
private enum class ProfileTab { POSTS, MEDIA, LIKES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToFollowers: (String) -> Unit,
    onNavigateToFollowing: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onOpenComments: (String) -> Unit = {},
    onOpenLikes: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onVideoClick: ((String) -> Unit)? = null,
    isBottomTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var postToShare by remember { mutableStateOf<Post?>(null) }
    var activeTab by remember { mutableStateOf(ProfileTab.POSTS) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("profile_screen")
    ) {
        when {
            uiState.isLoading -> ProfileSkeleton()

            uiState.isBlocked -> BlockedProfileView(
                isBlockedByMe = uiState.isBlockedByMe,
                isActionLoading = uiState.isActionLoading,
                onUnblock = { viewModel.unblockUser() }
            )

            uiState.user == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                HubEmptyState(
                    title = strings.lot5.profileNotFound,
                    description = strings.lot5.profileNotFound,
                    icon = Icons.Outlined.CloudOff,
                    actionLabel = strings.lot3.retryLabel,
                    onAction = { viewModel.loadProfile() }
                )
            }

            else -> {
                val user = uiState.user!!
                val pullRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = pullRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("profile_posts_grid"),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // — En-tête complet (bannière, identité, stats, actions, onglets).
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // .appbar — logo 30 + titre nom + engrenage / options.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isBottomTab && onNavigateBack != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(HubGlass)
                                                .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                                                .clickable(onClick = onNavigateBack)
                                                .testTag("profile_back_button"),
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
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(brush = hubPrimaryGradient()),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "H",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Text(
                                        text = user.displayName?.takeIf { it.isNotBlank() }
                                            ?: user.username,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.3).sp,
                                        color = HubWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (uiState.isOwnProfile) {
                                        // Signets (fonction existante) + Paramètres.
                                        ProfileIconAction(
                                            onClick = onNavigateToBookmarks,
                                            modifier = Modifier.testTag("profile_bookmarks_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BookmarkBorder,
                                                contentDescription = strings.lot5.bookmarksTitle,
                                                tint = HubWhite,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        ProfileIconAction(
                                            onClick = onNavigateToSettings,
                                            modifier = Modifier.testTag("profile_settings_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = strings.settingsTitle,
                                                tint = HubWhite,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    } else {
                                        ProfileIconAction(
                                            onClick = { showMenu = true },
                                            modifier = Modifier.testTag("profile_more_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = strings.lot3.postOptionsDesc,
                                                tint = HubWhite,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Bannière dégradée 108 / rayon 26 + halo clair.
                                Box(
                                    modifier = Modifier
                                        .padding(start = 14.dp, end = 14.dp, top = 4.dp)
                                        .fillMaxWidth()
                                        .height(108.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    HubViolet,
                                                    HubBlue,
                                                    LocalHubColors.current.accent2
                                                )
                                            )
                                        )
                                        .testTag("profile_banner"),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.35f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Avatar 84 chevauchant la bannière de 30.
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pullUp(30.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier.testTag("profile_avatar")
                                        ) {
                                            UserAvatar(
                                                name = user.displayName ?: user.username,
                                                photoUrl = user.photoUrl,
                                                size = 84.dp,
                                                modifier = Modifier.border(
                                                    3.dp,
                                                    LocalHubColors.current.card,
                                                    CircleShape
                                                )
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (uiState.isOwnProfile) {
                                                ProfileIconAction(
                                                    onClick = onNavigateToBookmarks,
                                                    small = true
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.BookmarkBorder,
                                                        contentDescription = strings.lot5.bookmarksTitle,
                                                        tint = HubWhite,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                ProfileIconAction(
                                                    small = true,
                                                    onClick = {
                                                        shareProfile(
                                                            context,
                                                            strings.lot5.profileShareText
                                                                .format(viewModel.resolvedUserId)
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Share,
                                                        contentDescription = strings.share,
                                                        tint = HubWhite,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            } else {
                                                ProfileIconAction(
                                                    small = true,
                                                    onClick = { viewModel.toggleFollow() }
                                                ) {
                                                    Icon(
                                                        imageVector = if (uiState.isFollowing) {
                                                            Icons.Filled.PersonRemove
                                                        } else {
                                                            Icons.Filled.PersonAdd
                                                        },
                                                        contentDescription = if (uiState.isFollowing) {
                                                            strings.unfollow
                                                        } else {
                                                            strings.follow
                                                        },
                                                        tint = HubWhite,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                if (uiState.isMessagingAllowed) {
                                                    ProfileIconAction(
                                                        small = true,
                                                        onClick = {
                                                            viewModel.openChat { convId ->
                                                                onNavigateToChat(convId)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.ChatBubbleOutline,
                                                            contentDescription = strings.lot5.profileMessageBtn,
                                                            tint = HubWhite,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Identité : nom 18 + badge vérifié, @pseudo, bio.
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp)
                                            .padding(top = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.displayName?.takeIf { it.isNotBlank() }
                                                    ?: user.username,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.2).sp,
                                                color = HubWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.verificationType == "admin") {
                                                Spacer(Modifier.width(6.dp))
                                                OfficialVerificationBadge(size = 16.dp)
                                            }
                                        }

                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HubTextTertiary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        if (!user.bio.isNullOrBlank()) {
                                            Text(
                                                text = user.bio,
                                                fontSize = 12.5.sp,
                                                lineHeight = 18.sp,
                                                color = HubTextSecondary,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }

                                        // Pilule date : naissance (mois année) sinon ancienneté.
                                        val pillDate = remember(user.birthdate, user.createdAt) {
                                            profilePillDate(user)
                                        }
                                        if (pillDate != null) {
                                            Row(
                                                modifier = Modifier.padding(top = 9.dp),
                                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                                            ) {
                                                ProfilePill(
                                                    text = pillDate,
                                                    icon = {
                                                        Icon(
                                                            imageVector = Icons.Outlined.CalendarMonth,
                                                            contentDescription = null,
                                                            tint = HubSecondary,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                    },
                                                    accent = false
                                                )
                                            }
                                        }
                                    }

                                    // Stats verre doux : Publications / Abonnés / Abonnements.
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 18.dp, vertical = 14.dp)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(LocalHubColors.current.glassSoft)
                                            .border(
                                                1.dp,
                                                HubStrokeSoft,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 13.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ProfileStatItem(
                                                count = user.postsCount,
                                                label = strings.lot5.profileStatPosts,
                                                onClick = null,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("profile_stat_posts")
                                            )
                                            StatDivider()
                                            ProfileStatItem(
                                                count = user.followersCount,
                                                label = strings.lot5.profileStatFollowers,
                                                onClick = { onNavigateToFollowers(user.uid) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("profile_stat_followers")
                                            )
                                            StatDivider()
                                            ProfileStatItem(
                                                count = user.followingCount,
                                                label = strings.lot5.profileStatFollowing,
                                                onClick = { onNavigateToFollowing(user.uid) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("profile_stat_following")
                                            )
                                        }
                                    }

                                    // Actions principales : Modifier / Partager · Suivre / Message.
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (uiState.isOwnProfile) {
                                            ProfileActionButton(
                                                text = strings.editProfile,
                                                filled = true,
                                                onClick = onNavigateToEditProfile,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("edit_profile_button")
                                            )
                                            ProfileActionButton(
                                                text = strings.share,
                                                filled = false,
                                                onClick = {
                                                    shareProfile(
                                                        context,
                                                        strings.lot5.profileShareText
                                                            .format(viewModel.resolvedUserId)
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            ProfileActionButton(
                                                text = if (uiState.isFollowing) {
                                                    strings.unfollow
                                                } else {
                                                    strings.follow
                                                },
                                                filled = !uiState.isFollowing,
                                                onClick = { viewModel.toggleFollow() },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("follow_unfollow_button")
                                            )
                                            if (uiState.isMessagingAllowed) {
                                                ProfileActionButton(
                                                    text = strings.lot5.profileMessageBtn,
                                                    filled = false,
                                                    onClick = {
                                                        viewModel.openChat { convId ->
                                                            onNavigateToChat(convId)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("message_user_button")
                                                )
                                            } else {
                                                ProfileActionButton(
                                                    text = strings.lot5.profileOfficialBtn,
                                                    filled = false,
                                                    onClick = {},
                                                    enabled = false,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // Onglets soulignés : Publications / Médias / J'aime.
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 18.dp, top = 14.dp)
                                            .background(Color.Transparent),
                                        horizontalArrangement = Arrangement.spacedBy(26.dp)
                                    ) {
                                        ProfileTabLabel(
                                            text = strings.lot5.profileTabPosts,
                                            selected = activeTab == ProfileTab.POSTS,
                                            onClick = { activeTab = ProfileTab.POSTS }
                                        )
                                        ProfileTabLabel(
                                            text = strings.lot5.profileTabMedia,
                                            selected = activeTab == ProfileTab.MEDIA,
                                            onClick = { activeTab = ProfileTab.MEDIA }
                                        )
                                        ProfileTabLabel(
                                            text = strings.lot5.profileTabLikes,
                                            selected = activeTab == ProfileTab.LIKES,
                                            onClick = { activeTab = ProfileTab.LIKES }
                                        )
                                        Spacer(Modifier.weight(1f))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 18.dp, top = 2.dp)
                                            .height(1.dp)
                                            .background(HubStrokeSoft)
                                    )
                                }
                                }
                                }

                                // Contenu de l'onglet (grille 3 carrés, gap 5).
                                val tabPosts = when (activeTab) {
                                    ProfileTab.POSTS -> uiState.posts
                                    ProfileTab.MEDIA -> uiState.posts.filter {
                                        !it.imageUrl.isNullOrBlank() || !it.videoUrl.isNullOrBlank()
                                    }

                                    ProfileTab.LIKES -> uiState.posts.filter {
                                        it.isLikedByCurrentUser
                                    }
                                }

                                if (tabPosts.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HubEmptyState(
                                                title = when (activeTab) {
                                                    ProfileTab.POSTS -> strings.noPostsYet
                                                    ProfileTab.MEDIA -> strings.lot5.profileMediaEmpty
                                                    ProfileTab.LIKES -> strings.lot5.profileLikesEmpty
                                                },
                                                description = when (activeTab) {
                                                    ProfileTab.POSTS -> strings.noPostsDescription
                                                    ProfileTab.MEDIA -> strings.lot5.profileMediaEmpty
                                                    ProfileTab.LIKES -> strings.lot5.profileLikesEmpty
                                                },
                                                icon = when (activeTab) {
                                                    ProfileTab.POSTS -> Icons.Outlined.DynamicFeed
                                                    ProfileTab.MEDIA -> Icons.Outlined.Image
                                                    ProfileTab.LIKES -> Icons.Outlined.FavoriteBorder
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    items(tabPosts, key = { it.id }) { post ->
                                        ProfileGridTile(
                                            post = post,
                                            onClick = { onPostClick(post.id) },
                                            onImageClick = {
                                                post.imageUrl?.let(onImageClick)
                                            }
                                        )
                                    }
                                }
                        }
                    }

                // Menu contextuel des autres profils.
                if (!uiState.isOwnProfile) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(LocalHubColors.current.card)
                                .testTag("profile_overflow_menu")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Flag,
                                            contentDescription = null,
                                            tint = HubWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(strings.reportPost, color = HubWhite)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showReportSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = null,
                                            tint = HubWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(strings.lot5.profileBlockAction, color = HubWhite)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showBlockDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        postToShare?.let { post ->
            SharePostBottomSheet(
                post = post,
                onDismiss = { postToShare = null },
                onRepost = { viewModel.repost(it) }
            )
        }

        if (showReportSheet) {
            ReportBottomSheet(
                targetType = "user",
                targetId = viewModel.resolvedUserId,
                targetName = if (uiState.user?.username.isNullOrBlank()) {
                    ""
                } else {
                    "@${uiState.user?.username}"
                },
                onDismiss = { showReportSheet = false },
                onSubmitReport = { reason, details ->
                    viewModel.reportUser(reason, details) { showReportSheet = false }
                },
                isSubmitting = uiState.isActionLoading
            )
        }

        BlockUserConfirmationDialog(
            username = uiState.user?.username ?: "",
            onConfirm = {
                viewModel.blockUser { showBlockDialog = false }
            },
            onDismiss = { if (!uiState.isActionLoading) showBlockDialog = false },
            isLoading = uiState.isActionLoading
        )
    }
}

/** Remonte le contenu de `amount` sous l'élément précédent (marge négative). */
private fun Modifier.pullUp(amount: Dp): Modifier = this.then(
    Modifier.layout { measurable ->
        val placeable = measurable.measure(constraints)
        val shift = amount.roundToPx()
        layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
            placeable.placeRelative(0, -shift)
        }
    }
)

/** Compteur FR « 1 284 » — groupement par milliers via la locale. */
private fun formatHubCount(count: Int): String =
    String.format(Locale.FRENCH, "%,d", count)

/** Partage le profil via une intent système (lien thehub://profile/<uid>). */
private fun shareProfile(
    context: android.content.Context,
    shareText: String
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    try {
        context.startActivity(Intent.createChooser(shareIntent, shareText))
    } catch (_: Exception) {
        // Aucune application compatible : on ignore silencieusement.
    }
}

/** Date de la pilule : « Mars 2001 » (naissance) sinon « Membre depuis Mars 2001 ». */
private fun profilePillDate(user: User): String? {
    return try {
        val birth = user.birthdate?.takeIf { it.isNotBlank() }
        if (birth != null) {
            val parsed = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).parse(birth)
            if (parsed != null) {
                SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(parsed)
            } else {
                null
            }
        } else {
            "Membre depuis " + SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
                .format(Date(user.createdAt))
        }
    } catch (_: Exception) {
        null
    }
}

/** Iconbtn verre de la maquette : 40/14 (ou 34/12 en petit). */
@Composable
private fun ProfileIconAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    content: @Composable () -> Unit
) {
    val sizeValue = if (small) 34.dp else 40.dp
    val shape = if (small) RoundedCornerShape(12.dp) else RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(sizeValue)
            .clip(shape)
            .background(HubGlass)
            .border(1.dp, HubStroke, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Stat .stat-num / .stat-lbl — nombre 18 800, libellé 10.5 700 uppercase. */
@Composable
private fun ProfileStatItem(
    count: Int,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = formatHubCount(count),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = HubWhite
        )
        Text(
            text = label.uppercase(Locale.FRENCH),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = HubTextTertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Séparateur vertical des stats. */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(HubStrokeSoft)
    )
}

/** Pilule .pill / .pill.ok — 10.5 800, icône 11. */
@Composable
private fun ProfilePill(
    text: String,
    icon: @Composable () -> Unit,
    accent: Boolean
) {
    val palette = LocalHubColors.current
    val bg = if (accent) palette.glassStrong else palette.success.copy(alpha = 0.15f)
    val border = if (accent) {
        palette.accent.copy(alpha = 0.35f)
    } else {
        palette.success.copy(alpha = 0.35f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(percent = 50))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
            color = if (accent) palette.accent else palette.success
        )
    }
}

/** Bouton sm de la maquette : .btn-primary (dégradé) ou .btn-glass (verre). */
@Composable
private fun ProfileActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .then(
                if (filled && enabled) {
                    Modifier.background(brush = hubPrimaryGradient(), shape = shape)
                } else {
                    Modifier
                        .background(HubGlass, shape)
                        .border(1.dp, HubStroke, shape)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) HubWhite else HubMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Onglet souligné .tabs — actif : texte plein + barre dégradée 3 dp. */
@Composable
private fun ProfileTabLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) HubWhite else HubTextTertiary
        )
        Spacer(Modifier.height(11.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(brush = hubPrimaryGradient())
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

/** Cellule de grille carrée : média réel, ou tuile nébuleuse avec extrait de texte. */
@Composable
private fun ProfileGridTile(
    post: Post,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val palette = LocalHubColors.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.card)
            .clickable(onClick = onClick)
    ) {
        when {
            !post.imageUrl.isNullOrBlank() -> {
                val strings = LocalHubStrings.current
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(post.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = strings.lot3.postImageDesc,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(HubGlassStrong)
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = HubTextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                )
            }

            else -> {
                // Tuile nébuleuse (media-art de la maquette) + extrait du texte.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(palette.bg1, palette.glow2.copy(alpha = 0.45f), palette.bg0)
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 12.dp, y = (-8).dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(palette.glow1.copy(alpha = 0.6f), Color.Transparent)
                                )
                            )
                    )
                    Text(
                        text = post.text.ifBlank { " " },
                        color = HubWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                    )
                }
            }
        }

        if (!post.videoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** État « chargement » du profil : squelette verre (bannière, avatar, textes). */
@Composable
private fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(HubGlass)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(HubGlass)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                HubSkeletonLine(height = 15.dp, widthFraction = 0.5f)
                Spacer(Modifier.height(8.dp))
                HubSkeletonLine(height = 12.dp, widthFraction = 0.3f)
            }
        }
        Spacer(Modifier.height(18.dp))
        HubSkeletonLine(height = 60.dp, widthFraction = 1f)
        Spacer(Modifier.height(16.dp))
        HubSkeletonLine(height = 40.dp, widthFraction = 1f)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(HubGlass)
                )
            }
        }
    }
}

/** Vue « bloqué » : verrou en verre + message + Débloquer. */
@Composable
private fun BlockedProfileView(
    isBlockedByMe: Boolean,
    isActionLoading: Boolean,
    onUnblock: () -> Unit
) {
    val strings = LocalHubStrings.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp)
        ) {
            GlassSurface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(26.dp),
                variant = GlassVariant.Card
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = if (isBlockedByMe) {
                    strings.lot5.profileBlockedByMe
                } else {
                    strings.lot5.profileBlockedYou
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                textAlign = TextAlign.Center
            )
            if (isBlockedByMe) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier.testTag("profile_unblock_button")
                ) {
                    HubButton(
                        text = strings.lot5.profileUnblockBtn,
                        onClick = onUnblock,
                        isLoading = isActionLoading,
                        enabled = !isActionLoading,
                        variant = HubButtonVariant.Secondary,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }
    }
}
