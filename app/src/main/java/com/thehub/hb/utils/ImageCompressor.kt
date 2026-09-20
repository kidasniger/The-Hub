package com.thehub.hb.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    /**
     * Reads a local image Uri, corrects EXIF orientation, resizes it for the Feed,
     * and compresses it to JPEG. The output is kept below [maxBytes] whenever possible.
     */
    suspend fun compressImageFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        quality: Int = 82,
        maxBytes: Int = 4 * 1024 * 1024
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext null
            }

            var sampleSize = 1
            val maxSide = max(bounds.outWidth, bounds.outHeight)

            while (maxSide / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    bitmap = rotateBitmapIfNeeded(bitmap, orientation)
                }
            } catch (_: Exception) {
                // Keep the decoded bitmap if EXIF metadata cannot be read.
            }

            bitmap = scaleToMaxDimension(bitmap, maxDimension)

            var currentQuality = quality.coerceIn(40, 100)
            var encoded = encodeJpeg(bitmap, currentQuality)

            // Reduce JPEG quality first.
            while (encoded.size > maxBytes && currentQuality > 50) {
                currentQuality = (currentQuality - 8).coerceAtLeast(50)
                encoded = encodeJpeg(bitmap, currentQuality)
            }

            // If quality reduction is insufficient, progressively scale down.
            repeat(3) {
                if (encoded.size <= maxBytes) return@repeat

                val smallerMax = (max(bitmap.width, bitmap.height) * 0.85f)
                    .toInt()
                    .coerceAtLeast(800)

                if (smallerMax >= max(bitmap.width, bitmap.height)) return@repeat

                val scaled = scaleToMaxDimension(bitmap, smallerMax)
                if (scaled !== bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }

                encoded = encodeJpeg(bitmap, currentQuality)
            }

            bitmap.recycle()

            // Do not silently upload the original full-resolution asset.
            if (encoded.isEmpty()) null else encoded
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleToMaxDimension(
        bitmap: Bitmap,
        maxDimension: Int
    ): Bitmap {
        val currentMax = max(bitmap.width, bitmap.height)
        if (currentMax <= maxDimension) return bitmap

        val ratio = maxDimension.toFloat() / currentMax
        val newWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        )
    }

    private fun encodeJpeg(
        bitmap: Bitmap,
        quality: Int
    ): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                output
            )
            output.toByteArray()
        }
    }

    private fun rotateBitmapIfNeeded(
        bitmap: Bitmap,
        orientation: Int
    ): Bitmap {
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        if (rotated !== bitmap) {
            bitmap.recycle()
        }

        return rotated
    }
}
