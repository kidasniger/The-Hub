package com.thehub.hb.data.remote

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thehub.hb.MainActivity
import com.thehub.hb.R
import com.thehub.hb.data.model.AppUpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadStatus
    data class Completed(val file: File, val downloadId: Long) : DownloadStatus
    data class Failed(val reason: String) : DownloadStatus
}

class AppUpdateDownloadManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AppUpdateDownload"
        private const val CACHED_DOWNLOAD_ID = -1L
        private const val UPDATE_NOTIFICATION_CHANNEL_ID = "app_updates"
        private const val UPDATE_NOTIFICATION_ID = 4108
        private const val NOTIFICATION_PREFS = "app_update_notification"
        private const val LAST_NOTIFIED_VERSION = "last_notified_version"
    }

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val notificationPrefs = context.getSharedPreferences(
        NOTIFICATION_PREFS,
        Context.MODE_PRIVATE
    )

    init {
        createUpdateNotificationChannel()
    }

    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status: StateFlow<DownloadStatus> = _status.asStateFlow()

    private var currentDownloadId: Long = -1L
    private var progressJob: Job? = null
    private var downloadCompleteReceiver: BroadcastReceiver? = null
    private var targetApkFile: File? = null

    fun getExistingDownloadedApk(
        fileName: String,
        expectedVersion: String,
        expectedSizeInBytes: Long
    ): File? {
        if (!UpdateSecurity.isSafeApkFileName(fileName, expectedVersion)) {
            Log.w(TAG, "Rejected invalid cached APK file name.")
            return null
        }

        val downloadDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        val file = File(downloadDir, fileName)

        if (!file.isFile) return null

        if (!isApkValid(
                apkFile = file,
                expectedVersion = expectedVersion,
                expectedSizeInBytes = expectedSizeInBytes
            )
        ) {
            try {
                file.delete()
            } catch (_: Exception) {
                Log.w(TAG, "Could not delete invalid cached APK: ${file.absolutePath}")
            }
            return null
        }

        return file
    }

    fun restoreCachedDownload(updateInfo: AppUpdateInfo): Boolean {
        val cachedFile = getExistingDownloadedApk(
            fileName = updateInfo.apkFileName,
            expectedVersion = updateInfo.latestVersion,
            expectedSizeInBytes = updateInfo.apkSizeInBytes
        ) ?: return false

        progressJob?.cancel()
        unregisterCompletionReceiver()
        currentDownloadId = -1L
        targetApkFile = cachedFile
        _status.value = DownloadStatus.Completed(
            file = cachedFile,
            downloadId = CACHED_DOWNLOAD_ID
        )
        notifyUpdateReadyIfNeeded(
            versionName = updateInfo.latestVersion
        )
        return true
    }

    fun startDownload(
        apkUrl: String,
        fileName: String,
        versionName: String,
        expectedSizeInBytes: Long = 0L
    ): Long {
        if (!UpdateSecurity.isValidUpdate(apkUrl, fileName, versionName)) {
            Log.e(TAG, "Rejected untrusted APK update request.")
            _status.value = DownloadStatus.Failed(
                "La source de mise à jour n'est pas fiable."
            )
            return -1L
        }

        if (restoreCachedDownload(
                AppUpdateInfo(
                    latestVersion = versionName,
                    currentVersion = "",
                    releaseTitle = "",
                    releaseNotes = "",
                    apkDownloadUrl = apkUrl,
                    apkFileName = fileName,
                    apkSizeInBytes = expectedSizeInBytes
                )
            )
        ) {
            return CACHED_DOWNLOAD_ID
        }

        cancelDownload()

        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir != null) {
            val file = File(downloadDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            targetApkFile = file
        }

        val uri = Uri.parse(apkUrl)
        val request = DownloadManager.Request(uri).apply {
            setTitle("The Hub $versionName")
            setDescription("Téléchargement de la mise à jour...")
            // DownloadManager handles progress; the app posts its own final
            // notification so its click can reopen the update BottomSheet.
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
        }

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
            _status.value = DownloadStatus.Failed(
                "Impossible de lancer le téléchargement: ${e.localizedMessage}"
            )
            return -1L
        }

        currentDownloadId = downloadId
        _status.value = DownloadStatus.Downloading(0, 0, 0)

        registerCompletionReceiver(
            downloadId = downloadId,
            fileName = fileName,
            expectedVersion = versionName,
            expectedSizeInBytes = expectedSizeInBytes
        )
        startProgressPolling(
            downloadId = downloadId,
            fileName = fileName,
            expectedVersion = versionName,
            expectedSizeInBytes = expectedSizeInBytes
        )

        return downloadId
    }

    private fun registerCompletionReceiver(
        downloadId: Long,
        fileName: String,
        expectedVersion: String,
        expectedSizeInBytes: Long
    ) {
        unregisterCompletionReceiver()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                val id =
                    intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    checkDownloadSuccess(
                        downloadId = downloadId,
                        fileName = fileName,
                        expectedVersion = expectedVersion,
                        expectedSizeInBytes = expectedSizeInBytes
                    )
                }
            }
        }
        downloadCompleteReceiver = receiver

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
    }

    private fun unregisterCompletionReceiver() {
        downloadCompleteReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {
            }
            downloadCompleteReceiver = null
        }
    }

    private fun startProgressPolling(
        downloadId: Long,
        fileName: String,
        expectedVersion: String,
        expectedSizeInBytes: Long
    ) {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                var cursor: Cursor? = null
                try {
                    cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex =
                            cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val statusCode =
                            if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        val bytesDownloaded =
                            if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                        val bytesTotal =
                            if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else 0L

                        when (statusCode) {
                            DownloadManager.STATUS_RUNNING,
                            DownloadManager.STATUS_PENDING -> {
                                val percent = if (bytesTotal > 0) {
                                    ((bytesDownloaded * 100) / bytesTotal)
                                        .toInt()
                                        .coerceIn(0, 100)
                                } else {
                                    0
                                }
                                _status.value = DownloadStatus.Downloading(
                                    progressPercent = percent,
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = bytesTotal
                                )
                            }

                            DownloadManager.STATUS_SUCCESSFUL -> {
                                checkDownloadSuccess(
                                    downloadId = downloadId,
                                    fileName = fileName,
                                    expectedVersion = expectedVersion,
                                    expectedSizeInBytes = expectedSizeInBytes
                                )
                                break
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reasonIndex =
                                    cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                val reasonCode =
                                    if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                _status.value = DownloadStatus.Failed(
                                    "Échec du téléchargement (code $reasonCode)"
                                )
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error querying download progress", e)
                } finally {
                    cursor?.close()
                }
                delay(500)
            }
        }
    }

    private fun checkDownloadSuccess(
        downloadId: Long,
        fileName: String,
        expectedVersion: String,
        expectedSizeInBytes: Long
    ) {
        progressJob?.cancel()
        unregisterCompletionReceiver()

        val downloadDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: run {
                currentDownloadId = -1L
                _status.value = DownloadStatus.Failed(
                    "Dossier de téléchargement introuvable."
                )
                return
            }
        val file = File(downloadDir, fileName)

        val validFile = when {
            file.isFile -> file
            targetApkFile?.isFile == true -> targetApkFile
            else -> null
        }

        if (validFile != null && isApkValid(
                apkFile = validFile,
                expectedVersion = expectedVersion,
                expectedSizeInBytes = expectedSizeInBytes
            )
        ) {
            currentDownloadId = -1L
            targetApkFile = validFile
            _status.value = DownloadStatus.Completed(validFile, downloadId)
            notifyUpdateReadyIfNeeded(expectedVersion)
        } else {
            if (validFile?.exists() == true) {
                try {
                    validFile.delete()
                } catch (_: Exception) {
                }
            }
            currentDownloadId = -1L
            _status.value = DownloadStatus.Failed(
                "Le fichier APK téléchargé est incomplet ou ne correspond pas à la version $expectedVersion."
            )
        }
    }

    private fun isApkValid(
        apkFile: File,
        expectedVersion: String,
        expectedSizeInBytes: Long
    ): Boolean {
        if (!apkFile.isFile || apkFile.length() <= 0L) {
            return false
        }

        if (expectedSizeInBytes > 0L && apkFile.length() != expectedSizeInBytes) {
            return false
        }

        val packageInfo = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not inspect APK: ${apkFile.absolutePath}", e)
            null
        } ?: return false

        val actualVersion = packageInfo.versionName?.trim().orEmpty()
        return packageInfo.packageName == context.packageName &&
            normalizeVersion(actualVersion) == normalizeVersion(expectedVersion)
    }

    private fun normalizeVersion(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
    }

    private fun createUpdateNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            UPDATE_NOTIFICATION_CHANNEL_ID,
            "Mises à jour",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications lorsque The Hub a terminé de télécharger une mise à jour."
        }
        manager.createNotificationChannel(channel)
    }

    private fun notifyUpdateReadyIfNeeded(versionName: String) {
        if (versionName.isBlank()) return

        val alreadyNotified = notificationPrefs.getString(LAST_NOTIFIED_VERSION, null) == versionName
        if (alreadyNotified) return

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission is not granted; update-ready notification skipped.")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_UPDATE_DIALOG, true)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            UPDATE_NOTIFICATION_ID,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            UPDATE_NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_hub_logo)
            .setContentTitle("Mise à jour prête")
            .setContentText("Appuyez pour ouvrir l'installation de la nouvelle version")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                UPDATE_NOTIFICATION_ID,
                notification
            )
            notificationPrefs.edit()
                .putString(LAST_NOTIFIED_VERSION, versionName)
                .apply()
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post update notification.", e)
        }
    }

    fun cancelDownload() {
        progressJob?.cancel()
        unregisterCompletionReceiver()
        if (currentDownloadId != -1L) {
            try {
                downloadManager.remove(currentDownloadId)
            } catch (_: Exception) {
            }
            currentDownloadId = -1L
        }
        _status.value = DownloadStatus.Idle
    }

    fun resetStatus() {
        _status.value = DownloadStatus.Idle
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 o"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.ROOT, "%.2f Go", gb)
        mb >= 1.0 -> String.format(java.util.Locale.ROOT, "%.1f Mo", mb)
        kb >= 1.0 -> String.format(java.util.Locale.ROOT, "%.1f Ko", kb)
        else -> "$bytes o"
    }
}
