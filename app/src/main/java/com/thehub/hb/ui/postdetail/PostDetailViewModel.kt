package com.thehub.hb.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Success(
        val post: Post,
        val commentsPreview: List<Comment>,
        val totalCommentsCount: Int,
        val isFollowingAuthor: Boolean = false,
        val isFollowActionLoading: Boolean = false,
        val currentUserId: String? = null
    ) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = postRepository.currentUserId

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
                val currentUid = currentUserId

                val authorIds = listOf(post.authorId) + listOfNotNull(post.originalPost?.authorId) + allComments.map { it.authorId }
                com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(authorIds)

                val initialFollowing = if (currentUid != null && currentUid != post.authorId) {
                    userRepository.checkIsFollowing(post.authorId).getOrDefault(false)
                } else false

                _uiState.value = PostDetailUiState.Success(
                    post = post,
                    commentsPreview = preview,
                    totalCommentsCount = allComments.size.coerceAtLeast(post.commentsCount),
                    isFollowingAuthor = initialFollowing,
                    currentUserId = currentUid
                )

                // Observe real-time post changes (deletions, edits, photo updates)
                launch {
                    postRepository.observePost(postId).collect { livePost ->
                        if (livePost == null) {
                            _uiState.value = PostDetailUiState.Error("Cette publication a été supprimée.")
                        } else {
                            val current = _uiState.value as? PostDetailUiState.Success
                            if (current != null) {
                                _uiState.value = current.copy(post = livePost)
                            }
                        }
                    }
                }

                // Observe real-time follow status
                if (currentUid != null && currentUid != post.authorId) {
                    launch {
                        userRepository.isFollowing(post.authorId).collect { following ->
                            val current = _uiState.value as? PostDetailUiState.Success
                            if (current != null) {
                                _uiState.value = current.copy(isFollowingAuthor = following)
                            }
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.value = PostDetailUiState.Error(
                    error.message ?: "Impossible de charger la publication."
                )
            }
        }
    }

    fun toggleFollowAuthor() {
        val currentState = _uiState.value as? PostDetailUiState.Success ?: return
        val authorId = currentState.post.authorId
        val currentUid = currentUserId
        if (currentUid == null || authorId == currentUid || currentState.isFollowActionLoading) return

        val currentlyFollowing = currentState.isFollowingAuthor
        _uiState.value = currentState.copy(isFollowActionLoading = true)

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                userRepository.unfollowUser(authorId)
            } else {
                userRepository.followUser(authorId)
            }

            val latestState = _uiState.value as? PostDetailUiState.Success
            if (latestState != null) {
                _uiState.value = latestState.copy(
                    isFollowActionLoading = false,
                    isFollowingAuthor = if (result.isSuccess) !currentlyFollowing else currentlyFollowing
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

    fun toggleBookmark() {
        val currentState = _uiState.value as? PostDetailUiState.Success ?: return
        val currentPost = currentState.post

        val newBookmarkedState = !currentPost.isBookmarkedByCurrentUser
        val updatedPost = currentPost.copy(isBookmarkedByCurrentUser = newBookmarkedState)
        _uiState.value = currentState.copy(post = updatedPost)

        viewModelScope.launch {
            val result = postRepository.toggleBookmark(postId)
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

