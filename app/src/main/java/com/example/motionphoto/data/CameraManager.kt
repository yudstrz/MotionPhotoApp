package com.example.motionphoto.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    
    private lateinit var cameraProvider: ProcessCameraProvider
    private var extensionsManager: ExtensionsManager? = null
    private val executor = Executors.newSingleThreadExecutor()
    
    // State callbacks
    var onQrCodeDetected: ((String) -> Unit)? = null
    
    suspend fun initialize(): Unit = suspendCancellableCoroutine { cont ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Initialize ExtensionsManager for HDR
            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
            extensionsManagerFuture.addListener({
                extensionsManager = extensionsManagerFuture.get()
                cont.resume(Unit)
            }, ContextCompat.getMainExecutor(context))
            
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun startCameraPreview(
        previewView: PreviewView,
        useHdr: Boolean = false,
        useQrScanner: Boolean = false
    ) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        
        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        // Apply HDR if requested and supported
        if (useHdr && extensionsManager != null) {
            if (extensionsManager!!.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)) {
                cameraSelector = extensionsManager!!.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.HDR)
            }
        }
        
        val useCases = mutableListOf<UseCase>(preview)
        
        // Apply QR Scanner if requested
        if (useQrScanner) {
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                barcode.rawValue?.let { value ->
                                    onQrCodeDetected?.invoke(value)
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }
            useCases.add(imageAnalysis)
        }
        
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            *useCases.toTypedArray()
        )
    }
    
    suspend fun captureMotionPhoto(
        videoDurationMs: Int = 3000,
        fps: Int = 30,
        useHdr: Boolean = false
    ): Result<Pair<File, File>> = withContext(Dispatchers.Default) {
        try {
            val photoFile = File.createTempFile("photo", ".jpg", context.cacheDir)
            val videoFile = File.createTempFile("video", ".mp4", context.cacheDir)
            
            val imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1080, 1920))
                .build()
            
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            
            val videoCapture = VideoCapture.withOutput(recorder)
            val preview = Preview.Builder().build()
            
            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            if (useHdr && extensionsManager?.isExtensionAvailable(cameraSelector, ExtensionMode.HDR) == true) {
                cameraSelector = extensionsManager!!.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.HDR)
            }
            
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )
            }
            
            coroutineScope {
                val photoTask = async {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    cont.resume(true)
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    cont.resumeWithException(exc)
                                }
                            }
                        )
                    }
                }
                
                val videoTask = async {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        val pendingRecording = recorder.prepareRecording(context, androidx.camera.video.FileOutputOptions.Builder(videoFile).build())
                        val recording = pendingRecording.start(executor) {}
                        
                        Handler(Looper.getMainLooper()).postDelayed(
                            { 
                                recording.stop()
                                cont.resume(true)
                            },
                            videoDurationMs.toLong()
                        )
                    }
                }
                
                photoTask.await()
                videoTask.await()
            }
            
            Result.success(Pair(photoFile, videoFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun release() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
        executor.shutdown()
    }
}
