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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCreationException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
    var lastVerificationEmailSentSuccessfully: Boolean = true
        private set
    var lastVerificationEmailError: Throwable? = null
        private set

    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun checkUsernameUnique(username: String): Boolean {
        return try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                firestore.collection("users")
                    .whereEqualTo("username", username.trim().lowercase())
                    .limit(1)
                    .get()
                    .await()
            }
            snapshot?.isEmpty ?: true
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "checkUsernameUnique skipped: ${e.message}")
            // If offline or permission check, allow progression or handle error
            true
        }
    }

    suspend fun signUp(email: String, username: String, password: String): Result<FirebaseUser> {
        return try {
            val cleanUsername = username.trim().lowercase()
            val isUnique = checkUsernameUnique(cleanUsername)
            if (!isUnique) {
                return Result.failure(Exception("Ce nom d'utilisateur est déjà pris."))
            }

            val existingUser = auth.currentUser
            val user = if (existingUser != null && existingUser.email?.equals(email.trim(), ignoreCase = true) == true) {
                android.util.Log.d("AuthRepository", "Utilisateur Firebase Auth déjà actif pour ${existingUser.email}")
                existingUser
            } else {
                try {
                    val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                    authResult.user ?: throw Exception("Utilisateur introuvable après création.")
                } catch (collision: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    try {
                        val signInResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                        signInResult.user ?: throw collision
                    } catch (_: Exception) {
                        throw collision
                    }
                }
            }

            // Send email verification
            try {
                user.sendEmailVerification().await()
                lastVerificationEmailSentSuccessfully = true
                lastVerificationEmailError = null
                android.util.Log.d("AuthRepository", "Email de vérification envoyé à ${user.email}")
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Échec d'envoi de l'email de vérification", e)
                lastVerificationEmailSentSuccessfully = false
                lastVerificationEmailError = e
            }

            // Save user document in Firestore
            val newUser = User(
                uid = user.uid,
                email = email.trim(),
                username = cleanUsername,
                displayName = null,
                photoUrl = null,
                bio = null,
                birthdate = null,
                createdAt = System.currentTimeMillis()
            )

            try {
                kotlinx.coroutines.withTimeout(10000L) {
                    firestore.collection("users").document(user.uid).set(newUser.toMap(), SetOptions.merge()).await()
                }
                android.util.Log.d("AuthRepository", "Document Firestore users/${user.uid} créé avec succès")
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Échec de création du document Firestore users/${user.uid}", e)
                throw FirestoreCreationException(
                    "Compte créé avec succès, mais échec d'enregistrement du profil (${e.localizedMessage ?: "erreur serveur Firestore"}).",
                    e
                )
            }

            // Save in local dataStore
            dataStoreManager.saveLastUser(
                email = email.trim(),
                username = cleanUsername
            )

            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "signUp failure: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: throw Exception("Connexion impossible.")

            // Fetch firestore user profile to update cached name/username/photo
            try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                if (doc.exists()) {
                    val userData = User.fromMap(doc.data ?: emptyMap())
                    dataStoreManager.saveLastUser(
                        email = user.email ?: email.trim(),
                        name = userData.displayName,
                        username = userData.username,
                        photoUrl = userData.photoUrl
                    )
                } else {
                    dataStoreManager.saveLastUser(email = user.email ?: email.trim())
                }
            } catch (_: Exception) {
                dataStoreManager.saveLastUser(email = user.email ?: email.trim())
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("Aucun utilisateur connecté.")
            user.sendEmailVerification().await()
            lastVerificationEmailSentSuccessfully = true
            lastVerificationEmailError = null
            Result.success(Unit)
        } catch (e: Exception) {
            lastVerificationEmailSentSuccessfully = false
            lastVerificationEmailError = e
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<FirebaseUser?> {
        return try {
            val user = auth.currentUser ?: return Result.success(null)
            user.reload().await()
            Result.success(auth.currentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPasswordReset(code: String, newPassword: String): Result<Unit> {
        return try {
            auth.confirmPasswordReset(code.trim(), newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
                updates["username"] = cleanUsername
                updates["usernameLower"] = cleanUsername
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
            android.util.Log.d("AuthRepository", "Connexion Google annulée par l'utilisateur")
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            android.util.Log.w("AuthRepository", "Aucun compte Google disponible", e)
            GoogleSignInResult.Error("Aucun compte Google disponible sur cet appareil.")
        } catch (e: Exception) {
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

    fun signOut() {
        auth.signOut()
    }
}
