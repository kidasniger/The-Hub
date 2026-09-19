package com.thehub.hb.ui.comments

import android.widget.Toast
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.withTimeoutOrNull

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
    val context = LocalContext.current
    val threads = remember(uiState.comments) {
        buildCommentThreads(uiState.comments)
    }

    LaunchedEffect(uiState.actionFeedbackMessage) {
        uiState.actionFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedbackMessage()
        }
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
                                        currentUserId = uiState.currentUserId,
                                        postAuthorId = uiState.postAuthorId,
                                        isReply = false,
                                        onUserClick = onUserClick,
                                        onLikeClick = { viewModel.toggleCommentLike(thread.rootComment.id) },
                                        onReplyClick = { viewModel.startReply(thread.rootComment) },
                                        onLongPress = { viewModel.onCommentLongPress(it) },
                                        onOptionsClick = { viewModel.onCommentLongPress(it) }
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
                                                                currentUserId = uiState.currentUserId,
                                                                postAuthorId = uiState.postAuthorId,
                                                                isReply = true,
                                                                onUserClick = onUserClick,
                                                                onLikeClick = { viewModel.toggleCommentLike(reply.id) },
                                                                onReplyClick = { viewModel.startReply(reply) },
                                                                onLongPress = { viewModel.onCommentLongPress(it) },
                                                                onOptionsClick = { viewModel.onCommentLongPress(it) }
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
                        .border(width = 1.dp, color = HubOutline)
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
                        .clip(MaterialTheme.shapes.medium)
                        .background(HubCard)
                        .border(1.dp, HubBorder, MaterialTheme.shapes.medium)
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

        // Comment Options Bottom Sheet ("⋮" or 2s press)
        uiState.selectedCommentForOptions?.let { comment ->
            CommentOptionsBottomSheet(
                comment = comment,
                currentUserId = uiState.currentUserId,
                postAuthorId = uiState.postAuthorId,
                onDismiss = { viewModel.dismissOptions() },
                onEdit = { viewModel.startEditComment(comment) },
                onDelete = { viewModel.startDeleteComment(comment) },
                onToggleHide = { viewModel.toggleHideComment(comment) },
                onReport = { viewModel.startReportComment(comment) }
            )
        }

        // Edit Comment Dialog
        uiState.commentToEdit?.let { comment ->
            EditCommentDialog(
                initialText = uiState.editText,
                isLoading = uiState.isActionLoading,
                onDismiss = { viewModel.cancelEditComment() },
                onConfirm = { newText ->
                    viewModel.updateEditText(newText)
                    viewModel.confirmEditComment()
                }
            )
        }

        // Delete Comment Dialog
        uiState.commentToDelete?.let { comment ->
            DeleteCommentDialog(
                isLoading = uiState.isActionLoading,
                onDismiss = { viewModel.cancelDeleteComment() },
                onConfirm = { viewModel.confirmDeleteComment() }
            )
        }

        // Report Comment Dialog
        uiState.commentToReport?.let { comment ->
            ReportCommentDialog(
                isLoading = uiState.isActionLoading,
                onDismiss = { viewModel.cancelReportComment() },
                onConfirm = { reason -> viewModel.submitReportComment(reason) }
            )
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: Comment,
    currentUserId: String,
    postAuthorId: String,
    isReply: Boolean = false,
    onUserClick: ((String) -> Unit)? = null,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onLongPress: (Comment) -> Unit,
    onOptionsClick: (Comment) -> Unit
) {
    val author = rememberLiveUser(
        userId = comment.authorId
    )
    val haptic = LocalHapticFeedback.current
    val isPostAuthor = currentUserId.isNotBlank() && currentUserId == postAuthorId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isReply) HubSurfaceElevated else HubSurface)
            .border(1.dp, HubOutline, MaterialTheme.shapes.medium)
            .pointerInput(comment.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTime = System.currentTimeMillis()
                    val up = withTimeoutOrNull(2000L) {
                        waitForUpOrCancellation()
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    if (up == null && elapsed >= 1950L) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(comment)
                        try {
                            waitForUpOrCancellation()
                        } catch (_: Exception) {}
                    }
                }
            }
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                    if (comment.isEdited) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(modifié)",
                            fontSize = 11.sp,
                            color = HubMuted,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                // Options button
                IconButton(
                    onClick = { onOptionsClick(comment) },
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("comment_options_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options du commentaire",
                        tint = HubMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (comment.isHidden && !isPostAuthor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = HubMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ce commentaire a été masqué par l'auteur de la publication.",
                        fontSize = 12.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = HubMuted
                    )
                }
            } else {
                if (comment.isHidden && isPostAuthor) {
                    Surface(
                        color = HubSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = HubSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Masqué pour les autres utilisateurs",
                                fontSize = 11.sp,
                                color = HubSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                FormattedCommentText(text = comment.text, isReply = isReply)
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentOptionsBottomSheet(
    comment: Comment,
    currentUserId: String,
    postAuthorId: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHide: () -> Unit,
    onReport: () -> Unit
) {
    val isCommentAuthor = currentUserId.isNotBlank() && currentUserId == comment.authorId
    val isPostAuthor = currentUserId.isNotBlank() && currentUserId == postAuthorId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = HubWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(HubBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Options du commentaire",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${comment.text.take(60)}${if (comment.text.length > 60) "..." else ""}\"",
                fontSize = 12.sp,
                color = HubMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = HubBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Modifier (Only for comment author)
            if (isCommentAuthor) {
                CommentOptionRow(
                    icon = Icons.Outlined.Edit,
                    title = "Modifier le commentaire",
                    subtitle = "Corriger ou mettre à jour votre texte",
                    onClick = onEdit,
                    testTag = "option_edit_comment"
                )
            }

            // 2. Masquer / Démasquer (For the post creator)
            if (isPostAuthor) {
                CommentOptionRow(
                    icon = if (comment.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    title = if (comment.isHidden) "Démasquer le commentaire" else "Masquer le commentaire",
                    subtitle = if (comment.isHidden) "Rendre à nouveau visible aux autres" else "Visible uniquement par vous en tant que créateur",
                    tint = HubSecondary,
                    onClick = onToggleHide,
                    testTag = "option_hide_comment"
                )
            }

            // 3. Supprimer (For comment author OR post author)
            if (isCommentAuthor || isPostAuthor) {
                CommentOptionRow(
                    icon = Icons.Outlined.Delete,
                    title = "Supprimer le commentaire",
                    subtitle = if (isCommentAuthor) "Supprimer définitivement votre commentaire" else "Supprimer ce commentaire de votre publication",
                    tint = HubError,
                    textColor = HubError,
                    onClick = onDelete,
                    testTag = "option_delete_comment"
                )
            }

            // 4. Signaler (For everyone)
            CommentOptionRow(
                icon = Icons.Outlined.Flag,
                title = "Signaler ce commentaire",
                subtitle = "Signaler un abus, spam ou contenu inapproprié",
                tint = HubMuted,
                onClick = onReport,
                testTag = "option_report_comment"
            )
        }
    }
}

@Composable
private fun CommentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    tint: androidx.compose.ui.graphics.Color = HubWhite,
    textColor: androidx.compose.ui.graphics.Color = HubWhite,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HubCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = HubSecondary
                )
            }
        }
    }
}

