package com.example.motionphoto.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FrameExtractor {
    
    fun extractFrameAtTime(
        videoPath: String,
        timestampUs: Long
    ): Result<Bitmap> = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        
        val bitmap = retriever.getFrameAtTime(
            timestampUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
        
        retriever.release()
        
        if (bitmap != null) {
            Result.success(bitmap)
        } else {
            Result.failure(Exception("Frame extraction returned null"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun extractAndEncodeFrame(
        videoPath: String,
        timestampUs: Long,
        outputJpegPath: String,
        quality: Int = 95
    ): Result<File> = withContext(Dispatchers.Default) {
        try {
            val bitmap = extractFrameAtTime(videoPath, timestampUs)
                .getOrThrow()
            
            // Encode to JPEG
            val outputFile = File(outputJpegPath)
            outputFile.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            }
            
            bitmap.recycle() // Free memory
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
