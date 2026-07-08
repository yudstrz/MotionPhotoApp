# Motion Photo App: Practical Implementation Roadmap
## Month-by-Month Execution Plan + Production Code Patterns

**Target Delivery**: 4 months (Native Kotlin) | 8 weeks (Flutter MVP)  
**Team Size**: 2-3 engineers  
**Platform**: Android (minimum API 21)

---

## 📊 Project Timeline

### Native Kotlin (Production) - 4 Months

```
┌────────────────────────────────────────────────────────────────────┐
│ Month 1: Camera Capture & Preview                     (Weeks 1-4)  │
├────────────────────────────────────────────────────────────────────┤
│ Week 1-2: CameraX setup, permissions, lifecycle                   │
│ Week 3-4: Simultaneous photo+video via SessionConfig              │
│ Deliverable: Working camera preview + test captures               │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Month 2: Video Processing & Frame Extraction          (Weeks 5-8)  │
├────────────────────────────────────────────────────────────────────┤
│ Week 5: MediaMetadataRetriever integration                        │
│ Week 6-7: Frame extraction UI (slider scrubber)                   │
│ Week 8: Re-encode selected frame to JPEG                          │
│ Deliverable: Key Photo editor (pick any frame as main photo)      │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Month 3: Metadata & File Muxing                       (Weeks 9-12) │
├────────────────────────────────────────────────────────────────────┤
│ Week 9: XMP metadata handling (reading/writing)                   │
│ Week 10: Photo + Video muxing (append bytes + metadata)           │
│ Week 11: Google Photos compatibility testing                      │
│ Week 12: Edge cases (scoped storage, low disk space)              │
│ Deliverable: Complete Motion Photo files                          │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Month 4: Polish, Testing & Release                   (Weeks 13-16) │
├────────────────────────────────────────────────────────────────────┤
│ Week 13: UI/UX refinement, settings                               │
│ Week 14: Device testing (Pixel 3/5/6/7, Samsung S22/S23)         │
│ Week 15: Performance optimization, battery profiling              │
│ Week 16: Beta release, crash reporting, analytics                │
│ Deliverable: v1.0 on Google Play Store                           │
└────────────────────────────────────────────────────────────────────┘
```

### Flutter MVP - 8 Weeks

```
┌────────────────────────────────────────────────────────────────────┐
│ Week 1-2: Setup + Camera UI                                       │
├────────────────────────────────────────────────────────────────────┤
│ Dart side: camera plugin integration, preview display             │
│ Kotlin side: Platform channel scaffolding                         │
│ Deliverable: Functional camera screen                             │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Week 3-4: Capture + Video Editing                                 │
├────────────────────────────────────────────────────────────────────┤
│ Kotlin: CameraX simultaneous capture (Photo + Video)              │
│ Dart: Video player + frame selector UI                            │
│ Deliverable: Can select frame from captured video                 │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Week 5-6: Metadata + Muxing                                       │
├────────────────────────────────────────────────────────────────────┤
│ Kotlin: XMP write + file muxing via platform channel              │
│ Dart: Progress UI + error handling                                │
│ Deliverable: Can save Motion Photo file                           │
└────────────────────────────────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────────────────────────────────┐
│ Week 7-8: Testing + Release                                       │
├────────────────────────────────────────────────────────────────────┤
│ Device testing, Google Photos integration check                   │
│ Beta on TestFlight / Firebase App Distribution                    │
│ Deliverable: Closed beta v0.1                                     │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🏗️ Architecture Diagram

### High-Level Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER INTERACTION                          │
│  (Camera Preview → Capture → Edit → Save)                       │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┬─────────────────┐
        │                                      │                 │
     (KOTLIN/NATIVE)                  (KOTLIN/NATIVE)      (DART/FLUTTER)
        │                                      │                 │
    ┌───▼──────────────────┐      ┌───────────▼───────────┐   ┌──▼────────┐
    │ CameraX API          │      │ Media Processing      │   │ UI Layer  │
    │                      │      │                       │   │           │
    │ • SessionConfig      │◄────►│ • MediaMetadata       │   │ • Preview │
    │ • ImageCapture       │      │   Retriever           │   │ • Sliders │
    │ • VideoCapture       │      │ • MediaCodec          │   │ • Gallery │
    └──────┬───────────────┘      │ • Commons-Imaging     │   └─────┬─────┘
           │                       │   (XMP write)        │         │
           │ File system           │ • MediaMuxer (if     │         │
           │ (JPEG + MP4)          │   needed)            │         │
           │                       └───────┬──────────────┘         │
           │                               │                       │
        ┌──▼───────────────────────────────▼──────────┐            │
        │                                              │            │
        │     MOTION PHOTO FILE                       │            │
        │  (JPEG + Embedded MP4 + XMP Metadata)      │            │
        │                                              │            │
        │  ┌─────────────────────────────────┐         │            │
        │  │ JPEG Photo (readable part)      │         │            │
        │  ├─────────────────────────────────┤         │            │
        │  │ MP4 Video (appended, hidden)    │         │            │
        │  ├─────────────────────────────────┤         │            │
        │  │ XMP Metadata:                   │         │            │
        │  │  • GCamera:MotionPhoto = 1      │         │            │
        │  │  • Item:Length = offset_bytes   │         │            │
        │  └─────────────────────────────────┘         │            │
        │                                              │            │
        └──────────────────┬───────────────────────────┘            │
                           │                                        │
                    Save to Device Storage                          │
                    (MediaStore API)                                │
                           │                                        │
                      ┌─────▼──────────────────────┐               │
                      │  Google Photos / Gallery   │               │
                      │  (Recognizes Motion Photo) │               │
                      └────────────────────────────┘               │
```

