package com.thehub.hb.ui.postdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.Post
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubButtonVariant
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import androidx.compose.material3.ExperimentalMaterial3Api
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.RelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    onNavigateBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("post_detail_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("post_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = "Publication",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            when (val state = uiState) {
                is PostDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                is PostDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = HubError, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            HubButton(
                                text = "Réessayer",
                                onClick = { viewModel.loadPostDetail() },
                                variant = HubButtonVariant.Secondary,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }

                is PostDetailUiState.Success -> {
                    val post = state.post
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Repost indication
                        if (post.isRepost) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Repost",
                                    tint = HubSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reposté par @${post.authorUsername}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HubSecondary
                                )
                            }
                        }

                        // Author Profile Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (onAuthorClick != null) {
                                        Modifier.clickable { onAuthorClick(post.authorId) }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            UserAvatar(
                                name = post.authorUsername,
                                photoUrl = post.authorPhotoUrl,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "@${post.authorUsername}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HubWhite
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = RelativeTime.format(post.createdAt),
                                    fontSize = 13.sp,
                                    color = HubMuted
                                )
                            }
                        }

                        // Full Post Text
                        if (post.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = post.text,
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                color = HubWhite,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Full Post Image
                        if (!post.imageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(HubSurfaceElevated)
                                    .clickable { onImageClick(post.imageUrl) }
                            ) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = "Image de la publication",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }

                        // Original Post if Repost
                        if (post.isRepost && post.originalPost != null) {
                            val orig = post.originalPost
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(HubCard)
                                    .border(1.dp, HubBorder, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(
                                        name = orig.authorUsername,
                                        photoUrl = orig.authorPhotoUrl,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "@${orig.authorUsername}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HubWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· ${RelativeTime.format(orig.createdAt)}",
                                        fontSize = 12.sp,
                                        color = HubMuted
                                    )
                                }
                                if (orig.text.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = orig.text, color = HubWhite, fontSize = 15.sp)
                                }
                                if (!orig.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AsyncImage(
                                        model = orig.imageUrl,
                                        contentDescription = "Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageClick(orig.imageUrl) },
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                        }

                        // Divider & Action Bar
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = HubBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Like
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleLike() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Aimer",
                                        tint = if (post.isLikedByCurrentUser) Color(0xFFE0245E) else HubMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = "${post.likesCount} J'aime",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (post.isLikedByCurrentUser) Color(0xFFE0245E) else HubSecondary,
                                    modifier = Modifier
                                        .clickable { onOpenLikes(post.id) }
                                        .padding(4.dp)
                                )
                            }

                            // Comments
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onOpenComments(post.id) }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Commentaires",
                                    tint = HubMuted,
                                    modifier = Modifier.size(21.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${state.totalCommentsCount} Commentaires",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HubSecondary
                                )
                            }

                            // Share
                            IconButton(
                                onClick = { postToShare = post },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("action_share_${post.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Partager",
                                    tint = HubMuted,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            // Bookmark
                            IconButton(
                                onClick = { viewModel.toggleBookmark() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("action_bookmark_${post.id}")
                            ) {
                                Icon(
                                    imageVector = if (post.isBookmarkedByCurrentUser) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = if (post.isBookmarkedByCurrentUser) "Retirer des signets" else "Enregistrer",
                                    tint = if (post.isBookmarkedByCurrentUser) HubWhite else HubMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = HubBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Comments Preview Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Commentaires",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = HubWhite
                            )

                            if (state.totalCommentsCount > 0) {
                                Text(
                                    text = "Voir tout (${state.totalCommentsCount})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HubSecondary,
                                    modifier = Modifier
                                        .clickable { onOpenComments(post.id) }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (state.commentsPreview.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(HubSurfaceElevated)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucun commentaire pour l'instant.\nSoyez le premier à commenter !",
                                    color = HubMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.commentsPreview.forEach { comment ->
                                    CommentItemPreview(comment = comment)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }

                    // Sticky Bottom Bar for Adding Comment
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HubSurfaceElevated)
                            .border(width = 1.dp, color = HubBorder)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(HubCard)
                                .border(1.dp, HubBorder, RoundedCornerShape(24.dp))
                                .clickable { onOpenComments(post.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Écrire un commentaire...",
                                color = HubMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = HubSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Share Post Bottom Sheet
        postToShare?.let { post ->
            SharePostBottomSheet(
                post = post,
                onDismiss = { postToShare = null },
                onRepost = { p ->
                    viewModel.repost()
                }
            )
        }
    }
}

@Composable
private fun CommentItemPreview(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HubSurfaceElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            name = comment.authorUsername,
            photoUrl = comment.authorPhotoUrl,
            size = 32.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.authorUsername}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "· ${RelativeTime.format(comment.createdAt)}",
                    fontSize = 11.sp,
                    color = HubMuted
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.text,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = HubWhite
            )
        }
    }
}
