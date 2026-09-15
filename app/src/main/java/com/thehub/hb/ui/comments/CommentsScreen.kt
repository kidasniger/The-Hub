package com.thehub.hb.ui.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.repository.rememberLiveUser
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
import com.thehub.hb.utils.RelativeTime

private data class CommentThread(
    val rootComment: Comment,
    val replies: List<Comment>
)

private fun buildCommentThreads(comments: List<Comment>): List<CommentThread> {
    val commentMap = comments.associateBy { it.id }

    fun findRootId(c: Comment): String {
        var current = c
        var hops = 0
        while (!current.parentCommentId.isNullOrBlank() && commentMap.containsKey(current.parentCommentId) && hops < 10) {
            current = commentMap[current.parentCommentId] ?: break
            hops++
        }
        return current.id
    }

    val rootComments = comments.filter { it.parentCommentId.isNullOrBlank() || !commentMap.containsKey(it.parentCommentId) }

    val repliesByRootId = mutableMapOf<String, MutableList<Comment>>()
    for (c in comments) {
        if (!c.parentCommentId.isNullOrBlank() && commentMap.containsKey(c.parentCommentId)) {
            val rootId = findRootId(c)
            repliesByRootId.getOrPut(rootId) { mutableListOf() }.add(c)
        }
    }

    return rootComments.map { root ->
        val replies = repliesByRootId[root.id]?.sortedBy { it.createdAt } ?: emptyList()
        CommentThread(
            rootComment = root,
            replies = replies
        )
    }
}