---

## 🔧 Project Structure (Kotlin)

```
motion-photo-app/
│
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/example/motionphoto/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── camera/
│   │   │   │   │   │   ├── CameraScreen.kt
│   │   │   │   │   │   └── CameraViewModel.kt
│   │   │   │   │   └── editor/
│   │   │   │   │       ├── KeyPhotoEditor.kt
│   │   │   │   │       └── EditorViewModel.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── MotionPhotoRepository.kt
│   │   │   │   │   ├── CameraManager.kt (CameraX wrapper)
│   │   │   │   │   └── MediaProcessor.kt (Muxing logic)
│   │   │   │   ├── models/
│   │   │   │   │   ├── MotionPhoto.kt
│   │   │   │   │   └── CaptureConfig.kt
│   │   │   │   └── utils/
│   │   │   │       ├── FrameExtractor.kt
│   │   │   │       ├── XmpMetadataHandler.kt
│   │   │   │       └── FileUtils.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/ (Unit tests)
│   │   └── androidTest/ (Instrumentation tests)
│   │
│   └── build.gradle.kts
│
├── .github/
│   └── workflows/
│       ├── ci.yml (Unit tests on every push)
│       ├── device-tests.yml (Emulator tests)
│       └── release.yml (Build & sign APK)
│
└── README.md
```

---

## 💻 Key Implementation Files

### 1. CameraManager.kt (Wrapper for CameraX)

```kotlin
// File: data/CameraManager.kt

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraController: CameraController? = null
    
    suspend fun initialize() {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()
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
            val photoFile = createTempFile("photo", ".jpg")
            val videoFile = createTempFile("video", ".mp4")
            
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
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            
            // Capture photo
            val photoTask = suspendCancellableCoroutine<Boolean> { cont ->
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
            
            // Capture video (start when photo is taken)
            val videoTask = suspendCancellableCoroutine<Boolean> { cont ->
                val recording = recorder.prepareRecording(context, videoFile)
                    .start(Executors.newSingleThreadExecutor()) { recordingStats ->
                        if (recordingStats.recordingStopReason != null) {
                            cont.resume(true)
                        }
                    }
                
                // Stop after 3 seconds
                Handler(Looper.getMainLooper()).postDelayed(
                    { recording.stop() },
                    videoDurationMs.toLong()
                )
            }
            
            awaitAll(photoTask, videoTask)
            
            Result.success(Pair(photoFile, videoFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun release() {
        // Unbind camera
    }
}
```

---

### 2. FrameExtractor.kt (Frame Extraction)

```kotlin
// File: utils/FrameExtractor.kt

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
```

---

### 3. XmpMetadataHandler.kt (Metadata Management)

```kotlin
// File: utils/XmpMetadataHandler.kt

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
                val isMotionPhoto = xmpDir.getString("GCamera:MotionPhoto") == "1"
                val offset = xmpDir.getString("Item:Length")?.toLongOrNull() ?: 0L
                val duration = xmpDir.getString("GCamera:MicroVideoDuration")?.toLongOrNull() ?: 3000L
                
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
```

---

### 4. MediaProcessor.kt (Muxing Logic)

```kotlin
// File: data/MediaProcessor.kt

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
```

---

### 5. MotionPhotoRepository.kt (Data Layer)

