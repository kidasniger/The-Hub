package com.thehub.hb.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkUrlDetectorTest {

    @Test
    fun extractsFirstHttpUrlAndTrimsSentencePunctuation() {
        assertEquals(
            "https://example.com/article",
            extractFirstHttpUrl("Lis ça : https://example.com/article.")
        )
    }

    @Test
    fun returnsOnlyTheFirstUrl() {
        assertEquals(
            "https://example.com",
            extractFirstHttpUrl("https://example.com https://example.org")
        )
    }

    @Test
    fun rejectsNonHttpSchemesAndEmptyText() {
        assertNull(extractFirstHttpUrl("www.example.com"))
        assertNull(extractFirstHttpUrl("mailto:test@example.com"))
        assertNull(extractFirstHttpUrl(""))
    }
}
