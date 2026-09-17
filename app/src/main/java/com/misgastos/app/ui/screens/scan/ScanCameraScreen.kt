package com.misgastos.app.ui.screens.scan

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.misgastos.app.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanCameraScreen(
    onCaptured: (Uri) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture
        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        if (capturing) return@Button
                        capturing = true
                        imageCapture?.let { capture ->
                            val file = createImageFile(context)
                            val output = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(
                                output,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                        val savedUri = results.savedUri ?: Uri.fromFile(file)
                                        capturing = false
                                        onCaptured(savedUri)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        capturing = false
                                    }
                                },
                            )
                        } ?: run { capturing = false }
                    },
                    enabled = !capturing,
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = null)
                    Text(stringResource(R.string.scan_capture))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(System.currentTimeMillis())
    val outputDir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
    return File(outputDir, "$name.jpg")
}
