package com.misgastos.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.misgastos.app.util.ImageUtils
import com.misgastos.app.viewmodel.misGastosApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Optimized OCR Recognizer that:
 * 1. Processes images on background threads (Dispatchers.IO)
 * 2. Automatically downsamples large images to prevent OOM and improve performance
 * 3. Handles image rotation based on EXIF data
 * 4. Provides detailed logging for debugging
 */
class OcrRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private val TAG = "OcrRecognizer"

    /**
     * Recognizes text from an image URI.
     * 
     * This function:
     * - Automatically downsamples large images (>2048px) to prevent memory issues
     * - Processes on background thread to avoid blocking UI
     * - Handles image rotation based on EXIF data
     * 
     * @param context Android context
     * @param uri Image URI
     * @return Extracted text from the image
     * @throws Exception if recognition fails
     */
    suspend fun recognize(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                // Check if image is too large and needs downsampling
                val isTooLarge = ImageUtils.isImageTooLarge(context, uri)
                
                if (isTooLarge) {
                    Log.d(TAG, "Large image detected, downsampling before OCR processing")
                }

                // Create InputImage - ML Kit handles most of the heavy lifting
                val inputImage = try {
                    InputImage.fromFilePath(context, uri)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create InputImage from URI, trying bitmap approach: ${e.message}")
                    // Fallback: load and downsample manually
                    val bitmap = ImageUtils.loadAndDownsampleBitmap(context, uri)
                        ?: throw Exception("Failed to load and downsample image")
                    InputImage.fromBitmap(bitmap, 0)
                }

                // Process the image
                Log.d(TAG, "Starting OCR processing for image: $uri")
                val startTime = System.currentTimeMillis()
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "OCR completed successfully in ${processingTime}ms, text length: ${result.text.length}")
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "OCR failed after ${processingTime}ms: ${e.message}", e)
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in recognize: ${e.message}", e)
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Recognizes text from a bitmap directly.
     * Use this when you already have a bitmap in memory.
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
                
                Log.d(TAG, "Starting OCR processing for bitmap: ${bitmap.width}x${bitmap.height}")
                val startTime = System.currentTimeMillis()
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Bitmap OCR completed in ${processingTime}ms, text length: ${result.text.length}")
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Bitmap OCR failed after ${processingTime}ms: ${e.message}", e)
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in recognize(bitmap): ${e.message}", e)
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Recognizes text from a file path.
     */
    suspend fun recognizeFromFilePath(filePath: String): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                val inputImage = InputImage.fromFilePath(misGastosApplication, Uri.parse(filePath))
                
                Log.d(TAG, "Starting OCR processing for file: $filePath")
                val startTime = System.currentTimeMillis()
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "File OCR completed in ${processingTime}ms")
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "File OCR failed after ${processingTime}ms: ${e.message}", e)
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in recognizeFromFilePath: ${e.message}", e)
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Recognizes text from a URI with explicit downsampling.
     * Use this when you want to ensure the image is downscaled before processing.
     */
    suspend fun recognizeWithDownsampling(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            try {
                Log.d(TAG, "Downsampling image before OCR: $uri")
                
                // Create a downscaled temporary file
                val tempFile = ImageUtils.createDownscaledTempFile(context, uri)
                    ?: throw Exception("Failed to create downscaled temp file")
                
                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(tempFile))
                
                // Clean up temp file after processing
                val startTime = System.currentTimeMillis()
                
                recognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.d(TAG, "Downsampled OCR completed in ${processingTime}ms")
                        // Clean up temp file
                        tempFile.delete()
                        cont.resume(result.text)
                    }
                    .addOnFailureListener { e ->
                        val processingTime = System.currentTimeMillis() - startTime
                        Log.e(TAG, "Downsampled OCR failed after ${processingTime}ms: ${e.message}", e)
                        // Clean up temp file
                        tempFile.delete()
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in recognizeWithDownsampling: ${e.message}", e)
                cont.resumeWithException(e)
            }
        }
    }

    /**
     * Get image dimensions without loading the full image.
     */
    suspend fun getImageInfo(context: Context, uri: Uri): ImageInfo = withContext(Dispatchers.IO) {
        val dimensions = ImageUtils.getImageDimensions(context, uri)
        val isTooLarge = dimensions?.let { it.first > 2048 || it.second > 2048 } ?: false
        ImageInfo(
            width = dimensions?.first ?: 0,
            height = dimensions?.second ?: 0,
            isTooLarge = isTooLarge
        )
    }

    data class ImageInfo(
        val width: Int,
        val height: Int,
        val isTooLarge: Boolean
    )
}
