package com.thehub.hb.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isDeletingAccount: Boolean = false,
    val deleteError: String? = null,
    val userEmail: String = ""
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(userEmail = authRepository.currentFirebaseUser?.email ?: "")
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val notifLikes: StateFlow<Boolean> = dataStoreManager.notifLikesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifComments: StateFlow<Boolean> = dataStoreManager.notifCommentsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifFollows: StateFlow<Boolean> = dataStoreManager.notifFollowsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifMessages: StateFlow<Boolean> = dataStoreManager.notifMessagesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentThemeMode: StateFlow<com.thehub.hb.ui.theme.AppThemeMode> = dataStoreManager.appThemeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.thehub.hb.ui.theme.AppThemeMode.DARK)

    val currentLanguage: StateFlow<com.thehub.hb.ui.theme.AppLanguage> = dataStoreManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.thehub.hb.ui.theme.AppLanguage.FR)

    fun setThemeMode(mode: com.thehub.hb.ui.theme.AppThemeMode) {
        viewModelScope.launch { dataStoreManager.setAppThemeMode(mode) }
    }

    fun setLanguage(language: com.thehub.hb.ui.theme.AppLanguage) {
        viewModelScope.launch { dataStoreManager.setAppLanguage(language) }
    }

    fun setNotifLikes(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setNotifLikes(enabled) }
    }

    fun setNotifComments(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setNotifComments(enabled) }
    }

    fun setNotifFollows(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setNotifFollows(enabled) }
    }

    fun setNotifMessages(enabled: Boolean) {
        viewModelScope.launch { dataStoreManager.setNotifMessages(enabled) }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSuccess()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, deleteError = null) }
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                _uiState.update { it.copy(isDeletingAccount = false) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteError = result.exceptionOrNull()?.message ?: "Impossible de supprimer le compte"
                    )
                }
            }
        }
    }

    fun clearDeleteError() {
        _uiState.update { it.copy(deleteError = null) }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(authRepository, userRepository, dataStoreManager) as T
        }
    }
}
