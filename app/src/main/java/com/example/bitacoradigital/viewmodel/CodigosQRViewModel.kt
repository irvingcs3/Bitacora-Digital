package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.CodigoQR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class CodigosQRViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _codigos = MutableStateFlow<List<CodigoQR>>(emptyList())
    val codigos: StateFlow<List<CodigoQR>> = _codigos.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarCodigos() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/invitaciones-detalle/?perimetro=$perimetroId")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val list = mutableListOf<CodigoQR>()
                        if (!jsonStr.isNullOrBlank()) {
                            val arr = JSONArray(jsonStr)
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                list.add(
                                    CodigoQR(
                                        id_invitacion = obj.optInt("id_invitacion"),
                                        nombre_invitado = obj.optString("nombre_invitado"),
                                        nombre_invitante = obj.optString("nombre_invitante"),
                                        destino = obj.optString("destino"),
                                        caducidad_dias = obj.optDouble("caducidad_dias"),
                                        estado = obj.optString("estado"),
                                        periodo_activo = obj.optString("periodo_activo")
                                    )
                                )
                            }
                        }
                        _codigos.value = list
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun borrarCodigo(id: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply { put("id_invitacion", id) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/borrar-qr/")
                    .post(body)
                    .addHeader("Authorization", "Bearer mfmssmcl")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarCodigos()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun modificarCaducidad(id: Int, dias: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply {
                    put("id_invitacion", id)
                    put("dias_extra", dias)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/modificar-cad/")
                    .post(body)
                    .addHeader("Authorization", "Bearer mfmssmcl")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarCodigos()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }
}

class CodigosQRViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodigosQRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodigosQRViewModel(prefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
