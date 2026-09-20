package com.thehub.hb.navigation

import android.net.Uri

object HubDeepLink {

    fun toRoute(rawUri: String?): String? {
        if (rawUri.isNullOrBlank()) return null

        val uri = try {
            Uri.parse(rawUri)
        } catch (_: Exception) {
            return null
        }

        if (uri.scheme != "thehub") return null

        val id = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }

        return when (uri.host) {
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
