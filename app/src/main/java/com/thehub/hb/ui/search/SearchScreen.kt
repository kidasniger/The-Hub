package com.thehub.hb.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.thehub.hb.ui.components.UserAvatar
import com.thehub.hb.ui.feed.components.PostCard
import com.thehub.hb.ui.theme.HubBlack
import com.thehub.hb.ui.theme.HubBorder
import com.thehub.hb.ui.theme.HubCard
import com.thehub.hb.ui.theme.HubDarkGray
import com.thehub.hb.ui.theme.HubMuted
import com.thehub.hb.ui.theme.HubSecondary
import com.thehub.hb.ui.theme.HubSurfaceDark
import com.thehub.hb.ui.theme.HubSurfaceElevated
import com.thehub.hb.ui.theme.HubWhite

enum class BlankSearchTab(val title: String) {
    TRENDING("Tendances"),
    HISTORY("Historique")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenShare: (Post) -> Unit,
    onUserClick: ((User) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val focusManager = LocalFocusManager.current
    var blankTab by remember { mutableStateOf(BlankSearchTab.TRENDING) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HubBlack)
            .statusBarsPadding()
            .testTag("search_screen")
    ) {
        // Search Input Bar
        SearchBarHeader(
            query = uiState.query,
            onQueryChanged = { viewModel.onQueryChanged(it) },
            onClearQuery = {
                viewModel.onQueryChanged("")
                focusManager.clearFocus()
            },
            onSearchSubmit = {
                focusManager.clearFocus()
                viewModel.performSearch(uiState.query, uiState.searchMode, saveToHistory = true)
            }
        )

        // Mode Selector Tabs:
        // When query is empty -> show Tendances / Historique
        // When query has text -> show Utilisateurs / Publications
        if (uiState.query.isBlank()) {
            BlankSearchTabs(
                selectedTab = blankTab,
                onTabSelected = { blankTab = it }
            )
        } else {
            SearchModeTabs(
                selectedMode = uiState.searchMode,
                onModeSelected = { viewModel.onSearchModeSelected(it) }
            )
        }

        HorizontalDivider(color = HubBorder, thickness = 1.dp)

        // Content Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_loading_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                uiState.query.isBlank() -> {
                    when (blankTab) {
                        BlankSearchTab.TRENDING -> {
                            TrendingSection(
                                uiState = uiState,
                                onHashtagClick = { tag ->
                                    viewModel.onHashtagClicked(tag)
                                    focusManager.clearFocus()
                                },
                                onPostClick = onPostClick,
                                onImageClick = onImageClick,
                                onToggleLike = { viewModel.toggleLike(it) },
                                onOpenComments = onOpenComments,
                                onOpenLikes = onOpenLikes,
                                onOpenShare = onOpenShare,
                                onToggleBookmark = { viewModel.toggleBookmark(it) },
                                onRefresh = { viewModel.loadTrending() }
                            )
                        }

                        BlankSearchTab.HISTORY -> {
                            SearchHistorySection(
                                history = searchHistory,
                                onHistoryItemClick = { item ->
                                    viewModel.onHistoryItemClicked(item)
                                    focusManager.clearFocus()
                                },
                                onRemoveItem = { viewModel.onRemoveHistoryItem(it) },
                                onClearAll = { viewModel.onClearAllHistory() }
                            )
                        }
                    }
                }

