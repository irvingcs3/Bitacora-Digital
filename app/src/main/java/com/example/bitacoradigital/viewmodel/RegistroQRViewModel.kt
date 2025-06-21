package com.example.bitacoradigital.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Checkpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class RegistroQRViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    private val _checkpoints = MutableStateFlow<List<Checkpoint>>(emptyList())
    val checkpoints: StateFlow<List<Checkpoint>> = _checkpoints.asStateFlow()

    val seleccionado = MutableStateFlow<Checkpoint?>(null)

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val resultado = MutableStateFlow<String?>(null)
    val imagenCrop = MutableStateFlow<Bitmap?>(null)
    val mostrandoImagen = MutableStateFlow(false)

    fun cargarCheckpoints() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val request = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/checkpoints/?perimetro=$perimetroId")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val jsonStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val arr = JSONArray(jsonStr ?: "[]")
                        val list = mutableListOf<Checkpoint>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
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

    fun procesarCodigo(codigo: String) {
        val checkpoint = seleccionado.value ?: return
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val json = JSONObject().apply {
                    put("codigo", codigo)
                    put("checkpoint", checkpoint.checkpoint_id)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/leer-qr")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = OkHttpClient()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val resStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        val obj = JSONObject(resStr ?: "{}")
                        resultado.value = obj.optString("estado")
                        obj.optString("crop")?.takeIf { it.isNotBlank() }?.let { base ->
                            val bytes = Base64.decode(base, Base64.DEFAULT)
                            imagenCrop.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } else {
                        resultado.value = "error"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                resultado.value = "error"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun reiniciar() {
        resultado.value = null
        imagenCrop.value = null
        mostrandoImagen.value = false
    }
}

class RegistroQRViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroQRViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistroQRViewModel(prefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

