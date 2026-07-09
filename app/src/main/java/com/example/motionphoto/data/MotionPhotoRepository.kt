package com.example.motionphoto.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.motionphoto.utils.FrameExtractor
import com.example.motionphoto.utils.XmpMetadataHandler
import java.io.File

import android.graphics.*
import android.location.Location
import android.media.ExifInterface

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
    
    suspend fun captureAndCreateMotionPhoto(
        useHdr: Boolean = false,
        addWatermark: Boolean = false,
        location: Location? = null,
        lensFacing: Int = 1,
        aspectRatioMode: Int = 0,
        isHighRes: Boolean = false,
        flashMode: Int = 0,
        isMotionPhotoEnabled: Boolean = true
    ): Result<CaptureResult> {
        try {
            // Step 1: Capture photo (+ optional video)
            val (photoFile, videoFile) = cameraManager.captureMotionPhoto(
                useHdr = useHdr,
                lensFacing = lensFacing,
                aspectRatioMode = aspectRatioMode,
                isHighRes = isHighRes,
                flashMode = flashMode,
                isMotionPhotoEnabled = isMotionPhotoEnabled
            ).getOrThrow()
                
            // Apply Watermark
            if (addWatermark) {
                val options = BitmapFactory.Options()
                options.inMutable = true
                var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                
                val exif = ExifInterface(photoFile.absolutePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 100f
                    setShadowLayer(5f, 2f, 2f, Color.BLACK)
                }
                canvas.drawText("Shot on MotionPhotoApp", 80f, bitmap.height - 80f, paint)
                
                java.io.FileOutputStream(photoFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
            
            // Apply Geotagging (Deferred: Android Framework ExifInterface lacks simple setLatLong)
            if (location != null) {
                // TODO: Implement GPS EXIF formatting for Location manually
            }
            
            val finalOutput: File
            
            // Step 2: Mux into single Motion Photo file (or just return photo)
            if (isMotionPhotoEnabled && videoFile != null) {
                val outputPath = File(getCacheDir(), "motion_photo_${System.currentTimeMillis()}.jpg")
                finalOutput = mediaProcessor.createMotionPhotoFile(
                    photoFile.absolutePath,
                    videoFile.absolutePath,
                    outputPath.absolutePath
                ).getOrThrow()
                
                photoFile.delete()
                videoFile.delete()
            } else {
                finalOutput = photoFile
            }
            
            return Result.success(
                CaptureResult(
                    motionPhotoFile = finalOutput,
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
