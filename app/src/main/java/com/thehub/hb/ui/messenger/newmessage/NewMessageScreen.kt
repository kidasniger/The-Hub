package com.thehub.hb.ui.messenger.newmessage

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.User
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubTextField
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.theme.HubAccent
import com.thehub.hb.ui.theme.HubAccent2
import com.thehub.hb.ui.theme.HubFabGlow
import com.thehub.hb.ui.theme.HubGlass
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

/**
 * Écran 12 « Nouveau message » — maquette Aurora Glass.
 *
 * App bar : croix + titre 16.5/800 + bouton « Discuter » (dégradé, désactivé
 * tant qu'aucun destinataire n'est choisi) ; champ « À : tapez un nom… » avec
 * pastille de compteur ; rangée « Destinataire : » avec chip.on supprimable ;
 * séparateur ; « Suggestions » ; rangées avatar 42 + @handle + interrupteur
 * 40×24. Chargement = squelettes, création = compteur, vide/erreur = états
 * du lot « États ».
 */
@Composable
fun NewMessageScreen(
    viewModel: NewMessageViewModel,
    onBack: () -> Unit,
    onConversationCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val strings = LocalHubStrings.current

    // Destinataire sélectionné (présentation : chip.on + interrupteur).
    var selectedUser by remember { mutableStateOf<User?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .testTag("new_message_screen")
    ) {
        // ---- .appbar : croix + titre + « Discuter » ----
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
                    .testTag("new_message_back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.back,
                    tint = HubWhite,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = strings.lot4.newMessageTitle,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = HubWhite,
                modifier = Modifier.weight(1f)
            )
            // Bouton principal .btn-primary.sm — désactivé : opacité .55.
            val canChat = selectedUser != null
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = if (canChat) 8.dp else 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false,
                        ambientColor = Color.Transparent,
                        spotColor = HubFabGlow
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(if (canChat) 1f else 0.55f)
                    .background(brush = hubPrimaryGradient())
                    .clickable(enabled = canChat) {
                        selectedUser?.let { user ->
                            viewModel.onUserSelected(user, onConversationCreated)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("new_message_chat_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.lot4.newMessageCta,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ---- Champ « À : tapez un nom… » + pastille de compteur ----
        Box(modifier = Modifier.padding(horizontal = 18.dp)) {
            HubTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = strings.lot4.newMessageToPlaceholder,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = HubTextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
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
                minHeight = 46,
                testTag = "user_search_input"
            )
            if (selectedUser != null) {
                // .pill.acc — compteur de destinataires (1).
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    HubAccent.copy(alpha = 0.22f),
                                    HubAccent2.copy(alpha = 0.22f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            HubAccent.copy(alpha = 0.35f),
                            RoundedCornerShape(percent = 50)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = strings.lot4.selectedRecipientsDesc.format(1),
                        tint = HubAccent,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "1",
                        color = HubAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ---- Rangée « Destinataire : » + chip.on ----
        selectedUser?.let { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.lot4.recipientLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubTextTertiary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(brush = hubPrimaryGradient())
                        .clickable { selectedUser = null }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.lot4.searchClearDesc,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // ---- Séparateur ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .height(1.dp)
                .background(HubStrokeSoft)
        )

        when (val state = uiState) {
            is NewMessageUiState.Loading -> {
                NewMessageLoadingSkeleton()
            }

            is NewMessageUiState.Creating -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = HubAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.lot4.openConversationLoading,
                            color = HubTextSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            is NewMessageUiState.Success -> {
                if (state.users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        HubEmptyState(
                            title = if (state.isSearch) {
                                strings.lot4.noUserFoundTitle
                            } else {
                                strings.lot4.noContactTitle
                            },
                            description = if (state.isSearch) {
                                strings.lot4.noUserFoundDesc
                            } else {
                                strings.lot4.noContactDesc
                            },
                            icon = Icons.Outlined.GroupAdd,
                            modifier = Modifier.testTag("new_message_empty_state")
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = strings.lot4.suggestionsTitle.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = HubTextTertiary,
                            modifier = Modifier.padding(start = 26.dp, top = 4.dp, bottom = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("user_search_results"),
                            contentPadding = PaddingValues(
                                top = 4.dp, bottom = 24.dp
                            )
                        ) {
                            items(
                                items = state.users,
                                key = { it.uid }
                            ) { user ->
                                UserSearchResultItem(
                                    user = user,
                                    isSelected = selectedUser?.uid == user.uid,
                                    onClick = {
                                        selectedUser = if (selectedUser?.uid == user.uid) {
                                            null
                                        } else {
                                            user
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            is NewMessageUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HubEmptyState(
                        title = strings.lot3.oopsLabel,
                        description = state.message,
                        icon = Icons.Outlined.Info,
                        actionLabel = strings.lot3.retryLabel,
                        onAction = { viewModel.loadInitialUsers() },
                        modifier = Modifier.testTag("new_message_error_state")
                    )
                }
            }

            is NewMessageUiState.Idle -> Unit
        }
    }
}

/** Rangée de résultat : avatar 42, nom, @handle, interrupteur 40×24. */
@Composable
private fun UserSearchResultItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val nameToDisplay = if (!user.displayName.isNullOrBlank()) {
        user.displayName
    } else {
        user.username
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .testTag("user_result_${user.uid}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UserAvatar(
            name = nameToDisplay,
            photoUrl = user.photoUrl,
            size = 42.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            HubVerifiedUserName(
                name = nameToDisplay,
                isVerified = user.isVerified,
                verificationType = user.verificationType,
                fontSize = 13.5.sp,
                color = HubWhite,
                weight = FontWeight.Bold,
                badgeSize = 15.dp
            )
            if (user.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${user.username}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = HubTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HubMiniToggle(selected = isSelected)
    }
}

/** Interrupteur .toggle.on 40×24 de la maquette (knob animé). */
@Composable
private fun HubMiniToggle(selected: Boolean) {
    val knobOffset by animateDpAsState(
        targetValue = if (selected) 19.dp else 3.dp,
        label = "hub_mini_toggle_knob"
    )
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) Color.Transparent else HubGlassStrong)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(percent = 50),
                        clip = false,
                        ambientColor = Color.Transparent,
                        spotColor = HubFabGlow
                    )
                } else {
                    Modifier.border(1.dp, HubStrokeSoft, RoundedCornerShape(percent = 50))
                }
            )
            .then(
                if (selected) {
                    Modifier.background(brush = hubPrimaryGradient())
                } else {
                    Modifier
                }
            )
            .testTag(if (selected) "toggle_on" else "toggle_off"),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = knobOffset)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else HubTextTertiary)
        )
    }
}

/** Chargement : squelettes de rangées (avatar + deux lignes .skel). */
@Composable
private fun NewMessageLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .testTag("new_message_skeleton")
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(HubGlassStrong)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    HubSkeletonLine(height = 13.dp, widthFraction = 0.42f)
                    Spacer(modifier = Modifier.height(7.dp))
                    HubSkeletonLine(height = 11.dp, widthFraction = 0.3f)
                }
            }
        }
    }
}
