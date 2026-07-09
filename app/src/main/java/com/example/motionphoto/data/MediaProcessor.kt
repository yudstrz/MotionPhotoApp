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
            
            // Step 1: Generate XMP metadata
            val videoSize = videoFile.length()
            val xmpHandler = XmpMetadataHandler()
            val xmpXml = xmpHandler.buildXmpXmlString(
                isMotionPhoto = true,
                videoOffsetBytes = videoSize,
                videoDurationMs = 3000
            )
            
            // Step 2: Read original photo bytes
            val originalPhotoBytes = photoFile.readBytes()
            
            // Step 3: Inject XMP into photo bytes safely
            val modifiedPhotoBytes = injectXmpMetadata(originalPhotoBytes, xmpXml)
            
            // Step 4: Write modified photo + video to output file
            outputFile.outputStream().use { outputStream ->
                outputStream.write(modifiedPhotoBytes)
                videoFile.inputStream().use { videoStream ->
                    videoStream.copyTo(outputStream)
                }
            }
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun injectXmpMetadata(photoBytes: ByteArray, xmpXml: String): ByteArray {
        try {
            val xmpBytes = xmpXml.toByteArray(Charsets.UTF_8)
            val xmpSegment = createXmpSegment(xmpBytes)
            
            // A valid JPEG starts with FF D8. We'll insert our APP1 segment right after it.
            // This is safer than trying to replace existing APP1 which might be EXIF,
            // or miscalculating the length of existing segments.
            
            if (photoBytes.size >= 2 && photoBytes[0] == 0xFF.toByte() && photoBytes[1] == 0xD8.toByte()) {
                val beforeInsert = photoBytes.sliceArray(0..1)
                val afterInsert = photoBytes.sliceArray(2 until photoBytes.size)
                return beforeInsert + xmpSegment + afterInsert
            } else {
                Log.w("MediaProcessor", "Not a valid JPEG (missing FF D8), appending XMP at the end (might not work).")
                return photoBytes + xmpSegment
            }
        } catch (e: Exception) {
            Log.w("MediaProcessor", "Failed to inject XMP metadata", e)
            return photoBytes // Return original if failed
        }
    }
    
    private fun createXmpSegment(xmpData: ByteArray): ByteArray {
        val marker = byteArrayOf(0xFF.toByte(), 0xE1.toByte())
        // Length includes the 2 bytes for the length itself
        val length = (xmpData.size + 2 + 29).toShort() // We also need to add standard XMP header
        
        // Standard XMP APP1 segment requires a specific namespace header
        val namespace = "http://ns.adobe.com/xap/1.0/\u0000".toByteArray(Charsets.UTF_8)
        
        val actualLength = (xmpData.size + namespace.size + 2).toShort()
        
        val lengthBytes = byteArrayOf(
            (actualLength.toInt() shr 8 and 0xFF).toByte(),
            (actualLength.toInt() and 0xFF).toByte()
        )
        return marker + lengthBytes + namespace + xmpData
    }
}
