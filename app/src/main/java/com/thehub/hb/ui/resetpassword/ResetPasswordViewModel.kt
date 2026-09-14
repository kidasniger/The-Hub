package com.thehub.hb.ui.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isSubmitted: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null
)

sealed interface ResetPasswordNavigationEvent {
    data object NavigateToLogin : ResetPasswordNavigationEvent
    data class NavigateToConfirmPassword(val oobCode: String) : ResetPasswordNavigationEvent
}

class ResetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ResetPasswordNavigationEvent>()
    val events: SharedFlow<ResetPasswordNavigationEvent> = _events.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun sendResetEmail() {
        val email = _uiState.value.email.trim()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        if (email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email requis.") }
            return
        }
        if (!emailRegex.matches(email)) {
            _uiState.update { it.copy(emailError = "Format d'email invalide.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                _uiState.update { it.copy(isSubmitted = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(generalError = throwable.localizedMessage ?: "Impossible d'envoyer l'email de réinitialisation.")
                }
            }
        }
    }
}
