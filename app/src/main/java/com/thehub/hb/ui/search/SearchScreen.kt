package com.thehub.hb.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.rememberLiveUser
import com.thehub.hb.ui.components.AppLogo
import com.thehub.hb.ui.components.GlassSurface
import com.thehub.hb.ui.components.GlassVariant
import com.thehub.hb.ui.components.HubDimens
import com.thehub.hb.ui.components.HubEmptyState
import com.thehub.hb.ui.components.HubSkeletonLine
import com.thehub.hb.ui.components.HubVerifiedUserName
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.theme.HubGlassStrong
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubStroke
import com.thehub.hb.ui.theme.HubStrokeSoft
import com.thehub.hb.ui.theme.HubTextSecondary
import com.thehub.hb.ui.theme.HubTextTertiary
import com.thehub.hb.ui.theme.HubWhite
import com.thehub.hb.ui.theme.LocalHubColors
import com.thehub.hb.ui.theme.LocalHubStrings
import com.thehub.hb.ui.theme.hubPrimaryGradient

/** Filtre de présentation de la maquette (✦ Tout / Personnes / Publications / Hashtags). */
private enum class SearchFilter(val title: String) {
    ALL("all"),
    PEOPLE("people"),
    POSTS("posts"),
    HASHTAGS("hashtags")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: ((String) -> Unit)? = null,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenShare: (Post) -> Unit,
    onUserClick: ((User) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val focusManager = LocalFocusManager.current
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    var inputFocused by remember { mutableStateOf(false) }

    // Retour : efface d'abord la requête avant de quitter l'onglet.
    BackHandler(enabled = uiState.query.isNotBlank()) {
        viewModel.onQueryChanged("")
        focusManager.clearFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // .appbar — logo 32 + titre « Recherche ».
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogo(size = 32.dp, animated = false)
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.lot5.searchTitle,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = HubWhite
            )
        }

