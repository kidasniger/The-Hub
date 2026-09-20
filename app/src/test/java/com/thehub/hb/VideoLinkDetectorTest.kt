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
    fun acceptsGenericHttpsVideoLinksManually() {
        assertEquals(
            "https://videos.example.com/watch/abc",
            VideoLinkDetector.normalizeManualUrl(" https://videos.example.com/watch/abc ")
        )
    }
}
