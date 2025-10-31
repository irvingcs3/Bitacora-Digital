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
        private const val CHECKPOINT_TORNIQUETES = 89
        private const val CHECKPOINT_TORNIQUETES_STANCE = 94
        private const val PERIMETRO_TORNIQUETES_STANCE = 4166
        private const val PERIMETRO_STANCE = 4378
        private const val OBTENER_DESTINO_URL = "http://qr.cs3.mx/bite/obtener-destino"
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

                // (NUEVO) Consultar destino para decidir si aplicamos override específico 94 -> 89
                val destinoInfo = withContext(Dispatchers.IO) { consultarDestino(codigo) }

// Override SÓLO si sel=94 y destino=Stance (independiente del perimetroId actual)
                val checkpointIdForRequest = obtenerCheckpointOverrideStance(
                    checkpointSeleccionadoId = checkpoint.checkpoint_id,
                    destinoInfo = destinoInfo,
                    codigo = codigo
                ) ?: checkpoint.checkpoint_id

                Log.d(TAG, "Override check: sel=${checkpoint.checkpoint_id} destinoId=${destinoInfo?.destinoId} " +
                        "destNom=${destinoInfo?.nombreDestino} final=$checkpointIdForRequest")


                val json = JSONObject().apply {
                    put("codigo", codigo)
                    put("checkpoint", checkpointIdForRequest)
                    put("x-session-token", token)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())

                Log.d(
                    TAG,
                    "Enviando handheld codigo=$codigo checkpoint=$checkpointIdForRequest" +
                            if (checkpointIdForRequest != checkpoint.checkpoint_id) " (override 94->89)" else ""
                )

                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val urls = listOf(
                    "http://192.168.2.200:3000/api/qr/leer",
                    "http://192.168.9.200:3000/api/qr/leer",
                    "http://192.168.100.8:3000/api/qr/leer"
                )

                var response: okhttp3.Response? = null
                for (url in urls) {
                    try {
                        val req = Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .build()
                        response = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                        break // primera que responda, salimos
                    } catch (_: Exception) {
                        // Intentar siguiente URL en caso de timeout o fallo de conexión
                    }
                }

                val resp = response ?: run {
                    networkError.value = "Este modulo esta unicamente pensado para redes internas de Lomas Country"
                    return@launch
                }

                resp.use {
                    val resStr = withContext(Dispatchers.IO) { it.body?.string() }
                    Log.d(TAG, "Respuesta: $resStr")
                    val obj = JSONObject(resStr ?: "{}")

                    val estado = obj.optString("estado", null)
                    val detalle = obj.optJSONObject("detalle")
                    val estadoDetalle = detalle?.optString("estado")
                    _resultado.value = when {
                        !estado.isNullOrBlank() -> estado
                        !estadoDetalle.isNullOrBlank() -> estadoDetalle
                        else -> obj.optString("error", "error")
                    }

                    obj.optString("crop")?.takeIf { it.isNotBlank() }?.let { base ->
                        val bytes = Base64.decode(base, Base64.DEFAULT)
                        imagenCrop.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
    // === Helpers para detectar destino y aplicar override 94 -> 89 (caso específico) ===

    private data class DestinoLookup(
        val ok: Boolean,
        val destinoId: Int?,
        val nombreDestino: String?,
        val error: String?
    )

    private suspend fun consultarDestino(codigo: String): DestinoLookup? {
        val json = JSONObject().apply { put("codigo", codigo) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(OBTENER_DESTINO_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        val client = OkHttpClient()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { resp ->
                    val responseBody = resp.body?.string()
                    Log.d(TAG, "obtener-destino handheld (${resp.code}): $responseBody")
                    if (responseBody.isNullOrBlank()) null
                    else {
                        val obj = JSONObject(responseBody)
                        DestinoLookup(
                            ok = obj.optBoolean("ok"),
                            destinoId = if (obj.has("destino")) obj.optInt("destino") else null,
                            nombreDestino = obj.optString("nombre_destino", null).takeIf { !it.isNullOrBlank() },
                            error = obj.optString("error", null).takeIf { !it.isNullOrBlank() }
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando obtener-destino (handheld)", e)
                null
            }
        }
    }

    private fun obtenerCheckpointOverrideStance(
        checkpointSeleccionadoId: Int,
        destinoInfo: DestinoLookup?,
        codigo: String
    ): Int? {
        // Solo si el operador seleccionó Torniquetes-Stance (94)
        if (checkpointSeleccionadoId != CHECKPOINT_TORNIQUETES_STANCE) return null

        val info = destinoInfo ?: return null
        if (!info.ok) return null

        // Detectar que el QR sea de destino "Stance"
        val esStancePorNombre = info.nombreDestino?.equals("stance", ignoreCase = true) == true
        val esStance = (info.destinoId == PERIMETRO_STANCE) || esStancePorNombre
        if (!esStance) return null

        Log.d(TAG, "Override handheld 94 -> 89 aplicado para QR $codigo")
        return CHECKPOINT_TORNIQUETES
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

