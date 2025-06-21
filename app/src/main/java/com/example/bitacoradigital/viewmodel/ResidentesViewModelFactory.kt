package com.example.bitacoradigital.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bitacoradigital.data.SessionPreferences

class ResidentesViewModelFactory(
    private val prefs: SessionPreferences,
    private val perimetroId: Int,
    private val empresaId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResidentesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResidentesViewModel(prefs, perimetroId, empresaId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
