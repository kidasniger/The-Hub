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
            val rawReleaseNotes = json.optString("body", "").trim()
            val cleanNotes = sanitizeReleaseNotes(rawReleaseNotes)
            val publishedAt = json.optString("published_at", "")

            // Look for APK in assets
            var apkDownloadUrl = ""
            var apkFileName = ""
            var apkSize = 0L
            var apkSha256 = ""

            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    val contentType = asset.optString("content_type", "")
                    val downloadUrl = asset.optString("browser_download_url", "")

                    if (
                        (name.endsWith(".apk", ignoreCase = true) ||
                            contentType == "application/vnd.android.package-archive") &&
                        UpdateSecurity.isTrustedApkUrl(downloadUrl) &&
                        UpdateSecurity.isSafeApkFileName(name, tagName)
                    ) {
                        apkDownloadUrl = downloadUrl
                        apkFileName = name
                        apkSize = asset.optLong("size", 0L)
                        apkSha256 = UpdateSecurity.normalizeSha256(asset.optString("digest", ""))
                        break
                    }
                }
            }

            val isNewer = isVersionGreater(remoteVersion = tagName, currentVersion = currentVersion)

            if (
                isNewer &&
                UpdateSecurity.isValidUpdate(
                    apkUrl = apkDownloadUrl,
                    fileName = apkFileName,
                    versionName = tagName
                )
            ) {
                val updateInfo = AppUpdateInfo(
                    latestVersion = tagName,
                    currentVersion = currentVersion,
                    releaseTitle = releaseName.takeIf { !it.contains("http") } ?: "Mise à jour $tagName",
                    releaseNotes = cleanNotes,
                    apkDownloadUrl = apkDownloadUrl,
                    apkFileName = apkFileName.ifBlank { "TheHub-$tagName.apk" },
                    apkSizeInBytes = apkSize,
                    apkSha256 = apkSha256,
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

    /**
     * Nettoie les notes de version GitHub pour supprimer tous les liens URL,
     * les mentions de PR/commits et formater proprement la liste des modifications.
     */
    fun sanitizeReleaseNotes(raw: String): String {
        if (raw.isBlank()) {
            return "• Améliorations générales des performances et de la fluidité\n• Corrections de bugs et renforcement de la stabilité"
        }

        val lines = raw.lines()
        val cleanedLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            // Ignorer les titres markdown "What's Changed" ou "Changelog"
            if (trimmed.startsWith("#") || trimmed.equals("What's Changed", ignoreCase = true)) {
                continue
            }
            // Ignorer les liens complets de comparaison GitHub "Full Changelog", "Compare"
            if (trimmed.contains("Full Changelog", ignoreCase = true) ||
                trimmed.contains("compare/", ignoreCase = true) ||
                trimmed.contains("/compare", ignoreCase = true)
            ) {
                continue
            }

            // Nettoyer les liens Markdown [texte](url) -> texte
            var cleaned = trimmed.replace(Regex("""\[(.*?)\]\(.*?\)"""), "$1")
            // Supprimer les suffixes de PR GitHub "by @user in https://..." ou "in https://..."
            cleaned = cleaned.replace(Regex("""by\s+@[\w-]+\s+in\s+https?://\S+"""), "")
            cleaned = cleaned.replace(Regex("""in\s+https?://\S+"""), "")
            // Supprimer toute URL résiduelle http/https
            cleaned = cleaned.replace(Regex("""https?://\S+"""), "")
            // Supprimer les références de commit (ex: (#123))
            cleaned = cleaned.replace(Regex("""\(\s*#\d+\s*\)"""), "")
            cleaned = cleaned.trim()

            if (cleaned.isBlank()) continue

            // Formater en puce élégante
            val bulletText = when {
                cleaned.startsWith("* ") -> "• " + cleaned.removePrefix("* ").trim()
                cleaned.startsWith("- ") -> "• " + cleaned.removePrefix("- ").trim()
                cleaned.startsWith("• ") -> cleaned
                else -> "• $cleaned"
            }

            if (bulletText.length > 2) {
                cleanedLines.add(bulletText)
            }
        }

        return if (cleanedLines.isNotEmpty()) {
            cleanedLines.joinToString("\n")
        } else {
            "• Améliorations générales des performances et de la fluidité\n• Corrections de bugs et renforcement de la stabilité"
        }
    }
}
