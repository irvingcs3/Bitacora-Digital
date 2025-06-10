package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bitacoradigital.model.LoginRequest
import com.example.bitacoradigital.network.RetrofitInstance
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

class LoginViewModel : ViewModel() {

    var loginState by mutableStateOf<String?>(null)
        private set

    fun login(
        email: String,
        password: String,
        sessionViewModel: SessionViewModel,
        onLoginSuccess: () -> Unit,
        onLoginDenied: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.authApi.login(LoginRequest(email, password))
                val token = response.meta.session_token
                val user = response.data.user

                if (user.empresas.any { it.B }) {
                    sessionViewModel.guardarSesion(token, user)
                    loginState = "Login exitoso"
                    onLoginSuccess()
                } else {
                    loginState = "Acceso denegado"
                    onLoginDenied()
                }

            } catch (e: Exception) {
                loginState = "Error: ${e.localizedMessage}"
            }
        }
    }
}
