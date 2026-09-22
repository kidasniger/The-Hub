package com.thehub.hb.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Post
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.MessageRepository
import com.thehub.hb.data.repository.PostRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileViewMode {
    GRID,
    LIST
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val isOwnProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isMessagingAllowed: Boolean = true,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val viewMode: ProfileViewMode = ProfileViewMode.GRID
)

class ProfileViewModel(
    private val targetUserId: String?,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val resolvedUserId: String
        get() {
            if (!targetUserId.isNullOrBlank()) return targetUserId
            return userRepository.currentUserId
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: ""
        }

    val isOwnProfile: Boolean
        get() {
            val current = userRepository.currentUserId
                ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: return false
            return targetUserId.isNullOrBlank() || targetUserId == current
        }

    private var userJob: Job? = null
    private var postsJob: Job? = null

    init {
        loadProfile()
        observeFollowStatus()
    }

    fun setViewMode(mode: ProfileViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    private fun observeFollowStatus() {
        val uid = resolvedUserId
        if (!isOwnProfile && uid.isNotBlank()) {
            viewModelScope.launch {
                userRepository.isFollowing(uid).collect { following ->
                    _uiState.update { it.copy(isFollowing = following) }
                }
            }
        }
    }

    fun loadProfile() {
        val uid = resolvedUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
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
            val adjustedUser = user?.copy(
                postsCount = posts.size
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    user = adjustedUser,
                    posts = posts,
                    isOwnProfile = isOwnProfile,
                    isBlocked = false,
                    isBlockedByMe = false,
                    isMessagingAllowed = user?.isVerified != true || user?.verificationType != "admin",
                    errorMessage = if (user == null) "Profil introuvable" else null
                )
            }

            // Real-time listener on user profile document (avatar, name, bio, counters)
            userJob?.cancel()
            userJob = viewModelScope.launch {
                userRepository.observeUserProfile(uid).collect { liveUser ->
                    if (liveUser != null) {
                        _uiState.update { current ->
                            val currentPostsCount = current.posts.size
                            val updatedUser = liveUser.copy(
                                postsCount = currentPostsCount
                            )
                            current.copy(user = updatedUser)
                        }
                    }
                }
            }

            // Real-time listener on user posts (posts deleted/added in Firestore update list and counter immediately)
            postsJob?.cancel()
            postsJob = viewModelScope.launch {
                userRepository.observeUserPosts(uid).collect { livePosts ->
                    _uiState.update { current ->
                        val updatedUser = current.user?.copy(
                            postsCount = livePosts.size
                        )
                        current.copy(
                            posts = livePosts,
                            user = updatedUser
                        )
                    }
                }
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

            val rawUser = userResult.getOrNull() ?: _uiState.value.user
            val posts = postsResult.getOrDefault(_uiState.value.posts)
            val adjustedUser = rawUser?.copy(
                postsCount = posts.size
            )

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    user = adjustedUser,
                    posts = posts
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
        if (isOwnProfile || uid.isBlank() || !_uiState.value.isMessagingAllowed) {
            _uiState.update { it.copy(errorMessage = "Ce compte officiel ne reçoit pas de messages privés.") }
            return
        }

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

    fun toggleBookmark(postId: String) {
        val currentPosts = _uiState.value.posts
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(isBookmarkedByCurrentUser = !post.isBookmarkedByCurrentUser)
            } else post
        }
        _uiState.update { it.copy(posts = updatedPosts) }

        viewModelScope.launch {
            val result = postRepository.toggleBookmark(postId)
            result.onFailure {
                _uiState.update { it.copy(posts = currentPosts) }
            }
        }
    }

    fun repost(post: Post, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = postRepository.repost(post.id)
            result.onSuccess {
                onSuccess()
                refresh()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Impossible de repartager cette publication")
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val currentPosts = _uiState.value.posts
        val updatedPosts = currentPosts.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLikedByCurrentUser
                val newLikesCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLikedByCurrentUser = newLiked, likesCount = newLikesCount)
            } else post
        }
        _uiState.update { it.copy(posts = updatedPosts) }

        viewModelScope.launch {
            val result = postRepository.toggleLike(postId)
            result.onFailure {
                _uiState.update { it.copy(posts = currentPosts) }
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
        private val messageRepository: MessageRepository,
        private val postRepository: PostRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(targetUserId, userRepository, messageRepository, postRepository) as T
        }
    }
}
