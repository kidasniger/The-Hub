package com.thehub.hb.ui.signup

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.FirestoreCreationException
import com.thehub.hb.data.repository.GoogleSignInResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false
)

sealed interface SignUpNavigationEvent {
    data object NavigateToVerifyEmail : SignUpNavigationEvent
    data object NavigateToLogin : SignUpNavigationEvent
    data object NavigateToTerms : SignUpNavigationEvent
    data object NavigateToCompleteProfile : SignUpNavigationEvent
    data object NavigateToFeed : SignUpNavigationEvent
}

class SignUpViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _events = Channel<SignUpNavigationEvent>(Channel.BUFFERED)
    val events: Flow<SignUpNavigationEvent> = _events.receiveAsFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onUsernameChange(username: String) {
        // Enforce lowercase alphanumeric and underscores/dots
        val clean = username.lowercase().replace("[^a-z0-9_.]".toRegex(), "")
        _uiState.update { it.copy(username = clean, usernameError = null, generalError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onConfirmPasswordChange(confirm: String) {
        _uiState.update { it.copy(confirmPassword = confirm, confirmPasswordError = null, generalError = null) }
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted, termsError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    private fun validate(): Boolean {
        var isValid = true
        val state = _uiState.value

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email requis.") }
            isValid = false
        } else if (!emailRegex.matches(state.email.trim())) {
            _uiState.update { it.copy(emailError = "Format d'email invalide.") }
            isValid = false
        }

        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Nom d'utilisateur requis.") }
            isValid = false
        } else if (state.username.length < 3) {
            _uiState.update { it.copy(usernameError = "Minimum 3 caractères.") }
            isValid = false
        }

        if (state.password.length < 8) {
            _uiState.update { it.copy(passwordError = "Le mot de passe doit comporter au moins 8 caractères.") }
            isValid = false
        }

        if (state.confirmPassword != state.password) {
            _uiState.update { it.copy(confirmPasswordError = "Les mots de passe ne correspondent pas.") }
            isValid = false
        }

        if (!state.termsAccepted) {
            _uiState.update { it.copy(termsError = "Tu dois accepter les conditions pour continuer.") }
            isValid = false
        }

        return isValid
    }

    fun signUp() {
        if (!validate()) return

        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            try {
                Log.d("SignUpViewModel", "Lancement de la création de compte pour ${state.email.trim()}")
                val result = authRepository.signUp(
                    email = state.email.trim(),
                    username = state.username.trim(),
                    password = state.password
                )

                result.onSuccess {
                    Log.d("SignUpViewModel", "Inscription réussie, émission de l'événement NavigateToVerifyEmail")
                    _events.send(SignUpNavigationEvent.NavigateToVerifyEmail)
                }.onFailure { throwable ->
                    Log.e("SignUpViewModel", "Échec de l'inscription: ${throwable.message}", throwable)
                    val message = when (throwable) {
                        is FirestoreCreationException -> throwable.message ?: "Échec d'enregistrement du profil Firestore."
                        is FirebaseAuthUserCollisionException -> "Cet email est déjà utilisé par un autre compte."
                        is FirebaseAuthWeakPasswordException -> "Le mot de passe est trop faible."
                        else -> throwable.localizedMessage ?: "Erreur lors de l'inscription."
                    }
                    if (message.contains("nom d'utilisateur", ignoreCase = true)) {
                        _uiState.update { it.copy(usernameError = message) }
                    } else {
                        _uiState.update { it.copy(generalError = message) }
                    }
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Exception inattendue lors de l'inscription", e)
                _uiState.update {
                    it.copy(generalError = e.localizedMessage ?: "Une erreur inattendue est survenue.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                Log.d("SignUpViewModel", "Fin de la tentative d'inscription, bouton réactivé (isLoading=false)")
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.update { it.copy(isGoogleLoading = true, generalError = null) }

        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(context)) {
                is GoogleSignInResult.Success -> {
                    _uiState.update { it.copy(isGoogleLoading = false) }
                    if (result.shouldCompleteProfile) {
                        _events.send(SignUpNavigationEvent.NavigateToCompleteProfile)
                    } else {
                        _events.send(SignUpNavigationEvent.NavigateToFeed)
                    }
                }
                is GoogleSignInResult.Error -> {
                    _uiState.update { it.copy(isGoogleLoading = false, generalError = result.message) }
                }
                is GoogleSignInResult.Cancelled -> {
                    _uiState.update { it.copy(isGoogleLoading = false) }
                }
            }
        }
    }
}

