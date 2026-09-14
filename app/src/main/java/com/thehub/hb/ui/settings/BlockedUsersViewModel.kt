package com.thehub.hb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.BlockedUser
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersUiState(
    val isLoading: Boolean = true,
    val blockedUsers: List<BlockedUser> = emptyList(),
    val actionLoadingMap: Map<String, Boolean> = emptyMap(),
    val errorMessage: String? = null
)

class BlockedUsersViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = userRepository.getBlockedUsers()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    blockedUsers = result.getOrDefault(emptyList())
                )
            }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(actionLoadingMap = it.actionLoadingMap + (targetUid to true))
            }

            val result = userRepository.unblockUser(targetUid)

            _uiState.update { current ->
                val newLoading = current.actionLoadingMap - targetUid
                if (result.isSuccess) {
                    current.copy(
                        actionLoadingMap = newLoading,
                        blockedUsers = current.blockedUsers.filter { it.uid != targetUid }
                    )
                } else {
                    current.copy(
                        actionLoadingMap = newLoading,
                        errorMessage = "Impossible de débloquer cet utilisateur"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BlockedUsersViewModel(userRepository) as T
        }
    }
}
