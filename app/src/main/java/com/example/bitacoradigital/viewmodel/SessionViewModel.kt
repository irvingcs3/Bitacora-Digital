package com.example.bitacoradigital.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.data.SessionPreferences
import android.util.Log
import com.example.bitacoradigital.model.User
import com.example.bitacoradigital.network.RetrofitInstance
import com.google.gson.Gson
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = SessionPreferences(application)
    private val gson = Gson()
    val sessionToken: StateFlow<String?> get() = _token

    private val _usuario = MutableStateFlow<User?>(null)
    val usuario: StateFlow<User?> = _usuario.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _tieneAccesoABitacora = MutableStateFlow<Boolean?>(null)
    val tieneAccesoABitacora: StateFlow<Boolean?> = _tieneAccesoABitacora.asStateFlow()
    private val _versionOk = MutableStateFlow<Boolean?>(null)
    val versionOk: StateFlow<Boolean?> = _versionOk.asStateFlow()
    val favoritoEmpresaId = prefs.favoritoEmpresaId
    val favoritoPerimetroId = prefs.favoritoPerimetroId
    val uuidBoton = prefs.uuidBoton

    // Check for updates periodically without replacing the current token
    private val REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes


    init {
        recuperarSesion()
        startSessionRefreshJob()
    }

    private fun recuperarSesion() {
        viewModelScope.launch {
            prefs.sessionToken
                .combine(prefs.jsonSession) { token, json -> token to json }
                .collect { (token, json) ->
                    if (!token.isNullOrBlank() && !json.isNullOrBlank()) {
                        try {
                            val user = gson.fromJson(json, User::class.java)
                            val acceso = user.empresas.any { it.B }

                            _token.value = token
                            _usuario.value = user
                            _tieneAccesoABitacora.value = acceso
                            _versionOk.value = null
                        } catch (e: Exception) {
                            // No cerrar sesión si falla el parseo, solo informar
                            Log.e("SessionViewModel", "Error al deserializar sesión", e)
                        }
                    } else {
                        // Si están vacíos o nulos
                        _tieneAccesoABitacora.value = null
                        _versionOk.value = null
                    }
                }
        }
    }

    private fun startSessionRefreshJob() {
        viewModelScope.launch {
            while (true) {
                val token = _token.value
                if (!token.isNullOrBlank()) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            RetrofitInstance.authApi.getSession(token)
                        }
                        val newToken = response.meta.session_token ?: token
                        guardarSesion(newToken, response.data.user)
                    } catch (_: Exception) {
                        // Ignorar errores de refresco
                    }
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }


    suspend fun guardarSesion(token: String, user: User) {
        _token.value = token
        _usuario.value = user
        val tieneAcceso = user.empresas.any { it.B }
        _tieneAccesoABitacora.value = tieneAcceso
        _versionOk.value = user.Version == com.example.bitacoradigital.util.Constants.APP_VERSION
        prefs.guardarSesion(token, user.id, user.persona_id, gson.toJson(user), user.Version)
    }


    fun setTemporalToken(token: String) {
        _token.value = token
    }

    suspend fun cerrarSesion() {
        prefs.cerrarSesion()
        _token.value = null
        _usuario.value = null
        _tieneAccesoABitacora.value = null // ← esto es CRUCIAL
        _versionOk.value = null
    }

    fun actualizarPerfil(
        nombre: String,
        apellidoPat: String,
        apellidoMat: String,
        telefono: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val token = _token.value ?: return@launch
            val actual = _usuario.value ?: return@launch
            try {
                val json = org.json.JSONObject().apply {
                    put("nombre", nombre)
                    put("apellido_pat", apellidoPat)
                    put("apellido_mat", apellidoMat)
                    put("telefono", telefono)
                }
                val body = json.toString()
                    .toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("https://bit.cs3.mx/api/v1/perfil/")
                    .patch(body)
                    .addHeader("x-session-token", token)
                    .addHeader("Content-Type", "application/json")
                    .build()
                val client = okhttp3.OkHttpClient()
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }
                response.use { resp ->
                    if (resp.isSuccessful) {
                        val updated = actual.copy(
                            nombre = nombre,
                            apellido_paterno = apellidoPat,
                            apellido_materno = apellidoMat,
                            telefono = telefono
                        )
                        guardarSesion(token, updated)
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SessionViewModel(context.applicationContext as Application) as T
                }
            }
        }
    }

}
