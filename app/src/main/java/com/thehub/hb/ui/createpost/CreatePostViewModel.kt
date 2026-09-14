package com.thehub.hb.ui.createpost

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val text: String = "",
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPublished: Boolean = false
) {
    val canPublish: Boolean
        get() = (text.isNotBlank() || selectedImageBytes != null) && !isLoading && text.length <= 500

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

    fun updateText(newText: String) {
        if (newText.length <= 500) {
            _uiState.value = _uiState.value.copy(text = newText, errorMessage = null)
        }
    }

    fun setImage(uri: Uri?, bytes: ByteArray?) {
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

    fun publishPost(onSuccess: (Post) -> Unit) {
        val currentState = _uiState.value
        if (!currentState.canPublish) return

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
                imageUrl = uploadedImageUrl
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
