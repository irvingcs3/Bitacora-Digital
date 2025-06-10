package com.example.bitacoradigital.model

data class JerarquiaNodo(
    val perimetro_id: Int,
    val nombre: String,
    val nivel: Int,
    val children: List<JerarquiaNodo>
)
