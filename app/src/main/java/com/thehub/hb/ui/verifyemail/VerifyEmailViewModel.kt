package com.thehub.hb.ui.verifyemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.thehub.hb.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyEmailUiState(
    val email: String = "",
    val cooldownSeconds: Int = 0,
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
    val canRetrySend: Boolean = false
)

sealed interface VerifyEmailNavigationEvent {
    data object NavigateToCompleteProfile : VerifyEmailNavigationEvent
    data object NavigateToFeed : VerifyEmailNavigationEvent
    data object NavigateToLogin : VerifyEmailNavigationEvent
}

class VerifyEmailViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerifyEmailUiState())
    val uiState: StateFlow<VerifyEmailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VerifyEmailNavigationEvent>()
    val events: SharedFlow<VerifyEmailNavigationEvent> = _events.asSharedFlow()

    private var cooldownJob: Job? = null

    init {
        val user = authRepository.currentFirebaseUser
        _uiState.update { it.copy(email = user?.email ?: "") }

        if (!authRepository.lastVerificationEmailSentSuccessfully) {
            val err = authRepository.lastVerificationEmailError
            val msg = if (err != null) parseErrorMessage(err) else "L'envoi initial de l'email a échoué. Veuillez réessayer."
            _uiState.update {
                it.copy(
                    errorMessage = msg,
                    canRetrySend = true,
                    cooldownSeconds = 0
                )
            }
        } else {
            startCooldown(60)
        }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.update { it.copy(cooldownSeconds = i) }
                delay(1000)
            }
            _uiState.update { it.copy(cooldownSeconds = 0) }
        }
    }

    fun checkEmailVerification() {
        _uiState.update { it.copy(isChecking = true, errorMessage = null, canRetrySend = false) }

        viewModelScope.launch {
            val result = authRepository.reloadUser()
            _uiState.update { it.copy(isChecking = false) }

            result.onSuccess { user ->
                if (user != null && user.isEmailVerified) {
                    // Check if profile is complete in Firestore
                    val profileResult = authRepository.getUserProfile(user.uid)
                    val profile = profileResult.getOrNull()
                    if (profile?.displayName.isNullOrBlank()) {
                        _events.emit(VerifyEmailNavigationEvent.NavigateToCompleteProfile)
                    } else {
                        _events.emit(VerifyEmailNavigationEvent.NavigateToFeed)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Email pas encore validé. N'oublie pas de vérifier le dossier spam.",
                            canRetrySend = false
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        errorMessage = throwable.localizedMessage ?: "Erreur lors de la vérification.",
                        canRetrySend = false
                    )
                }
            }
        }
    }

    fun resendVerificationEmail() {
        if (_uiState.value.cooldownSeconds > 0) return

        _uiState.update {
            it.copy(
                isResending = true,
                errorMessage = null,
                snackbarMessage = null,
                canRetrySend = false
            )
        }
        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            _uiState.update { it.copy(isResending = false) }

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        snackbarMessage = "Email renvoyé",
                        errorMessage = null,
                        canRetrySend = false
                    )
                }
                startCooldown(60)
            }.onFailure { throwable ->
                val errorMsg = parseErrorMessage(throwable)
                _uiState.update {
                    it.copy(
                        errorMessage = errorMsg,
                        canRetrySend = true
                    )
                }
            }
        }
    }

    private fun parseErrorMessage(throwable: Throwable): String {
        val message = throwable.message ?: ""
        return when {
            throwable is FirebaseNetworkException ||
            message.contains("network", ignoreCase = true) ||
            message.contains("connexion", ignoreCase = true) ||
            message.contains("offline", ignoreCase = true) ||
            message.contains("interrupted", ignoreCase = true) -> {
                "Erreur réseau : vérifiez votre connexion internet."
            }
            message.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) ||
            message.contains("too-many-requests", ignoreCase = true) ||
            message.contains("quota", ignoreCase = true) ||
            message.contains("blocked all requests", ignoreCase = true) -> {
                "Quota Firebase dépassé : veuillez patienter un moment avant de renvoyer un email."
            }
            else -> throwable.localizedMessage ?: "Impossible d'envoyer l'email."
        }
    }

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun signOut() {
        authRepository.signOut()
        viewModelScope.launch {
            _events.emit(VerifyEmailNavigationEvent.NavigateToLogin)
        }
    }
}

