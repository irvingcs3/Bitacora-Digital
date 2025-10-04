package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class DronGuardViewModel(private val prefs: SessionPreferences) : ViewModel() {

    companion object {
        private const val DEFAULT_UUID = "a1b9c0f1-65e3-4e32-b439-9b5df53172f1"
        private const val PROJECT_ID = 2
    }

    private val _uuid = MutableStateFlow<String?>(DEFAULT_UUID)
    val uuid: StateFlow<String?> = _uuid.asStateFlow()

    private val _direccionEvento = MutableStateFlow<String?>(null)
    val direccionEvento: StateFlow<String?> = _direccionEvento.asStateFlow()

    private val client = OkHttpClient()

    init {
        viewModelScope.launch {
            prefs.uuidBoton.collect { value ->
                Log.d("DronGuard", "UUID collected from prefs: $value")
                _uuid.value = value ?: DEFAULT_UUID
            }
        }
    }

    fun clearDireccionEvento() {
        _direccionEvento.value = null
    }

    fun enviarAlerta(lat: Double, lng: Double) {
        viewModelScope.launch {

            _direccionEvento.value = null

            val id = uuid.value ?: run {
                Log.e("DronGuard", "UUID nulo, no se puede enviar alerta")
                return@launch
            }
            Log.d("DronGuard", "UUID listo para alerta: $id")

            try {
                val bodyJson = JSONObject().apply {
                    put("uuid_usuario", id)
                    put("lat", lat.toString())
                    put("lng", lng.toString())
                    put("project", PROJECT_ID)
                }
                Log.d("DronGuard", "Enviando alerta uuid=$id lat=$lat lng=$lng project=$PROJECT_ID")

                val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(com.example.bitacoradigital.util.Constants.DRON_GUARD_SEND)
                    .post(body)
                    .addHeader("X-Authorization", com.example.bitacoradigital.util.Constants.DRON_GUARD_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val respBody = response.body?.string()
                Log.d("DronGuard", "Response code: ${response.code} body: ${respBody}")
                if (response.isSuccessful) {
                    try {
                        val json = JSONObject(respBody ?: "{}")
                        _direccionEvento.value = json.optString("direccion_evento")
                    } catch (e: Exception) {
                        Log.e("DronGuard", "Error parsing response", e)
                    }
                }
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
