package com.thehub.hb.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object ImageSaver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Download and save an image to the device gallery / Pictures folder.
     * Works seamlessly on Android 10+ (scoped storage) and older versions.
     */
    suspend fun saveImageToGallery(context: Context, imageUrl: String): Result<String> = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("URL d'image invalide"))
        }

        try {
            val request = Request.Builder().url(imageUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Échec du téléchargement (code ${response.code})"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Image vide"))
            val imageBytes = responseBody.bytes()

            val fileName = "TheHub_${System.currentTimeMillis()}.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TheHub")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Impossible de créer l'entrée média"))

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(imageBytes)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val theHubDir = File(picturesDir, "TheHub").apply { mkdirs() }
                val imageFile = File(theHubDir, fileName)

                FileOutputStream(imageFile).use { output ->
                    output.write(imageBytes)
                    output.flush()
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image enregistrée dans la galerie", Toast.LENGTH_SHORT).show()
            }

            Result.success("Image enregistrée dans la galerie")
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Échec de l'enregistrement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            Result.failure(e)
        }
    }
}
