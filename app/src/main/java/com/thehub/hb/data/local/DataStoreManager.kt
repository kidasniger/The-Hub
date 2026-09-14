package com.thehub.hb.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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
}
