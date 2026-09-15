package com.thehub.hb.data.repository

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.thehub.hb.data.model.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared in-memory user cache repository.
 * Keeps a Map<String, UserInfo> and Firestore snapshot listeners on users/{userId}
 * so that any changes to profile photo, displayName, or username instantly reflect
 * across all feeds, posts, comments, messages, notifications, and lists.
 */
class UserCacheRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        @Volatile
        private var instance: UserCacheRepository? = null

        fun getInstance(firestore: FirebaseFirestore = FirebaseFirestore.getInstance()): UserCacheRepository {
            return instance ?: synchronized(this) {
                instance ?: UserCacheRepository(firestore).also { instance = it }
            }
        }
    }

    private val _usersCache = MutableStateFlow<Map<String, UserInfo>>(emptyMap())
    val usersCache: StateFlow<Map<String, UserInfo>> = _usersCache.asStateFlow()

    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    /**
     * Start observing a user by ID using Firestore real-time snapshot listener.
     * Caches result in-memory so all components share the same state and update live.
     */
    fun observeUser(
        userId: String,
        fallbackUsername: String? = null,
        fallbackDisplayName: String? = null,
        fallbackPhotoUrl: String? = null
    ) {
        if (userId.isBlank()) return

        // Seed fallback data if not yet in cache
        if (!_usersCache.value.containsKey(userId)) {
            val initial = UserInfo(
                uid = userId,
                displayName = fallbackDisplayName?.trim()?.takeIf { it.isNotBlank() },
                username = fallbackUsername?.trim() ?: "",
                photoUrl = fallbackPhotoUrl
            )
            _usersCache.update { current ->
                if (!current.containsKey(userId)) {
                    current + (userId to initial)
                } else current
            }
        }

        // Only register listener once per user
        if (listeners.containsKey(userId)) return

        try {
            val registration = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("UserCacheRepository", "Listener error for user $userId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val username = snapshot.getString("username") ?: ""
                        val displayName = snapshot.getString("displayName")
                        val photoUrl = snapshot.getString("photoUrl")
                        val updated = UserInfo(
                            uid = userId,
                            displayName = displayName,
                            username = username,
                            photoUrl = photoUrl
                        )
                        _usersCache.update { it + (userId to updated) }
                    }
                }
            val existing = listeners.putIfAbsent(userId, registration)
            if (existing != null) {
                registration.remove()
            }
        } catch (e: Exception) {
            Log.e("UserCacheRepository", "Failed to observe user $userId: ${e.message}")
        }
    }

    /**
     * Observe multiple users at once (e.g. from loaded post list).
     */
    fun observeUsers(userIds: Collection<String>) {
        userIds.distinct().forEach { id ->
            if (id.isNotBlank()) {
                observeUser(id)
            }
        }
    }

    /**
     * Store or update user info directly into memory cache.
     */
    fun putUser(userInfo: UserInfo) {
        if (userInfo.uid.isNotBlank()) {
            _usersCache.update { it + (userInfo.uid to userInfo) }
        }
    }

    /**
     * Get current user info from cache synchronously.
     */
    fun getUser(userId: String): UserInfo? {
        return _usersCache.value[userId]
    }

    /**
     * Flow for a specific user ID.
     */
    fun getUserFlow(userId: String): Flow<UserInfo?> {
        observeUser(userId)
        return _usersCache.map { it[userId] }.distinctUntilChanged()
    }

    /**
     * Clear all listeners and cached data (e.g. on sign out).
     */
    fun clear() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
        _usersCache.value = emptyMap()
    }
}

/**
 * Composable helper that observes a user by ID in real-time from the shared memory cache.
 * Automatically updates when the user changes their photo or displayName.
 */
@Composable
fun rememberLiveUser(
    userId: String,
    fallbackUsername: String = "",
    fallbackDisplayName: String? = null,
    fallbackPhotoUrl: String? = null
): UserInfo {
    val repository = remember { UserCacheRepository.getInstance() }
    val cache by repository.usersCache.collectAsState()

    DisposableEffect(userId) {
        if (userId.isNotBlank()) {
            repository.observeUser(
                userId = userId,
                fallbackUsername = fallbackUsername,
                fallbackDisplayName = fallbackDisplayName,
                fallbackPhotoUrl = fallbackPhotoUrl
            )
        }
        onDispose { }
    }

    return remember(cache, userId, fallbackUsername, fallbackDisplayName, fallbackPhotoUrl) {
        cache[userId] ?: UserInfo(
            uid = userId,
            displayName = fallbackDisplayName?.trim()?.takeIf { it.isNotBlank() },
            username = fallbackUsername.trim(),
            photoUrl = fallbackPhotoUrl
        )
    }
}
