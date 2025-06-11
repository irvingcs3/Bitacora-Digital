package com.example.bitacoradigital.model

data class PasswordResetRequest(
    val key: String,
    val password: String
)
