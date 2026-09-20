package com.thehub.hb

import com.thehub.hb.utils.VideoLinkDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLinkDetectorTest {

    @Test
    fun detectsSupportedVideoProviders() {
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://youtu.be/abc123"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://vimeo.com/123456"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.dailymotion.com/video/x123"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.tiktok.com/@creator/video/123456"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.instagram.com/reel/ABC123/"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.facebook.com/reel/123456"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.twitch.tv/videos/123456"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://x.com/example/status/123456"))
    }

    @Test
    fun detectsDirectVideoFiles() {
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://cdn.example.com/video.mp4"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://cdn.example.com/video.webm"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://cdn.example.com/stream.m3u8"))
        assertFalse(VideoLinkDetector.isKnownVideoUrl("https://example.com/photo.jpg"))
    }

    @Test
    fun extractsVideoUrlFromPostText() {
        val url = VideoLinkDetector.extractVideoUrl(
            "Regarde cette vidéo https://youtu.be/abc123 maintenant."
        )
        assertEquals("https://youtu.be/abc123", url)
    }

    @Test
    fun extractsGenericVideoPageLinks() {
        assertEquals(
            "https://video.example.com/watch/abc123",
            VideoLinkDetector.extractVideoUrl(
                "Regarde https://video.example.com/watch/abc123 maintenant."
            )
        )
    }

    @Test
    fun acceptsGenericHttpsVideoLinksManually() {
        assertEquals(
            "https://videos.example.com/watch/abc",
            VideoLinkDetector.normalizeManualUrl(" https://videos.example.com/watch/abc ")
        )
    }
}
