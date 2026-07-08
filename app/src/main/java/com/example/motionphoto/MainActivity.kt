package com.example.motionphoto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.motionphoto.data.CameraManager
import com.example.motionphoto.data.MediaProcessor
import com.example.motionphoto.data.MotionPhotoRepository
import com.example.motionphoto.data.SettingsManager
import com.example.motionphoto.theme.MotionPhotoTheme
import com.example.motionphoto.ui.camera.CameraScreen
import com.example.motionphoto.ui.camera.CameraViewModel
import com.example.motionphoto.ui.settings.SettingsScreen
import com.example.motionphoto.utils.FrameExtractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var settingsManager: SettingsManager

    private val isReady = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeCamera()
        } else {
            Toast.makeText(this, "Some permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsManager = SettingsManager(this)
        val mediaProcessor = MediaProcessor(this)
        val frameExtractor = FrameExtractor()
        cameraManager = CameraManager(this, this)
        val repository = MotionPhotoRepository(this, cameraManager, mediaProcessor, frameExtractor)
        cameraViewModel = CameraViewModel(repository)
        
        // Use a state to control which screen is shown
        val showSettings = mutableStateOf(false)

        setContent {
            MotionPhotoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    if (showSettings.value) {
                        SettingsScreen(
                            settingsManager = settingsManager,
                            onBack = { showSettings.value = false }
                        )
                    } else if (isReady.value) {
                        CameraScreen(
                            viewModel = cameraViewModel,
                            cameraManager = cameraManager,
                            settingsManager = settingsManager,
                            onSettingsClick = { showSettings.value = true }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (allPermissionsGranted()) {
            initializeCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun initializeCamera() {
        lifecycleScope.launch {
            cameraManager.initialize()
            isReady.value = true
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            lifecycleScope.launch {
                val volumeShutter = settingsManager.volumeShutterFlow.first()
                if (volumeShutter) {
                    val useHdr = settingsManager.hdrFlow.first()
                    val playSound = settingsManager.shutterSoundFlow.first()
                    val addWatermark = settingsManager.watermarkFlow.first()
                    cameraViewModel.captureMotionPhoto(useHdr = useHdr, addWatermark = addWatermark, playSound = playSound)
                }
            }
            return true 
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
