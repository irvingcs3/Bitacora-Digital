package com.example.bitacoradigital.model

data class LoginResponse(
    val status: Int,
    val data: LoginData,
    val meta: Meta
)

data class LoginData(
    val user: User,
    val methods: List<AuthMethod>
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val display: String,
    val nombre: String?,
    val apellido_paterno: String?,
    val apellido_materno: String?,
    val has_usable_password: Boolean,
    val empresas: List<Empresa>
)
data class PerimetroVisual(
    val empresaId: Int,
    val empresaNombre: String,
    val perimetroId: Int,
    val perimetroNombre: String,
    val rol: String,
    val modulos: Map<String, List<String>>, // Ej: "Dashboard" -> ["Ver Dashboard"]
    var esFavorito: Boolean = false
)


data class Empresa(
    val id: Int,
    val nombre: String,
    val B: Boolean, // acceso a Bitácora
    val R: Boolean,
    val perimetros: List<Perimetro>
)

data class Perimetro(
    val id: Int,
    val nombre: String,
    val rol: Map<String, Map<String, List<String>>>
)

data class AuthMethod(
    val method: String,
    val at: Double,
    val email: String
)

data class Meta(
    val is_authenticated: Boolean,
    val session_token: String
)
