package com.example.bitacoradigital.model

data class PerfilResponse(
    val id: Int,
    val rol: RolResponse,
    val perimetro: PerimetroResponse,
    val activo: Boolean
)

data class RolResponse(
    val id: Int,
    val nombre: String,
    val permisos: List<PermisoResponse>
)

data class PermisoResponse(
    val modulo: String,
    val funcionalidad: String,
    val subfuncionalidad: String,
    val valor: Boolean
)

data class PerimetroResponse(
    val id: Int,
    val nombre: String,
    val nivel: Int,
    val tipo: String,
    val padre: Int?
)
