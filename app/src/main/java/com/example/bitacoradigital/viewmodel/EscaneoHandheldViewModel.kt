package com.example.bitacoradigital.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Base64
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Checkpoint
import com.example.bitacoradigital.repository.CheckpointRepository
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
import java.util.concurrent.TimeUnit

class EscaneoHandheldViewModel(
    private val prefs: SessionPreferences,
    private val perimetroId: Int,
    private val repo: CheckpointRepository = CheckpointRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "EscaneoHandheld"
    }

    private val _checkpoints = MutableStateFlow<List<Checkpoint>>(emptyList())
    val checkpoints: StateFlow<List<Checkpoint>> = _checkpoints.asStateFlow()

    val seleccionado = MutableStateFlow<Checkpoint?>(null)
    val scannedText = MutableStateFlow<String?>(null)

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _resultado = MutableStateFlow<String?>(null)
    val resultado: StateFlow<String?> = _resultado.asStateFlow()

    val imagenCrop = MutableStateFlow<Bitmap?>(null)
    val mostrandoImagen = MutableStateFlow(false)

    val networkError = MutableStateFlow<String?>(null)

    private val scannerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getStringExtra("data")
            if (!data.isNullOrBlank()) {
                Log.d(TAG, "Scan recibido: $data")
                scannedText.value = data
            }
        }
    }

    fun startScanner(context: Context) {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(scannerReceiver, IntentFilter("scanner-data"))
    }

    fun stopScanner(context: Context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(scannerReceiver)
    }

    fun cargarCheckpoints() {
        viewModelScope.launch {
            _cargando.value = true
            networkError.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                Log.d(TAG, "Cargando checkpoints para perimetro $perimetroId")
                val list = withContext(Dispatchers.IO) { repo.getCheckpoints(perimetroId, token) }
                _checkpoints.value = list
            } catch (e: Exception) {
                Log.d(TAG, "Error cargando checkpoints", e)
                networkError.value = "Error de red"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun procesarCodigo(codigo: String) {
        val checkpoint = seleccionado.value ?: return
        viewModelScope.launch {
            _cargando.value = true
            networkError.value = null
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                val json = JSONObject().apply {
                    put("codigo", codigo)
                    put("checkpoint", checkpoint.checkpoint_id)
                    put("x-session-token", token)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                val urls = listOf(
                    "http://192.168.2.200:3000/api/qr/leer",
                    "http://192.168.9.200:3000/api/qr/leer"
                )
                var successResponse: okhttp3.Response? = null
                for (url in urls) {
                    try {
                        val req = Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .build()
                        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                        if (resp.isSuccessful) {
                            successResponse = resp
                            break
                        } else {
                            resp.close()
                        }
                    } catch (_: Exception) {
                        // Intentar siguiente URL
                    }
                }
                val response = successResponse ?: run {
                    networkError.value = "Este modulo esta unicamente pensado para redes internas de Lomas Country"
                    return@launch
                }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val resStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.d(TAG, "Respuesta exitosa: $resStr")
                        val obj = JSONObject(resStr ?: "{}")
                        val estado = obj.optString("estado", null)
                        if (!estado.isNullOrBlank()) {
                            _resultado.value = estado
                        } else {
                            val detalle = obj.optJSONObject("detalle")
                            val estadoDetalle = detalle?.optString("estado")
                            _resultado.value = when (estadoDetalle) {
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
                        Log.d(TAG, "Error ${'$'}{resp.code}")
                        _resultado.value = "error"
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Excepcion procesando codigo", e)
                networkError.value = "Error de red"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun reiniciar() {
        _resultado.value = null
        imagenCrop.value = null
        mostrandoImagen.value = false
    }
}

class EscaneoHandheldViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EscaneoHandheldViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EscaneoHandheldViewModel(prefs, perimetroId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

