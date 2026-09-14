package com.thehub.hb.ui.messenger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface MessengerUiState {
    object Loading : MessengerUiState
    data class Success(
        val conversations: List<Conversation>,
        val filteredConversations: List<Conversation>,
        val searchQuery: String = "",
        val currentUserId: String = ""
    ) : MessengerUiState
    data class Error(val message: String) : MessengerUiState
}

class MessengerViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {

    val currentUserId: String = messageRepository.currentUserId ?: ""

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val uiState: StateFlow<MessengerUiState> = combine(
        messageRepository.getConversations(),
        _searchQuery
    ) { conversations, query ->
        val filtered = if (query.isBlank()) {
            conversations
        } else {
            val q = query.trim().lowercase()
            conversations.filter { conv ->
                val otherInfo = conv.getOtherParticipantInfo(currentUserId)
                otherInfo.username.lowercase().contains(q) ||
                        (otherInfo.displayName?.lowercase()?.contains(q) == true) ||
                        conv.lastMessageText.lowercase().contains(q)
            }
        }
        MessengerUiState.Success(
            conversations = conversations,
            filteredConversations = filtered,
            searchQuery = query,
            currentUserId = currentUserId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MessengerUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    class Factory(
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessengerViewModel(messageRepository) as T
        }
    }
}
