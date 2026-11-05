package com.thehub.hb.ui.likeslist

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.LikerUser
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
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

@Composable
fun LikesListScreen(
    viewModel: LikesListViewModel,
    onNavigateBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("likes_list_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // .appbar — retour (verre), titre 16/800 + sous-titre « N personnes ».
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LikesGlassIcon(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("likes_list_back_button")
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
                    Text(
                        text = strings.likes,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = HubWhite
                    )
                    val subtitle = when (val state = uiState) {
                        is LikesListUiState.Success -> strings.lot3.likesPeopleFormat.format(state.likers.size)
                        else -> ""
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HubMuted
                        )
                    }
                }
            }

            // .seg — filtre « Tous / Abonnements » de la maquette.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    variant = GlassVariant.Soft
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        SegmentLabel(text = strings.lot3.filterAll, selected = true, modifier = Modifier.weight(1f))
                        SegmentLabel(text = strings.following, selected = false, modifier = Modifier.weight(1f))
                    }
                }
            }

            when (val state = uiState) {
                is LikesListUiState.Loading -> {
                    // État « chargement » : rangées squelettes.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        repeat(5) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(HubGlassStrong)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    HubSkeletonLine(height = 13.dp, widthFraction = 0.4f)
                                    Spacer(Modifier.height(6.dp))
                                    HubSkeletonLine(height = 11.dp, widthFraction = 0.25f)
                                }
                            }
                        }
                    }
                }

                is LikesListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HubEmptyState(
                            title = strings.lot3.oopsLabel,
                            description = state.message,
                            icon = Icons.Outlined.CloudOff,
                            actionLabel = strings.lot3.retryLabel,
                            onAction = { viewModel.loadLikers() }
                        )
                    }
                }

                is LikesListUiState.Success -> {
                    if (state.likers.isEmpty()) {
                        // État « vide » : cœur dans une carte de verre 76.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HubEmptyState(
                                title = strings.lot3.noLikesTitle,
                                description = strings.lot3.noLikesDesc,
                                icon = Icons.Outlined.FavoriteBorder
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = state.likers,
                                key = { it.uid }
                            ) { liker ->
                                LikerItemRow(
                                    liker = liker,
                                    onClick = {
                                        onUserClick(liker.uid)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Iconbtn verre 40/14 de l'appbar mentions J'aime. */
@Composable
private fun LikesGlassIcon(
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

/** Segment .seg — actif : verre renforcé + contour, inactif : transparent. */
@Composable
private fun SegmentLabel(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(HubGlassStrong, shape)
                        .border(1.dp, HubStroke, shape)
                } else {
                    Modifier
                }
            )
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) HubWhite else HubSecondary
        )
    }
}

@Composable
private fun LikerItemRow(
    liker: LikerUser,
    onClick: () -> Unit
) {
    val liveUser = rememberLiveUser(
        userId = liker.uid,
        fallbackUsername = liker.username,
        fallbackDisplayName = liker.displayName,
        fallbackPhotoUrl = liker.photoUrl
    )
    val strings = LocalHubStrings.current

    // Rangée .row de la maquette : 12 16, rayon 18, appui = verre doux.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = liveUser.effectiveName,
            photoUrl = liveUser.photoUrl,
            size = 42.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            HubVerifiedUserName(
                name = liveUser.effectiveName,
                isVerified = liveUser.isVerified,
                verificationType = liveUser.verificationType,
                fontSize = 13.5.sp,
                color = HubWhite,
                badgeSize = 15.dp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (liker.username.isNotBlank()) "@${liker.username}" else " ",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = HubMuted,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = strings.likes,
            tint = HubError,
            modifier = Modifier.size(16.dp)
        )
    }
}
