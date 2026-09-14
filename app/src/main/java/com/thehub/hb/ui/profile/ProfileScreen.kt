package com.thehub.hb.ui.profile

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.ui.components.BlockUserConfirmationDialog
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.ReportBottomSheet
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    isBottomTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = HubBlack,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("profile_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            ProfileTopBar(
                isOwnProfile = uiState.isOwnProfile,
                username = uiState.user?.username ?: "",
                isBottomTab = isBottomTab,
                onNavigateBack = onNavigateBack,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToBookmarks = onNavigateToBookmarks,
                onMenuClick = { showMenu = true }
            )

            // Dropdown Menu for Other User
            if (!uiState.isOwnProfile) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(HubCard)
                            .testTag("profile_overflow_menu")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = HubWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Signaler", color = HubWhite)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showReportSheet = true
                            },
                            modifier = Modifier.testTag("menu_report_user")
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = HubError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Bloquer cet utilisateur", color = HubError)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showBlockDialog = true
                            },
                            modifier = Modifier.testTag("menu_block_user")
                        )
                    }
                }
            }

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

                uiState.isBlocked -> {
                    BlockedContentUnavailableView(
                        isBlockedByMe = uiState.isBlockedByMe,
                        onUnblock = { viewModel.unblockUser() },
                        isActionLoading = uiState.isActionLoading
                    )
                }

                uiState.user == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Profil introuvable",
                            fontSize = 16.sp,
                            color = HubSecondary
                        )
                    }
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Header span all columns
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ProfileHeader(
                                        user = user,
                                        isOwnProfile = uiState.isOwnProfile,
                                        isFollowing = uiState.isFollowing,
                                        isActionLoading = uiState.isActionLoading,
                                        onEditProfile = onNavigateToEditProfile,
                                        onToggleFollow = { viewModel.toggleFollow() },
                                        onSendMessage = {
                                            viewModel.openChat { convId ->
                                                onNavigateToChat(convId)
                                            }
                                        },
                                        onFollowersClick = { onNavigateToFollowers(user.uid) },
                                        onFollowingClick = { onNavigateToFollowing(user.uid) }
                                    )

                                    HorizontalDivider(
                                        color = HubBorder,
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GridOn,
                                            contentDescription = null,
                                            tint = HubWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Publications",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HubWhite
                                        )
                                    }
                                }
                            }

                            // Empty state or Post Grid Items
                            if (uiState.posts.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (uiState.isOwnProfile) "Tu n'as encore rien publié" else "Aucune publication pour le moment",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = HubSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = uiState.posts,
                                    key = { it.id }
                                ) { post ->
                                    PostGridThumbnail(
                                        post = post,
                                        onClick = { onPostClick(post.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Report Bottom Sheet
    if (showReportSheet && uiState.user != null) {
        ReportBottomSheet(
            targetType = "user",
            targetId = uiState.user!!.uid,
            targetName = uiState.user!!.username,
            onDismiss = { showReportSheet = false },
            onSubmitReport = { reason, details ->
                viewModel.reportUser(reason, details) {
                    showReportSheet = false
                }
            },
            isSubmitting = uiState.isActionLoading
        )
    }

    // Block User Dialog
    if (showBlockDialog && uiState.user != null) {
        BlockUserConfirmationDialog(
            username = uiState.user!!.username,
            onConfirm = {
                viewModel.blockUser {
                    showBlockDialog = false
                }
            },
            onDismiss = { showBlockDialog = false },
            isLoading = uiState.isActionLoading
        )
    }
}

@Composable
private fun ProfileTopBar(
    isOwnProfile: Boolean,
    username: String,
    isBottomTab: Boolean,
    onNavigateBack: (() -> Unit)?,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isBottomTab && onNavigateBack != null) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = HubWhite
                )
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = if (username.isNotBlank()) "@$username" else "Profil",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isOwnProfile) {
            IconButton(
                onClick = onNavigateToBookmarks,
                modifier = Modifier.testTag("profile_bookmarks_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Signets",
                    tint = HubWhite
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.testTag("profile_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Paramètres",
                    tint = HubWhite
                )
            }
        } else {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("profile_more_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = HubWhite
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isActionLoading: Boolean,
    onEditProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onSendMessage: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // User Info Top Row: Avatar and Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = user.displayName ?: user.username,
                photoUrl = user.photoUrl,
                size = 80.dp
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Stats row
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    count = user.postsCount,
                    label = "Publications",
                    onClick = null,
                    testTag = "profile_stat_posts"
                )
                StatItem(
                    count = user.followersCount,
                    label = "Abonnés",
                    onClick = onFollowersClick,
                    testTag = "profile_stat_followers"
                )
                StatItem(
                    count = user.followingCount,
                    label = "Abonnements",
                    onClick = onFollowingClick,
                    testTag = "profile_stat_following"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Names and Bio
        Text(
            text = user.displayName?.takeIf { it.isNotBlank() } ?: "@${user.username}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )

        if (!user.displayName.isNullOrBlank()) {
            Text(
                text = "@${user.username}",
                fontSize = 13.sp,
                color = HubSecondary
            )
        }

        if (!user.bio.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = user.bio,
                fontSize = 14.sp,
                color = HubWhite,
                lineHeight = 19.sp
            )
        }

        // Join Date
        val joinDateText = remember(user.createdAt) {
            try {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
                "Membre depuis " + sdf.format(Date(user.createdAt))
            } catch (_: Exception) {
                null
            }
        }
        if (joinDateText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = joinDateText,
                fontSize = 12.sp,
                color = HubMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        if (isOwnProfile) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HubSurfaceElevated)
                    .border(1.dp, HubBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onEditProfile)
                    .padding(vertical = 10.dp)
                    .testTag("edit_profile_button"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = HubWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Modifier le profil",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubWhite
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Follow / Unfollow Button
                Box(modifier = Modifier.weight(1f)) {
                    HubButton(
                        text = if (isFollowing) "Ne plus suivre" else "Suivre",
                        onClick = onToggleFollow,
                        isLoading = isActionLoading,
                        enabled = !isActionLoading,
                        variant = if (isFollowing) HubButtonVariant.Secondary else HubButtonVariant.Primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("follow_unfollow_button")
                    )
                }

                // Send Message Button
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HubSurfaceElevated)
                        .border(1.dp, HubBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onSendMessage)
                        .padding(horizontal = 12.dp)
                        .testTag("message_user_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = HubWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    count: Int,
    label: String,
    onClick: (() -> Unit)?,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = HubSecondary
        )
    }
}

@Composable
private fun PostGridThumbnail(
    post: Post,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(HubCard)
            .clickable(onClick = onClick)
            .testTag("profile_post_thumb_${post.id}")
    ) {
        if (!post.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(post.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Publication",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Text Preview thumbnail
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.text,
                    fontSize = 11.sp,
                    color = HubSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Repost badge overlay
        if (post.isRepost) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(HubBlack.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repost",
                    tint = HubWhite,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun BlockedContentUnavailableView(
    isBlockedByMe: Boolean,
    onUnblock: () -> Unit,
    isActionLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Contenu indisponible",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isBlockedByMe) {
                    "Vous avez bloqué cet utilisateur. Vous ne pouvez plus voir son profil ni ses publications."
                } else {
                    "Ce profil n'est pas disponible pour le moment."
                },
                fontSize = 14.sp,
                color = HubSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (isBlockedByMe) {
                Spacer(modifier = Modifier.height(20.dp))
                HubButton(
                    text = "Débloquer",
                    onClick = onUnblock,
                    isLoading = isActionLoading,
                    enabled = !isActionLoading,
                    variant = HubButtonVariant.Secondary,
                    modifier = Modifier.testTag("unblock_from_profile_button")
                )
            }
        }
    }
}
