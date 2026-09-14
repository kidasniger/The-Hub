package com.thehub.hb.ui.messenger.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.model.ParticipantInfo
import com.thehub.hb.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversation: Conversation? = null,
    val otherParticipantInfo: ParticipantInfo = ParticipantInfo(),
    val otherUserId: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val inputText: String = "",
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val errorMessage: String? = null
)

class ChatViewModel(
    val conversationId: String,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val currentUserId: String = messageRepository.currentUserId ?: ""

    private val _inputState = MutableStateFlow(
        ChatInputState(
            inputText = "",
            selectedImageUri = null,
            selectedImageBytes = null,
            isSending = false,
            errorMessage = null
        )
    )

    private data class ChatInputState(
        val inputText: String,
        val selectedImageUri: Uri?,
        val selectedImageBytes: ByteArray?,
        val isSending: Boolean,
        val errorMessage: String?
    )

    val uiState: StateFlow<ChatUiState> = combine(
        messageRepository.getConversation(conversationId),
        messageRepository.getMessages(conversationId),
        _inputState
    ) { conv, messages, input ->
        val otherId = conv?.getOtherParticipantId(currentUserId) ?: ""
        val otherInfo = conv?.getOtherParticipantInfo(currentUserId) ?: ParticipantInfo()

        ChatUiState(
            conversation = conv,
            otherParticipantInfo = otherInfo,
            otherUserId = otherId,
            messages = messages,
            isLoading = conv == null && messages.isEmpty(),
            isSending = input.isSending,
            inputText = input.inputText,
            selectedImageUri = input.selectedImageUri,
            selectedImageBytes = input.selectedImageBytes,
            errorMessage = input.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        markAsRead()
        viewModelScope.launch {
            messageRepository.getMessages(conversationId).collect {
                markAsRead()
            }
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            messageRepository.markAsRead(conversationId)
        }
    }

    fun onInputTextChanged(text: String) {
        _inputState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun onImageSelected(uri: Uri?, bytes: ByteArray?) {
        _inputState.update {
            it.copy(
                selectedImageUri = uri,
                selectedImageBytes = bytes,
                errorMessage = null
            )
        }
    }

    fun onRemoveSelectedImage() {
        _inputState.update {
            it.copy(
                selectedImageUri = null,
                selectedImageBytes = null
            )
        }
    }

    fun sendMessage() {
        val current = _inputState.value
        val text = current.inputText.trim()
        val imageBytes = current.selectedImageBytes

        if (text.isBlank() && imageBytes == null) return
        if (current.isSending) return

        viewModelScope.launch {
            _inputState.update { it.copy(isSending = true, errorMessage = null) }

            var imageUrl: String? = null
            if (imageBytes != null) {
                val uploadResult = messageRepository.uploadMessageImage(imageBytes)
                if (uploadResult.isFailure) {
                    _inputState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = uploadResult.exceptionOrNull()?.message
                                ?: "Échec du téléchargement de l'image."
                        )
                    }
                    return@launch
                }
                imageUrl = uploadResult.getOrNull()
            }

            val result = messageRepository.sendMessage(
                conversationId = conversationId,
                text = if (text.isNotBlank()) text else null,
                imageUrl = imageUrl
            )

            result.fold(
                onSuccess = {
                    _inputState.update {
                        it.copy(
                            inputText = "",
                            selectedImageUri = null,
                            selectedImageBytes = null,
                            isSending = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _inputState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = err.message ?: "Impossible d'envoyer le message."
                        )
                    }
                }
            )
        }
    }

    fun clearErrorMessage() {
        _inputState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val conversationId: String,
        private val messageRepository: MessageRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(conversationId, messageRepository) as T
        }
    }
}
