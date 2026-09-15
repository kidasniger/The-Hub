package com.thehub.hb.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.PostMediaImage
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.thehub.hb.utils.ImageSaver
import kotlinx.coroutines.launch
import com.thehub.hb.utils.RelativeTime

@Composable
fun PostCard(
    post: Post,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenShare: (Post) -> Unit,
    onToggleBookmark: ((String) -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null,
    onMoreOptionsClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = HubDarkGray),
                onClick = { onPostClick(post.id) }
            )
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = HubCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val author = rememberLiveUser(
                userId = post.authorId
            )

            // Repost banner if repost
            if (post.isRepost) {
                val reposter = rememberLiveUser(
                    userId = post.authorId
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repost",
                        tint = HubSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reposté par ${reposter.effectiveName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSecondary
                    )
                }
            }

            // Author Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onAuthorClick != null) Modifier.clickable { onAuthorClick(post.authorId) }
                            else Modifier
                        )
                ) {
                    UserAvatar(
                        name = author.effectiveName,
                        photoUrl = author.photoUrl,
                        size = 40.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = author.effectiveName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HubWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "·",
                                fontSize = 13.sp,
                                color = HubMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = RelativeTime.format(post.createdAt),
                                fontSize = 13.sp,
                                color = HubMuted
                            )
                        }
                    }
                }

                if (onMoreOptionsClick != null) {
                    IconButton(
                        onClick = { onMoreOptionsClick(post) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("post_options_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options de la publication",
                            tint = HubMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Post Text
            if (post.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.text,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = HubWhite
                )
            }

            // Post Image
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostMediaImage(
                    imageUrl = post.imageUrl,
                    contentDescription = "Image de la publication",
                    cornerRadius = 12.dp,
                    showSaveButton = true,
                    saveButtonTag = "post_card_save_image_button",
                    onImageClick = onImageClick
                )
            }

            // Embedded Original Post if Repost
            if (post.isRepost && post.originalPost != null) {
                Spacer(modifier = Modifier.height(12.dp))
                EmbeddedOriginalPost(
                    originalPost = post.originalPost,
                    onImageClick = onImageClick,
                    onPostClick = onPostClick
                )
            }

            // Action Bar: Like, Comment, Share
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like Button & Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("action_like_${post.id}")
                ) {
                    IconButton(
                        onClick = { onToggleLike(post.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Aimer",
                            tint = if (post.isLikedByCurrentUser) Color(0xFFE0245E) else HubMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (post.isLikedByCurrentUser) Color(0xFFE0245E) else HubSecondary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .clickable { onOpenLikes(post.id) }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                // Comment Button & Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onOpenComments(post.id) }
                        .padding(4.dp)
                        .testTag("action_comment_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Commentaires",
                        tint = HubMuted,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSecondary
                    )
                }

                // Share Button
                IconButton(
                    onClick = { onOpenShare(post) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_share_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Partager",
                        tint = HubMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Bookmark Button
                IconButton(
                    onClick = { onToggleBookmark?.invoke(post.id) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_bookmark_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isBookmarkedByCurrentUser) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (post.isBookmarkedByCurrentUser) "Retirer des signets" else "Enregistrer",
                        tint = if (post.isBookmarkedByCurrentUser) HubWhite else HubMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbeddedOriginalPost(
    originalPost: Post,
    onImageClick: (String) -> Unit,
    onPostClick: (String) -> Unit
) {
    val origAuthor = rememberLiveUser(
        userId = originalPost.authorId
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HubSurfaceElevated)
            .border(1.dp, HubBorder, RoundedCornerShape(12.dp))
            .clickable { onPostClick(originalPost.id) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                name = origAuthor.effectiveName,
                photoUrl = origAuthor.photoUrl,
                size = 28.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = origAuthor.effectiveName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "· ${RelativeTime.format(originalPost.createdAt)}",
                fontSize = 12.sp,
                color = HubMuted
            )
        }

        if (originalPost.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = originalPost.text,
                fontSize = 14.sp,
                color = HubWhite,
                lineHeight = 18.sp
            )
        }

        if (!originalPost.imageUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            PostMediaImage(
                imageUrl = originalPost.imageUrl,
                contentDescription = "Image repostée",
                cornerRadius = 8.dp,
                showSaveButton = false,
                onImageClick = onImageClick
            )
        }
    }
}
