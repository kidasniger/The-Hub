package com.thehub.hb.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hub_settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_vu")
        val KEY_LAST_USER_EMAIL = stringPreferencesKey("last_user_email")
        val KEY_LAST_USER_NAME = stringPreferencesKey("last_user_name")
        val KEY_LAST_USERNAME = stringPreferencesKey("last_username")
        val KEY_LAST_PHOTO_URL = stringPreferencesKey("last_photo_url")
        val KEY_SEARCH_HISTORY = stringPreferencesKey("recent_search_history")
        val KEY_BOOKMARKED_POSTS = stringSetPreferencesKey("bookmarked_post_ids")
        val KEY_LIKED_COMMENTS = stringSetPreferencesKey("liked_comment_ids")
        val KEY_NOTIF_LIKES = booleanPreferencesKey("notif_likes")
        val KEY_NOTIF_COMMENTS = booleanPreferencesKey("notif_comments")
        val KEY_NOTIF_FOLLOWS = booleanPreferencesKey("notif_follows")
        val KEY_NOTIF_MESSAGES = booleanPreferencesKey("notif_messages")
        val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val appThemeMode: Flow<com.thehub.hb.ui.theme.AppThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_APP_THEME_MODE]
        com.thehub.hb.ui.theme.AppThemeMode.fromKey(raw)
    }

    suspend fun setAppThemeMode(mode: com.thehub.hb.ui.theme.AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME_MODE] = mode.key
        }
    }

    val appLanguage: Flow<com.thehub.hb.ui.theme.AppLanguage> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_APP_LANGUAGE]
        com.thehub.hb.ui.theme.AppLanguage.fromCode(raw)
    }

    suspend fun setAppLanguage(language: com.thehub.hb.ui.theme.AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language.code
        }
    }

    val notifLikesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_LIKES] ?: true
    }

    val notifCommentsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_COMMENTS] ?: true
    }

    val notifFollowsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_FOLLOWS] ?: true
    }

    val notifMessagesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIF_MESSAGES] ?: true
    }

    suspend fun setNotifLikes(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_LIKES] = enabled }
    }

    suspend fun setNotifComments(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_COMMENTS] = enabled }
    }

    suspend fun setNotifFollows(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_FOLLOWS] = enabled }
    }

    suspend fun setNotifMessages(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_MESSAGES] = enabled }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val searchHistory: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_SEARCH_HISTORY] ?: ""
        if (raw.isBlank()) {
            emptyList()
        } else {
            try {
                val jsonArray = org.json.JSONArray(raw)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optString(i)
                    if (item.isNotBlank()) list.add(item)
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val lastUserEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_USER_EMAIL]
    }

    val lastUserName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_USER_NAME]
    }

    val lastUsername: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_USERNAME]
    }

    val lastPhotoUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_PHOTO_URL]
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveLastUser(email: String, name: String? = null, username: String? = null, photoUrl: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_USER_EMAIL] = email
            if (name != null) preferences[KEY_LAST_USER_NAME] = name
            if (username != null) preferences[KEY_LAST_USERNAME] = username
            if (photoUrl != null) preferences[KEY_LAST_PHOTO_URL] = photoUrl
        }
    }

    suspend fun clearLastUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_USER_EMAIL)
            preferences.remove(KEY_LAST_USER_NAME)
            preferences.remove(KEY_LAST_USERNAME)
            preferences.remove(KEY_LAST_PHOTO_URL)
        }
    }

    suspend fun addSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_SEARCH_HISTORY] ?: ""
            val currentList = mutableListOf<String>()
            try {
                if (raw.isNotBlank()) {
                    val jsonArray = org.json.JSONArray(raw)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.optString(i)
                        if (item.isNotBlank() && !item.equals(clean, ignoreCase = true)) {
                            currentList.add(item)
                        }
                    }
                }
            } catch (_: Exception) {}
            currentList.add(0, clean)
            val capped = currentList.take(20)
            preferences[KEY_SEARCH_HISTORY] = org.json.JSONArray(capped).toString()
        }
    }

    suspend fun removeSearchQuery(query: String) {
        val clean = query.trim()
        context.dataStore.edit { preferences ->
            val raw = preferences[KEY_SEARCH_HISTORY] ?: ""
            val currentList = mutableListOf<String>()
            try {
                if (raw.isNotBlank()) {
                    val jsonArray = org.json.JSONArray(raw)
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.optString(i)
                        if (item.isNotBlank() && !item.equals(clean, ignoreCase = true)) {
                            currentList.add(item)
                        }
                    }
                }
            } catch (_: Exception) {}
            preferences[KEY_SEARCH_HISTORY] = org.json.JSONArray(currentList).toString()
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_SEARCH_HISTORY)
        }
    }

    val bookmarkedPostIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BOOKMARKED_POSTS] ?: emptySet()
    }

    suspend fun getLocalBookmarkedIds(): Set<String> {
        return try {
            bookmarkedPostIds.first()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun setLocalBookmarkedIds(ids: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BOOKMARKED_POSTS] = ids
        }
    }

    suspend fun toggleLocalBookmark(postId: String): Boolean {
        var isBookmarked = false
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_BOOKMARKED_POSTS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(postId)) {
                current.remove(postId)
                isBookmarked = false
            } else {
                current.add(postId)
                isBookmarked = true
            }
            preferences[KEY_BOOKMARKED_POSTS] = current
        }
        return isBookmarked
    }

    val likedCommentIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_LIKED_COMMENTS] ?: emptySet()
    }

    suspend fun getLocalLikedCommentIds(): Set<String> {
        return try {
            likedCommentIds.first()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun toggleLocalCommentLike(commentId: String): Boolean {
        var isLiked = false
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_LIKED_COMMENTS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(commentId)) {
                current.remove(commentId)
                isLiked = false
            } else {
                current.add(commentId)
                isLiked = true
            }
            preferences[KEY_LIKED_COMMENTS] = current
        }
        return isLiked
    }

    suspend fun setLocalCommentLiked(commentId: String, liked: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_LIKED_COMMENTS]?.toMutableSet() ?: mutableSetOf()
            if (liked) {
                current.add(commentId)
            } else {
                current.remove(commentId)
            }
            preferences[KEY_LIKED_COMMENTS] = current
        }
    }
}
