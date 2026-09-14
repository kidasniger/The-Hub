package com.thehub.hb.ui.messenger.newmessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewMessageUiState {
    object Idle : NewMessageUiState
    object Loading : NewMessageUiState
    data class Success(val users: List<User>, val isSearch: Boolean) : NewMessageUiState
    data class Creating(val targetUserId: String) : NewMessageUiState
    data class Error(val message: String) : NewMessageUiState
}

class NewMessageViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<NewMessageUiState>(NewMessageUiState.Loading)
    val uiState: StateFlow<NewMessageUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInitialUsers()
    }

    fun loadInitialUsers() {
        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading
            val result = messageRepository.searchUsers("")
            result.fold(
                onSuccess = { users ->
                    _uiState.value = NewMessageUiState.Success(users = users, isSearch = false)
                },
                onFailure = { err ->
                    val msg = err.message ?: ""
                    val friendly = if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("insufficient permissions", ignoreCase = true)) {
                        "Erreur d'accès Firestore : veuillez configurer les règles de sécurité dans la console Firebase."
                    } else {
                        err.message ?: "Erreur de chargement"
                    }
                    _uiState.value = NewMessageUiState.Error(friendly)
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.value = NewMessageUiState.Loading
            val result = messageRepository.searchUsers(query)
            result.fold(
                onSuccess = { users ->
                    _uiState.value = NewMessageUiState.Success(
                        users = users,
                        isSearch = query.isNotBlank()
                    )
                },
                onFailure = { err ->
                    val msg = err.message ?: ""
                    val friendly = if (msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("insufficient permissions", ignoreCase = true)) {
                        "Erreur d'accès Firestore : veuillez configurer les règles de sécurité dans la console Firebase."
                    } else {
                        err.message ?: "Erreur de recherche"
                    }
                    _uiState.value = NewMessageUiState.Error(friendly)
                }
            )
        }
    }

    fun onUserSelected(user: User, onConversationReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Creating(user.uid)
            val result = messageRepository.getOrCreateConversation(user.uid)
            result.fold(
                onSuccess = { conversation ->
                    onConversationReady(conversation.id)
                },
                onFailure = { err ->
                    _uiState.value = NewMessageUiState.Error(
                        err.message ?: "Impossible de créer la conversation."
                    )
                }
            )
        }
    }

    class Factory(
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewMessageViewModel(messageRepository) as T
        }
    }
}
