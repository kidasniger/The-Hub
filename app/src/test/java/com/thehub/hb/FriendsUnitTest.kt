package com.thehub.hb

import com.thehub.hb.data.model.User
import com.thehub.hb.ui.friends.FriendsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsUnitTest {

    @Test
    fun testFriendsUiStateFilterByUsername() {
        val user1 = User(uid = "u1", username = "alice", displayName = "Alice Dupont")
        val user2 = User(uid = "u2", username = "bob", displayName = "Bob Martin")
        val user3 = User(uid = "u3", username = "charlie", displayName = "Charlie Brown")

        val state = FriendsUiState(
            isLoading = false,
            users = listOf(user1, user2, user3),
            searchQuery = "ali"
        )

        val filtered = state.filteredUsers
        assertEquals(1, filtered.size)
        assertEquals("alice", filtered[0].username)
    }

    @Test
    fun testFriendsUiStateFilterByDisplayName() {
        val user1 = User(uid = "u1", username = "al_dup", displayName = "Alice Dupont")
        val user2 = User(uid = "u2", username = "bob", displayName = "Bob Martin")

        val state = FriendsUiState(
            isLoading = false,
            users = listOf(user1, user2),
            searchQuery = "martin"
        )

        val filtered = state.filteredUsers
        assertEquals(1, filtered.size)
        assertEquals("bob", filtered[0].username)
    }

    @Test
    fun testFriendsUiStateEmptyQueryReturnsAll() {
        val user1 = User(uid = "u1", username = "alice")
        val user2 = User(uid = "u2", username = "bob")

        val state = FriendsUiState(
            isLoading = false,
            users = listOf(user1, user2),
            searchQuery = "  "
        )

        assertEquals(2, state.filteredUsers.size)
    }

    @Test
    fun testMutualRelationLogic() {
        // User follows set
        val myFollowing = setOf("userB", "userC", "userD")
        val myFollowers = setOf("userA", "userB", "userD", "userE")

        // Mutual friends: intersection of following and followers
        val mutualFriends = myFollowing.intersect(myFollowers)

        assertTrue(mutualFriends.contains("userB"))
        assertTrue(mutualFriends.contains("userD"))
        assertFalse(mutualFriends.contains("userC")) // I follow C, but C does not follow me
        assertFalse(mutualFriends.contains("userA")) // A follows me, but I do not follow A
        assertEquals(setOf("userB", "userD"), mutualFriends)
    }
}
