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
import java.util.concurrent.TimeUnit

class CodigosQRViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int,
    private val empresaId: Int
) : ViewModel() {

    private val _codigos = MutableStateFlow<List<CodigoQR>>(emptyList())
    val codigos: StateFlow<List<CodigoQR>> = _codigos.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val pageSize = 10
    private val _page = MutableStateFlow(1)
    val page: StateFlow<Int> = _page.asStateFlow()
    private val _pageCount = MutableStateFlow(1)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private var nextUrl: String? = null
    private var prevUrl: String? = null

    fun cargarCodigos(url: String? = null) {
        viewModelScope.launch {
            if (url == null) _page.value = 1
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val finalUrl = url ?: "https://bit.cs3.mx/api/v1/registro-visita/?perimetro_id=$perimetroId&page=${_page.value}&page_size=$pageSize"
                val request = Request.Builder()
                    .url(finalUrl)
                    .get()
                    .addHeader("x-session-token", token)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(0, TimeUnit.MILLISECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(0, TimeUnit.MILLISECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val list = mutableListOf<CodigoQR>()
                        if (!jsonStr.isNullOrBlank()) {
                            val obj = JSONObject(jsonStr)
                            nextUrl = obj.optString("next", null).takeIf { it != "null" }
                            prevUrl = obj.optString("previous", null).takeIf { it != "null" }
                            _pageCount.value = (obj.optInt("count") + pageSize - 1) / pageSize
                            val arr = obj.optJSONArray("results") ?: JSONArray()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val persona = item.optJSONObject("persona_data")
                                val perimetro = item.optJSONObject("perimetro_data")
                                val nombre = buildString {
                                    persona?.optString("nombre")?.let { append(it).append(' ') }
                                    persona?.optString("apellido_pat")?.let { append(it).append(' ') }
                                    persona?.optString("apellido_mat")?.let { append(it) }
                                }.trim()
                                list.add(
                                    CodigoQR(
                                        id_invitacion = item.optInt("registro_visita_id"),
                                        nombre_invitado = nombre,
                                        nombre_invitante = "",
                                        destino = perimetro?.optString("nombre") ?: "",
                                        caducidad_dias = 0.0,
                                        estado = "ACTIVO",
                                        periodo_activo = item.optString("fecha")
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

    fun cargarSiguiente() {
        nextUrl?.let {
            _page.value += 1
            cargarCodigos(it)
        }
    }

    fun cargarAnterior() {
        if (_page.value > 1) {
            prevUrl?.let {
                _page.value -= 1
                cargarCodigos(it)
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
    private val perimetroId: Int,
    private val empresaId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodigosQRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodigosQRViewModel(prefs, perimetroId, empresaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
