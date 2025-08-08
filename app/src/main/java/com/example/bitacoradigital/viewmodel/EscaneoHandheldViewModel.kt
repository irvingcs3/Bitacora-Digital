package com.example.bitacoradigital.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
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

    val networkError = MutableStateFlow(false)

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
            networkError.value = false
            try {
                val token = withContext(Dispatchers.IO) { prefs.sessionToken.first() } ?: return@launch
                Log.d(TAG, "Cargando checkpoints para perimetro $perimetroId")
                val list = withContext(Dispatchers.IO) { repo.getCheckpoints(perimetroId, token) }
                _checkpoints.value = list
            } catch (e: Exception) {
                Log.d(TAG, "Error cargando checkpoints", e)
                networkError.value = true
            } finally {
                _cargando.value = false
            }
        }
    }

    fun procesarCodigo(codigo: String) {
        val checkpoint = seleccionado.value ?: return
        viewModelScope.launch {
            _cargando.value = true
            networkError.value = false
            try {
                val json = JSONObject().apply {
                    put("codigo", codigo)
                    put("checkpoint", checkpoint.checkpoint_id)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("http://qr.cs3.mx/bite/leer-qr")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val resStr = withContext(Dispatchers.IO) { resp.body?.string() }
                        Log.d(TAG, "Respuesta exitosa: $resStr")
                        val obj = JSONObject(resStr ?: "{}")
                        val estado = obj.optString("estado", null)
                        _resultado.value = estado ?: obj.optString("error", "error")
                    } else {
                        Log.d(TAG, "Error ${'$'}{resp.code}")
                        _resultado.value = "error"
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Excepcion procesando codigo", e)
                networkError.value = true
            } finally {
                _cargando.value = false
            }
        }
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

