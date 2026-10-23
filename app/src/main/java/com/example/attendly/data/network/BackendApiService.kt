package com.example.attendly.data.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object BackendConfig {
    const val BASE_URL = "https://attendly-backend-qn6r.onrender.com/"
}

class BackendApiService {
    private val gson = Gson()
    private val TAG = "BackendApiService"

    suspend fun registerInstallation(installationId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BackendConfig.BASE_URL}api/registrations")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            val payload = mapOf(
                "installationId" to installationId,
                "name" to name
            )
            OutputStreamWriter(connection.outputStream).use { gson.toJson(payload, it) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(Unit)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Server error $responseCode"
                Log.e(TAG, "Registration failed with $responseCode: $errorText")
                Result.failure(Exception("Server returned status $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure during registration", e)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(installationId: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BackendConfig.BASE_URL}api/registrations/$installationId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            val payload = mapOf("name" to name)
            OutputStreamWriter(connection.outputStream).use { gson.toJson(payload, it) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Result.success(Unit)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Server error $responseCode"
                Log.e(TAG, "Update name failed with $responseCode: $errorText")
                Result.failure(Exception("Server returned status $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network failure during update display name", e)
            Result.failure(e)
        }
    }
}
