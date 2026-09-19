package com.thehub.hb.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.GoogleSignInResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val generalError: String? = null,
    val isGoogleLoading: Boolean = false
)

sealed interface LoginNavigationEvent {
    data object NavigateToFeed : LoginNavigationEvent
    data object NavigateToCompleteProfile : LoginNavigationEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginNavigationEvent>()
    val events: SharedFlow<LoginNavigationEvent> = _events.asSharedFlow()

    fun signInWithGoogle(context: Context) {
        if (_uiState.value.isGoogleLoading) return

        _uiState.update {
            it.copy(
                isGoogleLoading = true,
                generalError = null
            )
        }

        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(context)) {
                is GoogleSignInResult.Success -> {
                    _uiState.update { it.copy(isGoogleLoading = false) }
                    if (result.shouldCompleteProfile) {
                        _events.emit(LoginNavigationEvent.NavigateToCompleteProfile)
                    } else {
                        _events.emit(LoginNavigationEvent.NavigateToFeed)
                    }
                }

                is GoogleSignInResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            generalError = result.message
                        )
                    }
                }

                GoogleSignInResult.Cancelled -> {
                    _uiState.update { it.copy(isGoogleLoading = false) }
                }
            }
        }
    }
}