```kotlin
// File: data/MotionPhotoRepository.kt

class MotionPhotoRepository(
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
            
            // Extract embedded video for re-muxing
            val metadata = XmpMetadataHandler().readMotionPhotoMetadata(motionPhotoFile)
                .getOrThrow()
            
            // TODO: Extract video from motionPhotoFile
            // For now, re-use existing approach
            
            Result.success(frameJpeg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveToGallery(motionPhotoFile: File): Result<Uri> {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            }
            
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: throw Exception("Failed to create media store entry")
            
            contentResolver.openOutputStream(uri)?.use { output ->
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
```

---

### 6. CameraViewModel.kt (MVVM Layer)

```kotlin
// File: ui/camera/CameraViewModel.kt

class CameraViewModel(
    private val repository: MotionPhotoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CameraUIState>(CameraUIState.Ready)
    val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()
    
    private val _captureProgress = MutableStateFlow(0f)
    val captureProgress: StateFlow<Float> = _captureProgress.asStateFlow()
    
    sealed class CameraUIState {
        object Ready : CameraUIState()
        object Capturing : CameraUIState()
        data class Editing(val motionPhotoFile: File) : CameraUIState()
        data class Error(val message: String) : CameraUIState()
    }
    
    fun captureMotionPhoto() {
        viewModelScope.launch {
            _uiState.value = CameraUIState.Capturing
            _captureProgress.value = 0.33f
            
            val result = repository.captureAndCreateMotionPhoto()
            
            _captureProgress.value = 1f
            
            result.onSuccess { capture ->
                _uiState.value = CameraUIState.Editing(capture.motionPhotoFile)
            }.onFailure { error ->
                _uiState.value = CameraUIState.Error(error.message ?: "Unknown error")
            }
        }
    }
    
    fun saveToGallery(motionPhotoFile: File) {
        viewModelScope.launch {
            val result = repository.saveToGallery(motionPhotoFile)
            
            result.onSuccess { uri ->
                _uiState.value = CameraUIState.Ready
                // Show success toast
            }.onFailure { error ->
                _uiState.value = CameraUIState.Error(error.message ?: "Save failed")
            }
        }
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests (Test Frame Extraction)

```kotlin
// File: src/test/kotlin/com/example/motionphoto/FrameExtractorTest.kt

class FrameExtractorTest {
    
    private lateinit var extractor: FrameExtractor
    private lateinit var testVideoPath: String
    
    @Before
    fun setUp() {
        extractor = FrameExtractor()
        // Copy test video from assets
        testVideoPath = copyTestResourceToTemp("test_video.mp4")
    }
    
    @Test
    fun testExtractFrameReturnsValidBitmap() {
        val result = extractor.extractFrameAtTime(testVideoPath, 1_000_000) // 1 second
        
        assertTrue(result.isSuccess)
        val bitmap = result.getOrNull()
        assertNotNull(bitmap)
        assertTrue(bitmap!!.width > 0)
        assertTrue(bitmap.height > 0)
    }
    
    @Test
    fun testMultipleFramesHaveDifferentContent() {
        val frame1 = extractor.extractFrameAtTime(testVideoPath, 500_000)
        val frame2 = extractor.extractFrameAtTime(testVideoPath, 1_500_000)
        
        assertTrue(frame1.isSuccess)
        assertTrue(frame2.isSuccess)
        
        // Frames should have different pixel content
        assertFalse(bitmapsAreEqual(frame1.getOrNull()!!, frame2.getOrNull()!!))
    }
}
```

### Instrumentation Tests (Device/Emulator)

```kotlin
// File: src/androidTest/kotlin/com/example/motionphoto/MotionPhotoCreationTest.kt

@RunWith(AndroidJUnit4::class)
class MotionPhotoCreationTest {
    
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = 
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testMotionPhotoFileContainsJpegAndMp4() {
        // Simulate capture flow
        onView(withId(R.id.capture_button)).perform(click())
        
        // Wait for capture to complete
        Thread.sleep(5000)
        
        // Verify file is created
        val motionPhotoFile = getLastCreatedFile()
        assertNotNull(motionPhotoFile)
        assertTrue(motionPhotoFile!!.exists())
        
        // Verify file structure (JPEG + MP4)
        val bytes = motionPhotoFile.readBytes()
        assertTrue(bytes.startsWith(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))) // JPEG magic
        
