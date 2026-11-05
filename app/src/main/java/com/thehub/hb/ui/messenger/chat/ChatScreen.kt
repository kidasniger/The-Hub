package com.thehub.hb.ui.messenger.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.LinkPreviewCard
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.ImageCompressor
import com.thehub.hb.utils.RelativeTime
import com.thehub.hb.utils.extractHubPostId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Écran 13 « Conversation » — maquette Aurora Glass.
 *
 * App bar bordée (retour, avatar 40 + point de présence, nom 15 + badge
 * vérifié, statut « En ligne » en vert / « Vu … », menu ⋮) ; fil .chat-wrap
 * (gap 9) avec pastille de date centrée, bulles ENTRANTES en verre
 * (.bubble.in : GlassSurface, rayon 20, coin bas-gauche 8) et bulles
 * SORTANTES en dégradé violet→bleu (.bubble.out, lueur + reflet), heures
 * 9.5 à l'intérieur (.btime) + réactions en pilule .react ; bulle
 * « en train d'écrire » à trois points animés ; composeur en verre rayon 26 :
 * « + », champ pilule « Écris ton message… » avec icônes photo, micro,
 * bouton envoyer 42/15 dégradé. Clavier : imePadding + bringIntoView.
 */
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
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current

    // Liste d'affichage : pastilles de date + messages (clés stables).
    val displayItems = remember(uiState.messages, strings) {
        buildList {
            var lastDayKey = -1L
            uiState.messages.forEach { msg ->
                val dayKey = dayKeyOf(msg.createdAt)
                if (dayKey != lastDayKey) {
                    add(
                        ChatRowItem.DatePill(
                            label = dateLabelFor(msg.createdAt, strings.lot4.todayLabel),
                            key = "date_$dayKey"
                        )
                    )
                    lastDayKey = dayKey
                }
                add(ChatRowItem.MessageItem(msg))
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bytes = ImageCompressor.compressImageFromUri(context, uri)
                if (bytes != null) {
                    viewModel.onImageSelected(uri, bytes)
                } else {
                    viewModel.showErrorMessage(strings.lot3.imagePrepareError)
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
                .first { it >= displayItems.size }
            listState.scrollToItem(displayItems.lastIndex)
            initialScrollDone = true
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.id) {
        val last = uiState.messages.lastOrNull() ?: return@LaunchedEffect
        if (initialScrollDone && last.senderId == viewModel.currentUserId) {
            listState.animateScrollToItem(displayItems.lastIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
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
    val nowSeconds = System.currentTimeMillis() / 1000L
    val isContactOnline = uiState.presence.isFresh(nowSeconds)

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- App bar bordée --stroke-soft ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassIconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Profil contact cliquable → infos de conversation.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onChatInfoClick(viewModel.conversationId) }
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                        .testTag("chat_header_profile"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    UserAvatar(
                        name = contactName,
                        photoUrl = liveContact.photoUrl,
                        size = 40.dp,
                        isOnline = isContactOnline
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        HubVerifiedUserName(
                            name = contactName,
                            isVerified = liveContact.isVerified,
                            verificationType = liveContact.verificationType,
                            fontSize = 15.sp,
                            color = HubWhite,
                            badgeSize = 16.dp
                        )
                        val statusText = when {
                            uiState.isOtherTyping -> strings.lot4.chatTypingLabel
                            isContactOnline -> strings.lot4.onlineLabel
                            uiState.presence.lastSeen != null -> strings.lot4.lastSeenFormat.format(
                                RelativeTime.format(uiState.presence.lastSeen)
                            )
                            else -> strings.lot4.offlineLabel
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                uiState.isOtherTyping -> HubAccent
                                isContactOnline -> HubSuccess
                                else -> HubTextTertiary
                            }
                        )
                    }
                }

                GlassIconButton(
                    onClick = { onChatInfoClick(viewModel.conversationId) },
                    modifier = Modifier.testTag("chat_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = strings.lot4.chatInfoTitle,
                        tint = HubWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HubStrokeSoft)
            )

            // ---- Fil de messages .chat-wrap ----
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HubAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                val lastSentMessage = uiState.messages.lastOrNull {
                    it.isSentBy(viewModel.currentUserId)
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (uiState.isLoadingOlderMessages) {
                        item(key = "loading_older_messages") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = HubTextTertiary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = displayItems,
                        key = { _, item -> item.key }
                    ) { _, item ->
                        when (item) {
                            is ChatRowItem.DatePill -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HubTextTertiary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(HubGlassSoft)
                                            .border(1.dp, HubStrokeSoft, RoundedCornerShape(percent = 50))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            is ChatRowItem.MessageItem -> {
                                val message = item.message
                                val isCurrentUser = message.isSentBy(viewModel.currentUserId)
                                val isLastSent = isCurrentUser && message.id == lastSentMessage?.id

                                // Heure visible si rupture d'envoyeur, > 5 min, ou dernier message.
                                val nextMsg = uiState.messages.getOrNull(
                                    uiState.messages.indexOfFirst { it.id == message.id } + 1
                                )
                                val showTime = nextMsg == null ||
                                        nextMsg.senderId != message.senderId ||
                                        (nextMsg.createdAt.toDate().time - message.createdAt.toDate().time) >
                                        5 * 60 * 1000L

                                MessageBubble(
                                    message = message,
                                    isCurrentUser = isCurrentUser,
                                    showTime = showTime,
                                    isLastSent = isLastSent,
                                    onImageClick = onImageClick,
                                    reactionList = uiState.reactions.filter {
                                        it.messageId == message.id
                                    },
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

                    if (uiState.isOtherTyping) {
                        item(key = "typing_indicator") {
                            TypingBubble()
                        }
                    }
                }
            }

            // ---- Barre réponse / modification (verre doux) ----
            if (uiState.replyingTo != null || uiState.editingMessageId != null) {
                GlassSurface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    variant = GlassVariant.Soft
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.editingMessageId != null) {
                                Icons.Outlined.Edit
                            } else {
                                Icons.AutoMirrored.Outlined.Reply
                            },
                            contentDescription = null,
                            tint = HubAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                text = if (uiState.editingMessageId != null) {
                                    strings.lot4.editBarTitle
                                } else {
                                    strings.lot4.replyBarTitle
                                },
                                color = HubAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.replyingTo?.text ?: strings.lot4.photoReferenceLabel,
                                color = HubWhite,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.dialogCancel,
                            tint = HubTextTertiary,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.cancelComposerMode() }
                                .padding(8.dp)
                        )
                    }
                }
            }

            // ---- Aperçu de l'image sélectionnée ----
            if (uiState.selectedImageUri != null) {
                GlassSurface(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    variant = GlassVariant.Soft
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = strings.lot3.selectedImageDesc,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(palette.scrim)
                                    .clickable { viewModel.onRemoveSelectedImage() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.lot3.removeImageDesc,
                                    tint = HubWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ---- Composeur .composer en verre rayon 26 ----
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                shape = RoundedCornerShape(26.dp),
                variant = GlassVariant.Card
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    // « + » accent — ouvre le sélecteur de photo.
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .testTag("chat_attach_image_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.lot4.attachPhotoDesc,
                            tint = HubAccent,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Champ pilule .cinput — placeholder contextuel + icônes photo.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(HubGlass)
                            .border(1.dp, HubStroke, RoundedCornerShape(percent = 50))
                            .padding(start = 15.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text = when {
                                        uiState.editingMessageId != null ->
                                            strings.lot4.editMessagePlaceholder
                                        uiState.replyingTo != null ->
                                            strings.lot4.replyPlaceholder
                                        else -> strings.lot4.chatInputPlaceholder
                                    },
                                    color = HubTextTertiary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
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
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(HubAccent),
                                maxLines = 4
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = HubAccent,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            // Icône 17 dp dans zone tactile 30 dp.
                        )
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = HubAccent,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        )
                    }

                    // Icône micro — élément décoratif de la maquette.
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = HubTextTertiary,
                        modifier = Modifier.size(20.dp)
                    )

                    // Bouton envoyer .sendbtn 42/15 dégradé.
                    val canSend = (
                        uiState.inputText.isNotBlank() ||
                            uiState.selectedImageBytes != null ||
                            uiState.editingMessageId != null
                        ) && !uiState.isSending
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = if (canSend) 12.dp else 0.dp,
                                shape = RoundedCornerShape(15.dp),
                                clip = false,
                                ambientColor = Color.Transparent,
                                spotColor = HubFabGlow
                            )
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                brush = if (canSend) {
                                    hubPrimaryGradient()
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            HubAccent.copy(alpha = 0.4f),
                                            HubAccent.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            )
                            .clickable(enabled = canSend) { viewModel.sendMessage() }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = strings.lot4.sendDesc,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                containerColor = HubCard,
                titleContentColor = HubWhite,
                textContentColor = HubTextSecondary,
                title = { Text(strings.lot4.deleteMessageTitle) },
                text = { Text(strings.lot4.deleteMessageBody) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessage(message.id)
                            messagePendingDeletion = null
                        }
                    ) {
                        Text(strings.lot4.deleteAction, color = HubError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messagePendingDeletion = null }) {
                        Text(strings.dialogCancel, color = HubWhite)
                    }
                }
            )
        }
    }
}

