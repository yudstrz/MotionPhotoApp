package com.example.motionphoto.utils

import com.example.motionphoto.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String
)

object UpdateChecker {
    private const val GITHUB_REPO = "yudstrz/MotionPhotoApp"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonObject = JSONObject(response.toString())
                val tagName = jsonObject.getString("tag_name")
                val htmlUrl = jsonObject.getString("html_url")
                val body = jsonObject.optString("body", "No release notes provided.")

                // Simple version comparison (assuming format like "1.0", "v1.0", etc.)
                val currentVersion = BuildConfig.VERSION_NAME
                val cleanTagName = tagName.replace("v", "")
                val cleanCurrentVersion = currentVersion.replace("v", "")

                val hasUpdate = cleanTagName != cleanCurrentVersion

                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        releaseUrl = htmlUrl,
                        releaseNotes = body
                    )
                )
            } else {
                Result.failure(Exception("HTTP Error: ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
