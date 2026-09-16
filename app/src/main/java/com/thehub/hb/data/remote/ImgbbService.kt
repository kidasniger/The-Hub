package com.thehub.hb.data.remote

import android.util.Base64
import android.util.Log
import com.thehub.hb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The Hub image transport.
 * All image bytes are sent directly to ImgBB. Firebase Storage is intentionally not used.
 */
class ImgbbService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val DEFAULT_KEY = "f5cf2a4a5280b1e58ae04cea77b5607d"
        private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
        private const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private const val TAG = "ImgbbService"

        private val API_KEY: String
            get() = BuildConfig.IMGBB_API_KEY
                .trim()
                .takeIf { it.isNotBlank() && it != "YOUR_IMGBB_API_KEY" }
                ?: DEFAULT_KEY
    }

    suspend fun uploadImage(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Image vide"))
        }
        if (imageBytes.size > MAX_IMAGE_BYTES) {
            return@withContext Result.failure(IllegalArgumentException("Image trop volumineuse (8 Mo maximum)"))
        }

        try {
            val key = API_KEY
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "upload.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("$UPLOAD_URL?key=$key")
                .post(requestBody)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                Log.d(TAG, "ImgBB upload response code: ${response.code}")

                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody)
                            .optJSONObject("error")
                            ?.optString("message")
                            ?.takeIf { it.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    } ?: "Échec de l'envoi de l'image (code ${response.code})"
                    return@withContext Result.failure(IOException(message))
                }

                val json = JSONObject(responseBody)
                if (!json.optBoolean("success")) {
                    val message = json.optJSONObject("error")?.optString("message")
                        ?.takeIf { it.isNotBlank() } ?: "Erreur d'upload ImgBB"
                    return@withContext Result.failure(IOException(message))
                }

                val url = json.optJSONObject("data")?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: return@withContext Result.failure(IOException("ImgBB n'a pas renvoyé d'URL d'image"))
                Result.success(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ImgBB upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
