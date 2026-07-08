package com.example.motionphoto.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.motionphoto.utils.FrameExtractor
import com.example.motionphoto.utils.XmpMetadataHandler
import java.io.File

class MotionPhotoRepository(
    private val context: Context,
    private val cameraManager: CameraManager,
    private val mediaProcessor: MediaProcessor,
    private val frameExtractor: FrameExtractor
) {
    
    data class CaptureResult(
        val motionPhotoFile: File,
        val keyPhotoIndex: Int = 0, // Which frame selected as key
        val capturedAt: Long = System.currentTimeMillis()
    )
    
    suspend fun captureAndCreateMotionPhoto(): Result<CaptureResult> {
        try {
            // Step 1: Capture photo + video simultaneously
            val (photoFile, videoFile) = cameraManager.captureMotionPhoto()
                .getOrThrow()
            
            // Step 2: Mux into single Motion Photo file
            val outputPath = File(getCacheDir(), "motion_photo_${System.currentTimeMillis()}.jpg")
            val motionPhotoFile = mediaProcessor.createMotionPhotoFile(
                photoFile.absolutePath,
                videoFile.absolutePath,
                outputPath.absolutePath
            ).getOrThrow()
            
            // Clean up temp files
            photoFile.delete()
            videoFile.delete()
            
            return Result.success(
                CaptureResult(
                    motionPhotoFile = motionPhotoFile,
                    keyPhotoIndex = 0
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun replaceKeyPhotoFrame(
        motionPhotoFile: File,
        newKeyFrameTimestampUs: Long
    ): Result<File> {
        try {
            // Extract frame from embedded video
            val frameJpeg = frameExtractor.extractAndEncodeFrame(
                motionPhotoFile.absolutePath,
                newKeyFrameTimestampUs,
                File(getCacheDir(), "new_key_photo.jpg").absolutePath
            ).getOrThrow()
            
            // In a full implementation, we'd extract the video and remux.
            
            return Result.success(frameJpeg)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun saveToGallery(motionPhotoFile: File): Result<Uri> {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("Failed to create media store entry")
            
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(motionPhotoFile.readBytes())
            }
            
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getCacheDir(): File {
        return File(context.cacheDir, "motion_photos").apply {
            mkdirs()
        }
    }
}
