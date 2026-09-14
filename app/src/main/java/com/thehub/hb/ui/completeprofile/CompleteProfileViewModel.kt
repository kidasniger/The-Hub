package com.thehub.hb.ui.completeprofile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CompleteProfileUiState(
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val displayName: String = "",
    val bio: String = "",
    val birthdate: String = "",
    val displayNameError: String? = null,
    val bioError: String? = null,
    val birthdateError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false
)

sealed interface CompleteProfileNavigationEvent {
    data object NavigateToFeed : CompleteProfileNavigationEvent
}

class CompleteProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompleteProfileUiState())
    val uiState: StateFlow<CompleteProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CompleteProfileNavigationEvent>()
    val events: SharedFlow<CompleteProfileNavigationEvent> = _events.asSharedFlow()

    init {
        // Pre-fill existing user info if any
        authRepository.currentFirebaseUser?.let { user ->
            _uiState.update {
                it.copy(
                    displayName = user.displayName ?: "",
                    selectedImageUri = user.photoUrl
                )
            }
        }
    }

    fun onImageSelected(context: Context, uri: Uri?) {
        if (uri == null) return
        _uiState.update { it.copy(selectedImageUri = uri) }

        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (_: Exception) {
                    null
                }
            }
            _uiState.update { it.copy(selectedImageBytes = bytes) }
        }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(displayName = name, displayNameError = null, generalError = null) }
    }

    fun onBioChange(bio: String) {
        if (bio.length <= 150) {
            _uiState.update { it.copy(bio = bio, bioError = null) }
        }
    }

    fun onBirthdateChange(birthdate: String) {
        _uiState.update { it.copy(birthdate = birthdate, birthdateError = null) }
    }

    fun completeProfile() {
        val state = _uiState.value
        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(displayNameError = "Veuillez entrer votre nom affiché.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            var uploadedPhotoUrl: String? = state.selectedImageUri?.toString()

            // If image bytes were loaded from gallery, upload to ImgBB
            if (state.selectedImageBytes != null) {
                val uploadResult = authRepository.uploadProfilePhoto(state.selectedImageBytes)
                uploadResult.onSuccess { url ->
                    uploadedPhotoUrl = url
                }.onFailure { uploadErr ->
                    // Fallback: continue even if ImgBB upload fails, or notify user
                }
            }

            val result = authRepository.completeProfile(
                displayName = state.displayName.trim(),
                bio = state.bio.ifBlank { null },
                birthdate = state.birthdate.ifBlank { null },
                photoUrl = uploadedPhotoUrl
            )

            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                _events.emit(CompleteProfileNavigationEvent.NavigateToFeed)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(generalError = throwable.localizedMessage ?: "Erreur lors de la mise à jour du profil.")
                }
            }
        }
    }
}
