package com.galeria.defensores.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    private const val MAX_DIMENSION = 300 // Max width/height in pixels
    private const val COMPRESSION_QUALITY = 70 // 0-100

    fun compressImage(context: Context, imageUri: Uri): ByteArray? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            val width = originalBitmap.width
            val height = originalBitmap.height

            val newWidth: Int
            val newHeight: Int

            if (width > height) {
                if (width > MAX_DIMENSION) {
                    newWidth = MAX_DIMENSION
                    newHeight = (height * (MAX_DIMENSION.toFloat() / width)).toInt()
                } else {
                    newWidth = width
                    newHeight = height
                }
            } else {
                if (height > MAX_DIMENSION) {
                    newHeight = MAX_DIMENSION
                    newWidth = (width * (MAX_DIMENSION.toFloat() / height)).toInt()
                } else {
                    newWidth = width
                    newHeight = height
                }
            }

            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            
            return outputStream.toByteArray()

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
        }
    }
}
