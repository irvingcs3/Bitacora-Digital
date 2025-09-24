package com.example.bitacoradigital.model

/** Modelo para una invitación de QR con detalles */
data class CodigoQR(
    val idQr: Int,
    val nombre_invitado: String,
    val nombre_invitante: String,
    val destino: String,
    val caducidad_dias: Double,
    val estado: String,
    val periodo_activo: String
)
