package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Checkpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CheckpointsViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _checkpoints = MutableStateFlow<List<Checkpoint>>(emptyList())
    val checkpoints: StateFlow<List<Checkpoint>> = _checkpoints.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun cargarCheckpoints() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/checkpoints/?perimetro_id=$perimetroId")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    val jsonArr = org.json.JSONArray(response.body?.string() ?: "[]")
                    val list = mutableListOf<Checkpoint>()
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        list.add(
                            Checkpoint(
                                checkpoint_id = obj.optInt("checkpoint_id"),
                                nombre = obj.optString("nombre"),
                                tipo = obj.optString("tipo"),
                                perimetro = obj.optInt("perimetro")
                            )
                        )
                    }
                    _checkpoints.value = list
                } else {
                    _error.value = "Error ${response.code}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun crearCheckpoint(nombre: String, tipo: String) {
        viewModelScope.launch {
            modificar(null, nombre, tipo)
        }
    }

    fun actualizarCheckpoint(id: Int, nombre: String, tipo: String) {
        viewModelScope.launch {
            modificar(id, nombre, tipo)
        }
    }

    fun eliminarCheckpoint(id: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/checkpoints/${id}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    cargarCheckpoints()
                } else {
                    _error.value = "Error ${response.code}"
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    private suspend fun modificar(id: Int?, nombre: String, tipo: String) {
        _cargando.value = true
        _error.value = null
        try {
            val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return
            val json = JSONObject().apply {
                put("nombre", nombre)
                put("tipo", tipo)
                put("perimetro", perimetroId)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val builder = Request.Builder()
                .addHeader("x-session-token", token)
                .addHeader("Content-Type", "application/json")
            val request = if (id == null) {
                builder.url("https://bit.cs3.mx/api/v1/checkpoints/").post(body).build()
            } else {
                builder.url("https://bit.cs3.mx/api/v1/checkpoints/${id}/").put(body).build()
            }
            val client = OkHttpClient()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful) {
                cargarCheckpoints()
            } else {
                _error.value = "Error ${response.code}"
            }
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        } finally {
            _cargando.value = false
        }
    }
}
