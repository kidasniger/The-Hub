package com.thehub.hb.ui.confirmpassword

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

data class ConfirmPasswordUiState(
    val oobCode: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val codeError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

sealed interface ConfirmPasswordNavigationEvent {
    data object NavigateToLogin : ConfirmPasswordNavigationEvent
}

class ConfirmPasswordViewModel(
    private val authRepository: AuthRepository,
    initialCode: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfirmPasswordUiState(oobCode = initialCode))
    val uiState: StateFlow<ConfirmPasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConfirmPasswordNavigationEvent>()
    val events: SharedFlow<ConfirmPasswordNavigationEvent> = _events.asSharedFlow()

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(oobCode = code, codeError = null, generalError = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPassword = password, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmPassword = confirm, confirmPasswordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun confirmReset() {
        val state = _uiState.value
        var isValid = true

        if (state.oobCode.isBlank()) {
            _uiState.update { it.copy(codeError = "Code de réinitialisation requis.") }
            isValid = false
        }

        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(passwordError = "Minimum 8 caractères requis.") }
            isValid = false
        }

        if (state.confirmPassword != state.newPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Les mots de passe ne correspondent pas.") }
            isValid = false
        }

        if (!isValid) return

        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.confirmPasswordReset(state.oobCode.trim(), state.newPassword)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess {
                _uiState.update { it.copy(isSuccess = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(generalError = throwable.localizedMessage ?: "Code invalide ou expiré.")
                }
            }
        }
    }
}
