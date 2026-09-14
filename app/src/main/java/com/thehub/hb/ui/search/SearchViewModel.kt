package com.thehub.hb.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.TrendingHashtag
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.NotificationRepository
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.SearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchMode(val title: String) {
    USERS("Utilisateurs"),
    POSTS("Publications")
}

data class SearchUiState(
    val query: String = "",
    val searchMode: SearchMode = SearchMode.USERS,
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val isTrendingLoading: Boolean = false,
    val trendingHashtags: List<TrendingHashtag> = emptyList(),
    val trendingPosts: List<Post> = emptyList(),
    val trendingError: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String?
        get() = searchRepository.currentUserId

    private val _queryDebounceFlow = MutableStateFlow("")

    init {
        loadTrending()
        viewModelScope.launch {
            _queryDebounceFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { text ->
                    if (text.isNotBlank()) {
                        performSearch(text, _uiState.value.searchMode)
                    } else {
                        _uiState.update {
                            it.copy(
                                users = emptyList(),
                                posts = emptyList(),
                                isLoading = false,
                                hasSearched = false,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrendingLoading = true, trendingError = null) }
            val result = postRepository.getTrendingData(days = 7)
            result.fold(
                onSuccess = { (hashtags, posts) ->
                    _uiState.update {
                        it.copy(
                            isTrendingLoading = false,
                            trendingHashtags = hashtags,
                            trendingPosts = posts,
                            trendingError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTrendingLoading = false,
                            trendingError = error.localizedMessage ?: "Erreur de chargement des tendances"
                        )
                    }
                }
            )
        }
    }

    fun onHashtagClicked(tag: String) {
        val formatted = if (tag.startsWith("#")) tag else "#$tag"
        _uiState.update { it.copy(query = formatted, searchMode = SearchMode.POSTS) }
        _queryDebounceFlow.value = formatted
        performSearch(formatted, SearchMode.POSTS, saveToHistory = true)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryDebounceFlow.value = newQuery
    }

    fun onSearchModeSelected(mode: SearchMode) {
        if (_uiState.value.searchMode == mode) return
        _uiState.update { it.copy(searchMode = mode) }
        val currentQuery = _uiState.value.query
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery, mode)
        }
    }

    fun onHistoryItemClicked(item: String) {
        _uiState.update { it.copy(query = item) }
        _queryDebounceFlow.value = item
        performSearch(item, _uiState.value.searchMode, saveToHistory = true)
    }

    fun onRemoveHistoryItem(item: String) {
        viewModelScope.launch {
            searchRepository.removeSearchQuery(item)
        }
    }

    fun onClearAllHistory() {
        viewModelScope.launch {
            searchRepository.clearSearchHistory()
        }
    }

    fun performSearch(query: String, mode: SearchMode, saveToHistory: Boolean = true) {
        val clean = query.trim()
        if (clean.isBlank()) return

        if (saveToHistory) {
            viewModelScope.launch {
                searchRepository.addSearchQuery(clean)
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearched = true) }

            when (mode) {
                SearchMode.USERS -> {
                    val result = searchRepository.searchUsers(clean)
                    result.fold(
                        onSuccess = { usersList ->
                            _uiState.update { it.copy(users = usersList, isLoading = false) }
                            // Fetch follow status for found users
                            checkFollowStatuses(usersList)
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.localizedMessage ?: "Erreur de recherche"
                                )
                            }
                        }
                    )
                }

                SearchMode.POSTS -> {
                    val result = if (clean.startsWith("#")) {
                        val tagResult = postRepository.getPostsByHashtag(clean)
                        if (tagResult.isSuccess && !tagResult.getOrNull().isNullOrEmpty()) {
                            tagResult
                        } else {
                            searchRepository.searchPosts(clean)
                        }
                    } else {
                        searchRepository.searchPosts(clean)
                    }

                    result.fold(
                        onSuccess = { postsList ->
                            _uiState.update { it.copy(posts = postsList, isLoading = false) }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.localizedMessage ?: "Erreur de recherche"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkFollowStatuses(users: List<User>) {
        val currentUid = currentUserId ?: return
        for (user in users) {
            if (user.uid == currentUid) continue
            viewModelScope.launch {
                notificationRepository.isFollowing(user.uid).collectLatest { isFollowing ->
                    _uiState.update { state ->
                        val updated = if (isFollowing) {
                            state.followingIds + user.uid
                        } else {
                            state.followingIds - user.uid
                        }
                        state.copy(followingIds = updated)
                    }
                }
            }
        }
    }

    fun toggleFollow(targetUid: String) {
        val isCurrentlyFollowing = _uiState.value.followingIds.contains(targetUid)
        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                _uiState.update { it.copy(followingIds = it.followingIds - targetUid) }
                val res = notificationRepository.unfollowUser(targetUid)
                if (res.isFailure) {
                    _uiState.update { it.copy(followingIds = it.followingIds + targetUid) }
                }
            } else {
                _uiState.update { it.copy(followingIds = it.followingIds + targetUid) }
                val res = notificationRepository.followUser(targetUid)
                if (res.isFailure) {
                    _uiState.update { it.copy(followingIds = it.followingIds - targetUid) }
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            // Optimistic update for both search results and trending posts
            _uiState.update { state ->
                val updatePost: (Post) -> Post = { p ->
                    if (p.id == postId) {
                        val newLiked = !p.isLikedByCurrentUser
                        val newCount = if (newLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                        p.copy(isLikedByCurrentUser = newLiked, likesCount = newCount)
                    } else p
                }
                state.copy(
                    posts = state.posts.map(updatePost),
                    trendingPosts = state.trendingPosts.map(updatePost)
                )
            }

            val result = postRepository.toggleLike(postId)
            if (result.isFailure) {
                // Revert on failure
                _uiState.update { state ->
                    val revertPost: (Post) -> Post = { p ->
                        if (p.id == postId) {
                            val originalLiked = !p.isLikedByCurrentUser
                            val originalCount = if (originalLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                            p.copy(isLikedByCurrentUser = originalLiked, likesCount = originalCount)
                        } else p
                    }
                    state.copy(
                        posts = state.posts.map(revertPost),
                        trendingPosts = state.trendingPosts.map(revertPost)
                    )
                }
            }
        }
    }

    fun toggleBookmark(postId: String) {
        viewModelScope.launch {
            // Optimistic bookmark update
            _uiState.update { state ->
                val updatePost: (Post) -> Post = { p ->
                    if (p.id == postId) {
                        p.copy(isBookmarkedByCurrentUser = !p.isBookmarkedByCurrentUser)
                    } else p
                }
                state.copy(
                    posts = state.posts.map(updatePost),
                    trendingPosts = state.trendingPosts.map(updatePost)
                )
            }

            val result = postRepository.toggleBookmark(postId)
            if (result.isFailure) {
                // Revert
                _uiState.update { state ->
                    val revertPost: (Post) -> Post = { p ->
                        if (p.id == postId) {
                            p.copy(isBookmarkedByCurrentUser = !p.isBookmarkedByCurrentUser)
                        } else p
                    }
                    state.copy(
                        posts = state.posts.map(revertPost),
                        trendingPosts = state.trendingPosts.map(revertPost)
                    )
                }
            }
        }
    }

    fun repost(post: Post, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = postRepository.repost(post.id)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}
