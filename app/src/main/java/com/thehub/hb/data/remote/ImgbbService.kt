package com.thehub.hb.data.remote

import android.util.Base64
import android.util.Log
import com.thehub.hb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImgbbService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val DEFAULT_KEY = "f5cf2a4a5280b1e58ae04cea77b5607d"
        private val API_KEY: String
            get() = if (BuildConfig.IMGBB_API_KEY.isNotBlank() && BuildConfig.IMGBB_API_KEY != "YOUR_IMGBB_API_KEY") {
                BuildConfig.IMGBB_API_KEY
            } else {
                DEFAULT_KEY
            }
        private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
        private const val TAG = "ImgbbService"
    }

    suspend fun uploadImage(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(IOException("Image vide."))
        }

        val key = API_KEY.trim()
        if (key.isBlank() || key == "YOUR_IMGBB_API_KEY") {
            return@withContext Result.failure(IOException("Clé ImgBB non configurée."))
        }

        // ImgBB accepts a base64 image in an x-www-form-urlencoded POST.
        // This avoids multipart/file MIME mismatches for Android Photo Picker media.
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        if (base64Image.isBlank()) {
            return@withContext Result.failure(IOException("Impossible d'encoder l'image."))
        }

        var lastFailure: Exception? = null

        repeat(3) { attempt ->
            try {
                val requestBody = FormBody.Builder()
                    .add("image", base64Image)
                    .build()

                val request = Request.Builder()
                    .url("$UPLOAD_URL?key=$key")
                    .header("Accept", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    Log.d(TAG, "ImgBB upload response code: " + response.code)

                    val json = try {
                        JSONObject(responseBody)
                    } catch (_: Exception) {
                        null
                    }

                    if (response.isSuccessful && json?.optBoolean("success") == true) {
                        val data = json.optJSONObject("data")
                        val url = data?.optString("url")?.trim().orEmpty()
                        if (url.isNotBlank()) {
                            Log.d(TAG, "ImgBB upload succeeded on attempt " + (attempt + 1))
                            return@withContext Result.success(url)
                        }
                    }

                    val errorObject = json?.optJSONObject("error")
                    val errorMessage = errorObject?.optString("message")
                        ?.takeIf { it.isNotBlank() }
                        ?: json?.optString("status")
                            ?.takeIf { it.isNotBlank() }
                        ?: responseBody.takeIf { it.isNotBlank() }
                        ?: "Échec de l'envoi de l'image (HTTP " + response.code + ")"

                    lastFailure = IOException(errorMessage)

                    // Retry only transient HTTP failures. Client/configuration
                    // errors are returned immediately with the real server message.
                    if (response.code in 400..499) {
                        return@withContext Result.failure(lastFailure!!)
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
            lastFailure ?: IOException("Échec de l'upload de l'image vers ImgBB.")
        )
    }
}

