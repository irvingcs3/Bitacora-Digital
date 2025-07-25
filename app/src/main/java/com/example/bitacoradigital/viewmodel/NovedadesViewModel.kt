package com.example.bitacoradigital.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class NovedadesViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _comentarios = MutableStateFlow<List<Novedad>>(emptyList())
    val comentarios: StateFlow<List<Novedad>> = _comentarios.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _destacados = MutableStateFlow<Set<Int>>(emptySet())
    val destacados: StateFlow<Set<Int>> = _destacados.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.destacados.collect { set ->
                _destacados.value = set.mapNotNull { it.toIntOrNull() }.toSet()
            }
        }
    }

    fun clearError() { _error.value = null }

    fun cargarComentarios() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                resp.use { r ->
                    if (r.isSuccessful) {
                        val arr = JSONArray(r.body?.string() ?: "[]")
                        val list = mutableListOf<Novedad>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val padreRaw = obj.opt("padre")
                            val padreRoot = when (padreRaw) {
                                null -> null
                                is Int -> if (padreRaw == 0) null else padreRaw
                                else -> null
                            }
                            if (obj.optInt("perimetro") == perimetroId && padreRoot == null) {
                                list.add(parseNovedad(obj))
                            }
                        }
                        _comentarios.value = list
                    } else {
                        _error.value = "Error ${r.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }

    fun editarComentario(id: Int, contenido: String) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val json = JSONObject().apply { put("contenido", contenido) }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .patch(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarComentarios()
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

    fun eliminarComentario(id: Int) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/${id}/")
                    .delete()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        cargarComentarios()
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

    private fun parseNovedad(obj: JSONObject): Novedad {
        val respuestasArr = obj.optJSONArray("respuestas") ?: JSONArray()
        val hijos = mutableListOf<Novedad>()
        for (i in 0 until respuestasArr.length()) {
            val child = respuestasArr.getJSONObject(i)
            hijos.add(parseNovedad(child))
        }
        val padreVal = obj.optInt("padre")
        val padreId = when {
            obj.isNull("padre") -> null
            padreVal == 0 -> null
            else -> padreVal
        }
        return Novedad(
            id = obj.optInt("id"),
            autor = obj.optString("autor"),
            contenido = obj.optString("contenido"),
            imagen = if (obj.isNull("imagen")) null else obj.optString("imagen"),
            fecha_creacion = obj.optString("fecha_creacion"),
            perimetro = obj.optInt("perimetro"),
            padre = padreId,
            respuestas = hijos
        )
    }

    fun publicarComentario(context: Context, contenido: String, imagenUri: Uri?, padreId: Int?) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("contenido", contenido)
                    .addFormDataPart("perimetro", perimetroId.toString())
                if (padreId != null) {
                    builder.addFormDataPart("padre", padreId.toString())
                }
                if (imagenUri != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(imagenUri)?.use { it.readBytes() }
                    }
                    bytes?.let {
                        builder.addFormDataPart(
                            "imagen",
                            "imagen.jpg",
                            it.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                    }
                }
                val requestBody = builder.build()
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/novedad/")
                    .post(requestBody)
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val resp = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                resp.use { r ->
                    if (!r.isSuccessful) {
                        _error.value = "Error ${r.code}"
                    } else {
                        cargarComentarios()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _cargando.value = false
            }
        }
    }
    fun toggleDestacado(id: Int) {
        viewModelScope.launch {
            prefs.toggleDestacado(id)
        }
    }
}

class NovedadesViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NovedadesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NovedadesViewModel(prefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}