                uiState.searchMode == SearchMode.USERS -> {
                    if (uiState.users.isEmpty() && uiState.hasSearched) {
                        EmptySearchResults(
                            query = uiState.query,
                            message = "Aucun utilisateur trouvé pour \"${uiState.query}\""
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_users_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.users,
                                key = { it.uid }
                            ) { user ->
                                val isFollowing = uiState.followingIds.contains(user.uid)
                                val isSelf = user.uid == viewModel.currentUserId

                                UserSearchResultRow(
                                    user = user,
                                    isFollowing = isFollowing,
                                    isSelf = isSelf,
                                    onToggleFollow = { viewModel.toggleFollow(user.uid) },
                                    onClick = { onUserClick?.invoke(user) }
                                )
                            }
                        }
                    }
                }

                uiState.searchMode == SearchMode.POSTS -> {
                    if (uiState.posts.isEmpty() && uiState.hasSearched) {
                        EmptySearchResults(
                            query = uiState.query,
                            message = "Aucune publication trouvée pour \"${uiState.query}\""
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_posts_list"),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = uiState.posts,
                                key = { it.id }
                            ) { post ->
                                PostCard(
                                    post = post,
                                    onPostClick = onPostClick,
                                    onImageClick = onImageClick,
                                    onToggleLike = { viewModel.toggleLike(post.id) },
                                    onOpenComments = onOpenComments,
                                    onOpenLikes = onOpenLikes,
                                    onOpenShare = onOpenShare,
                                    onToggleBookmark = { viewModel.toggleBookmark(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBarHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearchSubmit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HubSurfaceDark)
                .border(1.dp, HubBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Recherche",
                tint = HubSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field"),
                textStyle = TextStyle(
                    color = HubWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                cursorBrush = SolidColor(HubWhite),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Rechercher sur The Hub...",
                            color = HubSecondary,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Effacer la recherche",
                        tint = HubSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchModeTabs(
    selectedMode: SearchMode,
    onModeSelected: (SearchMode) -> Unit
) {
    val modes = SearchMode.entries
    val selectedIndex = modes.indexOf(selectedMode)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = HubBlack,
        contentColor = HubWhite,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = HubWhite,
                height = 2.dp
            )
        },
        divider = {}
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selectedMode
            Tab(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                text = {
                    Text(
                        text = mode.title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HubWhite else HubSecondary
                    )
                },
                modifier = Modifier.testTag("search_tab_${mode.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit
) {
    if (history.isEmpty()) {
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
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Explorer The Hub",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Recherchez des créateurs par nom ou pseudo, ou trouvez des publications par mot-clé.",
                    fontSize = 13.sp,
                    color = HubSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("search_history_section")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = HubSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recherches récentes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubWhite
                    )
                }

                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_all_history_button")
                ) {
                    Text(
                        text = "Tout effacer",
                        fontSize = 12.sp,
                        color = HubSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { queryItem ->
                    SearchHistoryChip(
                        text = queryItem,
                        onClick = { onHistoryItemClick(queryItem) },
                        onDelete = { onRemoveItem(queryItem) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HubSurfaceDark)
            .border(1.dp, HubBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp)
            .testTag("history_chip_$text"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = HubWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Supprimer $text",
                tint = HubSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun UserSearchResultRow(
    user: User,
    isFollowing: Boolean,
    isSelf: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HubCard)
            .border(1.dp, HubBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("search_user_item_${user.uid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = user.displayName ?: user.username,
            photoUrl = user.photoUrl,
            size = 46.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.username,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HubWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "@${user.username}",
                fontSize = 13.sp,
                color = HubSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.bio,
                    fontSize = 12.sp,
                    color = HubMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!isSelf) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isFollowing) HubSurfaceElevated else HubWhite)
                    .border(1.dp, if (isFollowing) HubBorder else HubWhite, RoundedCornerShape(20.dp))
                    .clickable(onClick = onToggleFollow)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("follow_button_${user.uid}")
            ) {
                Text(
                    text = if (isFollowing) "Abonné" else "Suivre",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFollowing) HubWhite else HubBlack
                )
            }
        }
    }
}

@Composable
private fun EmptySearchResults(
    query: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("empty_search_results"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HubSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = HubSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HubSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BlankSearchTabs(
    selectedTab: BlankSearchTab,
    onTabSelected: (BlankSearchTab) -> Unit
) {
    val tabs = BlankSearchTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = HubBlack,
        contentColor = HubWhite,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = HubWhite,
                height = 2.dp
            )
        },
        divider = {}
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HubWhite else HubSecondary
                    )
                },
                modifier = Modifier.testTag("blank_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendingSection(
    uiState: SearchUiState,
    onHashtagClick: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenLikes: (String) -> Unit,
    onOpenShare: (Post) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("trending_section"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Hashtags Populaires (Top 10)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HubSurfaceDark)
                    .border(1.dp, HubBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .testTag("trending_hashtags_container")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = HubWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hashtags populaires",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HubWhite
                            )
                        }
                        Text(
                            text = "Les 10 plus fréquents ces 7 derniers jours",
                            fontSize = 12.sp,
                            color = HubSecondary
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("refresh_trending_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser les tendances",
                            tint = HubSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.isTrendingLoading && uiState.trendingHashtags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (uiState.trendingHashtags.isEmpty()) {
                    Text(
                        text = "Aucun hashtag récent pour le moment.",
                        fontSize = 13.sp,
                        color = HubMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.trendingHashtags.forEach { item ->
                            HashtagChip(
                                hashtag = item,
                                onClick = { onHashtagClick(item.tag) }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Header for Publications Tendance
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Publications tendance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HubWhite
                )
                Text(
                    text = "Les publications les plus populaires des 7 derniers jours",
                    fontSize = 12.sp,
                    color = HubSecondary
                )
            }
        }

        // Section 3: Trending Posts List
        if (uiState.isTrendingLoading && uiState.trendingPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HubWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else if (uiState.trendingPosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(HubSurfaceDark)
                        .padding(24.dp)
                        .testTag("empty_trending_posts"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune publication populaire ces 7 derniers jours.",
                        fontSize = 14.sp,
                        color = HubSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = uiState.trendingPosts,
                key = { "trending_${it.id}" }
            ) { post ->
                PostCard(
                    post = post,
                    onPostClick = onPostClick,
                    onImageClick = onImageClick,
                    onToggleLike = { onToggleLike(post.id) },
                    onOpenComments = onOpenComments,
                    onOpenLikes = onOpenLikes,
                    onOpenShare = onOpenShare,
                    onToggleBookmark = { onToggleBookmark(post.id) }
                )
            }
        }
    }
}

@Composable
private fun HashtagChip(
    hashtag: com.thehub.hb.data.model.TrendingHashtag,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(HubCard)
            .border(1.dp, HubBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("trending_hashtag_${hashtag.tag}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${hashtag.tag}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = HubWhite
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${hashtag.count}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = HubSecondary
        )
    }
}
