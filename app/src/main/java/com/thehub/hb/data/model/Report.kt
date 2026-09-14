package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val targetType: String = "", // "user" | "post"
    val targetId: String = "",
    val reason: String = "",
    val details: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "pending"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "reporterId" to reporterId,
        "targetType" to targetType,
        "targetId" to targetId,
        "reason" to reason,
        "details" to details,
        "createdAt" to createdAt,
        "status" to status
    )

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): Report {
            return Report(
                id = doc.id,
                reporterId = doc.getString("reporterId") ?: "",
                targetType = doc.getString("targetType") ?: "",
                targetId = doc.getString("targetId") ?: "",
                reason = doc.getString("reason") ?: "",
                details = doc.getString("details"),
                createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                status = doc.getString("status") ?: "pending"
            )
        }
    }
}
