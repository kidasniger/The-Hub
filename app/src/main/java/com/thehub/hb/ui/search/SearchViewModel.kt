package com.thehub.hb.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.TrendingHashtag
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.NotificationRepository
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.SearchRepository
import com.thehub.hb.data.repository.UserRepository
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
import kotlinx.coroutines.Job
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
    val trendingError: String? = null,
    val suggestedUsers: List<User> = emptyList(),
    val suggestedFollowingIds: Set<String> = emptySet(),
    val isSuggestionsLoading: Boolean = false,
    val suggestionsError: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchRepository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String?
        get() = searchRepository.currentUserId

    private val _queryDebounceFlow = MutableStateFlow("")
    private val followStatusJobs = mutableMapOf<String, Job>()
    private val suggestedFollowStatusJobs = mutableMapOf<String, Job>()
    private var searchJob: Job? = null
    private var trendingJob: Job? = null
    private var suggestionsJob: Job? = null

    init {
        loadTrending()
        loadSuggestions()
        viewModelScope.launch {
            _queryDebounceFlow
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { text ->
                    if (text.isNotBlank()) {
                        performSearch(text, _uiState.value.searchMode)
                    } else {
                        searchJob?.cancel()
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

    fun loadSuggestions() {
        suggestionsJob?.cancel()
        suggestedFollowStatusJobs.values.forEach { it.cancel() }
        suggestedFollowStatusJobs.clear()

        suggestionsJob = viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionsLoading = true, suggestionsError = null) }
            val result = searchRepository.getSuggestedUsers(limit = 8)
            result.fold(
                onSuccess = { users ->
                    _uiState.update {
                        it.copy(
                            suggestedUsers = users,
                            suggestedFollowingIds = emptySet(),
                            isSuggestionsLoading = false,
                            suggestionsError = null
                        )
                    }
                    checkSuggestedFollowStatuses(users)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSuggestionsLoading = false,
                            suggestionsError = error.localizedMessage
                                ?: "Impossible de charger les créateurs à découvrir."
                        )
                    }
                }
            )
        }
    }

    private fun checkSuggestedFollowStatuses(users: List<User>) {
        val currentUid = currentUserId ?: return
        suggestedFollowStatusJobs.values.forEach { it.cancel() }
        suggestedFollowStatusJobs.clear()
        _uiState.update { it.copy(suggestedFollowingIds = emptySet()) }

        users.forEach { user ->
            if (user.uid == currentUid) return@forEach
            suggestedFollowStatusJobs[user.uid] = viewModelScope.launch {
                notificationRepository.isFollowing(user.uid).collectLatest { isFollowing ->
                    _uiState.update { state ->
                        val updated = if (isFollowing) {
                            state.suggestedFollowingIds + user.uid
                        } else {
                            state.suggestedFollowingIds - user.uid
                        }
                        state.copy(suggestedFollowingIds = updated)
                    }
                }
            }
        }
    }

    fun loadTrending() {
        trendingJob?.cancel()
        trendingJob = viewModelScope.launch {
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

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
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

        // Cancel collectors from the previous result set so repeated searches do not
        // accumulate long-lived Firestore listeners for stale users.
        followStatusJobs.values.forEach { it.cancel() }
        followStatusJobs.clear()
        _uiState.update { it.copy(followingIds = emptySet()) }

        for (user in users) {
            if (user.uid == currentUid) continue
            followStatusJobs[user.uid] = viewModelScope.launch {
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
        if (targetUid == currentUserId) return

        val isCurrentlyFollowing = _uiState.value.followingIds.contains(targetUid)
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    followingIds = if (isCurrentlyFollowing) {
                        state.followingIds - targetUid
                    } else {
                        state.followingIds + targetUid
                    },
                    suggestedFollowingIds = if (isCurrentlyFollowing) {
                        state.suggestedFollowingIds - targetUid
                    } else {
                        state.suggestedFollowingIds + targetUid
                    },
                    errorMessage = null
                )
            }

            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(targetUid)
            } else {
                userRepository.followUser(targetUid)
            }

            if (result.isFailure) {
                val raw = result.exceptionOrNull()?.localizedMessage.orEmpty()
                val friendly = when {
                    raw.contains("PERMISSION_DENIED", ignoreCase = true) ||
                        raw.contains("insufficient permissions", ignoreCase = true) ->
                        "Impossible de modifier l'abonnement : autorisations Firestore insuffisantes."
                    raw.isNotBlank() -> raw
                    else -> "Impossible de modifier l'abonnement."
                }
                _uiState.update { state ->
                    state.copy(
                        followingIds = if (isCurrentlyFollowing) {
                            state.followingIds + targetUid
                        } else {
                            state.followingIds - targetUid
                        },
                        suggestedFollowingIds = if (isCurrentlyFollowing) {
                            state.suggestedFollowingIds + targetUid
                        } else {
                            state.suggestedFollowingIds - targetUid
                        },
                        errorMessage = friendly
                    )
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
