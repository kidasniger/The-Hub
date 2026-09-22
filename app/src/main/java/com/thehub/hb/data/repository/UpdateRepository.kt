package com.thehub.hb.data.repository

import com.thehub.hb.BuildConfig
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.model.UpdateCheckResult
import com.thehub.hb.data.remote.GitHubUpdateService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.thehub.hb.data.remote.UpdateSecurity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpdateRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
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
        val githubResult = gitHubUpdateService.checkForUpdate(currentVersion)

        // Admin policy can force or require a minimum version independently
        // of GitHub's latest public release metadata.
        val policyResult = try {
            val policy = firestore.collection("system").document("version").get().await()
            val minimumCode = policy.getLong("minimumSupportedCode") ?: 0L
            val targetCode = policy.getLong("versionCode") ?: 0L
            val apkUrl = policy.getString("apkUrl").orEmpty()
            val releaseNotes = policy.getString("releaseNotes").orEmpty()
            val versionName = policy.getString("versionName").orEmpty()
            val force = policy.getBoolean("forceUpdate") == true
            val currentCode = BuildConfig.VERSION_CODE.toLong()
            val fileName = if (versionName.isNotBlank()) "TheHub-$versionName.apk" else ""
            val isTrustedPolicyUpdate = UpdateSecurity.isValidUpdate(
                apkUrl = apkUrl,
                fileName = fileName,
                versionName = versionName
            )
            val mustUpdate = isTrustedPolicyUpdate &&
                targetCode > currentCode &&
                (currentCode < minimumCode || force || versionName.isNotBlank())
            if (mustUpdate && versionName.isNotBlank()) {
                UpdateCheckResult.UpdateAvailable(
                    AppUpdateInfo(
                        latestVersion = versionName,
                        currentVersion = currentVersion,
                        releaseTitle = "Mise à jour The Hub",
                        releaseNotes = releaseNotes.ifBlank { "Une nouvelle version est disponible." },
                        apkDownloadUrl = apkUrl,
                        apkFileName = fileName,
                        apkSizeInBytes = 0L,
                        isUpdateAvailable = true
                    )
                )
            } else null
        } catch (_: Exception) {
            null
        }

        val result = policyResult ?: githubResult
        _updateState.value = result
        return result
    }

    fun dismissUpdate() {
        _updateState.value = null
    }
}
