package com.guiorioli.ollamatalk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.Base64

object ImageUtils {

    private const val MAX_DIMENSION = 1024
    private const val COMPRESS_QUALITY = 80

    fun compressAndEncode(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val scaled = scaleDown(bitmap, MAX_DIMENSION)
            if (scaled != bitmap) bitmap.recycle()

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            scaled.recycle()

            Base64.getEncoder().encodeToString(bytes)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val (width, height) = bitmap.width to bitmap.height
        val max = maxOf(width, height)
        if (max <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / max
        return Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
    }
}
