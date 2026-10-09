package com.misgastos.app.util

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Performance monitoring utility to detect and log:
 * - Frame drops
 * - Long-running operations on main thread
 * - ANR conditions
 */
@Stable
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    
    // Thresholds
    private const val FRAME_TIME_THRESHOLD_MS = 16L // ~60 FPS
    private const val MAIN_THREAD_WARNING_THRESHOLD_MS = 100L // 100ms on main thread is concerning
    private const val ANR_RISK_THRESHOLD_MS = 4000L // 4 seconds - approaching ANR
    private const val ANR_THRESHOLD_MS = 5000L // 5 seconds - ANR territory
    
    // Frame tracking
    private var lastFrameTime: Long = 0
    private var frameCount: Int = 0
    private var skippedFrames: Int = 0
    private var totalSkippedFrames: Int = 0
    
    // Main thread operation tracking
    private val mainThreadOperations = mutableMapOf<String, Long>()
    
    // ANR detection
    private var lastMainThreadCheck: Long = 0
    private var mainThreadBlocked: Boolean = false
    
    // Handler for periodic checks
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L // Check every second
    
    // Callback for performance warnings
    private var onPerformanceWarning: ((String) -> Unit)? = null
    private var onAnrDetected: (() -> Unit)? = null
    
    init {
        // Start periodic monitoring
        startMonitoring()
    }
    
    /**
     * Start the performance monitoring.
     */
    fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                checkMainThreadBlocked()
                handler.postDelayed(this, checkInterval)
            }
        })
    }
    
    /**
     * Stop the performance monitoring.
     */
    fun stopMonitoring() {
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Called at the start of each frame.
     */
    fun onFrameStart() {
        val now = SystemClock.elapsedRealtime()
        
        if (lastFrameTime > 0) {
            val frameTime = now - lastFrameTime
            
            // Detect frame drops
            if (frameTime > FRAME_TIME_THRESHOLD_MS * 2) {
                val skipped = (frameTime / FRAME_TIME_THRESHOLD_MS).toInt() - 1
                if (skipped > 0) {
                    skippedFrames += skipped
                    totalSkippedFrames += skipped
                    
                    if (skipped > 10) {
                        logWarning("Skipped $skipped frames! Frame time: ${frameTime}ms")
                        onPerformanceWarning?.invoke("Skipped $skipped frames! Frame time: ${frameTime}ms")
                    }
                }
            }
        }
        
        lastFrameTime = now
        frameCount++
    }
    
    /**
     * Called at the end of each frame.
     */
    fun onFrameEnd() {
        // Frame completed successfully
    }
    
    /**
     * Mark the start of a potentially long operation on the main thread.
     */
    fun startMainThreadOperation(tag: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "startMainThreadOperation called from non-main thread: $tag")
            return
        }
        
        mainThreadOperations[tag] = SystemClock.elapsedRealtime()
        Log.d(TAG, "Started main thread operation: $tag")
    }
    
    /**
     * Mark the end of a potentially long operation on the main thread.
     */
    fun endMainThreadOperation(tag: String) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "endMainThreadOperation called from non-main thread: $tag")
            return
        }
        
        val startTime = mainThreadOperations.remove(tag)
        if (startTime != null) {
            val duration = SystemClock.elapsedRealtime() - startTime
            
            if (duration > MAIN_THREAD_WARNING_THRESHOLD_MS) {
                logWarning("Main thread operation '$tag' took ${duration}ms")
                onPerformanceWarning?.invoke("Main thread operation '$tag' took ${duration}ms")
            }
            
            if (duration > ANR_RISK_THRESHOLD_MS) {
                logError("Main thread operation '$tag' took ${duration}ms - ANR RISK!")
                onPerformanceWarning?.invoke("Main thread operation '$tag' took ${duration}ms - ANR RISK!")
            }
            
            Log.d(TAG, "Completed main thread operation: $tag in ${duration}ms")
        }
    }
    
    /**
     * Check if the main thread is blocked.
     */
    private fun checkMainThreadBlocked() {
        val now = SystemClock.elapsedRealtime()
        
        // Check for operations that are taking too long
        mainThreadOperations.forEach { (tag, startTime) ->
            val duration = now - startTime
            
            if (duration > ANR_RISK_THRESHOLD_MS) {
                logError("Main thread operation '$tag' has been running for ${duration}ms - BLOCKING!")
                onPerformanceWarning?.invoke("Main thread operation '$tag' has been running for ${duration}ms - BLOCKING!")
            }
            
            if (duration > ANR_THRESHOLD_MS) {
                logError("ANR DETECTED! Main thread operation '$tag' exceeded ${ANR_THRESHOLD_MS}ms")
                onAnrDetected?.invoke()
            }
        }
        
        lastMainThreadCheck = now
    }
    
    /**
     * Get the total number of skipped frames.
     */
    fun getTotalSkippedFrames(): Int = totalSkippedFrames
    
    /**
     * Get the current frame count.
     */
    fun getFrameCount(): Int = frameCount
    
    /**
     * Get the skipped frames in the current session.
     */
    fun getSkippedFrames(): Int = skippedFrames
    
    /**
     * Reset the skipped frames counter for the current session.
     */
    fun resetSkippedFrames() {
        skippedFrames = 0
    }
    
    /**
     * Set a callback for performance warnings.
     */
    fun setOnPerformanceWarning(callback: (String) -> Unit) {
        onPerformanceWarning = callback
    }
    
    /**
     * Set a callback for ANR detection.
     */
    fun setOnAnrDetected(callback: () -> Unit) {
        onAnrDetected = callback
    }
    
    /**
     * Log a performance warning.
     */
    private fun logWarning(message: String) {
        Log.w(TAG, message)
    }
    
    /**
     * Log a performance error.
     */
    private fun logError(message: String) {
        Log.e(TAG, message)
    }
    
    /**
     * Utility function to run a potentially blocking operation safely.
     * If on main thread, it will warn and suggest moving to background.
     */
    fun <T> runSafely(
        tag: String,
        thresholdMs: Long = MAIN_THREAD_WARNING_THRESHOLD_MS,
        block: () -> T
    ): T {
        val isMainThread = Looper.myLooper() == Looper.getMainLooper()
        
        if (isMainThread) {
            startMainThreadOperation(tag)
            try {
                return block()
            } finally {
                endMainThreadOperation(tag)
            }
        } else {
            return block()
        }
    }
    
    /**
     * Utility function to run a blocking operation on a background thread.
     */
    fun <T> runOnBackground(
        tag: String,
        scope: CoroutineScope,
        block: suspend () -> T
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            scope.launch(Dispatchers.IO) {
                try {
                    block()
                } catch (e: Exception) {
                    Log.e(TAG, "Background operation '$tag' failed", e)
                }
            }
        } else {
            // Already on background thread
            scope.launch {
                try {
                    block()
                } catch (e: Exception) {
                    Log.e(TAG, "Background operation '$tag' failed", e)
                }
            }
        }
    }
    
    /**
     * Check if we're on the main thread.
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
    
    /**
     * Assert that we're NOT on the main thread.
     * Throws if called from main thread.
     */
    fun assertBackgroundThread(tag: String = "operation") {
        if (isMainThread()) {
            throw IllegalStateException("Operation '$tag' must be called from a background thread")
        }
    }
}
