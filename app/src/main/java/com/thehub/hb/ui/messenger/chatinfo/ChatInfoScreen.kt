package com.thehub.hb.ui.messenger.chatinfo

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubButton
import com.thehub.hb.ui.components.HubErrorBanner
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubError
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassSoft
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubSuccess
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient
import com.thehub.hb.utils.RelativeTime

/**
 * Écran 14 « Infos de la conversation » — maquette Aurora Glass.
 *
 * En-tête centré : avatar 88, nom 17/800 + badge vérifié, @handle, pilule
 * verte « Conversations chiffrées » ; rangée de quatre tuiles .glass-soft
 * r18 (Profil, Rechercher, Muet, Bloquer en danger) ; recherche dans la
 * conversation (carte de verre) ; « Médias partagés » (mosaïque d'art
 * décoratif 74/14) ; carte de réglages r20 (notifications, messages
 * éphémères, téléchargement auto) ; bouton danger pleine largeur
 * « Supprimer la conversation ». États : chargement squelette, bandeau
 * bloqué, dialogues et snackbar conservés.
 */
@Composable
fun ChatInfoScreen(
    viewModel: ChatInfoViewModel,
    onBack: () -> Unit,
    onConversationDeleted: () -> Unit,
    onViewProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val strings = LocalHubStrings.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    // Interrupteurs de présentation (maquette) — sans effet sur les données.
    var conversationMuted by remember { mutableStateOf(false) }
    var ephemeralEnabled by remember { mutableStateOf(false) }

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
    val handle = contactUser?.username ?: otherInfo?.username ?: ""

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("chat_info_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- .appbar : retour + titre ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubGlass)
                        .border(1.dp, HubStroke, RoundedCornerShape(14.dp))
                        .clickable(onClick = onBack)
                        .testTag("chat_info_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = HubWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = strings.lot4.chatInfoTitle,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = HubWhite
                )
            }

            if (uiState.isLoading) {
                ChatInfoLoadingSkeleton()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // ---- En-tête profil ----
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        UserAvatar(
                            name = displayName,
                            photoUrl = photoUrl,
                            size = 88.dp
                        )
                        HubVerifiedUserName(
                            name = displayName,
                            isVerified = liveContact.isVerified,
                            verificationType = liveContact.verificationType,
                            fontSize = 17.sp,
                            color = HubWhite,
                            weight = FontWeight.ExtraBold,
                            badgeSize = 16.dp
                        )
                        if (handle.isNotBlank()) {
                            Text(
                                text = "@$handle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = HubTextTertiary
                            )
                        }
                        // .pill.ok — conversations chiffrées.
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(HubSuccess.copy(alpha = 0.15f))
                                .border(
                                    1.dp,
                                    HubSuccess.copy(alpha = 0.35f),
                                    RoundedCornerShape(percent = 50)
                                )
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = HubSuccess,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = strings.lot4.encryptedPill,
                                color = HubSuccess,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }

                    // ---- Rangée de tuiles d'action ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChatInfoActionTile(
                            icon = Icons.Outlined.Person,
                            label = strings.lot4.actionProfile,
                            description = strings.lot4.actionProfileDesc,
                            modifier = Modifier.weight(1f),
                            onClick = { onViewProfile(contactUid) },
                            testTag = "chat_info_profile_option"
                        )
                        ChatInfoActionTile(
                            icon = Icons.Outlined.Search,
                            label = strings.lot4.actionSearch,
                            description = strings.lot4.actionSearchDesc,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.toggleSearch(!uiState.isSearching) },
                            testTag = "chat_info_search_option"
                        )
                        ChatInfoActionTile(
                            icon = if (conversationMuted) {
                                Icons.Outlined.NotificationsOff
                            } else {
                                Icons.Outlined.Notifications
                            },
                            label = strings.lot4.actionMute,
                            description = strings.lot4.actionMuteDesc,
                            modifier = Modifier.weight(1f),
                            selected = conversationMuted,
                            onClick = { conversationMuted = !conversationMuted },
                            testTag = "chat_info_mute_option"
                        )
                        ChatInfoDangerTile(
                            icon = Icons.Outlined.Block,
                            label = strings.lot4.actionBlock,
                            description = strings.lot4.actionBlockDesc,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!uiState.isBlocked) {
                                    showBlockConfirmDialog = true
                                }
                            },
                            testTag = "chat_info_block_option"
                        )
                    }

                    // ---- Recherche dans la conversation ----
                    if (uiState.isSearching) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            variant = GlassVariant.Soft
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                HubTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                                    placeholder = strings.lot4.searchInConversationPlaceholder,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = HubTextTertiary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = strings.lot4.searchClearDesc,
                                                tint = HubTextTertiary,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clickable { viewModel.onSearchQueryChanged("") }
                                            )
                                        }
                                    } else null,
                                    minHeight = 42,
                                    testTag = "chat_info_search_input"
                                )
                                if (uiState.searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (uiState.searchResults.isEmpty()) {
                                        Text(
                                            text = strings.lot4.searchNoResult,
                                            color = HubTextTertiary,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        uiState.searchResults.take(10).forEach { msg ->
                                            SearchResultRow(
                                                message = msg,
                                                currentUserId = viewModel.currentUserId
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- Médias partagés (art décoratif de la maquette) ----
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.lot4.sharedMediaTitle.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = HubTextTertiary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MediaArtTile(
                                colors = listOf(
                                    Color(0xFF06283D), Color(0xFF0B4F6C), Color(0xFF013A5E)
                                ),
                                glowColor = Color(0xFF22D3EE),
                                modifier = Modifier.weight(1f)
                            )
                            MediaArtTile(
                                colors = listOf(
                                    Color(0xFF3B0D3F), Color(0xFF7A1F4F), Color(0xFF43124D)
                                ),
                                glowColor = Color(0xFFF472B6),
                                modifier = Modifier.weight(1f)
                            )
                            MediaArtTile(
                                colors = listOf(
                                    Color(0xFF0C3323), Color(0xFF116B4E), Color(0xFF0B4A3A)
                                ),
                                glowColor = Color(0xFF34D399),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // ---- Carte de réglages r20 ----
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        variant = GlassVariant.Card
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ChatInfoSettingsRow(
                                icon = if (conversationMuted) {
                                    Icons.Outlined.NotificationsOff
                                } else {
                                    Icons.Outlined.Notifications
                                },
                                title = strings.lot4.rowNotificationsTitle,
                                subtitle = if (conversationMuted) {
                                    strings.lot4.rowNotificationsOffSub
                                } else {
                                    strings.lot4.rowNotificationsOnSub
                                },
                                trailing = {
                                    HubSettingsToggle(
                                        selected = !conversationMuted,
                                        onToggle = { conversationMuted = !conversationMuted }
                                    )
                                }
                            )
                            ChatInfoDivider()
                            ChatInfoSettingsRow(
                                icon = Icons.Outlined.Schedule,
                                title = strings.lot4.rowEphemeralTitle,
                                subtitle = strings.lot4.rowEphemeralSub,
                                trailing = {
                                    HubSettingsToggle(
                                        selected = ephemeralEnabled,
                                        onToggle = { ephemeralEnabled = !ephemeralEnabled }
                                    )
                                }
                            )
                            ChatInfoDivider()
                            ChatInfoSettingsRow(
                                icon = Icons.Outlined.Image,
                                title = strings.lot4.rowAutoDownloadTitle,
                                subtitle = strings.lot4.rowAutoDownloadSub,
                                trailing = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = HubTextTertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }
                    }

                    // ---- Bandeau bloqué ----
                    if (uiState.isBlocked) {
                        HubErrorBanner(
                            message = strings.lot4.blockedUserBanner,
                            modifier = Modifier.testTag("chat_info_blocked_banner")
                        )
                    }

                    // ---- Supprimer la conversation ----
                    HubButton(
                        text = strings.lot4.deleteConversationCta,
                        onClick = { showDeleteConfirmDialog = true },
                        variant = com.thehub.hb.ui.components.HubButtonVariant.Danger,
                        isLoading = uiState.isDeleting,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = HubWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        testTag = "chat_info_delete_option"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // ---- Dialogues ----
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                containerColor = HubCard,
                titleContentColor = HubWhite,
                textContentColor = HubWhite.copy(alpha = 0.68f),
                title = {
                    Text(
                        text = strings.lot4.dialogDeleteConvTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(text = strings.lot4.dialogDeleteConvBody, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteConversation(onDeleted = onConversationDeleted)
                        }
                    ) {
                        Text(
                            text = strings.lot4.deleteAction,
                            color = HubError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(text = strings.dialogCancel, color = HubWhite)
                    }
                }
            )
        }

        if (showBlockConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBlockConfirmDialog = false },
                containerColor = HubCard,
                titleContentColor = HubWhite,
                textContentColor = HubWhite.copy(alpha = 0.68f),
                title = {
                    Text(
                        text = strings.lot4.dialogBlockTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = { Text(text = strings.lot4.dialogBlockBody, fontSize = 14.sp) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBlockConfirmDialog = false
                            viewModel.blockUser()
                        }
                    ) {
                        Text(
                            text = strings.lot4.blockCta,
                            color = HubError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockConfirmDialog = false }) {
                        Text(text = strings.dialogCancel, color = HubWhite)
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Tuile d'action .glass-soft r18 — icône 19 + libellé 10.5/700. */
@Composable
private fun ChatInfoActionTile(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    testTag: String = "chat_info_action_tile"
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HubGlassSoft)
            .border(1.dp, if (selected) HubAccent.copy(alpha = 0.35f) else HubStrokeSoft, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = HubAccent,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = HubAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Tuile d'action en danger (fond danger 10 %, contour danger 28 %). */
@Composable
private fun ChatInfoDangerTile(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "chat_info_danger_tile"
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HubError.copy(alpha = 0.10f))
            .border(1.dp, HubError.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = HubError,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = HubError,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rangée de réglages .row — ricon 38/r13, titre 13.5/700, sous-titre 11.5. */
@Composable
private fun ChatInfoSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(HubGlassSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HubAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = HubTextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}

/** Séparateur .divider entre les rangées de réglages. */
@Composable
private fun ChatInfoDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(HubStrokeSoft)
    )
}

/** Interrupteur .toggle 46×27 de la maquette (knob animé). */
@Composable
private fun HubSettingsToggle(
    selected: Boolean,
    onToggle: () -> Unit
) {
    val knobOffset by animateDpAsState(
        targetValue = if (selected) 21.dp else 3.dp,
        label = "hub_settings_toggle_knob"
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(27.dp)
            .shadow(
                elevation = if (selected) 6.dp else 0.dp,
                shape = RoundedCornerShape(percent = 50),
                clip = false,
                ambientColor = Color.Transparent,
                spotColor = HubFabGlow
            )
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) Color.Transparent else HubGlassSoft)
            .then(
                if (selected) {
                    Modifier.background(brush = hubPrimaryGradient())
                } else {
                    Modifier.border(1.dp, HubStroke, RoundedCornerShape(percent = 50))
                }
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = knobOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else HubTextTertiary)
        )
    }
}

/** Mosaïque « art média » décorative de la maquette (74 dp, r14). */
@Composable
private fun MediaArtTile(
    colors: List<Color>,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(colors)
            )
            .drawBehind {
                // Lueur radiale décorative (.media-art) + voile haut / bas.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.72f, size.height * 0.3f),
                        radius = size.minDimension * 0.75f
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.16f)
                        )
                    )
                )
            }
            .semantics { contentDescription = strings.lot4.sharedMediaArtDesc }
            .testTag("chat_info_media_art")
    )
}

/** Rangée de résultat de recherche dans la conversation. */
@Composable
private fun SearchResultRow(
    message: Message,
    currentUserId: String
) {
    val strings = LocalHubStrings.current
    val isMe = message.isSentBy(currentUserId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HubGlass)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) strings.lot4.searchYouLabel else strings.lot4.searchContactLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = HubTextTertiary
            )
            Text(
                text = message.text ?: "",
                fontSize = 13.sp,
                color = HubWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = RelativeTime.formatTimeOnly(message.createdAt),
            fontSize = 11.sp,
            color = HubTextTertiary
        )
    }
}

/** Chargement : squelette d'en-tête + tuiles + réglages. */
@Composable
private fun ChatInfoLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("chat_info_skeleton"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(HubGlassSoft)
        )
        Spacer(modifier = Modifier.height(16.dp))
        HubSkeletonLine(height = 16.dp, widthFraction = 0.5f)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HubGlassSoft)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HubSkeletonLine(height = 14.dp, widthFraction = 0.4f, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(16.dp))
        HubSkeletonLine(height = 120.dp, widthFraction = 1f)
    }
}
