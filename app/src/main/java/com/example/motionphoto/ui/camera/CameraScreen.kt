package com.example.motionphoto.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import com.example.motionphoto.data.CameraManager
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    cameraManager.startCameraPreview(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
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
