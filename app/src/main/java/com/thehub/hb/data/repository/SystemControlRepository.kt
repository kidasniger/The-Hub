package com.thehub.hb.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemEmergencyState(
    val maintenance: Boolean = false,
    val registrations: Boolean = true,
    val posts: Boolean = true,
    val comments: Boolean = true,
    val messaging: Boolean = true,
    val notifications: Boolean = true,
    val uploads: Boolean = true,
    val message: String = ""
)

data class SystemControls(
    val emergency: SystemEmergencyState = SystemEmergencyState(),
    val featureFlags: Map<String, Boolean> = emptyMap()
) {
    fun isFeatureEnabled(key: String, default: Boolean = true): Boolean =
        featureFlags[key] ?: default

    fun postsEnabled(): Boolean =
        emergency.posts && isFeatureEnabled("create_post")

    fun messagingEnabled(): Boolean =
        emergency.messaging && isFeatureEnabled("messaging")
}

class SystemControlRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _controls = MutableStateFlow(SystemControls())
    val controls: StateFlow<SystemControls> = _controls.asStateFlow()

    suspend fun refresh(): Result<SystemControls> = withContext(Dispatchers.IO) {
        if (auth.currentUser == null) {
            return@withContext Result.success(_controls.value)
        }

        try {
            val emergencyDoc = firestore.collection("system")
                .document("emergency")
                .get()
                .await()

            val flagsSnapshot = firestore.collection("featureFlags")
                .limit(200)
                .get()
                .await()

            val controls = SystemControls(
                emergency = SystemEmergencyState(
                    maintenance = emergencyDoc.getBoolean("maintenance") == true,
                    registrations = emergencyDoc.getBoolean("registrations") != false,
                    posts = emergencyDoc.getBoolean("posts") != false,
                    comments = emergencyDoc.getBoolean("comments") != false,
                    messaging = emergencyDoc.getBoolean("messaging") != false,
                    notifications = emergencyDoc.getBoolean("notifications") != false,
                    uploads = emergencyDoc.getBoolean("uploads") != false,
                    message = emergencyDoc.getString("message").orEmpty()
                ),
                featureFlags = flagsSnapshot.documents.associate { doc ->
                    doc.id to (doc.getBoolean("enabled") == true)
                }
            )

            _controls.value = controls
            Result.success(controls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun reset() {
        _controls.value = SystemControls()
    }
}
