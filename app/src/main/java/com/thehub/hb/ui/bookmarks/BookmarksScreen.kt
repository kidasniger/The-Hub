package com.thehub.hb.ui.bookmarks

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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings

/**
 * Écran 21 — Favoris : appbar (retour + titre + compteur + actualiser),
 * cartes publication verre (PostCard), états chargement / vide / erreur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onNavigateBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val uiState by viewModel.uiState.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = (uiState as? BookmarksUiState.Success)?.isRefreshing == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("bookmarks_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // .appbar — retour verre + titre + compteur + actualiser verre.
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
                        .testTag("bookmarks_back_button"),
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
                        text = strings.lot5.bookmarksTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = HubWhite
                    )
                    if (uiState is BookmarksUiState.Success) {
                        val count = (uiState as BookmarksUiState.Success).posts.size
                        Text(
                            text = if (count == 1) {
                                strings.lot5.bookmarksCountOne
                            } else {
                                strings.lot5.bookmarksCountFormat.format(count)
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.thehub.hb.ui.theme.HubTextTertiary,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubGlass)
                        .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                        .clickable(onClick = { viewModel.refresh() })
                        .testTag("bookmarks_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = strings.lot5.bookmarksRefreshDesc,
                        tint = HubWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Contenu : chargement / vide / erreur / liste de publications.
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is BookmarksUiState.Loading -> BookmarksSkeletonList()

                    is BookmarksUiState.Empty -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HubEmptyState(
                            title = strings.lot5.bookmarksEmptyTitle,
                            description = strings.lot5.bookmarksEmptyDesc,
                            icon = Icons.Outlined.BookmarkBorder
                        )
                    }

                    is BookmarksUiState.Error -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HubEmptyState(
                            title = strings.lot3.oopsLabel,
                            description = state.message,
                            icon = Icons.Outlined.CloudOff,
                            actionLabel = strings.lot3.retryLabel,
                            onAction = { viewModel.loadBookmarks(isRefresh = false) }
                        )
                    }

                    is BookmarksUiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 2.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(13.dp),
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
                                    onVideoClick = onVideoClick,
                                    onToggleLike = { viewModel.toggleLike(it) },
                                    onOpenComments = onOpenComments,
                                    onOpenLikes = onOpenLikes,
                                    onOpenShare = { postToShare = it },
                                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                                    onAuthorClick = onAuthorClick
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feuille de partage (corrigée : flux conversations protégé).
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

/** État « chargement » : cartes de publication squelettes verre. */
@Composable
private fun BookmarksSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        repeat(3) {
            GlassSurface(
                shape = RoundedCornerShape(com.thehub.hb.ui.components.HubDimens.CardRadius),
                variant = GlassVariant.Card,
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            HubSkeletonLine(height = 13.dp, widthFraction = 0.35f)
                            Spacer(Modifier.height(6.dp))
                            HubSkeletonLine(height = 11.dp, widthFraction = 0.25f)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HubSkeletonLine(height = 14.dp, widthFraction = 0.9f)
                    Spacer(Modifier.height(8.dp))
                    HubSkeletonLine(height = 14.dp, widthFraction = 0.6f)
                }
            }
        }
    }
}
