package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bitacoradigital.model.SignupRequest
import com.example.bitacoradigital.model.SignupResponse
import com.example.bitacoradigital.model.VerifyEmailRequest
import com.example.bitacoradigital.network.RetrofitInstance
import com.google.gson.Gson

import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {
    var signupState by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    fun signup(
        request: SignupRequest,
        sessionViewModel: SessionViewModel,
        onAwaitCode: () -> Unit
    ) {
        viewModelScope.launch {
            loading = true
            try {
                val response = RetrofitInstance.authApi.signup(request)
                if (response.code() == 401) {
                    val json = response.errorBody()?.string()
                    val body = json?.let { Gson().fromJson(it, SignupResponse::class.java) }
                    val token = body?.meta?.session_token
                    if (token != null) {
                        sessionViewModel.setTemporalToken(token)
                        signupState = null
                        onAwaitCode()
                    } else {
                        signupState = "Error en la respuesta"
                    }
                } else if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.meta?.session_token
                    if (token != null) {
                        sessionViewModel.setTemporalToken(token)
                        signupState = null
                        onAwaitCode()
                    } else {
                        signupState = "Respuesta inesperada"
                    }
                } else {
                    signupState = "Error: ${response.code()}"
                }

            } catch (e: Exception) {
                signupState = "Error: ${e.localizedMessage}"
            } finally {
                loading = false
            }
        }
    }

    fun verifyEmail(
        code: String,
        sessionViewModel: SessionViewModel,
        homeViewModel: HomeViewModel,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            loading = true
            try {
                val token = sessionViewModel.token.value ?: return@launch
                val response = RetrofitInstance.authApi.verifyEmail(token, VerifyEmailRequest(code))
                sessionViewModel.guardarSesion(response.meta.session_token, response.data.user)
                homeViewModel.cargarDesdeLogin(response.data.user, sessionViewModel)
                signupState = null
                onSuccess()
            } catch (e: Exception) {
                signupState = "Error: ${e.localizedMessage}"
            } finally {
                loading = false
            }
        }
    }
}
