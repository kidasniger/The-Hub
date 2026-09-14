package com.thehub.hb.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Success(
        val posts: List<Post>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

class FeedViewModel(
    private val postRepository: PostRepository,
    private val messageRepository: MessageRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    val unreadConversationsCount: StateFlow<Int> = messageRepository?.getUnreadConversationsCount()
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        ) ?: MutableStateFlow(0).asStateFlow()

    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isLoadingPage = false

    init {
        loadFeed(isRefresh = false)
    }

    fun refresh() {
        loadFeed(isRefresh = true)
    }

    fun loadFeed(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                val currentPosts = (_uiState.value as? FeedUiState.Success)?.posts ?: emptyList()
                if (currentPosts.isNotEmpty()) {
                    _uiState.value = FeedUiState.Success(
                        posts = currentPosts,
                        isRefreshing = true,
                        isLoadingMore = false,
                        hasMore = true
                    )
                } else {
                    _uiState.value = FeedUiState.Loading
                }
                lastVisibleDocument = null
            } else if (_uiState.value !is FeedUiState.Success) {
                _uiState.value = FeedUiState.Loading
            }

            val result = postRepository.getFeed(pageSize = 15, lastVisible = null)
            result.onSuccess { (posts, lastDoc) ->
                lastVisibleDocument = lastDoc
                if (posts.isEmpty()) {
                    _uiState.value = FeedUiState.Empty
                } else {
                    _uiState.value = FeedUiState.Success(
                        posts = posts,
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasMore = lastDoc != null && posts.size >= 15
                    )
                }
            }.onFailure { exception ->
                _uiState.value = FeedUiState.Error(
                    exception.message ?: "Impossible de charger les publications."
                )
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value as? FeedUiState.Success ?: return
        if (currentState.isLoadingMore || !currentState.hasMore || isLoadingPage) return
        val lastDoc = lastVisibleDocument ?: return

        isLoadingPage = true
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            val result = postRepository.getFeed(pageSize = 15, lastVisible = lastDoc)
            isLoadingPage = false
            result.onSuccess { (newPosts, nextDoc) ->
                lastVisibleDocument = nextDoc
                val updatedList = currentState.posts + newPosts
                _uiState.value = currentState.copy(
                    posts = updatedList,
                    isLoadingMore = false,
                    hasMore = nextDoc != null && newPosts.isNotEmpty()
                )
            }.onFailure {
                _uiState.value = currentState.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _uiState.value as? FeedUiState.Success ?: return
        val originalPosts = currentState.posts

        // Optimistic UI update
        val updatedPosts = originalPosts.map { post ->
            if (post.id == postId) {
                val newLikedState = !post.isLikedByCurrentUser
                val newLikesCount = if (newLikedState) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLikedByCurrentUser = newLikedState, likesCount = newLikesCount)
            } else post
        }
        _uiState.value = currentState.copy(posts = updatedPosts)

        viewModelScope.launch {
            val result = postRepository.toggleLike(postId)
            result.onFailure {
                // Revert on error
                _uiState.value = currentState.copy(posts = originalPosts)
            }
        }
    }

    fun repost(post: Post, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = postRepository.repost(post.id)
            result.onSuccess { newRepost ->
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    _uiState.value = currentState.copy(posts = listOf(newRepost.copy(originalPost = post)) + currentState.posts)
                } else {
                    refresh()
                }
                onSuccess()
            }
        }
    }
}
