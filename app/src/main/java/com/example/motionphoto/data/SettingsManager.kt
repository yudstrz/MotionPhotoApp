package com.example.motionphoto.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "camera_settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_GRID = booleanPreferencesKey("key_grid")
        val KEY_TIMER = intPreferencesKey("key_timer") // 0, 3, 5, 10
        val KEY_SHUTTER_SOUND = booleanPreferencesKey("key_shutter_sound")
        val KEY_VOLUME_SHUTTER = booleanPreferencesKey("key_volume_shutter")
        val KEY_LOCATION = booleanPreferencesKey("key_location")
        val KEY_WATERMARK = booleanPreferencesKey("key_watermark")
        val KEY_TAP_CAPTURE = booleanPreferencesKey("key_tap_capture")
        val KEY_SCAN_QR = booleanPreferencesKey("key_scan_qr")
        val KEY_HDR = booleanPreferencesKey("key_hdr")
    }

    val gridFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_GRID] ?: false }
    val timerFlow: Flow<Int> = context.dataStore.data.map { it[KEY_TIMER] ?: 0 }
    val shutterSoundFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHUTTER_SOUND] ?: true }
    val volumeShutterFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_VOLUME_SHUTTER] ?: false }
    val locationFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_LOCATION] ?: false }
    val watermarkFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_WATERMARK] ?: false }
    val tapCaptureFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_TAP_CAPTURE] ?: false }
    val scanQrFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SCAN_QR] ?: false }
    val hdrFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HDR] ?: false }

    suspend fun setGrid(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GRID] = enabled }
    }
    
    suspend fun setTimer(seconds: Int) {
        context.dataStore.edit { it[KEY_TIMER] = seconds }
    }
    
    suspend fun setShutterSound(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHUTTER_SOUND] = enabled }
    }
    
    suspend fun setVolumeShutter(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOLUME_SHUTTER] = enabled }
    }
    
    suspend fun setLocation(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOCATION] = enabled }
    }
    
    suspend fun setWatermark(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WATERMARK] = enabled }
    }
    
    suspend fun setTapCapture(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TAP_CAPTURE] = enabled }
    }
    
    suspend fun setScanQr(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SCAN_QR] = enabled }
    }
    
    suspend fun setHdr(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HDR] = enabled }
    }
}
