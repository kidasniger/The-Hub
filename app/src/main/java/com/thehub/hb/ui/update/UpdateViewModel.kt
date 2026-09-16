package com.thehub.hb.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thehub.hb.BuildConfig
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.model.UpdateCheckResult
import com.thehub.hb.data.remote.AppUpdateDownloadManager
import com.thehub.hb.data.remote.DownloadStatus
import com.thehub.hb.data.repository.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(
    private val updateRepository: UpdateRepository,
    private val downloadManager: AppUpdateDownloadManager
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val downloadStatus: StateFlow<DownloadStatus> = downloadManager.status

    init {
        // Automatically check for updates on startup
        checkForUpdates(silent = true)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _isChecking.value = true
            when (val result = updateRepository.checkForUpdate(currentVersion = BuildConfig.VERSION_NAME, forceRefresh = !silent)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateInfo.value = result.updateInfo
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateInfo.value = null
                    if (!silent) {
                        _userMessage.value = "The Hub est déjà à jour (v${BuildConfig.VERSION_NAME})."
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (!silent) {
                        _userMessage.value = result.message
                    }
                }
            }
            _isChecking.value = false
        }
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        downloadManager.startDownload(
            apkUrl = updateInfo.apkDownloadUrl,
            fileName = updateInfo.apkFileName,
            versionName = updateInfo.latestVersion
        )
    }

    fun dismiss() {
        _updateInfo.value = null
        updateRepository.dismissUpdate()
    }

    class Factory(
        private val updateRepository: UpdateRepository,
        private val downloadManager: AppUpdateDownloadManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpdateViewModel(updateRepository, downloadManager) as T
        }
    }
}
