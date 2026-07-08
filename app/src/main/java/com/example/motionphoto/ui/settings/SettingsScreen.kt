package com.example.motionphoto.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.motionphoto.data.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    val gridEnabled by settingsManager.gridFlow.collectAsState(initial = false)
    val timer by settingsManager.timerFlow.collectAsState(initial = 0)
    val shutterSound by settingsManager.shutterSoundFlow.collectAsState(initial = true)
    val volumeShutter by settingsManager.volumeShutterFlow.collectAsState(initial = false)
    val location by settingsManager.locationFlow.collectAsState(initial = false)
    val watermark by settingsManager.watermarkFlow.collectAsState(initial = false)
    val tapCapture by settingsManager.tapCaptureFlow.collectAsState(initial = false)
    val scanQr by settingsManager.scanQrFlow.collectAsState(initial = false)
    val hdr by settingsManager.hdrFlow.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { SettingsHeader("General Settings") }
            item { 
                SettingsSwitch("Shutter Sound", shutterSound) { 
                    coroutineScope.launch { settingsManager.setShutterSound(it) }
                } 
            }
            item { 
                SettingsSwitch("Grid", gridEnabled) { 
                    coroutineScope.launch { settingsManager.setGrid(it) }
                } 
            }
            item { 
                SettingsSwitch("Location (Geotagging)", location) { 
                    coroutineScope.launch { settingsManager.setLocation(it) }
                } 
            }
            item { 
                SettingsSwitch("Set Volume Buttons as Shutter", volumeShutter) { 
                    coroutineScope.launch { settingsManager.setVolumeShutter(it) }
                } 
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { SettingsHeader("Capture Settings") }
            item { 
                SettingsSwitch("HDR (Hardware Dependent)", hdr) { 
                    coroutineScope.launch { settingsManager.setHdr(it) }
                } 
            }
            item { 
                SettingsSwitch("Customized Watermark", watermark) { 
                    coroutineScope.launch { settingsManager.setWatermark(it) }
                } 
            }
            item { 
                // Timer dropdown simplified as a rotating option for this UI
                SettingsClickableItem("Timer", "Current: ${if (timer == 0) "Off" else "${timer}s"}") { 
                    coroutineScope.launch { 
                        val next = when (timer) {
                            0 -> 3
                            3 -> 5
                            5 -> 10
                            else -> 0
                        }
                        settingsManager.setTimer(next)
                    }
                } 
            }
            item { 
                SettingsSwitch("Shooting Method: Tap to Capture", tapCapture) { 
                    coroutineScope.launch { settingsManager.setTapCapture(it) }
                } 
            }
            item { 
                SettingsSwitch("Scan QR Codes", scanQr) { 
                    coroutineScope.launch { settingsManager.setScanQr(it) }
                } 
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color.White)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, color = Color.White)
        Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}
