package com.thehub.hb

import android.os.Build
import com.thehub.hb.data.repository.AuthRepository
import com.thehub.hb.data.repository.GOOGLE_WEB_CLIENT_ID
import com.thehub.hb.data.repository.GoogleSignInResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class GoogleSignInConfigUnitTest {

    @Test
    fun testGoogleWebClientIdConstant() {
        // Must match the Web OAuth client ID in the Firebase console and google-services.json
        val expectedWebClientId = "183373607979-d1qu0ogpl24dptctim56nlght54hs8a7.apps.googleusercontent.com"
        assertEquals(expectedWebClientId, GOOGLE_WEB_CLIENT_ID)
    }

    @Test
    fun testGoogleServicesJsonFileIntegrity() {
        // Locate google-services.json from root or app folder
        val potentialPaths = listOf(
            File("app/google-services.json"),
            File("google-services.json"),
            File("../app/google-services.json")
        )
        val file = potentialPaths.firstOrNull { it.exists() }
        assertNotNull("google-services.json should exist in the project", file)

        val content = file!!.readText(Charsets.UTF_8)
        val json = JSONObject(content)

        // 1. Verify project_info
        val projectInfo = json.getJSONObject("project_info")
        assertEquals("the-hub-f95f4", projectInfo.getString("project_id"))
        assertEquals("183373607979", projectInfo.getString("project_number"))

        // 2. Verify client matching com.thehub.hb
        val clients = json.getJSONArray("client")
        var matchingClient: JSONObject? = null
        for (i in 0 until clients.length()) {
            val c = clients.getJSONObject(i)
            val pkg = c.optJSONObject("client_info")
                ?.optJSONObject("android_client_info")
                ?.optString("package_name")
            if (pkg == "com.thehub.hb") {
                matchingClient = c
                break
            }
        }
        assertNotNull("client for package com.thehub.hb must exist", matchingClient)

        // 3. Verify OAuth clients
        val oauthClients = matchingClient!!.getJSONArray("oauth_client")
        var hasAndroidClient = false
        var hasWebClient = false

        for (i in 0 until oauthClients.length()) {
            val oc = oauthClients.getJSONObject(i)
            val type = oc.optInt("client_type")
            if (type == 1) {
                val androidInfo = oc.optJSONObject("android_info")
                if (androidInfo?.optString("package_name") == "com.thehub.hb" &&
                    androidInfo.optString("certificate_hash") == "f5cd8c88c4c85fd59184215706da52c62c57d037"
                ) {
                    hasAndroidClient = true
                }
            } else if (type == 3) {
                if (oc.optString("client_id") == GOOGLE_WEB_CLIENT_ID) {
                    hasWebClient = true
                }
            }
        }

        assertTrue("OAuth client for Android (client_type 1) must be present", hasAndroidClient)
        assertTrue("OAuth client for Web (client_type 3) must be present and match GOOGLE_WEB_CLIENT_ID", hasWebClient)
    }

    @Test
    fun testGoogleSignInResultTypes() {
        val cancelled = GoogleSignInResult.Cancelled
        assertTrue(cancelled is GoogleSignInResult)

        val error = GoogleSignInResult.Error("Network failure")
        assertEquals("Network failure", error.message)
        assertTrue(error is GoogleSignInResult)
    }

    @Test
    fun testUsernameValidation() {
        assertTrue(AuthRepository.isValidUsername("valid_user"))
        assertTrue(AuthRepository.isValidUsername("john.doe"))
        assertTrue(AuthRepository.isValidUsername("user123"))

        assertFalse(AuthRepository.isValidUsername("ab")) // too short (<3)
        assertFalse(AuthRepository.isValidUsername("a".repeat(31))) // too long (>30)
        assertFalse(AuthRepository.isValidUsername("user with spaces"))
        assertFalse(AuthRepository.isValidUsername("user@name"))
        assertFalse(AuthRepository.isValidUsername("user!"))
    }
}
