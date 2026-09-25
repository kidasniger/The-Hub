package com.thehub.hb

import com.thehub.hb.ui.createpost.CreatePostUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePostUiStateUnitTest {
    @Test
    fun longPublicationTextIsNotTruncatedOrBlocked() {
        val text = buildString {
            repeat(20_000) { append('A') }
        }

        val state = CreatePostUiState(text = text)

        assertEquals(20_000, state.charCount)
        assertTrue(state.canPublish)
    }

    @Test
    fun longPublicationTextIsAcceptedWithMediaToo() {
        val text = "Texte long\n".repeat(5_000)
        val state = CreatePostUiState(text = text)

        assertTrue(state.canPublish)
        assertEquals(text.length, state.charCount)
    }
}
