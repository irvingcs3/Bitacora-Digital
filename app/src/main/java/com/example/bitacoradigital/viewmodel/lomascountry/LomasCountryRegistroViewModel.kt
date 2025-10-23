package com.example.bitacoradigital.viewmodel.lomascountry

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LomasCountryRegistroViewModel(
    apiService: ApiService,
    sessionPrefs: SessionPreferences,
    perimetroId: Int
) : RegistroVisitaViewModel(apiService, sessionPrefs, perimetroId, true) {

    private val _telefonosDisponibles = MutableStateFlow<List<String>>(emptyList())
    val telefonosDisponibles: StateFlow<List<String>> = _telefonosDisponibles

    private val _errorTelefonos = MutableStateFlow<String?>(null)
    val errorTelefonos: StateFlow<String?> = _errorTelefonos

    private var telefonosPollingJob: Job? = null

    private val telefonosHttpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .build()

    private val telefonosEndpoints = listOf(
        "http://192.168.9.200:3457/bite/registro/list",
        "http://192.168.2.200:3457/bite/registro/list",
        "http://192.168.8.100:3457/bite/registro/list"
    )

    init {
        iniciarActualizacionTelefonos()
    }

    fun iniciarActualizacionTelefonos() {
        if (telefonosPollingJob?.isActive == true) return
        telefonosPollingJob = viewModelScope.launch {
            while (isActive) {
                val telefonos = obtenerTelefonosDisponibles()
                if (telefonos != null) {
                    _telefonosDisponibles.value = telefonos
                    _errorTelefonos.value = null
                } else if (_telefonosDisponibles.value.isEmpty()) {
                    _errorTelefonos.value = "No se pudo obtener la lista de números"
                }
                delay(2000)
            }
        }
    }

    fun detenerActualizacionTelefonos() {
        telefonosPollingJob?.cancel()
        telefonosPollingJob = null
    }

    fun reiniciarSeleccionTelefono() {
        super.reiniciar()
        iniciarActualizacionTelefonos()
    }

    private suspend fun obtenerTelefonosDisponibles(): List<String>? = withContext(Dispatchers.IO) {
        for (endpoint in telefonosEndpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .get()
                    .addHeader("Accept", "application/json")
                    .build()
                val response = telefonosHttpClient.newCall(request).execute()
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        val json = JSONObject(body ?: "{}")
                        val array = json.optJSONArray("list") ?: JSONArray()
                        val list = mutableListOf<String>()
                        for (i in 0 until array.length()) {
                            val numero = array.optString(i)
                            if (numero.isNotBlank()) {
                                list.add(numero)
                            }
                        }
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                Log.w("LomasCountryRegistro", "Error consultando números en $endpoint", e)
            }
        }
        null
    }

    fun prepararRegistroConTelefono(numero: String) {
        val limpio = numero.filter { it.isDigit() }
        reiniciar()
        detenerActualizacionTelefonos()
        telefono.value = limpio
        numeroVerificado.value = true
        cargarJerarquiaDestino()
    }

    override fun onCleared() {
        super.onCleared()
        detenerActualizacionTelefonos()
    }
}
