package com.example.bitacoradigital.model

data class PerfilActivo(
    val perfilId: Int,
    val usuarioId: Int,
    val perimetroId: Int,
    val perimetroNombre: String,
    val rolNombre: String,
    val dataPermisos: Map<String, Map<String, List<String>>>
)
