package com.example.bitacoradigital.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            nombreUsuario = "Admin",
            totalVisitas = 12,
            totalAlertas = 2,
            ultimoEvento = "10:45 AM - Entrada Torre A"
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState
}

data class DashboardUiState(
    val nombreUsuario: String,
    val totalVisitas: Int,
    val totalAlertas: Int,
    val ultimoEvento: String
)
