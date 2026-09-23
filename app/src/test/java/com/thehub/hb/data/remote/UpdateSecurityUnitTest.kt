package com.thehub.hb.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSecurityUnitTest {
    @Test fun trustedReleaseIsAccepted() {
        val url = "https://github.com/kidasniger/The-Hub/releases/download/v1.0.161/TheHub-v1.0.161.apk"
        assertTrue(UpdateSecurity.isTrustedApkUrl(url))
        assertTrue(UpdateSecurity.isValidUpdate(url, "TheHub-v1.0.161.apk", "1.0.161"))
    }

    @Test fun foreignAndCleartextUrlsAreRejected() {
        assertFalse(UpdateSecurity.isTrustedApkUrl("http://github.com/kidasniger/The-Hub/releases/download/v1.0.161/TheHub-v1.0.161.apk"))
        assertFalse(UpdateSecurity.isTrustedApkUrl("https://example.com/TheHub-v1.0.161.apk"))
        assertFalse(UpdateSecurity.isTrustedApkUrl("https://github.com/kidasniger/The-Hub/releases/download/../v1.0.161/TheHub-v1.0.161.apk"))
    }

    @Test fun unsafeNamesAndVersionMismatchesAreRejected() {
        assertFalse(UpdateSecurity.isSafeApkFileName("../TheHub-v1.0.161.apk", "1.0.161"))
        assertFalse(UpdateSecurity.isSafeApkFileName("TheHub-v1.0.160.apk", "1.0.161"))
        assertFalse(UpdateSecurity.isSafeApkFileName("TheHub-v1.0.161.zip", "1.0.161"))
    }
    @Test fun sha256DigestIsNormalized() {
        val digest = "a".repeat(64)
        assertTrue(UpdateSecurity.isValidSha256(digest))
        assertTrue(UpdateSecurity.normalizeSha256("sha256:$digest") == digest)
        assertFalse(UpdateSecurity.isValidSha256("deadbeef"))
        assertTrue(UpdateSecurity.normalizeSha256("not-a-digest").isEmpty())
    }

}
