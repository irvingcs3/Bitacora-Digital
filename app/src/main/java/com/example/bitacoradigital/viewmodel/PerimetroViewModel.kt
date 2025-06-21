package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.JerarquiaNodo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

class PerimetroViewModel(
    private val apiService: ApiService,
    private val sessionPrefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _jerarquia = MutableStateFlow<JerarquiaNodo?>(null)
    val jerarquia: StateFlow<JerarquiaNodo?> = _jerarquia.asStateFlow()

    private val _ruta = MutableStateFlow<List<JerarquiaNodo>>(emptyList())
    val ruta: StateFlow<List<JerarquiaNodo>> = _ruta.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun cargarJerarquia() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: throw Exception("Token vacío")
                val response = withContext(Dispatchers.IO) {
                    apiService.getJerarquiaPorNivel(perimetroId, token)
                }
                _jerarquia.value = response
                _ruta.value = listOf(response)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun navegarHacia(nodo: JerarquiaNodo) {
        _ruta.update { it + nodo }
    }

    fun retroceder() {
        _ruta.update { if (it.isNotEmpty()) it.dropLast(1) else it }
    }

    fun crearHijo(nombre: String) {
        val actual = _ruta.value.lastOrNull() ?: return
        crear(nombre, actual.nivel + 1, actual.perimetro_id)
    }

    private fun crear(nombre: String, nivel: Int, padreId: Int?) {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: return@launch
                val json = JSONObject().apply {
                    put("nombre", nombre)
                    put("nivel", nivel)
                    if (padreId != null) put("padre", padreId)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/")
                    .post(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarJerarquia()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun eliminarPerimetro(id: Int) {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/${id}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarJerarquia()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun editarPerimetro(id: Int, nombre: String) {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    sessionPrefs.sessionToken.first()
                } ?: return@launch
                val json = JSONObject().apply { put("nombre", nombre) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/${id}/")
                    .put(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarJerarquia()
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun crearSubzona(nombre: String, padre: JerarquiaNodo) {
        crear(nombre, padre.nivel + 1, padre.perimetro_id)
    }
}
