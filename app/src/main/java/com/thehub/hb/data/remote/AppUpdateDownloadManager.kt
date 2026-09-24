package com.thehub.hb.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.thehub.hb.MainActivity
import com.thehub.hb.R
import com.thehub.hb.data.model.AppUpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

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
        private const val MAX_DOWNLOAD_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1200L
        private const val PROGRESS_UPDATE_BYTES = 64 * 1024L
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

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
    private var activeDownloadJob: Job? = null
    private var activeTemporaryFile: File? = null
    private var targetApkFile: File? = null

    fun getExistingDownloadedApk(
        fileName: String,
        expectedVersion: String,
        expectedSizeInBytes: Long,
        expectedSha256: String = ""
    ): File? {
        if (!UpdateSecurity.isSafeApkFileName(fileName, expectedVersion)) {
            Log.w(TAG, "Rejected invalid cached APK file name.")
            return null
        }

        val downloadDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        val file = File(downloadDir, fileName)

        if (!file.isFile) return null

        val validationError = validateApk(
            apkFile = file,
            expectedVersion = expectedVersion,
            expectedSizeInBytes = expectedSizeInBytes,
            expectedSha256 = expectedSha256
        )

        if (validationError != null) {
            Log.w(TAG, "Cached APK rejected: $validationError")
            runCatching { file.delete() }
            return null
        }

        return file
    }

    fun restoreCachedDownload(updateInfo: AppUpdateInfo): Boolean {
        val cachedFile = getExistingDownloadedApk(
            fileName = updateInfo.apkFileName,
            expectedVersion = updateInfo.latestVersion,
            expectedSizeInBytes = updateInfo.apkSizeInBytes,
            expectedSha256 = updateInfo.apkSha256
        ) ?: return false

        activeDownloadJob?.cancel()
        activeDownloadJob = null
        currentDownloadId = -1L
        targetApkFile = cachedFile
        _status.value = DownloadStatus.Completed(
            file = cachedFile,
            downloadId = CACHED_DOWNLOAD_ID
        )
        notifyUpdateReadyIfNeeded(updateInfo.latestVersion)
        return true
    }

    /**
     * Downloads the release APK directly with OkHttp.
     *
     * The APK is first written to a temporary file. Only after byte-count,
     * SHA-256, package/version and production-certificate validation succeeds
     * is it moved to the final APK filename. Failed downloads are retried.
     */
    fun startDownload(
        apkUrl: String,
        fileName: String,
        versionName: String,
        expectedSizeInBytes: Long = 0L,
        expectedSha256: String = ""
    ): Long {
        val normalizedSha256 = UpdateSecurity.normalizeSha256(expectedSha256)

        if (!UpdateSecurity.isValidUpdate(apkUrl, fileName, versionName)) {
            Log.e(TAG, "Rejected untrusted APK update request.")
            _status.value = DownloadStatus.Failed(
                "La source de mise à jour est non fiable."
            )
            return -1L
        }

        if (normalizedSha256.isBlank() && expectedSizeInBytes <= 0L) {
            _status.value = DownloadStatus.Failed(
                "Les informations d’intégrité de la mise à jour sont incomplètes."
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
                    apkSizeInBytes = expectedSizeInBytes,
                    apkSha256 = normalizedSha256
                )
            )
        ) {
            return CACHED_DOWNLOAD_ID
        }

        cancelDownload()

        val downloadDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: run {
                    _status.value = DownloadStatus.Failed(
                        "Dossier de téléchargement introuvable."
                    )
                    return -1L
                }

        val finalFile = File(downloadDir, fileName)
        val downloadId = System.currentTimeMillis().coerceAtLeast(1L)
        val temporaryFile = File(downloadDir, ".$fileName.$downloadId.download")

        runCatching { temporaryFile.delete() }
        runCatching { finalFile.delete() }

        targetApkFile = finalFile
        activeTemporaryFile = temporaryFile
        currentDownloadId = downloadId
        _status.value = DownloadStatus.Downloading(
            progressPercent = 0,
            bytesDownloaded = 0,
            totalBytes = expectedSizeInBytes
        )

        activeDownloadJob = coroutineScope.launch {
            var lastError = "Échec du téléchargement."
            try {
                for (attempt in 1..MAX_DOWNLOAD_ATTEMPTS) {
                    if (!isActive) throw CancellationException()

                    try {
                        downloadAndValidate(
                            apkUrl = apkUrl,
                            temporaryFile = temporaryFile,
                            finalFile = finalFile,
                            versionName = versionName,
                            expectedSizeInBytes = expectedSizeInBytes,
                            expectedSha256 = normalizedSha256
                        )

                        currentDownloadId = -1L
                        targetApkFile = finalFile
                        _status.value = DownloadStatus.Completed(
                            file = finalFile,
                            downloadId = downloadId
                        )
                        notifyUpdateReadyIfNeeded(versionName)
                        return@launch
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e.message ?: "Échec du téléchargement."
                        Log.w(
                            TAG,
                            "Update download attempt $attempt/$MAX_DOWNLOAD_ATTEMPTS failed: $lastError",
                            e
                        )
                        runCatching { temporaryFile.delete() }
                        if (attempt < MAX_DOWNLOAD_ATTEMPTS) {
                            delay(RETRY_DELAY_MS)
                            _status.value = DownloadStatus.Downloading(
                                progressPercent = 0,
                                bytesDownloaded = 0,
                                totalBytes = expectedSizeInBytes
                            )
                        }
                    }
                }

                currentDownloadId = -1L
                targetApkFile = null
                _status.value = DownloadStatus.Failed(
                    "Téléchargement vérifié impossible après $MAX_DOWNLOAD_ATTEMPTS tentatives : $lastError"
                )
            } catch (e: CancellationException) {
                runCatching { temporaryFile.delete() }
                if (currentDownloadId == downloadId) {
                    currentDownloadId = -1L
                    targetApkFile = null
                    activeTemporaryFile = null
                }
                throw e
            } finally {
                if (currentDownloadId == -1L || currentDownloadId == downloadId) {
                    activeDownloadJob = null
                    activeTemporaryFile = null
                }
            }
        }

        return downloadId
    }

    private fun downloadAndValidate(
        apkUrl: String,
        temporaryFile: File,
        finalFile: File,
        versionName: String,
        expectedSizeInBytes: Long,
        expectedSha256: String
    ) {
        val request = Request.Builder()
            .url(apkUrl)
            .header("Accept", "application/vnd.android.package-archive")
            .header("Accept-Encoding", "identity")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .header("User-Agent", "TheHub-Android-App")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Serveur de mise à jour HTTP ${response.code}.")
            }

            val body = response.body ?: throw IOException("Réponse de mise à jour vide.")
            val responseLength = body.contentLength()
            val totalBytes = when {
                expectedSizeInBytes > 0L -> expectedSizeInBytes
                responseLength > 0L -> responseLength
                else -> 0L
            }

            if (
                expectedSizeInBytes > 0L &&
                responseLength > 0L &&
                responseLength != expectedSizeInBytes
            ) {
                throw IOException(
                    "Taille HTTP reçue $responseLength octets, attendu $expectedSizeInBytes."
                )
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var bytesWritten = 0L
            var lastProgressBytes = 0L

            temporaryFile.parentFile?.mkdirs()

            body.byteStream().use { input ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        bytesWritten += count

                        if (
                            bytesWritten - lastProgressBytes >= PROGRESS_UPDATE_BYTES ||
                            (totalBytes > 0L && bytesWritten >= totalBytes)
                        ) {
                            val percent = if (totalBytes > 0L) {
                                ((bytesWritten * 100L) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)
                            } else {
                                0
                            }
                            _status.value = DownloadStatus.Downloading(
                                progressPercent = percent,
                                bytesDownloaded = bytesWritten,
                                totalBytes = totalBytes
                            )
                            lastProgressBytes = bytesWritten
                        }
                    }
                    output.flush()
                }
            }

            if (expectedSizeInBytes > 0L && bytesWritten != expectedSizeInBytes) {
                throw IOException(
                    "Téléchargement incomplet : $bytesWritten octets sur $expectedSizeInBytes."
                )
            }

            val actualSha256 = digest.digest().joinToString("") { byte ->
                "%02x".format(Locale.ROOT, byte)
            }

            if (
                expectedSha256.isNotBlank() &&
                !actualSha256.equals(expectedSha256, ignoreCase = true)
            ) {
                throw IOException("Empreinte SHA-256 reçue différente de celle publiée.")
            }

            val validationError = validateApk(
                apkFile = temporaryFile,
                expectedVersion = versionName,
                expectedSizeInBytes = expectedSizeInBytes,
                expectedSha256 = expectedSha256
            )
            if (validationError != null) {
                throw IOException(validationError)
            }
        }

        if (!temporaryFile.isFile || temporaryFile.length() <= 0L) {
            throw IOException("Le fichier APK temporaire est vide.")
        }

        if (!temporaryFile.renameTo(finalFile)) {
            temporaryFile.copyTo(finalFile, overwrite = true)
            runCatching { temporaryFile.delete() }
        }
    }

    private fun validateApk(
        apkFile: File,
        expectedVersion: String,
        expectedSizeInBytes: Long,
        expectedSha256: String
    ): String? {
        if (!apkFile.isFile || apkFile.length() <= 0L) {
            return "Le fichier APK est absent ou vide."
        }

        if (expectedSizeInBytes > 0L && apkFile.length() != expectedSizeInBytes) {
            return "Taille APK incorrecte : ${apkFile.length()} au lieu de $expectedSizeInBytes octets."
        }

        val normalizedSha256 = UpdateSecurity.normalizeSha256(expectedSha256)
        if (normalizedSha256.isNotBlank()) {
            val actualSha256 = runCatching {
                MessageDigest.getInstance("SHA-256").let { digest ->
                    apkFile.inputStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            digest.update(buffer, 0, count)
                        }
                    }
                    digest.digest().joinToString("") { byte ->
                        "%02x".format(Locale.ROOT, byte)
                    }
                }
            }.getOrElse {
                return "Impossible de calculer l’empreinte SHA-256 de l’APK."
            }

            if (!actualSha256.equals(normalizedSha256, ignoreCase = true)) {
                return "L’empreinte SHA-256 de l’APK ne correspond pas à la version publiée."
            }
        }

        val packageInfo = try {
            @Suppress("DEPRECATION")
            val archiveInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, archiveInfoFlags)
        } catch (e: Exception) {
            Log.w(TAG, "Could not inspect APK: ${apkFile.absolutePath}", e)
            null
        } ?: return "Impossible de lire les métadonnées de l’APK."

        val actualVersion = packageInfo.versionName?.trim().orEmpty()
        if (packageInfo.packageName != context.packageName) {
            return "L’APK téléchargé n’est pas une version de The Hub."
        }

        if (normalizeVersion(actualVersion) != normalizeVersion(expectedVersion)) {
            return "L’APK téléchargé est en version $actualVersion au lieu de $expectedVersion."
        }

        if (!hasExpectedProductionCertificate(packageInfo)) {
            return "La signature de production de l’APK est différente de celle attendue."
        }

        return null
    }

    private fun hasExpectedProductionCertificate(
        packageInfo: android.content.pm.PackageInfo
    ): Boolean {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.toList().orEmpty()
        }

        if (signatures.isEmpty()) return false

        return signatures.any { signature ->
            val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
            digest.joinToString(":") { byte -> "%02X".format(Locale.ROOT, byte) } ==
                "F5:CD:8C:88:C4:C8:5F:D5:91:84:21:57:06:DA:52:C6:2C:57:D0:37"
        }
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

        val alreadyNotified =
            notificationPrefs.getString(LAST_NOTIFIED_VERSION, null) == versionName
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
            .setContentText("Appuyez pour ouvrir l’installation de la nouvelle version")
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
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        currentDownloadId = -1L

        runCatching { activeTemporaryFile?.delete() }
        activeTemporaryFile = null
        targetApkFile = null
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
