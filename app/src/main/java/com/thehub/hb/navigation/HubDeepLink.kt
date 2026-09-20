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

        val segments = uri.pathSegments.filter { it.isNotBlank() }
        val target = uri.host?.takeIf { it.isNotBlank() }
            ?: segments.firstOrNull()
            ?: return null
        val id = if (uri.host?.isNotBlank() == true) {
            segments.firstOrNull()
        } else {
            segments.drop(1).firstOrNull()
        }

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
