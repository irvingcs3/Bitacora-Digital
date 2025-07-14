package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DronGuardViewModel(private val prefs: SessionPreferences) : ViewModel() {

    val uuid: StateFlow<String?> = prefs.uuidBoton.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, null)

    private val client = OkHttpClient()

    fun enviarAlerta(lat: Double, lng: Double) {
        viewModelScope.launch {
            val id = uuid.value ?: return@launch
            try {
                val bodyJson = JSONObject().apply {
                    put("uuid_usuario", id)
                    put("lat", lat.toString())
                    put("lng", lng.toString())
                }
                Log.d("DronGuard", "Enviando alerta uuid=$id lat=$lat lng=$lng")

                val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(com.example.bitacoradigital.util.Constants.DRON_GUARD_SEND)
                    .post(body)
                    .addHeader("X-Authorization", com.example.bitacoradigital.util.Constants.DRON_GUARD_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val respBody = response.body?.string()
                Log.d("DronGuard", "Response code: ${'$'}{response.code} body: ${'$'}respBody")
                response.close()
            } catch (e: Exception) {
                Log.e("DronGuard", "Error enviando alerta", e)
            }
        }
    }

    class Factory(private val prefs: SessionPreferences) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DronGuardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DronGuardViewModel(prefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
