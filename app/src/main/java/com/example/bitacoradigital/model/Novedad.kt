package com.example.bitacoradigital.model

data class Novedad(
    val id: Int,
    val autor: String,
    val contenido: String,
    val imagen: String?,
    val fecha_creacion: String,
    val perimetro: Int,
    val padre: Int?,
    val respuestas: List<Novedad>
)