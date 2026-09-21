package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AdminStats(
    val users: Int = 0,
    val posts: Int = 0,
    val reports: Int = 0,
    val admins: Int = 0
)

data class AdminUserRow(
    val uid: String,
    val email: String,
    val displayName: String,
    val username: String,
    val photoUrl: String?,
    val postsCount: Long,
    val followersCount: Long,
    val followingCount: Long,
    val deleted: Boolean,
    val suspended: Boolean
)

data class AdminLogRow(
    val action: String,
    val adminId: String,
    val targetId: String?,
    val createdAt: Timestamp?
)

data class AdminPostRow(
    val id: String,
    val authorId: String,
    val text: String,
    val createdAt: Timestamp?,
    val imageUrl: String?,
    val likes: Long,
    val comments: Long
)

data class AdminReportRow(
    val id: String,
    val reporterId: String,
    val targetId: String,
    val reason: String,
    val status: String
)

data class AdminAccountRow(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String,
    val active: Boolean
)

// Validation marker: admin v3 current-main CI check.
class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "kidasniger@gmail.com"
    }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun isCurrentUserAdmin(): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        try {
            firestore.collection("admins").document(uid).get().await()
                .getBoolean("active") == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isCurrentUserSuperAdmin(): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext false
        try {
            val doc = firestore.collection("admins").document(uid).get().await()
            doc.getBoolean("active") == true && doc.getString("role") == "superadmin"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Registers the authenticated Google account as the first administrator when
     * its verified email matches the bootstrap allowlist enforced by Firestore rules.
     */
    suspend fun ensureBootstrapAdmin(): Boolean = withContext(Dispatchers.IO) {
        val user = auth.currentUser ?: return@withContext false
        val uid = user.uid
        val email = user.email?.trim()?.lowercase()
            ?: return@withContext false

        val existing = firestore.collection("admins").document(uid).get().await()
        if (
            existing.getBoolean("active") == true
            && existing.getString("role") == "superadmin"
        ) {
            return@withContext true
        }

        if (!user.isEmailVerified || email != BOOTSTRAP_ADMIN_EMAIL) {
            return@withContext false
        }

        try {
            if (existing.exists()) {
                firestore.collection("admins").document(uid).update(
                    mapOf(
                        "email" to email,
                        "displayName" to (user.displayName ?: "KIDAS"),
                        "role" to "superadmin",
                        "active" to true
                    )
                ).await()
            } else {
                firestore.collection("admins").document(uid)
                    .set(
                        mapOf(
                            "uid" to uid,
                            "email" to email,
                            "displayName" to (user.displayName ?: "KIDAS"),
                            "role" to "superadmin",
                            "active" to true,
                            "createdAt" to Timestamp.now(),
                            "createdBy" to uid
                        ),
                        SetOptions.merge()
                    )
                    .await()
            }
            isCurrentUserSuperAdmin()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getStats(): Result<AdminStats> = withContext(Dispatchers.IO) {
        try {
            val users = firestore.collection("users").get().await().size()
            val posts = firestore.collection("posts").get().await().size()
            val reports = firestore.collection("reports").get().await().size()
            val admins = firestore.collection("admins").get().await().size()
            Result.success(AdminStats(users, posts, reports, admins))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsers(): Result<List<AdminUserRow>> = withContext(Dispatchers.IO) {
        try {
            val activeAdminIds = firestore.collection("admins")
                .whereEqualTo("active", true)
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()

            val rows = firestore.collection("users")
                .limit(200)
                .get()
                .await()
                .documents
                .asSequence()
                .filterNot { activeAdminIds.contains(it.id) }
                .map { doc ->
                    AdminUserRow(
                        uid = doc.id,
                        email = doc.getString("email").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty().ifBlank { "Utilisateur" },
                        username = doc.getString("username").orEmpty(),
                        photoUrl = doc.getString("photoUrl"),
                        postsCount = doc.getLong("postsCount") ?: 0L,
                        followersCount = doc.getLong("followersCount") ?: 0L,
                        followingCount = doc.getLong("followingCount") ?: 0L,
                        deleted = doc.getBoolean("isDeleted") == true,
                        suspended = doc.getBoolean("isSuspended") == true
                    )
                }
                .sortedBy { it.displayName.lowercase() }
                .toList()

            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentLogs(limit: Long = 30): Result<List<AdminLogRow>> =
        withContext(Dispatchers.IO) {
            try {
                val rows = firestore.collection("adminLogs")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        AdminLogRow(
                            action = doc.getString("action").orEmpty(),
                            adminId = doc.getString("adminId").orEmpty(),
                            targetId = doc.getString("targetId"),
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                Result.success(rows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun setUserSuspended(uid: String, suspended: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                require(uid.isNotBlank()) { "UID utilisateur invalide." }
                require(uid != currentUserId) { "Un administrateur ne peut pas se désactiver lui-même." }
                firestore.collection("users").document(uid)
                    .update("isSuspended", suspended)
                    .await()
                writeLog(
                    action = if (suspended) "suspend_user" else "restore_user",
                    targetId = uid
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun setUserDeleted(uid: String, deleted: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                require(uid.isNotBlank()) { "UID utilisateur invalide." }
                require(uid != currentUserId) { "Un administrateur ne peut pas supprimer son propre compte." }
                firestore.collection("users").document(uid)
                    .update("isDeleted", deleted)
                    .await()
                writeLog(
                    action = if (deleted) "delete_user" else "restore_deleted_user",
                    targetId = uid
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPosts(): Result<List<AdminPostRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = firestore.collection("posts")
                .limit(200)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminPostRow(
                        id = doc.id,
                        authorId = doc.getString("authorId").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt"),
                        imageUrl = doc.getString("imageUrl"),
                        likes = doc.getLong("likesCount") ?: 0L,
                        comments = doc.getLong("commentsCount") ?: 0L
                    )
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(postId.isNotBlank()) { "Publication invalide." }
            firestore.collection("posts").document(postId).delete().await()
            writeLog("delete_post", postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(): Result<List<AdminReportRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = firestore.collection("reports")
                .limit(200)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminReportRow(
                        id = doc.id,
                        reporterId = doc.getString("reporterId").orEmpty(),
                        targetId = doc.getString("targetId")
                            ?: doc.getString("postId")
                            ?: doc.getString("userId")
                            ?: "",
                        reason = doc.getString("reason").orEmpty().ifBlank { "Motif non précisé" },
                        status = doc.getString("status").orEmpty().ifBlank { "pending" }
                    )
                }
                .sortedBy { it.status }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveReport(reportId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("reports").document(reportId)
                .set(
                    mapOf(
                        "status" to "resolved",
                        "resolvedAt" to Timestamp.now(),
                        "resolvedBy" to currentUserId
                    ),
                    SetOptions.merge()
                )
                .await()
            writeLog("resolve_report", reportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdmins(): Result<List<AdminAccountRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = firestore.collection("admins")
                .limit(100)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminAccountRow(
                        uid = doc.id,
                        email = doc.getString("email").orEmpty(),
                        displayName = doc.getString("displayName").orEmpty().ifBlank { "Administrateur" },
                        role = doc.getString("role").orEmpty().ifBlank { "admin" },
                        active = doc.getBoolean("active") != false
                    )
                }
                .sortedBy { it.displayName.lowercase() }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addAdmin(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val creatorUid = currentUserId ?: throw IllegalStateException("Session administrateur invalide.")
            require(isCurrentUserSuperAdmin()) { "Seul le superadministrateur peut gérer les administrateurs." }
            require(targetUid.isNotBlank()) { "UID invalide." }

            val userDoc = firestore.collection("users").document(targetUid).get().await()
            require(userDoc.exists()) { "Utilisateur introuvable." }

            firestore.collection("admins").document(targetUid)
                .set(
                    mapOf(
                        "uid" to targetUid,
                        "email" to userDoc.getString("email").orEmpty(),
                        "displayName" to userDoc.getString("displayName").orEmpty(),
                        "role" to "admin",
                        "active" to true,
                        "createdAt" to Timestamp.now(),
                        "createdBy" to creatorUid
                    ),
                    SetOptions.merge()
                )
                .await()
            writeLog("add_admin", targetUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAdmin(targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(isCurrentUserSuperAdmin()) { "Seul le superadministrateur peut gérer les administrateurs." }
            require(targetUid.isNotBlank()) { "UID invalide." }
            require(targetUid != currentUserId) { "Impossible de supprimer votre propre accès administrateur." }
            firestore.collection("admins").document(targetUid).delete().await()
            writeLog("remove_admin", targetUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun writeLog(action: String, targetId: String?) {
        try {
            firestore.collection("adminLogs").add(
                mapOf(
                    "adminId" to currentUserId,
                    "action" to action,
                    "targetId" to targetId,
                    "createdAt" to Timestamp.now()
                )
            ).await()
        } catch (_: Exception) {
        }
    }
}