/** Élément de liste du fil : pastille de date ou message. */
private sealed interface ChatRowItem {
    val key: String

    data class DatePill(
        val label: String,
        override val key: String
    ) : ChatRowItem

    data class MessageItem(val message: Message) : ChatRowItem {
        override val key: String
            get() = message.id.ifEmpty { message.createdAt.seconds.toString() }
    }
}

private fun dayKeyOf(timestamp: com.google.firebase.Timestamp): Long {
    val calendar = java.util.Calendar.getInstance().apply { time = timestamp.toDate() }
    return calendar.get(java.util.Calendar.YEAR) * 1000L +
        calendar.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun dateLabelFor(timestamp: com.google.firebase.Timestamp, todayLabel: String): String {
    val date = timestamp.toDate()
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { time = date }
    val sameDay = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        todayLabel
    } else {
        SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(date.time))
    }
}

/** Bulle de saisie « en train d'écrire » : trois points .typing animés. */
@Composable
private fun TypingBubble() {
    GlassSurface(
        shape = RoundedCornerShape(
            topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 20.dp
        ),
        variant = GlassVariant.Card
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val transition = rememberInfiniteTransition(label = "typing_dot_$index")
                val bounce by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1200
                            0f at 0
                            -1f at 300
                            0f at 600
                            0f at 1200
                        },
                        initialStartOffset = StartOffset(index * 150)
                    ),
                    label = "typing_dot_offset_$index"
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .graphicsLayer { translationY = bounce * 4.dp.toPx() }
                        .clip(CircleShape)
                        .background(HubTextTertiary)
                )
            }
        }
    }
}

