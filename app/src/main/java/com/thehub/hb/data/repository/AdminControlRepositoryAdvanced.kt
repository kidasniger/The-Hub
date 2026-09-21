package com.thehub.hb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private fun v2Day(offset: Int = 0): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    c.add(Calendar.DAY_OF_YEAR, offset)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
}

private suspend fun AdminControlRepository.ensureAdminV2(superOnly: Boolean = false) {
    val me = firestore.collection("admins").document(currentAdminId).get().await()
    require(me.getBoolean("active") == true) { "Accès administrateur refusé." }
    if (superOnly) require(me.getString("role") == "superadmin") { "Action réservée au superadministrateur." }
}

suspend fun AdminControlRepository.featureFlags(): Result<List<AdminFeatureFlagV2>> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        Result.success(firestore.collection("featureFlags").limit(200).get().await().documents.map {
            AdminFeatureFlagV2(it.id, it.getBoolean("enabled") == true, it.getString("description").orEmpty())
        }.sortedBy { it.key })
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.setFeatureFlag(key: String, enabled: Boolean, description: String): Result<Unit> =
    withContext(Dispatchers.IO) {
        try {
            ensureAdminV2(true)
            firestore.collection("featureFlags").document(key).set(
                mapOf("enabled" to enabled, "description" to description.take(500),
                    "updatedBy" to currentAdminId, "updatedAt" to Timestamp.now()), SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

suspend fun AdminControlRepository.emergency(): Result<AdminEmergencyV2> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        val d = firestore.collection("system").document("emergency").get().await()
        Result.success(AdminEmergencyV2(
            maintenance = d.getBoolean("maintenance") == true,
            registrations = d.getBoolean("registrations") != false,
            posts = d.getBoolean("posts") != false,
            comments = d.getBoolean("comments") != false,
            messaging = d.getBoolean("messaging") != false,
            notifications = d.getBoolean("notifications") != false,
            uploads = d.getBoolean("uploads") != false,
            message = d.getString("message").orEmpty()
        ))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.setEmergency(value: AdminEmergencyV2): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2(true)
        firestore.collection("system").document("emergency").set(
            mapOf(
                "maintenance" to value.maintenance, "registrations" to value.registrations,
                "posts" to value.posts, "comments" to value.comments, "messaging" to value.messaging,
                "notifications" to value.notifications, "uploads" to value.uploads,
                "message" to value.message.take(1000),
                "updatedBy" to currentAdminId, "updatedAt" to Timestamp.now()
            ), SetOptions.merge()
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.investigation(userId: String): Result<List<AdminInvestigationEventV2>> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        val events = mutableListOf<AdminInvestigationEventV2>()
        firestore.collection("adminLogs").whereEqualTo("targetId", userId).limit(500).get().await().documents.forEach {
            events += AdminInvestigationEventV2(it.getString("action").orEmpty(), userId, it.getString("adminId").orEmpty(), "audit", it.getTimestamp("createdAt"))
        }
        firestore.collection("sanctions").whereEqualTo("userId", userId).limit(500).get().await().documents.forEach {
            events += AdminInvestigationEventV2("sanction_" + it.getString("type").orEmpty(), userId, it.getString("createdBy").orEmpty(), "sanctions", it.getTimestamp("createdAt"))
        }
        firestore.collection("reports").whereEqualTo("targetId", userId).limit(500).get().await().documents.forEach {
            events += AdminInvestigationEventV2("report_" + it.getString("status").orEmpty(), userId, it.getString("reporterId").orEmpty(), "reports", it.getTimestamp("createdAt"))
        }
        Result.success(events.sortedByDescending { it.createdAt?.seconds ?: 0L })
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.analyticsV2(): Result<AdminAnalyticsV2> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        val active = firestore.collectionGroup("activeUsers")
            .whereGreaterThanOrEqualTo("day", v2Day(-29))
            .whereLessThanOrEqualTo("day", v2Day()).limit(20000).get().await().documents
        val dau = active.filter { it.getString("day") == v2Day() }.mapNotNull { it.getString("userId") }.toSet().size
        val wau = active.filter { it.getString("day").orEmpty() >= v2Day(-6) }.mapNotNull { it.getString("userId") }.toSet().size
        val mau = active.mapNotNull { it.getString("userId") }.toSet().size
        val cohortStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -8) }.timeInMillis
        val cohortEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
        val cohort = firestore.collection("users").get().await().documents.filter {
            val t = it.getTimestamp("createdAt")?.toDate()?.time ?: (it.getLong("createdAt") ?: 0L)
            t in cohortStart..cohortEnd
        }.map { it.id }.toSet()
        val retained = active.filter { it.getString("day") == v2Day() }.mapNotNull { it.getString("userId") }.toSet().intersect(cohort).size
        Result.success(AdminAnalyticsV2(
            dau, wau, mau, retained,
            firestore.collection("posts").limit(2000).get().await().size(),
            firestore.collectionGroup("comments").limit(3000).get().await().size(),
            0,
            firestore.collection("reports").limit(2000).get().await().size(),
            firestore.collection("sanctions").whereEqualTo("type", "suspend").limit(500).get().await().size()
        ))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.antiSpamConfig(): Result<AdminAntiSpamConfigV2> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        val d = firestore.collection("antiSpam").document("config").get().await()
        Result.success(AdminAntiSpamConfigV2(
            d.getLong("postsPerHour") ?: 10, d.getLong("commentsPerHour") ?: 30, d.getLong("messagesPerHour") ?: 60,
            d.getLong("loginAttemptsPerHour") ?: 20, d.getLong("reportsPerHour") ?: 10,
            d.getLong("duplicateWindowMinutes") ?: 30, d.getLong("botScoreThreshold") ?: 80
        ))
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.setAntiSpamConfig(v: AdminAntiSpamConfigV2): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2(true)
        firestore.collection("antiSpam").document("config").set(
            mapOf(
                "postsPerHour" to v.postsPerHour, "commentsPerHour" to v.commentsPerHour,
                "messagesPerHour" to v.messagesPerHour, "loginAttemptsPerHour" to v.loginAttemptsPerHour,
                "reportsPerHour" to v.reportsPerHour, "duplicateWindowMinutes" to v.duplicateWindowMinutes,
                "botScoreThreshold" to v.botScoreThreshold, "updatedBy" to currentAdminId, "updatedAt" to Timestamp.now()
            ), SetOptions.merge()
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.supportTickets(): Result<List<AdminSupportTicketV2>> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        Result.success(firestore.collection("supportTickets").limit(500).get().await().documents.map {
            AdminSupportTicketV2(
                it.id, it.getString("userId").orEmpty(), it.getString("subject").orEmpty(),
                it.getString("message").orEmpty(), it.getString("status").orEmpty().ifBlank { "open" },
                it.getString("priority").orEmpty().ifBlank { "normal" },
                it.getString("assignedAdminId").orEmpty(), it.getTimestamp("createdAt")
            )
        }.sortedByDescending { it.createdAt?.seconds ?: 0L })
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.assignTicket(ticketId: String, adminId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        firestore.collection("supportTickets").document(ticketId).update(
            mapOf("assignedAdminId" to adminId, "status" to "in_progress", "updatedAt" to Timestamp.now())
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}

suspend fun AdminControlRepository.replyTicket(ticketId: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        ensureAdminV2()
        require(message.isNotBlank())
        firestore.collection("supportTickets").document(ticketId).collection("responses").add(
            mapOf("adminId" to currentAdminId, "message" to message.take(3000), "createdAt" to Timestamp.now())
        ).await()
        firestore.collection("supportTickets").document(ticketId).update(
            mapOf("status" to "waiting_user", "lastResponseAt" to Timestamp.now(), "lastResponseBy" to currentAdminId)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
