package com.thehub.hb.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.thehub.hb.HubApplication
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val messageRepository: MessageRepository? = null,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = postRepository.currentUserId

    val unreadConversationsCount: StateFlow<Int> = messageRepository?.getUnreadConversationsCount()
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        ) ?: MutableStateFlow(0).asStateFlow()

    private var feedJob: Job? = null
    private var feedLimit: Long = 30L

    init {
        startObservingFeed(isRefresh = false)
    }

    fun refresh() {
        HubApplication.clearImageCache()
        startObservingFeed(isRefresh = true)
    }

    fun loadFeed(isRefresh: Boolean = false) {
        startObservingFeed(isRefresh = isRefresh)
    }

    fun startObservingFeed(isRefresh: Boolean = false) {
        feedJob?.cancel()
        if (isRefresh) {
            val currentPosts = (_uiState.value as? FeedUiState.Success)?.posts ?: emptyList()
            if (currentPosts.isNotEmpty()) {
                _uiState.value = FeedUiState.Success(
                    posts = currentPosts,
                    isRefreshing = true,
                    isLoadingMore = false,
                    hasMore = true
                )
            }
        } else if (_uiState.value !is FeedUiState.Success) {
            _uiState.value = FeedUiState.Loading
        }

        feedJob = viewModelScope.launch {
            try {
                postRepository.observeFeed(limit = feedLimit).collect { posts ->
                    val authorIds = posts.flatMap { listOfNotNull(it.authorId, it.originalPost?.authorId) }
                    com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(authorIds)

                    if (posts.isEmpty()) {
                        _uiState.value = FeedUiState.Empty
                    } else {
                        _uiState.value = FeedUiState.Success(
                            posts = posts,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = posts.size.toLong() >= feedLimit
                        )
                    }
                }
            } catch (e: CancellationException) {
                // Cancelled due to refresh cancellation - rethrow so coroutine terminates cleanly without error UI
                throw e
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is FeedUiState.Success) {
                    // Do not flash "Oups !" error screen if posts were already displayed
                    _uiState.value = currentState.copy(
                        isRefreshing = false,
                        isLoadingMore = false
                    )
                } else {
                    _uiState.value = FeedUiState.Error(
                        e.message ?: "Impossible de charger les publications."
                    )
                }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value as? FeedUiState.Success ?: return
        if (currentState.isLoadingMore || !currentState.hasMore) return

        feedLimit += 20L
        _uiState.value = currentState.copy(isLoadingMore = true)
        startObservingFeed(isRefresh = false)
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

    fun toggleBookmark(postId: String) {
        val currentState = _uiState.value as? FeedUiState.Success ?: return
        val originalPosts = currentState.posts

        val updatedPosts = originalPosts.map { post ->
            if (post.id == postId) {
                post.copy(isBookmarkedByCurrentUser = !post.isBookmarkedByCurrentUser)
            } else post
        }
        _uiState.value = currentState.copy(posts = updatedPosts)

        viewModelScope.launch {
            val result = postRepository.toggleBookmark(postId)
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

    fun deletePost(postId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = postRepository.deletePost(postId)
            result.onSuccess {
                val currentState = _uiState.value as? FeedUiState.Success
                if (currentState != null) {
                    _uiState.value = currentState.copy(
                        posts = currentState.posts.filter { it.id != postId }
                    )
                }
                onComplete(true, null)
            }.onFailure { e ->
                onComplete(false, e.message ?: "Échec de la suppression de la publication.")
            }
        }
    }

    fun reportPost(postId: String, reason: String, details: String?, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (userRepository != null) {
                val result = userRepository.reportContent("post", postId, reason, details)
                result.onSuccess {
                    onComplete(true, null)
                }.onFailure { e ->
                    onComplete(false, e.message ?: "Échec du signalement.")
                }
            } else {
                onComplete(true, null)
            }
        }
    }
}
