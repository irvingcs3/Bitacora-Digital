package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bitacoradigital.model.PasswordRequest
import com.example.bitacoradigital.model.PasswordResetRequest
import com.example.bitacoradigital.model.SignupResponse
import com.example.bitacoradigital.network.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    var state by mutableStateOf<String?>(null)
        private set

    fun requestCode(email: String, sessionViewModel: SessionViewModel, onAwaitCode: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.authApi.passwordRequest(PasswordRequest(email))
                val body = if (response.isSuccessful) {
                    response.body()
                } else {
                    val json = response.errorBody()?.string()
                    json?.let { Gson().fromJson(it, SignupResponse::class.java) }
                }
                val token = body?.meta?.session_token
                if (token != null) {
                    sessionViewModel.setTemporalToken(token)
                    state = null
                    onAwaitCode()
                } else {
                    state = "Error en la respuesta"
                }
            } catch (e: Exception) {
                state = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun resetPassword(code: String, password: String, sessionViewModel: SessionViewModel, homeViewModel: HomeViewModel, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = sessionViewModel.token.value ?: return@launch
                val response = RetrofitInstance.authApi.passwordReset(token, PasswordResetRequest(code, password))
                sessionViewModel.guardarSesion(response.meta.session_token, response.data.user)
                homeViewModel.cargarDesdeLogin(response.data.user, sessionViewModel)
                state = null
                onSuccess()
            } catch (e: Exception) {
                state = "Error: ${e.localizedMessage}"
            }
        }
    }
}
