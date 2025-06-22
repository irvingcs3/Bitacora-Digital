package com.example.bitacoradigital.model

/** Informacion del seguimiento actual de una invitacion */
data class SeguimientoInfo(
    val fase: String,
    val checkpointActualId: Int?,
    val checkpointActualNombre: String?,
    val checkpointActualPerimetro: String?,
    val siguientePerimetro: String?,
    val siguiente: List<CheckpointSimple>
)

/** Modelo simple de checkpoint */
data class CheckpointSimple(
    val checkpoint_id: Int,
    val nombre: String
)

/** Registro historico de un checkpoint visitado por el QR */
data class HistorialQR(
    val fecha: String,
    val checkpoint: String,
    val perimetro: String
)
