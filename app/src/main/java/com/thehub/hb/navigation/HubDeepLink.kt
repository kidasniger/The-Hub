package com.thehub.hb.navigation

object HubDeepLink {

    private const val WEB_HOST = "the-hub-f95f4.web.app"

    fun toRoute(rawUri: String?): String? {
        if (rawUri.isNullOrBlank()) return null

        val normalized = rawUri.trim()

        if (normalized.startsWith("thehub://", ignoreCase = true)) {
            val payload = normalized
                .substring("thehub://".length)
                .substringBefore('?')
                .trim('/')

            if (payload.isBlank()) return null

            val segments = payload
                .split('/')
                .filter { it.isNotBlank() }

            val target = segments.firstOrNull()?.lowercase() ?: return null
            val id = decode(segments.getOrNull(1))

            return routeFor(target, id)
        }

        val uri = try {
            java.net.URI(normalized)
        } catch (_: Exception) {
            return null
        }

        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals(WEB_HOST, ignoreCase = true)
        ) {
            return null
        }

        val segments = uri.rawPath.orEmpty()
            .split('/')
            .filter { it.isNotBlank() }

        val target = segments.firstOrNull()?.lowercase() ?: return null
        val id = decode(segments.getOrNull(1))

        return routeFor(target, id)
    }

    private fun decode(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return try {
            java.net.URLDecoder.decode(value, Charsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun routeFor(target: String, id: String?): String? =
        when (target) {
            "feed" -> Screen.Feed.route
            "post" -> id?.let(Screen.PostDetail::createRoute)
            "comments" -> id?.let(Screen.Comments::createRoute)
            "chat" -> id?.let(Screen.Chat::createRoute)
            "profile" -> id?.let(Screen.Profile::createRoute)
            "messenger" -> Screen.Messenger.route
            else -> null
        }
}
