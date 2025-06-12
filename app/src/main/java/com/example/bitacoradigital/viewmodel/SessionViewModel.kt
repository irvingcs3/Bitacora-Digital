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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
    val favoritoEmpresaId = prefs.favoritoEmpresaId
    val favoritoPerimetroId = prefs.favoritoPerimetroId

    // Check for updates periodically without replacing the current token
    private val REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes


    init {
        recuperarSesion()
        startSessionRefreshJob()
    }

    private fun recuperarSesion() {
        viewModelScope.launch {
            prefs.sessionToken.combine(prefs.jsonSession) { token, json ->
                if (!token.isNullOrBlank() && !json.isNullOrBlank()) {
                    try {
                        val user = gson.fromJson(json, User::class.java)
                        val acceso = user.empresas.any { it.B }

                        _token.value = token
                        _usuario.value = user
                        _tieneAccesoABitacora.value = acceso
                    } catch (e: Exception) {
                        // No cerrar sesión si falla el parseo, solo informar
                        Log.e("SessionViewModel", "Error al deserializar sesión", e)
                    }
                } else {
                    // Si están vacíos o nulos
                    _tieneAccesoABitacora.value = null
                }
            }.collect()
        }
    }

    private fun startSessionRefreshJob() {
        viewModelScope.launch {
            while (true) {
                val token = _token.value
                if (!token.isNullOrBlank()) {
                    try {
                        val response = RetrofitInstance.authApi.getSession(token)
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
        prefs.guardarSesion(token, user.id, gson.toJson(user))
    }

    fun setTemporalToken(token: String) {
        _token.value = token
    }

    suspend fun cerrarSesion() {
        prefs.cerrarSesion()
        _token.value = null
        _usuario.value = null
        _tieneAccesoABitacora.value = null // ← esto es CRUCIAL
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
