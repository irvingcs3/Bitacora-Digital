package com.example.bitacoradigital.model

/** Modelo para una novedad/comentario tipo foro */
data class Novedad(
    val id: Int,
    val autor: Int,
    val contenido: String,
    val imagen: String?,
    val fecha_creacion: String,
    val perimetro: Int,
    val padre: Int?,
    val respuestas: List<Novedad>
)
