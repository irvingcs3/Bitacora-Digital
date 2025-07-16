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

    fun clearError() { _error.value = null }

    fun cargarComentarios() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val request = Request.Builder()
                    .url("http://192.168.100.8:8601/api/v1/novedad/")
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
                            if (obj.optInt("perimetro") == perimetroId && obj.isNull("padre")) {
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

    private fun parseNovedad(obj: JSONObject): Novedad {
        val respuestasArr = obj.optJSONArray("respuestas") ?: JSONArray()
        val hijos = mutableListOf<Novedad>()
        for (i in 0 until respuestasArr.length()) {
            val child = respuestasArr.getJSONObject(i)
            hijos.add(parseNovedad(child))
        }
        return Novedad(
            id = obj.optInt("id"),
            autor = obj.optInt("autor"),
            contenido = obj.optString("contenido"),
            imagen = if (obj.isNull("imagen")) null else obj.optString("imagen"),
            fecha_creacion = obj.optString("fecha_creacion"),
            perimetro = obj.optInt("perimetro"),
            padre = if (obj.isNull("padre")) null else obj.optInt("padre"),
            respuestas = hijos
        )
    }

    fun publicarComentario(context: Context, contenido: String, imagenUri: Uri?, padreId: Int?) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.firstOrNull() } ?: return@launch
                val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("contenido", contenido)
                    .addFormDataPart("perimetro", perimetroId.toString())
                    .addFormDataPart("padre", padreId?.toString() ?: "null")
                if (imagenUri != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(imagenUri)?.use { it.readBytes() }
                    }
                    bytes?.let {
                        builder.addFormDataPart(
                            "file",
                            "imagen.jpg",
                            it.toRequestBody("image/*".toMediaTypeOrNull())
                        )
                    }
                }
                val requestBody = builder.build()
                val request = Request.Builder()
                    .url("http://192.168.100.8:8601/api/v1/novedad/")
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