/**
 * Bulle de message : sortante en dégradé violet→bleu (lueur + reflet haut),
 * entrante en verre (.bubble.in) ; heure 9.5 + état d'envoi à l'intérieur ;
 * réactions en pilules .react superposées au bord bas de la bulle.
 */
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
    val strings = LocalHubStrings.current
    var menuExpanded by remember(message.id) { mutableStateOf(false) }

    val outShape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 8.dp
    )
    val inShape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 20.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box {
            if (isCurrentUser) {
                // .bubble.out — dégradé violet→bleu, texte blanc, lueur violette.
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = outShape,
                            clip = false,
                            ambientColor = Color.Transparent,
                            spotColor = HubFabGlow
                        )
                        .clip(outShape)
                        .background(brush = hubPrimaryGradient())
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { menuExpanded = true }
                        )
                        .testTag("message_bubble_" + message.id)
                ) {
                    // Reflet interne haut 1 dp rgba(255,255,255,.3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    BubbleContent(
                        message = message,
                        isCurrentUser = true,
                        onImageClick = onImageClick,
                        onPostClick = onPostClick,
                        showFooter = showTime || isLastSent,
                        footerTime = RelativeTime.formatTimeOnly(message.createdAt),
                        footerStatus = { OutBubbleStatus(message, isLastSent) },
                        editedLabel = if (message.editedAt != null && !message.isDeleted) {
                            strings.lot3.editedLabel
                        } else null
                    )
                }
            } else {
                // .bubble.in — verre (GlassSurface), rayon 20, coin bas-gauche 8.
                GlassSurface(
                    modifier = Modifier.widthIn(max = 300.dp),
                    shape = inShape,
                    variant = GlassVariant.Card
                ) {
                    Column(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { menuExpanded = true }
                            )
                            .testTag("message_bubble_" + message.id)
                    ) {
                        BubbleContent(
                            message = message,
                            isCurrentUser = false,
                            onImageClick = onImageClick,
                            onPostClick = onPostClick,
                            showFooter = showTime,
                            footerTime = RelativeTime.formatTimeOnly(message.createdAt),
                            footerStatus = null,
                            editedLabel = if (message.editedAt != null && !message.isDeleted) {
                                strings.lot3.editedLabel
                            } else null
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                Text(
                    text = strings.lot4.reactPickerTitle,
                    color = HubTextTertiary,
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
                        text = { Text(strings.lot4.replyAction, color = HubWhite) },
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
                        text = { Text(strings.lot4.editAction, color = HubWhite) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.lot4.deleteAction, color = HubError) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
                if (message.status == Message.STATUS_FAILED) {
                    DropdownMenuItem(
                        text = { Text(strings.lot4.retryAction, color = HubWhite) },
                        onClick = {
                            menuExpanded = false
                            onRetry()
                        }
                    )
                }
            }
        }

        // Réactions .react — pilules de verre superposées au bord de la bulle.
        if (reactionList.isNotEmpty() && !message.isDeleted) {
            val grouped = reactionList.groupingBy { it.emoji }.eachCount()
            Row(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (emoji, count) ->
                    val mine = reactionList.any {
                        it.userId == currentUserId && it.emoji == emoji
                    }
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(percent = 50),
                                clip = false,
                                ambientColor = Color.Transparent,
                                spotColor = LocalHubColors.current.shadowColor
                            )
                            .clip(RoundedCornerShape(percent = 50))
                            .background(HubGlassStrong)
                            .border(1.dp, HubStroke, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            fontSize = 11.sp,
                            color = if (mine) HubAccent else HubWhite
                        )
                    }
                }
            }
        }
    }
}

