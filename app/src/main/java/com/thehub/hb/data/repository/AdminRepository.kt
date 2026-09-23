package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

data class AdminCommentRow(
    val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Timestamp?,
    val likes: Long
)

data class AdminSupportTicketRow(
    val id: String,
    val userId: String,
    val subject: String,
    val message: String,
    val status: String,
    val priority: String,
    val createdAt: Timestamp?
)

data class AdminAnnouncementRow(
    val id: String,
    val title: String,
    val body: String,
    val active: Boolean,
    val createdAt: Timestamp?
)

data class AdminSystemConfig(
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val newRegistrations: Boolean = true,
    val postsEnabled: Boolean = true,
    val messagingEnabled: Boolean = true
)

data class AdminAnalytics(
    val users: Int = 0,
    val activeUsers: Int = 0,
    val suspendedUsers: Int = 0,
    val deletedUsers: Int = 0,
    val posts: Int = 0,
    val comments: Int = 0,
    val reports: Int = 0,
    val pendingReports: Int = 0,
    val admins: Int = 0,
    val users7d: Int = 0,
    val posts7d: Int = 0,
    val users30d: Int = 0,
    val posts30d: Int = 0
) 

data class AdminSearchHit(
    val type: String,
    val id: String,
    val title: String,
    val subtitle: String
)

data class AdminSanctionRow(
    val id: String,
    val userId: String,
    val type: String,
    val reason: String,
    val createdBy: String,
    val createdAt: Timestamp?,
    val expiresAt: Timestamp?
)

data class AdminAntiSpamAlert(
    val userId: String,
    val label: String,
    val score: Int,
    val reason: String
)

