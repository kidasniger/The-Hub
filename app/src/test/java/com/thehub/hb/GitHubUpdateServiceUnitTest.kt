package com.thehub.hb

import com.thehub.hb.data.remote.GitHubUpdateService
import com.thehub.hb.data.remote.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateServiceUnitTest {

    private val service = GitHubUpdateService()

    @Test
    fun testVersionComparison_NewerMinorOrPatch() {
        // Remote is newer
        assertTrue(service.isVersionGreater("v1.0.19", "1.0"))
        assertTrue(service.isVersionGreater("v1.0.19", "1.0.0"))
        assertTrue(service.isVersionGreater("v1.0.19", "1.0.18"))
        assertTrue(service.isVersionGreater("2.0.0", "1.9.9"))
        assertTrue(service.isVersionGreater("1.1.0", "1.0.99"))
    }

    @Test
    fun testVersionComparison_SameVersion() {
        // Equal versions -> no update
        assertFalse(service.isVersionGreater("v1.0.19", "1.0.19"))
        assertFalse(service.isVersionGreater("1.0", "1.0.0"))
        assertFalse(service.isVersionGreater("v1.0", "1.0"))
    }

    @Test
    fun testVersionComparison_OlderVersion() {
        // Remote is older
        assertFalse(service.isVersionGreater("v1.0.18", "1.0.19"))
        assertFalse(service.isVersionGreater("0.9.9", "1.0.0"))
        assertFalse(service.isVersionGreater("v1.0.2", "1.0.19"))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("0 o", formatFileSize(0))
        assertEquals("500 o", formatFileSize(500))
        assertEquals("1.0 Ko", formatFileSize(1024))
        assertEquals("20.0 Mo", formatFileSize(20 * 1024 * 1024))
    }
}
