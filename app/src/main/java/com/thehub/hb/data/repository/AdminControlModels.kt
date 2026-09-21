package com.thehub.hb.data.repository

import com.google.firebase.Timestamp

data class AdminDashboardV2(
    val users: Int = 0, val activeToday: Int = 0, val active7d: Int = 0, val active30d: Int = 0,
    val newUsersToday: Int = 0, val suspendedUsers: Int = 0, val postsToday: Int = 0,
    val commentsToday: Int = 0, val pendingReports: Int = 0, val adminActionsToday: Int = 0
)
data class AdminUserDetailV2(
    val uid: String, val email: String, val displayName: String, val username: String,
    val photoUrl: String?, val createdAt: Timestamp?, val lastSeen: Timestamp?, val status: String,
    val role: String, val verified: Boolean, val verificationType: String?, val posts: Int,
    val comments: Int, val followers: Long, val following: Long, val reports: Int, val sanctions: Int,
    val internalNote: String
)
data class AdminSanctionV2(
    val id: String, val userId: String, val type: String, val reason: String, val proof: String,
    val createdBy: String, val createdAt: Timestamp?, val expiresAt: Timestamp?
)
data class AdminQueueItemV2(
    val id: String, val targetId: String, val reporterId: String, val reason: String,
    val priority: String, val status: String, val createdAt: Timestamp?
)
data class AdminAuditV2(
    val id: String, val action: String, val adminId: String, val targetId: String?,
    val result: String, val type: String, val createdAt: Timestamp?
)
data class AdminRolePermissionV2(
    val uid: String, val email: String, val displayName: String, val role: String,
    val active: Boolean, val permissions: Set<String>
)
data class AdminSessionV2(
    val uid: String, val lastSeen: Timestamp?, val active: Boolean, val revokedAt: Timestamp?
)
data class AdminBroadcastV2(
    val id: String, val title: String, val body: String, val segment: String, val recipients: Int,
    val delivered: Int, val opened: Int, val errors: Int, val createdAt: Timestamp?
)
data class AdminAnnouncementV2(
    val id: String, val title: String, val body: String, val segment: String, val priority: String,
    val publishAt: Timestamp?, val expiresAt: Timestamp?, val status: String
)
data class AdminVersionV2(
    val versionName: String, val versionCode: Long, val minimumSupportedCode: Long,
    val apkUrl: String, val releaseNotes: String, val forceUpdate: Boolean, val updatedAt: Timestamp?
)
data class AdminFeatureFlagV2(val key: String, val enabled: Boolean, val description: String)
data class AdminEmergencyV2(
    val maintenance: Boolean = false, val registrations: Boolean = true, val posts: Boolean = true,
    val comments: Boolean = true, val messaging: Boolean = true, val notifications: Boolean = true,
    val uploads: Boolean = true, val message: String = ""
)
data class AdminInvestigationEventV2(
    val action: String, val targetId: String?, val adminId: String, val source: String, val createdAt: Timestamp?
)
data class AdminAnalyticsV2(
    val dau: Int = 0, val wau: Int = 0, val mau: Int = 0, val retention7d: Int = 0,
    val posts: Int = 0, val comments: Int = 0, val likes: Int = 0, val reports: Int = 0,
    val suspensions: Int = 0
)
data class AdminAntiSpamConfigV2(
    val postsPerHour: Long = 10, val commentsPerHour: Long = 30, val messagesPerHour: Long = 60,
    val loginAttemptsPerHour: Long = 20, val reportsPerHour: Long = 10,
    val duplicateWindowMinutes: Long = 30, val botScoreThreshold: Long = 80
)
data class AdminSupportTicketV2(
    val id: String, val userId: String, val subject: String, val message: String, val status: String,
    val priority: String, val assignedAdminId: String, val createdAt: Timestamp?
)
