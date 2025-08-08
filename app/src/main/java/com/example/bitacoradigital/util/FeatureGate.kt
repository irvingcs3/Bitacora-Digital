package com.example.bitacoradigital.util

object FeatureGate {
    fun hasPermission(permission: String, permisos: List<String>): Boolean {
        return permisos.contains(permission)
    }
}

