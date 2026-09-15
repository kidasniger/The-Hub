package com.thehub.hb.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehub.hb.data.model.NotificationItem
import com.thehub.hb.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val groupedNotifications: Map<String, List<NotificationItem>> = emptyMap(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val unreadCount: StateFlow<Int> = notificationRepository.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        observeNotifications()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationRepository.getNotifications().collectLatest { items ->
                val actorIds = items.map { it.actorId }
                com.thehub.hb.data.repository.UserCacheRepository.getInstance().observeUsers(actorIds)
                val grouped = groupNotifications(items)
                val unread = items.count { !it.isRead }
                _uiState.update {
                    it.copy(
                        notifications = items,
                        groupedNotifications = grouped,
                        unreadCount = unread,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNotificationClicked(notification: NotificationItem) {
        if (!notification.isRead) {
            viewModelScope.launch {
                notificationRepository.markAsRead(notification.id)
            }
        }
    }

    fun onMarkAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    private fun groupNotifications(items: List<NotificationItem>): Map<String, List<NotificationItem>> {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfWeek = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val groups = linkedMapOf<String, MutableList<NotificationItem>>()
        groups["Aujourd'hui"] = mutableListOf()
        groups["Cette semaine"] = mutableListOf()
        groups["Plus ancien"] = mutableListOf()

        for (item in items) {
            val time = item.createdAt.toDate().time
            when {
                time >= startOfToday -> groups["Aujourd'hui"]!!.add(item)
                time >= startOfWeek -> groups["Cette semaine"]!!.add(item)
                else -> groups["Plus ancien"]!!.add(item)
            }
        }

        return groups.filterValues { it.isNotEmpty() }
    }
}
