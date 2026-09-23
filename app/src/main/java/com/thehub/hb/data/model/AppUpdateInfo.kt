package com.thehub.hb.data.model

/**
 * Data model containing GitHub release information for application updates.
 */
data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeInBytes: Long,
    val apkSha256: String = "",
    val publishedAt: String = "",
    val isUpdateAvailable: Boolean = false
)

/**
 * Result state when checking for updates.
 */
sealed interface UpdateCheckResult {
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String, val throwable: Throwable? = null) : UpdateCheckResult
}
