package com.thehub.hb.ui.messenger.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.model.MessageReaction
import com.thehub.hb.data.model.ParticipantInfo
import com.thehub.hb.data.model.PresenceState
import com.thehub.hb.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ChatComposerMessage(
    val messageId: String,
    val text: String?,
    val imageUrl: String?
)

data class ChatUiState(
    val conversation: Conversation? = null,
    val otherParticipantInfo: ParticipantInfo = ParticipantInfo(),
    val otherUserId: String = "",
    val messages: List<Message> = emptyList(),
    val reactions: List<MessageReaction> = emptyList(),
    val isOtherTyping: Boolean = false,
    val presence: PresenceState = PresenceState(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val inputText: String = "",
    val selectedImageUri: Uri? = null,
    val selectedImageBytes: ByteArray? = null,
    val replyingTo: ChatComposerMessage? = null,
    val editingMessageId: String? = null,
    val hasMoreOlderMessages: Boolean = true,
    val isLoadingOlderMessages: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    val conversationId: String,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val currentUserId: String = messageRepository.currentUserId ?: ""

    private val firestoreOperationMutex = Mutex()
    private val _inputText = MutableStateFlow("")
    private val _isSending = MutableStateFlow(false)
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    private val _selectedImageBytes = MutableStateFlow<ByteArray?>(null)
    private val _replyingTo = MutableStateFlow<ChatComposerMessage?>(null)
    private val _editingMessageId = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _olderMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _optimisticMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _isLoadingOlder = MutableStateFlow(false)

    private var olderCursor: com.google.firebase.firestore.DocumentSnapshot? = null
    private var typingJob: Job? = null

    private val latestMessages = messageRepository.getLatestMessages(conversationId)
    private val conversationFlow = messageRepository.getConversation(conversationId)

    val uiState: StateFlow<ChatUiState> = combine(
        conversationFlow,
        latestMessages,
        messageRepository.observeMessageReactions(conversationId),
        _inputText,
        _isSending,
        _selectedImageUri,
        _selectedImageBytes,
        _replyingTo,
        _editingMessageId,
        _olderMessages,
        _optimisticMessages,
        _isLoadingOlder
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val conv = values[0] as Conversation?
        @Suppress("UNCHECKED_CAST")
        val latest = values[1] as List<Message>
        @Suppress("UNCHECKED_CAST")
        val reactions = values[2] as List<MessageReaction>
        val input = values[3] as String
        val isSending = values[4] as Boolean
        val selectedUri = values[5] as Uri?
        val selectedBytes = values[6] as ByteArray?
        val replying = values[7] as ChatComposerMessage?
        val editing = values[8] as String?
        @Suppress("UNCHECKED_CAST")
        val older = values[9] as List<Message>
        @Suppress("UNCHECKED_CAST")
        val optimistic = values[10] as List<Message>
        val loadingOlder = values[11] as Boolean

        val merged = (older + latest + optimistic)
            .associateBy { it.id }
            .values
            .sortedWith(compareBy<Message> { it.createdAt.seconds }.thenBy { it.createdAt.nanoseconds }.thenBy { it.id })

        val otherId = conv?.getOtherParticipantId(currentUserId) ?: ""
        val otherInfo = conv?.getOtherParticipantInfo(currentUserId) ?: ParticipantInfo()

        ChatUiState(
            conversation = conv,
            otherParticipantInfo = otherInfo,
            otherUserId = otherId,
            messages = merged,
            reactions = reactions,
            isOtherTyping = false,
            presence = PresenceState(),
            isLoading = conv == null && merged.isEmpty(),
            isSending = isSending,
            inputText = input,
            selectedImageUri = selectedUri,
            selectedImageBytes = selectedBytes,
            replyingTo = replying,
            editingMessageId = editing,
            hasMoreOlderMessages = olderCursor != null || latest.size >= PAGE_SIZE,
            isLoadingOlderMessages = loadingOlder,
            errorMessage = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    val enhancedUiState: StateFlow<ChatUiState> = combine(
        uiState,
        conversationFlow,
        messageRepository.getLatestMessages(conversationId),
        messageRepository.observeMessageReactions(conversationId)
    ) { state, conv, _, _ ->
        state.copy(
            isOtherTyping = false,
            presence = if (state.otherUserId.isBlank()) {
                PresenceState()
            } else {
                state.presence
            }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatUiState()
    )

    init {
        observeTypingAndPresence()
        observeDeliveryAndRead()
    }

    private fun observeTypingAndPresence() {
        viewModelScope.launch {
            conversationFlow.collect { conv ->
                val otherId = conv?.getOtherParticipantId(currentUserId).orEmpty()
                if (otherId.isBlank()) return@collect

                launch {
                    messageRepository.observeTyping(conversationId, otherId).collect { typing ->
                        _typingState.value = typing
                    }
                }
                launch {
                    messageRepository.observePresence(otherId).collect { presence ->
                        _presenceState.value = presence
                    }
                }
            }
        }
    }

    private val _typingState = MutableStateFlow(false)
    private val _presenceState = MutableStateFlow(PresenceState())

    val realtimeUiState: StateFlow<ChatUiState> = combine(
        uiState,
        _typingState,
        _presenceState
    ) { state, typing, presence ->
        state.copy(
            isOtherTyping = typing,
            presence = presence,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatUiState()
    )

    private fun observeDeliveryAndRead() {
        viewModelScope.launch {
            latestMessages.collect { messages ->
                val incoming = messages
                    .filter {
                        it.senderId != currentUserId &&
                            it.status == Message.STATUS_SENT &&
                            !it.isDeleted
                    }
                    .map { it.id }
                if (incoming.isNotEmpty()) {
                    messageRepository.markDelivered(conversationId, incoming)
                }
                markAsRead()
            }
        }
    }

    fun markAsRead() {
        viewModelScope.launch {
            firestoreOperationMutex.withLock {
                messageRepository.markAsRead(conversationId)
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        _errorMessage.value = null
        typingJob?.cancel()
        if (text.isBlank()) {
            viewModelScope.launch { messageRepository.setTyping(conversationId, false) }
        } else {
            viewModelScope.launch { messageRepository.setTyping(conversationId, true) }
            typingJob = viewModelScope.launch {
                delay(1800)
                messageRepository.setTyping(conversationId, false)
            }
        }
    }

    fun onImageSelected(uri: Uri?, bytes: ByteArray?) {
        _selectedImageUri.value = uri
        _selectedImageBytes.value = bytes
        _errorMessage.value = null
    }

    fun onRemoveSelectedImage() {
        _selectedImageUri.value = null
        _selectedImageBytes.value = null
    }

    fun startReply(message: Message) {
        if (message.isDeleted) return
        _editingMessageId.value = null
        _replyingTo.value = ChatComposerMessage(message.id, message.text, message.imageUrl)
    }

    fun startEdit(message: Message) {
        if (!message.isSentBy(currentUserId) || message.isDeleted) return
        _replyingTo.value = null
        _editingMessageId.value = message.id
        _inputText.value = message.text.orEmpty()
        _selectedImageUri.value = null
        _selectedImageBytes.value = null
    }

    fun cancelComposerMode() {
        _replyingTo.value = null
        _editingMessageId.value = null
        _inputText.value = ""
        _selectedImageUri.value = null
        _selectedImageBytes.value = null
        viewModelScope.launch { messageRepository.setTyping(conversationId, false) }
    }

    fun react(messageId: String, emoji: String) {
        viewModelScope.launch {
            val current = uiState.value.reactions.firstOrNull {
                it.messageId == messageId && it.userId == currentUserId
            }
            if (current?.emoji == emoji) {
                messageRepository.removeMessageReaction(conversationId, messageId)
            } else {
                messageRepository.setMessageReaction(conversationId, messageId, emoji)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(conversationId, messageId)
                .onFailure { _errorMessage.value = it.message ?: "Impossible de supprimer le message." }
        }
    }

    fun loadOlderMessages() {
        if (_isLoadingOlder.value || olderCursor == null && latestMessages == null) return
        viewModelScope.launch {
            _isLoadingOlder.value = true
            try {
                val result = messageRepository.loadOlderMessages(conversationId, olderCursor, PAGE_SIZE)
                result.fold(
                    onSuccess = { page ->
                        val current = _olderMessages.value
                        _olderMessages.value = (current + page.messages).distinctBy { it.id }
                        olderCursor = page.cursor
                        if (!page.hasMore) olderCursor = null
                    },
                    onFailure = { _errorMessage.value = it.message ?: "Impossible de charger les anciens messages." }
                )
            } finally {
                _isLoadingOlder.value = false
            }
        }
    }

    fun retryMessage(message: Message) {
        if (message.status != Message.STATUS_FAILED) return
        _optimisticMessages.update { list -> list.map { if (it.id == message.id) it.copy(status = Message.STATUS_PENDING) else it } }
        sendOptimisticMessage(
            localMessage = message,
            text = message.text,
            imageUrl = message.imageUrl,
            replyToMessageId = message.replyToMessageId,
            replyToText = message.replyToText
        )
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        val imageBytes = _selectedImageBytes.value
        val editingId = _editingMessageId.value
        val replying = _replyingTo.value

        if (editingId != null) {
            editCurrentMessage(editingId, text)
            return
        }

        if (text.isBlank() && imageBytes == null) return
        if (_isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            var imageUrl: String? = null
            if (imageBytes != null) {
                val uploadResult = messageRepository.uploadMessageImage(imageBytes)
                if (uploadResult.isFailure) {
                    _isSending.value = false
                    _errorMessage.value = uploadResult.exceptionOrNull()?.message ?: "Échec du téléchargement de l'image."
                    return@launch
                }
                imageUrl = uploadResult.getOrNull()
            }

            val optimistic = Message(
                id = "local_" + System.currentTimeMillis(),
                senderId = currentUserId,
                text = if (text.isBlank()) null else text,
                imageUrl = imageUrl,
                createdAt = com.google.firebase.Timestamp.now(),
                status = Message.STATUS_PENDING,
                replyToMessageId = replying?.messageId,
                replyToText = replying?.text
            )
            _optimisticMessages.update { it + optimistic }
            clearComposerAfterSend()

            sendOptimisticMessage(
                localMessage = optimistic,
                text = optimistic.text,
                imageUrl = optimistic.imageUrl,
                replyToMessageId = optimistic.replyToMessageId,
                replyToText = optimistic.replyToText
            )
        }
    }

    private fun sendOptimisticMessage(
        localMessage: Message,
        text: String?,
        imageUrl: String?,
        replyToMessageId: String?,
        replyToText: String?
    ) {
        viewModelScope.launch {
            val result = firestoreOperationMutex.withLock {
                messageRepository.sendMessage(
                    conversationId = conversationId,
                    text = text,
                    imageUrl = imageUrl,
                    replyToMessageId = replyToMessageId,
                    replyToText = replyToText
                )
            }

            result.fold(
                onSuccess = {
                    _optimisticMessages.update { list -> list.filterNot { it.id == localMessage.id } }
                },
                onFailure = { err ->
                    _optimisticMessages.update { list ->
                        list.map { if (it.id == localMessage.id) it.copy(status = Message.STATUS_FAILED) else it }
                    }
                    _errorMessage.value = err.message ?: "Impossible d'envoyer le message."
                }
            )
            _isSending.value = false
        }
    }

    private fun editCurrentMessage(messageId: String, text: String) {
        viewModelScope.launch {
            _isSending.value = true
            val result = messageRepository.editMessage(conversationId, messageId, text, null)
            result.onSuccess {
                _editingMessageId.value = null
                _inputText.value = ""
            }.onFailure {
                _errorMessage.value = it.message ?: "Impossible de modifier le message."
            }
            _isSending.value = false
        }
    }

    private fun clearComposerAfterSend() {
        _inputText.value = ""
        _selectedImageUri.value = null
        _selectedImageBytes.value = null
        _replyingTo.value = null
        _editingMessageId.value = null
        viewModelScope.launch { messageRepository.setTyping(conversationId, false) }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        typingJob?.cancel()
        viewModelScope.launch { messageRepository.setTyping(conversationId, false) }
        super.onCleared()
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

    companion object {
        const val PAGE_SIZE = 50L
    }
}
