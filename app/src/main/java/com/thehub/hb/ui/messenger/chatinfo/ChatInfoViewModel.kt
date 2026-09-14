package com.thehub.hb.ui.messenger.chatinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatInfoUiState(
    val conversation: Conversation? = null,
    val contactUser: User? = null,
    val otherUserId: String = "",
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Message> = emptyList(),
    val isBlocked: Boolean = false,
    val isDeleting: Boolean = false,
    val actionMessage: String? = null,
    val errorMessage: String? = null
)

class ChatInfoViewModel(
    val conversationId: String,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val currentUserId: String = messageRepository.currentUserId ?: ""

    private val _state = MutableStateFlow(
        InternalState(
            contactUser = null,
            isSearching = false,
            searchQuery = "",
            isBlocked = false,
            isDeleting = false,
            actionMessage = null,
            errorMessage = null
        )
    )

    private data class InternalState(
        val contactUser: User?,
        val isSearching: Boolean,
        val searchQuery: String,
        val isBlocked: Boolean,
        val isDeleting: Boolean,
        val actionMessage: String?,
        val errorMessage: String?
    )

    val uiState: StateFlow<ChatInfoUiState> = combine(
        messageRepository.getConversation(conversationId),
        messageRepository.getMessages(conversationId),
        _state
    ) { conv, messages, internal ->
        val otherId = conv?.getOtherParticipantId(currentUserId) ?: ""

        val results = if (internal.isSearching && internal.searchQuery.isNotBlank()) {
            val q = internal.searchQuery.trim().lowercase()
            messages.filter { msg ->
                msg.text?.lowercase()?.contains(q) == true
            }
        } else {
            emptyList()
        }

        ChatInfoUiState(
            conversation = conv,
            contactUser = internal.contactUser,
            otherUserId = otherId,
            isLoading = conv == null,
            isSearching = internal.isSearching,
            searchQuery = internal.searchQuery,
            searchResults = results,
            isBlocked = internal.isBlocked,
            isDeleting = internal.isDeleting,
            actionMessage = internal.actionMessage,
            errorMessage = internal.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatInfoUiState()
    )

    init {
        loadContactDetails()
    }

    private fun loadContactDetails() {
        viewModelScope.launch {
            messageRepository.getConversation(conversationId).collect { conv ->
                if (conv != null) {
                    val otherId = conv.getOtherParticipantId(currentUserId)
                    if (otherId.isNotEmpty()) {
                        val isBlocked = messageRepository.isUserBlocked(otherId)
                        val userResult = messageRepository.getUserProfile(otherId)
                        _state.update {
                            it.copy(
                                contactUser = userResult.getOrNull(),
                                isBlocked = isBlocked
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleSearch(enabled: Boolean) {
        _state.update {
            it.copy(
                isSearching = enabled,
                searchQuery = if (!enabled) "" else it.searchQuery
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun blockUser() {
        val otherId = uiState.value.otherUserId
        if (otherId.isBlank()) return

        viewModelScope.launch {
            val result = messageRepository.blockUser(otherId)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isBlocked = true,
                            actionMessage = "Utilisateur bloqué."
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(errorMessage = err.message ?: "Impossible de bloquer l'utilisateur.")
                    }
                }
            )
        }
    }

    fun deleteConversation(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null) }
            val result = messageRepository.deleteConversation(conversationId)
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isDeleting = false) }
                    onDeleted()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = err.message ?: "Impossible de supprimer la conversation."
                        )
                    }
                }
            )
        }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val conversationId: String,
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatInfoViewModel(conversationId, messageRepository) as T
        }
    }
}
