package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val _uuid = MutableStateFlow<String?>(null)
    val uuid: StateFlow<String?> = _uuid.asStateFlow()

    private val client = OkHttpClient()

    init {
        viewModelScope.launch {
            prefs.uuidBoton.collect { value ->
                Log.d("DronGuard", "UUID collected from prefs: $value")
                _uuid.value = value
            }
        }
    }

    fun registrarBotonPanico() {
        viewModelScope.launch {
            try {
                val existing = prefs.uuidBoton.first()
                if (!existing.isNullOrBlank()) {
                    Log.d("DronGuard", "UUID ya almacenado: $existing")
                    return@launch
                }

                val bodyJson = JSONObject().apply {
                    put("nombre", "javier")
                    put("apellido_paterno", "fernandez")
                    put("apellido_materno", "ayala")
                    put("direccion", "av. revolucion 439")
                    put("calle", "av. revolucion 439")
                    put("colonia", "San Pedro de los Pinos")
                    put("municipio", "Benito Juarez")
                    put("estado", "CDMX")
                    put("cp", "03800")
                    put("celular", "5535033739")
                    put("correo", "fjfayala@gmail.com")
                    put("contacto_1", "franciso fernandez")
                    put("telefono_1", "5512345678")
                    put("contacto_2", "roverto ayala")
                    put("telefono_2", "5587654321")
                }

                val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(com.example.bitacoradigital.util.Constants.DRON_GUARD_REGISTRO)
                    .post(body)
                    .addHeader("X-Authorization", com.example.bitacoradigital.util.Constants.DRON_GUARD_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                Log.d("DronGuard", "Registro enviado")

                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    val uuid = JSONObject(respBody ?: "{}").optString("uuid_usuario")
                    if (uuid.isNotEmpty()) {
                        prefs.guardarUuidBoton(uuid)
                        Log.d("DronGuard", "Registro exitoso uuid=$uuid")
                    } else {
                        Log.d("DronGuard", "Registro sin uuid: $respBody")
                    }
                } else {
                    Log.e("DronGuard", "Fallo registro code=${'$'}{response.code} body=${'$'}respBody")
                }
                response.close()
            } catch (e: Exception) {
                Log.e("DronGuard", "Error registrando usuario", e)
            }
        }
    }

    fun enviarAlerta(lat: Double, lng: Double) {
        viewModelScope.launch {

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
                Log.d("DronGuard", "Response code: ${response.code} body: ${respBody}")
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
