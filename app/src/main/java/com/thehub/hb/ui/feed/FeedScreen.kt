package com.thehub.hb.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onCreatePost: () -> Unit,
    onOpenMessenger: () -> Unit,
    onAuthorClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadConversationsCount by viewModel.unreadConversationsCount.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    val listState = rememberLazyListState()

    // Pagination detection
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val isRefreshing = (uiState as? FeedUiState.Success)?.isRefreshing == true
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .testTag("feed_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(
                        size = 32.dp,
                        animated = false
                    )
                    Spacer(modifier = Modifier.width(10.dp))
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
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HubSurfaceElevated)
                            .testTag("feed_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser",
                            tint = HubWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = onOpenMessenger,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(HubSurfaceElevated)
                                .testTag("feed_messenger_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = "Messagerie",
                                tint = HubWhite,
                                modifier = Modifier.size(20.dp)
                            )
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

            // Main Content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is FeedUiState.Loading -> {
                        FeedSkeletonList()
                    }

                    is FeedUiState.Empty -> {
                        FeedEmptyState(onCreatePost = onCreatePost)
                    }

                    is FeedUiState.Error -> {
                        FeedErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadFeed(isRefresh = false) }
                        )
                    }

                    is FeedUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.posts,
                                key = { it.id }
                            ) { post ->
                                PostCard(
                                    post = post,
                                    onPostClick = onPostClick,
                                    onImageClick = onImageClick,
                                    onToggleLike = { viewModel.toggleLike(it) },
                                    onOpenComments = onOpenComments,
                                    onOpenLikes = onOpenLikes,
                                    onOpenShare = { postToShare = it },
                                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                                    onAuthorClick = onAuthorClick
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = HubWhite,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Extra bottom spacer so last post isn't hidden under bottom bar / FAB
                            item {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }
                    }
                }
            }
        }

        // Quick Create Floating Action Button
        FloatingActionButton(
            onClick = onCreatePost,
            containerColor = HubWhite,
            contentColor = HubBlack,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 84.dp)
                .size(56.dp)
                .testTag("feed_fab_create_post")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nouvelle publication",
                modifier = Modifier.size(28.dp)
            )
        }

        // Share Post Bottom Sheet
        postToShare?.let { post ->
            SharePostBottomSheet(
                post = post,
                onDismiss = { postToShare = null },
                onRepost = { p ->
                    viewModel.repost(p)
                }
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(HubCard)
                    .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HubDarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HubDarkGray)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(HubBorder)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HubDarkGray)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HubDarkGray)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyState(
    onCreatePost: () -> Unit
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
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DynamicFeed,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Aucune publication",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Soyez le premier à partager une pensée ou une photo avec la communauté !",
                fontSize = 14.sp,
                color = HubSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            HubButton(
                text = "Créer une publication",
                onClick = onCreatePost,
                modifier = Modifier.width(220.dp),
                testTag = "feed_empty_create_post"
            )
        }
    }
}

@Composable
private fun FeedErrorState(
    message: String,
    onRetry: () -> Unit
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
            Text(
                text = "Oups !",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HubError
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = HubSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            HubButton(
                text = "Réessayer",
                onClick = onRetry,
                variant = HubButtonVariant.Secondary,
                modifier = Modifier.width(180.dp)
            )
        }
    }
}
