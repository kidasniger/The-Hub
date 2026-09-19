package com.thehub.hb.ui.messenger.chatinfo

import androidx.compose.foundation.background
import com.thehub.hb.ui.theme.HubSurface
import com.thehub.hb.ui.theme.HubOutline
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.thehub.hb.data.repository.rememberLiveUser
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Message
import com.thehub.hb.ui.components.HubTextField
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

@Composable
fun ChatInfoScreen(
    viewModel: ChatInfoViewModel,
    onBack: () -> Unit,
    onConversationDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    val otherInfo = uiState.conversation?.getOtherParticipantInfo(viewModel.currentUserId)
    val contactUser = uiState.contactUser
    val contactUid = contactUser?.uid ?: uiState.otherUserId

    val liveContact = rememberLiveUser(
        userId = contactUid,
        fallbackUsername = contactUser?.username ?: otherInfo?.username ?: "",
        fallbackDisplayName = contactUser?.displayName ?: otherInfo?.displayName,
        fallbackPhotoUrl = contactUser?.photoUrl ?: otherInfo?.photoUrl
    )

    val displayName = liveContact.effectiveName
    val photoUrl = liveContact.photoUrl
    val bio = contactUser?.bio

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("chat_info_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_info_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = HubWhite
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Détails",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = HubWhite)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Big Avatar
                    UserAvatar(
                        name = displayName,
                        photoUrl = photoUrl,
                        size = 96.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubWhite,
                        textAlign = TextAlign.Center
                    )



                    if (!bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(HubSurfaceElevated)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = bio,
                                fontSize = 14.sp,
                                color = HubWhite,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (uiState.isBlocked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HubError.copy(alpha = 0.2f))
                                .border(1.dp, HubError.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cet utilisateur est bloqué",
                                color = HubError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Options List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(HubSurfaceElevated)
                    ) {
                        // Option 1: Search in conversation
                        OptionRow(
                            icon = Icons.Default.Search,
                            title = "Rechercher dans la conversation",
                            onClick = { viewModel.toggleSearch(!uiState.isSearching) },
                            testTag = "chat_info_search_option"
                        )

                        if (uiState.isSearching) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                HubTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                                    placeholder = "Rechercher un mot...",
                                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                        {
                                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Effacer",
                                                    tint = HubMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null,
                                    minHeight = 42,
                                    testTag = "chat_info_search_input"
                                )

                                if (uiState.searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (uiState.searchResults.isEmpty()) {
                                        Text(
                                            text = "Aucun message trouvé.",
                                            color = HubMuted,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        uiState.searchResults.take(10).forEach { msg ->
                                            SearchResultRow(message = msg, currentUserId = viewModel.currentUserId)
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(HubBorder)
                        )

                        // Option 2: Block user
                        OptionRow(
                            icon = Icons.Default.Block,
                            title = if (uiState.isBlocked) "Utilisateur bloqué" else "Bloquer cet utilisateur",
                            titleColor = if (uiState.isBlocked) HubMuted else HubWhite,
                            iconTint = if (uiState.isBlocked) HubMuted else HubWhite,
                            onClick = {
                                if (!uiState.isBlocked) {
                                    showBlockConfirmDialog = true
                                }
                            },
                            testTag = "chat_info_block_option"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(HubBorder)
                        )

                        // Option 3: Delete conversation
                        OptionRow(
                            icon = Icons.Default.DeleteOutline,
                            title = "Supprimer la conversation",
                            titleColor = HubError,
                            iconTint = HubError,
                            onClick = { showDeleteConfirmDialog = true },
                            testTag = "chat_info_delete_option"
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Supprimer la conversation ?",
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                },
                text = {
                    Text(
                        text = "Tous les messages de cette conversation seront définitivement supprimés. Cette action est irréversible.",
                        color = HubMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteConversation(onDeleted = onConversationDeleted)
                        }
                    ) {
                        Text(
                            text = "Supprimer",
                            color = HubError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(text = "Annuler", color = HubWhite)
                    }
                },
                containerColor = HubCard,
                textContentColor = HubWhite,
                titleContentColor = HubWhite
            )
        }

        // Block confirmation dialog
        if (showBlockConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBlockConfirmDialog = false },
                title = {
                    Text(
                        text = "Bloquer cet utilisateur ?",
                        fontWeight = FontWeight.Bold,
                        color = HubWhite
                    )
                },
                text = {
                    Text(
                        text = "Vous ne recevrez plus de notifications de sa part et il sera ajouté à vos contacts bloqués.",
                        color = HubMuted,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBlockConfirmDialog = false
                            viewModel.blockUser()
                        }
                    ) {
                        Text(
                            text = "Bloquer",
                            color = HubError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockConfirmDialog = false }) {
                        Text(text = "Annuler", color = HubWhite)
                    }
                },
                containerColor = HubCard,
                textContentColor = HubWhite,
                titleContentColor = HubWhite
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = HubWhite,
    iconTint: androidx.compose.ui.graphics.Color = HubWhite,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor
        )
    }
}

@Composable
private fun SearchResultRow(
    message: Message,
    currentUserId: String
) {
    val isMe = message.isSentBy(currentUserId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(HubCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) "Vous :" else "Contact :",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = HubMuted
            )
            Text(
                text = message.text ?: "",
                fontSize = 13.sp,
                color = HubWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = RelativeTime.formatTimeOnly(message.createdAt),
            fontSize = 11.sp,
            color = HubMuted
        )
    }
}
