package com.example.bitacoradigital.viewmodel

import android.util.Log
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
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class CodigosQRViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int,
    private val empresaId: Int
) : ViewModel() {

    private val tag = "CodigosQRVM"

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
    private var currentSearch: String? = null


    fun cargarCodigos(url: String? = null, search: String? = currentSearch) {
        viewModelScope.launch {
            if (url == null) _page.value = 1
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                Log.d(tag, "Using token length ${token.length}")

                val baseUrl ="https://bit.cs3.mx/api/v1/invitaciones-detalle/?perimetro_id=$perimetroId&empresa_id=$empresaId&page=${_page.value}&page_size=$pageSize"

                val finalUrl = url ?: if (search.isNullOrBlank()) {
                    baseUrl
                } else {
                    baseUrl + "&search=" + URLEncoder.encode(search, "UTF-8")
                }
                Log.d(tag, "Requesting $finalUrl")

                val request = Request.Builder()
                    .url(finalUrl)
                    .get()
                    .addHeader("x-session-token", token)
                    .build()

                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60, TimeUnit.SECONDS)
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    Log.d(tag, "Response code: ${resp.code}")

                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val list = mutableListOf<CodigoQR>()
                        if (!jsonStr.isNullOrBlank()) {
                            try {
                                val obj = JSONObject(jsonStr)
                                nextUrl = obj.optString("next", null).takeIf { it != "null" }
                                prevUrl = obj.optString("previous", null).takeIf { it != "null" }
                                val arr = obj.optJSONArray("results") ?: JSONArray().apply {
                                    if (length() == 0 && obj.has("id_qr")) {
                                        put(obj)
                                    }
                                }
                                val total = obj.optInt("count").takeIf { it > 0 } ?: arr.length()
                                _pageCount.value = if (total > 0) {
                                    (total + pageSize - 1) / pageSize
                                } else {
                                    1
                                }
                                for (i in 0 until arr.length()) {
                                    arr.optJSONObject(i)?.let { item ->
                                        parseCodigo(item)?.let { list.add(it) }
                                    }
                                }
                            } catch (e: JSONException) {
                                val arr = JSONArray(jsonStr)
                                nextUrl = null
                                prevUrl = null
                                val total = arr.length()
                                _pageCount.value = if (total > 0) {
                                    (total + pageSize - 1) / pageSize
                                } else {
                                    1
                                }
                                for (i in 0 until arr.length()) {
                                    arr.optJSONObject(i)?.let { item ->
                                        parseCodigo(item)?.let { list.add(it) }
                                    }
                                }
                            }
                        }
                        _codigos.value = list
                        currentSearch = search

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

    fun buscarCodigos(query: String) {
        currentSearch = query.takeIf { it.isNotBlank() }
        cargarCodigos()
    }

    fun borrarCodigo(id: Int) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply { put("id_qr", id) }
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
                    put("id_qr", id)
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
private fun parseCodigo(item: JSONObject): CodigoQR? {
    val idQr = when (val raw = item.opt("id_qr")) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: 0
        else -> 0
    }
    if (idQr == 0) return null
    return CodigoQR(
        idQr = idQr,
        nombre_invitado = item.optString("nombre_invitado"),
        nombre_invitante = item.optString("nombre_invitante"),
        destino = item.optString("destino"),
        caducidad_dias = item.optDouble("caducidad_dias"),
        estado = item.optString("estado"),
        periodo_activo = item.optString("periodo_activo")
    )
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
