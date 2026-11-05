package com.thehub.hb.ui.comments

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubErrorBanner
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val threads = remember(uiState.comments) {
        buildCommentThreads(uiState.comments)
    }

    LaunchedEffect(uiState.actionFeedbackMessage) {
        uiState.actionFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .testTag("comments_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // .appbar — retour (verre), titre 16/800 + compteur.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommentsGlassIcon(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("comments_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.comments,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                            color = HubWhite
                        )
                        if (uiState.comments.isNotEmpty()) {
                            Text(
                                text = " · ${uiState.comments.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HubMuted
                            )
                        }
                    }
                }
            }

            // Onglets soulignés de la maquette (Pertinents actif).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                CommentsTabLabel(text = strings.relevantTab, selected = true)
                CommentsTabLabel(text = strings.recentTab, selected = false)
            }

            if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    HubErrorBanner(message = uiState.errorMessage ?: "")
                }
            }

            // Zone de contenu.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && uiState.comments.isEmpty() -> {
                        // État « chargement » : bulles squelettes.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(4) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(HubGlassStrong)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        HubSkeletonLine(height = 52.dp, widthFraction = 1f)
                                    }
                                }
                            }
                        }
                    }

                    uiState.comments.isEmpty() -> {
                        // État « vide » : icône verre 76 + textes de la maquette.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HubEmptyState(
                                title = strings.noCommentsTitle,
                                description = strings.noCommentsDesc,
                                icon = Icons.Outlined.ChatBubbleOutline
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

                                    // Réponses imbriquées.
                                    if (thread.replies.isNotEmpty()) {
                                        val hasMultiple = thread.replies.size > 1
                                        val isExpanded = uiState.expandedThreads.contains(thread.rootComment.id)

                                        if (hasMultiple && !isExpanded) {
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
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = strings.seeRepliesFormat.format(thread.replies.size),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HubSecondary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = HubSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 34.dp, top = 4.dp)
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
                                                                    .background(HubStrokeSoft)
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
                                                    Spacer(Modifier.height(8.dp))
                                                }

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
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = strings.hideReplies,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = HubSecondary
                                                        )
                                                        Spacer(Modifier.width(4.dp))
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )

            // Bandeau « réponse en cours » au-dessus du composeur.
            AnimatedVisibility(
                visible = uiState.replyingTo != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HubGlassSoft)
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
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.replyToFormat.format(uiState.replyingTo?.authorUsername ?: ""),
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
                            contentDescription = strings.cancelReplyDesc,
                            tint = HubSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // .composer verre — avatar, pastille de saisie, envoi dégradé.
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
                        UserAvatar(
                            name = uiState.currentUserUsername,
                            photoUrl = uiState.currentUserPhotoUrl,
                            size = 34.dp
                        )

                        Spacer(Modifier.width(9.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(HubGlass)
                                .border(1.dp, HubStroke, RoundedCornerShape(percent = 50))
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text = if (uiState.replyingTo != null) {
                                        strings.replyToFormat.format(uiState.replyingTo?.authorUsername ?: "")
                                    } else {
                                        strings.addCommentPlaceholder
                                    },
                                    color = HubMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
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
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(palette.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { viewModel.sendComment() }),
                                maxLines = 4
                            )
                        }

                        Spacer(Modifier.width(9.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (uiState.canSend) {
                                        Modifier.background(brush = hubPrimaryGradient())
                                    } else {
                                        Modifier
                                            .background(HubGlass)
                                            .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                                    }
                                )
                                .clickable(enabled = uiState.canSend) { viewModel.sendComment() }
                                .testTag("comments_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSending) {
                                CircularProgressIndicator(
                                    color = HubWhite,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = strings.send,
                                    tint = if (uiState.canSend) HubWhite else HubMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feuille d'options d'un commentaire.
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

        uiState.commentToDelete?.let { comment ->
            DeleteCommentDialog(
                isLoading = uiState.isActionLoading,
                onDismiss = { viewModel.cancelDeleteComment() },
                onConfirm = { viewModel.confirmDeleteComment() }
            )
        }

        uiState.commentToReport?.let { comment ->
            ReportCommentDialog(
                isLoading = uiState.isActionLoading,
                onDismiss = { viewModel.cancelReportComment() },
                onConfirm = { reason -> viewModel.submitReportComment(reason) }
            )
        }
    }
}

/** Iconbtn verre 40/14 de l'appbar commentaires. */
@Composable
private fun CommentsGlassIcon(
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

/** Onglet souligné .tabs — actif : texte + barre dégradée 3 dp. */
@Composable
private fun CommentsTabLabel(text: String, selected: Boolean) {
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val author = rememberLiveUser(
        userId = comment.authorId
    )
    val haptic = LocalHapticFeedback.current
    val isPostAuthor = currentUserId.isNotBlank() && currentUserId == postAuthorId
    val isCommentAuthor = comment.authorId == postAuthorId

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                size = if (isReply) 30.dp else 34.dp
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Bulle .glass-soft rayon 16 — nom 12/800, badge Auteur, heure 10.
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                variant = GlassVariant.Soft
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HubVerifiedUserName(
                                name = author.effectiveName,
                                isVerified = author.isVerified,
                                verificationType = author.verificationType,
                                fontSize = 12.sp,
                                color = HubWhite,
                                weight = FontWeight.ExtraBold,
                                badgeSize = if (isReply) 13.dp else 14.dp,
                                modifier = Modifier.then(
                                    if (onUserClick != null) Modifier.clickable { onUserClick(comment.authorId) } else Modifier
                                )
                            )
                            // Badge-vip « Auteur » de la maquette (dégradé de marque).
                            if (isCommentAuthor) {
                                Text(
                                    text = strings.authorBadge,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.4.sp,
                                    color = HubWhite,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(brush = hubPrimaryGradient())
                                        .padding(horizontal = 7.dp, vertical = 1.5.dp)
                                )
                            }
                        }

                        Text(
                            text = RelativeTime.format(comment.createdAt),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = HubMuted
                        )

                        IconButton(
                            onClick = { onOptionsClick(comment) },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("comment_options_${comment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = strings.commentOptionsTitle,
                                tint = HubMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

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
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = strings.hiddenByAuthorLabel,
                                fontSize = 12.5.sp,
                                fontStyle = FontStyle.Italic,
                                color = HubMuted
                            )
                        }
                    } else {
                        if (comment.isHidden && isPostAuthor) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(HubGlassStrong)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = HubSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = strings.hiddenForOthersLabel,
                                    fontSize = 11.sp,
                                    color = HubSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        FormattedCommentText(text = comment.text, isReply = isReply)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Actions : J'aime (13) + Répondre (11), gap 14, padding 6 4.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp)
            ) {
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
                        contentDescription = if (comment.isLikedByCurrentUser) strings.unlikeDesc else strings.likeDesc,
                        tint = if (comment.isLikedByCurrentUser) HubError else HubMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    if (comment.likesCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${comment.likesCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comment.isLikedByCurrentUser) HubError else HubMuted
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReplyClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("comment_reply_button_${comment.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = HubMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.replyLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubMuted
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
        fontSize = if (isReply) 12.sp else 12.5.sp,
        lineHeight = if (isReply) 18.sp else 19.sp,
        color = HubSecondary
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val isCommentAuthor = currentUserId.isNotBlank() && currentUserId == comment.authorId
    val isPostAuthor = currentUserId.isNotBlank() && currentUserId == postAuthorId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.bg1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.stroke)
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
                text = strings.commentOptionsTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HubWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "\"${comment.text.take(60)}${if (comment.text.length > 60) "..." else ""}\"",
                fontSize = 12.sp,
                color = HubMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HubStrokeSoft)
            )
            Spacer(Modifier.height(10.dp))

            // 1. Modifier (auteur du commentaire uniquement)
            if (isCommentAuthor) {
                CommentOptionRow(
                    icon = Icons.Outlined.Edit,
                    title = strings.editCommentTitle,
                    subtitle = strings.editCommentSubtitle,
                    onClick = onEdit,
                    testTag = "option_edit_comment"
                )
            }

            // 2. Masquer / Démasquer (créateur de la publication)
            if (isPostAuthor) {
                CommentOptionRow(
                    icon = if (comment.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    title = if (comment.isHidden) strings.unhideCommentTitle else strings.hideCommentTitle,
                    subtitle = if (comment.isHidden) strings.unhideCommentSubtitle else strings.hideCommentSubtitle,
                    tint = HubSecondary,
                    onClick = onToggleHide,
                    testTag = "option_hide_comment"
                )
            }

            // 3. Supprimer (auteur du commentaire OU de la publication)
            if (isCommentAuthor || isPostAuthor) {
                CommentOptionRow(
                    icon = Icons.Outlined.Delete,
                    title = strings.deleteCommentTitle,
                    subtitle = if (isCommentAuthor) strings.deleteCommentSubtitleOwn else strings.deleteCommentSubtitleAuthor,
                    tint = HubError,
                    textColor = HubError,
                    onClick = onDelete,
                    testTag = "option_delete_comment"
                )
            }

            // 4. Signaler (tout le monde)
            CommentOptionRow(
                icon = Icons.Outlined.Flag,
                title = strings.reportCommentTitle,
                subtitle = strings.reportCommentSubtitle,
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
                .background(HubGlassSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = palette.bg1,
        title = {
            Text(
                text = strings.editCommentTitle,
                fontWeight = FontWeight.ExtraBold,
                color = HubWhite
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(strings.editCommentInputPlaceholder, color = HubMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HubWhite,
                        unfocusedTextColor = HubWhite,
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = HubStrokeSoft,
                        focusedContainerColor = HubGlassSoft,
                        unfocusedContainerColor = HubGlassSoft
                    ),
                    maxLines = 5,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != initialText && !isLoading,
                modifier = Modifier.testTag("edit_comment_save_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.accent
                    )
                } else {
                    Text(
                        strings.saveLabel,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(strings.dialogCancel, color = HubMuted)
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = palette.bg1,
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
                text = strings.deleteCommentDialogTitle,
                fontWeight = FontWeight.ExtraBold,
                color = HubWhite,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = strings.deleteCommentDialogMessage,
                color = HubSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading,
                modifier = Modifier.testTag("delete_comment_confirm_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HubError
                    )
                } else {
                    Text(
                        strings.deletePostOption,
                        fontWeight = FontWeight.Bold,
                        color = HubError
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(strings.dialogCancel, color = HubMuted)
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val reportReasons = listOf(
        strings.reportReasonSpam,
        strings.reportReasonHarassment,
        strings.reportReasonHate,
        strings.reportReasonSexual,
        strings.reportReasonMisinformation,
        strings.reportReasonOther
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = palette.bg1,
        title = {
            Text(
                text = strings.reportCommentTitle,
                fontWeight = FontWeight.ExtraBold,
                color = HubWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.reportCommentQuestion,
                    color = HubSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
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
                                selectedColor = palette.accent,
                                unselectedColor = HubMuted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
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
            TextButton(
                onClick = { onConfirm(selectedReason) },
                enabled = !isLoading,
                modifier = Modifier.testTag("report_comment_confirm_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.accent
                    )
                } else {
                    Text(
                        strings.reportPost,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(strings.dialogCancel, color = HubMuted)
            }
        }
    )
}
