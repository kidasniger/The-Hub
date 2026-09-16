package com.thehub.hb.data.remote

import android.util.Log
import com.thehub.hb.BuildConfig
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.model.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubUpdateService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val owner: String = "kidasniger",
    private val repo: String = "The-Hub"
) {
    companion object {
        private const val TAG = "GitHubUpdateService"
    }

    private val apiUrl: String
        get() = "https://api.github.com/repos/$owner/$repo/releases/latest"

    /**
     * Checks GitHub releases for a newer version than [currentVersion].
     */
    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "TheHub-Android-App")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersion)
                }
                val errorMsg = "GitHub API error HTTP ${response.code}: $responseBody"
                Log.e(TAG, errorMsg)
                return@withContext UpdateCheckResult.Error(errorMsg)
            }

            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "").trim()
            val publishedAt = json.optString("published_at", "")

            // Look for APK in assets
            var apkDownloadUrl = ""
            var apkFileName = ""
            var apkSize = 0L

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val contentType = asset.optString("content_type", "")
                    val downloadUrl = asset.optString("browser_download_url", "")

                    if (name.endsWith(".apk", ignoreCase = true) ||
                        contentType == "application/vnd.android.package-archive"
                    ) {
                        apkDownloadUrl = downloadUrl
                        apkFileName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val isNewer = isVersionGreater(remoteVersion = tagName, currentVersion = currentVersion)

            if (isNewer && apkDownloadUrl.isNotBlank()) {
                val updateInfo = AppUpdateInfo(
                    latestVersion = tagName,
                    currentVersion = currentVersion,
                    releaseTitle = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName.ifBlank { "TheHub-$tagName.apk" },
                    apkSizeInBytes = apkSize,
                    publishedAt = publishedAt,
                    isUpdateAvailable = true
                )
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check update from GitHub", e)
            UpdateCheckResult.Error("Erreur lors de la vérification de mise à jour: ${e.localizedMessage}", e)
        }
    }

    /**
     * Compares two semantic version strings (e.g. "v1.0.19" vs "1.0").
     * Returns true if [remoteVersion] is strictly greater than [currentVersion].
     */
    fun isVersionGreater(remoteVersion: String, currentVersion: String): Boolean {
        val cleanRemote = cleanVersion(remoteVersion)
        val cleanCurrent = cleanVersion(currentVersion)

        if (cleanRemote.isEmpty() || cleanCurrent.isEmpty()) {
            return false
        }

        val remoteParts = cleanRemote.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun cleanVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-") // Ignore build metadata / qualifiers
            .filter { it.isDigit() || it == '.' }
    }
}
