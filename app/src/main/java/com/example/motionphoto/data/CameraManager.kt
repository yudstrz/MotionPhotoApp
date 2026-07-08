package com.example.motionphoto.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    
    private lateinit var cameraProvider: ProcessCameraProvider
    
    suspend fun initialize(): Unit = suspendCancellableCoroutine { cont ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            cont.resume(Unit)
        }, Executors.newSingleThreadExecutor())
    }
    
    fun startCameraPreview(previewView: PreviewView) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview
        )
    }
    
    suspend fun captureMotionPhoto(
        videoDurationMs: Int = 3000,
        fps: Int = 30
    ): Result<Pair<File, File>> = withContext(Dispatchers.Default) {
        try {
            val photoFile = File.createTempFile("photo", ".jpg", context.cacheDir)
            val videoFile = File.createTempFile("video", ".mp4", context.cacheDir)
            
            // Photo capture
            val imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1080, 1920))
                .build()
            
            // Video capture (3 seconds max)
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.HD)
                )
                .build()
            
            val videoCapture = VideoCapture.withOutput(recorder)
            
            // Preview for real-time feedback
            val preview = Preview.Builder().build()
            
            // Bind all three simultaneously
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // Unbind use cases before rebinding
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
                // Capture photo
                val photoTask = async {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                            Executors.newSingleThreadExecutor(),
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
                
                // Capture video (start when photo is taken)
                val videoTask = async {
                    suspendCancellableCoroutine<Boolean> { cont ->
                        val pendingRecording = recorder.prepareRecording(context, androidx.camera.video.FileOutputOptions.Builder(videoFile).build())
                        
                        val recording = pendingRecording.start(Executors.newSingleThreadExecutor()) { recordingStats ->
                            // we can handle events here
                        }
                        
                        // Stop after duration
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
    }
}
