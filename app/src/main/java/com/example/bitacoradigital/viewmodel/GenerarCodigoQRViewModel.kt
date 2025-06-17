package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class GenerarCodigoQRViewModel(private val prefs: SessionPreferences) : ViewModel() {
    val telefono = MutableStateFlow("")
    val caducidad = MutableStateFlow("")
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun enviarInvitacion() {
        viewModelScope.launch {
            _cargando.value = true
            _mensaje.value = null
            try {
                val id = withContext(Dispatchers.IO) {
                    prefs.userId.firstOrNull()
                } ?: throw Exception("ID de usuario no disponible")
                val cadDias = caducidad.value.toIntOrNull() ?: 0
                val json = JSONObject().apply {
                    put("id_invitante", id)
                    put("telefono_invitado", telefono.value)
                    put("cad", cadDias)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/enviar-invitacion-id")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                _mensaje.value = if (response.isSuccessful) {
                    "Invitación enviada"
                } else {
                    "Error ${response.code}"
                }
            } catch (e: Exception) {
                _mensaje.value = "Error al enviar invitación: ${e.message ?: e.toString()}"
            } finally {
                _cargando.value = false
            }
        }
    }
}

class GenerarCodigoQRViewModelFactory(private val prefs: SessionPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GenerarCodigoQRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GenerarCodigoQRViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
