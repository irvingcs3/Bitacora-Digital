package com.example.bitacoradigital.model

/** Respuesta de contadores del dashboard */
data class DashboardCounts(
    val total_residentes: Int,
    val visitas_hoy: Int,
    val total_qrs: Int
)

/** Estado de invitaciones por QR */
data class InvitacionEstado(
    val estado: String,
    val cantidad: Int
)

/** Registros de accesos por día */
data class VisitasDia(
    val name: String,
    val visitas: Int
)
