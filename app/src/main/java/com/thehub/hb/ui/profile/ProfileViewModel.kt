package com.thehub.hb.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null
)

class ProfileViewModel(
    private val targetUserId: String?,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val resolvedUserId: String
        get() = if (targetUserId.isNullOrBlank()) {
            userRepository.currentUserId ?: ""
        } else {
            targetUserId
        }

    val isOwnProfile: Boolean
        get() {
            val current = userRepository.currentUserId ?: return false
            return targetUserId.isNullOrBlank() || targetUserId == current
        }

    init {
        loadProfile()
        observeFollowStatus()
    }

    private fun observeFollowStatus() {
        if (!isOwnProfile && resolvedUserId.isNotBlank()) {
            viewModelScope.launch {
                userRepository.isFollowing(resolvedUserId).collect { following ->
                    _uiState.update { it.copy(isFollowing = following) }
                }
            }
        }
    }

    fun loadProfile() {
        val uid = resolvedUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Utilisateur introuvable") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Check blocking if other user
            var isBlocked = false
            var isBlockedByMe = false
            if (!isOwnProfile) {
                val blockResult = userRepository.isBlocked(uid)
                isBlocked = blockResult.getOrDefault(false)
                isBlockedByMe = userRepository.isBlockedByMe(uid)
            }

            if (isBlocked) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOwnProfile = false,
                        isBlocked = true,
                        isBlockedByMe = isBlockedByMe,
                        user = null,
                        posts = emptyList()
                    )
                }
                return@launch
            }

            val userResult = userRepository.getUserProfile(uid)
            val postsResult = userRepository.getUserPosts(uid)

            val user = userResult.getOrNull()
            val posts = postsResult.getOrDefault(emptyList())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    user = user,
                    posts = posts,
                    isOwnProfile = isOwnProfile,
                    isBlocked = false,
                    isBlockedByMe = false,
                    errorMessage = if (user == null) "Profil introuvable" else null
                )
            }
        }
    }

    fun refresh() {
        val uid = resolvedUserId
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val userResult = userRepository.getUserProfile(uid)
            val postsResult = userRepository.getUserPosts(uid)

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    user = userResult.getOrNull() ?: it.user,
                    posts = postsResult.getOrDefault(it.posts)
                )
            }
        }
    }

    fun toggleFollow() {
        val uid = resolvedUserId
        if (isOwnProfile || uid.isBlank()) return

        val currentlyFollowing = _uiState.value.isFollowing
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = if (currentlyFollowing) {
                userRepository.unfollowUser(uid)
            } else {
                userRepository.followUser(uid)
            }

            if (result.isSuccess) {
                val delta = if (currentlyFollowing) -1 else 1
                _uiState.update { current ->
                    current.copy(
                        isActionLoading = false,
                        isFollowing = !currentlyFollowing,
                        user = current.user?.let { u ->
                            u.copy(followersCount = (u.followersCount + delta).coerceAtLeast(0))
                        }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = "Échec de l'action d'abonnement"
                    )
                }
            }
        }
    }

    fun blockUser(onSuccess: () -> Unit) {
        val uid = resolvedUserId
        if (isOwnProfile || uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = userRepository.blockUser(uid)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        isBlocked = true,
                        isBlockedByMe = true,
                        user = null,
                        posts = emptyList(),
                        userMessage = "Utilisateur bloqué"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = "Impossible de bloquer cet utilisateur"
                    )
                }
            }
        }
    }

    fun unblockUser() {
        val uid = resolvedUserId
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = userRepository.unblockUser(uid)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        isBlocked = false,
                        isBlockedByMe = false,
                        userMessage = "Utilisateur débloqué"
                    )
                }
                loadProfile()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = "Impossible de débloquer cet utilisateur"
                    )
                }
            }
        }
    }

    fun reportUser(reason: String, details: String?, onSuccess: () -> Unit) {
        val uid = resolvedUserId
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = userRepository.reportContent(
                targetType = "user",
                targetId = uid,
                reason = reason,
                details = details
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        userMessage = "Signalement envoyé. Merci de nous aider à préserver la communauté."
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = "Échec de l'envoi du signalement"
                    )
                }
            }
        }
    }

    fun openChat(onNavigateToChat: (String) -> Unit) {
        val uid = resolvedUserId
        if (isOwnProfile || uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = messageRepository.getOrCreateConversation(uid)
            _uiState.update { it.copy(isActionLoading = false) }

            result.onSuccess { conv ->
                onNavigateToChat(conv.id)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Impossible d'ouvrir la conversation")
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val targetUserId: String?,
        private val userRepository: UserRepository,
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(targetUserId, userRepository, messageRepository) as T
        }
    }
}
