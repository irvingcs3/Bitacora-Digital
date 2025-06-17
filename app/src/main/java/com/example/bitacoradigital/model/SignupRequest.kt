package com.example.bitacoradigital.model

data class SignupRequest(
    val email: String,
    val password: String,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombreInstancia: String,
    val telefono: String
)
