package com.thehub.hb

import com.google.firebase.Timestamp
import com.thehub.hb.data.model.Comment
import com.thehub.hb.data.model.Post
import com.thehub.hb.utils.RelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SocialCoreUnitTest {

    @Test
    fun testPostDataModel() {
        val now = Timestamp.now()
        val post = Post(
            id = "post123",
            authorId = "user1",
            authorUsername = "alice",
            authorPhotoUrl = "https://example.com/avatar.jpg",
            text = "Hello The Hub!",
            imageUrl = "https://example.com/photo.jpg",
            createdAt = now,
            likesCount = 5,
            commentsCount = 2,
            isRepost = false,
            originalPostId = null,
            isLikedByCurrentUser = true
        )

        assertEquals("post123", post.id)
        assertEquals("alice", post.authorUsername)
        assertEquals(5, post.likesCount)
        assertEquals(2, post.commentsCount)
        assertTrue(post.isLikedByCurrentUser)
        assertFalse(post.isRepost)

        val map = post.toMap()
        assertEquals("user1", map["authorId"])
        assertEquals("alice", map["authorUsername"])
        assertEquals("Hello The Hub!", map["text"])
        assertEquals(5, map["likesCount"])
    }

    @Test
    fun testCommentDataModel() {
        val now = Timestamp.now()
        val comment = Comment(
            id = "c1",
            authorId = "user2",
            authorUsername = "bob",
            authorPhotoUrl = null,
            text = "Superbe publication !",
            createdAt = now
        )

        assertEquals("c1", comment.id)
        assertEquals("bob", comment.authorUsername)
        assertEquals("Superbe publication !", comment.text)

        val map = comment.toMap()
        assertEquals("user2", map["authorId"])
        assertEquals("Superbe publication !", map["text"])
    }

    @Test
    fun testRelativeTimeFormatting() {
        val now = Timestamp.now()
        assertEquals("À l'instant", RelativeTime.format(now))

        // 10 minutes ago
        val tenMinutesAgo = Timestamp(Date(System.currentTimeMillis() - 10 * 60 * 1000L))
        assertEquals("il y a 10min", RelativeTime.format(tenMinutesAgo))

        // 2 hours ago
        val twoHoursAgo = Timestamp(Date(System.currentTimeMillis() - 2 * 3600 * 1000L))
        assertEquals("il y a 2h", RelativeTime.format(twoHoursAgo))

        // 3 days ago
        val threeDaysAgo = Timestamp(Date(System.currentTimeMillis() - 3 * 24 * 3600 * 1000L))
        assertEquals("il y a 3j", RelativeTime.format(threeDaysAgo))
    }
}
