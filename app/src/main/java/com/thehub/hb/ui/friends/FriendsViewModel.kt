package com.thehub.hb.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.SearchRepository
import com.thehub.hb.data.repository.UserCacheRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FriendsTab {
    FRIENDS,
    CONNECTIONS,
    DISCOVER
}

enum class ConnectionsTab {
    FOLLOWERS,
    FOLLOWING
}

data class FriendsUiState(
    val isLoading: Boolean = true,
    val users: List<User> = emptyList(),
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val searchResults: List<User> = emptyList(),
    val selectedTab: FriendsTab = FriendsTab.FRIENDS,
    val connectionsTab: ConnectionsTab = ConnectionsTab.FOLLOWERS,
    val searchQuery: String = "",
    val isSearchLoading: Boolean = false,
    val followingMap: Map<String, Boolean> = emptyMap(),
    val actionLoadingMap: Map<String, Boolean> = emptyMap(),
    val errorMessage: String? = null
) {
    /**
     * Backward-compatible local filtering used by legacy UI tests and callers.
     * The Discover tab now performs remote search through SearchRepository.
     */
    val filteredUsers: List<User>
        get() {
            val query = searchQuery.trim()
            if (query.isBlank()) return users
            return users.filter { user ->
                user.username.contains(query, ignoreCase = true) ||
                    user.displayName.orEmpty().contains(query, ignoreCase = true)
            }
        }
}

class FriendsViewModel(
    private val targetUserId: String,
    private val userRepository: UserRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    val currentUserId: String?
        get() = userRepository.currentUserId

    private val effectiveUserId: String
        get() = targetUserId.ifBlank { currentUserId ?: "" }

    private var searchJob: Job? = null

    init {
        loadRelations()
    }

    fun selectTab(tab: FriendsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectConnectionsTab(tab: ConnectionsTab) {
        _uiState.update { it.copy(connectionsTab = tab) }
    }

    fun loadRelations() {
        val uid = effectiveUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val (friends, followers, following) = coroutineScope {
                val friendsDeferred = async { userRepository.getFriends(uid).getOrDefault(emptyList()) }
                val followersDeferred = async { userRepository.getFollowers(uid).getOrDefault(emptyList()) }
                val followingDeferred = async { userRepository.getFollowing(uid).getOrDefault(emptyList()) }
                Triple(
                    friendsDeferred.await(),
                    followersDeferred.await(),
                    followingDeferred.await()
                )
            }

            val targets = (friends + followers + following)
                .filter { it.uid.isNotBlank() && it.uid != currentUserId }
                .distinctBy { it.uid }

            val followingStates = coroutineScope {
                targets.map { user ->
                    async {
                        user.uid to userRepository.checkIsFollowing(user.uid).getOrDefault(false)
                    }
                }.awaitAll().toMap()
            }

            UserCacheRepository.getInstance().observeUsers(targets.map { it.uid })

            _uiState.update {
                it.copy(
                    isLoading = false,
                    users = friends,
                    followers = followers,
                    following = following,
                    followingMap = followingStates
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearchLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearchLoading = true, errorMessage = null) }

            val result = searchRepository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    val curId = currentUserId
                    val results = users
                        .filter { it.uid.isNotBlank() && it.uid != curId }
                        .distinctBy { it.uid }

                    val followingStates = coroutineScope {
                        results.map { user ->
                            async {
                                user.uid to userRepository.checkIsFollowing(user.uid).getOrDefault(false)
                            }
                        }.awaitAll().toMap()
                    }

                    UserCacheRepository.getInstance().observeUsers(results.map { it.uid })

                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            followingMap = it.followingMap + followingStates,
                            isSearchLoading = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            searchResults = emptyList(),
                            isSearchLoading = false,
                            errorMessage = err.message ?: "Erreur de recherche."
                        )
                    }
                }
            )
        }
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
        private val userRepository: UserRepository,
        private val searchRepository: SearchRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FriendsViewModel(
                targetUserId = targetUserId,
                userRepository = userRepository,
                searchRepository = searchRepository
            ) as T
        }
    }
}
