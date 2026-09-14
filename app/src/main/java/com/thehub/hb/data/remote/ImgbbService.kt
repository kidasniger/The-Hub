package com.thehub.hb.data.remote

import android.util.Base64
import com.thehub.hb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImgbbService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private val API_KEY: String
            get() = if (BuildConfig.IMGBB_API_KEY.isNotEmpty()) BuildConfig.IMGBB_API_KEY else "f5cf2a4a5280b1e58ae04cea77b5607d"
        private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
    }

    suspend fun uploadImage(imageBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", base64Image)
                .build()

            val request = Request.Builder()
                .url("$UPLOAD_URL?key=$API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Upload échoué: code ${response.code}"))
            }

            val json = JSONObject(responseBody)
            if (json.optBoolean("success")) {
                val data = json.getJSONObject("data")
                val url = data.getString("url")
                Result.success(url)
            } else {
                val errorMsg = json.optJSONObject("error")?.optString("message") ?: "Erreur d'upload"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
