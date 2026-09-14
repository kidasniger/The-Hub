package com.thehub.hb.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BookmarksUiState {
    data object Loading : BookmarksUiState
    data object Empty : BookmarksUiState
    data class Success(
        val posts: List<Post>,
        val isRefreshing: Boolean = false
    ) : BookmarksUiState
    data class Error(val message: String) : BookmarksUiState
}

class BookmarksViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookmarksUiState>(BookmarksUiState.Loading)
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    init {
        loadBookmarks(isRefresh = false)
    }

    fun refresh() {
        loadBookmarks(isRefresh = true)
    }

    fun loadBookmarks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                val currentPosts = (_uiState.value as? BookmarksUiState.Success)?.posts ?: emptyList()
                if (currentPosts.isNotEmpty()) {
                    _uiState.value = BookmarksUiState.Success(posts = currentPosts, isRefreshing = true)
                } else {
                    _uiState.value = BookmarksUiState.Loading
                }
            } else if (_uiState.value !is BookmarksUiState.Success) {
                _uiState.value = BookmarksUiState.Loading
            }

            val result = postRepository.getBookmarks()
            result.onSuccess { posts ->
                if (posts.isEmpty()) {
                    _uiState.value = BookmarksUiState.Empty
                } else {
                    _uiState.value = BookmarksUiState.Success(posts = posts, isRefreshing = false)
                }
            }.onFailure { e ->
                _uiState.value = BookmarksUiState.Error(
                    e.message ?: "Impossible de charger vos publications enregistrées."
                )
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentState = _uiState.value as? BookmarksUiState.Success ?: return
        val originalPosts = currentState.posts

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
                _uiState.value = currentState.copy(posts = originalPosts)
            }
        }
    }

    fun toggleBookmark(postId: String) {
        val currentState = _uiState.value as? BookmarksUiState.Success ?: return
        val originalPosts = currentState.posts

        // Optimistically remove from bookmarks list
        val remainingPosts = originalPosts.filter { it.id != postId }
        if (remainingPosts.isEmpty()) {
            _uiState.value = BookmarksUiState.Empty
        } else {
            _uiState.value = currentState.copy(posts = remainingPosts)
        }

        viewModelScope.launch {
            val result = postRepository.toggleBookmark(postId)
            result.onFailure {
                // Revert
                _uiState.value = currentState.copy(posts = originalPosts)
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
