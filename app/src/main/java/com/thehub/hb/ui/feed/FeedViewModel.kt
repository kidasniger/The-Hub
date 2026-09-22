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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private companion object {
        const val PAGE_SIZE = 20L
    }

    private var feedJob: Job? = null
    private var lastFeedVisible: DocumentSnapshot? = null
    private val postActionMutexes = mutableMapOf<String, Mutex>()

    private fun actionMutexFor(postId: String): Mutex =
        postActionMutexes.getOrPut(postId) { Mutex() }
    private var hasMoreFeedPages = true

    init {
        loadFirstPage(isRefresh = false)
    }

    fun refresh() {
        HubApplication.clearImageCache()
        loadFirstPage(isRefresh = true)
    }

    fun loadFeed(isRefresh: Boolean = false) {
        loadFirstPage(isRefresh = isRefresh)
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        feedJob?.cancel()

        val previousCursor = lastFeedVisible
        val previousHasMore = hasMoreFeedPages
        lastFeedVisible = null
        hasMoreFeedPages = true

        val currentPosts = (_uiState.value as? FeedUiState.Success)?.posts ?: emptyList()
        if (isRefresh && currentPosts.isNotEmpty()) {
            _uiState.value = FeedUiState.Success(
                posts = currentPosts,
                isRefreshing = true,
                isLoadingMore = false,
                hasMore = true
            )
        } else if (!isRefresh) {
            _uiState.value = FeedUiState.Loading
        }

        feedJob = viewModelScope.launch {
            try {
                val result = postRepository.getFeed(
                    pageSize = PAGE_SIZE,
                    lastVisible = null
                )

                result.onSuccess { (posts, lastVisible) ->
                    val authorIds = posts.flatMap { listOfNotNull(it.authorId, it.originalPost?.authorId) }
                    com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(authorIds)

                    lastFeedVisible = lastVisible
                    hasMoreFeedPages = posts.size.toLong() == PAGE_SIZE && lastVisible != null

                    if (posts.isEmpty()) {
                        _uiState.value = FeedUiState.Empty
                        hasMoreFeedPages = false
                    } else {
                        _uiState.value = FeedUiState.Success(
                            posts = posts,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = hasMoreFeedPages
                        )
                    }
                }.onFailure { e ->
                    val currentStateAfterFailure = _uiState.value
                    if (currentStateAfterFailure is FeedUiState.Success) {
                        if (isRefresh) {
                            lastFeedVisible = previousCursor
                            hasMoreFeedPages = previousHasMore
                        }
                        _uiState.value = currentStateAfterFailure.copy(
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = if (isRefresh) previousHasMore else currentStateAfterFailure.hasMore
                        )
                    } else {
                        _uiState.value = FeedUiState.Error(
                            e.message ?: "Impossible de charger les publications."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val currentStateAfterFailure = _uiState.value
                if (currentStateAfterFailure is FeedUiState.Success) {
                    if (isRefresh) {
                        lastFeedVisible = previousCursor
                        hasMoreFeedPages = previousHasMore
                    }
                    _uiState.value = currentStateAfterFailure.copy(
                        isRefreshing = false,
                        isLoadingMore = false,
                        hasMore = if (isRefresh) previousHasMore else currentStateAfterFailure.hasMore
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
        if (currentState.isLoadingMore || !currentState.hasMore || !hasMoreFeedPages) return

        val cursor = lastFeedVisible ?: return
        _uiState.value = currentState.copy(isLoadingMore = true)

        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            try {
                val result = postRepository.getFeed(
                    pageSize = PAGE_SIZE,
                    lastVisible = cursor
                )

                result.onSuccess { (newPosts, newLastVisible) ->
                    val authorIds = newPosts.flatMap { listOfNotNull(it.authorId, it.originalPost?.authorId) }
                    com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(authorIds)

                    lastFeedVisible = newLastVisible
                    hasMoreFeedPages = newPosts.size.toLong() == PAGE_SIZE && newLastVisible != null

                    val latestState = _uiState.value
                    if (latestState is FeedUiState.Success) {
                        _uiState.value = latestState.copy(
                            posts = latestState.posts + newPosts,
                            isLoadingMore = false,
                            isRefreshing = false,
                            hasMore = hasMoreFeedPages
                        )
                    }
                }.onFailure { e ->
                    val latestState = _uiState.value
                    if (latestState is FeedUiState.Success) {
                        _uiState.value = latestState.copy(
                            isLoadingMore = false,
                            isRefreshing = false
                        )
                    } else {
                        _uiState.value = FeedUiState.Error(
                            e.message ?: "Impossible de charger les publications."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val latestState = _uiState.value
                if (latestState is FeedUiState.Success) {
                    _uiState.value = latestState.copy(
                        isLoadingMore = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            actionMutexFor(postId).withLock {
                val currentState = _uiState.value as? FeedUiState.Success ?: return@withLock
                val originalPost = currentState.posts.firstOrNull { it.id == postId } ?: return@withLock
                val updatedPost = originalPost.copy(
                    isLikedByCurrentUser = !originalPost.isLikedByCurrentUser,
                    likesCount = if (!originalPost.isLikedByCurrentUser) {
                        originalPost.likesCount + 1
                    } else {
                        (originalPost.likesCount - 1).coerceAtLeast(0)
                    }
                )
                _uiState.value = currentState.copy(
                    posts = currentState.posts.map { if (it.id == postId) updatedPost else it }
                )

                val result = postRepository.toggleLike(postId)
                if (result.isFailure) {
                    val latest = _uiState.value as? FeedUiState.Success
                    if (latest != null) {
                        _uiState.value = latest.copy(
                            posts = latest.posts.map { if (it.id == postId) originalPost else it }
                        )
                    }
                }
            }
        }
    }

    fun toggleBookmark(postId: String) {
        viewModelScope.launch {
            actionMutexFor("bookmark:$postId").withLock {
                val currentState = _uiState.value as? FeedUiState.Success ?: return@withLock
                val originalPost = currentState.posts.firstOrNull { it.id == postId } ?: return@withLock
                val updatedPost = originalPost.copy(
                    isBookmarkedByCurrentUser = !originalPost.isBookmarkedByCurrentUser
                )
                _uiState.value = currentState.copy(
                    posts = currentState.posts.map { if (it.id == postId) updatedPost else it }
                )

                val result = postRepository.toggleBookmark(postId)
                if (result.isFailure) {
                    val latest = _uiState.value as? FeedUiState.Success
                    if (latest != null) {
                        _uiState.value = latest.copy(
                            posts = latest.posts.map { if (it.id == postId) originalPost else it }
                        )
                    }
                }
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
