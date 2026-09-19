package com.thehub.hb.ui.bookmarks

import androidx.compose.foundation.background
import com.thehub.hb.ui.theme.HubOutline
import com.thehub.hb.ui.theme.HubSurface
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
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
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = (uiState as? BookmarksUiState.Success)?.isRefreshing == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("bookmarks_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HubSurfaceElevated)
                            .testTag("bookmarks_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = HubWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Signets",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HubWhite
                        )
                        if (uiState is BookmarksUiState.Success) {
                            val count = (uiState as BookmarksUiState.Success).posts.size
                            Text(
                                text = "$count publication${if (count > 1) "s" else ""} enregistrée${if (count > 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = HubSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(HubSurfaceElevated)
                        .testTag("bookmarks_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualiser",
                        tint = HubWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Main content
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is BookmarksUiState.Loading -> {
                        BookmarksSkeletonList()
                    }

                    is BookmarksUiState.Empty -> {
                        BookmarksEmptyState()
                    }

                    is BookmarksUiState.Error -> {
                        BookmarksErrorState(
                            message = state.message,
                            onRetry = { viewModel.loadBookmarks(isRefresh = false) }
                        )
                    }

                    is BookmarksUiState.Success -> {
                        LazyColumn(
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

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }

        // Share Bottom Sheet
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
private fun BookmarksEmptyState() {
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
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Aucune publication enregistrée pour l'instant",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enregistrez des publications avec l'icône marque-page pour les retrouver facilement ici à tout moment.",
                fontSize = 14.sp,
                color = HubSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BookmarksErrorState(
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

@Composable
private fun BookmarksSkeletonList() {
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
                    .height(140.dp)
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
                            .fillMaxWidth(0.85f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(HubDarkGray)
                    )
                }
            }
        }
    }
}
