package com.example.motionphoto.utils

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.xmp.XmpDirectory
import java.io.File

class XmpMetadataHandler {
    
    data class MotionPhotoMetadata(
        val isMotionPhoto: Boolean,
        val videoOffset: Long,
        val videoDuration: Long
    )
    
    fun readMotionPhotoMetadata(jpegFile: File): Result<MotionPhotoMetadata> {
        return try {
            val metadata = ImageMetadataReader.readMetadata(jpegFile)
            val xmpDir = metadata.getFirstDirectoryOfType(XmpDirectory::class.java)
            
            if (xmpDir != null) {
                // In a real implementation we would parse the XMP string property correctly.
                // The library com.drewnoakes:metadata-extractor returns raw values or maps
                // so we will mock reading the custom tags based on the standard properties format
                val properties = xmpDir.xmpProperties
                val isMotionPhoto = properties["GCamera:MotionPhoto"] == "1"
                val offset = properties["Item:Length"]?.toLongOrNull() ?: 0L
                val duration = properties["GCamera:MicroVideoDuration"]?.toLongOrNull() ?: 3000L
                
                Result.success(
                    MotionPhotoMetadata(
                        isMotionPhoto = isMotionPhoto,
                        videoOffset = offset,
                        videoDuration = duration
                    )
                )
            } else {
                Result.success(MotionPhotoMetadata(false, 0, 0))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun buildXmpXmlString(
        isMotionPhoto: Boolean,
        videoOffsetBytes: Long,
        videoDurationMs: Long
    ): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
                <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about=""
                        xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
                        xmlns:Item="http://ns.google.com/photos/1.0/container/">
                        <GCamera:MotionPhoto>${if (isMotionPhoto) 1 else 0}</GCamera:MotionPhoto>
                        <GCamera:MicroVideoDuration>$videoDurationMs</GCamera:MicroVideoDuration>
                        <Item:Length>$videoOffsetBytes</Item:Length>
                        <Item:Semantic>MotionPhoto</Item:Semantic>
                    </rdf:Description>
                </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()
    }
}
