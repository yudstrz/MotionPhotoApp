package com.example.motionphoto.ui.camera

import android.content.Intent
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.motionphoto.data.CameraManager
import com.example.motionphoto.data.SettingsManager
import com.example.motionphoto.utils.UpdateChecker
import com.example.motionphoto.utils.UpdateInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraManager: CameraManager,
    settingsManager: SettingsManager,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is CameraViewModel.CameraUIState.Ready -> {
            CameraContent(
                cameraManager = cameraManager,
                viewModel = viewModel,
                settingsManager = settingsManager,
                onSettingsClick = onSettingsClick,
                isCapturing = false
            )
        }
        is CameraViewModel.CameraUIState.Capturing -> {
            // Keep showing camera content, but in capturing state
            CameraContent(
                cameraManager = cameraManager,
                viewModel = viewModel,
                settingsManager = settingsManager,
                onSettingsClick = onSettingsClick,
                isCapturing = true
            )
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
    cameraManager: CameraManager,
    viewModel: CameraViewModel,
    settingsManager: SettingsManager,
    onSettingsClick: () -> Unit,
    isCapturing: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }

    val gridEnabled by settingsManager.gridFlow.collectAsState(initial = false)
    val timerSetting by settingsManager.timerFlow.collectAsState(initial = 0)
    val tapCaptureEnabled by settingsManager.tapCaptureFlow.collectAsState(initial = false)
    val useHdr by settingsManager.hdrFlow.collectAsState(initial = false)
    val scanQr by settingsManager.scanQrFlow.collectAsState(initial = false)
    val playSound by settingsManager.shutterSoundFlow.collectAsState(initial = true)
    val addWatermark by settingsManager.watermarkFlow.collectAsState(initial = false)
    
    val motionPhotoEnabled by settingsManager.motionPhotoFlow.collectAsState(initial = true)
    val aspectRatioMode by settingsManager.aspectRatioFlow.collectAsState(initial = 0)
    val lensFacing by settingsManager.lensFacingFlow.collectAsState(initial = 1)
    val flashMode by settingsManager.flashModeFlow.collectAsState(initial = 0)
    val highResEnabled by settingsManager.highResFlow.collectAsState(initial = false)
    
    // Timer state
    var countdown by remember { mutableStateOf(0) }

    val onCaptureAction: () -> Unit = {
        if (countdown == 0 && !isCapturing) {
            val capture = {
                viewModel.captureMotionPhoto(
                    useHdr = useHdr, 
                    addWatermark = addWatermark, 
                    playSound = playSound,
                    lensFacing = lensFacing,
                    aspectRatioMode = aspectRatioMode,
                    isHighRes = highResEnabled,
                    flashMode = flashMode,
                    isMotionPhotoEnabled = motionPhotoEnabled
                )
            }
            if (timerSetting > 0) {
                coroutineScope.launch {
                    countdown = timerSetting
                    while (countdown > 0) {
                        delay(1000)
                        countdown -= 1
                    }
                    capture()
                }
            } else {
                capture()
            }
        }
    }
    
    // QR Popup State
    var detectedQr by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(cameraManager, scanQr, useHdr) {
        cameraManager.onQrCodeDetected = { value ->
            if (scanQr && detectedQr == null) {
                detectedQr = value
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx)
            },
            update = { view ->
                cameraManager.startCameraPreview(
                    previewView = view,
                    useHdr = useHdr,
                    useQrScanner = scanQr,
                    lensFacing = lensFacing,
                    aspectRatioMode = aspectRatioMode,
                    isHighRes = highResEnabled
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tapCaptureEnabled) {
                    if (tapCaptureEnabled) {
                        detectTapGestures(
                            onTap = { onCaptureAction() }
                        )
                    }
                }
        )
        
        // Grid Overlay
        if (gridEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                // Vertical lines
                drawLine(start = Offset(canvasWidth / 3, 0f), end = Offset(canvasWidth / 3, canvasHeight), color = Color.White.copy(alpha = 0.5f), strokeWidth = 2f)
                drawLine(start = Offset(canvasWidth * 2 / 3, 0f), end = Offset(canvasWidth * 2 / 3, canvasHeight), color = Color.White.copy(alpha = 0.5f), strokeWidth = 2f)
                
                // Horizontal lines
                drawLine(start = Offset(0f, canvasHeight / 3), end = Offset(canvasWidth, canvasHeight / 3), color = Color.White.copy(alpha = 0.5f), strokeWidth = 2f)
                drawLine(start = Offset(0f, canvasHeight * 2 / 3), end = Offset(canvasWidth, canvasHeight * 2 / 3), color = Color.White.copy(alpha = 0.5f), strokeWidth = 2f)
            }
        }
        
        // Timer Overlay
        if (countdown > 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = countdown.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape).padding(32.dp)
                )
            }
        }
        
        // QR Code Popup
        if (detectedQr != null) {
            AlertDialog(
                onDismissRequest = { detectedQr = null },
                title = { Text("QR Code Detected") },
                text = { Text(detectedQr!!) },
                confirmButton = {
                    TextButton(onClick = { detectedQr = null }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Top Bar for Camera Controls & Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Toggle
            TextButton(onClick = { 
                coroutineScope.launch { settingsManager.setFlashMode((flashMode + 1) % 3) } 
            }) {
                Text(when (flashMode) {
                    1 -> "⚡ On"
                    2 -> "⚡ Off"
                    else -> "⚡ Auto"
                }, color = Color.White)
            }
            
            // 50MP (High Res) Toggle
            TextButton(onClick = { 
                coroutineScope.launch { settingsManager.setHighRes(!highResEnabled) }
            }) {
                Text(if (highResEnabled) "50M" else "STD", color = if (highResEnabled) Color.Yellow else Color.White)
            }
            
            // Motion Photo Toggle
            TextButton(onClick = { 
                coroutineScope.launch { settingsManager.setMotionPhoto(!motionPhotoEnabled) }
            }) {
                Text(if (motionPhotoEnabled) "Motion On" else "Motion Off", color = if (motionPhotoEnabled) Color.Yellow else Color.White)
            }
            
            // Aspect Ratio Toggle
            TextButton(onClick = { 
                coroutineScope.launch { settingsManager.setAspectRatio((aspectRatioMode + 1) % 2) } // Only 4:3 and 16:9 natively supported for now
            }) {
                Text(when(aspectRatioMode) {
                    1 -> "16:9"
                    else -> "4:3"
                }, color = Color.White)
            }
            
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }
        
        // Bottom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer for alignment
            Spacer(modifier = Modifier.size(48.dp))
            
            // Minimalist Shutter Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .border(width = 4.dp, color = Color.White, shape = CircleShape)
                    .clickable { onCaptureAction() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White, CircleShape)
                )
            }
            
            // Camera Flip Button
            IconButton(
                onClick = { 
                    coroutineScope.launch { settingsManager.setLensFacing(if (lensFacing == 1) 0 else 1) }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera, // A flip icon would be better, using generic camera icon for now
                    contentDescription = "Flip Camera",
                    tint = Color.White
                )
            }
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUpdateDialog = false 
                updateInfo = null
                updateMessage = ""
            },
            title = { Text(if (updateInfo != null) "Update Available" else "Update Status") },
            text = {
                if (updateInfo != null) {
                    Text("A new version (${updateInfo!!.latestVersion}) is available!\n\nRelease Notes:\n${updateInfo!!.releaseNotes}")
                } else {
                    Text(updateMessage)
                }
            },
            confirmButton = {
                if (updateInfo != null) {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.releaseUrl))
                        context.startActivity(intent)
                        showUpdateDialog = false
                    }) {
                        Text("Download")
                    }
                } else {
                    Button(onClick = { showUpdateDialog = false }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (updateInfo != null) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Later")
                    }
                }
            }
        )
    }
}

@Composable
fun CapturingProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EditingScreen(motionPhotoFile: File, onSaveClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editing Motion Photo: ${motionPhotoFile.name}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSaveClick) {
            Text("Save to Gallery")
        }
    }
}

@Composable
fun ErrorDialog(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error: $message", color = Color.Red)
    }
}