@Composable
fun CommentsScreen(
    viewModel: CommentsViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val threads = remember(uiState.comments) {
        buildCommentThreads(uiState.comments)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .imePadding()
            .testTag("comments_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("comments_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                Text(
                    text = "Commentaires",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(start = 8.dp)
                )

                if (uiState.comments.isNotEmpty()) {
                    Text(
                        text = " (${uiState.comments.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = HubSecondary
                    )
                }
            }

            // Error banner if any
            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .background(HubError.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = HubError,
                        fontSize = 12.sp
                    )
                }
            }

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && uiState.comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HubWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    uiState.comments.isEmpty() -> {
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
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(HubSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = HubSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Aucun commentaire pour l'instant",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HubWhite
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Lancez la discussion en écrivant le premier commentaire !",
                                    fontSize = 13.sp,
                                    color = HubSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = threads,
                                key = { it.rootComment.id }
                            ) { thread ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    CommentItemRow(
                                        comment = thread.rootComment,
                                        isReply = false,
                                        onUserClick = onUserClick,
                                        onLikeClick = { viewModel.toggleCommentLike(thread.rootComment.id) },
                                        onReplyClick = { viewModel.startReply(thread.rootComment) }
                                    )

                                    // Replies section
                                    if (thread.replies.isNotEmpty()) {
                                        val hasMultiple = thread.replies.size > 1
                                        val isExpanded = uiState.expandedThreads.contains(thread.rootComment.id)

                                        if (hasMultiple && !isExpanded) {
                                            // Collapsed toggle link for multiple replies
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 28.dp, top = 6.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.toggleThreadExpanded(thread.rootComment.id) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .testTag("expand_replies_${thread.rootComment.id}"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(20.dp)
                                                        .height(1.dp)
                                                        .background(HubSecondary.copy(alpha = 0.5f))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Voir les ${thread.replies.size} réponses",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HubSecondary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = HubSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            // Show replies indented visually under the parent comment
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 24.dp, top = 8.dp)
                                            ) {
                                                thread.replies.forEach { reply ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(16.dp)
                                                                .padding(top = 16.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(12.dp)
                                                                    .height(1.dp)
                                                                    .background(HubBorder)
                                                            )
                                                        }
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            CommentItemRow(
                                                                comment = reply,
                                                                isReply = true,
                                                                onUserClick = onUserClick,
                                                                onLikeClick = { viewModel.toggleCommentLike(reply.id) },
                                                                onReplyClick = { viewModel.startReply(reply) }
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }

                                                // If multiple and expanded, show collapse toggle link
                                                if (hasMultiple) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 16.dp, top = 4.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.toggleThreadExpanded(thread.rootComment.id) }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                            .testTag("collapse_replies_${thread.rootComment.id}"),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(20.dp)
                                                                .height(1.dp)
                                                                .background(HubSecondary.copy(alpha = 0.5f))
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Masquer les réponses",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = HubSecondary
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.KeyboardArrowUp,
                                                            contentDescription = null,
                                                            tint = HubSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reply banner above sticky input bar if in reply mode
            AnimatedVisibility(
                visible = uiState.replyingTo != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HubSurfaceElevated)
                        .border(width = 1.dp, color = HubBorder)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("comment_reply_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Réponse à @${uiState.replyingTo?.authorUsername}",
                        color = HubSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.cancelReply() },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("comment_cancel_reply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Annuler la réponse",
                            tint = HubSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Sticky Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HubSurfaceElevated)
                    .border(width = 1.dp, color = HubBorder)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = uiState.currentUserUsername,
                    photoUrl = uiState.currentUserPhotoUrl,
                    size = 36.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(HubCard)
                        .border(1.dp, HubBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (uiState.inputText.isEmpty()) {
                        val placeholder = if (uiState.replyingTo != null) {
                            "Répondre à @${uiState.replyingTo?.authorUsername}..."
                        } else {
                            "Ajouter un commentaire..."
                        }
                        Text(
                            text = placeholder,
                            color = HubMuted,
                            fontSize = 14.sp
                        )
                    }

                    BasicTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comments_input_field"),
                        textStyle = TextStyle(
                            color = HubWhite,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(HubWhite),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.sendComment() }),
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.sendComment() },
                    enabled = uiState.canSend,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (uiState.canSend) HubWhite else HubDarkGray)
                        .testTag("comments_send_button")
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            color = HubBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (uiState.canSend) HubBlack else HubMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: Comment,
    isReply: Boolean = false,
    onUserClick: ((String) -> Unit)? = null,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val author = rememberLiveUser(
        userId = comment.authorId
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isReply) HubSurfaceElevated else HubCard)
            .border(1.dp, HubBorder, RoundedCornerShape(12.dp))
            .padding(if (isReply) 12.dp else 14.dp)
            .testTag("comment_item_${comment.id}"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.then(
                if (onUserClick != null) Modifier.clickable { onUserClick(comment.authorId) } else Modifier
            )
        ) {
            UserAvatar(
                name = author.effectiveName,
                photoUrl = author.photoUrl,
                size = if (isReply) 30.dp else 38.dp
            )
        }

        Spacer(modifier = Modifier.width(if (isReply) 10.dp else 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = author.effectiveName,
                    fontSize = if (isReply) 13.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.then(
                        if (onUserClick != null) Modifier.clickable { onUserClick(comment.authorId) } else Modifier
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "·",
                    fontSize = 12.sp,
                    color = HubMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = RelativeTime.format(comment.createdAt),
                    fontSize = 12.sp,
                    color = HubMuted
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            FormattedCommentText(text = comment.text, isReply = isReply)

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: "J'aime" with Heart icon & counter + "Répondre" link
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart Like button with counter
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("comment_like_button_${comment.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (comment.isLikedByCurrentUser) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (comment.isLikedByCurrentUser) "Je n'aime plus" else "J'aime",
                        tint = if (comment.isLikedByCurrentUser) HubError else HubSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likesCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (comment.isLikedByCurrentUser) HubError else HubSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Reply link
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReplyClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("comment_reply_button_${comment.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Répondre",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FormattedCommentText(text: String, isReply: Boolean) {
    val words = text.split(" ")
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (word.startsWith("@") && word.length > 1) {
                withStyle(
                    SpanStyle(
                        color = HubWhite,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(word)
                }
            } else {
                append(word)
            }
            if (index < words.size - 1) {
                append(" ")
            }
        }
    }
    Text(
        text = annotatedString,
        fontSize = if (isReply) 13.5.sp else 14.sp,
        lineHeight = 20.sp,
        color = HubWhite
    )
}

