package com.thehub.hb.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Success(
        val post: Post,
        val commentsPreview: List<Comment>,
        val totalCommentsCount: Int
    ) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    init {
        loadPostDetail()
    }

    fun loadPostDetail() {
        viewModelScope.launch {
            _uiState.value = PostDetailUiState.Loading
            val postResult = postRepository.getPost(postId)

            postResult.onSuccess { post ->
                val commentsResult = postRepository.getComments(postId)
                val allComments = commentsResult.getOrDefault(emptyList())
                val preview = allComments.take(3)

                _uiState.value = PostDetailUiState.Success(
                    post = post,
                    commentsPreview = preview,
                    totalCommentsCount = allComments.size.coerceAtLeast(post.commentsCount)
                )
            }.onFailure { error ->
                _uiState.value = PostDetailUiState.Error(
                    error.message ?: "Impossible de charger la publication."
                )
            }
        }
    }

    fun toggleLike() {
        val currentState = _uiState.value as? PostDetailUiState.Success ?: return
        val currentPost = currentState.post

        val newLikedState = !currentPost.isLikedByCurrentUser
        val newLikesCount = if (newLikedState) currentPost.likesCount + 1 else (currentPost.likesCount - 1).coerceAtLeast(0)
        val updatedPost = currentPost.copy(
            isLikedByCurrentUser = newLikedState,
            likesCount = newLikesCount
        )

        _uiState.value = currentState.copy(post = updatedPost)

        viewModelScope.launch {
            val result = postRepository.toggleLike(postId)
            if (result.isFailure) {
                // Revert
                _uiState.value = currentState.copy(post = currentPost)
            }
        }
    }

    fun repost(onSuccess: () -> Unit = {}) {
        val currentState = _uiState.value as? PostDetailUiState.Success ?: return
        viewModelScope.launch {
            val result = postRepository.repost(currentState.post.id)
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }
}
