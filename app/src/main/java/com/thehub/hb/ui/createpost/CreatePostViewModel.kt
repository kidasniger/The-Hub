package com.thehub.hb.ui.createpost

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.utils.VideoLinkDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val text: String = "",
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val videoUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPublished: Boolean = false,
    val isEditMode: Boolean = false,
    val editPostId: String? = null
) {
    val canPublish: Boolean
        get() = if (isEditMode) {
            text.isNotBlank() && !isLoading && text.length <= 500
        } else {
            (text.isNotBlank() || selectedImageBytes != null || videoUrl != null) && !isLoading && text.length <= 500
        }

    val charCount: Int
        get() = text.length

    val isOverLimit: Boolean
        get() = text.length > 500
}

class CreatePostViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun initForEdit(
        postId: String,
        initialText: String? = null,
        initialVideoUrl: String? = null
    ) {
        _uiState.value = CreatePostUiState(
            text = initialText ?: "",
            videoUrl = initialVideoUrl,
            isEditMode = true,
            editPostId = postId,
            isLoading = initialText.isNullOrBlank()
        )
        if (initialText.isNullOrBlank()) {
            viewModelScope.launch {
                val result = postRepository.getPost(postId)
                result.onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        text = post.text,
                        videoUrl = post.videoUrl,
                        isLoading = false
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Impossible de charger la publication."
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = CreatePostUiState()
    }

    fun updateText(newText: String) {
        if (newText.length <= 500) {
            val detectedVideoUrl = VideoLinkDetector.extractVideoUrl(newText)
            _uiState.value = _uiState.value.copy(
                text = newText,
                videoUrl = detectedVideoUrl ?: _uiState.value.videoUrl,
                errorMessage = null
            )
        }
    }

    fun setVideoUrl(url: String?) {
        _uiState.value = _uiState.value.copy(
            videoUrl = url?.trim()?.takeIf { it.isNotBlank() },
            errorMessage = null
        )
    }

    fun clearVideoUrl() {
        _uiState.value = _uiState.value.copy(videoUrl = null)
    }

    fun setImage(uri: Uri?, bytes: ByteArray?) {
        if (_uiState.value.isEditMode) return
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            selectedImageBytes = bytes,
            errorMessage = null
        )
    }

    fun clearImage() {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = null,
            selectedImageBytes = null
        )
    }

    fun showErrorMessage(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun publishPost(onSuccess: (Post) -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canPublish) return

        if (currentState.isEditMode && currentState.editPostId != null) {
            viewModelScope.launch {
                _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
                val updateResult = postRepository.updatePostText(
                    currentState.editPostId,
                    currentState.text,
                    currentState.videoUrl
                )
                updateResult.onSuccess {
                    val postResult = postRepository.getPost(currentState.editPostId)
                    val post = postResult.getOrNull() ?: Post(
                        id = currentState.editPostId,
                        text = currentState.text.trim(),
                        authorId = postRepository.currentUserId ?: "",
                        authorUsername = "",
                        videoUrl = currentState.videoUrl,
                        createdAt = com.google.firebase.Timestamp.now()
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPublished = true
                    )
                    onSuccess(post)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Impossible de modifier la publication."
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

            var uploadedImageUrl: String? = null
            if (currentState.selectedImageBytes != null) {
                val uploadResult = postRepository.uploadPostImage(currentState.selectedImageBytes)
                if (uploadResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = uploadResult.exceptionOrNull()?.message ?: "Échec de l'upload de l'image."
                    )
                    return@launch
                }
                uploadedImageUrl = uploadResult.getOrNull()
            }

            val result = postRepository.createPost(
                text = currentState.text,
                imageUrl = uploadedImageUrl,
                videoUrl = currentState.videoUrl
            )

            result.onSuccess { createdPost ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPublished = true
                )
                onSuccess(createdPost)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Impossible de créer la publication."
                )
            }
        }
    }
}
