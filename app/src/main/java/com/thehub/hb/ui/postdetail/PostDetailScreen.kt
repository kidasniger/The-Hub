package com.thehub.hb.ui.postdetail

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.DeletePostConfirmationDialog
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubDimens
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.PostMediaImage
import com.thehub.hb.ui.components.VideoLinkCard
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.components.PostOptionsBottomSheet
import com.thehub.hb.ui.components.ReportBottomSheet
import com.thehub.hb.ui.components.copyPostLinkToClipboard
import com.thehub.hb.ui.feed.components.SharePostBottomSheet
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubInfo
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.utils.RelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    onNavigateBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onAuthorClick: ((String) -> Unit)? = null,
    onEditPost: (Post) -> Unit = {},
    onPostDeleted: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val uiState by viewModel.uiState.collectAsState()
    var postToShare by remember { mutableStateOf<Post?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var isDeletingPost by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("post_detail_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // .appbar — retour (verre), titre 16.5/800, partage + options (verre).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailGlassIcon(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("post_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = strings.postDetailTitle,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite,
                    modifier = Modifier.weight(1f)
                )

                when (val state = uiState) {
                    is PostDetailUiState.Success -> {
                        DetailGlassIcon(
                            onClick = { postToShare = state.post },
                            modifier = Modifier.testTag("post_detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = strings.shareDesc,
                                tint = HubWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        DetailGlassIcon(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("post_detail_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = strings.postOptionsDesc,
                                tint = HubWhite,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    else -> Unit
                }
            }

            when (val state = uiState) {
                is PostDetailUiState.Loading -> {
                    PostDetailSkeleton()
                }

                is PostDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HubEmptyState(
                            title = strings.oopsLabel,
                            description = state.message,
                            icon = Icons.Outlined.CloudOff,
                            actionLabel = strings.retryLabel,
                            onAction = { viewModel.loadPostDetail() }
                        )
                    }
                }

                is PostDetailUiState.Success -> {
                    val post = state.post
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val author = rememberLiveUser(userId = post.authorId)

                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(HubDimens.CardRadius),
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
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = strings.repostedByFormat.format(reposter.effectiveName),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HubSecondary
                                        )
                                    }
                                }

                                // .phead — avatar 46, meta @handle · heure · visibilité.
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (onAuthorClick != null) {
                                                    Modifier.clickable { onAuthorClick(post.authorId) }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        UserAvatar(
                                            name = author.effectiveName,
                                            photoUrl = author.photoUrl,
                                            size = 46.dp
                                        )
                                        Spacer(Modifier.width(11.dp))
                                        Column {
                                            HubVerifiedUserName(
                                                name = author.effectiveName,
                                                isVerified = author.isVerified,
                                                verificationType = author.verificationType,
                                                fontSize = 13.5.sp,
                                                color = HubWhite,
                                                badgeSize = 15.dp
                                            )
                                            Spacer(Modifier.height(1.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = buildString {
                                                        if (author.username.isNotBlank()) append("@${author.username} · ")
                                                        append(RelativeTime.format(post.createdAt))
                                                        append(" · ${strings.publicLabel}")
                                                    },
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = HubMuted
                                                )
                                            }
                                        }
                                    }

                                    val detailState = uiState as? PostDetailUiState.Success
                                    val currentUid = detailState?.currentUserId
                                    if (currentUid != null && currentUid != post.authorId) {
                                        val isFollowing = detailState.isFollowingAuthor
                                        val isFollowLoading = detailState.isFollowActionLoading
                                        // .btn.xs — primaire (S'abonner) ou verre (Abonné).
                                        DetailFollowButton(
                                            isFollowing = isFollowing,
                                            isLoading = isFollowLoading,
                                            followLabel = strings.follow,
                                            followingLabel = strings.unfollow,
                                            onClick = { viewModel.toggleFollowAuthor() },
                                            modifier = Modifier.testTag("post_detail_follow_button")
                                        )
                                    }
                                }

                                // .ptext 15 / 1.55.
                                if (post.text.isNotBlank()) {
                                    Spacer(Modifier.height(11.dp))
                                    Text(
                                        text = post.text,
                                        fontSize = 15.sp,
                                        lineHeight = 23.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HubWhite,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    if (post.videoUrl.isNullOrBlank()) {
                                        LinkPreviewCard(
                                            text = post.text,
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                                        )
                                    }
                                }

                                if (!post.imageUrl.isNullOrBlank()) {
                                    Spacer(Modifier.height(13.dp))
                                    Box(Modifier.padding(horizontal = 14.dp)) {
                                        PostMediaImage(
                                            imageUrl = post.imageUrl,
                                            contentDescription = strings.postImageDesc,
                                            cornerRadius = 20.dp,
                                            showSaveButton = true,
                                            saveButtonTag = "post_detail_save_image_button",
                                            onImageClick = onImageClick,
                                            maxHeight = 360.dp
                                        )
                                    }
                                }

                                if (!post.imageUrl.isNullOrBlank() && !post.videoUrl.isNullOrBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Box(Modifier.padding(horizontal = 14.dp)) {
                                        VideoLinkCard(
                                            videoUrl = post.videoUrl!!,
                                            onClick = onVideoClick
                                        )
                                    }
                                }

                                if (post.isRepost && post.originalPost != null) {
                                    val orig = post.originalPost
                                    val origAuthor = rememberLiveUser(userId = orig.authorId)
                                    Spacer(Modifier.height(12.dp))
                                    Box(Modifier.padding(horizontal = 14.dp)) {
                                        GlassSurface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            variant = GlassVariant.Soft
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    UserAvatar(
                                                        name = origAuthor.effectiveName,
                                                        photoUrl = origAuthor.photoUrl,
                                                        size = 32.dp
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    HubVerifiedUserName(
                                                        name = origAuthor.effectiveName,
                                                        isVerified = origAuthor.isVerified,
                                                        verificationType = origAuthor.verificationType,
                                                        fontSize = 13.sp,
                                                        color = HubWhite,
                                                        badgeSize = 14.dp
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "· ${RelativeTime.format(orig.createdAt)}",
                                                        fontSize = 11.5.sp,
                                                        color = HubMuted
                                                    )
                                                }
                                                if (orig.text.isNotBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = orig.text,
                                                        color = HubSecondary,
                                                        fontSize = 13.5.sp,
                                                        lineHeight = 20.sp
                                                    )
                                                }
                                                if (!orig.imageUrl.isNullOrBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    PostMediaImage(
                                                        imageUrl = orig.imageUrl,
                                                        contentDescription = strings.repostImageDesc,
                                                        cornerRadius = 12.dp,
                                                        showSaveButton = false,
                                                        onImageClick = onImageClick
                                                    )
                                                }
                                                if (!orig.videoUrl.isNullOrBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    VideoLinkCard(
                                                        videoUrl = orig.videoUrl!!,
                                                        onClick = onVideoClick
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // .pactions bordées (border-top stroke-soft).
                                Spacer(Modifier.height(11.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(HubStrokeSoft)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable { viewModel.toggleLike() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (post.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = if (post.isLikedByCurrentUser) strings.unlikeDesc else strings.likeDesc,
                                                tint = if (post.isLikedByCurrentUser) HubError else HubTextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = strings.likesCountFormat.format(post.likesCount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (post.isLikedByCurrentUser) HubError else HubTextTertiary,
                                            modifier = Modifier
                                                .clickable { onOpenLikes(post.id) }
                                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { onOpenComments(post.id) }
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = strings.commentsDesc,
                                            tint = HubInfo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = strings.commentsCountFormat.format(state.totalCommentsCount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HubInfo
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { postToShare = post }
                                            .testTag("action_share_${post.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = strings.shareDesc,
                                            tint = HubSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable { viewModel.toggleBookmark() }
                                            .testTag("action_bookmark_${post.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (post.isBookmarkedByCurrentUser) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = if (post.isBookmarkedByCurrentUser) strings.bookmarkRemoveDesc else strings.bookmarkAddDesc,
                                            tint = if (post.isBookmarkedByCurrentUser) HubWhite else HubTextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Section commentaires — titre + aperçu en .row.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.comments,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HubWhite
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "· ${state.totalCommentsCount}",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HubMuted
                            )
                            Spacer(Modifier.weight(1f))
                            if (state.totalCommentsCount > 0) {
                                Text(
                                    text = strings.viewAllFormat.format(state.totalCommentsCount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .clickable { onOpenComments(post.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (state.commentsPreview.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(HubGlassSoft)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.noCommentsYetDesc,
                                    color = HubMuted,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.commentsPreview.forEach { comment ->
                                    CommentItemPreview(comment = comment, onOpenComments = { onOpenComments(post.id) })
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = strings.viewAllFormat.format(state.totalCommentsCount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = HubMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenComments(post.id) }
                                    .padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    // .composer verre — pastille de saisie + bouton d'envoi dégradé.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            variant = GlassVariant.Card
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(HubGlass)
                                        .border(1.dp, HubStroke, RoundedCornerShape(percent = 50))
                                        .clickable { onOpenComments(post.id) }
                                        .padding(horizontal = 15.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.addCommentPlaceholder,
                                        color = HubMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(brush = com.thehub.hb.ui.theme.hubPrimaryGradient())
                                        .clickable { onOpenComments(post.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Send,
                                        contentDescription = strings.send,
                                        tint = HubWhite,
                                        modifier = Modifier.size(17.dp)
                                    )
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
                onRepost = { viewModel.repost() }
            )
        }

        val detailSuccessState = uiState as? PostDetailUiState.Success
        val currentPost = detailSuccessState?.post

        if (showOptionsMenu && currentPost != null) {
            PostOptionsBottomSheet(
                post = currentPost,
                currentUserId = detailSuccessState.currentUserId,
                onDismiss = { showOptionsMenu = false },
                onEdit = { post ->
                    onEditPost(post)
                },
                onDelete = {
                    showDeleteConfirmation = true
                },
                onReport = {
                    showReportSheet = true
                },
                onCopyLink = { post ->
                    copyPostLinkToClipboard(context, post.id, strings.linkCopiedToast)
                }
            )
        }

        DeletePostConfirmationDialog(
            isOpen = showDeleteConfirmation,
            isDeleting = isDeletingPost,
            onConfirm = {
                isDeletingPost = true
                viewModel.deletePost { success, errorMsg ->
                    isDeletingPost = false
                    showDeleteConfirmation = false
                    if (success) {
                        Toast.makeText(context, strings.postDeletedToast, Toast.LENGTH_SHORT).show()
                        onPostDeleted()
                    } else {
                        Toast.makeText(context, errorMsg ?: strings.deleteErrorToast, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = {
                if (!isDeletingPost) {
                    showDeleteConfirmation = false
                }
            }
        )

        if (showReportSheet && currentPost != null) {
            ReportBottomSheet(
                targetType = "post",
                targetId = currentPost.id,
                targetName = if (currentPost.authorUsername.isNotBlank()) "@${currentPost.authorUsername}" else strings.thisPostLabel,
                onDismiss = { showReportSheet = false },
                onSubmitReport = { reason, details ->
                    isSubmittingReport = true
                    viewModel.reportPost(reason, details) { success, errorMsg ->
                        isSubmittingReport = false
                        showReportSheet = false
                        if (success) {
                            Toast.makeText(context, strings.reportSentToast, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(
                                context,
                                errorMsg ?: strings.reportErrorToast,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                isSubmitting = isSubmittingReport
            )
        }
    }
}

/** Iconbtn verre 40/14 de l'appbar détail. */
@Composable
private fun DetailGlassIcon(
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

/** .btn.xs — primaire dégradé (S'abonner) ou verre (Abonné), 11.5/700, rayon 10. */
@Composable
private fun DetailFollowButton(
    isFollowing: Boolean,
    isLoading: Boolean,
    followLabel: String,
    followingLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (!isFollowing) {
                    Modifier.background(brush = com.thehub.hb.ui.theme.hubPrimaryGradient(), shape = shape)
                } else {
                    Modifier
                        .background(HubGlass, shape)
                        .border(1.dp, HubStroke, shape)
                }
            )
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = HubWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isFollowing) followingLabel else followLabel,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
        }
    }
}

/** État « chargement » : carte de publication squelette. */
@Composable
private fun PostDetailSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HubDimens.CardRadius),
            variant = GlassVariant.Card
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HubGlassStrong)
                    )
                    Spacer(Modifier.width(11.dp))
                    Column {
                        HubSkeletonLine(height = 13.dp, widthFraction = 0.4f)
                        Spacer(Modifier.height(6.dp))
                        HubSkeletonLine(height = 11.dp, widthFraction = 0.3f)
                    }
                }
                Spacer(Modifier.height(14.dp))
                HubSkeletonLine(height = 14.dp, widthFraction = 0.92f)
                Spacer(Modifier.height(8.dp))
                HubSkeletonLine(height = 14.dp, widthFraction = 0.75f)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HubGlassStrong)
                )
            }
        }
    }
}

/** Aperçu de commentaire en .row (avatar 42, titre 13.5/700, texte 12.5). */
@Composable
private fun CommentItemPreview(
    comment: Comment,
    onOpenComments: () -> Unit
) {
    val strings = LocalHubStrings.current
    val commentAuthor = rememberLiveUser(userId = comment.authorId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenComments() }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = commentAuthor.effectiveName,
            photoUrl = commentAuthor.photoUrl,
            size = 42.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HubVerifiedUserName(
                    name = commentAuthor.effectiveName,
                    isVerified = commentAuthor.isVerified,
                    verificationType = commentAuthor.verificationType,
                    fontSize = 13.5.sp,
                    color = HubWhite,
                    badgeSize = 15.dp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "· ${RelativeTime.format(comment.createdAt)}",
                    fontSize = 11.5.sp,
                    color = HubMuted
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = HubSecondary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
