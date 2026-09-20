package com.thehub.hb

import com.thehub.hb.navigation.HubDeepLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubDeepLinkUnitTest {

    @Test
    fun postDeepLink_resolvesToPostDetail() {
        assertEquals(
            "post_detail/post-123",
            HubDeepLink.toRoute("thehub://post/post-123")
        )
    }

    @Test
    fun commentsDeepLink_resolvesToComments() {
        assertEquals(
            "comments/post-123",
            HubDeepLink.toRoute("thehub://comments/post-123")
        )
    }

    @Test
    fun chatDeepLink_resolvesToConversation() {
        assertEquals(
            "chat/conversation-123",
            HubDeepLink.toRoute("thehub://chat/conversation-123")
        )
    }

    @Test
    fun profileDeepLink_resolvesToProfile() {
        assertEquals(
            "profile/user-123",
            HubDeepLink.toRoute("thehub://profile/user-123")
        )
    }

    @Test
    fun invalidDeepLink_returnsNull() {
        assertNull(HubDeepLink.toRoute("https://example.com/post/post-123"))
    }
}