        // Verify contains MP4 signature somewhere (0x66747970 = "ftyp")
        val mp4Signature = byteArrayOf(0x66, 0x74, 0x79, 0x70)
        assertTrue(bytes.contains(mp4Signature))
    }
}
```

---

## 📋 Dependency Declaration

### build.gradle.kts (Complete)

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.example.motionphoto"
    compileSdk = 35 // Android 15
    
    defaultConfig {
        applicationId = "com.example.motionphoto"
        minSdk = 21 // CameraX minimum
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    
    // Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    
    // Camera (Latest CameraX)
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-video:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.camera:camera-extensions:1.5.0")
    
    // Media
    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-transformer:1.4.0")
    
    // Metadata
    implementation("com.drewnoakes:metadata-extractor:2.18.0")
    implementation("org.apache.commons:commons-imaging:1.0-alpha1")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    
    // Storage
    implementation("androidx.activity:activity-ktx:1.9.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
```

---

## 📱 UI/UX Mockup (Jetpack Compose)

```kotlin
// File: ui/camera/CameraScreen.kt

@Composable
fun CameraScreen(viewModel: CameraViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )
    
    when (uiState) {
        is CameraViewModel.CameraUIState.Ready -> {
            CameraContent(
                onCaptureClick = { viewModel.captureMotionPhoto() },
                onPermissionDenied = { /* Show permission UI */ }
            )
        }
        is CameraViewModel.CameraUIState.Capturing -> {
            CapturingProgress()
        }
        is CameraViewModel.CameraUIState.Editing -> {
            val motionPhotoFile = (uiState as CameraViewModel.CameraUIState.Editing).motionPhotoFile
            EditingScreen(
                motionPhotoFile = motionPhotoFile,
                onSaveClick = { viewModel.saveToGallery(motionPhotoFile) }
            )
        }
        is CameraViewModel.CameraUIState.Error -> {
            ErrorDialog(
                message = (uiState as CameraViewModel.CameraUIState.Error).message
            )
        }
    }
}

@Composable
fun CameraContent(
    onCaptureClick: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview would go here
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    // Bind CameraX preview
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Capture button at bottom
        Button(
            onClick = onCaptureClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Capture",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
```

---

## 🔍 Testing Checklist

### Pre-Release Testing Matrix

| Feature | Pixel 3 | Pixel 5 | Galaxy S22 | Notes |
|---------|---------|---------|-----------|-------|
| Photo capture | ✓ | ✓ | ✓ | Portrait/Landscape |
| 3-sec video | ✓ | ✓ | ✓ | Check framerate |
| Frame extraction | ✓ | ✓ | ✓ | Sync frame quality |
| Metadata write | ✓ | ✓ | ⚠️ | Samsung variant? |
| Google Photos recognition | ✓ | ✓ | ✓ | Check playback |
| Samsung Gallery | ~ | ✓ | ✓ | Different metadata |
| Low disk space | ✓ | ✓ | ✓ | <100MB available |
| Battery drain | ✓ | ✓ | ✓ | 30min continuous use |

---

## 🚀 Release Checklist

- [ ] Code review (all PRs require 2 approvals)
- [ ] Unit test coverage > 80%
- [ ] All device tests passing
- [ ] Performance profiling (APK < 15 MB, RAM < 200 MB)
- [ ] Battery benchmark (< 10% drain/hour at 60fps)
- [ ] Privacy policy updated
- [ ] Google Photos integration tested
- [ ] Release notes written
- [ ] Beta testing (48 hours minimum)
- [ ] Crash reporting configured (Firebase Crashlytics)
- [ ] Analytics enabled (Firebase Analytics)
- [ ] App signing certificate generated
- [ ] Play Store listing prepared
- [ ] Screenshots captured (3 languages minimum)

---

## 📊 Success Metrics (Target)

| Metric | Target | How to Measure |
|--------|--------|-----------------|
| **Capture Time** | < 500ms | Analytics event timing |
| **Frame Extraction** | < 100ms | MediaMetadataRetriever profiling |
| **Crash Rate** | < 0.05% | Firebase Crashlytics |
| **ANR Rate** | < 0.01% | Android Vitals |
| **Memory Leak Free** | 100% | Android Profiler tests |
| **Battery Impact** | < 2% per hour | Device profiling |
| **Google Photos Compat** | 100% | Manual testing all formats |

---

## 📚 Required Reading

1. **CameraX Migration Guide** (official Google docs)
2. **Motion Photo Format 1.0 Specification** (Android Media)
3. **Scoped Storage Migration** (Android 11+)
4. **Kotlin Coroutines Patterns** (Medium article series)
5. **MediaMetadataRetriever Performance Tips** (Stack Overflow collection)

---

**Document Version**: 1.0  
**Last Updated**: July 8, 2026  
**Target Release**: October 2026 (Native) | August 2026 (Flutter MVP)

