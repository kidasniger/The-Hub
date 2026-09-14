package com.thehub.hb.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val originalUser: User? = null,
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val birthdate: String = "",
    val photoUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val displayNameError: String? = null,
    val usernameError: String? = null,
    val bioError: String? = null,
    val generalError: String? = null,
    val saveSuccess: Boolean = false
) {
    val hasChanges: Boolean
        get() {
            if (originalUser == null) return false
            if (selectedImageBytes != null) return true
            if (displayName.trim() != (originalUser.displayName ?: "").trim()) return true
            if (username.trim() != originalUser.username.trim()) return true
            if (bio.trim() != (originalUser.bio ?: "").trim()) return true
            if (birthdate.trim() != (originalUser.birthdate ?: "").trim()) return true
            return false
        }

    val isValid: Boolean
        get() {
            if (username.trim().length < 3) return false
            if (bio.length > 150) return false
            return hasChanges && !isSaving
        }
}

class EditProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = userRepository.currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = userRepository.getUserProfile(uid)
            val user = result.getOrNull()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        originalUser = user,
                        displayName = user.displayName ?: "",
                        username = user.username,
                        bio = user.bio ?: "",
                        birthdate = user.birthdate ?: "",
                        photoUrl = user.photoUrl
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = "Impossible de charger les données du profil"
                    )
                }
            }
        }
    }

    fun onDisplayNameChanged(value: String) {
        _uiState.update { it.copy(displayName = value, displayNameError = null) }
    }

    fun onUsernameChanged(value: String) {
        val clean = value.replace("@", "").trim()
        val error = when {
            clean.length < 3 -> "Au moins 3 caractères"
            clean.contains(" ") -> "Pas d'espaces autorisés"
            else -> null
        }
        _uiState.update { it.copy(username = clean, usernameError = error) }
    }

    fun onBioChanged(value: String) {
        if (value.length <= 150) {
            _uiState.update { it.copy(bio = value, bioError = null) }
        }
    }

    fun onBirthdateChanged(value: String) {
        _uiState.update { it.copy(birthdate = value) }
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

    fun saveProfile(onSuccess: () -> Unit) {
        val state = _uiState.value
        val cleanUsername = state.username.trim()

        if (cleanUsername.length < 3) {
            _uiState.update { it.copy(usernameError = "Le nom d'utilisateur doit faire au moins 3 caractères.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, generalError = null) }

            // Upload photo if new image selected
            var uploadedUrl = state.photoUrl
            val bytes = state.selectedImageBytes
            if (bytes != null) {
                val uploadResult = userRepository.uploadProfilePhoto(bytes)
                if (uploadResult.isSuccess) {
                    uploadedUrl = uploadResult.getOrNull()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            generalError = "Échec du téléversement de la photo de profil."
                        )
                    }
                    return@launch
                }
            }

            val updateResult = userRepository.updateProfile(
                displayName = state.displayName.trim(),
                username = cleanUsername,
                bio = state.bio.trim().takeIf { it.isNotBlank() },
                birthdate = state.birthdate.trim().takeIf { it.isNotBlank() },
                photoUrl = uploadedUrl
            )

            if (updateResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        photoUrl = uploadedUrl,
                        selectedImageBytes = null
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        generalError = updateResult.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour du profil"
                    )
                }
            }
        }
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(userRepository) as T
        }
    }
}
