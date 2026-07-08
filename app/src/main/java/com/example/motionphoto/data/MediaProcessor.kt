package com.example.motionphoto.data

import android.content.Context
import android.util.Log
import com.example.motionphoto.utils.XmpMetadataHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaProcessor(private val context: Context) {
    
    suspend fun createMotionPhotoFile(
        photoPath: String,
        videoPath: String,
        outputPath: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(photoPath)
            val videoFile = File(videoPath)
            val outputFile = File(outputPath)
            
            // Step 1: Concatenate JPEG + MP4
            photoFile.inputStream().use { photoStream ->
                videoFile.inputStream().use { videoStream ->
                    outputFile.outputStream().use { outputStream ->
                        // Write photo
                        photoStream.copyTo(outputStream)
                        
                        // Write video (appended)
                        videoStream.copyTo(outputStream)
                    }
                }
            }
            
            // Step 2: Update XMP metadata
            val videoSize = videoFile.length()
            val xmpHandler = XmpMetadataHandler()
            val xmpXml = xmpHandler.buildXmpXmlString(
                isMotionPhoto = true,
                videoOffsetBytes = videoSize,
                videoDurationMs = 3000
            )
            
            // Step 3: Inject XMP into output file
            injectXmpMetadata(outputFile, xmpXml)
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun injectXmpMetadata(jpegFile: File, xmpXml: String) {
        try {
            // Read current JPEG
            val originalBytes = jpegFile.readBytes()
            
            // Find APP1 marker (FFE1) for XMP insertion
            val xmpMarker = byteArrayOf(0xFF.toByte(), 0xE1.toByte())
            val markerPosition = findMarkerPosition(originalBytes, xmpMarker)
            
            if (markerPosition >= 0) {
                // Skip old XMP, write new one
                val beforeXmp = originalBytes.sliceArray(0 until markerPosition)
                val xmpBytes = xmpXml.toByteArray(Charsets.UTF_8)
                
                val newBytes = beforeXmp + createXmpSegment(xmpBytes) + 
                    originalBytes.sliceArray(markerPosition + 1 until originalBytes.size)
                
                jpegFile.writeBytes(newBytes)
            } else {
                // Append new XMP segment if none exists
                val xmpBytes = xmpXml.toByteArray(Charsets.UTF_8)
                jpegFile.appendBytes(createXmpSegment(xmpBytes))
            }
        } catch (e: Exception) {
            Log.w("MediaProcessor", "Failed to inject XMP metadata", e)
        }
    }
    
    private fun createXmpSegment(xmpData: ByteArray): ByteArray {
        val marker = byteArrayOf(0xFF.toByte(), 0xE1.toByte())
        val length = (xmpData.size + 4).toShort()
        val lengthBytes = byteArrayOf(
            (length.toInt() shr 8 and 0xFF).toByte(),
            (length.toInt() and 0xFF).toByte()
        )
        return marker + lengthBytes + xmpData
    }
    
    private fun findMarkerPosition(data: ByteArray, marker: ByteArray): Int {
        for (i in 0 until data.size - marker.size) {
            if (data[i] == marker[0] && data[i + 1] == marker[1]) {
                return i
            }
        }
        return -1
    }
}
