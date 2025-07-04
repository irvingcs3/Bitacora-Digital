package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.model.LoginRequest
import com.example.bitacoradigital.network.RetrofitInstance
import com.example.bitacoradigital.model.SignupResponse
import com.google.gson.Gson
import retrofit2.HttpException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.*

class LoginViewModel : ViewModel() {

    var loginState by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    fun clearState() {
        loginState = null
    }

    fun login(
        email: String,
        password: String,
        sessionViewModel: SessionViewModel,
        onLoginSuccess: () -> Unit,
        onLoginDenied: () -> Unit,
        onAwaitCode: () -> Unit,
        onUpdateRequired: () -> Unit
    ) {
        viewModelScope.launch {
            loading = true
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.authApi.login(LoginRequest(email, password))
                }
                val token = response.meta.session_token
                val user = response.data.user

                if (token == null) {
                    loginState = "Respuesta inesperada"
                    return@launch
                }

                if (user.empresas.any { it.B }) {
                    sessionViewModel.guardarSesion(token, user)
                    if (user.Version == com.example.bitacoradigital.util.Constants.APP_VERSION) {
                        loginState = "Login exitoso"
                        onLoginSuccess()
                    } else {
                        loginState = null
                        onUpdateRequired()
                    }
                } else {
                    loginState = "Acceso denegado"
                    onLoginDenied()
                }

            } catch (e: HttpException) {
                if (e.code() == 401) {
                    val json = e.response()?.errorBody()?.string()
                    val body = json?.let { Gson().fromJson(it, SignupResponse::class.java) }
                    val token = body?.meta?.session_token
                    if (token != null) {
                        sessionViewModel.setTemporalToken(token)
                        loginState = null
                        onAwaitCode()
                    } else {
                        loginState = "Cuenta no verificada"
                    }
                } else {
                    loginState = "Error: ${e.localizedMessage}"
                }
            } catch (e: Exception) {
                loginState = "Error: ${e.localizedMessage}"
            } finally {
                loading = false
            }
        }
    }
}
