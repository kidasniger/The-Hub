package com.thehub.hb.utils

import android.net.Uri

object VideoLinkDetector {
    private val urlRegex = Regex("""https?://[^\s<>]+""", RegexOption.IGNORE_CASE)
    private val directVideoExtensions = listOf(
        ".mp4", ".webm", ".m4v", ".mov", ".m3u8", ".mkv"
    )

    fun extractVideoUrl(text: String): String? {
        return urlRegex.findAll(text)
            .map { it.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}') }
            .firstOrNull { isKnownVideoUrl(it) }
    }

    fun normalizeManualUrl(input: String): String? {
        val value = input.trim()
        if (value.isBlank()) return null

        val uri = try {
            Uri.parse(value)
        } catch (_: Exception) {
            return null
        }

        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        return if ((scheme == "https" || scheme == "http") && !host.isNullOrBlank()) {
            value
        } else {
            null
        }
    }

    fun isKnownVideoUrl(url: String): Boolean {
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        if (uri.scheme?.lowercase() !in setOf("http", "https")) return false

        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase().orEmpty()

        if (directVideoExtensions.any { path.substringBefore('?').endsWith(it) }) {
            return true
        }

        return host == "youtube.com" ||
            host == "www.youtube.com" ||
            host == "m.youtube.com" ||
            host == "youtu.be" ||
            host.endsWith(".youtube.com") ||
            host == "vimeo.com" ||
            host == "www.vimeo.com" ||
            host == "player.vimeo.com" ||
            host == "dailymotion.com" ||
            host == "www.dailymotion.com" ||
            host == "dai.ly" ||
            host == "streamable.com" ||
            host == "www.streamable.com" ||
            host == "wistia.com" ||
            host.endsWith(".wistia.com")
    }

    fun isDirectMediaUrl(url: String): Boolean {
        val path = try {
            Uri.parse(url).path?.lowercase().orEmpty()
        } catch (_: Exception) {
            ""
        }
        return directVideoExtensions.any { path.endsWith(it) }
    }

    fun providerLabel(url: String): String {
        val host = try { Uri.parse(url).host?.lowercase().orEmpty() } catch (_: Exception) { "" }
        return when {
            host.contains("youtube") || host == "youtu.be" -> "YouTube"
            host.contains("vimeo") -> "Vimeo"
            host.contains("dailymotion") || host == "dai.ly" -> "Dailymotion"
            host.contains("streamable") -> "Streamable"
            host.contains("wistia") -> "Wistia"
            isDirectMediaUrl(url) -> "Vidéo directe"
            else -> "Lien vidéo"
        }
    }
}
