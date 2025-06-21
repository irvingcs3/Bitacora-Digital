package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Residente
import com.example.bitacoradigital.model.NodoHoja
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
import org.json.JSONArray
import org.json.JSONObject

class ResidentesViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int,
    private val empresaId: Int
) : ViewModel() {

    private val _residentes = MutableStateFlow<List<Residente>>(emptyList())
    val residentes: StateFlow<List<Residente>> = _residentes.asStateFlow()

    private val _nodosHoja = MutableStateFlow<List<NodoHoja>>(emptyList())
    val nodosHoja: StateFlow<List<NodoHoja>> = _nodosHoja.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    fun cargarResidentes() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/residentes")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val arr = JSONArray(jsonStr ?: "[]")
                        val list = mutableListOf<Residente>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Residente(
                                    id = obj.optInt("id"),
                                    name = obj.optString("name"),
                                    email = obj.optString("email"),
                                    registrationDate = obj.optString("registrationDate"),
                                    perimeterName = obj.optString("perimeterName"),
                                    perimeterId = obj.optInt("perimeterId")
                                )
                            )
                        }
                        _residentes.value = list
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

    fun cargarNodosHoja() {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perimetro/${perimetroId}/nodos-hoja")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val arr = JSONArray(jsonStr ?: "[]")
                        val list = mutableListOf<NodoHoja>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(NodoHoja(obj.optInt("id"), obj.optString("name")))
                        }
                        _nodosHoja.value = list
                    } else {
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            }
        }
    }

    fun eliminarResidente(usuarioId: Int, perimetroHojaId: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/usuario-perimetro/remove/3/${usuarioId}/${perimetroHojaId}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarResidentes()
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

    fun invitarResidente(correo: String, nodoHojaId: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val json = JSONObject().apply {
                    put("correo", correo)
                    put("id_empresa", empresaId)
                    put("id_perimetro", nodoHojaId)
                    put("id_rol", 3)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/enviar-invitacion/")
                    .post(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarResidentes()
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
