package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
import com.example.bitacoradigital.model.SignupRequest
import com.example.bitacoradigital.model.SignupResponse
import com.example.bitacoradigital.model.VerifyEmailRequest
import com.example.bitacoradigital.network.RetrofitInstance
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignupViewModel : ViewModel() {
    var signupState by mutableStateOf<String?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    private suspend fun verificarNumeroWhatsApp(numero: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply { put("number", numero) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://bit.cs3.mx/v1/whatsapp/exist/number")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val res = JSONObject(response.body?.string() ?: "{}")
                res.optBoolean("exist", false)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Signup", "Error verificando numero", e)
            false
        }
    }

    fun signup(
        request: SignupRequest,
        sessionViewModel: SessionViewModel,
        onAwaitCode: () -> Unit
    ) {
        viewModelScope.launch {
            loading = true
            try {
                val valido = verificarNumeroWhatsApp(request.telefono)
                if (!valido) {
                    signupState = "Número inválido o no verificado en WhatsApp"
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.authApi.signup(request)
                }
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
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.authApi.verifyEmail(token, VerifyEmailRequest(code))
                }
                val newToken = response.meta.session_token ?: token
                sessionViewModel.guardarSesion(newToken, response.data.user)
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