        // .input — champ de recherche, halo accent au focus.
        SearchInputField(
            query = uiState.query,
            focused = inputFocused,
            onQueryChanged = { viewModel.onQueryChanged(it) },
            onClearQuery = {
                viewModel.onQueryChanged("")
                focusManager.clearFocus()
            },
            onSearch = {
                if (uiState.query.isNotBlank()) {
                    focusManager.clearFocus()
                    viewModel.performSearch(uiState.query, uiState.searchMode)
                }
            },
            onFocusChanged = { inputFocused = it },
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 10.dp)
                .testTag("search_input_field")
        )

        // .chips — Tout / Personnes / Publications / Hashtags.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchFilterChip(
                text = strings.lot5.searchChipAll,
                selected = filter == SearchFilter.ALL,
                onClick = {
                    filter = SearchFilter.ALL
                    if (uiState.searchMode != SearchMode.USERS) {
                        viewModel.onSearchModeSelected(SearchMode.USERS)
                    }
                },
                modifier = Modifier.testTag("search_tab_all")
            )
            SearchFilterChip(
                text = strings.lot5.searchChipPeople,
                selected = filter == SearchFilter.PEOPLE,
                onClick = {
                    filter = SearchFilter.PEOPLE
                    viewModel.onSearchModeSelected(SearchMode.USERS)
                },
                modifier = Modifier.testTag("search_tab_users")
            )
            SearchFilterChip(
                text = strings.lot5.searchChipPosts,
                selected = filter == SearchFilter.POSTS,
                onClick = {
                    filter = SearchFilter.POSTS
                    viewModel.onSearchModeSelected(SearchMode.POSTS)
                },
                modifier = Modifier.testTag("search_tab_posts")
            )
            SearchFilterChip(
                text = strings.lot5.searchChipHashtags,
                selected = filter == SearchFilter.HASHTAGS,
                onClick = {
                    filter = SearchFilter.HASHTAGS
                    viewModel.onSearchModeSelected(SearchMode.POSTS)
                },
                modifier = Modifier.testTag("search_tab_hashtags")
            )
        }

        when {
            // Requête vide → découverte (tendances, historique, suggestions).
            uiState.query.isBlank() -> BlankSearchContent(
                viewModel = viewModel,
                uiState = uiState,
                searchHistory = searchHistory,
                onUserClick = onUserClick,
                onPostClick = onPostClick
            )

            uiState.isLoading -> SearchLoadingState()

            uiState.errorMessage != null -> SearchErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = {
                    viewModel.performSearch(
                        uiState.query,
                        uiState.searchMode,
                        saveToHistory = false
                    )
                }
            )

            filter == SearchFilter.POSTS || filter == SearchFilter.HASHTAGS -> {
                if (uiState.posts.isEmpty()) {
                    SearchNoResultState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_posts_list")
                    ) {
                        items(uiState.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onPostClick = onPostClick,
                                onImageClick = onImageClick,
                                onVideoClick = onVideoClick,
                                onToggleLike = { viewModel.toggleLike(it) },
                                onOpenComments = onOpenComments,
                                onOpenLikes = onOpenLikes,
                                onOpenShare = onOpenShare,
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onAuthorClick = { authorId ->
                                    uiState.users.firstOrNull { it.uid == authorId }
                                        ?.let { onUserClick?.invoke(it) }
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                // Tout / Personnes : tendances filtrées + personnes trouvées.
                if (uiState.users.isEmpty()) {
                    SearchNoResultState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 2.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_users_list")
                    ) {
                        val matchedTrends = uiState.trendingHashtags.filter { hashtag ->
                            hashtag.tag.contains(uiState.query.trim(), ignoreCase = true)
                        }
                        if (filter == SearchFilter.ALL && matchedTrends.isNotEmpty()) {
                            item {
                                TrendingHashtagsCard(
                                    hashtags = matchedTrends,
                                    onHashtagClick = { viewModel.onHashtagClicked(it) },
                                    modifier = Modifier.testTag("trending_hashtags_container")
                                )
                            }
                        }
                        item {
                            SectionHeader(
                                title = strings.lot5.searchPeopleSectionFormat
                                    .format(uiState.users.size),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        item {
                            GlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                variant = GlassVariant.Card,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    uiState.users.forEachIndexed { index, user ->
                                        SuggestedUserRow(
                                            user = user,
                                            isFollowing =
                                                uiState.followingIds.contains(user.uid) ||
                                                    uiState.suggestedFollowingIds.contains(user.uid),
                                            isSelf = user.uid == viewModel.currentUserId,
                                            onUserClick = { onUserClick?.invoke(user) },
                                            onToggleFollow = { viewModel.toggleFollow(user.uid) },
                                            showTopDivider = index > 0
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

/** Champ de recherche .input — icône accent, halo accent au focus, effacement. */
@Composable
private fun SearchInputField(
    query: String,
    focused: Boolean,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalHubColors.current
    val strings = LocalHubStrings.current
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.field, shape)
            .border(
                width = 1.dp,
                brush = if (focused || query.isNotBlank()) {
                    SolidColor(palette.accent.copy(alpha = 0.55f))
                } else {
                    SolidColor(palette.strokeSoft)
                },
                shape = shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(17.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubWhite
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) }
            )
            if (query.isEmpty()) {
                Text(
                    text = strings.searchPlaceholder,
                    fontSize = 14.sp,
                    color = HubTextTertiary,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = strings.lot4.searchClearDesc,
                tint = HubTextTertiary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClearQuery)
                    .testTag("clear_search_button")
            )
        }
    }
}

/** Chip .chip de la maquette — pilule, sélection = dégradé de marque. */
@Composable
private fun SearchFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) HubWhite else HubTextSecondary,
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(brush = hubPrimaryGradient(), shape = shape)
                } else {
                    Modifier
                        .background(HubGlassStrong.copy(alpha = 0.55f), shape)
                        .border(1.dp, HubStrokeSoft, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/** Contenu « découverte » : tendances, historique, personnes suggérées, publications. */
@Composable
private fun BlankSearchContent(
    viewModel: SearchViewModel,
    uiState: SearchUiState,
    searchHistory: List<String>,
    onUserClick: ((User) -> Unit)?,
    onPostClick: (String) -> Unit
) {
    val strings = LocalHubStrings.current
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // — Carte tendances (verre, flamme warning).
        item {
            if (uiState.trendingError != null && uiState.trendingHashtags.isEmpty()) {
                GlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    variant = GlassVariant.Card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HubEmptyState(
                        title = strings.lot5.searchNoResultTitle,
                        description = uiState.trendingError.orEmpty(),
                        icon = Icons.Outlined.Whatshot,
                        actionLabel = strings.lot3.retryLabel,
                        onAction = { viewModel.loadTrending() },
                        modifier = Modifier.testTag("search_error_state")
                    )
                }
            } else if (uiState.isTrendingLoading && uiState.trendingHashtags.isEmpty()) {
                GlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    variant = GlassVariant.Card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        repeat(4) {
                            HubSkeletonLine(height = 13.dp, widthFraction = 0.75f)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            } else if (uiState.trendingHashtags.isNotEmpty()) {
                TrendingHashtagsCard(
                    hashtags = uiState.trendingHashtags,
                    onHashtagClick = { viewModel.onHashtagClicked(it) },
                    modifier = Modifier.testTag("trending_section")
                )
            }
        }

        // — Historique de recherche (verre, rangées icône horloge).
        if (searchHistory.isNotEmpty()) {
            item {
                GlassSurface(
                    shape = RoundedCornerShape(22.dp),
                    variant = GlassVariant.Card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_history_section")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.lot5.searchHistoryTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = HubTextTertiary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = strings.lot5.searchHistoryClearDesc,
                                tint = HubTextTertiary,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.onClearAllHistory() }
                                    .padding(6.dp)
                                    .testTag("clear_all_history_button")
                            )
                        }
                        searchHistory.forEach { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onHistoryItemClicked(historyItem) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = HubTextTertiary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = historyItem,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HubWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = strings.lot5.searchHistoryRemoveDesc,
                                    tint = HubTextTertiary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.onRemoveHistoryItem(historyItem) }
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // — Personnes suggérées (verre, rangées avatar + Suivre).
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = strings.lot5.searchPeopleSectionFormat
                        .format(uiState.suggestedUsers.size),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Spacer(Modifier.height(6.dp))
                when {
                    uiState.isSuggestionsLoading && uiState.suggestedUsers.isEmpty() -> {
                        GlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            variant = GlassVariant.Card,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                repeat(3) {
                                    HubSkeletonLine(height = 13.dp, widthFraction = 0.6f)
                                    Spacer(Modifier.height(14.dp))
                                }
                            }
                        }
                    }

                    uiState.suggestionsError != null && uiState.suggestedUsers.isEmpty() -> {
                        GlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            variant = GlassVariant.Card,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("refresh_suggestions_button")
                        ) {
                            HubEmptyState(
                                title = strings.lot5.searchNoResultTitle,
                                description = uiState.suggestionsError.orEmpty(),
                                actionLabel = strings.lot3.retryLabel,
                                onAction = { viewModel.loadSuggestions() }
                            )
                        }
                    }

                    uiState.suggestedUsers.isNotEmpty() -> {
                        GlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            variant = GlassVariant.Card,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggested_users_container")
                        ) {
                            Column {
                                uiState.suggestedUsers.forEachIndexed { index, user ->
                                    SuggestedUserRow(
                                        user = user,
                                        isFollowing = uiState.suggestedFollowingIds
                                            .contains(user.uid),
                                        isSelf = user.uid == viewModel.currentUserId,
                                        onUserClick = { onUserClick?.invoke(user) },
                                        onToggleFollow = { viewModel.toggleFollow(user.uid) },
                                        showTopDivider = index > 0
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // — Publications tendance (cartes compactes verre doux).
        if (uiState.trendingPosts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = strings.lot5.searchPostsSectionFormat
                        .format(uiState.trendingPosts.size),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            items(
                uiState.trendingPosts,
                key = { "trend_${it.id}" }
            ) { post ->
                TrendingPostMiniCard(
                    post = post,
                    onClick = { onPostClick(post.id) },
                    modifier = Modifier.testTag("empty_trending_posts")
                )
            }
        }

        // Fond de liste : laisse respirer la nébuleuse.
        item {
            Spacer(Modifier.height(4.dp))
        }
    }
}

/** Carte .glass des tendances : flamme warning + rangées hashtag avec compteur. */
@Composable
private fun TrendingHashtagsCard(
    hashtags: List<com.thehub.hb.data.model.TrendingHashtag>,
    onHashtagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    GlassSurface(
        shape = RoundedCornerShape(22.dp),
        variant = GlassVariant.Card,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Whatshot,
                    contentDescription = strings.lot5.searchTrendingIconDesc,
                    tint = palette.warning,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.lot5.searchTrendingTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = HubTextTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            hashtags.forEach { hashtag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onHashtagClick(hashtag.tag) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("trending_hashtag_${hashtag.tag}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(palette.warning.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                palette.warning.copy(alpha = 0.28f),
                                RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Whatshot,
                            contentDescription = null,
                            tint = palette.warning,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hashtag.tag.startsWith("#")) hashtag.tag else "#${hashtag.tag}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubWhite
                        )
                        Text(
                            text = strings.lot5.searchTrendingCountFormat
                                .format(hashtag.count),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = HubTextTertiary
                        )
                    }
                }
            }
        }
    }
}

/** Titre de section .section-title — 12 / 800 / interlettre 1.2 / uppercase. */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = HubTextTertiary,
        modifier = modifier
    )
}

/** Rangée personne de la maquette : avatar 42, nom vérifié, bouton verre xs. */
@Composable
private fun SuggestedUserRow(
    user: User,
    isFollowing: Boolean,
    isSelf: Boolean,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit,
    showTopDivider: Boolean
) {
    val strings = LocalHubStrings.current
    val liveUser = rememberLiveUser(
        userId = user.uid,
        fallbackUsername = user.username,
        fallbackDisplayName = user.displayName,
        fallbackPhotoUrl = user.photoUrl
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(HubStrokeSoft)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onUserClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("search_user_item_${user.uid}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(
                name = liveUser.effectiveName,
                photoUrl = liveUser.photoUrl,
                size = 42.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                HubVerifiedUserName(
                    name = liveUser.effectiveName,
                    isVerified = liveUser.isVerified,
                    verificationType = liveUser.verificationType,
                    fontSize = 13.5.sp,
                    color = HubWhite,
                    badgeSize = 14.dp
                )
                Text(
                    text = strings.lot5.followListSubFollowersFormat
                        .format(user.followersCount),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = HubTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isSelf) {
                SearchFollowButton(
                    text = if (isFollowing) {
                        strings.lot5.searchFollowedBtn
                    } else {
                        strings.follow
                    },
                    isFollowing = isFollowing,
                    onClick = onToggleFollow,
                    modifier = Modifier.testTag("follow_button_${user.uid}")
                )
            }
        }
    }
}

/** Bouton xs de la maquette : verre (« Abonné ») ou dégradé (« Suivre »). */
@Composable
private fun SearchFollowButton(
    text: String,
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isFollowing) {
                    Modifier
                        .background(HubGlassStrong, shape)
                        .border(1.dp, HubStroke, shape)
                } else {
                    Modifier.background(brush = hubPrimaryGradient(), shape = shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )
    }
}

