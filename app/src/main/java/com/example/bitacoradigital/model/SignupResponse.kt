package com.example.bitacoradigital.model

data class SignupResponse(
    val status: Int,
    val data: SignupData,
    val meta: SignupMeta
)

data class SignupData(
    val flows: List<SignupFlow>
)

data class SignupFlow(
    val id: String,
    val is_pending: Boolean? = null
)

data class SignupMeta(
    val is_authenticated: Boolean,
    val session_token: String
)
