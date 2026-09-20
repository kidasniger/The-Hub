package com.thehub.hb.navigation

import android.net.Uri

object HubDeepLink {

    fun toRoute(rawUri: String?): String? {
        if (rawUri.isNullOrBlank()) return null

        val normalized = rawUri.trim()
        if (!normalized.startsWith("thehub://", ignoreCase = true)) return null

        val payload = normalized
            .substring("thehub://".length)
            .substringBefore('?')
            .trim('/')

        if (payload.isBlank()) return null

        val segments = payload
            .split('/')
            .filter { it.isNotBlank() }

        val target = segments.firstOrNull()?.lowercase() ?: return null
        val id = segments.getOrNull(1)?.let(Uri::decode)

        return when (target) {
            "feed" -> Screen.Feed.route
            "post" -> id?.let(Screen.PostDetail::createRoute)
            "comments" -> id?.let(Screen.Comments::createRoute)
            "chat" -> id?.let(Screen.Chat::createRoute)
            "profile" -> id?.let(Screen.Profile::createRoute)
            "messenger" -> Screen.Messenger.route
            else -> null
        }
    }
}
