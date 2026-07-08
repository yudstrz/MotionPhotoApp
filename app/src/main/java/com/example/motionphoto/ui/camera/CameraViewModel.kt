package com.example.motionphoto.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motionphoto.data.MotionPhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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
                // Show success toast (not implemented yet)
            }.onFailure { error ->
                _uiState.value = CameraUIState.Error(error.message ?: "Save failed")
            }
        }
    }
}
