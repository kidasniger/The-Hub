package com.thehub.hb.ui.messenger.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    private val _editingImageUrl = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _olderMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _optimisticMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _isLoadingOlder = MutableStateFlow(false)
    private val _hasMoreOlderMessages = MutableStateFlow(true)

    private var oldestMessageId: String? = null
    private var typingJob: Job? = null

    private val conversationFlow = messageRepository.getConversation(conversationId)
    private val latestMessages = messageRepository.getLatestMessages(conversationId)
    private val reactionsFlow = messageRepository.observeMessageReactions(conversationId)

    private val otherUserIdFlow = conversationFlow
        .map { it?.getOtherParticipantId(currentUserId).orEmpty() }
        .distinctUntilChanged()

    private val typingFlow = otherUserIdFlow.flatMapLatest { otherId ->
        if (otherId.isBlank()) kotlinx.coroutines.flow.flowOf(false)
        else messageRepository.observeTyping(conversationId, otherId)
    }

    private val presenceFlow = otherUserIdFlow.flatMapLatest { otherId ->
        if (otherId.isBlank()) kotlinx.coroutines.flow.flowOf(PresenceState())
        else messageRepository.observePresence(otherId)
    }

    private val mergedMessagesFlow = combine(
        latestMessages,
        _olderMessages,
        _optimisticMessages
    ) { latest, older, optimistic ->
        if (oldestMessageId == null && latest.size >= PAGE_SIZE) {
            oldestMessageId = latest.firstOrNull()?.id
        }
        (older + latest + optimistic)
            .associateBy { it.id }
            .values
            .sortedWith(
                compareBy<Message> { it.createdAt.seconds }
                    .thenBy { it.createdAt.nanoseconds }
                    .thenBy { it.id }
            )
    }

    private val conversationInputFlow = combine(
        _inputText,
        _isSending,
        _selectedImageUri,
        _selectedImageBytes,
        _replyingTo,
        _editingMessageId,
        _isLoadingOlder
    ) { inputText, isSending, selectedUri, selectedBytes, replying, editingId, loadingOlder ->
        ComposerState(
            inputText,
            isSending,
            selectedUri,
            selectedBytes,
            replying,
            editingId,
            loadingOlder
        )
    }

    val uiState: StateFlow<ChatUiState> = combine(
        conversationFlow,
        mergedMessagesFlow,
        reactionsFlow,
        typingFlow,
        presenceFlow,
        conversationInputFlow,
        _hasMoreOlderMessages,
        _errorMessage
    ) { conv, messages, reactions, typing, presence, input, hasMore, error ->
        val otherId = conv?.getOtherParticipantId(currentUserId).orEmpty()
        val otherInfo = conv?.getOtherParticipantInfo(currentUserId) ?: ParticipantInfo()

        ChatUiState(
            conversation = conv,
            otherParticipantInfo = otherInfo,
            otherUserId = otherId,
            messages = messages,
            reactions = reactions,
            isOtherTyping = typing,
            presence = presence,
            isLoading = conv == null && messages.isEmpty(),
            isSending = input.isSending,
            inputText = input.inputText,
            selectedImageUri = input.selectedUri,
            selectedImageBytes = input.selectedBytes,
            replyingTo = input.replyingTo,
            editingMessageId = input.editingId,
            hasMoreOlderMessages = hasMore,
            isLoadingOlderMessages = input.loadingOlder,
            errorMessage = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ChatUiState()
    )

    private data class ComposerState(
        val inputText: String,
        val isSending: Boolean,
        val selectedUri: Uri?,
        val selectedBytes: ByteArray?,
        val replyingTo: ChatComposerMessage?,
        val editingId: String?,
        val loadingOlder: Boolean
    )

    init {
        viewModelScope.launch {
            latestMessages.collect { messages ->
                val incoming = messages.filter {
                    it.senderId != currentUserId &&
                        it.status == Message.STATUS_SENT &&
                        !it.isDeleted
                }.map { it.id }

                if (incoming.isNotEmpty()) {
                    messageRepository.markDelivered(conversationId, incoming)
                }
                markAsRead()

                if (messages.size < PAGE_SIZE && _olderMessages.value.isEmpty()) {
                    _hasMoreOlderMessages.value = false
                }
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
        _editingImageUrl.value = null
        _replyingTo.value = ChatComposerMessage(
            messageId = message.id,
            text = message.text,
            imageUrl = message.imageUrl
        )
    }

    fun startEdit(message: Message) {
        if (!message.isSentBy(currentUserId) || message.isDeleted) return
        _replyingTo.value = null
        _editingMessageId.value = message.id
        _editingImageUrl.value = message.imageUrl
        _inputText.value = message.text.orEmpty()
        _selectedImageUri.value = null
        _selectedImageBytes.value = null
    }

    fun cancelComposerMode() {
        _replyingTo.value = null
        _editingMessageId.value = null
        _editingImageUrl.value = null
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
                .onFailure {
                    _errorMessage.value = it.message ?: "Impossible de supprimer le message."
                }
        }
    }

    fun loadOlderMessages() {
        if (_isLoadingOlder.value || !_hasMoreOlderMessages.value) return

        viewModelScope.launch {
            _isLoadingOlder.value = true
            val result = messageRepository.loadOlderMessages(
                conversationId = conversationId,
                beforeMessageId = oldestMessageId,
                limit = PAGE_SIZE
            )
            result.fold(
                onSuccess = { page ->
                    if (page.messages.isEmpty()) {
                        _hasMoreOlderMessages.value = false
                    } else {
                        _olderMessages.update { current ->
                            (current + page.messages).distinctBy { it.id }
                        }
                        oldestMessageId = page.messages.firstOrNull()?.id
                        if (!page.hasMore) {
                            _hasMoreOlderMessages.value = false
                        }
                    }
                },
                onFailure = {
                    _errorMessage.value = it.message
                        ?: "Impossible de charger les anciens messages."
                }
            )
            _isLoadingOlder.value = false
        }
    }

    fun retryMessage(message: Message) {
        if (message.status != Message.STATUS_FAILED) return

        _optimisticMessages.update { list ->
            list.map {
                if (it.id == message.id) it.copy(status = Message.STATUS_PENDING) else it
            }
        }
        sendOptimisticMessage(
            localMessage = message.copy(status = Message.STATUS_PENDING),
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
            editCurrentMessage(editingId, text, _editingImageUrl.value)
            return
        }

        if (text.isBlank() && imageBytes == null) return
        if (_isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            var imageUrl: String? = _editingImageUrl.value
            if (imageBytes != null) {
                val uploadResult = messageRepository.uploadMessageImage(imageBytes)
                if (uploadResult.isFailure) {
                    _isSending.value = false
                    _errorMessage.value = uploadResult.exceptionOrNull()?.message
                        ?: "Échec du téléchargement de l'image."
                    return@launch
                }
                imageUrl = uploadResult.getOrNull()
            }

            val optimistic = Message(
                id = "local_" + System.currentTimeMillis(),
                senderId = currentUserId,
                text = text.ifBlank { null },
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
                    _optimisticMessages.update { list ->
                        list.filterNot { it.id == localMessage.id }
                    }
                },
                onFailure = { err ->
                    _optimisticMessages.update { list ->
                        list.map {
                            if (it.id == localMessage.id) {
                                it.copy(status = Message.STATUS_FAILED)
                            } else {
                                it
                            }
                        }
                    }
                    _errorMessage.value = err.message ?: "Impossible d'envoyer le message."
                }
            )
            _isSending.value = false
        }
    }

    private fun editCurrentMessage(
        messageId: String,
        text: String,
        imageUrl: String?
    ) {
        viewModelScope.launch {
            _isSending.value = true
            val result = messageRepository.editMessage(
                conversationId = conversationId,
                messageId = messageId,
                text = text,
                imageUrl = imageUrl
            )
            result.onSuccess {
                cancelComposerMode()
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
        _editingImageUrl.value = null
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
