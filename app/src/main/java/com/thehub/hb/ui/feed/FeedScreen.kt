package com.thehub.hb.ui.feed

import android.widget.Toast
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.DeletePostConfirmationDialog
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.PostOptionsBottomSheet
import com.thehub.hb.ui.components.ReportBottomSheet
import com.thehub.hb.ui.components.copyPostLinkToClipboard
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onCreatePost: () -> Unit,
    onOpenMessenger: () -> Unit,
    onOpenFriends: () -> Unit = {},
    onDiscoverUsers: () -> Unit = {},
    onAuthorClick: ((String) -> Unit)? = null,
    onEditPost: (Post) -> Unit = {},
    messageRepository: MessageRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val unreadConversationsCount by viewModel.unreadConversationsCount.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    var postForOptions by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var postToReport by remember { mutableStateOf<Post?>(null) }
    var isDeletingPost by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val isRefreshing = (uiState as? FeedUiState.Success)?.isRefreshing == true
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .testTag("feed_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.refresh() }
                            .testTag("feed_logo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 32.dp, animated = false)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "The Hub",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HubWhite,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenFriends,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HubSurfaceElevated)
                            .testTag("feed_friends_button")
                    ) {
                        Icon(Icons.Outlined.People, "Amis", tint = HubWhite, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = onOpenMessenger,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(HubSurfaceElevated)
                                .testTag("feed_messenger_button")
                        ) {
                            Icon(Icons.Outlined.Email, "Messagerie", tint = HubWhite, modifier = Modifier.size(20.dp))
                        }
                        if (unreadConversationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(HubWhite)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .testTag("feed_messenger_badge")
                            ) {
                                Text(
                                    text = if (unreadConversationsCount > 99) "99+" else unreadConversationsCount.toString(),
                                    color = HubBlack,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is FeedUiState.Loading -> FeedSkeletonList()
                    is FeedUiState.Empty -> FeedEmptyState(onCreatePost, onDiscoverUsers)
                    is FeedUiState.Error -> FeedErrorState(state.message) { viewModel.loadFeed(isRefresh = false) }
                    is FeedUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onPostClick = onPostClick,
                                    onImageClick = onImageClick,
                                    onVideoClick = onVideoClick,
                                    onToggleLike = { viewModel.toggleLike(it) },
                                    onOpenComments = onOpenComments,
                                    onOpenLikes = onOpenLikes,
                                    onOpenShare = { postToShare = it },
                                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                                    onAuthorClick = onAuthorClick,
                                    onMoreOptionsClick = { postForOptions = it }
                                )
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = HubWhite, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }

        postToShare?.let { post ->
            SharePostBottomSheet(
                post = post,
                onDismiss = { postToShare = null },
                onRepost = { viewModel.repost(it) },
                messageRepository = messageRepository
            )
        }

        postForOptions?.let { post ->
            PostOptionsBottomSheet(
                post = post,
                currentUserId = viewModel.currentUserId,
                onDismiss = { postForOptions = null },
                onEdit = onEditPost,
                onDelete = { postToDelete = it },
                onReport = { postToReport = it },
                onCopyLink = { copyPostLinkToClipboard(context, it.id) }
            )
        }

        DeletePostConfirmationDialog(
            isOpen = postToDelete != null,
            isDeleting = isDeletingPost,
            onConfirm = {
                val targetPost = postToDelete ?: return@DeletePostConfirmationDialog
                isDeletingPost = true
                viewModel.deletePost(targetPost.id) { success, errorMsg ->
                    isDeletingPost = false
                    postToDelete = null
                    Toast.makeText(
                        context,
                        if (success) "Publication supprimée" else (errorMsg ?: "Erreur de suppression"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { if (!isDeletingPost) postToDelete = null }
        )

        postToReport?.let { post ->
            ReportBottomSheet(
                targetType = "post",
                targetId = post.id,
                targetName = if (post.authorUsername.isNotBlank()) "@${post.authorUsername}" else "cette publication",
                onDismiss = { postToReport = null },
                onSubmitReport = { reason, details ->
                    isSubmittingReport = true
                    viewModel.reportPost(post.id, reason, details) { success, errorMsg ->
                        isSubmittingReport = false
                        postToReport = null
                        Toast.makeText(
                            context,
                            if (success) "Signalement envoyé. Merci de nous aider à garder The Hub sûr."
                            else (errorMsg ?: "Erreur lors du signalement"),
                            if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                isSubmitting = isSubmittingReport
            )
        }
    }
}

@Composable
private fun FeedSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(HubSurface)
                    .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(HubDarkGray))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Box(Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(HubDarkGray))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(HubBorder))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(HubDarkGray))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(HubDarkGray))
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyState(onCreatePost: () -> Unit, onDiscoverUsers: () -> Unit) {
    val strings = com.thehub.hb.ui.theme.LocalHubStrings.current
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DynamicFeed, null, tint = HubSecondary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.noPostsYet,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                strings.noPostsDescription,
                fontSize = 14.sp,
                color = HubSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(28.dp))
            HubButton(
                text = strings.tabCreate,
                onClick = onCreatePost,
                modifier = Modifier.width(230.dp),
                testTag = "feed_empty_create_post"
            )
            Spacer(Modifier.height(10.dp))
            HubButton(
                text = strings.discoverUsers,
                onClick = onDiscoverUsers,
                variant = HubButtonVariant.Secondary,
                modifier = Modifier.width(230.dp),
                testTag = "feed_empty_discover_users"
            )
        }
    }
}

@Composable
private fun FeedErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oups !", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HubError)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, color = HubSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            HubButton(
                text = "Réessayer",
                onClick = onRetry,
                variant = HubButtonVariant.Secondary,
                modifier = Modifier.width(180.dp)
            )
        }
    }
}