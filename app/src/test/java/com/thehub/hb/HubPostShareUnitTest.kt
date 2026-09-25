package com.thehub.hb

import com.thehub.hb.utils.buildHubPostShareLink
import com.thehub.hb.utils.buildHubPostShareMessage
import com.thehub.hb.utils.extractHubPostId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubPostShareUnitTest {

    @Test
    fun buildPostShareLink_usesTheHubScheme() {
        assertEquals(
            "https://the-hub-f95f4.web.app/post/post-123",
            buildHubPostShareLink("post-123")
        )
    }

    @Test
    fun buildPostShareMessage_containsLink() {
        assertEquals(
            "📌 Publication The Hub\nthehub://post/post-123",
            buildHubPostShareMessage("post-123")
        )
    }

    @Test
    fun extractHubPostId_readsSharedPostId() {
        assertEquals(
            "post-123",
            extractHubPostId("📌 Publication The Hub\nthehub://post/post-123")
        )
    }

    @Test
    fun extractHubPostId_stripsTrailingPunctuation() {
        assertEquals(
            "post-123",
            extractHubPostId("thehub://post/post-123.")
        )
    }

    @Test
    fun extractHubPostId_returnsNullWhenMissing() {
        assertNull(extractHubPostId("message normal"))
    }
    @Test
    fun extractPublicHttpsLink_returnsPostId() {
        assertEquals(
            "post-123",
            extractHubPostId("📌 Publication The Hub\nhttps://the-hub-f95f4.web.app/post/post-123")
        )
    }

    @Test
    fun extractLegacyTheHubLink_stillReturnsPostId() {
        assertEquals(
            "post-123",
            extractHubPostId("thehub://post/post-123.")
        )
    }

}
