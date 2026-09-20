package com.thehub.hb

import com.thehub.hb.data.model.PostMediaType
import org.junit.Assert.assertEquals
import org.junit.Test

// Regression coverage for backward-compatible post media metadata.
class PostMediaTypeTest {

    @Test
    fun infersMediaTypeFromLegacyUrls() {
        assertEquals(
            PostMediaType.IMAGE,
            PostMediaType.fromUrls(
                imageUrl = "https://cdn.example.com/photo.jpg",
                videoUrl = null
            )
        )

        assertEquals(
            PostMediaType.VIDEO_LINK,
            PostMediaType.fromUrls(
                imageUrl = null,
                videoUrl = "https://www.youtube.com/watch?v=abc123"
            )
        )

        assertEquals(
            PostMediaType.VIDEO_LINK,
            PostMediaType.fromUrls(
                imageUrl = "https://cdn.example.com/photo.jpg",
                videoUrl = "https://vimeo.com/123456"
            )
        )

        assertEquals(
            PostMediaType.NONE,
            PostMediaType.fromUrls(null, null)
        )
    }

    @Test
    fun storedValuesRemainCaseInsensitive() {
        assertEquals(
            PostMediaType.VIDEO_LINK,
            PostMediaType.fromStoredValue("video_link")
        )
    }

    @Test
    fun invalidStoredValuesAreIgnored() {
        assertEquals(
            null,
            PostMediaType.fromStoredValue("unknown")
        )
    }
}
