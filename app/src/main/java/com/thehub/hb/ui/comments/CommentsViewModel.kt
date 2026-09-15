package com.thehub.hb.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommentsUiState(
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val inputText: String = "",
    val currentUserUsername: String = "utilisateur",
    val currentUserPhotoUrl: String? = null,
    val errorMessage: String? = null,
    val replyingTo: Comment? = null,
    val expandedThreads: Set<String> = emptySet()
) {
    val canSend: Boolean
        get() = inputText.isNotBlank() && !isSending
}

class CommentsViewModel(
    private val postId: String,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
        startListeningComments()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val uid = authRepository.currentFirebaseUser?.uid ?: return@launch
            val profile = authRepository.getUserProfile(uid).getOrNull()
            if (profile != null) {
                _uiState.value = _uiState.value.copy(
                    currentUserUsername = profile.username,
                    currentUserPhotoUrl = profile.photoUrl
                )
            } else {
                val email = authRepository.currentFirebaseUser?.email ?: ""
                val fallbackUsername = if (email.contains("@")) email.substringBefore("@") else "utilisateur"
                _uiState.value = _uiState.value.copy(
                    currentUserUsername = fallbackUsername,
                    currentUserPhotoUrl = authRepository.currentFirebaseUser?.photoUrl?.toString()
                )
            }
        }
    }

    private fun startListeningComments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                postRepository.observeComments(postId).collect { commentList ->
                    com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(commentList.map { it.authorId })
                    _uiState.value = _uiState.value.copy(
                        comments = commentList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                // Fallback to one-time fetch
                val result = postRepository.getComments(postId)
                result.onSuccess { list ->
                    com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(list.map { it.authorId })
                    _uiState.value = _uiState.value.copy(comments = list, isLoading = false)
                }.onFailure { error ->
                    val msg = error.message ?: ""
                    val friendly = if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("insufficient permissions", ignoreCase = true)) {
                        "Erreur d'accès Firestore : veuillez configurer les règles de sécurité dans la console Firebase."
                    } else {
                        error.message ?: "Impossible de charger les commentaires."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = friendly
                    )
                }
            }
        }
    }

    fun updateInputText(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText, errorMessage = null)
    }

    fun startReply(comment: Comment) {
        val currentText = _uiState.value.inputText
        val mention = "@${comment.authorUsername} "
        val newText = if (currentText.isBlank()) {
            mention
        } else if (!currentText.contains(mention)) {
            "$mention$currentText"
        } else {
            currentText
        }
        _uiState.value = _uiState.value.copy(
            replyingTo = comment,
            inputText = newText,
            errorMessage = null
        )
    }

    fun cancelReply() {
        val replyingTo = _uiState.value.replyingTo
        var text = _uiState.value.inputText
        if (replyingTo != null) {
            val mention = "@${replyingTo.authorUsername} "
            if (text.trim() == mention.trim()) {
                text = ""
            }
        }
        _uiState.value = _uiState.value.copy(
            replyingTo = null,
            inputText = text
        )
    }

    fun toggleThreadExpanded(rootCommentId: String) {
        val current = _uiState.value.expandedThreads
        val updated = if (current.contains(rootCommentId)) {
            current - rootCommentId
        } else {
            current + rootCommentId
        }
        _uiState.value = _uiState.value.copy(expandedThreads = updated)
    }

    fun toggleCommentLike(commentId: String) {
        val currentState = _uiState.value
        val originalComments = currentState.comments

        // Optimistic UI update
        val updatedComments = originalComments.map { comment ->
            if (comment.id == commentId) {
                val newLikedState = !comment.isLikedByCurrentUser
                val newCount = if (newLikedState) comment.likesCount + 1 else (comment.likesCount - 1).coerceAtLeast(0)
                comment.copy(isLikedByCurrentUser = newLikedState, likesCount = newCount)
            } else comment
        }
        _uiState.value = currentState.copy(comments = updatedComments)

        viewModelScope.launch {
            val result = postRepository.toggleCommentLike(postId, commentId)
            result.onFailure {
                // Revert on failure
                _uiState.value = currentState.copy(comments = originalComments)
            }
        }
    }

    fun sendComment() {
        val currentState = _uiState.value
        if (!currentState.canSend) return

        val replyingToComment = currentState.replyingTo
        val parentId = replyingToComment?.let { it.parentCommentId ?: it.id }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isSending = true)
            val result = postRepository.addComment(
                postId = postId,
                text = currentState.inputText,
                parentCommentId = parentId
            )

            result.onSuccess {
                val newExpanded = if (parentId != null) {
                    currentState.expandedThreads + parentId
                } else {
                    currentState.expandedThreads
                }
                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    replyingTo = null,
                    isSending = false,
                    expandedThreads = newExpanded
                )
            }.onFailure { error ->
                val msg = error.message ?: ""
                val friendly = if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("insufficient permissions", ignoreCase = true)) {
                    "Erreur d'autorisation Firestore : autorisations insuffisantes pour publier le commentaire."
                } else {
                    error.message ?: "Erreur lors de l'envoi du commentaire."
                }
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = friendly
                )
            }
        }
    }
}
