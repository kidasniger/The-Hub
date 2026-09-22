package com.thehub.hb

import com.thehub.hb.data.model.User
import com.thehub.hb.data.repository.rankSuggestedUsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchDiscoveryUnitTest {

    private fun user(
        uid: String,
        followers: Int,
        posts: Int,
        createdAt: Long
    ) = User(
        uid = uid,
        username = uid,
        usernameLower = uid,
        followersCount = followers,
        postsCount = posts,
        createdAt = createdAt
    )

    @Test
    fun rankSuggestions_excludesCurrentFollowedAndBlocked_andSortsDeterministically() {
        val candidates = listOf(
            user("self", 999, 999, 999),
            user("followed", 900, 900, 900),
            user("blocked", 800, 800, 800),
            user("popular", 700, 5, 100),
            user("active", 650, 20, 200),
            user("recent", 650, 20, 300),
            user("duplicate", 100, 1, 1),
            user("duplicate", 500, 50, 500)
        )

        val result = rankSuggestedUsers(
            candidates = candidates,
            excludedIds = setOf("followed", "blocked"),
            currentUserId = "self",
            limit = 3
        )

        assertEquals(listOf("popular", "recent", "active"), result.map { it.uid })
        assertFalse(result.any { it.uid == "self" })
        assertFalse(result.any { it.uid == "followed" })
        assertFalse(result.any { it.uid == "blocked" })
    }

    @Test
    fun rankSuggestions_respectsLimit_andRemovesDuplicateIds() {
        val candidates = listOf(
            user("alpha", 10, 1, 1),
            user("alpha", 99, 99, 99),
            user("beta", 50, 2, 2)
        )

        val result = rankSuggestedUsers(
            candidates = candidates,
            excludedIds = emptySet(),
            currentUserId = null,
            limit = 2
        )

        assertEquals(2, result.size)
        assertEquals("alpha", result.first().uid)
        assertEquals(1, result.count { it.uid == "alpha" })
    }
}
