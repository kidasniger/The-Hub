package com.thehub.hb.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false
)

sealed interface LoginNavigationEvent {
    data object NavigateToFeed : LoginNavigationEvent
    data object NavigateToVerifyEmail : LoginNavigationEvent
    data object NavigateToResetPassword : LoginNavigationEvent
    data object NavigateToSignUp : LoginNavigationEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    initialEmail: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(email = initialEmail))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginNavigationEvent>()
    val events: SharedFlow<LoginNavigationEvent> = _events.asSharedFlow()

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, emailError = null, generalError = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, passwordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun validate(): Boolean {
        var isValid = true
        val state = _uiState.value

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Veuillez entrer votre email.") }
            isValid = false
        } else if (!emailRegex.matches(state.email.trim())) {
            _uiState.update { it.copy(emailError = "Format d'email invalide.") }
            isValid = false
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Veuillez entrer votre mot de passe.") }
            isValid = false
        }

        return isValid
    }

    fun login() {
        if (!validate()) return

        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = authRepository.signIn(state.email.trim(), state.password)
            _uiState.update { it.copy(isLoading = false) }

            result.onSuccess { user ->
                if (user.isEmailVerified) {
                    _events.emit(LoginNavigationEvent.NavigateToFeed)
                } else {
                    _events.emit(LoginNavigationEvent.NavigateToVerifyEmail)
                }
            }.onFailure { throwable ->
                val friendlyMessage = when (throwable) {
                    is FirebaseAuthInvalidUserException -> "Aucun compte n'existe avec cet email."
                    is FirebaseAuthInvalidCredentialsException -> "Email ou mot de passe incorrect."
                    else -> throwable.localizedMessage ?: "Erreur de connexion, veuillez réessayer."
                }
                _uiState.update { it.copy(generalError = friendlyMessage) }
            }
        }
    }
}