/** Mini-carte publication .glass-soft — avatar 30, texte 12/1.5. */
@Composable
private fun TrendingPostMiniCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalHubStrings.current
    val palette = LocalHubColors.current
    val author = rememberLiveUser(userId = post.authorId)
    val timeLabel = remember(post.createdAt) {
        val format = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
        format.format(post.createdAt.toDate())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.glassSoft)
            .border(1.dp, HubStrokeSoft, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                UserAvatar(
                    name = author.effectiveName,
                    photoUrl = author.photoUrl,
                    size = 30.dp
                )
                Text(
                    text = author.effectiveName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = timeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubTextTertiary
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = post.text,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = HubTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (post.imageUrl != null) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = strings.lot3.postImageDesc,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent
                )
            }
        }
    }
}

/** État « chargement » : squelettes verre. */
@Composable
private fun SearchLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            GlassSurface(
                shape = RoundedCornerShape(22.dp),
                variant = GlassVariant.Card,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_loading_indicator")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    HubSkeletonLine(height = 13.dp, widthFraction = 0.55f)
                    Spacer(Modifier.height(10.dp))
                    HubSkeletonLine(height = 13.dp, widthFraction = 0.8f)
                    Spacer(Modifier.height(10.dp))
                    HubSkeletonLine(height = 13.dp, widthFraction = 0.4f)
                }
            }
        }
    }
}

/** État « erreur » : HubEmptyState + Réessayer. */
@Composable
private fun SearchErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("search_error_state"),
        contentAlignment = Alignment.Center
    ) {
        HubEmptyState(
            title = strings.lot3.oopsLabel,
            description = message,
            icon = Icons.Outlined.Search,
            actionLabel = strings.lot3.retryLabel,
            onAction = onRetry
        )
    }
}

/** État « aucun résultat » : HubEmptyState + description maquette. */
@Composable
private fun SearchNoResultState() {
    val strings = LocalHubStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("empty_search_results"),
        contentAlignment = Alignment.Center
    ) {
        HubEmptyState(
            title = strings.lot5.searchNoResultTitle,
            description = strings.lot5.searchNoResultDesc,
            icon = Icons.Outlined.Search
        )
    }
}
