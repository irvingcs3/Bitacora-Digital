package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.CheckpointSimple
import com.example.bitacoradigital.model.HistorialQR
import com.example.bitacoradigital.model.SeguimientoInfo
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

class SeguimientoQRViewModel(
    private val prefs: SessionPreferences,
    private val idInvitacion: Int
) : ViewModel() {

    private val _info = MutableStateFlow<SeguimientoInfo?>(null)
    val info: StateFlow<SeguimientoInfo?> = _info.asStateFlow()

    private val _historial = MutableStateFlow<List<HistorialQR>>(emptyList())
    val historial: StateFlow<List<HistorialQR>> = _historial.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarTodo() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                cargarInfo(token)
                cargarHistorial(token)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    private suspend fun cargarInfo(token: String) {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/siguiente-checkpoint/?id_invitacion=${idInvitacion}")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val client = OkHttpClient()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        response.use { resp ->
            if (resp.isSuccessful) {
                val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                val obj = JSONObject(jsonStr ?: "{}")
                val siguienteList = mutableListOf<CheckpointSimple>()
                val arr = obj.optJSONArray("siguiente") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    siguienteList.add(CheckpointSimple(item.optInt("checkpoint_id"), item.optString("nombre")))
                }
                _info.value = SeguimientoInfo(
                    fase = obj.optString("fase"),
                    checkpointActualId = obj.optJSONObject("checkpoint_actual")?.optInt("id"),
                    checkpointActualNombre = obj.optJSONObject("checkpoint_actual")?.optString("nombre"),
                    checkpointActualPerimetro = obj.optString("checkpoint_actual_perimetro"),
                    siguientePerimetro = if (obj.isNull("siguiente_perimetro")) null else obj.optString("siguiente_perimetro"),
                    siguiente = siguienteList,
                    mensaje = obj.optString("mensaje", null)
                )
            } else {
                _error.value = "Error ${resp.code}"
            }
        }
    }

    private suspend fun cargarHistorial(token: String) {
        val request = Request.Builder()
            .url("https://bit.cs3.mx/api/v1/checkpoints/seguimiento-qr/?id_invitacion=${idInvitacion}")
            .get()
            .addHeader("x-session-token", token)
            .build()
        val client = OkHttpClient()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        response.use { resp ->
            if (resp.isSuccessful) {
                val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                val arr = JSONArray(jsonStr ?: "[]")
                val list = mutableListOf<HistorialQR>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        HistorialQR(
                            fecha = obj.optString("fecha"),
                            checkpoint = obj.optString("checkpoint"),
                            perimetro = obj.optString("perimetro")
                        )
                    )
                }
                _historial.value = list
            } else {
                _error.value = "Error ${resp.code}"
            }
        }
    }

    fun modificarCaducidad(dias: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply {
                    put("id_invitacion", idInvitacion)
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
                response.close()
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun borrarCodigo() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply { put("id_invitacion", idInvitacion) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/borrar-qr/")
                    .post(body)
                    .addHeader("Authorization", "Bearer mfmssmcl")
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.close()
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }
}

class SeguimientoQRViewModelFactory(
    private val prefs: SessionPreferences,
    private val idInvitacion: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeguimientoQRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeguimientoQRViewModel(prefs, idInvitacion) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
