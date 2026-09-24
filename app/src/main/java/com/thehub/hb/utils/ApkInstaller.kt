package com.thehub.hb.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.thehub.hb.data.remote.UpdateSecurity
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private const val PREFS = "apk_installer"
    private const val PENDING_APK_PATH = "pending_apk_path"

    /**
     * Checks if the app currently has permission to install unknown apps (Android 8.0+).
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system settings to allow this app to install unknown apps.
     */
    fun openInstallPermissionSettings(context: Context, apkFile: File? = null) {
        if (apkFile != null && isAllowedApkFile(context, apkFile)) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PENDING_APK_PATH, apkFile.absolutePath)
                .apply()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /** Retries a pending APK installation after the user enables unknown-app installs. */
    fun resumePendingInstall(context: Context): Boolean {
        if (!canRequestPackageInstalls(context)) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pendingPath = prefs.getString(PENDING_APK_PATH, null) ?: return false
        val pendingFile = File(pendingPath)

        if (!isAllowedApkFile(context, pendingFile)) {
            prefs.edit().remove(PENDING_APK_PATH).apply()
            return false
        }

        val success = installApk(context, pendingFile)
        if (success) {
            prefs.edit().remove(PENDING_APK_PATH).apply()
        }
        return success
    }

    /**
     * Launches Android's PackageInstaller to install the given [apkFile].
     * Uses [FileProvider] to grant secure read URI permission to the system installer.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!isAllowedApkFile(context, apkFile)) {
            Log.e(TAG, "Cannot install APK outside the managed updater directory.")
            return false
        }

        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "Cannot install APK: file does not exist or is empty at ${apkFile.absolutePath}")
            return false
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (installIntent.resolveActivity(context.packageManager) == null) {
                Log.e(TAG, "No Android package installer is available for APK installation.")
                return false
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK install intent", e)
            return false
        }
    }

    private fun isAllowedApkFile(context: Context, apkFile: File): Boolean {
        if (!UpdateSecurity.isSafeApkFileName(apkFile.name)) return false

        val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: return false
        val root = runCatching { downloadDir.canonicalFile }.getOrNull() ?: return false
        val candidate = runCatching { apkFile.canonicalFile }.getOrNull() ?: return false
        val rootPath = root.path.trimEnd(File.separatorChar) + File.separator
        return candidate.path.startsWith(rootPath)
    }
}
