package com.thehub.hb.ui.feed

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.DeletePostConfirmationDialog
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.HubDimens
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.PostOptionsBottomSheet
import com.thehub.hb.ui.components.ReportBottomSheet
import com.thehub.hb.ui.components.copyPostLinkToClipboard
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
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
            .testTag("feed_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            // .appbar — logo 36 + wordmark « The Hub » (fin en dégradé), iconbtns verre 40/14.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
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
                        AppLogo(size = 36.dp, animated = false)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("The ")
                            withStyle(SpanStyle(brush = hubPrimaryGradient())) {
                                append("Hub")
                            }
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color = HubWhite
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(
                        onClick = onOpenFriends,
                        modifier = Modifier.testTag("feed_friends_button")
                    ) {
                        Icon(
                            Icons.Outlined.People,
                            contentDescription = strings.friendsDesc,
                            tint = HubWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(contentAlignment = Alignment.TopEnd) {
                        GlassIconButton(
                            onClick = onOpenMessenger,
                            modifier = Modifier.testTag("feed_messenger_button")
                        ) {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = strings.messengerDesc,
                                tint = HubWhite,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        if (unreadConversationsCount > 0) {
                            // Badge .bdg — dégradé de marque, liseré fond, 10/800.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(y = (-4).dp)
                                    .background(
                                        brush = hubPrimaryGradient(),
                                        shape = RoundedCornerShape(9.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        palette.black.copy(alpha = 0.65f),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                    .testTag("feed_messenger_badge")
                            ) {
                                Text(
                                    text = if (unreadConversationsCount > 99) "99+" else unreadConversationsCount.toString(),
                                    color = HubWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // .tabs soulignés de la maquette (Pour vous actif) + icône tendance.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                FeedTabLabel(text = strings.forYou, selected = true)
                FeedTabLabel(text = strings.following, selected = false)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.TrendingUp,
                    contentDescription = strings.feedTrendsDesc,
                    tint = HubTextTertiary,
                    modifier = Modifier.size(17.dp)
                )
            }

            // .chips de la maquette (✦ Tout actif, hashtags de démonstration).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedChip(text = strings.feedChipAll, selected = true)
                FeedChip(text = strings.feedChipTheHub, selected = false)
                FeedChip(text = strings.feedChipNigerTech, selected = false)
                FeedChip(text = strings.feedChipDesignSystem, selected = false)
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
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 90.dp),
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
                                        CircularProgressIndicator(
                                            color = palette.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
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
                onCopyLink = { copyPostLinkToClipboard(context, it.id, strings.linkCopiedToast) }
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
                        if (success) strings.postDeletedToast else (errorMsg ?: strings.deleteErrorToast),
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
                targetName = if (post.authorUsername.isNotBlank()) "@${post.authorUsername}" else strings.thisPostLabel,
                onDismiss = { postToReport = null },
                onSubmitReport = { reason, details ->
                    isSubmittingReport = true
                    viewModel.reportPost(post.id, reason, details) { success, errorMsg ->
                        isSubmittingReport = false
                        postToReport = null
                        Toast.makeText(
                            context,
                            if (success) strings.reportSentToast else (errorMsg ?: strings.reportErrorToast),
                            if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                isSubmitting = isSubmittingReport
            )
        }
    }
}

/** Iconbtn verre de la maquette : 40 dp, rayon 14, verre + contour. */
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

/** Onglet souligné .tabs — actif : texte plein + barre dégradée 3 dp. */
@Composable
private fun FeedTabLabel(text: String, selected: Boolean) {
    Column(
        modifier = Modifier.padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) HubWhite else HubMuted
        )
        Spacer(Modifier.height(13.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(brush = hubPrimaryGradient())
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
    }
}

/** Chip .chip de la maquette — pilule, sélection = dégradé de marque. */
@Composable
private fun FeedChip(text: String, selected: Boolean) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) HubWhite else HubSecondary,
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(brush = hubPrimaryGradient(), shape = shape)
                } else {
                    Modifier
                        .background(HubGlassStrong, shape)
                        .border(1.dp, HubStrokeSoft, shape)
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/** État « chargement » : cartes de publication squelettes (verre + shimmer). */
@Composable
private fun FeedSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HubDimens.CardRadius),
                variant = GlassVariant.Card
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(HubGlassStrong)
                        )
                        Spacer(Modifier.width(11.dp))
                        Column {
                            HubSkeletonLine(height = 13.dp, widthFraction = 0.35f)
                            Spacer(Modifier.height(6.dp))
                            HubSkeletonLine(height = 11.dp, widthFraction = 0.25f)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HubSkeletonLine(height = 14.dp, widthFraction = 0.9f)
                    Spacer(Modifier.height(8.dp))
                    HubSkeletonLine(height = 14.dp, widthFraction = 0.6f)
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(HubGlassStrong)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyState(onCreatePost: () -> Unit, onDiscoverUsers: () -> Unit) {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HubEmptyState(
                title = strings.noPostsYet,
                description = strings.noPostsDescription,
                icon = Icons.Outlined.DynamicFeed
            )
            Spacer(Modifier.height(8.dp))
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
    val strings = LocalHubStrings.current
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        HubEmptyState(
            title = strings.oopsLabel,
            description = message,
            icon = Icons.Outlined.CloudOff,
            actionLabel = strings.retryLabel,
            onAction = onRetry
        )
    }
}
