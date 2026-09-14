package com.thehub.hb.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantsInfo: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageAt: Timestamp = Timestamp.now(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap()
) {
    fun getOtherParticipantId(currentUid: String?): String {
        if (currentUid == null) return participantIds.firstOrNull() ?: ""
        return participantIds.firstOrNull { it != currentUid } ?: ""
    }

    fun getOtherParticipantInfo(currentUid: String?): ParticipantInfo {
        val otherId = getOtherParticipantId(currentUid)
        return participantsInfo[otherId] ?: ParticipantInfo()
    }

    fun getUnreadCountFor(currentUid: String?): Int {
        if (currentUid == null) return 0
        return unreadCount[currentUid] ?: 0
    }

    fun toMap(): Map<String, Any?> {
        val participantsMap = participantsInfo.mapValues { it.value.toMap() }
        return mapOf(
            "participantIds" to participantIds,
            "participantsInfo" to participantsMap,
            "lastMessageText" to lastMessageText,
            "lastMessageAt" to lastMessageAt,
            "lastMessageSenderId" to lastMessageSenderId,
            "unreadCount" to unreadCount
        )
    }

    companion object {
        fun generateDeterministicId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }

        @Suppress("UNCHECKED_CAST")
        fun fromSnapshot(doc: DocumentSnapshot): Conversation {
            val data = doc.data ?: emptyMap<String, Any?>()
            val participantIds = (data["participantIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            
            val participantsRaw = data["participantsInfo"] as? Map<String, Any?> ?: emptyMap()
            val participantsInfo = participantsRaw.mapValues { entry ->
                ParticipantInfo.fromMap(entry.value as? Map<String, Any?>)
            }

            val unreadRaw = data["unreadCount"] as? Map<String, Any?> ?: emptyMap()
            val unreadCount = unreadRaw.mapValues { entry ->
                (entry.value as? Number)?.toInt() ?: 0
            }

            val lastMessageAt = doc.getTimestamp("lastMessageAt") ?: Timestamp.now()

            return Conversation(
                id = doc.id,
                participantIds = participantIds,
                participantsInfo = participantsInfo,
                lastMessageText = doc.getString("lastMessageText") ?: "",
                lastMessageAt = lastMessageAt,
                lastMessageSenderId = doc.getString("lastMessageSenderId") ?: "",
                unreadCount = unreadCount
            )
        }
    }
}
