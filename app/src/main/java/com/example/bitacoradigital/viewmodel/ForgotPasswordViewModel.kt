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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForgotPasswordViewModel : ViewModel() {
    var state by mutableStateOf<String?>(null)
        private set

    fun requestCode(email: String, sessionViewModel: SessionViewModel, onAwaitCode: () -> Unit) {
        viewModelScope.launch {

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.authApi.passwordRequest(PasswordRequest(email))
                }
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

    fun resetPassword(
        code: String,
        password: String,
        sessionViewModel: SessionViewModel,
        onSuccess: () -> Unit,
        onUpdateRequired: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val token = sessionViewModel.token.value ?: return@launch
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.authApi.passwordReset(
                        token,
                        PasswordResetRequest(code, password)
                    )
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val newToken = body.meta.session_token ?: token
                        sessionViewModel.guardarSesion(newToken, body.data.user)
                        state = null
                        if (body.data.user.Version == com.example.bitacoradigital.util.Constants.APP_VERSION) {
                            onSuccess()
                        } else {
                            onUpdateRequired()
                        }
                    } else {
                        state = "Respuesta inesperada"
                    }
                } else if (response.code() == 401) {
                    val json = response.errorBody()?.string()
                    val body = json?.let { Gson().fromJson(it, SignupResponse::class.java) }
                    val pending = body?.data?.flows?.find { it.id == "password_reset_by_code" }?.is_pending
                    if (pending == false) {
                        state = null
                        onSuccess()
                    } else {
                        state = "Código incorrecto"
                    }
                } else {
                    state = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                state = "Error: ${e.localizedMessage}"

            }
        }
    }
}
