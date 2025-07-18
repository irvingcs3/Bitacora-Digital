package com.example.bitacoradigital.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import org.json.JSONArray
import org.json.JSONObject

class RegistroQRViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "RegistroQR"
    }

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
    private val _nombreSiguientePerimetro = MutableStateFlow<String?>(null)
    val nombreSiguientePerimetro: StateFlow<String?> = _nombreSiguientePerimetro.asStateFlow()

    fun cargarCheckpoints() {
        viewModelScope.launch {
            _cargando.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                Log.d(TAG, "Cargando checkpoints con token=$token y perimetro=$perimetroId")
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
                        Log.d(TAG, "Checkpoints respuesta: $jsonStr")
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
                        val err = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.e(TAG, "Error ${resp.code} al cargar checkpoints: $err")
                        _error.value = "Error ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepcion cargando checkpoints", e)
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
                Log.d(TAG, "Enviando codigo $codigo al checkpoint ${checkpoint.checkpoint_id}")
                Log.d(TAG, "Request body: $json")
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
                        Log.d(TAG, "Respuesta exitosa: $resStr")
                        val obj = JSONObject(resStr ?: "{}")

                        val estado = obj.optString("estado", null)
                        if (!estado.isNullOrBlank()) {
                            resultado.value = estado
                            if (estado == "valido") {
                                obj.optJSONArray("siguiente_checkpoints")?.let { arr ->
                                    if (arr.length() > 0) {
                                        val cpId = arr.getJSONObject(0).optInt("checkpoint_id")
                                        if (cpId != 0) cargarNombreSiguientePerimetro(cpId)
                                    }
                                }
                            }
                        } else {
                            val detalle = obj.optJSONObject("detalle")
                            val estadoDetalle = detalle?.optString("estado")
                            resultado.value = when (estadoDetalle) {
                                "invalido" -> "no permitido"
                                null -> obj.optString("error", "error")
                                else -> estadoDetalle
                            }
                        }

                        obj.optString("crop")?.takeIf { it.isNotBlank() }?.let { base ->
                            val bytes = Base64.decode(base, Base64.DEFAULT)
                            imagenCrop.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } else {
                        val errorBody = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.e(TAG, "Error ${resp.code}: $errorBody")
                        resultado.value = "error"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepcion procesando codigo", e)
                _error.value = e.localizedMessage
                resultado.value = "error"
            } finally {
                _cargando.value = false
            }
        }
    }

    private fun cargarNombreSiguientePerimetro(checkpointId: Int) {
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val client = OkHttpClient()

                val cpRequest = Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/checkpoints/$checkpointId")
                    .get()
                    .addHeader("x-session-token", token)
                    .build()
                val cpResp = withContext(Dispatchers.IO) { client.newCall(cpRequest).execute() }
                cpResp.use { cpr ->
                    if (cpr.isSuccessful) {
                        val cpJson = JSONObject(withContext(Dispatchers.IO) { cpr.body?.string() } ?: "{}")
                        val perimetroIdNext = cpJson.optInt("perimetro")
                        if (perimetroIdNext != 0) {
                            val perRequest = Request.Builder()
                                .url("https://bit.cs3.mx/api/v1/perimetro/$perimetroIdNext")
                                .get()
                                .addHeader("x-session-token", token)
                                .build()
                            val perResp = withContext(Dispatchers.IO) { client.newCall(perRequest).execute() }
                            perResp.use { pr ->
                                if (pr.isSuccessful) {
                                    val perJson = JSONObject(withContext(Dispatchers.IO) { pr.body?.string() } ?: "{}")
                                    _nombreSiguientePerimetro.value = perJson.optString("nombre")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando siguiente perimetro", e)
            }
        }
    }

    fun reiniciar() {
        resultado.value = null
        imagenCrop.value = null
        mostrandoImagen.value = false
        _nombreSiguientePerimetro.value = null
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

