package com.thehub.hb.ui.messenger.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.utils.RelativeTime
import com.thehub.hb.utils.extractHubPostId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onChatInfoClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onPostClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val inputBringIntoViewRequester = remember { BringIntoViewRequester() }
    var inputFieldFocused by remember { mutableStateOf(false) }
    var initialScrollDone by remember(viewModel.conversationId) { mutableStateOf(false) }
    var messagePendingDeletion by remember { mutableStateOf<Message?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = com.thehub.hb.utils.ImageCompressor.compressImageFromUri(context, uri)
                if (bytes != null) {
                    viewModel.onImageSelected(uri, bytes)
                } else {
                    viewModel.showErrorMessage("Impossible de lire ou de préparer cette image.")
                }
            }
        }
    }

    LaunchedEffect(
        viewModel.conversationId,
        uiState.messages.size,
        uiState.messages.lastOrNull()?.id
    ) {
        if (!initialScrollDone && uiState.messages.isNotEmpty()) {
            // Wait until LazyColumn has actually composed its items before jumping
            // to the newest message. Otherwise scrollToItem can run while the list
            // still reports zero items and leave the user at the beginning.
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it >= uiState.messages.size }
            listState.scrollToItem(uiState.messages.lastIndex)
            initialScrollDone = true
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        val last = uiState.messages.lastOrNull() ?: return@LaunchedEffect
        if (initialScrollDone && last.senderId == viewModel.currentUserId) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.firstVisibleItemIndex to listState.isScrollInProgress
        }.collect { (firstIndex, scrolling) ->
            if (scrolling && firstIndex <= 2 && viewModel.uiState.value.hasMoreOlderMessages) {
                viewModel.loadOlderMessages()
            }
        }
    }

    LaunchedEffect(inputFieldFocused) {
        if (inputFieldFocused) {
            delay(100)
            inputBringIntoViewRequester.bringIntoView()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    val liveContact = rememberLiveUser(
        userId = uiState.otherUserId,
        fallbackUsername = uiState.otherParticipantInfo.username,
        fallbackDisplayName = uiState.otherParticipantInfo.displayName,
        fallbackPhotoUrl = uiState.otherParticipantInfo.photoUrl
    )
    val contactName = liveContact.effectiveName

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }

                // Contact Profile info clickable to navigate to ChatInfo
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onChatInfoClick(viewModel.conversationId) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("chat_header_profile"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = contactName,
                        photoUrl = liveContact.photoUrl,
                        size = 38.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusText = when {
                            uiState.isOtherTyping -> "écrit..."
                            uiState.presence.isFresh(System.currentTimeMillis() / 1000L) -> "En ligne"
                            uiState.presence.lastSeen != null -> "Vu " + RelativeTime.format(uiState.presence.lastSeen)
                            else -> "Hors ligne"
                        }
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = if (uiState.isOtherTyping || statusText == "En ligne") HubSecondary else HubMuted
                        )
                    }
                }

                IconButton(
                    onClick = { onChatInfoClick(viewModel.conversationId) },
                    modifier = Modifier.testTag("chat_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Infos de la conversation",
                        tint = HubWhite
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HubBorder)
            )

            // Message List
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HubWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                val lastSentMessage = uiState.messages.lastOrNull { it.isSentBy(viewModel.currentUserId) }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (uiState.isLoadingOlderMessages) {
                        item(key = "loading_older_messages") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = HubMuted,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = uiState.messages,
                        key = { _, msg -> msg.id.ifEmpty { msg.createdAt.seconds.toString() } }
                    ) { index, message ->
                        val isCurrentUser = message.isSentBy(viewModel.currentUserId)
                        val isLastSent = isCurrentUser && message.id == lastSentMessage?.id

                        // Check if time should be shown (next msg from other sender or > 5 min later or last message)
                        val nextMsg = uiState.messages.getOrNull(index + 1)
                        val showTime = nextMsg == null ||
                                nextMsg.senderId != message.senderId ||
                                (nextMsg.createdAt.toDate().time - message.createdAt.toDate().time) > 5 * 60 * 1000L

                        MessageBubble(
                            message = message,
                            isCurrentUser = isCurrentUser,
                            showTime = showTime,
                            isLastSent = isLastSent,
                            onImageClick = onImageClick,
                            reactionList = uiState.reactions.filter { it.messageId == message.id },
                            currentUserId = viewModel.currentUserId,
                            onReact = { viewModel.react(message.id, it) },
                            onReply = { viewModel.startReply(message) },
                            onEdit = { viewModel.startEdit(message) },
                            onDelete = { messagePendingDeletion = message },
                            onRetry = { viewModel.retryMessage(message) },
                            onPostClick = onPostClick
                        )
                    }
                }
            }

            if (uiState.replyingTo != null || uiState.editingMessageId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(HubSurfaceElevated).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.editingMessageId != null) "Modification" else "Réponse",
                            color = HubSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.replyingTo?.text ?: "Photo",
                            color = HubWhite,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { viewModel.cancelComposerMode() }) {
                        Icon(Icons.Default.Close, contentDescription = "Annuler", tint = HubMuted)
                    }
                }
            }

            // Image Preview (if user selected photo before sending)
            if (uiState.selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HubSurfaceElevated)
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uiState.selectedImageUri,
                            contentDescription = "Image sélectionnée",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = { viewModel.onRemoveSelectedImage() },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Supprimer l'image",
                                tint = HubWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HubBlack)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Image picker button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HubSurfaceElevated)
                        .testTag("chat_attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Joindre une photo",
                        tint = HubWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HubSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.inputText.isEmpty()) {
                        Text(
                            text = when {
                                uiState.editingMessageId != null -> "Modifier le message..."
                                uiState.replyingTo != null -> "Répondre..."
                                else -> "Votre message..."
                            },
                            color = HubMuted,
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(inputBringIntoViewRequester)
                            .onFocusChanged { inputFieldFocused = it.isFocused }
                            .testTag("chat_input_field"),
                        textStyle = TextStyle(
                            color = HubWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(HubWhite),
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = (
                    uiState.inputText.isNotBlank() ||
                        uiState.selectedImageBytes != null ||
                        uiState.editingMessageId != null
                    ) && !uiState.isSending

                // Send button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) HubWhite else HubDarkGray)
                        .testTag("chat_send_button")
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            color = HubBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = if (canSend) HubBlack else HubMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
        )

        messagePendingDeletion?.let { message ->
            AlertDialog(
                onDismissRequest = { messagePendingDeletion = null },
                title = { Text("Supprimer le message") },
                text = {
                    Text(
                        "Ce message sera remplacé par « Message supprimé ». Cette action ne peut pas être annulée."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(message.id)
                            messagePendingDeletion = null
                        }
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messagePendingDeletion = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    showTime: Boolean,
    isLastSent: Boolean,
    onImageClick: (String) -> Unit,
    reactionList: List<com.thehub.hb.data.model.MessageReaction>,
    currentUserId: String,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPostClick: (String) -> Unit
) {
    var menuExpanded by remember(message.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        val bubbleShape = if (isCurrentUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }
        val bubbleBackground = if (isCurrentUser) HubWhite else HubCard
        val textColor = if (isCurrentUser) HubBlack else HubWhite

        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(bubbleShape)
                    .background(bubbleBackground)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuExpanded = true }
                    )
                    .testTag("message_bubble_" + message.id)
            ) {
                if (!message.replyToText.isNullOrBlank() || !message.replyToMessageId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrentUser) HubBlack.copy(alpha = 0.08f) else HubWhite.copy(alpha = 0.08f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = message.replyToText ?: "Photo",
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (message.isDeleted) {
                    Text(
                        text = "Message supprimé",
                        color = textColor.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                } else {
                    if (!message.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Photo envoyée",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clickable { onImageClick(message.imageUrl) },
                            contentScale = ContentScale.Crop
                        )
                    }

                    if (!message.text.isNullOrBlank()) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                        LinkPreviewCard(
                            text = message.text,
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        )
                        extractHubPostId(message.text)?.let { sharedPostId ->
                            SharedPostLinkCard(
                                postId = sharedPostId,
                                isCurrentUser = isCurrentUser,
                                onPostClick = onPostClick
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                Text(
                    text = "Réagir",
                    color = HubMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                listOf("👍", "❤️", "😂", "😮", "😢", "🔥").forEach { emoji ->
                    DropdownMenuItem(
                        text = { Text(emoji) },
                        onClick = {
                            menuExpanded = false
                            onReact(emoji)
                        }
                    )
                }
                if (!message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Répondre") },
                        onClick = {
                            menuExpanded = false
                            onReply()
                        }
                    )
                }
                if (isCurrentUser && !message.isDeleted &&
                    message.status != Message.STATUS_PENDING &&
                    message.status != Message.STATUS_FAILED
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
                if (message.status == Message.STATUS_FAILED) {
                    DropdownMenuItem(
                        text = { Text("Réessayer") },
                        onClick = {
                            menuExpanded = false
                            onRetry()
                        }
                    )
                }
            }
        }

        if (reactionList.isNotEmpty() && !message.isDeleted) {
            val grouped = reactionList.groupingBy { it.emoji }.eachCount()
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (emoji, count) ->
                    Text(
                        text = emoji + " " + count,
                        fontSize = 11.sp,
                        color = if (reactionList.any { it.userId == currentUserId && it.emoji == emoji }) HubSecondary else HubMuted
                    )
                }
            }
        }

        if (message.editedAt != null && !message.isDeleted) {
            Text(
                text = "modifié",
                fontSize = 10.sp,
                color = HubMuted,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

        if (showTime || (isCurrentUser && isLastSent)) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showTime) {
                    Text(
                        text = RelativeTime.formatTimeOnly(message.createdAt),
                        fontSize = 11.sp,
                        color = HubMuted
                    )
                }

                if (isCurrentUser && isLastSent) {
                    when (message.status) {
                        Message.STATUS_PENDING -> Text("…", fontSize = 13.sp, color = HubMuted)
                        Message.STATUS_FAILED -> Text("Échec", fontSize = 11.sp, color = HubMuted, fontWeight = FontWeight.Medium)
                        Message.STATUS_READ -> Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Message lu",
                            tint = HubSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Message.STATUS_DELIVERED -> Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Message délivré",
                            tint = HubMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Envoyé",
                            tint = HubMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun SharedPostLinkCard(
    postId: String,
    isCurrentUser: Boolean,
    onPostClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentUser) HubBlack.copy(alpha = 0.06f) else HubSurfaceElevated)
            .padding(12.dp)
    ) {
        Text(
            text = "📌 Publication The Hub",
            color = if (isCurrentUser) HubBlack else HubWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cette publication a été partagée dans cette conversation.",
            color = if (isCurrentUser) HubBlack.copy(alpha = 0.7f) else HubMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = { onPostClick(postId) }
        ) {
            Text("Ouvrir la publication")
        }
    }
}
