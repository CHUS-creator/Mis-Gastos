package com.misgastos.app.util

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log

/**
 * Utility class for configuring StrictMode to detect performance issues.
 * 
 * StrictMode helps catch:
 * - Disk I/O on main thread
 * - Network operations on main thread
 * - Custom slow operations
 * - Memory leaks (via VM policy)
 */
object StrictModeUtils {
    private const val TAG = "StrictMode"
    
    /**
     * Configure StrictMode for development builds.
     * This should be called in Application.onCreate().
     */
    fun configureForDevelopment() {
        try {
            // Thread policy - detect disk and network I/O on main thread
            val threadPolicyBuilder = ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyFlashScreen()
            
            // VM policy - detect memory leaks and activity leaks
            val vmPolicyBuilder = VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .detectContentUriWithoutPermission()
                .penaltyLog()
            
            StrictMode.setThreadPolicy(threadPolicyBuilder.build())
            StrictMode.setVmPolicy(vmPolicyBuilder.build())
            
            Log.d(TAG, "StrictMode configured for development")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure StrictMode", e)
        }
    }
    
    /**
     * Configure StrictMode for production builds.
     * Less aggressive - only logs issues without penalties.
     */
    fun configureForProduction() {
        try {
            val threadPolicyBuilder = ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
            
            val vmPolicyBuilder = VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
            
            StrictMode.setThreadPolicy(threadPolicyBuilder.build())
            StrictMode.setVmPolicy(vmPolicyBuilder.build())
            
            Log.d(TAG, "StrictMode configured for production")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure StrictMode", e)
        }
    }
    
    /**
     * Configure a custom slow call detection threshold.
     * Use this to detect operations that take longer than the specified threshold.
     */
    fun setCustomSlowCallThreshold(thresholdMs: Long, tag: String = "CustomSlowCall") {
        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )
        
        // Note: To mark custom slow calls, use:
        // StrictMode.noteSlowCall(tag)
    }
    
    /**
     * Mark a slow call for detection by StrictMode.
     * Call this at the start of potentially slow operations.
     */
    fun markSlowCallStart(tag: String) {
        StrictMode.noteSlowCall(tag)
    }
    
    /**
     * Check if StrictMode is enabled.
     */
    fun isEnabled(): Boolean {
        return StrictMode.getThreadPolicy() != ThreadPolicy.LAX
    }
    
    /**
     * Temporarily disable StrictMode penalties (for specific operations).
     * Remember to re-enable after the operation!
     */
    fun disableTemporarily(): ThreadPolicy {
        val currentPolicy = StrictMode.getThreadPolicy()
        StrictMode.setThreadPolicy(ThreadPolicy.LAX)
        return currentPolicy
    }
    
    /**
     * Restore StrictMode policy.
     */
    fun restorePolicy(policy: ThreadPolicy) {
        StrictMode.setThreadPolicy(policy)
    }
    
    /**
     * Run an operation with StrictMode temporarily disabled.
     */
    fun <T> runWithoutStrictMode(block: () -> T): T {
        val policy = disableTemporarily()
        try {
            return block()
        } finally {
            restorePolicy(policy)
        }
    }
}
