package com.example.bitacoradigital.repository

import com.example.bitacoradigital.model.LoginRequest
import com.example.bitacoradigital.model.LoginResponse
import com.example.bitacoradigital.model.PerfilResponse
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.network.RetrofitInstance

class UserRepository(private val apiService: ApiService) {

    suspend fun login(email: String, password: String): LoginResponse {
        val request = LoginRequest(email = email, password = password)
        return RetrofitInstance.authApi.login(request)
    }

    suspend fun fetchPerfiles(token: String): PerfilResponse {
        return apiService.getPerfilActivo("Bearer $token")
    }

}
