package com.example.motionphoto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.motionphoto.data.CameraManager
import com.example.motionphoto.data.MediaProcessor
import com.example.motionphoto.data.MotionPhotoRepository
import com.example.motionphoto.theme.MotionPhotoTheme
import com.example.motionphoto.ui.camera.CameraScreen
import com.example.motionphoto.ui.camera.CameraViewModel
import com.example.motionphoto.utils.FrameExtractor
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraViewModel: CameraViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mediaProcessor = MediaProcessor(this)
        val frameExtractor = FrameExtractor()
        cameraManager = CameraManager(this, this)
        val repository = MotionPhotoRepository(this, cameraManager, mediaProcessor, frameExtractor)
        cameraViewModel = CameraViewModel(repository)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun startCamera() {
        lifecycleScope.launch {
            cameraManager.initialize()
            setContent {
                MotionPhotoTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CameraScreen(
                            viewModel = cameraViewModel,
                            cameraManager = cameraManager
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
