package com.example.motionphoto.ui.camera

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import com.example.motionphoto.data.CameraManager
import com.example.motionphoto.utils.UpdateChecker
import com.example.motionphoto.utils.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    cameraManager: CameraManager
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (uiState) {
        is CameraViewModel.CameraUIState.Ready -> {
            CameraContent(
                cameraManager = cameraManager,
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
    cameraManager: CameraManager,
    onCaptureClick: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    cameraManager.startCameraPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Bar for Update Checker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isCheckingUpdate = true
                        val result = UpdateChecker.checkForUpdates()
                        isCheckingUpdate = false
                        if (result.isSuccess) {
                            val info = result.getOrNull()
                            if (info != null && info.hasUpdate) {
                                updateInfo = info
                                showUpdateDialog = true
                            } else {
                                updateMessage = "You are on the latest version."
                                showUpdateDialog = true
                            }
                        } else {
                            updateMessage = "Failed to check for updates."
                            showUpdateDialog = true
                        }
                    }
                }
            ) {
                if (isCheckingUpdate) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Check for Updates",
                        tint = Color.White
                    )
                }
            }
        }
        
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