/** Indicateur d'état d'envoi dans l'heure des bulles sortantes. */
@Composable
private fun OutBubbleStatus(message: Message, isLastSent: Boolean) {
    val strings = LocalHubStrings.current
    if (!isLastSent) return
    when (message.status) {
        Message.STATUS_PENDING -> Text(
            text = "…",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.65f)
        )
        Message.STATUS_FAILED -> Text(
            text = strings.lot4.failedStatusLabel,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f)
        )
        Message.STATUS_READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = strings.lot4.readStatusDesc,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp)
        )
        Message.STATUS_DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = strings.lot4.deliveredStatusDesc,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        else -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = strings.lot4.sentStatusDesc,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
        )
    }
}

/** Contenu de bulle : citation, média supprimé/image, texte, liens, heure. */
@Composable
private fun BubbleContent(
    message: Message,
    isCurrentUser: Boolean,
    onImageClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    showFooter: Boolean,
    footerTime: String,
    footerStatus: (@Composable () -> Unit)?,
    editedLabel: String?
) {
    val strings = LocalHubStrings.current
    val textColor = if (isCurrentUser) Color.White else HubWhite

    if (!message.replyToText.isNullOrBlank() || !message.replyToMessageId.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isCurrentUser) {
                        Color.Black.copy(alpha = 0.16f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                )
                .padding(8.dp)
        ) {
            Text(
                text = message.replyToText ?: strings.lot4.photoReferenceLabel,
                color = textColor.copy(alpha = 0.75f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (message.isDeleted) {
        Text(
            text = strings.lot4.deletedMessageLabel,
            color = textColor.copy(alpha = 0.55f),
            fontSize = 13.5.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    } else {
        if (!message.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = message.imageUrl,
                contentDescription = strings.lot3.postImageDesc,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onImageClick(message.imageUrl!!) },
                contentScale = ContentScale.Crop
            )
        }

        if (!message.text.isNullOrBlank()) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 13.5.sp,
                lineHeight = 19.6.sp,
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

    // .btime — heure 9.5 + état d'envoi, alignée à droite (sortant) / gauche (entrant).
    if (showFooter) {
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = footerTime,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCurrentUser) {
                    Color.White.copy(alpha = 0.65f)
                } else {
                    HubWhite.copy(alpha = 0.65f)
                }
            )
            if (editedLabel != null) {
                Text(
                    text = "· $editedLabel",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrentUser) {
                        Color.White.copy(alpha = 0.45f)
                    } else {
                        HubTextTertiary
                    }
                )
            }
            footerStatus?.invoke()
        }
    }
}

/** Carte « publication partagée » dans la conversation (lien Hub). */
@Composable
private fun SharedPostLinkCard(
    postId: String,
    isCurrentUser: Boolean,
    onPostClick: (String) -> Unit
) {
    val strings = LocalHubStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentUser) {
                    Color.Black.copy(alpha = 0.18f)
                } else {
                    HubGlassStrong
                }
            )
            .padding(12.dp)
    ) {
        Text(
            text = strings.lot4.sharedPostCardTitle,
            color = if (isCurrentUser) Color.White else HubWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.lot4.sharedPostCardDesc,
            color = if (isCurrentUser) Color.White.copy(alpha = 0.75f) else HubTextTertiary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.lot4.openPostCta,
            color = if (isCurrentUser) Color.White else HubAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onPostClick(postId) }
                .padding(vertical = 4.dp)
        )
    }
}

/**
 * Bouton icône .iconbtn de la maquette : 40 dp, rayon 14, fond --glass,
 * contour --stroke.
 */
@Composable
private fun GlassIconButton(
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
