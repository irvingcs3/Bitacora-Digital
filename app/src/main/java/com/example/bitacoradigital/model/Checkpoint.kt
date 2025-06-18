package com.example.bitacoradigital.model

/** Modelo para un checkpoint de acceso */
data class Checkpoint(
    val checkpoint_id: Int,
    val nombre: String,
    val tipo: String,
    val perimetro: Int
)
