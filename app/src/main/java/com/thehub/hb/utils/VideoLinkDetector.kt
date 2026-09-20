package com.thehub.hb.utils

import java.net.URI

enum class VideoSourceType {
    YOUTUBE,
    VIMEO,
    DAILYMOTION,
    DIRECT_MEDIA,
    EXTERNAL_VIDEO_PAGE
}

object VideoLinkDetector {
    private val urlRegex = Regex("""https?://[^\s<>]+""", RegexOption.IGNORE_CASE)

    private val directVideoExtensions = listOf(
        ".mp4", ".webm", ".m4v", ".mov", ".m3u8", ".mkv", ".avi", ".3gp"
    )

    private val videoPathMarkers = setOf(
        "video", "videos", "watch", "reel", "reels", "short", "shorts",
        "clip", "clips", "embed", "player", "live", "stream", "streams"
    )

    fun extractVideoUrl(text: String): String? {
        return urlRegex.findAll(text)
            .map { it.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}') }
            .firstOrNull { isKnownVideoUrl(it) }
    }

    fun normalizeManualUrl(input: String): String? {
        val value = input.trim()
        if (value.isBlank()) return null

        val uri = parseUri(value) ?: return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host

        return if (
            (scheme == "https" || scheme == "http") &&
            !host.isNullOrBlank() &&
            isKnownVideoUrl(value)
        ) {
            value
        } else {
            null
        }
    }

    fun isKnownVideoUrl(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https")) return false

        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase().orEmpty()
        val normalizedPath = path.trim('/')

        if (directVideoExtensions.any { path.endsWith(it) }) {
            return true
        }

        if (
            host == "youtube.com" ||
            host == "www.youtube.com" ||
            host == "m.youtube.com" ||
            host == "music.youtube.com" ||
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
            host.endsWith(".wistia.com") ||
            host == "tiktok.com" ||
            host == "www.tiktok.com" ||
            host.endsWith(".tiktok.com") ||
            host == "facebook.com" ||
            host == "www.facebook.com" ||
            host == "m.facebook.com" ||
            host == "fb.watch" ||
            host == "instagram.com" ||
            host == "www.instagram.com" ||
            host == "twitch.tv" ||
            host == "www.twitch.tv" ||
            host == "clips.twitch.tv" ||
            host == "x.com" ||
            host == "www.x.com" ||
            host == "twitter.com" ||
            host == "www.twitter.com"
        ) {
            return true
        }

        val pathSegments = normalizedPath
            .split('/')
            .filter { it.isNotBlank() }

        return pathSegments.any { segment ->
            segment in videoPathMarkers ||
                segment.startsWith("video") ||
                segment.startsWith("watch")
        }
    }

    fun sourceType(url: String): VideoSourceType {
        val uri = parseUri(url) ?: return VideoSourceType.EXTERNAL_VIDEO_PAGE
        val host = uri.host?.lowercase().orEmpty()

        return when {
            isDirectMediaUrl(url) -> VideoSourceType.DIRECT_MEDIA
            host == "youtube.com" ||
                host == "www.youtube.com" ||
                host == "m.youtube.com" ||
                host == "music.youtube.com" ||
                host == "youtu.be" ||
                host.endsWith(".youtube.com") -> VideoSourceType.YOUTUBE
            host == "vimeo.com" ||
                host == "www.vimeo.com" ||
                host == "player.vimeo.com" -> VideoSourceType.VIMEO
            host == "dailymotion.com" ||
                host == "www.dailymotion.com" ||
                host == "dai.ly" -> VideoSourceType.DAILYMOTION
            else -> VideoSourceType.EXTERNAL_VIDEO_PAGE
        }
    }

    fun isDirectMediaUrl(url: String): Boolean {
        val path = parseUri(url)?.path?.lowercase().orEmpty()
        return directVideoExtensions.any { extension ->
            path.endsWith(extension)
        }
    }

    fun providerLabel(url: String): String {
        val host = parseUri(url)?.host?.lowercase().orEmpty()

        return when {
            host.contains("youtube") || host == "youtu.be" -> "YouTube"
            host.contains("vimeo") -> "Vimeo"
            host.contains("dailymotion") || host == "dai.ly" -> "Dailymotion"
            host.contains("streamable") -> "Streamable"
            host.contains("wistia") -> "Wistia"
            host.contains("tiktok") -> "TikTok"
            host.contains("facebook") || host == "fb.watch" -> "Facebook"
            host.contains("instagram") -> "Instagram"
            host.contains("twitch") -> "Twitch"
            host.contains("twitter") || host == "x.com" -> "X"
            isDirectMediaUrl(url) -> "Vidéo directe"
            else -> "Lien vidéo"
        }
    }

    private fun parseUri(value: String): URI? {
        return try {
            URI(value)
        } catch (_: Exception) {
            null
        }
    }
}
