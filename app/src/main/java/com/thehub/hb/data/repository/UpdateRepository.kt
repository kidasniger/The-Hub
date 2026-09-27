package com.thehub.hb.data.repository

import com.thehub.hb.BuildConfig
import com.thehub.hb.data.model.AppUpdateInfo
import com.thehub.hb.data.model.UpdateCheckResult
import com.thehub.hb.data.remote.GitHubUpdateService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UpdateRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gitHubUpdateService: GitHubUpdateService = GitHubUpdateService()
) {
    private val _updateState = MutableStateFlow<UpdateCheckResult?>(null)
    val updateState: StateFlow<UpdateCheckResult?> = _updateState.asStateFlow()

    private val testPopupNotes = """
• La clé API ImgBB a été retirée du code Android : elle n'est plus embarquée dans l'APK.
• Les images sont désormais envoyées par l'application à une Cloud Function Firebase authentifiée.
• Firebase Storage n'est pas utilisé pour les images : ImgBB reste le stockage des images.
• Firestore continue de stocker uniquement l'URL retournée par ImgBB.
• La clé ImgBB est maintenant prévue dans Firebase Secret Manager côté serveur.
• La Cloud Function vérifie l'authentification, limite la taille de l'image et valide que l'URL retournée provient d'ImgBB.
• Le pipeline CI/CD a été renforcé : les versions ne sont préparées qu'après un build valide et les releases sont bloquées sans la clé ImgBB.
• Le déploiement production de la Function reste en attente de la configuration du secret IMGBB_API_KEY.
""".trimIndent()

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        forceRefresh: Boolean = false
    ): UpdateCheckResult {
        if (BuildConfig.DEBUG && BuildConfig.TEST_UPDATE_POPUP) {
            val testUpdate = UpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    latestVersion = "v1.0.181",
                    currentVersion = currentVersion,
                    releaseTitle = "Mise à jour The Hub — sécurité ImgBB",
                    releaseNotes = testPopupNotes,
                    apkDownloadUrl = "",
                    apkFileName = "",
                    apkSizeInBytes = 0L,
                    isUpdateAvailable = true
                )
            )
            _updateState.value = testUpdate
            return testUpdate
        }
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
            val mustUpdate = apkUrl.isNotBlank() && (
                currentCode < minimumCode ||
                    (force && targetCode > currentCode) ||
                    (!versionName.isNullOrBlank() && targetCode > currentCode)
            )
            if (mustUpdate && versionName.isNotBlank()) {
                UpdateCheckResult.UpdateAvailable(
                    AppUpdateInfo(
                        latestVersion = versionName,
                        currentVersion = currentVersion,
                        releaseTitle = "Mise à jour The Hub",
                        releaseNotes = releaseNotes.ifBlank { "Une nouvelle version est disponible." },
                        apkDownloadUrl = apkUrl,
                        apkFileName = "TheHub-$versionName.apk",
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
