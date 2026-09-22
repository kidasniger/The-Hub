package com.thehub.hb

import com.google.firebase.Timestamp
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.ui.notifications.NotificationFilter
import com.thehub.hb.ui.notifications.filterNotifications
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFiltersUnitTest {

    private fun item(id: String, type: String, read: Boolean) = NotificationItem(
        id = id,
        actorId = "actor-$id",
        type = type,
        createdAt = Timestamp.now(),
        isRead = read
    )

    @Test
    fun unreadFilter_keepsOnlyUnreadNotifications() {
        val items = listOf(
            item("1", NotificationItem.TYPE_LIKE, false),
            item("2", NotificationItem.TYPE_COMMENT, true),
            item("3", NotificationItem.TYPE_FOLLOW, false)
        )

        assertEquals(
            listOf("1", "3"),
            filterNotifications(items, NotificationFilter.UNREAD).map { it.id }
        )
    }

    @Test
    fun activityFilter_excludesOnlyMessages() {
        val items = listOf(
            item("1", NotificationItem.TYPE_LIKE, false),
            item("2", NotificationItem.TYPE_MESSAGE, false),
            item("3", NotificationItem.TYPE_REPLY_COMMENT, true)
        )

        assertEquals(
            listOf("1", "3"),
            filterNotifications(items, NotificationFilter.ACTIVITY).map { it.id }
        )
    }

    @Test
    fun messagesFilter_keepsOnlyMessages() {
        val items = listOf(
            item("1", NotificationItem.TYPE_MESSAGE, false),
            item("2", NotificationItem.TYPE_FOLLOW, false)
        )

        assertEquals(
            listOf("1"),
            filterNotifications(items, NotificationFilter.MESSAGES).map { it.id }
        )
    }
}
