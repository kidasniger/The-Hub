package com.thehub.hb.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FollowListType {
    FOLLOWERS,
    FOLLOWING
}

data class FollowListUiState(
    val isLoading: Boolean = true,
    val type: FollowListType = FollowListType.FOLLOWERS,
    val users: List<User> = emptyList(),
    val searchQuery: String = "",
    val followingMap: Map<String, Boolean> = emptyMap(),
    val actionLoadingMap: Map<String, Boolean> = emptyMap(),
    val errorMessage: String? = null
) {
    val filteredUsers: List<User>
        get() {
            if (searchQuery.isBlank()) return users
            val q = searchQuery.trim().lowercase()
            return users.filter {
                it.username.lowercase().contains(q) ||
                        (it.displayName?.lowercase()?.contains(q) == true)
            }
        }
}

class FollowListViewModel(
    private val targetUserId: String,
    private val type: FollowListType,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState(type = type))
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = userRepository.currentUserId

    init {
        loadList()
    }

    fun loadList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (type == FollowListType.FOLLOWERS) {
                userRepository.getFollowers(targetUserId)
            } else {
                userRepository.getFollowing(targetUserId)
            }

            val users = result.getOrDefault(emptyList())
            com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(users.map { it.uid })

            // Resolve follow status in parallel so large follower/following lists
            // do not wait on one Firestore read at a time.
            val currentUid = userRepository.currentUserId
            val followMap = if (currentUid != null) {
                coroutineScope {
                    users
                        .filter { it.uid != currentUid }
                        .map { user ->
                            async {
                                user.uid to userRepository.checkIsFollowing(user.uid).getOrDefault(false)
                            }
                        }
                        .awaitAll()
                        .toMap()
                }
            } else {
                emptyMap()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    users = users,
                    followingMap = followMap
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFollowUser(targetUid: String) {
        val currentUid = currentUserId ?: return
        if (targetUid == currentUid) return

        val isCurrentlyFollowing = _uiState.value.followingMap[targetUid] == true

        viewModelScope.launch {
            _uiState.update {
                it.copy(actionLoadingMap = it.actionLoadingMap + (targetUid to true))
            }

            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(targetUid)
            } else {
                userRepository.followUser(targetUid)
            }

            _uiState.update { current ->
                val newLoading = current.actionLoadingMap - targetUid
                if (result.isSuccess) {
                    current.copy(
                        actionLoadingMap = newLoading,
                        followingMap = current.followingMap + (targetUid to !isCurrentlyFollowing)
                    )
                } else {
                    current.copy(
                        actionLoadingMap = newLoading,
                        errorMessage = "Échec de l'action d'abonnement"
                    )
                }
            }
        }
    }

    class Factory(
        private val targetUserId: String,
        private val type: FollowListType,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FollowListViewModel(targetUserId, type, userRepository) as T
        }
    }
}
