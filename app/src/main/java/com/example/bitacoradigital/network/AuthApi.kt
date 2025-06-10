package com.example.bitacoradigital.network

import com.example.bitacoradigital.model.LoginRequest
import com.example.bitacoradigital.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("_allauth/app/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

}
