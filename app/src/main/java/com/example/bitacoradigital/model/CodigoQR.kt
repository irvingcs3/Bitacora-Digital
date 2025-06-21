package com.example.bitacoradigital.model

/** Modelo para una invitación de QR */
data class CodigoQR(
    val id_invitacion: Int,
    val id_telefono: Int,
    val telefono: String,
    val lada: String?,
    val id_cad_invitacion: Int,
    val id_cad_qr: Int,
    val timestamp_inicio: Long,
    val timestamp_final: Long
)
