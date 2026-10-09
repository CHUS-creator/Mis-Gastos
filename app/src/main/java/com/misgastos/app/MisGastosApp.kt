package com.misgastos.app

import android.app.Application
import android.util.Log
import com.misgastos.app.util.PerformanceMonitor
import com.misgastos.app.viewmodel.misGastosApplication

class MisGastosApp : Application() {
    
    private val TAG = "MisGastosApp"
    
    override fun onCreate() {
        super.onCreate()
        misGastosApplication = this
        
        // Initialize StrictMode for development
        StrictModeUtils.configureForDevelopment()
        
        // Initialize performance monitoring
        PerformanceMonitor.startMonitoring()
        
        // Set up performance warning callbacks
        PerformanceMonitor.setOnPerformanceWarning { warning ->
            Log.w(TAG, "Performance warning: $warning")
        }
        
        PerformanceMonitor.setOnAnrDetected {
            Log.e(TAG, "ANR detected! Application may freeze")
        }
        
        Log.d(TAG, "Application initialized with performance monitoring and StrictMode")
    }
    
    override fun onTerminate() {
        PerformanceMonitor.stopMonitoring()
        super.onTerminate()
    }
}
