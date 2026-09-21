package com.thehub.hb.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminControlRepository(
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentAdminId: String get() = auth.currentUser?.uid.orEmpty()

    private suspend fun requireAdmin() {
        require(currentAdminId.isNotBlank()) { "Session administrateur invalide." }
        val doc = firestore.collection("admins").document(currentAdminId).get().await()
        require(doc.getBoolean("active") == true) { "Accès administrateur refusé." }
    }

    private suspend fun requireSuperAdmin() {
        requireAdmin()
        val doc = firestore.collection("admins").document(currentAdminId).get().await()
        require(doc.getString("role") == "superadmin") { "Action réservée au superadministrateur." }
    }

    private suspend fun requireRecentReauth() {
        requireAdmin()
        val stamp = firestore.collection("adminSecurity").document(currentAdminId)
            .get().await().getTimestamp("reauthenticatedAt")
        require(stamp != null && System.currentTimeMillis() - stamp.toDate().time <= 15L * 60L * 1000L) {
            "Réauthentification administrateur requise (valide 15 minutes)."
        }
    }

    private fun dayKey(offset: Int = 0): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.DAY_OF_YEAR, offset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(cal.time)
    }

    private fun ts(millis: Long) = Timestamp(Date(millis))

    suspend fun reauthenticateAdmin(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val manager = CredentialManager.create(context)
            val option = GetSignInWithGoogleOption.Builder(
                "183373607979-d1qu0ogpl24dptctim56nlght54hs8a7.apps.googleusercontent.com"
            ).build()
            val response = manager.getCredential(
                GetCredentialRequest.Builder().addCredentialOption(option).build(), context
            )
            val credential = response.credential
            require(
                credential is CustomCredential &&
                    (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)
            ) { "Réauthentification Google impossible." }
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            auth.currentUser?.reauthenticate(GoogleAuthProvider.getCredential(google.idToken, null))?.await()
            firestore.collection("adminSecurity").document(currentAdminId).set(
                mapOf("reauthenticatedAt" to FieldValue.serverTimestamp(), "uid" to currentAdminId), SetOptions.merge()
            ).await()
            audit("reauthenticate_admin", currentAdminId, "security", "success")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dashboard(): Result<AdminDashboardV2> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val users = firestore.collection("users").limit(3000).get().await().documents
            val posts = firestore.collection("posts").limit(3000).get().await().documents
            val comments = firestore.collectionGroup("comments").limit(5000).get().await().documents
            val reports = firestore.collection("reports").limit(2000).get().await().documents
            val logs = firestore.collection("adminLogs").limit(2000).get().await().documents
            val presence = firestore.collectionGroup("presence").limit(5000).get().await().documents
            val now = System.currentTimeMillis()
            fun created(d: com.google.firebase.firestore.DocumentSnapshot) =
                d.getTimestamp("createdAt")?.toDate()?.time ?: (d.getLong("createdAt") ?: 0L)
            fun activeCount(windowMillis: Long) =
                presence.mapNotNull { it.getTimestamp("lastSeen")?.toDate()?.time }
                    .count { it >= now - windowMillis }
            Result.success(AdminDashboardV2(
                users = users.size,
                activeToday = activeCount(24L * 60L * 60L * 1000L),
                active7d = activeCount(7L * 24L * 60L * 60L * 1000L),
                active30d = activeCount(30L * 24L * 60L * 60L * 1000L),
                newUsersToday = users.count { created(it) >= start },
                suspendedUsers = users.count { it.getBoolean("isSuspended") == true },
                postsToday = posts.count { created(it) >= start },
                commentsToday = comments.count { created(it) >= start },
                pendingReports = reports.count { it.getString("status").orEmpty() != "resolved" },
                adminActionsToday = logs.count { created(it) >= start }
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun findUser(query: String): Result<AdminUserDetailV2?> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val q = query.trim()
            require(q.isNotBlank())
            val user = firestore.collection("users").limit(2000).get().await().documents.firstOrNull {
                it.id == q ||
                    it.getString("email").orEmpty().equals(q, true) ||
                    it.getString("username").orEmpty().equals(q.trimStart('@'), true)
            } ?: return@withContext Result.success(null)
            val id = user.id
            val admin = firestore.collection("admins").document(id).get().await()
            val control = firestore.collection("adminUserControls").document(id).get().await()
            Result.success(AdminUserDetailV2(
                uid = id,
                email = user.getString("email").orEmpty(),
                displayName = user.getString("displayName").orEmpty().ifBlank { "Utilisateur" },
                username = user.getString("username").orEmpty(),
                photoUrl = user.getString("photoUrl"),
                createdAt = user.getTimestamp("createdAt"),
                lastSeen = firestore.collection("users").document(id).collection("presence").document("current").get().await().getTimestamp("lastSeen"),
                status = when {
                    user.getBoolean("isDeleted") == true -> "supprimé"
                    user.getBoolean("isSuspended") == true -> "suspendu"
                    else -> "actif"
                },
                role = admin.getString("role").orEmpty().ifBlank { "user" },
                verified = user.getBoolean("isVerified") == true,
                verificationType = user.getString("verificationType"),
                posts = firestore.collection("posts").whereEqualTo("authorId", id).limit(1000).get().await().size(),
                comments = firestore.collectionGroup("comments").whereEqualTo("authorId", id).limit(1000).get().await().size(),
                followers = user.getLong("followersCount") ?: 0L,
                following = user.getLong("followingCount") ?: 0L,
                reports = firestore.collection("reports").whereEqualTo("targetId", id).limit(1000).get().await().size(),
                sanctions = firestore.collection("sanctions").whereEqualTo("userId", id).limit(500).get().await().size(),
                internalNote = control.getString("internalNote").orEmpty()
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setUserControls(userId: String, note: String, post: Boolean, comment: Boolean, message: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                requireRecentReauth()
                firestore.collection("adminUserControls").document(userId).set(
                    mapOf(
                        "userId" to userId,
                        "internalNote" to note.take(2000),
                        "postRestricted" to post,
                        "postRestrictedUntil" to null,
                        "commentRestricted" to comment,
                        "commentRestrictedUntil" to null,
                        "messagingRestricted" to message,
                        "messagingRestrictedUntil" to null,
                        "updatedBy" to currentAdminId,
                        "updatedAt" to Timestamp.now()
                    ), SetOptions.merge()
                ).await()
                audit("update_user_controls", userId, "users", "success")
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun suspendUser(userId: String, value: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
            require(userId != currentAdminId)
            firestore.collection("users").document(userId).set(
                mapOf("isSuspended" to value, "suspendedUntil" to null),
                SetOptions.merge()
            ).await()
            audit(if (value) "suspend_user" else "restore_user", userId, "users", "success")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun applySanction(userId: String, type: String, reason: String, hours: Long?, proof: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                requireRecentReauth()
                require(userId.isNotBlank() && reason.isNotBlank())
                val expires = hours?.takeIf { it > 0 }?.let { ts(System.currentTimeMillis() + it * 3600000L) }
                firestore.collection("sanctions").add(
                    mapOf(
                        "userId" to userId, "type" to type, "reason" to reason.take(1000),
                        "proof" to proof.take(2000), "createdBy" to currentAdminId,
                        "createdAt" to Timestamp.now(), "expiresAt" to expires, "active" to true
                    )
                ).await()
                val controls = mutableMapOf<String, Any?>(
                    "userId" to userId,
                    "updatedBy" to currentAdminId,
                    "updatedAt" to Timestamp.now()
                )
                when (type) {
                    "post_restriction" -> {
                        controls["postRestricted"] = true
                        controls["postRestrictedUntil"] = expires
                    }
                    "comment_restriction" -> {
                        controls["commentRestricted"] = true
                        controls["commentRestrictedUntil"] = expires
                    }
                    "messaging_restriction" -> {
                        controls["messagingRestricted"] = true
                        controls["messagingRestrictedUntil"] = expires
                    }
                    "suspend", "permanent_suspension" -> {
                        controls["suspendedUntil"] = expires
                        firestore.collection("users").document(userId).set(
                            mapOf("isSuspended" to true, "suspendedUntil" to expires),
                            SetOptions.merge()
                        ).await()
                    }
                }
                if (controls.size > 3) {
                    firestore.collection("adminUserControls").document(userId).set(controls, SetOptions.merge()).await()
                }
                audit("apply_sanction", userId, "sanctions", "success")
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun sanctions(): Result<List<AdminSanctionV2>> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            Result.success(firestore.collection("sanctions").limit(500).get().await().documents.map {
                AdminSanctionV2(
                    it.id, it.getString("userId").orEmpty(), it.getString("type").orEmpty(),
                    it.getString("reason").orEmpty(), it.getString("proof").orEmpty(),
                    it.getString("createdBy").orEmpty(), it.getTimestamp("createdAt"), it.getTimestamp("expiresAt")
                )
            }.sortedByDescending { it.createdAt?.seconds ?: 0L })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun moderationQueue(): Result<List<AdminQueueItemV2>> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val reports = firestore.collection("reports").limit(1000).get().await().documents
            val meta = firestore.collection("moderationQueue").limit(1000).get().await().documents.associateBy { it.id }
            Result.success(reports.map {
                val m = meta[it.id]
                AdminQueueItemV2(
                    it.id,
                    it.getString("targetId") ?: it.getString("postId") ?: it.getString("userId").orEmpty(),
                    it.getString("reporterId").orEmpty(),
                    it.getString("reason").orEmpty().ifBlank { "Motif non précisé" },
                    m?.getString("priority").orEmpty().ifBlank { "normal" },
                    m?.getString("status").orEmpty().ifBlank { it.getString("status").orEmpty().ifBlank { "new" } },
                    it.getTimestamp("createdAt")
                )
            }.sortedWith(compareByDescending<AdminQueueItemV2> { it.priority == "urgent" }.thenBy { it.createdAt?.seconds ?: 0L }))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateQueue(id: String, status: String, priority: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
            firestore.collection("moderationQueue").document(id).set(
                mapOf("status" to status, "priority" to priority, "updatedAt" to Timestamp.now(), "updatedBy" to currentAdminId),
                SetOptions.merge()
            ).await()
            audit("moderation_queue_" + status, id, "moderation", "success")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun moderatePost(postId: String, hidden: Boolean, deleted: Boolean, commentsLocked: Boolean, pinned: Boolean, official: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                requireRecentReauth()
                val patch = mapOf(
                    "postId" to postId, "isHidden" to hidden, "isDeletedByAdmin" to deleted,
                    "commentsLocked" to commentsLocked, "isPinned" to pinned, "isOfficial" to official,
                    "updatedBy" to currentAdminId, "updatedAt" to Timestamp.now()
                )
                firestore.collection("postModeration").document(postId).set(patch, SetOptions.merge()).await()
                firestore.collection("posts").document(postId).set(patch, SetOptions.merge()).await()
                audit("moderate_post", postId, "posts", "success")
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun roles(): Result<List<AdminRolePermissionV2>> = withContext(Dispatchers.IO) {
        try {
            requireSuperAdmin()
            Result.success(firestore.collection("admins").limit(200).get().await().documents.map {
                val p = firestore.collection("adminPermissions").document(it.id).get().await()
                    .get("permissions") as? List<*> ?: emptyList<Any?>()
                AdminRolePermissionV2(
                    it.id, it.getString("email").orEmpty(), it.getString("displayName").orEmpty().ifBlank { "Administrateur" },
                    it.getString("role").orEmpty().ifBlank { "admin" }, it.getBoolean("active") != false,
                    p.filterIsInstance<String>().toSet()
                )
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateRole(target: String, role: String, permissions: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
                requireSuperAdmin()
            require(target != currentAdminId)
            require(role in setOf("superadmin", "admin", "moderator", "support", "analyst", "community_manager"))
            firestore.collection("admins").document(target).set(
                mapOf("role" to role, "active" to true), SetOptions.merge()
            ).await()
            firestore.collection("adminPermissions").document(target).set(
                mapOf("uid" to target, "permissions" to permissions.toList(), "updatedBy" to currentAdminId, "updatedAt" to Timestamp.now()),
                SetOptions.merge()
            ).await()
            audit("update_admin_role", target, "roles", "success")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sessions(): Result<List<AdminSessionV2>> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val ids = firestore.collection("admins").whereEqualTo("active", true).get().await().documents.map { it.id }
            val cutoff = System.currentTimeMillis() - 1800000L
            Result.success(ids.map {
                val p = firestore.collection("users").document(it).collection("presence").document("current").get().await()
                val c = firestore.collection("adminSessions").document(it).get().await()
                val last = p.getTimestamp("lastSeen")
                AdminSessionV2(it, last, (last?.toDate()?.time ?: 0L) >= cutoff, c.getTimestamp("revokedAt"))
            })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun revokeSessions(target: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
                requireSuperAdmin()
            firestore.collection("adminSessions").document(target).set(
                mapOf("revokedAt" to Timestamp.now(), "revokedBy" to currentAdminId), SetOptions.merge()
            ).await()
            audit("revoke_admin_sessions", target, "security", "success")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun audits(action: String = "", admin: String = "", target: String = ""): Result<List<AdminAuditV2>> =
        withContext(Dispatchers.IO) {
            try {
                requireAdmin()
                Result.success(firestore.collection("adminLogs").limit(2000).get().await().documents.map {
                    AdminAuditV2(
                        it.id, it.getString("action").orEmpty(), it.getString("adminId").orEmpty(),
                        it.getString("targetId"), it.getString("result").orEmpty().ifBlank { "success" },
                        it.getString("type").orEmpty().ifBlank { "general" }, it.getTimestamp("createdAt")
                    )
                }.filter {
                    (action.isBlank() || it.action.contains(action, true)) &&
                        (admin.isBlank() || it.adminId.contains(admin, true)) &&
                        (target.isBlank() || it.targetId.orEmpty().contains(target, true))
                }.sortedByDescending { it.createdAt?.seconds ?: 0L })
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun sendNotification(title: String, body: String, segment: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
                requireSuperAdmin()
            val users = firestore.collection("users").limit(2000).get().await().documents
            val cutoff = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
            val activeIds = firestore.collectionGroup("presence").limit(5000).get().await().documents
                .filter { (it.getTimestamp("lastSeen")?.toDate()?.time ?: 0L) >= cutoff }
                .mapNotNull { it.reference.parent.parent?.id }.toSet()
            val recipients = users.filter {
                it.id != currentAdminId && when (segment) {
                    "active_7d" -> activeIds.contains(it.id)
                    "admins" -> firestore.collection("admins").document(it.id).get().await().exists()
                    else -> true
                }
            }
            val batch = firestore.batch()
            val campaign = firestore.collection("adminBroadcasts").document()
            recipients.forEach { user ->
                batch.set(
                    firestore.collection("notifications").document(),
                    mapOf(
                        "recipientId" to user.id, "actorId" to currentAdminId, "actorUsername" to "thehub",
                        "actorDisplayName" to auth.currentUser?.displayName.orEmpty().ifBlank { "The Hub" },
                        "type" to "ADMIN_BROADCAST", "commentText" to body.take(1500), "title" to title.take(120),
                        "batchId" to campaign.id, "createdAt" to Timestamp.now(), "isRead" to false, "pushSent" to false
                    )
                )
            }
            if (recipients.isNotEmpty()) batch.commit().await()
            campaign.set(
                mapOf(
                    "title" to title.take(120), "body" to body.take(1500), "segment" to segment,
                    "recipients" to recipients.size, "delivered" to 0, "opened" to 0, "errors" to 0,
                    "createdAt" to Timestamp.now(), "createdBy" to currentAdminId
                )
            ).await()
            audit("send_segmented_notification", campaign.id, "notifications", "success")
            Result.success(recipients.size)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun broadcasts(): Result<List<AdminBroadcastV2>> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val campaigns = firestore.collection("adminBroadcasts").limit(100).get().await().documents
            Result.success(campaigns.map { campaign ->
                val notifications = firestore.collection("notifications")
                    .whereEqualTo("batchId", campaign.id).limit(5000).get().await().documents
                val delivered = notifications.count { (it.getLong("pushSentDeviceCount") ?: 0L) > 0L }
                val opened = notifications.count { it.getBoolean("isRead") == true }
                val errors = notifications.count { it.getString("pushSkippedReason").orEmpty().isNotBlank() }
                AdminBroadcastV2(
                    campaign.id, campaign.getString("title").orEmpty(), campaign.getString("body").orEmpty(),
                    campaign.getString("segment").orEmpty(), campaign.getLong("recipients")?.toInt() ?: notifications.size,
                    delivered, opened, errors, campaign.getTimestamp("createdAt")
                )
            }.sortedByDescending { it.createdAt?.seconds ?: 0L })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun scheduleAnnouncement(title: String, body: String, segment: String, priority: String, publishAt: Timestamp?, expiresAt: Timestamp?): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                requireRecentReauth()
                requireSuperAdmin()
                firestore.collection("announcements").add(
                    mapOf(
                        "title" to title.take(120), "body" to body.take(3000), "segment" to segment,
                        "priority" to priority, "publishAt" to publishAt, "expiresAt" to expiresAt,
                        "status" to if ((publishAt?.seconds ?: 0L) <= Timestamp.now().seconds) "published" else "scheduled",
                        "active" to true, "createdBy" to currentAdminId, "createdAt" to Timestamp.now()
                    )
                ).await()
                audit("schedule_announcement", null, "announcements", "success")
                Result.success(Unit)
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun announcements(): Result<List<AdminAnnouncementV2>> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            Result.success(firestore.collection("announcements").limit(200).get().await().documents.map {
                AdminAnnouncementV2(
                    it.id, it.getString("title").orEmpty(), it.getString("body").orEmpty(),
                    it.getString("segment").orEmpty(), it.getString("priority").orEmpty(),
                    it.getTimestamp("publishAt"), it.getTimestamp("expiresAt"),
                    it.getString("status").orEmpty().ifBlank { "published" }
                )
            }.sortedByDescending { it.publishAt?.seconds ?: 0L })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun version(): Result<AdminVersionV2> = withContext(Dispatchers.IO) {
        try {
            requireAdmin()
            val d = firestore.collection("system").document("version").get().await()
            Result.success(AdminVersionV2(
                d.getString("versionName").orEmpty().ifBlank { "1.0.149" },
                d.getLong("versionCode") ?: 149L, d.getLong("minimumSupportedCode") ?: 0L,
                d.getString("apkUrl").orEmpty(), d.getString("releaseNotes").orEmpty(),
                d.getBoolean("forceUpdate") == true, d.getTimestamp("updatedAt")
            ))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun setVersion(v: AdminVersionV2): Result<Unit> = withContext(Dispatchers.IO) {
        try {
                requireRecentReauth()
                requireSuperAdmin()
            firestore.collection("system").document("version").set(
                mapOf(
                    "versionName" to v.versionName, "versionCode" to v.versionCode,
                    "minimumSupportedCode" to v.minimumSupportedCode, "apkUrl" to v.apkUrl,
                    "releaseNotes" to v.releaseNotes, "forceUpdate" to v.forceUpdate,
                    "updatedAt" to Timestamp.now(), "updatedBy" to currentAdminId
                ), SetOptions.merge()
            ).await()
            audit("update_version_policy", null, "system", "success")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun audit(action: String, target: String?, type: String, result: String) {
        try {
            firestore.collection("adminLogs").add(
                mapOf(
                    "adminId" to currentAdminId, "action" to action, "targetId" to target,
                    "type" to type, "result" to result, "createdAt" to Timestamp.now()
                )
            ).await()
        } catch (_: Exception) {}
    }
}
