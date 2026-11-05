package com.thehub.hb.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubDimens
import com.thehub.hb.ui.components.PostMediaImage
import com.thehub.hb.ui.components.VideoLinkCard
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.components.OfficialVerificationBadge
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubInfo
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.utils.RelativeTime

/**
 * Carte de publication « Aurora Glass » — reproduit .post.glass de la maquette
 * (écran 06) : verre Card rayon --r-xl 30, en-tête avatar 42, texte 14/1.55,
 * média rayon 20, barre d'actions .pa (hot danger / cool info / share success).
 */
@Composable
fun PostCard(
    post: Post,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    onToggleLike: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenShare: (Post) -> Unit,
    onToggleBookmark: ((String) -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null,
    onMoreOptionsClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val author = rememberLiveUser(userId = post.authorId)

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = HubMuted),
                onClick = { onPostClick(post.id) }
            )
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(HubDimens.CardRadius), // .post — var(--r-xl) 30
        variant = GlassVariant.Card
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (post.isRepost) {
                val reposter = rememberLiveUser(userId = post.authorId)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.lot3.repostedByFormat.format(reposter.effectiveName),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSecondary
                    )
                }
            }

            // .post .phead — 14 16 0, avatar 42, écart 11.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 10.dp, top = 14.dp)
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
                        size = 42.dp
                    )

                    Spacer(modifier = Modifier.width(11.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = author.effectiveName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = HubWhite
                            )
                            if (author.verificationType == "admin") {
                                Spacer(modifier = Modifier.width(5.dp))
                                OfficialVerificationBadge(size = 15.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (author.username.isNotBlank()) {
                                Text(
                                    text = "@${author.username}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HubMuted,
                                    maxLines = 1
                                )
                                Text(" · ", fontSize = 11.5.sp, color = HubMuted)
                            }
                            Text(
                                text = RelativeTime.format(post.createdAt),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = HubMuted
                            )
                        }
                    }
                }

                if (onMoreOptionsClick != null) {
                    // Maquette : iconbtn « nu » (border:0;background:transparent;box-shadow:none).
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onMoreOptionsClick(post) }
                            .testTag("post_options_button_${post.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = strings.lot3.postOptionsDesc,
                            tint = HubTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val hasVideo = !post.videoUrl.isNullOrBlank()
            val textIsOnlyVideoUrl = hasVideo &&
                post.text.trim().equals(post.videoUrl?.trim(), ignoreCase = true)

            // .post .ptext — 11 16 0, 14 / 1.55 / 450.
            if (post.text.isNotBlank() && !textIsOnlyVideoUrl) {
                Spacer(modifier = Modifier.height(11.dp))
                Text(
                    text = post.text,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = HubWhite,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                if (!hasVideo) {
                    LinkPreviewCard(
                        text = post.text,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
            }

            // .post .pmedia — marges 13 14 0, rayon 20.
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(13.dp))
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    PostMediaImage(
                        imageUrl = post.imageUrl,
                        contentDescription = strings.lot3.postImageDesc,
                        cornerRadius = 20.dp,
                        showSaveButton = true,
                        saveButtonTag = "post_card_save_image_button",
                        onImageClick = onImageClick,
                        maxHeight = 360.dp
                    )
                }
            }

            if (!post.videoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(13.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    VideoLinkCard(
                        videoUrl = post.videoUrl!!,
                        onClick = onVideoClick
                    )
                }
            }

            if (post.isRepost && post.originalPost != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    EmbeddedOriginalPost(
                        originalPost = post.originalPost,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onPostClick = onPostClick
                    )
                }
            }

            // .post .pactions — 12 18 14, .pa 12/700 (hot danger · cool info · share success).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("action_like_${post.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onToggleLike(post.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (post.isLikedByCurrentUser) strings.lot3.unlikeDesc else strings.lot3.likeDesc,
                            tint = if (post.isLikedByCurrentUser) HubError else HubTextTertiary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isLikedByCurrentUser) HubError else HubTextTertiary,
                        modifier = Modifier
                            .clickable { onOpenLikes(post.id) }
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenComments(post.id) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("action_comment_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = strings.lot3.commentsDesc,
                        tint = HubInfo,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubInfo
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenShare(post) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("action_share_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = strings.lot3.shareDesc,
                        tint = HubSuccess,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.repostsCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubSuccess
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onToggleBookmark?.invoke(post.id) }
                        .testTag("action_bookmark_${post.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (post.isBookmarkedByCurrentUser) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (post.isBookmarkedByCurrentUser) strings.lot3.bookmarkRemoveDesc else strings.lot3.bookmarkAddDesc,
                        tint = if (post.isBookmarkedByCurrentUser) HubWhite else HubTextTertiary,
                        modifier = Modifier.size(17.dp)
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
    onVideoClick: ((String) -> Unit)?,
    onPostClick: (String) -> Unit
) {
    val strings = LocalHubStrings.current
    val origAuthor = rememberLiveUser(userId = originalPost.authorId)

    // Publi d'origine incrustée : verre doux .glass-soft rayon --r-sm 14.
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPostClick(originalPost.id) },
        shape = RoundedCornerShape(14.dp),
        variant = GlassVariant.Soft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    fontSize = 11.5.sp,
                    color = HubMuted
                )
            }

            if (originalPost.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = originalPost.text,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = HubSecondary
                )
            }

            if (!originalPost.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                PostMediaImage(
                    imageUrl = originalPost.imageUrl,
                    contentDescription = strings.lot3.repostImageDesc,
                    cornerRadius = 12.dp,
                    showSaveButton = false,
                    onImageClick = onImageClick
                )
            }

            if (!originalPost.videoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                VideoLinkCard(
                    videoUrl = originalPost.videoUrl!!,
                    onClick = onVideoClick
                )
            }
        }
    }
}
