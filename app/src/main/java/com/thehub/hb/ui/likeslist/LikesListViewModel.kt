package com.thehub.hb.ui.likeslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.LikerUser
import com.thehub.hb.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LikesListUiState {
    data object Loading : LikesListUiState
    data class Success(val likers: List<LikerUser>) : LikesListUiState
    data class Error(val message: String) : LikesListUiState
}

class LikesListViewModel(
    private val postId: String,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LikesListUiState>(LikesListUiState.Loading)
    val uiState: StateFlow<LikesListUiState> = _uiState.asStateFlow()

    init {
        loadLikers()
    }

    fun loadLikers() {
        viewModelScope.launch {
            _uiState.value = LikesListUiState.Loading
            val result = postRepository.getLikers(postId)
            result.onSuccess { list ->
                _uiState.value = LikesListUiState.Success(list)
            }.onFailure { error ->
                _uiState.value = LikesListUiState.Error(
                    error.message ?: "Impossible de charger la liste des mentions J'aime."
                )
            }
        }
    }
}
