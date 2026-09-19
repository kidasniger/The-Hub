package com.thehub.hb.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.UserCacheRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val isLoading: Boolean = true,
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

class FriendsViewModel(
    private val targetUserId: String,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = userRepository.currentUserId

    private val effectiveUserId: String
        get() = targetUserId.ifBlank { currentUserId ?: "" }

    init {
        loadFriends()
    }

    fun loadFriends() {
        val uid = effectiveUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false, users = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.getFriends(uid)
            val friends = result.getOrDefault(emptyList())

            UserCacheRepository.getInstance().observeUsers(friends.map { it.uid })

            val curId = currentUserId
            val followMap = if (curId == null) {
                emptyMap()
            } else {
                coroutineScope {
                    friends
                        .asSequence()
                        .filter { it.uid != curId }
                        .map { friend ->
                            async {
                                friend.uid to userRepository.checkIsFollowing(friend.uid).getOrDefault(true)
                            }
                        }
                        .toList()
                        .awaitAll()
                        .toMap()
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    users = friends,
                    followingMap = followMap
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleFollowUser(targetUid: String) {
        val curId = currentUserId ?: return
        if (targetUid == curId) return

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
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FriendsViewModel(targetUserId, userRepository) as T
        }
    }
}
