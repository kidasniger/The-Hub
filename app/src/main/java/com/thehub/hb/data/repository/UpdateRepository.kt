package com.thehub.hb.data.repository

import com.thehub.hb.BuildConfig
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.model.UpdateCheckResult
import com.thehub.hb.data.remote.GitHubUpdateService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpdateRepository(
    private val gitHubUpdateService: GitHubUpdateService = GitHubUpdateService()
) {
    private val _updateState = MutableStateFlow<UpdateCheckResult?>(null)
    val updateState: StateFlow<UpdateCheckResult?> = _updateState.asStateFlow()

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        forceRefresh: Boolean = false
    ): UpdateCheckResult {
        if (!forceRefresh && _updateState.value is UpdateCheckResult.UpdateAvailable) {
            return _updateState.value!!
        }
        val result = gitHubUpdateService.checkForUpdate(currentVersion)
        _updateState.value = result
        return result
    }

    fun dismissUpdate() {
        _updateState.value = null
    }
}
