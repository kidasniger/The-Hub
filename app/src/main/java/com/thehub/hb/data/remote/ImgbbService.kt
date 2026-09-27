package com.thehub.hb.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImgbbService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val UPLOAD_FUNCTION_URL =
            "https://europe-west1-the-hub-f95f4.cloudfunctions.net/uploadImageToImgbb"
        private const val TAG = "ImgbbService"
    }

    suspend fun uploadImage(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(IOException("Image vide."))
        }

        val user = auth.currentUser
            ?: return@withContext Result.failure(IOException("Session Firebase requise."))

        var lastFailure: Exception? = null
        var forceRefresh = false

        repeat(3) { attempt ->
            try {
                val idToken = user.getIdToken(forceRefresh).await()?.token
                    ?.trim()
                    .orEmpty()

                if (idToken.isBlank()) {
                    return@withContext Result.failure(
                        IOException("Impossible d'obtenir le jeton Firebase.")
                    )
                }

                val requestBody = imageBytes.toRequestBody(
                    "application/octet-stream".toMediaType()
                )

                val request = Request.Builder()
                    .url(UPLOAD_FUNCTION_URL)
                    .header("Authorization", "Bearer $idToken")
                    .header("Accept", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    Log.d(TAG, "Image upload response code: " + response.code)

                    val json = try {
                        JSONObject(responseBody)
                    } catch (_: Exception) {
                        null
                    }

                    if (response.isSuccessful) {
                        val url = json?.optString("url")?.trim().orEmpty()
                        if (url.startsWith("https://")) {
                            Log.d(
                                TAG,
                                "ImgBB upload succeeded on attempt " + (attempt + 1)
                            )
                            return@withContext Result.success(url)
                        }
                    }

                    if (response.code == 401 && attempt == 0) {
                        forceRefresh = true
                        lastFailure = IOException("Session Firebase expirée.")
                    } else {
                        val errorMessage = json
                            ?.optString("error")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Échec de l'envoi de l'image (HTTP " + response.code + ")."

                        lastFailure = IOException(errorMessage)

                        if (response.code in 400..499 && response.code != 429) {
                            return@withContext Result.failure(lastFailure!!)
                        }
                    }
                }
            } catch (e: Exception) {
                lastFailure = e
            }

            if (attempt < 2) {
                kotlinx.coroutines.delay(750L * (attempt + 1))
            }
        }

        Result.failure(
            lastFailure ?: IOException("Échec de l'upload de l'image.")
        )
    }
}