data class AdminStorageStats(
    val imagePosts: Int = 0,
    val videoPosts: Int = 0,
    val mediaPosts: Int = 0
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

// Validation marker: final admin v4 current-main check.
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
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "isVerified" to true,
                        "verificationType" to "admin"
                    )
                ).await()
            } catch (_: Exception) {
            }
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
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "isVerified" to true,
                        "verificationType" to "admin"
                    )
                ).await()
            } catch (_: Exception) {
            }
            isCurrentUserSuperAdmin()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun globalSearch(query: String): Result<List<AdminSearchHit>> = withContext(Dispatchers.IO) {
        try {
            val q = query.trim().lowercase()
            require(q.length >= 2) { "La recherche doit contenir au moins 2 caractères." }

            val users = firestore.collection("users").limit(300).get().await().documents
            val posts = firestore.collection("posts").limit(300).get().await().documents
            val reports = firestore.collection("reports").limit(200).get().await().documents

            val hits = mutableListOf<AdminSearchHit>()
            users.filter {
                listOf(
                    it.id,
                    it.getString("email").orEmpty(),
                    it.getString("username").orEmpty(),
                    it.getString("displayName").orEmpty()
                ).any { value -> value.lowercase().contains(q) }
            }.take(30).forEach {
                hits += AdminSearchHit(
                    "Utilisateur",
                    it.id,
                    it.getString("displayName").orEmpty().ifBlank { "Utilisateur" },
                    it.getString("email").orEmpty().ifBlank { "@" + it.getString("username").orEmpty() }
                )
            }
            posts.filter {
                listOf(it.id, it.getString("authorId").orEmpty(), it.getString("text").orEmpty())
                    .any { value -> value.lowercase().contains(q) }
            }.take(30).forEach {
                hits += AdminSearchHit(
                    "Publication",
                    it.id,
                    it.getString("text").orEmpty().ifBlank { "Publication sans texte" }.take(90),
                    "Auteur " + it.getString("authorId").orEmpty()
                )
            }
            reports.filter {
                listOf(it.id, it.getString("reporterId").orEmpty(), it.getString("targetId").orEmpty(), it.getString("reason").orEmpty())
                    .any { value -> value.lowercase().contains(q) }
            }.take(20).forEach {
                hits += AdminSearchHit(
                    "Signalement",
                    it.id,
                    it.getString("reason").orEmpty().ifBlank { "Signalement" },
                    "Cible " + it.getString("targetId").orEmpty()
                )
            }
            Result.success(hits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSanctions(): Result<List<AdminSanctionRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = firestore.collection("sanctions")
                .limit(300)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminSanctionRow(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        type = doc.getString("type").orEmpty(),
                        reason = doc.getString("reason").orEmpty(),
                        createdBy = doc.getString("createdBy").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt"),
                        expiresAt = doc.getTimestamp("expiresAt")
                    )
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applySanction(
        userId: String,
        type: String,
        reason: String,
        durationHours: Long?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(userId.isNotBlank()) { "Utilisateur invalide." }
            require(reason.isNotBlank()) { "Motif obligatoire." }
            val now = Timestamp.now()
            val expires = durationHours?.takeIf { it > 0 }?.let {
                Timestamp(java.util.Date(System.currentTimeMillis() + it * 60L * 60L * 1000L))
            }
            firestore.collection("sanctions").add(
                mapOf(
                    "userId" to userId,
                    "type" to type,
                    "reason" to reason.trim().take(500),
                    "createdBy" to currentUserId,
                    "createdAt" to now,
                    "expiresAt" to expires
                )
            ).await()

            if (type == "suspend" || type == "ban") {
                firestore.collection("users").document(userId).update("isSuspended", true).await()
            }
            writeLog("apply_sanction", userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun broadcastAnnouncement(title: String, body: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                require(isCurrentUserSuperAdmin()) { "Seul le superadministrateur peut diffuser une annonce." }
                require(title.isNotBlank() && body.isNotBlank()) { "Titre et message obligatoires." }
                val actor = auth.currentUser
                val actorId = actor?.uid.orEmpty()
                val actorName = actor?.displayName.orEmpty().ifBlank { "The Hub" }
                val snapshot = firestore.collection("users").limit(450).get().await()
                val batch = firestore.batch()
                var count = 0
                snapshot.documents.forEach { user ->
                    val recipient = user.id
                    if (recipient.isNotBlank() && recipient != actorId) {
                        val ref = firestore.collection("notifications").document()
                        batch.set(
                            ref,
                            mapOf(
                                "recipientId" to recipient,
                                "actorId" to actorId,
                                "actorUsername" to "thehub",
                                "actorDisplayName" to actorName,
                                "actorPhotoUrl" to actor?.photoUrl?.toString(),
                                "type" to com.thehub.hb.data.model.NotificationItem.TYPE_ANNOUNCEMENT,
                                "commentText" to body.trim().take(1500),
                                "createdAt" to Timestamp.now(),
                                "isRead" to false,
                                "pushSent" to false
                            )
                        )
                        count++
                    }
                }
                if (count > 0) batch.commit().await()
                writeLog("broadcast_announcement", count.toString())
                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAntiSpamAlerts(): Result<List<AdminAntiSpamAlert>> = withContext(Dispatchers.IO) {
        try {
            val posts = firestore.collection("posts").limit(500).get().await().documents
            val reports = firestore.collection("reports").limit(500).get().await().documents
            val duplicateCounts = posts
                .mapNotNull { doc ->
                    val text = doc.getString("text").orEmpty().trim().lowercase()
                    val author = doc.getString("authorId").orEmpty()
                    if (text.length < 20 || author.isBlank()) null else author to text.replace(Regex("\\s+"), " ")
                }
                .groupBy { it }
                .mapValues { it.value.size }
            val reportCounts = reports
                .mapNotNull {
                    val target = it.getString("targetId").orEmpty()
                    target.takeIf { value -> value.isNotBlank() }
                }
                .groupingBy { it }
                .eachCount()

            val alerts = mutableListOf<AdminAntiSpamAlert>()
            reportCounts.filter { it.value >= 3 }.forEach { (userId, count) ->
                alerts += AdminAntiSpamAlert(userId, "Utilisateur", (count * 10).coerceAtMost(100), "$count signalements ciblés")
            }
            duplicateCounts.filter { it.value >= 3 }.forEach { (key, count) ->
                val userId = key.first
                alerts += AdminAntiSpamAlert(userId, "Publications", (count * 12).coerceAtMost(100), "$count publications identiques ou quasi identiques")
            }
            Result.success(alerts.distinctBy { it.userId + it.reason }.sortedByDescending { it.score })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStorageStats(): Result<AdminStorageStats> = withContext(Dispatchers.IO) {
        try {
            val posts = firestore.collection("posts").limit(1000).get().await().documents
            val image = posts.count { !it.getString("imageUrl").isNullOrBlank() }
            val video = posts.count { !it.getString("videoUrl").isNullOrBlank() }
            Result.success(AdminStorageStats(image, video, image + video))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStats(): Result<AdminStats> = withContext(Dispatchers.IO) {
        try {
            val (users, posts, reports, admins) = coroutineScope {
                val deferred = listOf(
                    async { firestore.collection("users").get().await().size() },
                    async { firestore.collection("posts").get().await().size() },
                    async { firestore.collection("reports").get().await().size() },
                    async { firestore.collection("admins").get().await().size() }
                )
                deferred.awaitAll().let { values ->
                    QuadStats(values[0], values[1], values[2], values[3])
                }
            }
            Result.success(AdminStats(users, posts, reports, admins))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class QuadStats(
        val users: Int,
        val posts: Int,
        val reports: Int,
        val admins: Int
    )

    suspend fun getAnalytics(): Result<AdminAnalytics> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val sevenDays = now - 7L * 24L * 60L * 60L * 1000L
            val thirtyDays = now - 30L * 24L * 60L * 60L * 1000L

            val userDocs = firestore.collection("users").limit(1000).get().await().documents
            val postDocs = firestore.collection("posts").limit(1000).get().await().documents
            val commentDocs = firestore.collectionGroup("comments").limit(1000).get().await().documents
            val reportDocs = firestore.collection("reports").limit(1000).get().await().documents
            val adminDocs = firestore.collection("admins").limit(100).get().await().documents
            val presenceDocs = firestore.collectionGroup("presence").limit(1000).get().await().documents

            fun createdMillis(doc: com.google.firebase.firestore.DocumentSnapshot): Long {
                val value = doc.get("createdAt")
                return when (value) {
                    is Timestamp -> value.toDate().time
                    is Number -> value.toLong()
                    else -> 0L
                }
            }

            val usersCount = userDocs.size
            val suspended = userDocs.count { it.getBoolean("isSuspended") == true }
            val deleted = userDocs.count { it.getBoolean("isDeleted") == true }
            val active = userDocsCountActive(userDocs, now)
            val users7 = userDocs.count { createdMillis(it) >= sevenDays }
            val users30 = userDocs.count { createdMillis(it) >= thirtyDays }
            val posts7 = postDocs.count { createdMillis(it) >= sevenDays }
            val posts30 = postDocs.count { createdMillis(it) >= thirtyDays }
            val activeWindow = now - 30L * 60L * 1000L
            val activeUsers = presenceDocs.mapNotNull { it.getTimestamp("lastSeen")?.toDate()?.time }
                .count { it >= activeWindow }

            Result.success(
                AdminAnalytics(
                    users = usersCount,
                    activeUsers = maxOf(activeUsers, 0).coerceAtMost(usersCount),
                    suspendedUsers = suspended,
                    deletedUsers = deleted,
                    posts = postDocs.size,
                    comments = commentDocs.size,
                    reports = reportDocs.size,
                    pendingReports = reportDocs.count { it.getString("status").orEmpty() != "resolved" },
                    admins = adminDocs.size,
                    users7d = users7,
                    posts7d = posts7,
                    users30d = users30,
                    posts30d = posts30
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun userDocsCountActive(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
        now: Long
    ): Int {
        return docs.count { doc ->
            doc.getBoolean("isDeleted") != true
                && doc.getBoolean("isSuspended") != true
        }
    }

    suspend fun getComments(): Result<List<AdminCommentRow>> = withContext(Dispatchers.IO) {
        try {
            val rows = firestore.collectionGroup("comments")
                .limit(300)
                .get()
                .await()
                .documents
                .map { doc ->
                    AdminCommentRow(
                        id = doc.id,
                        postId = doc.reference.parent.parent?.id.orEmpty(),
                        authorId = doc.getString("authorId").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt"),
                        likes = doc.getLong("likesCount") ?: 0L
                    )
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(rows)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                require(postId.isNotBlank() && commentId.isNotBlank()) { "Commentaire invalide." }
                firestore.collection("posts").document(postId)
                    .collection("comments").document(commentId).delete().await()
                try {
                    firestore.collection("posts").document(postId)
                        .update(
                            "commentsCount",
                            com.google.firebase.firestore.FieldValue.increment(-1)
                        ).await()
                } catch (_: Exception) {
                }
                writeLog("delete_comment", commentId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSupportTickets(): Result<List<AdminSupportTicketRow>> =
        withContext(Dispatchers.IO) {
            try {
                val rows = firestore.collection("supportTickets")
                    .limit(300)
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        AdminSupportTicketRow(
                            id = doc.id,
                            userId = doc.getString("userId").orEmpty(),
                            subject = doc.getString("subject").orEmpty().ifBlank { "Demande de support" },
                            message = doc.getString("message").orEmpty(),
                            status = doc.getString("status").orEmpty().ifBlank { "open" },
                            priority = doc.getString("priority").orEmpty().ifBlank { "normal" },
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                Result.success(rows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun resolveSupportTicket(ticketId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                require(ticketId.isNotBlank()) { "Ticket invalide." }
                firestore.collection("supportTickets").document(ticketId)
                    .update(
                        mapOf(
                            "status" to "resolved",
                            "resolvedAt" to Timestamp.now(),
                            "resolvedBy" to currentUserId
                        )
                    ).await()
                writeLog("resolve_support_ticket", ticketId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAnnouncements(): Result<List<AdminAnnouncementRow>> =
        withContext(Dispatchers.IO) {
            try {
                val rows = firestore.collection("announcements")
                    .limit(100)
                    .get()
                    .await()
                    .documents
                    .map { doc ->
                        AdminAnnouncementRow(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            body = doc.getString("body").orEmpty(),
                            active = doc.getBoolean("active") != false,
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                Result.success(rows)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun publishAnnouncement(title: String, body: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                require(title.isNotBlank()) { "Titre obligatoire." }
                require(body.isNotBlank()) { "Message obligatoire." }
                firestore.collection("announcements").add(
                    mapOf(
                        "title" to title.trim().take(120),
                        "body" to body.trim().take(2000),
                        "active" to true,
                        "createdAt" to Timestamp.now(),
                        "createdBy" to currentUserId
                    )
                ).await()
                writeLog("publish_announcement", null)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun setSystemConfig(config: AdminSystemConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("system").document("config")
                    .set(
                        mapOf(
                            "maintenance" to config.maintenance,
                            "maintenanceMessage" to config.maintenanceMessage.take(500),
                            "newRegistrations" to config.newRegistrations,
                            "postsEnabled" to config.postsEnabled,
                            "messagingEnabled" to config.messagingEnabled,
                            "updatedAt" to Timestamp.now(),
                            "updatedBy" to currentUserId
                        ),
                        SetOptions.merge()
                    ).await()
                writeLog("update_system_config", null)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSystemConfig(): Result<AdminSystemConfig> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("system").document("config").get().await()
            Result.success(
                AdminSystemConfig(
                    maintenance = doc.getBoolean("maintenance") == true,
                    maintenanceMessage = doc.getString("maintenanceMessage").orEmpty(),
                    newRegistrations = doc.getBoolean("newRegistrations") != false,
                    postsEnabled = doc.getBoolean("postsEnabled") != false,
                    messagingEnabled = doc.getBoolean("messagingEnabled") != false
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveSessions(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cutoff = Timestamp(java.util.Date(System.currentTimeMillis() - 30L * 60L * 1000L))
            val count = firestore.collectionGroup("presence")
                .whereGreaterThanOrEqualTo("lastSeen", cutoff)
                .limit(1000)
                .get()
                .await()
                .documents
                .count { it.getBoolean("online") == true || (it.getTimestamp("lastSeen")?.compareTo(cutoff) ?: -1) >= 0 }
            Result.success(count)
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
            firestore.collection("users").document(targetUid).update(
                mapOf(
                    "isVerified" to true,
                    "verificationType" to "admin"
                )
            ).await()
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
            firestore.collection("users").document(targetUid).update(
                mapOf(
                    "isVerified" to false,
                    "verificationType" to null
                )
            ).await()
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
