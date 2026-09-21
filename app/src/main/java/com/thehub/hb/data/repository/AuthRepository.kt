package com.thehub.hb.data.repository

import android.content.Context
import com.thehub.hb.data.local.DataStoreManager
import com.thehub.hb.data.model.User
import com.thehub.hb.data.remote.ImgbbService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


const val GOOGLE_WEB_CLIENT_ID = "183373607979-d1qu0ogpl24dptctim56nlght54hs8a7.apps.googleusercontent.com"

sealed class GoogleSignInResult {
    data class Success(val shouldCompleteProfile: Boolean, val user: FirebaseUser) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val imgbbService: ImgbbService = ImgbbService(),
    private val dataStoreManager: DataStoreManager
) {

    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    companion object {
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9._]{3,30}$")

        fun isValidUsername(username: String): Boolean {
            val clean = username.trim()
            return clean.matches(USERNAME_REGEX)
        }
    }

    suspend fun checkUsernameUnique(username: String): Boolean = withContext(Dispatchers.IO) {
        val clean = username.trim().lowercase()
        if (!isValidUsername(clean)) return@withContext false
        return@withContext try {
            val usernameDoc = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("usernames").document(clean).get().await()
            }
            if (usernameDoc != null && usernameDoc.exists()) {
                val currentUid = auth.currentUser?.uid
                val ownerUid = usernameDoc.getString("uid")
                return@withContext (currentUid != null && ownerUid == currentUid)
            }

            val snapshot = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("users")
                    .whereEqualTo("usernameLower", clean)
                    .limit(1)
                    .get()
                    .await()
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val currentUid = auth.currentUser?.uid
                return@withContext (currentUid != null && snapshot.documents.all { it.id == currentUid })
            }
            true
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "checkUsernameUnique skipped: ${e.message}")
            false
        }
    }

    suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = User.fromMap(doc.data ?: emptyMap())
                if (doc.getString("usernameLower").isNullOrBlank() && user.username.isNotBlank()) {
                    try {
                        doc.reference.update("usernameLower", user.username.lowercase())
                    } catch (_: Exception) {}
                }
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<String> {
        return imgbbService.uploadImage(imageBytes)
    }

    suspend fun completeProfile(
        displayName: String,
        username: String? = null,
        bio: String?,
        birthdate: String?,
        photoUrl: String?
    ): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Aucun utilisateur connecté.")
            
            // Update Firestore
            val updates = mutableMapOf<String, Any?>(
                "displayName" to displayName.trim(),
                "bio" to bio?.trim(),
                "birthdate" to birthdate?.trim()
            )
            val cleanUsername = username?.trim()?.lowercase()
            if (!cleanUsername.isNullOrBlank()) {
                if (!isValidUsername(cleanUsername)) {
                    return Result.failure(Exception("Le nom d'utilisateur doit contenir entre 3 et 30 caractères (lettres, chiffres, tirets bas ou points)."))
                }
                val isUnique = checkUsernameUnique(cleanUsername)
                if (!isUnique) {
                    return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
                }
                updates["username"] = cleanUsername
                updates["usernameLower"] = cleanUsername
                try {
                    firestore.collection("usernames").document(cleanUsername).set(
                        mapOf(
                            "uid" to user.uid,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        ),
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.w("AuthRepository", "Could not reserve username: ${e.message}")
                }
            }
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl
            }
            firestore.collection("users").document(user.uid)
                .set(updates, SetOptions.merge())
                .await()

            // Update Firebase Auth profile
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName.trim()
                if (photoUrl != null) {
                    this.photoUri = android.net.Uri.parse(photoUrl)
                }
            }
            user.updateProfile(profileUpdates).await()

            // Update local DataStore
            dataStoreManager.saveLastUser(
                email = user.email ?: "",
                name = displayName.trim(),
                username = cleanUsername,
                photoUrl = photoUrl
            )

            // Update shared in-memory UserCacheRepository immediately
            com.thehub.hb.data.repository.UserCacheRepository.getInstance().putUser(
                com.thehub.hb.data.model.UserInfo(
                    uid = user.uid,
                    displayName = displayName.trim(),
                    username = cleanUsername ?: "",
                    photoUrl = photoUrl
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(context: Context): GoogleSignInResult {
        var previousPushUserId: String? = null

        suspend fun restorePreviousPushRegistration() {
            val uid = previousPushUserId
            if (!uid.isNullOrBlank() && auth.currentUser?.uid == uid) {
                try {
                    NotificationRepository(firestore, auth).registerCurrentFcmToken()
                } catch (_: Exception) {}
            }
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                 credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                // Remove this physical device token while the previous Firebase
                // account is still authenticated. Doing it after the auth switch
                // would fail the Firestore ownership rule.
                val previousUid = auth.currentUser?.uid
                previousPushUserId = previousUid
                if (!previousUid.isNullOrBlank()) {
                    try {
                        NotificationRepository(firestore, auth)
                            .unregisterFcmTokenForUser(previousUid)
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "AuthRepository",
                            "Could not unregister previous account FCM token: " + e.message
                        )
                    }
                }

                // Invalidate the physical device token before switching accounts.
                // This guarantees that an old account can no longer target this
                // device even if an obsolete Firestore token document remains.
                try {
                    FirebaseMessaging.getInstance().deleteToken().await()
                } catch (e: Exception) {
                    android.util.Log.w(
                        "AuthRepository",
                        "Could not invalidate old FCM token before account switch: " + e.message
                    )
                }

                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: throw Exception("Utilisateur introuvable après connexion.")

                // Check if user document already exists in Firestore
                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val isNewUser = !userDoc.exists()

                val shouldCompleteProfile: Boolean
                if (isNewUser) {
                    val userEmail = (firebaseUser.email ?: googleIdTokenCredential.id).trim()
                    val userDisplayName = (googleIdTokenCredential.displayName ?: firebaseUser.displayName)?.trim()
                    val userPhotoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: firebaseUser.photoUrl?.toString()

                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = userEmail,
                        username = "",
                        usernameLower = "",
                        displayName = userDisplayName,
                        photoUrl = userPhotoUrl,
                        bio = null,
                        birthdate = null,
                        createdAt = System.currentTimeMillis()
                    )

                    firestore.collection("users").document(firebaseUser.uid)
                        .set(newUser.toMap(), SetOptions.merge())
                        .await()

                    dataStoreManager.saveLastUser(
                        email = userEmail,
                        name = userDisplayName,
                        photoUrl = userPhotoUrl
                    )

                    shouldCompleteProfile = true
                } else {
                    if (userDoc.getBoolean("isDeleted") == true) {
                        signOut()
                        dataStoreManager.clearAll()
                        return GoogleSignInResult.Error("Ce compte a été supprimé.")
                    }
                    if (userDoc.getBoolean("isSuspended") == true) {
                        signOut()
                        dataStoreManager.clearAll()
                        return GoogleSignInResult.Error("Ce compte est temporairement suspendu.")
                    }
                    val userData = User.fromMap(userDoc.data ?: emptyMap())
                    dataStoreManager.saveLastUser(
                        email = firebaseUser.email ?: userData.email,
                        name = userData.displayName ?: firebaseUser.displayName,
                        username = userData.username,
                        photoUrl = userData.photoUrl ?: firebaseUser.photoUrl?.toString()
                    )

                    shouldCompleteProfile = userData.username.isBlank() || userData.displayName.isNullOrBlank()
                }

                GoogleSignInResult.Success(
                    shouldCompleteProfile = shouldCompleteProfile,
                    user = firebaseUser
                )
            } else {
                GoogleSignInResult.Error("Type d'identifiant Google inattendu.")
            }
        } catch (e: GetCredentialCancellationException) {
            restorePreviousPushRegistration()
            android.util.Log.d("AuthRepository", "Connexion Google annulée par l'utilisateur")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            restorePreviousPushRegistration()
            android.util.Log.w("AuthRepository", "Aucun compte Google disponible", e)
            GoogleSignInResult.Error("Aucun compte Google disponible sur cet appareil.")
        } catch (e: Exception) {
            restorePreviousPushRegistration()
            android.util.Log.e("AuthRepository", "Échec de connexion Google", e)
            if (e.message?.contains("cancel", ignoreCase = true) == true ||
                e.cause?.message?.contains("cancel", ignoreCase = true) == true
            ) {
                GoogleSignInResult.Cancelled
            } else {
                val friendlyMessage = when {
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Erreur réseau. Vérifie ta connexion Internet et réessaie."
                    else -> e.localizedMessage ?: "Impossible de se connecter avec Google."
                }
                GoogleSignInResult.Error(friendlyMessage)
            }
        }
    }

    suspend fun signOut() {
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            try {
                NotificationRepository(firestore, auth)
                    .unregisterFcmTokenForUser(uid)
            } catch (e: Exception) {
                android.util.Log.w(
                    "AuthRepository",
                    "Could not unregister FCM token before sign out: " + e.message
                )
            }
        }

        try {
            // Invalidate the device token as part of logout. A subsequent login
            // will obtain/register a fresh token for the new Firebase UID.
            FirebaseMessaging.getInstance().deleteToken().await()
        } catch (e: Exception) {
            android.util.Log.w(
                "AuthRepository",
                "Could not invalidate FCM token on logout: " + e.message
            )
        }

        auth.signOut()
        try {
            com.thehub.hb.data.repository.UserCacheRepository.getInstance().clear()
        } catch (_: Exception) {}
    }
}
