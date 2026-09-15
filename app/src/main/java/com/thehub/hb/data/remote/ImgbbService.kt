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
        try {
            val key = API_KEY.trim()
            val mediaType = "image/jpeg".toMediaTypeOrNull()

            // ImgBB accepts either multipart file or base64 string
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", key)
                .addFormDataPart("image", "upload.jpg", imageBytes.toRequestBody(mediaType))
                .build()

            val request = Request.Builder()
                .url("$UPLOAD_URL?key=$key")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            Log.d(TAG, "ImgBB upload response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optJSONObject("error")?.optString("message") ?: "Erreur ${response.code}: $responseBody"
                } catch (_: Exception) {
                    "Échec de l'envoi de l'image (code ${response.code})"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val json = JSONObject(responseBody)
            if (json.optBoolean("success")) {
                val data = json.getJSONObject("data")
                val url = data.getString("url")
                Result.success(url)
            } else {
                val errorMsg = json.optJSONObject("error")?.optString("message") ?: "Erreur d'upload ImgBB"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ImgBB upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

