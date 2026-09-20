package com.thehub.hb

import com.thehub.hb.utils.VideoLinkDetector
import com.thehub.hb.utils.VideoSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoLinkDetectorTest {

    @Test
    fun detectsSupportedVideoProviders() {
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://youtu.be/abc123"))
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://music.youtube.com/watch?v=abc123"))
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
        assertTrue(VideoLinkDetector.isKnownVideoUrl("https://cdn.example.com/video.mp4?token=abc"))
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
    fun acceptsOnlyDetectableVideoLinksManually() {
        assertEquals(
            "https://videos.example.com/watch/abc",
            VideoLinkDetector.normalizeManualUrl(" https://videos.example.com/watch/abc ")
        )
        assertEquals(
            null,
            VideoLinkDetector.normalizeManualUrl("https://example.com/article/abc")
        )
    }

    @Test
    fun extractsYouTubeIdsAndShortsFormat() {
        assertEquals(
            "abc123",
            VideoLinkDetector.youtubeVideoId("https://www.youtube.com/watch?v=abc123")
        )
        assertEquals(
            "xyz789",
            VideoLinkDetector.youtubeVideoId("https://youtube.com/shorts/xyz789?si=test")
        )
        assertEquals(
            "short123",
            VideoLinkDetector.youtubeVideoId("https://youtu.be/short123")
        )

        assertTrue(
            VideoLinkDetector.isYouTubeShorts(
                "https://www.youtube.com/shorts/xyz789"
            )
        )
        assertFalse(
            VideoLinkDetector.isYouTubeShorts(
                "https://www.youtube.com/watch?v=xyz789"
            )
        )
    }

    @Test
    fun buildsYouTubeThumbnailUrl() {
        assertEquals(
            "https://i.ytimg.com/vi/abc123/hqdefault.jpg",
            VideoLinkDetector.thumbnailUrl(
                "https://www.youtube.com/watch?v=abc123"
            )
        )
        assertEquals(
            "https://i.ytimg.com/vi/xyz789/hqdefault.jpg",
            VideoLinkDetector.thumbnailUrl(
                "https://youtube.com/shorts/xyz789"
            )
        )
        assertEquals(
            null,
            VideoLinkDetector.thumbnailUrl(
                "https://www.tiktok.com/@creator/video/123456"
            )
        )
    }

    @Test
    fun classifiesVideoSourcesForViewer() {
        assertEquals(
            VideoSourceType.YOUTUBE,
            VideoLinkDetector.sourceType("https://www.youtube.com/watch?v=abc123")
        )
        assertEquals(
            VideoSourceType.VIMEO,
            VideoLinkDetector.sourceType("https://vimeo.com/123456")
        )
        assertEquals(
            VideoSourceType.DAILYMOTION,
            VideoLinkDetector.sourceType("https://www.dailymotion.com/video/x123")
        )
        assertEquals(
            VideoSourceType.DIRECT_MEDIA,
            VideoLinkDetector.sourceType("https://cdn.example.com/movie.mp4")
        )
        assertEquals(
            VideoSourceType.EXTERNAL_VIDEO_PAGE,
            VideoLinkDetector.sourceType("https://www.tiktok.com/@creator/video/123456")
        )
        assertEquals(
            VideoSourceType.EXTERNAL_VIDEO_PAGE,
            VideoLinkDetector.sourceType("https://video.example.com/watch/abc")
        )
    }
}
