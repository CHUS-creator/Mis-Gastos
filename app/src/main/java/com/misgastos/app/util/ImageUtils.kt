package com.misgastos.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    private const val MAX_DIMENSION = 2048
    private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB
    private const val QUALITY = 90

    /**
     * Loads and downsamples a bitmap from a URI to a reasonable size for OCR processing.
     * This should be called from a background thread (Dispatchers.IO).
     */
    fun loadAndDownsampleBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate inSampleSize for downsampling
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val inputStream2 = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2.close()

            // If still too large, scale it down
            if (bitmap != null && (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION)) {
                bitmap = scaleBitmap(bitmap, MAX_DIMENSION, MAX_DIMENSION)
            }

            // Rotate according to EXIF data
            bitmap?.let { rotateBitmapIfNeeded(context, uri, it) }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads and downsamples a bitmap from a file path to a reasonable size for OCR processing.
     */
    fun loadAndDownsampleBitmap(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            // Calculate inSampleSize for downsampling
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            var bitmap = BitmapFactory.decodeFile(filePath, options)

            // If still too large, scale it down
            if (bitmap != null && (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION)) {
                bitmap = scaleBitmap(bitmap, MAX_DIMENSION, MAX_DIMENSION)
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a downscaled temporary file for OCR processing.
     * This is useful when you need a file path for ML Kit.
     */
    fun createDownscaledTempFile(context: Context, uri: Uri): File? {
        val bitmap = loadAndDownsampleBitmap(context, uri) ?: return null
        return saveBitmapToTempFile(context, bitmap)
    }

    /**
     * Creates a downscaled temporary file from a bitmap.
     */
    fun saveBitmapToTempFile(context: Context, bitmap: Bitmap): File? {
        return try {
            val tempDir = File(context.cacheDir, "ocr_temp")
            if (!tempDir.exists()) tempDir.mkdirs()

            val tempFile = File.createTempFile("ocr_${System.currentTimeMillis()}", ".jpg", tempDir)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate an inSampleSize for use in a BitmapFactory.Options object.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }

            // Round up to nearest power of 2
            val totalPixels = (width / inSampleSize) * (height / inSampleSize)
            val totalReqPixelsCap = reqWidth * reqHeight * 2

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Scale a bitmap to fit within the specified dimensions while maintaining aspect ratio.
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = scaleWidth.coerceAtMost(scaleHeight)

        if (scale >= 1.0f) return bitmap

        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(
            bitmap, 0, 0, width, height,
            matrix, true
        )
    }

    /**
     * Rotate bitmap according to EXIF orientation data.
     */
    private fun rotateBitmapIfNeeded(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            // If EXIF reading fails, return the original bitmap
            bitmap
        }
    }

    /**
     * Get the dimensions of an image from a URI without loading the full image.
     */
    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if an image is too large for efficient OCR processing.
     */
    fun isImageTooLarge(context: Context, uri: Uri): Boolean {
        val dimensions = getImageDimensions(context, uri) ?: return false
        return dimensions.first > MAX_DIMENSION || dimensions.second > MAX_DIMENSION
    }
}
