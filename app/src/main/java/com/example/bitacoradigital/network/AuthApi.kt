package com.example.bitacoradigital.network

import com.example.bitacoradigital.model.LoginRequest
import com.example.bitacoradigital.model.LoginResponse
import com.example.bitacoradigital.model.SignupRequest
import com.example.bitacoradigital.model.SignupResponse
import com.example.bitacoradigital.model.VerifyEmailRequest
import com.example.bitacoradigital.model.PasswordRequest
import com.example.bitacoradigital.model.PasswordResetRequest
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("_allauth/app/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("_allauth/app/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): retrofit2.Response<SignupResponse>

    @POST("_allauth/app/v1/auth/email/verify")
    suspend fun verifyEmail(
        @Header("x-session-token") token: String,
        @Body request: VerifyEmailRequest
    ): LoginResponse

    @POST("_allauth/app/v1/auth/password/request")
    suspend fun passwordRequest(
        @Body request: PasswordRequest
    ): retrofit2.Response<SignupResponse>

    @POST("_allauth/app/v1/auth/password/reset")
    suspend fun passwordReset(
        @Header("x-session-token") token: String,
        @Body request: PasswordResetRequest
    ): LoginResponse

}
