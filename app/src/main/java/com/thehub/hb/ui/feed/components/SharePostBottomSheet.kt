package com.thehub.hb.ui.feed.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.utils.buildHubPostShareLink
import com.thehub.hb.utils.buildHubPostShareMessage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Feuille de partage de la maquette (écran 06 — actions de publication).
 *
 * Robustesse : le flux Firestore `getConversations()` peut se terminer avec une
 * erreur (réseau indisponible, permissions) ; il est collecté ici avec `.catch`
 * afin qu'aucune exception ne remonte dans la composition (crash corrigé).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    post: Post,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onRepost: (Post) -> Unit,
    messageRepository: MessageRepository? = null
) {
    val context = LocalContext.current
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val repository = remember { messageRepository ?: MessageRepository() }
    // FIX CRASH : catch → la liste reste vide si le listener Firestore échoue.
    val conversations by remember(repository) {
        repository.getConversations().catch { emit(emptyList()) }
    }.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var showConversationPicker by remember { mutableStateOf(false) }
    var sendingConversationId by remember { mutableStateOf<String?>(null) }
    var shareError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.card,
        contentColor = HubWhite,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(HubStroke)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("share_post_bottom_sheet")
        ) {
            if (!showConversationPicker) {
                Text(
                    text = strings.lot5.shareSheetTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Option 1 : reposter sur mon profil.
                ShareOptionItem(
                    icon = Icons.Default.Repeat,
                    title = strings.lot5.shareOptionRepost,
                    subtitle = strings.lot5.shareOptionRepostSub,
                    enabled = true,
                    onClick = {
                        onRepost(post)
                        onDismiss()
                    },
                    testTag = "share_option_repost"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2 : envoyer dans The Hub.
                ShareOptionItem(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = strings.lot5.shareOptionMessage,
                    subtitle = strings.lot5.shareOptionMessageSub,
                    enabled = true,
                    onClick = {
                        shareError = null
                        showConversationPicker = true
                    },
                    testTag = "share_option_message"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3 : partager via une application installée.
                ShareOptionItem(
                    icon = Icons.AutoMirrored.Outlined.Send,
                    title = strings.lot5.shareOptionExternal,
                    subtitle = strings.lot5.shareOptionExternalSub,
                    enabled = true,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, buildHubPostShareLink(post.id))
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(shareIntent, strings.lot5.shareSheetTitle)
                            )
                            onDismiss()
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                strings.lot5.shareNoAppToast,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    testTag = "share_option_external"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 4 : copier le lien.
                ShareOptionItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = strings.lot5.shareOptionCopyLink,
                    subtitle = buildHubPostShareLink(post.id),
                    enabled = true,
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val link = buildHubPostShareLink(post.id)
                        val clip = ClipData.newPlainText(strings.lot5.shareClipLabel, link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            strings.lot5.shareCopiedToast,
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    },
                    testTag = "share_option_copy_link"
                )

                Spacer(modifier = Modifier.height(28.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showConversationPicker = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.lot5.sharePickerBackDesc,
                            tint = HubWhite
                        )
                    }
                    Text(
                        text = strings.lot5.sharePickerTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                }

                shareError?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = HubMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (conversations.isEmpty()) {
                    Text(
                        text = strings.lot5.shareNoConversations,
                        color = HubMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.55f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationShareRow(
                                conversation = conversation,
                                currentUserId = repository.currentUserId.orEmpty(),
                                isSending = sendingConversationId == conversation.id,
                                onClick = {
                                    if (sendingConversationId == null) {
                                        val recipientId = conversation.getOtherParticipantId(
                                            repository.currentUserId
                                        )
                                        if (recipientId.isBlank()) {
                                            shareError = strings.lot5.shareInvalidConversation
                                        } else {
                                            sendingConversationId = conversation.id
                                            shareError = null
                                            coroutineScope.launch {
                                                runCatching {
                                                    val permission =
                                                        repository.checkCanSendMessage(recipientId)
                                                    if (permission.isFailure) {
                                                        shareError = permission.exceptionOrNull()
                                                            ?.message
                                                            ?: strings.lot5.shareSendImpossible
                                                        sendingConversationId = null
                                                    } else {
                                                        repository.sendMessage(
                                                            conversationId = conversation.id,
                                                            text = buildHubPostShareMessage(post.id),
                                                            imageUrl = null
                                                        ).fold(
                                                            onSuccess = {
                                                                Toast.makeText(
                                                                    context,
                                                                    strings.lot5.shareSentToast,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                onDismiss()
                                                            },
                                                            onFailure = {
                                                                shareError = it.message
                                                                    ?: strings.lot5.shareSendFailed
                                                                sendingConversationId = null
                                                            }
                                                        )
                                                    }
                                                }.onFailure {
                                                    shareError = strings.lot5.shareSendFailed
                                                    sendingConversationId = null
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/** Option de la feuille : rangée verre, icône en pastille, titre + sous-titre. */
@Composable
private fun ShareOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HubGlassStrong)
            .border(1.dp, HubStrokeSoft, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(HubStrokeSoft.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HubWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = HubWhite
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = HubSecondary
            )
        }
    }
}

/** Rangée de conversation du sélecteur : avatar 44 + nom vérifié + aperçu. */
@Composable
private fun ConversationShareRow(
    conversation: Conversation,
    currentUserId: String,
    isSending: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalHubStrings.current
    val otherInfo = conversation.getOtherParticipantInfo(currentUserId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HubGlassStrong)
            .border(1.dp, HubStrokeSoft, RoundedCornerShape(14.dp))
            .clickable(enabled = !isSending, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = otherInfo.displayName ?: otherInfo.username,
            photoUrl = otherInfo.photoUrl,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            HubVerifiedUserName(
                name = otherInfo.displayName
                    ?: otherInfo.username.ifBlank { strings.lot5.shareRowFallbackName },
                isVerified = otherInfo.isVerified,
                verificationType = otherInfo.verificationType,
                fontSize = 15.sp,
                color = HubWhite,
                weight = FontWeight.SemiBold,
                badgeSize = 15.dp
            )
            Text(
                text = if (conversation.lastMessageText.isBlank()) {
                    strings.lot5.shareRowNoMessage
                } else {
                    conversation.lastMessageText
                },
                color = HubTextTertiary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (isSending) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = HubWhite,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = strings.lot5.shareRowSendDesc,
                tint = HubSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
