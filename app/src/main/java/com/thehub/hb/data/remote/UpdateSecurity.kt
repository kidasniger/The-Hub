package com.thehub.hb.data.remote

import java.net.URI

/** Centralizes validation for self-updates. */
object UpdateSecurity {
    private const val TRUSTED_HOST = "github.com"
    private const val TRUSTED_RELEASE_PREFIX = "/kidasniger/The-Hub/releases/download/"
    private val VERSION_REGEX = Regex("^v?\\d+\\.\\d+\\.\\d+$")
    private val APK_NAME_REGEX = Regex("^TheHub-v?\\d+\\.\\d+\\.\\d+\\.apk$")

    fun isSupportedVersion(raw: String): Boolean = VERSION_REGEX.matches(raw.trim())

    fun normalizeVersion(raw: String): String = raw.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore("-")

    fun isTrustedApkUrl(rawUrl: String): Boolean {
        val url = rawUrl.trim()
        if (url.isBlank()) return false
        return runCatching {
            val uri = URI(url)
            val path = uri.path.orEmpty()
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.host?.lowercase() == TRUSTED_HOST &&
                path.startsWith(TRUSTED_RELEASE_PREFIX) &&
                !path.contains("//") &&
                !path.split('/').any { it == ".." } &&
                uri.fragment == null
        }.getOrDefault(false)
    }

    fun isSafeApkFileName(rawName: String, expectedVersion: String? = null): Boolean {
        val name = rawName.trim()
        if (name.length > 120 || name != name.substringAfterLast('/')) return false
        if (name.contains('\\') || !APK_NAME_REGEX.matches(name)) return false
        val expected = expectedVersion?.trim().orEmpty()
        if (expected.isBlank()) return true
        val actualFromName = name.removePrefix("TheHub-").removeSuffix(".apk")
        return isSupportedVersion(expected) && normalizeVersion(actualFromName) == normalizeVersion(expected)
    }

    fun isValidUpdate(apkUrl: String, fileName: String, versionName: String): Boolean =
        isSupportedVersion(versionName) &&
            isTrustedApkUrl(apkUrl) &&
            isSafeApkFileName(fileName, versionName)
}
