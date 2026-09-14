package com.thehub.hb

import com.google.firebase.Timestamp
import com.thehub.hb.data.model.Conversation
import com.thehub.hb.data.model.Message
import com.thehub.hb.data.model.ParticipantInfo
import com.thehub.hb.utils.RelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class MessagingUnitTest {

    @Test
    fun testDeterministicConversationId() {
        val uidA = "alice123"
        val uidB = "bob456"

        val id1 = Conversation.generateDeterministicId(uidA, uidB)
        val id2 = Conversation.generateDeterministicId(uidB, uidA)

        assertEquals("alice123_bob456", id1)
        assertEquals(id1, id2)
    }

    @Test
    fun testConversationModel() {
        val now = Timestamp.now()
        val infoAlice = ParticipantInfo(username = "alice", displayName = "Alice Wonderland", photoUrl = "https://example.com/alice.jpg")
        val infoBob = ParticipantInfo(username = "bob", displayName = "Bob Builder", photoUrl = null)

        val conversation = Conversation(
            id = "alice123_bob456",
            participantIds = listOf("alice123", "bob456"),
            participantsInfo = mapOf(
                "alice123" to infoAlice,
                "bob456" to infoBob
            ),
            lastMessageText = "Salut Bob !",
            lastMessageAt = now,
            lastMessageSenderId = "alice123",
            unreadCount = mapOf(
                "alice123" to 0,
                "bob456" to 3
            )
        )

        assertEquals("bob456", conversation.getOtherParticipantId("alice123"))
        assertEquals("alice123", conversation.getOtherParticipantId("bob456"))

        assertEquals("Bob Builder", conversation.getOtherParticipantInfo("alice123").displayName)
        assertEquals("alice", conversation.getOtherParticipantInfo("bob456").username)

        assertEquals(0, conversation.getUnreadCountFor("alice123"))
        assertEquals(3, conversation.getUnreadCountFor("bob456"))

        val map = conversation.toMap()
        assertEquals(listOf("alice123", "bob456"), map["participantIds"])
        assertEquals("Salut Bob !", map["lastMessageText"])
    }

    @Test
    fun testMessageModel() {
        val now = Timestamp.now()
        val message = Message(
            id = "m1",
            senderId = "alice123",
            text = "Voici une photo",
            imageUrl = "https://example.com/img.jpg",
            createdAt = now,
            status = Message.STATUS_SENT
        )

        assertEquals("m1", message.id)
        assertEquals("alice123", message.senderId)
        assertTrue(message.isSentBy("alice123"))
        assertFalse(message.isSentBy("bob456"))

        val map = message.toMap()
        assertEquals("alice123", map["senderId"])
        assertEquals("Voici une photo", map["text"])
        assertEquals("https://example.com/img.jpg", map["imageUrl"])
        assertEquals(Message.STATUS_SENT, map["status"])
    }

    @Test
    fun testParticipantInfoSerialization() {
        val info = ParticipantInfo(
            username = "charlie",
            displayName = "Charlie Chaplin",
            photoUrl = "https://example.com/charlie.png"
        )
        val map = info.toMap()
        assertEquals("charlie", map["username"])
        assertEquals("Charlie Chaplin", map["displayName"])

        val parsed = ParticipantInfo.fromMap(map)
        assertEquals(info.username, parsed.username)
        assertEquals(info.displayName, parsed.displayName)
        assertEquals(info.photoUrl, parsed.photoUrl)
    }

    @Test
    fun testTimeFormatting() {
        val now = Timestamp.now()
        val timeOnly = RelativeTime.formatTimeOnly(now)
        assertNotNull(timeOnly)
        assertTrue(timeOnly.matches(Regex("\\d{2}:\\d{2}")))

        val convDate = RelativeTime.formatConversationDate(now)
        assertEquals(timeOnly, convDate)
    }
}