@Composable
private fun EditCommentDialog(
    initialText: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Modifier le commentaire",
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Votre commentaire...", color = HubMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HubWhite,
                        unfocusedTextColor = HubWhite,
                        focusedBorderColor = HubWhite,
                        unfocusedBorderColor = HubBorder,
                        focusedContainerColor = HubCard,
                        unfocusedContainerColor = HubCard
                    ),
                    maxLines = 5,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != initialText && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubWhite,
                    contentColor = HubBlack
                ),
                modifier = Modifier.testTag("edit_comment_save_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HubBlack
                    )
                } else {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler", color = HubMuted)
            }
        }
    )
}

@Composable
private fun DeleteCommentDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = HubError,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Supprimer le commentaire ?",
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Cette action est irréversible. Le commentaire sera définitivement effacé.",
                color = HubSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubError,
                    contentColor = HubWhite
                ),
                modifier = Modifier.testTag("delete_comment_confirm_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HubWhite
                    )
                } else {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler", color = HubMuted)
            }
        }
    )
}

@Composable
private fun ReportCommentDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val reportReasons = listOf(
        "Spam ou contenu indésirable",
        "Harcèlement ou intimidation",
        "Propos haineux ou violents",
        "Contenu sexuel explicite",
        "Désinformation ou arnaque",
        "Autre raison"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Signaler ce commentaire",
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pourquoi souhaitez-vous signaler ce commentaire ?",
                    color = HubSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = HubWhite,
                                unselectedColor = HubMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            color = if (selectedReason == reason) HubWhite else HubSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason) },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubWhite,
                    contentColor = HubBlack
                ),
                modifier = Modifier.testTag("report_comment_confirm_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HubBlack
                    )
                } else {
                    Text("Signaler", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Annuler", color = HubMuted)
            }
        }
    )
}

