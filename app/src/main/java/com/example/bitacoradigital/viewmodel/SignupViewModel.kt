package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.bitacoradigital.model.SignupRequest
import com.example.bitacoradigital.model.VerifyEmailRequest
import com.example.bitacoradigital.network.RetrofitInstance
import kotlinx.coroutines.launch

class SignupViewModel : ViewModel() {
    var signupState by mutableStateOf<String?>(null)
        private set

    fun signup(
        request: SignupRequest,
        sessionViewModel: SessionViewModel,
        onAwaitCode: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.authApi.signup(request)
                sessionViewModel.setTemporalToken(response.meta.session_token)
                signupState = null
                onAwaitCode()
            } catch (e: Exception) {
                signupState = "Error: ${e.localizedMessage}"
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
            try {
                val token = sessionViewModel.token.value ?: return@launch
                val response = RetrofitInstance.authApi.verifyEmail(token, VerifyEmailRequest(code))
                sessionViewModel.guardarSesion(response.meta.session_token, response.data.user)
                homeViewModel.cargarDesdeLogin(response.data.user, sessionViewModel)
                signupState = null
                onSuccess()
            } catch (e: Exception) {
                signupState = "Error: ${e.localizedMessage}"
            }
        }
    }
}
